package com.example.healthcareai.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.healthcareai.data.model.ReminderItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class ReminderRepository private constructor(context: Context) {

    private val appContext: Context = context.applicationContext
    private val prefs: SharedPreferences =
        appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val PREF_NAME = "prefs_health_reminders"
        private const val KEY_REMINDERS_LIST = "key_reminders_list"

        @Volatile
        private var instance: ReminderRepository? = null

        fun getInstance(context: Context): ReminderRepository {
            return instance ?: synchronized(this) {
                instance ?: ReminderRepository(context.applicationContext).also { instance = it }
            }
        }
    }

    init {
        // Ensure no fake/mock reminders exist
        val mockIds = setOf("reminder_cetirizine", "reminder_water", "reminder_exercise")
        val current = getAllReminders()
        if (current.any { it.id in mockIds }) {
            val cleaned = current.filterNot { it.id in mockIds }
            saveList(cleaned)
        }
    }

    @Synchronized
    fun getAllReminders(): List<ReminderItem> {
        val json = prefs.getString(KEY_REMINDERS_LIST, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<ReminderItem>>() {}.type
            gson.fromJson<List<ReminderItem>>(json, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    @Synchronized
    fun getRemindersForProfile(profileId: String?): List<ReminderItem> {
        if (profileId.isNullOrEmpty()) return getAllReminders()
        val primaryId = ProfileRepository.getInstance(appContext).getPrimaryProfile().id
        val isPrimary = (profileId == primaryId)
        return getAllReminders().filter { reminder ->
            if (isPrimary) {
                reminder.profileId.isNullOrEmpty() || reminder.profileId == profileId
            } else {
                reminder.profileId == profileId
            }
        }
    }

    @Synchronized
    fun getReminder(id: String): ReminderItem? {
        return getAllReminders().firstOrNull { it.id == id }
    }

    @Synchronized
    fun saveReminder(reminder: ReminderItem) {
        val currentList = getAllReminders().toMutableList()
        val index = currentList.indexOfFirst { it.id == reminder.id }
        if (index >= 0) {
            currentList[index] = reminder
        } else {
            currentList.add(0, reminder)
        }
        saveList(currentList)
    }

    @Synchronized
    fun deleteReminder(id: String) {
        val currentList = getAllReminders().toMutableList()
        currentList.removeAll { it.id == id }
        saveList(currentList)
    }

    @Synchronized
    fun deleteRemindersForProfile(profileId: String) {
        val currentList = getAllReminders().toMutableList()
        currentList.removeAll { it.profileId == profileId }
        saveList(currentList)
    }

    @Synchronized
    fun setReminderDone(id: String, isDone: Boolean) {
        val currentList = getAllReminders().toMutableList()
        val index = currentList.indexOfFirst { it.id == id }
        if (index >= 0) {
            currentList[index].isDone = isDone
            saveList(currentList)
        }
    }

    @Synchronized
    fun toggleReminderDone(id: String): Boolean {
        val currentList = getAllReminders().toMutableList()
        val index = currentList.indexOfFirst { it.id == id }
        if (index >= 0) {
            val newStatus = !currentList[index].isDone
            currentList[index].isDone = newStatus
            saveList(currentList)
            return newStatus
        }
        return false
    }

    private fun saveList(list: List<ReminderItem>) {
        val json = gson.toJson(list)
        prefs.edit().putString(KEY_REMINDERS_LIST, json).apply()
    }
}
