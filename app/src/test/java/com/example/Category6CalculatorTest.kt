package com.example

import com.example.fiqih.Category6Calculator
import com.example.fiqih.FiqihCalculatorEngine
import com.example.model.*
import org.junit.Assert.*
import org.junit.Test

class Category6CalculatorTest {

    /**
     * CONTOH REFERENSI WAJIB dari Tuhfatun Niswah & Fiqih Mazhab Syafi'i:
     * - monthLength = 30
     * - habitDuration = 5
     * - knownPositionWindowStart = 1
     * - knownPositionWindowEnd = 10
     * - knownCertainPureDays = [1]
     *
     * Hasil yang WAJIB dihasilkan:
     * - Hari 1: SUCI_YAKIN (ihtiyatAction = NONE)
     * - Hari 2–5: SYAK_HAID_Suci (ihtiyatAction = WUDHU_SETIAP_FARDHU)
     * - Hari 6: HAID_YAKIN (ihtiyatAction = NONE)
     * - Hari 7–10: SYAK_HAID_Suci_PUTUS (ihtiyatAction = GHUSL_SETIAP_FARDHU)
     * - Hari 11–30: SUCI_YAKIN (ihtiyatAction = NONE)
     */
    @Test
    fun testCategory6_CompulsoryReferenceExample() {
        val result = Category6Calculator.calculateCategory6(
            monthLength = 30,
            habitDurationDays = 5,
            positionWindowStart = 1,
            positionWindowEnd = 10,
            certainPureDays = setOf(1),
            certainHaidDays = emptySet()
        )

        assertTrue("Hasil harus valid", result.isValid)
        assertNull("Tidak boleh ada error message", result.errorMessage)
        assertEquals("Total interval kandidat awal = 6", 6, result.generatedCandidatesCount)
        assertEquals("Kandidat tereliminasi (1..5) = 1", 1, result.eliminatedPureCandidates.size)
        assertEquals(1..5, result.eliminatedPureCandidates.first())

        assertEquals("Kandidat valid yang tersisa = 5", 5, result.validCandidates.size)
        assertEquals(listOf(2..6, 3..7, 4..8, 5..9, 6..10), result.validCandidates)

        val periods = result.periodResults
        assertEquals("Harus terbagi menjadi 5 periode fase hukum", 5, periods.size)

        // Periode 1: Hari 1 -> SUCI_YAKIN, NONE
        val p1 = periods[0]
        assertEquals(1, p1.startDay)
        assertEquals(1, p1.endDay)
        assertEquals(FiqhPeriodStatus.SUCI_YAKIN, p1.status)
        assertEquals(IhtiyatAction.NONE, p1.ihtiyatAction)

        // Periode 2: Hari 2–5 -> SYAK_HAID_Suci, WUDHU_SETIAP_FARDHU
        val p2 = periods[1]
        assertEquals(2, p2.startDay)
        assertEquals(5, p2.endDay)
        assertEquals(FiqhPeriodStatus.SYAK_HAID_Suci, p2.status)
        assertEquals(IhtiyatAction.WUDHU_SETIAP_FARDHU, p2.ihtiyatAction)

        // Periode 3: Hari 6 -> HAID_YAKIN, NONE
        val p3 = periods[2]
        assertEquals(6, p3.startDay)
        assertEquals(6, p3.endDay)
        assertEquals(FiqhPeriodStatus.HAID_YAKIN, p3.status)
        assertEquals(IhtiyatAction.NONE, p3.ihtiyatAction)

        // Periode 4: Hari 7–10 -> SYAK_HAID_Suci_PUTUS, GHUSL_SETIAP_FARDHU
        val p4 = periods[3]
        assertEquals(7, p4.startDay)
        assertEquals(10, p4.endDay)
        assertEquals(FiqhPeriodStatus.SYAK_HAID_Suci_PUTUS, p4.status)
        assertEquals(IhtiyatAction.GHUSL_SETIAP_FARDHU, p4.ihtiyatAction)

        // Periode 5: Hari 11–30 -> SUCI_YAKIN, NONE
        val p5 = periods[4]
        assertEquals(11, p5.startDay)
        assertEquals(30, p5.endDay)
        assertEquals(FiqhPeriodStatus.SUCI_YAKIN, p5.status)
        assertEquals(IhtiyatAction.NONE, p5.ihtiyatAction)
    }

