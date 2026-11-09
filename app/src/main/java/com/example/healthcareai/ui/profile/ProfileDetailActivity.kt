package com.example.healthcareai.ui.profile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import coil.load
import coil.transform.CircleCropTransformation
import com.example.healthcareai.R
import com.example.healthcareai.data.model.ReminderItem
import com.example.healthcareai.data.model.UserProfile
import com.example.healthcareai.data.repository.HealthRecordRepository
import com.example.healthcareai.data.repository.ProfileRepository
import com.example.healthcareai.data.repository.ReminderRepository
import com.example.healthcareai.data.repository.ScanHistoryRepository
import com.example.healthcareai.databinding.ActivityProfileDetailBinding
import com.example.healthcareai.databinding.ItemReminderCardBinding
import com.example.healthcareai.databinding.ItemScanHistoryCardBinding
import com.example.healthcareai.ui.base.BaseActivity
import com.example.healthcareai.ui.camera.AiScanHistoryActivity
import com.example.healthcareai.ui.camera.AnalysisResultsActivity
import com.example.healthcareai.ui.camera.CameraActivity
import com.example.healthcareai.ui.dialog.DeleteConfirmationDialog
import com.example.healthcareai.ui.reminder.HealthRecordListActivity
import com.example.healthcareai.ui.reminder.NewReminderActivity
import com.example.healthcareai.ui.reminder.ReminderActivity
import com.example.healthcareai.ui.reminder.ReminderDetailBottomSheetDialog
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ProfileDetailActivity : BaseActivity() {

    private lateinit var binding: ActivityProfileDetailBinding
    private lateinit var profileRepository: ProfileRepository
    private var profileId: String? = null
    private var currentProfile: UserProfile? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        profileRepository = ProfileRepository.getInstance(this)
        profileId = intent.getStringExtra(EXTRA_PROFILE_ID)

        setupWindowInsets()
        setupListeners()
        loadProfileData()
        syncHealthRecordCounts()
        syncReminders()
        syncScanHistory()
    }

    override fun onResume() {
        super.onResume()
        loadProfileData()
        syncHealthRecordCounts()
        syncReminders()
        syncScanHistory()
    }

    private fun setupWindowInsets() {
        setupHeaderTopMargin(binding.topBar, binding.rootProfileDetail)
    }

    private fun loadProfileData() {
        val allProfiles = profileRepository.getAllProfiles()
        val profile = allProfiles.find { it.id == profileId } ?: profileRepository.getPrimaryProfile()
        currentProfile = profile

        // Top bar title: Always "Detail Profile"
        binding.tvTitle.setText(R.string.title_detail_profile)

        // Delete button visible only for non-primary family members
        binding.btnDelete.visibility = if (!profile.isPrimary) View.VISIBLE else View.GONE

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

        // Name & Relationship
        binding.tvProfileName.text = if (profile.name.isNotBlank()) profile.name else "--"
        if (!profile.relationship.isNullOrBlank()) {
            val locRel = com.example.healthcareai.util.ProfileLocalizationHelper.getLocalizedRelationship(this, profile.relationship)
            if (locRel.isNotBlank()) {
                binding.tvProfileRelationship.visibility = View.VISIBLE
                binding.tvProfileRelationship.text = "($locRel)"
            } else {
                binding.tvProfileRelationship.visibility = View.GONE
            }
        } else {
            binding.tvProfileRelationship.visibility = View.GONE
        }

        // Gender
        binding.tvProfileGender.text = com.example.healthcareai.util.ProfileLocalizationHelper.getLocalizedGender(this, profile.gender)

        // Stats: Age, Height, Weight
        binding.tvAge.text = calculateAge(profile.dateOfBirth)
        binding.tvHeight.text = if (profile.heightCm > 0) "${profile.heightCm} cm" else "-- cm"
        binding.tvWeight.text = if (profile.weightKg > 0) "${profile.weightKg} kg" else "-- kg"
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
        binding.btnBack.setOnClickListener {
            finish()
        }

        val onEditClick = View.OnClickListener {
            currentProfile?.let { profile ->
                AddProfileActivity.startEdit(this, profile.id)
            }
        }
        binding.cardProfile.setOnClickListener(onEditClick)
        binding.ivEditProfile.setOnClickListener(onEditClick)

        binding.btnDelete.setOnClickListener {
            currentProfile?.let { profile ->
                showDeleteConfirmationDialog(profile)
            }
        }

        // Health Records - filtered by this member's profile
        binding.cardMedications.setOnClickListener {
            HealthRecordListActivity.start(this, HealthRecordListActivity.CATEGORY_MEDICATION, currentProfile?.id)
        }
        binding.cardAllergies.setOnClickListener {
            HealthRecordListActivity.start(this, HealthRecordListActivity.CATEGORY_ALLERGIES, currentProfile?.id)
        }
        binding.cardConditions.setOnClickListener {
            HealthRecordListActivity.start(this, HealthRecordListActivity.CATEGORY_CONDITIONS, currentProfile?.id)
        }
        binding.cardVaccinations.setOnClickListener {
            HealthRecordListActivity.start(this, HealthRecordListActivity.CATEGORY_VACCINATIONS, currentProfile?.id)
        }

        // Today's reminder - for this specific member
        binding.btnAddReminder.setOnClickListener {
            NewReminderActivity.start(this, profileId = currentProfile?.id)
        }

        binding.tvViewAllReminders.setOnClickListener {
            ReminderActivity.start(this, profileId = currentProfile?.id)
        }

        binding.tvViewAllScans.setOnClickListener {
            val targetId = currentProfile?.id ?: profileRepository.getPrimaryProfile().id
            AiScanHistoryActivity.start(this, targetId)
        }

        // AI Scan
        binding.btnStartFirstScan.setOnClickListener {
            val intent = Intent(this, CameraActivity::class.java).apply {
                currentProfile?.id?.let { putExtra(CameraActivity.EXTRA_PROFILE_ID, it) }
            }
            startActivity(intent)
        }
    }

    private fun syncScanHistory() {
        val targetId = currentProfile?.id
        val scanRepo = ScanHistoryRepository.getInstance(this)
        val scans = scanRepo.getScansForProfile(targetId)

        if (scans.isEmpty()) {
            binding.cardScan.visibility = View.VISIBLE
            binding.layoutScanList.visibility = View.GONE
            binding.tvViewAllScans.visibility = View.GONE
        } else {
            binding.cardScan.visibility = View.GONE
            binding.layoutScanList.visibility = View.VISIBLE
            binding.tvViewAllScans.visibility = View.VISIBLE
            binding.layoutScanList.removeAllViews()

            for (scan in scans.take(2)) {
                val itemBinding = ItemScanHistoryCardBinding.inflate(
                    layoutInflater,
                    binding.layoutScanList,
                    false
                )

                itemBinding.tvScanTitle.text = com.example.healthcareai.util.LocaleHelper.formatScanTitle(this, scan.title, scan.category)
                itemBinding.tvScanDate.text = if (scan.timestamp > 0L) {
                    com.example.healthcareai.util.LocaleHelper.formatDate(this, scan.timestamp)
                } else {
                    com.example.healthcareai.util.LocaleHelper.formatStoredDate(this, scan.dateFormatted)
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
                        val intent = Intent(this, AnalysisResultsActivity::class.java).apply {
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
                    DeleteConfirmationDialog.show(
                        context = this,
                        onDeleteConfirmed = {
                            scanRepo.deleteScan(scan.id)
                            try {
                                scan.imagePath?.let { path ->
                                    val file = File(path)
                                    if (file.exists()) file.delete()
                                }
                            } catch (_: Exception) {}
                            Toast.makeText(this, R.string.msg_record_deleted, Toast.LENGTH_SHORT).show()
                            syncScanHistory()
                        }
                    )
                }

                binding.layoutScanList.addView(itemBinding.root)
            }
        }
    }

    private fun showDeleteConfirmationDialog(profile: UserProfile) {
        DeleteConfirmationDialog.show(
            context = this,
            onDeleteConfirmed = {
                profileRepository.deleteProfile(profile.id)
                Toast.makeText(this, R.string.msg_member_removed, Toast.LENGTH_SHORT).show()
                finish()
            }
        )
    }

    private fun syncHealthRecordCounts() {
        val targetId = currentProfile?.id
        val repo = HealthRecordRepository.getInstance(this)
        val medCount = repo.getRecordsByCategory("Medication", targetId).size
        val allergyCount = repo.getRecordsByCategory("Allergies", targetId).size
        val conditionCount = repo.getRecordsByCategory("Conditions", targetId).size
        val vaccCount = repo.getRecordsByCategory("Vaccinations", targetId).size

        binding.tvMedicationsCount.text = getString(R.string.record_active_format, medCount)
        binding.tvAllergiesCount.text = getString(R.string.record_reported_format, allergyCount)
        binding.tvConditionsCount.text = getString(R.string.record_reported_format, conditionCount)
        binding.tvVaccinationsCount.text = getString(R.string.record_recorded_format, vaccCount)
    }

    private fun syncReminders() {
        val targetId = currentProfile?.id
        val reminderRepo = ReminderRepository.getInstance(this)
        val reminders = reminderRepo.getRemindersForProfile(targetId)

        if (reminders.isEmpty()) {
            binding.layoutEmptyReminder.visibility = View.VISIBLE
            binding.layoutReminderList.visibility = View.GONE
            binding.tvViewAllReminders.visibility = View.GONE
        } else {
            binding.layoutEmptyReminder.visibility = View.GONE
            binding.layoutReminderList.visibility = View.VISIBLE
            binding.tvViewAllReminders.visibility = View.VISIBLE
            binding.layoutReminderList.removeAllViews()

            val profile = currentProfile ?: profileRepository.getPrimaryProfile()

            val sorted = reminders.sortedWith(
                compareBy<ReminderItem> { it.isDone }
                    .thenBy { com.example.healthcareai.util.ReminderScheduler.parseTimeToMinutes(it.timeList.firstOrNull().orEmpty()) }
            )

            for (reminder in sorted.take(2)) {
                val cardBinding = ItemReminderCardBinding.inflate(
                    layoutInflater,
                    binding.layoutReminderList,
                    false
                )

                cardBinding.ivReminderIcon.setImageResource(reminder.getIconResId())
                cardBinding.tvReminderTitle.text = reminder.getLocalizedName(this)

                val desc = reminder.getLocalizedDescription(this)
                if (desc.isNotBlank()) {
                    cardBinding.tvReminderDesc.visibility = View.VISIBLE
                    cardBinding.tvReminderDesc.text = desc
                } else {
                    cardBinding.tvReminderDesc.visibility = View.GONE
                }

                val time = reminder.getDisplayTime(this)
                if (time.isNotBlank()) {
                    cardBinding.tvReminderTime.visibility = View.VISIBLE
                    cardBinding.tvReminderTime.text = time
                } else {
                    cardBinding.tvReminderTime.visibility = View.GONE
                }

                if (reminder.isDone) {
                    cardBinding.btnMarkDone.visibility = View.GONE
                    cardBinding.layoutTakenUndo.visibility = View.VISIBLE
                } else {
                    cardBinding.btnMarkDone.visibility = View.VISIBLE
                    cardBinding.layoutTakenUndo.visibility = View.GONE
                }

                cardBinding.btnMarkDone.setOnClickListener {
                    reminderRepo.setReminderDone(reminder.id, true)
                    Toast.makeText(this, R.string.msg_marked_as_done, Toast.LENGTH_SHORT).show()
                    syncReminders()
                }

                cardBinding.btnUndo.setOnClickListener {
                    reminderRepo.setReminderDone(reminder.id, false)
                    Toast.makeText(this, R.string.msg_marked_as_undone, Toast.LENGTH_SHORT).show()
                    syncReminders()
                }

                cardBinding.cardReminderRoot.setOnClickListener {
                    ReminderDetailBottomSheetDialog.show(
                        supportFragmentManager,
                        reminder.id,
                        onUpdated = { syncReminders() },
                        onDeleted = { syncReminders() }
                    )
                }

                binding.layoutReminderList.addView(cardBinding.root)
            }
        }
    }

    companion object {
        const val EXTRA_PROFILE_ID = "extra_profile_id"

        fun start(context: Context, profileId: String) {
            val intent = Intent(context, ProfileDetailActivity::class.java).apply {
                putExtra(EXTRA_PROFILE_ID, profileId)
            }
            context.startActivity(intent)
        }
    }
}
