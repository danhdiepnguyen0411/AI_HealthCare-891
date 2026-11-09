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
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import com.example.healthcareai.R
import com.example.healthcareai.databinding.BottomSheetSelectCustomDaysBinding
import com.example.healthcareai.util.DialogImmersiveHelper
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.util.Calendar

class SelectCustomDaysBottomSheetDialog : BottomSheetDialogFragment() {

    private var _binding: BottomSheetSelectCustomDaysBinding? = null
    private val binding get() = _binding!!

    private val selectedDays = mutableSetOf<Int>()
    private var onDaysSelectedListener: ((String) -> Unit)? = null

    override fun getTheme(): Int = R.style.TransparentBottomSheetDialogTheme

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetSelectCustomDaysBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.getString(ARG_CURRENT_CUSTOM_KEY)?.let { key ->
            parseDaysFromKey(key)
        }

        setupDaysSelection()

        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val navInsets = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.navigationBars())
            val density = resources.displayMetrics.density
            val baseBottomPadding = (24 * density).toInt()
            v.setPadding(
                v.paddingLeft,
                v.paddingTop,
                v.paddingRight,
                baseBottomPadding + navInsets.bottom
            )
            insets
        }

        binding.btnSaveCustomDays.setOnClickListener {
            if (selectedDays.isEmpty()) {
                Toast.makeText(requireContext(), R.string.error_select_at_least_one_day, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Ordering: Monday to Sunday
            val standardOrder = listOf(
                Calendar.MONDAY,
                Calendar.TUESDAY,
                Calendar.WEDNESDAY,
                Calendar.THURSDAY,
                Calendar.FRIDAY,
                Calendar.SATURDAY,
                Calendar.SUNDAY
            )
            val sorted = standardOrder.filter { selectedDays.contains(it) }

            val resultKey = when {
                sorted.size == 7 -> SelectRepeatBottomSheetDialog.REPEAT_EVERY_DAY
                sorted.size == 5 && !sorted.contains(Calendar.SATURDAY) && !sorted.contains(Calendar.SUNDAY) -> {
                    SelectRepeatBottomSheetDialog.REPEAT_WEEKDAYS
                }
                else -> "Custom:" + sorted.joinToString(",")
            }

            onDaysSelectedListener?.invoke(resultKey)
            dismiss()
        }
    }

    private fun parseDaysFromKey(key: String) {
        selectedDays.clear()
        val lower = key.trim().lowercase()
        when {
            lower.contains("every day") || lower.contains("hằng ngày") || lower.contains("daily") -> {
                selectedDays.addAll(listOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY))
            }
            lower.contains("weekday") || lower.contains("thứ 2 đến thứ 6") -> {
                selectedDays.addAll(listOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY))
            }
            lower.startsWith("custom:") -> {
                val csv = key.substringAfter("custom:").trim()
                val parsed = csv.split(",").mapNotNull { it.trim().toIntOrNull() }
                selectedDays.addAll(parsed)
            }
            else -> {
                // Default to Weekdays if empty
                selectedDays.addAll(listOf(Calendar.MONDAY, Calendar.WEDNESDAY, Calendar.FRIDAY))
            }
        }
    }

    private data class DayViewBundle(
        val dayOfWeek: Int,
        val card: FrameLayout,
        val tvTitle: TextView,
        val ivCheck: ImageView
    )

    private fun setupDaysSelection() {
        val bundles = listOf(
            DayViewBundle(Calendar.MONDAY, binding.cardDayMonday, binding.tvDayMonday, binding.ivCheckMonday),
            DayViewBundle(Calendar.TUESDAY, binding.cardDayTuesday, binding.tvDayTuesday, binding.ivCheckTuesday),
            DayViewBundle(Calendar.WEDNESDAY, binding.cardDayWednesday, binding.tvDayWednesday, binding.ivCheckWednesday),
            DayViewBundle(Calendar.THURSDAY, binding.cardDayThursday, binding.tvDayThursday, binding.ivCheckThursday),
            DayViewBundle(Calendar.FRIDAY, binding.cardDayFriday, binding.tvDayFriday, binding.ivCheckFriday),
            DayViewBundle(Calendar.SATURDAY, binding.cardDaySaturday, binding.tvDaySaturday, binding.ivCheckSaturday),
            DayViewBundle(Calendar.SUNDAY, binding.cardDaySunday, binding.tvDaySunday, binding.ivCheckSunday)
        )

        fun updateItemUI(bundle: DayViewBundle) {
            val context = context ?: return
            val isSelected = selectedDays.contains(bundle.dayOfWeek)
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

        for (bundle in bundles) {
            updateItemUI(bundle)
            bundle.card.setOnClickListener {
                if (selectedDays.contains(bundle.dayOfWeek)) {
                    selectedDays.remove(bundle.dayOfWeek)
                } else {
                    selectedDays.add(bundle.dayOfWeek)
                }
                updateItemUI(bundle)
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

    override fun onResume() {
        super.onResume()
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
        const val TAG = "SelectCustomDaysBottomSheetDialog"
        private const val ARG_CURRENT_CUSTOM_KEY = "arg_current_custom_key"

        fun show(
            fragmentManager: FragmentManager,
            currentKey: String = "",
            onSelected: (String) -> Unit
        ): SelectCustomDaysBottomSheetDialog {
            val existing = fragmentManager.findFragmentByTag(TAG) as? SelectCustomDaysBottomSheetDialog
            if (existing != null) {
                existing.dismissAllowingStateLoss()
            }
            val dialog = SelectCustomDaysBottomSheetDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_CURRENT_CUSTOM_KEY, currentKey)
                }
                this.onDaysSelectedListener = onSelected
            }
            dialog.show(fragmentManager, TAG)
            return dialog
        }
    }
}

