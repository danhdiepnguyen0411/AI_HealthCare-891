package com.example.healthcareai.ui.symptom

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.healthcareai.R
import com.example.healthcareai.databinding.ItemSymptomBinding

data class SymptomItem(
    val id: String,
    val name: String
)

class SymptomsAdapter(
    private val isItemSelected: (String) -> Boolean,
    private val onItemClick: (SymptomItem) -> Unit
) : ListAdapter<SymptomItem, SymptomsAdapter.SymptomViewHolder>(SymptomDiffCallback()) {

    class SymptomDiffCallback : DiffUtil.ItemCallback<SymptomItem>() {
        override fun areItemsTheSame(oldItem: SymptomItem, newItem: SymptomItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: SymptomItem, newItem: SymptomItem): Boolean {
            return oldItem.name == newItem.name
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SymptomViewHolder {
        val binding = ItemSymptomBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SymptomViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SymptomViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item, isItemSelected(item.id))
    }

    override fun onBindViewHolder(
        holder: SymptomViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.contains(PAYLOAD_SELECTION_CHANGED)) {
            val item = getItem(position)
            holder.updateSelection(isItemSelected(item.id))
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    inner class SymptomViewHolder(
        private val binding: ItemSymptomBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SymptomItem, selected: Boolean) {
            binding.tvSymptomName.text = item.name
            updateSelection(selected)

            binding.cardSymptomItem.setOnClickListener {
                onItemClick(item)
                notifyItemChanged(bindingAdapterPosition, PAYLOAD_SELECTION_CHANGED)
            }
        }

        fun updateSelection(selected: Boolean) {
            val context = itemView.context
            if (selected) {
                binding.cardSymptomItem.setBackgroundResource(R.drawable.bg_symptom_card_selected)
                binding.tvSymptomName.setTextColor(ContextCompat.getColor(context, R.color.white))
                binding.ivCheckbox.setImageResource(R.drawable.bg_checkbox_selected_white)
            } else {
                binding.cardSymptomItem.setBackgroundResource(R.drawable.bg_symptom_card_unselected)
                binding.tvSymptomName.setTextColor(ContextCompat.getColor(context, R.color.color_text))
                binding.ivCheckbox.setImageResource(R.drawable.bg_checkbox_unchecked)
            }
        }
    }

    companion object {
        const val PAYLOAD_SELECTION_CHANGED = "payload_selection_changed"
    }
}
