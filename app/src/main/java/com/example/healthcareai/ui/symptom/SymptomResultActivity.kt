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
import com.example.healthcareai.ui.camera.ReportResultBottomSheetDialog
import com.example.healthcareai.ui.chat.AiChatActivity
import com.example.healthcareai.util.DialogImmersiveHelper
import com.example.healthcareai.util.LocaleHelper
import java.util.Locale

class SymptomResultActivity : BaseActivity() {

    private lateinit var binding: ActivitySymptomResultBinding
    private var assessmentResult: SymptomAssessmentResult? = null
    private val conditionsList = mutableListOf<SymptomCondition>()

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
            val statusBarTop = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            val navBarBottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            val density = resources.displayMetrics.density

            if (statusBarTop > 0) {
                binding.topBar.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    topMargin = statusBarTop + 8
                }
            }
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
            val lang = LocaleHelper.getSavedLanguageCode(this).lowercase(Locale.ROOT)
            val conditionNames = conditionsList.joinToString(", ") { it.name }

            val prompt = if (lang.startsWith("vi")) {
                "Kết quả đánh giá triệu chứng của tôi cho thấy nguy cơ: $conditionNames. Bác sĩ AI có thể giải thích chi tiết hơn về các căn bệnh này, nguyên nhân và hướng chăm sóc điều trị an toàn không?"
            } else {
                "My symptom assessment results indicate possible conditions: $conditionNames. Could you explain more about these conditions, potential causes, and appropriate care guidance?"
            }

            val intent = Intent(this, AiChatActivity::class.java).apply {
                putExtra(AiChatActivity.EXTRA_INITIAL_PROMPT, prompt)
                putExtra(AiChatActivity.EXTRA_AUTO_SEND, true)
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
