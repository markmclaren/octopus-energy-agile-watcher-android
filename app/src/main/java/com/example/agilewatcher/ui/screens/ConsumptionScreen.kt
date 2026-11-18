package com.example.agilewatcher.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.agilewatcher.data.AgileCalculator
import com.example.agilewatcher.data.DailyConsumptionSummary
import com.example.agilewatcher.data.OctopusAccountConfig
import com.example.agilewatcher.data.ThirtyDayConsumptionOverview
import com.example.agilewatcher.ui.theme.*
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.max

@Composable
fun ConsumptionTabScreen(
    title: String,
    isGas: Boolean,
    overview: ThirtyDayConsumptionOverview?,
    selectedDay: DailyConsumptionSummary?,
    useThreeDecimals: Boolean,
    onSelectDay: (DailyConsumptionSummary?) -> Unit,
    onOpenGasTariffSetup: () -> Unit = {},
    onOpenSettings: () -> Unit = {}
) {
    if (selectedDay != null) {
        DayConsumptionDetailScreen(
            day = selectedDay,
            unitLabel = overview?.unitLabel ?: if (isGas) "m³" else "kWh",
            useThreeDecimals = useThreeDecimals,
            onBack = { onSelectDay(null) }
        )
    } else {
        ConsumptionOverviewScreen(
            title = title,
            isGas = isGas,
            overview = overview,
            useThreeDecimals = useThreeDecimals,
            onSelectDay = { onSelectDay(it) },
            onOpenGasTariffSetup = onOpenGasTariffSetup,
            onOpenSettings = onOpenSettings
        )
    }
}

