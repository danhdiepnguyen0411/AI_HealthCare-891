package com.example.healthcareai.ui.language

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.healthcareai.R
import com.example.healthcareai.data.model.LanguageModel
import com.example.healthcareai.databinding.ItemLanguageBinding

class LanguageAdapter(
    private val items: MutableList<LanguageModel>,
    private val onItemSelected: (LanguageModel, View) -> Unit
) : RecyclerView.Adapter<LanguageAdapter.LanguageViewHolder>() {

    var onItemBound: ((position: Int, itemView: View) -> Unit)? = null

    fun getSelectedPosition(): Int = items.indexOfFirst { it.isSelected }

    fun getSelectedLanguage(): LanguageModel? = items.find { it.isSelected }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageViewHolder {
        val binding = ItemLanguageBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return LanguageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LanguageViewHolder, position: Int) {
        holder.bind(items[position])
        onItemBound?.invoke(position, holder.binding.root)
    }

    override fun getItemCount(): Int = items.size

    inner class LanguageViewHolder(
        val binding: ItemLanguageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: LanguageModel) {
            val context = binding.root.context

            binding.ivFlag.setImageResource(item.flagRes)
            binding.tvLanguageName.text = item.name
            binding.tvNativeName.text = item.nativeName

            if (item.isSelected) {
                binding.rootContainer.setBackgroundResource(R.drawable.bg_language_card_selected)
                binding.tvLanguageName.setTextColor(Color.WHITE)
                binding.tvNativeName.setTextColor(Color.WHITE)
                binding.ivRadio.setImageResource(R.drawable.sellect_lang)
            } else {
                binding.rootContainer.setBackgroundResource(R.drawable.bg_language_card_unselected)
                binding.tvLanguageName.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                binding.tvNativeName.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                binding.ivRadio.setImageResource(R.drawable.unsellect_lang)
            }

            // Hand hint exactly like VIDEO_762Y4
            if (item.showTapHint) {
                binding.ivItemTapHint.visibility = View.VISIBLE
                binding.ivItemTapHint.load(R.drawable.hand_click)
            } else {
                binding.ivItemTapHint.visibility = View.GONE
            }

            binding.root.setOnClickListener {
                val currentPosition = bindingAdapterPosition
                if (currentPosition == RecyclerView.NO_POSITION) return@setOnClickListener

                val clickedRadioView = binding.ivRadio

                // Clear tap hint across all items once user makes a selection
                items.forEach {
                    it.showTapHint = false
                    it.isSelected = false
                }
                items[currentPosition].isSelected = true
                notifyDataSetChanged()

                onItemSelected(items[currentPosition], clickedRadioView)
            }
        }
    }
}
