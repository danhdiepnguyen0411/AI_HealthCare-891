package com.example.healthcareai.ui.dialog

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.ViewGroup
import android.view.Window
import com.example.healthcareai.data.local.PreferenceHelper
import com.example.healthcareai.databinding.DialogCameraPermissionBinding
import com.example.healthcareai.util.DialogImmersiveHelper
import com.example.healthcareai.util.PermissionReturnHelper

object CustomCameraPermissionDialog {

    fun show(
        activity: Activity,
        onGranted: (() -> Unit)? = null,
        onDenied: (() -> Unit)? = null
    ): Dialog? {
        if (activity.isFinishing || activity.isDestroyed) return null

        val dialog = Dialog(activity)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val binding = DialogCameraPermissionBinding.inflate(activity.layoutInflater)
        dialog.setContentView(binding.root)

        val displayMetrics = activity.resources.displayMetrics
        val targetWidth = (displayMetrics.widthPixels * 0.85f).toInt().coerceAtMost(
            (340 * displayMetrics.density).toInt()
        )

        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(targetWidth, ViewGroup.LayoutParams.WRAP_CONTENT)
            setDimAmount(0.55f)
        }

        dialog.setCancelable(true)
        dialog.setCanceledOnTouchOutside(false)

        val preferenceHelper = PreferenceHelper.getInstance(activity)

        binding.btnGoToSettings.setOnClickListener {
            // 1. Open App Details Settings without FLAG_ACTIVITY_NEW_TASK so app stays stacked directly below
            PermissionReturnHelper.openAppSettings(activity)

            // 2. Start background polling (200ms) with instant auto-return (bringAppToFront)
            PermissionReturnHelper.startPollingForPermission(
                context = activity,
                checkGranted = {
                    PermissionReturnHelper.isPermissionGranted(activity, Manifest.permission.CAMERA)
                },
                onGranted = {
                    preferenceHelper.cameraDenialCount = 0
                    if (dialog.isShowing && !activity.isFinishing && !activity.isDestroyed) {
                        try {
                            dialog.dismiss()
                        } catch (_: Exception) {}
                    }
                    onGranted?.invoke()
                }
            )
        }

        binding.btnDontAllow.setOnClickListener {
            PermissionReturnHelper.stopPolling()
            dialog.dismiss()
            onDenied?.invoke()
        }

        dialog.setOnDismissListener {
            if (!PermissionReturnHelper.isPermissionGranted(activity, Manifest.permission.CAMERA)) {
                PermissionReturnHelper.stopPolling()
            }
        }

        DialogImmersiveHelper.showImmersive(dialog)
        // Ensure compact width is strictly applied and never stretches to screen edges
        dialog.window?.setLayout(targetWidth, ViewGroup.LayoutParams.WRAP_CONTENT)
        return dialog
    }
}
