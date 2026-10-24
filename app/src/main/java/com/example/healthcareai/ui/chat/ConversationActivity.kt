package com.example.healthcareai.ui.chat

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.healthcareai.data.repository.AiChatRepository
import com.example.healthcareai.databinding.ActivityConversationBinding
import com.example.healthcareai.ui.base.BaseActivity

/**
 * Screen displaying the user's past Conversation sessions with the AI Health Assistant.
 */
class ConversationActivity : BaseActivity() {

    private lateinit var binding: ActivityConversationBinding
    private lateinit var repository: AiChatRepository
    private lateinit var adapter: ConversationAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConversationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = AiChatRepository.getInstance(this)

        setupWindowInsets()
        setupListeners()
        setupRecyclerView()
    }

    override fun onResume() {
        super.onResume()
        loadConversations()
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.rootConversation) { _, insets ->
            val statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val navBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            val density = resources.displayMetrics.density

            // Position header comfortably below the status bar
            val targetTopMargin = if (statusBars.top > 0) {
                statusBars.top + (6 * density).toInt()
            } else {
                (20 * density).toInt()
            }
            binding.topBar.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = targetTopMargin
            }

            // Bottom navigation bar inset padding for RecyclerView and Empty layout
            val bottomPadding = if (navBars.bottom > 0) navBars.bottom + (16 * density).toInt() else (24 * density).toInt()
            binding.rvConversations.setPadding(0, (12 * density).toInt(), 0, bottomPadding)
            binding.layoutEmpty.setPadding(
                (32 * density).toInt(),
                0,
                (32 * density).toInt(),
                bottomPadding
            )

            insets
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        adapter = ConversationAdapter(
            onItemClick = { session ->
                val intent = Intent(this, AiChatActivity::class.java).apply {
                    putExtra(AiChatActivity.EXTRA_CONVERSATION_ID, session.id)
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                startActivity(intent)
                finish()
            },
            onItemDelete = { session ->
                repository.deleteConversation(session.id)
                adapter.removeConversation(session)
                if (adapter.itemCount == 0) {
                    binding.layoutEmpty.visibility = View.VISIBLE
                    binding.rvConversations.visibility = View.GONE
                }
            }
        )
        binding.rvConversations.apply {
            layoutManager = LinearLayoutManager(this@ConversationActivity)
            adapter = this@ConversationActivity.adapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                        this@ConversationActivity.adapter.closeOpenItem()
                    }
                }
            })
        }
    }

    private fun loadConversations() {
        val list = repository.getConversations()
        if (list.isEmpty()) {
            binding.layoutEmpty.visibility = View.VISIBLE
            binding.rvConversations.visibility = View.GONE
        } else {
            binding.layoutEmpty.visibility = View.GONE
            binding.rvConversations.visibility = View.VISIBLE
            adapter.setConversations(list)
        }
    }
}
