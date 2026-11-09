package com.example.healthcareai.ui.chat

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import coil.load
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.healthcareai.R
import com.example.healthcareai.data.local.PreferenceHelper
import com.example.healthcareai.data.model.ChatMessage
import com.example.healthcareai.data.model.ConversationSession
import com.example.healthcareai.data.repository.AiChatRepository
import com.example.healthcareai.databinding.ActivityAiChatBinding
import com.example.healthcareai.ui.base.BaseActivity
import com.example.healthcareai.ui.dialog.CustomRecordAudioPermissionDialog
import com.example.healthcareai.util.LocaleHelper
import com.example.healthcareai.util.PermissionReturnHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * AI Health Assistant Chat Screen powered by agentId = "402".
 * Supports closing into Conversation history, resuming past conversations,
 * and starting fresh chat sessions.
 */
class AiChatActivity : BaseActivity() {

    companion object {
        const val EXTRA_CONVERSATION_ID = "extra_conversation_id"
        const val EXTRA_INITIAL_PROMPT = "extra_initial_prompt"
        const val EXTRA_API_CONTEXT = "extra_api_context"
        const val EXTRA_AUTO_SEND = "extra_auto_send"
        const val EXTRA_START_NEW_CHAT = "extra_start_new_chat"
    }

    private lateinit var binding: ActivityAiChatBinding
    private lateinit var chatAdapter: ChatMessageAdapter
    private lateinit var chatRepository: AiChatRepository

    private var currentSession: ConversationSession = ConversationSession()

    private var isImeAnimationRunning = false
    private var wasKeyboardOpen = false

    private var speechRecognizer: SpeechRecognizer? = null
    private var isRecordingVoice = false
    private var recognizedVoiceText = ""
    private var waveformAnimationJob: Job? = null
    private var selectedImageUri: Uri? = null

    private var micPermissionDialog: Dialog? = null
    private var isWaitingForMicPermissionFromSettings = false

