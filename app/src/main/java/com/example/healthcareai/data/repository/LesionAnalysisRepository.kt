package com.example.healthcareai.data.repository

import android.content.Context
import android.util.Log
import com.example.healthcareai.HealthCareApp
import com.example.healthcareai.data.model.LesionAnalysisResult
import com.example.healthcareai.util.LocaleHelper
import com.google.gson.Gson
import com.google.gson.JsonElement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import kotlin.coroutines.resume

class LesionAnalysisRepository private constructor(private val context: Context) {

    companion object {
        private const val TAG = "LesionAnalysisRepo"
        const val AGENT_CODE = "medical_visual_lesion_analysis"

        private val gson = Gson()

        @Volatile
        private var instance: LesionAnalysisRepository? = null

        fun getInstance(context: Context): LesionAnalysisRepository {
            return instance ?: synchronized(this) {
                val safeContext = context.applicationContext ?: context
                instance ?: LesionAnalysisRepository(safeContext).also { instance = it }
            }
        }

        /**
         * Strips markdown fences (e.g. ```json ... ```) from strings.
         */
        fun cleanMarkdownJson(raw: String): String {
            var str = raw.trim()
            if (str.startsWith("```")) {
                str = str.replace(Regex("^```(?:json)?\\s*", RegexOption.IGNORE_CASE), "")
                str = str.replace(Regex("\\s*```$"), "")
            }
            return str.trim()
        }

        /**
         * Helper to verify if the parsed result has substantive lesion findings or summary,
         * and was not marked as unidentified.
         */
        fun isIdentified(result: LesionAnalysisResult?): Boolean {
            if (result == null) return false
            if (result.isIdentified == false) return false

            val summaryLower = result.summary?.lowercase(Locale.ROOT) ?: ""
            if (summaryLower.contains("unable to identify") ||
                summaryLower.contains("cannot identify") ||
                summaryLower.contains("could not identify") ||
                summaryLower.contains("không thể nhận diện") ||
                summaryLower.contains("không nhận diện được")) {
                return false
            }

            val hasFindings = !result.keyFindings.isNullOrEmpty() && result.keyFindings.any { !it.title.isNullOrBlank() }
            val hasSummary = !result.summary.isNullOrBlank()
            return hasFindings || hasSummary
        }

        private fun tryParseResultString(jsonStr: String): LesionAnalysisResult? {
            val cleaned = cleanMarkdownJson(jsonStr)
            if (cleaned.isBlank()) return null
            return try {
                gson.fromJson(cleaned, LesionAnalysisResult::class.java)
            } catch (_: Exception) {
                null
            }
        }

        /**
         * Parses ArtMagicClient JsonElement response into LesionAnalysisResult.
         * Handles direct JSON, wrapped `data`, `result`, `content`, strings, and markdown blocks.
         */
        fun parseLesionAnalysisResponse(response: JsonElement?): LesionAnalysisResult? {
            if (response == null || response.isJsonNull) return null

            val resString = response.toString()

            // 1. Direct parse attempt
            val direct = tryParseResultString(resString)
            if (isIdentified(direct)) {
                return direct
            }

            // 2. If it's a JsonPrimitive (raw string)
            if (response.isJsonPrimitive && response.asJsonPrimitive.isString) {
                val fromPrimitive = tryParseResultString(response.asString)
                if (isIdentified(fromPrimitive)) {
                    return fromPrimitive
                }
            }

            // 3. If JsonObject, explore keys: "data", "result", "content", "output", "response"
            if (response.isJsonObject) {
                val obj = response.asJsonObject

                for (key in listOf("data", "result", "output", "content", "response")) {
                    if (obj.has(key) && !obj.get(key).isJsonNull) {
                        val elem = obj.get(key)
                        if (elem.isJsonObject) {
                            val subObj = elem.asJsonObject
                            val parsedSub = tryParseResultString(subObj.toString())
                            if (isIdentified(parsedSub)) return parsedSub

                            // Check nested subObj["data"] or subObj["result"]
                            for (subKey in listOf("data", "result", "output", "content")) {
                                if (subObj.has(subKey) && !subObj.get(subKey).isJsonNull) {
                                    val subElem = subObj.get(subKey)
                                    if (subElem.isJsonObject) {
                                        val parsedNested = tryParseResultString(subElem.toString())
                                        if (isIdentified(parsedNested)) return parsedNested
                                    } else if (subElem.isJsonPrimitive && subElem.asJsonPrimitive.isString) {
                                        val parsedNested = tryParseResultString(subElem.asString)
                                        if (isIdentified(parsedNested)) return parsedNested
                                    }
                                }
                            }
                        } else if (elem.isJsonPrimitive && elem.asJsonPrimitive.isString) {
                            val parsedPrimitive = tryParseResultString(elem.asString)
                            if (isIdentified(parsedPrimitive)) return parsedPrimitive
                        }
                    }
                }
            }

            return null
        }
    }

