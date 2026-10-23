package com.example.healthcareai.ui.heartrate

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.healthcareai.databinding.BottomSheetHowToMeasureBinding
import com.example.healthcareai.util.DialogImmersiveHelper
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/**
 * Bottom sheet displaying 3-step measurement guide with phone illustration,
 * matching Figma design system.
 */
class HowToMeasureBottomSheetDialog : BottomSheetDialogFragment() {

    private var _binding: BottomSheetHowToMeasureBinding? = null
    private val binding get() = _binding!!

    override fun getTheme(): Int = com.example.healthcareai.R.style.TransparentBottomSheetDialogTheme

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetHowToMeasureBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnClose.setOnClickListener {
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.let {
            if (it is BottomSheetDialog) {
                DialogImmersiveHelper.setupBottomSheetImmersive(it)
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let { sheet ->
                sheet.background = null
                sheet.setBackgroundColor(Color.TRANSPARENT)
                val behavior = BottomSheetBehavior.from(sheet)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
            }
        }
        DialogImmersiveHelper.setupBottomSheetImmersive(dialog)
        return dialog
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "HowToMeasureBottomSheetDialog"

        fun show(fragmentManager: androidx.fragment.app.FragmentManager) {
            if (fragmentManager.findFragmentByTag(TAG) == null) {
                HowToMeasureBottomSheetDialog().show(fragmentManager, TAG)
            }
        }

        fun showAsDialog(context: Context): BottomSheetDialog {
            val dialog = BottomSheetDialog(context)
            val sheetBinding = BottomSheetHowToMeasureBinding.inflate(android.view.LayoutInflater.from(context))
            dialog.setContentView(sheetBinding.root)

            sheetBinding.btnClose.setOnClickListener {
                dialog.dismiss()
            }

            dialog.setOnShowListener {
                val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                bottomSheet?.let { sheet ->
                    sheet.background = null
                    sheet.setBackgroundColor(Color.TRANSPARENT)
                    val behavior = BottomSheetBehavior.from(sheet)
                    behavior.state = BottomSheetBehavior.STATE_EXPANDED
                    behavior.skipCollapsed = true
                }
            }

            DialogImmersiveHelper.showImmersive(dialog)
            return dialog
        }
    }
}
