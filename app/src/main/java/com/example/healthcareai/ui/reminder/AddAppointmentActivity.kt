package com.example.healthcareai.ui.reminder

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.InputFilter
import android.widget.Toast
import com.example.healthcareai.R
import com.example.healthcareai.data.model.HealthRecordItem
import com.example.healthcareai.data.repository.HealthRecordRepository
import com.example.healthcareai.data.repository.ProfileRepository
import com.example.healthcareai.databinding.ActivityAddAppointmentBinding
import com.example.healthcareai.ui.base.BaseActivity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

class AddAppointmentActivity : BaseActivity() {

    private lateinit var binding: ActivityAddAppointmentBinding
    private var editingRecordId: String? = null
    private var existingRecord: HealthRecordItem? = null
    private var profileId: String? = null
    private var selectedStatus: String = STATUS_UPCOMING
    private var selectedAppointmentDateFormatted: String = ""

    private val dateTimeFormat = SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.US)
    private val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.US)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddAppointmentBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupHeaderTopMargin(binding.topBar, binding.root)

        binding.etAppointmentName.filters = arrayOf(InputFilter.LengthFilter(30))
        binding.etClinician.filters = arrayOf(InputFilter.LengthFilter(30))
        binding.etClinic.filters = arrayOf(InputFilter.LengthFilter(30))
        binding.etNotes.filters = arrayOf(InputFilter.LengthFilter(100))

        editingRecordId = intent.getStringExtra(EXTRA_RECORD_ID)
        profileId = intent.getStringExtra(EXTRA_PROFILE_ID)
        if (!editingRecordId.isNullOrEmpty()) {
            existingRecord = HealthRecordRepository.getInstance(this).getAllRecords().find { it.id == editingRecordId }
            if (profileId.isNullOrEmpty()) {
                profileId = existingRecord?.profileId
            }
        }

        setupListeners()
        populateExistingData()
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        // Status chips
        val chips = listOf(
            binding.chipStatusUpcoming to STATUS_UPCOMING,
            binding.chipStatusCompleted to STATUS_COMPLETED,
            binding.chipStatusCancelled to STATUS_CANCELLED
        )

        fun updateChips(status: String) {
            selectedStatus = status
            for ((chipView, chipStatus) in chips) {
                if (chipStatus.equals(status, ignoreCase = true)) {
                    chipView.setBackgroundResource(R.drawable.bg_health_pill_chip_selected)
                    chipView.setTextColor(Color.WHITE)
                } else {
                    chipView.setBackgroundResource(R.drawable.bg_health_pill_chip_unselected)
                    chipView.setTextColor(Color.parseColor("#3772FF"))
                }
            }
        }

        for ((chipView, chipStatus) in chips) {
            chipView.setOnClickListener {
                updateChips(chipStatus)
            }
        }

        // Date & Time picker for Appointment
        binding.cardAppointmentDate.setOnClickListener {
            hideKeyboard()
            val cal = Calendar.getInstance()
            if (selectedAppointmentDateFormatted.isNotBlank()) {
                try {
                    dateTimeFormat.parse(selectedAppointmentDateFormatted)?.let { cal.time = it }
                } catch (_: Exception) {
                    try {
                        dateFormat.parse(selectedAppointmentDateFormatted)?.let { cal.time = it }
                    } catch (_: Exception) {}
                }
            }

            val datePickerDialog = DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    val chosenCal = Calendar.getInstance().apply {
                        set(Calendar.YEAR, year)
                        set(Calendar.MONTH, month)
                        set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    }

                    // Prompt for time as well
                    val timePickerDialog = TimePickerDialog(
                        this,
                        { _, hourOfDay, minute ->
                            chosenCal.set(Calendar.HOUR_OF_DAY, hourOfDay)
                            chosenCal.set(Calendar.MINUTE, minute)
                            selectedAppointmentDateFormatted = dateTimeFormat.format(chosenCal.time)
                            binding.tvAppointmentDate.text = com.example.healthcareai.util.LocaleHelper.formatDateTime(this, chosenCal.time)
                        },
                        cal.get(Calendar.HOUR_OF_DAY),
                        cal.get(Calendar.MINUTE),
                        false
                    )
                    timePickerDialog.setOnCancelListener {
                        // User cancelled time picker: fallback to date only
                        selectedAppointmentDateFormatted = dateFormat.format(chosenCal.time)
                        binding.tvAppointmentDate.text = com.example.healthcareai.util.LocaleHelper.formatDate(this, chosenCal.time)
                    }
                    timePickerDialog.show()
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            )
            datePickerDialog.show()
        }

        binding.btnSaveAppointment.setOnClickListener {
            val appointmentName = binding.etAppointmentName.text?.toString()?.trim().orEmpty()
            if (appointmentName.isEmpty()) {
                binding.etAppointmentName.requestFocus()
                Toast.makeText(this, R.string.error_appointment_name_empty, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            hideKeyboard()

            val clinician = binding.etClinician.text?.toString()?.trim().orEmpty()
            val clinic = binding.etClinic.text?.toString()?.trim().orEmpty()
            val notes = binding.etNotes.text?.toString()?.trim().orEmpty()
            val dateStr = selectedAppointmentDateFormatted.ifBlank {
                binding.tvAppointmentDate.text?.toString()?.trim().orEmpty()
            }

            val details = when {
                dateStr.isNotBlank() && clinic.isNotBlank() -> "$dateStr · $clinic"
                dateStr.isNotBlank() && clinician.isNotBlank() -> "$dateStr · $clinician"
                dateStr.isNotBlank() -> dateStr
                clinician.isNotBlank() && clinic.isNotBlank() -> "$clinician · $clinic"
                clinician.isNotBlank() -> clinician
                clinic.isNotBlank() -> clinic
                else -> ""
            }

            val effectiveProfileId = existingRecord?.profileId
                ?: profileId
                ?: ProfileRepository.getInstance(this).getPrimaryProfile().id

            val record = existingRecord?.copy(
                name = appointmentName,
                details = details,
                status = selectedStatus,
                clinician = clinician,
                clinic = clinic,
                diagnosisDate = dateStr,
                notes = notes,
                profileId = effectiveProfileId
            ) ?: HealthRecordItem(
                id = UUID.randomUUID().toString(),
                profileId = effectiveProfileId,
                category = "Appointments",
                name = appointmentName,
                details = details,
                status = selectedStatus,
                clinician = clinician,
                clinic = clinic,
                diagnosisDate = dateStr,
                notes = notes,
                createdAt = System.currentTimeMillis()
            )

            HealthRecordRepository.getInstance(this).saveRecord(record)

            val resultIntent = Intent().apply {
                putExtra(AddHealthRecordActivity.EXTRA_RECORD_ID, record.id)
                putExtra(AddHealthRecordActivity.EXTRA_RECORD_NAME, record.name)
            }
            setResult(RESULT_OK, resultIntent)
            Toast.makeText(this, R.string.msg_appointment_saved, Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun populateExistingData() {
        val record = existingRecord ?: return
        binding.tvTitle.text = getString(R.string.title_edit_appointment)
        binding.etAppointmentName.setText(record.name)
        binding.etClinician.setText(record.clinician)
        binding.etClinic.setText(record.clinic)
        binding.etNotes.setText(record.notes)

        if (record.diagnosisDate.isNotBlank()) {
            selectedAppointmentDateFormatted = record.diagnosisDate
            binding.tvAppointmentDate.text = com.example.healthcareai.util.LocaleHelper.formatStoredDate(this, record.diagnosisDate)
        }

        if (record.status.isNotBlank()) {
            selectedStatus = record.status
            when (record.status.lowercase()) {
                STATUS_COMPLETED.lowercase() -> {
                    binding.chipStatusUpcoming.setBackgroundResource(R.drawable.bg_health_pill_chip_unselected)
                    binding.chipStatusUpcoming.setTextColor(Color.parseColor("#3772FF"))
                    binding.chipStatusCompleted.setBackgroundResource(R.drawable.bg_health_pill_chip_selected)
                    binding.chipStatusCompleted.setTextColor(Color.WHITE)
                    binding.chipStatusCancelled.setBackgroundResource(R.drawable.bg_health_pill_chip_unselected)
                    binding.chipStatusCancelled.setTextColor(Color.parseColor("#3772FF"))
                }
                STATUS_CANCELLED.lowercase() -> {
                    binding.chipStatusUpcoming.setBackgroundResource(R.drawable.bg_health_pill_chip_unselected)
                    binding.chipStatusUpcoming.setTextColor(Color.parseColor("#3772FF"))
                    binding.chipStatusCompleted.setBackgroundResource(R.drawable.bg_health_pill_chip_unselected)
                    binding.chipStatusCompleted.setTextColor(Color.parseColor("#3772FF"))
                    binding.chipStatusCancelled.setBackgroundResource(R.drawable.bg_health_pill_chip_selected)
                    binding.chipStatusCancelled.setTextColor(Color.WHITE)
                }
                else -> {
                    binding.chipStatusUpcoming.setBackgroundResource(R.drawable.bg_health_pill_chip_selected)
                    binding.chipStatusUpcoming.setTextColor(Color.WHITE)
                    binding.chipStatusCompleted.setBackgroundResource(R.drawable.bg_health_pill_chip_unselected)
                    binding.chipStatusCompleted.setTextColor(Color.parseColor("#3772FF"))
                    binding.chipStatusCancelled.setBackgroundResource(R.drawable.bg_health_pill_chip_unselected)
                    binding.chipStatusCancelled.setTextColor(Color.parseColor("#3772FF"))
                }
            }
        }
    }

    companion object {
        const val EXTRA_RECORD_ID = "extra_record_id"
        const val EXTRA_PROFILE_ID = "extra_profile_id"

        const val STATUS_UPCOMING = "Upcoming"
        const val STATUS_COMPLETED = "Completed"
        const val STATUS_CANCELLED = "Cancelled"

        fun start(context: Context, recordId: String? = null, profileId: String? = null) {
            val intent = Intent(context, AddAppointmentActivity::class.java).apply {
                recordId?.let { putExtra(EXTRA_RECORD_ID, it) }
                profileId?.let { putExtra(EXTRA_PROFILE_ID, it) }
            }
            context.startActivity(intent)
        }
    }
}

