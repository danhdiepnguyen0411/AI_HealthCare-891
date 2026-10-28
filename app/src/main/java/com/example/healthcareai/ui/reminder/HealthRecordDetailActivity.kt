package com.example.healthcareai.ui.reminder

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import com.example.healthcareai.R
import com.example.healthcareai.data.model.HealthRecordItem
import com.example.healthcareai.data.repository.HealthRecordRepository
import com.example.healthcareai.databinding.ActivityHealthRecordDetailBinding
import com.example.healthcareai.ui.base.BaseActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

open class HealthRecordDetailActivity : BaseActivity() {

    private lateinit var binding: ActivityHealthRecordDetailBinding
    private var recordId: String? = null
    private var currentRecord: HealthRecordItem? = null
    private val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.US)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHealthRecordDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        recordId = intent.getStringExtra(EXTRA_RECORD_ID)
        if (recordId.isNullOrEmpty()) {
            finish()
            return
        }

        setupWindowInsets()
        setupListeners()
        loadData()
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.rootHealthRecordDetail) { _, insets ->
            val statusBarTop = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            val density = resources.displayMetrics.density
            val targetTopMargin = if (statusBarTop > 0) {
                statusBarTop + (8 * density).toInt()
            } else {
                (16 * density).toInt()
            }
            binding.topBar.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                if (topMargin != targetTopMargin) {
                    topMargin = targetTopMargin
                }
            }

            val navBarBottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            binding.layoutBottomAction.updatePadding(bottom = (24 * density).toInt() + navBarBottom)
            insets
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun loadData() {
        val id = recordId ?: return
        val record = HealthRecordRepository.getInstance(this).getAllRecords().find { it.id == id }
        if (record == null) {
            finish()
            return
        }
        currentRecord = record
        bindRecord(record)
    }

    private fun bindRecord(record: HealthRecordItem) {
        val cat = record.category.trim().lowercase()
        when {
            cat == "medication" || cat == "medications" -> bindMedication(record)
            cat == "allergy" || cat == "allergies" -> bindAllergy(record)
            cat == "condition" || cat == "conditions" -> bindCondition(record)
            cat == "vaccination" || cat == "vaccinations" -> bindVaccination(record)
            else -> bindMedication(record)
        }
    }

    private fun bindMedication(record: HealthRecordItem) {
        binding.tvTopBarTitle.text = getString(R.string.title_medication_details)
        binding.flIconContainer.setBackgroundResource(R.drawable.bg_icon_medication)
        binding.ivRecordIcon.setImageResource(R.drawable.ic_medications)
        binding.ivRecordIcon.setColorFilter(Color.parseColor("#0F67FE"))

        binding.tvRecordName.text = record.name
        binding.tvRecordSubtitle.visibility = View.GONE
        binding.tvStatusBadge.visibility = View.GONE

        // Row 1: Reason | Strength
        binding.tvLabelRow1Left.text = "Reason"
        binding.tvValueRow1Left.text = record.reason.ifBlank { "Not provided" }

        binding.tvLabelRow1Right.text = "Strength"
        binding.tvValueRow1Right.text = record.strength.ifBlank { "Not provided" }
        binding.layoutRow1Right.visibility = View.VISIBLE

        // Row 2: Form | (hidden)
        binding.tvLabelRow2Left.text = "Form"
        binding.tvValueRow2Left.text = record.form.ifBlank { "Not provided" }

        binding.layoutRow2Right.visibility = View.INVISIBLE

        // Row 3: Notes
        binding.tvLabelNotes.text = "Notes"
        binding.tvValueNotes.text = record.notes.ifBlank { "Not provided" }

        // Bottom button
        binding.tvEditButtonText.text = "Edit medication"
        binding.btnEditRecord.setOnClickListener {
            AddMedicationActivity.start(this, record.id)
        }
        binding.btnUpdateStatus.visibility = View.GONE
    }

    private fun bindAllergy(record: HealthRecordItem) {
        binding.tvTopBarTitle.text = getString(R.string.title_allergy_details)
        binding.flIconContainer.setBackgroundResource(R.drawable.bg_icon_allergy)
        binding.ivRecordIcon.setImageResource(R.drawable.ic_allergies)
        binding.ivRecordIcon.setColorFilter(Color.parseColor("#CA8E2E"))

        binding.tvRecordName.text = record.name
        binding.tvRecordSubtitle.visibility = View.VISIBLE
        binding.tvRecordSubtitle.text = record.type.ifBlank { "Medication" }
        binding.tvStatusBadge.visibility = View.GONE

        // Row 1: Reaction | Severity
        binding.tvLabelRow1Left.text = "Reaction"
        binding.tvValueRow1Left.text = record.reaction.ifBlank { "Not provided" }

        binding.tvLabelRow1Right.text = "Severity"
        binding.tvValueRow1Right.text = record.severity.ifBlank { "Not provided" }
        binding.layoutRow1Right.visibility = View.VISIBLE

        // Row 2: First noticed | Updated
        binding.tvLabelRow2Left.text = "First noticed"
        binding.tvValueRow2Left.text = record.firstNoticedDate.ifBlank { "Not provided" }

        binding.tvLabelRow2Right.text = "Updated"
        binding.tvValueRow2Right.text = dateFormat.format(Date(record.createdAt))
        binding.layoutRow2Right.visibility = View.VISIBLE

        // Row 3: Note
        binding.tvLabelNotes.text = "Note"
        binding.tvValueNotes.text = record.notes.ifBlank { "Not provided" }

        // Bottom button
        binding.tvEditButtonText.text = "Edit allergy"
        binding.btnEditRecord.setOnClickListener {
            AddAllergyActivity.start(this, record.id)
        }
        binding.btnUpdateStatus.visibility = View.GONE
    }

    private fun bindCondition(record: HealthRecordItem) {
        binding.tvTopBarTitle.text = getString(R.string.title_condition_details)
        binding.flIconContainer.setBackgroundResource(R.drawable.bg_icon_condition)
        binding.ivRecordIcon.setImageResource(R.drawable.ic_conditions)
        binding.ivRecordIcon.setColorFilter(Color.parseColor("#BC4255"))

        binding.tvRecordName.text = record.name
        binding.tvRecordSubtitle.visibility = View.GONE
        binding.tvStatusBadge.visibility = View.VISIBLE
        binding.tvStatusBadge.text = record.status.ifBlank { "Ongoing" }

        // Row 1: Clinician | Clinic
        binding.tvLabelRow1Left.text = "Clinician"
        binding.tvValueRow1Left.text = record.clinician.ifBlank { "Not provided" }

        binding.tvLabelRow1Right.text = "Clinic"
        binding.tvValueRow1Right.text = record.clinic.ifBlank { "Not provided" }
        binding.layoutRow1Right.visibility = View.VISIBLE

        // Row 2: Diagnosis date | (hidden)
        binding.tvLabelRow2Left.text = "Diagnosis date"
        binding.tvValueRow2Left.text = record.diagnosisDate.ifBlank { "Not provided" }

        binding.layoutRow2Right.visibility = View.INVISIBLE

        // Row 3: Note
        binding.tvLabelNotes.text = "Note"
        binding.tvValueNotes.text = record.notes.ifBlank { "Not provided" }

        // Bottom buttons
        binding.tvEditButtonText.text = "Edit condition"
        binding.btnEditRecord.setOnClickListener {
            AddConditionActivity.start(this, record.id)
        }

        binding.btnUpdateStatus.visibility = View.VISIBLE
        binding.btnUpdateStatus.setOnClickListener {
            showUpdateStatusDialog(record)
        }
    }

    private fun bindVaccination(record: HealthRecordItem) {
        binding.tvTopBarTitle.text = getString(R.string.title_vaccination_details)
        binding.flIconContainer.setBackgroundResource(R.drawable.bg_icon_vaccination)
        binding.ivRecordIcon.setImageResource(R.drawable.ic_vaccination)
        binding.ivRecordIcon.setColorFilter(Color.parseColor("#00A86B"))

        binding.tvRecordName.text = record.name
        binding.tvRecordSubtitle.visibility = View.VISIBLE
        val subtitle = when {
            record.doseNumber.isNotBlank() && record.clinic.isNotBlank() -> "${record.doseNumber} ${record.clinic}"
            record.doseNumber.isNotBlank() -> record.doseNumber
            record.clinic.isNotBlank() -> record.clinic
            record.details.isNotBlank() -> record.details
            else -> "Not provided"
        }
        binding.tvRecordSubtitle.text = subtitle
        binding.tvStatusBadge.visibility = View.GONE

        // Row 1: Dose number | Batch number
        binding.tvLabelRow1Left.text = "Dose number"
        binding.tvValueRow1Left.text = record.doseNumber.ifBlank { "Not provided" }

        binding.tvLabelRow1Right.text = "Batch number"
        binding.tvValueRow1Right.text = record.batchNumber.ifBlank { "Not provided" }
        binding.layoutRow1Right.visibility = View.VISIBLE

        // Row 2: Clinic | Date received
        binding.tvLabelRow2Left.text = "Clinic"
        binding.tvValueRow2Left.text = record.clinic.ifBlank { "Not provided" }

        binding.tvLabelRow2Right.text = "Date received"
        binding.tvValueRow2Right.text = if (record.diagnosisDate.isNotBlank()) record.diagnosisDate else "Not provided"
        binding.layoutRow2Right.visibility = View.VISIBLE

        // Row 3: Note
        binding.tvLabelNotes.text = "Note"
        binding.tvValueNotes.text = record.notes.ifBlank { "Not provided" }

        // Bottom button
        binding.tvEditButtonText.text = "Edit vaccination"
        binding.btnEditRecord.setOnClickListener {
            AddVaccinationActivity.start(this, record.id)
        }
        binding.btnUpdateStatus.visibility = View.GONE
    }

    private fun showUpdateStatusDialog(record: HealthRecordItem) {
        val currentStatus = record.status.ifBlank { UpdateStatusBottomSheetDialog.STATUS_ONGOING }
        UpdateStatusBottomSheetDialog.show(
            supportFragmentManager,
            currentStatus
        ) { newStatus ->
            val updatedRecord = record.copy(status = newStatus)
            HealthRecordRepository.getInstance(this).saveRecord(updatedRecord)
            currentRecord = updatedRecord
            bindRecord(updatedRecord)
            Toast.makeText(this, "Status updated to $newStatus", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        const val EXTRA_RECORD_ID = "extra_record_id"
        const val EXTRA_CATEGORY = "extra_category"

        fun start(context: Context, recordId: String, category: String? = null) {
            val cat = category?.trim()?.lowercase().orEmpty()
            when {
                cat == "medication" || cat == "medications" -> MedicationDetailActivity.start(context, recordId)
                cat == "allergy" || cat == "allergies" -> AllergyDetailActivity.start(context, recordId)
                cat == "condition" || cat == "conditions" -> ConditionDetailActivity.start(context, recordId)
                cat == "vaccination" || cat == "vaccinations" -> VaccinationDetailActivity.start(context, recordId)
                else -> {
                    val intent = Intent(context, HealthRecordDetailActivity::class.java).apply {
                        putExtra(EXTRA_RECORD_ID, recordId)
                        category?.let { putExtra(EXTRA_CATEGORY, it) }
                    }
                    context.startActivity(intent)
                }
            }
        }
    }
}

class MedicationDetailActivity : HealthRecordDetailActivity() {
    companion object {
        fun start(context: Context, recordId: String) {
            val intent = Intent(context, MedicationDetailActivity::class.java).apply {
                putExtra(EXTRA_RECORD_ID, recordId)
                putExtra(EXTRA_CATEGORY, "Medication")
            }
            context.startActivity(intent)
        }
    }
}

class AllergyDetailActivity : HealthRecordDetailActivity() {
    companion object {
        fun start(context: Context, recordId: String) {
            val intent = Intent(context, AllergyDetailActivity::class.java).apply {
                putExtra(EXTRA_RECORD_ID, recordId)
                putExtra(EXTRA_CATEGORY, "Allergies")
            }
            context.startActivity(intent)
        }
    }
}

class ConditionDetailActivity : HealthRecordDetailActivity() {
    companion object {
        fun start(context: Context, recordId: String) {
            val intent = Intent(context, ConditionDetailActivity::class.java).apply {
                putExtra(EXTRA_RECORD_ID, recordId)
                putExtra(EXTRA_CATEGORY, "Conditions")
            }
            context.startActivity(intent)
        }
    }
}

class VaccinationDetailActivity : HealthRecordDetailActivity() {
    companion object {
        fun start(context: Context, recordId: String) {
            val intent = Intent(context, VaccinationDetailActivity::class.java).apply {
                putExtra(EXTRA_RECORD_ID, recordId)
                putExtra(EXTRA_CATEGORY, "Vaccinations")
            }
            context.startActivity(intent)
        }
    }
}
