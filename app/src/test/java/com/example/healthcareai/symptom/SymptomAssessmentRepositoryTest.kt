package com.example.healthcareai.symptom

import com.example.healthcareai.data.repository.SymptomAssessmentRepository
import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SymptomAssessmentRepositoryTest {

    @Test
    fun testParseDirectJson() {
        val json = """
            {
              "triage_level": "Consult a doctor",
              "triage_description": "Your symptoms may require medical evaluation.",
              "reported_symptoms_summary": "mild fever, fatigue",
              "possible_conditions": [
                {
                  "name": "Contact dermatitis",
                  "match_label": "Strong Match",
                  "match_progress": 80,
                  "recommended_actions": [
                    "Avoid known allergens",
                    "Consider over-the-counter antihistamines"
                  ]
                },
                {
                  "name": "Allergic reactions to insect bite or sting",
                  "match_label": "Moderate Match",
                  "match_progress": 45,
                  "recommended_actions": [
                    "Rest and hydrate frequently",
                    "Monitor temperature for 24 hours"
                  ]
                }
              ],
              "care_advice": {
                "self_care": [
                  "Ensure you get at least 8 hours of sleep to help your body recover.",
                  "Drink plenty of water or clear fluids to stay hydrated."
                ],
                "monitor_symptoms": [
                  "Check your temperature twice a day.",
                  "Keep an eye on your resting heart rate using your wearable."
                ],
                "when_to_seek_care": [
                  "If your fever exceeds 39.4°C, pain persists for more than 48 hours, or you experience difficulty breathing, seek medical attention immediately."
                ]
              }
            }
        """.trimIndent()

        val jsonElement = JsonParser.parseString(json)
        val result = SymptomAssessmentRepository.parseSymptomAssessmentResponse(jsonElement)

        assertNotNull(result)
        assertEquals("Consult a doctor", result?.triageLevel)
        assertEquals(2, result?.possibleConditions?.size)
        assertEquals("Contact dermatitis", result?.possibleConditions?.get(0)?.name)
        assertEquals(80, result?.possibleConditions?.get(0)?.matchProgress)
        assertEquals(2, result?.careAdvice?.selfCare?.size)
        assertEquals(2, result?.careAdvice?.monitorSymptoms?.size)
        assertEquals(1, result?.careAdvice?.whenToSeekCare?.size)
    }

    @Test
    fun testParseArtMagicWrappedResponse() {
        val wrappedJson = """
            {
              "status": {
                "code": "200",
                "message": "Thành công",
                "label": "API_STATUS_SUCCESS",
                "requestId": "abc123xyz"
              },
              "data": {
                "version": null,
                "data": {
                  "triage_level": "Consult a doctor",
                  "triage_description": "Schedule an appointment with your doctor.",
                  "reported_symptoms_summary": "Skin redness, Itching",
                  "possible_conditions": [
                    {
                      "name": "Contact dermatitis",
                      "match_label": "Strong Match",
                      "match_progress": 85,
                      "recommended_actions": [
                        "Avoid irritants"
                      ]
                    }
                  ],
                  "care_advice": {
                    "self_care": ["Wash with cool water"],
                    "monitor_symptoms": ["Check temperature"],
                    "when_to_seek_care": ["High fever"]
                  }
                }
              }
            }
        """.trimIndent()

        val jsonElement = JsonParser.parseString(wrappedJson)
        val result = SymptomAssessmentRepository.parseSymptomAssessmentResponse(jsonElement)

        assertNotNull(result)
        assertEquals("Consult a doctor", result?.triageLevel)
        assertEquals("Contact dermatitis", result?.possibleConditions?.get(0)?.name)
        assertEquals(85, result?.possibleConditions?.get(0)?.matchProgress)
        assertEquals("Wash with cool water", result?.careAdvice?.selfCare?.get(0))
    }

    @Test
    fun testParseMarkdownWrappedJson() {
        val markdown = """
            ```json
            {
              "triage_level": "Self-Care",
              "triage_description": "Manage symptoms with home care.",
              "possible_conditions": [
                {
                  "name": "Mild dermatitis",
                  "match_label": "Moderate Match",
                  "match_progress": 50,
                  "recommended_actions": ["Moisturize skin"]
                }
              ]
            }
            ```
        """.trimIndent()

        val parsed = SymptomAssessmentRepository.tryParseResultString(markdown)
        assertNotNull(parsed)
        assertEquals("Self-Care", parsed?.triageLevel)
        assertEquals("Mild dermatitis", parsed?.possibleConditions?.get(0)?.name)
    }

    @Test
    fun testParseRealArtMagicResponseWithTriageObject() {
        val realJson = """
            {
              "status": {
                "code": "200",
                "message": "Thành công",
                "label": "API_STATUS_SUCCESS",
                "requestId": null
              },
              "data": {
                "version": null,
                "data": {
                  "triage": {
                    "title": "Lower Limb Pain Assessment",
                    "description": "Informational insights on sharp leg pain."
                  },
                  "possible_conditions": [
                    {
                      "name": "Patellofemoral Pain Syndrome",
                      "match_label": "Moderate Match",
                      "match_percentage": 65,
                      "recommended_actions": [
                        "Rest and activity modification"
                      ]
                    }
                  ],
                  "care_advice": {
                    "summary": "General tips for leg pain.",
                    "self_care": [
                      "Rest the affected leg."
                    ],
                    "monitor_symptoms": [
                      "Observe for increased swelling."
                    ],
                    "when_to_seek_care": [
                      "If pain is severe and prevents normal walking."
                    ]
                  }
                }
              }
            }
        """.trimIndent()

        val jsonElement = JsonParser.parseString(realJson)
        val result = SymptomAssessmentRepository.parseSymptomAssessmentResponse(jsonElement)

        assertNotNull(result)
        assertEquals("Lower Limb Pain Assessment", result?.triageLevel)
        assertEquals("Patellofemoral Pain Syndrome", result?.possibleConditions?.get(0)?.name)
        assertEquals(65, result?.possibleConditions?.get(0)?.matchProgress)
        assertEquals(1, result?.careAdvice?.whenToSeekCare?.size)
        assertEquals("If pain is severe and prevents normal walking.", result?.careAdvice?.whenToSeekCare?.get(0))
    }
}

