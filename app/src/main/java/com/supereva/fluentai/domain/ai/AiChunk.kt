package com.supereva.fluentai.domain.ai

/**
 * An incremental response fragment from the AI conversation engine.
 *
 * During a streaming response, the engine emits a sequence of chunks:
 * - Each chunk carries the **accumulated** text so far in [textPartial].
 * - [audioPartialPath] points to a playable audio segment (nullable
 *   when the engine is text-only or the segment is not yet available).
 * - [isFinal] is `true` on the last chunk, signalling that the
 *   response is complete and the UI can finalize the message bubble.
 *
 * Pure Kotlin — no Android dependencies.
 */
data class AiChunk(
    val textPartial: String,
    val audioPartialPath: String? = null,
    val audioPCM: ByteArray? = null,
    val isFinal: Boolean = false
)
