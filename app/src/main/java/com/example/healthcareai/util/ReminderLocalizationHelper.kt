package com.example.healthcareai.util

import android.content.Context
import androidx.annotation.StringRes
import com.example.healthcareai.R
import java.text.DateFormatSymbols

object ReminderLocalizationHelper {

    private fun getStringSafe(context: Context, @StringRes resId: Int): String {
        return try {
            val lang = LocaleHelper.getSavedLanguageCode(context)
            LocaleHelper.getLocalizedString(context, lang, resId)
        } catch (_: Exception) {
            context.getString(resId)
        }
    }

    fun getLocalizedName(context: Context, rawName: String, category: String = ""): String {
        val catLower = category.trim().lowercase()
        val nameLower = rawName.trim().lowercase()

        // 1. Match category for fixed reminder types
        when {
            catLower == "heart rate" || catLower == "heart_rate" || catLower == "nhịp tim" ||
            catLower == "đo nhịp tim" || catLower.contains("heart") -> {
                return getStringSafe(context, R.string.title_heart_rate)
            }
            catLower == "drink water" || catLower == "water" || catLower == "uống nước" || catLower.contains("water") -> {
                return getStringSafe(context, R.string.cat_drink_water)
            }
            catLower == "temperature" || catLower == "check temperature" || catLower == "nhiệt độ" ||
            catLower == "kiểm tra nhiệt độ" || catLower.contains("temp") -> {
                return getStringSafe(context, R.string.reminder_check_temperature_title)
            }
            catLower == "exercise" || catLower == "tập thể dục" || catLower.contains("exercise") || catLower.contains("thể dục") -> {
                return getStringSafe(context, R.string.cat_exercise)
            }
            catLower == "sleep" || catLower == "giấc ngủ" || catLower.contains("sleep") || catLower.contains("ngủ") -> {
                return getStringSafe(context, R.string.cat_sleep)
            }
        }

        // 2. If rawName is blank, resolve from category
        if (rawName.isBlank()) {
            return when {
                catLower.contains("medicat") || catLower.contains("thuốc") -> getStringSafe(context, R.string.record_medications)
                catLower.contains("vaccin") || catLower.contains("tiêm") -> getStringSafe(context, R.string.record_vaccinations)
                catLower.contains("appoint") || catLower.contains("hẹn") -> getStringSafe(context, R.string.category_appointments_title)
                else -> category
            }
        }

        // 3. Match against known name phrases in various languages
        return when {
            nameLower == "heart rate" || nameLower == "heart_rate" || nameLower == "measure heart rate" ||
            nameLower == "đo nhịp tim" || nameLower == "nhịp tim" || nameLower.contains("heart rate") ||
            nameLower.contains("nhịp tim") || nameLower.contains("heart_rate") ||
            nameLower == "herzfrequenz" || nameLower == "rythme cardiaque" || nameLower == "ritmo cardíaco" ||
            nameLower == "frequência cardíaca" || nameLower == "frequencia cardiaca" || nameLower == "пульс" ||
            nameLower == "kalp atış hızı" || nameLower == "detak jantung" || nameLower == "हृदय गति" ||
            nameLower == "হৃদস্পন্দন" || nameLower == "نبض القلب" || nameLower == "심박수" || nameLower == "心拍数" ||
            nameLower.contains("心率") -> {
                getStringSafe(context, R.string.title_heart_rate)
            }
            nameLower == "drink water" || nameLower == "water" || nameLower == "uống nước" ||
            nameLower.contains("drink water") || nameLower.contains("uống nước") ||
            nameLower == "wasser trinken" || nameLower == "boire de l'eau" || nameLower == "beber agua" ||
            nameLower == "beber água" || nameLower == "пить воду" || nameLower == "su iç" ||
            nameLower == "minum air" || nameLower == "पानी पीना" || nameLower == "পানি পান করুন" ||
            nameLower == "شرب الماء" || nameLower == "물 마시기" || nameLower == "水分補給" ||
            nameLower.contains("喝水") -> {
                getStringSafe(context, R.string.cat_drink_water)
            }
            nameLower == "check temperature" || nameLower == "temperature" || nameLower == "nhiệt độ" ||
            nameLower == "kiểm tra nhiệt độ" || nameLower.contains("temperature") || nameLower.contains("nhiệt độ") ||
            nameLower == "temperatur prüfen" || nameLower == "vérifier la température" || nameLower == "medir la temperatura" ||
            nameLower == "verificar temperatura" || nameLower == "измерить температуру" || nameLower == "sıcaklığı kontrol et" ||
            nameLower == "periksa suhu" || nameLower == "तापमान जांचें" || nameLower == "तापমাত্রা পরীক্ষা" ||
            nameLower == "قياس درجة الحرارة" || nameLower == "체온 측정" || nameLower == "体温を測る" ||
            nameLower.contains("体温") || nameLower.contains("体温测量") || nameLower.contains("温度") -> {
                getStringSafe(context, R.string.reminder_check_temperature_title)
            }
            nameLower == "exercise" || nameLower == "tập thể dục" || nameLower.contains("exercise") || nameLower.contains("thể dục") ||
            nameLower == "sport" || nameLower == "exercice" || nameLower == "ejercicio" || nameLower == "exercício" ||
            nameLower == "упражнения" || nameLower == "egzersiz" || nameLower == "olahraga" || nameLower == "व्यायाम" ||
            nameLower == "ব্যায়াম" || nameLower == "ممارسة الرياضة" || nameLower == "운동" || nameLower == "運動" ||
            nameLower.contains("锻炼") -> {
                getStringSafe(context, R.string.cat_exercise)
            }
            nameLower == "sleep" || nameLower == "giấc ngủ" || nameLower.contains("sleep") || nameLower.contains("giấc ngủ") ||
            nameLower == "schlaf" || nameLower == "sommeil" || nameLower == "sueño" || nameLower == "sono" ||
            nameLower == "сон" || nameLower == "uyku" || nameLower == "tidur" || nameLower == "नींद" ||
            nameLower == "ঘুম" || nameLower == "النوم" || nameLower == "수면" || nameLower == "睡眠" -> {
                getStringSafe(context, R.string.cat_sleep)
            }
            nameLower == "medication" || nameLower == "medicine" || nameLower == "thuốc" || nameLower.contains("medication") || nameLower == "uống thuốc" ||
            nameLower == "medikament" || nameLower == "médicament" || nameLower == "medicamento" ||
            nameLower == "лекарство" || nameLower == "ilaç" || nameLower == "obat" || nameLower == "दवा" ||
            nameLower == "ওষুধ" || nameLower == "دواء" || nameLower == "복약" || nameLower == "服薬" ||
            nameLower.contains("药物") || nameLower.contains("用药") -> {
                getStringSafe(context, R.string.record_medications)
            }
            nameLower == "vaccination" || nameLower == "vaccine" || nameLower == "tiêm chủng" || nameLower.contains("vaccin") || nameLower.contains("tiêm chủng") ||
            nameLower == "impfung" || nameLower == "vacunación" || nameLower == "vacinação" || nameLower == "вакцинация" ||
            nameLower == "aşı" || nameLower == "vaksinasi" || nameLower == "टीकाकरण" || nameLower == "টিকাদান" ||
            nameLower == "التطعيم" || nameLower == "예방접종" || nameLower == "予防接種" || nameLower.contains("接种") || nameLower.contains("疫苗") -> {
                getStringSafe(context, R.string.record_vaccinations)
            }
            nameLower == "appointments" || nameLower == "appointment" || nameLower == "lịch hẹn" || nameLower.contains("appointment") || nameLower.contains("lịch hẹn") ||
            nameLower == "arzttermin" || nameLower == "rendez-vous" || nameLower == "cita médica" || nameLower == "consultas" ||
            nameLower == "прием у врача" || nameLower == "randevular" || nameLower == "janji temu" || nameLower == "अपॉइंटमेंट" ||
            nameLower == "অ্যাপয়েন্টমেন্ট" || nameLower == "المواعيد" || nameLower == "진료 예약" || nameLower == "予約" || nameLower.contains("预约") -> {
                getStringSafe(context, R.string.category_appointments_title)
            }
            nameLower.contains("observe") || nameLower.contains("skin care") -> {
                getStringSafe(context, R.string.reminder_observe_area_title)
            }
            nameLower == "cetirizine" -> {
                getStringSafe(context, R.string.reminder_cetirizine_title)
            }
            else -> rawName
        }
    }

