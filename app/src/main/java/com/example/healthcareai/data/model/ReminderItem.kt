package com.example.healthcareai.data.model

import java.io.Serializable
import java.util.UUID

data class ReminderItem(
    val id: String = UUID.randomUUID().toString(),
    var profileId: String? = null,
    var name: String = "",
    var category: String = "Medication",
    var strength: String = "",
    var form: String = "",
    var repeat: String = "Every day",
    var timeList: List<String> = emptyList(),
    var startDate: String = "",
    var endDate: String = "",
    var notes: String = "",
    var isDone: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) : Serializable {

    fun getDisplayDescription(): String {
        return when {
            strength.isNotBlank() && form.isNotBlank() -> "$strength · 1 $form"
            strength.isNotBlank() -> strength
            form.isNotBlank() -> form
            notes.isNotBlank() -> notes
            else -> ""
        }
    }

    fun getLocalizedName(context: android.content.Context): String {
        return com.example.healthcareai.util.ReminderLocalizationHelper.getLocalizedName(context, name)
    }

    fun getLocalizedDescription(context: android.content.Context): String {
        return com.example.healthcareai.util.ReminderLocalizationHelper.getLocalizedDescription(context, getDisplayDescription())
    }

    fun getDisplayTime(context: android.content.Context? = null): String {
        val firstTime = com.example.healthcareai.util.ReminderScheduler.sortTimeList(timeList).firstOrNull().orEmpty()
        return if (context != null) {
            com.example.healthcareai.util.LocaleHelper.formatTimeAmPm(context, firstTime)
        } else {
            firstTime
        }
    }

    fun getIconResId(): Int {
        val lowerCat = category.lowercase()
        val lowerName = name.lowercase()
        return when {
            lowerCat.contains("water") || lowerName.contains("water") -> com.example.healthcareai.R.drawable.ic_drink_water
            lowerCat.contains("exercise") || lowerName.contains("exercise") -> com.example.healthcareai.R.drawable.ic_do_exercise
            lowerCat.contains("sleep") || lowerName.contains("sleep") -> com.example.healthcareai.R.drawable.ic_sleep
            lowerCat.contains("temperature") || lowerName.contains("temp") -> com.example.healthcareai.R.drawable.ic_temperature
            lowerCat.contains("heart") -> com.example.healthcareai.R.drawable.ic_heart_rate
            lowerCat.contains("appointment") -> com.example.healthcareai.R.drawable.ic_appointments
            lowerCat.contains("vaccin") -> com.example.healthcareai.R.drawable.ic_vaccination
            else -> com.example.healthcareai.R.drawable.ic_medications
        }
    }

    fun isScheduledForDay(targetCal: java.util.Calendar, context: android.content.Context? = null): Boolean {
        val target = java.util.Calendar.getInstance().apply {
            set(targetCal.get(java.util.Calendar.YEAR), targetCal.get(java.util.Calendar.MONTH), targetCal.get(java.util.Calendar.DAY_OF_MONTH), 0, 0, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }

        if (startDate.isNotBlank()) {
            val startCal = com.example.healthcareai.util.LocaleHelper.parseStoredDate(startDate)
            if (startCal != null) {
                val start = java.util.Calendar.getInstance().apply {
                    time = startCal
                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }
                if (target.before(start)) return false
            }
        }

        if (endDate.isNotBlank()) {
            val endCal = com.example.healthcareai.util.LocaleHelper.parseStoredDate(endDate)
            if (endCal != null) {
                val end = java.util.Calendar.getInstance().apply {
                    time = endCal
                    set(java.util.Calendar.HOUR_OF_DAY, 23)
                    set(java.util.Calendar.MINUTE, 59)
                    set(java.util.Calendar.SECOND, 59)
                    set(java.util.Calendar.MILLISECOND, 999)
                }
                if (target.after(end)) return false
            }
        }

        val lowerRepeat = repeat.trim().lowercase()
        val dayOfWeek = target.get(java.util.Calendar.DAY_OF_WEEK)

        // 1. Once / Một lần: ONLY scheduled on the exact day it was set!
        if (lowerRepeat.contains("once") || lowerRepeat.contains("một lần") || lowerRepeat == "1 lần") {
            val compareDate = if (startDate.isNotBlank()) {
                com.example.healthcareai.util.LocaleHelper.parseStoredDate(startDate)
            } else if (createdAt > 0L) {
                java.util.Date(createdAt)
            } else null

            if (compareDate != null) {
                val cCal = java.util.Calendar.getInstance().apply { time = compareDate }
                return cCal.get(java.util.Calendar.YEAR) == target.get(java.util.Calendar.YEAR) &&
                       cCal.get(java.util.Calendar.DAY_OF_YEAR) == target.get(java.util.Calendar.DAY_OF_YEAR)
            }
            return false
        }

        // 2. Every day / Hàng ngày
        if (lowerRepeat.contains("every day") || lowerRepeat.contains("daily") || lowerRepeat.contains("hằng ngày") || lowerRepeat.contains("mỗi ngày")) {
            return true
        }

        // 3. Weekdays / Thứ 2 đến thứ 6
        if (lowerRepeat.contains("weekday") || lowerRepeat.contains("thứ 2 đến thứ 6") || lowerRepeat.contains("các ngày trong tuần") || lowerRepeat.contains("ngày trong tuần") || lowerRepeat.contains("monday to friday")) {
            return dayOfWeek in java.util.Calendar.MONDAY..java.util.Calendar.FRIDAY
        }

        // 4. Custom / Tùy chỉnh (e.g. "Custom:2,4,6")
        if (lowerRepeat.startsWith("custom:")) {
            val csv = repeat.substringAfter(":").trim()
            val allowedDays = csv.split(",").mapNotNull { it.trim().toIntOrNull() }
            if (allowedDays.isNotEmpty()) {
                return allowedDays.contains(dayOfWeek)
            }
        }

        // Fallbacks for legacy options
        if (lowerRepeat.contains("weekend") || lowerRepeat.contains("cuối tuần")) {
            return dayOfWeek == java.util.Calendar.SATURDAY || dayOfWeek == java.util.Calendar.SUNDAY
        }

        return true
    }
}
