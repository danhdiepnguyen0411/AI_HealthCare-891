package com.example.healthcareai.ui.symptom

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import com.example.healthcareai.R
import com.example.healthcareai.data.model.SymptomAssessmentResult
import com.example.healthcareai.data.model.SymptomCareAdvice
import com.example.healthcareai.data.model.SymptomCondition
import com.example.healthcareai.databinding.ActivitySymptomResultBinding
import com.example.healthcareai.databinding.ItemPossibleConditionBinding
import com.example.healthcareai.databinding.ItemRecommendedActionBinding
import com.example.healthcareai.ui.base.BaseActivity
import com.example.healthcareai.ui.base.applyHeaderTopMargin
import com.example.healthcareai.ui.camera.ReportResultBottomSheetDialog
import com.example.healthcareai.ui.chat.AiChatActivity
import com.example.healthcareai.data.repository.AiChatRepository
import com.example.healthcareai.util.DialogImmersiveHelper
import com.example.healthcareai.util.LocaleHelper
import java.util.Locale
import java.util.UUID

class SymptomResultActivity : BaseActivity() {

    private lateinit var binding: ActivitySymptomResultBinding
    private var assessmentResult: SymptomAssessmentResult? = null
    private val conditionsList = mutableListOf<SymptomCondition>()
    private var currentConversationId: String? = null

