package com.example.healthcareai.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.healthcareai.R
import com.example.healthcareai.ui.main.MainActivity
import com.example.healthcareai.ui.splash.SplashActivity

object NotificationHelper {

    const val CHANNEL_ID = "channel_daily_health_reminders"
    private const val NOTIFICATION_ID = 9101

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.noti_channel_name)
            val descriptionText = context.getString(R.string.noti_channel_desc)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
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

        val notification = NotificationCompat.Builder(attachedContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_sl_my_health)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(attachedContext).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
        }
    }
}
