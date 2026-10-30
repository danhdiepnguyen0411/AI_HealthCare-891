package com.example.healthcareai.ui.dialog

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.ViewGroup
import android.view.Window
import com.example.healthcareai.databinding.DialogExitAppBinding
import com.example.healthcareai.util.DialogImmersiveHelper

object CustomExitAppDialog {

    fun show(
        activity: Activity,
        onStay: (() -> Unit)? = null,
        onExit: (() -> Unit)? = null
    ): Dialog? {
        if (activity.isFinishing || activity.isDestroyed) return null

        val dialog = Dialog(activity)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val binding = DialogExitAppBinding.inflate(activity.layoutInflater)
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
        dialog.setCanceledOnTouchOutside(true)

        binding.btnStay.setOnClickListener {
            dialog.dismiss()
            onStay?.invoke()
        }

        binding.btnExit.setOnClickListener {
            dialog.dismiss()
            onExit?.invoke()
        }

        DialogImmersiveHelper.showImmersive(dialog)
        // Ensure compact width is strictly applied and never stretches to screen edges
        dialog.window?.setLayout(targetWidth, ViewGroup.LayoutParams.WRAP_CONTENT)
        return dialog
    }
}
