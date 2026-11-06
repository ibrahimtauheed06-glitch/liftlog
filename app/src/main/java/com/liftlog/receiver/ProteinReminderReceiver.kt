package com.liftlog.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.liftlog.LiftLogApplication
import com.liftlog.MainActivity
import com.liftlog.R
import com.liftlog.data.repository.ProteinRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ProteinReminderReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "liftlog_protein_reminder_channel"
        const val NOTIFICATION_ID = 2002
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val app = context.applicationContext as? LiftLogApplication ?: return
        val container = app.container
        val settingsRepo = container.settingsRepository

        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            CoroutineScope(Dispatchers.IO).launch {
                val settings = settingsRepo.settingsFlow.first()
                if (settings.proteinReminderEnabled) {
                    ProteinReminderScheduler.schedule(context, settings.proteinReminderHour)
                }
            }
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = settingsRepo.settingsFlow.first()
                if (!settings.proteinReminderEnabled) return@launch

                val now = System.currentTimeMillis()
                val (startTs, endTs) = ProteinRepository.getDayWindow(now, settings.dayRolloverHour)
                val entries = container.database.proteinDao().getEntriesBetweenSync(startTs, endTs)
                val totalGrams = entries.sumOf { it.grams }

                if (totalGrams < settings.proteinTarget) {
                    val remaining = settings.proteinTarget - totalGrams
                    showNotification(context, totalGrams, remaining, settings.proteinTarget)
                }

                // Reschedule for next day
                ProteinReminderScheduler.schedule(context, settings.proteinReminderHour)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showNotification(context: Context, currentGrams: Double, remainingGrams: Double, targetGrams: Double) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Protein Goal Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily reminders when protein intake is below target"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("NAVIGATE_TO", "protein")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Protein Goal • ${remainingGrams.toInt()}g Remaining")
            .setContentText("You've logged ${currentGrams.toInt()}g of ${targetGrams.toInt()}g today.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
