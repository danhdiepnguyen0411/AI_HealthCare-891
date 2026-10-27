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

    fun getDisplayTime(): String {
        return timeList.firstOrNull().orEmpty()
    }

    fun getIconResId(): Int {
        val lowerCat = category.lowercase()
        val lowerName = name.lowercase()
        return when {
            lowerCat.contains("water") || lowerName.contains("water") -> com.example.healthcareai.R.drawable.ic_drink_water
            lowerCat.contains("exercise") || lowerName.contains("exercise") || lowerCat.contains("workout") || lowerName.contains("workout") -> com.example.healthcareai.R.drawable.ic_do_exercise
            else -> com.example.healthcareai.R.drawable.ic_medications
        }
    }
}
