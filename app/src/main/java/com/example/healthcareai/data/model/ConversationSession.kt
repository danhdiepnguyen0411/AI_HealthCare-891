package com.example.healthcareai.data.model

import java.util.UUID

/**
 * Model representing an archived or active chat conversation session with the AI assistant.
 */
data class ConversationSession(
    val id: String = UUID.randomUUID().toString(),
    var title: String = "",
    var lastSnippet: String = "",
    var updatedAt: Long = System.currentTimeMillis(),
    val messages: MutableList<ChatMessage> = mutableListOf()
)
