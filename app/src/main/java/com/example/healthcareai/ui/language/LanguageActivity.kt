package com.example.healthcareai.ui.language

import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
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
    private var hasHandMovedToDone = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferenceHelper = PreferenceHelper.getInstance(this)
        initialSavedLangCode = preferenceHelper.selectedLanguageCode

        isFromSettings = intent.getBooleanExtra(EXTRA_IS_FROM_SETTINGS, false)

        if (!isFromSettings) {
            // Guarantee fresh onboarding state on every first-language entry
            hasHandMovedToDone = false
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
            binding.btnBack.setOnClickListener {
                handleBackPressed()
            }
            updateDoneButtonState(hasSelection = true, animate = false)
        } else {
            // First launch: hide back button completely, show subtitle
            binding.btnBack.visibility = View.GONE
            binding.tvSubtitle.visibility = View.VISIBLE
            updateDoneButtonState(hasSelection = false, animate = false)
        }

        binding.btnDone.setOnClickListener {
            if (!binding.btnDone.isEnabled) return@setOnClickListener
            onConfirmLanguage()
        }

        binding.ivFloatingHand.setOnClickListener {
            if (binding.btnDone.isEnabled) {
                binding.btnDone.performClick()
            }
        }
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
                it.showTapHint = false
            }
            if (resultList.none { it.isSelected }) {
                resultList.firstOrNull()?.isSelected = true
            }
            selectedLanguage = resultList.find { it.isSelected }
            binding.ivFloatingHand.visibility = View.GONE
        } else {
            // First time entry: NO language selected initially, show hand hint at position 4 (index 3)
            val targetIndex = if (resultList.size >= 4) 3 else 0
            resultList.forEachIndexed { index, item ->
                item.isSelected = false
                item.showTapHint = (index == targetIndex)
            }
            selectedLanguage = null
            binding.ivFloatingHand.visibility = View.GONE
        }

        adapter = LanguageAdapter(resultList) { selected, clickedRadioView ->
            selectedLanguage = selected
            updateDoneButtonState(hasSelection = true, animate = true)

            if (!isFromSettings) {
                binding.rootLayout.post {
                    if (isFinishing || isDestroyed) return@post
                    if (!hasHandMovedToDone) {
                        hasHandMovedToDone = true
                        animateHandToDone(clickedRadioView)
                    } else {
                        ensureHandAtDone()
                    }
                }
            }
        }

        binding.rvLanguages.layoutManager = LinearLayoutManager(this)
        binding.rvLanguages.adapter = adapter
    }

    private fun findSelectedRadioView(): View? {
        val selectedIndex = adapter.getSelectedPosition()
        if (selectedIndex >= 0) {
            val viewHolder = binding.rvLanguages.findViewHolderForAdapterPosition(selectedIndex)
                as? LanguageAdapter.LanguageViewHolder
            if (viewHolder != null) {
                return viewHolder.binding.ivRadio
            }
        }
        return null
    }

    private fun animateHandToDone(clickedRadioView: View? = null) {
        if (isFinishing || isDestroyed) return

        val floatingHand = binding.ivFloatingHand
        floatingHand.animate().cancel()

        val rootLoc = IntArray(2)
        val targetLoc = IntArray(2)
        binding.rootLayout.getLocationInWindow(rootLoc)
        binding.btnDone.getLocationInWindow(targetLoc)

        val density = resources.displayMetrics.density
        val handW = HAND_SIZE_DP * density
        val handH = HAND_SIZE_DP * density

        val hotspotRatioX = HAND_HOTSPOT_RATIO_X
        val hotspotRatioY = HAND_HOTSPOT_RATIO_Y

        val doneCenterX = (targetLoc[0] - rootLoc[0]) + binding.btnDone.width / 2f
        val doneCenterY = (targetLoc[1] - rootLoc[1]) + binding.btnDone.height / 2f

        val endX = doneCenterX - handW * hotspotRatioX
        val endY = doneCenterY - handH * hotspotRatioY

        val radioView = clickedRadioView ?: findSelectedRadioView()
        val startLoc = IntArray(2)
        val (startX, startY) = if (radioView != null && radioView.isAttachedToWindow) {
            radioView.getLocationInWindow(startLoc)
            val radioCenterX = (startLoc[0] - rootLoc[0]) + radioView.width / 2f
            val radioCenterY = (startLoc[1] - rootLoc[1]) + radioView.height / 2f
            Pair(radioCenterX - handW * hotspotRatioX, radioCenterY - handH * hotspotRatioY)
        } else {
            Pair(endX, endY + 250f * density)
        }

        floatingHand.x = startX
        floatingHand.y = startY
        floatingHand.visibility = View.VISIBLE
        floatingHand.load(R.drawable.hand_click)

        floatingHand.animate()
            .x(endX)
            .y(endY)
            .setDuration(650L)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                if (!isFinishing && !isDestroyed) {
                    floatingHand.x = endX
                    floatingHand.y = endY
                    floatingHand.visibility = View.VISIBLE
                }
            }
            .start()
    }

    private fun ensureHandAtDone() {
        if (isFinishing || isDestroyed) return

        val floatingHand = binding.ivFloatingHand
        val rootLoc = IntArray(2)
        val targetLoc = IntArray(2)

        binding.rootLayout.getLocationInWindow(rootLoc)
        binding.btnDone.getLocationInWindow(targetLoc)

        val density = resources.displayMetrics.density
        val handW = HAND_SIZE_DP * density
        val handH = HAND_SIZE_DP * density

        val hotspotRatioX = HAND_HOTSPOT_RATIO_X
        val hotspotRatioY = HAND_HOTSPOT_RATIO_Y

        val doneCenterX = (targetLoc[0] - rootLoc[0]) + binding.btnDone.width / 2f
        val doneCenterY = (targetLoc[1] - rootLoc[1]) + binding.btnDone.height / 2f

        val endX = doneCenterX - handW * hotspotRatioX
        val endY = doneCenterY - handH * hotspotRatioY

        floatingHand.animate().cancel()
        floatingHand.animate()
            .x(endX)
            .y(endY)
            .setDuration(250L)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                if (!isFinishing && !isDestroyed) {
                    floatingHand.x = endX
                    floatingHand.y = endY
                    floatingHand.visibility = View.VISIBLE
                }
            }
            .start()
        floatingHand.visibility = View.VISIBLE
        floatingHand.load(R.drawable.hand_click)
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

    override fun onDestroy() {
        super.onDestroy()
        binding.ivFloatingHand.animate().cancel()
    }

    companion object {
        const val EXTRA_IS_FROM_SETTINGS = "extra_is_from_settings"
        private const val HAND_SIZE_DP = 80f
        private const val HAND_HOTSPOT_RATIO_X = 72.5f / 150f
        private const val HAND_HOTSPOT_RATIO_Y = 56.0f / 150f
    }
}
