package com.example.healthcareai.ui.reminder

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.healthcareai.data.model.ReminderItem
import com.example.healthcareai.databinding.ItemReminderCardBinding

class ReminderAdapter(
    private val onItemClick: (ReminderItem) -> Unit,
    private val onMarkDoneClick: (ReminderItem, Int) -> Unit,
    private val onUndoClick: (ReminderItem, Int) -> Unit
) : RecyclerView.Adapter<ReminderAdapter.ReminderViewHolder>() {

    private val items = mutableListOf<ReminderItem>()

    fun submitList(newList: List<ReminderItem>) {
        val diffCallback = object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = items.size
            override fun getNewListSize(): Int = newList.size

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return items[oldItemPosition].id == newList[newItemPosition].id
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                val old = items[oldItemPosition]
                val new = newList[newItemPosition]
                return old == new
            }
        }
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        items.clear()
        items.addAll(newList)
        diffResult.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReminderViewHolder {
        val binding = ItemReminderCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ReminderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReminderViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ReminderViewHolder(private val binding: ItemReminderCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ReminderItem) {
            binding.ivReminderIcon.setImageResource(item.getIconResId())
            binding.tvReminderTitle.text = item.name
            val desc = item.getDisplayDescription()
            if (desc.isNotBlank()) {
                binding.tvReminderDesc.visibility = View.VISIBLE
                binding.tvReminderDesc.text = desc
            } else {
                binding.tvReminderDesc.visibility = View.GONE
            }
            val time = item.getDisplayTime()
            if (time.isNotBlank()) {
                binding.tvReminderTime.visibility = View.VISIBLE
                binding.tvReminderTime.text = time
            } else {
                binding.tvReminderTime.visibility = View.GONE
            }

            if (item.isDone) {
                binding.btnMarkDone.visibility = View.GONE
                binding.layoutTakenUndo.visibility = View.VISIBLE
            } else {
                binding.btnMarkDone.visibility = View.VISIBLE
                binding.layoutTakenUndo.visibility = View.GONE
            }

            binding.cardReminderRoot.setOnClickListener {
                onItemClick(item)
            }

            binding.btnMarkDone.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    onMarkDoneClick(item, pos)
                }
            }

            binding.btnUndo.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    onUndoClick(item, pos)
                }
            }
        }
    }
}
