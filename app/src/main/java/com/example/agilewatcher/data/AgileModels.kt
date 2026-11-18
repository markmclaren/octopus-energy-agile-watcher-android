package com.example.agilewatcher.data

import com.example.agilewatcher.BuildConfig
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

val UK_ZONE: ZoneId = ZoneId.of("Europe/London")

private val TIME_FMT = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
private val DAY_HEADER_FMT = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.US)
private val FULL_DAY_FMT = DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.US)
private val AXIS_DAY_FMT = DateTimeFormatter.ofPattern("MMM d", Locale.US)
private val CHECKED_FMT = DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a", Locale.US)

data class GspRegion(
    val code: String,
    val name: String,
    val areaDescription: String,
    val multiplier: Double,
    val peakAdderPence: Double
)

val UK_GSP_REGIONS: List<GspRegion> = listOf(
    GspRegion("A", "Eastern England", "East Anglia, Essex, Norfolk, Suffolk", 2.1, 13.0),
    GspRegion("B", "East Midlands", "Nottinghamshire, Derbyshire, Leicestershire", 2.0, 14.0),
    GspRegion("C", "London", "Greater London", 2.0, 12.0),
    GspRegion("D", "Merseyside & North Wales", "Liverpool, Cheshire, North Wales", 2.2, 13.0),
    GspRegion("E", "West Midlands", "Birmingham, Staffordshire, Worcestershire", 2.1, 12.0),
    GspRegion("F", "North Eastern England", "Newcastle, Durham, Teesside", 2.1, 12.0),
    GspRegion("G", "North Western England", "Manchester, Lancashire, Cumbria", 2.1, 12.0),
    GspRegion("H", "Southern England", "Oxfordshire, Hampshire, Dorset, Isle of Wight", 2.1, 12.0),
    GspRegion("J", "South Eastern England", "Kent, Surrey, Sussex", 2.2, 12.0),
    GspRegion("K", "Southern Wales", "Cardiff, Swansea, South Wales", 2.2, 12.0),
    GspRegion("L", "South Western England", "Bristol, Somerset, Devon, Cornwall", 2.3, 11.0),
    GspRegion("M", "Yorkshire", "Leeds, Sheffield, Humberside", 2.2, 13.0),
    GspRegion("N", "Southern Scotland", "Edinburgh, Glasgow, Central & Borders", 2.1, 13.0),
    GspRegion("P", "Northern Scotland", "Aberdeen, Highlands & Islands", 2.4, 12.0)
)

fun findGspRegion(code: String): GspRegion {
    val clean = code.trim().uppercase().removePrefix("_")
    return UK_GSP_REGIONS.firstOrNull { it.code == clean }
        ?: UK_GSP_REGIONS.first { it.code == "C" }
}

data class AgileSlot(
    val validFromUtc: String,
    val validToUtc: String,
    val startEpochMillis: Long,
    val endEpochMillis: Long,
    val valueExcVat: Double,
    val valueIncVat: Double
) {
    val startUk: ZonedDateTime
        get() = Instant.ofEpochMilli(startEpochMillis).atZone(UK_ZONE)

    val endUk: ZonedDateTime
        get() = Instant.ofEpochMilli(endEpochMillis).atZone(UK_ZONE)

    val localDateIso: String
        get() = startUk.toLocalDate().toString()

    val dayHeaderLabel: String
        get() = startUk.format(DAY_HEADER_FMT)

    val fullDayHeaderLabel: String
        get() = startUk.format(FULL_DAY_FMT)

    val timeRangeLabel: String
        get() = "${startUk.format(TIME_FMT)} – ${endUk.format(TIME_FMT)}"

    val startTimeLabel: String
        get() = startUk.format(TIME_FMT)

    val isPlunge: Boolean
        get() = valueIncVat < 0.0

    fun isCurrentAt(nowMillis: Long): Boolean =
        nowMillis in startEpochMillis until endEpochMillis
}

data class BestTimeWindow(
    val durationHours: Int,
    val startEpochMillis: Long,
    val endEpochMillis: Long,
    val timeRangeLabel: String,
    val averagePencePerKwh: Double,
    val slots: List<AgileSlot>
)

