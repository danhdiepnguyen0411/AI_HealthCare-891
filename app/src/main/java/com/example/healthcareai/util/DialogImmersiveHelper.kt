package com.example.healthcareai.util

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog

object DialogImmersiveHelper {

    fun hideWindowNavigationBar(window: Window) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        window.addFlags(
            WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.navigationBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            )
        }
    }

    fun setupBottomSheetImmersive(dialog: BottomSheetDialog) {
        val win = dialog.window
        if (win != null) {
            hideWindowNavigationBar(win)
            win.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            win.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            win.decorView.background = null
            win.decorView.setBackgroundColor(Color.TRANSPARENT)
        }

        dialog.findViewById<View>(android.R.id.content)?.let { content ->
            content.background = null
            content.setBackgroundColor(Color.TRANSPARENT)
        }

        val container = dialog.findViewById<FrameLayout>(com.google.android.material.R.id.container)
        container?.fitsSystemWindows = false
        container?.clipChildren = false
        container?.clipToPadding = false
        container?.setPadding(0, 0, 0, 0)
        container?.background = null
        container?.setBackgroundColor(Color.TRANSPARENT)
        container?.let {
            ViewCompat.setOnApplyWindowInsetsListener(it) { view, _ ->
                view.setPadding(0, 0, 0, 0)
                WindowInsetsCompat.CONSUMED
            }
        }

        val coordinator = dialog.findViewById<CoordinatorLayout>(com.google.android.material.R.id.coordinator)
        coordinator?.fitsSystemWindows = false
        coordinator?.clipChildren = false
        coordinator?.clipToPadding = false
        coordinator?.setPadding(0, 0, 0, 0)
        coordinator?.background = null
        coordinator?.setBackgroundColor(Color.TRANSPARENT)
        coordinator?.let {
            ViewCompat.setOnApplyWindowInsetsListener(it) { view, _ ->
                view.setPadding(0, 0, 0, 0)
                WindowInsetsCompat.CONSUMED
            }
        }

        val bottomSheet = dialog.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
        bottomSheet?.let { sheet ->
            sheet.fitsSystemWindows = false
            sheet.clipChildren = false
            sheet.clipToPadding = false
            sheet.setPadding(0, 0, 0, 0)
            sheet.background = null
            sheet.setBackgroundColor(Color.TRANSPARENT)
            sheet.elevation = 0f
            sheet.outlineProvider = null
            ViewCompat.setElevation(sheet, 0f)
            ViewCompat.setBackground(sheet, null)

            sheet.addOnLayoutChangeListener { v, _, _, _, _, _, _, _, _ ->
                if (v.background != null) {
                    v.background = null
                    v.setBackgroundColor(Color.TRANSPARENT)
                }
                if (v.elevation != 0f) v.elevation = 0f
                if (v.outlineProvider != null) v.outlineProvider = null
            }

            ViewCompat.setOnApplyWindowInsetsListener(sheet) { view, _ ->
                view.setPadding(0, 0, 0, 0)
                WindowInsetsCompat.CONSUMED
            }

            try {
                val behavior = BottomSheetBehavior.from(sheet)
                try {
                    val field = BottomSheetBehavior::class.java.getDeclaredField("materialShapeDrawable")
                    field.isAccessible = true
                    field.set(behavior, null)
                } catch (_: Throwable) {}
                try {
                    val tintField = BottomSheetBehavior::class.java.getDeclaredField("backgroundTint")
                    tintField.isAccessible = true
                    tintField.set(behavior, null)
                } catch (_: Throwable) {}

                behavior.isGestureInsetBottomIgnored = true
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
            } catch (_: Exception) {}
        }
    }

    /**
     * Call this instead of dialog.show() to prevent the navigation bar from flashing.
     */
    fun showImmersive(dialog: Dialog) {
        val win = dialog.window
        if (win != null) {
            win.addFlags(
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            )
            hideWindowNavigationBar(win)
        }

        if (dialog is BottomSheetDialog) {
            setupBottomSheetImmersive(dialog)
        }

        dialog.show()

        if (win != null) {
            hideWindowNavigationBar(win)
            win.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
            win.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        }

        if (dialog is BottomSheetDialog) {
            setupBottomSheetImmersive(dialog)
        }
    }
}
