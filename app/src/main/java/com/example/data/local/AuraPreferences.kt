package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.VoiceSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuraPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("aura_voice_prefs", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<VoiceSettings> = _settings.asStateFlow()

    private fun loadSettings(): VoiceSettings {
        return VoiceSettings(
            pitch = prefs.getFloat(KEY_PITCH, 1.0f),
            speed = prefs.getFloat(KEY_SPEED, 1.0f),
            voiceName = prefs.getString(KEY_VOICE_NAME, "") ?: "",
            continuousConversation = prefs.getBoolean(KEY_CONTINUOUS, true),
            backgroundListening = prefs.getBoolean(KEY_BACKGROUND, false),
            memoryEnabled = prefs.getBoolean(KEY_MEMORY_ENABLED, true),
            hapticFeedback = prefs.getBoolean(KEY_HAPTIC, true)
        )
    }

    fun updateSettings(newSettings: VoiceSettings) {
        prefs.edit()
            .putFloat(KEY_PITCH, newSettings.pitch)
            .putFloat(KEY_SPEED, newSettings.speed)
            .putString(KEY_VOICE_NAME, newSettings.voiceName)
            .putBoolean(KEY_CONTINUOUS, newSettings.continuousConversation)
            .putBoolean(KEY_BACKGROUND, newSettings.backgroundListening)
            .putBoolean(KEY_MEMORY_ENABLED, newSettings.memoryEnabled)
            .putBoolean(KEY_HAPTIC, newSettings.hapticFeedback)
            .apply()
        _settings.value = newSettings
    }

    companion object {
        private const val KEY_PITCH = "voice_pitch"
        private const val KEY_SPEED = "voice_speed"
        private const val KEY_VOICE_NAME = "voice_name"
        private const val KEY_CONTINUOUS = "continuous_conversation"
        private const val KEY_BACKGROUND = "background_listening"
        private const val KEY_MEMORY_ENABLED = "memory_enabled"
        private const val KEY_HAPTIC = "haptic_feedback"
    }
}
