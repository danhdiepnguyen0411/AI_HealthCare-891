package com.example.healthcareai.ui.reminder

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import com.example.healthcareai.R
import com.example.healthcareai.databinding.BottomSheetSelectSeverityBinding
import com.example.healthcareai.util.DialogImmersiveHelper
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class SelectSeverityBottomSheetDialog : BottomSheetDialogFragment() {

    private var _binding: BottomSheetSelectSeverityBinding? = null
    private val binding get() = _binding!!

    private var currentSelectedSeverity: String = SEVERITY_MILD
    private var onSeveritySelectedListener: ((String) -> Unit)? = null

    override fun getTheme(): Int = R.style.TransparentBottomSheetDialogTheme

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetSelectSeverityBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.getString(ARG_SELECTED_SEVERITY)?.let {
            if (it.isNotEmpty()) currentSelectedSeverity = it
        }

        setupSeveritySelection()

        binding.btnContinue.setOnClickListener {
            onSeveritySelectedListener?.invoke(currentSelectedSeverity)
            dismiss()
        }
    }

    private data class SeverityViewBundle(
        val key: String,
        val card: FrameLayout,
        val tvTitle: TextView,
        val ivCheck: ImageView
    )

    private fun setupSeveritySelection() {
        val severityOptions = listOf(
            SeverityViewBundle(SEVERITY_MILD, binding.cardSeverityMild, binding.tvTitleSeverityMild, binding.ivCheckSeverityMild),
            SeverityViewBundle(SEVERITY_MODERATE, binding.cardSeverityModerate, binding.tvTitleSeverityModerate, binding.ivCheckSeverityModerate),
            SeverityViewBundle(SEVERITY_SEVERE, binding.cardSeveritySevere, binding.tvTitleSeveritySevere, binding.ivCheckSeveritySevere),
            SeverityViewBundle(SEVERITY_NOT_SURE, binding.cardSeverityNotSure, binding.tvTitleSeverityNotSure, binding.ivCheckSeverityNotSure)
        )

        fun updateUI(selectedKey: String) {
            currentSelectedSeverity = selectedKey
            val context = context ?: return

            for (bundle in severityOptions) {
                val isSelected = bundle.key.equals(selectedKey, ignoreCase = true)
                if (isSelected) {
                    bundle.card.setBackgroundResource(R.drawable.bg_category_card_selected)
                    bundle.tvTitle.setTextColor(ContextCompat.getColor(context, R.color.white))
                    bundle.ivCheck.setImageResource(R.drawable.bg_checkbox_selected_white)
                } else {
                    bundle.card.setBackgroundResource(R.drawable.bg_category_card_unselected)
                    bundle.tvTitle.setTextColor(ContextCompat.getColor(context, R.color.color_text))
                    bundle.ivCheck.setImageResource(R.drawable.bg_checkbox_unchecked)
                }
            }
        }

        updateUI(currentSelectedSeverity)

        for (bundle in severityOptions) {
            bundle.card.setOnClickListener {
                updateUI(bundle.key)
            }
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
        const val TAG = "SelectSeverityBottomSheetDialog"
        private const val ARG_SELECTED_SEVERITY = "arg_selected_severity"

        const val SEVERITY_MILD = "Mild"
        const val SEVERITY_MODERATE = "Moderate"
        const val SEVERITY_SEVERE = "Severe"
        const val SEVERITY_NOT_SURE = "Not sure"

        fun show(
            fragmentManager: FragmentManager,
            selectedSeverity: String = SEVERITY_MILD,
            onSelected: (String) -> Unit
        ): SelectSeverityBottomSheetDialog {
            val existing = fragmentManager.findFragmentByTag(TAG) as? SelectSeverityBottomSheetDialog
            if (existing != null) {
                existing.dismissAllowingStateLoss()
            }
            val dialog = SelectSeverityBottomSheetDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_SELECTED_SEVERITY, selectedSeverity)
                }
                this.onSeveritySelectedListener = onSelected
            }
            dialog.show(fragmentManager, TAG)
            return dialog
        }
    }
}
