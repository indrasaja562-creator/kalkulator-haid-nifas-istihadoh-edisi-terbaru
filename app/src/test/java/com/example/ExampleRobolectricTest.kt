package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.fiqih.FiqihCalculatorEngine
import com.example.model.AdatMemoryType
import com.example.model.BloodColor
import com.example.model.CaseType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Kalkulator Haid", appName)
    }

    @Test
    fun `test normal 7 day haid calculation`() {
        val start = 1700000000000L
        val end = start + (7 * 24 * 3600_000L) // 7 days

        val result = FiqihCalculatorEngine.calculate(
            caseType = CaseType.HAID,
            startEpochMillis = start,
            endEpochMillis = end,
            hasPreviousAdat = true,
            adatDurationDays = 7,
            adatCycleDays = 28,
            adatMemoryType = AdatMemoryType.INGAT_LENGKAP_QADRAN_WAQTAN,
            bloodColor = BloodColor.MERAH,
            isThick = true,
            isOdorous = true,
            intervals = emptyList(),
            prayerAtStart = null,
            hadPrayedAtStart = true,
            prayerAtStop = null
        )

        assertNotNull(result.haidOrNifasSegment)
        assertEquals(7L, result.haidOrNifasSegment?.durationDays)
        assertTrue(result.statusSummary.contains("Haid Sah"))
        assertTrue(result.references.isNotEmpty())
    }

    @Test
    fun `test blood less than 24 hours is istihadhah fasid`() {
        val start = 1700000000000L
        val end = start + (10 * 3600_000L) // 10 hours

        val result = FiqihCalculatorEngine.calculate(
            caseType = CaseType.HAID,
            startEpochMillis = start,
            endEpochMillis = end,
            hasPreviousAdat = true,
            adatDurationDays = 7,
            adatCycleDays = 28,
            adatMemoryType = AdatMemoryType.INGAT_LENGKAP_QADRAN_WAQTAN,
            bloodColor = BloodColor.MERAH,
            isThick = true,
            isOdorous = true,
            intervals = emptyList(),
            prayerAtStart = null,
            hadPrayedAtStart = true,
            prayerAtStop = null
        )

        assertTrue(result.statusSummary.contains("Darah Rusak") || result.statusSummary.contains("Bukan Haid"))
        assertNotNull(result.istihadhahSegment)
    }

    @Test
    fun `test blood over 15 days split with adat`() {
        val start = 1700000000000L
        val end = start + (20 * 24 * 3600_000L) // 20 days

        val result = FiqihCalculatorEngine.calculate(
            caseType = CaseType.HAID,
            startEpochMillis = start,
            endEpochMillis = end,
            hasPreviousAdat = true,
            adatDurationDays = 7,
            adatCycleDays = 28,
            adatMemoryType = AdatMemoryType.INGAT_LENGKAP_QADRAN_WAQTAN,
            bloodColor = BloodColor.MERAH,
            isThick = false,
            isOdorous = false,
            intervals = emptyList(),
            prayerAtStart = null,
            hadPrayedAtStart = true,
            prayerAtStop = null
        )

        assertNotNull(result.haidOrNifasSegment)
        assertNotNull(result.istihadhahSegment)
        assertEquals(7L, result.haidOrNifasSegment?.durationDays)
        assertEquals(13L, result.istihadhahSegment?.durationDays)
    }
}
