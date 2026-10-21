package com.example.healthcareai.ui.uninstall

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.core.widget.doAfterTextChanged
import com.example.healthcareai.R
import com.example.healthcareai.data.local.PreferenceHelper
import com.example.healthcareai.databinding.ActivityUninstallReasonBinding
import com.example.healthcareai.ui.base.BaseActivity
import com.example.healthcareai.ui.intro.IntroActivity
import com.example.healthcareai.ui.language.LanguageActivity
import com.example.healthcareai.ui.main.MainActivity

class UninstallReasonActivity : BaseActivity() {

    private lateinit var binding: ActivityUninstallReasonBinding
    private var selectedOptionIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUninstallReasonBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initViews()
    }

    private fun initViews() {
        binding.btnBack.setOnClickListener { goToHome() }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                goToHome()
            }
        })

        binding.btnCancel.setOnClickListener { goToHome() }

        binding.cardOption1.setOnClickListener { selectOption(1) }
        binding.cardOption2.setOnClickListener { selectOption(2) }
        binding.cardOption3.setOnClickListener { selectOption(3) }
        binding.cardOption4.setOnClickListener { selectOption(4) }

        binding.etCustomFeedback.doAfterTextChanged {
            updateConfirmButtonState()
        }

        binding.btnConfirmUninstall.setOnClickListener {
            openAppDetailsSettings()
        }
    }

    private fun selectOption(index: Int) {
        selectedOptionIndex = index

        val cards = arrayOf(binding.cardOption1, binding.cardOption2, binding.cardOption3, binding.cardOption4)
        val checks = arrayOf(binding.ivCheck1, binding.ivCheck2, binding.ivCheck3, binding.ivCheck4)

        for (i in 0 until 4) {
            val isSelected = (i + 1 == index)
            cards[i].setBackgroundResource(
                if (isSelected) R.drawable.bg_uninstall_card_selected else R.drawable.bg_uninstall_card_unselected
            )
            checks[i].setImageResource(
                if (isSelected) R.drawable.ic_radio_selected else R.drawable.ic_radio_unselected
            )
        }

        if (index == 4) {
            binding.etCustomFeedback.visibility = View.VISIBLE
            binding.etCustomFeedback.requestFocus()
        } else {
            binding.etCustomFeedback.visibility = View.GONE
        }

        updateConfirmButtonState()
    }

    private fun isReasonValid(): Boolean {
        return when (selectedOptionIndex) {
            1, 2, 3 -> true
            4 -> binding.etCustomFeedback.text.toString().trim().isNotEmpty()
            else -> false
        }
    }

    private fun updateConfirmButtonState() {
        val isValid = isReasonValid()
        binding.btnConfirmUninstall.isEnabled = isValid
        binding.btnConfirmUninstall.alpha = if (isValid) 1.0f else 0.4f
    }

    private fun openAppDetailsSettings() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (_: Exception) {
            try {
                val uninstallIntent = Intent(Intent.ACTION_DELETE).apply {
                    data = Uri.parse("package:$packageName")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(uninstallIntent)
            } catch (_: Exception) {}
        }
    }

    private fun goToHome() {
        val prefs = PreferenceHelper.getInstance(this)
        val targetActivity = when {
            !prefs.isFirstLanguageSet -> LanguageActivity::class.java
            !prefs.isIntroCompleted -> IntroActivity::class.java
            else -> MainActivity::class.java
        }

        val intent = Intent(this, targetActivity).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("from_uninstall", true)
            putExtra(MainActivity.EXTRA_TARGET_TAB, MainActivity.TAB_HOME)
            putExtra(MainActivity.EXTRA_SKIP_NOTI_POPUP, true)
            putExtra(MainActivity.EXTRA_SKIP_DAILY_REWARD, true)
        }
        startActivity(intent)
        finish()
    }
}
