package com.stabila.app.notifications

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.stabila.app.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {
    
    @Inject lateinit var notificationScheduler: NotificationScheduler
    override fun onReceive(context: Context, intent: Intent) {
        // Build the intent to open the app when tapped
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, "daily_test_channel")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(context.getString(com.stabila.app.R.string.notification_reminder_title))
            .setContentText(context.getString(com.stabila.app.R.string.notification_reminder_body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            try {
                notify(NOTIFICATION_ID, builder.build())
            } catch (e: SecurityException) {
                // Permission not granted, ignore or log
            }
        }
        
        // Reschedule for the next day since exact alarms are one-shot
        val timeString = intent.getStringExtra("EXTRA_TIME_STRING")
        if (timeString != null) {
            notificationScheduler.scheduleDailyReminder(timeString)
        }
    }

    companion object {
        const val NOTIFICATION_ID = 1002
    }
}
