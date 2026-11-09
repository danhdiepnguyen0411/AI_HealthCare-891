package com.example.healthcareai.util

import com.example.healthcareai.ui.reminder.HorizontalCalendarAdapter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class CalendarHighlightAndScrollTest {

    @Test
    fun testIsSameDayLogic() {
        val today = Calendar.getInstance()
        val sameToday = today.clone() as Calendar

        val tomorrow = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
        val nextYear = (today.clone() as Calendar).apply { add(Calendar.YEAR, 1) }

        assertTrue(HorizontalCalendarAdapter.isSameDay(today, sameToday))
        assertFalse(HorizontalCalendarAdapter.isSameDay(today, tomorrow))
        assertFalse(HorizontalCalendarAdapter.isSameDay(today, nextYear))
    }

    @Test
    fun testDayStateDifferentiation() {
        val today = Calendar.getInstance()
        val tomorrow = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
        val yesterday = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }

        // When today is selected
        var selectedDay = today
        assertTrue(HorizontalCalendarAdapter.isSameDay(today, selectedDay)) // selected && isToday
        assertFalse(HorizontalCalendarAdapter.isSameDay(tomorrow, selectedDay)) // unselected
        assertFalse(HorizontalCalendarAdapter.isSameDay(tomorrow, today)) // not today

        // When user switches to tomorrow
        selectedDay = tomorrow
        assertFalse(HorizontalCalendarAdapter.isSameDay(today, selectedDay)) // today is NOT selected
        assertTrue(HorizontalCalendarAdapter.isSameDay(today, today)) // today is STILL today!
        assertTrue(HorizontalCalendarAdapter.isSameDay(tomorrow, selectedDay)) // tomorrow is selected
    }

    @Test
    fun testDayOfWeekAbbreviationVietnamese() {
        val cal = Calendar.getInstance()
        val viLocale = java.util.Locale.forLanguageTag("vi")

        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        assertEquals("T2", HorizontalCalendarAdapter.getDayOfWeekAbbreviation(cal, viLocale))

        cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        assertEquals("CN", HorizontalCalendarAdapter.getDayOfWeekAbbreviation(cal, viLocale))
    }
}

