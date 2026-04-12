package com.supereva.fluentai.domain.session

import com.supereva.fluentai.domain.ai.AiChunk
import com.supereva.fluentai.domain.ai.AiConversationEngine
import com.supereva.fluentai.domain.session.model.Difficulty
import com.supereva.fluentai.domain.session.model.SessionTurn
import com.supereva.fluentai.domain.session.model.SpeakingSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

/**
 * Orchestrates the lifecycle of a single [SpeakingSession].
 *
 * Implementations must be **thread-safe**: every mutation produces an
 * immutable copy of the session, and state is emitted through
 * [StateFlow] so that observers always see a consistent snapshot.
 *
 * Designed for streaming-AI integration — callers can
 * [appendOrUpdateAiTurn] incrementally as chunks arrive.
 */
interface SpeakingSessionCoordinator {

    /** Current lifecycle state of the session. */
    val sessionState: StateFlow<SessionState>

    /** Current session snapshot, or `null` if no session is active. */
    val currentSession: StateFlow<SpeakingSession?>

    /**
     * Begin a new session for the given [topicId] and [difficulty].
     *
     * Appends an AI intro message, emits [SessionState.AiSpeaking],
     * then transitions to [SessionState.Listening] after a short delay.
     *
     * @param scope coroutine scope used for the intro-speaking delay.
     * If a session is already active this is a no-op (resilient).
     */
    fun startSession(topicId: String, firstQuestion: String, difficulty: Difficulty, scope: CoroutineScope)

    /**
     * Safely tear down any in-flight session, then start a fresh one.
     *
     * Use this from entry-points (e.g. Home screen) where the
     * previous session may not have been cleaned up yet.
     *
     * @param scope coroutine scope used for the intro-speaking delay.
     */
    fun ensureFreshSession(topicId: String, firstQuestion: String, difficulty: Difficulty, scope: CoroutineScope)

    /**
     * Append a user-produced turn to the active session.
     * Recalculates [SpeakingSession.scoreProgress] if the turn carries a score.
     */
    fun appendUserTurn(turn: SessionTurn)

    /**
     * Append an AI-produced turn to the active session.
     * Can be called multiple times to support streaming token delivery.
     */
    fun appendAiTurn(turn: SessionTurn)

    /**
     * Append an AI turn and simulate word-by-word streaming.
     *
     * The turn is appended with [SessionTurn.isStreaming] = `true`,
     * then its transcript grows word-by-word every 60 ms.
     * State transitions: → [SessionState.AiSpeaking] during streaming,
     * → [SessionState.Listening] when complete.
     */
    fun streamAiTurn(turn: SessionTurn)

    /**
     * Process an incremental AI response chunk.
     *
     * - If the last message is an in-progress AI turn (`isStreaming = true`),
     *   its transcript is updated in-place with [AiChunk.textPartial].
     * - Otherwise a **new** AI turn is appended with `isStreaming = true`.
     * - When [AiChunk.isFinal] is `true`, the turn is marked complete
     *   (`isStreaming = false`).
     *
     * This produces a single growing chat bubble in the UI during streaming.
     */
    fun appendOrUpdateAiTurn(chunk: AiChunk)

    /**
     * Transition to an arbitrary [SessionState].
     * Useful for UI-driven state changes (e.g., Listening → Processing).
     */
    fun transitionTo(state: SessionState)

    /**
     * End the current session.
     * Transitions state to [SessionState.Completed] with the final session snapshot.
     */
    fun endSession()

    /**
     * Signal that the user's turn has been fully processed.
     * Transitions state to [SessionState.AiThinking].
     */
    fun onUserTurnProcessed()

    /**
     * Signal that the AI reply generation has started.
     * Transitions state to [SessionState.AiSpeaking].
     */
    fun onReplyGenerationStarted()

    /**
     * Attempt to interrupt the AI while it is speaking.
     *
     * If the current state is [SessionState.AiSpeaking]:
     * - Emits [SessionState.UserInterrupting] (transient signal for UI).
     * - Immediately transitions to [SessionState.Listening].
     *
     * If the current state is anything else, this is a no-op.
     */
    fun interruptAiSpeaking()

    /**
     * Subscribe to an [AiConversationEngine]'s chunk stream.
     *
     * The coordinator will:
     * 1. Transition to [SessionState.AiSpeaking] when partial chunks arrive.
     * 2. Call [appendOrUpdateAiTurn] for each chunk.
     * 3. Transition back to [SessionState.Listening] when [AiChunk.isFinal].
     *
     * Collection runs in the provided [scope] and is cancelled
     * when the session ends or a new subscription replaces it.
     */
    /**
     * Speak the given [text] using the configured TTS engine.
     * Use this for word-level pronunciation practice or UI feedback.
     */
    suspend fun speakText(text: String)

    fun subscribeToAiEngine(engine: AiConversationEngine, scope: CoroutineScope)
}
