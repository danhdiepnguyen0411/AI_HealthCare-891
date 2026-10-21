package com.example.healthcareai.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.healthcareai.util.LocaleHelper

class PreferenceHelper private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "app_healthcare_preferences"
        private const val KEY_FIRST_LANGUAGE_SET = "key_first_language_set"
        private const val KEY_SELECTED_LANGUAGE = "key_selected_language"
        private const val KEY_INTRO_COMPLETED = "key_intro_completed"
        private const val KEY_NOTI_DENIAL_COUNT = "key_noti_denial_count"
        private const val KEY_CAMERA_DENIAL_COUNT = "key_camera_denial_count"

        @Volatile
        private var instance: PreferenceHelper? = null

        fun getInstance(context: Context): PreferenceHelper {
            return instance ?: synchronized(this) {
                val safeContext = context.applicationContext ?: context
                instance ?: PreferenceHelper(safeContext).also { instance = it }
            }
        }
    }

    var isFirstLanguageSet: Boolean
        get() = prefs.getBoolean(KEY_FIRST_LANGUAGE_SET, false)
        set(value) {
            prefs.edit().putBoolean(KEY_FIRST_LANGUAGE_SET, value).commit()
        }

    var selectedLanguageCode: String
        get() {
            val defaultCode = LocaleHelper.getDeviceLanguageCode()
            return prefs.getString(KEY_SELECTED_LANGUAGE, defaultCode) ?: defaultCode
        }
        set(value) {
            prefs.edit().putString(KEY_SELECTED_LANGUAGE, value).commit()
        }

    var isIntroCompleted: Boolean
        get() = prefs.getBoolean(KEY_INTRO_COMPLETED, false)
        set(value) = prefs.edit().putBoolean(KEY_INTRO_COMPLETED, value).apply()

    var notiDenialCount: Int
        get() = prefs.getInt(KEY_NOTI_DENIAL_COUNT, 0)
        set(value) = prefs.edit().putInt(KEY_NOTI_DENIAL_COUNT, value).apply()

    var cameraDenialCount: Int
        get() = prefs.getInt(KEY_CAMERA_DENIAL_COUNT, 0)
        set(value) = prefs.edit().putInt(KEY_CAMERA_DENIAL_COUNT, value).apply()

    /**
     * Resets language selection back to system default if onboarding was not fully completed.
     */
    fun clearLanguageIfIntroNotCompleted(context: Context? = null) {
        if (!isIntroCompleted) {
            val devLang = LocaleHelper.getDeviceLanguageCode()
            prefs.edit()
                .remove(KEY_FIRST_LANGUAGE_SET)
                .remove(KEY_SELECTED_LANGUAGE)
                .apply()
            context?.let { ctx ->
                LocaleHelper.setLocale(ctx, devLang)
            }
        }
    }
}
