package com.mtss.alcoholtracker.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mtss.alcoholtracker.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Alarms do not survive reboots or updates; re-arm every reminder. */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val items = AppDatabase.get(context).dao().remindersOnce()
                ReminderScheduler.rescheduleAll(context, items)
            } finally {
                pending.finish()
            }
        }
    }
}
