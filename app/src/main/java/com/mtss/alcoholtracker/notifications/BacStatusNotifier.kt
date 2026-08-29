package com.mtss.alcoholtracker.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.mtss.alcoholtracker.AlcoholApp
import com.mtss.alcoholtracker.MainActivity
import com.mtss.alcoholtracker.R
import com.mtss.alcoholtracker.domain.AlcoholMath
import com.mtss.alcoholtracker.util.LocaleText

/**
 * The Android analogue of the design's Live Activity: while an estimate is
 * settling, a silent ongoing notification shows the value and time to zero.
 * Discretion applies — with discreet notifications on, the copy never names
 * alcohol or shows the number on the lock screen.
 */
object BacStatusNotifier {

    private const val ID = 4242

    fun update(
        context: Context,
        estimate: AlcoholMath.BacEstimate?,
        enabled: Boolean,
        discreet: Boolean,
        unitPercent: Boolean
    ) {
        val nm = NotificationManagerCompat.from(context)
        if (estimate == null || !enabled || estimate.percent <= 0.002) {
            nm.cancel(ID)
            return
        }
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        val value =
            if (unitPercent) context.getString(R.string.bac_value_percent, estimate.percent)
            else context.getString(R.string.bac_value_permille, estimate.percent * 10)
        val toZero = AlcoholMath.formatHours(estimate.hoursToZero)

        val open = PendingIntent.getActivity(
            context, 1, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val discreetTitle = context.getString(R.string.notif_bac_discreet_title)
        val discreetBody = context.getString(R.string.notif_bac_discreet_body, toZero)
        val publicVersion = NotificationCompat.Builder(context, AlcoholApp.CHANNEL_BAC)
            .setSmallIcon(R.drawable.ic_droplet)
            .setContentTitle(discreetTitle)
            .setContentText(discreetBody)
            .build()
        val builder = NotificationCompat.Builder(context, AlcoholApp.CHANNEL_BAC)
            .setSmallIcon(R.drawable.ic_droplet)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(open)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicVersion)
        if (discreet) {
            builder.setContentTitle(discreetTitle)
            builder.setContentText(discreetBody)
        } else {
            // punch-list B5, safety-relevant. Two guarantees, both in code rather
            // than left to each translator to work around in copy:
            //
            // 1. The title's qualifier ("Estimated" / 추정 / ค่าประมาณ) is emitted
            //    before the number, so an end-truncated lock-screen title can
            //    never reduce an estimate to a bare reading.
            // 2. The collapsed body is the never-drive sentence, not the
            //    countdown. Whichever order a pack writes notif_bac_body in, the
            //    one line that always survives truncation is the warning; the
            //    countdown moves to the header sub-text and the expanded view.
            builder.setContentTitle(
                LocaleText.qualifierFirst(context, R.string.notif_bac_title, value)
            )
            builder.setContentText(context.getString(R.string.bac_disclaimer_short))
            builder.setSubText(toZero)
            builder.setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(context.getString(R.string.notif_bac_body, toZero))
            )
        }
        try {
            nm.notify(ID, builder.build())
        } catch (_: SecurityException) {
        }
    }
}