    @Test
    fun testCategory6_InputValidation() {
        // habitDurationDays < 1
        val res1 = Category6Calculator.calculateCategory6(
            monthLength = 30, habitDurationDays = 0, positionWindowStart = 1, positionWindowEnd = 10
        )
        assertFalse(res1.isValid)
        assertNotNull(res1.errorMessage)

        // habitDurationDays > monthLength
        val res2 = Category6Calculator.calculateCategory6(
            monthLength = 30, habitDurationDays = 31, positionWindowStart = 1, positionWindowEnd = 30
        )
        assertFalse(res2.isValid)

        // positionWindowStart < 1
        val res3 = Category6Calculator.calculateCategory6(
            monthLength = 30, habitDurationDays = 5, positionWindowStart = 0, positionWindowEnd = 10
        )
        assertFalse(res3.isValid)

        // positionWindowEnd > monthLength
        val res4 = Category6Calculator.calculateCategory6(
            monthLength = 30, habitDurationDays = 5, positionWindowStart = 1, positionWindowEnd = 31
        )
        assertFalse(res4.isValid)

        // positionWindowStart > positionWindowEnd
        val res5 = Category6Calculator.calculateCategory6(
            monthLength = 30, habitDurationDays = 5, positionWindowStart = 10, positionWindowEnd = 5
        )
        assertFalse(res5.isValid)

        // habitDurationDays > windowLength
        val res6 = Category6Calculator.calculateCategory6(
            monthLength = 30, habitDurationDays = 6, positionWindowStart = 1, positionWindowEnd = 5
        )
        assertFalse(res6.isValid)
    }

    @Test
    fun testCategory6_CertainHaidDaysFilter() {
        // Window 1..10, duration 5, pure days [1], certain haid days = [4]
        // Candidates before haid filter: 2..6, 3..7, 4..8, 5..9, 6..10
        // Candidates containing 4: 2..6, 3..7, 4..8 (3 candidates)
        // 5..9 and 6..10 do not contain 4, so they are eliminated.
        val res = Category6Calculator.calculateCategory6(
            monthLength = 30,
            habitDurationDays = 5,
            positionWindowStart = 1,
            positionWindowEnd = 10,
            certainPureDays = setOf(1),
            certainHaidDays = setOf(4)
        )

        assertTrue(res.isValid)
        assertEquals(3, res.validCandidates.size)
        assertEquals(listOf(2..6, 3..7, 4..8), res.validCandidates)
        assertEquals(listOf(5..9, 6..10), res.eliminatedHaidCandidates)
    }

    @Test
    fun testCategory6_ContradictionHandling() {
        // Window 1..5, duration 5 -> only candidate is 1..5
        // User sets certainPureDays = [3]
        // 1..5 overlaps with 3, leaving 0 valid candidates.
        val res = Category6Calculator.calculateCategory6(
            monthLength = 30,
            habitDurationDays = 5,
            positionWindowStart = 1,
            positionWindowEnd = 5,
            certainPureDays = setOf(3)
        )

        assertFalse("Harus invalid karena kontradiksi", res.isValid)
        assertNotNull(res.errorMessage)
        assertTrue(res.validCandidates.isEmpty())
    }

