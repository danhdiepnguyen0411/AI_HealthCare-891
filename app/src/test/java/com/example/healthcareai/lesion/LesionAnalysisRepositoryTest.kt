package com.example.healthcareai.lesion

import com.example.healthcareai.data.model.KeyFinding
import com.example.healthcareai.data.model.LesionAnalysisResult
import com.example.healthcareai.data.repository.LesionAnalysisRepository
import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LesionAnalysisRepositoryTest {

    @Test
    fun testParse_directJson() {
        val json = """
            {
                "summary": "Mild benign pigmented nevus.",
                "key_findings": [
                    { "title": "Slight redness", "confidence_rate": "92%", "description": "Small localized erythema." }
                ],
                "care_suggestions": [
                    { "title": "Hydrate skin", "description": "Apply gentle moisturizer." }
                ],
                "when_to_seek_care": [
                    "If lesion bleeds"
                ]
            }
        """.trimIndent()

        val element = JsonParser.parseString(json)
        val result = LesionAnalysisRepository.parseLesionAnalysisResponse(element)

        assertNotNull(result)
        assertEquals("Mild benign pigmented nevus.", result?.summary)
        assertEquals(1, result?.keyFindings?.size)
        assertEquals("Slight redness", result?.keyFindings?.first()?.title)
        assertEquals(1, result?.careSuggestions?.size)
        assertEquals(1, result?.whenToSeekCare?.size)
    }

    @Test
    fun testParse_nestedDataWrapper() {
        val json = """
            {
                "status": 200,
                "data": {
                    "summary": "Localized skin rash observed.",
                    "keyFindings": [
                        { "name": "Patchy rash", "confidence": "88%", "desc": "Mild scaling." }
                    ]
                }
            }
        """.trimIndent()

        val element = JsonParser.parseString(json)
        val result = LesionAnalysisRepository.parseLesionAnalysisResponse(element)

        assertNotNull(result)
        assertEquals("Localized skin rash observed.", result?.summary)
        assertEquals("Patchy rash", result?.keyFindings?.first()?.title)
        assertEquals("88%", result?.keyFindings?.first()?.confidenceRate)
    }

    @Test
    fun testParse_markdownJsonString() {
        val markdownString = "```json\n{\"summary\": \"Eczema flair suspected.\", \"key_findings\": [{\"title\": \"Dryness\"}]}\n```"
        val json = "{\"data\": ${com.google.gson.Gson().toJson(markdownString)}}"

        val element = JsonParser.parseString(json)
        val result = LesionAnalysisRepository.parseLesionAnalysisResponse(element)

        assertNotNull(result)
        assertEquals("Eczema flair suspected.", result?.summary)
        assertEquals("Dryness", result?.keyFindings?.first()?.title)
    }

    @Test
    fun testIsIdentified_returnsFalseWhenExplicitlyNotIdentified() {
        val result = LesionAnalysisResult(
            isIdentified = false,
            summary = "Unable to identify condition from this image."
        )
        assertFalse(LesionAnalysisRepository.isIdentified(result))
    }

    @Test
    fun testIsIdentified_returnsFalseWhenEmpty() {
        val emptyResult = LesionAnalysisResult()
        assertFalse(LesionAnalysisRepository.isIdentified(emptyResult))
    }

    @Test
    fun testIsIdentified_returnsTrueWhenValid() {
        val validResult = LesionAnalysisResult(
            summary = "Benign skin spot",
            keyFindings = listOf(KeyFinding(title = "Normal borders"))
        )
        assertTrue(LesionAnalysisRepository.isIdentified(validResult))
    }
}

