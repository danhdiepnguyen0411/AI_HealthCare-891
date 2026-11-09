package com.example.healthcareai.ui.reminder

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.healthcareai.R
import com.example.healthcareai.data.model.HealthRecordItem
import com.example.healthcareai.data.repository.HealthRecordRepository
import com.example.healthcareai.databinding.ActivityHealthRecordListBinding
import com.example.healthcareai.databinding.ItemHealthRecordCardBinding
import com.example.healthcareai.ui.base.BaseActivity
import com.example.healthcareai.ui.dialog.DeleteConfirmationDialog
import com.example.healthcareai.ui.heartrate.view.SwipeRevealLayout
import com.example.healthcareai.util.LocaleHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HealthRecordListActivity : BaseActivity() {

    private lateinit var binding: ActivityHealthRecordListBinding
    private var currentCategory: String = CATEGORY_MEDICATION
    private var profileId: String? = null

    private lateinit var adapter: HealthRecordListAdapter
    private val dateFormat: SimpleDateFormat
        get() {
            val locale = LocaleHelper.getSavedLocale(this)
            val pattern = android.text.format.DateFormat.getBestDateTimePattern(locale, "MMMd")
            return SimpleDateFormat(pattern, locale)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHealthRecordListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentCategory = intent.getStringExtra(EXTRA_CATEGORY) ?: CATEGORY_MEDICATION
        profileId = intent.getStringExtra(EXTRA_PROFILE_ID)

        setupHeaderTopMargin(binding.topBar, binding.root)
        setupUI()
        setupRecyclerView()
        loadData()
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        when (currentCategory.trim().lowercase()) {
            CATEGORY_MEDICATION.lowercase(), "medications" -> {
                binding.tvTitle.text = getString(R.string.record_medications)
                binding.tvAddButtonText.text = getString(R.string.title_add_medication)
                binding.tvEmptyTitle.text = getString(R.string.no_records_yet_format, getString(R.string.record_medications).lowercase())
                binding.tvEmptySubtitle.text = getString(R.string.empty_records_desc)
            }
            CATEGORY_ALLERGIES.lowercase(), "allergy" -> {
                binding.tvTitle.text = getString(R.string.record_allergies)
                binding.tvAddButtonText.text = getString(R.string.title_add_allergy)
                binding.tvEmptyTitle.text = getString(R.string.no_records_yet_format, getString(R.string.record_allergies).lowercase())
                binding.tvEmptySubtitle.text = getString(R.string.empty_records_desc)
            }
            CATEGORY_CONDITIONS.lowercase(), "condition" -> {
                binding.tvTitle.text = getString(R.string.record_conditions)
                binding.tvAddButtonText.text = getString(R.string.title_add_condition)
                binding.tvEmptyTitle.text = getString(R.string.no_records_yet_format, getString(R.string.record_conditions).lowercase())
                binding.tvEmptySubtitle.text = getString(R.string.empty_records_desc)
            }
            CATEGORY_VACCINATIONS.lowercase(), "vaccination" -> {
                binding.tvTitle.text = getString(R.string.record_vaccinations)
                binding.tvAddButtonText.text = getString(R.string.title_add_vaccination)
                binding.tvEmptyTitle.text = getString(R.string.no_records_yet_format, getString(R.string.record_vaccinations).lowercase())
                binding.tvEmptySubtitle.text = getString(R.string.empty_records_desc)
            }
            CATEGORY_APPOINTMENTS.lowercase(), "appointment", "appointments" -> {
                binding.tvTitle.text = getString(R.string.category_appointments_title)
                binding.tvAddButtonText.text = getString(R.string.title_add_appointment)
                binding.tvEmptyTitle.text = getString(R.string.no_records_yet_format, getString(R.string.category_appointments_title).lowercase())
                binding.tvEmptySubtitle.text = getString(R.string.empty_records_desc)
            }
            else -> {
                binding.tvTitle.text = currentCategory
                binding.tvAddButtonText.text = getString(R.string.btn_add_record_format, currentCategory)
                binding.tvEmptyTitle.text = getString(R.string.no_records_yet_format, currentCategory)
                binding.tvEmptySubtitle.text = getString(R.string.empty_records_desc)
            }
        }

        binding.btnAddRecord.setOnClickListener {
            when (currentCategory.trim().lowercase()) {
                CATEGORY_MEDICATION.lowercase(), "medications" -> AddMedicationActivity.start(this, profileId = profileId)
                CATEGORY_ALLERGIES.lowercase(), "allergy" -> AddAllergyActivity.start(this, profileId = profileId)
                CATEGORY_CONDITIONS.lowercase(), "condition" -> AddConditionActivity.start(this, profileId = profileId)
                CATEGORY_VACCINATIONS.lowercase(), "vaccination" -> AddVaccinationActivity.start(this, profileId = profileId)
                CATEGORY_APPOINTMENTS.lowercase(), "appointment", "appointments" -> AddAppointmentActivity.start(this, profileId = profileId)
                else -> AddMedicationActivity.start(this, profileId = profileId)
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = HealthRecordListAdapter(
            items = emptyList(),
            onItemClick = { item ->
                HealthRecordDetailActivity.start(this, item.id, currentCategory, profileId = profileId ?: item.profileId)
            },
            onItemDelete = { item ->
                showDeleteConfirmationDialog(item)
            }
        )
        binding.rvHealthRecords.layoutManager = LinearLayoutManager(this)
        binding.rvHealthRecords.adapter = adapter
    }

    private fun showDeleteConfirmationDialog(record: HealthRecordItem) {
        DeleteConfirmationDialog.show(
            context = this,
            onDeleteConfirmed = {
                HealthRecordRepository.getInstance(this).deleteRecord(record.id)
                Toast.makeText(this, R.string.msg_record_deleted, Toast.LENGTH_SHORT).show()
                loadData()
            }
        )
    }

    private fun loadData() {
        val records = HealthRecordRepository.getInstance(this).getRecordsByCategory(currentCategory, profileId)
        if (records.isEmpty()) {
            binding.rvHealthRecords.visibility = View.GONE
            binding.layoutEmptyState.visibility = View.VISIBLE
        } else {
            binding.rvHealthRecords.visibility = View.VISIBLE
            binding.layoutEmptyState.visibility = View.GONE
            adapter.updateItems(records)
        }
    }

    private inner class HealthRecordListAdapter(
        private var items: List<HealthRecordItem>,
        private val onItemClick: (HealthRecordItem) -> Unit,
        private val onItemDelete: (HealthRecordItem) -> Unit
    ) : RecyclerView.Adapter<HealthRecordListAdapter.ViewHolder>() {

        private var currentlyOpenLayout: SwipeRevealLayout? = null

        fun updateItems(newItems: List<HealthRecordItem>) {
            items = newItems
            currentlyOpenLayout = null
            notifyDataSetChanged()
        }

        inner class ViewHolder(val binding: ItemHealthRecordCardBinding) :
            RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val itemBinding = ItemHealthRecordCardBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return ViewHolder(itemBinding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]

            // 1. Date
            val ctx = this@HealthRecordListActivity
            val dateText = when (currentCategory.trim().lowercase()) {
                CATEGORY_ALLERGIES.lowercase(), "allergy" -> {
                    if (item.firstNoticedDate.isNotBlank()) LocaleHelper.formatStoredDate(ctx, item.firstNoticedDate, "MMMd") else dateFormat.format(Date(item.createdAt))
                }
                CATEGORY_CONDITIONS.lowercase(), "condition" -> {
                    if (item.diagnosisDate.isNotBlank()) LocaleHelper.formatStoredDate(ctx, item.diagnosisDate, "MMMd") else dateFormat.format(Date(item.createdAt))
                }
                CATEGORY_APPOINTMENTS.lowercase(), "appointment", "appointments" -> {
                    if (item.diagnosisDate.isNotBlank()) LocaleHelper.formatStoredDate(ctx, item.diagnosisDate, "MMMd") else dateFormat.format(Date(item.createdAt))
                }
                else -> {
                    dateFormat.format(Date(item.createdAt))
                }
            }
            holder.binding.tvRecordDate.text = dateText

            // 2. Name
            holder.binding.tvRecordName.text = item.name

            // 3. Subtitle / Details
            val subtitleText = when (currentCategory.trim().lowercase()) {
                CATEGORY_MEDICATION.lowercase(), "medications" -> {
                    when {
                        item.strength.isNotBlank() && item.form.isNotBlank() -> "${item.strength} · 1 ${item.form}"
                        item.strength.isNotBlank() -> item.strength
                        item.form.isNotBlank() -> item.form
                        item.details.isNotBlank() -> item.details.replace(" · Once daily", "").replace("Once daily", "").trim()
                        item.reason.isNotBlank() -> item.reason
                        else -> item.notes
                    }
                }
                CATEGORY_ALLERGIES.lowercase(), "allergy" -> {
                    when {
                        item.type.isNotBlank() && item.reaction.isNotBlank() -> "${item.type} · ${item.reaction}"
                        item.type.isNotBlank() -> item.type
                        item.reaction.isNotBlank() -> item.reaction
                        item.details.isNotBlank() -> item.details
                        else -> item.notes
                    }
                }
                CATEGORY_CONDITIONS.lowercase(), "condition" -> {
                    when {
                        item.notes.isNotBlank() -> item.notes
                        item.status.isNotBlank() && item.clinic.isNotBlank() -> "${item.status} · ${item.clinic}"
                        item.status.isNotBlank() -> item.status
                        item.details.isNotBlank() -> item.details
                        else -> item.clinician
                    }
                }
                CATEGORY_VACCINATIONS.lowercase(), "vaccination" -> {
                    when {
                        item.doseNumber.isNotBlank() && item.clinic.isNotBlank() -> "${item.doseNumber} · ${item.clinic}"
                        item.doseNumber.isNotBlank() -> item.doseNumber
                        item.clinic.isNotBlank() -> item.clinic
                        item.details.isNotBlank() -> item.details
                        else -> item.notes
                    }
                }
                CATEGORY_APPOINTMENTS.lowercase(), "appointment", "appointments" -> {
                    when {
                        item.clinician.isNotBlank() && item.clinic.isNotBlank() -> "${item.clinician} · ${item.clinic}"
                        item.clinician.isNotBlank() -> item.clinician
                        item.clinic.isNotBlank() -> item.clinic
                        item.details.isNotBlank() -> item.details
                        else -> item.notes
                    }
                }
                else -> item.details.ifBlank { item.notes }
            }

            if (subtitleText.isNotBlank()) {
                holder.binding.tvRecordSubtitle.text = subtitleText
                holder.binding.tvRecordSubtitle.visibility = View.VISIBLE
            } else {
                holder.binding.tvRecordSubtitle.visibility = View.GONE
            }

            // Close swipe layout on bind
            holder.binding.swipeRevealLayout.close(false)

            // Close other open swipe layouts when this one opens
            holder.binding.swipeRevealLayout.onSwipeListener = object : SwipeRevealLayout.OnSwipeListener {
                override fun onOpened(layout: SwipeRevealLayout) {
                    if (currentlyOpenLayout != null && currentlyOpenLayout != layout) {
                        currentlyOpenLayout?.close(true)
                    }
                    currentlyOpenLayout = layout
                }

                override fun onClosed(layout: SwipeRevealLayout) {
                    if (currentlyOpenLayout == layout) {
                        currentlyOpenLayout = null
                    }
                }
            }

            // Click listener for foreground
            val clickListener = View.OnClickListener {
                if (holder.binding.swipeRevealLayout.isOpen) {
                    holder.binding.swipeRevealLayout.close(true)
                } else {
                    onItemClick(item)
                }
            }
            holder.binding.cardRecordRoot.setOnClickListener(clickListener)
            holder.binding.swipeRevealLayout.setOnClickListener(clickListener)

            // Delete action button
            holder.binding.btnDeleteAction.setOnClickListener {
                holder.binding.swipeRevealLayout.close(false)
                onItemDelete(item)
            }
        }

        override fun getItemCount(): Int = items.size
    }

    companion object {
        const val EXTRA_CATEGORY = "extra_category"
        const val EXTRA_PROFILE_ID = "extra_profile_id"

        const val CATEGORY_MEDICATION = "Medication"
        const val CATEGORY_ALLERGIES = "Allergies"
        const val CATEGORY_CONDITIONS = "Conditions"
        const val CATEGORY_VACCINATIONS = "Vaccinations"
        const val CATEGORY_APPOINTMENTS = "Appointments"

        fun start(context: Context, category: String, profileId: String? = null) {
            val intent = Intent(context, HealthRecordListActivity::class.java).apply {
                putExtra(EXTRA_CATEGORY, category)
                profileId?.let { putExtra(EXTRA_PROFILE_ID, it) }
            }
            context.startActivity(intent)
        }
    }
}
