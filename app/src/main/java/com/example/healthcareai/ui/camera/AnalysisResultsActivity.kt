package com.example.healthcareai.ui.camera

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import coil.load
import coil.transform.CircleCropTransformation
import com.example.healthcareai.R
import com.example.healthcareai.data.model.CareSuggestion
import com.example.healthcareai.data.model.HealthRecordItem
import com.example.healthcareai.data.model.KeyFinding
import com.example.healthcareai.data.model.LesionAnalysisResult
import com.example.healthcareai.data.model.ScanHistoryItem
import com.example.healthcareai.data.model.UserProfile
import com.example.healthcareai.data.repository.AiChatRepository
import com.example.healthcareai.data.repository.HealthRecordRepository
import com.example.healthcareai.data.repository.LesionAnalysisRepository
import com.example.healthcareai.data.repository.ProfileRepository
import com.example.healthcareai.data.repository.ScanHistoryRepository
import com.example.healthcareai.databinding.ActivityAnalysisResultsBinding
import com.example.healthcareai.ui.base.BaseActivity
import com.example.healthcareai.ui.chat.AiChatActivity
import com.example.healthcareai.ui.dialog.LeaveWithoutSavingDialog
import com.example.healthcareai.ui.main.MainActivity
import com.example.healthcareai.ui.reminder.NewReminderActivity
import com.example.healthcareai.util.DialogImmersiveHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

class AnalysisResultsActivity : BaseActivity() {

    private lateinit var binding: ActivityAnalysisResultsBinding
    private lateinit var analysisRepo: LesionAnalysisRepository
    private lateinit var profileRepo: ProfileRepository

    private var currentImageFile: File? = null
    private var currentCategory: String = "SKIN"
    private var currentProfile: UserProfile? = null
    private var lastAnalysisResult: LesionAnalysisResult? = null
    private val savedProfileIds = mutableSetOf<String>()
    private var currentScanId: String? = null
    private var currentConversationId: String? = null

    private var isPhotoPickerActive = false
    private var lastPickerLaunchTime = 0L

