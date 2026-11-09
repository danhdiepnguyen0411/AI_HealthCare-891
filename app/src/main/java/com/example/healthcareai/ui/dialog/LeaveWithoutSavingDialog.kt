package com.example.healthcareai.ui.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.ViewGroup
import android.view.Window
import com.example.healthcareai.databinding.DialogLeaveWithoutSavingBinding
import com.example.healthcareai.util.DialogImmersiveHelper

class LeaveWithoutSavingDialog(
    context: Context,
    private val onStay: () -> Unit = {},
    private val onExitWithoutSaving: () -> Unit
) : Dialog(context) {

    private lateinit var binding: DialogLeaveWithoutSavingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        binding = DialogLeaveWithoutSavingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            val width = (context.resources.displayMetrics.widthPixels * 0.85f).toInt().coerceAtMost(
                (340 * context.resources.displayMetrics.density).toInt()
            )
            setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
            setDimAmount(0.55f)
        }

        setupViews()
    }

    private fun setupViews() {
        binding.btnStayOnPage.setOnClickListener {
            dismiss()
            onStay()
        }

        binding.btnExitWithoutSaving.setOnClickListener {
            dismiss()
            onExitWithoutSaving()
        }
    }

    companion object {
        fun show(
            context: Context,
            onStay: () -> Unit = {},
            onExitWithoutSaving: () -> Unit
        ): LeaveWithoutSavingDialog {
            val dialog = LeaveWithoutSavingDialog(context, onStay, onExitWithoutSaving)
            DialogImmersiveHelper.showImmersive(dialog)
            return dialog
        }
    }
}

