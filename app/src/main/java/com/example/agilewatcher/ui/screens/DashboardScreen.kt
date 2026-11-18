package com.example.agilewatcher.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.agilewatcher.data.AgileCalculator
import com.example.agilewatcher.data.AgileSlot
import com.example.agilewatcher.ui.AgileUiState
import com.example.agilewatcher.ui.theme.*

@Composable
fun DashboardScreen(
    state: AgileUiState,
    onRefresh: () -> Unit,
    onToggleEarlierPrices: () -> Unit,
    onOpenBestTimes: () -> Unit,
    onScheduleSlotReminder: (AgileSlot) -> Unit
) {
    val colors = LocalAgileColors.current
    val context = LocalContext.current
    val decimals = state.appPreferences.useThreeDecimals
    val nowMs = System.currentTimeMillis()

    var selectedSlotForAction by remember { mutableStateOf<AgileSlot?>(null) }

    // Filter slots based on "Earlier Prices" toggle (matching 02-dashboard.png)
    val visibleSlots = remember(state.slots, state.showEarlierPricesToday, state.currentSlot) {
        if (state.showEarlierPricesToday) {
            state.slots
        } else {
            val cutoff = state.currentSlot?.startEpochMillis ?: nowMs
            state.slots.filter { it.startEpochMillis >= cutoff }.ifEmpty { state.slots }
        }
    }

    val groupedByDay = remember(visibleSlots) {
        visibleSlots.groupBy { it.dayHeaderLabel }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.pageBackground)
    ) {
        // Top Header Bar matching 02-dashboard.png ("Agile Prices" centered, "Best Times" pill right)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 12.dp)
        ) {
            IconButton(
                onClick = onRefresh,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh Prices",
                    tint = AgilePinkPrimary
                )
            }

            Text(
                text = "Agile Prices",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                modifier = Modifier.align(Alignment.Center)
            )

            Surface(
                color = colors.cardSurface,
                shape = RoundedCornerShape(24.dp),
                shadowElevation = 2.dp,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .clip(RoundedCornerShape(24.dp))
                    .clickable { onOpenBestTimes() }
                    .border(1.dp, colors.gridLine, RoundedCornerShape(24.dp))
            ) {
                Text(
                    text = "Best Times",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

        if (state.isRefreshing) {
            LinearProgressIndicator(
                color = AgilePinkPrimary,
                trackColor = Color.Transparent,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Hero Current Price Card matching 02-dashboard.png
            item {
                CurrentPriceHeroCard(
                    currentSlot = state.currentSlot,
                    nextSlot = state.nextSlot,
                    averageForDayPence = state.averageForTodayPence,
                    checkedAtEpochMillis = state.checkedAtEpochMillis,
                    showEarlierPrices = state.showEarlierPricesToday,
                    useThreeDecimals = decimals,
                    onToggleEarlierPrices = onToggleEarlierPrices,
                    onShareCurrentPrice = {
                        val curStr = state.currentSlot?.let {
                            "${AgileCalculator.formatRatePence(it.valueIncVat, decimals)}p/kWh (${it.timeRangeLabel})"
                        } ?: "N/A"
                        val nxtStr = state.nextSlot?.let {
                            "${AgileCalculator.formatRatePence(it.valueIncVat, decimals)}p/kWh"
                        } ?: "N/A"
                        val avgStr = "${AgileCalculator.formatRatePence(state.averageForTodayPence, decimals)}p/kWh"
                        shareText(
                            context = context,
                            text = "⚡ Octopus Agile Electricity Rates\nCurrent Price: $curStr\nNext Half Hour: $nxtStr\nAverage for Day: $avgStr"
                        )
                    }
                )
            }

            // Grouped Day Sections & Half-Hour Rate Cards
            groupedByDay.forEach { (dayLabel, daySlots) ->
                val lowestInDay = daySlots.minByOrNull { it.valueIncVat }?.startEpochMillis

                item(key = "header_$dayLabel") {
                    Text(
                        text = dayLabel,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = AgilePinkPrimary,
                        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                    )
                }

                items(daySlots, key = { it.startEpochMillis }) { slot ->
                    val isCurrent = state.currentSlot?.startEpochMillis == slot.startEpochMillis
                    val isLowest = lowestInDay == slot.startEpochMillis
                    val isHighCost = slot.valueIncVat >= state.appPreferences.highCostThresholdPence

                    HalfHourRateRowCard(
                        slot = slot,
                        isCurrent = isCurrent,
                        isLowestOfDay = isLowest,
                        isHighCost = isHighCost,
                        useThreeDecimals = decimals,
                        onClick = { selectedSlotForAction = slot }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Slot Action Dialog (Remind Me / Share Cost at Specific Time)
    selectedSlotForAction?.let { slot ->
        val rateFormatted = "${AgileCalculator.formatRatePence(slot.valueIncVat, decimals)}p/kWh"
        AlertDialog(
            onDismissRequest = { selectedSlotForAction = null },
            containerColor = colors.cardSurface,
            title = {
                Text(
                    text = "${slot.dayHeaderLabel} · ${slot.timeRangeLabel}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = colors.textPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Unit Rate: $rateFormatted (inc. VAT)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AgilePinkPrimary
                    )
                    Text(
                        text = "Excluding VAT: ${AgileCalculator.formatRatePence(slot.valueExcVat, decimals)}p/kWh",
                        fontSize = 13.sp,
                        color = colors.textSecondary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onScheduleSlotReminder(slot)
                        selectedSlotForAction = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AgilePinkPrimary)
                ) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Remind Me", color = Color.White)
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            shareText(
                                context,
                                "⚡ Octopus Agile rate on ${slot.dayHeaderLabel} (${slot.timeRangeLabel}): $rateFormatted"
                            )
                            selectedSlotForAction = null
                        }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = AgilePinkPrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share", color = AgilePinkPrimary)
                    }
                    TextButton(onClick = { selectedSlotForAction = null }) {
                        Text("Close", color = colors.textSecondary)
                    }
                }
            }
        )
    }
}

@Composable
private fun CurrentPriceHeroCard(
    currentSlot: AgileSlot?,
    nextSlot: AgileSlot?,
    averageForDayPence: Double,
    checkedAtEpochMillis: Long,
    showEarlierPrices: Boolean,
    useThreeDecimals: Boolean,
    onToggleEarlierPrices: () -> Unit,
    onShareCurrentPrice: () -> Unit
) {
    val curPriceStr = currentSlot?.let {
        "${AgileCalculator.formatRatePence(it.valueIncVat, useThreeDecimals)}p"
    } ?: "--.-p"
    val nextPriceStr = nextSlot?.let {
        "${AgileCalculator.formatRatePence(it.valueIncVat, useThreeDecimals)}p/kWh"
    } ?: "Awaiting publication"
    val avgPriceStr = "${AgileCalculator.formatRatePence(averageForDayPence, useThreeDecimals)}p/kWh"
    val checkedStr = AgileCalculator.formatTimestamp(checkedAtEpochMillis)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(AgilePinkGradientTop, AgilePinkGradientBottom)
                )
            )
            .padding(22.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "CURRENT PRICE",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                color = Color.White.copy(alpha = 0.92f)
            )

            Text(
                text = buildAnnotatedString {
                    withStyle(
                        SpanStyle(
                            fontSize = 46.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    ) {
                        append(curPriceStr)
                    }
                    withStyle(
                        SpanStyle(
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.95f)
                        )
                    ) {
                        append(" /kWh")
                    }
                }
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "Next Half Hour: $nextPriceStr",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )

            Text(
                text = "Average for Day: $avgPriceStr",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )

            Text(
                text = "Prices checked: $checkedStr",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
            )

            // Bottom Row: "↓ Earlier Prices" Pill + Share Circle Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = AgilePinkSoftPill,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .clickable { onToggleEarlierPrices() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (showEarlierPrices) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (showEarlierPrices) "Hide Earlier" else "Earlier Prices",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Surface(
                    color = AgilePinkSoftPill,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable { onShareCurrentPrice() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.IosShare,
                            contentDescription = "Share Current Price",
                            tint = Color.White,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HalfHourRateRowCard(
    slot: AgileSlot,
    isCurrent: Boolean,
    isLowestOfDay: Boolean,
    isHighCost: Boolean,
    useThreeDecimals: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalAgileColors.current
    val rateStr = "${AgileCalculator.formatRatePence(slot.valueIncVat, useThreeDecimals)}p/kWh"

    val rateColor = when {
        slot.isPlunge -> PlungeCyan
        isCurrent -> AgilePinkPrimary
        isLowestOfDay -> LowestRateGreen
        isHighCost -> HighCostAlertRed
        else -> colors.textPrimary
    }

    val timeColor = if (isCurrent) AgilePinkPrimary else colors.textPrimary

    Surface(
        color = colors.cardSurface,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = slot.timeRangeLabel,
                    fontSize = 16.sp,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                    color = timeColor
                )
                if (slot.isPlunge) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = "Plunge Pricing Below 0p",
                        tint = PlungeCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Text(
                text = rateStr,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = rateColor
            )
        }
    }
}

fun shareText(context: Context, text: String) {
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, text)
        type = "text/plain"
    }
    val chooser = Intent.createChooser(sendIntent, "Share Agile Watcher")
    context.startActivity(chooser)
}
