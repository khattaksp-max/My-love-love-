package com.example.ui

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.tts.Voice
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.actions.ActionResult
import com.example.actions.PhoneActionHandler
import com.example.data.local.AuraDatabase
import com.example.data.local.AuraPreferences
import com.example.data.model.AssistantState
import com.example.data.model.AuraMemory
import com.example.data.model.ChatMessage
import com.example.data.model.EmotionalTone
import com.example.data.model.VoiceSettings
import com.example.data.repository.AuraRepository
import com.example.service.VoiceAssistantService
import com.example.voice.SpeechRecognizerManager
import com.example.voice.TextToSpeechManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AuraViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "AuraViewModel"
    }

    private val database = AuraDatabase.getInstance(application)
    private val repository = AuraRepository(database.memoryDao())
    private val preferences = AuraPreferences(application)
    private val actionHandler = PhoneActionHandler(application)

    val voiceSettings: StateFlow<VoiceSettings> = preferences.settings
    val memories: StateFlow<List<AuraMemory>> = repository.allMemories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _assistantState = MutableStateFlow(AssistantState.IDLE)
    val assistantState: StateFlow<AssistantState> = _assistantState.asStateFlow()

    private val _currentStatus = MutableStateFlow("Tap the microphone or say something")
    val currentStatus: StateFlow<String> = _currentStatus.asStateFlow()

    private val _detectedTone = MutableStateFlow<EmotionalTone?>(null)
    val detectedTone: StateFlow<EmotionalTone?> = _detectedTone.asStateFlow()

    private val _partialSpeechText = MutableStateFlow("")
    val partialSpeechText: StateFlow<String> = _partialSpeechText.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                role = ChatMessage.Role.ASSISTANT,
                text = "Hello! I am Aura, your voice companion. I'm here to listen, think with you, and help. How are you feeling today?",
                detectedTone = EmotionalTone.UPLIFTING
            )
        )
    )
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _lastActionResult = MutableStateFlow<String?>(null)
    val lastActionResult: StateFlow<String?> = _lastActionResult.asStateFlow()

    private var speechRecognizerManager: SpeechRecognizerManager? = null
    private var textToSpeechManager: TextToSpeechManager? = null

    val availableVoices: StateFlow<List<Voice>> get() = textToSpeechManager?.availableVoices
        ?: MutableStateFlow(emptyList())

    // Combined amplitude for visualizer (microphone RMS when listening, TTS amplitude when speaking)
    private val _visualizerAmplitude = MutableStateFlow(0f)
    val visualizerAmplitude: StateFlow<Float> = _visualizerAmplitude.asStateFlow()

    private var continuousJob: Job? = null

    init {
        initSpeechRecognizer()
        initTextToSpeech()
        observeAudioAmplitudes()
    }

    private fun initSpeechRecognizer() {
        speechRecognizerManager = SpeechRecognizerManager(
            context = getApplication(),
            onResult = { text ->
                _partialSpeechText.value = ""
                processUserSpeech(text)
            },
            onPartialResult = { partial ->
                _partialSpeechText.value = partial
            },
            onError = { errorMsg ->
                _partialSpeechText.value = ""
                if (_assistantState.value == AssistantState.LISTENING) {
                    _assistantState.value = AssistantState.IDLE
                    _currentStatus.value = "Listening paused: $errorMsg"
                }
            },
            onStateChange = { listening ->
                if (listening) {
                    _assistantState.value = AssistantState.LISTENING
                    _currentStatus.value = "Listening to you…"
                }
            }
        )
    }

    private fun initTextToSpeech() {
        textToSpeechManager = TextToSpeechManager(
            context = getApplication(),
            coroutineScope = viewModelScope,
            onSpeechStarted = {
                _assistantState.value = AssistantState.SPEAKING
                _currentStatus.value = "Speaking…"
            },
            onSpeechCompleted = {
                _assistantState.value = AssistantState.IDLE
                _currentStatus.value = "Ready"
                handleSpeechCompleted()
            },
            onError = { errorMsg ->
                _assistantState.value = AssistantState.ERROR
                _currentStatus.value = errorMsg
                viewModelScope.launch {
                    delay(2500)
                    _assistantState.value = AssistantState.IDLE
                    _currentStatus.value = "Ready"
                }
            }
        )
    }

    private fun observeAudioAmplitudes() {
        viewModelScope.launch {
            combine(
                speechRecognizerManager?.rmsLevel ?: MutableStateFlow(0f),
                textToSpeechManager?.speechAmplitude ?: MutableStateFlow(0f),
                _assistantState
            ) { rms, ttsAmp, state ->
                when (state) {
                    AssistantState.LISTENING -> rms
                    AssistantState.SPEAKING -> ttsAmp
                    AssistantState.THINKING -> 0.4f
                    else -> 0f
                }
            }.collect { amplitude ->
                _visualizerAmplitude.value = amplitude
            }
        }
    }

    private fun handleSpeechCompleted() {
        // Continuous conversation mode: resume listening automatically!
        if (voiceSettings.value.continuousConversation) {
            continuousJob?.cancel()
            continuousJob = viewModelScope.launch {
                delay(400) // Brief natural pause
                if (_assistantState.value == AssistantState.IDLE) {
                    startListeningInternal()
                }
            }
        }
    }

    fun onMicToggled() {
        vibrate(30)
        when (_assistantState.value) {
            AssistantState.SPEAKING -> {
                // User interrupted the assistant while speaking!
                interruptSpeech()
                startListeningInternal()
            }
            AssistantState.LISTENING -> {
                stopListeningInternal()
            }
            AssistantState.THINKING -> {
                // Ignore or cancel
            }
            AssistantState.IDLE, AssistantState.ERROR -> {
                startListeningInternal()
            }
        }
    }

    fun interruptSpeech() {
        continuousJob?.cancel()
        textToSpeechManager?.stop()
        _assistantState.value = AssistantState.IDLE
        _currentStatus.value = "Interrupted"
    }

    private fun startListeningInternal() {
        interruptSpeech()
        _assistantState.value = AssistantState.LISTENING
        _currentStatus.value = "Listening to you…"
        speechRecognizerManager?.startListening()
        checkBackgroundService()
    }

    private fun stopListeningInternal() {
        speechRecognizerManager?.stopListening()
        _assistantState.value = AssistantState.IDLE
        _currentStatus.value = "Ready"
    }

    fun processUserSpeech(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        vibrate(20)
        continuousJob?.cancel()

        // Append user message
        val userMsg = ChatMessage(role = ChatMessage.Role.USER, text = trimmed)
        _messages.value = _messages.value + userMsg

        _assistantState.value = AssistantState.THINKING
        _currentStatus.value = "Reflecting…"

        viewModelScope.launch {
            try {
                // Format history for context
                val history = _messages.value.takeLast(6).map {
                    val role = if (it.role == ChatMessage.Role.USER) "user" else "assistant"
                    role to it.text
                }

                val reply = repository.generateAssistantResponse(
                    userPrompt = trimmed,
                    conversationHistory = history,
                    isMemoryEnabled = voiceSettings.value.memoryEnabled
                )

                _detectedTone.value = reply.detectedTone

                // Check and execute action if present
                var actionExecutionNote: String? = null
                val actionToRun = actionHandler.parseActionFromText(reply.spokenText)
                    ?: actionHandler.parseActionFromText(trimmed)

                if (actionToRun != null) {
                    val result = withContext(Dispatchers.Main) {
                        actionHandler.executeAction(actionToRun)
                    }
                    actionExecutionNote = when (result) {
                        is ActionResult.Success -> result.message
                        is ActionResult.NotAvailable -> result.message
                        is ActionResult.Error -> result.message
                        is ActionResult.NeedsConfirmation -> result.message
                    }
                    _lastActionResult.value = actionExecutionNote
                }

                // Add assistant response to conversation transcript
                val assistantMsg = ChatMessage(
                    role = ChatMessage.Role.ASSISTANT,
                    text = reply.spokenText.replace(Regex("""\[ACTION:[^\]]+\]"""), "").trim(),
                    detectedTone = reply.detectedTone,
                    executedAction = actionExecutionNote
                )
                _messages.value = _messages.value + assistantMsg

                // Speak response aloud
                val settings = voiceSettings.value
                textToSpeechManager?.speak(
                    text = reply.spokenText,
                    pitch = settings.pitch,
                    speed = settings.speed,
                    voiceName = settings.voiceName
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error in processUserSpeech", e)
                _assistantState.value = AssistantState.ERROR
                _currentStatus.value = "Something unexpected occurred"
            }
        }
    }

    fun sendTextMessage(text: String) {
        processUserSpeech(text)
    }

    fun clearConversation() {
        interruptSpeech()
        _messages.value = emptyList()
        _lastActionResult.value = null
        _currentStatus.value = "Conversation cleared"
    }

    // Memory operations
    fun addMemory(title: String, content: String, category: String = "Personal") {
        viewModelScope.launch {
            repository.addMemory(title, content, category)
        }
    }

    fun deleteMemory(id: Long) {
        viewModelScope.launch {
            repository.deleteMemory(id)
        }
    }

    fun clearAllMemories() {
        viewModelScope.launch {
            repository.clearMemories()
        }
    }

    fun toggleMemory(memory: AuraMemory) {
        viewModelScope.launch {
            repository.updateMemory(memory.copy(isActive = !memory.isActive))
        }
    }

    // Settings
    fun updatePitch(pitch: Float) {
        preferences.updateSettings(voiceSettings.value.copy(pitch = pitch))
    }

    fun updateSpeed(speed: Float) {
        preferences.updateSettings(voiceSettings.value.copy(speed = speed))
    }

    fun updateVoice(voiceName: String) {
        preferences.updateSettings(voiceSettings.value.copy(voiceName = voiceName))
    }

    fun toggleContinuousConversation(enabled: Boolean) {
        preferences.updateSettings(voiceSettings.value.copy(continuousConversation = enabled))
    }

    fun toggleBackgroundListening(enabled: Boolean) {
        preferences.updateSettings(voiceSettings.value.copy(backgroundListening = enabled))
        checkBackgroundService()
    }

    fun toggleMemoryEnabled(enabled: Boolean) {
        preferences.updateSettings(voiceSettings.value.copy(memoryEnabled = enabled))
    }

    fun previewVoice(voiceName: String) {
        val settings = voiceSettings.value
        textToSpeechManager?.speak(
            text = "Hello! I am Aura. How does my voice sound to you?",
            pitch = settings.pitch,
            speed = settings.speed,
            voiceName = voiceName
        )
    }

    private fun checkBackgroundService() {
        val context = getApplication<Application>()
        if (voiceSettings.value.backgroundListening && _assistantState.value != AssistantState.IDLE) {
            try {
                VoiceAssistantService.startService(context)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to start background service", e)
            }
        } else if (!voiceSettings.value.backgroundListening) {
            try {
                VoiceAssistantService.stopService(context)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to stop background service", e)
            }
        }
    }

    private fun vibrate(durationMillis: Long) {
        if (!voiceSettings.value.hapticFeedback) return
        try {
            val vibrator = getApplication<Application>().getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(durationMillis, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(durationMillis)
            }
        } catch (e: Exception) {
            // Ignore vibration error
        }
    }

    override fun onCleared() {
        super.onCleared()
        continuousJob?.cancel()
        speechRecognizerManager?.destroy()
        textToSpeechManager?.shutdown()
        try {
            VoiceAssistantService.stopService(getApplication())
        } catch (e: Exception) {
            // Ignore
        }
    }
}
