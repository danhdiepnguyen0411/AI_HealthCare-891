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
import com.example.healthcareai.databinding.ActivityProfileDetailBinding
import com.example.healthcareai.databinding.ItemReminderCardBinding
import com.example.healthcareai.ui.base.BaseActivity
import com.example.healthcareai.ui.camera.CameraActivity
import com.example.healthcareai.ui.dialog.DeleteConfirmationDialog
import com.example.healthcareai.ui.reminder.HealthRecordListActivity
import com.example.healthcareai.ui.reminder.NewReminderActivity
import com.example.healthcareai.ui.reminder.ReminderActivity
import com.example.healthcareai.ui.reminder.ReminderDetailBottomSheetDialog
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
    }

    override fun onResume() {
        super.onResume()
        loadProfileData()
        syncHealthRecordCounts()
        syncReminders()
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.rootProfileDetail) { _, insets ->
            val statusBarTop = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            val density = resources.displayMetrics.density
            val targetTopMargin = if (statusBarTop > 0) {
                statusBarTop + (8 * density).toInt()
            } else {
                (24 * density).toInt()
            }
            binding.topBar.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                if (topMargin != targetTopMargin) {
                    topMargin = targetTopMargin
                }
            }
            insets
        }
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

        // Name
        binding.tvProfileName.text = if (profile.name.isNotBlank()) profile.name else "--"

        // Gender & Relationship
        val genderStr = if (profile.gender.isNotBlank()) profile.gender else "--"
        binding.tvProfileGender.text = if (!profile.relationship.isNullOrEmpty()) {
            "$genderStr (${profile.relationship})"
        } else {
            genderStr
        }

        // Stats: Age, Height, Weight
        binding.tvAge.text = calculateAge(profile.dateOfBirth)
        binding.tvHeight.text = if (profile.heightCm > 0) "${profile.heightCm} cm" else "-- cm"
        binding.tvWeight.text = if (profile.weightKg > 0) "${profile.weightKg} kg" else "-- kg"
    }

    private fun calculateAge(dobString: String?): String {
        if (dobString.isNullOrBlank()) return "--"
        return try {
            val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.US)
            val birthDate = sdf.parse(dobString) ?: return "--"
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

        // AI Scan
        binding.btnStartFirstScan.setOnClickListener {
            startActivity(Intent(this, CameraActivity::class.java))
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

        binding.tvMedicationsCount.text = "$medCount active"
        binding.tvAllergiesCount.text = "$allergyCount reported"
        binding.tvConditionsCount.text = "$conditionCount active"
        binding.tvVaccinationsCount.text = "$vaccCount recorded"
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
                    .thenBy { it.timeList.firstOrNull() ?: "" }
            )

            for (reminder in sorted.take(2)) {
                val cardBinding = ItemReminderCardBinding.inflate(
                    layoutInflater,
                    binding.layoutReminderList,
                    false
                )

                cardBinding.ivReminderIcon.setImageResource(reminder.getIconResId())
                cardBinding.tvReminderTitle.text = reminder.name

                val desc = reminder.getDisplayDescription()
                if (desc.isNotBlank()) {
                    cardBinding.tvReminderDesc.visibility = View.VISIBLE
                    cardBinding.tvReminderDesc.text = desc
                } else {
                    cardBinding.tvReminderDesc.visibility = View.GONE
                }

                val time = reminder.getDisplayTime()
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