    fun getLocalizedDescription(context: Context, rawDesc: String): String {
        if (rawDesc.isBlank()) return rawDesc
        val lower = rawDesc.trim().lowercase()
        return when {
            lower.contains("hydration accelerates") -> getStringSafe(context, R.string.reminder_water_notes)
            lower.contains("monitor temperature for early fever") -> getStringSafe(context, R.string.reminder_check_temperature_notes)
            lower.contains("check for any changes in redness") -> getStringSafe(context, R.string.reminder_observe_area_notes)
            else -> rawDesc
        }
    }

    fun getLocalizedRepeat(context: Context, repeatKey: String): String {
        val lower = repeatKey.trim().lowercase()
        return when {
            lower == "once" || lower == "một lần" || lower == "1 lần" -> {
                getStringSafe(context, R.string.repeat_once)
            }
            lower == "every day" || lower == "daily" || lower == "hằng ngày" || lower == "mỗi ngày" -> {
                getStringSafe(context, R.string.repeat_every_day)
            }
            lower == "weekdays" || lower == "thứ 2 đến thứ 6" || lower == "các ngày trong tuần" || lower == "ngày trong tuần" || lower == "monday to friday" -> {
                getStringSafe(context, R.string.repeat_weekdays)
            }
            lower == "custom" || lower == "tùy chỉnh" -> {
                getStringSafe(context, R.string.repeat_custom)
            }
            lower.startsWith("custom:") -> {
                formatCustomDays(context, repeatKey.substringAfter(":").trim())
            }
            lower == "weekends" || lower == "cuối tuần" -> getStringSafe(context, R.string.repeat_weekends)
            lower == "weekly" || lower == "hàng tuần" -> getStringSafe(context, R.string.repeat_weekly)
            lower == "monthly" || lower == "hàng tháng" -> getStringSafe(context, R.string.repeat_monthly)
            else -> repeatKey
        }
    }

