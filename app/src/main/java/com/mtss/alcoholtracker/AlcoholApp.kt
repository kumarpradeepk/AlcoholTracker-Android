package com.mtss.alcoholtracker

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.mtss.alcoholtracker.domain.AlcoholMath
import com.mtss.alcoholtracker.domain.UnitsConfig
import com.mtss.alcoholtracker.util.Formatters

class AlcoholApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Locale-sensitive singletons, bound before any UI or notification runs.
        Formatters.bind(this)
        AlcoholMath.bind(UnitsConfig.countryFor(this))
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_REMINDERS,
                getString(R.string.notif_channel_reminders),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = getString(R.string.notif_channel_reminders_desc)
                setShowBadge(false)
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_BAC,
                getString(R.string.notif_channel_bac),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notif_channel_bac_desc)
                setShowBadge(false)
            }
        )
    }

    companion object {
        const val CHANNEL_REMINDERS = "reminders"
        const val CHANNEL_BAC = "bac_status"
    }
}