data class HalfHourConsumption(
    val intervalStartUtc: String,
    val intervalEndUtc: String,
    val startEpochMillis: Long,
    val endEpochMillis: Long,
    val consumption: Double,      // Raw meter reading (kWh for electric, m³ or kWh for gas)
    val energyKwh: Double,        // Converted energy in kWh
    val unitRatePence: Double,    // Rate in p/kWh for this half hour
    val costPence: Double         // energyKwh * unitRatePence
) {
    val startUk: ZonedDateTime
        get() = Instant.ofEpochMilli(startEpochMillis).atZone(UK_ZONE)

    val endUk: ZonedDateTime
        get() = Instant.ofEpochMilli(endEpochMillis).atZone(UK_ZONE)

    val localDateIso: String
        get() = startUk.toLocalDate().toString()

    val timeRangeLabel: String
        get() = "${startUk.format(TIME_FMT)} – ${endUk.format(TIME_FMT)}"

    val costPounds: Double
        get() = costPence / 100.0
}

data class DailyConsumptionSummary(
    val dateIso: String,
    val shortLabel: String,
    val fullLabel: String,
    val axisLabel: String,
    val totalConsumption: Double,
    val totalEnergyKwh: Double,
    val usageCostPounds: Double,
    val standingChargePounds: Double,
    val includeStandingCharge: Boolean,
    val averageRatePence: Double,
    val readingsCount: Int,
    val slots: List<HalfHourConsumption>
) {
    val isPartial: Boolean
        get() = readingsCount < 48

    val totalCostPounds: Double
        get() = usageCostPounds + if (includeStandingCharge) standingChargePounds else 0.0
}

data class ThirtyDayConsumptionOverview(
    val totalCost30DaysPounds: Double,
    val totalUsage30Days: Double,
    val last7DaysCostPounds: Double,
    val last7DaysUsage: Double,
    val avgReportedDayCostPounds: Double,
    val avgReportedDayUsage: Double,
    val standingChargePoundsPerDay: Double,
    val includeStandingCharge: Boolean,
    val unitLabel: String,
    val lastCheckedLabel: String,
    val latestReadingLabel: String,
    val isSampleData: Boolean,
    val daysChronological: List<DailyConsumptionSummary>
) {
    val daysNewestFirst: List<DailyConsumptionSummary>
        get() = daysChronological.asReversed()
}

data class OctopusAccountConfig(
    val apiKey: String = BuildConfig.OCTOPUS_API_KEY,
    val accountNumber: String = BuildConfig.OCTOPUS_ACCOUNT_NUMBER,
    val baseUrl: String = BuildConfig.OCTOPUS_BASE_URL.ifBlank { "https://api.oegb-kraken.energy" },
    val agileProductCode: String = BuildConfig.OCTOPUS_DEFAULT_PRODUCT.ifBlank { "AGILE-24-10-01" },
    val regionCode: String = BuildConfig.OCTOPUS_DEFAULT_REGION.ifBlank { "C" },
    val agileTariffCode: String = "E-1R-${BuildConfig.OCTOPUS_DEFAULT_PRODUCT.ifBlank { "AGILE-24-10-01" }}-${BuildConfig.OCTOPUS_DEFAULT_REGION.ifBlank { "C" }}",
    val electricMpan: String = BuildConfig.OCTOPUS_ELECTRIC_MPAN,
    val electricSerial: String = BuildConfig.OCTOPUS_ELECTRIC_SERIAL,
    val electricStandingChargePence: Double = 67.0,
    val gasMprn: String = BuildConfig.OCTOPUS_GAS_MPRN,
    val gasSerial: String = BuildConfig.OCTOPUS_GAS_SERIAL,
    val gasProductCode: String = "",
    val gasTariffCode: String = "",
    val gasUnitRatePence: Double = 6.24,
    val gasStandingChargePence: Double = 29.0,
    val gasIsCubicMeters: Boolean = true,
    val addressSummary: String = ""
) {
    val hasApiKey: Boolean
        get() = apiKey.isNotBlank()

    val hasElectricMeter: Boolean
        get() = hasApiKey && electricMpan.isNotBlank() && electricSerial.isNotBlank()

    val hasGasMeter: Boolean
        get() = hasApiKey && gasMprn.isNotBlank() && gasSerial.isNotBlank()
}

data class AppPreferences(
    val themeMode: String = "LIGHT", // "LIGHT", "DARK", "SYSTEM"
    val includeStandingCharge: Boolean = true,
    val useThreeDecimals: Boolean = false,
    val highCostThresholdPence: Double = 35.0,
    val hasCompletedOnboarding: Boolean = BuildConfig.OCTOPUS_API_KEY.isNotBlank(),
    val showSampleConsumptionIfNoMeter: Boolean = true
)

