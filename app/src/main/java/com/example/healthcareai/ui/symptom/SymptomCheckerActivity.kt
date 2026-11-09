package com.example.healthcareai.ui.symptom

import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import com.example.healthcareai.R
import com.example.healthcareai.databinding.ActivitySymptomCheckerBinding
import com.example.healthcareai.ui.base.BaseActivity
import com.example.healthcareai.ui.base.applyHeaderTopMargin

class SymptomCheckerActivity : BaseActivity() {

    private lateinit var binding: ActivitySymptomCheckerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySymptomCheckerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWindowInsets()
        setupListeners()
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.rootSymptomCheckerLayout) { _, insets ->
            binding.topBar.applyHeaderTopMargin(insets)

            val navBarBottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            val density = resources.displayMetrics.density

            if (navBarBottom > 0) {
                binding.btnStartAssessment.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    bottomMargin = navBarBottom + (16 * density).toInt()
                }
            }
            insets
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnStartAssessment.setOnClickListener {
            startActivity(Intent(this, SymptomAssessmentActivity::class.java))
        }
    }
}
