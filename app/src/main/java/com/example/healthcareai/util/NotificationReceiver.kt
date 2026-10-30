package com.example.healthcareai.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.example.healthcareai.data.repository.ReminderRepository

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        Log.d("NotificationReceiver", "Received broadcast action: $action")

        when (action) {
            // 1. Daily localized system health tips (8h, 15h, 21h)
            NotificationScheduler.ACTION_TRIGGER_NOTIFICATION -> {
                val slot = intent.getStringExtra(NotificationScheduler.EXTRA_SLOT)
                    ?: NotificationScheduler.SLOT_8H

                // Display localized notification with rotated content
                NotificationHelper.showScheduledNotification(context, slot)

                // Reschedule for next day at the exact same hour
                NotificationScheduler.scheduleSlot(context, slot)
            }

            // 2. Specific user reminders (Medication, Water, Appointments, etc.)
            ReminderScheduler.ACTION_TRIGGER_REMINDER -> {
                val reminderId = intent.getStringExtra(ReminderScheduler.EXTRA_REMINDER_ID) ?: return
                val timeIndex = intent.getIntExtra(ReminderScheduler.EXTRA_TIME_INDEX, 0)
                val timeStr = intent.getStringExtra(ReminderScheduler.EXTRA_TIME_STR) ?: ""
                val name = intent.getStringExtra(ReminderScheduler.EXTRA_REMINDER_NAME) ?: "Reminder"
                val desc = intent.getStringExtra(ReminderScheduler.EXTRA_REMINDER_DESC) ?: ""
                val category = intent.getStringExtra(ReminderScheduler.EXTRA_REMINDER_CATEGORY) ?: "Health"
                val repeat = intent.getStringExtra(ReminderScheduler.EXTRA_REMINDER_REPEAT) ?: "Every day"

                // Show heads-up reminder notification to user
                NotificationHelper.showReminderNotification(
                    context = context,
                    reminderId = reminderId,
                    name = name,
                    description = desc,
                    category = category,
                    timeIndex = timeIndex
                )

                // Reschedule for next recurrence (e.g. tomorrow, next weekday, next week)
                ReminderScheduler.rescheduleNextOccurrence(
                    context = context,
                    reminderId = reminderId,
                    timeIndex = timeIndex,
                    timeStr = timeStr,
                    repeat = repeat
                )
            }

            // 3. Mark reminder done directly from notification action button
            ReminderScheduler.ACTION_MARK_REMINDER_DONE -> {
                val reminderId = intent.getStringExtra(ReminderScheduler.EXTRA_REMINDER_ID)
                val notiId = intent.getIntExtra("extra_notification_id", -1)
                if (notiId != -1) {
                    NotificationManagerCompat.from(context).cancel(notiId)
                }
                if (!reminderId.isNullOrEmpty()) {
                    ReminderRepository.getInstance(context).setReminderDone(reminderId, true)
                }
            }

            // 4. System events: device reboot, package updated, time/timezone changed
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            "android.intent.action.TIME_SET",
            Intent.ACTION_TIMEZONE_CHANGED -> {
                Log.d("NotificationReceiver", "System event $action: Rescheduling all alarms and daily reminders")
                NotificationScheduler.scheduleDailyNotifications(context)
                ReminderScheduler.rescheduleAllReminders(context)
            }
        }
    }
}
