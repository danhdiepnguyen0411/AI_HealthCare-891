package com.example.healthcareai.ui.main

import android.content.Intent
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
import com.example.healthcareai.databinding.FragmentHomeBinding
import com.example.healthcareai.ui.camera.CameraActivity
import com.example.healthcareai.ui.heartrate.HeartRateActivity
import com.example.healthcareai.ui.symptom.SymptomCheckerActivity
import java.util.Calendar

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupWindowInsets()
        updateGreeting()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        updateGreeting()
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val statusBarTop = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            val density = resources.displayMetrics.density
            val topPadding = if (statusBarTop > 0) {
                statusBarTop + (10 * density).toInt()
            } else {
                (50 * density).toInt()
            }
            binding.topBar.setPadding(
                (16 * density).toInt(),
                topPadding,
                (16 * density).toInt(),
                (16 * density).toInt()
            )
            insets
        }
    }

    /**
     * Updates the greeting based on the current time of day:
     * - Morning: 05:00 - 11:59 ("Good morning")
     * - Afternoon: 12:00 - 17:59 ("Good afternoon")
     * - Evening / Night: 18:00 - 04:59 ("Good evening")
     */
    private fun updateGreeting() {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greetingRes = when (hour) {
            in 5..11 -> R.string.home_greeting_morning
            in 12..17 -> R.string.home_greeting_afternoon
            else -> R.string.home_greeting_evening
        }
        binding.tvGreeting.setText(greetingRes)
    }

    private fun setupListeners() {
        // "Get Pro" button
        binding.btnGetPro.setOnClickListener {
            Toast.makeText(
                requireContext(),
                "HealthLink Pro: Unlimited AI scans & 24/7 care support!",
                Toast.LENGTH_SHORT
            ).show()
        }

        // "Chat with AI" button in the assistant card
        binding.btnChatWithAi.setOnClickListener {
            Toast.makeText(
                requireContext(),
                "Connecting to your personal AI Health Assistant...",
                Toast.LENGTH_SHORT
            ).show()
        }

        // Feature Card 1: Health Scan -> Switch to Scan Tab
        binding.cardHealthScan.setOnClickListener {
            (activity as? MainActivity)?.navigateToTab(1)
        }

        // Feature Card 2: Measure Heart Rate
        binding.cardMeasureHeartRate.setOnClickListener {
            startActivity(Intent(requireContext(), HeartRateActivity::class.java))
        }

        // Feature Card 3: Symptom Checker
        binding.cardSymptomChecker.setOnClickListener {
            startActivity(Intent(requireContext(), SymptomCheckerActivity::class.java))
        }

        // Today's reminder: "View all" and "Add reminder"
        binding.tvViewAllReminders.setOnClickListener {
            (activity as? MainActivity)?.navigateToTab(2)
        }

        binding.btnAddReminder.setOnClickListener {
            (activity as? MainActivity)?.navigateToTab(2)
        }

        // AI scan history: "View all" and "Start first scan"
        binding.tvViewAllScans.setOnClickListener {
            (activity as? MainActivity)?.navigateToTab(1)
        }

        binding.btnStartFirstScan.setOnClickListener {
            val intent = Intent(requireContext(), CameraActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = HomeFragment()
    }
}
