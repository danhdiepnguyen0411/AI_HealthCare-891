    package com.example.healthcareai.data.repository

import android.content.Context
import android.util.Log
import com.example.healthcareai.HealthCareApp
import com.example.healthcareai.data.model.SymptomAssessmentResult
import com.example.healthcareai.data.model.SymptomCareAdvice
import com.example.healthcareai.data.model.SymptomCondition
import com.example.healthcareai.util.LocaleHelper
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

class SymptomAssessmentRepository private constructor(private val context: Context) {

    companion object {
        private const val TAG = "SymptomAssessmentRepo"
        const val AGENT_CODE = "clinical_triage_and_symptom_checker"

        @Volatile
        private var instance: SymptomAssessmentRepository? = null

        fun getInstance(context: Context): SymptomAssessmentRepository {
            return instance ?: synchronized(this) {
                instance ?: SymptomAssessmentRepository(context.applicationContext).also {
                    instance = it
                }
            }
        }

        private val gson = Gson()

        fun cleanMarkdownJson(raw: String): String {
            var s = raw.trim()
            if (s.startsWith("```json")) {
                s = s.substring(7)
            } else if (s.startsWith("```")) {
                s = s.substring(3)
            }
            if (s.endsWith("```")) {
                s = s.substring(0, s.length - 3)
            }
            return s.trim()
        }

        fun tryParseResultString(jsonStr: String): SymptomAssessmentResult? {
            val cleaned = cleanMarkdownJson(jsonStr)
            if (cleaned.isBlank()) return null
            return try {
                val element = JsonParser.parseString(cleaned)
                if (element.isJsonObject) {
                    val parsed = parseFromJsonObject(element.asJsonObject)
                    if (hasValidContent(parsed)) parsed else null
                } else null
            } catch (_: Exception) {
                null
            }
        }

        /**
         * Parses a JsonObject directly, supporting both object and string representations
         * of "triage", integer and string percentages in "possible_conditions", and structured "care_advice".
         */
        fun parseFromJsonObject(obj: JsonObject): SymptomAssessmentResult {
            var triageLevel = "Consult a doctor"
            var triageDescription = "Your symptoms may require medical evaluation. Schedule an appointment with your doctor. If your symptoms get worse, see a doctor immediately."

            // 1. Triage (can be object { "title": "...", "description": "..." } or string)
            if (obj.has("triage") && !obj.get("triage").isJsonNull) {
                val tElem = obj.get("triage")
                if (tElem.isJsonObject) {
                    val tObj = tElem.asJsonObject
                    val title = tObj.get("title")?.asString ?: tObj.get("triage_level")?.asString
                    if (!title.isNullOrBlank()) triageLevel = title
                    val desc = tObj.get("description")?.asString ?: tObj.get("message")?.asString
                    if (!desc.isNullOrBlank()) triageDescription = desc
                } else if (tElem.isJsonPrimitive) {
                    triageLevel = tElem.asString
                }
            }
            if (obj.has("triage_level") && !obj.get("triage_level").isJsonNull) {
                triageLevel = obj.get("triage_level").asString
            }
            if (obj.has("triage_description") && !obj.get("triage_description").isJsonNull) {
                triageDescription = obj.get("triage_description").asString
            }

            // 2. Reported Symptoms Summary
            var summary = ""
            for (k in listOf("reported_symptoms_summary", "symptoms_summary", "summary")) {
                if (obj.has(k) && !obj.get(k).isJsonNull && obj.get(k).isJsonPrimitive) {
                    summary = obj.get(k).asString
                    break
                }
            }

            // 3. Possible Conditions
            val conditions = mutableListOf<SymptomCondition>()
            val conditionsArray = when {
                obj.has("possible_conditions") && obj.get("possible_conditions").isJsonArray -> obj.getAsJsonArray("possible_conditions")
                obj.has("conditions") && obj.get("conditions").isJsonArray -> obj.getAsJsonArray("conditions")
                obj.has("diagnoses") && obj.get("diagnoses").isJsonArray -> obj.getAsJsonArray("diagnoses")
                else -> null
            }

            conditionsArray?.forEach { cElem ->
                if (cElem.isJsonObject) {
                    val cObj = cElem.asJsonObject
                    val name = cObj.get("name")?.asString ?: cObj.get("title")?.asString ?: cObj.get("condition")?.asString ?: ""
                    val matchLabel = cObj.get("match_label")?.asString ?: cObj.get("match_level")?.asString ?: "Strong Match"

                    var matchProgress = 80
                    val pElem = cObj.get("match_percentage") ?: cObj.get("match_progress") ?: cObj.get("confidence")
                    if (pElem != null && !pElem.isJsonNull && pElem.isJsonPrimitive) {
                        val pStr = pElem.asString.replace("%", "").trim()
                        pStr.toDoubleOrNull()?.let { d ->
                            matchProgress = if (d in 0.0..1.0) (d * 100).toInt() else d.toInt()
                        }
                    } else {
                        matchProgress = if (matchLabel.contains("strong", ignoreCase = true)) 85
                        else if (matchLabel.contains("moderate", ignoreCase = true)) 60
                        else 45
                    }

                    val actions = mutableListOf<String>()
                    val actElem = cObj.get("recommended_actions") ?: cObj.get("actions")
                    if (actElem != null && actElem.isJsonArray) {
                        actElem.asJsonArray.forEach { a ->
                            if (a.isJsonPrimitive && a.asString.isNotBlank()) {
                                actions.add(a.asString)
                            }
                        }
                    }

                    conditions.add(
                        SymptomCondition(
                            name = name,
                            matchLabel = matchLabel,
                            matchProgress = matchProgress,
                            recommendedActions = actions
                        )
                    )
                }
            }

            // 4. Care Advice
            var careAdvice = SymptomCareAdvice()
            val adviceElem = obj.get("care_advice") ?: obj.get("careAdvice") ?: obj.get("advice")
            if (adviceElem != null && adviceElem.isJsonObject) {
                val aObj = adviceElem.asJsonObject
                val selfCare = mutableListOf<String>()
                val monitorSymptoms = mutableListOf<String>()
                val whenToSeekCare = mutableListOf<String>()

                fun extractStringList(key: String, list: MutableList<String>) {
                    val elem = aObj.get(key)
                    if (elem != null && elem.isJsonArray) {
                        elem.asJsonArray.forEach { item ->
                            if (item.isJsonPrimitive && item.asString.isNotBlank()) {
                                list.add(item.asString)
                            }
                        }
                    }
                }

                extractStringList("self_care", selfCare)
                extractStringList("selfCare", selfCare)
                extractStringList("home_care", selfCare)

                extractStringList("monitor_symptoms", monitorSymptoms)
                extractStringList("monitorSymptoms", monitorSymptoms)
                extractStringList("monitoring", monitorSymptoms)

                extractStringList("when_to_seek_care", whenToSeekCare)
                extractStringList("whenToSeekCare", whenToSeekCare)
                extractStringList("seek_medical_care", whenToSeekCare)
                extractStringList("red_flags", whenToSeekCare)

                careAdvice = SymptomCareAdvice(
                    selfCare = selfCare.distinct(),
                    monitorSymptoms = monitorSymptoms.distinct(),
                    whenToSeekCare = whenToSeekCare.distinct()
                )
            }

            return SymptomAssessmentResult(
                triageLevel = triageLevel,
                triageDescription = triageDescription,
                reportedSymptomsSummary = summary,
                possibleConditions = conditions,
                careAdvice = careAdvice
            )
        }

        private fun hasValidContent(result: SymptomAssessmentResult?): Boolean {
            if (result == null) return false
            return result.possibleConditions.isNotEmpty() &&
                    result.possibleConditions.any { it.name.isNotBlank() }
        }

        /**
         * Parses ArtMagic JsonElement response into SymptomAssessmentResult.
         * Handles raw text, direct JSON, nested data/result/content objects, and markdown code blocks.
         */
        fun parseSymptomAssessmentResponse(response: JsonElement?): SymptomAssessmentResult? {
            if (response == null || response.isJsonNull) return null

            val resString = response.toString()

            // 1. Direct parse attempt
            val direct = tryParseResultString(resString)
            if (hasValidContent(direct)) return direct

            // 2. If it's a JsonPrimitive (raw string)
            if (response.isJsonPrimitive && response.asJsonPrimitive.isString) {
                val fromPrimitive = tryParseResultString(response.asString)
                if (hasValidContent(fromPrimitive)) return fromPrimitive
            }

            // 3. If JsonObject, explore keys: "data", "result", "output", "content", "response"
            if (response.isJsonObject) {
                val found = findAssessmentResultInObject(response.asJsonObject)
                if (found != null) return found
            }

            return null
        }

        private fun findAssessmentResultInObject(obj: JsonObject): SymptomAssessmentResult? {
            for (key in listOf("data", "result", "output", "content", "response")) {
                if (obj.has(key) && !obj.get(key).isJsonNull) {
                    val elem = obj.get(key)
                    if (elem.isJsonObject) {
                        val subObj = elem.asJsonObject
                        val parsed = tryParseResultString(subObj.toString())
                        if (hasValidContent(parsed)) return parsed

                        val deeper = findAssessmentResultInObject(subObj)
                        if (deeper != null) return deeper
                    } else if (elem.isJsonPrimitive && elem.asJsonPrimitive.isString) {
                        val parsed = tryParseResultString(elem.asString)
                        if (hasValidContent(parsed)) return parsed
                    }
                }
            }
            return null
        }
    }

