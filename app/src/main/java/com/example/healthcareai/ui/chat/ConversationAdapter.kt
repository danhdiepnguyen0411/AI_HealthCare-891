package com.example.healthcareai.ui.chat

import android.content.Context
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.healthcareai.R
import com.example.healthcareai.data.model.ConversationSession

import com.example.healthcareai.ui.heartrate.view.SwipeRevealLayout

/**
 * Adapter for displaying saved Conversation sessions with swipe-to-reveal delete.
 */
class ConversationAdapter(
    private val conversations: MutableList<ConversationSession> = mutableListOf(),
    private val onItemClick: (ConversationSession) -> Unit,
    private val onItemDelete: (ConversationSession) -> Unit
) : RecyclerView.Adapter<ConversationAdapter.ConversationViewHolder>() {

    private var currentlyOpenLayout: SwipeRevealLayout? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_conversation, parent, false)
        return ConversationViewHolder(view)
    }

    override fun onBindViewHolder(holder: ConversationViewHolder, position: Int) {
        holder.bind(conversations[position])
    }

    override fun getItemCount(): Int = conversations.size

    fun setConversations(newList: List<ConversationSession>) {
        conversations.clear()
        conversations.addAll(newList)
        notifyDataSetChanged()
    }

    fun removeConversation(session: ConversationSession) {
        val index = conversations.indexOfFirst { it.id == session.id }
        if (index != -1) {
            conversations.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    fun closeOpenItem() {
        currentlyOpenLayout?.close(true)
    }

    inner class ConversationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val swipeRevealLayout: SwipeRevealLayout = itemView.findViewById(R.id.swipeRevealLayout)
        private val cardForeground: View = itemView.findViewById(R.id.cardForeground)
        private val btnDeleteAction: View = itemView.findViewById(R.id.btnDeleteAction)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvConversationTitle)
        private val tvSnippet: TextView = itemView.findViewById(R.id.tvConversationSnippet)
        private val tvTime: TextView = itemView.findViewById(R.id.tvConversationTime)

        fun bind(session: ConversationSession) {
            tvTitle.text = session.title.ifBlank {
                itemView.context.getString(R.string.title_conversation)
            }
            tvSnippet.text = session.lastSnippet.ifBlank {
                "..."
            }
            tvTime.text = formatRelativeTime(itemView.context, session.updatedAt)

            // Reset layout position when recycled
            swipeRevealLayout.close(false)

            swipeRevealLayout.onSwipeListener = object : SwipeRevealLayout.OnSwipeListener {
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

            cardForeground.setOnClickListener {
                if (swipeRevealLayout.isOpen) {
                    swipeRevealLayout.close(true)
                } else {
                    onItemClick(session)
                }
            }

            btnDeleteAction.setOnClickListener {
                swipeRevealLayout.close(false)
                onItemDelete(session)
            }
        }

        private fun formatRelativeTime(context: Context, timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diffMs = now - timestamp

            val diffMins = diffMs / (60 * 1000)
            val diffHours = diffMs / (60 * 60 * 1000)
            val diffDays = diffMs / (24 * 60 * 60 * 1000)

            return when {
                diffMins < 1 -> context.getString(R.string.time_just_now)
                diffMins == 1L -> context.getString(R.string.time_minute_ago)
                diffMins < 60 -> context.getString(R.string.time_minutes_ago, diffMins)
                diffHours == 1L -> context.getString(R.string.time_hour_ago)
                diffHours < 24 -> context.getString(R.string.time_hours_ago, diffHours)
                diffDays == 1L -> context.getString(R.string.time_day_ago)
                diffDays < 7 -> context.getString(R.string.time_days_ago, diffDays)
                else -> DateUtils.getRelativeTimeSpanString(timestamp, now, DateUtils.DAY_IN_MILLIS).toString()
            }
        }
    }
}
