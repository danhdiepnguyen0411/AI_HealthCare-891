package com.example.healthcareai.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.healthcareai.R
import com.example.healthcareai.ui.main.MainActivity
import com.example.healthcareai.ui.splash.SplashActivity

object NotificationHelper {

    const val CHANNEL_ID = "channel_daily_health_reminders"
    const val CHANNEL_REMINDERS_ID = "channel_health_user_reminders"

    private const val NOTIFICATION_ID = 9101

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                    ?: return

            // 1. Daily Health Tips Channel
            val name = context.getString(R.string.noti_channel_name)
            val descriptionText = context.getString(R.string.noti_channel_desc)
            val channelDaily = NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_HIGH).apply {
                description = descriptionText
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channelDaily)

            // 2. User Medication & Reminders Channel
            val reminderName = context.getString(R.string.noti_reminders_channel_name)
            val reminderDesc = context.getString(R.string.noti_reminders_channel_desc)
            val channelReminders = NotificationChannel(CHANNEL_REMINDERS_ID, reminderName, NotificationManager.IMPORTANCE_HIGH).apply {
                description = reminderDesc
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channelReminders)
        }
    }

    fun showScheduledNotification(context: Context, slot: String) {
        val attachedContext = LocaleHelper.onAttach(context)
        if (!NotificationManagerCompat.from(attachedContext).areNotificationsEnabled()) {
            return
        }

        createNotificationChannel(attachedContext)

        val (title, message, targetTab) = when (slot) {
            NotificationScheduler.SLOT_8H -> Triple(
                attachedContext.getString(R.string.noti_title_morning),
                attachedContext.getString(R.string.noti_msg_morning),
                MainActivity.TAB_HOME
            )
            NotificationScheduler.SLOT_15H -> Triple(
                attachedContext.getString(R.string.noti_title_afternoon),
                attachedContext.getString(R.string.noti_msg_afternoon),
                MainActivity.TAB_MY_HEALTH
            )
            NotificationScheduler.SLOT_21H -> Triple(
                attachedContext.getString(R.string.noti_title_evening),
                attachedContext.getString(R.string.noti_msg_evening),
                MainActivity.TAB_HOME
            )
            else -> Triple(
                attachedContext.getString(R.string.noti_title_morning),
                attachedContext.getString(R.string.noti_msg_morning),
                MainActivity.TAB_HOME
            )
        }

        val intent = Intent(attachedContext, SplashActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("from_notification", true)
            putExtra(MainActivity.EXTRA_TARGET_TAB, targetTab)
        }

        val pendingIntent = PendingIntent.getActivity(
            attachedContext,
            slot.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Custom notification layout matching Figma node 1:4045
        val collapsedViews = createCustomNotificationRemoteViews(
            context = attachedContext,
            title = title,
            message = message,
            isExpanded = false
        )
        val expandedViews = createCustomNotificationRemoteViews(
            context = attachedContext,
            title = title,
            message = message,
            isExpanded = true
        )

        val notification = NotificationCompat.Builder(attachedContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setCustomContentView(collapsedViews)
            .setCustomBigContentView(expandedViews)
            .setCustomHeadsUpContentView(collapsedViews)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(attachedContext).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
        }
    }

    /**
     * Shows notification for a specific user reminder (e.g. medication, water, appointment).
     * Guaranteed heads-up notification with action button.
     */
    fun showReminderNotification(
        context: Context,
        reminderId: String,
        name: String,
        description: String,
        category: String,
        timeIndex: Int,
        profileId: String? = null
    ) {
        val attachedContext = LocaleHelper.onAttach(context)
        if (!NotificationManagerCompat.from(attachedContext).areNotificationsEnabled()) {
            return
        }

        createNotificationChannel(attachedContext)

        val notiId = ((reminderId.hashCode() * 31 + timeIndex) and 0x7FFFFFFF)

        val effectiveProfileId = profileId
            ?: com.example.healthcareai.data.repository.ReminderRepository.getInstance(attachedContext).getReminder(reminderId)?.profileId
            ?: com.example.healthcareai.data.repository.ProfileRepository.getInstance(attachedContext).getPrimaryProfile().id

        // Main click intent -> navigate directly to member's reminder screen
        val openIntent = Intent(attachedContext, SplashActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("from_notification", true)
            putExtra(MainActivity.EXTRA_TARGET_SCREEN, MainActivity.TARGET_REMINDER)
            putExtra(MainActivity.EXTRA_REMINDER_ID, reminderId)
            putExtra(MainActivity.EXTRA_PROFILE_ID, effectiveProfileId)
            putExtra(MainActivity.EXTRA_TARGET_TAB, MainActivity.TAB_HOME)
        }
        val openPendingIntent = PendingIntent.getActivity(
            attachedContext,
            notiId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // "Mark as done" action button
        val doneIntent = Intent(attachedContext, NotificationReceiver::class.java).apply {
            action = ReminderScheduler.ACTION_MARK_REMINDER_DONE
            putExtra(ReminderScheduler.EXTRA_REMINDER_ID, reminderId)
            putExtra("extra_notification_id", notiId)
        }
        val donePendingIntent = PendingIntent.getBroadcast(
            attachedContext,
            notiId + 1000,
            doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val bodyText = if (description.isNotBlank()) {
            description
        } else {
            attachedContext.getString(R.string.noti_reminder_default_desc, name)
        }

        // Custom notification layout matching Figma node 1:4045
        val collapsedViews = createCustomNotificationRemoteViews(
            context = attachedContext,
            title = name,
            message = bodyText,
            isExpanded = false
        )
        val expandedViews = createCustomNotificationRemoteViews(
            context = attachedContext,
            title = name,
            message = bodyText,
            isExpanded = true
        )

        val notification = NotificationCompat.Builder(attachedContext, CHANNEL_REMINDERS_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(name)
            .setContentText(bodyText)
            .setCustomContentView(collapsedViews)
            .setCustomBigContentView(expandedViews)
            .setCustomHeadsUpContentView(collapsedViews)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .addAction(
                R.drawable.ic_check,
                attachedContext.getString(R.string.noti_reminder_action_done),
                donePendingIntent
            )
            .build()

        try {
            NotificationManagerCompat.from(attachedContext).notify(notiId, notification)
        } catch (_: SecurityException) {
        }
    }

    /**
     * Builds custom notification RemoteViews matching Figma node 1:4045
     */
    private fun createCustomNotificationRemoteViews(
        context: Context,
        title: String,
        message: String,
        isExpanded: Boolean
    ): RemoteViews {
        val layoutRes = if (isExpanded) {
            R.layout.layout_custom_notification_expanded
        } else {
            R.layout.layout_custom_notification
        }
        return RemoteViews(context.packageName, layoutRes).apply {
            setImageViewResource(R.id.ivNotiIcon, R.drawable.ic_notification_logo)
            setTextViewText(R.id.tvNotiAppName, context.getString(R.string.noti_app_name_display))
            setTextViewText(R.id.tvNotiTime, context.getString(R.string.noti_time_now))
            setTextViewText(R.id.tvNotiTitle, title)
            setTextViewText(R.id.tvNotiMessage, message)
        }
    }

    /**
     * Test notification directly matching Figma node 1:4045
     */
    fun showTestNotification(context: Context) {
        showReminderNotification(
            context = context,
            reminderId = "test_reminder_exercise",
            name = "Time to exercise",
            description = "Let's keep your body active!",
            category = "Exercise",
            timeIndex = 0
        )
    }
}
