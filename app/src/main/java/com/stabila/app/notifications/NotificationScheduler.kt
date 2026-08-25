package com.stabila.app.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun scheduleDailyReminder(timeString: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("EXTRA_TIME_STRING", timeString)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REMINDER_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Parse time string like "09:00 AM"
        val (hourOfDay, minute) = parseTimeString(timeString)
        
        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, hourOfDay)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            
            // If the time has already passed today, schedule for tomorrow
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        try {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    fun cancelReminder() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REMINDER_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
    
    private fun parseTimeString(timeString: String): Pair<Int, Int> {
        return try {
            val parts = timeString.split(" ")
            val timeParts = parts[0].split(":")
            var hour = timeParts[0].toInt()
            val minute = timeParts[1].toInt()
            val amPm = if (parts.size > 1) parts[1] else "AM"
            
            if (amPm.equals("PM", ignoreCase = true) && hour < 12) {
                hour += 12
            } else if (amPm.equals("AM", ignoreCase = true) && hour == 12) {
                hour = 0
            }
            Pair(hour, minute)
        } catch (e: Exception) {
            Pair(9, 0) // Fallback to 9 AM
        }
    }

    companion object {
        const val REMINDER_REQUEST_CODE = 1001
    }
}
