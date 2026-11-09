package com.example.healthcareai.util

import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderTimeSortTest {

    @Test
    fun testSortTimeListChronologicalOrder() {
        val userScreenshotTimes = listOf(
            "08:00 AM",
            "08:00 PM",
            "3:37 PM",
            "3:39 PM",
            "3:54 PM",
            "3:21 PM"
        )

        val sorted = ReminderScheduler.sortTimeList(userScreenshotTimes)

        val expected = listOf(
            "08:00 AM",
            "3:21 PM",
            "3:37 PM",
            "3:39 PM",
            "3:54 PM",
            "08:00 PM"
        )

        assertEquals(expected, sorted)
    }

    @Test
    fun testSortWithMidnightAndNoon() {
        val times = listOf(
            "11:30 PM",
            "12:00 PM",
            "12:00 AM",
            "6:15 AM",
            "1:00 PM"
        )

        val sorted = ReminderScheduler.sortTimeList(times)

        val expected = listOf(
            "12:00 AM",
            "6:15 AM",
            "12:00 PM",
            "1:00 PM",
            "11:30 PM"
        )

        assertEquals(expected, sorted)
    }

    @Test
    fun testParseAndSortVietnameseSaCh() {
        val saPair = ReminderScheduler.parseTimeString("6:30 SA")
        assertEquals(Pair(6, 30), saPair)

        val chPair = ReminderScheduler.parseTimeString("8:30 CH")
        assertEquals(Pair(20, 30), chPair)

        val timesVi = listOf("8:30 CH", "6:30 SA", "12:15 CH", "11:00 SA")
        val sortedVi = ReminderScheduler.sortTimeList(timesVi)
        val expectedVi = listOf("6:30 SA", "11:00 SA", "12:15 CH", "8:30 CH")
        assertEquals(expectedVi, sortedVi)
    }

    @Test
    fun testParseAndSortHindiAndBengali() {
        val hiAm = ReminderScheduler.parseTimeString("6:30 पूर्वाह्न")
        assertEquals(Pair(6, 30), hiAm)

        val hiPm = ReminderScheduler.parseTimeString("8:30 अपराह्न")
        assertEquals(Pair(20, 30), hiPm)

        val timesHi = listOf("8:30 अपराह्न", "6:30 पूर्वाह्न", "12:15 अपराह्न", "11:00 पूर्वाह्न")
        val sortedHi = ReminderScheduler.sortTimeList(timesHi)
        val expectedHi = listOf("6:30 पूर्वाह्न", "11:00 पूर्वाह्न", "12:15 अपराह्न", "8:30 अपराह्न")
        assertEquals(expectedHi, sortedHi)

        val bnAm = ReminderScheduler.parseTimeString("6:30 পূর্বাহ্ন")
        assertEquals(Pair(6, 30), bnAm)

        val bnPm = ReminderScheduler.parseTimeString("8:30 অপরাহ্ন")
        assertEquals(Pair(20, 30), bnPm)
    }

    @Test
    fun testParseAndSortOtherLocales() {
        // Arabic
        assertEquals(Pair(6, 30), ReminderScheduler.parseTimeString("6:30 ص"))
        assertEquals(Pair(20, 30), ReminderScheduler.parseTimeString("8:30 م"))

        // Chinese
        assertEquals(Pair(6, 30), ReminderScheduler.parseTimeString("6:30 上午"))
        assertEquals(Pair(20, 30), ReminderScheduler.parseTimeString("8:30 下午"))

        // Japanese
        assertEquals(Pair(6, 30), ReminderScheduler.parseTimeString("6:30 午前"))
        assertEquals(Pair(20, 30), ReminderScheduler.parseTimeString("8:30 午後"))

        // Russian
        assertEquals(Pair(6, 30), ReminderScheduler.parseTimeString("6:30 ДП"))
        assertEquals(Pair(20, 30), ReminderScheduler.parseTimeString("8:30 ПП"))

        // Turkish
        assertEquals(Pair(6, 30), ReminderScheduler.parseTimeString("6:30 ÖÖ"))
        assertEquals(Pair(20, 30), ReminderScheduler.parseTimeString("8:30 ÖS"))
    }
}

