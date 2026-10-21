package com.example.healthcareai

import android.app.Application
import android.os.Build
import coil.Coil
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.example.healthcareai.util.LocaleHelper
import com.example.healthcareai.util.NotificationHelper
import com.example.healthcareai.util.ShortcutHelper

class HealthCareApp : Application() {

    override fun onCreate() {
        super.onCreate()
        LocaleHelper.onAttach(this)
        NotificationHelper.createNotificationChannel(this)
        ShortcutHelper.setupUninstallShortcut(this)

        val imageLoader = ImageLoader.Builder(this)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .crossfade(true)
            .build()
        Coil.setImageLoader(imageLoader)
    }
}
