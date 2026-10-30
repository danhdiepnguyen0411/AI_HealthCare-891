package com.example.healthcareai.ui.splash

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.animation.DecelerateInterpolator
import androidx.activity.OnBackPressedCallback
import androidx.core.animation.doOnEnd
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import android.Manifest
import android.app.Dialog
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import com.example.healthcareai.data.local.PreferenceHelper
import com.example.healthcareai.databinding.ActivitySplashBinding
import com.example.healthcareai.ui.base.BaseActivity
import com.example.healthcareai.ui.intro.IntroActivity
import com.example.healthcareai.ui.language.LanguageActivity
import com.example.healthcareai.ui.main.MainActivity
import com.example.healthcareai.ui.uninstall.UninstallActivity
import com.example.healthcareai.util.NotificationPermissionHelper
import com.example.healthcareai.util.NotificationScheduler
import com.example.healthcareai.util.PermissionReturnHelper
import com.example.healthcareai.util.ReminderScheduler
import com.example.healthcareai.util.ShortcutHelper

@SuppressLint("CustomSplashScreen")
class SplashActivity : BaseActivity() {

    private lateinit var binding: ActivitySplashBinding
    private lateinit var preferenceHelper: PreferenceHelper
    private var progressAnimator: ValueAnimator? = null
    private var isNavigatingOrFinished = false
    private var isPendingNavigation = false
    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            preferenceHelper.notiDenialCount = 0
        } else {
            preferenceHelper.notiDenialCount++
        }
        onNotificationHandledAndNavigate()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        preferenceHelper = PreferenceHelper.getInstance(this)

        // 1. Enforce reset if previous onboarding was incomplete
        if (!preferenceHelper.isIntroCompleted) {
            preferenceHelper.clearLanguageIfIntroNotCompleted(this)
        }

        super.onCreate(savedInstanceState)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                cancelSplashAndFinish()
            }
        })

        // Prevent duplicate task creation when launched from launcher (unless it is for uninstall)
        if (!isForUninstall(intent)) {
            if (!isTaskRoot && preferenceHelper.isIntroCompleted &&
                (intent?.action == Intent.ACTION_MAIN || intent?.hasCategory(Intent.CATEGORY_LAUNCHER) == true)) {
                finish()
                return
            }
        }

        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Ensure white status bar icons on top of blue background
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }

        // Setup Dynamic Shortcuts (e.g. Uninstall retention shortcut)
        ShortcutHelper.setupUninstallShortcut(this)

        startLoadingProgress()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (isForUninstall(intent)) {
            if (progressAnimator == null || !progressAnimator!!.isRunning) {
                startLoadingProgress()
            }
        } else if (!isTaskRoot && preferenceHelper.isIntroCompleted &&
            (intent.action == Intent.ACTION_MAIN || intent.hasCategory(Intent.CATEGORY_LAUNCHER))) {
            finish()
        }
    }

    private fun startLoadingProgress() {
        isNavigatingOrFinished = false
        isPendingNavigation = false
        progressAnimator?.removeAllUpdateListeners()
        progressAnimator?.removeAllListeners()
        progressAnimator?.cancel()

        progressAnimator = ValueAnimator.ofInt(0, 100).apply {
            duration = 2500L
            interpolator = DecelerateInterpolator()
            addUpdateListener { animator ->
                val progress = animator.animatedValue as Int
                binding.progressBar.progress = progress
            }
            doOnEnd {
                if (!isNavigatingOrFinished && !isFinishing && !isDestroyed) {
                    if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                        onSplashFinished()
                    } else {
                        isPendingNavigation = true
                    }
                }
            }
            start()
        }
    }

    override fun onResume() {
        super.onResume()
        if (isNavigatingOrFinished || isFinishing || isDestroyed) return

        if (isPendingNavigation) {
            isPendingNavigation = false
            onSplashFinished()
        } else if (progressAnimator?.isPaused == true) {
            progressAnimator?.resume()
        }
    }

    override fun onPause() {
        super.onPause()
        if (progressAnimator?.isRunning == true) {
            progressAnimator?.pause()
        }
    }

    private fun onSplashFinished() {
        if (isNavigatingOrFinished || isFinishing || isDestroyed) return

        // If launched via Uninstall shortcut -> navigate to UninstallActivity
        if (isForUninstall(intent)) {
            isNavigatingOrFinished = true
            intent?.action = null
            intent?.removeExtra("is_for_uninstall")
            startActivity(Intent(this, UninstallActivity::class.java))
            finish()
            return
        }

        handleNotificationPermissionAtSplashEnd()
    }

    private fun handleNotificationPermissionAtSplashEnd() {
        if (PermissionReturnHelper.isNotificationGranted(this)) {
            preferenceHelper.notiDenialCount = 0
            NotificationScheduler.scheduleDailyNotifications(this)
            ReminderScheduler.rescheduleAllReminders(this)
            navigateNext()
            return
        }

        val denialCount = preferenceHelper.notiDenialCount
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && denialCount < 2) {
            // First 2 times: Launch native OS notification permission dialog on Splash
            requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            // 3rd time onwards (denialCount >= 2): Do not show on Splash,
            // defer custom notification dialog to be shown when entering Home (MainActivity)
            navigateNext()
        }
    }

    private fun onNotificationHandledAndNavigate() {
        if (isNavigatingOrFinished || isFinishing || isDestroyed) return
        NotificationScheduler.scheduleDailyNotifications(this)
        ReminderScheduler.rescheduleAllReminders(this)
        navigateNext()
    }

    private fun navigateNext() {
        if (isNavigatingOrFinished || isFinishing || isDestroyed) return
        isNavigatingOrFinished = true

        val nextIntent = when {
            !preferenceHelper.isFirstLanguageSet -> Intent(this, LanguageActivity::class.java)
            !preferenceHelper.isIntroCompleted -> Intent(this, IntroActivity::class.java)
            !preferenceHelper.isProfileCreated -> Intent(this, com.example.healthcareai.ui.profile.AddProfileActivity::class.java)
            else -> {
                Intent(this, MainActivity::class.java).apply {
                    if (intent?.getBooleanExtra("from_notification", false) == true) {
                        putExtra("from_notification", true)
                        putExtra(MainActivity.EXTRA_TARGET_TAB, intent?.getStringExtra(MainActivity.EXTRA_TARGET_TAB))
                    }
                }
            }
        }
        startActivity(nextIntent)
        finish()
    }

    private fun cancelSplashAndFinish() {
        if (isNavigatingOrFinished) return
        isNavigatingOrFinished = true
        isPendingNavigation = false
        progressAnimator?.removeAllUpdateListeners()
        progressAnimator?.removeAllListeners()
        progressAnimator?.cancel()
        progressAnimator = null
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        isNavigatingOrFinished = true
        isPendingNavigation = false
        progressAnimator?.removeAllUpdateListeners()
        progressAnimator?.removeAllListeners()
        progressAnimator?.cancel()
        progressAnimator = null
    }

    companion object {
        fun isForUninstall(intent: Intent?): Boolean {
            if (intent == null) return false
            if (intent.action == ShortcutHelper.ACTION_UNINSTALL_SHORTCUT) return true
            if (intent.getBooleanExtra("is_for_uninstall", false)) return true
            if (intent.getStringExtra("is_for_uninstall") == "true") return true
            return false
        }
    }
}
