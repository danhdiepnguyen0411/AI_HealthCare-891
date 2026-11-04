package com.example.healthcareai.util

import android.content.Context
import com.example.healthcareai.R

object ReminderLocalizationHelper {

    fun getLocalizedName(context: Context, rawName: String): String {
        if (rawName.isBlank()) return rawName
        val lower = rawName.trim().lowercase()
        return when {
            lower == "drink water" || lower == "water" -> context.getString(R.string.reminder_water_title)
            lower == "check temperature" || lower == "temperature" -> context.getString(R.string.reminder_check_temperature_title)
            lower.contains("observe") || lower.contains("skin care") -> context.getString(R.string.reminder_observe_area_title)
            lower == "cetirizine" -> context.getString(R.string.reminder_cetirizine_title)
            else -> rawName
        }
    }

    fun getLocalizedDescription(context: Context, rawDesc: String): String {
        if (rawDesc.isBlank()) return rawDesc
        val lower = rawDesc.trim().lowercase()
        return when {
            lower.contains("hydration accelerates") -> context.getString(R.string.reminder_water_notes)
            lower.contains("monitor temperature for early fever") -> context.getString(R.string.reminder_check_temperature_notes)
            lower.contains("check for any changes in redness") -> context.getString(R.string.reminder_observe_area_notes)
            else -> rawDesc
        }
    }

    fun getLocalizedRepeat(context: Context, repeatKey: String): String {
        val lower = repeatKey.trim().lowercase()
        return when (lower) {
            "every day", "daily", "hằng ngày", "mỗi ngày" -> context.getString(R.string.repeat_every_day)
            "weekdays", "các ngày trong tuần" -> context.getString(R.string.repeat_weekdays)
            "weekends", "cuối tuần" -> context.getString(R.string.repeat_weekends)
            "weekly", "hàng tuần" -> context.getString(R.string.repeat_weekly)
            "monthly", "hàng tháng" -> context.getString(R.string.repeat_monthly)
            "once", "một lần" -> context.getString(R.string.repeat_once)
            else -> repeatKey
        }
    }

    fun getLocalizedSchedule(context: Context, scheduleDisplay: String): String {
        if (scheduleDisplay.isBlank()) return scheduleDisplay
        val lower = scheduleDisplay.trim().lowercase()
        return when {
            lower == "choose times" -> context.getString(R.string.hint_repeat_schedule)
            scheduleDisplay.contains("Daily", ignoreCase = true) -> {
                val dailyLocalized = context.getString(R.string.repeat_every_day)
                scheduleDisplay.replace("Daily", dailyLocalized, ignoreCase = true)
            }
            else -> scheduleDisplay
        }
    }
}

