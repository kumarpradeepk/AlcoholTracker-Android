package com.mtss.alcoholtracker.notifications

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.mtss.alcoholtracker.AlcoholApp
import com.mtss.alcoholtracker.MainActivity
import com.mtss.alcoholtracker.R
import com.mtss.alcoholtracker.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra("id") ?: return
        val title = intent.getStringExtra("title") ?: context.getString(R.string.notif_discreet_title)
        val message = intent.getStringExtra("message") ?: ""
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = SettingsRepository.get(context).flow.first()
                // Discretion by default: the lock screen never names alcohol.
                val shownTitle: String
                val shownBody: String
                if (settings.discreet) {
                    shownTitle = context.getString(R.string.notif_discreet_title)
                    shownBody = context.getString(R.string.notif_discreet_body)
                } else {
                    shownTitle = title
                    shownBody = message.ifEmpty {
                        context.getString(R.string.notif_reminder_body_fallback)
                    }
                }
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED || android.os.Build.VERSION.SDK_INT < 33
                ) {
                    val open = android.app.PendingIntent.getActivity(
                        context, 0, Intent(context, MainActivity::class.java),
                        android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                    )
                    val n = NotificationCompat.Builder(context, AlcoholApp.CHANNEL_REMINDERS)
                        .setSmallIcon(R.drawable.ic_droplet)
                        .setContentTitle(shownTitle)
                        .setContentText(shownBody)
                        .setContentIntent(open)
                        .setAutoCancel(true)
                        .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                        .setPublicVersion(
                            NotificationCompat.Builder(context, AlcoholApp.CHANNEL_REMINDERS)
                                .setSmallIcon(R.drawable.ic_droplet)
                                .setContentTitle(context.getString(R.string.notif_discreet_title))
                                .setContentText(context.getString(R.string.notif_discreet_body))
                                .build()
                        )
                        .build()
                    NotificationManagerCompat.from(context).notify(id.hashCode(), n)
                }
            } catch (_: SecurityException) {
                // Permission revoked between the check and the notify — nothing to do.
            } finally {
                pending.finish()
            }
        }
    }
}
