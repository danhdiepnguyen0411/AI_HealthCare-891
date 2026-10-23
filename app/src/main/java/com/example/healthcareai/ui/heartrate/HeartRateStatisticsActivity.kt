package com.example.healthcareai.ui.heartrate

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.healthcareai.R
import com.example.healthcareai.data.repository.HeartRateRecord
import com.example.healthcareai.data.repository.HeartRateRepository
import com.example.healthcareai.databinding.ActivityHeartRateStatisticsBinding
import com.example.healthcareai.ui.base.BaseActivity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

class HeartRateStatisticsActivity : BaseActivity() {

    private lateinit var binding: ActivityHeartRateStatisticsBinding
    private lateinit var repository: HeartRateRepository
    private lateinit var historyAdapter: HeartRateHistoryAdapter

    private var currentTab = StatsTab.OVERVIEW
    private var weekOffset: Int = 0 // 0 = current week, -1 = last week, etc.

    private enum class StatsTab {
        OVERVIEW, HISTORY
    }

    private val urbanistBold: Typeface? by lazy {
        ResourcesCompat.getFont(this, R.font.urbanist_bold)
    }

    private val urbanistMedium: Typeface? by lazy {
        ResourcesCompat.getFont(this, R.font.urbanist_medium)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHeartRateStatisticsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = HeartRateRepository.getInstance(this)

        setupWindowInsets()
        setupRecyclerView()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        refreshData()
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.rootView) { _, insets ->
            val statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            binding.topBar.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = statusBars.top.coerceAtLeast(12)
            }
            insets
        }
    }

    private fun setupRecyclerView() {
        historyAdapter = HeartRateHistoryAdapter(
            onItemDelete = { record ->
                repository.deleteRecord(record.id)
                android.widget.Toast.makeText(this, R.string.heart_rate_deleted, android.widget.Toast.LENGTH_SHORT).show()
                refreshData()
            }
        )

        binding.rvHistory.apply {
            layoutManager = LinearLayoutManager(this@HeartRateStatisticsActivity)
            adapter = historyAdapter
            addOnScrollListener(object : androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: androidx.recyclerview.widget.RecyclerView, newState: Int) {
                    if (newState == androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_DRAGGING) {
                        historyAdapter.closeOpenItem()
                    }
                }
            })
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnTabOverview.setOnClickListener {
            if (currentTab != StatsTab.OVERVIEW) {
                currentTab = StatsTab.OVERVIEW
                updateTabsUI()
                refreshData()
            }
        }

        binding.btnTabHistory.setOnClickListener {
            if (currentTab != StatsTab.HISTORY) {
                currentTab = StatsTab.HISTORY
                updateTabsUI()
                refreshData()
            }
        }

        binding.btnPrevWeek.setOnClickListener {
            weekOffset--
            loadOverviewChartData(repository.getAllRecords(), animate = true)
        }

        binding.btnNextWeek.setOnClickListener {
            if (weekOffset < 0) {
                weekOffset++
                loadOverviewChartData(repository.getAllRecords(), animate = true)
            }
        }

        binding.btnEmptyMeasureNow.setOnClickListener {
            val intent = Intent(this, HeartRateActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            startActivity(intent)
            finish()
        }
    }

    private fun updateTabsUI() {
        val colorActive = Color.WHITE
        val colorInactive = ContextCompat.getColor(this, R.color.stats_tab_inactive_text)

        when (currentTab) {
            StatsTab.OVERVIEW -> {
                binding.btnTabOverview.setBackgroundResource(R.drawable.bg_stats_tab_selected)
                binding.btnTabOverview.setTextColor(colorActive)
                binding.btnTabOverview.typeface = urbanistBold

                binding.btnTabHistory.setBackgroundColor(Color.TRANSPARENT)
                binding.btnTabHistory.setTextColor(colorInactive)
                binding.btnTabHistory.typeface = urbanistMedium
            }
            StatsTab.HISTORY -> {
                binding.btnTabHistory.setBackgroundResource(R.drawable.bg_stats_tab_selected)
                binding.btnTabHistory.setTextColor(colorActive)
                binding.btnTabHistory.typeface = urbanistBold

                binding.btnTabOverview.setBackgroundColor(Color.TRANSPARENT)
                binding.btnTabOverview.setTextColor(colorInactive)
                binding.btnTabOverview.typeface = urbanistMedium
            }
        }
    }

    private fun refreshData() {
        val records = repository.getAllRecords()

        // 1. Top metric cards
        if (records.isEmpty()) {
            binding.tvAverageValue.text = getString(R.string.statistics_empty_val)
            binding.tvHighestValue.text = getString(R.string.statistics_empty_val)
            binding.tvLowestValue.text = getString(R.string.statistics_empty_val)
        } else {
            val avg = records.map { it.bpm }.average().roundToInt()
            val highest = records.maxOf { it.bpm }
            val lowest = records.minOf { it.bpm }

            binding.tvAverageValue.text = avg.toString()
            binding.tvHighestValue.text = highest.toString()
            binding.tvLowestValue.text = lowest.toString()
        }

        // 2. Tab content display
        if (records.isEmpty()) {
            binding.layoutEmptyState.visibility = View.VISIBLE
            binding.layoutOverviewData.visibility = View.GONE
            binding.rvHistory.visibility = View.GONE

            if (currentTab == StatsTab.OVERVIEW) {
                binding.tvEmptyTitle.setText(R.string.statistics_no_data_title)
                binding.tvEmptySubtitle.setText(R.string.statistics_no_data_desc)
            } else {
                binding.tvEmptyTitle.setText(R.string.statistics_no_history_title)
                binding.tvEmptySubtitle.setText(R.string.statistics_no_history_desc)
            }
        } else {
            binding.layoutEmptyState.visibility = View.GONE

            when (currentTab) {
                StatsTab.OVERVIEW -> {
                    binding.layoutOverviewData.visibility = View.VISIBLE
                    binding.rvHistory.visibility = View.GONE
                    loadOverviewChartData(records, animate = false)
                }
                StatsTab.HISTORY -> {
                    binding.layoutOverviewData.visibility = View.GONE
                    binding.rvHistory.visibility = View.VISIBLE
                    historyAdapter.submitList(records)
                }
            }
        }
    }

    private fun loadOverviewChartData(records: List<HeartRateRecord>, animate: Boolean) {
        // Calculate Monday 00:00:00 and Sunday 23:59:59 for weekOffset
        val cal = Calendar.getInstance()
        cal.firstDayOfWeek = Calendar.MONDAY
        // Snap to Monday of current week
        val currentDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val daysFromMonday = if (currentDayOfWeek == Calendar.SUNDAY) 6 else currentDayOfWeek - Calendar.MONDAY
        cal.add(Calendar.DAY_OF_YEAR, -daysFromMonday)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        // Apply week offset
        cal.add(Calendar.WEEK_OF_YEAR, weekOffset)

        val mondayStart = cal.timeInMillis
        val mondayCal = cal.clone() as Calendar

        cal.add(Calendar.DAY_OF_YEAR, 6)
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val sundayEnd = cal.timeInMillis
        val sundayCal = cal.clone() as Calendar

        // Format Date Range e.g. "June 1 - June 7"
        binding.tvWeekRange.text = formatWeekRange(mondayCal, sundayCal)

        // Update Next Week button state
        binding.btnNextWeek.apply {
            isEnabled = weekOffset < 0
            alpha = if (weekOffset < 0) 1.0f else 0.35f
        }

        // Compute 7 days BPM (Mon=0..Sun=6)
        val dailyAverages = mutableListOf<Int?>()
        val dayCal = mondayCal.clone() as Calendar

        for (i in 0 until 7) {
            val dayStart = dayCal.timeInMillis
            val dayEnd = dayStart + 24 * 60 * 60 * 1000 - 1

            val dayRecords = records.filter { it.timestamp in dayStart..dayEnd }
            if (dayRecords.isNotEmpty()) {
                dailyAverages.add(dayRecords.map { it.bpm }.average().roundToInt())
            } else {
                dailyAverages.add(null)
            }
            dayCal.add(Calendar.DAY_OF_YEAR, 1)
        }

        binding.chartWeeklyHeartRate.setData(dailyAverages, animate = animate)
    }

    private fun formatWeekRange(startCal: Calendar, endCal: Calendar): String {
        val sameMonth = startCal.get(Calendar.MONTH) == endCal.get(Calendar.MONTH)
        return if (sameMonth) {
            val monthFormat = SimpleDateFormat("MMMM d", Locale.ENGLISH)
            val endDayFormat = SimpleDateFormat("d", Locale.ENGLISH)
            "${monthFormat.format(startCal.time)} - ${endDayFormat.format(endCal.time)}"
        } else {
            val format = SimpleDateFormat("MMMM d", Locale.ENGLISH)
            "${format.format(startCal.time)} - ${format.format(endCal.time)}"
        }
    }

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, HeartRateStatisticsActivity::class.java))
        }
    }
}
