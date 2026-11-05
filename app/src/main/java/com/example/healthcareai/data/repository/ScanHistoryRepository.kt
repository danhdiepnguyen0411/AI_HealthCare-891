package com.example.healthcareai.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.healthcareai.data.model.ScanHistoryItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class ScanHistoryRepository private constructor(context: Context) {

    private val appContext: Context = context.applicationContext
    private val prefs: SharedPreferences =
        appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val PREF_NAME = "prefs_scan_history"
        private const val KEY_SCANS_LIST = "key_scans_list"

        @Volatile
        private var instance: ScanHistoryRepository? = null

        fun getInstance(context: Context): ScanHistoryRepository {
            return instance ?: synchronized(this) {
                instance ?: ScanHistoryRepository(context.applicationContext).also { instance = it }
            }
        }
    }

    @Synchronized
    fun getAllScans(): List<ScanHistoryItem> {
        val json = prefs.getString(KEY_SCANS_LIST, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<ScanHistoryItem>>() {}.type
            gson.fromJson<List<ScanHistoryItem>>(json, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    @Synchronized
    fun getScansForProfile(profileId: String?): List<ScanHistoryItem> {
        if (profileId.isNullOrEmpty()) return getAllScans()
        val primaryProfileId = ProfileRepository.getInstance(appContext).getPrimaryProfile().id
        val isPrimary = (profileId == primaryProfileId)
        return getAllScans().filter { item ->
            if (isPrimary) {
                item.profileId.isNullOrEmpty() || item.profileId == profileId
            } else {
                item.profileId == profileId
            }
        }
    }

    @Synchronized
    fun getScanById(id: String): ScanHistoryItem? {
        return getAllScans().firstOrNull { it.id == id }
    }

    @Synchronized
    fun findScanByImagePath(imagePath: String?): ScanHistoryItem? {
        if (imagePath.isNullOrEmpty()) return null
        return getAllScans().firstOrNull { it.imagePath == imagePath }
    }

    @Synchronized
    fun updateConversationId(scanId: String, conversationId: String) {
        val current = getAllScans().toMutableList()
        val index = current.indexOfFirst { it.id == scanId }
        if (index >= 0) {
            current[index] = current[index].copy(conversationId = conversationId)
            val json = gson.toJson(current)
            prefs.edit().putString(KEY_SCANS_LIST, json).apply()
        }
    }

    @Synchronized
    fun saveScan(scan: ScanHistoryItem) {
        val current = getAllScans().toMutableList()
        val index = current.indexOfFirst { it.id == scan.id }
        if (index >= 0) {
            current[index] = scan
        } else {
            current.add(0, scan)
        }
        val json = gson.toJson(current)
        prefs.edit().putString(KEY_SCANS_LIST, json).apply()
    }

    @Synchronized
    fun deleteScan(id: String) {
        val current = getAllScans().toMutableList()
        current.removeAll { it.id == id }
        val json = gson.toJson(current)
        prefs.edit().putString(KEY_SCANS_LIST, json).apply()
    }

    @Synchronized
    fun deleteScansForProfile(profileId: String) {
        val current = getAllScans().toMutableList()
        current.removeAll { it.profileId == profileId }
        val json = gson.toJson(current)
        prefs.edit().putString(KEY_SCANS_LIST, json).apply()
    }
}

