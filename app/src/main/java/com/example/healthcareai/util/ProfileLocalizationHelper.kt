package com.example.healthcareai.util

import android.content.Context
import com.example.healthcareai.R

object ProfileLocalizationHelper {

    fun getLocalizedGender(context: Context, gender: String?): String {
        if (gender.isNullOrBlank()) return "--"
        val lower = gender.trim().lowercase()
        return when {
            lower == "male" || lower == "nam" || lower == "masculino" || lower == "homme" -> context.getString(R.string.gender_male)
            lower == "female" || lower == "nữ" || lower == "femenino" || lower == "femme" -> context.getString(R.string.gender_female)
            lower == "other" || lower == "khác" || lower == "otro" || lower == "autre" -> context.getString(R.string.gender_other)
            else -> gender
        }
    }

    fun getLocalizedRelationship(context: Context, relationship: String?): String {
        if (relationship.isNullOrBlank()) return ""
        val lower = relationship.trim().lowercase()
        return when (lower) {
            "father", "bố", "cha", "padre", "père" -> context.getString(R.string.rel_father)
            "mother", "mẹ", "madre", "mère" -> context.getString(R.string.rel_mother)
            "spouse", "vợ chồng", "cónyuge" -> context.getString(R.string.rel_spouse)
            "husband", "chồng", "esposo", "mari" -> context.getString(R.string.rel_husband)
            "wife", "vợ", "esposa", "femme" -> context.getString(R.string.rel_wife)
            "son", "con trai", "hijo", "fils" -> context.getString(R.string.rel_son)
            "daughter", "con gái", "hija", "fille" -> context.getString(R.string.rel_daughter)
            "brother", "anh trai", "em trai", "hermano", "frère" -> context.getString(R.string.rel_brother)
            "sister", "chị gái", "em gái", "hermana", "soeur" -> context.getString(R.string.rel_sister)
            "grandparent", "ông bà", "abuelos", "grand-parent" -> context.getString(R.string.rel_grandparent)
            "other", "khác", "otro", "autre" -> context.getString(R.string.rel_other)
            "you", "bạn", "tú", "vous", "creator", "owner", "primary" -> context.getString(R.string.label_relationship_you)
            else -> relationship
        }
    }

    fun formatGenderAndRelationship(context: Context, gender: String?, relationship: String?): String {
        val locGender = getLocalizedGender(context, gender)
        val locRel = getLocalizedRelationship(context, relationship)

        return when {
            locGender != "--" && locRel.isNotBlank() -> "$locGender ($locRel)"
            locGender != "--" -> locGender
            locRel.isNotBlank() -> "($locRel)"
            else -> "--"
        }
    }
}

