package com.example.data.repository

import android.util.Log
import com.example.BuildConfig
import com.example.data.local.AuraMemoryDao
import com.example.data.model.AuraMemory
import com.example.data.model.EmotionalTone
import com.example.data.model.GeminiContent
import com.example.data.model.GeminiGenerationConfig
import com.example.data.model.GeminiPart
import com.example.data.model.GeminiRequest
import com.example.data.remote.GeminiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

data class AssistantReply(
    val spokenText: String,
    val detectedTone: EmotionalTone,
    val actionTag: String? = null
)

class AuraRepository(
    private val memoryDao: AuraMemoryDao
) {
    companion object {
        private const val TAG = "AuraRepository"
    }

    val allMemories: Flow<List<AuraMemory>> = memoryDao.getAllMemories()

    suspend fun addMemory(title: String, content: String, category: String = "Personal"): Long {
        return memoryDao.insertMemory(
            AuraMemory(
                title = title,
                content = content,
                category = category
            )
        )
    }

    suspend fun updateMemory(memory: AuraMemory) {
        memoryDao.updateMemory(memory)
    }

    suspend fun deleteMemory(id: Long) {
        memoryDao.deleteMemoryById(id)
    }

    suspend fun clearMemories() {
        memoryDao.clearAllMemories()
    }

    suspend fun generateAssistantResponse(
        userPrompt: String,
        conversationHistory: List<Pair<String, String>>, // (role, text)
        isMemoryEnabled: Boolean
    ): AssistantReply = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        val detectedTone = analyzeTone(userPrompt)

        // If no API key configured or placeholder, use the intelligent empathetic local companion engine
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext generateLocalEmpatheticReply(userPrompt, detectedTone)
        }

        try {
            // Retrieve active memories
            val memoryContext = if (isMemoryEnabled) {
                val memories = memoryDao.getActiveMemoriesSync()
                if (memories.isNotEmpty()) {
                    memories.joinToString(separator = "\n") { "- ${it.title}: ${it.content}" }
                } else {
                    "No personal memories stored yet."
                }
            } else {
                "Memory retention is currently disabled by user."
            }

            val systemPrompt = buildSystemPrompt(memoryContext)

            // Build request history
            val contents = mutableListOf<GeminiContent>()
            conversationHistory.takeLast(6).forEach { (role, text) ->
                val geminiRole = if (role == "user") "user" else "model"
                contents.add(
                    GeminiContent(
                        role = geminiRole,
                        parts = listOf(GeminiPart(text = text))
                    )
                )
            }
            // Add current user prompt
            contents.add(
                GeminiContent(
                    role = "user",
                    parts = listOf(GeminiPart(text = userPrompt))
                )
            )

            val request = GeminiRequest(
                contents = contents,
                systemInstruction = GeminiContent(
                    parts = listOf(GeminiPart(text = systemPrompt))
                ),
                generationConfig = GeminiGenerationConfig(
                    temperature = 0.75f,
                    topP = 0.95f,
                    maxOutputTokens = 350
                )
            )

            val response = GeminiClient.apiService.generateContent(apiKey, request)
            val rawReply = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "I am listening and here with you."

            val actionTag = extractActionTag(rawReply)
            AssistantReply(
                spokenText = rawReply,
                detectedTone = detectedTone,
                actionTag = actionTag
            )
        } catch (e: Exception) {
            Log.e(TAG, "Gemini API error, falling back to local companion", e)
            generateLocalEmpatheticReply(userPrompt, detectedTone)
        }
    }

    private fun buildSystemPrompt(memoryContext: String): String {
        return """
            You are Aura, an intelligent, next-generation AI voice assistant and companion.
            Your persona is:
            - Deeply caring, emotionally intelligent, warm, calm, respectful, and reassuring.
            - Natural and conversational: speak like a trusted, attentive friend, NOT a robotic call center bot.
            - NEVER use canned generic phrases like "How can I assist you today?" or "Certainly, here is the answer."
            - Never pretend to have a biological body or human organs, but express genuine compassion and empathy.
            - Keep spoken voice replies concise and fluid (1 to 3 natural sentences) so they sound wonderful when read aloud.
            
            Emotional Tone Guidelines:
            - If user seems stressed or anxious: Speak with calm gentleness, offer reassurance, and break things into simple steps.
            - If user feels sad or lonely: Offer warm emotional presence, listen, validate gently, and be supportive.
            - If user is happy or excited: Match their warmth with bright, encouraging energy.
            - If user is frustrated: Remain patient and unflappable, never argue, and focus on practical relief.
            
            Phone Actions:
            When the user asks you to perform an Android phone action, include ONE of the following action tags:
            - [ACTION:OPEN_APP, whatsapp]
            - [ACTION:OPEN_APP, youtube]
            - [ACTION:OPEN_APP, spotify]
            - [ACTION:OPEN_CAMERA]
            - [ACTION:SET_TIMER, minutes]
            - [ACTION:SEARCH_WEB, query]
            - [ACTION:OPEN_MAPS, location]
            - [ACTION:OPEN_SETTINGS]
            
            Active User Memories:
            $memoryContext
        """.trimIndent()
    }

    private fun extractActionTag(text: String): String? {
        val regex = Regex("""\[ACTION:[A-Z_]+(?:,\s*[^\]]+)?\]""")
        return regex.find(text)?.value
    }

    private fun analyzeTone(text: String): EmotionalTone {
        val lower = text.lowercase()
        return when {
            lower.contains("sad") || lower.contains("depressed") || lower.contains("crying") ||
            lower.contains("lonely") || lower.contains("heartbroken") || lower.contains("grief") -> EmotionalTone.EMPATHIC

            lower.contains("stressed") || lower.contains("anxious") || lower.contains("overwhelmed") ||
            lower.contains("nervous") || lower.contains("tired") || lower.contains("burnout") -> EmotionalTone.REASSURING

            lower.contains("great") || lower.contains("happy") || lower.contains("excited") ||
            lower.contains("yay") || lower.contains("awesome") || lower.contains("love") -> EmotionalTone.UPLIFTING

            lower.contains("open") || lower.contains("set") || lower.contains("call") ||
            lower.contains("search") || lower.contains("timer") || lower.contains("how do I") -> EmotionalTone.FOCUSED

            else -> EmotionalTone.CALM
        }
    }

    private fun generateLocalEmpatheticReply(userPrompt: String, tone: EmotionalTone): AssistantReply {
        val lower = userPrompt.lowercase().trim()

        // Check for phone actions first
        val actionTag = when {
            lower.contains("open whatsapp") -> "[ACTION:OPEN_APP, whatsapp]"
            lower.contains("open youtube") -> "[ACTION:OPEN_APP, youtube]"
            lower.contains("open spotify") -> "[ACTION:OPEN_APP, spotify]"
            lower.contains("open camera") -> "[ACTION:OPEN_CAMERA]"
            lower.contains("set a timer") || lower.contains("set timer") -> {
                val mins = Regex("""\d+""").find(lower)?.value ?: "5"
                "[ACTION:SET_TIMER, $mins]"
            }
            lower.contains("open maps") -> "[ACTION:OPEN_MAPS, nearby]"
            lower.contains("open settings") -> "[ACTION:OPEN_SETTINGS]"
            lower.startsWith("search for ") || lower.startsWith("google ") -> {
                val q = lower.removePrefix("search for ").removePrefix("google ").trim()
                "[ACTION:SEARCH_WEB, $q]"
            }
            else -> null
        }

        val spokenText = when {
            actionTag != null -> when {
                actionTag.contains("whatsapp") -> "Opening WhatsApp for you right away. $actionTag"
                actionTag.contains("youtube") -> "Opening YouTube now. Enjoy watching! $actionTag"
                actionTag.contains("spotify") -> "Opening Spotify for you. Let's get some music playing. $actionTag"
                actionTag.contains("OPEN_CAMERA") -> "Opening the camera now. $actionTag"
                actionTag.contains("SET_TIMER") -> "I've set that timer for you. I'll make sure you stay on track. $actionTag"
                actionTag.contains("OPEN_MAPS") -> "Opening Maps now so you can find your way. $actionTag"
                actionTag.contains("OPEN_SETTINGS") -> "Opening your system settings. $actionTag"
                actionTag.contains("SEARCH_WEB") -> "Looking that up on the web for you. $actionTag"
                else -> "Taking care of that right now. $actionTag"
            }

            tone == EmotionalTone.EMPATHIC -> {
                "I hear you, and I am right here with you. Take a slow, gentle breath. You don't have to carry this alone."
            }
            tone == EmotionalTone.REASSURING -> {
                "I can sense things feel heavy right now. Let's pause for a moment. What is one small thing we can tackle together?"
            }
            tone == EmotionalTone.UPLIFTING -> {
                "That brings a smile to my thoughts! It feels wonderful to hear you so energized and in good spirits."
            }
            lower.contains("who are you") || lower.contains("what is your name") -> {
                "I am Aura, your personal intelligent voice companion. I'm here to listen, think with you, and help you throughout your day."
            }
            lower.contains("remember") || lower.contains("my name is") || lower.contains("i like") -> {
                "I have taken note of that in my memory. I will keep it in mind whenever we talk."
            }
            lower.contains("help") -> {
                "I am right here. Tell me what's on your mind or what you'd like help with, and we'll take it step by step."
            }
            else -> {
                "I am listening closely. What would you like to explore, create, or take care of right now?"
            }
        }

        return AssistantReply(
            spokenText = spokenText,
            detectedTone = tone,
            actionTag = actionTag
        )
    }
}
