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
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.card.MaterialCardView
import com.example.healthcareai.databinding.ItemHomeFamilyAvatarBinding
import coil.load
import coil.transform.CircleCropTransformation
import com.example.healthcareai.data.repository.ProfileRepository
import com.example.healthcareai.ui.chat.AiChatActivity
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
            startActivity(Intent(requireContext(), AiChatActivity::class.java))
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

        binding.layoutMemberAdd.setOnClickListener {
            AddProfileActivity.start(requireContext(), isAddMember = true)
        }

        binding.scrollFamilyAvatars.setOnScrollChangeListener { _, _, _, _, _ ->
            binding.cardFamilyHub.updateTabBoundsForScroll()
        }
    }

    private fun navigateToSelectedProfileDetail() {
        val ctx = context ?: return
        val profileRepository = ProfileRepository.getInstance(ctx)
        val familyMembers = profileRepository.getFamilyMembers()
        if (familyMembers.isEmpty()) {
            AddProfileActivity.start(ctx, isAddMember = true)
            return
        }
        val targetProfile = familyMembers.getOrNull(selectedFamilyMemberIndex) ?: familyMembers[0]
        ProfileDetailActivity.start(ctx, targetProfile.id)
    }

    /**
     * Synchronizes profiles dynamically from ProfileRepository and populates
     * only family members (excluding primary/creator profile) in layoutFamilyAvatarsRow
     * before layoutMemberAdd.
     */
    private fun syncFamilyProfiles() {
        val context = context ?: return
        val profileRepository = ProfileRepository.getInstance(context)
        val density = resources.displayMetrics.density
        val strokeSelected = (1 * density).toInt()

        val familyMembers = profileRepository.getFamilyMembers()

        val row = binding.layoutFamilyAvatarsRow
        val addBtn = binding.layoutMemberAdd
        val addBtnIndex = row.indexOfChild(addBtn)
        if (addBtnIndex > 0) {
            row.removeViews(0, addBtnIndex)
        }

        if (familyMembers.isEmpty()) {
            selectedFamilyMemberIndex = -1
            (addBtn.layoutParams as? ViewGroup.MarginLayoutParams)?.apply {
                marginStart = 0
            }
            binding.cardFamilyHub.selectMember(-1, animate = false, force = true)
            return
        }

        if (selectedFamilyMemberIndex !in familyMembers.indices) {
            selectedFamilyMemberIndex = 0
        }

        (addBtn.layoutParams as? ViewGroup.MarginLayoutParams)?.apply {
            marginStart = (20 * density).toInt()
        }

        for ((index, profile) in familyMembers.withIndex()) {
            val itemBinding = ItemHomeFamilyAvatarBinding.inflate(layoutInflater, row, false)
            val lp = LinearLayout.LayoutParams(
                (64 * density).toInt(),
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = if (index == 0) 0 else (20 * density).toInt()
            }
            itemBinding.root.layoutParams = lp

            itemBinding.tvMemberName.text = profile.name
            if (!profile.avatarUri.isNullOrEmpty()) {
                itemBinding.ivMemberAvatar.load(profile.avatarUri) {
                    crossfade(true)
                    transformations(CircleCropTransformation())
                    placeholder(R.drawable.profile_person)
                    error(R.drawable.profile_person)
                }
            } else {
                itemBinding.ivMemberAvatar.setImageResource(R.drawable.profile_person)
            }

            val isSelected = (index == selectedFamilyMemberIndex)
            itemBinding.cardMemberAvatar.strokeWidth = if (isSelected) strokeSelected else 0
            itemBinding.tvMemberName.visibility = if (isSelected) View.VISIBLE else View.INVISIBLE

            itemBinding.root.setOnClickListener {
                if (selectedFamilyMemberIndex == index) {
                    ProfileDetailActivity.start(requireContext(), profile.id)
                } else {
                    switchFamilyMember(index)
                }
            }

            val insertIndex = row.indexOfChild(addBtn).coerceAtLeast(0)
            row.addView(itemBinding.root, insertIndex)
        }

        binding.cardFamilyHub.selectMember(selectedFamilyMemberIndex, animate = false, force = true)
    }

    /**
     * Switches active family member with smooth background tab animation,
     * border toggle, and name visibility toggle.
     */
    private fun switchFamilyMember(index: Int) {
        if (selectedFamilyMemberIndex == index) return
        selectedFamilyMemberIndex = index

        val density = resources.displayMetrics.density
        val strokeSelected = (1 * density).toInt()

        val row = binding.layoutFamilyAvatarsRow
        val addBtn = binding.layoutMemberAdd
        val addBtnIndex = row.indexOfChild(addBtn)
        val profileCount = if (addBtnIndex >= 0) addBtnIndex else row.childCount

        for (i in 0 until profileCount) {
            val child = row.getChildAt(i) ?: continue
            val card = child.findViewById<MaterialCardView>(R.id.cardMemberAvatar)
            val tvName = child.findViewById<TextView>(R.id.tvMemberName)
            if (i == index) {
                card?.strokeWidth = strokeSelected
                tvName?.visibility = View.VISIBLE
            } else {
                card?.strokeWidth = 0
                tvName?.visibility = View.INVISIBLE
            }
        }

        // Animate custom tab background in FamilyHubCardView
        binding.cardFamilyHub.selectMember(index, animate = true)

        // Smooth scroll to make selected member visible if near/beyond edge
        val selectedChild = row.getChildAt(index)
        if (selectedChild != null) {
            val scrollX = binding.scrollFamilyAvatars.scrollX
            val scrollWidth = binding.scrollFamilyAvatars.width
            val childLeft = selectedChild.left
            val childRight = selectedChild.right
            val margin = (20 * density).toInt()

            if (childLeft - margin < scrollX) {
                binding.scrollFamilyAvatars.smoothScrollTo((childLeft - margin).coerceAtLeast(0), 0)
            } else if (childRight + margin > scrollX + scrollWidth && scrollWidth > 0) {
                binding.scrollFamilyAvatars.smoothScrollTo(childRight + margin - scrollWidth, 0)
            }
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
