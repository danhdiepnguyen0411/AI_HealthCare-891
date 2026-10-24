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
import com.example.healthcareai.R
import com.example.healthcareai.databinding.FragmentHomeBinding
import com.example.healthcareai.ui.camera.CameraActivity
import com.example.healthcareai.ui.heartrate.HeartRateActivity
import com.example.healthcareai.ui.symptom.SymptomCheckerActivity
import coil.load
import coil.transform.CircleCropTransformation
import com.example.healthcareai.data.repository.ProfileRepository
import com.example.healthcareai.ui.profile.AddProfileActivity
import com.example.healthcareai.ui.profile.ProfileDetailActivity
import java.util.Calendar

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private var selectedFamilyMemberIndex = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupWindowInsets()
        updateGreeting()
        setupListeners()
        syncFamilyProfiles()
    }

    override fun onResume() {
        super.onResume()
        updateGreeting()
        syncFamilyProfiles()
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val statusBarTop = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            val density = resources.displayMetrics.density
            val topPadding = if (statusBarTop > 0) {
                statusBarTop + (10 * density).toInt()
            } else {
                (50 * density).toInt()
            }
            binding.topBar.setPadding(
                (16 * density).toInt(),
                topPadding,
                (16 * density).toInt(),
                (16 * density).toInt()
            )
            insets
        }
    }

    /**
     * Updates the greeting based on the current time of day:
     * - Morning: 05:00 - 11:59 ("Good morning")
     * - Afternoon: 12:00 - 17:59 ("Good afternoon")
     * - Evening / Night: 18:00 - 04:59 ("Good evening")
     */
    private fun updateGreeting() {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greetingRes = when (hour) {
            in 5..11 -> R.string.home_greeting_morning
            in 12..17 -> R.string.home_greeting_afternoon
            else -> R.string.home_greeting_evening
        }
        binding.tvGreeting.setText(greetingRes)
    }

    private var isReminder1Done = false
    private var isReminder2Done = false

    private fun setupListeners() {
        // "Get Pro" button
        binding.btnGetPro.setOnClickListener {
            Toast.makeText(
                requireContext(),
                "HealthLink Pro: Unlimited AI scans & 24/7 care support!",
                Toast.LENGTH_SHORT
            ).show()
        }

        // "Chat with AI" button in the assistant card
        binding.btnChatWithAi.setOnClickListener {
            Toast.makeText(
                requireContext(),
                "Connecting to your personal AI Health Assistant...",
                Toast.LENGTH_SHORT
            ).show()
        }

        // Feature Card 1: Health Scan -> Switch to Scan Tab
        binding.cardHealthScan.setOnClickListener {
            (activity as? MainActivity)?.navigateToTab(1)
        }

        // Feature Card 2: Measure Heart Rate
        binding.cardMeasureHeartRate.setOnClickListener {
            startActivity(Intent(requireContext(), HeartRateActivity::class.java))
        }

        // Feature Card 3: Symptom Checker
        binding.cardSymptomChecker.setOnClickListener {
            startActivity(Intent(requireContext(), SymptomCheckerActivity::class.java))
        }

        // Today's reminder: "View all" and "Add reminder"
        binding.tvViewAllReminders.setOnClickListener {
            (activity as? MainActivity)?.navigateToTab(2)
        }

        binding.btnAddReminder.setOnClickListener {
            (activity as? MainActivity)?.navigateToTab(2)
        }

        // Reminder 1 Mark as done
        binding.btnMarkDoneReminder1.setOnClickListener {
            isReminder1Done = !isReminder1Done
            if (isReminder1Done) {
                binding.btnMarkDoneReminder1.setText(R.string.btn_done)
                binding.btnMarkDoneReminder1.alpha = 0.7f
                Toast.makeText(requireContext(), R.string.msg_marked_as_done, Toast.LENGTH_SHORT).show()
            } else {
                binding.btnMarkDoneReminder1.setText(R.string.btn_mark_as_done)
                binding.btnMarkDoneReminder1.alpha = 1.0f
            }
        }

        // Reminder 2 Mark as done
        binding.btnMarkDoneReminder2.setOnClickListener {
            isReminder2Done = !isReminder2Done
            if (isReminder2Done) {
                binding.btnMarkDoneReminder2.setText(R.string.btn_done)
                binding.btnMarkDoneReminder2.alpha = 0.7f
                Toast.makeText(requireContext(), R.string.msg_marked_as_done, Toast.LENGTH_SHORT).show()
            } else {
                binding.btnMarkDoneReminder2.setText(R.string.btn_mark_as_done)
                binding.btnMarkDoneReminder2.alpha = 1.0f
            }
        }

        // AI scan history: "View all" and "Start first scan"
        binding.tvViewAllScans.setOnClickListener {
            (activity as? MainActivity)?.navigateToTab(1)
        }

        binding.btnStartFirstScan.setOnClickListener {
            val intent = Intent(requireContext(), CameraActivity::class.java)
            startActivity(intent)
        }

        binding.cardScan1.setOnClickListener {
            (activity as? MainActivity)?.navigateToTab(1)
        }

        binding.cardScan2.setOnClickListener {
            (activity as? MainActivity)?.navigateToTab(1)
        }

        // Family Hub: Manage & Card click
        binding.tvManageFamily.setOnClickListener {
            (activity as? MainActivity)?.navigateToTab(3)
        }

        binding.cardFamilyHub.setOnClickListener {
            navigateToSelectedProfileDetail()
        }

        binding.layoutMemberThang.setOnClickListener {
            if (selectedFamilyMemberIndex == 0) {
                navigateToSelectedProfileDetail()
            } else {
                switchFamilyMember(0)
            }
        }

        binding.layoutMemberWoman.setOnClickListener {
            if (selectedFamilyMemberIndex == 1) {
                navigateToSelectedProfileDetail()
            } else {
                switchFamilyMember(1)
            }
        }

        binding.layoutMemberAdd.setOnClickListener {
            AddProfileActivity.start(requireContext(), isAddMember = true)
        }
    }

    private fun navigateToSelectedProfileDetail() {
        val ctx = context ?: return
        val profileRepository = ProfileRepository.getInstance(ctx)
        val allProfiles = profileRepository.getAllProfiles()
        val familyMembers = allProfiles.filter { !it.isPrimary }
        val targetProfile = if (selectedFamilyMemberIndex == 0 || familyMembers.isEmpty()) {
            profileRepository.getPrimaryProfile()
        } else {
            familyMembers.getOrNull(0) ?: profileRepository.getPrimaryProfile()
        }
        ProfileDetailActivity.start(ctx, targetProfile.id)
    }

    /**
     * Synchronizes profiles dynamically from ProfileRepository.
     */
    private fun syncFamilyProfiles() {
        val context = context ?: return
        val profileRepository = ProfileRepository.getInstance(context)
        val density = resources.displayMetrics.density
        val strokeSelected = (1 * density).toInt()

        // 1. Primary Profile (Member 1)
        val primary = profileRepository.getPrimaryProfile()
        binding.tvMemberThangName.text = primary.name
        if (!primary.avatarUri.isNullOrEmpty()) {
            binding.ivMemberThangAvatar.load(primary.avatarUri) {
                crossfade(true)
                transformations(CircleCropTransformation())
                placeholder(R.drawable.profile_person)
                error(R.drawable.profile_person)
            }
        } else {
            binding.ivMemberThangAvatar.setImageResource(R.drawable.profile_person)
        }

        // 2. Family Members (Member 2)
        val all = profileRepository.getAllProfiles()
        val familyMembers = all.filter { !it.isPrimary }
        if (familyMembers.isNotEmpty()) {
            val second = familyMembers[0]
            binding.layoutMemberWoman.visibility = View.VISIBLE
            binding.tvMemberWomanName.text = second.name
            if (!second.avatarUri.isNullOrEmpty()) {
                binding.ivMemberWomanAvatar.load(second.avatarUri) {
                    crossfade(true)
                    transformations(CircleCropTransformation())
                    placeholder(R.drawable.profile_person)
                    error(R.drawable.profile_person)
                }
            } else {
                binding.ivMemberWomanAvatar.setImageResource(R.drawable.profile_person)
            }
        } else {
            binding.layoutMemberWoman.visibility = View.GONE
            selectedFamilyMemberIndex = 0
            binding.cardFamilyHub.selectMember(0, animate = false)
            binding.cardMemberThangAvatar.strokeWidth = strokeSelected
            binding.tvMemberThangName.visibility = View.VISIBLE
            binding.cardMemberWomanAvatar.strokeWidth = 0
            binding.tvMemberWomanName.visibility = View.INVISIBLE
        }
    }

    /**
     * Switches active family member with smooth background tab animation,
     * border toggle, and name visibility toggle without mock data.
     */
    private fun switchFamilyMember(index: Int) {
        if (selectedFamilyMemberIndex == index) return
        selectedFamilyMemberIndex = index

        val density = resources.displayMetrics.density
        val strokeSelected = (1 * density).toInt()

        // Animate custom tab background in FamilyHubCardView
        binding.cardFamilyHub.selectMember(index, animate = true)

        if (index == 0) {
            // Member 1 (Primary) selected
            binding.cardMemberThangAvatar.strokeWidth = strokeSelected
            binding.tvMemberThangName.visibility = View.VISIBLE

            binding.cardMemberWomanAvatar.strokeWidth = 0
            binding.tvMemberWomanName.visibility = View.INVISIBLE
        } else {
            // Member 2 (Family Member) selected
            binding.cardMemberWomanAvatar.strokeWidth = strokeSelected
            binding.tvMemberWomanName.visibility = View.VISIBLE

            binding.cardMemberThangAvatar.strokeWidth = 0
            binding.tvMemberThangName.visibility = View.INVISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = HomeFragment()
    }
}
