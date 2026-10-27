package com.example.healthcareai.ui.main

import android.content.Context
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.example.healthcareai.R
import com.example.healthcareai.data.model.UserProfile
import com.example.healthcareai.data.repository.ReminderRepository
import com.example.healthcareai.databinding.ItemFamilyMemberBinding
import com.example.healthcareai.ui.heartrate.view.SwipeRevealLayout
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

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
            val context = binding.root.context

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

            // Stats: Age, Height, Weight, Reminder
            binding.tvMemberAge.text = calculateAge(member.dateOfBirth)
            binding.tvMemberHeight.text = formatUnit(context, member.heightCm, "cm")
            binding.tvMemberWeight.text = formatUnit(context, member.weightKg, "kg")

            val reminderRepo = ReminderRepository.getInstance(context)
            val reminders = reminderRepo.getAllReminders()
            val memberReminderCount = reminders.count { reminder ->
                (reminder.profileId != null && reminder.profileId == member.id) ||
                (member.name.isNotBlank() && (
                    reminder.notes.contains(member.name, ignoreCase = true) ||
                    reminder.name.contains(member.name, ignoreCase = true)
                ))
            }
            binding.tvMemberReminder.text = memberReminderCount.toString()

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

        private fun formatUnit(context: Context, value: Int, unit: String): CharSequence {
            if (value <= 0) return "-- $unit"
            val fullText = "$value $unit"
            val spannable = SpannableString(fullText)
            val unitIndex = fullText.indexOf(unit)
            if (unitIndex >= 0) {
                spannable.setSpan(
                    RelativeSizeSpan(0.85f),
                    unitIndex,
                    fullText.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                spannable.setSpan(
                    ForegroundColorSpan(ContextCompat.getColor(context, R.color.color_subtext)),
                    unitIndex,
                    fullText.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            return spannable
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
