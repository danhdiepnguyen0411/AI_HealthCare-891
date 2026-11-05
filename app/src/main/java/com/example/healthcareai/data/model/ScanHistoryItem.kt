package com.example.healthcareai.data.model

import java.io.Serializable
import java.util.UUID

data class ScanHistoryItem(
    val id: String = UUID.randomUUID().toString(),
    val profileId: String,
    val category: String, // "SKIN", "EYE", "NAIL", "HAIR", "ORAL", etc.
    val title: String,
    val summary: String = "",
    val imagePath: String? = null,
    val dateFormatted: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val analysisResult: LesionAnalysisResult? = null,
    val conversationId: String? = null
) : Serializable

