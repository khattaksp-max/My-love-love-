package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "aura_memories")
data class AuraMemory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val content: String,
    val category: String = "Preference",
    val timestamp: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)

enum class AssistantState {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING,
    ERROR
}

enum class EmotionalTone(val label: String, val iconDescription: String) {
    CALM("Calm & Grounded", "Peaceful presence"),
    EMPATHIC("Compassionate", "Gentle, listening presence"),
    UPLIFTING("Supportive & Warm", "Joyful encouragement"),
    FOCUSED("Attentive & Action-Oriented", "Direct practical assistance"),
    REASSURING("Reassuring", "Comforting reassurance")
}

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val role: Role,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val detectedTone: EmotionalTone? = null,
    val executedAction: String? = null
) {
    enum class Role {
        USER,
        ASSISTANT,
        SYSTEM
    }
}

data class VoiceSettings(
    val pitch: Float = 1.0f,
    val speed: Float = 1.0f,
    val voiceName: String = "",
    val continuousConversation: Boolean = true,
    val backgroundListening: Boolean = false,
    val memoryEnabled: Boolean = true,
    val hapticFeedback: Boolean = true
)