    @Test
    fun testCategory6_Case6a_Siklus30_Adat5_Rentang1to10_Hari1Suci() {
        // 6a. Siklus 30, adat 5, rentang 1-10, hari 1 yakin suci
        val res = Category6Calculator.calculateCategory6(
            monthLength = 30,
            habitDurationDays = 5,
            positionWindowStart = 1,
            positionWindowEnd = 10,
            certainPureDays = setOf(1)
        )
        assertTrue(res.isValid)
        val p = res.periodResults
        assertEquals(5, p.size)
        // Hari 1 suci yakin
        assertEquals(1, p[0].startDay); assertEquals(1, p[0].endDay); assertEquals(FiqhPeriodStatus.SUCI_YAKIN, p[0].status)
        // Hari 2-5 syak wudhu
        assertEquals(2, p[1].startDay); assertEquals(5, p[1].endDay); assertEquals(FiqhPeriodStatus.SYAK_HAID_Suci, p[1].status); assertEquals(IhtiyatAction.WUDHU_SETIAP_FARDHU, p[1].ihtiyatAction)
        // Hari 6 haid yakin
        assertEquals(6, p[2].startDay); assertEquals(6, p[2].endDay); assertEquals(FiqhPeriodStatus.HAID_YAKIN, p[2].status)
        // Hari 7-10 syak mandi
        assertEquals(7, p[3].startDay); assertEquals(10, p[3].endDay); assertEquals(FiqhPeriodStatus.SYAK_HAID_Suci_PUTUS, p[3].status); assertEquals(IhtiyatAction.GHUSL_SETIAP_FARDHU, p[3].ihtiyatAction)
        // Hari 11-30 suci yakin
        assertEquals(11, p[4].startDay); assertEquals(30, p[4].endDay); assertEquals(FiqhPeriodStatus.SUCI_YAKIN, p[4].status)
    }

    @Test
    fun testCategory6_Case6b_Siklus30_Adat15_Rentang1to20_TanpaHariSuci() {
        // 6b. Siklus 30, adat 15, rentang 1-20, tanpa hari suci
        val res = Category6Calculator.calculateCategory6(
            monthLength = 30,
            habitDurationDays = 15,
            positionWindowStart = 1,
            positionWindowEnd = 20,
            certainPureDays = emptySet()
        )
        assertTrue(res.isValid)
        val p = res.periodResults
        assertEquals(4, p.size)
        // Hari 1-5 wudhu
        assertEquals(1, p[0].startDay); assertEquals(5, p[0].endDay); assertEquals(FiqhPeriodStatus.SYAK_HAID_Suci, p[0].status); assertEquals(IhtiyatAction.WUDHU_SETIAP_FARDHU, p[0].ihtiyatAction)
        // Hari 6-15 haid yakin
        assertEquals(6, p[1].startDay); assertEquals(15, p[1].endDay); assertEquals(FiqhPeriodStatus.HAID_YAKIN, p[1].status)
        // Hari 16-20 mandi
        assertEquals(16, p[2].startDay); assertEquals(20, p[2].endDay); assertEquals(FiqhPeriodStatus.SYAK_HAID_Suci_PUTUS, p[2].status); assertEquals(IhtiyatAction.GHUSL_SETIAP_FARDHU, p[2].ihtiyatAction)
        // Hari 21-30 suci yakin
        assertEquals(21, p[3].startDay); assertEquals(30, p[3].endDay); assertEquals(FiqhPeriodStatus.SUCI_YAKIN, p[3].status)
    }

    @Test
    fun testCategory6_Case6c_Siklus30_Adat5_Rentang1to30_TanpaHariSuci() {
        // 6c. Siklus 30, adat 5, rentang 1-30, tanpa hari suci
        val res = Category6Calculator.calculateCategory6(
            monthLength = 30,
            habitDurationDays = 5,
            positionWindowStart = 1,
            positionWindowEnd = 30,
            certainPureDays = emptySet()
        )
        assertTrue(res.isValid)
        val p = res.periodResults
        assertEquals(2, p.size)
        // Hari 1-5 wudhu
        assertEquals(1, p[0].startDay); assertEquals(5, p[0].endDay); assertEquals(FiqhPeriodStatus.SYAK_HAID_Suci, p[0].status); assertEquals(IhtiyatAction.WUDHU_SETIAP_FARDHU, p[0].ihtiyatAction)
        // Hari 6-30 mandi
        assertEquals(6, p[1].startDay); assertEquals(30, p[1].endDay); assertEquals(FiqhPeriodStatus.SYAK_HAID_Suci_PUTUS, p[1].status); assertEquals(IhtiyatAction.GHUSL_SETIAP_FARDHU, p[1].ihtiyatAction)
        // Tidak ada haid yakin
        assertFalse(p.any { it.status == FiqhPeriodStatus.HAID_YAKIN })
    }