    private fun formatCustomDays(context: Context, csv: String): String {
        val days = csv.split(",").mapNotNull { it.trim().toIntOrNull() }
        if (days.isEmpty()) return getStringSafe(context, R.string.repeat_custom)
        if (days.size == 7) return getStringSafe(context, R.string.repeat_every_day)
        if (days.size == 5 && !days.contains(java.util.Calendar.SATURDAY) && !days.contains(java.util.Calendar.SUNDAY)) {
            return getStringSafe(context, R.string.repeat_weekdays)
        }

        val isVi = LocaleHelper.getSavedLocale(context).language.lowercase().startsWith("vi")

        return days.mapNotNull { dayOfWeek ->
            when (dayOfWeek) {
                java.util.Calendar.MONDAY -> if (isVi) "Thứ 2" else "Mon"
                java.util.Calendar.TUESDAY -> if (isVi) "Thứ 3" else "Tue"
                java.util.Calendar.WEDNESDAY -> if (isVi) "Thứ 4" else "Wed"
                java.util.Calendar.THURSDAY -> if (isVi) "Thứ 5" else "Thu"
                java.util.Calendar.FRIDAY -> if (isVi) "Thứ 6" else "Fri"
                java.util.Calendar.SATURDAY -> if (isVi) "Thứ 7" else "Sat"
                java.util.Calendar.SUNDAY -> if (isVi) "Chủ nhật" else "Sun"
                else -> null
            }
        }.joinToString(", ")
    }

