package com.example.reminder

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import java.util.*

object CycleNotificationScheduler {

    const val CHANNEL_ID_CYCLE = "fiqih_cycle_prediction_channel"
    const val NOTIFICATION_ID_CYCLE = 2001
    const val ACTION_CYCLE_REMINDER = "com.example.ACTION_CYCLE_REMINDER"
    const val EXTRA_DAYS_BEFORE = "extra_days_before"
    const val EXTRA_PREDICTED_DATE = "extra_predicted_date"

    private const val PREFS_NAME = "fiqih_cycle_notifications"
    private const val KEY_ENABLED = "cycle_notifications_enabled"
    private const val KEY_DAYS_BEFORE = "cycle_notifications_days_before"
    private const val KEY_SCHEDULED_TIME = "cycle_notifications_scheduled_time"

    fun initNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val channel = NotificationChannel(
                CHANNEL_ID_CYCLE,
                "Pengingat Siklus Haid Berikutnya",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi pemberitahuan beberapa hari sebelum prediksi waktu keluarnya darah haid"
                enableVibration(true)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun isNotificationEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_ENABLED, true) // default enabled
    }

    fun setNotificationEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
        if (!enabled) {
            cancelScheduledAlarm(context)
        }
    }

    fun getDaysBeforeAlert(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_DAYS_BEFORE, 2) // default 2 days before
    }

    fun setDaysBeforeAlert(context: Context, days: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_DAYS_BEFORE, days.coerceIn(1, 5)).apply()
    }

    /**
     * Schedules a local alarm alert a few days before the predicted next cycle start date.
     * @param nextCycleStartEpochDay epoch day (days since 1970-01-01)
     */
    fun scheduleNextCycleAlert(
        context: Context,
        nextCycleStartEpochDay: Long?,
        predictedDateText: String?
    ) {
        if (nextCycleStartEpochDay == null) {
            cancelScheduledAlarm(context)
            return
        }

        if (!isNotificationEnabled(context)) {
            cancelScheduledAlarm(context)
            return
        }

        val daysBefore = getDaysBeforeAlert(context)
        val alertEpochDay = nextCycleStartEpochDay - daysBefore

        // Calculate alarm time in millis: 08:00 AM on the alert day
        val alarmCal = Calendar.getInstance().apply {
            timeInMillis = alertEpochDay * 24 * 3600 * 1000L
            set(Calendar.HOUR_OF_DAY, 8)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val now = System.currentTimeMillis()
        var targetTimeMillis = alarmCal.timeInMillis

        // If the 08:00 AM on alert day has already passed but today is still before the cycle start,
        // trigger in next 30 minutes or tomorrow morning
        if (targetTimeMillis <= now) {
            val startMillis = nextCycleStartEpochDay * 24 * 3600 * 1000L
            if (startMillis > now) {
                // Schedule alert for tomorrow at 08:00 AM or 1 minute from now for testing
                targetTimeMillis = now + (60 * 1000L)
            } else {
                // Past prediction date
                return
            }
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, CycleReminderReceiver::class.java).apply {
            action = ACTION_CYCLE_REMINDER
            putExtra(EXTRA_DAYS_BEFORE, daysBefore)
            putExtra(EXTRA_PREDICTED_DATE, predictedDateText ?: "beberapa hari lagi")
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            NOTIFICATION_ID_CYCLE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    targetTimeMillis,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    targetTimeMillis,
                    pendingIntent
                )
            }

            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putLong(KEY_SCHEDULED_TIME, targetTimeMillis).apply()
        } catch (e: SecurityException) {
            // In case of permission issues on strict OEM devices
            alarmManager.set(AlarmManager.RTC_WAKEUP, targetTimeMillis, pendingIntent)
        }
    }

    fun cancelScheduledAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, CycleReminderReceiver::class.java).apply {
            action = ACTION_CYCLE_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            NOTIFICATION_ID_CYCLE,
            intent,
            PendingIntent.FLAG_NO_CREATE or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_SCHEDULED_TIME).apply()
    }

    /**
     * Sends an immediate local notification (used when alarm fires or when user tests notification in settings).
     */
    fun showCycleAlertNotification(
        context: Context,
        daysBefore: Int,
        predictedDateText: String
    ) {
        initNotificationChannel(context)

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Intent to open MainActivity on tab 1 (Calendar)
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("OPEN_TAB", 1) // Calendar tab
        }

        val contentPendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_CYCLE,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val title = "Pengingat Perkiraan Haid ($daysBefore Hari Lagi)"
        val message = "Berdasarkan catatan kalender dan adat Anda, haid berikutnya diperkirakan mulai sekitar $predictedDateText. Siapkan perlengkapan dan amati keluarnya darah."

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_CYCLE)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .build()

        notificationManager.notify(NOTIFICATION_ID_CYCLE, notification)
    }
}
