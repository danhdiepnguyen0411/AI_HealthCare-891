package com.example.healthcareai.data.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class SymptomAssessmentResult(
    @SerializedName("triage_level", alternate = ["triageLevel", "triage", "recommendation", "urgency", "level"])
    val triageLevel: String = "Consult a doctor",

    @SerializedName("triage_description", alternate = ["triageDescription", "triage_message", "triageMessage", "description", "message", "summary"])
    val triageDescription: String = "Your symptoms may require medical evaluation. Schedule an appointment with your doctor. If your symptoms get worse, see a doctor immediately.",

    @SerializedName("reported_symptoms_summary", alternate = ["reportedSymptomsSummary", "symptoms_summary", "symptomsSummary"])
    val reportedSymptomsSummary: String = "",

    @SerializedName("possible_conditions", alternate = ["possibleConditions", "conditions", "diagnoses", "results"])
    val possibleConditions: List<SymptomCondition> = emptyList(),

    @SerializedName("care_advice", alternate = ["careAdvice", "advice"])
    val careAdvice: SymptomCareAdvice = SymptomCareAdvice()
) : Serializable

data class SymptomCondition(
    @SerializedName("name", alternate = ["title", "condition", "condition_name", "conditionName"])
    val name: String = "",

    @SerializedName("match_label", alternate = ["matchLabel", "match_level", "matchLevel", "likelihood", "probability"])
    val matchLabel: String = "Strong Match",

    @SerializedName("match_progress", alternate = ["matchProgress", "match_percentage", "matchPercentage", "progress", "confidence"])
    val matchProgress: Int = 80,

    @SerializedName("recommended_actions", alternate = ["recommendedActions", "actions", "recommendations"])
    val recommendedActions: List<String> = emptyList(),

    @SerializedName("care_advice", alternate = ["careAdvice", "advice"])
    val careAdvice: SymptomCareAdvice? = null
) : Serializable

data class SymptomCareAdvice(
    @SerializedName("self_care", alternate = ["selfCare", "self_care_steps", "selfCareSteps", "home_care", "homeCare"])
    val selfCare: List<String> = emptyList(),

    @SerializedName("monitor_symptoms", alternate = ["monitorSymptoms", "monitoring", "symptoms_to_monitor", "symptomsToMonitor"])
    val monitorSymptoms: List<String> = emptyList(),

    @SerializedName("when_to_seek_care", alternate = ["whenToSeekCare", "seek_medical_care", "seekMedicalCare", "when_to_seek_medical_care", "red_flags", "redFlags", "emergency_signs", "emergencySigns", "warning_signs"])
    val whenToSeekCare: List<String> = emptyList()
) : Serializable