    @Test
    fun testCategory6_Case6d_Siklus30_Adat10_Rentang1to30() {
        // 6d. Siklus 30, adat 10, rentang 1-30
        val res = Category6Calculator.calculateCategory6(
            monthLength = 30,
            habitDurationDays = 10,
            positionWindowStart = 1,
            positionWindowEnd = 30,
            certainPureDays = emptySet()
        )
        assertTrue(res.isValid)
        val p = res.periodResults
        assertEquals(2, p.size)
        // Hari 1-10 wudhu
        assertEquals(1, p[0].startDay); assertEquals(10, p[0].endDay); assertEquals(FiqhPeriodStatus.SYAK_HAID_Suci, p[0].status); assertEquals(IhtiyatAction.WUDHU_SETIAP_FARDHU, p[0].ihtiyatAction)
        // Hari 11-30 mandi
        assertEquals(11, p[1].startDay); assertEquals(30, p[1].endDay); assertEquals(FiqhPeriodStatus.SYAK_HAID_Suci_PUTUS, p[1].status); assertEquals(IhtiyatAction.GHUSL_SETIAP_FARDHU, p[1].ihtiyatAction)
        // Tidak ada haid yakin
        assertFalse(p.any { it.status == FiqhPeriodStatus.HAID_YAKIN })
    }

    @Test
    fun testCategory6_Case6e_AdatOver15_Rejected() {
        // 6e. Validasi: Adat haid > 15 hari ditolak (misal adat 16 atau 20 hari)
        val res = Category6Calculator.calculateCategory6(
            monthLength = 30,
            habitDurationDays = 16,
            positionWindowStart = 1,
            positionWindowEnd = 30,
            certainPureDays = emptySet()
        )
        assertFalse("Adat 16 hari harus ditolak (isValid = false)", res.isValid)
        assertNotNull(res.errorMessage)
        assertTrue(res.errorMessage!!.contains("15 hari"))

        // Pengujian integrasi engine: adat 20 hari tidak boleh di-coerce diam-diam menjadi 15
        val now = 1700000000000L
        val eighteenDaysMs = 18 * 24 * 3600_000L
        val start = now - eighteenDaysMs
        val result = FiqihCalculatorEngine.calculate(
            caseType = CaseType.HAID,
            startEpochMillis = start,
            endEpochMillis = now,
            hasPreviousAdat = true,
            adatDurationDays = 20, // Input tidak valid
            adatCycleDays = 30,
            adatMemoryType = AdatMemoryType.INGAT_QADRAN_LUPA_WAQTAN,
            haidCategory = HaidCategory.MUTADAH_DZAKIRAH_QADRAN_DUNAN_WAQT,
            bloodColor = BloodColor.MERAH,
            isThick = true,
            isOdorous = true,
            intervals = listOf(BleedingInterval(startEpochMillis = start, endEpochMillis = now, bloodColor = BloodColor.MERAH)),
            prayerAtStart = null,
            hadPrayedAtStart = true,
            prayerAtStop = null,
            category6WindowStart = 1,
            category6WindowEnd = 30,
            category6PureDays = null,
            category6HaidDays = null,
            category6MonthLength = 30
        )
        assertNotNull(result.category6Calculation)
        assertFalse("Engine harus menolak adat 20 hari", result.category6Calculation!!.isValid)
        assertTrue(result.statusSummary.contains("Kesalahan Input"))
    }

