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

    @Test
    fun `test calendar prediction for active bleeding predicts stop date`() {
        val todayEpoch = com.example.model.CalendarDateHelper.getTodayEpochDay()
        val userProfile = com.example.data.entities.UserAdatProfileEntity(
            usualHaidDays = 7,
            usualSuciDays = 23
        )

        // User logged blood for the past 3 days (including today)
        val logs = listOf(
            com.example.data.entities.DailyBloodLogEntity(
                dateString = "2026-09-11",
                epochDay = todayEpoch - 2,
                hasBlood = true,
                bloodColor = "HITAM",
                startTime = "08:00",
                stopTime = "20:00"
            ),
            com.example.data.entities.DailyBloodLogEntity(
                dateString = "2026-09-12",
                epochDay = todayEpoch - 1,
                hasBlood = true,
                bloodColor = "MERAH",
                startTime = "08:00",
                stopTime = "20:00"
            ),
            com.example.data.entities.DailyBloodLogEntity(
                dateString = "2026-09-13",
                epochDay = todayEpoch,
                hasBlood = true,
                bloodColor = "MERAH",
                startTime = "08:00",
                stopTime = "20:00"
            )
        )

        val prediction = com.example.fiqih.CalendarPredictionEngine.computeCyclePrediction(logs, userProfile)
        assertTrue(prediction.summary.isActiveHaid)
        assertEquals(3, prediction.summary.activeHaidDaysSoFar)
        assertNotNull(prediction.summary.predictedStopDateText)
        assertEquals(4, prediction.summary.daysUntilPredictedStop)
        assertNotNull(prediction.summary.predictedNextHaidStartText)
    }

    @Test
    fun `test calendar month grid generation`() {
        val userProfile = com.example.data.entities.UserAdatProfileEntity(
            usualHaidDays = 7,
            usualSuciDays = 23
        )
        val days = com.example.fiqih.CalendarPredictionEngine.generateDaysForMonth(
            year = 2026,
            month = 8, // September (0-indexed)
            logs = emptyList(),
            userProfile = userProfile
        )

        // Calendar grid must always be a multiple of 7 (full weeks)
        assertEquals(0, days.size % 7)
        assertTrue(days.size >= 28)
        // September has 30 days
        val currentMonthDays = days.filter { it.isCurrentMonth }
        assertEquals(30, currentMonthDays.size)
    }

    @Test
    fun `test cycle notification scheduler preferences and scheduling logic`() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        com.example.reminder.CycleNotificationScheduler.initNotificationChannel(context)

        // Test default enabled and days before
        assertTrue(com.example.reminder.CycleNotificationScheduler.isNotificationEnabled(context))
        assertEquals(2, com.example.reminder.CycleNotificationScheduler.getDaysBeforeAlert(context))

        // Update settings
        com.example.reminder.CycleNotificationScheduler.setDaysBeforeAlert(context, 3)
        assertEquals(3, com.example.reminder.CycleNotificationScheduler.getDaysBeforeAlert(context))

        // Schedule alert with future epoch day
        val futureEpochDay = (System.currentTimeMillis() / (24 * 3600 * 1000L)) + 10
        com.example.reminder.CycleNotificationScheduler.scheduleNextCycleAlert(
            context,
            futureEpochDay,
            "10 Hari Lagi"
        )

        // Cancel alert
        com.example.reminder.CycleNotificationScheduler.cancelScheduledAlarm(context)
    }

    @Test
    fun `test theme palette and mode defaults and parsing`() {
        val defaultPalette = com.example.ui.theme.ThemePalette.fromId("EMERALD")
        assertEquals(com.example.ui.theme.ThemePalette.EMERALD, defaultPalette)

        val rosePalette = com.example.ui.theme.ThemePalette.fromId("ROSE")
        assertEquals(com.example.ui.theme.ThemePalette.ROSE, rosePalette)

        val lavenderPalette = com.example.ui.theme.ThemePalette.fromId("LAVENDER")
        assertEquals(com.example.ui.theme.ThemePalette.LAVENDER, lavenderPalette)

        val oceanPalette = com.example.ui.theme.ThemePalette.fromId("OCEAN")
        assertEquals(com.example.ui.theme.ThemePalette.OCEAN, oceanPalette)

        val olivePalette = com.example.ui.theme.ThemePalette.fromId("OLIVE")
        assertEquals(com.example.ui.theme.ThemePalette.OLIVE, olivePalette)

        val dynamicPalette = com.example.ui.theme.ThemePalette.fromId("DYNAMIC")
        assertEquals(com.example.ui.theme.ThemePalette.DYNAMIC, dynamicPalette)

        val fallbackPalette = com.example.ui.theme.ThemePalette.fromId("UNKNOWN_THEME")
        assertEquals(com.example.ui.theme.ThemePalette.EMERALD, fallbackPalette)

        val systemMode = com.example.ui.theme.ThemeMode.fromId("SYSTEM")
        assertEquals(com.example.ui.theme.ThemeMode.SYSTEM, systemMode)

        val darkMode = com.example.ui.theme.ThemeMode.fromId("DARK")
        assertEquals(com.example.ui.theme.ThemeMode.DARK, darkMode)

        val lightMode = com.example.ui.theme.ThemeMode.fromId("LIGHT")
        assertEquals(com.example.ui.theme.ThemeMode.LIGHT, lightMode)
    }

    @Test
    fun `test category 6 calculation result through engine`() {
        val start = 1700000000000L
        val end = start + (25 * 24 * 3600_000L) // 25 days, exceeds 15 days

        val result = FiqihCalculatorEngine.calculate(
            caseType = CaseType.HAID,
            startEpochMillis = start,
            endEpochMillis = end,
            hasPreviousAdat = true,
            adatDurationDays = 5,
            adatCycleDays = 28,
            adatMemoryType = AdatMemoryType.INGAT_QADRAN_LUPA_WAQTAN,
            bloodColor = BloodColor.MERAH,
            isThick = false,
            isOdorous = false,
            intervals = emptyList(),
            prayerAtStart = null,
            hadPrayedAtStart = true,
            prayerAtStop = null,
            category6WindowStart = 1,
            category6WindowEnd = 10,
            category6PureDays = setOf(1),
            category6HaidDays = emptySet(),
            category6MonthLength = 30
        )

        assertNotNull(result.category6Calculation)
        val cat6 = result.category6Calculation!!
        assertTrue(cat6.isValid)
        assertEquals(5, cat6.habitDurationDays)
        assertEquals(5, cat6.periodResults.size)
        assertEquals(com.example.model.FiqhPeriodStatus.SUCI_YAKIN, cat6.periodResults[0].status)
        assertEquals(com.example.model.FiqhPeriodStatus.SYAK_HAID_Suci, cat6.periodResults[1].status)
        assertEquals(com.example.model.FiqhPeriodStatus.HAID_YAKIN, cat6.periodResults[2].status)
        assertEquals(com.example.model.FiqhPeriodStatus.SYAK_HAID_Suci_PUTUS, cat6.periodResults[3].status)
        assertEquals(com.example.model.FiqhPeriodStatus.SUCI_YAKIN, cat6.periodResults[4].status)
    }

    @Test
    fun `test setAdatMemoryType sets hasPreviousAdat true when choosing INGAT_QADRAN_LUPA_WAQTAN after BELUM_PERNAH_HAID`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val viewModel = com.example.ui.viewmodel.FiqihViewModel(app)

        viewModel.setAdatMemoryType(AdatMemoryType.BELUM_PERNAH_HAID)
        assertEquals(false, viewModel.hasPreviousAdat.value)

        viewModel.setAdatMemoryType(AdatMemoryType.INGAT_QADRAN_LUPA_WAQTAN)
        assertEquals(true, viewModel.hasPreviousAdat.value)
    }
}