    private val recordAudioPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            val pref = PreferenceHelper.getInstance(this)
            if (isGranted) {
                pref.micDenialCount = 0
                startVoiceRecording()
            } else {
                pref.micDenialCount++
                Toast.makeText(
                    this,
                    R.string.ai_chat_mic_permission_required,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                selectedImageUri = uri
                binding.layoutImagePreview.visibility = View.VISIBLE
                binding.ivPreviewImage.load(uri) {
                    crossfade(true)
                }
                updateSendButtonState()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAiChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        chatRepository = AiChatRepository.getInstance(this)

        setupWindowInsets()
        setupRecyclerView()
        setupClickListeners()
        setupBackPress()
        initConversationSession(intent)
    }

    private fun setupBackPress() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                saveCurrentSessionIfActive()
                finish()
            }
        })
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        chatRepository.unregisterListener(currentSession.id)
        saveCurrentSessionIfActive()
        initConversationSession(intent)
    }

    override fun onResume() {
        super.onResume()
        checkAndResumePendingResponse()
        // Synchronous check if microphone permission was granted via Settings while paused
        if (PermissionReturnHelper.isPermissionGranted(this, Manifest.permission.RECORD_AUDIO)) {
            val pref = PreferenceHelper.getInstance(this)
            pref.micDenialCount = 0
            dismissMicPermissionDialog()
            if (isWaitingForMicPermissionFromSettings) {
                isWaitingForMicPermissionFromSettings = false
                startVoiceRecording()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (isRecordingVoice) {
            stopVoiceRecording(applyToInput = true)
        }
        saveCurrentSessionIfActive()
    }

    override fun onStop() {
        super.onStop()
        if (isRecordingVoice) {
            stopVoiceRecording(applyToInput = false)
        }
    }

    private fun setupWindowInsets() {
        ViewCompat.setWindowInsetsAnimationCallback(
            binding.rootAiChat,
            object : WindowInsetsAnimationCompat.Callback(DISPATCH_MODE_STOP) {
                override fun onPrepare(animation: WindowInsetsAnimationCompat) {
                    super.onPrepare(animation)
                    if (animation.typeMask and WindowInsetsCompat.Type.ime() != 0) {
                        isImeAnimationRunning = true
                    }
                }

                override fun onProgress(
                    insets: WindowInsetsCompat,
                    runningAnimations: MutableList<WindowInsetsAnimationCompat>
                ): WindowInsetsCompat {
                    val statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
                    val navBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
                    val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
                    updateWindowInsets(statusBars.top, navBars.bottom, ime.bottom)
                    return insets
                }

                override fun onEnd(animation: WindowInsetsAnimationCompat) {
                    super.onEnd(animation)
                    if (animation.typeMask and WindowInsetsCompat.Type.ime() != 0) {
                        isImeAnimationRunning = false
                        val currentInsets = ViewCompat.getRootWindowInsets(binding.rootAiChat)
                        val statusBars = currentInsets?.getInsets(WindowInsetsCompat.Type.statusBars())?.top ?: 0
                        val navBars = currentInsets?.getInsets(WindowInsetsCompat.Type.navigationBars())?.bottom ?: 0
                        val ime = currentInsets?.getInsets(WindowInsetsCompat.Type.ime())?.bottom ?: 0
                        updateWindowInsets(statusBars, navBars, ime)
                    }
                }
            }
        )

        ViewCompat.setOnApplyWindowInsetsListener(binding.rootAiChat) { _, insets ->
            if (!isImeAnimationRunning) {
                val statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
                val navBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
                val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
                updateWindowInsets(statusBars.top, navBars.bottom, ime.bottom)
            }
            insets
        }
    }

    private fun updateWindowInsets(statusTop: Int, navBottom: Int, imeBottom: Int) {
        val density = resources.displayMetrics.density

        // 1. Position topBar comfortably below the status bar with full height intact (standardized 16dp spacing)
        val targetTopMargin = if (statusTop > 0) {
            statusTop + (16 * density).toInt()
        } else {
            (24 * density).toInt()
        }
        (binding.topBar.layoutParams as? ViewGroup.MarginLayoutParams)?.let { lp ->
            if (lp.topMargin != targetTopMargin) {
                lp.topMargin = targetTopMargin
                binding.topBar.layoutParams = lp
            }
        }

        // 2. Adjust bottom bar so it never hugs the bottom edge and animates smoothly with keyboard
        val baseBottomPadding = if (navBottom > 0) {
            navBottom + (12 * density).toInt()
        } else {
            (24 * density).toInt()
        }
        val targetBottomPadding = maxOf(baseBottomPadding, imeBottom + (8 * density).toInt())

        if (binding.bottomBarContainer.paddingBottom != targetBottomPadding) {
            binding.bottomBarContainer.updatePadding(
                bottom = targetBottomPadding,
                top = (12 * density).toInt(),
                left = (16 * density).toInt(),
                right = (16 * density).toInt()
            )
        }

        val isKeyboardOpen = imeBottom > (50 * density)
        if (isKeyboardOpen && !wasKeyboardOpen) {
            binding.rvChatMessages.post { scrollToBottom() }
        }
        wasKeyboardOpen = isKeyboardOpen
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatMessageAdapter()
        val layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = false
        }
        binding.rvChatMessages.layoutManager = layoutManager
        binding.rvChatMessages.adapter = chatAdapter
    }

    private fun setupClickListeners() {
        // Back Button
        binding.btnBack.setOnClickListener {
            saveCurrentSessionIfActive()
            finish()
        }

        // Conversation History Button (ic_conversation)
        binding.btnConversationHistory.setOnClickListener {
            if (isRecordingVoice) {
                stopVoiceRecording(applyToInput = true)
            }
            saveCurrentSessionIfActive()
            val intent = Intent(this, ConversationActivity::class.java)
            startActivity(intent)
        }

        // Suggestion Card 1: Healthy lifestyle tips
        binding.cardSuggestion1.setOnClickListener {
            val query = getString(R.string.ai_chat_suggestion_1_query)
            sendMessage(query)
        }

        // Suggestion Card 2: Explain my scan results
        binding.cardSuggestion2.setOnClickListener {
            val query = getString(R.string.ai_chat_suggestion_2_query)
            sendMessage(query)
        }

        // Suggestion Card 3: Review my health data
        binding.cardSuggestion3.setOnClickListener {
            val query = getString(R.string.ai_chat_suggestion_3_query)
            sendMessage(query)
        }

        // Gallery Button
        binding.btnGallery.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        // Remove Preview Image Button
        binding.btnRemovePreviewImage.setOnClickListener {
            selectedImageUri = null
            binding.layoutImagePreview.visibility = View.GONE
            updateSendButtonState()
        }

        binding.etMessageInput.doAfterTextChanged {
            updateSendButtonState()
        }
        updateSendButtonState()

        // Send Text / Image Button
        binding.btnSend.setOnClickListener {
            val text = binding.etMessageInput.text?.toString()?.trim().orEmpty()
            val uri = selectedImageUri
            if (text.isNotEmpty() || uri != null) {
                sendMessage(text, uri?.toString())
                binding.etMessageInput.text?.clear()
                selectedImageUri = null
                binding.layoutImagePreview.visibility = View.GONE
                updateSendButtonState()
            }
        }

        // Input Action Send on Keyboard
        binding.etMessageInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                binding.btnSend.performClick()
                true
            } else {
                false
            }
        }

        // Mic Button
        binding.btnMic.setOnClickListener {
            checkAndStartVoiceRecording()
        }

        // Voice Recording: Cancel Button (X)
        binding.btnCancelVoice.setOnClickListener {
            cancelVoiceRecording()
        }

        // Voice Recording: Pause / Stop Button
        binding.btnPauseVoice.setOnClickListener {
            stopVoiceRecording(applyToInput = true)
        }

        // Voice Recording: Send Button
        binding.btnSendVoice.setOnClickListener {
            val textToSend = recognizedVoiceText.trim()
            stopVoiceRecording(applyToInput = false)
            if (textToSend.isNotEmpty()) {
                sendMessage(textToSend)
            } else {
                Toast.makeText(this, R.string.ai_chat_listening, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateSendButtonState() {
        val hasText = binding.etMessageInput.text?.toString()?.trim().isNullOrEmpty().not()
        val canSend = hasText || selectedImageUri != null
        binding.btnSend.isEnabled = canSend
        binding.btnSend.isClickable = canSend
        binding.btnSend.isFocusable = false
        binding.btnSend.isFocusableInTouchMode = false
        binding.btnSend.alpha = if (canSend) 1.0f else 0.45f
    }

    private fun initConversationSession(intent: Intent?) {
        val conversationId = intent?.getStringExtra(EXTRA_CONVERSATION_ID)
        val initialPrompt = intent?.getStringExtra(EXTRA_INITIAL_PROMPT) ?: intent?.getStringExtra("extra_initial_prompt")
        val apiContext = intent?.getStringExtra(EXTRA_API_CONTEXT)
        val autoSend = intent?.getBooleanExtra(EXTRA_AUTO_SEND, false) ?: false

        if (!conversationId.isNullOrEmpty()) {
            val existing = chatRepository.getConversationById(conversationId)
            val firstExistingUserMsg = existing?.messages?.firstOrNull { it.isUser }?.content
            val shouldReuseExisting = existing != null && (
                initialPrompt.isNullOrBlank() ||
                    !autoSend ||
                    firstExistingUserMsg == initialPrompt
                )

            if (shouldReuseExisting && existing != null) {
                currentSession = existing
                chatAdapter.setMessages(existing.messages)
                if (existing.messages.isNotEmpty()) {
                    showActiveChatState()
                    scrollToBottom()
                    checkAndResumePendingResponse()
                } else {
                    showWelcomeState()
                }
                attachSessionListener()
                return
            }
            // If a conversation ID was provided for a new session linked to a result, initialize with this ID
            currentSession = ConversationSession(
                id = if (existing == null) conversationId else java.util.UUID.randomUUID().toString()
            )
        } else {
            // When accessing without an explicit conversationId (e.g. from Home or "New Chat"), ALWAYS start a fresh conversation
            currentSession = ConversationSession()
        }

        attachSessionListener()
        chatAdapter.setMessages(emptyList())
        showWelcomeState()

        if (!initialPrompt.isNullOrBlank()) {
            if (autoSend) {
                binding.root.post {
                    sendMessage(initialPrompt, apiContext = apiContext)
                }
            } else {
                binding.etMessageInput.setText(initialPrompt)
                binding.etMessageInput.setSelection(initialPrompt.length)
                binding.etMessageInput.requestFocus()
            }
        }
    }

    private fun attachSessionListener() {
        chatRepository.registerListener(currentSession.id, object : AiChatRepository.MessageResponseListener {
            override fun onResponse(sessionId: String, botMessage: ChatMessage) {
                if (sessionId == currentSession.id) {
                    chatAdapter.removeLoadingMessage()
                    val alreadyInAdapter = chatAdapter.getItems().any {
                        !it.isUser && !it.isLoading && it.content == botMessage.content &&
                                Math.abs(it.timestamp - botMessage.timestamp) < 5000
                    }
                    if (!alreadyInAdapter) {
                        chatAdapter.addMessage(botMessage)
                        saveCurrentSessionIfActive()
                        scrollToBottom()
                    }
                }
            }

            override fun onError(sessionId: String, error: Throwable) {
                if (sessionId == currentSession.id) {
                    chatAdapter.removeLoadingMessage()
                    val fallback = getString(R.string.ai_chat_error_fallback)
                    val botMsg = ChatMessage(
                        content = fallback,
                        isUser = false,
                        timestamp = System.currentTimeMillis()
                    )
                    chatAdapter.addMessage(botMsg)
                    saveCurrentSessionIfActive()
                    scrollToBottom()
                }
            }
        })
    }

    private fun checkAndResumePendingResponse() {
        val sessionId = currentSession.id
        val isPending = chatRepository.isSessionPendingResponse(sessionId)
        val items = chatAdapter.getItems()
        val lastNonDate = items.lastOrNull { !it.isDateHeader }
        val lastIsUser = lastNonDate?.isUser == true

        if (isPending) {
            if (items.none { it.isLoading }) {
                val loadingMsg = ChatMessage(
                    content = "",
                    isUser = false,
                    isLoading = true
                )
                chatAdapter.addMessage(loadingMsg)
                scrollToBottom()
            }
        } else if (lastNonDate != null && lastNonDate.isUser) {
            if (items.none { it.isLoading }) {
                val loadingMsg = ChatMessage(
                    content = "",
                    isUser = false,
                    isLoading = true
                )
                chatAdapter.addMessage(loadingMsg)
                scrollToBottom()
            }
            val langCode = LocaleHelper.getSavedLanguageCode(this)
            val apiPrompt = when {
                lastNonDate.content.isNotEmpty() && !lastNonDate.imageUri.isNullOrEmpty() ->
                    "${lastNonDate.content} [Attached Image]"
                !lastNonDate.imageUri.isNullOrEmpty() ->
                    "I have uploaded an image for health analysis. Please guide me."
                else -> lastNonDate.content
            }
            attachSessionListener()
            chatRepository.sendMessageForSession(sessionId, apiPrompt, langCode)
        }
    }

    private fun saveCurrentSessionIfActive() {
        val messages = chatAdapter.getItems().filter { !it.isLoading }
        if (messages.any { it.isUser }) {
            currentSession.messages.clear()
            currentSession.messages.addAll(messages)
            val firstUserMsg = messages.firstOrNull { it.isUser }
            if (currentSession.title.isBlank()) {
                currentSession.title = chatRepository.generateTitleFromMessage(firstUserMsg?.content.orEmpty())
            }
            if (currentSession.createdAt <= 0L || (firstUserMsg != null && currentSession.createdAt > firstUserMsg.timestamp)) {
                currentSession.createdAt = firstUserMsg?.timestamp ?: currentSession.createdAt
            }
            val lastNonDateMsg = messages.lastOrNull { !it.isDateHeader }
            currentSession.lastSnippet = lastNonDateMsg?.content.orEmpty().take(80)
            currentSession.updatedAt = lastNonDateMsg?.timestamp ?: currentSession.createdAt
            chatRepository.saveOrUpdateConversation(currentSession)
        }
    }

    private fun showWelcomeState() {
        binding.layoutWelcome.visibility = View.VISIBLE
        binding.rvChatMessages.visibility = View.GONE
    }

    private fun showActiveChatState() {
        binding.layoutWelcome.visibility = View.GONE
        binding.rvChatMessages.visibility = View.VISIBLE
    }

    private fun sendMessage(content: String, imageUri: String? = null, apiContext: String? = null) {
        val trimmed = content.trim()
        if (trimmed.isEmpty() && imageUri.isNullOrEmpty()) return

        // Switch to active chat state if not already
        showActiveChatState()

        val currentItems = chatAdapter.getItems()
        val shouldAddDateHeader = currentItems.isEmpty() || !hasDateHeaderToday(currentItems)

        if (shouldAddDateHeader) {
            val dateHeader = ChatMessage(
                content = formatCurrentDateHeader(),
                isUser = false,
                isDateHeader = true,
                formattedDate = formatCurrentDateHeader()
            )
            chatAdapter.addMessage(dateHeader)
        }

        // 1. Add user message
        val userMsg = ChatMessage(
            content = trimmed,
            isUser = true,
            timestamp = System.currentTimeMillis(),
            imageUri = imageUri
        )
        if (currentSession.createdAt <= 0L || currentSession.messages.none { it.isUser }) {
            currentSession.createdAt = userMsg.timestamp
        }
        chatAdapter.addMessage(userMsg)
        saveCurrentSessionIfActive()
        scrollToBottom()

        // 2. Add loading bot typing message
        val loadingMsg = ChatMessage(
            content = "",
            isUser = false,
            isLoading = true
        )
        chatAdapter.addMessage(loadingMsg)
        scrollToBottom()

        // 3. Dispatch to repository background scope so it completes even if user navigates away
        val langCode = LocaleHelper.getSavedLanguageCode(this)
        val langName = LocaleHelper.getNativeNameByCode(langCode)
        val basePrompt = when {
            trimmed.isNotEmpty() && !imageUri.isNullOrEmpty() -> "$trimmed [Attached Image]"
            !imageUri.isNullOrEmpty() -> "I have uploaded an image for health analysis. Please guide me in $langName."
            !apiContext.isNullOrBlank() -> "$trimmed\n\n$apiContext"
            else -> trimmed
        }
        val apiPrompt = if (!langCode.lowercase(Locale.ROOT).startsWith("en")) {
            "$basePrompt\n(Please respond entirely in $langName.)"
        } else {
            basePrompt
        }

        attachSessionListener()
        chatRepository.sendMessageForSession(currentSession.id, apiPrompt, langCode)
    }

    private fun hasDateHeaderToday(messages: List<ChatMessage>): Boolean {
        val todayStr = getString(R.string.ai_chat_today)
        return messages.any { it.isDateHeader && (it.formattedDate?.startsWith("Today") == true || it.formattedDate?.startsWith(todayStr) == true) }
    }

    private fun formatCurrentDateHeader(): String {
        val todayStr = getString(R.string.ai_chat_today)
        val timeStr = com.example.healthcareai.util.LocaleHelper.formatTime(this, System.currentTimeMillis(), "hh:mm a")
        return "$todayStr, $timeStr"
    }

    private fun scrollToBottom() {
        binding.rvChatMessages.post {
            val count = chatAdapter.itemCount
            if (count > 0) {
                binding.rvChatMessages.smoothScrollToPosition(count - 1)
            }
        }
    }

    // =========================================================================
    // VOICE SPEECH-TO-TEXT LOGIC
    // =========================================================================

    private fun checkAndStartVoiceRecording() {
        val permission = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
        if (permission == PackageManager.PERMISSION_GRANTED) {
            val pref = PreferenceHelper.getInstance(this)
            pref.micDenialCount = 0
            startVoiceRecording()
        } else {
            val pref = PreferenceHelper.getInstance(this)
            if (pref.micDenialCount >= 2) {
                showCustomMicPermissionDialog()
            } else {
                recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    private fun showCustomMicPermissionDialog() {
        if (micPermissionDialog?.isShowing == true) return
        val pref = PreferenceHelper.getInstance(this)
        isWaitingForMicPermissionFromSettings = true
        micPermissionDialog = CustomRecordAudioPermissionDialog.show(
            activity = this,
            onGranted = {
                pref.micDenialCount = 0
                isWaitingForMicPermissionFromSettings = false
                startVoiceRecording()
            },
            onDenied = {
                isWaitingForMicPermissionFromSettings = false
            }
        )
    }

    private fun dismissMicPermissionDialog() {
        micPermissionDialog?.dismiss()
        micPermissionDialog = null
    }

    private fun startVoiceRecording() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "Speech recognition is not available on this device", Toast.LENGTH_SHORT).show()
            return
        }

        recognizedVoiceText = ""
        isRecordingVoice = true

        // Switch bottom bar to voice mode
        binding.layoutInputNormal.visibility = View.GONE
        binding.layoutInputVoice.visibility = View.VISIBLE
        binding.btnSendVoice.isEnabled = false
        binding.btnSendVoice.isClickable = false
        binding.btnSendVoice.alpha = 0.45f
        startWaveformAnimation()

        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}

                override fun onBeginningOfSpeech() {}

                override fun onRmsChanged(rmsdB: Float) {}

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {}

                override fun onError(error: Int) {}

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        recognizedVoiceText = matches[0]
                    }
                    val hasVoiceText = recognizedVoiceText.isNotBlank()
                    binding.btnSendVoice.isEnabled = hasVoiceText
                    binding.btnSendVoice.isClickable = hasVoiceText
                    binding.btnSendVoice.alpha = if (hasVoiceText) 1.0f else 0.45f
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        recognizedVoiceText = matches[0]
                    }
                    val hasVoiceText = recognizedVoiceText.isNotBlank()
                    binding.btnSendVoice.isEnabled = hasVoiceText
                    binding.btnSendVoice.isClickable = hasVoiceText
                    binding.btnSendVoice.alpha = if (hasVoiceText) 1.0f else 0.45f
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, LocaleHelper.getSavedLanguageCode(this@AiChatActivity))
        }

        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            stopVoiceRecording(applyToInput = false)
        }
    }

    private fun stopVoiceRecording(applyToInput: Boolean) {
        isRecordingVoice = false
        stopWaveformAnimation()

        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        speechRecognizer = null

        binding.layoutInputVoice.visibility = View.GONE
        binding.layoutInputNormal.visibility = View.VISIBLE

        if (applyToInput && recognizedVoiceText.isNotEmpty()) {
            binding.etMessageInput.setText(recognizedVoiceText)
            binding.etMessageInput.setSelection(recognizedVoiceText.length)
        }
    }

    private fun cancelVoiceRecording() {
        recognizedVoiceText = ""
        stopVoiceRecording(applyToInput = false)
    }

    private fun startWaveformAnimation() {
        waveformAnimationJob?.cancel()
        waveformAnimationJob = lifecycleScope.launch {
            val wavePatterns = listOf(
                "• • • • • • • • • • • • • • • • • • • •",
                "• • · · • • · · • • · · • • · · • • · ·",
                "· · • • · · • • · · • • · · • • · · • •",
                "• · • · • · • · • · • · • · • · • · • ·"
            )
            var index = 0
            while (isActive && isRecordingVoice) {
                binding.tvVoiceWaveform.text = wavePatterns[index % wavePatterns.size]
                index++
                delay(220)
            }
        }
    }

    private fun stopWaveformAnimation() {
        waveformAnimationJob?.cancel()
        waveformAnimationJob = null
    }

    override fun onDestroy() {
        super.onDestroy()
        dismissMicPermissionDialog()
        PermissionReturnHelper.stopPolling()
        chatRepository.unregisterListener(currentSession.id)
        saveCurrentSessionIfActive()
        stopVoiceRecording(applyToInput = false)
    }
}
