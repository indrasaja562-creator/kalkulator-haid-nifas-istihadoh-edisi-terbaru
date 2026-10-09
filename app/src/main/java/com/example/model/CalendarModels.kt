package com.example.model

import com.example.data.entities.DailyBloodLogEntity
import java.text.SimpleDateFormat
import java.util.*

enum class CalendarBloodColor(
    val idName: String,
    val label: String,
    val colorHex: Long,
    val fiqihRank: Int,
    val fiqihStrength: String,
    val fiqihDesc: String
) {
    HITAM("HITAM", "Hitam (Aswad)", 0xFF212121, 1, "Paling Kuat", "Tingkat 1 - Darah paling kuat & pekat"),
    MERAH("MERAH", "Merah (Ahmar)", 0xFFD32F2F, 2, "Kuat", "Tingkat 2 - Darah kuat segar"),
    MERAH_TUA("MERAH_TUA", "Merah Tua / Marun", 0xFF880E4F, 3, "Kuat-Sedang", "Tingkat 3 - Darah kuat-sedang"),
    COKLAT("COKLAT", "Coklat (Kudrah)", 0xFF795548, 4, "Lemah", "Tingkat 4 - Cairan keruh kecoklatan"),
    KUNING("KUNING", "Kuning (Shufrah)", 0xFFF57F17, 5, "Paling Lemah", "Tingkat 5 - Cairan kekuningan"),
    KERUH("KERUH", "Keruh (Kudrah Khafifah)", 0xFF757575, 6, "Lemah", "Tingkat 6 - Warna kelabu/keruh"),
    BENING("BENING", "Bening / Putih Suci (Naqa')", 0xFF2E7D32, 7, "Tanda Suci", "Kering atau keluar keputihan jernih tanda suci mutlak")
}

enum class FlowIntensity(val idName: String, val label: String) {
    DERAS("DERAS", "Deras / Banyak"),
    SEDANG("SEDANG", "Sedang / Normal"),
    SEDIKIT("SEDIKIT", "Sedikit / Tetesan"),
    FLEK("FLEK", "Flek / Noda Kecoklatan")
}

data class CalendarDayItem(
    val dateString: String, // "yyyy-MM-dd"
    val dayOfMonth: Int,
    val month: Int, // 0..11
    val year: Int,
    val epochDay: Long,
    val isCurrentMonth: Boolean,
    val isToday: Boolean,
    val log: DailyBloodLogEntity?,
    val isPredictedHaid: Boolean = false,
    val isPredictedStopDay: Boolean = false,
    val isPredictedSuci: Boolean = false,
    val cycleDayIndex: Int? = null
)

data class CyclePredictionSummary(
    val currentStatusTitle: String,
    val currentStatusDesc: String,
    val isActiveHaid: Boolean,
    val activeHaidDaysSoFar: Int,
    val predictedStopDateText: String?,
    val daysUntilPredictedStop: Int?,
    val predictedNextHaidStartText: String?,
    val daysUntilNextHaid: Int?,
    val predictedNextHaidStopText: String?,
    val fiqihNote: String
)

object CalendarDateHelper {
    val ISO_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val DISPLAY_DAY_DATE = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID"))
    val DISPLAY_SHORT_DATE = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
    val DISPLAY_MONTH_YEAR = SimpleDateFormat("MMMM yyyy", Locale("id", "ID"))

    fun toEpochDay(cal: Calendar): Long {
        val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(Calendar.YEAR, cal.get(Calendar.YEAR))
            set(Calendar.MONTH, cal.get(Calendar.MONTH))
            set(Calendar.DAY_OF_MONTH, cal.get(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return utcCal.timeInMillis / (24 * 3600 * 1000L)
    }

    fun fromEpochDay(epochDay: Long): Calendar {
        return Calendar.getInstance().apply {
            timeInMillis = epochDay * (24 * 3600 * 1000L)
        }
    }

    fun formatIso(cal: Calendar): String {
        return String.format(
            Locale.US,
            "%04d-%02d-%02d",
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun getTodayEpochDay(): Long {
        return toEpochDay(Calendar.getInstance())
    }

    fun getTodayIso(): String {
        return formatIso(Calendar.getInstance())
    }
}
