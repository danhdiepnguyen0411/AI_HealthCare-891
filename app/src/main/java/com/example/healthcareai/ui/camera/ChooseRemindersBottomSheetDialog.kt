package com.example.healthcareai.ui.camera

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.healthcareai.R
import com.example.healthcareai.data.model.LesionAnalysisResult
import com.example.healthcareai.data.model.ReminderItem
import com.example.healthcareai.data.repository.ReminderRepository
import com.example.healthcareai.databinding.BottomSheetChooseRemindersBinding
import com.example.healthcareai.databinding.ItemChooseReminderBinding
import com.example.healthcareai.ui.reminder.NewReminderActivity
import com.example.healthcareai.util.DialogImmersiveHelper
import com.example.healthcareai.util.ReminderLocalizationHelper
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.io.Serializable
import java.util.UUID

class ChooseRemindersBottomSheetDialog : BottomSheetDialogFragment() {

    private var _binding: BottomSheetChooseRemindersBinding? = null
    private val binding get() = _binding!!

    private var profileId: String? = null
    private var analysisResult: LesionAnalysisResult? = null
    private var category: String? = null
    private var onRemindersSavedListener: ((Int) -> Unit)? = null

    private val remindersList = mutableListOf<RecommendedReminder>()
    private lateinit var adapter: RecommendedRemindersAdapter