    @Test
    fun testCategory6_Case6f_NoDefaultHaidYakinWhenNoPositionInput() {
        // 6f. Tanpa input rentang dan hari suci sama sekali (semua parameter posisi menggunakan default / null)
        val now = 1700000000000L
        val eighteenDaysMs = 18 * 24 * 3600_000L
        val start = now - eighteenDaysMs

        val result = FiqihCalculatorEngine.calculate(
            caseType = CaseType.HAID,
            startEpochMillis = start,
            endEpochMillis = now,
            hasPreviousAdat = true,
            adatDurationDays = 5,
            adatCycleDays = 30,
            adatMemoryType = AdatMemoryType.INGAT_QADRAN_LUPA_WAQTAN,
            haidCategory = HaidCategory.MUTADAH_DZAKIRAH_QADRAN_DUNAN_WAQT,
            bloodColor = BloodColor.MERAH,
            isThick = true,
            isOdorous = true,
            intervals = listOf(BleedingInterval(startEpochMillis = start, endEpochMillis = now, bloodColor = BloodColor.MERAH)),
            prayerAtStart = null,
            hadPrayedAtStart = true,
            prayerAtStop = null,
            rememberedCertainPureDayOfMonth = null,
            rememberedCertainHaidDayOfMonth = null,
            category6WindowStart = 1,
            category6WindowEnd = null, // Default null -> otomatis mengikuti monthLength (30)
            category6PureDays = null,  // Fallback menjadi emptySet(), bukan setOf(1)
            category6HaidDays = null,
            category6MonthLength = 30
        )
        assertNotNull(result.category6Calculation)
        val cat6 = result.category6Calculation!!
        assertTrue(cat6.isValid)
        assertEquals(30, cat6.positionWindowEnd)
        // Pastikan TIDAK ADA haid yakin yang muncul secara palsu dari fallback
        assertFalse("Tidak boleh ada HAID_YAKIN ketika tanpa rentang posisi dan tanpa hari suci", cat6.periodResults.any { it.status == FiqhPeriodStatus.HAID_YAKIN })
        assertEquals("Harus ada 2 fase hukum (Syak Wudhu 1-5 dan Syak Mandi 6-30)", 2, cat6.periodResults.size)
        assertEquals(1, cat6.periodResults[0].startDay)
        assertEquals(5, cat6.periodResults[0].endDay)
        assertEquals(FiqhPeriodStatus.SYAK_HAID_Suci, cat6.periodResults[0].status)
        assertEquals(6, cat6.periodResults[1].startDay)
        assertEquals(30, cat6.periodResults[1].endDay)
        assertEquals(FiqhPeriodStatus.SYAK_HAID_Suci_PUTUS, cat6.periodResults[1].status)
    }

    @Test
    fun testCategory6_Case6g_31DayMonth_WindowCovers31Days() {
        // 6g. Bulan 31 hari: category6WindowEnd null harus mencakup hingga hari ke-31
        val now = 1700000000000L
        val eighteenDaysMs = 18 * 24 * 3600_000L
        val start = now - eighteenDaysMs

        val result = FiqihCalculatorEngine.calculate(
            caseType = CaseType.HAID,
            startEpochMillis = start,
            endEpochMillis = now,
            hasPreviousAdat = true,
            adatDurationDays = 5,
            adatCycleDays = 31,
            adatMemoryType = AdatMemoryType.INGAT_QADRAN_LUPA_WAQTAN,
            haidCategory = HaidCategory.MUTADAH_DZAKIRAH_QADRAN_DUNAN_WAQT,
            bloodColor = BloodColor.MERAH,
            isThick = true,
            isOdorous = true,
            intervals = listOf(BleedingInterval(startEpochMillis = start, endEpochMillis = now, bloodColor = BloodColor.MERAH)),
            prayerAtStart = null,
            hadPrayedAtStart = true,
            prayerAtStop = null,
            category6WindowStart = 1,
            category6WindowEnd = null,
            category6PureDays = null,
            category6HaidDays = null,
            category6MonthLength = 31
        )
        assertNotNull(result.category6Calculation)
        val cat6 = result.category6Calculation!!
        assertTrue(cat6.isValid)
        assertEquals(31, cat6.monthLength)
        assertEquals(31, cat6.positionWindowEnd)
        assertEquals(31, cat6.periodResults.last().endDay)
    }
}
