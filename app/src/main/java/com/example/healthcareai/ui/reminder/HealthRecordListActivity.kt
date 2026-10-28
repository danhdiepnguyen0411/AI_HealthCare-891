package com.example.healthcareai.ui.reminder

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.healthcareai.data.model.HealthRecordItem
import com.example.healthcareai.data.repository.HealthRecordRepository
import com.example.healthcareai.databinding.ActivityHealthRecordListBinding
import com.example.healthcareai.databinding.ItemHealthRecordCardBinding
import com.example.healthcareai.ui.base.BaseActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HealthRecordListActivity : BaseActivity() {

    private lateinit var binding: ActivityHealthRecordListBinding
    private var currentCategory: String = CATEGORY_MEDICATION
    private var profileId: String? = null

    private lateinit var adapter: HealthRecordListAdapter
    private val dateFormat = SimpleDateFormat("MMM d", Locale.US)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHealthRecordListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentCategory = intent.getStringExtra(EXTRA_CATEGORY) ?: CATEGORY_MEDICATION
        profileId = intent.getStringExtra(EXTRA_PROFILE_ID)

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
                binding.tvTitle.text = "Medications"
                binding.tvAddButtonText.text = "Add medication"
                binding.tvEmptyTitle.text = "No medications yet"
                binding.tvEmptySubtitle.text = "Add your first record using the button below"
            }
            CATEGORY_ALLERGIES.lowercase(), "allergy" -> {
                binding.tvTitle.text = "Allergies"
                binding.tvAddButtonText.text = "Add allergy"
                binding.tvEmptyTitle.text = "No allergies yet"
                binding.tvEmptySubtitle.text = "Add your first record using the button below"
            }
            CATEGORY_CONDITIONS.lowercase(), "condition" -> {
                binding.tvTitle.text = "Conditions"
                binding.tvAddButtonText.text = "Add condition"
                binding.tvEmptyTitle.text = "No conditions yet"
                binding.tvEmptySubtitle.text = "Add your first record using the button below"
            }
            CATEGORY_VACCINATIONS.lowercase(), "vaccination" -> {
                binding.tvTitle.text = "Vaccinations"
                binding.tvAddButtonText.text = "Add vaccination"
                binding.tvEmptyTitle.text = "No vaccinations yet"
                binding.tvEmptySubtitle.text = "Add your first record using the button below"
            }
            else -> {
                binding.tvTitle.text = currentCategory
                binding.tvAddButtonText.text = "Add $currentCategory"
                binding.tvEmptyTitle.text = "No $currentCategory yet"
                binding.tvEmptySubtitle.text = "Add your first record using the button below"
            }
        }

        binding.btnAddRecord.setOnClickListener {
            when (currentCategory.trim().lowercase()) {
                CATEGORY_MEDICATION.lowercase(), "medications" -> AddMedicationActivity.start(this, profileId = profileId)
                CATEGORY_ALLERGIES.lowercase(), "allergy" -> AddAllergyActivity.start(this, profileId = profileId)
                CATEGORY_CONDITIONS.lowercase(), "condition" -> AddConditionActivity.start(this, profileId = profileId)
                CATEGORY_VACCINATIONS.lowercase(), "vaccination" -> AddVaccinationActivity.start(this, profileId = profileId)
                else -> AddMedicationActivity.start(this, profileId = profileId)
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = HealthRecordListAdapter(emptyList()) { item ->
            HealthRecordDetailActivity.start(this, item.id, currentCategory, profileId = profileId ?: item.profileId)
        }
        binding.rvHealthRecords.layoutManager = LinearLayoutManager(this)
        binding.rvHealthRecords.adapter = adapter
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
        private val onItemClick: (HealthRecordItem) -> Unit
    ) : RecyclerView.Adapter<HealthRecordListAdapter.ViewHolder>() {

        fun updateItems(newItems: List<HealthRecordItem>) {
            items = newItems
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
            val dateText = when (currentCategory.trim().lowercase()) {
                CATEGORY_ALLERGIES.lowercase(), "allergy" -> {
                    if (item.firstNoticedDate.isNotBlank()) item.firstNoticedDate else dateFormat.format(Date(item.createdAt))
                }
                CATEGORY_CONDITIONS.lowercase(), "condition" -> {
                    if (item.diagnosisDate.isNotBlank()) item.diagnosisDate else dateFormat.format(Date(item.createdAt))
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
                        item.strength.isNotBlank() && item.form.isNotBlank() -> "${item.strength} · 1 ${item.form} · Once daily"
                        item.strength.isNotBlank() -> item.strength
                        item.details.isNotBlank() -> item.details
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
                else -> item.details.ifBlank { item.notes }
            }

            if (subtitleText.isNotBlank()) {
                holder.binding.tvRecordSubtitle.text = subtitleText
                holder.binding.tvRecordSubtitle.visibility = View.VISIBLE
            } else {
                holder.binding.tvRecordSubtitle.visibility = View.GONE
            }

            holder.binding.cardRecordRoot.setOnClickListener {
                onItemClick(item)
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

        fun start(context: Context, category: String, profileId: String? = null) {
            val intent = Intent(context, HealthRecordListActivity::class.java).apply {
                putExtra(EXTRA_CATEGORY, category)
                profileId?.let { putExtra(EXTRA_PROFILE_ID, it) }
            }
            context.startActivity(intent)
        }
    }
}
