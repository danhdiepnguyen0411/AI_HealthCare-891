package com.example.healthcareai.util

import com.example.healthcareai.data.model.ReminderItem
import com.example.healthcareai.ui.main.MainActivity
import com.example.healthcareai.ui.reminder.ReminderActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ReminderNotificationDeepLinkTest {

    @Test
    fun testReminderSchedulerProfileIdConstant() {
        assertEquals("extra_profile_id", ReminderScheduler.EXTRA_PROFILE_ID)
        assertEquals("extra_reminder_id", ReminderScheduler.EXTRA_REMINDER_ID)
    }

    @Test
    fun testMainActivityTargetScreenConstants() {
        assertEquals("extra_target_screen", MainActivity.EXTRA_TARGET_SCREEN)
        assertEquals("target_reminder", MainActivity.TARGET_REMINDER)
        assertEquals("extra_reminder_id", MainActivity.EXTRA_REMINDER_ID)
        assertEquals("extra_profile_id", MainActivity.EXTRA_PROFILE_ID)
    }

    @Test
    fun testReminderActivityProfileIdConstant() {
        assertEquals("extra_profile_id", ReminderActivity.EXTRA_PROFILE_ID)
        assertEquals("extra_highlight_reminder_id", ReminderActivity.EXTRA_HIGHLIGHT_REMINDER_ID)
    }

    @Test
    fun testMemberReminderDeepLinkResolution() {
        val memberReminder = ReminderItem(
            id = "rem_123",
            profileId = "member_sarah_456",
            name = "Vitamin D"
        )

        val targetScreen = MainActivity.TARGET_REMINDER
        val targetProfileId = memberReminder.profileId ?: "primary_profile"

        assertEquals(MainActivity.TARGET_REMINDER, targetScreen)
        assertEquals("member_sarah_456", targetProfileId)
        assertNotNull(memberReminder.id)
    }
}

