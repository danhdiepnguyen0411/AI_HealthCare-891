package com.example.healthcareai.ui.reminder

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.healthcareai.R
import com.example.healthcareai.data.model.HealthRecordItem
import com.example.healthcareai.data.repository.HealthRecordRepository
import com.example.healthcareai.databinding.BottomSheetChooseHealthItemBinding
import com.example.healthcareai.databinding.ItemChooseHealthRecordBinding
import com.example.healthcareai.util.DialogImmersiveHelper
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class ChooseHealthItemBottomSheetDialog : BottomSheetDialogFragment() {

    private var _binding: BottomSheetChooseHealthItemBinding? = null
    private val binding get() = _binding!!

    private var currentCategory: String = SelectCategoryBottomSheetDialog.CATEGORY_MEDICATION
    private var selectedItemId: String? = null
    private var profileId: String? = null
    private var onItemSelectedListener: ((HealthRecordItem) -> Unit)? = null

    private lateinit var adapter: HealthRecordAdapter

    private val addRecordLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val newlyCreatedId = result.data?.getStringExtra(AddHealthRecordActivity.EXTRA_RECORD_ID)
            loadRecords(newlyCreatedId)
            // If newly added item exists, auto-select it and notify listener
            if (newlyCreatedId != null) {
                val createdRecord = HealthRecordRepository.getInstance(requireContext())
                    .getRecordsByCategory(currentCategory, profileId)
                    .find { it.id == newlyCreatedId }
                if (createdRecord != null) {
                    onItemSelectedListener?.invoke(createdRecord)
                    dismiss()
                }
            }
        }
    }

    override fun getTheme(): Int = R.style.TransparentBottomSheetDialogTheme

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetChooseHealthItemBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        currentCategory = arguments?.getString(ARG_CATEGORY) ?: SelectCategoryBottomSheetDialog.CATEGORY_MEDICATION
        selectedItemId = arguments?.getString(ARG_SELECTED_ITEM_ID)
        profileId = arguments?.getString(ARG_PROFILE_ID)

        setupUI()
        setupRecyclerView()
        loadRecords(selectedItemId)
    }

    private fun setupUI() {
        when (currentCategory.lowercase()) {
            SelectCategoryBottomSheetDialog.CATEGORY_MEDICATION.lowercase() -> {
                binding.tvTitleChooseItem.text = "Choose medication"
                binding.btnAddRecord.text = "+ Add medication"
                binding.ivEmptyIcon.setImageResource(R.drawable.ic_medications)
                binding.tvEmptyTitle.text = "No medication added yet"
            }
            SelectCategoryBottomSheetDialog.CATEGORY_VACCINATION.lowercase() -> {
                binding.tvTitleChooseItem.text = "Choose vaccination"
                binding.btnAddRecord.text = "+ Add vaccination"
                binding.ivEmptyIcon.setImageResource(R.drawable.ic_vaccination)
                binding.tvEmptyTitle.text = "No vaccination added yet"
            }
            SelectCategoryBottomSheetDialog.CATEGORY_APPOINTMENTS.lowercase() -> {
                binding.tvTitleChooseItem.text = "Choose appointment"
                binding.btnAddRecord.text = "+ Add appointment"
                binding.ivEmptyIcon.setImageResource(R.drawable.ic_appointments)
                binding.tvEmptyTitle.text = "No appointments added yet"
            }
            else -> {
                binding.tvTitleChooseItem.text = "Choose $currentCategory"
                binding.btnAddRecord.text = "+ Add $currentCategory"
                binding.ivEmptyIcon.setImageResource(R.drawable.ic_medications)
                binding.tvEmptyTitle.text = "No $currentCategory added yet"
            }
        }

        binding.btnAddRecord.setOnClickListener {
            val intent = AddHealthRecordActivity.getIntent(requireContext(), currentCategory, profileId = profileId)
            addRecordLauncher.launch(intent)
        }
    }

    private fun setupRecyclerView() {
        adapter = HealthRecordAdapter(
            items = emptyList(),
            selectedId = selectedItemId,
            onItemClick = { item ->
                onItemSelectedListener?.invoke(item)
                dismiss()
            }
        )
        binding.rvHealthItems.layoutManager = LinearLayoutManager(requireContext())
        binding.rvHealthItems.adapter = adapter
    }

    private fun loadRecords(selectedId: String? = null) {
        val repo = HealthRecordRepository.getInstance(requireContext())
        val records = repo.getRecordsByCategory(currentCategory, profileId)

        if (records.isEmpty()) {
            binding.rvHealthItems.visibility = View.GONE
            binding.layoutEmptyState.visibility = View.VISIBLE
        } else {
            binding.rvHealthItems.visibility = View.VISIBLE
            binding.layoutEmptyState.visibility = View.GONE
            adapter.updateData(records, selectedId ?: selectedItemId)
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.let {
            if (it is BottomSheetDialog) {
                DialogImmersiveHelper.setupBottomSheetImmersive(it)
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let { sheet ->
                sheet.background = null
                sheet.setBackgroundColor(Color.TRANSPARENT)
                val behavior = BottomSheetBehavior.from(sheet)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
            }
        }
        DialogImmersiveHelper.setupBottomSheetImmersive(dialog)
        return dialog
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    class HealthRecordAdapter(
        private var items: List<HealthRecordItem>,
        private var selectedId: String?,
        private val onItemClick: (HealthRecordItem) -> Unit
    ) : RecyclerView.Adapter<HealthRecordAdapter.ViewHolder>() {

        fun updateData(newItems: List<HealthRecordItem>, newSelectedId: String?) {
            items = newItems
            selectedId = newSelectedId
            notifyDataSetChanged()
        }

        inner class ViewHolder(val binding: ItemChooseHealthRecordBinding) :
            RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemChooseHealthRecordBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.binding.tvItemName.text = item.name

            // Details string
            val details = when {
                item.strength.isNotBlank() && item.form.isNotBlank() -> "${item.strength} · ${item.form}"
                item.strength.isNotBlank() -> item.strength
                item.form.isNotBlank() -> item.form
                item.details.isNotBlank() -> item.details
                else -> ""
            }

            if (details.isNotBlank()) {
                holder.binding.tvItemDetails.text = details
                holder.binding.tvItemDetails.visibility = View.VISIBLE
            } else {
                holder.binding.tvItemDetails.visibility = View.GONE
            }

            val isSelected = (item.id == selectedId || item.name.equals(selectedId, ignoreCase = true))
            holder.binding.ivCheck.setImageResource(
                if (isSelected) R.drawable.bg_checkbox_checked else R.drawable.bg_checkbox_unchecked
            )

            holder.binding.cardItem.setOnClickListener {
                selectedId = item.id
                notifyDataSetChanged()
                onItemClick(item)
            }
        }

        override fun getItemCount(): Int = items.size
    }

    companion object {
        const val TAG = "ChooseHealthItemBottomSheetDialog"
        private const val ARG_CATEGORY = "arg_category"
        private const val ARG_SELECTED_ITEM_ID = "arg_selected_item_id"
        private const val ARG_PROFILE_ID = "arg_profile_id"

        fun show(
            fragmentManager: FragmentManager,
            category: String,
            selectedItemId: String? = null,
            profileId: String? = null,
            onSelected: (HealthRecordItem) -> Unit
        ): ChooseHealthItemBottomSheetDialog {
            val existing = fragmentManager.findFragmentByTag(TAG) as? ChooseHealthItemBottomSheetDialog
            if (existing != null) {
                existing.dismissAllowingStateLoss()
            }
            val dialog = ChooseHealthItemBottomSheetDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_CATEGORY, category)
                    putString(ARG_SELECTED_ITEM_ID, selectedItemId)
                    putString(ARG_PROFILE_ID, profileId)
                }
                this.onItemSelectedListener = onSelected
            }
            dialog.show(fragmentManager, TAG)
            return dialog
        }
    }
}
