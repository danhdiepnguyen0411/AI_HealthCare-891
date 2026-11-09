package com.example.healthcareai.ui.camera

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.healthcareai.R
import com.example.healthcareai.data.model.ScanHistoryItem
import com.example.healthcareai.data.repository.ProfileRepository
import com.example.healthcareai.data.repository.ScanHistoryRepository
import com.example.healthcareai.databinding.ActivityAiScanHistoryBinding
import com.example.healthcareai.ui.base.BaseActivity
import com.example.healthcareai.ui.base.applyHeaderTopMargin
import com.example.healthcareai.ui.dialog.DeleteConfirmationDialog
import java.io.File

class AiScanHistoryActivity : BaseActivity() {

    private lateinit var binding: ActivityAiScanHistoryBinding
    private lateinit var scanRepo: ScanHistoryRepository
    private lateinit var adapter: AiScanHistoryAdapter
    private var filterProfileId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAiScanHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        scanRepo = ScanHistoryRepository.getInstance(this)
        filterProfileId = intent.getStringExtra(EXTRA_PROFILE_ID)

        setupWindowInsets()
        setupListeners()
        setupRecyclerView()
    }

    override fun onResume() {
        super.onResume()
        binding.tvTitle.setText(R.string.title_ai_scan_history)
        binding.tvEmptyTitle.setText(R.string.scan_history_empty_title)
        binding.tvEmptyDesc.setText(R.string.scan_history_empty_desc)
        loadScans()
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.rootAiScanHistory) { _, insets ->
            binding.topBar.applyHeaderTopMargin(insets)

            val navBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            val density = resources.displayMetrics.density

            val bottomPadding = if (navBars.bottom > 0) {
                navBars.bottom + (16 * density).toInt()
            } else {
                (24 * density).toInt()
            }
            binding.rvScans.setPadding(0, (12 * density).toInt(), 0, bottomPadding)
            binding.layoutEmpty.setPadding(0, 0, 0, bottomPadding)

            insets
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            if (::adapter.isInitialized && adapter.closeOpenItem()) {
                return@setOnClickListener
            }
            finish()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (::adapter.isInitialized && adapter.closeOpenItem()) {
                    return
                }
                finish()
            }
        })

        binding.btnStartScan.setOnClickListener {
            val targetProfile = filterProfileId ?: ProfileRepository.getInstance(this).getPrimaryProfile().id
            val intent = Intent(this, CameraActivity::class.java).apply {
                putExtra(CameraActivity.EXTRA_PROFILE_ID, targetProfile)
            }
            startActivity(intent)
            finish()
        }
    }

    private fun setupRecyclerView() {
        adapter = AiScanHistoryAdapter(
            onItemClick = { item ->
                openScanDetail(item)
            },
            onItemDelete = { item ->
                showDeleteConfirmationDialog(item)
            }
        )
        binding.rvScans.apply {
            layoutManager = LinearLayoutManager(this@AiScanHistoryActivity)
            adapter = this@AiScanHistoryActivity.adapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                        this@AiScanHistoryActivity.adapter.closeOpenItem()
                    }
                }
            })
        }
    }

    private fun showDeleteConfirmationDialog(item: ScanHistoryItem) {
        DeleteConfirmationDialog.show(
            context = this,
            onDeleteConfirmed = {
                scanRepo.deleteScan(item.id)
                adapter.removeItem(item)
                try {
                    item.imagePath?.let { path ->
                        val file = File(path)
                        if (file.exists()) file.delete()
                    }
                } catch (_: Exception) {}
                Toast.makeText(this, R.string.msg_record_deleted, Toast.LENGTH_SHORT).show()
                if (adapter.itemCount == 0) {
                    binding.rvScans.visibility = View.GONE
                    binding.layoutEmpty.visibility = View.VISIBLE
                }
            }
        )
    }

    private fun loadScans() {
        val targetProfileId = filterProfileId.takeIf { !it.isNullOrEmpty() }
            ?: ProfileRepository.getInstance(this).getPrimaryProfile().id
        val scans = scanRepo.getScansForProfile(targetProfileId)

        if (scans.isEmpty()) {
            binding.rvScans.visibility = View.GONE
            binding.layoutEmpty.visibility = View.VISIBLE
        } else {
            binding.layoutEmpty.visibility = View.GONE
            binding.rvScans.visibility = View.VISIBLE
            adapter.submitList(scans)
        }
    }

    private fun openScanDetail(item: ScanHistoryItem) {
        val intent = Intent(this, AnalysisResultsActivity::class.java).apply {
            putExtra(AnalysisResultsActivity.EXTRA_IMAGE_PATH, item.imagePath)
            putExtra(AnalysisResultsActivity.EXTRA_CATEGORY, item.category)
            putExtra(AnalysisResultsActivity.EXTRA_PROFILE_ID, item.profileId)
            putExtra(AnalysisResultsActivity.EXTRA_IS_SUCCESS, true)
            putExtra(AnalysisResultsActivity.EXTRA_ANALYSIS_RESULT, item.analysisResult)
            putExtra(AnalysisResultsActivity.EXTRA_SCAN_ID, item.id)
            item.conversationId?.let { putExtra(AnalysisResultsActivity.EXTRA_CONVERSATION_ID, it) }
        }
        startActivity(intent)
    }

    companion object {
        const val EXTRA_PROFILE_ID = "extra_profile_id"

        fun start(context: Context, profileId: String? = null) {
            val intent = Intent(context, AiScanHistoryActivity::class.java).apply {
                profileId?.let { putExtra(EXTRA_PROFILE_ID, it) }
            }
            context.startActivity(intent)
        }
    }
}
