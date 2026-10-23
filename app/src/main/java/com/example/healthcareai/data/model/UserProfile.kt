package com.example.healthcareai.data.model

data class UserProfile(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val avatarUri: String? = null,
    val dateOfBirth: String,
    val gender: String,
    val heightCm: Int,
    val weightKg: Int,
    val isPrimary: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
