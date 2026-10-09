package com.example.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "calculation_history",
    indices = [Index(value = ["caseType", "startEpochMillis", "endEpochMillis"], unique = true)]
)
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

@Entity(
    tableName = "qadha_prayers",
    indices = [Index(value = ["prayerName", "dateString", "reason"], unique = true)]
)
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

@Entity(tableName = "daily_blood_logs")
data class DailyBloodLogEntity(
    @PrimaryKey val dateString: String, // "yyyy-MM-dd"
    val epochDay: Long = 0L,
    val caseType: String = "HAID",
    val hasBlood: Boolean = true,
    val bloodColor: String = "MERAH",
    val flowIntensity: String = "SEDANG",
    val startTime: String = "08:00",
    val stopTime: String = "20:00",
    val isPaused: Boolean = false,
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
