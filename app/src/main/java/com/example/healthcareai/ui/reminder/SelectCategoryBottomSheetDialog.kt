package com.example.healthcareai.ui.reminder

import android.app.Dialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
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

    private var currentSelectedCategory: String = CATEGORY_TEMPERATURE
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
        val card: LinearLayout,
        val ivIcon: ImageView,
        val tvTitle: TextView
    )

    private fun setupCategorySelection() {
        val categories = listOf(
            CategoryViewBundle(CATEGORY_TEMPERATURE, binding.cardCategoryTemperature, binding.ivIconTemperature, binding.tvTitleTemperature),
            CategoryViewBundle(CATEGORY_HEART_RATE, binding.cardCategoryHeartRate, binding.ivIconHeartRate, binding.tvTitleHeartRate),
            CategoryViewBundle(CATEGORY_MEDICATION, binding.cardCategoryMedication, binding.ivIconMedication, binding.tvTitleMedication),
            CategoryViewBundle(CATEGORY_DRINK_WATER, binding.cardCategoryDrinkWater, binding.ivIconDrinkWater, binding.tvTitleDrinkWater),
            CategoryViewBundle(CATEGORY_EXERCISE, binding.cardCategoryExercise, binding.ivIconExercise, binding.tvTitleExercise),
            CategoryViewBundle(CATEGORY_SLEEP, binding.cardCategorySleep, binding.ivIconSleep, binding.tvTitleSleep),
            CategoryViewBundle(CATEGORY_APPOINTMENTS, binding.cardCategoryAppointments, binding.ivIconAppointments, binding.tvTitleAppointments),
            CategoryViewBundle(CATEGORY_VACCINATION, binding.cardCategoryVaccination, binding.ivIconVaccination, binding.tvTitleVaccination)
        )

        fun updateUI(selectedKey: String) {
            currentSelectedCategory = selectedKey
            val context = context ?: return

            val whiteColor = ContextCompat.getColor(context, R.color.white)
            val textColor = ContextCompat.getColor(context, R.color.color_text)
            val blueTint = ColorStateList.valueOf(Color.parseColor("#4E75F7"))
            val whiteTint = ColorStateList.valueOf(whiteColor)

            for (bundle in categories) {
                val isSelected = bundle.key.equals(selectedKey, ignoreCase = true)
                if (isSelected) {
                    bundle.card.setBackgroundResource(R.drawable.bg_category_grid_selected)
                    bundle.tvTitle.setTextColor(whiteColor)
                    ImageViewCompat.setImageTintList(bundle.ivIcon, whiteTint)
                } else {
                    bundle.card.setBackgroundResource(R.drawable.bg_category_grid_unselected)
                    bundle.tvTitle.setTextColor(textColor)
                    ImageViewCompat.setImageTintList(bundle.ivIcon, blueTint)
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

        const val CATEGORY_TEMPERATURE = "Temperature"
        const val CATEGORY_HEART_RATE = "Heart rate"
        const val CATEGORY_MEDICATION = "Medication"
        const val CATEGORY_DRINK_WATER = "Drink Water"
        const val CATEGORY_EXERCISE = "Exercise"
        const val CATEGORY_SLEEP = "Sleep"
        const val CATEGORY_APPOINTMENTS = "Appointments"
        const val CATEGORY_VACCINATION = "Vaccination"

        fun show(
            fragmentManager: FragmentManager,
            selectedCategory: String = CATEGORY_MEDICATION,
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
