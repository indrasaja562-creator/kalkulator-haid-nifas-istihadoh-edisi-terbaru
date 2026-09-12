package com.example.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.R
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

data class PrayerSchedule(
    val dateString: String,
    val subuh: String,
    val dzuhur: String,
    val ashar: String,
    val maghrib: String,
    val isya: String,
    val nextPrayerName: String,
    val nextPrayerTime: String
)

object PrayerReminderHelper {

    private const val CHANNEL_ID_PRAYER = "fiqih_prayer_channel"
    private const val CHANNEL_ID_MANDI = "fiqih_mandi_channel"

    fun initNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val prayerChannel = NotificationChannel(
                CHANNEL_ID_PRAYER,
                "Pengingat Waktu Shalat",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifikasi masuk waktu shalat untuk wanita suci dan mustahadhah"
            }

            val mandiChannel = NotificationChannel(
                CHANNEL_ID_MANDI,
                "Pengingat Mandi Wajib & Suci",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Pengingat waktu mandi besar saat darah berhenti atau mencapai adat"
            }

            notificationManager.createNotificationChannel(prayerChannel)
            notificationManager.createNotificationChannel(mandiChannel)
        }
    }

    fun showMandiReminderNotification(context: Context, title: String, message: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val builder = NotificationCompat.Builder(context, CHANNEL_ID_MANDI)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        notificationManager.notify(1001, builder.build())
    }

    /**
     * Calculates Indonesian standard prayer times (WIB baseline: approx -6.2 Lat, 106.8 Lon Jakarta)
     */
    fun calculatePrayerTimes(calendar: Calendar = Calendar.getInstance()): PrayerSchedule {
        val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
        val lat = -6.2088 // Jakarta approx
        val lng = 106.8456
        val timeZone = 7.0 // UTC+7

        // Solar declination approx
        val b = 2.0 * Math.PI * (dayOfYear - 81) / 365.0
        val eot = 9.87 * sin(2 * b) - 7.53 * cos(b) - 1.5 * sin(b) // Equation of time in minutes
        val declination = 23.45 * sin(2.0 * Math.PI * (284 + dayOfYear) / 365.0) // degrees

        val solarNoon = 12.0 + (4.0 * (timeZone * 15.0 - lng) - eot) / 60.0

        val latRad = Math.toRadians(lat)
        val decRad = Math.toRadians(declination)

        // Subuh: sun angle -20 degrees (Kemenag standard)
        val subuhAngle = Math.toRadians(-20.0)
        val cosSubuh = (sin(subuhAngle) - sin(latRad) * sin(decRad)) / (cos(latRad) * cos(decRad))
        val subuhHour = if (cosSubuh in -1.0..1.0) {
            solarNoon - Math.toDegrees(acos(cosSubuh)) / 15.0
        } else {
            4.5
        }

        // Maghrib: sun zenith 90.833 degrees (-0.833 below horizon)
        val maghribAngle = Math.toRadians(-0.833)
        val cosMaghrib = (sin(maghribAngle) - sin(latRad) * sin(decRad)) / (cos(latRad) * cos(decRad))
        val maghribHour = if (cosMaghrib in -1.0..1.0) {
            solarNoon + Math.toDegrees(acos(cosMaghrib)) / 15.0
        } else {
            18.0
        }

        // Ashar: Shadow length = tan(abs(lat - declination)) + 1
        val asharShadow = 1.0 + tan(abs(latRad - decRad))
        val asharAltitude = atan(1.0 / asharShadow)
        val cosAshar = (sin(asharAltitude) - sin(latRad) * sin(decRad)) / (cos(latRad) * cos(decRad))
        val asharHour = if (cosAshar in -1.0..1.0) {
            solarNoon + Math.toDegrees(acos(cosAshar)) / 15.0
        } else {
            15.25
        }

        // Isya: sun angle -18 degrees
        val isyaAngle = Math.toRadians(-18.0)
        val cosIsya = (sin(isyaAngle) - sin(latRad) * sin(decRad)) / (cos(latRad) * cos(decRad))
        val isyaHour = if (cosIsya in -1.0..1.0) {
            solarNoon + Math.toDegrees(acos(cosIsya)) / 15.0
        } else {
            19.2
        }

        val dzuhurHour = solarNoon + (2.0 / 60.0) // ihtiyat 2 min

        fun formatHours(h: Double): String {
            val totalMins = (h * 60).roundToInt()
            val hour = (totalMins / 60) % 24
            val min = totalMins % 60
            return String.format(Locale.getDefault(), "%02d:%02d", hour, min)
        }

        val subuhStr = formatHours(subuhHour + (2.0 / 60.0))
        val dzuhurStr = formatHours(dzuhurHour)
        val asharStr = formatHours(asharHour + (2.0 / 60.0))
        val maghribStr = formatHours(maghribHour + (2.0 / 60.0))
        val isyaStr = formatHours(isyaHour + (2.0 / 60.0))

        val nowHour = calendar.get(Calendar.HOUR_OF_DAY) + calendar.get(Calendar.MINUTE) / 60.0

        val (nextName, nextTime) = when {
            nowHour < subuhHour -> "Subuh" to subuhStr
            nowHour < dzuhurHour -> "Dzuhur" to dzuhurStr
            nowHour < asharHour -> "Ashar" to asharStr
            nowHour < maghribHour -> "Maghrib" to maghribStr
            nowHour < isyaHour -> "Isya" to isyaStr
            else -> "Subuh Besok" to subuhStr
        }

        val dateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID"))
        val dateString = dateFormat.format(calendar.time)

        return PrayerSchedule(
            dateString = dateString,
            subuh = subuhStr,
            dzuhur = dzuhurStr,
            ashar = asharStr,
            maghrib = maghribStr,
            isya = isyaStr,
            nextPrayerName = nextName,
            nextPrayerTime = nextTime
        )
    }
}
