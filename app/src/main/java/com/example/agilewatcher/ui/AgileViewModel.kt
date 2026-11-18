package com.example.agilewatcher.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.agilewatcher.data.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

data class AgileUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isDiscoveringAccount: Boolean = false,
    val accountConfig: OctopusAccountConfig = OctopusAccountConfig(),
    val appPreferences: AppPreferences = AppPreferences(),
    val showOnboardingScreen: Boolean = false,
    val slots: List<AgileSlot> = emptyList(),
    val currentSlot: AgileSlot? = null,
    val nextSlot: AgileSlot? = null,
    val averageForTodayPence: Double = 0.0,
    val lowestTodaySlot: AgileSlot? = null,
    val checkedAtEpochMillis: Long = System.currentTimeMillis(),
    val showEarlierPricesToday: Boolean = false,
    // Best Times Sheet / Screen State (Screenshot 03)
    val isBestTimesVisible: Boolean = false,
    val bestTimesDurationHours: Int = 1, // 1, 2, or 4 hours
    val bestTimesChartDay: String = "TODAY", // "TODAY" or "TOMORROW"
    val bestWindowToday: BestTimeWindow? = null,
    val bestWindowTomorrow: BestTimeWindow? = null,
    // Electricity 30-day & Day Detail State (Screenshots 04 & 05)
    val electricOverview: ThirtyDayConsumptionOverview? = null,
    val selectedElectricDay: DailyConsumptionSummary? = null,
    // Gas 30-day & Day Detail + Setup Tariff Dialog State (Screenshot 06)
    val gasOverview: ThirtyDayConsumptionOverview? = null,
    val selectedGasDay: DailyConsumptionSummary? = null,
    val isGasTariffDialogVisible: Boolean = false,
    // Active Agile Products from Octopus API
    val availableAgileProducts: List<AgileProductInfo> = emptyList(),
    val errorMessage: String? = null,
    val userNotification: String? = null
)

class AgileViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AgileRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(
        run {
            val cfg = repository.loadAccountConfig()
            val prefs = repository.loadAppPreferences()
            AgileUiState(
                accountConfig = cfg,
                appPreferences = prefs,
                showOnboardingScreen = !prefs.hasCompletedOnboarding && cfg.apiKey.isBlank()
            )
        }
    )
    val uiState: StateFlow<AgileUiState> = _uiState.asStateFlow()

    private var autoRefreshJob: Job? = null

    init {
        loadAvailableProducts()
        startAutoRefresh()
    }

    private fun loadAvailableProducts() {
        viewModelScope.launch {
            val products = repository.apiClient.fetchActiveAgileProducts()
            if (products.isNotEmpty()) {
                _uiState.value = _uiState.value.copy(availableAgileProducts = products)
            }
        }
    }

    private fun startAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = viewModelScope.launch {
            while (true) {
                refreshAll()
                // Refresh every 5 minutes (or when user pulls/taps refresh)
                delay(5 * 60 * 1000L)
            }
        }
    }

    fun refreshAll() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            val data = repository.refreshAllData()
            val duration = _uiState.value.bestTimesDurationHours
            val (todayWin, tomorrowWin) = computeBestWindows(data.slots, duration)

            // Keep selected day detail in sync if open
            val updatedElecDay = _uiState.value.selectedElectricDay?.let { sel ->
                data.electricOverview?.daysChronological?.firstOrNull { it.dateIso == sel.dateIso } ?: sel
            }
            val updatedGasDay = _uiState.value.selectedGasDay?.let { sel ->
                data.gasOverview?.daysChronological?.firstOrNull { it.dateIso == sel.dateIso } ?: sel
            }

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                isRefreshing = false,
                accountConfig = repository.loadAccountConfig(),
                slots = data.slots,
                currentSlot = data.currentSlot,
                nextSlot = data.nextSlot,
                averageForTodayPence = data.averageForTodayPence,
                lowestTodaySlot = data.lowestTodaySlot,
                checkedAtEpochMillis = data.checkedAtEpochMillis,
                bestWindowToday = todayWin,
                bestWindowTomorrow = tomorrowWin,
                electricOverview = data.electricOverview,
                selectedElectricDay = updatedElecDay,
                gasOverview = data.gasOverview,
                selectedGasDay = updatedGasDay,
                errorMessage = data.errorMessage
            )

            AgileRateWidgetProvider.updateAllWidgets(getApplication())
        }
    }

    private fun computeBestWindows(
        slots: List<AgileSlot>,
        durationHours: Int
    ): Pair<BestTimeWindow?, BestTimeWindow?> {
        val nowMs = System.currentTimeMillis()
        val nowUk = ZonedDateTime.now(UK_ZONE)
        val todayIso = nowUk.toLocalDate().toString()
        val tomorrowIso = nowUk.toLocalDate().plusDays(1).toString()

        val todaySlots = slots.filter { it.localDateIso == todayIso }
        val tomorrowSlots = slots.filter { it.localDateIso == tomorrowIso }

        val todayWin = AgileCalculator.findBestWindow(
            slots = todaySlots,
            durationHours = durationHours,
            minStartEpochMillis = nowMs
        )
        val tomorrowStartMs = nowUk.toLocalDate().plusDays(1).atStartOfDay(UK_ZONE).toInstant().toEpochMilli()
        val tomorrowWin = AgileCalculator.findBestWindow(
            slots = tomorrowSlots,
            durationHours = durationHours,
            minStartEpochMillis = tomorrowStartMs
        )
        return Pair(todayWin, tomorrowWin)
    }

    fun toggleEarlierPricesToday() {
        _uiState.value = _uiState.value.copy(
            showEarlierPricesToday = !_uiState.value.showEarlierPricesToday
        )
    }

    fun showBestTimes(show: Boolean) {
        _uiState.value = _uiState.value.copy(isBestTimesVisible = show)
    }

    fun setBestTimesDuration(hours: Int) {
        val (todayWin, tomorrowWin) = computeBestWindows(_uiState.value.slots, hours)
        _uiState.value = _uiState.value.copy(
            bestTimesDurationHours = hours,
            bestWindowToday = todayWin,
            bestWindowTomorrow = tomorrowWin
        )
    }

    fun setBestTimesChartDay(day: String) {
        _uiState.value = _uiState.value.copy(bestTimesChartDay = day)
    }

    fun selectElectricDay(day: DailyConsumptionSummary?) {
        _uiState.value = _uiState.value.copy(selectedElectricDay = day)
    }

    fun selectGasDay(day: DailyConsumptionSummary?) {
        _uiState.value = _uiState.value.copy(selectedGasDay = day)
    }

    fun showGasTariffDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(isGasTariffDialogVisible = show)
    }

    fun showOnboarding(show: Boolean) {
        _uiState.value = _uiState.value.copy(showOnboardingScreen = show)
    }

    fun skipOnboardingToPublicPrices(regionCode: String = _uiState.value.accountConfig.regionCode) {
        val cfg = _uiState.value.accountConfig
        val updatedCfg = cfg.copy(
            regionCode = regionCode,
            agileTariffCode = AgileCalculator.buildAgileTariffCode(cfg.agileProductCode, regionCode)
        )
        val updatedPrefs = _uiState.value.appPreferences.copy(hasCompletedOnboarding = true)
        repository.saveAccountConfig(updatedCfg)
        repository.saveAppPreferences(updatedPrefs)
        _uiState.value = _uiState.value.copy(
            accountConfig = updatedCfg,
            appPreferences = updatedPrefs,
            showOnboardingScreen = false,
            userNotification = "Showing live Agile prices for Region $regionCode (${findGspRegion(regionCode).name})."
        )
        refreshAll()
    }

    fun connectWithApiKey(apiKey: String, accountNumber: String = "") {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDiscoveringAccount = true)
            val result = repository.discoverAndSaveAccount(apiKey, accountNumber)
            val updatedCfg = repository.loadAccountConfig()
            val updatedPrefs = repository.loadAppPreferences()
            _uiState.value = _uiState.value.copy(
                isDiscoveringAccount = false,
                accountConfig = updatedCfg,
                appPreferences = updatedPrefs,
                showOnboardingScreen = if (result.success) false else _uiState.value.showOnboardingScreen,
                userNotification = result.message
            )
            if (result.success) {
                refreshAll()
            }
        }
    }

    fun updateAccountConfig(newConfig: OctopusAccountConfig) {
        repository.saveAccountConfig(newConfig)
        _uiState.value = _uiState.value.copy(
            accountConfig = newConfig,
            userNotification = "Account & tariff settings saved."
        )
        refreshAll()
    }

    fun updateGasTariffSettings(
        unitRatePence: Double,
        standingChargePence: Double,
        isCubicMeters: Boolean,
        gasMprn: String,
        gasSerial: String
    ) {
        val updated = _uiState.value.accountConfig.copy(
            gasUnitRatePence = unitRatePence,
            gasStandingChargePence = standingChargePence,
            gasIsCubicMeters = isCubicMeters,
            gasMprn = gasMprn.trim(),
            gasSerial = gasSerial.trim()
        )
        repository.saveAccountConfig(updated)
        _uiState.value = _uiState.value.copy(
            accountConfig = updated,
            isGasTariffDialogVisible = false,
            userNotification = "Gas tariff updated (${AgileCalculator.formatRatePence(unitRatePence)}p/kWh, standing charge £${String.format("%.2f", standingChargePence / 100.0)}/day)."
        )
        refreshAll()
    }

    fun updateAppPreferences(newPrefs: AppPreferences) {
        repository.saveAppPreferences(newPrefs)
        _uiState.value = _uiState.value.copy(appPreferences = newPrefs)
        refreshAll()
    }

    fun scheduleWindowReminder(window: BestTimeWindow) {
        val decimals = _uiState.value.appPreferences.useThreeDecimals
        val avgStr = AgileCalculator.formatRatePence(window.averagePencePerKwh, decimals)
        val title = "⚡ Best Agile Time (${window.durationHours}h): ${window.timeRangeLabel}"
        val message = "Average rate: ${avgStr}p/kWh. Time to run your high-usage appliances!"
        val ok = AgileReminderReceiver.scheduleReminder(
            context = getApplication(),
            title = title,
            message = message,
            triggerAtMillis = window.startEpochMillis
        )
        _uiState.value = _uiState.value.copy(
            userNotification = if (ok) {
                "Reminder set for ${window.timeRangeLabel} (${avgStr}p/kWh average)."
            } else {
                "Could not schedule reminder."
            }
        )
    }

    fun scheduleSlotReminder(slot: AgileSlot) {
        val decimals = _uiState.value.appPreferences.useThreeDecimals
        val rateStr = AgileCalculator.formatRatePence(slot.valueIncVat, decimals)
        val title = "⚡ Agile Rate Alert: ${slot.timeRangeLabel}"
        val message = "Electricity rate is ${rateStr}p/kWh for ${slot.timeRangeLabel}."
        val ok = AgileReminderReceiver.scheduleReminder(
            context = getApplication(),
            title = title,
            message = message,
            triggerAtMillis = slot.startEpochMillis
        )
        _uiState.value = _uiState.value.copy(
            userNotification = if (ok) {
                "Reminder set for ${slot.timeRangeLabel} (${rateStr}p/kWh)."
            } else {
                "Could not schedule reminder."
            }
        )
    }

    fun notifyUser(msg: String) {
        _uiState.value = _uiState.value.copy(userNotification = msg)
    }

    fun clearNotification() {
        _uiState.value = _uiState.value.copy(userNotification = null)
    }

    override fun onCleared() {
        super.onCleared()
        autoRefreshJob?.cancel()
    }
}
