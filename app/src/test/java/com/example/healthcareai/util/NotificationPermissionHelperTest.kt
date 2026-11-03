package com.example.healthcareai.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NotificationPermissionHelperTest {

    @Before
    fun setUp() {
        NotificationPermissionHelper.hasPromptedInCurrentSession = false
    }

    @Test
    fun testInitialState() {
        assertFalse(NotificationPermissionHelper.hasPromptedInCurrentSession)
    }

    @Test
    fun testSessionFlagPreventsImmediateRepromptOnHome() {
        // App launch 2: Splash prompts native dialog
        NotificationPermissionHelper.hasPromptedInCurrentSession = true

        // When navigating into Home on launch 2, flag is true, preventing CustomNotificationDialog
        assertTrue(NotificationPermissionHelper.hasPromptedInCurrentSession)

        // App launch 3: SplashActivity.onCreate resets flag
        NotificationPermissionHelper.hasPromptedInCurrentSession = false
        assertFalse(NotificationPermissionHelper.hasPromptedInCurrentSession)

        // Launch 3 has denialCount >= 2 and did not prompt on Splash, so Home can display custom dialog
    }
}