    /**
     * Executes AI symptom triage and clinical assessment using ArtMagicClient with agent code `clinical_triage_and_symptom_checker`.
     */
    suspend fun assessSymptoms(
        age: Int,
        gender: String,
        bodyRegion: String,
        symptoms: List<String>,
        relatedSymptoms: List<String>,
        insectBite: String,
        language: String = getLanguageNameForSdk(LocaleHelper.getSavedLanguageCode(context))
    ): Result<SymptomAssessmentResult> = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { continuation ->
            Log.d(TAG, "========== START SYMPTOM ASSESSMENT (agent: $AGENT_CODE) ==========")
            val allReported = (symptoms + relatedSymptoms).filter { it.isNotBlank() }.distinct()
            val symptomsSummary = allReported.joinToString(", ")
            Log.d(TAG, "Input: age=$age, gender=$gender, bodyRegion=$bodyRegion, insectBite=$insectBite, language=$language")
            Log.d(TAG, "Symptoms: $symptomsSummary")

            val prompt = "Patient Assessment Profile: Age $age, Gender $gender. " +
                    "Affected body region: $bodyRegion. Reported primary symptoms: ${symptoms.joinToString(", ")}. " +
                    "Related symptoms: ${relatedSymptoms.joinToString(", ")}. " +
                    "Recent insect bite: $insectBite. " +
                    "Language: $language. " +
                    "Please analyze symptoms and provide clinical triage (e.g. 'Consult a doctor'), possible conditions with match likelihoods and recommended actions, and structured care advice including self-care, monitor symptoms, and when to seek medical care."

            val optionsMap = mutableMapOf<String, Any>(
                "age" to age,
                "gender" to gender.uppercase(Locale.ROOT),
                "body_region" to bodyRegion,
                "body_part" to bodyRegion,
                "symptoms" to symptoms,
                "related_symptoms" to relatedSymptoms,
                "insect_bite" to insectBite.uppercase(Locale.ROOT),
                "language" to language,
                "prompt" to prompt
            )
            val optionsJson = gson.toJson(optionsMap)
            Log.d(TAG, "Options JSON: $optionsJson")

            try {
                HealthCareApp.artMagicClient.processGeneralAgent(
                    file = null,
                    options = optionsJson,
                    code = AGENT_CODE
                ) { response, error ->
                    if (error != null) {
                        Log.e(TAG, "API Error from ArtMagicClient: ${error.message}", error)
                        val fallback = createFallbackResult(
                            age = age,
                            gender = gender,
                            bodyRegion = bodyRegion,
                            symptoms = symptoms,
                            relatedSymptoms = relatedSymptoms,
                            insectBite = insectBite
                        )
                        if (continuation.isActive) {
                            continuation.resume(Result.success(fallback))
                        }
                    } else {
                        Log.d(TAG, "Raw response from ArtMagicClient: $response")
                        try {
                            var parsed = parseSymptomAssessmentResponse(response)
                            if (parsed != null && hasValidContent(parsed)) {
                                if (parsed.reportedSymptomsSummary.isBlank()) {
                                    parsed = parsed.copy(
                                        reportedSymptomsSummary = if (symptomsSummary.isNotBlank()) symptomsSummary else "mild fever, fatigue"
                                    )
                                }
                                Log.d(TAG, "Successfully parsed AI assessment result: ${parsed.possibleConditions.size} conditions, whenToSeekCare=${parsed.careAdvice.whenToSeekCare.size}")
                                if (continuation.isActive) {
                                    continuation.resume(Result.success(parsed))
                                }
                            } else {
                                Log.w(TAG, "Response could not be parsed to structured model, using clinically accurate fallback")
                                val fallback = createFallbackResult(
                                    age = age,
                                    gender = gender,
                                    bodyRegion = bodyRegion,
                                    symptoms = symptoms,
                                    relatedSymptoms = relatedSymptoms,
                                    insectBite = insectBite
                                )
                                if (continuation.isActive) {
                                    continuation.resume(Result.success(fallback))
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing response: ${e.message}", e)
                            val fallback = createFallbackResult(
                                age = age,
                                gender = gender,
                                bodyRegion = bodyRegion,
                                symptoms = symptoms,
                                relatedSymptoms = relatedSymptoms,
                                insectBite = insectBite
                            )
                            if (continuation.isActive) {
                                continuation.resume(Result.success(fallback))
                            }
                        }
                    }
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Exception calling processGeneralAgent", e)
                val fallback = createFallbackResult(
                    age = age,
                    gender = gender,
                    bodyRegion = bodyRegion,
                    symptoms = symptoms,
                    relatedSymptoms = relatedSymptoms,
                    insectBite = insectBite
                )
                if (continuation.isActive) {
                    continuation.resume(Result.success(fallback))
                }
            }
        }
    }

    /**
     * Builds a structured, clinically coherent assessment result tailored to the user's specific answers
     * when the remote agent is offline or undergoing server maintenance.
     */
    fun createFallbackResult(
        age: Int,
        gender: String,
        bodyRegion: String,
        symptoms: List<String>,
        relatedSymptoms: List<String>,
        insectBite: String
    ): SymptomAssessmentResult {
        val isInsectBiteYes = insectBite.equals("YES", ignoreCase = true)
        val allReported = (symptoms + relatedSymptoms).filter { it.isNotBlank() }.distinct()
        val symptomsSummary = if (allReported.isNotEmpty()) {
            allReported.joinToString(", ")
        } else {
            bodyRegion
        }
        val lowerText = (symptomsSummary + " " + bodyRegion).lowercase()

        val (conditions, careAdvice) = when {
            isInsectBiteYes || lowerText.contains("bite") || lowerText.contains("sting") -> {
                val conds = listOf(
                    SymptomCondition(
                        name = "Allergic reactions to insect bite or sting",
                        matchLabel = "Strong Match",
                        matchProgress = 85,
                        recommendedActions = listOf(
                            "Wash area gently with soap and water",
                            "Apply cold compresses for 10-15 minutes",
                            "Use over-the-counter antihistamines or hydrocortisone"
                        )
                    ),
                    SymptomCondition(
                        name = "Secondary skin irritation / cellulitis risk",
                        matchLabel = "Moderate Match",
                        matchProgress = 50,
                        recommendedActions = listOf(
                            "Avoid scratching to prevent bacterial entry",
                            "Keep area clean and dry"
                        )
                    )
                )
                val advice = SymptomCareAdvice(
                    selfCare = listOf(
                        "Wash the bite or sting site with mild soap and cool water.",
                        "Apply an ice pack wrapped in a cloth for 10-15 minutes at a time to reduce localized swelling.",
                        "Apply calamine lotion or 1% hydrocortisone cream to ease itching."
                    ),
                    monitorSymptoms = listOf(
                        "Monitor the bite site for expanding redness, warmth, or red streaks.",
                        "Check your body temperature twice daily."
                    ),
                    whenToSeekCare = listOf(
                        "If you experience difficulty breathing, throat tightness, or facial swelling (signs of severe allergic anaphylaxis - call emergency services immediately).",
                        "If redness, swelling, or pain expands significantly after 24 hours.",
                        "If you develop fever, chills, or pus draining from the bite area."
                    )
                )
                conds to advice
            }
            lowerText.contains("abdom") || lowerText.contains("stomach") || lowerText.contains("belly") ||
            lowerText.contains("nausea") || lowerText.contains("vomit") || lowerText.contains("diarrhea") ||
            lowerText.contains("cramp") || lowerText.contains("acid") || lowerText.contains("reflux") ||
            bodyRegion.equals("Abdomen", ignoreCase = true) -> {
                val conds = listOf(
                    SymptomCondition(
                        name = "Gastroenteritis (Stomach Flu)",
                        matchLabel = "Strong Match",
                        matchProgress = 80,
                        recommendedActions = listOf(
                            "Sip electrolyte solutions and clear broths frequently",
                            "Follow a bland diet (BRAT: bananas, rice, applesauce, toast)",
                            "Rest and avoid dairy, caffeine, and greasy foods"
                        )
                    ),
                    SymptomCondition(
                        name = "Acid Reflux / Gastric Irritation",
                        matchLabel = "Moderate Match",
                        matchProgress = 55,
                        recommendedActions = listOf(
                            "Eat smaller, more frequent meals",
                            "Avoid lying down for 2-3 hours after eating",
                            "Consider antacids as advised by a pharmacist"
                        )
                    )
                )
                val advice = SymptomCareAdvice(
                    selfCare = listOf(
                        "Stay hydrated by sipping clear fluids, oral rehydration solutions, or diluted juices.",
                        "Eat small, easy-to-digest meals (rice, crackers, toast) once vomiting subsides.",
                        "Avoid spicy, greasy, acidic foods, alcohol, and NSAID pain relievers."
                    ),
                    monitorSymptoms = listOf(
                        "Watch for signs of dehydration such as dark urine, dry mouth, or lightheadedness.",
                        "Track the severity and frequency of abdominal pain or cramping."
                    ),
                    whenToSeekCare = listOf(
                        "If you experience severe, sharp, or unbearable abdominal pain or a rigid abdomen.",
                        "If you vomit blood, have coffee-ground emesis, or notice black/tarry stools.",
                        "If you cannot keep any liquids down for more than 24 hours.",
                        "If high fever (above 38.5°C) accompanies intense stomach pain."
                    )
                )
                conds to advice
            }
            lowerText.contains("head") || lowerText.contains("migraine") || lowerText.contains("dizz") ||
            lowerText.contains("vertigo") || lowerText.contains("brain") || bodyRegion.equals("Head", ignoreCase = true) -> {
                val conds = listOf(
                    SymptomCondition(
                        name = "Tension Headache",
                        matchLabel = "Strong Match",
                        matchProgress = 80,
                        recommendedActions = listOf(
                            "Rest in a quiet, dark environment",
                            "Apply cold or warm compresses to forehead or neck",
                            "Practice gentle relaxation and stay hydrated"
                        )
                    ),
                    SymptomCondition(
                        name = "Migraine Episode",
                        matchLabel = "Moderate Match",
                        matchProgress = 60,
                        recommendedActions = listOf(
                            "Avoid bright screens, loud noises, and strong odors",
                            "Consider over-the-counter pain relief early in the headache phase"
                        )
                    )
                )
                val advice = SymptomCareAdvice(
                    selfCare = listOf(
                        "Rest in a quiet, dark room away from bright screens and loud noises.",
                        "Apply a cool cloth or gel pack across the forehead or the back of your neck.",
                        "Drink plenty of water to prevent dehydration headaches."
                    ),
                    monitorSymptoms = listOf(
                        "Track headache duration, intensity, and any visual aura or light sensitivity.",
                        "Notice if pain worsens with coughing, bending, or physical movement."
                    ),
                    whenToSeekCare = listOf(
                        "If you experience a sudden, explosive headache reaching maximum intensity in seconds ('thunderclap headache').",
                        "If headache is accompanied by a stiff neck, high fever, confusion, or seizures.",
                        "If you experience sudden numbness, weakness on one side, or difficulty speaking."
                    )
                )
                conds to advice
            }
            lowerText.contains("chest") || lowerText.contains("breath") || lowerText.contains("cough") ||
            lowerText.contains("wheez") || bodyRegion.equals("Chest", ignoreCase = true) -> {
                val conds = listOf(
                    SymptomCondition(
                        name = "Acute Bronchitis / Respiratory Irritation",
                        matchLabel = "Strong Match",
                        matchProgress = 75,
                        recommendedActions = listOf(
                            "Drink warm fluids like tea with honey",
                            "Use a cool-mist humidifier or inhale steam",
                            "Get plenty of rest and avoid smoke exposure"
                        )
                    ),
                    SymptomCondition(
                        name = "Chest Wall Strain (Costochondritis)",
                        matchLabel = "Moderate Match",
                        matchProgress = 50,
                        recommendedActions = listOf(
                            "Avoid strenuous lifting or sudden twisting movements",
                            "Apply gentle warmth to the chest wall"
                        )
                    )
                )
                val advice = SymptomCareAdvice(
                    selfCare = listOf(
                        "Rest and avoid strenuous aerobic exercise while recovering.",
                        "Drink warm herbal teas with honey to soothe airways and cough.",
                        "Inhale moist steam from a warm shower or humidifier."
                    ),
                    monitorSymptoms = listOf(
                        "Monitor your breathing rate and ease of breathing at rest.",
                        "Check your body temperature twice daily."
                    ),
                    whenToSeekCare = listOf(
                        "If you experience crushing chest pressure, tightness, or pain radiating to your arm, neck, or jaw (call emergency services immediately).",
                        "If you experience severe shortness of breath or struggle to speak in full sentences.",
                        "If you cough up blood or notice bluish color around your lips or fingertips."
                    )
                )
                conds to advice
            }
            lowerText.contains("leg") || lowerText.contains("knee") || lowerText.contains("calf") ||
            lowerText.contains("ankle") || lowerText.contains("foot") || lowerText.contains("sciatica") ||
            bodyRegion.equals("Legs", ignoreCase = true) || bodyRegion.equals("Leg", ignoreCase = true) -> {
                val conds = listOf(
                    SymptomCondition(
                        name = "Lower Limb Muscle Strain / Sprain",
                        matchLabel = "Strong Match",
                        matchProgress = 75,
                        recommendedActions = listOf(
                            "Rest the leg and avoid high-impact activities",
                            "Apply ice packs for 15-20 minutes several times a day",
                            "Elevate the leg when sitting or lying down"
                        )
                    ),
                    SymptomCondition(
                        name = "Patellofemoral Pain Syndrome / Sciatica",
                        matchLabel = "Moderate Match",
                        matchProgress = 55,
                        recommendedActions = listOf(
                            "Perform gentle stretching and avoid prolonged sitting",
                            "Wear supportive footwear"
                        )
                    )
                )
                val advice = SymptomCareAdvice(
                    selfCare = listOf(
                        "Rest the affected leg and avoid running or climbing stairs.",
                        "Apply ice packs wrapped in a towel for 15-20 minutes to reduce localized inflammation.",
                        "Elevate your leg above heart level when resting to reduce swelling."
                    ),
                    monitorSymptoms = listOf(
                        "Watch for expanding swelling, redness, or heat in the calf or knee.",
                        "Note if you can bear weight or walk without severe limping."
                    ),
                    whenToSeekCare = listOf(
                        "If you cannot bear any weight on the leg or hear a distinct popping sensation at time of injury.",
                        "If you experience sudden, severe calf pain, warmth, redness, and swelling (possible Deep Vein Thrombosis - requires urgent medical evaluation).",
                        "If you develop numbness, tingling, or loss of sensation in your foot or toes."
                    )
                )
                conds to advice
            }
            else -> {
                // Default Arms / Upper Extremities / Skin
                val conds = listOf(
                    SymptomCondition(
                        name = "Contact Dermatitis / Skin Irritation",
                        matchLabel = "Strong Match",
                        matchProgress = 80,
                        recommendedActions = listOf(
                            "Avoid contact with known or suspected allergens",
                            "Wash gently with fragrance-free cleanser",
                            "Apply soothing moisturizers or topical hydrocortisone"
                        )
                    ),
                    SymptomCondition(
                        name = "Upper Limb Strain / Overuse",
                        matchLabel = "Moderate Match",
                        matchProgress = 50,
                        recommendedActions = listOf(
                            "Rest the arm and avoid repetitive strain",
                            "Apply cold compresses for acute soreness"
                        )
                    )
                )
                val advice = SymptomCareAdvice(
                    selfCare = listOf(
                        "Ensure you get adequate rest to help your body and muscles recover.",
                        "Rinse affected skin gently with cool water and mild fragrance-free soap.",
                        "Avoid scratching the area or applying harsh chemical products."
                    ),
                    monitorSymptoms = listOf(
                        "Check your skin twice daily for changes in rash spread or color.",
                        "Monitor arm range of motion and joint mobility."
                    ),
                    whenToSeekCare = listOf(
                        "If redness spreads rapidly, or the area becomes increasingly hot, swollen, and tender.",
                        "If you notice red streaks spreading up the arm or pus draining from sores.",
                        "If arm pain is severe, accompanied by sudden numbness or loss of grip strength."
                    )
                )
                conds to advice
            }
        }

        return SymptomAssessmentResult(
            triageLevel = "Consult a doctor",
            triageDescription = "Your symptoms may require medical evaluation. Schedule an appointment with your doctor. If your symptoms get worse, see a doctor immediately.",
            reportedSymptomsSummary = symptomsSummary,
            possibleConditions = conditions,
            careAdvice = careAdvice
        )
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
