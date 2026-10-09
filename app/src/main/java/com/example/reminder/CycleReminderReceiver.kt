package com.example.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.AppDatabase
import com.example.fiqih.CalendarPredictionEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class CycleReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action

        if (action == CycleNotificationScheduler.ACTION_CYCLE_REMINDER) {
            val daysBefore = intent.getIntExtra(CycleNotificationScheduler.EXTRA_DAYS_BEFORE, 2)
            val predictedDate = intent.getStringExtra(CycleNotificationScheduler.EXTRA_PREDICTED_DATE)
                ?: "beberapa hari lagi"
            CycleNotificationScheduler.showCycleAlertNotification(context, daysBefore, predictedDate)
        } else if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            // Re-schedule alarm from Room database
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    val logs = db.fiqihDao().getAllBloodLogs().firstOrNull() ?: emptyList()
                    val profile = db.fiqihDao().getUserProfile().firstOrNull()
                        ?: com.example.data.entities.UserAdatProfileEntity()
                    val prediction = CalendarPredictionEngine.computeCyclePrediction(logs, profile)
                    val nextStartEpoch = prediction.nextCycleStartEpochDay
                    val dateText = prediction.summary.predictedNextHaidStartText

                    CycleNotificationScheduler.scheduleNextCycleAlert(
                        context,
                        nextStartEpoch,
                        dateText
                    )
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