    private val pickGalleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        isPhotoPickerActive = false
        if (uri != null) {
            handleGalleryUri(uri)
        }
    }

    override fun onResume() {
        super.onResume()
        isPhotoPickerActive = false
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("saved_scan_id", currentScanId)
        outState.putString("saved_conversation_id", currentConversationId)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAnalysisResultsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        analysisRepo = LesionAnalysisRepository.getInstance(this)
        profileRepo = ProfileRepository.getInstance(this)

        val imagePath = intent.getStringExtra(EXTRA_IMAGE_PATH)
        currentCategory = intent.getStringExtra(EXTRA_CATEGORY) ?: "SKIN"
        val profileId = intent.getStringExtra(EXTRA_PROFILE_ID)
        currentScanId = savedInstanceState?.getString("saved_scan_id") ?: intent.getStringExtra(EXTRA_SCAN_ID)
        currentConversationId = savedInstanceState?.getString("saved_conversation_id") ?: intent.getStringExtra(EXTRA_CONVERSATION_ID)

        val scanRepo = ScanHistoryRepository.getInstance(this)
        if (!currentScanId.isNullOrEmpty() && currentConversationId.isNullOrEmpty()) {
            currentConversationId = scanRepo.getScanById(currentScanId!!)?.conversationId
        }
        if (currentConversationId.isNullOrEmpty() && !imagePath.isNullOrEmpty()) {
            val matchingScan = scanRepo.findScanByImagePath(imagePath)
            if (matchingScan != null) {
                if (currentScanId.isNullOrEmpty()) currentScanId = matchingScan.id
                currentConversationId = matchingScan.conversationId
            }
        }
        if (currentConversationId.isNullOrEmpty() && !imagePath.isNullOrEmpty()) {
            val cachedConvId = getSharedPreferences(PREFS_RESULT_CONV, Context.MODE_PRIVATE)
                .getString("conv_for_$imagePath", null)
            if (!cachedConvId.isNullOrEmpty()) {
                currentConversationId = cachedConvId
            }
        }

        currentProfile = if (!profileId.isNullOrEmpty()) {
            profileRepo.getAllProfiles().find { it.id == profileId } ?: profileRepo.getPrimaryProfile()
        } else {
            profileRepo.getPrimaryProfile()
        }

        if (!currentScanId.isNullOrEmpty()) {
            currentProfile?.id?.let { savedProfileIds.add(it) }
        }

        setupWindowInsets()
        setupListeners()
        bindProfileInfo()

        val hasPreloadedResult = intent.hasExtra(EXTRA_IS_SUCCESS)
        val isSuccess = intent.getBooleanExtra(EXTRA_IS_SUCCESS, false)
        val preloadedResult = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra(EXTRA_ANALYSIS_RESULT, LesionAnalysisResult::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra(EXTRA_ANALYSIS_RESULT) as? LesionAnalysisResult
        }

        if (!imagePath.isNullOrEmpty()) {
            val file = File(imagePath)
            if (file.exists()) {
                currentImageFile = file
                if (hasPreloadedResult) {
                    if (isSuccess && preloadedResult != null && LesionAnalysisRepository.isIdentified(preloadedResult)) {
                        lastAnalysisResult = preloadedResult
                        showSuccessState(file, preloadedResult)
                    } else {
                        showUnableState()
                    }
                    return
                } else {
                    executeLesionAnalysis(file)
                    return
                }
            }
        }

        // Fallback: If no file provided, show Unable to identify
        showUnableState()
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.rootAnalysisLayout) { _, insets ->
            val statusBarTop = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            val navBarBottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            val density = resources.displayMetrics.density

            val targetTopMargin = if (statusBarTop > 0) {
                statusBarTop + (6 * density).toInt()
            } else {
                (20 * density).toInt()
            }
            binding.topBar.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = targetTopMargin
            }

            if (navBarBottom > 0) {
                binding.bottomBarSuccess.setPadding(
                    (16 * density).toInt(),
                    (8 * density).toInt(),
                    (16 * density).toInt(),
                    navBarBottom + (12 * density).toInt()
                )
                binding.layoutUnableToIdentify.setPadding(
                    0,
                    0,
                    0,
                    navBarBottom
                )
            }
            insets
        }
    }

    private fun setupListeners() {
        // Back
        binding.btnBack.setOnClickListener {
            handleBackPressed()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleBackPressed()
            }
        })

        // Report Result Button
        binding.btnInfo.setOnClickListener {
            showReportResultBottomSheet()
        }

        // Profile Click (Allow switching profile)
        binding.cardProfile.setOnClickListener {
            showProfileSelectionDialog()
        }

        // Rescan -> Back to camera
        binding.btnRescan.setOnClickListener {
            val intent = Intent(this, CameraActivity::class.java).apply {
                putExtra(CameraActivity.EXTRA_CATEGORY, currentCategory)
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            startActivity(intent)
            finish()
        }

        // Choose from gallery
        binding.btnChooseGallery.setOnClickListener {
            val now = android.os.SystemClock.elapsedRealtime()
            if (isPhotoPickerActive || now - lastPickerLaunchTime < 1000L) {
                return@setOnClickListener
            }
            lastPickerLaunchTime = now
            isPhotoPickerActive = true
            try {
                pickGalleryLauncher.launch("image/*")
            } catch (e: Exception) {
                isPhotoPickerActive = false
                android.util.Log.e(TAG, "Failed to launch gallery picker", e)
            }
        }

        // Save result
        binding.btnSaveResult.setOnClickListener {
            handleSaveResult()
        }

        // Set a reminder
        binding.btnSetReminder.setOnClickListener {
            ChooseRemindersBottomSheetDialog.show(
                fragmentManager = supportFragmentManager,
                profileId = currentProfile?.id,
                analysisResult = lastAnalysisResult,
                category = currentCategory
            )
        }

        // Ask AI about this result
        binding.btnAskAi.setOnClickListener {
            val convId = getOrCreateConversationId()
            val existing = AiChatRepository.getInstance(this).getConversationById(convId)
            val hasExistingMessages = existing != null && existing.messages.any { it.isUser }

            val intent = Intent(this, AiChatActivity::class.java).apply {
                putExtra(AiChatActivity.EXTRA_CONVERSATION_ID, convId)
                if (!hasExistingMessages) {
                    val prompt = buildAskAiPrompt()
                    putExtra(AiChatActivity.EXTRA_INITIAL_PROMPT, prompt)
                    putExtra(AiChatActivity.EXTRA_AUTO_SEND, true)
                }
            }
            startActivity(intent)
        }
    }

    private fun buildAskAiPrompt(): String {
        val lang = com.example.healthcareai.util.LocaleHelper.getSavedLanguageCode(this).lowercase(Locale.ROOT)
        val validFindings = lastAnalysisResult?.keyFindings?.filter { !it.title.isNullOrBlank() }
        val findingTitles = validFindings?.mapNotNull { it.title?.trim() }?.take(2)
        val conditionName = when {
            !findingTitles.isNullOrEmpty() -> findingTitles.joinToString(", ")
            else -> null
        }
        val summary = lastAnalysisResult?.summary?.trim()

        val categoryNameVi = when (currentCategory.uppercase(Locale.ROOT)) {
            "SKIN" -> "vùng da"
            "EYE" -> "mắt"
            "NAIL" -> "móng"
            "HAIR" -> "tóc và da đầu"
            "ORAL" -> "răng miệng"
            else -> "sức khỏe"
        }

        val categoryNameEn = when (currentCategory.uppercase(Locale.ROOT)) {
            "SKIN" -> "skin"
            "EYE" -> "eye"
            "NAIL" -> "nail"
            "HAIR" -> "hair and scalp"
            "ORAL" -> "oral"
            else -> "health"
        }

        return if (lang.startsWith("vi")) {
            when {
                !conditionName.isNullOrBlank() -> {
                    "Kết quả quét phân tích $categoryNameVi của tôi cho thấy dấu hiệu của: $conditionName. Bác sĩ AI có thể tư vấn chi tiết hơn về căn bệnh này, nguyên nhân, mức độ nguy hiểm và hướng chăm sóc điều trị an toàn không?"
                }
                !summary.isNullOrBlank() -> {
                    "Kết quả quét phân tích $categoryNameVi của tôi ghi nhận: \"$summary\". Bác sĩ AI có thể tư vấn chi tiết hơn về triệu chứng này và hướng chăm sóc điều trị an toàn không?"
                }
                else -> {
                    "Tôi vừa thực hiện quét kiểm tra $categoryNameVi. Bác sĩ AI có thể tư vấn các triệu chứng bất thường cần lưu ý và cách chăm sóc phù hợp không?"
                }
            }
        } else {
            when {
                !conditionName.isNullOrBlank() -> {
                    "My $categoryNameEn scan analysis shows signs of: $conditionName. Could you explain more about this condition, its potential causes, risk level, and recommended care steps?"
                }
                !summary.isNullOrBlank() -> {
                    "My $categoryNameEn scan result notes: \"$summary\". Could you give me more detailed health guidance about these symptoms and care recommendations?"
                }
                else -> {
                    "I recently performed a $categoryNameEn scan. Could you provide detailed guidance on common symptoms and appropriate care advice?"
                }
            }
        }
    }

    private fun bindProfileInfo() {
        val profile = currentProfile ?: profileRepo.getPrimaryProfile()
        val displayName = profile.name.ifBlank { "Alex Morgan" }
        binding.tvProfileName.text = displayName

        val avatarUri = profile.avatarUri
        if (!avatarUri.isNullOrEmpty()) {
            binding.ivProfileAvatar.load(avatarUri) {
                crossfade(true)
                transformations(CircleCropTransformation())
                error(R.drawable.profile_person)
            }
        } else {
            binding.ivProfileAvatar.setImageResource(R.drawable.profile_person)
        }
        updateSaveResultButtonState()
    }

    private fun executeLesionAnalysis(file: File) {
        savedProfileIds.clear()
        currentScanId = null
        currentConversationId = null
        updateSaveResultButtonState()
        val processingDialog = AiProcessingDialog.show(supportFragmentManager)

        val profile = currentProfile ?: profileRepo.getPrimaryProfile()
        val age = calculateAge(profile.dateOfBirth)
        val gender = if (profile.gender.equals("FEMALE", ignoreCase = true)) "FEMALE" else "MALE"

        lifecycleScope.launch {
            val result = analysisRepo.scanAi(
                imageFile = file,
                category = currentCategory,
                age = age,
                gender = gender
            )

            processingDialog.dismissSafely()

            result.onSuccess { data ->
                if (LesionAnalysisRepository.isIdentified(data)) {
                    lastAnalysisResult = data
                    showSuccessState(file, data)
                } else {
                    showUnableState()
                }
            }.onFailure { error ->
                android.util.Log.e(TAG, "Lesion analysis failed: ${error.message}", error)
                showUnableState()
            }
        }
    }

    private fun showLoadingState() {
        binding.layoutLoading.visibility = View.VISIBLE
        binding.layoutUnableToIdentify.visibility = View.GONE
        binding.layoutSuccess.visibility = View.GONE
        binding.bottomBarSuccess.visibility = View.GONE
        binding.btnInfo.visibility = View.GONE
    }

    private fun showUnableState() {
        binding.layoutLoading.visibility = View.GONE
        binding.layoutUnableToIdentify.visibility = View.VISIBLE
        binding.layoutSuccess.visibility = View.GONE
        binding.bottomBarSuccess.visibility = View.GONE
        binding.btnInfo.visibility = View.GONE
    }

    private fun showSuccessState(file: File, result: LesionAnalysisResult) {
        binding.layoutLoading.visibility = View.GONE
        binding.layoutUnableToIdentify.visibility = View.GONE
        binding.layoutSuccess.visibility = View.VISIBLE
        binding.bottomBarSuccess.visibility = View.VISIBLE
        binding.btnInfo.visibility = View.VISIBLE

        // Load photo into ImageView
        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
        val imgWidth = bitmap?.width ?: 0
        val imgHeight = bitmap?.height ?: 0

        binding.ivScannedPhoto.load(file) {
            crossfade(true)
        }

        // Setup Detection Box Overlay (only draws if server provided a valid box)
        binding.detectionOverlayView.setDetectionBox(result.detectionBox, imgWidth, imgHeight)
        updateSaveResultButtonState()

        // Summary (only show if server provided summary)
        val summary = result.summary.orEmpty().trim()
        if (summary.isNotBlank()) {
            binding.layoutSummaryRow.visibility = View.VISIBLE
            binding.tvSummary.text = summary
        } else {
            binding.layoutSummaryRow.visibility = View.GONE
        }

        // Populate Key Findings (only show real data from server)
        populateKeyFindings(result.keyFindings.orEmpty())

        // Populate Care Suggestions (only show real data from server)
        populateCareSuggestions(result.careSuggestions.orEmpty())

        // Populate When to Seek Care (only show real data from server)
        populateWhenToSeekCare(result.whenToSeekCare.orEmpty())
    }

    private fun populateKeyFindings(findings: List<KeyFinding>) {
        binding.layoutKeyFindingsList.removeAllViews()
        val validFindings = findings.filter { !it.title.isNullOrBlank() }

        if (validFindings.isEmpty()) {
            binding.tvHeaderKeyFindings.visibility = View.GONE
            binding.cardKeyFindings.visibility = View.GONE
            return
        }

        binding.tvHeaderKeyFindings.visibility = View.VISIBLE
        binding.cardKeyFindings.visibility = View.VISIBLE
        val inflater = LayoutInflater.from(this)
        for (item in validFindings) {
            val itemView = inflater.inflate(R.layout.item_key_finding, binding.layoutKeyFindingsList, false)
            bindFindingItem(itemView, item)
            binding.layoutKeyFindingsList.addView(itemView)
        }
    }

    private fun bindFindingItem(view: View, item: KeyFinding) {
        val tvTitle = view.findViewById<TextView>(R.id.tvFindingTitle)
        val tvRate = view.findViewById<TextView>(R.id.tvConfidenceRate)
        val tvDesc = view.findViewById<TextView>(R.id.tvFindingDesc)

        tvTitle.text = item.title.orEmpty()
        val rate = item.confidenceRate
        if (!rate.isNullOrBlank()) {
            tvRate.visibility = View.VISIBLE
            tvRate.text = rate
        } else {
            tvRate.visibility = View.GONE
        }
        tvDesc.text = item.description.orEmpty()
    }

    private fun populateCareSuggestions(suggestions: List<CareSuggestion>) {
        binding.layoutCareSuggestionsList.removeAllViews()
        val validSuggestions = suggestions.filter { !it.title.isNullOrBlank() || !it.description.isNullOrBlank() }

        if (validSuggestions.isEmpty()) {
            binding.tvHeaderCareSuggestions.visibility = View.GONE
            binding.cardCareSuggestions.visibility = View.GONE
            return
        }

        binding.tvHeaderCareSuggestions.visibility = View.VISIBLE
        binding.cardCareSuggestions.visibility = View.VISIBLE
        val inflater = LayoutInflater.from(this)
        for (item in validSuggestions) {
            val itemView = inflater.inflate(R.layout.item_care_suggestion, binding.layoutCareSuggestionsList, false)
            bindCareItem(itemView, item)
            binding.layoutCareSuggestionsList.addView(itemView)
        }
    }

    private fun bindCareItem(view: View, item: CareSuggestion) {
        val tvTitle = view.findViewById<TextView>(R.id.tvCareTitle)
        val tvDesc = view.findViewById<TextView>(R.id.tvCareDesc)

        tvTitle.text = item.title.orEmpty()
        tvDesc.text = item.description.orEmpty()
    }

    private fun populateWhenToSeekCare(bullets: List<String>) {
        binding.layoutWhenToSeekList.removeAllViews()
        val validBullets = bullets.filter { it.isNotBlank() }

        if (validBullets.isEmpty()) {
            binding.cardWhenToSeekCare.visibility = View.GONE
            return
        }

        binding.cardWhenToSeekCare.visibility = View.VISIBLE
        val inflater = LayoutInflater.from(this)
        for (text in validBullets) {
            val itemView = inflater.inflate(R.layout.item_when_to_seek_care, binding.layoutWhenToSeekList, false)
            val tvSeek = itemView.findViewById<TextView>(R.id.tvSeekText)
            tvSeek.text = text
            binding.layoutWhenToSeekList.addView(itemView)
        }
    }

    private fun handleGalleryUri(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                val tempFile = File(cacheDir, "gallery_lesion_${System.currentTimeMillis()}.jpg")
                inputStream?.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                withContext(Dispatchers.Main) {
                    currentImageFile = tempFile
                    executeLesionAnalysis(tempFile)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@AnalysisResultsActivity, getString(R.string.toast_photo_load_failed), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showInfoDisclaimerDialog() {
        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.ai_health_disclaimer_title)
            .setMessage(R.string.ai_health_disclaimer_desc)
            .setPositiveButton(R.string.btn_understood, null)
            .create()

        DialogImmersiveHelper.showImmersive(dialog)
    }

    private fun showReportResultBottomSheet() {
        ReportResultBottomSheetDialog.show(supportFragmentManager)
    }

    private fun showProfileSelectionDialog() {
        SaveResultToBottomSheetDialog.show(
            fragmentManager = supportFragmentManager,
            selectedProfileId = currentProfile?.id
        ) { selectedProfile ->
            currentProfile = selectedProfile
            bindProfileInfo()
        }
    }

    private fun handleBackPressed() {
        val hasUnsavedResult = binding.bottomBarSuccess.visibility == View.VISIBLE &&
                savedProfileIds.isEmpty() &&
                currentScanId.isNullOrEmpty()

        if (hasUnsavedResult) {
            LeaveWithoutSavingDialog.show(
                context = this,
                onStay = {
                    // Stay on this page
                },
                onExitWithoutSaving = {
                    exitToMainScreen()
                }
            )
        } else {
            exitToMainScreen()
        }
    }

    private fun exitToMainScreen() {
        if (intent.hasExtra(EXTRA_SCAN_ID) || isTaskRoot) {
            finish()
        } else {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            finish()
        }
    }

    private fun handleSaveResult() {
        val targetProfile = currentProfile ?: profileRepo.getPrimaryProfile()
        if (savedProfileIds.contains(targetProfile.id)) {
            Toast.makeText(this, R.string.msg_result_already_saved, Toast.LENGTH_SHORT).show()
            return
        }

        val findingTitle = lastAnalysisResult?.keyFindings?.firstOrNull { !it.title.isNullOrBlank() }?.title?.trim()
        val categoryLabel = when (currentCategory.uppercase(Locale.ROOT)) {
            "SKIN" -> "Skin"
            "EYE" -> "Eye"
            "NAIL" -> "Nail"
            "HAIR" -> "Hair"
            "ORAL" -> "Oral"
            else -> currentCategory.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        }
        val conditionName = findingTitle ?: "$categoryLabel Assessment"
        val diagnosisDate = SimpleDateFormat("MMM d, yyyy", Locale.US).format(Date())
        val summary = lastAnalysisResult?.summary?.trim().orEmpty()
        val details = if (summary.isNotBlank()) summary else "Ongoing · $diagnosisDate"

        // 1. Save to Health Records (Conditions)
        val record = HealthRecordItem(
            id = UUID.randomUUID().toString(),
            profileId = targetProfile.id,
            category = "Conditions",
            name = conditionName,
            details = details,
            status = "Ongoing",
            diagnosisDate = diagnosisDate,
            clinician = "AI Health Scan",
            notes = summary
        )
        HealthRecordRepository.getInstance(this).saveRecord(record)

        val scanId = currentScanId ?: UUID.randomUUID().toString()
        currentScanId = scanId

        // 2. Save to Scan History for this profile
        val scanItem = ScanHistoryItem(
            id = scanId,
            profileId = targetProfile.id,
            category = currentCategory,
            title = conditionName,
            summary = summary,
            imagePath = currentImageFile?.absolutePath,
            dateFormatted = diagnosisDate,
            timestamp = System.currentTimeMillis(),
            analysisResult = lastAnalysisResult,
            conversationId = currentConversationId
        )
        ScanHistoryRepository.getInstance(this).saveScan(scanItem)

        savedProfileIds.add(targetProfile.id)
        updateSaveResultButtonState()
        Toast.makeText(this, R.string.msg_result_saved_to_profile, Toast.LENGTH_SHORT).show()
    }

    private fun updateSaveResultButtonState() {
        val targetProfile = currentProfile ?: profileRepo.getPrimaryProfile()
        if (savedProfileIds.contains(targetProfile.id)) {
            binding.tvSaveResultText.setText(R.string.btn_saved_result)
        } else {
            binding.tvSaveResultText.setText(R.string.btn_save_result)
        }
    }

    private fun calculateAge(dobString: String): Int {
        if (dobString.isBlank()) return 20
        val formats = listOf(
            SimpleDateFormat("MMM dd, yyyy", Locale.US),
            SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()),
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()),
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()),
            SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        )
        for (sdf in formats) {
            try {
                val birthDate = sdf.parse(dobString)
                if (birthDate != null) {
                    val birthCal = Calendar.getInstance().apply { time = birthDate }
                    val today = Calendar.getInstance()
                    var age = today.get(Calendar.YEAR) - birthCal.get(Calendar.YEAR)
                    if (today.get(Calendar.DAY_OF_YEAR) < birthCal.get(Calendar.DAY_OF_YEAR)) {
                        age--
                    }
                    if (age in 1..120) return age
                }
            } catch (_: Exception) {}
        }
        return 20
    }

    private fun getOrCreateConversationId(): String {
        currentConversationId?.let { return it }

        val scanRepo = ScanHistoryRepository.getInstance(this)
        val scanId = currentScanId
        if (!scanId.isNullOrEmpty()) {
            val scan = scanRepo.getScanById(scanId)
            if (!scan?.conversationId.isNullOrEmpty()) {
                currentConversationId = scan!!.conversationId
                return currentConversationId!!
            }
        }

        val imgPath = currentImageFile?.absolutePath
        if (!imgPath.isNullOrEmpty()) {
            val matchingScan = scanRepo.findScanByImagePath(imgPath)
            if (matchingScan != null) {
                if (currentScanId.isNullOrEmpty()) currentScanId = matchingScan.id
                if (!matchingScan.conversationId.isNullOrEmpty()) {
                    currentConversationId = matchingScan.conversationId
                    return currentConversationId!!
                }
            }

            val cachedConvId = getSharedPreferences(PREFS_RESULT_CONV, Context.MODE_PRIVATE)
                .getString("conv_for_$imgPath", null)
            if (!cachedConvId.isNullOrEmpty()) {
                currentConversationId = cachedConvId
                return currentConversationId!!
            }
        }

        val newConvId = UUID.randomUUID().toString()
        currentConversationId = newConvId

        if (!imgPath.isNullOrEmpty()) {
            getSharedPreferences(PREFS_RESULT_CONV, Context.MODE_PRIVATE)
                .edit()
                .putString("conv_for_$imgPath", newConvId)
                .apply()
        }

        if (!scanId.isNullOrEmpty()) {
            scanRepo.updateConversationId(scanId, newConvId)
        }

        return newConvId
    }

    companion object {
        private const val TAG = "AnalysisResultsAct"
        private const val PREFS_RESULT_CONV = "prefs_result_conversation_map"
        const val EXTRA_IMAGE_PATH = "extra_image_path"
        const val EXTRA_CATEGORY = "extra_scan_category"
        const val EXTRA_PROFILE_ID = "extra_profile_id"
        const val EXTRA_ANALYSIS_RESULT = "extra_analysis_result"
        const val EXTRA_IS_SUCCESS = "extra_is_success"
        const val EXTRA_SCAN_ID = "extra_scan_id"
        const val EXTRA_CONVERSATION_ID = "extra_conversation_id"
    }
}
