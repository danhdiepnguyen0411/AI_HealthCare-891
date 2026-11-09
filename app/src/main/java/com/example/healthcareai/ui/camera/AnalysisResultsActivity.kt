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
import com.example.healthcareai.ui.base.applyHeaderTopMargin
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
            binding.topBar.applyHeaderTopMargin(insets)

            val navBarBottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            val density = resources.displayMetrics.density

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

        // Profile selector: allows selecting which profile to save result to
        binding.cardProfile.setOnClickListener {
            if (intent.getStringExtra(EXTRA_SCAN_ID).isNullOrEmpty()) {
                showProfileSelectionDialog()
            }
        }

        // Save result button
        binding.btnSaveResult.setOnClickListener {
            handleSaveResult()
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
            val (displayPrompt, apiContextPrompt) = buildAskAiPrompts()
            val convId = getOrCreateConversationId(displayPrompt)
            val existing = AiChatRepository.getInstance(this).getConversationById(convId)
            val firstUserMsg = existing?.messages?.firstOrNull { it.isUser }?.content
            val hasMatchingMessages = existing != null && firstUserMsg == displayPrompt

            val intent = Intent(this, AiChatActivity::class.java).apply {
                putExtra(AiChatActivity.EXTRA_CONVERSATION_ID, convId)
                if (!hasMatchingMessages) {
                    putExtra(AiChatActivity.EXTRA_INITIAL_PROMPT, displayPrompt)
                    if (!apiContextPrompt.isNullOrBlank()) {
                        putExtra(AiChatActivity.EXTRA_API_CONTEXT, apiContextPrompt)
                    }
                    putExtra(AiChatActivity.EXTRA_AUTO_SEND, true)
                }
            }
            startActivity(intent)
        }
    }

    private fun isEnglishTextForNonEnglishLocale(text: String?, lang: String): Boolean {
        if (text.isNullOrBlank() || lang.startsWith("en")) return false
        val trimmed = text.trim()
        // For non-Latin scripts or Vietnamese with diacritics, pure ASCII text is English
        val isAllAscii = trimmed.all { it.code < 128 }
        if (isAllAscii && (
                lang.startsWith("vi") || lang.startsWith("ru") || lang.startsWith("hi") ||
                lang.startsWith("ja") || lang.startsWith("ko") || lang.startsWith("zh") ||
                lang.startsWith("ar") || lang.startsWith("bn")
            )
        ) {
            return true
        }
        val lower = trimmed.lowercase(Locale.ROOT)
        val englishMarkers = listOf(
            "nail ", "skin ", "discoloration", "thickening", "periungual", "detected",
            "findings", "based on", "provided image", "redness", "scaling", "observed",
            "dermatitis", "infection", "irritation", "inflammation", "lesion", "symptom",
            "condition", "assessment", "unable to", "relevant visual"
        )
        return englishMarkers.any { lower.contains(it) }
    }

    private fun buildAskAiPrompts(): Pair<String, String?> {
        val lang = com.example.healthcareai.util.LocaleHelper.getSavedLanguageCode(this).lowercase(Locale.ROOT)
        val validFindings = lastAnalysisResult?.keyFindings?.filter { !it.title.isNullOrBlank() }
        val findingTitles = validFindings?.mapNotNull { it.title?.trim() }?.take(2)
        val conditionName = when {
            !findingTitles.isNullOrEmpty() -> findingTitles.joinToString(", ")
            else -> null
        }
        val summary = lastAnalysisResult?.summary?.trim()
        val catLabel = getLocalizedCategoryLabel(lang)

        val localizedCondition = conditionName?.takeIf { !isEnglishTextForNonEnglishLocale(it, lang) }
        val localizedSummary = summary?.takeIf { !isEnglishTextForNonEnglishLocale(it, lang) }
        val localizedSubject = localizedCondition ?: localizedSummary

        val apiContext = if (!conditionName.isNullOrBlank() || !summary.isNullOrBlank()) {
            buildString {
                append("Scan category: $currentCategory.")
                if (!conditionName.isNullOrBlank()) append(" Detected findings: $conditionName.")
                if (!summary.isNullOrBlank()) append(" Summary: $summary.")
            }
        } else {
            null
        }

        val displayPrompt = when {
            lang.startsWith("vi") -> {
                when {
                    !localizedCondition.isNullOrBlank() ->
                        "Kết quả quét phân tích $catLabel của tôi cho thấy dấu hiệu của: $localizedCondition. Bác sĩ AI có thể tư vấn chi tiết hơn về tình trạng này, nguyên nhân, mức độ nguy hiểm và hướng chăm sóc điều trị an toàn không?"
                    !localizedSummary.isNullOrBlank() ->
                        "Kết quả quét phân tích $catLabel của tôi ghi nhận: \"$localizedSummary\". Bác sĩ AI có thể tư vấn chi tiết hơn về triệu chứng này và hướng chăm sóc điều trị an toàn không?"
                    else ->
                        "Tôi vừa thực hiện quét phân tích $catLabel. Bác sĩ AI có thể giải thích chi tiết kết quả quét của tôi, nguyên nhân, các triệu chứng cần lưu ý và hướng chăm sóc an toàn không?"
                }
            }
            lang.startsWith("hi") -> {
                if (!localizedSubject.isNullOrBlank()) {
                    "मेरे $catLabel स्कैन परिणाम में यह पाया गया है: \"$localizedSubject\"। कृपया इसके संभावित कारणों, लक्षणों और सुरक्षित देखभाल के उपायों के बारे में विस्तार से बताएं।"
                } else {
                    "मैंने अभी अपना $catLabel स्कैन किया है। कृपया मेरे स्कैन परिणामों, संभावित कारणों, लक्षणों और सुरक्षित देखभाल के उपायों के बारे में विस्तार से बताएं।"
                }
            }
            lang.startsWith("ru") -> {
                if (!localizedSubject.isNullOrBlank()) {
                    "Результат моего сканирования ($catLabel) показал: «$localizedSubject». Подскажите, пожалуйста, подробнее о возможных причинах, симптомах и рекомендациях по безопасному уходу."
                } else {
                    "Я только что провел(а) сканирование ($catLabel). Подскажите, пожалуйста, подробнее о результатах моего сканирования, возможных причинах, симптомах и рекомендациях по безопасному уходу."
                }
            }
            lang.startsWith("id") || lang.startsWith("in") -> {
                if (!localizedSubject.isNullOrBlank()) {
                    "Hasil pemindaian $catLabel saya menunjukkan: \"$localizedSubject\". Bisakah Anda menjelaskan lebih detail mengenai kemungkinan penyebab, gejala, dan saran perawatan yang aman?"
                } else {
                    "Saya baru saja melakukan pemindaian $catLabel. Bisakah Anda menjelaskan lebih detail mengenai hasil pemindaian saya, kemungkinan penyebab, gejala, dan saran perawatan yang aman?"
                }
            }
            lang.startsWith("de") -> {
                if (!localizedSubject.isNullOrBlank()) {
                    "Mein Scan-Ergebnis ($catLabel) zeigt: „$localizedSubject“. Können Sie mir bitte mehr über mögliche Ursachen, Symptome und sichere Pflegeempfehlungen erklären?"
                } else {
                    "Ich habe gerade einen Scan ($catLabel) durchgeführt. Können Sie mir bitte meine Scan-Ergebnisse, mögliche Ursachen, Symptome und sichere Pflegeempfehlungen näher erklären?"
                }
            }
            lang.startsWith("es") -> {
                if (!localizedSubject.isNullOrBlank()) {
                    "El resultado de mi escaneo ($catLabel) muestra: \"$localizedSubject\". ¿Podría explicarme con más detalle las posibles causas, síntomas y recomendaciones de cuidado seguro?"
                } else {
                    "Acabo de realizar un escaneo ($catLabel). ¿Podría explicarme con más detalle los resultados de mi escaneo, las posibles causas, los síntomas y las recomendaciones de cuidado seguro?"
                }
            }
            lang.startsWith("fr") -> {
                if (!localizedSubject.isNullOrBlank()) {
                    "Le résultat de mon scan ($catLabel) indique : « $localizedSubject ». Pouvez-vous m'expliquer plus en détail les causes possibles, les symptômes et les conseils de soins appropriés ?"
                } else {
                    "Je viens d'effectuer un scan ($catLabel). Pouvez-vous m'expliquer plus en détail les résultats de mon scan, les causes possibles, les symptômes et les conseils de soins appropriés ?"
                }
            }
            lang.startsWith("pt") -> {
                if (!localizedSubject.isNullOrBlank()) {
                    "O resultado do meu exame ($catLabel) mostra: \"$localizedSubject\". Poderia explicar com mais detalhes as possíveis causas, sintomas e recomendações de cuidados seguros?"
                } else {
                    "Acabei de realizar um exame ($catLabel). Poderia explicar com mais detalhes os resultados do meu exame, as possíveis causas, sintomas e recomendações de cuidados seguros?"
                }
            }
            lang.startsWith("ja") -> {
                if (!localizedSubject.isNullOrBlank()) {
                    "スキャン結果（$catLabel）は「${localizedSubject}」を示しています。考えられる原因、症状、および適切なケア方法について詳しく教えてください。"
                } else {
                    "先ほど${catLabel}のスキャンを行いました。スキャン結果の詳細、考えられる原因、症状、および適切なケア方法について詳しく教えてください。"
                }
            }
            lang.startsWith("ko") -> {
                if (!localizedSubject.isNullOrBlank()) {
                    "스캔 분석 결과($catLabel) \"$localizedSubject\" 소견이 확인되었습니다. 가능한 원인, 증상 및 안전한 관리 방법에 대해 자세히 설명해 주시겠어요?"
                } else {
                    "방금 $catLabel 스캔을 진행했습니다. 스캔 분석 결과와 가능한 원인, 증상 및 안전한 관리 방법에 대해 자세히 설명해 주시겠어요?"
                }
            }
            lang.startsWith("zh_tw") -> {
                if (!localizedSubject.isNullOrBlank()) {
                    "我的${catLabel}掃描分析結果顯示：「$localizedSubject」。請問這可能的原因、症狀以及安全的護理建議是什麼？"
                } else {
                    "我剛完成了${catLabel}掃描分析。請幫我詳細解讀掃描結果、可能的原因、需要注意的症狀以及安全的護理建議。"
                }
            }
            lang.startsWith("zh") -> {
                if (!localizedSubject.isNullOrBlank()) {
                    "我的${catLabel}扫描分析结果显示：“$localizedSubject”。请问这可能的原因、症状以及安全的护理建议是什么？"
                } else {
                    "我刚完成了${catLabel}扫描分析。请帮我详细解读扫描结果、可能的原因、需要注意的症状以及安全的护理建议。"
                }
            }
            lang.startsWith("ar") -> {
                if (!localizedSubject.isNullOrBlank()) {
                    "أظهرت نتيجة فحص ($catLabel) الخاصة بي: \"$localizedSubject\". هل يمكنك شرح المزيد عن الأسباب المحتملة والأعراض وإرشادات الرعاية الآمنة؟"
                } else {
                    "لقد أجريت للتو فحص ($catLabel). هل يمكنك شرح نتائج الفحص الخاصة بي والأسباب المحتملة والأعراض وإرشادات الرعاية الآمنة بالتفصيل؟"
                }
            }
            lang.startsWith("tr") -> {
                if (!localizedSubject.isNullOrBlank()) {
                    "Tarama sonucum ($catLabel) şunu gösteriyor: \"$localizedSubject\". Olası nedenler, belirtiler ve güvenli bakım önerileri hakkında daha fazla bilgi verebilir misiniz?"
                } else {
                    "Az önce bir $catLabel taraması yaptım. Tarama sonuçlarım, olası nedenler, belirtiler ve güvenli bakım önerileri hakkında detaylı bilgi verebilir misiniz?"
                }
            }
            lang.startsWith("bn") -> {
                if (!localizedSubject.isNullOrBlank()) {
                    "আমার স্ক্যান ফলাফলে ($catLabel) দেখা গেছে: \"$localizedSubject\"। অনুগ্রহ করে এর সম্ভাব্য কারণ, লক্ষণ এবং নিরাপদ যত্নের পরামর্শ সম্পর্কে বিস্তারিত বলুন।"
                } else {
                    "আমি এইমাত্র একটি $catLabel স্ক্যান করেছি। অনুগ্রহ করে আমার স্ক্যান ফলাফল, সম্ভাব্য কারণ, লক্ষণ এবং নিরাপদ যত্নের পরামর্শ সম্পর্কে বিস্তারিত বলুন।"
                }
            }
            else -> {
                val catEn = getLocalizedCategoryLabel("en")
                when {
                    !conditionName.isNullOrBlank() ->
                        "My $catEn scan analysis shows signs of: $conditionName. Could you explain more about this condition, its potential causes, risk level, and recommended care steps?"
                    !summary.isNullOrBlank() ->
                        "My $catEn scan result notes: \"$summary\". Could you give me more detailed health guidance about these symptoms and care recommendations?"
                    else ->
                        "I recently performed a $catEn scan. Could you provide detailed guidance on common symptoms and appropriate care advice?"
                }
            }
        }
        return displayPrompt to apiContext
    }

    private fun getLocalizedCategoryLabel(lang: String): String {
        val isVi = lang.startsWith("vi")
        return when (currentCategory.uppercase(Locale.ROOT)) {
            "SKIN" -> if (isVi) "vùng da" else com.example.healthcareai.util.LocaleHelper.getLocalizedString(this, lang, R.string.category_skin)
            "EYE" -> if (isVi) "mắt" else com.example.healthcareai.util.LocaleHelper.getLocalizedString(this, lang, R.string.category_eye)
            "NAIL" -> if (isVi) "móng" else com.example.healthcareai.util.LocaleHelper.getLocalizedString(this, lang, R.string.category_nail)
            "ORAL" -> if (isVi) "răng miệng" else com.example.healthcareai.util.LocaleHelper.getLocalizedString(this, lang, R.string.category_mouth)
            "HAIR" -> when {
                isVi -> "tóc và da đầu"
                lang.startsWith("hi") -> "बाल और स्कैल्प"
                lang.startsWith("ru") -> "волосы и кожа головы"
                lang.startsWith("id") || lang.startsWith("in") -> "rambut dan kulit kepala"
                lang.startsWith("de") -> "Haare und Kopfhaut"
                lang.startsWith("es") -> "cabello y cuero cabelludo"
                lang.startsWith("fr") -> "cheveux et cuir chevelu"
                lang.startsWith("pt") -> "cabelo e couro cabeludo"
                lang.startsWith("ja") -> "髪と頭皮"
                lang.startsWith("ko") -> "모발 및 두피"
                lang.startsWith("zh_tw") -> "頭髮與頭皮"
                lang.startsWith("zh") -> "头发与头皮"
                lang.startsWith("ar") -> "الشعر وفروة الرأس"
                lang.startsWith("tr") -> "saç ve saç derisi"
                lang.startsWith("bn") -> "চুল ও মাথার ত্বক"
                else -> "hair and scalp"
            }
            else -> when {
                isVi -> "sức khỏe"
                lang.startsWith("hi") -> "स्वास्थ्य"
                lang.startsWith("ru") -> "здоровье"
                lang.startsWith("id") || lang.startsWith("in") -> "kesehatan"
                lang.startsWith("de") -> "Gesundheit"
                lang.startsWith("es") -> "salud"
                lang.startsWith("fr") -> "santé"
                lang.startsWith("pt") -> "saúde"
                lang.startsWith("ja") -> "健康"
                lang.startsWith("ko") -> "건강"
                lang.startsWith("zh") -> "健康"
                lang.startsWith("ar") -> "الصحة"
                lang.startsWith("tr") -> "sağlık"
                lang.startsWith("bn") -> "স্বাস্থ্য"
                else -> "health"
            }
        }
    }

    private fun bindProfileInfo() {
        val profile = currentProfile ?: profileRepo.getPrimaryProfile()
        val displayName = profile.name.ifBlank { "Alex Morgan" }
        binding.tvProfileName.text = displayName

        val isHistoricalScan = !intent.getStringExtra(EXTRA_SCAN_ID).isNullOrEmpty()
        if (isHistoricalScan) {
            binding.tvProfileSelectorLabel.setText(R.string.results_for)
            binding.ivProfileChevron.visibility = View.GONE
            binding.cardProfile.isClickable = false
            binding.cardProfile.isFocusable = false
        } else {
            binding.tvProfileSelectorLabel.setText(R.string.title_save_result_to)
            binding.ivProfileChevron.visibility = View.VISIBLE
            binding.cardProfile.isClickable = true
            binding.cardProfile.isFocusable = true
        }

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
    }

    private fun showProfileSelectionDialog() {
        SaveResultToBottomSheetDialog.show(
            fragmentManager = supportFragmentManager,
            selectedProfileId = currentProfile?.id
        ) { selectedProfile ->
            currentProfile = selectedProfile
            bindProfileInfo()
            updateSaveResultButtonState()
        }
    }

    private fun executeLesionAnalysis(file: File) {
        savedProfileIds.clear()
        currentScanId = null
        currentConversationId = null
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
        if (!LesionAnalysisRepository.isIdentified(result)) {
            showUnableState()
            return
        }

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

        // Update save button state based on whether result has been saved
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

    private fun handleBackPressed() {
        val hasUnsavedResult = binding.bottomBarSuccess.visibility == View.VISIBLE &&
                savedProfileIds.isEmpty() &&
                intent.getStringExtra(EXTRA_SCAN_ID).isNullOrEmpty()

        if (hasUnsavedResult) {
            LeaveWithoutSavingDialog.show(
                context = this,
                onStay = {
                    // User chose to stay on this page to save
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

        saveResultInternal(showToast = true)
        updateSaveResultButtonState()
    }

    private fun updateSaveResultButtonState() {
        val targetProfile = currentProfile ?: profileRepo.getPrimaryProfile()
        if (savedProfileIds.contains(targetProfile.id)) {
            binding.tvSaveResultText.setText(R.string.btn_saved_result)
            binding.ivSaveResultIcon.setImageResource(R.drawable.ic_check)
        } else {
            binding.tvSaveResultText.setText(R.string.btn_save_result)
            binding.ivSaveResultIcon.setImageResource(R.drawable.ic_folder_open)
        }
    }

    private fun saveResultInternal(showToast: Boolean = false) {
        val result = lastAnalysisResult ?: return
        val targetProfile = currentProfile ?: profileRepo.getPrimaryProfile()

        val findingTitle = result.keyFindings?.firstOrNull { !it.title.isNullOrBlank() }?.title?.trim()
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
        val summary = result.summary?.trim().orEmpty()
        val details = if (summary.isNotBlank()) summary else "Ongoing · $diagnosisDate"

        // 1. Save to Health Records (Conditions) if not already saved for this profile
        if (!savedProfileIds.contains(targetProfile.id)) {
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
        }

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
            analysisResult = result,
            conversationId = currentConversationId
        )
        ScanHistoryRepository.getInstance(this).saveScan(scanItem)

        savedProfileIds.add(targetProfile.id)
        if (showToast) {
            Toast.makeText(this, R.string.msg_result_saved_to_profile, Toast.LENGTH_SHORT).show()
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

    private fun getOrCreateConversationId(expectedPrompt: String? = null): String {
        val lang = com.example.healthcareai.util.LocaleHelper.getSavedLanguageCode(this).lowercase(Locale.ROOT)
        val chatRepo = AiChatRepository.getInstance(this)

        fun isMatchingConversation(convId: String?): Boolean {
            if (convId.isNullOrEmpty()) return false
            val existing = chatRepo.getConversationById(convId) ?: return true
            val firstUserMsg = existing.messages.firstOrNull { it.isUser }?.content ?: return true
            return expectedPrompt.isNullOrBlank() || firstUserMsg == expectedPrompt
        }

        if (isMatchingConversation(currentConversationId)) {
            return currentConversationId!!
        }

        val scanRepo = ScanHistoryRepository.getInstance(this)
        val scanId = currentScanId
        val imgPath = currentImageFile?.absolutePath

        if (!imgPath.isNullOrEmpty()) {
            val cachedLangConvId = getSharedPreferences(PREFS_RESULT_CONV, Context.MODE_PRIVATE)
                .getString("conv_for_${imgPath}_$lang", null)
            if (isMatchingConversation(cachedLangConvId)) {
                currentConversationId = cachedLangConvId
                return currentConversationId!!
            }
        }

        if (!scanId.isNullOrEmpty()) {
            val scan = scanRepo.getScanById(scanId)
            if (isMatchingConversation(scan?.conversationId)) {
                currentConversationId = scan!!.conversationId
                return currentConversationId!!
            }
        }

        if (!imgPath.isNullOrEmpty()) {
            val matchingScan = scanRepo.findScanByImagePath(imgPath)
            if (matchingScan != null) {
                if (currentScanId.isNullOrEmpty()) currentScanId = matchingScan.id
                if (isMatchingConversation(matchingScan.conversationId)) {
                    currentConversationId = matchingScan.conversationId
                    return currentConversationId!!
                }
            }
        }

        val newConvId = UUID.randomUUID().toString()
        currentConversationId = newConvId

        if (!imgPath.isNullOrEmpty()) {
            getSharedPreferences(PREFS_RESULT_CONV, Context.MODE_PRIVATE)
                .edit()
                .putString("conv_for_${imgPath}_$lang", newConvId)
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
