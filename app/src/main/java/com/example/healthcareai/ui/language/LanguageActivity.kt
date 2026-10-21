package com.example.healthcareai.ui.language

import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.healthcareai.data.local.PreferenceHelper
import com.example.healthcareai.data.model.LanguageModel
import com.example.healthcareai.databinding.ActivityLanguageBinding
import com.example.healthcareai.ui.base.BaseActivity
import com.example.healthcareai.ui.intro.IntroActivity
import com.example.healthcareai.ui.main.MainActivity
import com.example.healthcareai.util.LocaleHelper
import com.example.healthcareai.util.ShortcutHelper

class LanguageActivity : BaseActivity() {

    private lateinit var binding: ActivityLanguageBinding
    private lateinit var preferenceHelper: PreferenceHelper
    private lateinit var adapter: LanguageAdapter

    private var isFromSettings = false
    private var initialSavedLangCode = "en"
    private var selectedLanguage: LanguageModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferenceHelper = PreferenceHelper.getInstance(this)
        initialSavedLangCode = preferenceHelper.selectedLanguageCode

        isFromSettings = intent.getBooleanExtra(EXTRA_IS_FROM_SETTINGS, false)

        binding = ActivityLanguageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupLanguageList()
        setupBackPress()
    }

    private fun setupToolbar() {
        if (isFromSettings) {
            binding.btnBack.visibility = android.view.View.VISIBLE
            binding.btnBack.setOnClickListener {
                handleBackPressed()
            }
            binding.btnDone.alpha = 1.0f
            binding.btnDone.isEnabled = true
        } else {
            binding.btnBack.visibility = android.view.View.GONE
            binding.btnDone.alpha = 0.35f
            binding.btnDone.isEnabled = false
        }

        binding.btnDone.setOnClickListener {
            onConfirmLanguage()
        }
    }

    private fun setupLanguageList() {
        val deviceLangCode = LocaleHelper.getDeviceLanguageCode()
        val allCodes = LocaleHelper.SUPPORTED_LANGUAGE_CODES.toMutableList()

        // Remove device language from list first to place it at position 4 (index 3)
        allCodes.remove(deviceLangCode)

        val targetList = mutableListOf<String>()
        val insertIndex = minOf(3, allCodes.size)

        for (i in 0 until insertIndex) {
            targetList.add(allCodes[i])
        }
        // Place device system language at index 3 (position 4)
        targetList.add(deviceLangCode)

        for (i in insertIndex until allCodes.size) {
            targetList.add(allCodes[i])
        }

        val languageModels = targetList.map { code ->
            val isDevice = (code == deviceLangCode)
            val isSelected = if (isFromSettings) {
                code == initialSavedLangCode
            } else {
                false // Initial state: no item selected on first launch
            }
            LanguageModel(
                code = code,
                name = LocaleHelper.getLanguageNameByCode(code),
                flagRes = LocaleHelper.getFlagResByCode(code),
                isSelected = isSelected,
                isDeviceLanguage = isDevice
            )
        }.toMutableList()

        if (isFromSettings) {
            selectedLanguage = languageModels.find { it.isSelected }
        }

        adapter = LanguageAdapter(languageModels) { item ->
            selectedLanguage = item
            binding.btnDone.alpha = 1.0f
            binding.btnDone.isEnabled = true
        }

        binding.rvLanguages.layoutManager = LinearLayoutManager(this)
        binding.rvLanguages.adapter = adapter
    }

    private fun onConfirmLanguage() {
        val lang = selectedLanguage ?: return

        preferenceHelper.selectedLanguageCode = lang.code
        preferenceHelper.isFirstLanguageSet = true

        LocaleHelper.setLocale(this, lang.code)
        ShortcutHelper.setupUninstallShortcut(this, lang.code)

        if (isFromSettings) {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(MainActivity.EXTRA_TARGET_TAB, MainActivity.TAB_SETTINGS)
            }
            startActivity(intent)
            finish()
        } else {
            startActivity(Intent(this, IntroActivity::class.java))
            finish()
        }
    }

    private fun setupBackPress() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleBackPressed()
            }
        })
    }

    private fun handleBackPressed() {
        if (isFromSettings) {
            // Restore previous language and finish
            LocaleHelper.setLocale(this, initialSavedLangCode)
            finish()
        } else {
            // First Launch exit: clear onboarding state and exit cleanly
            preferenceHelper.clearLanguageIfIntroNotCompleted(this)
            finishAffinity()
        }
    }

    companion object {
        const val EXTRA_IS_FROM_SETTINGS = "extra_is_from_settings"
    }
}
