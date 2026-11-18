package com.example.agilewatcher.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

data class AgileProductInfo(
    val code: String,
    val fullName: String,
    val displayName: String,
    val description: String
)

data class AccountDiscoveryResult(
    val success: Boolean,
    val updatedConfig: OctopusAccountConfig? = null,
    val message: String
)

class OctopusApiClient(
    var config: OctopusAccountConfig = OctopusAccountConfig()
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private fun cleanBaseUrl(): String =
        config.baseUrl.trim().removeSuffix("/").ifBlank { "https://api.oegb-kraken.energy" }

    /**
     * Parses an ISO-8601 timestamp string (e.g. "2026-11-18T22:30:00Z" or "2026-06-01T01:00:00+01:00")
     * into epoch milliseconds.
     */
    fun parseIsoToMillis(iso: String): Long {
        return try {
            Instant.parse(iso).toEpochMilli()
        } catch (_: Exception) {
            try {
                OffsetDateTime.parse(iso).toInstant().toEpochMilli()
            } catch (_: Exception) {
                0L
            }
        }
    }

    /**
     * Fetches half-hourly Agile unit rates for the configured product and tariff code.
     * Public endpoint — does not require an API key.
     */
    suspend fun fetchAgileUnitRates(
        periodFromIso: String,
        periodToIso: String,
        productCode: String = config.agileProductCode,
        tariffCode: String = config.agileTariffCode
    ): Result<List<AgileSlot>> = withContext(Dispatchers.IO) {
        try {
            val base = cleanBaseUrl()
            val prod = productCode.trim().ifBlank { "AGILE-24-10-01" }
            val tariff = tariffCode.trim().ifBlank { "E-1R-$prod-${config.regionCode.ifBlank { "C" }}" }
            val url = "$base/v1/products/$prod/electricity-tariffs/$tariff/standard-unit-rates/" +
                "?period_from=$periodFromIso&period_to=$periodToIso&page_size=1500"

            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response from Octopus API"))
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${response.code}: Failed to fetch Agile rates for $tariff"))
                }
                val json = JSONObject(body)
                val results = json.optJSONArray("results") ?: JSONArray()
                val slots = mutableListOf<AgileSlot>()

                for (i in 0 until results.length()) {
                    val item = results.optJSONObject(i) ?: continue
                    val validFrom = item.optString("valid_from", "")
                    val validTo = item.optString("valid_to", "")
                    val startMs = parseIsoToMillis(validFrom)
                    val endMs = parseIsoToMillis(validTo)
                    if (startMs <= 0L || endMs <= 0L) continue

                    val excVat = item.optDouble("value_exc_vat", 0.0)
                    val incVat = item.optDouble("value_inc_vat", excVat * 1.05)
                    slots.add(
                        AgileSlot(
                            validFromUtc = validFrom,
                            validToUtc = validTo,
                            startEpochMillis = startMs,
                            endEpochMillis = endMs,
                            valueExcVat = excVat,
                            valueIncVat = incVat
                        )
                    )
                }

                Result.success(slots.sortedBy { it.startEpochMillis })
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetches the current standing charge (inc VAT, in pence/day) for the electricity tariff.
     */
    suspend fun fetchElectricStandingChargePence(
        productCode: String = config.agileProductCode,
        tariffCode: String = config.agileTariffCode
    ): Double? = withContext(Dispatchers.IO) {
        try {
            val base = cleanBaseUrl()
            val url = "$base/v1/products/$productCode/electricity-tariffs/$tariffCode/standing-charges/?page_size=5"
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val json = JSONObject(response.body?.string() ?: return@withContext null)
                val results = json.optJSONArray("results") ?: return@withContext null
                if (results.length() > 0) {
                    return@withContext results.getJSONObject(0).optDouble("value_inc_vat", 67.0)
                }
            }
        } catch (_: Exception) {
        }
        null
    }

    /**
     * Fetches active Agile import products from `/v1/products/?brand=OCTOPUS_ENERGY`.
     */
    suspend fun fetchActiveAgileProducts(): List<AgileProductInfo> = withContext(Dispatchers.IO) {
        try {
            val base = cleanBaseUrl()
            val url = "$base/v1/products/?brand=OCTOPUS_ENERGY&is_variable=true&is_green=true"
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val json = JSONObject(response.body?.string() ?: return@withContext emptyList())
                val results = json.optJSONArray("results") ?: return@withContext emptyList()
                val list = mutableListOf<AgileProductInfo>()
                for (i in 0 until results.length()) {
                    val item = results.optJSONObject(i) ?: continue
                    val code = item.optString("code", "")
                    val direction = item.optString("direction", "IMPORT")
                    if (code.contains("AGILE", ignoreCase = true) && !direction.equals("EXPORT", ignoreCase = true) && !code.contains("OUTGOING", ignoreCase = true)) {
                        list.add(
                            AgileProductInfo(
                                code = code,
                                fullName = item.optString("full_name", code),
                                displayName = item.optString("display_name", "Agile Octopus"),
                                description = item.optString("description", "")
                            )
                        )
                    }
                }
                return@withContext list
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Discovers the Octopus Account Number from an API key alone using the Kraken GraphQL endpoint:
     * 1. mutation obtainKrakenToken(input: { APIKey }) -> token
     * 2. query { viewer { accounts { number } } } -> account number
     */
    suspend fun discoverAccountNumberViaGraphQl(apiKey: String): String? = withContext(Dispatchers.IO) {
        try {
            val base = cleanBaseUrl()
            val graphqlUrl = "$base/v1/graphql/"

            // 1. Obtain Kraken JWT from APIKey
            val tokenMutation = JSONObject().apply {
                put(
                    "query",
                    "mutation ObtainToken(\$input: ObtainJSONWebTokenInput!) { obtainKrakenToken(input: \$input) { token } }"
                )
                put(
                    "variables",
                    JSONObject().apply {
                        put("input", JSONObject().apply { put("APIKey", apiKey.trim()) })
                    }
                )
            }

            val tokenReq = Request.Builder()
                .url(graphqlUrl)
                .post(tokenMutation.toString().toRequestBody(jsonMediaType))
                .build()

            val jwtToken = client.newCall(tokenReq).execute().use { resp ->
                val str = resp.body?.string() ?: return@use null
                val json = JSONObject(str)
                json.optJSONObject("data")
                    ?.optJSONObject("obtainKrakenToken")
                    ?.optString("token", "")
                    ?.takeIf { it.isNotBlank() }
            } ?: return@withContext null

            // 2. Query viewer.accounts
            val viewerQuery = JSONObject().apply {
                put("query", "query { viewer { accounts { number } } }")
            }

            val viewerReq = Request.Builder()
                .url(graphqlUrl)
                .addHeader("Authorization", jwtToken)
                .post(viewerQuery.toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(viewerReq).execute().use { resp ->
                val str = resp.body?.string() ?: return@use null
                val json = JSONObject(str)
                val accounts = json.optJSONObject("data")
                    ?.optJSONObject("viewer")
                    ?.optJSONArray("accounts")
                if (accounts != null && accounts.length() > 0) {
                    return@withContext accounts.getJSONObject(0).optString("number", "").takeIf { it.isNotBlank() }
                }
            }
        } catch (_: Exception) {
        }
        null
    }

    /**
     * Connects with the user's Octopus API key, discovers their account number (if not provided),
     * and inspects `/v1/accounts/{account_number}/` to find their home address, electricity MPAN &
     * serial number, Agile tariff & region, and optional gas MPRN, serial number & tariff.
     */
    suspend fun discoverAccountDetails(
        apiKeyInput: String,
        explicitAccountNumber: String = config.accountNumber
    ): AccountDiscoveryResult = withContext(Dispatchers.IO) {
        val cleanKey = apiKeyInput.trim()
        if (cleanKey.isBlank()) {
            return@withContext AccountDiscoveryResult(
                success = false,
                message = "Please enter your Octopus API key (starts with sk_live_...)."
            )
        }

        try {
            val resolvedAccountNumber = explicitAccountNumber.trim().ifBlank {
                discoverAccountNumberViaGraphQl(cleanKey) ?: ""
            }

            if (resolvedAccountNumber.isBlank()) {
                return@withContext AccountDiscoveryResult(
                    success = false,
                    message = "Could not automatically look up account number from API key. Please check your key or enter your Account Number (e.g. A-AAAA1111) in Settings."
                )
            }

            val base = cleanBaseUrl()
            val url = "$base/v1/accounts/$resolvedAccountNumber/"
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", Credentials.basic(cleanKey, ""))
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext AccountDiscoveryResult(
                        success = false,
                        message = "Octopus API returned HTTP ${response.code} for account $resolvedAccountNumber. Check your API key and account number."
                    )
                }

                val json = JSONObject(body)
                val properties = json.optJSONArray("properties") ?: JSONArray()
                if (properties.length() == 0) {
                    return@withContext AccountDiscoveryResult(
                        success = false,
                        message = "No properties found on account $resolvedAccountNumber."
                    )
                }

                // Prefer active property (moved_out_at == null)
                var chosenProp = properties.getJSONObject(0)
                for (i in 0 until properties.length()) {
                    val prop = properties.getJSONObject(i)
                    if (prop.isNull("moved_out_at")) {
                        chosenProp = prop
                        break
                    }
                }

                val addr1 = chosenProp.optString("address_line_1", "")
                val postcode = chosenProp.optString("postcode", "")
                val addressSummary = listOf(addr1, postcode).filter { it.isNotBlank() }.joinToString(", ")

                var mpan = config.electricMpan
                var elecSerial = config.electricSerial
                var agileTariff = config.agileTariffCode
                var agileProduct = config.agileProductCode
                var region = config.regionCode

                val elecPoints = chosenProp.optJSONArray("electricity_meter_points") ?: JSONArray()
                for (i in 0 until elecPoints.length()) {
                    val ep = elecPoints.getJSONObject(i)
                    if (ep.optBoolean("is_export", false)) continue
                    mpan = ep.optString("mpan", mpan)

                    val meters = ep.optJSONArray("meters") ?: JSONArray()
                    if (meters.length() > 0) {
                        elecSerial = meters.getJSONObject(meters.length() - 1).optString("serial_number", elecSerial)
                    }

                    val agreements = ep.optJSONArray("agreements") ?: JSONArray()
                    val latestTariff = pickActiveTariffCode(agreements)
                    if (!latestTariff.isNullOrBlank()) {
                        AgileCalculator.extractRegionFromTariff(latestTariff)?.let { region = it }
                        if (latestTariff.contains("AGILE", ignoreCase = true)) {
                            agileTariff = latestTariff
                            agileProduct = AgileCalculator.extractProductCodeFromTariff(latestTariff)
                        } else {
                            // Account is on a Fixed/Variable/Go tariff; keep Agile product and apply discovered GSP region
                            agileProduct = config.agileProductCode.ifBlank { "AGILE-24-10-01" }
                            agileTariff = AgileCalculator.buildAgileTariffCode(agileProduct, region)
                        }
                    }
                    break
                }

                var mprn = config.gasMprn
                var gasSerial = config.gasSerial
                var gasTariff = config.gasTariffCode
                var gasProduct = config.gasProductCode

                val gasPoints = chosenProp.optJSONArray("gas_meter_points") ?: JSONArray()
                if (gasPoints.length() > 0) {
                    val gp = gasPoints.getJSONObject(0)
                    mprn = gp.optString("mprn", mprn)
                    val meters = gp.optJSONArray("meters") ?: JSONArray()
                    if (meters.length() > 0) {
                        gasSerial = meters.getJSONObject(meters.length() - 1).optString("serial_number", gasSerial)
                    }
                    val agreements = gp.optJSONArray("agreements") ?: JSONArray()
                    val latestGasTariff = pickActiveTariffCode(agreements)
                    if (!latestGasTariff.isNullOrBlank()) {
                        gasTariff = latestGasTariff
                        gasProduct = AgileCalculator.extractProductCodeFromTariff(latestGasTariff)
                    }
                }

                val elecStanding = fetchElectricStandingChargePence(agileProduct, agileTariff)
                    ?: config.electricStandingChargePence

                var gasUnitRate = config.gasUnitRatePence
                var gasStanding = config.gasStandingChargePence
                if (gasProduct.isNotBlank() && gasTariff.isNotBlank()) {
                    fetchGasTariffPricing(gasProduct, gasTariff)?.let { (unit, standing) ->
                        gasUnitRate = unit
                        gasStanding = standing
                    }
                }

                val updated = config.copy(
                    apiKey = cleanKey,
                    accountNumber = resolvedAccountNumber,
                    agileProductCode = agileProduct,
                    regionCode = region,
                    agileTariffCode = agileTariff,
                    electricMpan = mpan,
                    electricSerial = elecSerial,
                    electricStandingChargePence = elecStanding,
                    gasMprn = mprn,
                    gasSerial = gasSerial,
                    gasProductCode = gasProduct,
                    gasTariffCode = gasTariff,
                    gasUnitRatePence = gasUnitRate,
                    gasStandingChargePence = gasStanding,
                    addressSummary = addressSummary
                )

                AccountDiscoveryResult(
                    success = true,
                    updatedConfig = updated,
                    message = "Connected to account $resolvedAccountNumber ($addressSummary) · Tariff: $agileTariff"
                )
            }
        } catch (e: Exception) {
            AccountDiscoveryResult(
                success = false,
                message = "Connection error: ${e.localizedMessage ?: e.message ?: "Unknown error"}"
            )
        }
    }

    private fun pickActiveTariffCode(agreements: JSONArray): String? {
        if (agreements.length() == 0) return null
        val nowMs = System.currentTimeMillis()
        var fallback: String? = null
        for (i in 0 until agreements.length()) {
            val ag = agreements.getJSONObject(i)
            val code = ag.optString("tariff_code", "").takeIf { it.isNotBlank() } ?: continue
            fallback = code
            val validFrom = parseIsoToMillis(ag.optString("valid_from", ""))
            val validToRaw = if (ag.isNull("valid_to")) "" else ag.optString("valid_to", "")
            val validTo = if (validToRaw.isBlank()) Long.MAX_VALUE else parseIsoToMillis(validToRaw)
            if (nowMs in validFrom..validTo) {
                return code
            }
        }
        return fallback
    }

    /**
     * Fetches current gas unit rate (p/kWh inc VAT) and standing charge (p/day inc VAT).
     */
    suspend fun fetchGasTariffPricing(
        gasProductCode: String,
        gasTariffCode: String
    ): Pair<Double, Double>? = withContext(Dispatchers.IO) {
        try {
            val base = cleanBaseUrl()
            val unitUrl = "$base/v1/products/$gasProductCode/gas-tariffs/$gasTariffCode/standard-unit-rates/?page_size=5"
            val standingUrl = "$base/v1/products/$gasProductCode/gas-tariffs/$gasTariffCode/standing-charges/?page_size=5"

            var unitRate: Double? = null
            var standingCharge: Double? = null

            client.newCall(Request.Builder().url(unitUrl).get().build()).execute().use { resp ->
                if (resp.isSuccessful) {
                    val results = JSONObject(resp.body?.string() ?: "{}").optJSONArray("results")
                    if (results != null && results.length() > 0) {
                        unitRate = results.getJSONObject(0).optDouble("value_inc_vat")
                    }
                }
            }

            client.newCall(Request.Builder().url(standingUrl).get().build()).execute().use { resp ->
                if (resp.isSuccessful) {
                    val results = JSONObject(resp.body?.string() ?: "{}").optJSONArray("results")
                    if (results != null && results.length() > 0) {
                        standingCharge = results.getJSONObject(0).optDouble("value_inc_vat")
                    }
                }
            }

            if (unitRate != null) {
                return@withContext Pair(unitRate!!, standingCharge ?: 29.0)
            }
        } catch (_: Exception) {
        }
        null
    }

    /**
     * Fetches 30 days of half-hourly electricity consumption from `/v1/electricity-meter-points/{mpan}/meters/{serial}/consumption/`
     * and pairs each half-hour slot with its Agile unit rate.
     */
    suspend fun fetchElectricityConsumption30Days(
        ratesByStartEpoch: Map<Long, Double>
    ): Result<List<HalfHourConsumption>> = withContext(Dispatchers.IO) {
        if (!config.hasElectricMeter) {
            return@withContext Result.failure(Exception("Electricity MPAN/Serial or API key not configured"))
        }
        try {
            val now = Instant.now().truncatedTo(ChronoUnit.MINUTES)
            val from = now.minus(30, ChronoUnit.DAYS)
            val base = cleanBaseUrl()
            val url = "$base/v1/electricity-meter-points/${config.electricMpan}/meters/${config.electricSerial}/consumption/" +
                "?period_from=$from&period_to=$now&page_size=1500&order_by=period"

            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", Credentials.basic(config.apiKey.trim(), ""))
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty consumption response"))
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${response.code} fetching electricity consumption"))
                }
                val json = JSONObject(body)
                val results = json.optJSONArray("results") ?: JSONArray()
                val readings = mutableListOf<HalfHourConsumption>()

                for (i in 0 until results.length()) {
                    val item = results.optJSONObject(i) ?: continue
                    val startStr = item.optString("interval_start", "")
                    val endStr = item.optString("interval_end", "")
                    val startMs = parseIsoToMillis(startStr)
                    val endMs = parseIsoToMillis(endStr)
                    if (startMs <= 0L) continue

                    val rawKwh = item.optDouble("consumption", 0.0)
                    // Guard against anomalous error spikes (> 500 kWh in 30 mins)
                    val kwh = if (rawKwh < 0.0 || rawKwh > 500.0) 0.0 else rawKwh
                    val rate = ratesByStartEpoch[startMs] ?: 0.0
                    readings.add(
                        HalfHourConsumption(
                            intervalStartUtc = startStr,
                            intervalEndUtc = endStr,
                            startEpochMillis = startMs,
                            endEpochMillis = endMs,
                            consumption = kwh,
                            energyKwh = kwh,
                            unitRatePence = rate,
                            costPence = kwh * rate
                        )
                    )
                }

                Result.success(readings.sortedBy { it.startEpochMillis })
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetches 30 days of half-hourly gas consumption from `/v1/gas-meter-points/{mprn}/meters/{serial}/consumption/`.
     */
    suspend fun fetchGasConsumption30Days(): Result<List<HalfHourConsumption>> = withContext(Dispatchers.IO) {
        if (!config.hasGasMeter) {
            return@withContext Result.failure(Exception("Gas MPRN/Serial or API key not configured"))
        }
        try {
            val now = Instant.now().truncatedTo(ChronoUnit.MINUTES)
            val from = now.minus(30, ChronoUnit.DAYS)
            val base = cleanBaseUrl()
            val url = "$base/v1/gas-meter-points/${config.gasMprn}/meters/${config.gasSerial}/consumption/" +
                "?period_from=$from&period_to=$now&page_size=1500&order_by=period"

            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", Credentials.basic(config.apiKey.trim(), ""))
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty gas consumption response"))
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${response.code} fetching gas consumption"))
                }
                val json = JSONObject(body)
                val results = json.optJSONArray("results") ?: JSONArray()
                val readings = mutableListOf<HalfHourConsumption>()

                for (i in 0 until results.length()) {
                    val item = results.optJSONObject(i) ?: continue
                    val startStr = item.optString("interval_start", "")
                    val endStr = item.optString("interval_end", "")
                    val startMs = parseIsoToMillis(startStr)
                    val endMs = parseIsoToMillis(endStr)
                    if (startMs <= 0L) continue

                    val rawVal = item.optDouble("consumption", 0.0)
                    // Guard against Octopus API error spikes (Agile Watcher v10.2 release note)
                    val cleanVal = if (rawVal < 0.0 || rawVal > 500.0) 0.0 else rawVal
                    val kwh = if (config.gasIsCubicMeters) {
                        AgileCalculator.gasCubicMetersToKwh(cleanVal)
                    } else {
                        cleanVal
                    }
                    val rate = config.gasUnitRatePence
                    readings.add(
                        HalfHourConsumption(
                            intervalStartUtc = startStr,
                            intervalEndUtc = endStr,
                            startEpochMillis = startMs,
                            endEpochMillis = endMs,
                            consumption = cleanVal,
                            energyKwh = kwh,
                            unitRatePence = rate,
                            costPence = kwh * rate
                        )
                    )
                }

                Result.success(readings.sortedBy { it.startEpochMillis })
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
