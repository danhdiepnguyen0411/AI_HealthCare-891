package com.example.healthcareai.ui.language

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.healthcareai.R
import com.example.healthcareai.data.model.LanguageModel
import com.example.healthcareai.databinding.ItemLanguageBinding

class LanguageAdapter(
    private val items: MutableList<LanguageModel>,
    private val onItemSelected: (LanguageModel) -> Unit
) : RecyclerView.Adapter<LanguageAdapter.LanguageViewHolder>() {

    private var showHintOnDeviceLanguage: Boolean = true

    fun hideHint() {
        showHintOnDeviceLanguage = false
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageViewHolder {
        val binding = ItemLanguageBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return LanguageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LanguageViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class LanguageViewHolder(
        private val binding: ItemLanguageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: LanguageModel) {
            binding.ivFlag.setImageResource(item.flagRes)
            binding.tvLanguageName.text = item.name
            binding.rootContainer.isSelected = item.isSelected

            binding.ivRadio.setImageResource(
                if (item.isSelected) R.drawable.ic_radio_selected else R.drawable.ic_radio_unselected
            )

            // Show animated hint only on device language if hint is active and no item is selected
            if (showHintOnDeviceLanguage && item.isDeviceLanguage && !item.isSelected) {
                binding.ivHandHint.visibility = View.VISIBLE
            } else {
                binding.ivHandHint.visibility = View.GONE
            }

            binding.root.setOnClickListener {
                val previousSelectedIndex = items.indexOfFirst { it.isSelected }
                val currentPosition = bindingAdapterPosition
                if (currentPosition == RecyclerView.NO_POSITION) return@setOnClickListener

                showHintOnDeviceLanguage = false
                if (previousSelectedIndex != -1 && previousSelectedIndex != currentPosition) {
                    items[previousSelectedIndex].isSelected = false
                    notifyItemChanged(previousSelectedIndex)
                }

                items[currentPosition].isSelected = true
                notifyItemChanged(currentPosition)
                onItemSelected(items[currentPosition])
            }
        }
    }
}
