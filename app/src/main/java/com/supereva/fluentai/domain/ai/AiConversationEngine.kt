package com.supereva.fluentai.domain.ai

import com.supereva.fluentai.domain.session.model.Difficulty
import kotlinx.coroutines.flow.Flow
import java.io.File

/**
 * Abstraction over the real-time AI conversation backend.
 *
 * Implementations may wrap:
 * - **Gemini Live** (streaming multi-modal)
 * - **OpenAI Realtime Voice** (WebSocket audio)
 * - **Local on-device models**
 * - **Stub / fake** for testing
 *
 * The engine manages its own connection lifecycle.
 * Callers feed user audio via [sendUserAudio] and observe AI
 * responses through [observeAiResponses] — a cold [Flow] that
 * emits [AiChunk]s as they arrive from the backend.
 *
 * **Threading contract:** all `suspend` functions are
 * main-safe; they suspend on IO internally.
 */
interface AiConversationEngine {

    /**
     * Open a session with the AI backend for the given topic
     * and difficulty. Must be called before [sendUserAudio].
     */
    suspend fun startSession(
        topicId: String,
        difficulty: Difficulty = Difficulty.BEGINNER
    )

    /**
     * Send a raw audio chunk (PCM 16-bit, 24kHz typically) to the AI for
     * real-time analysis.
     */
    suspend fun sendStreamChunk(chunk: ByteArray)

    /**
     * Send a recorded audio file to the AI for analysis.
     *
     * The engine transcribes + analyses the audio and emits
     * response chunks through [observeAiResponses].
     */
    suspend fun sendUserAudio(file: File)

    /**
     * Cold flow of [AiChunk]s from the AI backend.
     *
     * Collectors receive incremental text (and optional audio)
     * fragments. The last chunk in a response has [AiChunk.isFinal]
     * set to `true`.
     *
     * A new flow is returned each call; callers should collect
     * for the lifetime of the session.
     */
    fun observeAiResponses(): Flow<AiChunk>

    /**
     * Gracefully close the AI session and release resources.
     */
    suspend fun endSession()
}
