package com.example.agilewatcher

import com.example.agilewatcher.data.AgileCalculator
import com.example.agilewatcher.data.AgileSlot
import com.example.agilewatcher.data.findGspRegion
import org.junit.Assert.*
import org.junit.Test

class AgileCalculatorTest {

    @Test
    fun tariffCodeExtractionAndBuild_isCorrect() {
        val tariff = "E-1R-AGILE-24-10-01-C"
        assertEquals("AGILE-24-10-01", AgileCalculator.extractProductCodeFromTariff(tariff))
        assertEquals("C", AgileCalculator.extractRegionFromTariff(tariff))
        assertEquals("E-1R-AGILE-24-10-01-M", AgileCalculator.buildAgileTariffCode("AGILE-24-10-01", "_M"))
    }

    @Test
    fun findBestWindow_findsLowestConsecutiveSlots() {
        val baseMs = 1761350400000L
        val halfHourMs = 30L * 60L * 1000L
        val rates = listOf(14.0, 12.0, 9.0, -2.0, 4.0, 15.0)
        val slots = rates.mapIndexed { idx, rate ->
            AgileSlot(
                validFromUtc = "2026-10-25T00:00:00Z",
                validToUtc = "2026-10-25T00:30:00Z",
                startEpochMillis = baseMs + idx * halfHourMs,
                endEpochMillis = baseMs + (idx + 1) * halfHourMs,
                valueExcVat = rate / 1.05,
                valueIncVat = rate
            )
        }

        val best1h = AgileCalculator.findBestWindow(slots, durationHours = 1, minStartEpochMillis = baseMs)
        assertNotNull(best1h)
        // Slots at index 3 (-2.0) and index 4 (4.0) have average 1.0p/kWh
        assertEquals(1.0, best1h!!.averagePencePerKwh, 0.001)
        assertEquals(baseMs + 3 * halfHourMs, best1h.startEpochMillis)
    }

    @Test
    fun gasCubicMetersToKwh_convertsAccurately() {
        val kwh = AgileCalculator.gasCubicMetersToKwh(1.0, 39.5)
        assertEquals(11.2206, kwh, 0.01)
    }

    @Test
    fun regionCConversion_matchesOctopusFormula() {
        val regionP = findGspRegion("P")
        val incVatPeak = AgileCalculator.convertRegionCPriceToRegion(32.0, isPeak4To7Pm = true, targetRegion = regionP)
        assertEquals(37.8, incVatPeak, 0.01)
    }
}
