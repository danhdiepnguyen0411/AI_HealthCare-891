package com.example.healthcareai.ui.language

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.activity.OnBackPressedCallback
import androidx.core.text.TextUtilsCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
    private var hasSelectedLanguage = false
    private var isConfirmed = false
    private var isHandGifLoaded = false

    private fun loadHandGif() {
        if (isHandGifLoaded) {
            (binding.ivFloatingHand.drawable as? android.graphics.drawable.Animatable)?.let {
                if (!it.isRunning) it.start()
            }
            return
        }
        isHandGifLoaded = true
        binding.ivFloatingHand.load(R.drawable.gif_hand_click) {
            listener(
                onSuccess = { _, result ->
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                        (result.drawable as? android.graphics.drawable.AnimatedImageDrawable)?.repeatCount =
                            android.graphics.drawable.AnimatedImageDrawable.REPEAT_INFINITE
                    }
                    (result.drawable as? android.graphics.drawable.Animatable)?.start()
                }
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferenceHelper = PreferenceHelper.getInstance(this)
        initialSavedLangCode = preferenceHelper.selectedLanguageCode

        isFromSettings = intent.getBooleanExtra(EXTRA_IS_FROM_SETTINGS, false)

        if (!isFromSettings) {
            // Guarantee fresh onboarding state on every first-language entry
            hasSelectedLanguage = false
            selectedLanguage = null
            preferenceHelper.isFirstLanguageSet = false
            preferenceHelper.clearLanguageIfIntroNotCompleted(this)
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
            val navBarBottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            val density = resources.displayMetrics.density

            val targetPaddingTop = if (statusBarTop > 0) {
                statusBarTop + (16 * density).toInt()
            } else {
                (24 * density).toInt()
            }
            binding.topBar.setPadding(
                (16 * density).toInt(),
                targetPaddingTop,
                (16 * density).toInt(),
                (12 * density).toInt()
            )

            if (navBarBottom > 0) {
                binding.rvLanguages.setPadding(
                    0,
                    (10 * density).toInt(),
                    0,
                    navBarBottom + (24 * density).toInt()
                )
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
            binding.ivFloatingHand.visibility = View.GONE
            binding.btnBack.setOnClickListener {
                handleBackPressed()
            }
            updateDoneButtonState(hasSelection = true, animate = false)
        } else {
            // First launch: hide back button, show subtitle, countdown & done initially hidden until language selection
            binding.btnBack.visibility = View.GONE
            binding.tvSubtitle.visibility = View.VISIBLE
            binding.btnDone.visibility = View.GONE
            binding.btnDone.alpha = 0f
            binding.btnDone.isEnabled = false
            binding.btnDone.isClickable = false
            binding.viewCountdown.visibility = View.GONE
            loadHandGif()
        }

        binding.btnDone.setOnClickListener {
            if (!binding.btnDone.isEnabled && isFromSettings) return@setOnClickListener
            onConfirmLanguage()
        }

        binding.viewCountdown.isClickable = false
        binding.viewCountdown.setOnClickListener(null)

        binding.ivFloatingHand.setOnClickListener {
            if (!hasSelectedLanguage) {
                selectLanguageAtPosition(3)
            } else if (binding.btnDone.visibility == View.VISIBLE && binding.btnDone.isEnabled) {
                onConfirmLanguage()
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
            }
            if (resultList.none { it.isSelected }) {
                resultList.firstOrNull()?.isSelected = true
            }
            selectedLanguage = resultList.find { it.isSelected }
        } else {
            // First time entry: do not preselect any language initially
            resultList.forEach {
                it.isSelected = false
            }
            selectedLanguage = null
        }

        val initialDir = if (LocaleHelper.isRtl(this)) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR
        adapter = LanguageAdapter(resultList, initialDir) { selected ->
            handleLanguageSelected(selected)
        }

        if (!isFromSettings) {
            adapter.onItemBound = { position, itemView ->
                if (position == 3 && !hasSelectedLanguage) {
                    itemView.post {
                        if (!hasSelectedLanguage && !isFinishing && !isDestroyed) {
                            positionHandOnItem4()
                        }
                    }
                }
            }
        }

        binding.rvLanguages.layoutManager = LinearLayoutManager(this)
        binding.rvLanguages.adapter = adapter

        if (!isFromSettings) {
            binding.rvLanguages.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                if (!hasSelectedLanguage && !isFromSettings && !isFinishing && !isDestroyed) {
                    positionHandOnItem4()
                }
            }

            binding.rvLanguages.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (!hasSelectedLanguage && !isFromSettings) {
                        positionHandOnItem4()
                    }
                }
            })

            binding.rvLanguages.post {
                if (!hasSelectedLanguage && !isFromSettings) {
                    positionHandOnItem4()
                }
            }
            binding.rvLanguages.postDelayed({
                if (!hasSelectedLanguage && !isFromSettings) {
                    positionHandOnItem4()
                }
            }, 50)
            binding.rvLanguages.postDelayed({
                if (!hasSelectedLanguage && !isFromSettings) {
                    positionHandOnItem4()
                }
            }, 150)
            binding.rvLanguages.postDelayed({
                if (!hasSelectedLanguage && !isFromSettings) {
                    positionHandOnItem4()
                }
            }, 300)
            binding.rvLanguages.postDelayed({
                if (!hasSelectedLanguage && !isFromSettings) {
                    positionHandOnItem4()
                }
            }, 600)
        }

        selectedLanguage?.let {
            applyPreviewLanguage(it.code)
        } ?: run {
            updateHeaderForLanguage(initialSavedLangCode)
        }
    }

    private fun selectLanguageAtPosition(position: Int) {
        if (position in 0 until adapter.itemCount) {
            adapter.selectPosition(position)
            val selected = adapter.getSelectedLanguage() ?: return
            handleLanguageSelected(selected)
        }
    }

    private fun handleLanguageSelected(selected: LanguageModel) {
        hasSelectedLanguage = true
        selectedLanguage = selected
        applyPreviewLanguage(selected.code)

        if (!isFromSettings) {
            binding.viewCountdown.cancelCountdown()
            binding.btnDone.visibility = View.GONE
            binding.btnDone.alpha = 0f
            binding.btnDone.isEnabled = false
            binding.btnDone.isClickable = false
            binding.viewCountdown.visibility = View.VISIBLE
            binding.viewCountdown.alpha = 1.0f

            binding.viewCountdown.startCountdown(
                totalDurationMs = 3000L,
                onFinished = {
                    if (!isFinishing && !isDestroyed) {
                        showDoneButtonAfterCountdown()
                    }
                }
            )

            animateHandToDone()
        } else {
            updateDoneButtonState(hasSelection = true, animate = true)
        }
    }

    private fun showDoneButtonAfterCountdown() {
        if (isFinishing || isDestroyed) return

        binding.viewCountdown.cancelCountdown()
        binding.viewCountdown.animate()
            .alpha(0f)
            .setDuration(150L)
            .withEndAction {
                if (isFinishing || isDestroyed) return@withEndAction
                binding.viewCountdown.visibility = View.GONE
            }
            .start()

        binding.btnDone.visibility = View.VISIBLE
        binding.btnDone.alpha = 0f
        binding.btnDone.isEnabled = true
        binding.btnDone.isClickable = true
        binding.btnDone.animate()
            .alpha(1.0f)
            .setDuration(200L)
            .start()

        binding.rootLayout.post {
            if (!isFinishing && !isDestroyed && hasSelectedLanguage) {
                val (endX, endY) = getTargetDoneEndCoordinates()
                binding.ivFloatingHand.animate()
                    .x(endX)
                    .y(endY)
                    .setDuration(150L)
                    .start()
            }
        }
    }

    private fun findRadioViewForPosition(position: Int): View? {
        if (position == 3) {
            adapter.item4RadioView?.let {
                if (it.isAttachedToWindow && it.width > 0) return it
            }
        }
        val lm = binding.rvLanguages.layoutManager as? LinearLayoutManager
        val itemView = lm?.findViewByPosition(position)
        val radio = itemView?.findViewById<View>(R.id.ivRadio)
        if (radio != null && radio.isAttachedToWindow && radio.width > 0) {
            return radio
        }
        val vh = binding.rvLanguages.findViewHolderForAdapterPosition(position) as? LanguageAdapter.LanguageViewHolder
        val vhRadio = vh?.binding?.ivRadio
        if (vhRadio != null && vhRadio.isAttachedToWindow && vhRadio.width > 0) {
            return vhRadio
        }
        return null
    }

    private fun findItem4RadioView(): View? = findRadioViewForPosition(3)

    private fun positionHandOnItem4(): Boolean {
        if (isFinishing || isDestroyed || isFromSettings || hasSelectedLanguage) return false
        if (binding.rootLayout.width == 0 || binding.rootLayout.height == 0) return false

        val rootLoc = IntArray(2)
        binding.rootLayout.getLocationInWindow(rootLoc)

        var radioCenterX = 0f
        var radioCenterY = 0f
        var foundPosition = false

        val targetView = findItem4RadioView()
        if (targetView != null && targetView.isAttachedToWindow && targetView.width > 0) {
            val targetLoc = IntArray(2)
            targetView.getLocationInWindow(targetLoc)
            radioCenterX = (targetLoc[0] - rootLoc[0]) + targetView.width / 2f
            radioCenterY = (targetLoc[1] - rootLoc[1]) + targetView.height / 2f
            foundPosition = true
        } else {
            val lm = binding.rvLanguages.layoutManager as? LinearLayoutManager
            val firstChild = lm?.getChildAt(0)
            if (firstChild != null) {
                val firstPos = lm.getPosition(firstChild)
                val radioInFirst = firstChild.findViewById<View>(R.id.ivRadio)
                if (firstPos != RecyclerView.NO_POSITION && radioInFirst != null && radioInFirst.width > 0) {
                    val firstRadioLoc = IntArray(2)
                    radioInFirst.getLocationInWindow(firstRadioLoc)
                    val lp = firstChild.layoutParams as? ViewGroup.MarginLayoutParams
                    val itemStride = firstChild.height + (lp?.topMargin ?: 0) + (lp?.bottomMargin ?: 0)
                    radioCenterX = (firstRadioLoc[0] - rootLoc[0]) + radioInFirst.width / 2f
                    radioCenterY = (firstRadioLoc[1] - rootLoc[1]) + radioInFirst.height / 2f + (3 - firstPos) * itemStride
                    foundPosition = true
                }
            }
        }

        if (!foundPosition || radioCenterX <= 0f) {
            return false
        }

        val density = resources.displayMetrics.density
        val handW = HAND_SIZE_DP * density
        val handH = HAND_SIZE_DP * density

        val startX = radioCenterX - handW * HAND_HOTSPOT_RATIO_X
        val startY = radioCenterY - handH * HAND_HOTSPOT_RATIO_Y

        binding.topBar.outlineProvider = null
        binding.topBar.elevation = 10f * density

        val isUnderHeader = radioCenterY < binding.topBar.bottom
        binding.ivFloatingHand.apply {
            animate().cancel()
            elevation = 5f * density
            x = startX
            y = startY
            visibility = if (startY + handH < 0f) View.INVISIBLE else View.VISIBLE
            isClickable = !isUnderHeader
            isFocusable = !isUnderHeader
        }
        loadHandGif()
        return true
    }

    private fun getTargetDoneEndCoordinates(): Pair<Float, Float> {
        val density = resources.displayMetrics.density
        val handW = HAND_SIZE_DP * density
        val handH = HAND_SIZE_DP * density

        val targetView = if (binding.viewCountdown.width > 0 && binding.viewCountdown.visibility == View.VISIBLE) {
            binding.viewCountdown
        } else if (binding.btnDone.width > 0 && binding.btnDone.visibility == View.VISIBLE) {
            binding.btnDone
        } else {
            binding.topBar
        }

        val rootLoc = IntArray(2)
        val targetLoc = IntArray(2)
        binding.rootLayout.getLocationInWindow(rootLoc)
        targetView.getLocationInWindow(targetLoc)

        val targetCenterX = if (targetView == binding.topBar) {
            val isRtl = binding.rootLayout.layoutDirection == View.LAYOUT_DIRECTION_RTL
            if (isRtl) {
                (targetLoc[0] - rootLoc[0]) + 35f * density
            } else {
                (targetLoc[0] - rootLoc[0]) + targetView.width - 35f * density
            }
        } else {
            (targetLoc[0] - rootLoc[0]) + targetView.width / 2f
        }

        val targetCenterY = (targetLoc[1] - rootLoc[1]) + targetView.height / 2f

        val endX = targetCenterX - handW * HAND_HOTSPOT_RATIO_X
        val endY = targetCenterY - handH * HAND_HOTSPOT_RATIO_Y
        return Pair(endX, endY)
    }

    private fun animateHandToDone() {
        if (isFinishing || isDestroyed) return

        val density = resources.displayMetrics.density
        val floatingHand = binding.ivFloatingHand
        floatingHand.animate().cancel()
        floatingHand.elevation = 100f * density
        floatingHand.isClickable = true
        floatingHand.isFocusable = true

        if (floatingHand.visibility != View.VISIBLE || floatingHand.x <= 0f || floatingHand.y < binding.topBar.bottom) {
            val selectedPos = adapter.getSelectedPosition().takeIf { it >= 0 } ?: 3
            val selectedRadio = findRadioViewForPosition(selectedPos)
            if (selectedRadio != null && selectedRadio.isAttachedToWindow && selectedRadio.width > 0) {
                val rootLoc = IntArray(2)
                val targetLoc = IntArray(2)
                binding.rootLayout.getLocationInWindow(rootLoc)
                selectedRadio.getLocationInWindow(targetLoc)
                val handW = HAND_SIZE_DP * density
                val handH = HAND_SIZE_DP * density
                val radioCenterX = (targetLoc[0] - rootLoc[0]) + selectedRadio.width / 2f
                val radioCenterY = (targetLoc[1] - rootLoc[1]) + selectedRadio.height / 2f
                floatingHand.x = radioCenterX - handW * HAND_HOTSPOT_RATIO_X
                floatingHand.y = radioCenterY - handH * HAND_HOTSPOT_RATIO_Y
            }
        }

        loadHandGif()
        floatingHand.visibility = View.VISIBLE

        binding.rootLayout.post {
            if (isFinishing || isDestroyed) return@post
            val (endX, endY) = getTargetDoneEndCoordinates()
            floatingHand.animate()
                .x(endX)
                .y(endY)
                .setDuration(600L)
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
    }

    private fun applyPreviewLanguage(languageCode: String) {
        val locale = LocaleHelper.getLocale(languageCode)
        val isRtl = TextUtilsCompat.getLayoutDirectionFromLocale(locale) == ViewCompat.LAYOUT_DIRECTION_RTL
        val dir = if (isRtl) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR

        // Temporarily update resources for localized string lookups
        LocaleHelper.updateResources(this, languageCode)

        // Dynamically apply layout direction across window and all container hierarchies
        window.decorView.layoutDirection = dir
        findViewById<View>(android.R.id.content)?.layoutDirection = dir
        binding.rootLayout.layoutDirection = dir
        binding.topBar.layoutDirection = dir
        binding.rvLanguages.layoutDirection = dir
        binding.btnBack.layoutDirection = dir
        binding.ivBack.layoutDirection = dir

        binding.ivBack.drawable?.mutate()?.let { drawable ->
            androidx.core.graphics.drawable.DrawableCompat.setLayoutDirection(drawable, dir)
            drawable.invalidateSelf()
        }
        binding.ivBack.invalidate()

        if (::adapter.isInitialized) {
            adapter.updateLayoutDirection(dir)
        }

        updateHeaderForLanguage(languageCode)

        binding.topBar.requestLayout()
        binding.rootLayout.requestLayout()
    }

    private fun updateHeaderForLanguage(languageCode: String) {
        binding.tvTitle.text = LocaleHelper.getLocalizedString(this, languageCode, R.string.title_language)
        if (binding.tvSubtitle.visibility == View.VISIBLE) {
            binding.tvSubtitle.text = LocaleHelper.getLocalizedString(this, languageCode, R.string.subtitle_select_language)
        }
        binding.tvBtnDone.text = LocaleHelper.getLocalizedString(this, languageCode, R.string.btn_done)
    }

    private fun onConfirmLanguage() {
        val lang = selectedLanguage ?: return
        isConfirmed = true

        preferenceHelper.selectedLanguageCode = lang.code
        preferenceHelper.isFirstLanguageSet = true

        LocaleHelper.setLocale(this, lang.code)
        ShortcutHelper.setupUninstallShortcut(this, lang.code)

        if (isFromSettings) {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra(MainActivity.EXTRA_TARGET_TAB, MainActivity.TAB_HOME)
                putExtra(MainActivity.EXTRA_SKIP_NOTI_POPUP, true)
            }
            startActivity(intent)
            finish()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                overrideActivityTransition(Activity.OVERRIDE_TRANSITION_CLOSE, 0, 0)
                overrideActivityTransition(Activity.OVERRIDE_TRANSITION_OPEN, 0, 0)
            } else {
                @Suppress("DEPRECATION")
                overridePendingTransition(0, 0)
            }
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
            LocaleHelper.setLocale(this, initialSavedLangCode)
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
        (binding.ivFloatingHand.drawable as? android.graphics.drawable.Animatable)?.let {
            if (!it.isRunning) it.start()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.viewCountdown.cancelCountdown()
        binding.ivFloatingHand.animate().cancel()
        (binding.ivFloatingHand.drawable as? android.graphics.drawable.Animatable)?.stop()
        if (!isConfirmed && isFromSettings) {
            LocaleHelper.setLocale(this, initialSavedLangCode)
        }
    }

    companion object {
        const val EXTRA_IS_FROM_SETTINGS = "extra_is_from_settings"
        private const val HAND_SIZE_DP = 48f
        private const val HAND_HOTSPOT_RATIO_X = 478f / 800f
        private const val HAND_HOTSPOT_RATIO_Y = 169f / 800f
    }
}
