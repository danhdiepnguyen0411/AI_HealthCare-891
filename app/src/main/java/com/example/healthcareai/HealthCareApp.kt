package com.example.healthcareai

import android.app.Application
import com.example.healthcareai.util.LocaleHelper
import com.example.healthcareai.util.NotificationHelper
import com.example.healthcareai.util.ShortcutHelper

class HealthCareApp : Application() {

    override fun onCreate() {
        super.onCreate()
        LocaleHelper.onAttach(this)
        NotificationHelper.createNotificationChannel(this)
        ShortcutHelper.setupUninstallShortcut(this)
    }
}
