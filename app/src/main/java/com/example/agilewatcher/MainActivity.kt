package com.example.agilewatcher

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.agilewatcher.ui.AgileViewModel
import com.example.agilewatcher.ui.screens.*
import com.example.agilewatcher.ui.theme.*

class MainActivity : ComponentActivity() {

    private val viewModel: AgileViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val state by viewModel.uiState.collectAsState()
            val snackbarHostState = remember { SnackbarHostState() }
            var selectedTab by remember { mutableStateOf(0) } // 0: Dashboard, 1: Electricity, 2: Gas, 3: Settings
            val context = LocalContext.current

            val notifPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { _ -> }

            fun ensureNotificationPermission() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val granted = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                    if (!granted) {
                        notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }

            LaunchedEffect(state.userNotification) {
                state.userNotification?.let { msg ->
                    snackbarHostState.showSnackbar(msg)
                    viewModel.clearNotification()
                }
            }

            AgileWatcherTheme(
                themeMode = state.appPreferences.themeMode,
                forceDark = state.showOnboardingScreen
            ) {
                val colors = LocalAgileColors.current

                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    containerColor = colors.pageBackground,
                    bottomBar = {
                        if (!state.showOnboardingScreen) {
                            AgileFloatingBottomBar(
                                selectedTab = selectedTab,
                                onSelectTab = { tab ->
                                    // If user taps Electricity or Gas tab while already on Day Detail, go back to 30-day overview
                                    if (tab == 1 && selectedTab == 1) viewModel.selectElectricDay(null)
                                    if (tab == 2 && selectedTab == 2) viewModel.selectGasDay(null)
                                    selectedTab = tab
                                }
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                ) { paddingValues ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        if (state.showOnboardingScreen) {
                            OnboardingScreen(
                                initialApiKey = state.accountConfig.apiKey,
                                initialAccountNumber = state.accountConfig.accountNumber,
                                initialRegionCode = state.accountConfig.regionCode,
                                isDiscovering = state.isDiscoveringAccount,
                                onConnectApiKey = { key, acc ->
                                    viewModel.connectWithApiKey(key, acc)
                                },
                                onSkipWithPublicRegion = { regionCode ->
                                    viewModel.skipOnboardingToPublicPrices(regionCode)
                                }
                            )
                        } else {
                            when (selectedTab) {
                                0 -> DashboardScreen(
                                    state = state,
                                    onRefresh = { viewModel.refreshAll() },
                                    onToggleEarlierPrices = { viewModel.toggleEarlierPricesToday() },
                                    onOpenBestTimes = { viewModel.showBestTimes(true) },
                                    onScheduleSlotReminder = { slot ->
                                        ensureNotificationPermission()
                                        viewModel.scheduleSlotReminder(slot)
                                    }
                                )

                                1 -> ConsumptionTabScreen(
                                    title = "Electric Consumption",
                                    isGas = false,
                                    overview = state.electricOverview,
                                    selectedDay = state.selectedElectricDay,
                                    useThreeDecimals = state.appPreferences.useThreeDecimals,
                                    onSelectDay = { viewModel.selectElectricDay(it) },
                                    onOpenSettings = { selectedTab = 3 }
                                )

                                2 -> ConsumptionTabScreen(
                                    title = "Gas Consumption",
                                    isGas = true,
                                    overview = state.gasOverview,
                                    selectedDay = state.selectedGasDay,
                                    useThreeDecimals = state.appPreferences.useThreeDecimals,
                                    onSelectDay = { viewModel.selectGasDay(it) },
                                    onOpenGasTariffSetup = { viewModel.showGasTariffDialog(true) },
                                    onOpenSettings = { selectedTab = 3 }
                                )

                                3 -> SettingsScreen(
                                    state = state,
                                    onAutoDiscoverAccount = { key, acc ->
                                        viewModel.connectWithApiKey(key, acc)
                                    },
                                    onSaveAccountConfig = { cfg ->
                                        viewModel.updateAccountConfig(cfg)
                                    },
                                    onUpdatePreferences = { prefs ->
                                        viewModel.updateAppPreferences(prefs)
                                    },
                                    onReopenOnboarding = {
                                        viewModel.showOnboarding(true)
                                    }
                                )
                            }
                        }

                        // Best Times Modal Sheet (Screenshot 03)
                        if (state.isBestTimesVisible) {
                            BestTimesModal(
                                state = state,
                                onRefresh = { viewModel.refreshAll() },
                                onSelectDuration = { viewModel.setBestTimesDuration(it) },
                                onSelectChartDay = { viewModel.setBestTimesChartDay(it) },
                                onRemindWindow = { window ->
                                    ensureNotificationPermission()
                                    viewModel.scheduleWindowReminder(window)
                                },
                                onDismiss = { viewModel.showBestTimes(false) }
                            )
                        }

                        // Gas Tariff Setup Dialog (Screenshot 06)
                        if (state.isGasTariffDialogVisible) {
                            GasTariffSetupDialog(
                                config = state.accountConfig,
                                onSave = { unitRate, standing, isM3, mprn, serial ->
                                    viewModel.updateGasTariffSettings(unitRate, standing, isM3, mprn, serial)
                                },
                                onDismiss = { viewModel.showGasTariffDialog(false) }
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class NavTabItem(
    val index: Int,
    val label: String,
    val icon: ImageVector
)

/**
 * Floating capsule Bottom Navigation Bar matching the iOS Agile Watcher tab bar
 * in Screenshots 02, 04, and 06 (`Dashboard`, `Electricity`, `Gas`, `Settings`).
 */
@Composable
private fun AgileFloatingBottomBar(
    selectedTab: Int,
    onSelectTab: (Int) -> Unit
) {
    val colors = LocalAgileColors.current
    val items = listOf(
        NavTabItem(0, "Dashboard", Icons.Default.Speed),
        NavTabItem(1, "Electricity", Icons.Default.Bolt),
        NavTabItem(2, "Gas", Icons.Default.LocalFireDepartment),
        NavTabItem(3, "Settings", Icons.Default.Settings)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 10.dp)
    ) {
        Surface(
            color = colors.cardSurface.copy(alpha = 0.96f),
            shape = RoundedCornerShape(34.dp),
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, colors.gridLine, RoundedCornerShape(34.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    val isSelected = selectedTab == item.index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(28.dp))
                            .background(
                                if (isSelected) colors.bottomBarPill else androidx.compose.ui.graphics.Color.Transparent
                            )
                            .clickable { onSelectTab(item.index) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                tint = if (isSelected) AgilePinkPrimary else colors.textPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = item.label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) AgilePinkPrimary else colors.textPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}
