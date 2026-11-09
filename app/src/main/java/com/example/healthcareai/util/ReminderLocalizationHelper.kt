package com.example.healthcareai.util

import android.content.Context
import com.example.healthcareai.R

object ReminderLocalizationHelper {

    fun getLocalizedName(context: Context, rawName: String): String {
        if (rawName.isBlank()) return rawName
        val lower = rawName.trim().lowercase()
        return when {
            lower == "heart rate" || lower == "heart_rate" || lower == "measure heart rate" || lower == "đo nhịp tim" || lower == "nhịp tim" -> {
                context.getString(R.string.title_heart_rate)
            }
            lower == "drink water" || lower == "water" || lower == "uống nước" -> {
                context.getString(R.string.reminder_water_title)
            }
            lower == "check temperature" || lower == "temperature" || lower == "nhiệt độ" || lower == "kiểm tra nhiệt độ" -> {
                context.getString(R.string.reminder_check_temperature_title)
            }
            lower == "exercise" || lower == "tập thể dục" -> {
                context.getString(R.string.cat_exercise)
            }
            lower == "sleep" || lower == "giấc ngủ" -> {
                context.getString(R.string.cat_sleep)
            }
            lower == "medication" || lower == "medicine" || lower == "thuốc" -> {
                context.getString(R.string.record_medications)
            }
            lower == "vaccination" || lower == "vaccine" || lower == "tiêm chủng" -> {
                context.getString(R.string.record_vaccinations)
            }
            lower == "appointments" || lower == "appointment" || lower == "lịch hẹn" -> {
                context.getString(R.string.category_appointments_title)
            }
            lower.contains("observe") || lower.contains("skin care") -> {
                context.getString(R.string.reminder_observe_area_title)
            }
            lower == "cetirizine" -> {
                context.getString(R.string.reminder_cetirizine_title)
            }
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
        return when {
            lower == "once" || lower == "một lần" || lower == "1 lần" -> {
                context.getString(R.string.repeat_once)
            }
            lower == "every day" || lower == "daily" || lower == "hằng ngày" || lower == "mỗi ngày" -> {
                context.getString(R.string.repeat_every_day)
            }
            lower == "weekdays" || lower == "thứ 2 đến thứ 6" || lower == "các ngày trong tuần" || lower == "ngày trong tuần" || lower == "monday to friday" -> {
                context.getString(R.string.repeat_weekdays)
            }
            lower == "custom" || lower == "tùy chỉnh" -> {
                context.getString(R.string.repeat_custom)
            }
            lower.startsWith("custom:") -> {
                formatCustomDays(context, repeatKey.substringAfter(":").trim())
            }
            lower == "weekends" || lower == "cuối tuần" -> context.getString(R.string.repeat_weekends)
            lower == "weekly" || lower == "hàng tuần" -> context.getString(R.string.repeat_weekly)
            lower == "monthly" || lower == "hàng tháng" -> context.getString(R.string.repeat_monthly)
            else -> repeatKey
        }
    }

    private fun formatCustomDays(context: Context, csv: String): String {
        val days = csv.split(",").mapNotNull { it.trim().toIntOrNull() }
        if (days.isEmpty()) return context.getString(R.string.repeat_custom)
        if (days.size == 7) return context.getString(R.string.repeat_every_day)
        if (days.size == 5 && !days.contains(java.util.Calendar.SATURDAY) && !days.contains(java.util.Calendar.SUNDAY)) {
            return context.getString(R.string.repeat_weekdays)
        }

        val isVi = LocaleHelper.getSavedLocale(context).language.lowercase().startsWith("vi")

        return days.mapNotNull { dayOfWeek ->
            when (dayOfWeek) {
                java.util.Calendar.MONDAY -> if (isVi) "Thứ 2" else "Mon"
                java.util.Calendar.TUESDAY -> if (isVi) "Thứ 3" else "Tue"
                java.util.Calendar.WEDNESDAY -> if (isVi) "Thứ 4" else "Wed"
                java.util.Calendar.THURSDAY -> if (isVi) "Thứ 5" else "Thu"
                java.util.Calendar.FRIDAY -> if (isVi) "Thứ 6" else "Fri"
                java.util.Calendar.SATURDAY -> if (isVi) "Thứ 7" else "Sat"
                java.util.Calendar.SUNDAY -> if (isVi) "Chủ nhật" else "Sun"
                else -> null
            }
        }.joinToString(", ")
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

