package com.example.healthcareai.data.model

import java.io.Serializable
import java.util.UUID

data class HealthRecordItem(
    val id: String = UUID.randomUUID().toString(),
    val profileId: String? = null,
    val category: String, // "Medication", "Allergies", "Conditions", "Vaccinations"
    val name: String,
    val details: String = "",
    val reason: String = "",
    val strength: String = "",
    val form: String = "",
    val type: String = "", // e.g. "Medication", "Food", "Other"
    val reaction: String = "",
    val severity: String = "",
    val firstNoticedDate: String = "",
    val status: String = "", // e.g. "Ongoing", "Resolved", "Unsure"
    val clinician: String = "",
    val clinic: String = "",
    val diagnosisDate: String = "",
    val doseNumber: String = "",
    val batchNumber: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
) : Serializable
