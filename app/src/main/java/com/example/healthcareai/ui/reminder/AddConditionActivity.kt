package com.example.healthcareai.ui.reminder

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import com.example.healthcareai.R
import com.example.healthcareai.data.model.HealthRecordItem
import com.example.healthcareai.data.repository.HealthRecordRepository
import com.example.healthcareai.databinding.ActivityAddConditionBinding
import com.example.healthcareai.ui.base.BaseActivity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

class AddConditionActivity : BaseActivity() {

    private lateinit var binding: ActivityAddConditionBinding
    private var editingRecordId: String? = null
    private var existingRecord: HealthRecordItem? = null
    private var selectedStatus: String = "Ongoing"
    private var selectedDiagnosisDateFormatted: String = ""

    private val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.US)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddConditionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        editingRecordId = intent.getStringExtra(EXTRA_RECORD_ID)
        if (!editingRecordId.isNullOrEmpty()) {
            existingRecord = HealthRecordRepository.getInstance(this).getAllRecords().find { it.id == editingRecordId }
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
            binding.chipStatusOngoing to "Ongoing",
            binding.chipStatusResolved to "Resolved",
            binding.chipStatusUnsure to "Unsure"
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

        // Date picker for Diagnosis date
        binding.cardDiagnosisDate.setOnClickListener {
            hideKeyboard()
            val cal = Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    val chosenCal = Calendar.getInstance().apply {
                        set(year, month, dayOfMonth)
                    }
                    selectedDiagnosisDateFormatted = dateFormat.format(chosenCal.time)
                    binding.tvDiagnosisDate.text = selectedDiagnosisDateFormatted
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        binding.btnSaveCondition.setOnClickListener {
            val conditionName = binding.etConditionName.text?.toString()?.trim().orEmpty()
            if (conditionName.isEmpty()) {
                binding.etConditionName.requestFocus()
                Toast.makeText(this, "Please enter condition name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            hideKeyboard()

            val clinician = binding.etClinician.text?.toString()?.trim().orEmpty()
            val clinic = binding.etClinic.text?.toString()?.trim().orEmpty()
            val notes = binding.etNotes.text?.toString()?.trim().orEmpty()

            val details = when {
                selectedDiagnosisDateFormatted.isNotBlank() -> "$selectedStatus · $selectedDiagnosisDateFormatted"
                clinic.isNotBlank() -> "$selectedStatus · $clinic"
                else -> selectedStatus
            }

            val record = existingRecord?.copy(
                name = conditionName,
                details = details,
                status = selectedStatus,
                clinician = clinician,
                clinic = clinic,
                diagnosisDate = selectedDiagnosisDateFormatted,
                notes = notes
            ) ?: HealthRecordItem(
                id = UUID.randomUUID().toString(),
                category = "Conditions",
                name = conditionName,
                details = details,
                status = selectedStatus,
                clinician = clinician,
                clinic = clinic,
                diagnosisDate = selectedDiagnosisDateFormatted,
                notes = notes,
                createdAt = System.currentTimeMillis()
            )

            HealthRecordRepository.getInstance(this).saveRecord(record)

            val resultIntent = Intent().apply {
                putExtra(AddHealthRecordActivity.EXTRA_RECORD_ID, record.id)
                putExtra(AddHealthRecordActivity.EXTRA_RECORD_NAME, record.name)
            }
            setResult(RESULT_OK, resultIntent)
            Toast.makeText(this, "Condition saved", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun populateExistingData() {
        val record = existingRecord ?: return
        binding.tvTitle.text = "Edit condition"
        binding.etConditionName.setText(record.name)
        binding.etClinician.setText(record.clinician)
        binding.etClinic.setText(record.clinic)
        if (record.diagnosisDate.isNotBlank()) {
            selectedDiagnosisDateFormatted = record.diagnosisDate
            binding.tvDiagnosisDate.text = record.diagnosisDate
        }
        binding.etNotes.setText(record.notes)

        val chips = listOf(
            binding.chipStatusOngoing to "Ongoing",
            binding.chipStatusResolved to "Resolved",
            binding.chipStatusUnsure to "Unsure"
        )
        selectedStatus = if (record.status.isNotBlank()) record.status else "Ongoing"
        for ((chipView, chipStatus) in chips) {
            if (chipStatus.equals(selectedStatus, ignoreCase = true)) {
                chipView.setBackgroundResource(R.drawable.bg_health_pill_chip_selected)
                chipView.setTextColor(Color.WHITE)
            } else {
                chipView.setBackgroundResource(R.drawable.bg_health_pill_chip_unselected)
                chipView.setTextColor(Color.parseColor("#3772FF"))
            }
        }
    }

    companion object {
        const val EXTRA_RECORD_ID = "extra_record_id"

        fun start(context: Context, recordId: String? = null) {
            val intent = Intent(context, AddConditionActivity::class.java).apply {
                recordId?.let { putExtra(EXTRA_RECORD_ID, it) }
            }
            context.startActivity(intent)
        }
    }
}
