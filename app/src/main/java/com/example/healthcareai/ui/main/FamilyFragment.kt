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
import com.example.healthcareai.R
import com.example.healthcareai.databinding.FragmentFamilyBinding

class FamilyFragment : Fragment() {

    private var _binding: FragmentFamilyBinding? = null
    private val binding get() = _binding!!

    // Simulating family members list (initially empty)
    private val familyMembers = mutableListOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFamilyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupWindowInsets()
        updateUiState()
        setupListeners()
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.rootFamilyLayout) { _, insets ->
            val statusBarTop = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            if (statusBarTop > 0) {
                binding.topBar.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    topMargin = statusBarTop + 8
                }
            }
            insets
        }
    }

    /**
     * Updates UI based on whether family members exist:
     * - When empty: Top right "+" button is hidden (GONE), empty state illustration and "Add first member" shown.
     * - When members exist: Top right "+" button is visible.
     */
    private fun updateUiState() {
        val hasMembers = familyMembers.isNotEmpty()
        binding.btnAddMemberTop.visibility = if (hasMembers) View.VISIBLE else View.GONE
        binding.layoutEmptyState.visibility = if (hasMembers) View.GONE else View.VISIBLE
    }

    private fun setupListeners() {
        binding.btnAddFirstMember.setOnClickListener {
            Toast.makeText(
                requireContext(),
                R.string.family_add_member_hint,
                Toast.LENGTH_SHORT
            ).show()
        }

        binding.btnAddMemberTop.setOnClickListener {
            Toast.makeText(
                requireContext(),
                R.string.family_add_member_hint,
                Toast.LENGTH_SHORT
            ).show()
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
