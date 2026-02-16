package com.supereva.fluentai.ui.practice

import com.supereva.fluentai.domain.session.model.SessionTurn

/**
 * Flat, UI-friendly representation of the current session state.
 *
 * Composed by [SpeakingSessionViewModel] by combining
 * [SessionState] and [SpeakingSession] flows from the coordinator.
 * Compose observes this single [StateFlow] to render the screen.
 */
data class SessionUiState(
    val topicId: String = "",
    val isSessionActive: Boolean = false,
    val isListening: Boolean = false,
    val isProcessing: Boolean = false,
    val isAiThinking: Boolean = false,
    val isAiSpeaking: Boolean = false,
    val isUserInterrupting: Boolean = false,
    val isCompleted: Boolean = false,
    val messages: List<SessionTurn> = emptyList(),
    val averageScore: Double = 0.0,
    val turnCount: Int = 0,
    val currentVolume: Float = 0f,
    val error: String? = null
)
