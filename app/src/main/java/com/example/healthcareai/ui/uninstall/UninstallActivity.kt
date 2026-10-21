package com.example.healthcareai.ui.uninstall

import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.healthcareai.data.local.PreferenceHelper
import com.example.healthcareai.databinding.ActivityUninstallBinding
import com.example.healthcareai.ui.base.BaseActivity
import com.example.healthcareai.ui.intro.IntroActivity
import com.example.healthcareai.ui.language.LanguageActivity
import com.example.healthcareai.ui.main.MainActivity

class UninstallActivity : BaseActivity() {

    private lateinit var binding: ActivityUninstallBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUninstallBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWindowInsets()
        initViews()
    }

    private fun setupWindowInsets() {
        val density = resources.displayMetrics.density

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val statusBar = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val navBar = insets.getInsets(WindowInsetsCompat.Type.navigationBars())

            // Top status bar padding for back button
            val topMargin = statusBar.top + (12 * density).toInt()
            (binding.btnBack.layoutParams as? ViewGroup.MarginLayoutParams)?.let { lp ->
                if (lp.topMargin != topMargin) {
                    lp.topMargin = topMargin
                    binding.btnBack.layoutParams = lp
                }
            }

            // Bottom navigation bar padding
            val bottomPadding = navBar.bottom + (24 * density).toInt()
            binding.llBottomButtons.setPadding(
                binding.llBottomButtons.paddingLeft,
                binding.llBottomButtons.paddingTop,
                binding.llBottomButtons.paddingRight,
                bottomPadding
            )

            insets
        }
    }

    private fun initViews() {
        binding.btnBack.setOnClickListener {
            goToHome()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                goToHome()
            }
        })

        // Card 1 "Try again" -> takes user back to Home
        binding.btnTryAgain1.setOnClickListener {
            goToHome()
        }

        // Card 2 "Explore" -> takes user back to Home
        binding.btnExplore.setOnClickListener {
            goToHome()
        }

        // "Don't want to uninstall" -> takes user back to Home
        binding.btnDontUninstall.setOnClickListener {
            goToHome()
        }

        // "Still want to uninstall" -> goes to Screen 2 (UninstallReasonActivity)
        binding.btnStillWantUninstall.setOnClickListener {
            startActivity(Intent(this, UninstallReasonActivity::class.java))
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
