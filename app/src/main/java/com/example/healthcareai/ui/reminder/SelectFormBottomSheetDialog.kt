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
import com.example.healthcareai.databinding.BottomSheetSelectFormBinding
import com.example.healthcareai.util.DialogImmersiveHelper
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class SelectFormBottomSheetDialog : BottomSheetDialogFragment() {

    private var _binding: BottomSheetSelectFormBinding? = null
    private val binding get() = _binding!!

    private var currentSelectedForm: String = FORM_TABLET
    private var onFormSelectedListener: ((String) -> Unit)? = null

    override fun getTheme(): Int = R.style.TransparentBottomSheetDialogTheme

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetSelectFormBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.getString(ARG_SELECTED_FORM)?.let {
            if (it.isNotEmpty()) currentSelectedForm = it
        }

        setupFormSelection()

        binding.btnContinue.setOnClickListener {
            onFormSelectedListener?.invoke(currentSelectedForm)
            dismiss()
        }
    }

    private data class FormViewBundle(
        val key: String,
        val card: FrameLayout,
        val tvTitle: TextView,
        val ivCheck: ImageView
    )

    private fun setupFormSelection() {
        val formOptions = listOf(
            FormViewBundle(FORM_TABLET, binding.cardFormTablet, binding.tvTitleFormTablet, binding.ivCheckFormTablet),
            FormViewBundle(FORM_CAPSULE, binding.cardFormCapsule, binding.tvTitleFormCapsule, binding.ivCheckFormCapsule),
            FormViewBundle(FORM_LIQUID, binding.cardFormLiquid, binding.tvTitleFormLiquid, binding.ivCheckFormLiquid),
            FormViewBundle(FORM_DROPS, binding.cardFormDrops, binding.tvTitleFormDrops, binding.ivCheckFormDrops),
            FormViewBundle(FORM_INJECTION, binding.cardFormInjection, binding.tvTitleFormInjection, binding.ivCheckFormInjection),
            FormViewBundle(FORM_INHALER, binding.cardFormInhaler, binding.tvTitleFormInhaler, binding.ivCheckFormInhaler),
            FormViewBundle(FORM_OTHER, binding.cardFormOther, binding.tvTitleFormOther, binding.ivCheckFormOther)
        )

        fun updateUI(selectedKey: String) {
            currentSelectedForm = selectedKey
            val context = context ?: return

            for (bundle in formOptions) {
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

        updateUI(currentSelectedForm)

        for (bundle in formOptions) {
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
        const val TAG = "SelectFormBottomSheetDialog"
        private const val ARG_SELECTED_FORM = "arg_selected_form"

        const val FORM_TABLET = "Tablet"
        const val FORM_CAPSULE = "Capsule"
        const val FORM_LIQUID = "Liquid"
        const val FORM_DROPS = "Drops"
        const val FORM_INJECTION = "Injection"
        const val FORM_INHALER = "Inhaler"
        const val FORM_OTHER = "Other"

        fun show(
            fragmentManager: FragmentManager,
            selectedForm: String = FORM_TABLET,
            onSelected: (String) -> Unit
        ): SelectFormBottomSheetDialog {
            val existing = fragmentManager.findFragmentByTag(TAG) as? SelectFormBottomSheetDialog
            if (existing != null) {
                existing.dismissAllowingStateLoss()
            }
            val dialog = SelectFormBottomSheetDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_SELECTED_FORM, selectedForm)
                }
                this.onFormSelectedListener = onSelected
            }
            dialog.show(fragmentManager, TAG)
            return dialog
        }
    }
}
