package com.example.healthcareai.util

import com.example.healthcareai.data.model.ReminderItem
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ReminderScheduleRepeatTest {

    @Test
    fun testOnceRepeatOnlyScheduledOnSetDay() {
        val reminder = ReminderItem(
            name = "Take pill",
            repeat = "Once",
            startDate = "2026-11-09"
        )

        val setDay = Calendar.getInstance().apply { set(2026, Calendar.NOVEMBER, 9) }
        val nextDay = Calendar.getInstance().apply { set(2026, Calendar.NOVEMBER, 10) }
        val prevDay = Calendar.getInstance().apply { set(2026, Calendar.NOVEMBER, 8) }

        assertTrue(reminder.isScheduledForDay(setDay))
        assertFalse(reminder.isScheduledForDay(nextDay))
        assertFalse(reminder.isScheduledForDay(prevDay))
    }

    @Test
    fun testEveryDayRepeatScheduledEverywhere() {
        val reminder = ReminderItem(
            name = "Drink water",
            repeat = "Every day"
        )

        val monday = Calendar.getInstance().apply { set(2026, Calendar.NOVEMBER, 9) }
        val sunday = Calendar.getInstance().apply { set(2026, Calendar.NOVEMBER, 15) }

        assertTrue(reminder.isScheduledForDay(monday))
        assertTrue(reminder.isScheduledForDay(sunday))
    }

    @Test
    fun testWeekdaysRepeatScheduledMonToFriOnly() {
        val reminder = ReminderItem(
            name = "Exercise",
            repeat = "Weekdays"
        )

        val monday = Calendar.getInstance().apply { set(2026, Calendar.NOVEMBER, 9) } // Monday
        val friday = Calendar.getInstance().apply { set(2026, Calendar.NOVEMBER, 13) } // Friday
        val saturday = Calendar.getInstance().apply { set(2026, Calendar.NOVEMBER, 14) } // Saturday
        val sunday = Calendar.getInstance().apply { set(2026, Calendar.NOVEMBER, 15) } // Sunday

        assertTrue(reminder.isScheduledForDay(monday))
        assertTrue(reminder.isScheduledForDay(friday))
        assertFalse(reminder.isScheduledForDay(saturday))
        assertFalse(reminder.isScheduledForDay(sunday))
    }

    @Test
    fun testCustomRepeatScheduledOnChosenDaysOnly() {
        // Mon (2), Wed (4), Fri (6)
        val reminder = ReminderItem(
            name = "Yoga",
            repeat = "Custom:2,4,6"
        )

        val monday = Calendar.getInstance().apply { set(2026, Calendar.NOVEMBER, 9) } // Monday (2)
        val tuesday = Calendar.getInstance().apply { set(2026, Calendar.NOVEMBER, 10) } // Tuesday (3)
        val wednesday = Calendar.getInstance().apply { set(2026, Calendar.NOVEMBER, 11) } // Wednesday (4)
        val thursday = Calendar.getInstance().apply { set(2026, Calendar.NOVEMBER, 12) } // Thursday (5)
        val friday = Calendar.getInstance().apply { set(2026, Calendar.NOVEMBER, 13) } // Friday (6)

        assertTrue(reminder.isScheduledForDay(monday))
        assertFalse(reminder.isScheduledForDay(tuesday))
        assertTrue(reminder.isScheduledForDay(wednesday))
        assertFalse(reminder.isScheduledForDay(thursday))
        assertTrue(reminder.isScheduledForDay(friday))
    }
}
