package com.example.healthcareai

import android.app.Application
import android.os.Build
import android.util.Log
import coil.Coil
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.claude.ArtMagicClient
import com.example.healthcareai.util.LocaleHelper
import com.example.healthcareai.util.NotificationHelper
import com.example.healthcareai.util.ShortcutHelper

class HealthCareApp : Application() {

    companion object {
        lateinit var instance: HealthCareApp
            private set
        lateinit var artMagicClient: ArtMagicClient
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

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

        // Initialize ArtMagic SDK and connect to server
        initArtMagicSdk()
    }

    private fun initArtMagicSdk() {
        try {
            val appId = BuildConfig.APP_ID
            val secretKey = BuildConfig.SECRET_KEY
            artMagicClient = ArtMagicClient(
                context = this,
                appId = appId,
                appSecretKey = secretKey
            )
            Log.d("HealthCareApp", "ArtMagicClient initialized successfully (appId: $appId)")

            // Test server connection and pre-fetch initial data
            artMagicClient.getTemplateApps(0, 10, "") { result, error ->
                if (error != null) {
                    Log.e("HealthCareApp", "ArtMagicClient server connection error: ${error.message}", error)
                } else {
                    Log.d("HealthCareApp", "ArtMagicClient connected to server successfully: $result")
                }
            }
        } catch (e: Throwable) {
            Log.e("HealthCareApp", "Failed to initialize ArtMagicClient: ${e.message}", e)
        }
    }
}
