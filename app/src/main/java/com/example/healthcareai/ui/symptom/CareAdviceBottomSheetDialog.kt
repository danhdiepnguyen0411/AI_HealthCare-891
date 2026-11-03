package com.example.healthcareai.ui.symptom

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import com.example.healthcareai.R
import com.example.healthcareai.databinding.BottomSheetCareAdviceBinding
import com.example.healthcareai.databinding.ItemRecommendedActionBinding
import com.example.healthcareai.util.DialogImmersiveHelper
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class CareAdviceBottomSheetDialog : BottomSheetDialogFragment() {

    private var _binding: BottomSheetCareAdviceBinding? = null
    private val binding get() = _binding!!

    private var conditionTitle: String = ""
    private var adviceList: ArrayList<String> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        conditionTitle = arguments?.getString(ARG_TITLE) ?: "Care Advice"
        adviceList = arguments?.getStringArrayList(ARG_ADVICE) ?: arrayListOf()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetCareAdviceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.tvConditionBadge.text = conditionTitle

        binding.btnClose.setOnClickListener {
            dismiss()
        }

        binding.btnGotIt.setOnClickListener {
            dismiss()
        }

        populateAdviceItems()
    }

    private fun populateAdviceItems() {
        binding.llCareAdviceItems.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())

        val items = if (adviceList.isNotEmpty()) {
            adviceList
        } else {
            listOf(
                "Keep the affected area clean and dry.",
                "Avoid scratching or rubbing to prevent secondary infection.",
                "Apply cool compresses for 10-15 minutes to reduce swelling.",
                "Seek medical attention if symptoms worsen or fever develops."
            )
        }

        for (advice in items) {
            val itemBinding = ItemRecommendedActionBinding.inflate(inflater, binding.llCareAdviceItems, false)
            itemBinding.tvActionText.text = advice
            binding.llCareAdviceItems.addView(itemBinding.root)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let { sheet ->
                sheet.background = ColorDrawable(Color.TRANSPARENT)
                val behavior = BottomSheetBehavior.from(sheet)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
            }
            dialog.window?.let { window ->
                DialogImmersiveHelper.hideWindowNavigationBar(window)
            }
        }
        return dialog
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "CareAdviceBottomSheet"
        private const val ARG_TITLE = "arg_condition_title"
        private const val ARG_ADVICE = "arg_advice_list"

        fun show(
            fragmentManager: FragmentManager,
            conditionTitle: String,
            adviceList: List<String>
        ) {
            val dialog = CareAdviceBottomSheetDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_TITLE, conditionTitle)
                    putStringArrayList(ARG_ADVICE, ArrayList(adviceList))
                }
            }
            dialog.show(fragmentManager, TAG)
        }
    }
}

