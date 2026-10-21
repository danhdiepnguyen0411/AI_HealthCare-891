package com.example.healthcareai.ui.intro

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
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

        setupSlides()
        setupViewPager()
        setupIndicators()
        setupListeners()
        setupBackPress()
    }

    private fun setupSlides() {
        slides = listOf(
            IntroSlide(
                title = getString(R.string.intro_title_1),
                description = getString(R.string.intro_desc_1),
                illustrationRes = R.drawable.logo_rm
            ),
            IntroSlide(
                title = getString(R.string.intro_title_2),
                description = getString(R.string.intro_desc_2),
                illustrationRes = R.drawable.logo_rm
            ),
            IntroSlide(
                title = getString(R.string.intro_title_3),
                description = getString(R.string.intro_desc_3),
                illustrationRes = R.drawable.logo_rm
            )
        )
    }

    private fun setupViewPager() {
        binding.viewPager.adapter = IntroAdapter(slides)
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateIndicators(position)
                if (position == slides.size - 1) {
                    binding.btnAction.text = getString(R.string.btn_get_started)
                    binding.btnSkip.visibility = View.INVISIBLE
                } else {
                    binding.btnAction.text = getString(R.string.btn_next)
                    binding.btnSkip.visibility = View.VISIBLE
                }
            }
        })
    }

    private fun setupIndicators() {
        binding.dotsContainer.removeAllViews()
        val dotParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(8, 0, 8, 0)
        }

        for (i in slides.indices) {
            val dot = ImageView(this).apply {
                setImageDrawable(ContextCompat.getDrawable(this@IntroActivity, R.drawable.ic_radio_unselected))
                layoutParams = dotParams
            }
            binding.dotsContainer.addView(dot)
        }
        updateIndicators(0)
    }

    private fun updateIndicators(selectedIndex: Int) {
        val count = binding.dotsContainer.childCount
        for (i in 0 until count) {
            val dot = binding.dotsContainer.getChildAt(i) as? ImageView
            if (i == selectedIndex) {
                dot?.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_radio_selected))
            } else {
                dot?.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_radio_unselected))
            }
        }
    }

    private fun setupListeners() {
        binding.btnSkip.setOnClickListener {
            completeIntro()
        }

        binding.btnAction.setOnClickListener {
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
                    // Slide 1 back pressed: clear incomplete state and exit app
                    preferenceHelper.clearLanguageIfIntroNotCompleted(this@IntroActivity)
                    finishAffinity()
                }
            }
        })
    }

    private fun completeIntro() {
        preferenceHelper.isIntroCompleted = true
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
