package com.example.healthcareai.ui.camera

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.example.healthcareai.R
import com.example.healthcareai.data.model.UserProfile
import com.example.healthcareai.data.repository.ProfileRepository
import com.example.healthcareai.databinding.BottomSheetSaveResultToBinding
import com.example.healthcareai.databinding.ItemBottomSheetProfileBinding
import com.example.healthcareai.util.DialogImmersiveHelper
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class SaveResultToBottomSheetDialog : BottomSheetDialogFragment() {

    private var _binding: BottomSheetSaveResultToBinding? = null
    private val binding get() = _binding!!

    private lateinit var profileRepo: ProfileRepository
    private var profilesList: List<UserProfile> = emptyList()
    private var selectedProfileId: String? = null
    private var onProfileSelectedListener: ((UserProfile) -> Unit)? = null

    override fun getTheme(): Int = R.style.TransparentBottomSheetDialogTheme

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetSaveResultToBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        profileRepo = ProfileRepository.getInstance(requireContext())

        selectedProfileId = arguments?.getString(ARG_SELECTED_PROFILE_ID)

        loadProfiles()
        setupRecyclerView()
        setupListeners()
    }

    private fun loadProfiles() {
        val all = profileRepo.getAllProfiles()
        profilesList = if (all.isNotEmpty()) {
            all
        } else {
            listOf(profileRepo.getPrimaryProfile())
        }

        if (selectedProfileId.isNullOrEmpty() || profilesList.none { it.id == selectedProfileId }) {
            selectedProfileId = profilesList.firstOrNull()?.id
        }
    }

    private fun setupRecyclerView() {
        val adapter = ProfileSelectionAdapter(
            profiles = profilesList,
            selectedId = selectedProfileId
        ) { clickedProfile ->
            selectedProfileId = clickedProfile.id
        }
        binding.rvProfiles.layoutManager = LinearLayoutManager(requireContext())
        binding.rvProfiles.adapter = adapter
    }

    private fun setupListeners() {
        // Close (X) button
        binding.btnClose.setOnClickListener {
            dismiss()
        }

        // Save Button
        binding.btnSave.setOnClickListener {
            val selected = profilesList.find { it.id == selectedProfileId }
                ?: profilesList.firstOrNull()
            if (selected != null) {
                onProfileSelectedListener?.invoke(selected)
            }
            dismiss()
        }
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
    // Adapter for Profile Items
    // =========================================================================
    private inner class ProfileSelectionAdapter(
        private val profiles: List<UserProfile>,
        private var selectedId: String?,
        private val onProfileClicked: (UserProfile) -> Unit
    ) : RecyclerView.Adapter<ProfileSelectionAdapter.ProfileViewHolder>() {

        inner class ProfileViewHolder(val itemBinding: ItemBottomSheetProfileBinding) :
            RecyclerView.ViewHolder(itemBinding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProfileViewHolder {
            val itemBinding = ItemBottomSheetProfileBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return ProfileViewHolder(itemBinding)
        }

        override fun onBindViewHolder(holder: ProfileViewHolder, position: Int) {
            val profile = profiles[position]
            val isSelected = profile.id == selectedId
            val context = holder.itemView.context

            // Name
            val displayName = profile.name.ifBlank { "User" }
            holder.itemBinding.tvProfileName.text = displayName

            // Relationship / Subtitle
            val relationship = if (profile.isPrimary || profile.relationship.isNullOrBlank()) {
                "You"
            } else {
                profile.relationship
            }
            holder.itemBinding.tvProfileRelationship.text = relationship

            // Avatar
            val avatarUri = profile.avatarUri
            if (!avatarUri.isNullOrEmpty()) {
                holder.itemBinding.ivProfileAvatar.load(avatarUri) {
                    crossfade(true)
                    transformations(CircleCropTransformation())
                    error(R.drawable.profile_person)
                }
            } else {
                holder.itemBinding.ivProfileAvatar.setImageResource(R.drawable.profile_person)
            }

            // Selection Visuals
            if (isSelected) {
                holder.itemBinding.cardProfileItem.setBackgroundResource(R.drawable.bg_bottom_sheet_item_selected)
                holder.itemBinding.tvProfileName.setTextColor(Color.WHITE)
                holder.itemBinding.tvProfileRelationship.setTextColor(Color.parseColor("#E0E7FF"))
                holder.itemBinding.ivCheckbox.setImageResource(R.drawable.bg_checkbox_selected_white)
            } else {
                holder.itemBinding.cardProfileItem.setBackgroundResource(R.drawable.bg_bottom_sheet_item_unselected)
                holder.itemBinding.tvProfileName.setTextColor(ContextCompat.getColor(context, R.color.color_text))
                holder.itemBinding.tvProfileRelationship.setTextColor(ContextCompat.getColor(context, R.color.neutral_03))
                holder.itemBinding.ivCheckbox.setImageResource(R.drawable.bg_checkbox_unchecked)
            }

            // Click listener
            holder.itemView.setOnClickListener {
                if (selectedId != profile.id) {
                    val prevId = selectedId
                    selectedId = profile.id
                    onProfileClicked(profile)

                    // Refresh both previously selected and newly selected positions
                    val prevIndex = profiles.indexOfFirst { it.id == prevId }
                    if (prevIndex >= 0) notifyItemChanged(prevIndex)
                    notifyItemChanged(position)
                }
            }
        }

        override fun getItemCount(): Int = profiles.size
    }

    companion object {
        const val TAG = "SaveResultToBottomSheetDialog"
        private const val ARG_SELECTED_PROFILE_ID = "arg_selected_profile_id"

        fun show(
            fragmentManager: FragmentManager,
            selectedProfileId: String?,
            onSelected: (UserProfile) -> Unit
        ): SaveResultToBottomSheetDialog {
            val existing = fragmentManager.findFragmentByTag(TAG) as? SaveResultToBottomSheetDialog
            if (existing != null) {
                existing.dismissAllowingStateLoss()
            }
            val dialog = SaveResultToBottomSheetDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_SELECTED_PROFILE_ID, selectedProfileId)
                }
                this.onProfileSelectedListener = onSelected
            }
            dialog.show(fragmentManager, TAG)
            return dialog
        }
    }
}

