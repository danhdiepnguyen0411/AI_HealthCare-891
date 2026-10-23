package com.example.healthcareai.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.healthcareai.data.local.PreferenceHelper
import com.example.healthcareai.data.model.UserProfile
import org.json.JSONArray
import org.json.JSONObject

/**
 * Repository to persist and manage user & family profiles.
 */
class ProfileRepository private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val preferenceHelper = PreferenceHelper.getInstance(context)

    companion object {
        private const val PREF_NAME = "app_healthcare_profiles"
        private const val KEY_PROFILES_JSON = "key_profiles_json"

        @Volatile
        private var instance: ProfileRepository? = null

        fun getInstance(context: Context): ProfileRepository {
            return instance ?: synchronized(this) {
                val safeContext = context.applicationContext ?: context
                instance ?: ProfileRepository(safeContext).also { instance = it }
            }
        }
    }

    /**
     * Saves or updates the primary profile and syncs with PreferenceHelper.
     */
    fun savePrimaryProfile(profile: UserProfile) {
        preferenceHelper.userName = profile.name
        preferenceHelper.userAvatarUri = profile.avatarUri
        preferenceHelper.userDob = profile.dateOfBirth
        preferenceHelper.userGender = profile.gender
        preferenceHelper.userHeight = profile.heightCm
        preferenceHelper.userWeight = profile.weightKg
        preferenceHelper.isProfileCreated = true

        val profiles = getAllProfiles().toMutableList()
        val existingIndex = profiles.indexOfFirst { it.isPrimary }
        if (existingIndex >= 0) {
            profiles[existingIndex] = profile.copy(isPrimary = true)
        } else {
            profiles.add(0, profile.copy(isPrimary = true))
        }
        persistProfiles(profiles)
    }

    /**
     * Adds or updates a family member profile.
     */
    fun addFamilyMemberProfile(profile: UserProfile) {
        val profiles = getAllProfiles().toMutableList()
        val existingIndex = profiles.indexOfFirst { it.id == profile.id }
        if (existingIndex >= 0) {
            profiles[existingIndex] = profile.copy(isPrimary = false)
        } else {
            profiles.add(profile.copy(isPrimary = false))
        }
        persistProfiles(profiles)
    }

    /**
     * Returns the primary user profile.
     */
    fun getPrimaryProfile(): UserProfile {
        val all = getAllProfiles()
        val found = all.find { it.isPrimary }
        if (found != null) return found

        return UserProfile(
            name = preferenceHelper.userName,
            avatarUri = preferenceHelper.userAvatarUri,
            dateOfBirth = preferenceHelper.userDob,
            gender = preferenceHelper.userGender,
            heightCm = preferenceHelper.userHeight,
            weightKg = preferenceHelper.userWeight,
            isPrimary = true
        )
    }

    /**
     * Retrieves all saved profiles.
     */
    fun getAllProfiles(): List<UserProfile> {
        val jsonStr = prefs.getString(KEY_PROFILES_JSON, null) ?: return emptyList()
        val list = mutableListOf<UserProfile>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    UserProfile(
                        id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                        name = obj.getString("name"),
                        avatarUri = if (obj.has("avatarUri") && !obj.isNull("avatarUri")) obj.getString("avatarUri") else null,
                        dateOfBirth = obj.getString("dateOfBirth"),
                        gender = obj.getString("gender"),
                        heightCm = obj.getInt("heightCm"),
                        weightKg = obj.getInt("weightKg"),
                        isPrimary = obj.optBoolean("isPrimary", false),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
        } catch (_: Exception) {
        }
        return list
    }

    private fun persistProfiles(profiles: List<UserProfile>) {
        val array = JSONArray()
        for (p in profiles) {
            val obj = JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("avatarUri", p.avatarUri ?: JSONObject.NULL)
                put("dateOfBirth", p.dateOfBirth)
                put("gender", p.gender)
                put("heightCm", p.heightCm)
                put("weightKg", p.weightKg)
                put("isPrimary", p.isPrimary)
                put("createdAt", p.createdAt)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_PROFILES_JSON, array.toString()).apply()
    }
}
