package com.example.healthcareai.util

import com.example.healthcareai.data.model.ReminderItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.util.Locale

class ReminderWheelTimeFormatTest {

    private fun formatWheelTime(hour24: Int, minute: Int): String {
        val hour12 = when {
            hour24 == 0 -> 12
            hour24 > 12 -> hour24 - 12
            else -> hour24
        }
        val amPm = if (hour24 >= 12) "PM" else "AM"
        return String.format(Locale.US, "%02d:%02d %s", hour12, minute, amPm)
    }

    private fun getNormalizedPosition(index: Int, count: Int): Int {
        if (count == 0) return 0
        return ((index % count) + count) % count
    }

    @Test
    fun testUserScreenshotTime1423() {
        val formatted = formatWheelTime(14, 23)
        assertEquals("02:23 PM", formatted)

        val parsed = ReminderScheduler.parseTimeString(formatted)
        assertNotNull(parsed)
        assertEquals(14, parsed!!.first)
        assertEquals(23, parsed.second)
    }

    @Test
    fun testMidnightAndNoon() {
        val midnight = formatWheelTime(0, 0)
        assertEquals("12:00 AM", midnight)
        val parsedMidnight = ReminderScheduler.parseTimeString(midnight)
        assertNotNull(parsedMidnight)
        assertEquals(0, parsedMidnight!!.first)
        assertEquals(0, parsedMidnight.second)

        val noon = formatWheelTime(12, 0)
        assertEquals("12:00 PM", noon)
        val parsedNoon = ReminderScheduler.parseTimeString(noon)
        assertNotNull(parsedNoon)
        assertEquals(12, parsedNoon!!.first)
        assertEquals(0, parsedNoon.second)
    }

    @Test
    fun testEdgeTimes() {
        val earlyMorning = formatWheelTime(1, 5)
        assertEquals("01:05 AM", earlyMorning)
        val parsedEarly = ReminderScheduler.parseTimeString(earlyMorning)
        assertNotNull(parsedEarly)
        assertEquals(1, parsedEarly!!.first)
        assertEquals(5, parsedEarly.second)

        val lateNight = formatWheelTime(23, 59)
        assertEquals("11:59 PM", lateNight)
        val parsedLate = ReminderScheduler.parseTimeString(lateNight)
        assertNotNull(parsedLate)
        assertEquals(23, parsedLate!!.first)
        assertEquals(59, parsedLate.second)
    }

    @Test
    fun testWheelCyclicNormalization() {
        val hourCount = 24
        assertEquals(0, getNormalizedPosition(0, hourCount))
        assertEquals(23, getNormalizedPosition(-1, hourCount))
        assertEquals(22, getNormalizedPosition(-2, hourCount))
        assertEquals(0, getNormalizedPosition(24, hourCount))
        assertEquals(1, getNormalizedPosition(25, hourCount))
        assertEquals(14, getNormalizedPosition(14 + 24 * 3, hourCount))
        assertEquals(14, getNormalizedPosition(14 - 24 * 5, hourCount))

        val minuteCount = 60
        assertEquals(0, getNormalizedPosition(0, minuteCount))
        assertEquals(59, getNormalizedPosition(-1, minuteCount))
        assertEquals(23, getNormalizedPosition(23 + 60 * 2, minuteCount))
        assertEquals(23, getNormalizedPosition(23 - 60 * 4, minuteCount))
    }

    @Test
    fun testSingleTimeReminderItemDisplay() {
        val reminder = ReminderItem(
            name = "Aspirin",
            timeList = listOf(formatWheelTime(14, 23))
        )

        assertEquals(1, reminder.timeList.size)
        assertEquals("02:23 PM", reminder.timeList[0])
        assertEquals("02:23 PM", reminder.getDisplayTime(null))
    }
}

