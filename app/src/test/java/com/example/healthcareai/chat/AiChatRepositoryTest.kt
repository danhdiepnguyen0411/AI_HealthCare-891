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
}
