package com.example.healthcareai.ui.heartrate

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.healthcareai.R
import com.example.healthcareai.data.repository.HeartRateRecord
import com.example.healthcareai.databinding.ItemHeartRateHistoryBinding
import com.example.healthcareai.ui.heartrate.view.SwipeRevealLayout
import com.example.healthcareai.util.LocaleHelper
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HeartRateHistoryAdapter(
    private val onItemDelete: (HeartRateRecord) -> Unit
) : ListAdapter<HeartRateRecord, HeartRateHistoryAdapter.HistoryViewHolder>(DiffCallback) {

    private var currentlyOpenLayout: SwipeRevealLayout? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemHeartRateHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return HistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun closeOpenItem(animate: Boolean = true) {
        currentlyOpenLayout?.close(animate)
        currentlyOpenLayout = null
    }

    fun closeAll(recyclerView: RecyclerView? = null) {
        closeOpenItem(false)
        recyclerView?.let { rv ->
            for (i in 0 until rv.childCount) {
                val child = rv.getChildAt(i)
                if (child is SwipeRevealLayout) {
                    child.close(false)
                } else {
                    child.findViewById<SwipeRevealLayout>(R.id.swipeRevealLayout)?.close(false)
                }
            }
        }
    }

    inner class HistoryViewHolder(
        private val binding: ItemHeartRateHistoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(record: HeartRateRecord) {
            val context = itemView.context
            val locale = LocaleHelper.getSavedLocale(context)

            // Close by default on rebind
            binding.swipeRevealLayout.close(false)

            binding.tvHistoryBpm.text = record.bpm.toString()
            val dateStr = DateFormat.getDateInstance(DateFormat.LONG, locale).format(Date(record.timestamp))
            val timeStr = LocaleHelper.formatTime(context, record.timestamp, "hh:mm a")
            binding.tvHistoryDateTime.text = "$dateStr · $timeStr"

            when {
                record.bpm < 60 -> {
                    binding.tvHistoryStatusBadge.text = context.getString(R.string.heart_rate_slow_label)
                    binding.tvHistoryStatusBadge.background = ContextCompat.getDrawable(context, R.drawable.bg_badge_stat_slow)
                }
                record.bpm > 100 -> {
                    binding.tvHistoryStatusBadge.text = context.getString(R.string.heart_rate_fast_label)
                    binding.tvHistoryStatusBadge.background = ContextCompat.getDrawable(context, R.drawable.bg_badge_stat_fast)
                }
                else -> {
                    binding.tvHistoryStatusBadge.text = context.getString(R.string.heart_rate_normal_label)
                    binding.tvHistoryStatusBadge.background = ContextCompat.getDrawable(context, R.drawable.bg_badge_stat_normal)
                }
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

            // Clicking foreground card: if open, close it; does NOT navigate to detail result
            binding.cardForeground.setOnClickListener {
                if (binding.swipeRevealLayout.isOpen) {
                    binding.swipeRevealLayout.close(true)
                }
            }

            // Clicking delete action button on the revealed red area (#FF4D4F, 90dp)
            binding.btnDeleteAction.setOnClickListener {
                binding.swipeRevealLayout.close(false)
                onItemDelete(record)
            }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<HeartRateRecord>() {
            override fun areItemsTheSame(oldItem: HeartRateRecord, newItem: HeartRateRecord): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: HeartRateRecord, newItem: HeartRateRecord): Boolean {
                return oldItem == newItem
            }
        }
    }
}
