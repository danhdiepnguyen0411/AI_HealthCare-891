package com.example.healthcareai.data.model

import androidx.annotation.DrawableRes

data class LanguageModel(
    val code: String,
    val name: String,
    val nativeName: String,
    @param:DrawableRes val flagRes: Int,
    var isSelected: Boolean = false,
    val isDeviceLanguage: Boolean = false,
    var showTapHint: Boolean = false
)
