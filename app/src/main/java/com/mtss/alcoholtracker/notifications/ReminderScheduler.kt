package com.mtss.alcoholtracker.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.mtss.alcoholtracker.data.ReminderItem
import java.util.Calendar

/**
 * Daily reminders via inexact repeating alarms. A gentle nudge does not need
 * exact-alarm privileges — inexact keeps us clear of SCHEDULE_EXACT_ALARM
 * friction and battery policies while still landing in the right minutes.
 */
object ReminderScheduler {

    fun schedule(context: Context, item: ReminderItem) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val at = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, item.timeMinutes / 60)
            set(Calendar.MINUTE, item.timeMinutes % 60)
            set(Calendar.SECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        am.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            at.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pending(context, item)
        )
    }

    fun cancel(context: Context, item: ReminderItem) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pending(context, item))
    }

    fun rescheduleAll(context: Context, items: List<ReminderItem>) {
        items.forEach { schedule(context, it) }
    }

    private fun pending(context: Context, item: ReminderItem): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("id", item.id)
            putExtra("title", item.title)
            putExtra("message", item.message)
        }
        return PendingIntent.getBroadcast(
            context,
            item.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
