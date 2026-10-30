package com.example.healthcareai.util

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import com.example.healthcareai.data.local.PreferenceHelper
import com.example.healthcareai.ui.dialog.CustomNotificationDialog

object NotificationPermissionHelper {

    /**
     * Checks notification permission and follows the strict 2 native attempts threshold:
     * - Attempt 1 & 2 (Android 13+): native system permission prompt via [launcher].
     * - Attempt 3+ (or whenever denialCount >= 2): custom rationale dialog matching design.
     */
    fun checkAndRequest(
        activity: Activity,
        launcher: ActivityResultLauncher<String>?,
        onGranted: () -> Unit,
        onDenied: (() -> Unit)? = null
    ): Dialog? {
        val preferenceHelper = PreferenceHelper.getInstance(activity)

        if (PermissionReturnHelper.isNotificationGranted(activity)) {
            preferenceHelper.notiDenialCount = 0
            onGranted()
            return null
        }

        val denialCount = preferenceHelper.notiDenialCount

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && denialCount < 2 && launcher != null) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            null
        } else {
            CustomNotificationDialog.show(activity, onGranted = {
                preferenceHelper.notiDenialCount = 0
                onGranted()
            }, onDenied = onDenied)
        }
    }

    /**
     * Handles the callback from native [ActivityResultLauncher] for POST_NOTIFICATIONS:
     * - If granted -> reset denial count to 0, invoke [onGranted].
     * - If denied -> increment denial count, invoke [onDenied]. Next launches will show custom popup.
     */
    fun onPermissionResult(
        activity: Activity,
        isGranted: Boolean,
        onGranted: () -> Unit,
        onDenied: (() -> Unit)? = null
    ): Dialog? {
        val preferenceHelper = PreferenceHelper.getInstance(activity)
        return if (isGranted) {
            preferenceHelper.notiDenialCount = 0
            onGranted()
            null
        } else {
            preferenceHelper.notiDenialCount++
            onDenied?.invoke()
            null
        }
    }
}
