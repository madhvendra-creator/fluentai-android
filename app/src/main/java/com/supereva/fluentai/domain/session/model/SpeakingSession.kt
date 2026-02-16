package com.supereva.fluentai.domain.session.model

/**
 * Immutable snapshot of an active speaking-practice session.
 *
 * All mutations must go through [copy] to guarantee thread-safe
 * state propagation via [kotlinx.coroutines.flow.StateFlow].
 *
 * @property sessionId     Unique identifier (UUID) for this session.
 * @property topicId       The conversation topic chosen by the user.
 * @property difficulty    Session difficulty level.
 * @property messages      Ordered list of conversation turns.
 * @property scoreProgress Running score summary derived from scored turns.
 * @property startedAt     Epoch milliseconds when the session was created.
 * @property updatedAt     Epoch milliseconds of the most recent mutation.
 */
data class SpeakingSession(
    val sessionId: String,
    val topicId: String,
    val difficulty: Difficulty,
    val messages: List<SessionTurn> = emptyList(),
    val scoreProgress: ScoreProgress = ScoreProgress(),
    val startedAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = startedAt
)
