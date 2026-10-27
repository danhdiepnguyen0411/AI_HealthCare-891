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
import com.example.healthcareai.data.repository.ProfileRepository
import com.example.healthcareai.data.repository.ReminderRepository
import com.example.healthcareai.databinding.FragmentMyHealthBinding
import com.example.healthcareai.databinding.ItemReminderCardBinding
import com.example.healthcareai.ui.camera.CameraActivity
import com.example.healthcareai.ui.profile.AddProfileActivity
import com.example.healthcareai.ui.reminder.NewReminderActivity
import com.example.healthcareai.ui.reminder.ReminderActivity
import com.example.healthcareai.ui.reminder.ReminderDetailBottomSheetDialog
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
    }

    override fun onResume() {
        super.onResume()
        loadPrimaryProfile()
        syncReminders()
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.rootMyHealth) { _, insets ->
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
        binding.tvProfileGender.text = if (profile.gender.isNotBlank()) profile.gender else "--"

        // Age calculation from DOB
        val ageStr = calculateAge(profile.dateOfBirth)
        binding.tvAge.text = ageStr

        // Height & Weight
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
        // Edit Profile
        binding.cardProfile.setOnClickListener {
            AddProfileActivity.start(requireContext())
        }

        // Health Records Cards
        binding.cardMedications.setOnClickListener {
            Toast.makeText(requireContext(), R.string.record_medications, Toast.LENGTH_SHORT).show()
        }
        binding.cardAllergies.setOnClickListener {
            Toast.makeText(requireContext(), R.string.record_allergies, Toast.LENGTH_SHORT).show()
        }
        binding.cardConditions.setOnClickListener {
            Toast.makeText(requireContext(), R.string.record_conditions, Toast.LENGTH_SHORT).show()
        }
        binding.cardVaccinations.setOnClickListener {
            Toast.makeText(requireContext(), R.string.record_vaccinations, Toast.LENGTH_SHORT).show()
        }

        // Reminder View All and Add
        binding.tvViewAllReminders.setOnClickListener {
            startActivity(Intent(requireContext(), ReminderActivity::class.java))
        }

        binding.btnAddReminder.setOnClickListener {
            startActivity(Intent(requireContext(), NewReminderActivity::class.java))
        }

        // AI scan history
        binding.tvViewAllScans.setOnClickListener {
            (activity as? MainActivity)?.navigateToTab(1)
        }

        binding.btnStartFirstScan.setOnClickListener {
            startActivity(Intent(requireContext(), CameraActivity::class.java))
        }

        binding.cardScan1.setOnClickListener {
            (activity as? MainActivity)?.navigateToTab(1)
        }

        binding.cardScan2.setOnClickListener {
            (activity as? MainActivity)?.navigateToTab(1)
        }
    }

    /**
     * Synchronizes Today's reminder cards dynamically from ReminderRepository.
     */
    private fun syncReminders() {
        val ctx = context ?: return
        val reminderRepo = ReminderRepository.getInstance(ctx)
        val reminders = reminderRepo.getAllReminders()

        if (reminders.isEmpty()) {
            binding.layoutEmptyReminder.visibility = View.VISIBLE
            binding.layoutReminderList.visibility = View.GONE
            binding.tvViewAllReminders.visibility = View.GONE
        } else {
            binding.layoutEmptyReminder.visibility = View.GONE
            binding.layoutReminderList.visibility = View.VISIBLE
            binding.tvViewAllReminders.visibility = View.VISIBLE

            binding.layoutReminderList.removeAllViews()
            for (item in reminders) {
                val cardBinding = ItemReminderCardBinding.inflate(layoutInflater, binding.layoutReminderList, false)
                cardBinding.ivReminderIcon.setImageResource(item.getIconResId())
                cardBinding.tvReminderTitle.text = item.name
                val desc = item.getDisplayDescription()
                if (desc.isNotBlank()) {
                    cardBinding.tvReminderDesc.visibility = View.VISIBLE
                    cardBinding.tvReminderDesc.text = desc
                } else {
                    cardBinding.tvReminderDesc.visibility = View.GONE
                }
                val time = item.getDisplayTime()
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = MyHealthFragment()
    }
}
