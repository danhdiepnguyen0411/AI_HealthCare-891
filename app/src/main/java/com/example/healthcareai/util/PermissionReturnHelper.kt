package com.example.healthcareai.util

import android.app.Activity
import android.app.ActivityManager
import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.healthcareai.ui.main.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object PermissionReturnHelper {

    private const val TAG = "PermissionReturnHelper"
    private var activePollingJob: Job? = null

    // ─── Settings Openers ────────────────────────────────────────────────────────

    /**
     * Opens Application Details Settings page.
     * Note: Does NOT add FLAG_ACTIVITY_NEW_TASK when context is an Activity,
     * allowing the Settings screen to stay inside the current task stack for instant back-navigation.
     */
    fun openAppSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                if (context !is Activity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open app details settings", e)
        }
    }

    /**
     * Directly opens the specific Notification Settings toggle for this app on Android 8.0+.
     */
    fun openNotificationSettings(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    if (context !is Activity) {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                }
                context.startActivity(intent)
                return
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open notification settings directly, falling back to app details", e)
        }
        openAppSettings(context)
    }

    // ─── Generic Permission Poller ──────────────────────────────────────────────

    /**
     * Polls every [intervalMs] up to [maxDurationMs] to detect when the permission is granted in Settings.
     * Once granted, automatically brings the app to front and invokes [onGranted].
     */
    fun startPollingForPermission(
        context: Context,
        intervalMs: Long = 200L,
        maxDurationMs: Long = 120_000L,
        checkGranted: () -> Boolean,
        onGranted: (() -> Unit)? = null
    ) {
        stopPolling()
        activePollingJob = CoroutineScope(Dispatchers.IO + Job()).launch {
            val startTime = System.currentTimeMillis()
            while (isActive && (System.currentTimeMillis() - startTime) < maxDurationMs) {
                delay(intervalMs)
                if (checkGranted()) {
                    withContext(Dispatchers.Main) {
                        stopPolling()
                        bringAppToFront(context)
                        delay(200L) // Allow window focus to settle
                        onGranted?.invoke()
                    }
                    break
                }
            }
        }
    }

    fun stopPolling() {
        activePollingJob?.cancel()
        activePollingJob = null
    }

    // ─── Helpers for Specific Permissions ──────────────────────────────────────

    fun isNotificationGranted(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    fun isPermissionGranted(context: Context, permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    // ─── Bring App to Front (3-Tier Approach) ──────────────────────────────────

    /**
     * Robust 3-approach algorithm to pop the OS Settings activity and resume the app immediately:
     * 1. Re-launch target Activity with CLEAR_TOP + SINGLE_TOP + NEW_TASK.
     * 2. ActivityManager.moveTaskToFront (Requires android.permission.REORDER_TASKS in Manifest).
     * 3. PendingIntent with MODE_BACKGROUND_ACTIVITY_START_ALLOWED (Android 14+ / OEM ROMs).
     */
    fun bringAppToFront(context: Context) {
        val appContext = context.applicationContext ?: context
        val targetClass = if (context is Activity) context.javaClass else MainActivity::class.java

        // Approach 1: Intent with CLEAR_TOP + SINGLE_TOP
        try {
            val reopenIntent = Intent(context, targetClass).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_NEW_TASK
                )
            }
            context.startActivity(reopenIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Approach 1 (startActivity) failed", e)
        }

        // Approach 2: ActivityManager.moveTaskToFront
        try {
            val am = appContext.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val activity = context as? Activity
            val taskInfo = am?.appTasks?.firstOrNull()?.taskInfo
            val taskInfoId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                taskInfo?.taskId
            } else {
                @Suppress("DEPRECATION")
                taskInfo?.id
            }
            val targetTaskId = activity?.taskId ?: taskInfoId

            if (targetTaskId != null && targetTaskId != -1) {
                am?.moveTaskToFront(targetTaskId, ActivityManager.MOVE_TASK_WITH_HOME)
            }
            am?.appTasks?.forEach { task ->
                try { task.moveToFront() } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            Log.e(TAG, "Approach 2 (moveTaskToFront) failed", e)
        }

        // Approach 3: PendingIntent with background activity start mode allowed (Android 14+)
        try {
            val bringIntent = Intent(appContext, targetClass).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                appContext,
                2001,
                bringIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val options = ActivityOptions.makeBasic().apply {
                    pendingIntentBackgroundActivityStartMode =
                        ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                }
                pendingIntent.send(appContext, 0, null, null, null, null, options.toBundle())
            } else {
                pendingIntent.send()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Approach 3 (PendingIntent) failed", e)
        }
    }
}
