package com.example.healthcareai.ui.dialog

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.ViewGroup
import android.view.Window
import com.example.healthcareai.data.local.PreferenceHelper
import com.example.healthcareai.databinding.DialogMedicalDisclaimerBinding
import com.example.healthcareai.util.DialogImmersiveHelper

object MedicalDisclaimerDialog {

    fun show(
        activity: Activity,
        onDismiss: (() -> Unit)? = null
    ): Dialog? {
        if (activity.isFinishing || activity.isDestroyed) return null

        PreferenceHelper.getInstance(activity).hasSeenMedicalDisclaimer = true

        val dialog = Dialog(activity)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val binding = DialogMedicalDisclaimerBinding.inflate(activity.layoutInflater)
        dialog.setContentView(binding.root)

        val displayMetrics = activity.resources.displayMetrics
        val targetWidth = (displayMetrics.widthPixels * 0.85f).toInt().coerceAtMost(
            (320 * displayMetrics.density).toInt()
        )

        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(targetWidth, ViewGroup.LayoutParams.WRAP_CONTENT)
            setDimAmount(0.55f)
        }

        dialog.setCancelable(true)
        dialog.setCanceledOnTouchOutside(true)

        binding.btnUnderstand.setOnClickListener {
            PreferenceHelper.getInstance(activity).hasSeenMedicalDisclaimer = true
            dialog.dismiss()
            onDismiss?.invoke()
        }

        dialog.setOnDismissListener {
            PreferenceHelper.getInstance(activity).hasSeenMedicalDisclaimer = true
        }

        DialogImmersiveHelper.showImmersive(dialog)
        dialog.window?.setLayout(targetWidth, ViewGroup.LayoutParams.WRAP_CONTENT)
        return dialog
    }
}
