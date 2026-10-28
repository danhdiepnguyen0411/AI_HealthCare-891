package com.example.healthcareai.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.healthcareai.data.model.HealthRecordItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class HealthRecordRepository private constructor(context: Context) {

    private val appContext: Context = context.applicationContext
    private val prefs: SharedPreferences =
        appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val PREF_NAME = "prefs_health_records"
        private const val KEY_RECORDS_LIST = "key_records_list"

        @Volatile
        private var instance: HealthRecordRepository? = null

        fun getInstance(context: Context): HealthRecordRepository {
            return instance ?: synchronized(this) {
                instance ?: HealthRecordRepository(context.applicationContext).also { instance = it }
            }
        }
    }

    @Synchronized
    fun getAllRecords(): List<HealthRecordItem> {
        val json = prefs.getString(KEY_RECORDS_LIST, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<HealthRecordItem>>() {}.type
            gson.fromJson<List<HealthRecordItem>>(json, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    @Synchronized
    fun getRecordsForProfile(profileId: String?): List<HealthRecordItem> {
        if (profileId.isNullOrEmpty()) return getAllRecords()
        val primaryProfileId = ProfileRepository.getInstance(appContext).getPrimaryProfile().id
        val isPrimary = (profileId == primaryProfileId)
        return getAllRecords().filter { item ->
            if (isPrimary) {
                item.profileId.isNullOrEmpty() || item.profileId == profileId
            } else {
                item.profileId == profileId
            }
        }
    }

    @Synchronized
    fun getRecordsByCategory(category: String, profileId: String? = null): List<HealthRecordItem> {
        val target = category.trim().lowercase()
        val records = if (profileId.isNullOrEmpty()) getAllRecords() else getRecordsForProfile(profileId)
        return records.filter { item ->
            val cat = item.category.trim().lowercase()
            when {
                cat == target -> true
                (target == "medication" || target == "medications") && (cat == "medication" || cat == "medications") -> true
                (target == "allergy" || target == "allergies") && (cat == "allergy" || cat == "allergies") -> true
                (target == "condition" || target == "conditions") && (cat == "condition" || cat == "conditions") -> true
                (target == "vaccination" || target == "vaccinations") && (cat == "vaccination" || cat == "vaccinations") -> true
                else -> false
            }
        }
    }

    @Synchronized
    fun saveRecord(record: HealthRecordItem) {
        val current = getAllRecords().toMutableList()
        val index = current.indexOfFirst { it.id == record.id }
        if (index >= 0) {
            current[index] = record
        } else {
            current.add(0, record)
        }
        val json = gson.toJson(current)
        prefs.edit().putString(KEY_RECORDS_LIST, json).apply()
    }

    @Synchronized
    fun deleteRecord(id: String) {
        val current = getAllRecords().toMutableList()
        current.removeAll { it.id == id }
        val json = gson.toJson(current)
        prefs.edit().putString(KEY_RECORDS_LIST, json).apply()
    }

    @Synchronized
    fun deleteRecordsForProfile(profileId: String) {
        val current = getAllRecords().toMutableList()
        current.removeAll { it.profileId == profileId }
        val json = gson.toJson(current)
        prefs.edit().putString(KEY_RECORDS_LIST, json).apply()
    }
}
