package com.example.healthcareai.ui.main

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import com.example.healthcareai.R
import com.example.healthcareai.data.local.PreferenceHelper
import com.example.healthcareai.databinding.FragmentSettingsBinding
import com.example.healthcareai.ui.language.LanguageActivity
import com.example.healthcareai.util.LocaleHelper
import com.example.healthcareai.util.PermissionReturnHelper

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupWindowInsets()
        updateCurrentLanguageLabel()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        updateCurrentLanguageLabel()
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.rootSettingsContainer) { _, insets ->
            val statusBarTop = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            val density = resources.displayMetrics.density
            val targetTopMargin = if (statusBarTop > 0) {
                statusBarTop + (8 * density).toInt()
            } else {
                (24 * density).toInt()
            }
            binding.topBar.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                if (topMargin != targetTopMargin) {
                    topMargin = targetTopMargin
                }
            }
            insets
        }
    }

    private fun updateCurrentLanguageLabel() {
        val pref = PreferenceHelper.getInstance(requireContext())
        val langCode = pref.selectedLanguageCode
        val langName = LocaleHelper.getLanguageNameByCode(langCode)
        binding.tvCurrentLanguage.text = langName
    }

    private fun setupListeners() {
        // 1. Language Item
        binding.cardLanguage.setOnClickListener {
            val intent = Intent(requireContext(), LanguageActivity::class.java).apply {
                putExtra(LanguageActivity.EXTRA_IS_FROM_SETTINGS, true)
            }
            startActivity(intent)
        }

        // 2. Permission Item
        binding.cardPermission.setOnClickListener {
            PermissionReturnHelper.openAppSettings(requireContext())
        }

        // 3. Share with friends
        binding.cardShareWithFriends.setOnClickListener {
            val appPackageName = requireContext().packageName
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name))
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Check out HealthLink AI - Your Personal Health Assistant: https://play.google.com/store/apps/details?id=$appPackageName"
                )
            }
            startActivity(
                Intent.createChooser(
                    shareIntent,
                    getString(R.string.settings_item_share)
                )
            )
        }

        // 4. Rate Us
        binding.cardRateUs.setOnClickListener {
            val appPackageName = requireContext().packageName
            try {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("market://details?id=$appPackageName")
                    )
                )
            } catch (e: Exception) {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=$appPackageName")
                    )
                )
            }
        }

        // 5. Privacy Policy
        binding.cardPrivacyPolicy.setOnClickListener {
            val privacyUrl = "https://sites.google.com/view/healthcareai-privacy-policy"
            try {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(privacyUrl))
                startActivity(browserIntent)
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "Opening Privacy Policy...",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = SettingsFragment()
    }
}
