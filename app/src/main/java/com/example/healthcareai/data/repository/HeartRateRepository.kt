package com.example.healthcareai.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.healthcareai.data.model.HrvResult
import com.example.healthcareai.data.model.SignalQuality
import org.json.JSONArray
import org.json.JSONObject

/**
 * Model representing a recorded heart rate measurement item.
 */
data class HeartRateRecord(
    val id: String = java.util.UUID.randomUUID().toString(),
    val bpm: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val hrvRmssd: Double? = null,
    val hrvSdnn: Double? = null,
    val quality: SignalQuality = SignalQuality.GOOD
)

/**
 * Repository to persist and retrieve heart rate measurements.
 */
class HeartRateRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "app_healthcare_heart_rate"
        private const val KEY_RECORDS_JSON = "key_heart_rate_records_json"
        private const val KEY_LATEST_BPM = "key_latest_heart_rate_bpm"

        @Volatile
        private var instance: HeartRateRepository? = null

        fun getInstance(context: Context): HeartRateRepository {
            return instance ?: synchronized(this) {
                val safeContext = context.applicationContext ?: context
                instance ?: HeartRateRepository(safeContext).also { instance = it }
            }
        }
    }

    /**
     * Saves a completed measurement.
     */
    fun saveMeasurement(bpm: Int, hrv: HrvResult?, quality: SignalQuality): HeartRateRecord {
        val record = HeartRateRecord(
            bpm = bpm,
            timestamp = System.currentTimeMillis(),
            hrvRmssd = hrv?.rmssd,
            hrvSdnn = hrv?.sdnn,
            quality = quality
        )

        val records = getAllRecords().toMutableList()
        records.add(0, record) // Insert newest first

        // Keep last 100 records
        val trimmed = if (records.size > 100) records.subList(0, 100) else records
        val jsonArray = JSONArray()
        for (item in trimmed) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("bpm", item.bpm)
                put("timestamp", item.timestamp)
                if (item.hrvRmssd != null) put("hrvRmssd", item.hrvRmssd)
                if (item.hrvSdnn != null) put("hrvSdnn", item.hrvSdnn)
                put("quality", item.quality.name)
            }
            jsonArray.put(obj)
        }

        prefs.edit()
            .putString(KEY_RECORDS_JSON, jsonArray.toString())
            .putInt(KEY_LATEST_BPM, bpm)
            .apply()

        return record
    }

    /**
     * Returns all historical records, sorted newest first.
     */
    fun getAllRecords(): List<HeartRateRecord> {
        val rawJson = prefs.getString(KEY_RECORDS_JSON, null) ?: return emptyList()
        val result = mutableListOf<HeartRateRecord>()
        try {
            val jsonArray = JSONArray(rawJson)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val qualityStr = obj.optString("quality", SignalQuality.GOOD.name)
                val quality = try {
                    SignalQuality.valueOf(qualityStr)
                } catch (e: Exception) {
                    SignalQuality.GOOD
                }

                result.add(
                    HeartRateRecord(
                        id = obj.optString("id"),
                        bpm = obj.getInt("bpm"),
                        timestamp = obj.getLong("timestamp"),
                        hrvRmssd = if (obj.has("hrvRmssd")) obj.getDouble("hrvRmssd") else null,
                        hrvSdnn = if (obj.has("hrvSdnn")) obj.getDouble("hrvSdnn") else null,
                        quality = quality
                    )
                )
            }
        } catch (e: Exception) {
            // Ignore parse errors
        }
        return result
    }

    /**
     * Deletes a record by its unique ID.
     */
    fun deleteRecord(id: String): Boolean {
        val records = getAllRecords().toMutableList()
        val removed = records.removeAll { it.id == id }
        if (!removed) return false

        val jsonArray = JSONArray()
        for (item in records) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("bpm", item.bpm)
                put("timestamp", item.timestamp)
                if (item.hrvRmssd != null) put("hrvRmssd", item.hrvRmssd)
                if (item.hrvSdnn != null) put("hrvSdnn", item.hrvSdnn)
                put("quality", item.quality.name)
            }
            jsonArray.put(obj)
        }

        val latestBpm = records.firstOrNull()?.bpm ?: 0
        prefs.edit()
            .putString(KEY_RECORDS_JSON, jsonArray.toString())
            .putInt(KEY_LATEST_BPM, latestBpm)
            .apply()
        return true
    }

    /**
     * Returns the latest measured BPM, or 0 if none.
     */
    fun getLatestBpm(): Int = prefs.getInt(KEY_LATEST_BPM, 0)
}
