package com.example.healthcareai.ui.chat

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.healthcareai.R
import com.example.healthcareai.data.model.ChatMessage
import com.example.healthcareai.data.model.ConversationSession
import com.example.healthcareai.data.repository.AiChatRepository
import com.example.healthcareai.databinding.ActivityAiChatBinding
import com.example.healthcareai.ui.base.BaseActivity
import com.example.healthcareai.util.LocaleHelper
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
    }

    private lateinit var binding: ActivityAiChatBinding
    private lateinit var chatAdapter: ChatMessageAdapter
    private lateinit var chatRepository: AiChatRepository

    private var currentSession: ConversationSession = ConversationSession()

    private var speechRecognizer: SpeechRecognizer? = null
    private var isRecordingVoice = false
    private var recognizedVoiceText = ""
    private var waveformAnimationJob: Job? = null

    private val recordAudioPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                startVoiceRecording()
            } else {
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
                val prompt = "I've uploaded an image for health analysis. Please guide me on what steps to take or what questions to consider."
                binding.etMessageInput.setText(prompt)
                binding.etMessageInput.setSelection(prompt.length)
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
        initConversationSession(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        saveCurrentSessionIfActive()
        initConversationSession(intent)
    }

    override fun onPause() {
        super.onPause()
        saveCurrentSessionIfActive()
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.rootAiChat) { _, insets ->
            val statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val navBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            val density = resources.displayMetrics.density

            // 1. Position topBar comfortably below the status bar with full height intact
            val targetTopMargin = if (statusBars.top > 0) {
                statusBars.top + (6 * density).toInt()
            } else {
                (20 * density).toInt()
            }
            binding.topBar.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = targetTopMargin
            }

            // 2. Adjust bottom bar so it never hugs the bottom edge ("không để sát đáy")
            val targetBottomPadding = if (ime.bottom > 0) {
                ime.bottom + (8 * density).toInt()
            } else {
                val navBottom = navBars.bottom
                if (navBottom > 0) navBottom + (12 * density).toInt() else (28 * density).toInt()
            }
            binding.bottomBarContainer.updatePadding(
                bottom = targetBottomPadding,
                top = (8 * density).toInt(),
                left = (16 * density).toInt(),
                right = (16 * density).toInt()
            )

            insets
        }
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

        // Dim / Enable Send Button based on input text
        val updateSendButtonState = {
            val hasText = binding.etMessageInput.text?.toString()?.trim().isNullOrEmpty().not()
            binding.btnSend.isEnabled = hasText
            binding.btnSend.isClickable = hasText
            binding.btnSend.isFocusable = hasText
            binding.btnSend.alpha = if (hasText) 1.0f else 0.45f
        }

        binding.etMessageInput.doAfterTextChanged {
            updateSendButtonState()
        }
        updateSendButtonState()

        // Send Text Button
        binding.btnSend.setOnClickListener {
            val text = binding.etMessageInput.text?.toString()?.trim().orEmpty()
            if (text.isNotEmpty()) {
                sendMessage(text)
                binding.etMessageInput.text?.clear()
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

    private fun initConversationSession(intent: Intent?) {
        val conversationId = intent?.getStringExtra(EXTRA_CONVERSATION_ID)
        if (!conversationId.isNullOrEmpty()) {
            val existing = chatRepository.getConversationById(conversationId)
            if (existing != null) {
                currentSession = existing
                chatAdapter.setMessages(existing.messages)
                if (existing.messages.isNotEmpty()) {
                    showActiveChatState()
                    scrollToBottom()
                } else {
                    showWelcomeState()
                }
                return
            }
        }

        // When launching without a specific conversation, always start a brand new conversation
        currentSession = ConversationSession()
        chatAdapter.setMessages(emptyList())
        showWelcomeState()
    }

    private fun saveCurrentSessionIfActive() {
        val messages = chatAdapter.getItems().filter { !it.isLoading }
        if (messages.any { it.isUser }) {
            currentSession.messages.clear()
            currentSession.messages.addAll(messages)
            if (currentSession.title.isBlank()) {
                val firstUserMsg = messages.firstOrNull { it.isUser }?.content.orEmpty()
                currentSession.title = chatRepository.generateTitleFromMessage(firstUserMsg)
            }
            val lastNonDateMsg = messages.lastOrNull { !it.isDateHeader }?.content.orEmpty()
            currentSession.lastSnippet = lastNonDateMsg.take(80)
            currentSession.updatedAt = System.currentTimeMillis()
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

    private fun sendMessage(content: String) {
        val trimmed = content.trim()
        if (trimmed.isEmpty()) return

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
            timestamp = System.currentTimeMillis()
        )
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

        // 3. Call AI API with agentId = "402"
        val langCode = LocaleHelper.getSavedLanguageCode(this)
        lifecycleScope.launch {
            val result = chatRepository.sendMessage(content = trimmed, langCode = langCode)
            chatAdapter.removeLoadingMessage()

            val botContent = result.getOrElse {
                getString(R.string.ai_chat_error_fallback)
            }.ifBlank {
                getString(R.string.ai_chat_error_fallback)
            }

            val botMsg = ChatMessage(
                content = botContent,
                isUser = false,
                timestamp = System.currentTimeMillis()
            )
            chatAdapter.addMessage(botMsg)
            saveCurrentSessionIfActive()
            scrollToBottom()
        }
    }

    private fun hasDateHeaderToday(messages: List<ChatMessage>): Boolean {
        val todayHeader = formatCurrentDateHeader()
        return messages.any { it.isDateHeader && it.formattedDate?.startsWith("Today") == true }
    }

    private fun formatCurrentDateHeader(): String {
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val timeStr = timeFormat.format(Calendar.getInstance().time)
        return "Today, $timeStr"
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
            startVoiceRecording()
        } else {
            recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
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
        saveCurrentSessionIfActive()
        stopVoiceRecording(applyToInput = false)
    }
}
