package com.supereva.fluentai.domain.session

/**
 * Stateless helper that encapsulates turn-taking transition rules.
 *
 * Enforces "one speaker at a time" by validating the current state
 * before returning the next state. The coordinator delegates to this
 * engine so transition logic stays in one place and is trivially testable.
 *
 * **No TTS, no coroutines** — pure functions only.
 */
class TurnTakingEngine {

    /**
     * Called after the user's turn has been processed (transcription + correction appended).
     * Signals that the AI is now preparing its reply.
     *
     * @return [SessionState.AiThinking]
     */
    fun onUserTurnProcessed(): SessionState = SessionState.AiThinking

    /**
     * Called when the AI reply generation actually begins
     * (i.e., the network call to [ConversationRepository.generateReply] starts).
     *
     * @return [SessionState.AiSpeaking]
     */
    fun onReplyGenerationStarted(): SessionState = SessionState.AiSpeaking

    /**
     * Called when the user taps the mic while the AI is speaking.
     *
     * @param currentState the coordinator's current [SessionState].
     * @return [SessionState.UserInterrupting] if [currentState] is [SessionState.AiSpeaking],
     *         or `null` if interruption is not valid in the current state (no-op).
     */
    fun onUserInterrupted(currentState: SessionState): SessionState? {
        return if (currentState is SessionState.AiSpeaking) {
            SessionState.UserInterrupting
        } else {
            null // Not valid — ignore
        }
    }

    /**
     * Called immediately after the interruption signal has been emitted.
     * Transitions the session back to listening so the user can speak.
     *
     * @return [SessionState.Listening]
     */
    fun onInterruptionHandled(): SessionState = SessionState.Listening
}
