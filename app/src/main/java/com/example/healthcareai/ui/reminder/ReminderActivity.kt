package com.example.healthcareai.ui.reminder

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.healthcareai.R
import com.example.healthcareai.data.model.ReminderItem
import com.example.healthcareai.data.repository.ProfileRepository
import com.example.healthcareai.data.repository.ReminderRepository
import com.example.healthcareai.databinding.ActivityReminderBinding
import com.example.healthcareai.ui.base.BaseActivity
import com.example.healthcareai.util.LocaleHelper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ReminderActivity : BaseActivity() {

    private lateinit var binding: ActivityReminderBinding
    private lateinit var reminderAdapter: ReminderAdapter
    private lateinit var horizontalCalendarAdapter: HorizontalCalendarAdapter
    private lateinit var monthCalendarAdapter: MonthCalendarAdapter

    private var profileId: String? = null

    private var selectedCalendarDay: Calendar = Calendar.getInstance()
    private var displayedMonthCalendar: Calendar = Calendar.getInstance()
    private var isMonthCalendarExpanded: Boolean = false

    private val daysList = mutableListOf<Calendar>()
    private var cachedAllReminders = listOf<ReminderItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReminderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        profileId = intent.getStringExtra(EXTRA_PROFILE_ID)

        setupHeaderTopMargin(binding.topBar, binding.root)
        setupTopBar()
        setupCalendar()
        setupRecyclerView()

        val highlightReminderId = intent.getStringExtra(EXTRA_HIGHLIGHT_REMINDER_ID)
        if (!highlightReminderId.isNullOrEmpty()) {
            intent.removeExtra(EXTRA_HIGHLIGHT_REMINDER_ID)
            binding.root.post {
                ReminderDetailBottomSheetDialog.show(supportFragmentManager, highlightReminderId) {
                    loadReminders()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val newProfileId = intent.getStringExtra(EXTRA_PROFILE_ID)
        val newHighlightId = intent.getStringExtra(EXTRA_HIGHLIGHT_REMINDER_ID)
        if (!newProfileId.isNullOrEmpty() && newProfileId != profileId) {
            profileId = newProfileId
            updateTopBarTitle()
            loadReminders()
        }
        if (!newHighlightId.isNullOrEmpty()) {
            intent.removeExtra(EXTRA_HIGHLIGHT_REMINDER_ID)
            binding.root.post {
                ReminderDetailBottomSheetDialog.show(supportFragmentManager, newHighlightId) {
                    loadReminders()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateTopBarTitle()
        loadReminders()
    }

    private fun setupTopBar() {
        updateTopBarTitle()

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnAddReminder.setOnClickListener {
            NewReminderActivity.start(this, profileId = profileId)
        }

        binding.btnEmptyAddReminder.setOnClickListener {
            NewReminderActivity.start(this, profileId = profileId)
        }
    }

    private fun updateTopBarTitle() {
        val effectiveProfileId = profileId ?: ProfileRepository.getInstance(this).getPrimaryProfile().id
        val profile = ProfileRepository.getInstance(this).getProfileById(effectiveProfileId)
        if (profile != null && !profile.isPrimary && profile.name.isNotBlank()) {
            binding.tvTitle.text = "${profile.name} - ${getString(R.string.title_reminder)}"
        } else {
            binding.tvTitle.text = getString(R.string.title_reminder)
        }
    }

    private fun setupCalendar() {
        generateHorizontalDaysList()

        // 1. Horizontal Scrollable Calendar Days Strip
        horizontalCalendarAdapter = HorizontalCalendarAdapter { pickedCal ->
            onDateSelected(pickedCal, fromMonthView = false)
        }
        val horizontalLayoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvCalendarDays.layoutManager = horizontalLayoutManager
        binding.rvCalendarDays.adapter = horizontalCalendarAdapter
        horizontalCalendarAdapter.setDays(daysList, selectedCalendarDay)

        // Center on today on first load (index 180 - 3)
        val todayPosition = horizontalCalendarAdapter.getPositionForDate(selectedCalendarDay)
        if (todayPosition != -1) {
            val initialScrollPos = (todayPosition - 3).coerceAtLeast(0)
            binding.rvCalendarDays.scrollToPosition(initialScrollPos)
        }

        // Update Month Year header when scrolling horizontally
        binding.rvCalendarDays.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (!isMonthCalendarExpanded) {
                    val firstPos = horizontalLayoutManager.findFirstVisibleItemPosition()
                    val lastPos = horizontalLayoutManager.findLastVisibleItemPosition()
                    if (firstPos != RecyclerView.NO_POSITION && lastPos != RecyclerView.NO_POSITION) {
                        val midPos = (firstPos + lastPos) / 2
                        horizontalCalendarAdapter.getDateAtPosition(midPos)?.let { cal ->
                            updateMonthYearHeader(cal)
                        }
                    }
                }
            }
        })

        // 2. Custom Full Month Calendar Grid
        monthCalendarAdapter = MonthCalendarAdapter { pickedCal ->
            onDateSelected(pickedCal, fromMonthView = true)
        }
        binding.rvMonthDays.layoutManager = GridLayoutManager(this, 7)
        binding.rvMonthDays.adapter = monthCalendarAdapter

        setupWeekdayLabels()

        // Month Navigation Buttons (< and >)
        binding.btnPrevMonth.setOnClickListener {
            displayedMonthCalendar.add(Calendar.MONTH, -1)
            renderMonthCalendar()
        }

        binding.btnNextMonth.setOnClickListener {
            displayedMonthCalendar.add(Calendar.MONTH, 1)
            renderMonthCalendar()
        }

        // Calendar Icon Action (Toggle Expand/Collapse)
        binding.ivCalendarAction.setOnClickListener {
            toggleMonthCalendar()
        }

        updateMonthYearHeader(selectedCalendarDay)
    }

    private fun generateHorizontalDaysList() {
        daysList.clear()
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -180) // 180 days in the past
        for (i in 0..360) {
            daysList.add(cal.clone() as Calendar)
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
    }

    private fun setupWeekdayLabels() {
        val currentLocale = LocaleHelper.getSavedLocale(this)
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)

        val weekdayViews = listOf(
            binding.tvWeekday0,
            binding.tvWeekday1,
            binding.tvWeekday2,
            binding.tvWeekday3,
            binding.tvWeekday4,
            binding.tvWeekday5,
            binding.tvWeekday6
        )

        for (i in 0 until 7) {
            weekdayViews[i].text = HorizontalCalendarAdapter.getDayOfWeekAbbreviation(cal, currentLocale)
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
    }

    private fun toggleMonthCalendar() {
        isMonthCalendarExpanded = !isMonthCalendarExpanded
        if (isMonthCalendarExpanded) {
            binding.layoutFullMonthCalendar.visibility = View.VISIBLE
            binding.rvCalendarDays.visibility = View.GONE
            binding.ivCalendarAction.setColorFilter(ContextCompat.getColor(this, R.color.brand_primary))

            displayedMonthCalendar = selectedCalendarDay.clone() as Calendar
            renderMonthCalendar()
        } else {
            binding.layoutFullMonthCalendar.visibility = View.GONE
            binding.rvCalendarDays.visibility = View.VISIBLE
            binding.ivCalendarAction.clearColorFilter()

            updateMonthYearHeader(selectedCalendarDay)
            scrollToSelectedDay()
        }
    }

    private fun renderMonthCalendar() {
        val currentLocale = LocaleHelper.getSavedLocale(this)
        val monthYearFormat = SimpleDateFormat("MMMM yyyy", currentLocale)
        val raw = monthYearFormat.format(displayedMonthCalendar.time)
        binding.tvFullMonthYearTitle.text = raw.replaceFirstChar { it.uppercase(currentLocale) }

        monthCalendarAdapter.setMonthData(
            monthYear = displayedMonthCalendar,
            selected = selectedCalendarDay,
            reminders = cachedAllReminders
        )
    }

    private fun updateMonthYearHeader(cal: Calendar) {
        val currentLocale = LocaleHelper.getSavedLocale(this)
        val monthYearFormat = SimpleDateFormat("MMMM yyyy", currentLocale)
        val rawMonthYear = monthYearFormat.format(cal.time)
        binding.tvCalendarMonthYear.text = rawMonthYear.replaceFirstChar { it.uppercase(currentLocale) }
    }

    private fun onDateSelected(newDate: Calendar, fromMonthView: Boolean) {
        selectedCalendarDay = newDate.clone() as Calendar

        horizontalCalendarAdapter.setSelectedDay(selectedCalendarDay)
        monthCalendarAdapter.setSelectedDay(selectedCalendarDay)

        updateMonthYearHeader(selectedCalendarDay)
        loadReminders(scrollToTop = true)

        if (fromMonthView) {
            // Also update displayed month if needed
            displayedMonthCalendar = selectedCalendarDay.clone() as Calendar
            renderMonthCalendar()
            scrollToSelectedDay()
        }
    }

    private fun scrollToSelectedDay() {
        val position = horizontalCalendarAdapter.getPositionForDate(selectedCalendarDay)
        if (position != -1) {
            val targetPos = (position - 3).coerceAtLeast(0)
            binding.rvCalendarDays.smoothScrollToPosition(targetPos)
        }
    }

    private fun setupRecyclerView() {
        reminderAdapter = ReminderAdapter(
            onItemClick = { item ->
                ReminderDetailBottomSheetDialog.show(
                    supportFragmentManager,
                    item.id,
                    onUpdated = { loadReminders() },
                    onDeleted = { loadReminders() }
                )
            },
            onMarkDoneClick = { item, _ ->
                ReminderRepository.getInstance(this).setReminderDone(item.id, true)
                Toast.makeText(this, R.string.msg_marked_as_done, Toast.LENGTH_SHORT).show()
                loadReminders()
            },
            onUndoClick = { item, _ ->
                ReminderRepository.getInstance(this).setReminderDone(item.id, false)
                Toast.makeText(this, R.string.msg_marked_as_undone, Toast.LENGTH_SHORT).show()
                loadReminders()
            }
        )

        binding.rvReminders.layoutManager = LinearLayoutManager(this)
        binding.rvReminders.adapter = reminderAdapter
    }

    private fun loadReminders(scrollToTop: Boolean = false) {
        val effectiveProfileId = profileId ?: ProfileRepository.getInstance(this).getPrimaryProfile().id
        val reminderRepo = ReminderRepository.getInstance(this)
        val allReminders = reminderRepo.getRemindersForProfile(effectiveProfileId)
        cachedAllReminders = allReminders

        // Top bar plus button: only visible if profile has any reminder (user request 3)
        binding.btnAddReminder.visibility = if (allReminders.isEmpty()) View.GONE else View.VISIBLE

        // Filter reminders scheduled for selected date
        val scheduledReminders = allReminders.filter { it.isScheduledForDay(selectedCalendarDay, this) }

        if (scheduledReminders.isEmpty()) {
            binding.rvReminders.visibility = View.GONE
            binding.layoutEmptyState.visibility = View.VISIBLE
            binding.btnEmptyAddReminder.visibility = View.VISIBLE
        } else {
            binding.rvReminders.visibility = View.VISIBLE
            binding.layoutEmptyState.visibility = View.GONE
            val sorted = scheduledReminders.sortedWith(
                compareBy<ReminderItem> { it.isDone }
                    .thenBy { com.example.healthcareai.util.ReminderScheduler.parseTimeToMinutes(it.timeList.firstOrNull().orEmpty()) }
            )
            if (scrollToTop) {
                binding.rvReminders.scrollToPosition(0)
            }
            reminderAdapter.submitList(sorted) {
                if (scrollToTop && !isFinishing && !isDestroyed) {
                    binding.rvReminders.scrollToPosition(0)
                }
            }
        }

        horizontalCalendarAdapter.setAllReminders(allReminders)
        if (isMonthCalendarExpanded) {
            renderMonthCalendar()
        }
    }

    companion object {
        const val EXTRA_PROFILE_ID = "extra_profile_id"
        const val EXTRA_HIGHLIGHT_REMINDER_ID = "extra_highlight_reminder_id"

        fun start(context: Context, profileId: String? = null, highlightReminderId: String? = null) {
            val intent = Intent(context, ReminderActivity::class.java).apply {
                profileId?.let { putExtra(EXTRA_PROFILE_ID, it) }
                highlightReminderId?.let { putExtra(EXTRA_HIGHLIGHT_REMINDER_ID, it) }
            }
            context.startActivity(intent)
        }
    }
}
