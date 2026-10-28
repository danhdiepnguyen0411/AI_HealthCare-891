package com.example.healthcareai.ui.reminder

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.example.healthcareai.R
import com.example.healthcareai.data.model.HealthRecordItem
import com.example.healthcareai.data.repository.HealthRecordRepository
import com.example.healthcareai.databinding.ActivityAddAllergyBinding
import com.example.healthcareai.ui.base.BaseActivity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

class AddAllergyActivity : BaseActivity() {

    private lateinit var binding: ActivityAddAllergyBinding
    private var editingRecordId: String? = null
    private var existingRecord: HealthRecordItem? = null
    private var profileId: String? = null
    private var selectedType: String = "Medication"
    private var selectedDateFormatted: String = ""

    private val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.US)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddAllergyBinding.inflate(layoutInflater)
        setContentView(binding.root)

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

        // Type chips
        val chips = listOf(
            binding.chipTypeMedication to "Medication",
            binding.chipTypeFood to "Food",
            binding.chipTypeOther to "Other"
        )

        fun updateChips(type: String) {
            selectedType = type
            for ((chipView, chipType) in chips) {
                if (chipType.equals(type, ignoreCase = true)) {
                    chipView.setBackgroundResource(R.drawable.bg_health_pill_chip_selected)
                    chipView.setTextColor(Color.WHITE)
                } else {
                    chipView.setBackgroundResource(R.drawable.bg_health_pill_chip_unselected)
                    chipView.setTextColor(Color.parseColor("#3772FF"))
                }
            }
        }

        for ((chipView, chipType) in chips) {
            chipView.setOnClickListener {
                updateChips(chipType)
            }
        }

        // Severity selector
        binding.cardSeverity.setOnClickListener {
            hideKeyboard()
            val currentSeverity = binding.tvSeverity.text.toString().trim()
            SelectSeverityBottomSheetDialog.show(
                supportFragmentManager,
                if (currentSeverity.isNotEmpty()) currentSeverity else SelectSeverityBottomSheetDialog.SEVERITY_MILD
            ) { selectedSeverity ->
                binding.tvSeverity.text = selectedSeverity
            }
        }

        // Date picker for First noticed
        binding.cardFirstNoticed.setOnClickListener {
            hideKeyboard()
            val cal = Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    val chosenCal = Calendar.getInstance().apply {
                        set(year, month, dayOfMonth)
                    }
                    selectedDateFormatted = dateFormat.format(chosenCal.time)
                    binding.tvFirstNoticed.text = selectedDateFormatted
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        binding.btnSaveAllergy.setOnClickListener {
            val allergenName = binding.etAllergenName.text?.toString()?.trim().orEmpty()
            if (allergenName.isEmpty()) {
                binding.etAllergenName.requestFocus()
                Toast.makeText(this, "Please enter allergen name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            hideKeyboard()

            val reaction = binding.etReaction.text?.toString()?.trim().orEmpty()
            val severity = binding.tvSeverity.text?.toString()?.trim().orEmpty()
            val notes = binding.etNotes.text?.toString()?.trim().orEmpty()

            val details = when {
                reaction.isNotBlank() && severity.isNotBlank() -> "$reaction · $severity"
                reaction.isNotBlank() -> reaction
                severity.isNotBlank() -> severity
                else -> selectedType
            }

            val effectiveProfileId = existingRecord?.profileId 
                ?: profileId 
                ?: com.example.healthcareai.data.repository.ProfileRepository.getInstance(this).getPrimaryProfile().id

            val record = existingRecord?.copy(
                name = allergenName,
                details = details,
                type = selectedType,
                reaction = reaction,
                severity = severity,
                firstNoticedDate = selectedDateFormatted,
                notes = notes,
                profileId = effectiveProfileId
            ) ?: HealthRecordItem(
                id = UUID.randomUUID().toString(),
                profileId = effectiveProfileId,
                category = "Allergies",
                name = allergenName,
                details = details,
                type = selectedType,
                reaction = reaction,
                severity = severity,
                firstNoticedDate = selectedDateFormatted,
                notes = notes,
                createdAt = System.currentTimeMillis()
            )

            HealthRecordRepository.getInstance(this).saveRecord(record)

            val resultIntent = Intent().apply {
                putExtra(AddHealthRecordActivity.EXTRA_RECORD_ID, record.id)
                putExtra(AddHealthRecordActivity.EXTRA_RECORD_NAME, record.name)
            }
            setResult(RESULT_OK, resultIntent)
            Toast.makeText(this, "Allergy saved", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun populateExistingData() {
        val record = existingRecord ?: return
        binding.tvTitle.text = "Edit allergy"
        binding.etAllergenName.setText(record.name)
        binding.etReaction.setText(record.reaction)
        if (record.severity.isNotBlank()) {
            binding.tvSeverity.text = record.severity
        }
        if (record.firstNoticedDate.isNotBlank()) {
            selectedDateFormatted = record.firstNoticedDate
            binding.tvFirstNoticed.text = record.firstNoticedDate
        }
        binding.etNotes.setText(record.notes)

        val chips = listOf(
            binding.chipTypeMedication to "Medication",
            binding.chipTypeFood to "Food",
            binding.chipTypeOther to "Other"
        )
        selectedType = if (record.type.isNotBlank()) record.type else "Medication"
        for ((chipView, chipType) in chips) {
            if (chipType.equals(selectedType, ignoreCase = true)) {
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
        const val EXTRA_PROFILE_ID = "extra_profile_id"

        fun start(context: Context, recordId: String? = null, profileId: String? = null) {
            val intent = Intent(context, AddAllergyActivity::class.java).apply {
                recordId?.let { putExtra(EXTRA_RECORD_ID, it) }
                profileId?.let { putExtra(EXTRA_PROFILE_ID, it) }
            }
            context.startActivity(intent)
        }
    }
}
