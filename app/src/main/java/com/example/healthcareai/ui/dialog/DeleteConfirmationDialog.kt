package com.example.healthcareai.ui.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.ViewGroup
import android.view.Window
import com.example.healthcareai.databinding.DialogDeleteConfirmBinding
import com.example.healthcareai.util.DialogImmersiveHelper

class DeleteConfirmationDialog(
    context: Context,
    private val title: String? = null,
    private val message: String? = null,
    private val onDeleteConfirmed: () -> Unit
) : Dialog(context) {

    private lateinit var binding: DialogDeleteConfirmBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        binding = DialogDeleteConfirmBinding.inflate(layoutInflater)
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
        if (!title.isNullOrEmpty()) {
            binding.tvDeleteTitle.text = title
        }
        if (!message.isNullOrEmpty()) {
            binding.tvDeleteMessage.text = message
        }

        binding.btnDeleteConfirm.setOnClickListener {
            dismiss()
            onDeleteConfirmed()
        }

        binding.btnCancelConfirm.setOnClickListener {
            dismiss()
        }
    }

    companion object {
        fun show(
            context: Context,
            title: String? = null,
            message: String? = null,
            onDeleteConfirmed: () -> Unit
        ): DeleteConfirmationDialog {
            val dialog = DeleteConfirmationDialog(context, title, message, onDeleteConfirmed)
            DialogImmersiveHelper.showImmersive(dialog)
            return dialog
        }
    }
}
