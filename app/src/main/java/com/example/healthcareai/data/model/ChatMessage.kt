package com.example.healthcareai.data.model

import java.util.UUID

/**
 * Model representing a message in the AI Health Assistant chat conversation.
 */
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    var isLoading: Boolean = false,
    val isDateHeader: Boolean = false,
    val formattedDate: String? = null
)
