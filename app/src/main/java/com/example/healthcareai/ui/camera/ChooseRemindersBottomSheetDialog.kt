package com.example.healthcareai.ui.camera

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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
        buildRecommendations()
        setupRecyclerView()
        setupActions()
        updateSaveButtonState()
    }

    private fun buildRecommendations() {
        remindersList.clear()

        // 1. Vital sign / Infection monitor: "Check temperature"
        remindersList.add(
            RecommendedReminder(
                name = "Check temperature",
                category = "Temperature",
                repeat = "Daily",
                scheduleDisplay = "Daily · 8:00 AM, 8:00 PM",
                timeList = listOf("08:00 AM", "08:00 PM"),
                notes = "Monitor temperature for early fever or infection warning.",
                iconResId = R.drawable.ic_temperature,
                isChecked = false
            )
        )

        // 2. AI Tailored Care Suggestion(s) based on scan results
        val suggestions = analysisResult?.careSuggestions.orEmpty()
        if (suggestions.isNotEmpty()) {
            val firstSuggestion = suggestions.first()
            val title = firstSuggestion.title?.takeIf { it.isNotBlank() } ?: "Skin Care & Observation"
            val desc = firstSuggestion.description.orEmpty()
            val lower = title.lowercase()
            val icon = when {
                lower.contains("water") || lower.contains("hydrat") -> R.drawable.ic_drink_water
                lower.contains("temp") -> R.drawable.ic_temperature
                else -> R.drawable.ic_medications
            }

            remindersList.add(
                RecommendedReminder(
                    name = title,
                    category = "Medication",
                    repeat = "Daily",
                    scheduleDisplay = "Daily · 8:00 AM, 8:00 PM",
                    timeList = listOf("08:00 AM", "08:00 PM"),
                    notes = desc,
                    iconResId = icon,
                    isChecked = false
                )
            )
        } else {
            // Default tailored treatment observation reminder
            val catLabel = category?.lowercase() ?: "skin"
            remindersList.add(
                RecommendedReminder(
                    name = "Observe $catLabel area",
                    category = "Observation",
                    repeat = "Daily",
                    scheduleDisplay = "Daily · 8:00 AM, 8:00 PM",
                    timeList = listOf("08:00 AM", "08:00 PM"),
                    notes = "Check for any changes in redness, size, or sensation.",
                    iconResId = R.drawable.ic_medications,
                    isChecked = false
                )
            )
        }

        // 3. Hydration & Cellular Recovery: "Drink water"
        remindersList.add(
            RecommendedReminder(
                name = "Drink water",
                category = "Water",
                repeat = "Daily",
                scheduleDisplay = "Choose times",
                timeList = listOf("08:00 AM", "12:00 PM", "04:00 PM", "08:00 PM"),
                notes = "Hydration accelerates tissue repair and skin barrier restoration.",
                iconResId = R.drawable.ic_drink_water,
                isChecked = false
            )
        )
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

    private fun setupActions() {
        // Save Button
        binding.btnSaveReminders.setOnClickListener {
            val selectedItems = remindersList.filter { it.isChecked }
            if (selectedItems.isEmpty()) return@setOnClickListener

            val repo = ReminderRepository.getInstance(requireContext())
            for (item in selectedItems) {
                val reminder = ReminderItem(
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
            }

            val count = selectedItems.size
            val msg = if (count == 1) {
                getString(R.string.msg_reminder_saved)
            } else {
                "$count reminders saved successfully"
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
            binding.btnSaveReminders.text = "Save"
            binding.btnSaveReminders.isEnabled = false
            binding.btnSaveReminders.alpha = 0.5f
        } else if (checkedCount == 1) {
            binding.btnSaveReminders.text = "Save 1 reminder"
            binding.btnSaveReminders.isEnabled = true
            binding.btnSaveReminders.alpha = 1.0f
        } else {
            binding.btnSaveReminders.text = "Save $checkedCount reminders"
            binding.btnSaveReminders.isEnabled = true
            binding.btnSaveReminders.alpha = 1.0f
        }
    }

    private fun handleEditReminder(item: RecommendedReminder) {
        val repo = ReminderRepository.getInstance(requireContext())
        val reminder = ReminderItem(
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

        NewReminderActivity.start(
            context = requireContext(),
            reminderId = reminder.id,
            profileId = profileId
        )
        dismiss()
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
            holder.itemBinding.tvReminderName.text = item.name
            holder.itemBinding.tvReminderSchedule.text = item.scheduleDisplay
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

