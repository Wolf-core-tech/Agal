package com.agal.assistant.state

import com.agal.assistant.engine.models.CommandHistoryEntry

enum class AssistantStatus {
    IDLE,
    LISTENING_WAKE_WORD,
    WAKE_WORD_TRIGGERED,
    LISTENING_COMMAND,
    PROCESSING,
    SPEAKING_REPLY,
    ERROR
}

data class AssistantUiState(
    val status: AssistantStatus = AssistantStatus.IDLE,
    val activeWakeWord: String = "Hey Agal",
    val listeningReply: String = "I'm listening.",
    val assistantLanguage: String = "EN",
    val isContinuousListening: Boolean = true,
    val lastRecognizedText: String = "",
    val activeStatusMessage: String = "Say \"Hey Agal\" or tap to speak",
    val isSpeaking: Boolean = false,
    val isMuted: Boolean = false,
    val soundLevel: Float = 0.0f,
    val speechRate: Float = 1.0f,
    val pitch: Float = 1.0f,
    val history: List<CommandHistoryEntry> = emptyList(),
    val totalCommandsExecuted: Int = 0,
    val isLocalAiReady: Boolean = true
)