    private fun getOrCreateConversationId(): String {
        currentConversationId?.let { return it }
        val newId = UUID.randomUUID().toString()
        currentConversationId = newId
        return newId
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySymptomResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWindowInsets()
        loadAssessmentData()
        setupListeners()
        populateConditions()
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.rootSymptomResultLayout) { _, insets ->
            binding.topBar.applyHeaderTopMargin(insets)

            val navBarBottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            val density = resources.displayMetrics.density

            if (navBarBottom > 0) {
                binding.bottomSection.setPadding(
                    (16 * density).toInt(),
                    (8 * density).toInt(),
                    (16 * density).toInt(),
                    navBarBottom + (12 * density).toInt()
                )
            }
            insets
        }
    }

    private fun loadAssessmentData() {
        @Suppress("DEPRECATION")
        val extraResult = intent.getSerializableExtra(EXTRA_ASSESSMENT_RESULT) as? SymptomAssessmentResult
        if (extraResult != null) {
            assessmentResult = extraResult
            binding.tvHeaderTitle.text = getString(R.string.symptom_result_doctor_title)
            binding.tvHeaderSubtitle.text = extraResult.triageDescription.ifBlank {
                getString(R.string.symptom_result_doctor_desc)
            }
            conditionsList.clear()
            conditionsList.addAll(extraResult.possibleConditions)
        } else {
            // Fallback for legacy intent extras
            loadLegacyConditionsData()
        }
    }

    private fun loadLegacyConditionsData() {
        conditionsList.clear()

        val insectBite = intent.getStringExtra(EXTRA_INSECT_BITE)
        val isInsectBiteYes = insectBite.equals("YES", ignoreCase = true)

        if (isInsectBiteYes) {
            conditionsList.add(
                SymptomCondition(
                    name = "Allergic reactions to insect bite or sting",
                    matchLabel = "Strong Match",
                    matchProgress = 80,
                    recommendedActions = listOf(
                        "Rest and hydrate frequently",
                        "Monitor temperature for 24 hours"
                    ),
                    careAdvice = SymptomCareAdvice(
                        selfCare = listOf(
                            "Clean the bite or sting site with mild soap and water.",
                            "Apply an ice pack wrapped in a cloth for 10-15 minutes at a time.",
                            "Use over-the-counter hydrocortisone cream or antihistamines to reduce itching."
                        ),
                        monitorSymptoms = listOf(
                            "Check your temperature twice a day.",
                            "Monitor for spreading redness."
                        ),
                        whenToSeekCare = listOf(
                            "If your fever exceeds 39.4°C, pain persists for more than 48 hours, or you experience difficulty breathing, seek medical attention immediately."
                        )
                    )
                )
            )
            conditionsList.add(
                SymptomCondition(
                    name = "Contact dermatitis",
                    matchLabel = "Moderate Match",
                    matchProgress = 45,
                    recommendedActions = listOf(
                        "Avoid known allergens",
                        "Consider over-the-counter antihistamines"
                    ),
                    careAdvice = SymptomCareAdvice(
                        selfCare = listOf(
                            "Identify and eliminate contact with the suspected trigger.",
                            "Rinse affected skin with cool water and mild fragrance-free soap.",
                            "Apply barrier creams or calamine lotion to soothe irritation."
                        ),
                        monitorSymptoms = listOf(
                            "Check skin twice daily for changes in rash spread."
                        ),
                        whenToSeekCare = listOf(
                            "If your fever exceeds 39.4°C, pain persists for more than 48 hours, or you experience difficulty breathing, seek medical attention immediately."
                        )
                    )
                )
            )
        } else {
            conditionsList.add(
                SymptomCondition(
                    name = "Contact dermatitis",
                    matchLabel = "Strong Match",
                    matchProgress = 80,
                    recommendedActions = listOf(
                        "Avoid known allergens",
                        "Consider over-the-counter antihistamines"
                    ),
                    careAdvice = SymptomCareAdvice(
                        selfCare = listOf(
                            "Ensure you get at least 8 hours of sleep to help your body recover.",
                            "Drink plenty of water or clear fluids to stay hydrated.",
                            "Identify and avoid contact with the triggering substance."
                        ),
                        monitorSymptoms = listOf(
                            "Check your temperature twice a day.",
                            "Keep an eye on your resting heart rate using your wearable."
                        ),
                        whenToSeekCare = listOf(
                            "If your fever exceeds 39.4°C, pain persists for more than 48 hours, or you experience difficulty breathing, seek medical attention immediately."
                        )
                    )
                )
            )
            conditionsList.add(
                SymptomCondition(
                    name = "Allergic reactions to insect bite or sting",
                    matchLabel = "Moderate Match",
                    matchProgress = 45,
                    recommendedActions = listOf(
                        "Rest and hydrate frequently",
                        "Monitor temperature for 24 hours"
                    ),
                    careAdvice = SymptomCareAdvice(
                        selfCare = listOf(
                            "Keep the area clean and avoid scratching to prevent infection.",
                            "Apply cold compresses to minimize localized swelling."
                        ),
                        monitorSymptoms = listOf(
                            "Monitor for escalating allergic symptoms or signs of infection."
                        ),
                        whenToSeekCare = listOf(
                            "If your fever exceeds 39.4°C, pain persists for more than 48 hours, or you experience difficulty breathing, seek medical attention immediately."
                        )
                    )
                )
            )
        }

        binding.tvHeaderTitle.text = getString(R.string.symptom_result_doctor_title)
        binding.tvHeaderSubtitle.text = getString(R.string.symptom_result_doctor_desc)

        assessmentResult = SymptomAssessmentResult(
            triageLevel = getString(R.string.symptom_result_doctor_title),
            triageDescription = getString(R.string.symptom_result_doctor_desc),
            reportedSymptomsSummary = intent.getStringArrayListExtra(EXTRA_SELECTED_SYMPTOMS)?.joinToString(", ") ?: getString(R.string.symptom_sample_mild_fever_fatigue),
            possibleConditions = conditionsList,
            careAdvice = conditionsList.firstOrNull()?.careAdvice ?: SymptomCareAdvice()
        )
    }

    private fun setupListeners() {
        // Back
        binding.btnBack.setOnClickListener {
            finish()
        }

        // Report Result Bottom Sheet (Same as Scan AI Result)
        binding.btnInfo.setOnClickListener {
            showReportResultBottomSheet()
        }

        // Ask AI about this result
        binding.btnAskAiResult.setOnClickListener {
            val lang = LocaleHelper.getSavedLanguageCode(this@SymptomResultActivity).lowercase(Locale.ROOT)
            val rawConditionNames = conditionsList.joinToString(", ") { it.name }
            val isEnglishConditions = !lang.startsWith("en") && rawConditionNames.isNotBlank() && (
                (rawConditionNames.all { it.code < 128 } && (
                    lang.startsWith("vi") || lang.startsWith("ru") || lang.startsWith("hi") ||
                    lang.startsWith("ja") || lang.startsWith("ko") || lang.startsWith("zh") ||
                    lang.startsWith("ar") || lang.startsWith("bn")
                )) || listOf(
                    "allergic", "dermatitis", "irritation", "gastroenteritis", "headache",
                    "migraine", "bronchitis", "strain", "sprain", "tendinitis", "shoulder",
                    "infection", "syndrome", "reflux", "bite", "sting"
                ).any { rawConditionNames.lowercase(Locale.ROOT).contains(it) }
            )
            val localizedConditions = rawConditionNames.takeIf { it.isNotBlank() && !isEnglishConditions }
            val apiContext = if (rawConditionNames.isNotBlank()) {
                "Symptom assessment possible conditions: $rawConditionNames. Reported symptoms: ${assessmentResult?.reportedSymptomsSummary.orEmpty()}."
            } else {
                null
            }

            val prompt = when {
                lang.startsWith("vi") ->
                    if (!localizedConditions.isNullOrBlank()) {
                        "Kết quả đánh giá triệu chứng của tôi cho thấy nguy cơ: $localizedConditions. Bác sĩ AI có thể giải thích chi tiết hơn về các căn bệnh này, nguyên nhân và hướng chăm sóc điều trị an toàn không?"
                    } else {
                        "Tôi vừa hoàn thành kiểm tra triệu chứng sức khỏe. Bác sĩ AI có thể giải thích chi tiết kết quả đánh giá của tôi, các nguyên nhân có thể gặp và hướng chăm sóc điều trị an toàn không?"
                    }
                lang.startsWith("hi") ->
                    if (!localizedConditions.isNullOrBlank()) {
                        "मेरे लक्षण मूल्यांकन के परिणाम संभावित स्थितियां दिखाते हैं: $localizedConditions। कृपया इन स्थितियों, संभावित कारणों और सुरक्षित देखभाल के उपायों के बारे में विस्तार से बताएं।"
                    } else {
                        "मैंने अभी अपना लक्षण मूल्यांकन पूरा किया है। कृपया मेरे परिणामों, संभावित कारणों और सुरक्षित देखभाल के उपायों के बारे में विस्तार से बताएं।"
                    }
                lang.startsWith("ru") ->
                    if (!localizedConditions.isNullOrBlank()) {
                        "Результаты оценки моих симптомов указывают на возможные состояния: $localizedConditions. Подскажите, пожалуйста, подробнее об этих состояниях, возможных причинах и рекомендациях по уходу."
                    } else {
                        "Я только что прошел(а) оценку симптомов. Подскажите, пожалуйста, подробнее о результатах оценки моих симптомов, возможных причинах и рекомендациях по безопасному уходу."
                    }
                lang.startsWith("id") || lang.startsWith("in") ->
                    if (!localizedConditions.isNullOrBlank()) {
                        "Hasil penilaian gejala saya menunjukkan kemungkinan kondisi: $localizedConditions. Bisakah Anda menjelaskan lebih detail tentang kondisi ini, kemungkinan penyebab, dan panduan perawatan yang tepat?"
                    } else {
                        "Saya baru saja menyelesaikan penilaian gejala kesehatan. Bisakah Anda menjelaskan lebih detail mengenai hasil penilaian saya, kemungkinan penyebab, dan panduan perawatan yang tepat?"
                    }
                lang.startsWith("de") ->
                    if (!localizedConditions.isNullOrBlank()) {
                        "Meine Symptombewertung weist auf mögliche Erkrankungen hin: $localizedConditions. Können Sie mir mehr über diese Erkrankungen, mögliche Ursachen und geeignete Pflegemaßnahmen erklären?"
                    } else {
                        "Ich habe gerade eine Symptombewertung durchgeführt. Können Sie mir bitte meine Bewertungsergebnisse, mögliche Ursachen und geeignete Pflegemaßnahmen näher erklären?"
                    }
                lang.startsWith("es") ->
                    if (!localizedConditions.isNullOrBlank()) {
                        "Los resultados de mi evaluación de síntomas indican posibles afecciones: $localizedConditions. ¿Podría explicarme más sobre estas afecciones, sus posibles causas y las recomendaciones de cuidado?"
                    } else {
                        "Acabo de completar una evaluación de síntomas. ¿Podría explicarme con más detalle los resultados de mi evaluación, las posibles causas y las recomendaciones de cuidado adecuadas?"
                    }
                lang.startsWith("fr") ->
                    if (!localizedConditions.isNullOrBlank()) {
                        "Les résultats de mon évaluation des symptômes indiquent des affections possibles : $localizedConditions. Pouvez-vous m'expliquer plus en détail ces affections, leurs causes potentielles et les conseils de soins appropriés ?"
                    } else {
                        "Je viens de terminer une évaluation des symptômes. Pouvez-vous m'expliquer plus en détail les résultats de mon évaluation, les causes potentielles et les conseils de soins appropriés ?"
                    }
                lang.startsWith("pt") ->
                    if (!localizedConditions.isNullOrBlank()) {
                        "Os resultados da minha avaliação de sintomas indicam possíveis condições: $localizedConditions. Poderia explicar mais sobre essas condições, possíveis causas e orientações de cuidados adequados?"
                    } else {
                        "Acabei de concluir uma avaliação de sintomas. Poderia explicar com mais detalhes os resultados da minha avaliação, possíveis causas e orientações de cuidados adequados?"
                    }
                lang.startsWith("ja") ->
                    if (!localizedConditions.isNullOrBlank()) {
                        "症状チェックの結果、考えられる状態として「${localizedConditions}」が示されました。これらの状態や原因、適切なケア方法について詳しく教えてください。"
                    } else {
                        "先ほど症状チェックを完了しました。診断結果の詳細、考えられる原因、および適切なケア方法について詳しく教えてください。"
                    }
                lang.startsWith("ko") ->
                    if (!localizedConditions.isNullOrBlank()) {
                        "증상 평가 결과 가능한 상태로 \"$localizedConditions\"이(가) 나타났습니다. 이러한 상태와 가능한 원인, 적절한 관리 방법에 대해 자세히 설명해 주시겠어요?"
                    } else {
                        "방금 증상 체크를 완료했습니다. 증상 평가 결과와 가능한 원인, 적절한 관리 방법에 대해 자세히 설명해 주시겠어요?"
                    }
                lang.startsWith("zh_tw") ->
                    if (!localizedConditions.isNullOrBlank()) {
                        "我的症狀評估結果顯示可能的狀況為：$localizedConditions。請問這些狀況的可能原因以及適當的照護建議是什麼？"
                    } else {
                        "我剛完成了症狀評估。請幫我詳細解讀評估結果、可能的原因以及適當的照護建議。"
                    }
                lang.startsWith("zh") ->
                    if (!localizedConditions.isNullOrBlank()) {
                        "我的症状评估结果显示可能的状况为：$localizedConditions。请问这些状况的可能原因以及适当的护理建议是什么？"
                    } else {
                        "我刚完成了症状评估。请帮我详细解读评估结果、可能的原因以及适当的护理建议。"
                    }
                lang.startsWith("ar") ->
                    if (!localizedConditions.isNullOrBlank()) {
                        "تشير نتائج تقييم الأعراض الخاصة بي إلى حالات محتملة: $localizedConditions. هل يمكنك شرح المزيد عن هذه الحالات والأسباب المحتملة وإرشادات الرعاية المناسبة؟"
                    } else {
                        "لقد أكملت للتو تقييم الأعراض. هل يمكنك شرح نتائج التقييم والأسباب المحتملة وإرشادات الرعاية المناسبة بالتفصيل؟"
                    }
                lang.startsWith("tr") ->
                    if (!localizedConditions.isNullOrBlank()) {
                        "Belirti değerlendirme sonuçlarım olası durumları gösteriyor: $localizedConditions. Bu durumlar, olası nedenler ve uygun bakım önerileri hakkında daha fazla bilgi verebilir misiniz?"
                    } else {
                        "Az önce belirti değerlendirmesini tamamladım. Değerlendirme sonuçlarım, olası nedenler ve uygun bakım önerileri hakkında detaylı bilgi verebilir misiniz?"
                    }
                lang.startsWith("bn") ->
                    if (!localizedConditions.isNullOrBlank()) {
                        "আমার লক্ষণ মূল্যায়নের ফলাফলে সম্ভাব্য অবস্থা দেখা গেছে: $localizedConditions। অনুগ্রহ করে এই অবস্থাগুলি, সম্ভাব্য কারণ এবং সঠিক যত্নের নির্দেশিকা সম্পর্কে বিস্তারিত বলুন।"
                    } else {
                        "আমি এইমাত্র আমার লক্ষণ মূল্যায়ন সম্পন্ন করেছি। অনুগ্রহ করে আমার ফলাফল, সম্ভাব্য কারণ এবং সঠিক যত্নের নির্দেশিকা সম্পর্কে বিস্তারিত বলুন।"
                    }
                else ->
                    "My symptom assessment results indicate possible conditions: $rawConditionNames. Could you explain more about these conditions, potential causes, and appropriate care guidance?"
            }

            var convId = getOrCreateConversationId()
            val existing = AiChatRepository.getInstance(this).getConversationById(convId)
            val firstUserMsg = existing?.messages?.firstOrNull { it.isUser }?.content
            val hasMatchingMessages = existing != null && firstUserMsg == prompt
            if (existing != null && !hasMatchingMessages) {
                convId = UUID.randomUUID().toString()
                currentConversationId = convId
            }

            val intent = Intent(this, AiChatActivity::class.java).apply {
                putExtra(AiChatActivity.EXTRA_CONVERSATION_ID, convId)
                if (!hasMatchingMessages) {
                    putExtra(AiChatActivity.EXTRA_INITIAL_PROMPT, prompt)
                    if (!apiContext.isNullOrBlank()) {
                        putExtra(AiChatActivity.EXTRA_API_CONTEXT, apiContext)
                    }
                    putExtra(AiChatActivity.EXTRA_AUTO_SEND, true)
                }
            }
            startActivity(intent)
        }
    }

    private fun populateConditions() {
        binding.llConditionsList.removeAllViews()
        val inflater = LayoutInflater.from(this)

        for (condition in conditionsList) {
            val itemBinding = ItemPossibleConditionBinding.inflate(inflater, binding.llConditionsList, false)

            // Progress & Match label
            itemBinding.pbMatch.progress = condition.matchProgress
            val localizedMatchLabel = when {
                condition.matchLabel.contains("Strong", ignoreCase = true) -> getString(R.string.match_strong)
                condition.matchLabel.contains("Moderate", ignoreCase = true) -> getString(R.string.match_moderate)
                condition.matchLabel.contains("Possible", ignoreCase = true) -> getString(R.string.match_possible)
                else -> condition.matchLabel
            }
            itemBinding.tvMatchLabel.text = localizedMatchLabel

            // Title
            itemBinding.tvConditionTitle.text = condition.name

            // Recommended actions list
            itemBinding.llActionsList.removeAllViews()
            for (action in condition.recommendedActions) {
                val actionBinding = ItemRecommendedActionBinding.inflate(inflater, itemBinding.llActionsList, false)
                actionBinding.tvActionText.text = action
                itemBinding.llActionsList.addView(actionBinding.root)
            }

            // View Care Advice -> Opens CareAdviceActivity
            itemBinding.btnViewCareAdvice.setOnClickListener {
                val intent = Intent(this, CareAdviceActivity::class.java).apply {
                    putExtra(CareAdviceActivity.EXTRA_ASSESSMENT_RESULT, assessmentResult)
                    putExtra(CareAdviceActivity.EXTRA_SPECIFIC_CONDITION, condition)
                    putExtra(CareAdviceActivity.EXTRA_SYMPTOMS_SUMMARY, assessmentResult?.reportedSymptomsSummary)
                }
                startActivity(intent)
            }

            binding.llConditionsList.addView(itemBinding.root)
        }
    }

    private fun showReportResultBottomSheet() {
        ReportResultBottomSheetDialog.show(supportFragmentManager)
    }

    private fun showInfoDisclaimerDialog() {
        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.medical_disclaimer_dialog_title)
            .setMessage(R.string.medical_disclaimer_full_text)
            .setPositiveButton(R.string.btn_understood, null)
            .create()

        DialogImmersiveHelper.showImmersive(dialog)
    }

    companion object {
        const val EXTRA_ASSESSMENT_RESULT = "extra_assessment_result"
        const val EXTRA_BODY_REGION = "extra_body_region"
        const val EXTRA_SELECTED_SYMPTOMS = "extra_selected_symptoms"
        const val EXTRA_INSECT_BITE = "extra_insect_bite"
    }
}
