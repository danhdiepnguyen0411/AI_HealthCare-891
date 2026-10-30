package com.example.healthcareai.ui.language

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.healthcareai.R
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

        if (!isFromSettings) {
            // Guarantee fresh onboarding state on every first-language entry
            selectedLanguage = null
            preferenceHelper.isFirstLanguageSet = false
        }

        binding = ActivityLanguageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWindowInsets()
        setupToolbar()
        setupLanguageList()
        setupBackPress()
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.rootLayout) { _, insets ->
            val statusBarTop = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            val density = resources.displayMetrics.density
            val targetTopMargin = if (statusBarTop > 0) {
                statusBarTop + (8 * density).toInt()
            } else {
                (36 * density).toInt()
            }
            binding.topBar.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                if (topMargin != targetTopMargin) {
                    topMargin = targetTopMargin
                }
            }
            insets
        }
        ViewCompat.requestApplyInsets(binding.rootLayout)
    }

    private fun setupToolbar() {
        if (isFromSettings) {
            binding.btnBack.visibility = View.VISIBLE
            binding.tvSubtitle.visibility = View.GONE
            binding.viewCountdown.visibility = View.GONE
            binding.btnDone.visibility = View.VISIBLE
            binding.btnBack.setOnClickListener {
                handleBackPressed()
            }
            updateDoneButtonState(hasSelection = true, animate = false)
        } else {
            // First launch: hide back button, show subtitle, countdown 5s before showing Done
            binding.btnBack.visibility = View.GONE
            binding.tvSubtitle.visibility = View.VISIBLE
            binding.btnDone.visibility = View.GONE
            binding.btnDone.alpha = 0f
            binding.btnDone.isEnabled = false
            binding.btnDone.isClickable = false
            startFirstLanguageCountdown()
        }

        binding.btnDone.setOnClickListener {
            if (!binding.btnDone.isEnabled) return@setOnClickListener
            onConfirmLanguage()
        }
    }

    private fun startFirstLanguageCountdown() {
        binding.viewCountdown.visibility = View.VISIBLE
        binding.btnDone.visibility = View.GONE
        binding.viewCountdown.startCountdown(
            totalDurationMs = 5000L,
            onFinished = {
                showDoneButtonAfterCountdown()
            }
        )
    }

    private fun showDoneButtonAfterCountdown() {
        if (isFinishing || isDestroyed) return

        binding.viewCountdown.animate()
            .alpha(0f)
            .setDuration(200L)
            .withEndAction {
                if (isFinishing || isDestroyed) return@withEndAction
                binding.viewCountdown.visibility = View.GONE
                binding.btnDone.alpha = 0f
                binding.btnDone.visibility = View.VISIBLE
                val hasSelection = (selectedLanguage != null)
                binding.btnDone.isEnabled = hasSelection
                binding.btnDone.isClickable = hasSelection
                binding.btnDone.animate()
                    .alpha(if (hasSelection) 1.0f else 0.35f)
                    .setDuration(250L)
                    .start()
            }
            .start()
    }

    private fun updateDoneButtonState(hasSelection: Boolean, animate: Boolean = false) {
        binding.btnDone.isEnabled = hasSelection
        binding.btnDone.isClickable = hasSelection
        val targetAlpha = if (hasSelection) 1.0f else 0.35f
        if (animate) {
            binding.btnDone.animate().alpha(targetAlpha).setDuration(200L).start()
        } else {
            binding.btnDone.alpha = targetAlpha
        }
    }

    private fun setupLanguageList() {
        val deviceLangCode = LocaleHelper.getDeviceLanguageCode()
        val allCodes = LocaleHelper.SUPPORTED_LANGUAGE_CODES.toMutableList()

        val allLanguageModels = allCodes.map { code ->
            LanguageModel(
                code = code,
                name = LocaleHelper.getLanguageNameByCode(code),
                nativeName = LocaleHelper.getNativeNameByCode(code),
                flagRes = LocaleHelper.getFlagResByCode(code),
                isSelected = false,
                isDeviceLanguage = (code == deviceLangCode)
            )
        }

        val map = allLanguageModels.associateBy { it.code }.toMutableMap()
        val resultList = mutableListOf<LanguageModel>()

        // 1. Position 1, 2, 3: Top candidate languages (excluding device language)
        val topCandidates = listOf("hi", "pt_BR", "es", "en", "vi", "fr")
        val top3Codes = topCandidates.filter { it != deviceLangCode }.take(3)

        for (code in top3Codes) {
            map.remove(code)?.let { resultList.add(it) }
        }

        // 2. Position 4 (index 3): ALWAYS the device language!
        val matchedDeviceItem = map.remove(deviceLangCode) ?: allLanguageModels.find { it.code == deviceLangCode }
        matchedDeviceItem?.let {
            resultList.add(it)
        }

        // 3. Position 5+: Remaining languages
        val remainingOrder = listOf(
            "en", "vi", "fr", "id", "ko", "ja", "de", "pt_PT", "ar", "bn", "ru", "tr", "zh_CN", "zh_TW"
        )
        for (code in remainingOrder) {
            map.remove(code)?.let { resultList.add(it) }
        }
        for (item in map.values) {
            resultList.add(item)
        }

        if (isFromSettings) {
            // Select currently saved language
            resultList.forEach {
                it.isSelected = (it.code == initialSavedLangCode)
            }
            if (resultList.none { it.isSelected }) {
                resultList.firstOrNull()?.isSelected = true
            }
            selectedLanguage = resultList.find { it.isSelected }
        } else {
            // First time entry: preselect device language at position 4 (index 3)
            val targetIndex = if (resultList.size >= 4) 3 else 0
            resultList.forEachIndexed { index, item ->
                item.isSelected = (index == targetIndex)
            }
            selectedLanguage = resultList.find { it.isSelected }
        }

        adapter = LanguageAdapter(resultList) { selected ->
            selectedLanguage = selected
            if (binding.btnDone.visibility == View.VISIBLE) {
                updateDoneButtonState(hasSelection = true, animate = true)
            }
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
        binding.viewCountdown.cancelCountdown()
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

    override fun onPause() {
        super.onPause()
        binding.viewCountdown.pauseCountdown()
    }

    override fun onResume() {
        super.onResume()
        binding.viewCountdown.resumeCountdown()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.viewCountdown.cancelCountdown()
    }

    companion object {
        const val EXTRA_IS_FROM_SETTINGS = "extra_is_from_settings"
    }
}
