package com.supereva.fluentai.domain.session.model

/**
 * A single conversation turn within a [SpeakingSession].
 *
 * @property role        Who produced this turn (USER or AI).
 * @property transcript  Raw transcription of what was spoken.
 * @property correctedText  Grammar-corrected version (may equal [transcript]).
 * @property feedback    Human-readable coaching feedback for this turn.
 * @property score       Numeric quality score; null when not yet evaluated
 *                       or when evaluation is not applicable (e.g., AI turns).
 * @property audioPath   Local file path to the recording; null for text-only turns.
 * @property isStreaming  `true` while the AI is still streaming this turn's content.
 *                       Set to `false` when the final chunk arrives.
 * @property timestamp   Epoch milliseconds when this turn was created.
 */
data class SessionTurn(
    val role: TurnRole,
    val transcript: String,
    val correctedText: String = transcript,
    val feedback: String = "",
    val score: Int? = null,
    val audioPath: String? = null,
    val isStreaming: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