    private val newReminderLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        refreshRemindersList()
    }

    data class RecommendedReminder(
        val id: String = UUID.randomUUID().toString(),
        var name: String,
        var category: String,
        var repeat: String = "Daily",
        var scheduleDisplay: String = "Daily · 8:00 AM, 8:00 PM",
        var timeList: List<String> = listOf("08:00 AM", "08:00 PM"),
        var notes: String = "",
        var iconResId: Int = R.drawable.ic_temperature,
        var isChecked: Boolean = false
    ) : Serializable

    override fun getTheme(): Int = R.style.TransparentBottomSheetDialogTheme

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        profileId = arguments?.getString(ARG_PROFILE_ID)
        category = arguments?.getString(ARG_CATEGORY)
        @Suppress("DEPRECATION")
        analysisResult = arguments?.getSerializable(ARG_ANALYSIS_RESULT) as? LesionAnalysisResult
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetChooseRemindersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        refreshRemindersList()
        setupActions()
    }

    private fun refreshRemindersList() {
        if (!isAdded || _binding == null) return

        val repo = ReminderRepository.getInstance(requireContext())
        val allSaved = repo.getAllReminders()
        val profileReminders = if (!profileId.isNullOrEmpty()) {
            repo.getRemindersForProfile(profileId)
        } else {
            allSaved
        }

        val existingMap = remindersList.associateBy { it.id }
        val newList = mutableListOf<RecommendedReminder>()

        // Load real reminders for this profile, eliminating any hardcoded items
        for (item in profileReminders) {
            val sortedTimes = com.example.healthcareai.util.ReminderScheduler.sortTimeList(item.timeList)
            val rep = ReminderLocalizationHelper.getLocalizedRepeat(requireContext(), item.repeat)
            val schedule = if (sortedTimes.isNotEmpty()) {
                "$rep · ${sortedTimes.joinToString(", ")}"
            } else {
                rep
            }
            val existingItem = existingMap[item.id]
            // Newly added reminders default to checked = true.
            // Existing reminders preserve their user-selected check status.
            val isChecked = existingItem?.isChecked ?: true

            newList.add(
                RecommendedReminder(
                    id = item.id,
                    name = item.name,
                    category = item.category,
                    repeat = item.repeat,
                    scheduleDisplay = schedule,
                    timeList = sortedTimes,
                    notes = item.notes,
                    iconResId = item.getIconResId(),
                    isChecked = isChecked
                )
            )
        }

        remindersList.clear()
        remindersList.addAll(newList)
        if (::adapter.isInitialized) {
            adapter.notifyDataSetChanged()
        }
        updateRemindersView()
        updateSaveButtonState()
    }

    private fun updateRemindersView() {
        if (remindersList.isEmpty()) {
            binding.rvReminders.visibility = View.GONE
            binding.layoutEmptyReminders.visibility = View.VISIBLE
            binding.btnSaveReminders.visibility = View.GONE
            binding.btnHeaderAddReminder.visibility = View.GONE
        } else {
            binding.rvReminders.visibility = View.VISIBLE
            binding.layoutEmptyReminders.visibility = View.GONE
            binding.btnSaveReminders.visibility = View.VISIBLE
            binding.btnHeaderAddReminder.visibility = View.VISIBLE
        }
    }

    private fun setupRecyclerView() {
        adapter = RecommendedRemindersAdapter(
            items = remindersList,
            onItemToggled = {
                updateSaveButtonState()
            },
            onEditClicked = { reminder ->
                handleEditReminder(reminder)
            }
        )
        binding.rvReminders.layoutManager = LinearLayoutManager(requireContext())
        binding.rvReminders.adapter = adapter
    }

    private fun openNewReminder() {
        val intent = Intent(requireContext(), NewReminderActivity::class.java).apply {
            profileId?.let { putExtra(NewReminderActivity.EXTRA_PROFILE_ID, it) }
        }
        newReminderLauncher.launch(intent)
    }

    private fun setupActions() {
        binding.btnHeaderAddReminder.setOnClickListener {
            openNewReminder()
        }

        binding.btnAddReminder.setOnClickListener {
            openNewReminder()
        }

        // Save Button
        binding.btnSaveReminders.setOnClickListener {
            val repo = ReminderRepository.getInstance(requireContext())
            val selectedItems = remindersList.filter { it.isChecked }
            val unselectedItems = remindersList.filter { !it.isChecked }

            // Delete unselected items if they were previously saved in repository
            for (unselected in unselectedItems) {
                if (repo.getReminder(unselected.id) != null) {
                    repo.deleteReminder(unselected.id)
                }
            }

            for (item in selectedItems) {
                val existing = repo.getReminder(item.id)
                val reminder = ReminderItem(
                    id = item.id,
                    profileId = profileId ?: existing?.profileId,
                    name = item.name,
                    category = item.category,
                    repeat = item.repeat,
                    timeList = item.timeList,
                    notes = item.notes,
                    isDone = existing?.isDone ?: false,
                    createdAt = existing?.createdAt ?: System.currentTimeMillis()
                )
                repo.saveReminder(reminder)
            }

            val count = selectedItems.size
            val msg = if (count == 1) {
                getString(R.string.msg_reminder_saved)
            } else {
                getString(R.string.btn_save_reminders_count, count)
            }
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
            onRemindersSavedListener?.invoke(count)
            dismiss()
        }

        // Cancel Button
        binding.btnCancel.setOnClickListener {
            dismiss()
        }
    }

    private fun updateSaveButtonState() {
        val checkedCount = remindersList.count { it.isChecked }
        if (checkedCount == 0) {
            binding.btnSaveReminders.text = getString(R.string.btn_save)
            binding.btnSaveReminders.isEnabled = false
            binding.btnSaveReminders.alpha = 0.5f
        } else if (checkedCount == 1) {
            binding.btnSaveReminders.text = getString(R.string.btn_save_reminder_single)
            binding.btnSaveReminders.isEnabled = true
            binding.btnSaveReminders.alpha = 1.0f
        } else {
            binding.btnSaveReminders.text = getString(R.string.btn_save_reminders_count, checkedCount)
            binding.btnSaveReminders.isEnabled = true
            binding.btnSaveReminders.alpha = 1.0f
        }
    }

    private fun handleEditReminder(item: RecommendedReminder) {
        val repo = ReminderRepository.getInstance(requireContext())
        val existing = repo.getReminder(item.id)
        val reminder = existing?.copy(
            name = item.name,
            category = item.category,
            repeat = item.repeat,
            timeList = item.timeList,
            notes = item.notes
        ) ?: ReminderItem(
            id = item.id,
            profileId = profileId,
            name = item.name,
            category = item.category,
            repeat = item.repeat,
            timeList = item.timeList,
            notes = item.notes,
            isDone = false,
            createdAt = System.currentTimeMillis()
        )
        repo.saveReminder(reminder)

        val intent = Intent(requireContext(), NewReminderActivity::class.java).apply {
            putExtra(NewReminderActivity.EXTRA_REMINDER_ID, reminder.id)
            (profileId ?: existing?.profileId)?.let { putExtra(NewReminderActivity.EXTRA_PROFILE_ID, it) }
        }
        newReminderLauncher.launch(intent)
    }

    override fun onResume() {
        super.onResume()
        dialog?.let {
            if (it is BottomSheetDialog) {
                DialogImmersiveHelper.setupBottomSheetImmersive(it)
                val bottomSheet = it.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                bottomSheet?.let { sheet ->
                    BottomSheetBehavior.from(sheet).state = BottomSheetBehavior.STATE_EXPANDED
                }
            }
        }
        refreshRemindersList()
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

    // =========================================================================
    // Adapter
    // =========================================================================
    private inner class RecommendedRemindersAdapter(
        private val items: List<RecommendedReminder>,
        private val onItemToggled: () -> Unit,
        private val onEditClicked: (RecommendedReminder) -> Unit
    ) : RecyclerView.Adapter<RecommendedRemindersAdapter.ReminderViewHolder>() {

        inner class ReminderViewHolder(val itemBinding: ItemChooseReminderBinding) :
            RecyclerView.ViewHolder(itemBinding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReminderViewHolder {
            val binding = ItemChooseReminderBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return ReminderViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ReminderViewHolder, position: Int) {
            val item = items[position]

            // Content
            val context = holder.itemView.context
            holder.itemBinding.tvReminderName.text = com.example.healthcareai.util.ReminderLocalizationHelper.getLocalizedName(context, item.name)
            holder.itemBinding.tvReminderSchedule.text = com.example.healthcareai.util.ReminderLocalizationHelper.getLocalizedSchedule(context, item.scheduleDisplay)
            holder.itemBinding.ivReminderIcon.setImageResource(item.iconResId)

            // Checkbox Visuals
            val checkboxRes = if (item.isChecked) {
                R.drawable.bg_checkbox_checked
            } else {
                R.drawable.bg_checkbox_unchecked
            }
            holder.itemBinding.ivCheckbox.setImageResource(checkboxRes)

            // Card / Checkbox click toggles selection
            val toggleAction = View.OnClickListener {
                item.isChecked = !item.isChecked
                notifyItemChanged(holder.bindingAdapterPosition)
                onItemToggled()
            }
            holder.itemBinding.cardReminderItem.setOnClickListener(toggleAction)
            holder.itemBinding.ivCheckbox.setOnClickListener(toggleAction)

            // Edit Action
            holder.itemBinding.tvEdit.setOnClickListener {
                onEditClicked(item)
            }
        }

        override fun getItemCount(): Int = items.size
    }

    companion object {
        const val TAG = "ChooseRemindersBottomSheetDialog"
        private const val ARG_PROFILE_ID = "arg_profile_id"
        private const val ARG_CATEGORY = "arg_category"
        private const val ARG_ANALYSIS_RESULT = "arg_analysis_result"

        fun show(
            fragmentManager: FragmentManager,
            profileId: String?,
            analysisResult: LesionAnalysisResult?,
            category: String?,
            onSaved: ((Int) -> Unit)? = null
        ): ChooseRemindersBottomSheetDialog {
            val existing = fragmentManager.findFragmentByTag(TAG) as? ChooseRemindersBottomSheetDialog
            if (existing != null) {
                existing.dismissAllowingStateLoss()
            }
            val dialog = ChooseRemindersBottomSheetDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_PROFILE_ID, profileId)
                    putString(ARG_CATEGORY, category)
                    putSerializable(ARG_ANALYSIS_RESULT, analysisResult)
                }
                this.onRemindersSavedListener = onSaved
            }
            dialog.show(fragmentManager, TAG)
            return dialog
        }
    }
}

