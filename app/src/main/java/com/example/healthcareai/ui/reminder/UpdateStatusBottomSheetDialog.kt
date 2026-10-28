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
import com.example.healthcareai.databinding.BottomSheetUpdateStatusBinding
import com.example.healthcareai.util.DialogImmersiveHelper
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class UpdateStatusBottomSheetDialog : BottomSheetDialogFragment() {

    private var _binding: BottomSheetUpdateStatusBinding? = null
    private val binding get() = _binding!!

    private var currentSelectedStatus: String = STATUS_ONGOING
    private var onStatusSavedListener: ((String) -> Unit)? = null

    override fun getTheme(): Int = R.style.TransparentBottomSheetDialogTheme

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetUpdateStatusBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.getString(ARG_SELECTED_STATUS)?.let {
            if (it.isNotEmpty()) currentSelectedStatus = it
        }

        setupStatusSelection()

        binding.btnClose.setOnClickListener {
            dismiss()
        }

        binding.btnSaveStatus.setOnClickListener {
            onStatusSavedListener?.invoke(currentSelectedStatus)
            dismiss()
        }
    }

    private data class StatusBundle(
        val key: String,
        val card: FrameLayout,
        val tvTitle: TextView,
        val tvSub: TextView,
        val ivCheck: ImageView
    )

    private fun setupStatusSelection() {
        val statusList = listOf(
            StatusBundle(STATUS_ONGOING, binding.cardStatusOngoing, binding.tvTitleOngoing, binding.tvSubOngoing, binding.ivCheckOngoing),
            StatusBundle(STATUS_RESOLVED, binding.cardStatusResolved, binding.tvTitleResolved, binding.tvSubResolved, binding.ivCheckResolved),
            StatusBundle(STATUS_UNSURE, binding.cardStatusUnsure, binding.tvTitleUnsure, binding.tvSubUnsure, binding.ivCheckUnsure)
        )

        fun updateUI(selectedKey: String) {
            currentSelectedStatus = selectedKey
            val context = context ?: return

            for (bundle in statusList) {
                val isSelected = bundle.key.equals(selectedKey, ignoreCase = true)
                if (isSelected) {
                    bundle.card.setBackgroundResource(R.drawable.bg_category_card_selected)
                    bundle.tvTitle.setTextColor(Color.WHITE)
                    bundle.tvSub.setTextColor(Color.parseColor("#E0EAFF"))
                    bundle.ivCheck.setImageResource(R.drawable.bg_checkbox_selected_white)
                } else {
                    bundle.card.setBackgroundResource(R.drawable.bg_category_card_unselected)
                    bundle.tvTitle.setTextColor(ContextCompat.getColor(context, R.color.color_text))
                    bundle.tvSub.setTextColor(ContextCompat.getColor(context, R.color.color_subtext))
                    bundle.ivCheck.setImageResource(R.drawable.bg_checkbox_unchecked)
                }
            }
        }

        updateUI(currentSelectedStatus)

        for (bundle in statusList) {
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
        const val TAG = "UpdateStatusBottomSheetDialog"
        private const val ARG_SELECTED_STATUS = "arg_selected_status"

        const val STATUS_ONGOING = "Ongoing"
        const val STATUS_RESOLVED = "Resolved"
        const val STATUS_UNSURE = "Unsure"

        fun show(
            fragmentManager: FragmentManager,
            currentStatus: String = STATUS_ONGOING,
            onStatusSaved: (String) -> Unit
        ): UpdateStatusBottomSheetDialog {
            val existing = fragmentManager.findFragmentByTag(TAG) as? UpdateStatusBottomSheetDialog
            if (existing != null) {
                existing.dismissAllowingStateLoss()
            }
            val dialog = UpdateStatusBottomSheetDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_SELECTED_STATUS, currentStatus)
                }
                this.onStatusSavedListener = onStatusSaved
            }
            dialog.show(fragmentManager, TAG)
            return dialog
        }
    }
}
