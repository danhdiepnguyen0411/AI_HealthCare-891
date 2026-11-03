package com.example.healthcareai.data.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class LesionAnalysisResult(
    @SerializedName(value = "detection_box", alternate = ["detectionBox", "box", "bbox"])
    val detectionBox: DetectionBox? = null,

    @SerializedName(value = "summary", alternate = ["description", "analysis_summary", "overview"])
    val summary: String? = null,

    @SerializedName(value = "key_findings", alternate = ["keyFindings", "findings", "findings_list"])
    val keyFindings: List<KeyFinding>? = null,

    @SerializedName(value = "care_suggestions", alternate = ["careSuggestions", "suggestions", "recommendations"])
    val careSuggestions: List<CareSuggestion>? = null,

    @SerializedName(value = "when_to_seek_care", alternate = ["whenToSeekCare", "when_to_seek_medical_care", "seekCare", "seek_care", "warnings"])
    val whenToSeekCare: List<String>? = null,

    @SerializedName(value = "is_identified", alternate = ["isIdentified", "identified", "identified_success"])
    val isIdentified: Boolean? = null
) : Serializable

data class DetectionBox(
    @SerializedName(value = "x", alternate = ["left", "xmin"])
    val x: Float = 0f,

    @SerializedName(value = "y", alternate = ["top", "ymin"])
    val y: Float = 0f,

    @SerializedName(value = "width", alternate = ["w"])
    val width: Float = 0f,

    @SerializedName(value = "height", alternate = ["h"])
    val height: Float = 0f
) : Serializable {
    fun isValid(): Boolean = width > 0f && height > 0f
}

data class KeyFinding(
    @SerializedName(value = "title", alternate = ["name", "finding"])
    val title: String? = null,

    @SerializedName(value = "confidence_rate", alternate = ["confidenceRate", "confidence", "rate", "percentage"])
    val confidenceRate: String? = null,

    @SerializedName(value = "description", alternate = ["desc", "detail", "details"])
    val description: String? = null
) : Serializable

data class CareSuggestion(
    @SerializedName(value = "title", alternate = ["name", "suggestion", "header"])
    val title: String? = null,

    @SerializedName(value = "description", alternate = ["desc", "detail", "details"])
    val description: String? = null
) : Serializable