object AgileCalculator {

    /**
     * Extracts the product code from a full Octopus tariff code.
     * Example: "E-1R-AGILE-24-10-01-C" -> "AGILE-24-10-01"
     *          "G-1R-VAR-22-11-01-C"   -> "VAR-22-11-01"
     */
    fun extractProductCodeFromTariff(tariffCode: String): String {
        val trimmed = tariffCode.trim()
        val regex = Regex("^[EG]-[12]R-(.+)-[A-P]$", RegexOption.IGNORE_CASE)
        val match = regex.matchEntire(trimmed)
        return match?.groupValues?.get(1) ?: trimmed
    }

    /**
     * Extracts the GSP region letter ("A".."P") from a tariff code.
     */
    fun extractRegionFromTariff(tariffCode: String): String? {
        val trimmed = tariffCode.trim().uppercase()
        val regex = Regex("^[EG]-[12]R-.+-([A-P])$")
        return regex.matchEntire(trimmed)?.groupValues?.get(1)
    }

    /**
     * Builds a standard single-register electricity tariff code from product code and GSP region.
     */
    fun buildAgileTariffCode(productCode: String, regionCode: String): String {
        val cleanProduct = productCode.trim().ifBlank { "AGILE-24-10-01" }
        val cleanRegion = regionCode.trim().uppercase().removePrefix("_").ifBlank { "C" }
        return "E-1R-$cleanProduct-$cleanRegion"
    }

    /**
     * Converts SMETS2 gas consumption in cubic meters (m³) to kWh using the standard
     * UK volume correction factor (1.02264) and calorific value (default 39.5 MJ/m³) / 3.6.
     */
    fun gasCubicMetersToKwh(cubicMeters: Double, calorificValue: Double = 39.5): Double {
        if (cubicMeters <= 0.0) return 0.0
        return cubicMeters * 1.02264 * calorificValue / 3.6
    }

    /**
     * Converts a Region C (London) Agile price to another UK DNO region using Octopus Energy's
     * published regional multiplier and 4 PM–7 PM peak adder formula.
     */
    fun convertRegionCPriceToRegion(
        priceExcVatRegionC: Double,
        isPeak4To7Pm: Boolean,
        targetRegion: GspRegion
    ): Double {
        val wholesaleBase = (if (isPeak4To7Pm) priceExcVatRegionC - 12.0 else priceExcVatRegionC) / 2.0
        val targetExcVat = wholesaleBase * targetRegion.multiplier +
            (if (isPeak4To7Pm) targetRegion.peakAdderPence else 0.0)
        return targetExcVat * 1.05
    }

    /**
     * Finds the cheapest consecutive N-hour window (2 * durationHours half-hour slots)
     * among slots whose start time is >= minStartEpochMillis.
     */
    fun findBestWindow(
        slots: List<AgileSlot>,
        durationHours: Int,
        minStartEpochMillis: Long
    ): BestTimeWindow? {
        val requiredSlots = durationHours * 2
        if (requiredSlots <= 0) return null

        val eligible = slots
            .filter { it.startEpochMillis >= minStartEpochMillis }
            .sortedBy { it.startEpochMillis }

        if (eligible.size < requiredSlots) return null

        var bestWindow: BestTimeWindow? = null
        val halfHourMs = 30L * 60L * 1000L

        for (i in 0..(eligible.size - requiredSlots)) {
            val windowSlots = eligible.subList(i, i + requiredSlots)
            // Verify slots are consecutive 30-min intervals
            var isContiguous = true
            for (j in 0 until windowSlots.size - 1) {
                if (abs(windowSlots[j + 1].startEpochMillis - windowSlots[j].endEpochMillis) > 1000L &&
                    abs(windowSlots[j + 1].startEpochMillis - windowSlots[j].startEpochMillis - halfHourMs) > 1000L
                ) {
                    isContiguous = false
                    break
                }
            }
            if (!isContiguous) continue

            val avg = windowSlots.map { it.valueIncVat }.average()
            if (bestWindow == null || avg < bestWindow.averagePencePerKwh) {
                val first = windowSlots.first()
                val last = windowSlots.last()
                val rangeLabel = "${first.startUk.format(TIME_FMT)} – ${last.endUk.format(TIME_FMT)}"
                bestWindow = BestTimeWindow(
                    durationHours = durationHours,
                    startEpochMillis = first.startEpochMillis,
                    endEpochMillis = last.endEpochMillis,
                    timeRangeLabel = rangeLabel,
                    averagePencePerKwh = avg,
                    slots = windowSlots
                )
            }
        }
        return bestWindow
    }

