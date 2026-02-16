package com.supereva.fluentai.domain.tts

import kotlinx.coroutines.flow.StateFlow

/**
 * Platform-agnostic text-to-speech contract.
 *
 * The domain layer depends on this interface only;
 * the concrete Android implementation lives in the data layer.
 */
interface TtsEngine {

    /**
     * Speak the given [text] aloud.
     *
     * This is a **suspend** function so it can be called from a coroutine
     * without blocking. Implementations should return once speech synthesis
     * has been queued (not necessarily finished).
     */
    suspend fun speak(text: String)

    /**
     * Stream raw PCM audio (16-bit, 24kHz) to the output.
     */
    suspend fun playPcm(data: ByteArray)

    /**
     * Immediately stop any in-progress speech.
     * Safe to call even if nothing is currently being spoken.
     */
    fun stop()

    /**
     * Observable flag indicating whether the engine is currently speaking.
     * UI can observe this to show visual indicators if needed.
     */
    val isSpeaking: StateFlow<Boolean>
}
