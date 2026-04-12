package com.supereva.fluentai.ui.practice

import com.supereva.fluentai.domain.session.model.SessionTurn

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
    val error: String? = null,
    val partialTranscript: String = "",
    val isAutocorrectEnabled: Boolean = false,
    val isMicHot: Boolean = false, // Tracks actual hardware mic state
    val lastCorrectAnswer: String? = null, // Correct translation shown after evaluation
    val isWaitingForNextQuestion: Boolean = false
)