    fun formatRatePence(pence: Double, threeDecimals: Boolean = false): String {
        val fmt = if (threeDecimals) "%.3f" else "%.2f"
        return String.format(Locale.US, fmt, pence)
    }

    fun formatPounds(pounds: Double): String {
        val sign = if (pounds < 0) "-" else ""
        return "${sign}£${String.format(Locale.US, "%.2f", abs(pounds))}"
    }

    fun formatTimestamp(epochMillis: Long): String {
        return Instant.ofEpochMilli(epochMillis).atZone(UK_ZONE).format(CHECKED_FMT)
    }

    /**
     * Aggregates raw half-hourly consumption intervals into a 30-day overview matching
     * the iOS Agile Watcher Electricity & Gas Consumption screens.
     */
    fun buildThirtyDayOverview(
        readings: List<HalfHourConsumption>,
        standingChargePencePerDay: Double,
        includeStandingCharge: Boolean,
        unitLabel: String,
        lastCheckedEpochMillis: Long,
        isSampleData: Boolean
    ): ThirtyDayConsumptionOverview {
        val standingChargePounds = standingChargePencePerDay / 100.0
        val groupedByDate = readings
            .sortedBy { it.startEpochMillis }
            .groupBy { it.localDateIso }

        val dailySummaries = groupedByDate.entries
            .sortedBy { it.key }
            .takeLast(30)
            .map { (dateIso, daySlots) ->
                val localDate = LocalDate.parse(dateIso)
                val zdt = localDate.atStartOfDay(UK_ZONE)
                val totalRaw = daySlots.sumOf { it.consumption }
                val totalKwh = daySlots.sumOf { it.energyKwh }
                val usageCostPounds = daySlots.sumOf { it.costPence } / 100.0
                val avgRate = if (totalKwh > 0.001) {
                    daySlots.sumOf { it.costPence } / totalKwh
                } else {
                    daySlots.map { it.unitRatePence }.average().takeIf { !it.isNaN() } ?: 0.0
                }
                DailyConsumptionSummary(
                    dateIso = dateIso,
                    shortLabel = zdt.format(DAY_HEADER_FMT),
                    fullLabel = zdt.format(FULL_DAY_FMT),
                    axisLabel = zdt.format(AXIS_DAY_FMT),
                    totalConsumption = totalRaw,
                    totalEnergyKwh = totalKwh,
                    usageCostPounds = usageCostPounds,
                    standingChargePounds = standingChargePounds,
                    includeStandingCharge = includeStandingCharge,
                    averageRatePence = avgRate,
                    readingsCount = daySlots.size,
                    slots = daySlots.sortedBy { it.startEpochMillis }
                )
            }

        val totalCost30 = dailySummaries.sumOf { it.totalCostPounds }
        val totalUsage30 = dailySummaries.sumOf { it.totalConsumption }

        // Last 7 completed/reported days
        val completedDays = dailySummaries.filter { !it.isPartial }.ifEmpty { dailySummaries }
        val last7 = completedDays.takeLast(7)
        val last7Cost = last7.sumOf { it.totalCostPounds }
        val last7Usage = last7.sumOf { it.totalConsumption }

        val reportedCount = completedDays.size.coerceAtLeast(1)
        val avgDayCost = completedDays.sumOf { it.totalCostPounds } / reportedCount
        val avgDayUsage = completedDays.sumOf { it.totalConsumption } / reportedCount

        val latestSlot = readings.maxByOrNull { it.endEpochMillis }
        val latestReadingStr = latestSlot?.let { formatTimestamp(it.endEpochMillis) } ?: "No readings"

        return ThirtyDayConsumptionOverview(
            totalCost30DaysPounds = totalCost30,
            totalUsage30Days = totalUsage30,
            last7DaysCostPounds = last7Cost,
            last7DaysUsage = last7Usage,
            avgReportedDayCostPounds = avgDayCost,
            avgReportedDayUsage = avgDayUsage,
            standingChargePoundsPerDay = standingChargePounds,
            includeStandingCharge = includeStandingCharge,
            unitLabel = unitLabel,
            lastCheckedLabel = formatTimestamp(lastCheckedEpochMillis),
            latestReadingLabel = latestReadingStr,
            isSampleData = isSampleData,
            daysChronological = dailySummaries
        )
    }

