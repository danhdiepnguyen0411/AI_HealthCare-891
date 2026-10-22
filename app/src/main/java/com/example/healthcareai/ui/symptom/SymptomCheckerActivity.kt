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
            val statusBarTop = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            val navBarBottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom

            if (statusBarTop > 0) {
                binding.topBar.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    topMargin = statusBarTop + 8
                }
            }
            if (navBarBottom > 0) {
                binding.btnStartAssessment.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    bottomMargin = navBarBottom + 24
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
