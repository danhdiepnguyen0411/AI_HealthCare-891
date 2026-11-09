package com.example.healthcareai.util

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.view.View
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
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

    fun getSavedLocale(context: Context): Locale {
        return getLocale(getSavedLanguageCode(context))
    }

    fun setLocale(context: Context, languageCode: String): Context {
        PreferenceHelper.getInstance(context).selectedLanguageCode = languageCode
        val updated = updateResources(context, languageCode)
        try {
            updateResources(context.applicationContext, languageCode)
        } catch (_: Exception) {}
        return updated
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

    fun getLocalizedString(
        context: Context,
        languageCode: String,
        @StringRes resId: Int,
        vararg formatArgs: Any
    ): String {
        return try {
            val locale = getLocale(languageCode)
            val config = Configuration(context.resources.configuration)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                config.setLocale(locale)
            } else {
                @Suppress("DEPRECATION")
                config.locale = locale
            }
            val localizedRes = context.createConfigurationContext(config).resources
            if (formatArgs.isNotEmpty()) {
                localizedRes.getString(resId, *formatArgs)
            } else {
                localizedRes.getString(resId)
            }
        } catch (_: Exception) {
            if (formatArgs.isNotEmpty()) {
                context.getString(resId, *formatArgs)
            } else {
                context.getString(resId)
            }
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

    /**
     * Returns the pair of (AM, PM) localized abbreviations for the given language code.
     */
    fun getAmPmStrings(langCode: String): Pair<String, String> {
        val lower = langCode.lowercase()
        return when {
            lower.startsWith("vi") -> Pair("SA", "CH")
            lower.startsWith("hi") -> Pair("पूर्वाह्न", "अपराह्न")
            lower.startsWith("bn") -> Pair("পূর্বাহ্ন", "অপরাহ্ন")
            lower.startsWith("ar") -> Pair("ص", "م")
            lower.startsWith("zh") -> Pair("上午", "下午")
            lower.startsWith("ja") -> Pair("午前", "午後")
            lower.startsWith("ko") -> Pair("오전", "오후")
            lower.startsWith("ru") -> Pair("ДП", "ПП")
            lower.startsWith("tr") -> Pair("ÖÖ", "ÖS")
            else -> Pair("AM", "PM")
        }
    }

    /**
     * Formats AM / PM text according to the active language.
     * In Vietnamese ("vi"): AM -> SA, PM -> CH.
     * In Hindi ("hi"): AM -> पूर्वाह्न, PM -> अपराह्न.
     * In Bengali ("bn"): AM -> পূর্বাহ্ন, PM -> অপরাহ্ন.
     * In Arabic ("ar"): AM -> ص, PM -> م.
     * In Chinese ("zh"): AM -> 上午, PM -> 下午.
     * In Japanese ("ja"): AM -> 午前, PM -> 午後.
     * In Korean ("ko"): AM -> 오전, PM -> 오후.
     * In Russian ("ru"): AM -> ДП, PM -> ПП.
     * In Turkish ("tr"): AM -> ÖÖ, PM -> ÖS.
     */
    fun formatTimeAmPm(rawTime: String, langCode: String): String {
        if (rawTime.isBlank()) return rawTime
        val (amStr, pmStr) = getAmPmStrings(langCode)
        if (amStr == "AM" && pmStr == "PM") {
            return rawTime
        }
        return rawTime
            .replace("(?i)\\bAM\\b".toRegex(), amStr)
            .replace("(?i)\\bPM\\b".toRegex(), pmStr)
            .replace("(?i)\\bsáng\\b".toRegex(), amStr)
            .replace("(?i)\\bchiều\\b".toRegex(), pmStr)
            .replace("(?i)\\btối\\b".toRegex(), pmStr)
    }

    fun formatTimeAmPm(context: Context, rawTime: String): String {
        return formatTimeAmPm(rawTime, getSavedLanguageCode(context))
    }

    /**
     * Formats a timestamp into a time string using the saved locale,
     * applying AM/PM -> SA/CH abbreviation if in Vietnamese.
     */
    fun formatTime(context: Context, timestamp: Long, pattern: String = "hh:mm a"): String {
        val locale = getSavedLocale(context)
        val sdf = java.text.SimpleDateFormat(pattern, locale)
        val formatted = sdf.format(java.util.Date(timestamp))
        return formatTimeAmPm(context, formatted)
    }

    /**
     * Parses a canonical or localized date string into a [java.util.Date].
     */
    fun parseStoredDate(rawDateStr: String?): java.util.Date? {
        val trimmed = rawDateStr?.trim() ?: return null
        if (trimmed.isEmpty()) return null
        val formats = listOf(
            java.text.SimpleDateFormat("MMM dd, yyyy", Locale.US),
            java.text.SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.US),
            java.text.SimpleDateFormat("MMM d, yyyy", Locale.US),
            java.text.SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()),
            java.text.SimpleDateFormat("MMM d, yyyy", Locale.getDefault()),
            java.text.SimpleDateFormat("dd/MM/yyyy", Locale.US),
            java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US),
            java.text.SimpleDateFormat("dd-MM-yyyy", Locale.US)
        )
        for (sdf in formats) {
            try {
                sdf.isLenient = false
                val parsed = sdf.parse(trimmed)
                if (parsed != null) return parsed
            } catch (_: Exception) {
            }
        }
        return null
    }

    /**
     * Formats a [java.util.Date] using the active app locale.
     */
    fun formatDate(context: Context, date: java.util.Date, skeleton: String = "yMMMd"): String {
        val locale = getSavedLocale(context)
        val pattern = android.text.format.DateFormat.getBestDateTimePattern(locale, skeleton)
        return java.text.SimpleDateFormat(pattern, locale).format(date)
    }

    /**
     * Formats a timestamp (millis) using the active app locale.
     */
    fun formatDate(context: Context, timestamp: Long, skeleton: String = "yMMMd"): String {
        return formatDate(context, java.util.Date(timestamp), skeleton)
    }

    /**
     * Formats a [java.util.Date] with both date and time using the active app locale.
     */
    fun formatDateTime(context: Context, date: java.util.Date, dateSkeleton: String = "yMMMd"): String {
        val datePart = formatDate(context, date, dateSkeleton)
        val timePart = formatTime(context, date.time, "h:mm a")
        return "$datePart · $timePart"
    }

    /**
     * Formats a stored date/datetime string (e.g. "Jan 01, 2000" or "Nov 6, 2026 · 9:00 AM")
     * into the currently selected app language.
     */
    fun formatStoredDate(context: Context, rawDateStr: String?, skeleton: String = "yMMMd"): String {
        val trimmed = rawDateStr?.trim().orEmpty()
        if (trimmed.isEmpty()) return ""
        if (trimmed.contains("·")) {
            try {
                val sdfDateTime = java.text.SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.US)
                val parsed = sdfDateTime.parse(trimmed)
                if (parsed != null) {
                    return formatDateTime(context, parsed, skeleton)
                }
            } catch (_: Exception) {
            }
        }
        val parsed = parseStoredDate(trimmed)
        if (parsed != null) {
            return formatDate(context, parsed, skeleton)
        }
        return formatTimeAmPm(context, trimmed)
    }

    /**
     * Localizes a scan history item title according to the active app language.
     */
    fun formatScanTitle(context: Context, rawTitle: String?, category: String?): String {
        val lang = getSavedLanguageCode(context).lowercase(Locale.ROOT)
        val trimmed = rawTitle?.trim().orEmpty()
        if (lang.startsWith("en") && trimmed.isNotEmpty()) return trimmed

        val lower = trimmed.lowercase(Locale.ROOT)
        if (lower == "nail discoloration") {
            return when {
                lang.startsWith("vi") -> "Đổi màu móng"
                lang.startsWith("ru") -> "Изменение цвета ногтей"
                lang.startsWith("hi") -> "नाखून का रंग बदलना"
                lang.startsWith("id") || lang.startsWith("in") -> "Perubahan Warna Kuku"
                lang.startsWith("de") -> "Nagelverfärbung"
                lang.startsWith("es") -> "Decoloración de las uñas"
                lang.startsWith("fr") -> "Décoloration des ongles"
                lang.startsWith("pt") -> "Descoloração das unhas"
                lang.startsWith("ja") -> "爪の変色"
                lang.startsWith("ko") -> "손톱 변색"
                lang.startsWith("zh_tw") -> "指甲變色"
                lang.startsWith("zh") -> "指甲变色"
                lang.startsWith("ar") -> "تغير لون الأظافر"
                lang.startsWith("tr") -> "Tırnak Renk Değişikliği"
                lang.startsWith("bn") -> "নখের রঙ পরিবর্তন"
                else -> trimmed
            }
        }
        if (lower == "nail thickening") {
            return when {
                lang.startsWith("vi") -> "Dày móng"
                lang.startsWith("ru") -> "Утолщение ногтей"
                lang.startsWith("hi") -> "नाखून का मोटा होना"
                lang.startsWith("id") || lang.startsWith("in") -> "Penebalan Kuku"
                lang.startsWith("de") -> "Nagelverdickung"
                lang.startsWith("es") -> "Engrosamiento de las uñas"
                lang.startsWith("fr") -> "Épaississement des ongles"
                lang.startsWith("pt") -> "Espessamento das unhas"
                lang.startsWith("ja") -> "爪の肥厚"
                lang.startsWith("ko") -> "손톱 비후"
                lang.startsWith("zh_tw") -> "指甲增厚"
                lang.startsWith("zh") -> "指甲增厚"
                lang.startsWith("ar") -> "سماكة الأظافر"
                lang.startsWith("tr") -> "Tırnak Kalınlaşması"
                lang.startsWith("bn") -> "নখ মোটা হয়ে যাওয়া"
                else -> trimmed
            }
        }
        if (lower == "periungual skin") {
            return when {
                lang.startsWith("vi") -> "Da quanh móng"
                lang.startsWith("ru") -> "Околоногтевая кожа"
                lang.startsWith("hi") -> "नाखून के आसपास की त्वचा"
                lang.startsWith("id") || lang.startsWith("in") -> "Kulit Sekitar Kuku"
                lang.startsWith("de") -> "Periunguale Haut"
                lang.startsWith("es") -> "Piel periungueal"
                lang.startsWith("fr") -> "Peau péri-unguéale"
                lang.startsWith("pt") -> "Pele periungueal"
                lang.startsWith("ja") -> "爪周囲の皮膚"
                lang.startsWith("ko") -> "손발톱 주위 피부"
                lang.startsWith("zh_tw") -> "甲周皮膚"
                lang.startsWith("zh") -> "甲周皮肤"
                lang.startsWith("ar") -> "الجلد المحيط بالأظافر"
                lang.startsWith("tr") -> "Tırnak Çevresi Derisi"
                lang.startsWith("bn") -> "নখের চারপাশের ত্বক"
                else -> trimmed
            }
        }
        if (lower.endsWith(" assessment") || trimmed.isEmpty()) {
            return when (category?.uppercase(Locale.ROOT)) {
                "SKIN" -> getLocalizedString(context, lang, R.string.category_skin)
                "EYE" -> getLocalizedString(context, lang, R.string.category_eye)
                "NAIL" -> getLocalizedString(context, lang, R.string.category_nail)
                "ORAL" -> getLocalizedString(context, lang, R.string.category_mouth)
                else -> trimmed.ifEmpty { getLocalizedString(context, lang, R.string.title_ai_scan_history) }
            }
        }
        return trimmed
    }
}
