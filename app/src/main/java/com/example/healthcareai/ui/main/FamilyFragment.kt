package com.example.healthcareai.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.healthcareai.R
import com.example.healthcareai.data.model.UserProfile
import com.example.healthcareai.data.repository.ProfileRepository
import com.example.healthcareai.databinding.FragmentFamilyBinding
import com.example.healthcareai.ui.dialog.DeleteConfirmationDialog
import com.example.healthcareai.ui.profile.AddProfileActivity
import com.example.healthcareai.ui.profile.ProfileDetailActivity

class FamilyFragment : Fragment() {

    private var _binding: FragmentFamilyBinding? = null
    private val binding get() = _binding!!
    private lateinit var profileRepository: ProfileRepository
    private lateinit var memberAdapter: FamilyMemberAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFamilyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        profileRepository = ProfileRepository.getInstance(requireContext())

        setupRecyclerView()
        setupWindowInsets()
        updateUiState()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        updateUiState()
    }

    private fun setupRecyclerView() {
        memberAdapter = FamilyMemberAdapter(
            onItemClick = { member ->
                ProfileDetailActivity.start(requireContext(), member.id)
            },
            onItemDelete = { member ->
                showDeleteMemberDialog(member)
            }
        )
        binding.rvFamilyMembers.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = memberAdapter
        }
    }

    private fun showDeleteMemberDialog(member: UserProfile) {
        DeleteConfirmationDialog.show(
            context = requireContext(),
            onDeleteConfirmed = {
                profileRepository.deleteProfile(member.id)
                Toast.makeText(requireContext(), R.string.msg_member_removed, Toast.LENGTH_SHORT).show()
                updateUiState()
            }
        )
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.rootFamilyLayout) { _, insets ->
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

    fun refreshData() {
        if (_binding != null) {
            updateUiState()
        }
    }

    /**
     * Updates UI based on whether family members exist:
     * - Top right "+" button is hidden (GONE) when no members exist, visible when members exist.
     * - Empty state shown when 0 members exist.
     * - Card list shown when 1+ members exist.
     */
    private fun updateUiState() {
        val familyMembers = profileRepository.getFamilyMembers()
        val hasMembers = familyMembers.isNotEmpty()

        binding.btnAddMemberTop.visibility = if (hasMembers) View.VISIBLE else View.GONE
        binding.layoutEmptyState.visibility = if (hasMembers) View.GONE else View.VISIBLE
        binding.rvFamilyMembers.visibility = if (hasMembers) View.VISIBLE else View.GONE

        if (hasMembers) {
            val reminderRepo = com.example.healthcareai.data.repository.ReminderRepository.getInstance(requireContext())
            val items = familyMembers.map { member ->
                val count = reminderRepo.getRemindersForProfile(member.id).size
                FamilyMemberItem(member, count)
            }
            memberAdapter.submitList(items)
        } else {
            memberAdapter.submitList(emptyList())
        }
    }

    private fun setupListeners() {
        binding.btnAddFirstMember.setOnClickListener {
            AddProfileActivity.start(requireContext(), isAddMember = true)
        }

        binding.btnAddMemberTop.setOnClickListener {
            AddProfileActivity.start(requireContext(), isAddMember = true)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = FamilyFragment()
    }
}
