package com.example.healthcareai.ui.reminder

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.healthcareai.R
import com.example.healthcareai.databinding.DialogAddTimeCustomBinding
import com.example.healthcareai.databinding.ItemReminderTimeChipBinding
import com.example.healthcareai.util.DialogImmersiveHelper
import java.util.Calendar
import java.util.Locale

class AddTimeCustomDialog(
    context: Context,
    private val alreadyAddedTimes: MutableList<String>,
    private val onTimeAdded: (String) -> Unit,
    private val onTimeRemoved: ((String) -> Unit)? = null
) : Dialog(context) {

    private lateinit var binding: DialogAddTimeCustomBinding
    private var isPm: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        binding = DialogAddTimeCustomBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            val width = (context.resources.displayMetrics.widthPixels * 0.94f).toInt().coerceAtMost(
                (380 * context.resources.displayMetrics.density).toInt()
            )
            setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
            setDimAmount(0.55f)
            @Suppress("DEPRECATION")
            setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }

        setupInitialTime()
        setupListeners()
        renderAlreadyAddedChips()
    }

    private fun setupInitialTime() {
        val now = Calendar.getInstance()
        var hour12 = now.get(Calendar.HOUR)
        if (hour12 == 0) hour12 = 12
        val minute = now.get(Calendar.MINUTE)
        isPm = now.get(Calendar.AM_PM) == Calendar.PM

        binding.etHour.setText(String.format(Locale.US, "%02d", hour12))
        binding.etMinute.setText(String.format(Locale.US, "%02d", minute))

        updateAmPm(isPm)
    }

    private fun updateAmPm(pm: Boolean) {
        isPm = pm
        val textColor = ContextCompat.getColor(context, R.color.color_text)
        if (pm) {
            binding.btnTogglePm.setBackgroundResource(R.drawable.bg_ampm_bottom_selected)
            binding.tvPm.setTextColor(Color.WHITE)
            binding.btnToggleAm.background = null
            binding.tvAm.setTextColor(textColor)
        } else {
            binding.btnToggleAm.setBackgroundResource(R.drawable.bg_ampm_top_selected)
            binding.tvAm.setTextColor(Color.WHITE)
            binding.btnTogglePm.background = null
            binding.tvPm.setTextColor(textColor)
        }
    }

    private fun setupListeners() {
        binding.btnToggleAm.setOnClickListener {
            updateAmPm(false)
        }

        binding.btnTogglePm.setOnClickListener {
            updateAmPm(true)
        }

        // Auto move focus to minute when 2 digits typed in hour
        binding.etHour.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s?.length == 2 && binding.etHour.hasFocus()) {
                    binding.etMinute.requestFocus()
                    binding.etMinute.selectAll()
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnAddTime.setOnClickListener {
            val hourRaw = binding.etHour.text?.toString()?.trim().orEmpty()
            val minuteRaw = binding.etMinute.text?.toString()?.trim().orEmpty()

            var hour = hourRaw.toIntOrNull() ?: 12
            if (hour == 0) hour = 12
            if (hour > 12) hour = 12

            var minute = minuteRaw.toIntOrNull() ?: 0
            if (minute > 59) minute = 59
            if (minute < 0) minute = 0

            val amPmStr = if (isPm) "PM" else "AM"
            val formattedTime = String.format(Locale.US, "%d:%02d %s", hour, minute, amPmStr)

            if (alreadyAddedTimes.contains(formattedTime)) {
                Toast.makeText(context, R.string.msg_time_already_added, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            onTimeAdded(formattedTime)
            dismiss()
        }
    }

    private fun renderAlreadyAddedChips() {
        if (alreadyAddedTimes.isEmpty()) {
            binding.layoutAlreadyAddSection.visibility = View.GONE
        } else {
            binding.layoutAlreadyAddSection.visibility = View.VISIBLE
            binding.flowAlreadyAddedTimes.removeAllViews()

            for (timeStr in alreadyAddedTimes) {
                val chipBinding = ItemReminderTimeChipBinding.inflate(layoutInflater, binding.flowAlreadyAddedTimes, false)
                chipBinding.tvChipTime.text = timeStr
                chipBinding.btnRemoveTime.setOnClickListener {
                    alreadyAddedTimes.remove(timeStr)
                    onTimeRemoved?.invoke(timeStr)
                    renderAlreadyAddedChips()
                }
                binding.flowAlreadyAddedTimes.addView(chipBinding.root)
            }
        }
    }

    companion object {
        fun show(
            context: Context,
            alreadyAddedTimes: List<String>,
            onTimeAdded: (String) -> Unit,
            onTimeRemoved: ((String) -> Unit)? = null
        ): AddTimeCustomDialog {
            val mutableList = alreadyAddedTimes.toMutableList()
            val dialog = AddTimeCustomDialog(context, mutableList, onTimeAdded, onTimeRemoved)
            DialogImmersiveHelper.showImmersive(dialog)
            return dialog
        }
    }
}
