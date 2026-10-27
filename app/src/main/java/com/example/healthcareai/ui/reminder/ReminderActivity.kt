package com.example.healthcareai.ui.reminder

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.healthcareai.R
import com.example.healthcareai.data.repository.ReminderRepository
import com.example.healthcareai.databinding.ActivityReminderBinding
import com.example.healthcareai.ui.base.BaseActivity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ReminderActivity : BaseActivity() {

    private lateinit var binding: ActivityReminderBinding
    private lateinit var reminderAdapter: ReminderAdapter

    private var selectedCalendarDay: Calendar = Calendar.getInstance()
    private val weekDaysCalendar = mutableListOf<Calendar>()
    private var selectedDayIndex = 3 // default to middle day or today

    private val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.US)
    private val dayNumberFormat = SimpleDateFormat("d", Locale.US)
    private val dayNameFormat = SimpleDateFormat("EEE", Locale.US) // "Mon", "Tue", etc.

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReminderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupTopBar()
        setupCalendar()
        setupRecyclerView()
    }

    override fun onResume() {
        super.onResume()
        loadReminders()
    }

    private fun setupTopBar() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnAddReminder.setOnClickListener {
            startActivity(Intent(this, NewReminderActivity::class.java))
        }

        binding.btnEmptyAddReminder.setOnClickListener {
            startActivity(Intent(this, NewReminderActivity::class.java))
        }
    }

    private fun setupCalendar() {
        // Prepare current week (Mon -> Sun) centered around selected day
        computeWeekDays(selectedCalendarDay)

        binding.ivCalendarAction.setOnClickListener {
            val datePicker = DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    selectedCalendarDay.set(year, month, dayOfMonth)
                    computeWeekDays(selectedCalendarDay)
                    renderCalendar()
                },
                selectedCalendarDay.get(Calendar.YEAR),
                selectedCalendarDay.get(Calendar.MONTH),
                selectedCalendarDay.get(Calendar.DAY_OF_MONTH)
            )
            datePicker.show()
        }

        renderCalendar()
    }

    private fun computeWeekDays(centerDate: Calendar) {
        weekDaysCalendar.clear()
        val cal = centerDate.clone() as Calendar
        // Align to Monday of this week
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val diffToMonday = when (dayOfWeek) {
            Calendar.SUNDAY -> -6
            else -> Calendar.MONDAY - dayOfWeek
        }
        cal.add(Calendar.DAY_OF_MONTH, diffToMonday)

        selectedDayIndex = 0
        for (i in 0 until 7) {
            val dayCal = cal.clone() as Calendar
            weekDaysCalendar.add(dayCal)
            if (isSameDay(dayCal, centerDate)) {
                selectedDayIndex = i
            }
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun renderCalendar() {
        binding.tvCalendarMonthYear.text = monthYearFormat.format(selectedCalendarDay.time)

        val dayViews = listOf(
            binding.day0.root to (binding.day0.tvDayNumber to (binding.day0.tvDayName to binding.day0.viewDayDot)),
            binding.day1.root to (binding.day1.tvDayNumber to (binding.day1.tvDayName to binding.day1.viewDayDot)),
            binding.day2.root to (binding.day2.tvDayNumber to (binding.day2.tvDayName to binding.day2.viewDayDot)),
            binding.day3.root to (binding.day3.tvDayNumber to (binding.day3.tvDayName to binding.day3.viewDayDot)),
            binding.day4.root to (binding.day4.tvDayNumber to (binding.day4.tvDayName to binding.day4.viewDayDot)),
            binding.day5.root to (binding.day5.tvDayNumber to (binding.day5.tvDayName to binding.day5.viewDayDot)),
            binding.day6.root to (binding.day6.tvDayNumber to (binding.day6.tvDayName to binding.day6.viewDayDot))
        )

        val textColorNormal = ContextCompat.getColor(this, R.color.color_text)
        val subtextColorNormal = ContextCompat.getColor(this, R.color.color_subtext)
        val textSelected = ContextCompat.getColor(this, R.color.white)
        val subtextSelected = Color.parseColor("#DCE6FF")

        for (i in 0 until 7) {
            val (rootView, texts) = dayViews[i]
            val (tvNum, nameDot) = texts
            val (tvName, viewDot) = nameDot
            val dayCal = weekDaysCalendar[i]

            tvNum.text = dayNumberFormat.format(dayCal.time)
            // 2-letter abbreviation matching design e.g. Mo, Tu, We/Wed, Th, Fr, Sa, Su
            val rawName = dayNameFormat.format(dayCal.time)
            tvName.text = if (rawName.length >= 2) rawName.substring(0, 2) else rawName

            val isSelected = (i == selectedDayIndex)
            if (isSelected) {
                rootView.setBackgroundResource(R.drawable.bg_calendar_day_selected)
                tvNum.setTextColor(textSelected)
                tvName.setTextColor(subtextSelected)
                viewDot.visibility = View.VISIBLE
            } else {
                rootView.background = null
                tvNum.setTextColor(textColorNormal)
                tvName.setTextColor(subtextColorNormal)
                viewDot.visibility = View.GONE
            }

            rootView.setOnClickListener {
                if (selectedDayIndex != i) {
                    selectedDayIndex = i
                    selectedCalendarDay = dayCal.clone() as Calendar
                    renderCalendar()
                }
            }
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

    private fun loadReminders() {
        val reminders = ReminderRepository.getInstance(this).getAllReminders()
        if (reminders.isEmpty()) {
            binding.rvReminders.visibility = View.GONE
            binding.layoutEmptyState.visibility = View.VISIBLE
        } else {
            binding.rvReminders.visibility = View.VISIBLE
            binding.layoutEmptyState.visibility = View.GONE
            reminderAdapter.submitList(reminders)
        }
    }
}
