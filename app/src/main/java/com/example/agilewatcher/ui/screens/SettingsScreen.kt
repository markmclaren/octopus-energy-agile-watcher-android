package com.example.agilewatcher.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.agilewatcher.data.*
import com.example.agilewatcher.ui.AgileUiState
import com.example.agilewatcher.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: AgileUiState,
    onAutoDiscoverAccount: (apiKey: String, accountNumber: String) -> Unit,
    onSaveAccountConfig: (OctopusAccountConfig) -> Unit,
    onUpdatePreferences: (AppPreferences) -> Unit,
    onReopenOnboarding: () -> Unit
) {
    val colors = LocalAgileColors.current
    val scrollState = rememberScrollState()

    val cfg = state.accountConfig
    val prefs = state.appPreferences

    var apiKey by remember(cfg.apiKey) { mutableStateOf(cfg.apiKey) }
    var showApiKey by remember { mutableStateOf(false) }
    var accountNumber by remember(cfg.accountNumber) { mutableStateOf(cfg.accountNumber) }
    var productCode by remember(cfg.agileProductCode) { mutableStateOf(cfg.agileProductCode) }
    var selectedRegion by remember(cfg.regionCode) { mutableStateOf(findGspRegion(cfg.regionCode)) }
    var tariffCode by remember(cfg.agileTariffCode) { mutableStateOf(cfg.agileTariffCode) }
    var electricMpan by remember(cfg.electricMpan) { mutableStateOf(cfg.electricMpan) }
    var electricSerial by remember(cfg.electricSerial) { mutableStateOf(cfg.electricSerial) }
    var elecStandingPence by remember(cfg.electricStandingChargePence) { mutableStateOf(cfg.electricStandingChargePence.toString()) }

    var gasMprn by remember(cfg.gasMprn) { mutableStateOf(cfg.gasMprn) }
    var gasSerial by remember(cfg.gasSerial) { mutableStateOf(cfg.gasSerial) }
    var gasUnitRatePence by remember(cfg.gasUnitRatePence) { mutableStateOf(cfg.gasUnitRatePence.toString()) }
    var gasStandingPence by remember(cfg.gasStandingChargePence) { mutableStateOf(cfg.gasStandingChargePence.toString()) }
    var gasIsCubicMeters by remember(cfg.gasIsCubicMeters) { mutableStateOf(cfg.gasIsCubicMeters) }

    var highCostThresholdText by remember(prefs.highCostThresholdPence) { mutableStateOf(prefs.highCostThresholdPence.toString()) }

    var regionExpanded by remember { mutableStateOf(false) }
    var productExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.pageBackground)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Settings",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Octopus Account & Auto-Discovery Card
            Surface(
                color = colors.cardSurface,
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Key, contentDescription = null, tint = AgilePinkPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Octopus Energy Account",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                        }
                        TextButton(onClick = onReopenOnboarding) {
                            Text("Welcome Wizard", color = AgilePinkPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    if (cfg.addressSummary.isNotBlank()) {
                        Text(
                            text = "📍 Connected Home: ${cfg.addressSummary}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = LowestRateGreen
                        )
                    }

                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        label = { Text("Octopus API Key (sk_live_...)") },
                        singleLine = true,
                        visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showApiKey = !showApiKey }) {
                                Icon(
                                    imageVector = if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle key visibility"
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = accountNumber,
                        onValueChange = { accountNumber = it },
                        label = { Text("Account Number (e.g. A-AAAA1111, auto-detected if blank)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = { onAutoDiscoverAccount(apiKey, accountNumber) },
                        enabled = !state.isDiscoveringAccount,
                        colors = ButtonDefaults.buttonColors(containerColor = AgilePinkPrimary),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (state.isDiscoveringAccount) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Finding Home & Meters...", color = Color.White, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.Search, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Auto-Discover Meters & Tariff from Octopus", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 2. Agile Tariff & UK GSP Region Configuration Card
            Surface(
                color = colors.cardSurface,
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Bolt, contentDescription = null, tint = AgilePinkPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Agile Electricity Tariff & Meter",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                    }

                    // Region Selector
                    ExposedDropdownMenuBox(
                        expanded = regionExpanded,
                        onExpandedChange = { regionExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = "Region ${selectedRegion.code} — ${selectedRegion.name}",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("UK DNO / GSP Region") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = regionExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = regionExpanded,
                            onDismissRequest = { regionExpanded = false },
                            containerColor = colors.cardSurface
                        ) {
                            UK_GSP_REGIONS.forEach { reg ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                "Region ${reg.code} — ${reg.name}",
                                                fontWeight = FontWeight.SemiBold,
                                                color = colors.textPrimary
                                            )
                                            Text(
                                                "${reg.areaDescription} (×${reg.multiplier}, +${reg.peakAdderPence.toInt()}p peak)",
                                                fontSize = 11.sp,
                                                color = colors.textSecondary
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedRegion = reg
                                        tariffCode = AgileCalculator.buildAgileTariffCode(productCode, reg.code)
                                        regionExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Agile Product Selector / Input
                    if (state.availableAgileProducts.isNotEmpty()) {
                        ExposedDropdownMenuBox(
                            expanded = productExpanded,
                            onExpandedChange = { productExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = productCode,
                                onValueChange = {
                                    productCode = it
                                    tariffCode = AgileCalculator.buildAgileTariffCode(it, selectedRegion.code)
                                },
                                label = { Text("Agile Product Code") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = productExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = productExpanded,
                                onDismissRequest = { productExpanded = false },
                                containerColor = colors.cardSurface
                            ) {
                                state.availableAgileProducts.forEach { prod ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(prod.code, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                                                Text(prod.fullName, fontSize = 12.sp, color = colors.textSecondary)
                                            }
                                        },
                                        onClick = {
                                            productCode = prod.code
                                            tariffCode = AgileCalculator.buildAgileTariffCode(prod.code, selectedRegion.code)
                                            productExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = productCode,
                            onValueChange = {
                                productCode = it
                                tariffCode = AgileCalculator.buildAgileTariffCode(it, selectedRegion.code)
                            },
                            label = { Text("Agile Product Code (e.g. AGILE-24-10-01)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    OutlinedTextField(
                        value = tariffCode,
                        onValueChange = { tariffCode = it },
                        label = { Text("Full Electricity Tariff Code") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = electricMpan,
                            onValueChange = { electricMpan = it },
                            label = { Text("Electricity MPAN") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = electricSerial,
                            onValueChange = { electricSerial = it },
                            label = { Text("Meter Serial") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = elecStandingPence,
                        onValueChange = { elecStandingPence = it },
                        label = { Text("Electricity Standing Charge (p/day inc. VAT)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 3. Gas Meter & Tariff Configuration Card
            Surface(
                color = colors.cardSurface,
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = AgilePinkPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Optional Gas Meter & Tariff",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = gasUnitRatePence,
                            onValueChange = { gasUnitRatePence = it },
                            label = { Text("Unit Rate (p/kWh)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = gasStandingPence,
                            onValueChange = { gasStandingPence = it },
                            label = { Text("Standing (p/day)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = gasMprn,
                            onValueChange = { gasMprn = it },
                            label = { Text("Gas MPRN") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = gasSerial,
                            onValueChange = { gasSerial = it },
                            label = { Text("Gas Meter Serial") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    SegmentedPillRow(
                        options = listOf(true to "SMETS2 (m³)", false to "SMETS1 (kWh)"),
                        selectedValue = gasIsCubicMeters,
                        onSelect = { gasIsCubicMeters = it }
                    )

                    Button(
                        onClick = {
                            onSaveAccountConfig(
                                cfg.copy(
                                    apiKey = apiKey.trim(),
                                    accountNumber = accountNumber.trim(),
                                    agileProductCode = productCode.trim(),
                                    regionCode = selectedRegion.code,
                                    agileTariffCode = tariffCode.trim(),
                                    electricMpan = electricMpan.trim(),
                                    electricSerial = electricSerial.trim(),
                                    electricStandingChargePence = elecStandingPence.toDoubleOrNull() ?: cfg.electricStandingChargePence,
                                    gasMprn = gasMprn.trim(),
                                    gasSerial = gasSerial.trim(),
                                    gasUnitRatePence = gasUnitRatePence.toDoubleOrNull() ?: cfg.gasUnitRatePence,
                                    gasStandingChargePence = gasStandingPence.toDoubleOrNull() ?: cfg.gasStandingChargePence,
                                    gasIsCubicMeters = gasIsCubicMeters
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AgilePinkPrimary),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save Tariff & Meter Settings", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // 4. Display & Alert Preferences Card (matching Agile Watcher features)
            Surface(
                color = colors.cardSurface,
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Tune, contentDescription = null, tint = AgilePinkPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Appearance & Preferences",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                    }

                    Text(
                        text = "Theme Appearance",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textSecondary
                    )

                    SegmentedPillRow(
                        options = listOf("LIGHT" to "Light", "DARK" to "Dark", "SYSTEM" to "System"),
                        selectedValue = prefs.themeMode,
                        onSelect = { onUpdatePreferences(prefs.copy(themeMode = it)) }
                    )

                    PreferenceSwitchRow(
                        title = "Include Standing Charge",
                        subtitle = "Include daily standing charge in 30-day & daily totals",
                        checked = prefs.includeStandingCharge,
                        onCheckedChange = { onUpdatePreferences(prefs.copy(includeStandingCharge = it)) }
                    )

                    PreferenceSwitchRow(
                        title = "3 Decimal Places",
                        subtitle = "Display Agile unit rates to 3 decimal places (e.g. 9.530p/kWh)",
                        checked = prefs.useThreeDecimals,
                        onCheckedChange = { onUpdatePreferences(prefs.copy(useThreeDecimals = it)) }
                    )

                    PreferenceSwitchRow(
                        title = "Sample Usage Preview When Unconfigured",
                        subtitle = "Show realistic 30-day sample charts before connecting a smart meter",
                        checked = prefs.showSampleConsumptionIfNoMeter,
                        onCheckedChange = { onUpdatePreferences(prefs.copy(showSampleConsumptionIfNoMeter = it)) }
                    )

                    OutlinedTextField(
                        value = highCostThresholdText,
                        onValueChange = {
                            highCostThresholdText = it
                            it.toDoubleOrNull()?.let { v ->
                                onUpdatePreferences(prefs.copy(highCostThresholdPence = v))
                            }
                        },
                        label = { Text("High Cost Alert Threshold (p/kWh)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Footer Note
            Text(
                text = "Agile Watcher for Android v${com.example.agilewatcher.BuildConfig.VERSION_NAME} · Powered by Octopus Energy REST & GraphQL APIs (api.oegb-kraken.energy). Your API key and meter details stay strictly on your device.",
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = colors.textMuted,
                modifier = Modifier.padding(bottom = 28.dp)
            )
        }
    }
}

@Composable
private fun PreferenceSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = LocalAgileColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
            Text(subtitle, fontSize = 12.sp, color = colors.textSecondary)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = AgilePinkPrimary
            )
        )
    }
}
