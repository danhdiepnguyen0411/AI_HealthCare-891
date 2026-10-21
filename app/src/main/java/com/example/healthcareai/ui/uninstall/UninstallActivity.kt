package com.example.healthcareai.ui.uninstall

import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
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

        initViews()
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

        binding.btnDontUninstall.setOnClickListener {
            goToHome()
        }

        binding.btnExplore.setOnClickListener {
            goToHome()
        }

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
