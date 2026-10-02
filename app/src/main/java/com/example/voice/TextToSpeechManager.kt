package com.example.voice

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.sin

class TextToSpeechManager(
    private val context: Context,
    private val coroutineScope: CoroutineScope,
    private val onSpeechStarted: () -> Unit = {},
    private val onSpeechCompleted: () -> Unit = {},
    private val onError: (String) -> Unit = {}
) {
    companion object {
        private const val TAG = "TextToSpeechManager"
        private const val UTTERANCE_ID = "aura_voice_utterance"
    }

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _speechAmplitude = MutableStateFlow(0f)
    val speechAmplitude: StateFlow<Float> = _speechAmplitude.asStateFlow()

    private val _availableVoices = MutableStateFlow<List<Voice>>(emptyList())
    val availableVoices: StateFlow<List<Voice>> = _availableVoices.asStateFlow()

    private var amplitudeJob: Job? = null

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true
                tts?.language = Locale.getDefault()
                loadVoices()
                setupListener()
            } else {
                Log.e(TAG, "TextToSpeech initialization failed with status $status")
                onError("Voice engine initialization failed")
            }
        }
    }

    private fun loadVoices() {
        try {
            val voices = tts?.voices?.filter { voice ->
                !voice.isNetworkConnectionRequired && voice.locale.language == Locale.getDefault().language
            } ?: emptyList()
            _availableVoices.value = voices.sortedBy { it.name }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying TTS voices", e)
        }
    }

    private fun setupListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _isSpeaking.value = true
                startAmplitudeSimulation()
                onSpeechStarted()
            }

            override fun onDone(utteranceId: String?) {
                _isSpeaking.value = false
                stopAmplitudeSimulation()
                onSpeechCompleted()
            }

            override fun onError(utteranceId: String?) {
                _isSpeaking.value = false
                stopAmplitudeSimulation()
                onError("Error during speech synthesis")
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                _isSpeaking.value = false
                stopAmplitudeSimulation()
                onError("TTS synthesis error: $errorCode")
            }
        })
    }

    private fun startAmplitudeSimulation() {
        amplitudeJob?.cancel()
        amplitudeJob = coroutineScope.launch(Dispatchers.Default) {
            var step = 0f
            while (isActive && _isSpeaking.value) {
                // Generate organic voice modulation wave
                val wave1 = (sin(step * 0.4f) + 1f) * 0.35f
                val wave2 = (sin(step * 0.9f + 1.2f) + 1f) * 0.25f
                val wave3 = (sin(step * 1.7f + 2.5f) + 1f) * 0.15f
                val amp = (wave1 + wave2 + wave3).coerceIn(0.15f, 0.95f)
                _speechAmplitude.value = amp
                step += 0.25f
                delay(40)
            }
            _speechAmplitude.value = 0f
        }
    }

    private fun stopAmplitudeSimulation() {
        amplitudeJob?.cancel()
        _speechAmplitude.value = 0f
    }

    fun speak(
        text: String,
        pitch: Float = 1.0f,
        speed: Float = 1.0f,
        voiceName: String = ""
    ) {
        if (!isInitialized || tts == null) {
            onError("Voice engine is still preparing")
            return
        }

        // Clean text: strip action tags and markdown symbols for natural speech
        val cleaned = text
            .replace(Regex("""\[ACTION:[^\]]+\]"""), "")
            .replace(Regex("""[*#_`~]"""), "")
            .trim()

        if (cleaned.isEmpty()) return

        stop()

        try {
            tts?.setPitch(pitch.coerceIn(0.5f, 2.0f))
            tts?.setSpeechRate(speed.coerceIn(0.5f, 2.0f))

            if (voiceName.isNotBlank()) {
                val selectedVoice = tts?.voices?.firstOrNull { it.name == voiceName }
                if (selectedVoice != null) {
                    tts?.voice = selectedVoice
                }
            }

            val params = Bundle().apply {
                putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, UTTERANCE_ID)
            }
            tts?.speak(cleaned, TextToSpeech.QUEUE_FLUSH, params, UTTERANCE_ID)
        } catch (e: Exception) {
            Log.e(TAG, "Error in speak()", e)
            onError("Failed to speak message")
        }
    }

    fun stop() {
        try {
            tts?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping TTS", e)
        } finally {
            _isSpeaking.value = false
            stopAmplitudeSimulation()
        }
    }

    fun shutdown() {
        stop()
        try {
            tts?.shutdown()
            tts = null
        } catch (e: Exception) {
            Log.e(TAG, "Error shutting down TTS", e)
        }
    }
}
