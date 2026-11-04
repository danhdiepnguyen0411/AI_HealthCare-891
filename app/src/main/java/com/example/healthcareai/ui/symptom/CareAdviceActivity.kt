package com.example.healthcareai.ui.symptom

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
import com.example.healthcareai.data.repository.SymptomAssessmentRepository
import com.example.healthcareai.databinding.ActivityCareAdviceBinding
import com.example.healthcareai.databinding.ItemCareBulletBinding
import com.example.healthcareai.databinding.ItemWhenToSeekCareBinding
import com.example.healthcareai.ui.base.BaseActivity
import com.example.healthcareai.ui.camera.ChooseRemindersBottomSheetDialog
import com.example.healthcareai.ui.main.MainActivity

class CareAdviceActivity : BaseActivity() {

    private lateinit var binding: ActivityCareAdviceBinding

    private var assessmentResult: SymptomAssessmentResult? = null
    private var specificCondition: SymptomCondition? = null
    private var customSymptomsSummary: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCareAdviceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWindowInsets()
        parseIntentData()
        setupListeners()
        populateCareAdviceUi()
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.rootCareAdviceLayout) { _, insets ->
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

    private fun parseIntentData() {
        @Suppress("DEPRECATION")
        assessmentResult = intent.getSerializableExtra(EXTRA_ASSESSMENT_RESULT) as? SymptomAssessmentResult
        @Suppress("DEPRECATION")
        specificCondition = intent.getSerializableExtra(EXTRA_SPECIFIC_CONDITION) as? SymptomCondition
        customSymptomsSummary = intent.getStringExtra(EXTRA_SYMPTOMS_SUMMARY)
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnSetReminder.setOnClickListener {
            ChooseRemindersBottomSheetDialog.show(
                fragmentManager = supportFragmentManager,
                profileId = null,
                analysisResult = null,
                category = "SYMPTOM"
            )
        }

        binding.btnReturnHome.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(MainActivity.EXTRA_TARGET_TAB, MainActivity.TAB_HOME)
            }
            startActivity(intent)
            finish()
        }
    }

    private fun populateCareAdviceUi() {
        // 1. Resolve symptoms summary for subtitle
        val summaryText = customSymptomsSummary
            ?: assessmentResult?.reportedSymptomsSummary?.takeIf { it.isNotBlank() }
            ?: getString(R.string.symptom_sample_mild_fever_fatigue)

        binding.tvCareAdviceSubtitle.text = getString(R.string.care_advice_subtitle_format, summaryText)

        // 2. Resolve Care Advice data (either from specific condition or root assessment result)
        val defaultAdvice = SymptomAssessmentRepository.getInstance(this).createFallbackResult(
            age = 20,
            gender = "MALE",
            bodyRegion = summaryText,
            symptoms = listOf(summaryText),
            relatedSymptoms = emptyList(),
            insectBite = "NO"
        ).careAdvice

        val careAdvice: SymptomCareAdvice = specificCondition?.careAdvice?.takeIf {
            it.selfCare.isNotEmpty() || it.monitorSymptoms.isNotEmpty() || it.whenToSeekCare.isNotEmpty()
        } ?: assessmentResult?.careAdvice?.takeIf {
            it.selfCare.isNotEmpty() || it.monitorSymptoms.isNotEmpty() || it.whenToSeekCare.isNotEmpty()
        } ?: defaultAdvice

        val inflater = LayoutInflater.from(this)

        // 3. Populate Self-Care
        binding.llSelfCareList.removeAllViews()
        val selfCarePoints = if (careAdvice.selfCare.isNotEmpty()) {
            careAdvice.selfCare
        } else {
            defaultAdvice.selfCare
        }
        for (point in selfCarePoints) {
            val bulletBinding = ItemCareBulletBinding.inflate(inflater, binding.llSelfCareList, false)
            bulletBinding.tvBulletText.text = point
            binding.llSelfCareList.addView(bulletBinding.root)
        }

        // 4. Populate Monitor Symptoms
        binding.llMonitorSymptomsList.removeAllViews()
        val monitorPoints = if (careAdvice.monitorSymptoms.isNotEmpty()) {
            careAdvice.monitorSymptoms
        } else {
            defaultAdvice.monitorSymptoms
        }
        for (point in monitorPoints) {
            val bulletBinding = ItemCareBulletBinding.inflate(inflater, binding.llMonitorSymptomsList, false)
            bulletBinding.tvBulletText.text = point
            binding.llMonitorSymptomsList.addView(bulletBinding.root)
        }

        // 5. Populate When to Seek Medical Care
        binding.llWhenToSeekCareList.removeAllViews()
        val seekCarePoints = if (careAdvice.whenToSeekCare.isNotEmpty()) {
            careAdvice.whenToSeekCare
        } else {
            defaultAdvice.whenToSeekCare
        }
        for (point in seekCarePoints) {
            val seekBinding = ItemWhenToSeekCareBinding.inflate(inflater, binding.llWhenToSeekCareList, false)
            seekBinding.tvSeekText.text = point
            binding.llWhenToSeekCareList.addView(seekBinding.root)
        }
    }

    companion object {
        const val EXTRA_ASSESSMENT_RESULT = "extra_assessment_result"
        const val EXTRA_SPECIFIC_CONDITION = "extra_specific_condition"
        const val EXTRA_SYMPTOMS_SUMMARY = "extra_symptoms_summary"
    }
}

