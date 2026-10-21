package com.example.healthcareai.ui.uninstall

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
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
    private var selectedOptionIndex: Int = -1

    private var lastImeInsetsBottom = 0
    private var lastGlobalImeBottom = 0
    private var wasKeyboardOpen = false
    private var globalLayoutListener: android.view.ViewTreeObserver.OnGlobalLayoutListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUninstallReasonBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initViews()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initViews() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })

        binding.cardOption1.setOnClickListener { selectOption(1) }
        binding.cardOption2.setOnClickListener { selectOption(2) }
        binding.cardOption3.setOnClickListener { selectOption(3) }
        binding.cardOption4.setOnClickListener { selectOption(4) }

        binding.btnDontUninstall.setOnClickListener {
            goToHome()
        }

        binding.btnStillWantUninstall.setOnClickListener {
            if (isUninstallReasonValid()) {
                openAppDetailsSettings()
            }
        }

        // Re-evaluate button state when user types in Option 4 feedback box
        binding.etCustomFeedback.doAfterTextChanged {
            if (selectedOptionIndex == 4) {
                updateStillUninstallButtonState()
            }
        }

        // Configure Done action on keyboard
        binding.etCustomFeedback.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                hideKeyboard()
                binding.etCustomFeedback.clearFocus()
                true
            } else {
                false
            }
        }

        binding.etCustomFeedback.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && selectedOptionIndex == 4) {
                binding.nestedScrollView.post { scrollToOthersCard() }
                binding.nestedScrollView.postDelayed({ scrollToOthersCard() }, 150)
                binding.nestedScrollView.postDelayed({ scrollToOthersCard() }, 300)
            }
        }

        binding.etCustomFeedback.setOnClickListener {
            if (selectedOptionIndex == 4) {
                binding.nestedScrollView.post { scrollToOthersCard() }
                binding.nestedScrollView.postDelayed({ scrollToOthersCard() }, 150)
                binding.nestedScrollView.postDelayed({ scrollToOthersCard() }, 300)
            }
        }

        binding.etCustomFeedback.setOnTouchListener { v, event ->
            if (v.hasFocus()) {
                v.parent.requestDisallowInterceptTouchEvent(true)
                if (event.action and MotionEvent.ACTION_MASK == MotionEvent.ACTION_UP) {
                    v.parent.requestDisallowInterceptTouchEvent(false)
                }
            }
            false
        }

        setupKeyboardInsetsListener()
        updateStillUninstallButtonState()
    }

    private fun selectOption(index: Int) {
        selectedOptionIndex = index

        val cards = arrayOf(
            binding.cardOption1,
            binding.cardOption2,
            binding.cardOption3,
            binding.cardOption4
        )
        val checks = arrayOf(
            binding.ivCheck1,
            binding.ivCheck2,
            binding.ivCheck3,
            binding.ivCheck4
        )
        val textViews = arrayOf(
            binding.tvOption1,
            binding.tvOption2,
            binding.tvOption3,
            binding.tvOption4
        )

        val unselectedTextColor = ContextCompat.getColor(this, R.color.color_text)

        for (i in 0 until 4) {
            val isSelected = (i + 1 == index)
            cards[i].setBackgroundResource(
                if (isSelected) R.drawable.bg_uninstall_card_selected else R.drawable.bg_uninstall_card_unselected
            )
            checks[i].setImageResource(
                if (isSelected) R.drawable.sellect_lang else R.drawable.unsellect_lang
            )
            textViews[i].setTextColor(
                if (isSelected) Color.WHITE else unselectedTextColor
            )
        }

        if (index == 4) {
            binding.etCustomFeedback.visibility = View.VISIBLE
            binding.etCustomFeedback.post {
                binding.etCustomFeedback.requestFocus()
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                imm?.showSoftInput(binding.etCustomFeedback, InputMethodManager.SHOW_IMPLICIT)
                scrollToOthersCard()
            }
            binding.nestedScrollView.postDelayed({ scrollToOthersCard() }, 150)
            binding.nestedScrollView.postDelayed({ scrollToOthersCard() }, 300)
            binding.nestedScrollView.postDelayed({ scrollToOthersCard() }, 500)
        } else {
            binding.etCustomFeedback.visibility = View.GONE
            hideKeyboard()
        }

        updateStillUninstallButtonState()
    }

    private fun isUninstallReasonValid(): Boolean {
        return when (selectedOptionIndex) {
            1, 2, 3 -> true
            4 -> binding.etCustomFeedback.text.toString().trim().isNotEmpty()
            else -> false
        }
    }

    private fun updateStillUninstallButtonState() {
        val isValid = isUninstallReasonValid()
        binding.btnStillWantUninstall.isEnabled = isValid
        binding.btnStillWantUninstall.alpha = if (isValid) 1.0f else 0.4f
    }

    private fun setupKeyboardInsetsListener() {
        val density = resources.displayMetrics.density

        // 1. WindowInsetsAnimationCompat for smooth 60fps keyboard animation
        ViewCompat.setWindowInsetsAnimationCallback(
            binding.root,
            object : WindowInsetsAnimationCompat.Callback(DISPATCH_MODE_STOP) {
                override fun onProgress(
                    insets: WindowInsetsCompat,
                    runningAnimations: MutableList<WindowInsetsAnimationCompat>
                ): WindowInsetsCompat {
                    val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
                    val navInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
                    val statusInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
                    lastImeInsetsBottom = imeInsets
                    updateInsets(statusInsets, navInsets, getEffectiveImeBottom())
                    return insets
                }
            }
        )

        // 2. OnApplyWindowInsetsListener for state changes
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val navBottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            val imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val statusTop = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            lastImeInsetsBottom = imeBottom
            updateInsets(statusTop, navBottom, getEffectiveImeBottom())
            insets
        }

        val currentInsets = ViewCompat.getRootWindowInsets(binding.root)
        if (currentInsets != null) {
            val navBottom = currentInsets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            val imeBottom = currentInsets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val statusTop = currentInsets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            lastImeInsetsBottom = imeBottom
            updateInsets(statusTop, navBottom, getEffectiveImeBottom())
        }

        // 3. GlobalLayoutListener as fallback
        val decorView = window.decorView
        val layoutListener = android.view.ViewTreeObserver.OnGlobalLayoutListener {
            val r = Rect()
            decorView.getWindowVisibleDisplayFrame(r)
            val screenHeight = decorView.rootView.height
            val heightDiff = screenHeight - r.bottom
            val isKeyboardVisible = heightDiff > (100 * density)
            lastGlobalImeBottom = if (isKeyboardVisible) heightDiff else 0
            val rootInsets = ViewCompat.getRootWindowInsets(binding.root)
            val navBottom = rootInsets?.getInsets(WindowInsetsCompat.Type.navigationBars())?.bottom ?: 0
            val statusTop = rootInsets?.getInsets(WindowInsetsCompat.Type.statusBars())?.top ?: 0
            updateInsets(statusTop, navBottom, getEffectiveImeBottom())
        }
        globalLayoutListener = layoutListener
        decorView.viewTreeObserver.addOnGlobalLayoutListener(layoutListener)
    }

    private fun getEffectiveImeBottom(): Int {
        return if (lastImeInsetsBottom > 0) lastImeInsetsBottom else lastGlobalImeBottom
    }

    private fun updateInsets(statusTop: Int, navBottom: Int, imeBottom: Int) {
        val density = resources.displayMetrics.density
        val isKeyboardOpen = imeBottom > 0
        val keyboardStateChanged = isKeyboardOpen != wasKeyboardOpen
        wasKeyboardOpen = isKeyboardOpen

        // Adjust top back button for status bar
        val topMargin = statusTop + (12 * density).toInt()
        (binding.btnBack.layoutParams as? ViewGroup.MarginLayoutParams)?.let { lp ->
            if (lp.topMargin != topMargin) {
                lp.topMargin = topMargin
                binding.btnBack.layoutParams = lp
            }
        }

        // Keep llBottomButtons fixed at footer
        val lp = binding.llBottomButtons.layoutParams as? ViewGroup.MarginLayoutParams
        if (lp != null && lp.bottomMargin != navBottom) {
            lp.bottomMargin = navBottom
            binding.llBottomButtons.layoutParams = lp
        }

        val defaultPaddingBottom = (24 * density).toInt()
        if (binding.llBottomButtons.paddingBottom != defaultPaddingBottom) {
            binding.llBottomButtons.setPadding(
                (20 * density).toInt(),
                binding.llBottomButtons.paddingTop,
                (20 * density).toInt(),
                defaultPaddingBottom
            )
        }

        // Adjust scroll view bottom padding when keyboard is open
        val footerHeight = binding.llBottomButtons.height.takeIf { it > 0 } ?: (140 * density).toInt()
        val keyboardOverlap = if (isKeyboardOpen) (imeBottom - footerHeight).coerceAtLeast(0) else 0
        val targetScrollPaddingBottom = (16 * density).toInt() + keyboardOverlap

        if (binding.nestedScrollView.paddingBottom != targetScrollPaddingBottom) {
            binding.nestedScrollView.setPadding(
                (20 * density).toInt(),
                binding.nestedScrollView.paddingTop,
                (20 * density).toInt(),
                targetScrollPaddingBottom
            )
        }

        // Scroll to option 4 when keyboard opens
        if (isKeyboardOpen && selectedOptionIndex == 4) {
            binding.nestedScrollView.post { scrollToOthersCard() }
            if (keyboardStateChanged) {
                binding.nestedScrollView.postDelayed({ scrollToOthersCard() }, 100)
                binding.nestedScrollView.postDelayed({ scrollToOthersCard() }, 250)
                binding.nestedScrollView.postDelayed({ scrollToOthersCard() }, 400)
            }
        }
    }

    private fun scrollToOthersCard() {
        val child = binding.nestedScrollView.getChildAt(0)
        if (child != null && binding.nestedScrollView.height > 0) {
            val childHeight = maxOf(child.height, child.bottom)
            val maxScroll = maxOf(
                0,
                childHeight + binding.nestedScrollView.paddingBottom - binding.nestedScrollView.height
            )
            binding.nestedScrollView.smoothScrollTo(0, maxScroll)
        } else {
            binding.nestedScrollView.fullScroll(View.FOCUS_DOWN)
        }
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

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (ev?.action == MotionEvent.ACTION_DOWN) {
            val v = currentFocus
            if (v is EditText) {
                val outRect = Rect()
                v.getGlobalVisibleRect(outRect)
                if (!outRect.contains(ev.rawX.toInt(), ev.rawY.toInt())) {
                    val cardRect = Rect()
                    binding.cardOption4.getGlobalVisibleRect(cardRect)
                    if (!cardRect.contains(ev.rawX.toInt(), ev.rawY.toInt())) {
                        v.clearFocus()
                        hideKeyboard()
                    }
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        currentFocus?.let { focus ->
            imm?.hideSoftInputFromWindow(focus.windowToken, 0)
        } ?: run {
            imm?.hideSoftInputFromWindow(binding.root.windowToken, 0)
        }
        hideNavigationBar()
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

    override fun onDestroy() {
        super.onDestroy()
        globalLayoutListener?.let {
            if (window.decorView.viewTreeObserver.isAlive) {
                window.decorView.viewTreeObserver.removeOnGlobalLayoutListener(it)
            }
        }
        globalLayoutListener = null
    }
}
