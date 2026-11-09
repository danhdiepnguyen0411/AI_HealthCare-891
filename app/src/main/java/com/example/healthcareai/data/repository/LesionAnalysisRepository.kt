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

        private val UNIDENTIFIED_PATTERNS = listOf(
            // Inability to identify / determine
            "unable to identify",
            "cannot identify",
            "could not identify",
            "failed to identify",
            "not able to identify",
            "unidentified",
            "not identified",
            "cannot be identified",
            "unable to determine",
            "cannot determine",
            "could not determine",
            "unable to analyze",
            "cannot analyze",
            "could not analyze",
            "inconclusive",

            // No condition / no lesion / normal / healthy
            "no lesion",
            "no skin condition",
            "no condition detected",
            "no condition found",
            "no abnormalities",
            "no abnormality",
            "no abnormal finding",
            "no visible lesion",
            "no visible abnormality",
            "no signs of",
            "no evidence of",
            "normal skin",
            "healthy skin",
            "clear skin",
            "normal appearance",
            "no disease",

            // Wrong category / not matching / invalid / blurry
            "wrong area",
            "wrong body part",
            "not matching the category",
            "not match the category",
            "does not match",
            "not skin",
            "not human skin",
            "not scalp",
            "not hair",
            "not eye",
            "not mouth",
            "not nail",
            "does not appear to be",
            "does not show",
            "does not contain",
            "blurry",
            "poor quality",
            "unclear image",
            "please retake",
            "please provide a clear",
            "retake the photo",

            // Vietnamese
            "không thể nhận diện",
            "không nhận diện được",
            "chưa thể nhận diện",
            "không xác định được",
            "không thể xác định",
            "chưa xác định được",
            "không tìm thấy",
            "không phát hiện",
            "không có tổn thương",
            "không thấy tổn thương",
            "không có dấu hiệu",
            "không thấy dấu hiệu",
            "không có bất thường",
            "không thấy bất thường",
            "không có bệnh lý",
            "không có bệnh",
            "da bình thường",
            "vùng da bình thường",
            "mắt bình thường",
            "miệng bình thường",
            "móng bình thường",
            "bình thường không có",
            "khỏe mạnh",
            "không phải vùng da",
            "không phải da đầu",
            "không phải mắt",
            "không phải miệng",
            "không phải móng",
            "sai vùng",
            "không đúng danh mục",
            "không khớp danh mục",
            "không phù hợp",
            "ảnh bị mờ",
            "ảnh quá mờ",
            "hình ảnh mờ",
            "chất lượng kém",
            "vui lòng chụp lại",
            "vui lòng cung cấp",
            "không có kết quả",
            "không rõ ràng",
            "chưa rõ ràng"
        )

        private val NONE_OR_NEGATIVE_TITLES = setOf(
            "none", "normal", "n/a", "na", "no lesion", "no condition", "no condition detected",
            "no abnormality", "healthy", "healthy skin", "clear", "nothing detected", "negative",
            "unidentified", "unknown", "inconclusive", "not identified",
            "không có", "bình thường", "không phát hiện", "không rõ", "chưa rõ", "chưa xác định",
            "không xác định", "không có tổn thương", "không có bất thường"
        )

        fun isNegativeOrUnidentifiedText(text: String): Boolean {
            if (text.isBlank()) return false
            val lower = text.lowercase(Locale.ROOT)
            return UNIDENTIFIED_PATTERNS.any { pattern -> lower.contains(pattern) }
        }

        fun isNegativeOrNoneTitle(title: String): Boolean {
            val clean = title.trim().lowercase(Locale.ROOT)
            if (clean.isBlank()) return true
            if (NONE_OR_NEGATIVE_TITLES.contains(clean)) return true
            if (clean.startsWith("no ") ||
                clean.startsWith("không ") ||
                clean.contains("unidentified") ||
                clean.contains("unable to identify") ||
                clean.contains("no lesion") ||
                clean.contains("no condition")) {
                return true
            }
            return false
        }

        /**
         * Helper to verify if the parsed result has substantive lesion findings,
         * and was not marked as unidentified, negative, or wrong scan area.
         */
        fun isIdentified(result: LesionAnalysisResult?): Boolean {
            if (result == null) return false
            if (result.isIdentified == false) return false

            val summaryLower = result.summary?.lowercase(Locale.ROOT) ?: ""
            if (isNegativeOrUnidentifiedText(summaryLower)) {
                return false
            }

            // Must have at least one substantive, valid condition finding
            val validFindings = result.keyFindings?.filter { finding ->
                val title = finding.title?.trim().orEmpty()
                title.isNotBlank() && !isNegativeOrNoneTitle(title)
            }.orEmpty()

            if (validFindings.isEmpty()) {
                return false
            }

            return true
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
        val langCode = LocaleHelper.getSavedLanguageCode(context)
        val langDisplayName = LocaleHelper.getNativeNameByCode(langCode)
        val categoryDescription = when (category.uppercase(Locale.ROOT)) {
            "SKIN" -> "Skin lesion or rash"
            "MOUTH" -> "Mouth or oral cavity condition"
            "EYE" -> "Eye or ocular area condition"
            "NAILS" -> "Nail or nail bed condition"
            else -> "Medical lesion or bodily condition"
        }
        val langInstruction = buildString {
            append("IMPORTANT: All text fields in the JSON response (summary, key_findings[].title, key_findings[].description, care_suggestions[].title, care_suggestions[].description, when_to_seek_care[]) MUST be written in $language ($langDisplayName, locale: $langCode). ")
            append("CATEGORY VALIDATION: The selected examination category is '$category' ($categoryDescription). ")
            append("If the uploaded photo does NOT correspond to the selected body area '$category' (for example: wrong body region, random non-medical object, furniture, clothing, or invalid image), ")
            append("or if the photo is too blurry/unclear to make an assessment, or if NO lesion, rash, disease, or abnormal condition is present: ")
            append("You MUST return is_identified = false, leave key_findings as an empty array [], and clearly state in summary that no condition could be identified or that the image does not match the category '$category'. ")
            append("Never invent, guess, or hallucinate findings when no abnormal condition is found or when the wrong area was scanned.")
        }

        suspendCancellableCoroutine { continuation ->
            Log.d(TAG, "========== START SCAN AI (agent: $AGENT_CODE) ==========")
            Log.d(TAG, "Input: category=$category, age=$age, gender=$gender, language=$language ($langCode)")
            Log.d(TAG, "File: ${imageFile.absolutePath}, size=${imageFile.length() / 1024} KB")

            val optionsMap = mapOf(
                "category" to category.uppercase(Locale.ROOT),
                "age" to age,
                "gender" to gender.uppercase(Locale.ROOT),
                "language" to language,
                "lang" to langCode,
                "output_language" to language,
                "response_language" to language,
                "instruction" to langInstruction
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
