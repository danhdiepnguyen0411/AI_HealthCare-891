package com.example.healthcareai.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.healthcareai.data.model.ReminderItem
import com.example.healthcareai.data.repository.ReminderRepository
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object ReminderScheduler {

    private const val TAG = "ReminderScheduler"

    const val ACTION_TRIGGER_REMINDER = "com.example.healthcareai.ACTION_TRIGGER_REMINDER"
    const val ACTION_MARK_REMINDER_DONE = "com.example.healthcareai.ACTION_MARK_REMINDER_DONE"

    const val EXTRA_REMINDER_ID = "extra_reminder_id"
    const val EXTRA_TIME_INDEX = "extra_time_index"
    const val EXTRA_TIME_STR = "extra_time_str"
    const val EXTRA_REMINDER_NAME = "extra_reminder_name"
    const val EXTRA_REMINDER_DESC = "extra_reminder_desc"
    const val EXTRA_REMINDER_CATEGORY = "extra_reminder_category"
    const val EXTRA_REMINDER_REPEAT = "extra_reminder_repeat"

    /**
     * Parses time strings into 24-hour (hour, minute) pair.
     * Supports formats like "8:00 AM", "08:30 PM", "14:00", etc.
     */
    fun parseTimeString(timeStr: String): Pair<Int, Int>? {
        val trimmed = timeStr.trim()
        if (trimmed.isEmpty()) return null

        val formats = listOf(
            SimpleDateFormat("h:mm a", Locale.US),
            SimpleDateFormat("hh:mm a", Locale.US),
            SimpleDateFormat("H:mm", Locale.US),
            SimpleDateFormat("HH:mm", Locale.US)
        )
        for (fmt in formats) {
            try {
                val date = fmt.parse(trimmed)
                if (date != null) {
                    val cal = Calendar.getInstance().apply { time = date }
                    return Pair(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
                }
            } catch (_: Exception) {}
        }

        // Manual regex/split fallback
        try {
            val parts = trimmed.split(" ")
            val timeParts = parts[0].split(":")
            var hour = timeParts[0].toInt()
            val minute = timeParts[1].toInt()
            if (parts.size > 1) {
                val amPm = parts[1].uppercase(Locale.US)
                if (amPm == "PM" && hour < 12) hour += 12
                if (amPm == "AM" && hour == 12) hour = 0
            }
            return Pair(hour, minute)
        } catch (_: Exception) {}

        return null
    }

    /**
     * Calculates the next trigger timestamp in milliseconds for the given time and repeat schedule.
     */
    fun calculateNextTriggerMillis(hour: Int, minute: Int, repeat: String): Long? {
        val now = System.currentTimeMillis()
        val target = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        when (repeat.lowercase(Locale.US)) {
            "once" -> {
                if (target.timeInMillis <= now) {
                    // Passed for today, not repeating
                    return null
                }
                return target.timeInMillis
            }
            "weekdays" -> {
                if (target.timeInMillis <= now) {
                    target.add(Calendar.DAY_OF_YEAR, 1)
                }
                while (target.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY ||
                    target.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                    target.add(Calendar.DAY_OF_YEAR, 1)
                }
                return target.timeInMillis
            }
            "weekends" -> {
                if (target.timeInMillis <= now) {
                    target.add(Calendar.DAY_OF_YEAR, 1)
                }
                while (target.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY &&
                    target.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
                    target.add(Calendar.DAY_OF_YEAR, 1)
                }
                return target.timeInMillis
            }
            "weekly" -> {
                if (target.timeInMillis <= now) {
                    target.add(Calendar.DAY_OF_YEAR, 7)
                }
                return target.timeInMillis
            }
            "monthly" -> {
                if (target.timeInMillis <= now) {
                    target.add(Calendar.MONTH, 1)
                }
                return target.timeInMillis
            }
            else -> {
                // "Every day" or default
                if (target.timeInMillis <= now) {
                    target.add(Calendar.DAY_OF_YEAR, 1)
                }
                return target.timeInMillis
            }
        }
    }

    /**
     * Unique stable request code per reminder and time index.
     */
    fun getRequestCode(reminderId: String, timeIndex: Int): Int {
        return (reminderId.hashCode() * 31 + timeIndex) and 0x7FFFFFFF
    }

    /**
     * Schedules alarms for all times specified in the reminder.
     * Guaranteed to fire even when the application process is completely killed.
     */
    fun scheduleReminder(context: Context, reminder: ReminderItem) {
        val times = reminder.timeList.ifEmpty {
            listOf("8:00 AM")
        }

        times.forEachIndexed { index, timeStr ->
            scheduleReminderTime(context, reminder, index, timeStr)
        }
    }

    /**
     * Schedules a single time slot for a reminder using AlarmManager RTC_WAKEUP.
     */
    fun scheduleReminderTime(
        context: Context,
        reminder: ReminderItem,
        timeIndex: Int,
        timeStr: String
    ) {
        val (hour, minute) = parseTimeString(timeStr) ?: return
        val triggerMillis = calculateNextTriggerMillis(hour, minute, reminder.repeat) ?: return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = ACTION_TRIGGER_REMINDER
            putExtra(EXTRA_REMINDER_ID, reminder.id)
            putExtra(EXTRA_TIME_INDEX, timeIndex)
            putExtra(EXTRA_TIME_STR, timeStr)
            putExtra(EXTRA_REMINDER_NAME, reminder.name)
            putExtra(EXTRA_REMINDER_DESC, reminder.getDisplayDescription())
            putExtra(EXTRA_REMINDER_CATEGORY, reminder.category)
            putExtra(EXTRA_REMINDER_REPEAT, reminder.repeat)
        }

        val requestCode = getRequestCode(reminder.id, timeIndex)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                alarmManager.canScheduleExactAlarms()
            } else {
                true
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (canExact) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pendingIntent
                )
            }
            Log.d(TAG, "Scheduled reminder '${reminder.name}' at $triggerMillis (slot: $timeStr, requestCode: $requestCode)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule alarm for reminder '${reminder.id}' at $timeStr", e)
        }
    }

    /**
     * Reschedules the next recurrence after an alarm fired in NotificationReceiver.
     */
    fun rescheduleNextOccurrence(
        context: Context,
        reminderId: String,
        timeIndex: Int,
        timeStr: String,
        repeat: String
    ) {
        if (repeat.equals("once", ignoreCase = true)) {
            return
        }

        val repo = ReminderRepository.getInstance(context)
        val reminder = repo.getReminder(reminderId)
        val effectiveReminder = reminder ?: ReminderItem(
            id = reminderId,
            name = "Reminder",
            repeat = repeat,
            timeList = listOf(timeStr)
        )

        scheduleReminderTime(context, effectiveReminder, timeIndex, timeStr)
    }

    /**
     * Cancels all scheduled alarms for the given reminder.
     */
    fun cancelReminder(context: Context, reminderId: String, maxTimes: Int = 10) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        for (i in 0 until maxTimes) {
            val intent = Intent(context, NotificationReceiver::class.java).apply {
                action = ACTION_TRIGGER_REMINDER
            }
            val requestCode = getRequestCode(reminderId, i)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
        Log.d(TAG, "Cancelled alarms for reminder: $reminderId")
    }

    /**
     * Reschedules all saved reminders. Called after device reboot, package update, or time changes.
     */
    fun rescheduleAllReminders(context: Context) {
        val repo = ReminderRepository.getInstance(context)
        val list = repo.getAllReminders()
        Log.d(TAG, "Rescheduling ${list.size} reminders on system broadcast/app launch")
        for (item in list) {
            scheduleReminder(context, item)
        }
    }
}
