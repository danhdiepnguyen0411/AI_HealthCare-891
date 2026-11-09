package com.example.healthcareai.ui.camera

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.healthcareai.R
import com.example.healthcareai.data.model.ScanHistoryItem
import com.example.healthcareai.databinding.ItemScanHistoryCardBinding
import com.example.healthcareai.ui.heartrate.view.SwipeRevealLayout
import java.io.File

class AiScanHistoryAdapter(
    private val onItemClick: (ScanHistoryItem) -> Unit,
    private val onItemDelete: (ScanHistoryItem) -> Unit
) : RecyclerView.Adapter<AiScanHistoryAdapter.ScanViewHolder>() {

    private val items = mutableListOf<ScanHistoryItem>()
    private var currentlyOpenLayout: SwipeRevealLayout? = null

    fun submitList(newItems: List<ScanHistoryItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun removeItem(item: ScanHistoryItem) {
        val index = items.indexOfFirst { it.id == item.id }
        if (index != -1) {
            items.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    fun closeOpenItem(): Boolean {
        return currentlyOpenLayout?.let {
            if (it.isOpen) {
                it.close(true)
                true
            } else {
                false
            }
        } ?: false
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScanViewHolder {
        val binding = ItemScanHistoryCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ScanViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ScanViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ScanViewHolder(private val binding: ItemScanHistoryCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ScanHistoryItem) {
            val ctx = binding.root.context
            binding.tvScanTitle.text = com.example.healthcareai.util.LocaleHelper.formatScanTitle(ctx, item.title, item.category)
            binding.tvScanDate.text = if (item.timestamp > 0L) {
                com.example.healthcareai.util.LocaleHelper.formatDate(ctx, item.timestamp)
            } else {
                com.example.healthcareai.util.LocaleHelper.formatStoredDate(ctx, item.dateFormatted)
            }

            val imagePath = item.imagePath
            if (!imagePath.isNullOrEmpty() && File(imagePath).exists()) {
                binding.ivScanThumbnail.load(File(imagePath)) {
                    crossfade(true)
                    error(R.drawable.no_scans_yet)
                }
            } else {
                binding.ivScanThumbnail.setImageResource(R.drawable.no_scans_yet)
            }

            // Close swipe layout by default when recycled/rebound
            binding.swipeRevealLayout.close(false)

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

            // Clicking foreground card: if open, close it; otherwise open item detail
            val clickListener = android.view.View.OnClickListener {
                if (binding.swipeRevealLayout.isOpen) {
                    binding.swipeRevealLayout.close(true)
                } else {
                    onItemClick(item)
                }
            }
            binding.cardScanRoot.setOnClickListener(clickListener)
            binding.swipeRevealLayout.setOnClickListener(clickListener)

            // Clicking delete action button on revealed red background
            binding.btnDeleteAction.setOnClickListener {
                binding.swipeRevealLayout.close(false)
                onItemDelete(item)
            }
        }
    }
}
