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
import com.example.healthcareai.databinding.ItemCalendarDayBinding
import com.example.healthcareai.util.LocaleHelper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class HorizontalCalendarAdapter(
    private val onDaySelected: (Calendar) -> Unit
) : RecyclerView.Adapter<HorizontalCalendarAdapter.DayViewHolder>() {

    private val days = mutableListOf<Calendar>()
    private var selectedCalendar: Calendar = Calendar.getInstance()
    private val todayCalendar: Calendar = Calendar.getInstance()
    private var remindersList: List<ReminderItem> = emptyList()

    fun setDays(newDays: List<Calendar>, selected: Calendar) {
        days.clear()
        days.addAll(newDays)
        selectedCalendar = selected.clone() as Calendar
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

    fun getPositionForDate(date: Calendar): Int {
        for (i in days.indices) {
            if (isSameDay(days[i], date)) return i
        }
        return -1
    }

    fun getDateAtPosition(position: Int): Calendar? {
        return if (position in days.indices) days[position] else null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val binding = ItemCalendarDayBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DayViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        holder.bind(days[position])
    }

    override fun getItemCount(): Int = days.size

    inner class DayViewHolder(
        private val binding: ItemCalendarDayBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(dayCal: Calendar) {
            val context = binding.root.context
            val currentLocale = LocaleHelper.getSavedLocale(context)
            val dayNumberFormat = SimpleDateFormat("d", currentLocale)

            binding.tvDayNumber.text = dayNumberFormat.format(dayCal.time)
            binding.tvDayName.text = getDayOfWeekAbbreviation(dayCal, currentLocale)

            val isSelected = isSameDay(dayCal, selectedCalendar)
            val isToday = isSameDay(dayCal, todayCalendar)
            val hasReminders = remindersList.any { it.isScheduledForDay(dayCal, context) }

            val textColorNormal = ContextCompat.getColor(context, R.color.color_text)
            val subtextColorNormal = ContextCompat.getColor(context, R.color.color_subtext)
            val primaryColor = ContextCompat.getColor(context, R.color.brand_primary)
            val textSelected = ContextCompat.getColor(context, R.color.white)
            val subtextSelected = Color.parseColor("#DCE6FF")

            when {
                isSelected && isToday -> {
                    binding.root.setBackgroundResource(R.drawable.bg_calendar_day_today_selected)
                    binding.tvDayNumber.setTextColor(textSelected)
                    binding.tvDayName.setTextColor(subtextSelected)
                    if (hasReminders) {
                        binding.viewDayDot.visibility = View.VISIBLE
                        binding.viewDayDot.setBackgroundResource(R.drawable.bg_circle_calendar_dot)
                    } else {
                        binding.viewDayDot.visibility = View.GONE
                    }
                }
                isSelected -> {
                    binding.root.setBackgroundResource(R.drawable.bg_calendar_day_selected)
                    binding.tvDayNumber.setTextColor(textSelected)
                    binding.tvDayName.setTextColor(subtextSelected)
                    if (hasReminders) {
                        binding.viewDayDot.visibility = View.VISIBLE
                        binding.viewDayDot.setBackgroundResource(R.drawable.bg_circle_calendar_dot)
                    } else {
                        binding.viewDayDot.visibility = View.GONE
                    }
                }
                isToday -> {
                    binding.root.setBackgroundResource(R.drawable.bg_calendar_day_today)
                    binding.tvDayNumber.setTextColor(primaryColor)
                    binding.tvDayName.setTextColor(primaryColor)
                    if (hasReminders) {
                        binding.viewDayDot.visibility = View.VISIBLE
                        binding.viewDayDot.setBackgroundResource(R.drawable.bg_circle_calendar_dot_blue)
                    } else {
                        binding.viewDayDot.visibility = View.GONE
                    }
                }
                else -> {
                    binding.root.background = null
                    binding.tvDayNumber.setTextColor(textColorNormal)
                    binding.tvDayName.setTextColor(subtextColorNormal)
                    if (hasReminders) {
                        binding.viewDayDot.visibility = View.VISIBLE
                        binding.viewDayDot.setBackgroundResource(R.drawable.bg_circle_calendar_dot_blue)
                    } else {
                        binding.viewDayDot.visibility = View.GONE
                    }
                }
            }

            binding.root.setOnClickListener {
                val wasSelected = isSelected
                if (!wasSelected) {
                    selectedCalendar = dayCal.clone() as Calendar
                    notifyDataSetChanged()
                }
                onDaySelected(dayCal)
            }
        }
    }

    companion object {
        fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
            return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                    cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
        }

        fun getDayOfWeekAbbreviation(cal: Calendar, locale: Locale): String {
            val lang = locale.language.lowercase()
            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
            if (lang.startsWith("vi")) {
                return when (dayOfWeek) {
                    Calendar.MONDAY -> "T2"
                    Calendar.TUESDAY -> "T3"
                    Calendar.WEDNESDAY -> "T4"
                    Calendar.THURSDAY -> "T5"
                    Calendar.FRIDAY -> "T6"
                    Calendar.SATURDAY -> "T7"
                    Calendar.SUNDAY -> "CN"
                    else -> ""
                }
            }
            val dayFormat = SimpleDateFormat("EEE", locale)
            val formatted = dayFormat.format(cal.time)
            return if (formatted.length > 3) formatted.take(3) else formatted
        }
    }
}

