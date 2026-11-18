package com.example.agilewatcher.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.agilewatcher.data.AgileCalculator
import com.example.agilewatcher.data.AgileSlot
import com.example.agilewatcher.data.BestTimeWindow
import com.example.agilewatcher.data.UK_ZONE
import com.example.agilewatcher.ui.AgileUiState
import com.example.agilewatcher.ui.theme.*
import java.time.ZonedDateTime
import kotlin.math.max
import kotlin.math.min

@Composable
fun BestTimesModal(
    state: AgileUiState,
    onRefresh: () -> Unit,
    onSelectDuration: (Int) -> Unit,
    onSelectChartDay: (String) -> Unit,
    onRemindWindow: (BestTimeWindow) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalAgileColors.current
    val decimals = state.appPreferences.useThreeDecimals
    val scrollState = rememberScrollState()

    val nowUk = remember(state.slots) { ZonedDateTime.now(UK_ZONE) }
    val todayIso = nowUk.toLocalDate().toString()
    val tomorrowIso = nowUk.toLocalDate().plusDays(1).toString()

    val chartSlots = remember(state.slots, state.bestTimesChartDay) {
        val targetIso = if (state.bestTimesChartDay == "TOMORROW") tomorrowIso else todayIso
        state.slots.filter { it.localDateIso == targetIso }.sortedBy { it.startEpochMillis }
    }

    val highlightedWindow = if (state.bestTimesChartDay == "TOMORROW") {
        state.bestWindowTomorrow
    } else {
        state.bestWindowToday
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = colors.pageBackground,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 18.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Drag Handle Pill
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(42.dp)
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(colors.gridLine)
                )

                // Top Header Bar matching 03-best-times.png (Refresh circle left, "Best Times" center, "Done" pill right)
                Box(modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        color = colors.cardSurface,
                        shape = CircleShape,
                        modifier = Modifier
                            .size(44.dp)
                            .align(Alignment.CenterStart)
                            .clip(CircleShape)
                            .clickable { onRefresh() }
                            .border(1.dp, colors.gridLine, CircleShape)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = AgilePinkPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Text(
                        text = "Best Times",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        modifier = Modifier.align(Alignment.Center)
                    )

                    Surface(
                        color = colors.cardSurface,
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .clip(RoundedCornerShape(24.dp))
                            .clickable { onDismiss() }
                            .border(1.dp, colors.gridLine, RoundedCornerShape(24.dp))
                    ) {
                        Text(
                            text = "Done",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AgilePinkPrimary,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                        )
                    }
                }

                // Title & Description matching 03-best-times.png
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Find a cheaper time",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Choose how long you need. We'll find the lowest average Agile rate in consecutive half-hour slots.",
                        fontSize = 15.sp,
                        lineHeight = 21.sp,
                        color = colors.textSecondary
                    )
                }

                // 3-Way Segmented Control: "1 hour" | "2 hours" | "4 hours"
                SegmentedPillRow(
                    options = listOf(1 to "1 hour", 2 to "2 hours", 4 to "4 hours"),
                    selectedValue = state.bestTimesDurationHours,
                    onSelect = onSelectDuration
                )

                // TODAY Card matching 03-best-times.png
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(AgilePinkGradientTop, AgilePinkGradientBottom)
                            )
                        )
                        .padding(20.dp)
                ) {
                    val winToday = state.bestWindowToday
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "TODAY",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                        if (winToday == null) {
                            Text(
                                text = "No complete ${state.bestTimesDurationHours}-hour window remains today.",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                        } else {
                            Text(
                                text = winToday.timeRangeLabel,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(SpanStyle(fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)) {
                                        append(AgileCalculator.formatRatePence(winToday.averagePencePerKwh, decimals))
                                    }
                                    withStyle(SpanStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White.copy(alpha = 0.92f))) {
                                        append(" p/kWh average")
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                color = AgilePinkSoftPill,
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(24.dp))
                                    .clickable { onRemindWindow(winToday) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.NotificationsNone,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Remind Me",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                // TOMORROW Card matching 03-best-times.png
                Surface(
                    color = colors.cardSurface,
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val winTomorrow = state.bestWindowTomorrow
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "TOMORROW",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = colors.textSecondary
                        )
                        if (winTomorrow != null) {
                            Text(
                                text = winTomorrow.timeRangeLabel,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = colors.textPrimary
                            )
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(SpanStyle(fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = colors.textPrimary)) {
                                        append(AgileCalculator.formatRatePence(winTomorrow.averagePencePerKwh, decimals))
                                    }
                                    withStyle(SpanStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)) {
                                        append(" p/kWh average")
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                color = AgilePinkPrimary,
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(24.dp))
                                    .clickable { onRemindWindow(winTomorrow) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.NotificationsNone,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Remind Me",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = "Tomorrow's Agile rates are typically published by Octopus Energy around 4:00 PM UK time.",
                                fontSize = 15.sp,
                                color = colors.textSecondary
                            )
                        }
                    }
                }

                // Half-hour prices Chart Card matching 03-best-times.png
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
                                text = "Half-hour prices",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )

                            Box(modifier = Modifier.width(190.dp)) {
                                SegmentedPillRow(
                                    options = listOf("TODAY" to "Today", "TOMORROW" to "Tomorrow"),
                                    selectedValue = state.bestTimesChartDay,
                                    onSelect = onSelectChartDay
                                )
                            }
                        }

                        if (chartSlots.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Tomorrow's half-hour rates are not yet published.",
                                    color = colors.textSecondary,
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            HalfHourPriceBarChart(
                                slots = chartSlots,
                                highlightedWindow = highlightedWindow
                            )
                        }

                        Text(
                            text = "Pink marks the cheapest complete window when one is available.",
                            fontSize = 14.sp,
                            lineHeight = 19.sp,
                            color = colors.textSecondary
                        )
                    }
                }

                Text(
                    text = "Averages assume even electricity use throughout the window. Your final bill may differ.",
                    fontSize = 12.sp,
                    color = colors.textMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )
            }
        }
    }
}

