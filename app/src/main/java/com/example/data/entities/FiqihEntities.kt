package com.example.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "calculation_history")
data class CalculationHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val caseType: String, // "HAID" or "NIFAS"
    val startEpochMillis: Long,
    val endEpochMillis: Long,
    val statusSummary: String,
    val categoryName: String,
    val categoryDetectionReason: String = "",
    val haidHours: Long,
    val istihadhahHours: Long,
    val suciHours: Long = 0L,
    val totalHours: Long = 0L,
    val totalDays: Int = 0,
    val shalatNote: String,
    val puasaNote: String,
    val mandiNote: String,
    val cycleLengthDays: Int = 0, // jarak siklus (hari) dari haid sebelumnya
    val note: String = ""
)

@Entity(tableName = "qadha_prayers")
data class QadhaPrayerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val prayerName: String, // e.g. "Dzuhur", "Ashar", "Maghrib", "Isya", "Subuh"
    val dateString: String,
    val reason: String, // e.g. "Haid datang setelah masuk waktu Dzuhur", "Suci di waktu Ashar (kaidah jamak)"
    val isCompleted: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_adat_profile")
data class UserAdatProfileEntity(
    @PrimaryKey val id: Int = 1,
    val userName: String = "Muslimah",
    val userBio: String = "Menjaga Kesucian & Ibadah Sesuai Mazhab Syafi'i",
    val usePersonalPhoto: Boolean = false,
    val photoUri: String? = null,
    val avatarTemplateIndex: Int = 0, // 0..5 preset avatar templates
    val usualHaidDays: Int = 7,
    val usualSuciDays: Int = 23,
    val usualNifasDays: Int = 40,
    val isMumayyizah: Boolean = true,
    val lastPeriodStartMillis: Long = 0L,
    val lastPeriodEndMillis: Long = 0L
)

