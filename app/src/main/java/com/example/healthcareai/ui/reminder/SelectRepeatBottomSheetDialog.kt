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
import com.example.healthcareai.databinding.BottomSheetSelectRepeatBinding
import com.example.healthcareai.util.DialogImmersiveHelper
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class SelectRepeatBottomSheetDialog : BottomSheetDialogFragment() {

    private var _binding: BottomSheetSelectRepeatBinding? = null
    private val binding get() = _binding!!

    private var currentSelectedRepeat: String = REPEAT_EVERY_DAY
    private var onRepeatSelectedListener: ((String) -> Unit)? = null

    override fun getTheme(): Int = R.style.TransparentBottomSheetDialogTheme

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetSelectRepeatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.getString(ARG_SELECTED_REPEAT)?.let {
            if (it.isNotEmpty()) currentSelectedRepeat = it
        }

        setupRepeatSelection()

        binding.btnContinue.setOnClickListener {
            onRepeatSelectedListener?.invoke(currentSelectedRepeat)
            dismiss()
        }
    }

    private data class RepeatViewBundle(
        val key: String,
        val card: FrameLayout,
        val tvTitle: TextView,
        val ivCheck: ImageView
    )

    private fun setupRepeatSelection() {
        val repeatOptions = listOf(
            RepeatViewBundle(
                REPEAT_EVERY_DAY,
                binding.cardRepeatEveryDay,
                binding.tvTitleRepeatEveryDay,
                binding.ivCheckRepeatEveryDay
            ),
            RepeatViewBundle(
                REPEAT_WEEKDAYS,
                binding.cardRepeatWeekdays,
                binding.tvTitleRepeatWeekdays,
                binding.ivCheckRepeatWeekdays
            ),
            RepeatViewBundle(
                REPEAT_WEEKENDS,
                binding.cardRepeatWeekends,
                binding.tvTitleRepeatWeekends,
                binding.ivCheckRepeatWeekends
            ),
            RepeatViewBundle(
                REPEAT_WEEKLY,
                binding.cardRepeatWeekly,
                binding.tvTitleRepeatWeekly,
                binding.ivCheckRepeatWeekly
            ),
            RepeatViewBundle(
                REPEAT_MONTHLY,
                binding.cardRepeatMonthly,
                binding.tvTitleRepeatMonthly,
                binding.ivCheckRepeatMonthly
            ),
            RepeatViewBundle(
                REPEAT_ONCE,
                binding.cardRepeatOnce,
                binding.tvTitleRepeatOnce,
                binding.ivCheckRepeatOnce
            )
        )

        fun updateUI(selectedKey: String) {
            currentSelectedRepeat = selectedKey
            val context = context ?: return

            for (bundle in repeatOptions) {
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

        updateUI(currentSelectedRepeat)

        for (bundle in repeatOptions) {
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
        const val TAG = "SelectRepeatBottomSheetDialog"
        private const val ARG_SELECTED_REPEAT = "arg_selected_repeat"

        const val REPEAT_EVERY_DAY = "Every day"
        const val REPEAT_WEEKDAYS = "Weekdays"
        const val REPEAT_WEEKENDS = "Weekends"
        const val REPEAT_WEEKLY = "Weekly"
        const val REPEAT_MONTHLY = "Monthly"
        const val REPEAT_ONCE = "Once"

        fun show(
            fragmentManager: FragmentManager,
            selectedRepeat: String = REPEAT_EVERY_DAY,
            onSelected: (String) -> Unit
        ): SelectRepeatBottomSheetDialog {
            val existing = fragmentManager.findFragmentByTag(TAG) as? SelectRepeatBottomSheetDialog
            if (existing != null) {
                existing.dismissAllowingStateLoss()
            }
            val dialog = SelectRepeatBottomSheetDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_SELECTED_REPEAT, selectedRepeat)
                }
                this.onRepeatSelectedListener = onSelected
            }
            dialog.show(fragmentManager, TAG)
            return dialog
        }
    }
}