@Composable
fun <T> SegmentedPillRow(
    options: List<Pair<T, String>>,
    selectedValue: T,
    onSelect: (T) -> Unit
) {
    val colors = LocalAgileColors.current
    Surface(
        color = colors.segmentTrack,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            options.forEach { (value, label) ->
                val selected = value == selectedValue
                Surface(
                    color = if (selected) colors.cardSurface else Color.Transparent,
                    shape = RoundedCornerShape(17.dp),
                    shadowElevation = if (selected) 1.dp else 0.dp,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(17.dp))
                        .clickable { onSelect(value) }
                ) {
                    Text(
                        text = label,
                        fontSize = 14.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = colors.textPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 7.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun HalfHourPriceBarChart(
    slots: List<AgileSlot>,
    highlightedWindow: BestTimeWindow?
) {
    val colors = LocalAgileColors.current
    val highlightedEpochs = remember(highlightedWindow) {
        highlightedWindow?.slots?.map { it.startEpochMillis }?.toSet() ?: emptySet()
    }

    val minRate = remember(slots) { min(-20.0, (slots.minOfOrNull { it.valueIncVat } ?: 0.0) - 5.0) }
    val maxRate = remember(slots) { max(40.0, (slots.maxOfOrNull { it.valueIncVat } ?: 35.0) + 5.0) }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                val w = size.width
                val h = size.height
                val range = (maxRate - minRate).coerceAtLeast(1.0)

                fun yForValue(v: Double): Float {
                    val norm = ((maxRate - v) / range).toFloat().coerceIn(0f, 1f)
                    return norm * h
                }

                val zeroY = yForValue(0.0)
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)

                // Horizontal gridlines at 40, 20, 0, -20
                listOf(40.0, 20.0, 0.0, -20.0).forEach { level ->
                    if (level in minRate..maxRate) {
                        val y = yForValue(level)
                        drawLine(
                            color = colors.gridLine,
                            start = Offset(0f, y),
                            end = Offset(w, y),
                            strokeWidth = if (level == 0.0) 2f else 1.2f
                        )
                    }
                }

                // Vertical dashed time gridlines (6 intervals across 48 slots)
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

                val count = max(slots.size, 48)
                val barSlotWidth = w / count.toFloat()
                val barWidth = (barSlotWidth * 0.85f).coerceAtLeast(2f)

                slots.forEachIndexed { idx, slot ->
                    val x = idx * barSlotWidth
                    val valY = yForValue(slot.valueIncVat)
                    val topY = min(zeroY, valY)
                    val barH = kotlin.math.abs(valY - zeroY).coerceAtLeast(2f)
                    val isHighlighted = slot.startEpochMillis in highlightedEpochs

                    drawRoundRect(
                        color = if (isHighlighted) AgilePinkPrimary else AgilePinkChartLight,
                        topLeft = Offset(x, topY),
                        size = Size(barWidth, barH),
                        cornerRadius = CornerRadius(2f, 2f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Right-side Y-axis labels matching 03-best-times.png (40, 20, 0, -20)
            Column(
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.Start
            ) {
                Text("40", fontSize = 12.sp, color = colors.textMuted)
                Text("20", fontSize = 12.sp, color = colors.textMuted)
                Text("0", fontSize = 12.sp, color = colors.textMuted)
                Text("-20", fontSize = 12.sp, color = colors.textMuted)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Bottom X-axis labels matching 03-best-times.png (12 AM, 3 AM, 7 AM, 11 AM, 3 PM, 7 PM)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 28.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf("12 AM", "3 AM", "7 AM", "11 AM", "3 PM", "7 PM").forEach { lbl ->
                Text(lbl, fontSize = 11.sp, color = colors.textMuted)
            }
        }
    }
}
