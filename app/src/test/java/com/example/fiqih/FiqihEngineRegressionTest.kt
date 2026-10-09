package com.example.fiqih

import com.example.model.*
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class FiqihEngineRegressionTest {

    private val oneHour = 3600_000L
    private val oneDay = 24 * oneHour

    @Test
    fun testNormalHaidSevenDays() {
        val start = 1_700_000_000_000L
        val end = start + (7 * oneDay)

        val result = FiqihCalculatorEngine.calculate(
            caseType = CaseType.HAID,
            startEpochMillis = start,
            endEpochMillis = end,
            hasPreviousAdat = false,
            adatDurationDays = 7,
            adatCycleDays = 28,
            adatMemoryType = AdatMemoryType.BELUM_PERNAH_HAID,
            bloodColor = BloodColor.MERAH,
            isThick = true,
            isOdorous = true,
            intervals = listOf(
                BleedingInterval(
                    startEpochMillis = start,
                    endEpochMillis = end,
                    bloodColor = BloodColor.MERAH,
                    isThick = true,
                    isOdorous = true
                )
            ),
            prayerAtStart = null,
            hadPrayedAtStart = false,
            prayerAtStop = null
        )

        assertNotNull(result)
        assertTrue(result.statusSummary.contains("Haid", ignoreCase = true))
        assertNotNull(result.haidOrNifasSegment)
        assertEquals(7L, result.haidOrNifasSegment?.durationDays)
    }

    @Test
    fun testMubtadiahMumayyizahHaidTamyiz() {
        // Fase 1: Darah Hitam (kuat) 5 hari
        // Fase 2: Darah Merah (lemah) 12 hari (Total 17 hari > 15 hari)
        val start = 1_700_000_000_000L
        val mid = start + (5 * oneDay)
        val end = start + (17 * oneDay)

        val result = FiqihCalculatorEngine.calculate(
            caseType = CaseType.HAID,
            startEpochMillis = start,
            endEpochMillis = end,
            hasPreviousAdat = false,
            adatDurationDays = 7,
            adatCycleDays = 28,
            adatMemoryType = AdatMemoryType.BELUM_PERNAH_HAID,
            haidCategory = HaidCategory.MUBTADIAH_MUMAYYIZAH,
            bloodColor = BloodColor.HITAM,
            isThick = true,
            isOdorous = true,
            intervals = listOf(
                BleedingInterval(startEpochMillis = start, endEpochMillis = mid, bloodColor = BloodColor.HITAM),
                BleedingInterval(startEpochMillis = mid, endEpochMillis = end, bloodColor = BloodColor.MERAH)
            ),
            prayerAtStart = null,
            hadPrayedAtStart = false,
            prayerAtStop = null
        )

        assertNotNull(result)
        assertTrue(result.caseCategory.contains("Mubtadi'ah Mumayyizah", ignoreCase = true))
        assertEquals(5L, result.haidOrNifasSegment?.durationDays)
        assertEquals(12L, result.istihadhahSegment?.durationDays)
    }

    @Test
    fun testMubtadiahGhairuMumayyizahHaidSingleColorOver15Days() {
        // Darah Merah 18 hari (> 15 hari tanpa tamyiz)
        val start = 1_700_000_000_000L
        val end = start + (18 * oneDay)

        val result = FiqihCalculatorEngine.calculate(
            caseType = CaseType.HAID,
            startEpochMillis = start,
            endEpochMillis = end,
            hasPreviousAdat = false,
            adatDurationDays = 7,
            adatCycleDays = 28,
            adatMemoryType = AdatMemoryType.BELUM_PERNAH_HAID,
            haidCategory = HaidCategory.MUBTADIAH_GHAIRU_MUMAYYIZAH,
            bloodColor = BloodColor.MERAH,
            isThick = true,
            isOdorous = true,
            intervals = listOf(
                BleedingInterval(startEpochMillis = start, endEpochMillis = end, bloodColor = BloodColor.MERAH)
            ),
            prayerAtStart = null,
            hadPrayedAtStart = false,
            prayerAtStop = null
        )

        assertNotNull(result)
        assertTrue(result.caseCategory.contains("Mubtadi'ah Ghairu Mumayyizah", ignoreCase = true))
        // Haid = 1 hari (24 jam), Istihadhah = sisa (17 hari)
        assertEquals(1L, result.haidOrNifasSegment?.durationDays)
        assertEquals(17L, result.istihadhahSegment?.durationDays)
    }

    @Test
    fun testMutadahGhairuMumayyizahDzakirahReturnsToAdat() {
        // Adat 7 hari, darah keluar 18 hari seragam
        val start = 1_700_000_000_000L
        val end = start + (18 * oneDay)

        val result = FiqihCalculatorEngine.calculate(
            caseType = CaseType.HAID,
            startEpochMillis = start,
            endEpochMillis = end,
            hasPreviousAdat = true,
            adatDurationDays = 7,
            adatCycleDays = 28,
            adatMemoryType = AdatMemoryType.INGAT_LENGKAP_QADRAN_WAQTAN,
            haidCategory = HaidCategory.MUTADAH_GHAIRU_MUMAYYIZAH_DZAKIRAH_QADRAN_WAQTAN,
            bloodColor = BloodColor.MERAH,
            isThick = true,
            isOdorous = true,
            intervals = listOf(
                BleedingInterval(startEpochMillis = start, endEpochMillis = end, bloodColor = BloodColor.MERAH)
            ),
            prayerAtStart = null,
            hadPrayedAtStart = false,
            prayerAtStop = null
        )

        assertNotNull(result)
        assertTrue(result.caseCategory.contains("Mu'tadah Ghairu Mumayyizah", ignoreCase = true))
        assertEquals(7L, result.haidOrNifasSegment?.durationDays)
        assertEquals(11L, result.istihadhahSegment?.durationDays)
    }

    @Test
    fun testNifasTuhfatunNiswahSixtyFiveDaysMumayyizah() {
        // Persalinan, darah 65 hari (> 60 hari): Hitam 40 hari, Kuning 25 hari
        val start = 1_700_000_000_000L
        val mid = start + (40 * oneDay)
        val end = start + (65 * oneDay)

        val result = FiqihCalculatorEngine.calculate(
            caseType = CaseType.NIFAS,
            startEpochMillis = start,
            endEpochMillis = end,
            hasPreviousAdat = false,
            adatDurationDays = 40,
            adatCycleDays = 28,
            adatMemoryType = AdatMemoryType.BELUM_PERNAH_HAID,
            nifasAdatCategory = NifasAdatCategory.MUBTADIAH_MUMAYYIZAH,
            deliveryEpochMillis = start,
            deliveryType = DeliveryType.TUNGGAL_NORMAL_SESAR,
            bloodColor = BloodColor.HITAM,
            isThick = true,
            isOdorous = true,
            intervals = listOf(
                BleedingInterval(startEpochMillis = start, endEpochMillis = mid, bloodColor = BloodColor.HITAM),
                BleedingInterval(startEpochMillis = mid, endEpochMillis = end, bloodColor = BloodColor.KUNING)
            ),
            prayerAtStart = null,
            hadPrayedAtStart = false,
            prayerAtStop = null
        )

        assertNotNull(result)
        assertTrue(result.caseCategory.contains("Nifas", ignoreCase = true))
        assertTrue(result.caseCategory.contains("Mumayyizah", ignoreCase = true))
        assertEquals(40L, result.haidOrNifasSegment?.durationDays)
        assertEquals(25L, result.istihadhahSegment?.durationDays)
    }

    @Test
    fun testNifasIntermittentPauseGreaterThan15DaysEndsNifas() {
        // Nifas darah 20 hari, jeda suci 16 hari (>= 15 hari memutus nifas secara sah), darah kedua 7 hari
        val start = 1_700_000_000_000L
        val endPhase1 = start + (20 * oneDay)
        val startPhase2 = endPhase1 + (16 * oneDay)
        val endPhase2 = startPhase2 + (7 * oneDay)

        val result = FiqihCalculatorEngine.calculate(
            caseType = CaseType.NIFAS,
            startEpochMillis = start,
            endEpochMillis = endPhase2,
            hasPreviousAdat = false,
            adatDurationDays = 40,
            adatCycleDays = 28,
            adatMemoryType = AdatMemoryType.BELUM_PERNAH_HAID,
            nifasAdatCategory = NifasAdatCategory.MUBTADIAH_GHAIRU_MUMAYYIZAH,
            deliveryEpochMillis = start,
            deliveryType = DeliveryType.TUNGGAL_NORMAL_SESAR,
            hasIntermittentPause = true,
            intermittentPauseDays = 16.0,
            bloodColor = BloodColor.MERAH,
            isThick = true,
            isOdorous = true,
            intervals = listOf(
                BleedingInterval(startEpochMillis = start, endEpochMillis = endPhase1, bloodColor = BloodColor.MERAH),
                BleedingInterval(startEpochMillis = startPhase2, endEpochMillis = endPhase2, bloodColor = BloodColor.MERAH)
            ),
            prayerAtStart = null,
            hadPrayedAtStart = false,
            prayerAtStop = null
        )

        assertNotNull(result)
        assertTrue(result.statusSummary.contains("Haid", ignoreCase = true) || result.categoryDetectionReason.contains("15 hari", ignoreCase = true))
    }

    @Test
    fun testTakmilatanLitTuhriUserCase_10Hitam_8Suci_12Merah() {
        // Kasus Audit: 
        // Darah hitam: 1–10 September 2026 (10 hari)
        // Jeda suci: 11–18 September 2026 (8 hari)
        // Darah merah: 19–30 September 2026 (12 hari)
        // Adat haid: 7 hari, Ingat lengkap, Takmilatan lit-Tuhri aktif, Suci sebelum 1 Sep >= 15 hari.
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.SEPTEMBER)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startSept1 = cal.timeInMillis
        val endSept10 = startSept1 + (10 * oneDay) // 11 Sep 00:00
        val startSept19 = endSept10 + (8 * oneDay) // 19 Sep 00:00 (jeda 8 hari bersih: 11 s/d 18 Sep)
        val endSept30 = startSept19 + (12 * oneDay) // 1 Okt 00:00 (12 hari darah: 19 s/d 30 Sep)

        val result = FiqihCalculatorEngine.calculate(
            caseType = CaseType.HAID,
            startEpochMillis = startSept1,
            endEpochMillis = endSept30,
            hasPreviousAdat = true,
            adatDurationDays = 7,
            adatCycleDays = 28,
            adatMemoryType = AdatMemoryType.INGAT_LENGKAP_QADRAN_WAQTAN,
            bloodColor = BloodColor.HITAM,
            isThick = true,
            isOdorous = true,
            isTakmilahEnabled = true,
            previousSuciDaysForTakmilah = 15, // Suci sebelum 1 Sep sempurna >= 15 hari
            hasIntermittentPause = true,
            intermittentPauseDays = 8.0,
            intervals = listOf(
                BleedingInterval(
                    startEpochMillis = startSept1,
                    endEpochMillis = endSept10,
                    bloodColor = BloodColor.HITAM,
                    isThick = true,
                    isOdorous = true
                ),
                BleedingInterval(
                    startEpochMillis = startSept19,
                    endEpochMillis = endSept30,
                    bloodColor = BloodColor.MERAH,
                    isThick = false,
                    isOdorous = false
                )
            ),
            prayerAtStart = null,
            hadPrayedAtStart = false,
            prayerAtStop = null
        )

        assertNotNull(result)
        assertTrue("Kategori harus Takmilatan lit-Tuhri", result.caseCategory.contains("Takmilatan lit-Tuhri", ignoreCase = true))
        
        val segments = result.allSegments
        assertEquals("Harus ada 4 fase berurutan", 4, segments.size)

        // 1. Masa Haid Pertama: 1–10 Sep (10 hari)
        val seg0 = segments[0]
        assertEquals("Masa Haid Pertama", seg0.title)
        assertEquals("Haid Sah", seg0.status)
        assertEquals(startSept1, seg0.startEpochMillis)
        assertEquals(endSept10, seg0.endEpochMillis)
        assertEquals("01 Sep 2026 – 10 Sep 2026", FiqihCalculatorEngine.formatDisplayRange(seg0.startEpochMillis, seg0.endEpochMillis))

        // 2. Masa Suci Pemisah (Naqa'): 11–18 Sep (8 hari)
        val seg1 = segments[1]
        assertEquals("Masa Suci Pemisah (Naqa')", seg1.title)
        assertTrue(seg1.status.contains("Suci"))
        assertEquals(endSept10, seg1.startEpochMillis)
        assertEquals(startSept19, seg1.endEpochMillis)
        assertEquals("11 Sep 2026 – 18 Sep 2026", FiqihCalculatorEngine.formatDisplayRange(seg1.startEpochMillis, seg1.endEpochMillis))

        // 3. Takmilatan lit-Tuhri: 19–25 Sep (7 hari penyempurna suci)
        val takmilahEndExpected = startSept19 + (7 * oneDay)
        val seg2 = segments[2]
        assertEquals("Takmilatan lit-Tuhri", seg2.title)
        assertTrue(seg2.status.contains("Istihadhah"))
        assertEquals(startSept19, seg2.startEpochMillis)
        assertEquals(takmilahEndExpected, seg2.endEpochMillis)
        assertEquals("19 Sep 2026 – 25 Sep 2026", FiqihCalculatorEngine.formatDisplayRange(seg2.startEpochMillis, seg2.endEpochMillis))

        // 4. Haid Kedua (Haid Baru): 26–30 Sep (5 hari sisa darah)
        val seg3 = segments[3]
        assertEquals("Haid Kedua (Haid Baru)", seg3.title)
        assertEquals("Haid Sah", seg3.status)
        assertEquals(takmilahEndExpected, seg3.startEpochMillis)
        assertEquals(endSept30, seg3.endEpochMillis)
        assertEquals("26 Sep 2026 – 30 Sep 2026", FiqihCalculatorEngine.formatDisplayRange(seg3.startEpochMillis, seg3.endEpochMillis))

        // Verifikasi tidak ada tumpang tindih
        assertEquals(seg0.endEpochMillis, seg1.startEpochMillis)
        assertEquals(seg1.endEpochMillis, seg2.startEpochMillis)
        assertEquals(seg2.endEpochMillis, seg3.startEpochMillis)

        // Verifikasi rujukan kitab tersedia
        assertTrue("Rujukan Tuhfatun Niswah harus ada", result.references.any { it.kitabName.contains("Tuhfatun Niswah") })
        assertTrue("Rujukan Uyunul Masa'il harus ada", result.references.any { it.kitabName.contains("Uyunul Masa-il") })
    }

    @Test
    fun testTakmilatanLitTuhriPreCycleDeficit_SevenDaysSuci() {
        // Kasus: Darah keluar terus menerus dari awal siklus, tapi masa suci sebelumnya hanya 7 hari
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.SEPTEMBER)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startSept1 = cal.timeInMillis
        val endSept20 = startSept1 + (20 * oneDay) // 20 hari

        val result = FiqihCalculatorEngine.calculate(
            caseType = CaseType.HAID,
            startEpochMillis = startSept1,
            endEpochMillis = endSept20,
            hasPreviousAdat = true,
            adatDurationDays = 7,
            adatCycleDays = 28,
            adatMemoryType = AdatMemoryType.INGAT_LENGKAP_QADRAN_WAQTAN,
            bloodColor = BloodColor.HITAM,
            isThick = true,
            isOdorous = true,
            isTakmilahEnabled = true,
            previousSuciDaysForTakmilah = 7, // Suci sebelumnya hanya 7 hari (kurang 8 hari)
            previousHaidContinuousForTakmilah = true,
            previousHaidDurationDaysForTakmilah = 7,
            intervals = listOf(
                BleedingInterval(
                    startEpochMillis = startSept1,
                    endEpochMillis = endSept20,
                    bloodColor = BloodColor.HITAM
                )
            ),
            prayerAtStart = null,
            hadPrayedAtStart = false,
            prayerAtStop = null
        )

        assertNotNull(result)
        val segments = result.allSegments
        assertTrue(segments.isNotEmpty())

        // 8 hari pertama: Takmilatan lit-Tuhri (15 - 7 = 8 hari) -> 1–8 Sep 2026
        val seg0 = segments[0]
        assertEquals("Takmilatan lit-Tuhri", seg0.title)
        assertTrue(seg0.status.contains("Istihadhah"))
        assertEquals("01 Sep 2026 – 08 Sep 2026", FiqihCalculatorEngine.formatDisplayRange(seg0.startEpochMillis, seg0.endEpochMillis))

        // Segmen kedua: Mulai tanggal 09 Sep, BUKAN tanggal 08 Sep!
        if (segments.size > 1) {
            val seg1 = segments[1]
            assertEquals(seg0.endEpochMillis, seg1.startEpochMillis)
            val rangeStr = FiqihCalculatorEngine.formatDisplayRange(seg1.startEpochMillis, seg1.endEpochMillis)
            assertTrue("Segmen berikutnya harus mulai tanggal 09, bukan 08: $rangeStr", rangeStr.startsWith("09 Sep") || rangeStr.startsWith("9"))
        }
    }

    @Test
    fun testTakmilatanLitTuhriNotAppliedWhenPreviousSuciIs15DaysAndContinuous() {
        // Jika masa suci sebelum darah sudah mencapai 15 hari dan darah keluar terus-menerus tanpa jeda,
        // maka syarat defisit suci Takmilah TIDAK terpenuhi.
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.SEPTEMBER)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startSept1 = cal.timeInMillis
        val endSept20 = startSept1 + (20 * oneDay)

        val result = FiqihCalculatorEngine.calculate(
            caseType = CaseType.HAID,
            startEpochMillis = startSept1,
            endEpochMillis = endSept20,
            hasPreviousAdat = true,
            adatDurationDays = 7,
            adatCycleDays = 28,
            adatMemoryType = AdatMemoryType.INGAT_LENGKAP_QADRAN_WAQTAN,
            bloodColor = BloodColor.HITAM,
            isThick = true,
            isOdorous = true,
            isTakmilahEnabled = true,
            previousSuciDaysForTakmilah = 15, // Suci sebelumnya sempurna 15 hari
            previousHaidContinuousForTakmilah = true,
            previousHaidDurationDaysForTakmilah = 7,
            intervals = listOf(
                BleedingInterval(
                    startEpochMillis = startSept1,
                    endEpochMillis = endSept20,
                    bloodColor = BloodColor.HITAM
                )
            ),
            prayerAtStart = null,
            hadPrayedAtStart = false,
            prayerAtStop = null
        )

        assertNotNull(result)
        // Tidak boleh dihukumi Takmilatan lit-Tuhri karena suci sebelumnya sudah 15 hari
        assertFalse(
            "Tidak boleh Takmilatan lit-Tuhri jika suci sebelumnya >= 15 hari tanpa jeda",
            result.caseCategory.contains("Takmilatan lit-Tuhri", ignoreCase = true)
        )
        // Harus kembali ke kategori haid / istihadhah adat (Mu'tadah Ghairu Mumayyizah)
        assertEquals(7L, result.haidOrNifasSegment?.durationDays)
        assertEquals(13L, result.istihadhahSegment?.durationDays)
    }

    @Test
    fun testCategory3_Case3a() {
        // 3a adat 3, lemah 15 (setelah adat), kuat 3: total 21 hari (jarak tepat 15 hari: haid 1-3, istihadhah 4-18, haid 19-21)
        val start = 1700000000000L
        val weakEnd = start + 18 * oneDay
        val strongEnd = weakEnd + 3 * oneDay
        val intervals = listOf(
            BleedingInterval(startEpochMillis = start, endEpochMillis = weakEnd, bloodColor = BloodColor.MERAH, isThick = false, isOdorous = false),
            BleedingInterval(startEpochMillis = weakEnd, endEpochMillis = strongEnd, bloodColor = BloodColor.HITAM, isThick = true, isOdorous = true)
        )
        val result = FiqihCalculatorEngine.calculate(
            caseType = CaseType.HAID,
            startEpochMillis = start,
            endEpochMillis = strongEnd,
            hasPreviousAdat = true,
            adatDurationDays = 3,
            adatCycleDays = 30,
            adatMemoryType = AdatMemoryType.INGAT_LENGKAP_QADRAN_WAQTAN,
            haidCategory = HaidCategory.MUTADAH_GHAIRU_MUMAYYIZAH_DZAKIRAH_QADRAN_WAQTAN, // Selain MUTADAH_MUMAYYIZAH
            bloodColor = BloodColor.HITAM,
            isThick = true,
            isOdorous = true,
            intervals = intervals,
            prayerAtStart = null,
            hadPrayedAtStart = true,
            prayerAtStop = null
        )
        val res = result.periodResults
        val haidSegments = res.filter { it.status == FiqihStatus.HAID }
        assertEquals(2, haidSegments.size)
        // Haid awal (adat 1-3)
        assertEquals(start, haidSegments[0].startEpochMillis)
        assertEquals(start + 3 * oneDay, haidSegments[0].endEpochMillis)
        // Haid akhir (darah kuat 19-21)
        assertEquals(weakEnd, haidSegments[1].startEpochMillis)
        assertEquals(strongEnd, haidSegments[1].endEpochMillis)
    }

    @Test
    fun testCategory3_Case3b() {
        // 3b adat 3, lemah 14 (total darah lemah sampai hari ke-17), kuat 3 (jarak 14): istihadhah 1-17, haid 18-20
        val start = 1700000000000L
        val weakEnd = start + 17 * oneDay
        val strongEnd = weakEnd + 3 * oneDay
        val intervals = listOf(
            BleedingInterval(startEpochMillis = start, endEpochMillis = weakEnd, bloodColor = BloodColor.MERAH, isThick = false, isOdorous = false),
            BleedingInterval(startEpochMillis = weakEnd, endEpochMillis = strongEnd, bloodColor = BloodColor.HITAM, isThick = true, isOdorous = true)
        )
        val result = FiqihCalculatorEngine.calculate(
            caseType = CaseType.HAID,
            startEpochMillis = start,
            endEpochMillis = strongEnd,
            hasPreviousAdat = true,
            adatDurationDays = 3,
            adatCycleDays = 30,
            adatMemoryType = AdatMemoryType.INGAT_LENGKAP_QADRAN_WAQTAN,
            haidCategory = HaidCategory.MUTADAH_GHAIRU_MUMAYYIZAH_DZAKIRAH_QADRAN_WAQTAN, // Selain MUTADAH_MUMAYYIZAH
            bloodColor = BloodColor.HITAM,
            isThick = true,
            isOdorous = true,
            intervals = intervals,
            prayerAtStart = null,
            hadPrayedAtStart = true,
            prayerAtStop = null
        )
        val res = result.periodResults
        val haidSegments = res.filter { it.status == FiqihStatus.HAID }
        assertEquals(1, haidSegments.size)
        // Haid hanya pada darah kuat (18-20)
        assertEquals(weakEnd, haidSegments[0].startEpochMillis)
        assertEquals(strongEnd, haidSegments[0].endEpochMillis)
    }

    @Test
    fun testCategory3_Case3c() {
        // 3c adat 8, lemah 24 (total hari ke-24), kuat 6 (jarak 16): haid 1-8, istihadhah 9-24, haid 25-30
        // Cek juga mandi di hari 15 dan 30 dan qadha 7 hari.
        val start = 1700000000000L
        val weakEnd = start + 24 * oneDay
        val strongEnd = weakEnd + 6 * oneDay
        val intervals = listOf(
            BleedingInterval(startEpochMillis = start, endEpochMillis = weakEnd, bloodColor = BloodColor.MERAH, isThick = false, isOdorous = false),
            BleedingInterval(startEpochMillis = weakEnd, endEpochMillis = strongEnd, bloodColor = BloodColor.HITAM, isThick = true, isOdorous = true)
        )
        val result = FiqihCalculatorEngine.calculate(
            caseType = CaseType.HAID,
            startEpochMillis = start,
            endEpochMillis = strongEnd,
            hasPreviousAdat = true,
            adatDurationDays = 8,
            adatCycleDays = 30,
            adatMemoryType = AdatMemoryType.INGAT_LENGKAP_QADRAN_WAQTAN,
            haidCategory = HaidCategory.MUTADAH_GHAIRU_MUMAYYIZAH_DZAKIRAH_QADRAN_WAQTAN, // Selain MUTADAH_MUMAYYIZAH
            bloodColor = BloodColor.HITAM,
            isThick = true,
            isOdorous = true,
            intervals = intervals,
            prayerAtStart = null,
            hadPrayedAtStart = true,
            prayerAtStop = null
        )
        val res = result.periodResults
        val haidSegments = res.filter { it.status == FiqihStatus.HAID }
        assertEquals(2, haidSegments.size)
        // Haid 1-8
        assertEquals(start, haidSegments[0].startEpochMillis)
        assertEquals(start + 8 * oneDay, haidSegments[0].endEpochMillis)
        // Haid 25-30 (berakhir hari ke-30)
        assertEquals(weakEnd, haidSegments[1].startEpochMillis)
        assertEquals(strongEnd, haidSegments[1].endEpochMillis)
        assertEquals(start + 30 * oneDay, haidSegments[1].endEpochMillis) // Mandi akhir haid di hari ke-30

        // Cek Qadha 7 hari (hari ke 9-15)
        val qadhaSegments = res.filter { it.needsQadha }
        val qadhaDays = qadhaSegments.sumOf { it.durationMillis } / oneDay
        assertEquals(7L, qadhaDays)

        // Cek Mandi di hari ke-15 (akhir segmen qadha batas 15 hari istihadhah)
        assertTrue("Harus ada batas evaluasi mandi di hari ke-15", res.any { it.endEpochMillis == start + 15 * oneDay && it.needsGhusl })
    }

    @Test
    fun testCategory3_Case3d() {
        // 3d adat 13, kuat 9 di awal, lemah 21: haid hanya hari 1-9
        val start = 1700000000000L
        val strongEnd = start + 9 * oneDay
        val weakEnd = strongEnd + 21 * oneDay
        val intervals = listOf(
            BleedingInterval(startEpochMillis = start, endEpochMillis = strongEnd, bloodColor = BloodColor.HITAM, isThick = true, isOdorous = true),
            BleedingInterval(startEpochMillis = strongEnd, endEpochMillis = weakEnd, bloodColor = BloodColor.MERAH, isThick = false, isOdorous = false)
        )
        val result = FiqihCalculatorEngine.calculate(
            caseType = CaseType.HAID,
            startEpochMillis = start,
            endEpochMillis = weakEnd,
            hasPreviousAdat = true,
            adatDurationDays = 13,
            adatCycleDays = 30,
            adatMemoryType = AdatMemoryType.INGAT_LENGKAP_QADRAN_WAQTAN,
            haidCategory = HaidCategory.MUTADAH_GHAIRU_MUMAYYIZAH_DZAKIRAH_QADRAN_WAQTAN, // Selain MUTADAH_MUMAYYIZAH
            bloodColor = BloodColor.HITAM,
            isThick = true,
            isOdorous = true,
            intervals = intervals,
            prayerAtStart = null,
            hadPrayedAtStart = true,
            prayerAtStop = null
        )
        val res = result.periodResults
        val haidSegments = res.filter { it.status == FiqihStatus.HAID }
        assertEquals(1, haidSegments.size)
        assertEquals(start, haidSegments[0].startEpochMillis)
        assertEquals(strongEnd, haidSegments[0].endEpochMillis)
        // Lemah 21 hari adalah istihadhah
        val istiSegments = res.filter { it.status == FiqihStatus.ISTIHADHAH }
        assertEquals(strongEnd, istiSegments.first().startEpochMillis)
        assertEquals(weakEnd, istiSegments.last().endEpochMillis)
    }

    @Test
    fun testCategory3_Case3e() {
        // 3e adat 8, lemah 12, kuat 6 (jarak 12): haid hanya hari 21-26 (darah kuat)
        val start = 1700000000000L
        val weakEnd = start + 20 * oneDay
        val strongEnd = weakEnd + 6 * oneDay
        val intervals = listOf(
            BleedingInterval(startEpochMillis = start, endEpochMillis = weakEnd, bloodColor = BloodColor.MERAH, isThick = false, isOdorous = false),
            BleedingInterval(startEpochMillis = weakEnd, endEpochMillis = strongEnd, bloodColor = BloodColor.HITAM, isThick = true, isOdorous = true)
        )
        val result = FiqihCalculatorEngine.calculate(
            caseType = CaseType.HAID,
            startEpochMillis = start,
            endEpochMillis = strongEnd,
            hasPreviousAdat = true,
            adatDurationDays = 8,
            adatCycleDays = 30,
            adatMemoryType = AdatMemoryType.INGAT_LENGKAP_QADRAN_WAQTAN,
            haidCategory = HaidCategory.MUTADAH_GHAIRU_MUMAYYIZAH_DZAKIRAH_QADRAN_WAQTAN, // Selain MUTADAH_MUMAYYIZAH
            bloodColor = BloodColor.HITAM,
            isThick = true,
            isOdorous = true,
            intervals = intervals,
            prayerAtStart = null,
            hadPrayedAtStart = true,
            prayerAtStop = null
        )
        val res = result.periodResults
        val haidSegments = res.filter { it.status == FiqihStatus.HAID }
        assertEquals(1, haidSegments.size)
        assertEquals(weakEnd, haidSegments[0].startEpochMillis)
        assertEquals(strongEnd, haidSegments[0].endEpochMillis)
    }

    @Test
    fun testCategory3_Case3f() {
        // 3f kuat 8, lemah 8, kuat 8: haid hanya 8 hari kuat pertama
        val start = 1700000000000L
        val strong1End = start + 8 * oneDay
        val weakEnd = strong1End + 8 * oneDay
        val strong2End = weakEnd + 8 * oneDay
        val intervals = listOf(
            BleedingInterval(startEpochMillis = start, endEpochMillis = strong1End, bloodColor = BloodColor.HITAM, isThick = true, isOdorous = true),
            BleedingInterval(startEpochMillis = strong1End, endEpochMillis = weakEnd, bloodColor = BloodColor.MERAH, isThick = false, isOdorous = false),
            BleedingInterval(startEpochMillis = weakEnd, endEpochMillis = strong2End, bloodColor = BloodColor.HITAM, isThick = true, isOdorous = true)
        )
        val result = FiqihCalculatorEngine.calculate(
            caseType = CaseType.HAID,
            startEpochMillis = start,
            endEpochMillis = strong2End,
            hasPreviousAdat = true,
            adatDurationDays = 5,
            adatCycleDays = 30,
            adatMemoryType = AdatMemoryType.INGAT_LENGKAP_QADRAN_WAQTAN,
            haidCategory = HaidCategory.MUTADAH_GHAIRU_MUMAYYIZAH_DZAKIRAH_QADRAN_WAQTAN, // Selain MUTADAH_MUMAYYIZAH
            bloodColor = BloodColor.HITAM,
            isThick = true,
            isOdorous = true,
            intervals = intervals,
            prayerAtStart = null,
            hadPrayedAtStart = true,
            prayerAtStop = null
        )
        val res = result.periodResults
        val haidSegments = res.filter { it.status == FiqihStatus.HAID }
        assertEquals(1, haidSegments.size)
        assertEquals(start, haidSegments[0].startEpochMillis)
        assertEquals(strong1End, haidSegments[0].endEpochMillis)
    }

    @Test
    fun testMubtadiahMumayyizahKuat8Lemah8Kuat8() {
        // Mubtadi'ah Mumayyizah dengan pola darah: kuat 8, lemah 8, kuat 8 (total 24 hari).
        // Syarat 3: darah lemah di antara dua darah kuat < 15 hari (hanya 8 hari),
        // sehingga darah kuat kedua dan lemah di antaranya dihukumi istihadhah,
        // tamyiz tetap sah, dan haid hanya 8 hari darah kuat pertama.
        val start = 1700000000000L
        val strong1End = start + 8 * oneDay
        val weakEnd = strong1End + 8 * oneDay
        val strong2End = weakEnd + 8 * oneDay
        val intervals = listOf(
            BleedingInterval(startEpochMillis = start, endEpochMillis = strong1End, bloodColor = BloodColor.HITAM, isThick = true, isOdorous = true),
            BleedingInterval(startEpochMillis = strong1End, endEpochMillis = weakEnd, bloodColor = BloodColor.MERAH, isThick = false, isOdorous = false),
            BleedingInterval(startEpochMillis = weakEnd, endEpochMillis = strong2End, bloodColor = BloodColor.HITAM, isThick = true, isOdorous = true)
        )
        val result = FiqihCalculatorEngine.calculate(
            caseType = CaseType.HAID,
            startEpochMillis = start,
            endEpochMillis = strong2End,
            hasPreviousAdat = false,
            adatDurationDays = 7,
            adatCycleDays = 30,
            adatMemoryType = AdatMemoryType.BELUM_PERNAH_HAID,
            haidCategory = HaidCategory.MUBTADIAH_MUMAYYIZAH,
            bloodColor = BloodColor.HITAM,
            isThick = true,
            isOdorous = true,
            intervals = intervals,
            prayerAtStart = null,
            hadPrayedAtStart = false,
            prayerAtStop = null
        )
        assertNotNull(result)
        assertTrue(result.caseCategory.contains("Mubtadi'ah Mumayyizah", ignoreCase = true))
        val haidSegments = result.periodResults.filter { it.status == FiqihStatus.HAID }
        assertEquals(1, haidSegments.size)
        assertEquals(start, haidSegments[0].startEpochMillis)
        assertEquals(strong1End, haidSegments[0].endEpochMillis)
        assertEquals(8L, result.haidOrNifasSegment?.durationDays)
    }

    @Test
    fun testTamyizMultipleStrongPhasesKuat5Lemah16Kuat5Lemah3Kuat5() {
        // Test: kuat 5, lemah 16, kuat 5, lemah 3, kuat 5 -> haid hanya fase kuat 1 dan 2; fase 3 istihadhah.
        val start = 1700000000000L
        val k1End = start + 5 * oneDay
        val l1End = k1End + 16 * oneDay
        val k2End = l1End + 5 * oneDay
        val l2End = k2End + 3 * oneDay
        val k3End = l2End + 5 * oneDay

        val intervals = listOf(
            BleedingInterval(startEpochMillis = start, endEpochMillis = k1End, bloodColor = BloodColor.HITAM, isThick = true, isOdorous = true),
            BleedingInterval(startEpochMillis = k1End, endEpochMillis = l1End, bloodColor = BloodColor.MERAH, isThick = false, isOdorous = false),
            BleedingInterval(startEpochMillis = l1End, endEpochMillis = k2End, bloodColor = BloodColor.HITAM, isThick = true, isOdorous = true),
            BleedingInterval(startEpochMillis = k2End, endEpochMillis = l2End, bloodColor = BloodColor.MERAH, isThick = false, isOdorous = false),
            BleedingInterval(startEpochMillis = l2End, endEpochMillis = k3End, bloodColor = BloodColor.HITAM, isThick = true, isOdorous = true)
        )

        val result = FiqihCalculatorEngine.calculate(
            caseType = CaseType.HAID,
            startEpochMillis = start,
            endEpochMillis = k3End,
            hasPreviousAdat = false,
            adatDurationDays = 7,
            adatCycleDays = 30,
            adatMemoryType = AdatMemoryType.BELUM_PERNAH_HAID,
            haidCategory = HaidCategory.MUBTADIAH_MUMAYYIZAH,
            bloodColor = BloodColor.HITAM,
            isThick = true,
            isOdorous = true,
            intervals = intervals,
            prayerAtStart = null,
            hadPrayedAtStart = false,
            prayerAtStop = null
        )

        assertNotNull(result)
        val haidSegments = result.periodResults.filter { it.status == FiqihStatus.HAID }
        assertEquals(2, haidSegments.size)
        // Fase 1: start s/d k1End (5 hari)
        assertEquals(start, haidSegments[0].startEpochMillis)
        assertEquals(k1End, haidSegments[0].endEpochMillis)
        // Fase 2: l1End s/d k2End (5 hari)
        assertEquals(l1End, haidSegments[1].startEpochMillis)
        assertEquals(k2End, haidSegments[1].endEpochMillis)

        // Fase 3 (l2End s/d k3End) harus ISTIHADHAH
        val istiSegments = result.periodResults.filter { it.status == FiqihStatus.ISTIHADHAH }
        assertTrue("Fase kuat 3 harus dihukumi istihadhah", istiSegments.any { it.startEpochMillis == l2End && it.endEpochMillis == k3End })
    }

    @Test
    fun testCategory7_Case7a() {
        // 7a tanggal mulai 20, darah 20 Jan - 4 Feb: 20 Jan haid; 21 Jan - 3 Feb ihtiyath; 4 Feb suci
        val calStart = Calendar.getInstance().apply {
            set(2024, Calendar.JANUARY, 20, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val calEnd = Calendar.getInstance().apply {
            set(2024, Calendar.FEBRUARY, 5, 0, 0, 0) // akhir 4 Feb / 5 Feb 00:00
            set(Calendar.MILLISECOND, 0)
        }
        val result = FiqihCalculatorEngine.calculate(
            caseType = CaseType.HAID,
            startEpochMillis = calStart.timeInMillis,
            endEpochMillis = calEnd.timeInMillis,
            hasPreviousAdat = true,
            adatDurationDays = 7,
            adatCycleDays = 30,
            adatMemoryType = AdatMemoryType.INGAT_WAQTAN_LUPA_QADRAN,
            haidCategory = HaidCategory.MUTADAH_DZAKIRAH_WAQTAN_DUNAN_QADR,
            bloodColor = BloodColor.MERAH,
            isThick = true,
            isOdorous = true,
            intervals = listOf(BleedingInterval(startEpochMillis = calStart.timeInMillis, endEpochMillis = calEnd.timeInMillis, bloodColor = BloodColor.MERAH)),
            prayerAtStart = null,
            hadPrayedAtStart = true,
            prayerAtStop = null,
            rememberedCertainHaidDayOfMonth = 20
        )
        val periods = result.periodResults
        // 20 Jan: Haid Yakin
        assertEquals(FiqihStatus.HAID, periods.first().status)
        assertEquals(calStart.timeInMillis, periods.first().startEpochMillis)
        // 21 Jan - 3 Feb (14 hari): Ihtiyath
        val ihtiyathPeriods = periods.filter { it.status == FiqihStatus.IHTIYATH }
        assertEquals(14, ihtiyathPeriods.size)
        // 4 Feb: Suci Yakin / Istihadhah
        assertEquals(FiqihStatus.ISTIHADHAH, periods.last().status)
    }

    @Test
    fun testCategory7_Case7b() {
        // 7b tanggal mulai 25, darah 25 Feb - 15 Mar 2024 (kabisat): 25 Feb haid; 26 Feb - 10 Mar ihtiyath; 11 Mar dst suci
        val calStart = Calendar.getInstance().apply {
            set(2024, Calendar.FEBRUARY, 25, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val calEnd = Calendar.getInstance().apply {
            set(2024, Calendar.MARCH, 16, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val result = FiqihCalculatorEngine.calculate(
            caseType = CaseType.HAID,
            startEpochMillis = calStart.timeInMillis,
            endEpochMillis = calEnd.timeInMillis,
            hasPreviousAdat = true,
            adatDurationDays = 7,
            adatCycleDays = 30,
            adatMemoryType = AdatMemoryType.INGAT_WAQTAN_LUPA_QADRAN,
            haidCategory = HaidCategory.MUTADAH_DZAKIRAH_WAQTAN_DUNAN_QADR,
            bloodColor = BloodColor.MERAH,
            isThick = true,
            isOdorous = true,
            intervals = listOf(BleedingInterval(startEpochMillis = calStart.timeInMillis, endEpochMillis = calEnd.timeInMillis, bloodColor = BloodColor.MERAH)),
            prayerAtStart = null,
            hadPrayedAtStart = true,
            prayerAtStop = null,
            rememberedCertainHaidDayOfMonth = 25
        )
        val periods = result.periodResults
        assertEquals(FiqihStatus.HAID, periods.first().status) // 25 Feb haid
        val ihtiyathPeriods = periods.filter { it.status == FiqihStatus.IHTIYATH }
        assertEquals(14, ihtiyathPeriods.size) // 26 Feb - 10 Mar (14 hari)
        assertEquals(FiqihStatus.ISTIHADHAH, periods.last().status) // 11-15 Mar suci
    }

    @Test
    fun testCategory7_Case7c() {
        // 7c tanggal mulai 1: hari 1 haid; hari 2-15 ihtiyath; hari 16-30 suci (darah 1-30 Apr 2024)
        val calStart = Calendar.getInstance().apply {
            set(2024, Calendar.APRIL, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val calEnd = Calendar.getInstance().apply {
            set(2024, Calendar.MAY, 1, 0, 0, 0) // akhir 30 Apr
            set(Calendar.MILLISECOND, 0)
        }
        val result = FiqihCalculatorEngine.calculate(
            caseType = CaseType.HAID,
            startEpochMillis = calStart.timeInMillis,
            endEpochMillis = calEnd.timeInMillis,
            hasPreviousAdat = true,
            adatDurationDays = 7,
            adatCycleDays = 30,
            adatMemoryType = AdatMemoryType.INGAT_WAQTAN_LUPA_QADRAN,
            haidCategory = HaidCategory.MUTADAH_DZAKIRAH_WAQTAN_DUNAN_QADR,
            bloodColor = BloodColor.MERAH,
            isThick = true,
            isOdorous = true,
            intervals = listOf(BleedingInterval(startEpochMillis = calStart.timeInMillis, endEpochMillis = calEnd.timeInMillis, bloodColor = BloodColor.MERAH)),
            prayerAtStart = null,
            hadPrayedAtStart = true,
            prayerAtStop = null,
            rememberedCertainHaidDayOfMonth = 1
        )
        val periods = result.periodResults
        assertEquals(FiqihStatus.HAID, periods.first().status) // Hari 1 haid
        val ihtiyathPeriods = periods.filter { it.status == FiqihStatus.IHTIYATH }
        assertEquals(14, ihtiyathPeriods.size) // Hari 2-15 (14 hari)
        val suciPeriods = periods.filter { it.status == FiqihStatus.ISTIHADHAH || it.status == FiqihStatus.SUCI }
        assertEquals(15, suciPeriods.size) // Hari 16-30 (15 hari)
    }

    @Test
    fun testCategory7_Case7d() {
        // 7d tanggal mulai 31, darah 31 Mar - 2 Mei: 31 Mar haid; 1-14 Apr ihtiyath; 15-29 Apr suci; 30 Apr haid; 1-2 Mei ihtiyath.
        val calStart = Calendar.getInstance().apply {
            set(2024, Calendar.MARCH, 31, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val calEnd = Calendar.getInstance().apply {
            set(2024, Calendar.MAY, 3, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val result = FiqihCalculatorEngine.calculate(
            caseType = CaseType.HAID,
            startEpochMillis = calStart.timeInMillis,
            endEpochMillis = calEnd.timeInMillis,
            hasPreviousAdat = true,
            adatDurationDays = 7,
            adatCycleDays = 30,
            adatMemoryType = AdatMemoryType.INGAT_WAQTAN_LUPA_QADRAN,
            haidCategory = HaidCategory.MUTADAH_DZAKIRAH_WAQTAN_DUNAN_QADR,
            bloodColor = BloodColor.MERAH,
            isThick = true,
            isOdorous = true,
            intervals = listOf(BleedingInterval(startEpochMillis = calStart.timeInMillis, endEpochMillis = calEnd.timeInMillis, bloodColor = BloodColor.MERAH)),
            prayerAtStart = null,
            hadPrayedAtStart = true,
            prayerAtStop = null,
            rememberedCertainHaidDayOfMonth = 31
        )
        val periods = result.periodResults
        assertFalse("Hasil tidak boleh kosong", periods.isEmpty())
        assertEquals(FiqihStatus.HAID, periods.first().status) // 31 Mar haid
        val haidDays = periods.filter { it.status == FiqihStatus.HAID }
        assertEquals(2, haidDays.size) // 31 Mar dan 30 Apr
    }

    // --- Aturan Tiga Tingkat Tuhfatun Niswah hal. 32 (Mubtadi'ah, hasPreviousAdat = false) ---

    @Test
    fun testTigaTingkat_Hitam7_Merah6_Coklat17() {
        // Tuhfatun Niswah hal. 32: hitam 7, merah 6, coklat 17 -> haid 13 hari (hitam + merah), istihadhah 17 hari
        val start = 1700000000000L
        val t1 = start + 7 * oneDay
        val t2 = t1 + 6 * oneDay
        val t3 = t2 + 17 * oneDay
        val intervals = listOf(
            BleedingInterval(startEpochMillis = start, endEpochMillis = t1, bloodColor = BloodColor.HITAM, isThick = true, isOdorous = true),
            BleedingInterval(startEpochMillis = t1, endEpochMillis = t2, bloodColor = BloodColor.MERAH, isThick = true, isOdorous = true),
            BleedingInterval(startEpochMillis = t2, endEpochMillis = t3, bloodColor = BloodColor.COKLAT, isThick = true, isOdorous = true)
        )
        val result = FiqihCalculatorEngine.calculate(
            caseType = CaseType.HAID,
            startEpochMillis = start,
            endEpochMillis = t3,
            hasPreviousAdat = false,
            adatDurationDays = 7,
            adatCycleDays = 30,
            adatMemoryType = AdatMemoryType.BELUM_PERNAH_HAID,
            haidCategory = HaidCategory.MUBTADIAH_MUMAYYIZAH,
            bloodColor = BloodColor.HITAM,
            isThick = true,
            isOdorous = true,
            intervals = intervals,
            prayerAtStart = null,
            hadPrayedAtStart = false,
            prayerAtStop = null
        )
        assertNotNull(result)
        val haidSegments = result.periodResults.filter { it.status == FiqihStatus.HAID }
        val totalHaidMs = haidSegments.sumOf { it.durationMillis }
        assertEquals(13 * oneDay, totalHaidMs)
        val totalIstiMs = result.periodResults.filter { it.status == FiqihStatus.ISTIHADHAH }.sumOf { it.durationMillis }
        assertEquals(17 * oneDay, totalIstiMs)
    }

    @Test
    fun testTigaTingkat_Merah6_Hitam3_Kuning20() {
        // Tuhfatun Niswah hal. 32: merah 6, hitam 3, kuning 20 -> haid hanya hitam (3 hari)
        val start = 1700000000000L
        val t1 = start + 6 * oneDay
        val t2 = t1 + 3 * oneDay
        val t3 = t2 + 20 * oneDay
        val intervals = listOf(
            BleedingInterval(startEpochMillis = start, endEpochMillis = t1, bloodColor = BloodColor.MERAH, isThick = true, isOdorous = true),
            BleedingInterval(startEpochMillis = t1, endEpochMillis = t2, bloodColor = BloodColor.HITAM, isThick = true, isOdorous = true),
            BleedingInterval(startEpochMillis = t2, endEpochMillis = t3, bloodColor = BloodColor.KUNING, isThick = true, isOdorous = true)
        )
        val result = FiqihCalculatorEngine.calculate(
            caseType = CaseType.HAID,
            startEpochMillis = start,
            endEpochMillis = t3,
            hasPreviousAdat = false,
            adatDurationDays = 7,
            adatCycleDays = 30,
            adatMemoryType = AdatMemoryType.BELUM_PERNAH_HAID,
            haidCategory = HaidCategory.MUBTADIAH_MUMAYYIZAH,
            bloodColor = BloodColor.HITAM,
            isThick = true,
            isOdorous = true,
            intervals = intervals,
            prayerAtStart = null,
            hadPrayedAtStart = false,
            prayerAtStop = null
        )
        assertNotNull(result)
        val haidSegments = result.periodResults.filter { it.status == FiqihStatus.HAID }
        val totalHaidMs = haidSegments.sumOf { it.durationMillis }
        assertEquals(3 * oneDay, totalHaidMs)
        assertEquals(t1, haidSegments.first().startEpochMillis)
        assertEquals(t2, haidSegments.first().endEpochMillis)
    }

    @Test
    fun testTigaTingkat_Hitam6_Coklat7_Merah8() {
        // Tuhfatun Niswah hal. 32: hitam 6, coklat 7, merah 8 -> haid hanya hitam (6 hari)
        val start = 1700000000000L
        val t1 = start + 6 * oneDay
        val t2 = t1 + 7 * oneDay
        val t3 = t2 + 8 * oneDay
        val intervals = listOf(
            BleedingInterval(startEpochMillis = start, endEpochMillis = t1, bloodColor = BloodColor.HITAM, isThick = true, isOdorous = true),
            BleedingInterval(startEpochMillis = t1, endEpochMillis = t2, bloodColor = BloodColor.COKLAT, isThick = true, isOdorous = true),
            BleedingInterval(startEpochMillis = t2, endEpochMillis = t3, bloodColor = BloodColor.MERAH, isThick = true, isOdorous = true)
        )
        val result = FiqihCalculatorEngine.calculate(
            caseType = CaseType.HAID,
            startEpochMillis = start,
            endEpochMillis = t3,
            hasPreviousAdat = false,
            adatDurationDays = 7,
            adatCycleDays = 30,
            adatMemoryType = AdatMemoryType.BELUM_PERNAH_HAID,
            haidCategory = HaidCategory.MUBTADIAH_MUMAYYIZAH,
            bloodColor = BloodColor.HITAM,
            isThick = true,
            isOdorous = true,
            intervals = intervals,
            prayerAtStart = null,
            hadPrayedAtStart = false,
            prayerAtStop = null
        )
        assertNotNull(result)
        val haidSegments = result.periodResults.filter { it.status == FiqihStatus.HAID }
        val totalHaidMs = haidSegments.sumOf { it.durationMillis }
        assertEquals(6 * oneDay, totalHaidMs)
        assertEquals(start, haidSegments.first().startEpochMillis)
        assertEquals(t1, haidSegments.first().endEpochMillis)
    }

    @Test
    fun testTigaTingkat_Hitam7_Merah9_Kuning13() {
        // Tuhfatun Niswah hal. 32: hitam 7, merah 9, kuning 13 (total hitam + merah 16 hari, lebih dari 15) -> haid hanya hitam (7 hari)
        val start = 1700000000000L
        val t1 = start + 7 * oneDay
        val t2 = t1 + 9 * oneDay
        val t3 = t2 + 13 * oneDay
        val intervals = listOf(
            BleedingInterval(startEpochMillis = start, endEpochMillis = t1, bloodColor = BloodColor.HITAM, isThick = true, isOdorous = true),
            BleedingInterval(startEpochMillis = t1, endEpochMillis = t2, bloodColor = BloodColor.MERAH, isThick = true, isOdorous = true),
            BleedingInterval(startEpochMillis = t2, endEpochMillis = t3, bloodColor = BloodColor.KUNING, isThick = true, isOdorous = true)
        )
        val result = FiqihCalculatorEngine.calculate(
            caseType = CaseType.HAID,
            startEpochMillis = start,
            endEpochMillis = t3,
            hasPreviousAdat = false,
            adatDurationDays = 7,
            adatCycleDays = 30,
            adatMemoryType = AdatMemoryType.BELUM_PERNAH_HAID,
            haidCategory = HaidCategory.MUBTADIAH_MUMAYYIZAH,
            bloodColor = BloodColor.HITAM,
            isThick = true,
            isOdorous = true,
            intervals = intervals,
            prayerAtStart = null,
            hadPrayedAtStart = false,
            prayerAtStop = null
        )
        assertNotNull(result)
        val haidSegments = result.periodResults.filter { it.status == FiqihStatus.HAID }
        val totalHaidMs = haidSegments.sumOf { it.durationMillis }
        assertEquals(7 * oneDay, totalHaidMs)
        assertEquals(start, haidSegments.first().startEpochMillis)
        assertEquals(t1, haidSegments.first().endEpochMillis)
    }

    @Test
    fun testCategory7_NullRememberedCertainHaidDayOfMonth() {
        val start = 1700000000000L
        val end = start + 20 * oneDay
        val result = FiqihCalculatorEngine.calculate(
            caseType = CaseType.HAID,
            startEpochMillis = start,
            endEpochMillis = end,
            hasPreviousAdat = true,
            adatDurationDays = 7,
            adatCycleDays = 30,
            adatMemoryType = AdatMemoryType.INGAT_WAQTAN_LUPA_QADRAN,
            haidCategory = HaidCategory.MUTADAH_DZAKIRAH_WAQTAN_DUNAN_QADR,
            bloodColor = BloodColor.MERAH,
            isThick = true,
            isOdorous = true,
            intervals = listOf(
                BleedingInterval(
                    startEpochMillis = start,
                    endEpochMillis = end,
                    bloodColor = BloodColor.MERAH,
                    isThick = true,
                    isOdorous = true
                )
            ),
            prayerAtStart = null,
            hadPrayedAtStart = true,
            prayerAtStop = null,
            rememberedCertainHaidDayOfMonth = null
        )
        assertNotNull(result)
        assertEquals("Tanggal mulai haid belum dipilih", result.statusSummary)
        assertTrue(result.periodResults.isEmpty())
        assertNull(result.haidOrNifasSegment)
        assertNull(result.istihadhahSegment)
    }

    @Test
    fun testTamyizMultiTier_A_Kuat8Lemah7Kuat8() {
        // a. kuat 8, lemah 7, kuat 8 -> haid 15 hari, kuat kedua istihadhah
        val start = 1700000000000L
        val t1 = start + 8 * oneDay
        val t2 = t1 + 7 * oneDay
        val t3 = t2 + 8 * oneDay
        val intervals = listOf(
            BleedingInterval(startEpochMillis = start, endEpochMillis = t1, bloodColor = BloodColor.HITAM, isThick = false, isOdorous = false),
            BleedingInterval(startEpochMillis = t1, endEpochMillis = t2, bloodColor = BloodColor.MERAH, isThick = false, isOdorous = false),
            BleedingInterval(startEpochMillis = t2, endEpochMillis = t3, bloodColor = BloodColor.HITAM, isThick = false, isOdorous = false)
        )
        val result = FiqihCalculatorEngine.calculate(
            caseType = CaseType.HAID,
            startEpochMillis = start,
            endEpochMillis = t3,
            hasPreviousAdat = false,
            adatDurationDays = 7,
            adatCycleDays = 30,
            adatMemoryType = AdatMemoryType.BELUM_PERNAH_HAID,
            haidCategory = HaidCategory.MUBTADIAH_MUMAYYIZAH,
            bloodColor = BloodColor.HITAM,
            isThick = false,
            isOdorous = false,
            intervals = intervals,
            prayerAtStart = null,
            hadPrayedAtStart = false,
            prayerAtStop = null
        )
        assertNotNull(result)
        val haidResults = result.periodResults.filter { it.status == FiqihStatus.HAID }
        val haidDuration = haidResults.sumOf { it.endEpochMillis - it.startEpochMillis }
        assertEquals(15 * oneDay, haidDuration)
        val istiResults = result.periodResults.filter { it.status == FiqihStatus.ISTIHADHAH }
        val istiDuration = istiResults.sumOf { it.endEpochMillis - it.startEpochMillis }
        assertEquals(8 * oneDay, istiDuration)
    }

    @Test
    fun testTamyizMultiTier_C_Hitam7Merah7Hitam7() {
        // c. hitam 7, merah 7, hitam 7 -> haid hitam + merah (14 hari), hitam kedua istihadhah
        val start = 1700000000000L
        val t1 = start + 7 * oneDay
        val t2 = t1 + 7 * oneDay
        val t3 = t2 + 7 * oneDay
        val intervals = listOf(
            BleedingInterval(startEpochMillis = start, endEpochMillis = t1, bloodColor = BloodColor.HITAM, isThick = false, isOdorous = false),
            BleedingInterval(startEpochMillis = t1, endEpochMillis = t2, bloodColor = BloodColor.MERAH, isThick = false, isOdorous = false),
            BleedingInterval(startEpochMillis = t2, endEpochMillis = t3, bloodColor = BloodColor.HITAM, isThick = false, isOdorous = false)
        )
        val result = FiqihCalculatorEngine.calculate(
            caseType = CaseType.HAID,
            startEpochMillis = start,
            endEpochMillis = t3,
            hasPreviousAdat = false,
            adatDurationDays = 7,
            adatCycleDays = 30,
            adatMemoryType = AdatMemoryType.BELUM_PERNAH_HAID,
            haidCategory = HaidCategory.MUBTADIAH_MUMAYYIZAH,
            bloodColor = BloodColor.HITAM,
            isThick = false,
            isOdorous = false,
            intervals = intervals,
            prayerAtStart = null,
            hadPrayedAtStart = false,
            prayerAtStop = null
        )
        assertNotNull(result)
        val haidResults = result.periodResults.filter { it.status == FiqihStatus.HAID }
        val haidDuration = haidResults.sumOf { it.endEpochMillis - it.startEpochMillis }
        assertEquals(14 * oneDay, haidDuration)
        val istiResults = result.periodResults.filter { it.status == FiqihStatus.ISTIHADHAH }
        val istiDuration = istiResults.sumOf { it.endEpochMillis - it.startEpochMillis }
        assertEquals(7 * oneDay, istiDuration)
    }

    @Test
    fun testTamyizMultiTier_D_Hitam4Merah3Coklat3Kuning4Keruh10() {
        // d. hitam 4, merah 3, coklat 3, kuning 4, keruh 10 -> haid 7 hari
        val start = 1700000000000L
        val t1 = start + 4 * oneDay
        val t2 = t1 + 3 * oneDay
        val t3 = t2 + 3 * oneDay
        val t4 = t3 + 4 * oneDay
        val t5 = t4 + 10 * oneDay
        val intervals = listOf(
            BleedingInterval(startEpochMillis = start, endEpochMillis = t1, bloodColor = BloodColor.HITAM, isThick = false, isOdorous = false),
            BleedingInterval(startEpochMillis = t1, endEpochMillis = t2, bloodColor = BloodColor.MERAH, isThick = false, isOdorous = false),
            BleedingInterval(startEpochMillis = t2, endEpochMillis = t3, bloodColor = BloodColor.COKLAT, isThick = false, isOdorous = false),
            BleedingInterval(startEpochMillis = t3, endEpochMillis = t4, bloodColor = BloodColor.KUNING, isThick = false, isOdorous = false),
            BleedingInterval(startEpochMillis = t4, endEpochMillis = t5, bloodColor = BloodColor.KERUH, isThick = false, isOdorous = false)
        )
        val result = FiqihCalculatorEngine.calculate(
            caseType = CaseType.HAID,
            startEpochMillis = start,
            endEpochMillis = t5,
            hasPreviousAdat = false,
            adatDurationDays = 7,
            adatCycleDays = 30,
            adatMemoryType = AdatMemoryType.BELUM_PERNAH_HAID,
            haidCategory = HaidCategory.MUBTADIAH_MUMAYYIZAH,
            bloodColor = BloodColor.HITAM,
            isThick = false,
            isOdorous = false,
            intervals = intervals,
            prayerAtStart = null,
            hadPrayedAtStart = false,
            prayerAtStop = null
        )
        assertNotNull(result)
        val haidResults = result.periodResults.filter { it.status == FiqihStatus.HAID }
        val haidDuration = haidResults.sumOf { it.endEpochMillis - it.startEpochMillis }
        assertEquals(7 * oneDay, haidDuration)
    }

    @Test
    fun testTamyizMultiTier_E_Hitam5Kuning5Merah20() {
        // e. hitam 5, kuning 5, merah 20 -> haid hanya hitam (5 hari)
        val start = 1700000000000L
        val t1 = start + 5 * oneDay
        val t2 = t1 + 5 * oneDay
        val t3 = t2 + 20 * oneDay
        val intervals = listOf(
            BleedingInterval(startEpochMillis = start, endEpochMillis = t1, bloodColor = BloodColor.HITAM, isThick = false, isOdorous = false),
            BleedingInterval(startEpochMillis = t1, endEpochMillis = t2, bloodColor = BloodColor.KUNING, isThick = false, isOdorous = false),
            BleedingInterval(startEpochMillis = t2, endEpochMillis = t3, bloodColor = BloodColor.MERAH, isThick = false, isOdorous = false)
        )
        val result = FiqihCalculatorEngine.calculate(
            caseType = CaseType.HAID,
            startEpochMillis = start,
            endEpochMillis = t3,
            hasPreviousAdat = false,
            adatDurationDays = 7,
            adatCycleDays = 30,
            adatMemoryType = AdatMemoryType.BELUM_PERNAH_HAID,
            haidCategory = HaidCategory.MUBTADIAH_MUMAYYIZAH,
            bloodColor = BloodColor.HITAM,
            isThick = false,
            isOdorous = false,
            intervals = intervals,
            prayerAtStart = null,
            hadPrayedAtStart = false,
            prayerAtStop = null
        )
        assertNotNull(result)
        val haidResults = result.periodResults.filter { it.status == FiqihStatus.HAID }
        val haidDuration = haidResults.sumOf { it.endEpochMillis - it.startEpochMillis }
        assertEquals(5 * oneDay, haidDuration)
    }

    @Test
    fun testTamyizMultiTier_Merah2Hitam5Merah4Kuning20() {
        // Mubtadi'ah: merah 2 hari, hitam 5 hari, merah 4 hari, kuning 20 hari (sifat sama).
        // Hasil: haid 9 hari (hitam 5 + merah 4), merah 2 hari awal istihadhah, kuning 20 hari istihadhah.
        val start = 1700000000000L
        val t1 = start + 2 * oneDay   // merah 2
        val t2 = t1 + 5 * oneDay      // hitam 5
        val t3 = t2 + 4 * oneDay      // merah 4
        val t4 = t3 + 20 * oneDay     // kuning 20
        val intervals = listOf(
            BleedingInterval(startEpochMillis = start, endEpochMillis = t1, bloodColor = BloodColor.MERAH, isThick = false, isOdorous = false),
            BleedingInterval(startEpochMillis = t1, endEpochMillis = t2, bloodColor = BloodColor.HITAM, isThick = false, isOdorous = false),
            BleedingInterval(startEpochMillis = t2, endEpochMillis = t3, bloodColor = BloodColor.MERAH, isThick = false, isOdorous = false),
            BleedingInterval(startEpochMillis = t3, endEpochMillis = t4, bloodColor = BloodColor.KUNING, isThick = false, isOdorous = false)
        )
        val result = FiqihCalculatorEngine.calculate(
            caseType = CaseType.HAID,
            startEpochMillis = start,
            endEpochMillis = t4,
            hasPreviousAdat = false,
            adatDurationDays = 7,
            adatCycleDays = 30,
            adatMemoryType = AdatMemoryType.BELUM_PERNAH_HAID,
            haidCategory = HaidCategory.MUBTADIAH_MUMAYYIZAH,
            bloodColor = BloodColor.HITAM,
            isThick = false,
            isOdorous = false,
            intervals = intervals,
            prayerAtStart = null,
            hadPrayedAtStart = false,
            prayerAtStop = null
        )
        assertNotNull(result)
        val haidResults = result.periodResults.filter { it.status == FiqihStatus.HAID }
        val haidDuration = haidResults.sumOf { it.endEpochMillis - it.startEpochMillis }
        assertEquals(9 * oneDay, haidDuration)

        val istiResults = result.periodResults.filter { it.status == FiqihStatus.ISTIHADHAH }
        val istiDuration = istiResults.sumOf { it.endEpochMillis - it.startEpochMillis }
        assertEquals(22 * oneDay, istiDuration)

        assertTrue("Merah 2 hari sebelum hitam harus berstatus istihadhah",
            istiResults.any { it.startEpochMillis == start && it.endEpochMillis == t1 })
        assertTrue("Hitam 5 hari harus berstatus haid",
            haidResults.any { it.startEpochMillis == t1 && it.endEpochMillis == t2 })
        assertTrue("Merah 4 hari setelah hitam harus berstatus haid",
            haidResults.any { it.startEpochMillis == t2 && it.endEpochMillis == t3 })
        val kuningIstiDuration = istiResults.filter { it.startEpochMillis >= t3 && it.endEpochMillis <= t4 }
            .sumOf { it.endEpochMillis - it.startEpochMillis }
        assertEquals("Kuning 20 hari harus berstatus istihadhah", 20 * oneDay, kuningIstiDuration)
    }

    @Test
    fun testTamyizMultiTier_Merah5Hitam5Merah20_DuaTingkat() {
        // Mubtadi'ah: merah 5 hari, hitam 5 hari, merah 20 hari (dua tingkat, sifat sama).
        // Hasil: haid hanya hitam 5 hari (karena hitam 5 + merah 20 = 25 hari > 15 hari).
        // Merah 5 hari awal dan merah 20 hari akhir berstatus istihadhah.
        val start = 1700000000000L
        val t1 = start + 5 * oneDay   // merah 5
        val t2 = t1 + 5 * oneDay      // hitam 5
        val t3 = t2 + 20 * oneDay     // merah 20
        val intervals = listOf(
            BleedingInterval(startEpochMillis = start, endEpochMillis = t1, bloodColor = BloodColor.MERAH, isThick = false, isOdorous = false),
            BleedingInterval(startEpochMillis = t1, endEpochMillis = t2, bloodColor = BloodColor.HITAM, isThick = false, isOdorous = false),
            BleedingInterval(startEpochMillis = t2, endEpochMillis = t3, bloodColor = BloodColor.MERAH, isThick = false, isOdorous = false)
        )
        val result = FiqihCalculatorEngine.calculate(
            caseType = CaseType.HAID,
            startEpochMillis = start,
            endEpochMillis = t3,
            hasPreviousAdat = false,
            adatDurationDays = 7,
            adatCycleDays = 30,
            adatMemoryType = AdatMemoryType.BELUM_PERNAH_HAID,
            haidCategory = HaidCategory.MUBTADIAH_MUMAYYIZAH,
            bloodColor = BloodColor.HITAM,
            isThick = false,
            isOdorous = false,
            intervals = intervals,
            prayerAtStart = null,
            hadPrayedAtStart = false,
            prayerAtStop = null
        )
        assertNotNull(result)
        val haidResults = result.periodResults.filter { it.status == FiqihStatus.HAID }
        val haidDuration = haidResults.sumOf { it.endEpochMillis - it.startEpochMillis }
        assertEquals(5 * oneDay, haidDuration)

        val istiResults = result.periodResults.filter { it.status == FiqihStatus.ISTIHADHAH }
        val istiDuration = istiResults.sumOf { it.endEpochMillis - it.startEpochMillis }
        assertEquals(25 * oneDay, istiDuration)

        assertTrue("Merah 5 hari awal harus berstatus istihadhah",
            istiResults.any { it.startEpochMillis == start && it.endEpochMillis == t1 })
        assertTrue("Hitam 5 hari harus berstatus haid",
            haidResults.any { it.startEpochMillis == t1 && it.endEpochMillis == t2 })
        val merahAkhirIstiDuration = istiResults.filter { it.startEpochMillis >= t2 && it.endEpochMillis <= t3 }
            .sumOf { it.endEpochMillis - it.startEpochMillis }
        assertEquals("Merah 20 hari akhir harus berstatus istihadhah", 20 * oneDay, merahAkhirIstiDuration)
    }

    @Test
    fun testMutadahMumayyizah_Kuat8Lemah7Kuat8() {
        // Mu'tadah (hasPreviousAdat = true, adat 5 hari): kuat 8, lemah 7, kuat 8.
        // Tamyiz mengalahkan adat: haid 15 hari (kuat 8 + lemah 7), kuat kedua istihadhah.
        val start = 1700000000000L
        val t1 = start + 8 * oneDay
        val t2 = t1 + 7 * oneDay
        val t3 = t2 + 8 * oneDay
        val intervals = listOf(
            BleedingInterval(startEpochMillis = start, endEpochMillis = t1, bloodColor = BloodColor.HITAM, isThick = false, isOdorous = false),
            BleedingInterval(startEpochMillis = t1, endEpochMillis = t2, bloodColor = BloodColor.MERAH, isThick = false, isOdorous = false),
            BleedingInterval(startEpochMillis = t2, endEpochMillis = t3, bloodColor = BloodColor.HITAM, isThick = false, isOdorous = false)
        )
        val result = FiqihCalculatorEngine.calculate(
            caseType = CaseType.HAID,
            startEpochMillis = start,
            endEpochMillis = t3,
            hasPreviousAdat = true,
            adatDurationDays = 5,
            adatCycleDays = 30,
            adatMemoryType = AdatMemoryType.INGAT_LENGKAP_QADRAN_WAQTAN,
            haidCategory = HaidCategory.MUTADAH_MUMAYYIZAH,
            bloodColor = BloodColor.HITAM,
            isThick = false,
            isOdorous = false,
            intervals = intervals,
            prayerAtStart = null,
            hadPrayedAtStart = false,
            prayerAtStop = null
        )
        assertNotNull(result)
        val haidResults = result.periodResults.filter { it.status == FiqihStatus.HAID }
        val haidDuration = haidResults.sumOf { it.endEpochMillis - it.startEpochMillis }
        assertEquals(15 * oneDay, haidDuration)
        val istiResults = result.periodResults.filter { it.status == FiqihStatus.ISTIHADHAH }
        val istiDuration = istiResults.sumOf { it.endEpochMillis - it.startEpochMillis }
        assertEquals(8 * oneDay, istiDuration)
    }

    @Test
    fun testUserCase_Adat7_Hitam1to9_Merah9to20() {
        val cal = Calendar.getInstance()
        cal.set(2026, Calendar.SEPTEMBER, 1, 0, 0, 0)
        val start = cal.timeInMillis
        cal.set(2026, Calendar.SEPTEMBER, 9, 0, 0, 0)
        val t1 = cal.timeInMillis
        cal.set(2026, Calendar.SEPTEMBER, 20, 0, 0, 0)
        val end = cal.timeInMillis

        val intervals = listOf(
            BleedingInterval(startEpochMillis = start, endEpochMillis = t1, bloodColor = BloodColor.HITAM, isThick = true, isOdorous = true),
            BleedingInterval(startEpochMillis = t1, endEpochMillis = end, bloodColor = BloodColor.MERAH, isThick = false, isOdorous = false)
        )
        val result = FiqihCalculatorEngine.calculate(
            caseType = CaseType.HAID,
            startEpochMillis = start,
            endEpochMillis = end,
            hasPreviousAdat = true,
            adatDurationDays = 7,
            adatCycleDays = 28,
            adatMemoryType = AdatMemoryType.INGAT_LENGKAP_QADRAN_WAQTAN,
            haidCategory = HaidCategory.MUTADAH_GHAIRU_MUMAYYIZAH_DZAKIRAH_QADRAN_WAQTAN,
            bloodColor = BloodColor.HITAM,
            isThick = true,
            isOdorous = true,
            intervals = intervals,
            prayerAtStart = null,
            hadPrayedAtStart = false,
            prayerAtStop = null
        )
        assertNotNull(result)
        assertTrue("Kategori harus Mu'tadah Mumayyizah", result.caseCategory.contains("Mu'tadah Mumayyizah", ignoreCase = true))
        assertEquals("Durasi haid segmen harus 8 hari (darah hitam tgl 1-9 Sept)", 8L, result.haidOrNifasSegment?.durationDays)
        assertEquals("Durasi istihadhah segmen harus 11 hari (darah merah tgl 9-20 Sept)", 11L, result.istihadhahSegment?.durationDays)
        assertTrue("Status summary harus menyebut Tamyiz Mengalahkan Adat", result.statusSummary.contains("Tamyiz Mengalahkan Adat", ignoreCase = true))

        val haidResults = result.periodResults.filter { it.status == FiqihStatus.HAID }
        assertEquals("Total durasi haid dari periodResults harus 8 hari", 8 * oneDay, haidResults.sumOf { it.endEpochMillis - it.startEpochMillis })
        val istiResults = result.periodResults.filter { it.status == FiqihStatus.ISTIHADHAH }
        assertEquals("Total durasi istihadhah dari periodResults harus 11 hari", 11 * oneDay, istiResults.sumOf { it.endEpochMillis - it.startEpochMillis })
    }

    @Test
    fun testUserCase_Adat7_Hitam9_Merah3_Hitam7() {
        val cal = Calendar.getInstance()
        cal.set(2026, Calendar.OCTOBER, 1, 0, 0, 0)
        val start = cal.timeInMillis
        cal.set(2026, Calendar.OCTOBER, 10, 0, 0, 0)
        val t1 = cal.timeInMillis
        cal.set(2026, Calendar.OCTOBER, 13, 0, 0, 0)
        val t2 = cal.timeInMillis
        cal.set(2026, Calendar.OCTOBER, 20, 0, 0, 0)
        val end = cal.timeInMillis

        val intervals = listOf(
            BleedingInterval(startEpochMillis = start, endEpochMillis = t1, bloodColor = BloodColor.HITAM, isThick = true, isOdorous = true),
            BleedingInterval(startEpochMillis = t1, endEpochMillis = t2, bloodColor = BloodColor.MERAH, isThick = false, isOdorous = false),
            BleedingInterval(startEpochMillis = t2, endEpochMillis = end, bloodColor = BloodColor.HITAM, isThick = true, isOdorous = false)
        )
        val result = FiqihCalculatorEngine.calculate(
            caseType = CaseType.HAID,
            startEpochMillis = start,
            endEpochMillis = end,
            hasPreviousAdat = true,
            adatDurationDays = 7,
            adatCycleDays = 28,
            adatMemoryType = AdatMemoryType.INGAT_LENGKAP_QADRAN_WAQTAN,
            haidCategory = HaidCategory.MUTADAH_GHAIRU_MUMAYYIZAH_DZAKIRAH_QADRAN_WAQTAN,
            bloodColor = BloodColor.HITAM,
            isThick = true,
            isOdorous = true,
            intervals = intervals,
            prayerAtStart = null,
            hadPrayedAtStart = false,
            prayerAtStop = null
        )
        assertNotNull(result)
        println("=== TEST RESULT 3 FASE ===")
        println("Category: ${result.caseCategory}")
        println("StatusSummary: ${result.statusSummary}")
        println("HaidSegment: ${result.haidOrNifasSegment?.durationDays} days (${result.haidOrNifasSegment?.durationHours} hrs)")
        println("IstiSegment: ${result.istihadhahSegment?.durationDays} days (${result.istihadhahSegment?.durationHours} hrs)")
        result.periodResults.forEach {
            println("Period: ${it.status} from ${it.startEpochMillis} to ${it.endEpochMillis} (dur: ${(it.endEpochMillis - it.startEpochMillis) / (1000 * 3600 * 24)} days)")
        }
    }
}

