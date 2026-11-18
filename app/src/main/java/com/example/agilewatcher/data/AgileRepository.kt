package com.example.agilewatcher.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.time.Instant
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

data class AgileDashboardData(
    val slots: List<AgileSlot>,
    val currentSlot: AgileSlot?,
    val nextSlot: AgileSlot?,
    val averageForTodayPence: Double,
    val lowestTodaySlot: AgileSlot?,
    val checkedAtEpochMillis: Long,
    val electricOverview: ThirtyDayConsumptionOverview?,
    val gasOverview: ThirtyDayConsumptionOverview?,
    val errorMessage: String? = null
)

class AgileRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("agile_watcher_prefs", Context.MODE_PRIVATE)

    val apiClient = OctopusApiClient(loadAccountConfig())

    fun loadAccountConfig(): OctopusAccountConfig {
        val d = OctopusAccountConfig()
        val prod = prefs.getString("agile_product", d.agileProductCode) ?: d.agileProductCode
        val reg = prefs.getString("region_code", d.regionCode) ?: d.regionCode
        val defaultTariff = AgileCalculator.buildAgileTariffCode(prod, reg)
        return OctopusAccountConfig(
            apiKey = prefs.getString("api_key", d.apiKey) ?: d.apiKey,
            accountNumber = prefs.getString("account_number", d.accountNumber) ?: d.accountNumber,
            baseUrl = prefs.getString("base_url", d.baseUrl) ?: d.baseUrl,
            agileProductCode = prod,
            regionCode = reg,
            agileTariffCode = prefs.getString("agile_tariff", defaultTariff) ?: defaultTariff,
            electricMpan = prefs.getString("electric_mpan", d.electricMpan) ?: d.electricMpan,
            electricSerial = prefs.getString("electric_serial", d.electricSerial) ?: d.electricSerial,
            electricStandingChargePence = prefs.getFloat("electric_standing_pence", d.electricStandingChargePence.toFloat()).toDouble(),
            gasMprn = prefs.getString("gas_mprn", d.gasMprn) ?: d.gasMprn,
            gasSerial = prefs.getString("gas_serial", d.gasSerial) ?: d.gasSerial,
            gasProductCode = prefs.getString("gas_product", d.gasProductCode) ?: d.gasProductCode,
            gasTariffCode = prefs.getString("gas_tariff", d.gasTariffCode) ?: d.gasTariffCode,
            gasUnitRatePence = prefs.getFloat("gas_unit_rate_pence", d.gasUnitRatePence.toFloat()).toDouble(),
            gasStandingChargePence = prefs.getFloat("gas_standing_pence", d.gasStandingChargePence.toFloat()).toDouble(),
            gasIsCubicMeters = prefs.getBoolean("gas_is_cubic_meters", d.gasIsCubicMeters),
            addressSummary = prefs.getString("address_summary", d.addressSummary) ?: d.addressSummary
        )
    }

    fun saveAccountConfig(cfg: OctopusAccountConfig) {
        prefs.edit()
            .putString("api_key", cfg.apiKey)
            .putString("account_number", cfg.accountNumber)
            .putString("base_url", cfg.baseUrl)
            .putString("agile_product", cfg.agileProductCode)
            .putString("region_code", cfg.regionCode)
            .putString("agile_tariff", cfg.agileTariffCode)
            .putString("electric_mpan", cfg.electricMpan)
            .putString("electric_serial", cfg.electricSerial)
            .putFloat("electric_standing_pence", cfg.electricStandingChargePence.toFloat())
            .putString("gas_mprn", cfg.gasMprn)
            .putString("gas_serial", cfg.gasSerial)
            .putString("gas_product", cfg.gasProductCode)
            .putString("gas_tariff", cfg.gasTariffCode)
            .putFloat("gas_unit_rate_pence", cfg.gasUnitRatePence.toFloat())
            .putFloat("gas_standing_pence", cfg.gasStandingChargePence.toFloat())
            .putBoolean("gas_is_cubic_meters", cfg.gasIsCubicMeters)
            .putString("address_summary", cfg.addressSummary)
            .apply()
        apiClient.config = cfg
    }

    fun loadAppPreferences(): AppPreferences {
        val d = AppPreferences()
        return AppPreferences(
            themeMode = prefs.getString("theme_mode", d.themeMode) ?: d.themeMode,
            includeStandingCharge = prefs.getBoolean("include_standing_charge", d.includeStandingCharge),
            useThreeDecimals = prefs.getBoolean("use_three_decimals", d.useThreeDecimals),
            highCostThresholdPence = prefs.getFloat("high_cost_threshold", d.highCostThresholdPence.toFloat()).toDouble(),
            hasCompletedOnboarding = prefs.getBoolean("has_completed_onboarding", d.hasCompletedOnboarding),
            showSampleConsumptionIfNoMeter = prefs.getBoolean("show_sample_consumption", d.showSampleConsumptionIfNoMeter)
        )
    }

    fun saveAppPreferences(appPrefs: AppPreferences) {
        prefs.edit()
            .putString("theme_mode", appPrefs.themeMode)
            .putBoolean("include_standing_charge", appPrefs.includeStandingCharge)
            .putBoolean("use_three_decimals", appPrefs.useThreeDecimals)
            .putFloat("high_cost_threshold", appPrefs.highCostThresholdPence.toFloat())
            .putBoolean("has_completed_onboarding", appPrefs.hasCompletedOnboarding)
            .putBoolean("show_sample_consumption", appPrefs.showSampleConsumptionIfNoMeter)
            .apply()
    }

    fun saveWidgetSnapshot(currentRateText: String, nextRateText: String, avgRateText: String) {
        prefs.edit()
            .putString("widget_current_rate", currentRateText)
            .putString("widget_next_rate", nextRateText)
            .putString("widget_avg_rate", avgRateText)
            .apply()
    }

    fun loadWidgetSnapshot(): Triple<String, String, String> {
        return Triple(
            prefs.getString("widget_current_rate", "--.-p/kWh") ?: "--.-p/kWh",
            prefs.getString("widget_next_rate", "Next: --.-p/kWh") ?: "Next: --.-p/kWh",
            prefs.getString("widget_avg_rate", "Avg today: --.-p/kWh") ?: "Avg today: --.-p/kWh"
        )
    }

    suspend fun discoverAndSaveAccount(apiKey: String, accountNumber: String = ""): AccountDiscoveryResult {
        val res = apiClient.discoverAccountDetails(apiKey, accountNumber)
        if (res.success && res.updatedConfig != null) {
            saveAccountConfig(res.updatedConfig)
            val currentPrefs = loadAppPreferences()
            saveAppPreferences(currentPrefs.copy(hasCompletedOnboarding = true))
        }
        return res
    }

    /**
     * Fetches live Agile prices (from start of yesterday UK time through end of tomorrow UK time,
     * plus 30 days back if electricity consumption is configured) and builds the complete
     * Dashboard + Electricity + Gas state.
     */
    suspend fun refreshAllData(): AgileDashboardData = coroutineScope {
        var cfg = loadAccountConfig()
        val appPrefs = loadAppPreferences()
        apiClient.config = cfg

        // If an API key was injected via .env / local.properties but meter MPAN/serials
        // haven't been discovered yet, auto-discover them from Octopus Energy automatically!
        if (cfg.hasApiKey && cfg.electricMpan.isBlank()) {
            val discovery = discoverAndSaveAccount(cfg.apiKey, cfg.accountNumber)
            if (discovery.success && discovery.updatedConfig != null) {
                cfg = discovery.updatedConfig
                apiClient.config = cfg
            }
        }

        val nowMs = System.currentTimeMillis()
        val nowUk = ZonedDateTime.now(UK_ZONE)
        val todayDateIso = nowUk.toLocalDate().toString()

        // If user has an electric meter configured, fetch 31 days of rates so 30-day consumption
        // costs are accurately priced against historical half-hour Agile rates; otherwise fetch 3 days.
        val daysBack = if (cfg.hasElectricMeter) 31L else 1L
        val periodFrom = nowUk.toLocalDate().minusDays(daysBack).atStartOfDay(UK_ZONE).toInstant().toString()
        val periodTo = nowUk.toLocalDate().plusDays(2).atStartOfDay(UK_ZONE).toInstant().toString()

        val ratesDeferred = async {
            apiClient.fetchAgileUnitRates(
                periodFromIso = periodFrom,
                periodToIso = periodTo,
                productCode = cfg.agileProductCode,
                tariffCode = cfg.agileTariffCode
            )
        }

        val ratesResult = ratesDeferred.await()
        val allSlots = ratesResult.getOrDefault(emptyList())
        val ratesByEpoch = allSlots.associate { it.startEpochMillis to it.valueIncVat }

        // Filter dashboard slots to Today & Tomorrow (in UK time)
        val todayStartEpoch = nowUk.toLocalDate().atStartOfDay(UK_ZONE).toInstant().toEpochMilli()
        val dashboardSlots = allSlots.filter { it.endEpochMillis > todayStartEpoch }

        val currentSlot = dashboardSlots.firstOrNull { it.isCurrentAt(nowMs) }
            ?: allSlots.firstOrNull { it.isCurrentAt(nowMs) }

        val nextSlot = if (currentSlot != null) {
            dashboardSlots.firstOrNull { it.startEpochMillis == currentSlot.endEpochMillis }
        } else {
            dashboardSlots.firstOrNull { it.startEpochMillis > nowMs }
        }

        val todaySlots = dashboardSlots.filter { it.localDateIso == todayDateIso }
        val avgToday = if (todaySlots.isNotEmpty()) {
            todaySlots.map { it.valueIncVat }.average()
        } else {
            0.0
        }
        val lowestToday = todaySlots.minByOrNull { it.valueIncVat }

        // Save snapshot for Android Home Screen Widget
        val decimals = appPrefs.useThreeDecimals
        val curText = currentSlot?.let { "${AgileCalculator.formatRatePence(it.valueIncVat, decimals)}p/kWh" } ?: "--.-p/kWh"
        val nxtText = nextSlot?.let { "Next: ${AgileCalculator.formatRatePence(it.valueIncVat, decimals)}p/kWh" } ?: "Next: --.-p/kWh"
        val avgText = "Avg today: ${AgileCalculator.formatRatePence(avgToday, decimals)}p/kWh"
        saveWidgetSnapshot(curText, nxtText, avgText)

        // Fetch Electricity & Gas 30-day consumption concurrently
        val elecDeferred = async {
            if (cfg.hasElectricMeter) {
                val res = apiClient.fetchElectricityConsumption30Days(ratesByEpoch)
                val readings = res.getOrNull()
                if (!readings.isNullOrEmpty()) {
                    AgileCalculator.buildThirtyDayOverview(
                        readings = readings,
                        standingChargePencePerDay = cfg.electricStandingChargePence,
                        includeStandingCharge = appPrefs.includeStandingCharge,
                        unitLabel = "kWh",
                        lastCheckedEpochMillis = nowMs,
                        isSampleData = false
                    )
                } else if (appPrefs.showSampleConsumptionIfNoMeter) {
                    AgileCalculator.generateSampleConsumption(
                        isElectric = true,
                        ratesByEpoch = ratesByEpoch,
                        config = cfg,
                        includeStandingCharge = appPrefs.includeStandingCharge,
                        nowMillis = nowMs
                    )
                } else null
            } else if (appPrefs.showSampleConsumptionIfNoMeter) {
                AgileCalculator.generateSampleConsumption(
                    isElectric = true,
                    ratesByEpoch = ratesByEpoch,
                    config = cfg,
                    includeStandingCharge = appPrefs.includeStandingCharge,
                    nowMillis = nowMs
                )
            } else null
        }

        val gasDeferred = async {
            if (cfg.hasGasMeter) {
                val res = apiClient.fetchGasConsumption30Days()
                val readings = res.getOrNull()
                if (!readings.isNullOrEmpty()) {
                    AgileCalculator.buildThirtyDayOverview(
                        readings = readings,
                        standingChargePencePerDay = cfg.gasStandingChargePence,
                        includeStandingCharge = appPrefs.includeStandingCharge,
                        unitLabel = if (cfg.gasIsCubicMeters) "m³" else "kWh",
                        lastCheckedEpochMillis = nowMs,
                        isSampleData = false
                    )
                } else if (appPrefs.showSampleConsumptionIfNoMeter) {
                    AgileCalculator.generateSampleConsumption(
                        isElectric = false,
                        ratesByEpoch = ratesByEpoch,
                        config = cfg,
                        includeStandingCharge = appPrefs.includeStandingCharge,
                        nowMillis = nowMs
                    )
                } else null
            } else if (appPrefs.showSampleConsumptionIfNoMeter) {
                AgileCalculator.generateSampleConsumption(
                    isElectric = false,
                    ratesByEpoch = ratesByEpoch,
                    config = cfg,
                    includeStandingCharge = appPrefs.includeStandingCharge,
                    nowMillis = nowMs
                )
            } else null
        }

        AgileDashboardData(
            slots = dashboardSlots,
            currentSlot = currentSlot,
            nextSlot = nextSlot,
            averageForTodayPence = avgToday,
            lowestTodaySlot = lowestToday,
            checkedAtEpochMillis = nowMs,
            electricOverview = elecDeferred.await(),
            gasOverview = gasDeferred.await(),
            errorMessage = ratesResult.exceptionOrNull()?.localizedMessage
        )
    }
}
