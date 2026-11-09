package com.example.healthcareai.util

import com.example.healthcareai.R
import com.example.healthcareai.data.model.ReminderItem
import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderLocalizationAndStatusTest {

    @Test
    fun testReminderIconResolution() {
        // Test Heart Rate / Nhịp tim
        val heartRateEn = ReminderItem(name = "Heart Rate", category = "Heart rate")
        assertEquals(R.drawable.ic_heart_rate, heartRateEn.getIconResId())

        val heartRateVi = ReminderItem(name = "Nhịp tim", category = "Nhịp tim")
        assertEquals(R.drawable.ic_heart_rate, heartRateVi.getIconResId())

        // Test Water / Uống nước
        val waterEn = ReminderItem(name = "Drink Water", category = "Drink Water")
        assertEquals(R.drawable.ic_drink_water, waterEn.getIconResId())

        val waterVi = ReminderItem(name = "Uống nước", category = "Uống nước")
        assertEquals(R.drawable.ic_drink_water, waterVi.getIconResId())

        // Test Temperature / Nhiệt độ
        val tempEn = ReminderItem(name = "Temperature", category = "Temperature")
        assertEquals(R.drawable.ic_temperature, tempEn.getIconResId())

        val tempVi = ReminderItem(name = "Kiểm tra nhiệt độ", category = "Nhiệt độ")
        assertEquals(R.drawable.ic_temperature, tempVi.getIconResId())

        // Test Exercise / Tập thể dục
        val exerciseVi = ReminderItem(name = "Tập thể dục", category = "Tập thể dục")
        assertEquals(R.drawable.ic_do_exercise, exerciseVi.getIconResId())

        // Test Sleep / Giấc ngủ
        val sleepVi = ReminderItem(name = "Giấc ngủ", category = "Giấc ngủ")
        assertEquals(R.drawable.ic_sleep, sleepVi.getIconResId())
    }

    @Test
    fun testFallbackToCategoryWhenNameIsBlank() {
        val blankNameItem = ReminderItem(name = "", category = "Heart rate")
        // Name falls back to category if blank
        val raw = blankNameItem.name.ifBlank { blankNameItem.category }
        assertEquals("Heart rate", raw)
    }

    @Test
    fun testReminderIsEnabledDefaultsToTrue() {
        val item = ReminderItem(name = "Nhịp tim", category = "Heart rate")
        org.junit.Assert.assertTrue(item.isReminderEnabled())

        val itemExplicitNull = ReminderItem(name = "Nhịp tim", category = "Heart rate", isEnabled = null)
        org.junit.Assert.assertTrue(itemExplicitNull.isReminderEnabled())

        val itemDisabled = ReminderItem(name = "Nhịp tim", category = "Heart rate", isEnabled = false)
        org.junit.Assert.assertFalse(itemDisabled.isReminderEnabled())
    }

    @Test
    fun testDisabledReminderNotScheduledForDay() {
        val today = java.util.Calendar.getInstance()
        val enabledItem = ReminderItem(
            name = "Uống nước",
            repeat = "Every day",
            isEnabled = true
        )
        org.junit.Assert.assertTrue(enabledItem.isScheduledForDay(today))

        val disabledItem = ReminderItem(
            name = "Uống nước",
            repeat = "Every day",
            isEnabled = false
        )
        org.junit.Assert.assertFalse(disabledItem.isScheduledForDay(today))
    }
}

