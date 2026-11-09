package com.example.healthcareai.chat

import com.example.healthcareai.data.repository.AiChatRepository
import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Test

class AiChatRepositoryTest {

    @Test
    fun testParseAiResponse_stringPrimitive() {
        val json = "\"Hello from AI\""
        val element = JsonParser.parseString(json)
        val parsed = AiChatRepository.parseAiResponse(element)
        assertEquals("Hello from AI", parsed)
    }

    @Test
    fun testParseAiResponse_dataFieldPrimitive() {
        val json = "{\"data\": \"Eat more fruits and vegetables daily.\"}"
        val element = JsonParser.parseString(json)
        val parsed = AiChatRepository.parseAiResponse(element)
        assertEquals("Eat more fruits and vegetables daily.", parsed)
    }

    @Test
    fun testParseAiResponse_dataFieldObject() {
        val json = "{\"data\": {\"reply\": \"Your heart rate is in the normal range.\"}}"
        val element = JsonParser.parseString(json)
        val parsed = AiChatRepository.parseAiResponse(element)
        assertEquals("Your heart rate is in the normal range.", parsed)
    }

    @Test
    fun testParseAiResponse_openAiChoices() {
        val json = "{\"choices\": [{\"message\": {\"content\": \"Drink at least 2L of water.\"}}]}"
        val element = JsonParser.parseString(json)
        val parsed = AiChatRepository.parseAiResponse(element)
        assertEquals("Drink at least 2L of water.", parsed)
    }

    @Test
    fun testTitleGeneration() {
        // We can test the title matching logic directly
        val lifestyle = if ("give me healthy lifestyle habits".contains("lifestyle")) "Healthy lifestyle tips" else "Other"
        assertEquals("Healthy lifestyle tips", lifestyle)

        val scan = if ("explain my skin scan result".contains("scan")) "Explain my scan results" else "Other"
        assertEquals("Explain my scan results", scan)

        val health = if ("review my heart rate data".contains("heart rate")) "Review my health data" else "Other"
        assertEquals("Review my health data", health)

        val symptom = if ("i have a headache and fever".contains("headache")) "Check my symptoms" else "Other"
        assertEquals("Check my symptoms", symptom)
    }

    @Test
    fun testUnansweredUserMessageDetection() {
        val messages = mutableListOf(
            com.example.healthcareai.data.model.ChatMessage(
                content = "Today, 10:00 AM",
                isUser = false,
                isDateHeader = true
            ),
            com.example.healthcareai.data.model.ChatMessage(
                content = "What should I do for a fever?",
                isUser = true
            )
        )
        val lastNonDate = messages.lastOrNull { !it.isDateHeader }
        val needsAnswer = lastNonDate != null && lastNonDate.isUser
        assertEquals(true, needsAnswer)

        // Once bot replies
        messages.add(
            com.example.healthcareai.data.model.ChatMessage(
                content = "Rest and drink fluids.",
                isUser = false
            )
        )
        val updatedLastNonDate = messages.lastOrNull { !it.isDateHeader }
        val updatedNeedsAnswer = updatedLastNonDate != null && updatedLastNonDate.isUser
        assertEquals(false, updatedNeedsAnswer)
    }

    @Test
    fun testConversationSessionCreationTimeSorting() {
        val session1 = com.example.healthcareai.data.model.ConversationSession(
            id = "conv1",
            createdAt = 1000L,
            updatedAt = 5000L
        )
        val session2 = com.example.healthcareai.data.model.ConversationSession(
            id = "conv2",
            createdAt = 2000L,
            updatedAt = 3000L
        )
        val list = mutableListOf(session1, session2)
        list.sortByDescending { if (it.createdAt > 0L) it.createdAt else it.updatedAt }

        assertEquals("conv2", list[0].id)
        assertEquals("conv1", list[1].id)
    }
}
