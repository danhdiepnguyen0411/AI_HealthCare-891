package com.example.healthcareai.ui.reminder

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.healthcareai.R
import com.example.healthcareai.data.model.ReminderItem
import com.example.healthcareai.databinding.ItemMonthCalendarDayBinding
import java.util.Calendar

data class MonthDayData(
    val calendar: Calendar,
    val isCurrentMonth: Boolean
)

class MonthCalendarAdapter(
    private val onDayClicked: (Calendar) -> Unit
) : RecyclerView.Adapter<MonthCalendarAdapter.MonthDayViewHolder>() {

    private val dayItems = mutableListOf<MonthDayData>()
    private var selectedCalendar: Calendar = Calendar.getInstance()
    private val todayCalendar: Calendar = Calendar.getInstance()
    private var remindersList: List<ReminderItem> = emptyList()

    fun setMonthData(
        monthYear: Calendar,
        selected: Calendar,
        reminders: List<ReminderItem> = remindersList
    ) {
        remindersList = reminders
        selectedCalendar = selected.clone() as Calendar

        dayItems.clear()
        val cal = monthYear.clone() as Calendar
        cal.set(Calendar.DAY_OF_MONTH, 1)

        val targetMonth = cal.get(Calendar.MONTH)
        val targetYear = cal.get(Calendar.YEAR)

        // Find leading days to Monday
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val leadingDays = when (dayOfWeek) {
            Calendar.SUNDAY -> 6
            else -> dayOfWeek - Calendar.MONDAY
        }

        cal.add(Calendar.DAY_OF_MONTH, -leadingDays)

        // Generate leading days + month days + trailing days
        while (true) {
            val isCurMonth = cal.get(Calendar.MONTH) == targetMonth && cal.get(Calendar.YEAR) == targetYear
            dayItems.add(MonthDayData(cal.clone() as Calendar, isCurMonth))

            cal.add(Calendar.DAY_OF_MONTH, 1)
            // Stop when we reached next month and row ends on Sunday
            if (cal.get(Calendar.MONTH) != targetMonth && cal.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY) {
                break
            }
            // Safeguard against infinite loops (max 42 cells)
            if (dayItems.size >= 42) break
        }

        notifyDataSetChanged()
    }

    fun setSelectedDay(selected: Calendar) {
        selectedCalendar = selected.clone() as Calendar
        notifyDataSetChanged()
    }

    fun setAllReminders(reminders: List<ReminderItem>) {
        remindersList = reminders
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MonthDayViewHolder {
        val binding = ItemMonthCalendarDayBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MonthDayViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MonthDayViewHolder, position: Int) {
        holder.bind(dayItems[position])
    }

    override fun getItemCount(): Int = dayItems.size

    inner class MonthDayViewHolder(
        private val binding: ItemMonthCalendarDayBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(data: MonthDayData) {
            val context = binding.root.context
            val dayCal = data.calendar
            val isCurrentMonth = data.isCurrentMonth

            val dayNumber = dayCal.get(Calendar.DAY_OF_MONTH)
            binding.tvDayNumber.text = dayNumber.toString()

            val isSelected = HorizontalCalendarAdapter.isSameDay(dayCal, selectedCalendar)
            val isToday = HorizontalCalendarAdapter.isSameDay(dayCal, todayCalendar)
            val hasReminders = remindersList.any { it.isScheduledForDay(dayCal, context) }

            val textColorNormal = ContextCompat.getColor(context, R.color.color_text)
            val textColorDimmed = Color.parseColor("#C4CAD4")
            val primaryColor = ContextCompat.getColor(context, R.color.brand_primary)
            val textWhite = ContextCompat.getColor(context, R.color.white)

            when {
                isSelected && isToday -> {
                    binding.layoutDayContainer.setBackgroundResource(R.drawable.bg_circle_calendar_today_selected)
                    binding.tvDayNumber.setTextColor(textWhite)
                    if (hasReminders) {
                        binding.viewDotIndicator.visibility = View.VISIBLE
                        binding.viewDotIndicator.setBackgroundResource(R.drawable.bg_circle_calendar_dot)
                    } else {
                        binding.viewDotIndicator.visibility = View.GONE
                    }
                }
                isSelected -> {
                    binding.layoutDayContainer.setBackgroundResource(R.drawable.bg_circle_calendar_selected)
                    binding.tvDayNumber.setTextColor(textWhite)
                    if (hasReminders) {
                        binding.viewDotIndicator.visibility = View.VISIBLE
                        binding.viewDotIndicator.setBackgroundResource(R.drawable.bg_circle_calendar_dot)
                    } else {
                        binding.viewDotIndicator.visibility = View.GONE
                    }
                }
                isToday && isCurrentMonth -> {
                    binding.layoutDayContainer.setBackgroundResource(R.drawable.bg_circle_calendar_today)
                    binding.tvDayNumber.setTextColor(primaryColor)
                    if (hasReminders) {
                        binding.viewDotIndicator.visibility = View.VISIBLE
                        binding.viewDotIndicator.setBackgroundResource(R.drawable.bg_circle_calendar_dot_blue)
                    } else {
                        binding.viewDotIndicator.visibility = View.GONE
                    }
                }
                isCurrentMonth -> {
                    binding.layoutDayContainer.background = null
                    binding.tvDayNumber.setTextColor(textColorNormal)
                    if (hasReminders) {
                        binding.viewDotIndicator.visibility = View.VISIBLE
                        binding.viewDotIndicator.setBackgroundResource(R.drawable.bg_circle_calendar_dot_blue)
                    } else {
                        binding.viewDotIndicator.visibility = View.GONE
                    }
                }
                else -> {
                    binding.layoutDayContainer.background = null
                    binding.tvDayNumber.setTextColor(textColorDimmed)
                    binding.viewDotIndicator.visibility = View.GONE
                }
            }

            binding.layoutDayRoot.setOnClickListener {
                onDayClicked(dayCal)
            }
        }
    }
}
