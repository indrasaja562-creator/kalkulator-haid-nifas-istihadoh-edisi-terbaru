package com.example

import com.example.fiqih.FiqihCalculatorEngine
import com.example.model.*
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testMutahayyirahMahdhah_Golongan5() {
    val now = 1700000000000L
    val eighteenDaysMs = 18 * 24 * 3600_000L
    val start = now - eighteenDaysMs

    val result = FiqihCalculatorEngine.calculate(
      caseType = CaseType.HAID,
      startEpochMillis = start,
      endEpochMillis = now,
      hasPreviousAdat = true,
      adatDurationDays = 7,
      adatCycleDays = 28,
      adatMemoryType = AdatMemoryType.LUPA_SEMUANYA_MUTAHAYYIRAH,
      haidCategory = HaidCategory.MUTADAH_NASIYAH_MUTAHAYYIRAH,
      bloodColor = BloodColor.MERAH,
      isThick = true,
      isOdorous = true,
      intervals = listOf(
        BleedingInterval(
          startEpochMillis = start,
          endEpochMillis = now,
          bloodColor = BloodColor.MERAH,
          isThick = true,
          isOdorous = true
        )
      ),
      prayerAtStart = null,
      hadPrayedAtStart = true,
      prayerAtStop = null
    )

    assertNotNull(result.istihadhahSegment)
    assertTrue(result.caseCategory.contains("Mutahayyirah Mahdhah"))
    assertTrue(result.shalatConsequence.contains("IHTIYATH", ignoreCase = true))
    assertNotNull(result.haidOrNifasSegment)
    assertTrue(result.haidOrNifasSegment?.title?.contains("Ihtiyath", ignoreCase = true) == true)
  }

  @Test
  fun testMutahayyirahDzakirahWaqtan_Golongan7() {
    val now = 1700000000000L
    val eighteenDaysMs = 18 * 24 * 3600_000L
    val start = now - eighteenDaysMs

    val result = FiqihCalculatorEngine.calculate(
      caseType = CaseType.HAID,
      startEpochMillis = start,
      endEpochMillis = now,
      hasPreviousAdat = true,
      adatDurationDays = 7,
      adatCycleDays = 28,
      adatMemoryType = AdatMemoryType.INGAT_WAQTAN_LUPA_QADRAN,
      haidCategory = HaidCategory.MUTADAH_DZAKIRAH_WAQTAN_DUNAN_QADR,
      bloodColor = BloodColor.MERAH,
      isThick = true,
      isOdorous = true,
      intervals = listOf(
        BleedingInterval(
          startEpochMillis = start,
          endEpochMillis = now,
          bloodColor = BloodColor.MERAH,
          isThick = true,
          isOdorous = true
        )
      ),
      prayerAtStart = null,
      hadPrayedAtStart = true,
      prayerAtStop = null,
      rememberedCertainHaidDayOfMonth = 1
    )

    assertNotNull(result.istihadhahSegment)
    assertTrue(result.caseCategory.contains("Dzakirah lil-Waqti dunan Qadr"))
    assertNotNull(result.haidOrNifasSegment)
    assertTrue(result.haidOrNifasSegment?.title?.contains("Haid Yakin", ignoreCase = true) == true)
  }

  @Test
  fun testMutahayyirahDzakirahQadran_Golongan6() {
    val now = 1700000000000L
    val eighteenDaysMs = 18 * 24 * 3600_000L
    val start = now - eighteenDaysMs

    val result = FiqihCalculatorEngine.calculate(
      caseType = CaseType.HAID,
      startEpochMillis = start,
      endEpochMillis = now,
      hasPreviousAdat = true,
      adatDurationDays = 7,
      adatCycleDays = 28,
      adatMemoryType = AdatMemoryType.INGAT_QADRAN_LUPA_WAQTAN,
      haidCategory = HaidCategory.MUTADAH_DZAKIRAH_QADRAN_DUNAN_WAQT,
      bloodColor = BloodColor.MERAH,
      isThick = true,
      isOdorous = true,
      intervals = listOf(
        BleedingInterval(
          startEpochMillis = start,
          endEpochMillis = now,
          bloodColor = BloodColor.MERAH,
          isThick = true,
          isOdorous = true
        )
      ),
      prayerAtStart = null,
      hadPrayedAtStart = true,
      prayerAtStop = null
    )

    assertNotNull(result.istihadhahSegment)
    assertTrue(result.caseCategory.contains("Dzakirah", ignoreCase = true) && result.caseCategory.contains("Qadran", ignoreCase = true))
    assertNotNull(result.haidOrNifasSegment)
  }

  @Test
  fun testUserAdatProfileEntity_defaultsAndValidation() {
    val profile = com.example.data.entities.UserAdatProfileEntity(
      userName = "Fathimah",
      usualHaidDays = 7,
      usualSuciDays = 23,
      usualNifasDays = 40
    )
    assertEquals("Fathimah", profile.userName)
    assertEquals(7, profile.usualHaidDays)
    assertEquals(23, profile.usualSuciDays)
    assertEquals(40, profile.usualNifasDays)
    assertTrue(profile.usualHaidDays in 1..15)
    assertTrue(profile.usualSuciDays >= 15)
  }
}
