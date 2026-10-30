package com.example.healthcareai.ui.dialog

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.ViewGroup
import android.view.Window
import com.example.healthcareai.data.local.PreferenceHelper
import com.example.healthcareai.databinding.DialogNotificationPermissionBinding
import com.example.healthcareai.util.DialogImmersiveHelper
import com.example.healthcareai.util.PermissionReturnHelper

object CustomNotificationDialog {

    fun show(
        activity: Activity,
        onGranted: (() -> Unit)? = null,
        onDenied: (() -> Unit)? = null
    ): Dialog? {
        if (activity.isFinishing || activity.isDestroyed) return null

        val dialog = Dialog(activity)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val binding = DialogNotificationPermissionBinding.inflate(activity.layoutInflater)
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
            PermissionReturnHelper.openNotificationSettings(activity)
            PermissionReturnHelper.startPollingForPermission(
                context = activity,
                checkGranted = {
                    PermissionReturnHelper.isNotificationGranted(activity)
                },
                onGranted = {
                    preferenceHelper.notiDenialCount = 0
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
            if (!PermissionReturnHelper.isNotificationGranted(activity)) {
                PermissionReturnHelper.stopPolling()
            }
        }

        DialogImmersiveHelper.showImmersive(dialog)
        // Ensure compact width is strictly applied and never stretches to screen edges
        dialog.window?.setLayout(targetWidth, ViewGroup.LayoutParams.WRAP_CONTENT)
        return dialog
    }
}
