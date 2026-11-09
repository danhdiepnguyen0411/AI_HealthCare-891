package com.example.healthcareai.ui.base

import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updateLayoutParams
import com.example.healthcareai.data.local.PreferenceHelper
import com.example.healthcareai.util.LocaleHelper

abstract class BaseActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }

    override fun applyOverrideConfiguration(overrideConfiguration: android.content.res.Configuration?) {
        if (overrideConfiguration != null) {
            val langCode = PreferenceHelper.getInstance(this).selectedLanguageCode
            val locale = LocaleHelper.getLocale(langCode)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                overrideConfiguration.setLocale(locale)
                overrideConfiguration.setLayoutDirection(locale)
            } else {
                @Suppress("DEPRECATION")
                overrideConfiguration.locale = locale
                @Suppress("DEPRECATION")
                overrideConfiguration.setLayoutDirection(locale)
            }
        }
        super.applyOverrideConfiguration(overrideConfiguration)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } catch (_: Exception) {
            // Guard against translucent orientation crash on Android 8.0
        }
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val langCode = PreferenceHelper.getInstance(this).selectedLanguageCode
        LocaleHelper.updateResources(this, langCode)
        LocaleHelper.applyLayoutDirection(this, langCode)
        hideNavigationBar()
    }

    override fun onResume() {
        super.onResume()
        val langCode = PreferenceHelper.getInstance(this).selectedLanguageCode
        LocaleHelper.updateResources(this, langCode)
        LocaleHelper.applyLayoutDirection(this, langCode)
        hideNavigationBar()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideNavigationBar()
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (ev?.action == MotionEvent.ACTION_DOWN) {
            val v = currentFocus
            if (v is EditText) {
                val outRect = Rect()
                v.getGlobalVisibleRect(outRect)
                if (!outRect.contains(ev.rawX.toInt(), ev.rawY.toInt())) {
                    v.clearFocus()
                    hideKeyboard()
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    open fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        val view = currentFocus ?: window.decorView
        imm?.hideSoftInputFromWindow(view.windowToken, 0)
        window.decorView.postDelayed({
            if (!isDestroyed && !isFinishing) {
                hideNavigationBar()
            }
        }, 350L)
    }

    fun hideNavigationBar() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.navigationBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            )
        }
    }

    /**
     * Standardized top inset configuration for topBar / header across all activities.
     * Ensures consistent comfortable spacing (16dp by default) below the system status bar.
     */
    fun setupHeaderTopMargin(
        headerView: View,
        rootView: View = headerView.rootView,
        additionalTopDp: Int = 16
    ) {
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { _, insets ->
            headerView.applyHeaderTopMargin(insets, additionalTopDp)
            insets
        }

        ViewCompat.getRootWindowInsets(rootView)?.let { currentInsets ->
            headerView.applyHeaderTopMargin(currentInsets, additionalTopDp)
        }
    }

    companion object {
        fun applyHeaderTopMargin(
            headerView: View,
            insets: WindowInsetsCompat,
            additionalTopDp: Int = 16
        ) {
            val statusBarTop = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            val density = headerView.resources.displayMetrics.density
            val extraPx = (additionalTopDp * density).toInt()
            val fallbackPx = ((additionalTopDp + 8) * density).toInt()
            val targetMarginTop = if (statusBarTop > 0) statusBarTop + extraPx else fallbackPx

            headerView.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                if (topMargin != targetMarginTop) {
                    topMargin = targetMarginTop
                }
            }
        }
    }
}

/**
 * Extension function allowing any View (topBar / header) to apply standardized top margin
 * from WindowInsetsCompat. Can be called inside custom setOnApplyWindowInsetsListener.
 */
fun View.applyHeaderTopMargin(insets: WindowInsetsCompat, additionalTopDp: Int = 16) {
    BaseActivity.applyHeaderTopMargin(this, insets, additionalTopDp)
}