    /**
     * Executes AI lesion analysis using ArtMagicClient with agent code `medical_visual_lesion_analysis`.
     * If the agent returns an error (e.g. template agent not assigned to App ID), it returns Result.failure.
     * No fallback hardcoded data is returned.
     */
    suspend fun scanAi(
        imageFile: File,
        category: String = "SKIN",
        age: Int = 20,
        gender: String = "MALE",
        language: String = getLanguageNameForSdk(LocaleHelper.getSavedLanguageCode(context))
    ): Result<LesionAnalysisResult> = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { continuation ->
            Log.d(TAG, "========== START SCAN AI (agent: $AGENT_CODE) ==========")
            Log.d(TAG, "Input: category=$category, age=$age, gender=$gender, language=$language")
            Log.d(TAG, "File: ${imageFile.absolutePath}, size=${imageFile.length() / 1024} KB")

            val optionsMap = mapOf(
                "category" to category.uppercase(Locale.ROOT),
                "age" to age,
                "gender" to gender.uppercase(Locale.ROOT),
                "language" to language
            )
            val optionsJson = gson.toJson(optionsMap)
            Log.d(TAG, "Options JSON: $optionsJson")

            try {
                HealthCareApp.artMagicClient.processGeneralAgent(
                    file = imageFile,
                    options = optionsJson,
                    code = AGENT_CODE
                ) { response, error ->
                    if (error != null) {
                        Log.e(TAG, "API Error from ArtMagicClient: ${error.message}", error)
                        if (continuation.isActive) {
                            continuation.resume(Result.failure(error))
                        }
                    } else {
                        Log.d(TAG, "Raw response from ArtMagicClient: $response")
                        try {
                            val parsed = parseLesionAnalysisResponse(response)
                            if (parsed != null && isIdentified(parsed)) {
                                if (continuation.isActive) {
                                    continuation.resume(Result.success(parsed))
                                }
                            } else {
                                Log.w(TAG, "Response could not be parsed or lesion could not be identified")
                                if (continuation.isActive) {
                                    continuation.resume(Result.failure(Exception("Unable to identify condition from image.")))
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing response: ${e.message}", e)
                            if (continuation.isActive) {
                                continuation.resume(Result.failure(e))
                            }
                        }
                    }
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Exception calling processGeneralAgent", e)
                if (continuation.isActive) {
                    continuation.resume(Result.failure(e))
                }
            }
        }
    }

    private fun getLanguageNameForSdk(langCode: String): String {
        return when (langCode.lowercase(Locale.ROOT)) {
            "vi" -> "vietnamese"
            "es" -> "spanish"
            "fr" -> "french"
            "de" -> "german"
            "pt", "pt_br", "pt_pt" -> "portuguese"
            "ja" -> "japanese"
            "ko" -> "korean"
            "zh", "zh_cn", "zh_tw" -> "chinese"
            "ru" -> "russian"
            "hi" -> "hindi"
            "id" -> "indonesian"
            "ar" -> "arabic"
            "tr" -> "turkish"
            "bn" -> "bengali"
            else -> "english"
        }
    }
}
