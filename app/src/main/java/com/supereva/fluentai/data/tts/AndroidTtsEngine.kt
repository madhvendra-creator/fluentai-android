package com.supereva.fluentai.data.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.supereva.fluentai.domain.tts.TtsEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

/**
 * Android [TextToSpeech]-backed implementation of [TtsEngine].
 *
 * Lives in the **data layer** so the domain layer stays free of
 * Android framework dependencies.
 *
 * Call [shutdown] when the app no longer needs TTS (e.g. in
 * `Application.onTerminate` or when the DI scope is destroyed).
 */
class AndroidTtsEngine(context: Context) : TtsEngine {

    private val _isSpeaking = MutableStateFlow(false)
    override val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private var tts: TextToSpeech? = null
    private var isInitialised = false

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeaking.value = true
                    }

                    override fun onDone(utteranceId: String?) {
                        _isSpeaking.value = false
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        _isSpeaking.value = false
                    }

                    override fun onError(utteranceId: String?, errorCode: Int) {
                        _isSpeaking.value = false
                    }
                })
                isInitialised = true
            }
        }
    }

    override suspend fun speak(text: String) {
        if (!isInitialised) return
        val utteranceId = UUID.randomUUID().toString()
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    private var audioTrack: android.media.AudioTrack? = null

    override suspend fun playPcm(data: ByteArray) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            if (audioTrack == null) {
                val sampleRate = 24000 // OpenAI Realtime default
                val minBufSize = android.media.AudioTrack.getMinBufferSize(
                    sampleRate,
                    android.media.AudioFormat.CHANNEL_OUT_MONO,
                    android.media.AudioFormat.ENCODING_PCM_16BIT
                )
                val bufferSize = minBufSize.coerceAtLeast(4096)

                audioTrack = android.media.AudioTrack.Builder()
                    .setAudioAttributes(
                        android.media.AudioAttributes.Builder()
                            .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    .setAudioFormat(
                        android.media.AudioFormat.Builder()
                            .setEncoding(android.media.AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(android.media.AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(android.media.AudioTrack.MODE_STREAM)
                    .build()

                audioTrack?.play()
                _isSpeaking.value = true
            }
            audioTrack?.write(data, 0, data.size)
        }
    }

    override fun stop() {
        tts?.stop()
        
        try {
            audioTrack?.stop()
            audioTrack?.flush()
        } catch (e: Exception) {
            // Ignore state errors on stop
        }
        // Don't release here, reusing is better for streaming, 
        // but stopping is good. OpenAI VAD might interrupt.
        // If we stop/flush, we are ready for next.

        _isSpeaking.value = false
    }

    override fun setLanguage(locale: Locale) {
        tts?.language = locale
    }

    /**
     * Release the underlying [TextToSpeech] engine.
     * After this call, [speak] and [stop] become no-ops.
     */
    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        
        audioTrack?.release()
        audioTrack = null
        
        isInitialised = false
        _isSpeaking.value = false
    }
}