    fun getLocalizedForm(context: Context, rawForm: String): String {
        if (rawForm.isBlank()) return rawForm
        val lower = rawForm.trim().lowercase()
        return when {
            lower == "tablet" || lower == "viên nén" || lower == "tablette" || lower == "comprimé" ||
            lower == "tableta" || lower == "comprimido" || lower == "таблетка" || lower == "片剂" ||
            lower == "錠剤" || lower.contains("tablet") || lower.contains("viên nén") ||
            lower.contains("정제") || lower.contains("أقراص") || lower.contains("ট্যাবলেট") ||
            lower.contains("गोली") -> {
                getStringSafe(context, R.string.form_tablet)
            }
            lower == "capsule" || lower == "viên nang" || lower == "kapsel" || lower == "cápsula" ||
            lower == "капсула" || lower == "胶囊" || lower == "カプセル" || lower == "캡슐" ||
            lower.contains("capsule") || lower.contains("viên nang") || lower.contains("كبسول") ||
            lower.contains("ক্যাপসুল") || lower.contains("कैप्सूल") -> {
                getStringSafe(context, R.string.form_capsule)
            }
            lower == "liquid" || lower == "dung dịch" || lower == "dạng lỏng" || lower == "flüssigkeit" ||
            lower == "liquide" || lower == "líquido" || lower == "жидкость" || lower == "口服液" ||
            lower == "液体" || lower == "액상" || lower == "سائل" || lower.contains("liquid") ||
            lower.contains("dung dịch") || lower.contains("तरल") || lower.contains("তরল") -> {
                getStringSafe(context, R.string.form_liquid)
            }
            lower == "drops" || lower == "thuốc nhỏ" || lower == "tropfen" || lower == "gouttes" ||
            lower == "gotas" || lower == "капли" || lower == "滴剂" || lower.contains("drop") ||
            lower.contains("nhỏ") || lower.contains("قطرات") || lower.contains("ড্রপ") ||
            lower.contains("बूंद") || lower.contains("点眼") || lower.contains("점적") -> {
                getStringSafe(context, R.string.form_drops)
            }
            lower == "injection" || lower == "thuốc tiêm" || lower == "tiêm" || lower == "injektion" ||
            lower == "inyección" || lower == "injeção" || lower == "инъекция" || lower == "注射剂" ||
            lower == "注射" || lower == "주사" || lower.contains("inject") || lower.contains("tiêm") ||
            lower.contains("حقن") || lower.contains("ইনজেকশন") || lower.contains("इंजेक्शन") -> {
                getStringSafe(context, R.string.form_injection)
            }
            lower == "inhaler" || lower == "ống hít" || lower == "hít" || lower == "inhalator" ||
            lower == "inhalateur" || lower == "inhalador" || lower == "ингалятор" || lower == "吸入剂" ||
            lower == "吸入器" || lower == "흡입기" || lower.contains("inhal") || lower.contains("hít") ||
            lower.contains("بخاخ") || lower.contains("ইনহেলার") || lower.contains("इन्हेलर") -> {
                getStringSafe(context, R.string.form_inhaler)
            }
            lower == "other" || lower == "khác" || lower == "sonstiges" || lower == "autre" ||
            lower == "otro" || lower == "outro" || lower == "другое" || lower == "其它" ||
            lower == "其他" || lower == "その他" || lower == "기타" || lower.contains("other") ||
            lower.contains("khác") || lower.contains("أخرى") || lower.contains("অন্যান্য") ||
            lower.contains("अन्य") -> {
                getStringSafe(context, R.string.form_other)
            }
            else -> rawForm
        }
    }

    fun getLocalizedSchedule(context: Context, scheduleDisplay: String): String {
        if (scheduleDisplay.isBlank()) return scheduleDisplay
        val lower = scheduleDisplay.trim().lowercase()
        return when {
            lower == "choose times" -> getStringSafe(context, R.string.hint_repeat_schedule)
            scheduleDisplay.contains("Daily", ignoreCase = true) -> {
                val dailyLocalized = getStringSafe(context, R.string.repeat_every_day)
                scheduleDisplay.replace("Daily", dailyLocalized, ignoreCase = true)
            }
            else -> scheduleDisplay
        }
    }
}
