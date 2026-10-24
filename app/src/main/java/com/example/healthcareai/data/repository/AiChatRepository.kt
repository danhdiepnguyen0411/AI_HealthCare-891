package com.example.healthcareai.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.healthcareai.HealthCareApp
import com.example.healthcareai.data.model.ChatMessage
import com.example.healthcareai.data.model.ConversationSession
import com.example.healthcareai.util.LocaleHelper
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * Repository to persist AI chat messages, manage Conversation sessions,
 * and interact with ArtMagicClient (agentId: 402).
 */
class AiChatRepository private constructor(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val TAG = "AiChatRepository"
        private const val PREF_NAME = "ai_chat_preferences"
        private const val KEY_CONVERSATIONS = "key_conversations_list"
        const val AGENT_ID = "402"

        @Volatile
        private var instance: AiChatRepository? = null

        fun getInstance(context: Context): AiChatRepository {
            return instance ?: synchronized(this) {
                val safeContext = context.applicationContext ?: context
                instance ?: AiChatRepository(safeContext).also { instance = it }
            }
        }

        /**
         * Helper to reliably parse the AI response text from any supported JsonElement shape.
         */
        fun parseAiResponse(response: JsonElement?): String {
            if (response == null || response.isJsonNull) return ""

            if (response.isJsonPrimitive) {
                return response.asString.trim()
            }

            if (response.isJsonObject) {
                val obj = response.asJsonObject

                // 1. Check data field (can be primitive or object)
                if (obj.has("data")) {
                    val dataElem = obj.get("data")
                    if (dataElem.isJsonPrimitive) {
                        return dataElem.asString.trim()
                    }
                    if (dataElem.isJsonObject) {
                        val dataObj = dataElem.asJsonObject
                        for (key in listOf("content", "message", "reply", "text", "answer", "result")) {
                            if (dataObj.has(key) && !dataObj.get(key).isJsonNull) {
                                val v = dataObj.get(key)
                                if (v.isJsonPrimitive) return v.asString.trim()
                            }
                        }
                    }
                }

                // 2. Check direct fields on the root object
                for (key in listOf("content", "message", "reply", "text", "answer", "result")) {
                    if (obj.has(key) && !obj.get(key).isJsonNull) {
                        val v = obj.get(key)
                        if (v.isJsonPrimitive) return v.asString.trim()
                    }
                }

                // 3. Fallback: OpenAI-style choices array
                if (obj.has("choices") && obj.get("choices").isJsonArray) {
                    val choices = obj.getAsJsonArray("choices")
                    if (choices.size() > 0 && choices[0].isJsonObject) {
                        val choiceObj = choices[0].asJsonObject
                        if (choiceObj.has("message") && choiceObj.get("message").isJsonObject) {
                            val msgObj = choiceObj.getAsJsonObject("message")
                            if (msgObj.has("content")) return msgObj.get("content").asString.trim()
                        } else if (choiceObj.has("text")) {
                            return choiceObj.get("text").asString.trim()
                        }
                    }
                }

                // 4. If all else fails, return raw string if not empty
                val raw = response.toString()
                return if (raw != "{}" && raw != "null") raw else ""
            }

            return response.toString()
        }
    }

    // =========================================================================
    // CONVERSATION SESSIONS MANAGEMENT
    // =========================================================================

    /**
     * Retrieves all saved conversation sessions sorted by most recent first.
     */
    fun getConversations(): MutableList<ConversationSession> {
        val json = prefs.getString(KEY_CONVERSATIONS, null) ?: return mutableListOf()
        return try {
            val type = object : TypeToken<MutableList<ConversationSession>>() {}.type
            val list = gson.fromJson<MutableList<ConversationSession>>(json, type) ?: mutableListOf()
            list.sortByDescending { it.updatedAt }
            list
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse saved conversation sessions", e)
            mutableListOf()
        }
    }

    /**
     * Retrieves a single conversation session by its ID.
     */
    fun getConversationById(id: String): ConversationSession? {
        val list = getConversations()
        return list.firstOrNull { it.id == id }
    }

    /**
     * Saves or updates a conversation session in the persistent list.
     */
    fun saveOrUpdateConversation(session: ConversationSession) {
        try {
            // Only save if session has at least one message from the user
            if (session.messages.none { it.isUser }) return

            val list = getConversations()
            val existingIndex = list.indexOfFirst { it.id == session.id }

            // Filter out temporary loading messages before persisting
            val cleanMessages = session.messages.filter { !it.isLoading }.toMutableList()
            session.messages.clear()
            session.messages.addAll(cleanMessages)

            if (session.title.isBlank()) {
                val firstUserMsg = cleanMessages.firstOrNull { it.isUser }?.content.orEmpty()
                session.title = generateTitleFromMessage(firstUserMsg)
            }

            if (session.lastSnippet.isBlank()) {
                val lastMsg = cleanMessages.lastOrNull { !it.isDateHeader }?.content.orEmpty()
                session.lastSnippet = lastMsg.take(80)
            }

            session.updatedAt = System.currentTimeMillis()

            if (existingIndex != -1) {
                list[existingIndex] = session
            } else {
                list.add(0, session)
            }

            val json = gson.toJson(list)
            prefs.edit().putString(KEY_CONVERSATIONS, json).apply()
            Log.d(TAG, "Saved conversation: ${session.id} ('${session.title}') with ${session.messages.size} msgs")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save conversation session", e)
        }
    }

    /**
     * Deletes a conversation by its ID.
     */
    fun deleteConversation(id: String) {
        try {
            val list = getConversations()
            val removed = list.removeAll { it.id == id }
            if (removed) {
                val json = gson.toJson(list)
                prefs.edit().putString(KEY_CONVERSATIONS, json).apply()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete conversation", e)
        }
    }

    /**
     * Helper to generate a friendly concise title from the user's initial prompt.
     */
    fun generateTitleFromMessage(userMessage: String): String {
        val lower = userMessage.lowercase()
        return when {
            lower.contains("lifestyle") || lower.contains("habit") || lower.contains("healthy") ->
                "Healthy lifestyle tips"
            lower.contains("scan") || lower.contains("skin") || lower.contains("dental") ->
                "Explain my scan results"
            lower.contains("health data") || lower.contains("heart rate") || lower.contains("activity") ->
                "Review my health data"
            lower.contains("symptom") || lower.contains("headache") || lower.contains("fever") ->
                "Check my symptoms"
            else -> {
                val trimmed = userMessage.trim().replace("\n", " ")
                if (trimmed.length > 32) trimmed.take(30).trimEnd() + "..." else trimmed.replaceFirstChar { it.uppercase() }
            }
        }
    }

    // =========================================================================
    // API CALL VIA ARTMAGIC SDK (agentId: 402)
    // =========================================================================

    /**
     * Sends a message to the AI Assistant via ArtMagicClient using agentId = 402.
     * Returns the parsed response text or throws on failure.
     */
    suspend fun sendMessage(
        content: String,
        langCode: String = LocaleHelper.getSavedLanguageCode(context)
    ): Result<String> = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { continuation ->
            Log.d(TAG, "========== SENDING MESSAGE TO AI ASSISTANT (agentId: $AGENT_ID, lang: $langCode) ==========")
            Log.d(TAG, "Content: $content")

            try {
                HealthCareApp.artMagicClient.sendMessage(
                    content = content,
                    agentId = AGENT_ID,
                    lang = langCode
                ) { response, error ->
                    if (error != null) {
                        Log.e(TAG, "API_ERROR from ArtMagicClient: ${error.message}", error)
                        if (continuation.isActive) {
                            continuation.resume(Result.failure(error))
                        }
                    } else {
                        Log.d(TAG, "Raw response from ArtMagicClient: $response")
                        val replyText = parseAiResponse(response)
                        if (continuation.isActive) {
                            continuation.resume(Result.success(replyText))
                        }
                    }
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Exception calling ArtMagicClient.sendMessage", e)
                if (continuation.isActive) {
                    continuation.resume(Result.failure(e))
                }
            }
        }
    }
}
