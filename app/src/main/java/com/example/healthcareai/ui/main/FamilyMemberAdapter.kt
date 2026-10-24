package com.example.healthcareai.ui.main

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.example.healthcareai.R
import com.example.healthcareai.data.model.UserProfile
import com.example.healthcareai.databinding.ItemFamilyMemberBinding
import com.example.healthcareai.ui.heartrate.view.SwipeRevealLayout

class FamilyMemberAdapter(
    private val onItemClick: (UserProfile) -> Unit = {},
    private val onItemDelete: (UserProfile) -> Unit = {}
) : ListAdapter<UserProfile, FamilyMemberAdapter.MemberViewHolder>(DiffCallback) {

    private var currentlyOpenLayout: SwipeRevealLayout? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        val binding = ItemFamilyMemberBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MemberViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun closeOpenItem() {
        currentlyOpenLayout?.close(true)
        currentlyOpenLayout = null
    }

    inner class MemberViewHolder(
        private val binding: ItemFamilyMemberBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(member: UserProfile) {
            // Close swipe layout by default on rebind
            binding.swipeRevealLayout.close(false)

            binding.tvMemberName.text = member.name

            if (!member.relationship.isNullOrEmpty()) {
                binding.tvMemberRelationship.visibility = View.VISIBLE
                binding.tvMemberRelationship.text = "(${member.relationship})"
            } else {
                binding.tvMemberRelationship.visibility = View.GONE
            }

            binding.tvMemberGender.text = member.gender

            if (!member.avatarUri.isNullOrEmpty()) {
                binding.ivMemberAvatar.load(member.avatarUri) {
                    crossfade(true)
                    transformations(CircleCropTransformation())
                    placeholder(R.drawable.profile_person)
                    error(R.drawable.profile_person)
                }
            } else {
                binding.ivMemberAvatar.setImageResource(R.drawable.profile_person)
            }

            // Close other open swipe layouts when this one opens
            binding.swipeRevealLayout.onSwipeListener = object : SwipeRevealLayout.OnSwipeListener {
                override fun onOpened(layout: SwipeRevealLayout) {
                    if (currentlyOpenLayout != null && currentlyOpenLayout != layout) {
                        currentlyOpenLayout?.close(true)
                    }
                    currentlyOpenLayout = layout
                }

                override fun onClosed(layout: SwipeRevealLayout) {
                    if (currentlyOpenLayout == layout) {
                        currentlyOpenLayout = null
                    }
                }
            }

            // Clicking foreground card: if open, close it; else navigate to detail profile
            val clickListener = View.OnClickListener {
                if (binding.swipeRevealLayout.isOpen) {
                    binding.swipeRevealLayout.close(true)
                } else {
                    onItemClick(member)
                }
            }
            binding.cardForeground.setOnClickListener(clickListener)
            binding.swipeRevealLayout.setOnClickListener(clickListener)

            // Clicking delete action button on the revealed red area (#FF4D4F)
            binding.btnDeleteAction.setOnClickListener {
                binding.swipeRevealLayout.close(false)
                onItemDelete(member)
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<UserProfile>() {
        override fun areItemsTheSame(oldItem: UserProfile, newItem: UserProfile): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: UserProfile, newItem: UserProfile): Boolean {
            return oldItem == newItem
        }
    }
}