    /**
     * Generates realistic 30-day half-hourly sample data for Electricity or Gas
     * so users can explore the full UI (matching screenshots 04, 05, 06) before
     * connecting a smart meter or while testing.
     */
    fun generateSampleConsumption(
        isElectric: Boolean,
        ratesByEpoch: Map<Long, Double>,
        config: OctopusAccountConfig,
        includeStandingCharge: Boolean,
        nowMillis: Long = System.currentTimeMillis()
    ): ThirtyDayConsumptionOverview {
        val todayUk = Instant.ofEpochMilli(nowMillis).atZone(UK_ZONE).toLocalDate()
        val slots = mutableListOf<HalfHourConsumption>()

        for (dayOffset in 29 downTo 0) {
            val date = todayUk.minusDays(dayOffset.toLong())
            val dayStart = date.atStartOfDay(UK_ZONE)
            // Make today a partial day (e.g. 3 readings received so far, matching screenshot 04 & 06)
            val maxIntervals = if (dayOffset == 0) 3 else 48
            val daySeed = date.dayOfYear + (if (isElectric) 17 else 53)
            val isSpikeDay = (dayOffset in listOf(2, 6, 10, 17, 28))

            for (slotIdx in 0 until maxIntervals) {
                val sStart = dayStart.plusMinutes(slotIdx * 30L)
                val sEnd = sStart.plusMinutes(30L)
                val sMillis = sStart.toInstant().toEpochMilli()
                val eMillis = sEnd.toInstant().toEpochMilli()
                val hour = sStart.hour

                if (isElectric) {
                    // Realistic household half-hour kWh profile with evening/laundry peaks
                    val baseLoad = 0.22 + 0.08 * sin(slotIdx * 0.25 + daySeed)
                    val morningPeak = if (hour in 7..8) 0.45 else 0.0
                    val eveningPeak = if (hour in 19..21) (if (isSpikeDay) 3.65 else 0.85) else 0.0
                    val kwh = (baseLoad + morningPeak + eveningPeak).coerceAtLeast(0.08)
                    val rate = ratesByEpoch[sMillis]
                        ?: (6.5 + 4.5 * sin((slotIdx - 10) * 0.18) + if (hour in 16..18) 16.0 else 0.0)
                    val costPence = kwh * rate
                    slots.add(
                        HalfHourConsumption(
                            intervalStartUtc = sStart.toInstant().toString(),
                            intervalEndUtc = sEnd.toInstant().toString(),
                            startEpochMillis = sMillis,
                            endEpochMillis = eMillis,
                            consumption = (kwh * 100.0).roundToInt() / 100.0,
                            energyKwh = kwh,
                            unitRatePence = (rate * 100.0).roundToInt() / 100.0,
                            costPence = costPence
                        )
                    )
                } else {
                    // Gas profile (in m³ or kWh)
                    val seasonalFactor = 1.0 + (29 - dayOffset) * 0.035
                    val heatingHour = if (hour in 6..8 || hour in 17..21) 0.14 * seasonalFactor else 0.01
                    val rawM3 = if (dayOffset == 0) 0.0 else (heatingHour * (if (isSpikeDay) 1.8 else 1.0))
                    val consumptionVal = if (config.gasIsCubicMeters) rawM3 else gasCubicMetersToKwh(rawM3)
                    val kwh = gasCubicMetersToKwh(rawM3)
                    val rate = config.gasUnitRatePence
                    slots.add(
                        HalfHourConsumption(
                            intervalStartUtc = sStart.toInstant().toString(),
                            intervalEndUtc = sEnd.toInstant().toString(),
                            startEpochMillis = sMillis,
                            endEpochMillis = eMillis,
                            consumption = (consumptionVal * 100.0).roundToInt() / 100.0,
                            energyKwh = kwh,
                            unitRatePence = rate,
                            costPence = kwh * rate
                        )
                    )
                }
            }
        }

        return buildThirtyDayOverview(
            readings = slots,
            standingChargePencePerDay = if (isElectric) config.electricStandingChargePence else config.gasStandingChargePence,
            includeStandingCharge = includeStandingCharge,
            unitLabel = if (isElectric || !config.gasIsCubicMeters) "kWh" else "m³",
            lastCheckedEpochMillis = nowMillis,
            isSampleData = true
        )
    }
}
