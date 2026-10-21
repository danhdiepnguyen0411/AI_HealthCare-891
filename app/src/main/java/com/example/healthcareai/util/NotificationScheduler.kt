package com.example.healthcareai.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.util.Calendar

object NotificationScheduler {

    private const val TAG = "NotificationScheduler"

    const val ACTION_TRIGGER_NOTIFICATION = "com.example.healthcareai.ACTION_TRIGGER_NOTIFICATION"
    const val EXTRA_SLOT = "extra_slot"

    const val SLOT_8H = "8h"
    const val SLOT_15H = "15h"
    const val SLOT_21H = "21h"

    const val REQUEST_CODE_8H = 8000
    const val REQUEST_CODE_15H = 15000
    const val REQUEST_CODE_21H = 21000

    private val ALL_SLOTS = listOf(SLOT_8H, SLOT_15H, SLOT_21H)

    fun scheduleDailyNotifications(context: Context) {
        ALL_SLOTS.forEach { slot ->
            scheduleSlot(context, slot)
        }
    }

    fun getNextTriggerMillis(hourOfDay: Int, minute: Int = 0): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, hourOfDay)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        return calendar.timeInMillis
    }

    fun scheduleSlot(context: Context, slot: String) {
        val (hour, requestCode) = when (slot) {
            SLOT_8H -> Pair(8, REQUEST_CODE_8H)
            SLOT_15H -> Pair(15, REQUEST_CODE_15H)
            SLOT_21H -> Pair(21, REQUEST_CODE_21H)
            else -> return
        }

        val triggerMillis = getNextTriggerMillis(hour, 0)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = ACTION_TRIGGER_NOTIFICATION
            putExtra(EXTRA_SLOT, slot)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule alarm for slot $slot", e)
        }
    }

    fun cancelDailyNotifications(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        ALL_SLOTS.forEach { slot ->
            val requestCode = when (slot) {
                SLOT_8H -> REQUEST_CODE_8H
                SLOT_15H -> REQUEST_CODE_15H
                SLOT_21H -> REQUEST_CODE_21H
                else -> return@forEach
            }
            val intent = Intent(context, NotificationReceiver::class.java).apply {
                action = ACTION_TRIGGER_NOTIFICATION
                putExtra(EXTRA_SLOT, slot)
            }
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
    }
}
