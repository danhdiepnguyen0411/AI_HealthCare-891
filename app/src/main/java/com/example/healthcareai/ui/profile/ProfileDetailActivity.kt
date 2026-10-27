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
import com.example.healthcareai.data.model.UserProfile
import com.example.healthcareai.data.repository.ProfileRepository
import com.example.healthcareai.databinding.ActivityProfileDetailBinding
import com.example.healthcareai.ui.base.BaseActivity
import com.example.healthcareai.ui.camera.CameraActivity
import com.example.healthcareai.ui.dialog.DeleteConfirmationDialog
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
    }

    override fun onResume() {
        super.onResume()
        loadProfileData()
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

        // Health Records
        binding.cardMedications.setOnClickListener {
            Toast.makeText(this, R.string.record_medications, Toast.LENGTH_SHORT).show()
        }
        binding.cardAllergies.setOnClickListener {
            Toast.makeText(this, R.string.record_allergies, Toast.LENGTH_SHORT).show()
        }
        binding.cardConditions.setOnClickListener {
            Toast.makeText(this, R.string.record_conditions, Toast.LENGTH_SHORT).show()
        }
        binding.cardVaccinations.setOnClickListener {
            Toast.makeText(this, R.string.record_vaccinations, Toast.LENGTH_SHORT).show()
        }

        // Today's reminder
        binding.btnAddReminder.setOnClickListener {
            startActivity(android.content.Intent(this, com.example.healthcareai.ui.reminder.NewReminderActivity::class.java))
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
