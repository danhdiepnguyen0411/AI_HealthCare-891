package com.example.healthcareai.ui.chat

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.healthcareai.R
import com.example.healthcareai.data.model.ChatMessage

/**
 * Adapter for rendering user and AI bot messages, typing indicators, and date chips.
 */
class ChatMessageAdapter(
    private val messages: MutableList<ChatMessage> = mutableListOf()
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_DATE = 0
        private const val VIEW_TYPE_USER = 1
        private const val VIEW_TYPE_BOT = 2
        private const val VIEW_TYPE_BOT_TYPING = 3
    }

    override fun getItemViewType(position: Int): Int {
        val item = messages[position]
        return when {
            item.isDateHeader -> VIEW_TYPE_DATE
            item.isUser -> VIEW_TYPE_USER
            item.isLoading -> VIEW_TYPE_BOT_TYPING
            else -> VIEW_TYPE_BOT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_DATE -> {
                val view = inflater.inflate(R.layout.item_chat_date, parent, false)
                DateViewHolder(view)
            }
            VIEW_TYPE_USER -> {
                val view = inflater.inflate(R.layout.item_chat_user, parent, false)
                UserViewHolder(view)
            }
            VIEW_TYPE_BOT_TYPING -> {
                val view = inflater.inflate(R.layout.item_chat_bot_typing, parent, false)
                BotTypingViewHolder(view)
            }
            else -> {
                val view = inflater.inflate(R.layout.item_chat_bot, parent, false)
                BotViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        when (holder) {
            is DateViewHolder -> holder.bind(message)
            is UserViewHolder -> holder.bind(message)
            is BotTypingViewHolder -> holder.bind()
            is BotViewHolder -> holder.bind(message)
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        if (holder is BotTypingViewHolder) {
            holder.stopTypingAnimation()
        }
    }

    override fun getItemCount(): Int = messages.size

    fun setMessages(newMessages: List<ChatMessage>) {
        messages.clear()
        messages.addAll(newMessages)
        notifyDataSetChanged()
    }

    fun addMessage(message: ChatMessage) {
        messages.add(message)
        notifyItemInserted(messages.size - 1)
    }

    fun removeLoadingMessage() {
        val index = messages.indexOfLast { it.isLoading }
        if (index != -1) {
            messages.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    fun getItems(): List<ChatMessage> = messages

    class DateViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDate: TextView = itemView.findViewById(R.id.tvDateHeader)

        fun bind(message: ChatMessage) {
            tvDate.text = message.formattedDate ?: message.content
        }
    }

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvUserMessage: TextView = itemView.findViewById(R.id.tvUserMessage)

        fun bind(message: ChatMessage) {
            tvUserMessage.text = message.content
        }
    }

    class BotTypingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dot1: View = itemView.findViewById(R.id.dot1)
        private val dot2: View = itemView.findViewById(R.id.dot2)
        private val dot3: View = itemView.findViewById(R.id.dot3)
        private var animators: List<ObjectAnimator>? = null

        fun bind() {
            startTypingAnimation()
        }

        fun startTypingAnimation() {
            stopTypingAnimation()

            val anim1 = ObjectAnimator.ofFloat(dot1, "translationY", 0f, -5f, 0f).apply {
                duration = 600
                repeatCount = ValueAnimator.INFINITE
                interpolator = AccelerateDecelerateInterpolator()
                startDelay = 0
            }
            val anim2 = ObjectAnimator.ofFloat(dot2, "translationY", 0f, -5f, 0f).apply {
                duration = 600
                repeatCount = ValueAnimator.INFINITE
                interpolator = AccelerateDecelerateInterpolator()
                startDelay = 150
            }
            val anim3 = ObjectAnimator.ofFloat(dot3, "translationY", 0f, -5f, 0f).apply {
                duration = 600
                repeatCount = ValueAnimator.INFINITE
                interpolator = AccelerateDecelerateInterpolator()
                startDelay = 300
            }

            animators = listOf(anim1, anim2, anim3)
            anim1.start()
            anim2.start()
            anim3.start()
        }

        fun stopTypingAnimation() {
            animators?.forEach { it.cancel() }
            animators = null
            dot1.translationY = 0f
            dot2.translationY = 0f
            dot3.translationY = 0f
        }
    }

    class BotViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvBotMessage: TextView = itemView.findViewById(R.id.tvBotMessage)

        fun bind(message: ChatMessage) {
            tvBotMessage.text = formatMarkdown(message.content)
        }

        private fun formatMarkdown(text: String): CharSequence {
            if (text.isBlank()) return ""
            return try {
                val formatted = text
                    .replace(Regex("\\*\\*(.*?)\\*\\*"), "<b>$1</b>")
                    .replace("\n", "<br/>")
                androidx.core.text.HtmlCompat.fromHtml(
                    formatted,
                    androidx.core.text.HtmlCompat.FROM_HTML_MODE_COMPACT
                )
            } catch (_: Exception) {
                text
            }
        }
    }
}
