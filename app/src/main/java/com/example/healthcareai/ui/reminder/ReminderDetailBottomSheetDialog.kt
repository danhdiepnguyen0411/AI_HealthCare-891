package com.example.healthcareai.ui.reminder

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentManager
import com.example.healthcareai.R
import com.example.healthcareai.data.repository.ReminderRepository
import com.example.healthcareai.databinding.BottomSheetReminderDetailBinding
import com.example.healthcareai.util.DialogImmersiveHelper
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class ReminderDetailBottomSheetDialog : BottomSheetDialogFragment() {

    private var _binding: BottomSheetReminderDetailBinding? = null
    private val binding get() = _binding!!

    private var reminderId: String? = null
    var onReminderUpdatedListener: (() -> Unit)? = null
    var onReminderDeletedListener: (() -> Unit)? = null

    override fun getTheme(): Int = R.style.TransparentBottomSheetDialogTheme

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        reminderId = arguments?.getString(ARG_REMINDER_ID)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetReminderDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindReminderData()
        setupActions()
    }

    private fun bindReminderData() {
        val id = reminderId ?: run {
            dismiss()
            return
        }
        val repository = ReminderRepository.getInstance(requireContext())
        val reminder = repository.getReminder(id) ?: run {
            dismiss()
            return
        }

        binding.ivReminderIcon.setImageResource(reminder.getIconResId())
        binding.tvReminderTitle.text = reminder.name
        val desc = reminder.getDisplayDescription()
        if (desc.isNotBlank()) {
            binding.tvReminderDesc.visibility = View.VISIBLE
            binding.tvReminderDesc.text = desc
        } else {
            binding.tvReminderDesc.visibility = View.GONE
        }
        val time = reminder.getDisplayTime()
        if (time.isNotBlank()) {
            binding.tvReminderTime.visibility = View.VISIBLE
            binding.tvReminderTime.text = time
        } else {
            binding.tvReminderTime.visibility = View.GONE
        }

        if (reminder.isDone) {
            binding.tvMarkDoneButtonText.setText(R.string.reminder_action_undo)
        } else {
            binding.tvMarkDoneButtonText.setText(R.string.btn_mark_as_done)
        }
    }

    private fun setupActions() {
        val id = reminderId ?: return

        // 1. Edit reminder
        binding.layoutEditReminder.setOnClickListener {
            val intent = Intent(requireContext(), NewReminderActivity::class.java).apply {
                putExtra(NewReminderActivity.EXTRA_REMINDER_ID, id)
            }
            startActivity(intent)
            dismiss()
        }

        // 2. Delete reminder
        binding.layoutDeleteReminder.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.reminder_action_delete)
                .setMessage(R.string.delete_reminder_confirm)
                .setPositiveButton(R.string.btn_delete) { _, _ ->
                    ReminderRepository.getInstance(requireContext()).deleteReminder(id)
                    Toast.makeText(requireContext(), R.string.msg_reminder_deleted, Toast.LENGTH_SHORT).show()
                    onReminderDeletedListener?.invoke()
                    dismiss()
                }
                .setNegativeButton(R.string.btn_cancel, null)
                .show()
        }

        // 3. Mark as done / Undo
        binding.btnMarkDoneSheet.setOnClickListener {
            val repository = ReminderRepository.getInstance(requireContext())
            val reminder = repository.getReminder(id)
            if (reminder != null) {
                val newStatus = !reminder.isDone
                repository.setReminderDone(id, newStatus)
                val msgRes = if (newStatus) R.string.msg_marked_as_done else R.string.msg_marked_as_undone
                Toast.makeText(requireContext(), msgRes, Toast.LENGTH_SHORT).show()
                onReminderUpdatedListener?.invoke()
            }
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
        const val TAG = "ReminderDetailBottomSheetDialog"
        private const val ARG_REMINDER_ID = "arg_reminder_id"

        fun show(
            fragmentManager: FragmentManager,
            reminderId: String,
            onUpdated: (() -> Unit)? = null,
            onDeleted: (() -> Unit)? = null
        ): ReminderDetailBottomSheetDialog {
            val existing = fragmentManager.findFragmentByTag(TAG) as? ReminderDetailBottomSheetDialog
            if (existing != null) {
                existing.dismissAllowingStateLoss()
            }
            val dialog = ReminderDetailBottomSheetDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_REMINDER_ID, reminderId)
                }
                this.onReminderUpdatedListener = onUpdated
                this.onReminderDeletedListener = onDeleted
            }
            dialog.show(fragmentManager, TAG)
            return dialog
        }
    }
}
