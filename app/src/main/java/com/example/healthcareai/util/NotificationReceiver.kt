package com.example.healthcareai.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return

        when (action) {
            NotificationScheduler.ACTION_TRIGGER_NOTIFICATION -> {
                val slot = intent.getStringExtra(NotificationScheduler.EXTRA_SLOT)
                    ?: NotificationScheduler.SLOT_8H

                // Display localized notification with rotated content
                NotificationHelper.showScheduledNotification(context, slot)

                // Reschedule for next day at the exact same hour
                NotificationScheduler.scheduleSlot(context, slot)
            }

            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> {
                NotificationScheduler.scheduleDailyNotifications(context)
            }
        }
    }
}
