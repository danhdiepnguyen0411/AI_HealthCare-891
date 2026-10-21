package com.example.healthcareai.ui.intro

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewpager2.widget.ViewPager2
import com.example.healthcareai.R
import com.example.healthcareai.data.local.PreferenceHelper
import com.example.healthcareai.databinding.ActivityIntroBinding
import com.example.healthcareai.ui.base.BaseActivity
import com.example.healthcareai.ui.main.MainActivity

class IntroActivity : BaseActivity() {

    private lateinit var binding: ActivityIntroBinding
    private lateinit var preferenceHelper: PreferenceHelper
    private lateinit var slides: List<IntroSlide>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferenceHelper = PreferenceHelper.getInstance(this)

        binding = ActivityIntroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWindowInsets()
        setupSlides()
        setupViewPager()
        setupIndicators()
        setupListeners()
        setupBackPress()
    }

    private fun setupWindowInsets() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.layoutBottom) { v, insets ->
            val navBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            val density = resources.displayMetrics.density
            val basePaddingBottom = (40 * density).toInt()
            v.setPadding(
                v.paddingLeft,
                v.paddingTop,
                v.paddingRight,
                basePaddingBottom + navBars.bottom
            )
            insets
        }
    }

    private fun setupSlides() {
        slides = listOf(
            IntroSlide(
                title = getString(R.string.intro_title_1),
                description = getString(R.string.intro_desc_1),
                illustrationRes = R.drawable.intro1,
                backgroundColor = Color.parseColor("#DDF0FD")
            ),
            IntroSlide(
                title = getString(R.string.intro_title_2),
                description = getString(R.string.intro_desc_2),
                illustrationRes = R.drawable.intro2,
                backgroundColor = Color.parseColor("#F6FCFD")
            ),
            IntroSlide(
                title = getString(R.string.intro_title_3),
                description = getString(R.string.intro_desc_3),
                illustrationRes = R.drawable.intro3,
                backgroundColor = Color.parseColor("#E7F3FC")
            )
        )
    }

    private fun setupViewPager() {
        binding.viewPager.apply {
            adapter = IntroAdapter(slides)
            offscreenPageLimit = 2
            setPageTransformer(IntroPageTransformer())
            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageScrolled(
                    position: Int,
                    positionOffset: Float,
                    positionOffsetPixels: Int
                ) {
                    super.onPageScrolled(position, positionOffset, positionOffsetPixels)
                    updateIndicatorsContinuous(position, positionOffset)
                }

                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    updateIndicatorsExact(position)
                }
            })
        }
    }

    private fun setupIndicators() {
        binding.layoutIndicators.removeAllViews()
        val density = resources.displayMetrics.density
        val activeWidth = (34 * density).toInt()
        val inactiveWidth = (8 * density).toInt()
        val heightPx = (8 * density).toInt()
        val marginPx = (6 * density).toInt()

        for (i in slides.indices) {
            val isActive = (i == 0)
            val dotView = View(this).apply {
                setBackgroundResource(
                    if (isActive) R.drawable.bg_intro_indicator_active
                    else R.drawable.bg_intro_indicator_inactive
                )
            }
            val lp = LinearLayout.LayoutParams(
                if (isActive) activeWidth else inactiveWidth,
                heightPx
            ).apply {
                if (i > 0) marginStart = marginPx
            }
            binding.layoutIndicators.addView(dotView, lp)
        }
    }

    private fun updateIndicatorsContinuous(position: Int, positionOffset: Float) {
        val count = slides.size
        val density = resources.displayMetrics.density
        val activeWidthPx = (34 * density).toInt()
        val inactiveWidthPx = (8 * density).toInt()
        val heightPx = (8 * density).toInt()
        val deltaWidth = activeWidthPx - inactiveWidthPx

        for (i in 0 until count) {
            val dot = binding.layoutIndicators.getChildAt(i) ?: continue
            val lp = dot.layoutParams as? LinearLayout.LayoutParams ?: continue

            val width = when (i) {
                position -> (activeWidthPx - deltaWidth * positionOffset).toInt()
                position + 1 -> (inactiveWidthPx + deltaWidth * positionOffset).toInt()
                else -> inactiveWidthPx
            }
            lp.width = width
            lp.height = heightPx
            dot.layoutParams = lp

            val isActive = if (positionOffset < 0.5f) (i == position) else (i == position + 1)
            dot.setBackgroundResource(
                if (isActive) R.drawable.bg_intro_indicator_active
                else R.drawable.bg_intro_indicator_inactive
            )
        }
        binding.layoutIndicators.requestLayout()

        // Update button text to GET STARTED on the final slide
        val effectivePosition = if (positionOffset >= 0.5f) position + 1 else position
        binding.btnNext.text = if (effectivePosition == slides.size - 1) {
            getString(R.string.btn_get_started)
        } else {
            getString(R.string.btn_next)
        }
    }

    private fun updateIndicatorsExact(selectedIndex: Int) {
        val count = slides.size
        val density = resources.displayMetrics.density
        val activeWidthPx = (34 * density).toInt()
        val inactiveWidthPx = (8 * density).toInt()
        val heightPx = (8 * density).toInt()

        for (i in 0 until count) {
            val dot = binding.layoutIndicators.getChildAt(i) ?: continue
            val lp = dot.layoutParams as? LinearLayout.LayoutParams ?: continue
            val isActive = (i == selectedIndex)
            lp.width = if (isActive) activeWidthPx else inactiveWidthPx
            lp.height = heightPx
            dot.layoutParams = lp
            dot.setBackgroundResource(
                if (isActive) R.drawable.bg_intro_indicator_active
                else R.drawable.bg_intro_indicator_inactive
            )
        }
        binding.layoutIndicators.requestLayout()

        // Ensure button text is updated on page snap
        binding.btnNext.text = if (selectedIndex == slides.size - 1) {
            getString(R.string.btn_get_started)
        } else {
            getString(R.string.btn_next)
        }
    }

    private fun setupListeners() {
        binding.btnNext.setOnClickListener {
            val current = binding.viewPager.currentItem
            if (current < slides.size - 1) {
                binding.viewPager.setCurrentItem(current + 1, true)
            } else {
                completeIntro()
            }
        }
    }

    private fun setupBackPress() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val current = binding.viewPager.currentItem
                if (current > 0) {
                    binding.viewPager.setCurrentItem(current - 1, true)
                } else {
                    // Slide 0 back press: reset language and clear onboarding state
                    preferenceHelper.clearLanguageIfIntroNotCompleted(this@IntroActivity)
                    finishAffinity()
                }
            }
        })
    }

    private fun completeIntro() {
        preferenceHelper.isIntroCompleted = true
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
        finish()
    }
}
