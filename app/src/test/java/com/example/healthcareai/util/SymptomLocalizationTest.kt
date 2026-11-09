package com.example.healthcareai.util

import org.junit.Assert.assertEquals
import org.junit.Test

class SymptomLocalizationTest {

    @Test
    fun testAll11ShoulderSymptomsVietnamese() {
        val expectedMap = mapOf(
            "Rotator cuff pain" to "Đau chóp xoay vai",
            "Shoulder blade pain" to "Đau xương bả vai",
            "Inability to lift arm above head" to "Không thể giơ tay qua đầu",
            "Clicking or popping in shoulder" to "Tiếng lạo xạo hoặc lục cục ở vai",
            "Shoulder muscle spasms" to "Co thắt cơ vai",
            "Stiffness or limited range of motion" to "Cứng khớp hoặc hạn chế vận động",
            "Sharp pain when lifting arm overhead" to "Đau nhói khi giơ tay qua đầu",
            "Dull ache radiating down upper arm" to "Đau âm ỉ lan xuống cánh tay trên",
            "Clicking or popping sensation in shoulder" to "Cảm giác lục cục hoặc lạo xạo ở vai",
            "Muscle tightness in upper back and neck" to "Căng cơ lưng trên và cổ",
            "Weakness when carrying objects" to "Yếu tay khi mang vác đồ vật"
        )

        for ((english, vietnamese) in expectedMap) {
            val localized = SymptomLocalizationHelper.getLocalizedSymptom("vi", english)
            assertEquals("Translation mismatch for '$english'", vietnamese, localized)
        }
    }

    @Test
    fun testFormatTimeAmPmVietnamese() {
        assertEquals("6:30 SA", LocaleHelper.formatTimeAmPm("6:30 AM", "vi"))
        assertEquals("8:30 CH", LocaleHelper.formatTimeAmPm("8:30 PM", "vi"))
        assertEquals("11:56 SA", LocaleHelper.formatTimeAmPm("11:56 AM", "vi"))
        assertEquals("12:00 CH", LocaleHelper.formatTimeAmPm("12:00 PM", "vi"))
    }

    @Test
    fun testFormatTimeAmPmAllSupportedLanguages() {
        // Vietnamese
        assertEquals("6:30 SA", LocaleHelper.formatTimeAmPm("6:30 AM", "vi"))
        assertEquals("8:30 CH", LocaleHelper.formatTimeAmPm("8:30 PM", "vi"))

        // Hindi
        assertEquals("6:30 पूर्वाह्न", LocaleHelper.formatTimeAmPm("6:30 AM", "hi"))
        assertEquals("8:30 अपराह्न", LocaleHelper.formatTimeAmPm("8:30 PM", "hi"))

        // Bengali
        assertEquals("6:30 পূর্বাহ্ন", LocaleHelper.formatTimeAmPm("6:30 AM", "bn"))
        assertEquals("8:30 অপরাহ্ন", LocaleHelper.formatTimeAmPm("8:30 PM", "bn"))

        // Arabic
        assertEquals("6:30 ص", LocaleHelper.formatTimeAmPm("6:30 AM", "ar"))
        assertEquals("8:30 م", LocaleHelper.formatTimeAmPm("8:30 PM", "ar"))

        // Chinese
        assertEquals("6:30 上午", LocaleHelper.formatTimeAmPm("6:30 AM", "zh"))
        assertEquals("8:30 下午", LocaleHelper.formatTimeAmPm("8:30 PM", "zh_CN"))

        // Japanese
        assertEquals("6:30 午前", LocaleHelper.formatTimeAmPm("6:30 AM", "ja"))
        assertEquals("8:30 午後", LocaleHelper.formatTimeAmPm("8:30 PM", "ja"))

        // Korean
        assertEquals("6:30 오전", LocaleHelper.formatTimeAmPm("6:30 AM", "ko"))
        assertEquals("8:30 오후", LocaleHelper.formatTimeAmPm("8:30 PM", "ko"))

        // Russian
        assertEquals("6:30 ДП", LocaleHelper.formatTimeAmPm("6:30 AM", "ru"))
        assertEquals("8:30 ПП", LocaleHelper.formatTimeAmPm("8:30 PM", "ru"))

        // Turkish
        assertEquals("6:30 ÖÖ", LocaleHelper.formatTimeAmPm("6:30 AM", "tr"))
        assertEquals("8:30 ÖS", LocaleHelper.formatTimeAmPm("8:30 PM", "tr"))
    }

    @Test
    fun testFormatTimeAmPmEnglishPreserved() {
        assertEquals("6:30 AM", LocaleHelper.formatTimeAmPm("6:30 AM", "en"))
        assertEquals("8:30 PM", LocaleHelper.formatTimeAmPm("8:30 PM", "en"))
    }
}
