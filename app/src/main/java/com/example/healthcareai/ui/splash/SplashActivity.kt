package com.example.healthcareai.ui.splash

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.animation.DecelerateInterpolator
import androidx.activity.OnBackPressedCallback
import androidx.core.animation.doOnEnd
import androidx.lifecycle.Lifecycle
import com.example.healthcareai.data.local.PreferenceHelper
import com.example.healthcareai.databinding.ActivitySplashBinding
import com.example.healthcareai.ui.base.BaseActivity
import com.example.healthcareai.ui.intro.IntroActivity
import com.example.healthcareai.ui.language.LanguageActivity
import com.example.healthcareai.ui.main.MainActivity
import com.example.healthcareai.ui.uninstall.UninstallActivity
import com.example.healthcareai.util.ShortcutHelper

@SuppressLint("CustomSplashScreen")
class SplashActivity : BaseActivity() {

    private lateinit var binding: ActivitySplashBinding
    private lateinit var preferenceHelper: PreferenceHelper
    private var progressAnimator: ValueAnimator? = null
    private var isNavigatingOrFinished = false
    private var isPendingNavigation = false

    override fun onCreate(savedInstanceState: Bundle?) {
        preferenceHelper = PreferenceHelper.getInstance(this)

        // 1. Enforce reset if previous onboarding was incomplete
        if (!preferenceHelper.isIntroCompleted) {
            preferenceHelper.clearLanguageIfIntroNotCompleted(this)
        }

        super.onCreate(savedInstanceState)

        // 2. Intercept Dynamic Uninstall shortcut
        if (isForUninstall(intent)) {
            intent?.action = null
            intent?.removeExtra("is_for_uninstall")
            startActivity(Intent(this, UninstallActivity::class.java))
            finish()
            return
        }

        // Prevent duplicate task creation when launched from launcher
        if (!isTaskRoot && preferenceHelper.isIntroCompleted &&
            (intent?.action == Intent.ACTION_MAIN || intent?.hasCategory(Intent.CATEGORY_LAUNCHER) == true)) {
            finish()
            return
        }

        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup Dynamic Shortcuts (e.g. Uninstall retention shortcut)
        ShortcutHelper.setupUninstallShortcut(this)

        // Cancel on back press
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                cancelSplashAndFinish()
            }
        })

        startLoadingProgress()
    }

    private fun startLoadingProgress() {
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
        navigateNext()
    }

    private fun navigateNext() {
        if (isNavigatingOrFinished || isFinishing || isDestroyed) return
        isNavigatingOrFinished = true

        val nextIntent = when {
            !preferenceHelper.isFirstLanguageSet -> Intent(this, LanguageActivity::class.java)
            !preferenceHelper.isIntroCompleted -> Intent(this, IntroActivity::class.java)
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
        progressAnimator?.cancel()
        finish()
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