@Composable
private fun ConsumptionOverviewScreen(
    title: String,
    isGas: Boolean,
    overview: ThirtyDayConsumptionOverview?,
    useThreeDecimals: Boolean,
    onSelectDay: (DailyConsumptionSummary) -> Unit,
    onOpenGasTariffSetup: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val colors = LocalAgileColors.current
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.pageBackground)
    ) {
        // Top Header Bar matching 04-electricity.png & 06-gas.png
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
        }

        if (overview == null || overview.daysChronological.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = colors.cardSurface,
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = if (isGas) Icons.Default.LocalFireDepartment else Icons.Default.Bolt,
                            contentDescription = null,
                            tint = AgilePinkPrimary,
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            text = "No $title Data Available",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "Connect your Octopus API key and smart meter in Settings (or enable sample preview) to view 30-day usage and costs.",
                            fontSize = 14.sp,
                            color = colors.textSecondary
                        )
                        Button(
                            onClick = onOpenSettings,
                            colors = ButtonDefaults.buttonColors(containerColor = AgilePinkPrimary),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Open Settings", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            return
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Hero 30-Day Summary Card matching 04-electricity.png & 06-gas.png
            item {
                ThirtyDayHeroCard(
                    overview = overview,
                    isGas = isGas,
                    onOpenGasTariffSetup = onOpenGasTariffSetup,
                    onShare = {
                        val unit = overview.unitLabel
                        val cost30 = AgileCalculator.formatPounds(overview.totalCost30DaysPounds)
                        val usage30 = String.format(Locale.US, "%.2f %s", overview.totalUsage30Days, unit)
                        val cost7 = AgileCalculator.formatPounds(overview.last7DaysCostPounds)
                        val usage7 = String.format(Locale.US, "%.2f %s", overview.last7DaysUsage, unit)
                        shareText(
                            context,
                            "📊 $title (Last 30 Days)\nTotal: $cost30 · $usage30\nLast 7 Days: $cost7 · $usage7"
                        )
                    }
                )
            }

            // Sample data banner if preview mode is active
            if (overview.isSampleData) {
                item {
                    Surface(
                        color = colors.cardSurface,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, AgilePinkSoftPill.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = AgilePinkPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Showing sample usage preview. Add your Octopus API key in Settings to load your smart meter.",
                                    fontSize = 12.sp,
                                    color = colors.textSecondary
                                )
                            }
                            TextButton(onClick = onOpenSettings) {
                                Text("Connect", color = AgilePinkPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // "Usage over 30 days" Bar Chart Card matching 04-electricity.png & 06-gas.png
            item {
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
                            Text(
                                text = "Usage over 30 days",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                            Text(
                                text = overview.unitLabel,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.textSecondary
                            )
                        }

                        ThirtyDayBarChart(days = overview.daysChronological)
                    }
                }
            }

            // "Daily usage" Header
            item {
                Text(
                    text = "Daily usage",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Daily usage cards (newest first, matching 04-electricity.png & 06-gas.png)
            items(overview.daysNewestFirst, key = { it.dateIso }) { day ->
                DailyUsageRowCard(
                    day = day,
                    isGas = isGas,
                    unitLabel = overview.unitLabel,
                    useThreeDecimals = useThreeDecimals,
                    onClick = { onSelectDay(day) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun ThirtyDayHeroCard(
    overview: ThirtyDayConsumptionOverview,
    isGas: Boolean,
    onOpenGasTariffSetup: () -> Unit,
    onShare: () -> Unit
) {
    val unit = overview.unitLabel
    val totalCostStr = AgileCalculator.formatPounds(overview.totalCost30DaysPounds)
    val totalUsageStr = String.format(Locale.US, " %.2f %s", overview.totalUsage30Days, unit)
    val last7CostStr = AgileCalculator.formatPounds(overview.last7DaysCostPounds)
    val last7UsageStr = String.format(Locale.US, "%.2f %s", overview.last7DaysUsage, unit)
    val avgCostStr = AgileCalculator.formatPounds(overview.avgReportedDayCostPounds)
    val avgUsageStr = String.format(Locale.US, "%.2f %s", overview.avgReportedDayUsage, unit)
    val standingStr = AgileCalculator.formatPounds(overview.standingChargePoundsPerDay)

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
                text = "Last 30 Days",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = buildAnnotatedString {
                    withStyle(
                        SpanStyle(
                            fontSize = 42.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    ) {
                        append(totalCostStr)
                    }
                    withStyle(
                        SpanStyle(
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.95f)
                        )
                    ) {
                        append(totalUsageStr)
                    }
                }
            )

            Text(
                text = "Last 7 Days: $last7CostStr · $last7UsageStr",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )

            Text(
                text = "Avg. reported day: $avgCostStr · $avgUsageStr",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = if (overview.includeStandingCharge) {
                    "Day pricing includes standing charge of $standingStr."
                } else {
                    "Day pricing excludes standing charge ($standingStr/day)."
                },
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.9f)
            )

            if (isGas) {
                Text(
                    text = "Costs are estimates using your saved gas rate. Your bill may differ.",
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }

            Text(
                text = "Last checked: ${overview.lastCheckedLabel} · Latest reading: ${overview.latestReadingLabel}",
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = Color.White.copy(alpha = 0.85f)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = if (isGas) Arrangement.SpaceBetween else Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isGas) {
                    // "Setup Tariff" pill button matching 06-gas.png
                    Surface(
                        color = AgilePinkSoftPill,
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .clickable { onOpenGasTariffSetup() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Setup Tariff",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                Surface(
                    color = AgilePinkSoftPill,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable { onShare() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.IosShare,
                            contentDescription = "Share 30-Day Consumption",
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
private fun ThirtyDayBarChart(days: List<DailyConsumptionSummary>) {
    val colors = LocalAgileColors.current
    val maxUsage = remember(days) {
        val rawMax = days.maxOfOrNull { it.totalConsumption } ?: 10.0
        if (rawMax <= 6.0) {
            ceil(rawMax / 2.0).coerceAtLeast(1.0) * 2.0
        } else {
            ceil(rawMax / 20.0).coerceAtLeast(1.0) * 20.0
        }
    }

    val topLabel = maxUsage.toInt().toString()
    val midHighLabel = (maxUsage * 2.0 / 3.0).toInt().toString()
    val midLowLabel = (maxUsage / 3.0).toInt().toString()

    // Select 5 axis labels spaced across the 30 days (matching Sep 26, Oct 3, Oct 10, Oct 17, Oct 24)
    val axisLabels = remember(days) {
        if (days.size >= 5) {
            val indices = listOf(0, days.size / 4, days.size / 2, (days.size * 3) / 4, days.lastIndex)
            indices.map { days[it].axisLabel }
        } else {
            days.map { it.axisLabel }
        }
    }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(185.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                val w = size.width
                val h = size.height
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)

                // Horizontal gridlines (4 levels: top, 2/3, 1/3, 0)
                for (i in 0..3) {
                    val y = (h * i) / 3f
                    drawLine(
                        color = colors.gridLine,
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = if (i == 3) 1.8f else 1.1f
                    )
                }

                // Vertical dashed gridlines
                for (g in 0..4) {
                    val x = (w * g) / 4f
                    drawLine(
                        color = colors.gridLine,
                        start = Offset(x, 0f),
                        end = Offset(x, h),
                        strokeWidth = 1f,
                        pathEffect = dashEffect
                    )
                }

                val count = max(days.size, 1)
                val slotWidth = w / count.toFloat()
                val barWidth = (slotWidth * 0.78f).coerceAtLeast(3f)

                days.forEachIndexed { idx, day ->
                    val ratio = (day.totalConsumption / maxUsage).toFloat().coerceIn(0f, 1f)
                    val barH = (ratio * h).coerceAtLeast(3f)
                    val x = idx * slotWidth + (slotWidth - barWidth) / 2f
                    val topY = h - barH

                    drawRoundRect(
                        color = AgilePinkPrimary,
                        topLeft = Offset(x, topY),
                        size = Size(barWidth, barH),
                        cornerRadius = CornerRadius(4f, 4f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.Start
            ) {
                Text(topLabel, fontSize = 12.sp, color = colors.textMuted)
                Text(midHighLabel, fontSize = 12.sp, color = colors.textMuted)
                Text(midLowLabel, fontSize = 12.sp, color = colors.textMuted)
                Text("0", fontSize = 12.sp, color = colors.textMuted)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            axisLabels.forEach { label ->
                Text(label, fontSize = 11.sp, color = colors.textMuted)
            }
        }
    }
}

@Composable
private fun DailyUsageRowCard(
    day: DailyConsumptionSummary,
    isGas: Boolean,
    unitLabel: String,
    useThreeDecimals: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalAgileColors.current
    val subtitle = if (day.isPartial) {
        "Partial readings · ${day.readingsCount} received"
    } else if (isGas) {
        "${day.readingsCount} readings · ${AgileCalculator.formatRatePence(day.averageRatePence, useThreeDecimals)}p/kWh"
    } else {
        "Avg. ${AgileCalculator.formatRatePence(day.averageRatePence, useThreeDecimals)}p/kWh · ${day.readingsCount} readings"
    }

    Surface(
        color = colors.cardSurface,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = day.shortLabel,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = colors.textSecondary
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = AgileCalculator.formatPounds(day.totalCostPounds),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Text(
                        text = String.format(Locale.US, "%.2f %s", day.totalConsumption, unitLabel),
                        fontSize = 13.sp,
                        color = colors.textSecondary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "View Day Details",
                    tint = colors.textMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Day Detail Screen matching Screenshot 05 (`05-electricity-day.png`):
 * - Top bar with `<` back circle button and `Sat, Oct 24`
 * - Hero Pink Card with `Saturday, October 24`, `36.29 kWh`, `Day cost: £2.87`
 * - "Usage through the day" 48-slot bar chart (`12 AM`, `4 AM`, `8 AM`, `12 PM`, `4 PM`, `8 PM`)
 * - "Half-hour readings" list with `12:00 AM – 12:30 AM`, `6.48p/kWh`, `£0.04`, `0.55 kWh`
 */
@Composable
fun DayConsumptionDetailScreen(
    day: DailyConsumptionSummary,
    unitLabel: String,
    useThreeDecimals: Boolean,
    onBack: () -> Unit
) {
    val colors = LocalAgileColors.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.pageBackground)
    ) {
        // Top Header Bar with Back Button matching 05-electricity-day.png
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 12.dp)
        ) {
            Surface(
                color = colors.cardSurface,
                shape = CircleShape,
                shadowElevation = 2.dp,
                modifier = Modifier
                    .size(44.dp)
                    .align(Alignment.CenterStart)
                    .clip(CircleShape)
                    .clickable { onBack() }
                    .border(1.dp, colors.gridLine, CircleShape)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = "Back",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Text(
                text = day.shortLabel,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Hero Day Card matching 05-electricity-day.png
            item {
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
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = day.fullLabel,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Text(
                            text = buildAnnotatedString {
                                withStyle(
                                    SpanStyle(
                                        fontSize = 42.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                ) {
                                    append(String.format(Locale.US, "%.2f", day.totalConsumption))
                                }
                                withStyle(
                                    SpanStyle(
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White.copy(alpha = 0.95f)
                                    )
                                ) {
                                    append(" $unitLabel")
                                }
                            }
                        )

                        Text(
                            text = "Day cost: ${AgileCalculator.formatPounds(day.totalCostPounds)}",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }

            // "Usage through the day" Chart Card matching 05-electricity-day.png
            item {
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
                            Text(
                                text = "Usage through the day",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                            Text(
                                text = unitLabel,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.textSecondary
                            )
                        }

                        DayHalfHourUsageChart(day = day)
                    }
                }
            }

            // "Half-hour readings" Section Title
            item {
                Text(
                    text = "Half-hour readings",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Half-hour reading cards matching 05-electricity-day.png
            items(day.slots, key = { it.startEpochMillis }) { reading ->
                Surface(
                    color = colors.cardSurface,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                text = reading.timeRangeLabel,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "${AgileCalculator.formatRatePence(reading.unitRatePence, useThreeDecimals)}p/kWh",
                                fontSize = 13.sp,
                                color = colors.textSecondary
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = AgileCalculator.formatPounds(reading.costPounds),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                            Text(
                                text = String.format(Locale.US, "%.2f %s", reading.consumption, unitLabel),
                                fontSize = 13.sp,
                                color = colors.textSecondary
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun DayHalfHourUsageChart(day: DailyConsumptionSummary) {
    val colors = LocalAgileColors.current
    val maxSlotUsage = remember(day.slots) {
        val raw = day.slots.maxOfOrNull { it.consumption } ?: 1.0
        ceil(raw).coerceAtLeast(1.0)
    }

    val l4 = String.format(Locale.US, "%.0f", maxSlotUsage)
    val l3 = String.format(Locale.US, "%.0f", maxSlotUsage * 0.75)
    val l2 = String.format(Locale.US, "%.0f", maxSlotUsage * 0.50)
    val l1 = String.format(Locale.US, "%.0f", maxSlotUsage * 0.25)

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(185.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                val w = size.width
                val h = size.height
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)

                // 5 horizontal gridlines (4, 3, 2, 1, 0)
                for (i in 0..4) {
                    val y = (h * i) / 4f
                    drawLine(
                        color = colors.gridLine,
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = if (i == 4) 1.8f else 1.1f
                    )
                }

                // 6 vertical dashed time gridlines
                for (g in 0..6) {
                    val x = (w * g) / 6f
                    drawLine(
                        color = colors.gridLine,
                        start = Offset(x, 0f),
                        end = Offset(x, h),
                        strokeWidth = 1f,
                        pathEffect = dashEffect
                    )
                }

                val slotCount = 48
                val slotWidth = w / slotCount.toFloat()
                val barWidth = (slotWidth * 0.85f).coerceAtLeast(3f)

                day.slots.forEachIndexed { idx, slot ->
                    if (idx < slotCount) {
                        val ratio = (slot.consumption / maxSlotUsage).toFloat().coerceIn(0f, 1f)
                        val barH = (ratio * h).coerceAtLeast(3f)
                        val x = idx * slotWidth
                        val topY = h - barH

                        drawRoundRect(
                            color = AgilePinkPrimary,
                            topLeft = Offset(x, topY),
                            size = Size(barWidth, barH),
                            cornerRadius = CornerRadius(3f, 3f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.Start
            ) {
                Text(l4, fontSize = 12.sp, color = colors.textMuted)
                Text(l3, fontSize = 12.sp, color = colors.textMuted)
                Text(l2, fontSize = 12.sp, color = colors.textMuted)
                Text(l1, fontSize = 12.sp, color = colors.textMuted)
                Text("0", fontSize = 12.sp, color = colors.textMuted)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Bottom X-axis labels matching 05-electricity-day.png (12 AM, 4 AM, 8 AM, 12 PM, 4 PM, 8 PM)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf("12 AM", "4 AM", "8 AM", "12 PM", "4 PM", "8 PM").forEach { lbl ->
                Text(lbl, fontSize = 11.sp, color = colors.textMuted)
            }
        }
    }
}

/**
 * Gas Tariff Setup Dialog triggered by the "Setup Tariff" button in 06-gas.png.
 */
@Composable
fun GasTariffSetupDialog(
    config: OctopusAccountConfig,
    onSave: (unitRatePence: Double, standingChargePence: Double, isCubicMeters: Boolean, mprn: String, serial: String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalAgileColors.current
    var unitRateText by remember { mutableStateOf(config.gasUnitRatePence.toString()) }
    var standingText by remember { mutableStateOf(config.gasStandingChargePence.toString()) }
    var isCubicMeters by remember { mutableStateOf(config.gasIsCubicMeters) }
    var mprnText by remember { mutableStateOf(config.gasMprn) }
    var serialText by remember { mutableStateOf(config.gasSerial) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.cardSurface,
        title = {
            Text(
                text = "🔥 Setup Gas Tariff & Meter",
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp,
                color = colors.textPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Enter your gas unit rate and standing charge (including VAT) to estimate 30-day gas costs.",
                    fontSize = 13.sp,
                    color = colors.textSecondary
                )

                OutlinedTextField(
                    value = unitRateText,
                    onValueChange = { unitRateText = it },
                    label = { Text("Gas Unit Rate (p/kWh inc. VAT)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = standingText,
                    onValueChange = { standingText = it },
                    label = { Text("Standing Charge (p/day inc. VAT)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Smart Meter Reading Units",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary
                )

                SegmentedPillRow(
                    options = listOf(true to "SMETS2 (m³)", false to "SMETS1 (kWh)"),
                    selectedValue = isCubicMeters,
                    onSelect = { isCubicMeters = it }
                )

                OutlinedTextField(
                    value = mprnText,
                    onValueChange = { mprnText = it },
                    label = { Text("Gas MPRN (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = serialText,
                    onValueChange = { serialText = it },
                    label = { Text("Gas Meter Serial (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val unitRate = unitRateText.toDoubleOrNull() ?: config.gasUnitRatePence
                    val standing = standingText.toDoubleOrNull() ?: config.gasStandingChargePence
                    onSave(unitRate, standing, isCubicMeters, mprnText, serialText)
                },
                colors = ButtonDefaults.buttonColors(containerColor = AgilePinkPrimary)
            ) {
                Text("Save Gas Tariff", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = colors.textSecondary)
            }
        }
    )
}
