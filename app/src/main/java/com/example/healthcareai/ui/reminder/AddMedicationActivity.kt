package com.example.healthcareai.ui.reminder

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.healthcareai.R
import com.example.healthcareai.data.model.HealthRecordItem
import com.example.healthcareai.data.repository.HealthRecordRepository
import com.example.healthcareai.databinding.ActivityAddMedicationBinding
import com.example.healthcareai.ui.base.BaseActivity
import java.util.UUID

class AddMedicationActivity : BaseActivity() {

    private lateinit var binding: ActivityAddMedicationBinding
    private var editingRecordId: String? = null
    private var existingRecord: HealthRecordItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddMedicationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        editingRecordId = intent.getStringExtra(EXTRA_RECORD_ID)
        if (!editingRecordId.isNullOrEmpty()) {
            existingRecord = HealthRecordRepository.getInstance(this).getAllRecords().find { it.id == editingRecordId }
            existingRecord?.let { record ->
                binding.tvTitle.text = "Edit medication"
                binding.etName.setText(record.name)
                binding.etReason.setText(record.reason)
                binding.etStrength.setText(record.strength)
                if (record.form.isNotBlank()) {
                    binding.tvForm.text = record.form
                }
                binding.etNotes.setText(record.notes)
            }
        }

        setupListeners()
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.cardForm.setOnClickListener {
            hideKeyboard()
            val currentForm = binding.tvForm.text.toString().trim()
            SelectFormBottomSheetDialog.show(
                supportFragmentManager,
                if (currentForm.isNotEmpty()) currentForm else SelectFormBottomSheetDialog.FORM_TABLET
            ) { selectedForm ->
                binding.tvForm.text = selectedForm
            }
        }

        binding.btnSaveMedication.setOnClickListener {
            val name = binding.etName.text?.toString()?.trim().orEmpty()
            if (name.isEmpty()) {
                binding.etName.requestFocus()
                Toast.makeText(this, "Please enter medication name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            hideKeyboard()

            val reason = binding.etReason.text?.toString()?.trim().orEmpty()
            val strength = binding.etStrength.text?.toString()?.trim().orEmpty()
            val form = binding.tvForm.text?.toString()?.trim().orEmpty()
            val notes = binding.etNotes.text?.toString()?.trim().orEmpty()

            val details = when {
                strength.isNotBlank() && form.isNotBlank() -> "$strength · $form"
                strength.isNotBlank() -> strength
                form.isNotBlank() -> form
                reason.isNotBlank() -> reason
                else -> ""
            }

            val record = existingRecord?.copy(
                name = name,
                details = details,
                reason = reason,
                strength = strength,
                form = form,
                notes = notes
            ) ?: HealthRecordItem(
                id = UUID.randomUUID().toString(),
                category = "Medication",
                name = name,
                details = details,
                reason = reason,
                strength = strength,
                form = form,
                notes = notes,
                createdAt = System.currentTimeMillis()
            )

            HealthRecordRepository.getInstance(this).saveRecord(record)

            val resultIntent = Intent().apply {
                putExtra(AddHealthRecordActivity.EXTRA_RECORD_ID, record.id)
                putExtra(AddHealthRecordActivity.EXTRA_RECORD_NAME, record.name)
            }
            setResult(RESULT_OK, resultIntent)
            Toast.makeText(this, "Medication saved", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    companion object {
        const val EXTRA_RECORD_ID = "extra_record_id"

        fun start(context: Context, recordId: String? = null) {
            val intent = Intent(context, AddMedicationActivity::class.java).apply {
                recordId?.let { putExtra(EXTRA_RECORD_ID, it) }
            }
            context.startActivity(intent)
        }
    }
}
