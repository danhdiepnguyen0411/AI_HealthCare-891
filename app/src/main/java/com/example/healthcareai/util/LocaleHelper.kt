package com.example.healthcareai.util

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.view.View
import androidx.annotation.DrawableRes
import androidx.core.text.TextUtilsCompat
import androidx.core.view.ViewCompat
import com.example.healthcareai.R
import com.example.healthcareai.data.local.PreferenceHelper
import java.util.Locale

object LocaleHelper {

    val SUPPORTED_LANGUAGE_CODES = listOf(
        "en", "vi", "es", "fr", "de", "pt_BR", "pt_PT", "ja",
        "ko", "zh_CN", "zh_TW", "ru", "hi", "id", "ar", "tr", "bn"
    )

    fun getDeviceLanguageCode(): String {
        val deviceLocale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val sysLocales = android.content.res.Resources.getSystem().configuration.locales
            if (!sysLocales.isEmpty) sysLocales[0] else Locale.getDefault()
        } else {
            @Suppress("DEPRECATION")
            android.content.res.Resources.getSystem().configuration.locale ?: Locale.getDefault()
        }
        val devLang = when (val l = deviceLocale.language.lowercase()) {
            "in" -> "id"
            "iw" -> "he"
            "ji" -> "yi"
            else -> l
        }
        val devCountry = deviceLocale.country.uppercase()

        // 1. Exact match with country (e.g. pt_BR, pt_PT, zh_CN, zh_TW)
        val matchedFull = SUPPORTED_LANGUAGE_CODES.find { code ->
            if (code.contains("_")) {
                val parts = code.split("_")
                parts[0].lowercase() == devLang && parts[1].uppercase() == devCountry
            } else {
                false
            }
        }
        if (matchedFull != null) return matchedFull

        // 2. Dialect fallback for Chinese / Portuguese
        if (devLang == "zh") {
            return if (devCountry == "TW" || devCountry == "HK" || devCountry == "MO") "zh_TW" else "zh_CN"
        }
        if (devLang == "pt") {
            return if (devCountry == "PT") "pt_PT" else "pt_BR"
        }

        // 3. Match language prefix
        val matchedPrefix = SUPPORTED_LANGUAGE_CODES.find { code ->
            val codeLang = if (code.contains("_")) code.split("_")[0] else code
            codeLang.lowercase() == devLang
        }
        if (matchedPrefix != null) return matchedPrefix

        return "en"
    }

    fun getLanguageNameByCode(code: String): String {
        return when (code) {
            "en" -> "English (UK)"
            "vi" -> "Vietnamese"
            "es" -> "Spanish"
            "fr" -> "French"
            "de" -> "German"
            "pt_BR" -> "Brasil"
            "pt_PT" -> "Portugal"
            "ja" -> "Japanese"
            "ko" -> "Korean"
            "zh_CN" -> "Chinese (Simplified)"
            "zh_TW" -> "Chinese (Traditional)"
            "ru" -> "Russian"
            "hi" -> "India"
            "id" -> "Indonesian"
            "ar" -> "Arabic"
            "tr" -> "Turkish"
            "bn" -> "Bengali"
            else -> "English"
        }
    }

    fun getNativeNameByCode(code: String): String {
        return when (code) {
            "en" -> "English"
            "vi" -> "Tiếng Việt"
            "es" -> "Español"
            "fr" -> "Français"
            "de" -> "Deutsch"
            "pt_BR" -> "Português"
            "pt_PT" -> "Português"
            "ja" -> "日本語"
            "ko" -> "한국어"
            "zh_CN" -> "简体中文"
            "zh_TW" -> "繁體中文"
            "ru" -> "Русский"
            "hi" -> "हिन्दी"
            "id" -> "Bahasa Indonesia"
            "ar" -> "العربية"
            "tr" -> "Türkçe"
            "bn" -> "বাংলা"
            else -> "English"
        }
    }

    @DrawableRes
    fun getFlagResByCode(code: String): Int {
        return when (code) {
            "en" -> R.drawable.ic_flag_en
            "vi" -> R.drawable.ic_flag_vn
            "es" -> R.drawable.ic_flag_es
            "fr" -> R.drawable.ic_flag_fr
            "de" -> R.drawable.ic_flag_de
            "pt_BR" -> R.drawable.ic_flag_br
            "pt_PT" -> R.drawable.ic_flag_pt
            "ja" -> R.drawable.ic_flag_jp
            "ko" -> R.drawable.ic_flag_kr
            "zh_CN" -> R.drawable.ic_flag_cn
            "zh_TW" -> R.drawable.ic_flag_cn
            "ru" -> R.drawable.ic_flag_ru
            "hi" -> R.drawable.ic_flag_ind
            "id" -> R.drawable.ic_flag_id
            "ar" -> R.drawable.ic_flag_sa
            "tr" -> R.drawable.ic_flag_tr
            "bn" -> R.drawable.ic_flag_bn
            else -> R.drawable.ic_flag_en
        }
    }

    fun onAttach(context: Context): Context {
        val langCode = getSavedLanguageCode(context)
        return updateResources(context, langCode)
    }

    fun getSavedLanguageCode(context: Context): String {
        return PreferenceHelper.getInstance(context).selectedLanguageCode
    }

    fun setLocale(context: Context, languageCode: String): Context {
        PreferenceHelper.getInstance(context).selectedLanguageCode = languageCode
        return updateResources(context, languageCode)
    }

    fun getLocale(languageCode: String): Locale {
        return when {
            languageCode.contains("_") -> {
                val parts = languageCode.split("_")
                Locale(parts[0], parts[1])
            }
            languageCode == "id" || languageCode == "in" -> Locale("in")
            languageCode == "he" || languageCode == "iw" -> Locale("iw")
            languageCode == "yi" || languageCode == "ji" -> Locale("ji")
            else -> Locale(languageCode)
        }
    }

    fun isRtl(context: Context): Boolean {
        val langCode = getSavedLanguageCode(context)
        val locale = getLocale(langCode)
        return TextUtilsCompat.getLayoutDirectionFromLocale(locale) == ViewCompat.LAYOUT_DIRECTION_RTL
    }

    fun applyLayoutDirection(activity: Activity, languageCode: String) {
        val locale = getLocale(languageCode)
        val dir = if (TextUtilsCompat.getLayoutDirectionFromLocale(locale) == ViewCompat.LAYOUT_DIRECTION_RTL) {
            View.LAYOUT_DIRECTION_RTL
        } else {
            View.LAYOUT_DIRECTION_LTR
        }
        activity.window.decorView.layoutDirection = dir
        activity.findViewById<View>(android.R.id.content)?.layoutDirection = dir
    }

    fun updateResources(context: Context, languageCode: String): Context {
        val locale = getLocale(languageCode)
        Locale.setDefault(locale)

        if (context is Activity) {
            applyLayoutDirection(context, languageCode)
        }

        val res = context.resources
        val config = Configuration(res.configuration)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale)
            config.setLayoutDirection(locale)
            val localizedContext = context.createConfigurationContext(config)
            @Suppress("DEPRECATION")
            res.updateConfiguration(config, res.displayMetrics)
            return localizedContext
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
            @Suppress("DEPRECATION")
            config.setLayoutDirection(locale)
            @Suppress("DEPRECATION")
            res.updateConfiguration(config, res.displayMetrics)
            return context
        }
    }
}
