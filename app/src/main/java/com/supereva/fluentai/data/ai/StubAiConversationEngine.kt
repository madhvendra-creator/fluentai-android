package com.supereva.fluentai.data.ai

import com.supereva.fluentai.domain.ai.AiChunk
import com.supereva.fluentai.domain.ai.AiConversationEngine
import com.supereva.fluentai.domain.session.model.Difficulty
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.io.File

/**
 * Fake [AiConversationEngine] that simulates streaming responses
 * by emitting hardcoded chunks with delays. No networking.
 *
 * Use this for:
 * - UI development and previews
 * - Integration testing of the coordinator + ViewModel pipeline
 * - Demos without an API key
 *
 * Replace with `GeminiLiveEngine`, `OpenAiRealtimeEngine`, etc.
 * when the real backend is ready.
 */
class StubAiConversationEngine : AiConversationEngine {

    private val _responses = MutableSharedFlow<AiChunk>(extraBufferCapacity = 16)

    private val stubResponses = listOf(
        "That's a great start!",
        " Let me give you some feedback.",
        " Your pronunciation was quite clear,",
        " but try to slow down a little",
        " when using longer sentences.",
        " Overall, well done! Keep practising."
    )

    override suspend fun startSession(topicId: String, difficulty: Difficulty) {
        // Emit an intro greeting through the chunk stream so the
        // coordinator treats it like any other AI response.
        // When swapped for a real engine, the backend will produce
        // its own greeting + TTS audio — no UI changes required.
        val topicLabel = topicId.replace("_", " ")
        val greeting = "Hi! Let's start practicing English about $topicLabel. " +
            "I'll listen to you speak and give you feedback. Ready when you are!"

        // Simulate a brief "thinking" pause before the greeting
        delay(400)

        _responses.emit(
            AiChunk(
                textPartial = greeting,
                audioPartialPath = null,
                isFinal = true
            )
        )
    }

    override suspend fun sendStreamChunk(chunk: ByteArray) {
        // No-op for stub
    }

    override suspend fun sendUserAudio(file: File) {
        // Simulate streaming AI response with incremental chunks
        val accumulated = StringBuilder()

        stubResponses.forEachIndexed { index, fragment ->
            accumulated.append(fragment)
            val isFinal = index == stubResponses.lastIndex

            _responses.emit(
                AiChunk(
                    textPartial = accumulated.toString(),
                    audioPartialPath = null,
                    isFinal = isFinal
                )
            )

            if (!isFinal) {
                delay(300) // simulate token-by-token latency
            }
        }
    }

    override fun observeAiResponses(): Flow<AiChunk> =
        _responses.asSharedFlow()

    override suspend fun endSession() {
        // No-op — nothing to tear down
    }

    override suspend fun commitUserTurn() {
        // No-op for stub
    }
}
