package com.example.healthcareai.ui.reminder

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.example.healthcareai.data.model.HealthRecordItem
import com.example.healthcareai.data.repository.HealthRecordRepository
import com.example.healthcareai.databinding.ActivityAddVaccinationBinding
import com.example.healthcareai.ui.base.BaseActivity
import java.util.UUID

class AddVaccinationActivity : BaseActivity() {

    private lateinit var binding: ActivityAddVaccinationBinding
    private var editingRecordId: String? = null
    private var existingRecord: HealthRecordItem? = null
    private var profileId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddVaccinationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        editingRecordId = intent.getStringExtra(EXTRA_RECORD_ID)
        profileId = intent.getStringExtra(EXTRA_PROFILE_ID)
        if (!editingRecordId.isNullOrEmpty()) {
            existingRecord = HealthRecordRepository.getInstance(this).getAllRecords().find { it.id == editingRecordId }
            if (profileId.isNullOrEmpty()) {
                profileId = existingRecord?.profileId
            }
            existingRecord?.let { record ->
                binding.tvTitle.text = "Edit vaccination"
                binding.etVaccineName.setText(record.name)
                binding.etDoseNumber.setText(record.doseNumber)
                binding.etBatchNumber.setText(record.batchNumber)
                binding.etClinic.setText(record.clinic)
                binding.etNotes.setText(record.notes)
            }
        }

        setupListeners()
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnSaveVaccination.setOnClickListener {
            val vaccineName = binding.etVaccineName.text?.toString()?.trim().orEmpty()
            if (vaccineName.isEmpty()) {
                binding.etVaccineName.requestFocus()
                Toast.makeText(this, "Please enter vaccine name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            hideKeyboard()

            val doseNumber = binding.etDoseNumber.text?.toString()?.trim().orEmpty()
            val batchNumber = binding.etBatchNumber.text?.toString()?.trim().orEmpty()
            val clinic = binding.etClinic.text?.toString()?.trim().orEmpty()
            val notes = binding.etNotes.text?.toString()?.trim().orEmpty()

            val details = when {
                doseNumber.isNotBlank() && clinic.isNotBlank() -> "$doseNumber · $clinic"
                doseNumber.isNotBlank() -> doseNumber
                clinic.isNotBlank() -> clinic
                else -> ""
            }

            val effectiveProfileId = existingRecord?.profileId 
                ?: profileId 
                ?: com.example.healthcareai.data.repository.ProfileRepository.getInstance(this).getPrimaryProfile().id

            val record = existingRecord?.copy(
                name = vaccineName,
                details = details,
                doseNumber = doseNumber,
                batchNumber = batchNumber,
                clinic = clinic,
                notes = notes,
                profileId = effectiveProfileId
            ) ?: HealthRecordItem(
                id = UUID.randomUUID().toString(),
                profileId = effectiveProfileId,
                category = "Vaccinations",
                name = vaccineName,
                details = details,
                doseNumber = doseNumber,
                batchNumber = batchNumber,
                clinic = clinic,
                notes = notes,
                createdAt = System.currentTimeMillis()
            )

            HealthRecordRepository.getInstance(this).saveRecord(record)

            val resultIntent = Intent().apply {
                putExtra(AddHealthRecordActivity.EXTRA_RECORD_ID, record.id)
                putExtra(AddHealthRecordActivity.EXTRA_RECORD_NAME, record.name)
            }
            setResult(RESULT_OK, resultIntent)
            Toast.makeText(this, "Vaccination saved", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    companion object {
        const val EXTRA_RECORD_ID = "extra_record_id"
        const val EXTRA_PROFILE_ID = "extra_profile_id"

        fun start(context: Context, recordId: String? = null, profileId: String? = null) {
            val intent = Intent(context, AddVaccinationActivity::class.java).apply {
                recordId?.let { putExtra(EXTRA_RECORD_ID, it) }
                profileId?.let { putExtra(EXTRA_PROFILE_ID, it) }
            }
            context.startActivity(intent)
        }
    }
}
