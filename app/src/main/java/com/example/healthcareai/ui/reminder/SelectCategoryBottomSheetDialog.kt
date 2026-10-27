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
import com.example.healthcareai.databinding.BottomSheetSelectCategoryBinding
import com.example.healthcareai.util.DialogImmersiveHelper
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class SelectCategoryBottomSheetDialog : BottomSheetDialogFragment() {

    private var _binding: BottomSheetSelectCategoryBinding? = null
    private val binding get() = _binding!!

    private var currentSelectedCategory: String = CATEGORY_HEALTH_MONITORING
    private var onCategorySelectedListener: ((String) -> Unit)? = null

    override fun getTheme(): Int = R.style.TransparentBottomSheetDialogTheme

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetSelectCategoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.getString(ARG_SELECTED_CATEGORY)?.let {
            currentSelectedCategory = it
        }

        setupCategorySelection()

        binding.btnContinue.setOnClickListener {
            onCategorySelectedListener?.invoke(currentSelectedCategory)
            dismiss()
        }
    }

    private data class CategoryViewBundle(
        val key: String,
        val card: FrameLayout,
        val tvTitle: TextView,
        val tvDesc: TextView,
        val ivCheck: ImageView
    )

    private fun setupCategorySelection() {
        val categories = listOf(
            CategoryViewBundle(
                CATEGORY_HEALTH_MONITORING,
                binding.cardCategoryHealthMonitoring,
                binding.tvTitleHealthMonitoring,
                binding.tvDescHealthMonitoring,
                binding.ivCheckHealthMonitoring
            ),
            CategoryViewBundle(
                CATEGORY_MEDICATION,
                binding.cardCategoryMedication,
                binding.tvTitleMedication,
                binding.tvDescMedication,
                binding.ivCheckMedication
            ),
            CategoryViewBundle(
                CATEGORY_HEALTHY_HABITS,
                binding.cardCategoryHealthyHabits,
                binding.tvTitleHealthyHabits,
                binding.tvDescHealthyHabits,
                binding.ivCheckHealthyHabits
            ),
            CategoryViewBundle(
                CATEGORY_APPOINTMENTS,
                binding.cardCategoryAppointments,
                binding.tvTitleAppointments,
                binding.tvDescAppointments,
                binding.ivCheckAppointments
            ),
            CategoryViewBundle(
                CATEGORY_VACCINATIONS,
                binding.cardCategoryVaccinations,
                binding.tvTitleVaccinations,
                binding.tvDescVaccinations,
                binding.ivCheckVaccinations
            ),
            CategoryViewBundle(
                CATEGORY_OTHER,
                binding.cardCategoryOther,
                binding.tvTitleOther,
                binding.tvDescOther,
                binding.ivCheckOther
            )
        )

        fun updateUI(selectedKey: String) {
            currentSelectedCategory = selectedKey
            val context = context ?: return

            for (bundle in categories) {
                val isSelected = bundle.key.equals(selectedKey, ignoreCase = true)
                if (isSelected) {
                    bundle.card.setBackgroundResource(R.drawable.bg_category_card_selected)
                    bundle.tvTitle.setTextColor(ContextCompat.getColor(context, R.color.white))
                    bundle.tvDesc.setTextColor(Color.parseColor("#DCE6FF"))
                    bundle.ivCheck.setImageResource(R.drawable.bg_checkbox_selected_white)
                } else {
                    bundle.card.setBackgroundResource(R.drawable.bg_category_card_unselected)
                    bundle.tvTitle.setTextColor(ContextCompat.getColor(context, R.color.color_text))
                    bundle.tvDesc.setTextColor(ContextCompat.getColor(context, R.color.color_subtext))
                    bundle.ivCheck.setImageResource(R.drawable.bg_checkbox_unchecked)
                }
            }
        }

        updateUI(currentSelectedCategory)

        for (bundle in categories) {
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
        const val TAG = "SelectCategoryBottomSheetDialog"
        private const val ARG_SELECTED_CATEGORY = "arg_selected_category"

        const val CATEGORY_HEALTH_MONITORING = "Health monitoring"
        const val CATEGORY_MEDICATION = "Medication"
        const val CATEGORY_HEALTHY_HABITS = "Healthy habits"
        const val CATEGORY_APPOINTMENTS = "Appointments"
        const val CATEGORY_VACCINATIONS = "Vaccinations"
        const val CATEGORY_OTHER = "Other"

        fun show(
            fragmentManager: FragmentManager,
            selectedCategory: String = CATEGORY_HEALTH_MONITORING,
            onSelected: (String) -> Unit
        ): SelectCategoryBottomSheetDialog {
            val existing = fragmentManager.findFragmentByTag(TAG) as? SelectCategoryBottomSheetDialog
            if (existing != null) {
                existing.dismissAllowingStateLoss()
            }
            val dialog = SelectCategoryBottomSheetDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_SELECTED_CATEGORY, selectedCategory)
                }
                this.onCategorySelectedListener = onSelected
            }
            dialog.show(fragmentManager, TAG)
            return dialog
        }
    }
}
