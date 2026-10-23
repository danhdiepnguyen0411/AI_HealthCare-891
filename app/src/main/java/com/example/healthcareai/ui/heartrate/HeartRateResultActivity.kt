package com.example.healthcareai.ui.heartrate

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import com.example.healthcareai.R
import com.example.healthcareai.data.repository.HeartRateRepository
import com.example.healthcareai.databinding.ActivityHeartRateResultBinding
import com.example.healthcareai.ui.base.BaseActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HeartRateResultActivity : BaseActivity() {

    private lateinit var binding: ActivityHeartRateResultBinding
    private var recordId: String? = null
    private var bpm: Int = 72
    private var timestamp: Long = System.currentTimeMillis()

    enum class BpmCategory {
        SLOW, NORMAL, FAST
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHeartRateResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        extractIntentData()
        setupWindowInsets()
        bindData()
        setupListeners()
    }

    private fun extractIntentData() {
        recordId = intent.getStringExtra(EXTRA_RECORD_ID)
        bpm = intent.getIntExtra(EXTRA_BPM, 72)
        timestamp = intent.getLongExtra(EXTRA_TIMESTAMP, System.currentTimeMillis())
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            binding.topBar.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = statusBars.top.coerceAtLeast(16)
            }
            insets
        }
    }

    private fun bindData() {
        // Date & Time formatting: e.g. "July 2 • 09:20 AM"
        val sdf = SimpleDateFormat("MMMM d • hh:mm a", Locale.ENGLISH)
        binding.tvDateTime.text = sdf.format(Date(timestamp))

        // BPM value
        binding.tvBpmValue.text = bpm.toString()

        // Category Classification
        val category = when {
            bpm < 60 -> BpmCategory.SLOW
            bpm > 100 -> BpmCategory.FAST
            else -> BpmCategory.NORMAL
        }

        val categoryColor = when (category) {
            BpmCategory.SLOW -> ContextCompat.getColor(this, R.color.heart_rate_slow)
            BpmCategory.NORMAL -> ContextCompat.getColor(this, R.color.heart_rate_normal)
            BpmCategory.FAST -> ContextCompat.getColor(this, R.color.heart_rate_fast)
        }

        val categoryName = when (category) {
            BpmCategory.SLOW -> getString(R.string.heart_rate_slow_label)
            BpmCategory.NORMAL -> getString(R.string.heart_rate_normal_label)
            BpmCategory.FAST -> getString(R.string.heart_rate_fast_label)
        }

        binding.tvCategoryTitle.text = categoryName
        binding.ivIndicatorTriangle.imageTintList = ColorStateList.valueOf(categoryColor)

        // Calculate triangle position on gradient bar
        val pct = when {
            bpm <= 40 -> 0.05f
            bpm < 60 -> 0.05f + (bpm - 40f) / 20f * 0.23f
            bpm <= 100 -> 0.28f + (bpm - 60f) / 40f * 0.44f
            bpm <= 160 -> 0.72f + (bpm - 100f) / 60f * 0.23f
            else -> 0.95f
        }

        binding.layoutIndicatorTrack.post {
            val trackWidth = binding.layoutIndicatorTrack.width
            val triangleWidth = binding.ivIndicatorTriangle.width
            val maxOffset = (trackWidth - triangleWidth).coerceAtLeast(0)
            binding.ivIndicatorTriangle.translationX = maxOffset * pct
        }

        highlightActiveCategory(category)
    }

    private fun highlightActiveCategory(activeCategory: BpmCategory) {
        val boldFont = ResourcesCompat.getFont(this, R.font.urbanist_bold)
        val mediumFont = ResourcesCompat.getFont(this, R.font.urbanist_medium)
        val colorActive = ContextCompat.getColor(this, R.color.color_text)
        val colorInactive = ContextCompat.getColor(this, R.color.color_subtext)

        fun styleRow(label: TextView, value: TextView, isActive: Boolean) {
            if (isActive) {
                label.typeface = boldFont
                label.setTextColor(colorActive)
                label.textSize = 16f
                value.typeface = boldFont
                value.setTextColor(colorActive)
                value.textSize = 16f
            } else {
                label.typeface = mediumFont
                label.setTextColor(colorInactive)
                label.textSize = 15f
                value.typeface = mediumFont
                value.setTextColor(colorInactive)
                value.textSize = 15f
            }
        }

        styleRow(binding.tvSlowLabel, binding.tvSlowValue, activeCategory == BpmCategory.SLOW)
        styleRow(binding.tvNormalLabel, binding.tvNormalValue, activeCategory == BpmCategory.NORMAL)
        styleRow(binding.tvFastLabel, binding.tvFastValue, activeCategory == BpmCategory.FAST)
    }

    private fun setupListeners() {
        binding.btnDone.setOnClickListener {
            val intent = Intent(this, HeartRateStatisticsActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            startActivity(intent)
            finish()
        }

        binding.btnDelete.setOnClickListener {
            showDeleteConfirmation()
        }
    }

    private fun showDeleteConfirmation() {
        AlertDialog.Builder(this)
            .setTitle(R.string.heart_rate_delete_confirm_title)
            .setMessage(R.string.heart_rate_delete_confirm_msg)
            .setPositiveButton(R.string.heart_rate_delete_btn) { _, _ ->
                recordId?.let { id ->
                    HeartRateRepository.getInstance(this).deleteRecord(id)
                }
                Toast.makeText(this, R.string.heart_rate_deleted, Toast.LENGTH_SHORT).show()
                finish()
            }
            .setNegativeButton(R.string.heart_rate_cancel_btn, null)
            .show()
    }

    companion object {
        const val EXTRA_RECORD_ID = "extra_record_id"
        const val EXTRA_BPM = "extra_bpm"
        const val EXTRA_TIMESTAMP = "extra_timestamp"

        fun createIntent(
            context: Context,
            recordId: String?,
            bpm: Int,
            timestamp: Long = System.currentTimeMillis()
        ): Intent {
            return Intent(context, HeartRateResultActivity::class.java).apply {
                putExtra(EXTRA_RECORD_ID, recordId)
                putExtra(EXTRA_BPM, bpm)
                putExtra(EXTRA_TIMESTAMP, timestamp)
            }
        }
    }
}
