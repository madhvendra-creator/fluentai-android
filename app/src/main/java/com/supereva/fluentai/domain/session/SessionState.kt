package com.supereva.fluentai.domain.session

import com.supereva.fluentai.domain.session.model.SpeakingSession

/**
 * Represents the lifecycle state of a speaking session.
 *
 * The coordinator drives transitions between these states; the UI
 * layer observes them via [kotlinx.coroutines.flow.StateFlow].
 */
sealed class SessionState {

    /** No session is active. */
    data object Idle : SessionState()

    /** Microphone is open; recording user speech. */
    data object Listening : SessionState()

    /** User audio is being transcribed / analysed. */
    data object Processing : SessionState()

    /** AI is preparing its reply (between user turn processed and reply generation). */
    data object AiThinking : SessionState()

    /** AI response audio is being played back. */
    data object AiSpeaking : SessionState()

    /** User tapped mic while AI was speaking — interruption in progress. */
    data object UserInterrupting : SessionState()

    /** Session finished normally. Carries the final snapshot. */
    data class Completed(val session: SpeakingSession) : SessionState()

    /** An unrecoverable error occurred. */
    data class Error(
        val message: String,
        val cause: Throwable? = null
    ) : SessionState()
}
