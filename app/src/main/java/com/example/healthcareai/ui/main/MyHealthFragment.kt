package com.example.healthcareai.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import coil.load
import coil.transform.CircleCropTransformation
import com.example.healthcareai.R
import com.example.healthcareai.data.repository.HealthRecordRepository
import com.example.healthcareai.data.repository.ProfileRepository
import com.example.healthcareai.data.repository.ScanHistoryRepository
import com.example.healthcareai.data.model.ReminderItem
import com.example.healthcareai.data.repository.ReminderRepository
import com.example.healthcareai.databinding.FragmentMyHealthBinding
import com.example.healthcareai.ui.base.applyHeaderTopMargin
import com.example.healthcareai.databinding.ItemReminderCardBinding
import com.example.healthcareai.databinding.ItemScanHistoryCardBinding
import com.example.healthcareai.ui.camera.AiScanHistoryActivity
import com.example.healthcareai.ui.camera.AnalysisResultsActivity
import com.example.healthcareai.ui.camera.CameraActivity
import com.example.healthcareai.ui.profile.AddProfileActivity
import com.example.healthcareai.ui.profile.ProfileDetailActivity
import com.example.healthcareai.ui.reminder.AddAllergyActivity
import com.example.healthcareai.ui.reminder.AddConditionActivity
import com.example.healthcareai.ui.reminder.AddMedicationActivity
import com.example.healthcareai.ui.reminder.HealthRecordListActivity
import com.example.healthcareai.ui.reminder.NewReminderActivity
import com.example.healthcareai.ui.reminder.ReminderActivity
import com.example.healthcareai.ui.reminder.ReminderDetailBottomSheetDialog
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MyHealthFragment : Fragment() {

    private var _binding: FragmentMyHealthBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyHealthBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupWindowInsets()
        setupListeners()
        loadPrimaryProfile()
        syncReminders()
        syncHealthRecordCounts()
        syncScanHistory()
    }

    override fun onResume() {
        super.onResume()
        loadPrimaryProfile()
        syncReminders()
        syncHealthRecordCounts()
        syncScanHistory()
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.rootMyHealth) { _, insets ->
            binding.topBar.applyHeaderTopMargin(insets)
            insets
        }
    }

    private fun loadPrimaryProfile() {
        val context = context ?: return
        val profileRepository = ProfileRepository.getInstance(context)
        val profile = profileRepository.getPrimaryProfile()

        // Avatar
        if (!profile.avatarUri.isNullOrEmpty()) {
            binding.ivProfileAvatar.load(profile.avatarUri) {
                crossfade(true)
                transformations(CircleCropTransformation())
                placeholder(R.drawable.profile_person)
                error(R.drawable.profile_person)
            }
        } else {
            binding.ivProfileAvatar.setImageResource(R.drawable.profile_person)
        }

        // Name & Gender
        binding.tvProfileName.text = if (profile.name.isNotBlank()) profile.name else "--"
        if (!profile.relationship.isNullOrBlank() && !profile.relationship.equals("you", ignoreCase = true)) {
            val locRel = com.example.healthcareai.util.ProfileLocalizationHelper.getLocalizedRelationship(requireContext(), profile.relationship)
            if (locRel.isNotBlank()) {
                binding.tvProfileRelationship.visibility = View.VISIBLE
                binding.tvProfileRelationship.text = "($locRel)"
            } else {
                binding.tvProfileRelationship.visibility = View.GONE
            }
        } else {
            binding.tvProfileRelationship.visibility = View.GONE
        }
        binding.tvProfileGender.text = com.example.healthcareai.util.ProfileLocalizationHelper.getLocalizedGender(requireContext(), profile.gender)

        // Age calculation from DOB
        val ageStr = calculateAge(profile.dateOfBirth)
        binding.tvAge.text = ageStr

        // Height & Weight
        binding.tvHeight.text = if (profile.heightCm > 0) "${profile.heightCm} cm" else "-- cm"
        binding.tvWeight.text = if (profile.weightKg > 0) "${profile.weightKg} kg" else "-- kg"
    }

    private fun syncHealthRecordCounts() {
        val context = context ?: return
        val primaryId = ProfileRepository.getInstance(context).getPrimaryProfile().id
        val repo = HealthRecordRepository.getInstance(context)
        val medCount = repo.getRecordsByCategory("Medication", primaryId).size
        val allergyCount = repo.getRecordsByCategory("Allergies", primaryId).size
        val conditionCount = repo.getRecordsByCategory("Conditions", primaryId).size
        val vaccCount = repo.getRecordsByCategory("Vaccinations", primaryId).size

        binding.tvMedicationsCount.text = getString(R.string.record_active_format, medCount)
        binding.tvAllergiesCount.text = getString(R.string.record_reported_format, allergyCount)
        binding.tvConditionsCount.text = getString(R.string.record_reported_format, conditionCount)
        binding.tvVaccinationsCount.text = getString(R.string.record_recorded_format, vaccCount)
    }

    private fun calculateAge(dobString: String?): String {
        if (dobString.isNullOrBlank()) return "--"
        return try {
            val birthDate = com.example.healthcareai.util.LocaleHelper.parseStoredDate(dobString) ?: return "--"
            val birthCal = Calendar.getInstance().apply { time = birthDate }
            val nowCal = Calendar.getInstance()
            var age = nowCal.get(Calendar.YEAR) - birthCal.get(Calendar.YEAR)
            if (nowCal.get(Calendar.DAY_OF_YEAR) < birthCal.get(Calendar.DAY_OF_YEAR)) {
                age--
            }
            if (age in 0..130) age.toString() else "--"
        } catch (_: Exception) {
            "--"
        }
    }

    private fun setupListeners() {
        // Open Edit Profile directly when clicking the profile card or pen icon
        val onEditClick = View.OnClickListener {
            val primaryId = ProfileRepository.getInstance(requireContext()).getPrimaryProfile().id
            AddProfileActivity.startEdit(requireContext(), primaryId)
        }
        binding.cardProfile.setOnClickListener(onEditClick)
        binding.ivEditProfile.setOnClickListener(onEditClick)

        // Health Records Cards (Admin / Primary Profile)
        binding.cardMedications.setOnClickListener {
            val primaryId = ProfileRepository.getInstance(requireContext()).getPrimaryProfile().id
            HealthRecordListActivity.start(requireContext(), HealthRecordListActivity.CATEGORY_MEDICATION, primaryId)
        }
        binding.cardAllergies.setOnClickListener {
            val primaryId = ProfileRepository.getInstance(requireContext()).getPrimaryProfile().id
            HealthRecordListActivity.start(requireContext(), HealthRecordListActivity.CATEGORY_ALLERGIES, primaryId)
        }
        binding.cardConditions.setOnClickListener {
            val primaryId = ProfileRepository.getInstance(requireContext()).getPrimaryProfile().id
            HealthRecordListActivity.start(requireContext(), HealthRecordListActivity.CATEGORY_CONDITIONS, primaryId)
        }
        binding.cardVaccinations.setOnClickListener {
            val primaryId = ProfileRepository.getInstance(requireContext()).getPrimaryProfile().id
            HealthRecordListActivity.start(requireContext(), HealthRecordListActivity.CATEGORY_VACCINATIONS, primaryId)
        }

        // Reminder View All and Add (Admin / Primary Profile)
        binding.tvViewAllReminders.setOnClickListener {
            val primaryId = ProfileRepository.getInstance(requireContext()).getPrimaryProfile().id
            ReminderActivity.start(requireContext(), profileId = primaryId)
        }

        binding.btnAddReminder.setOnClickListener {
            val primaryId = ProfileRepository.getInstance(requireContext()).getPrimaryProfile().id
            NewReminderActivity.start(requireContext(), profileId = primaryId)
        }

        // AI scan history
        binding.tvViewAllScans.setOnClickListener {
            val primaryId = ProfileRepository.getInstance(requireContext()).getPrimaryProfile().id
            AiScanHistoryActivity.start(requireContext(), primaryId)
        }

        binding.btnStartFirstScan.setOnClickListener {
            val primaryId = ProfileRepository.getInstance(requireContext()).getPrimaryProfile().id
            val intent = Intent(requireContext(), CameraActivity::class.java).apply {
                putExtra(CameraActivity.EXTRA_PROFILE_ID, primaryId)
            }
            startActivity(intent)
        }
    }

    /**
     * Synchronizes Today's reminder cards dynamically from ReminderRepository.
     */
    private fun syncReminders() {
        val ctx = context ?: return
        val primaryProfile = ProfileRepository.getInstance(ctx).getPrimaryProfile()
        val reminderRepo = ReminderRepository.getInstance(ctx)
        val reminders = reminderRepo.getRemindersForProfile(primaryProfile.id)

        if (reminders.isEmpty()) {
            binding.layoutEmptyReminder.visibility = View.VISIBLE
            binding.layoutReminderList.visibility = View.GONE
            binding.tvViewAllReminders.visibility = View.GONE
        } else {
            binding.layoutEmptyReminder.visibility = View.GONE
            binding.layoutReminderList.visibility = View.VISIBLE
            binding.tvViewAllReminders.visibility = View.VISIBLE

            binding.layoutReminderList.removeAllViews()
            val sorted = reminders.sortedWith(
                compareBy<ReminderItem> { it.isDone }
                    .thenBy { com.example.healthcareai.util.ReminderScheduler.parseTimeToMinutes(it.timeList.firstOrNull().orEmpty()) }
            )
            for (item in sorted.take(2)) {
                val cardBinding = ItemReminderCardBinding.inflate(layoutInflater, binding.layoutReminderList, false)
                val cardCtx = cardBinding.root.context
                cardBinding.ivReminderIcon.setImageResource(item.getIconResId())
                cardBinding.tvReminderTitle.text = item.getLocalizedName(cardCtx)
                val desc = item.getLocalizedDescription(cardCtx)
                if (desc.isNotBlank()) {
                    cardBinding.tvReminderDesc.visibility = View.VISIBLE
                    cardBinding.tvReminderDesc.text = desc
                } else {
                    cardBinding.tvReminderDesc.visibility = View.GONE
                }
                val time = item.getDisplayTime(requireContext())
                if (time.isNotBlank()) {
                    cardBinding.tvReminderTime.visibility = View.VISIBLE
                    cardBinding.tvReminderTime.text = time
                } else {
                    cardBinding.tvReminderTime.visibility = View.GONE
                }

                if (item.isDone) {
                    cardBinding.btnMarkDone.visibility = View.GONE
                    cardBinding.layoutTakenUndo.visibility = View.VISIBLE
                } else {
                    cardBinding.btnMarkDone.visibility = View.VISIBLE
                    cardBinding.layoutTakenUndo.visibility = View.GONE
                }

                cardBinding.cardReminderRoot.setOnClickListener {
                    ReminderDetailBottomSheetDialog.show(
                        parentFragmentManager,
                        item.id,
                        onUpdated = { syncReminders() },
                        onDeleted = { syncReminders() }
                    )
                }

                cardBinding.btnMarkDone.setOnClickListener {
                    reminderRepo.setReminderDone(item.id, true)
                    Toast.makeText(ctx, R.string.msg_marked_as_done, Toast.LENGTH_SHORT).show()
                    syncReminders()
                }

                cardBinding.btnUndo.setOnClickListener {
                    reminderRepo.setReminderDone(item.id, false)
                    Toast.makeText(ctx, R.string.msg_marked_as_undone, Toast.LENGTH_SHORT).show()
                    syncReminders()
                }

                binding.layoutReminderList.addView(cardBinding.root)
            }
        }
    }

    private fun syncScanHistory() {
        val ctx = context ?: return
        val primaryId = ProfileRepository.getInstance(ctx).getPrimaryProfile().id
        val scans = ScanHistoryRepository.getInstance(ctx).getScansForProfile(primaryId)

        if (scans.isEmpty()) {
            binding.layoutEmptyScans.visibility = View.VISIBLE
            binding.layoutScanList.visibility = View.GONE
            binding.tvViewAllScans.visibility = View.GONE
        } else {
            binding.layoutEmptyScans.visibility = View.GONE
            binding.layoutScanList.visibility = View.VISIBLE
            binding.tvViewAllScans.visibility = View.VISIBLE
            binding.layoutScanList.removeAllViews()

            for (scan in scans.take(2)) {
                val itemBinding = ItemScanHistoryCardBinding.inflate(
                    layoutInflater,
                    binding.layoutScanList,
                    false
                )
                itemBinding.tvScanTitle.text = com.example.healthcareai.util.LocaleHelper.formatScanTitle(ctx, scan.title, scan.category)
                itemBinding.tvScanDate.text = if (scan.timestamp > 0L) {
                    com.example.healthcareai.util.LocaleHelper.formatDate(ctx, scan.timestamp)
                } else {
                    com.example.healthcareai.util.LocaleHelper.formatStoredDate(ctx, scan.dateFormatted)
                }

                val imagePath = scan.imagePath
                if (!imagePath.isNullOrEmpty() && File(imagePath).exists()) {
                    itemBinding.ivScanThumbnail.load(File(imagePath)) {
                        crossfade(true)
                        error(R.drawable.no_scans_yet)
                    }
                } else {
                    itemBinding.ivScanThumbnail.setImageResource(R.drawable.no_scans_yet)
                }

                itemBinding.swipeRevealLayout.close(false)
                val openDetailListener = View.OnClickListener {
                    if (itemBinding.swipeRevealLayout.isOpen) {
                        itemBinding.swipeRevealLayout.close(true)
                    } else {
                        val intent = Intent(requireContext(), AnalysisResultsActivity::class.java).apply {
                            putExtra(AnalysisResultsActivity.EXTRA_IMAGE_PATH, scan.imagePath)
                            putExtra(AnalysisResultsActivity.EXTRA_CATEGORY, scan.category)
                            putExtra(AnalysisResultsActivity.EXTRA_PROFILE_ID, scan.profileId)
                            putExtra(AnalysisResultsActivity.EXTRA_IS_SUCCESS, true)
                            putExtra(AnalysisResultsActivity.EXTRA_ANALYSIS_RESULT, scan.analysisResult)
                            putExtra(AnalysisResultsActivity.EXTRA_SCAN_ID, scan.id)
                            scan.conversationId?.let { putExtra(AnalysisResultsActivity.EXTRA_CONVERSATION_ID, it) }
                        }
                        startActivity(intent)
                    }
                }
                itemBinding.cardScanRoot.setOnClickListener(openDetailListener)
                itemBinding.swipeRevealLayout.setOnClickListener(openDetailListener)

                itemBinding.btnDeleteAction.setOnClickListener {
                    itemBinding.swipeRevealLayout.close(false)
                    com.example.healthcareai.ui.dialog.DeleteConfirmationDialog.show(
                        context = requireContext(),
                        onDeleteConfirmed = {
                            ScanHistoryRepository.getInstance(requireContext()).deleteScan(scan.id)
                            try {
                                scan.imagePath?.let { path ->
                                    val file = File(path)
                                    if (file.exists()) file.delete()
                                }
                            } catch (_: Exception) {}
                            android.widget.Toast.makeText(requireContext(), R.string.msg_record_deleted, android.widget.Toast.LENGTH_SHORT).show()
                            syncScanHistory()
                        }
                    )
                }

                binding.layoutScanList.addView(itemBinding.root)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = MyHealthFragment()
    }
}
