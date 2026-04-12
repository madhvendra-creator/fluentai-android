package com.supereva.fluentai.data.repository

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.supereva.fluentai.domain.repository.SpeechRepository
import com.supereva.fluentai.domain.repository.SpeechResult
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import java.io.File

class NativeSpeechRepository(
    private val context: Context
) : SpeechRepository {

    // ── Single-instance recognizer ──────────────────────────────────────
    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val audioManager by lazy { context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager }

    // Shared flow that the single RecognitionListener emits into.
    // Replay 0 so subscribers only get live events.
    private val _results = MutableSharedFlow<SpeechResult>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    // Whether this is the FIRST startListening call in a user session
    // (i.e. the user just tapped the green button). We only mute beeps
    // on this first call; internal restarts skip the mute to avoid
    // audio routing disruption.
    @Volatile
    private var isFirstListenInSession = true

    // Track whether a recognition session is active to prevent double-starts
    @Volatile
    private var isRecognitionActive = false

    // ── Beep Muting ─────────────────────────────────────────────────────
    private val streamsToMute = listOf(
        android.media.AudioManager.STREAM_SYSTEM,
        android.media.AudioManager.STREAM_NOTIFICATION,
        android.media.AudioManager.STREAM_MUSIC,
        android.media.AudioManager.STREAM_RING
    )

    private fun muteBeeps() {
        streamsToMute.forEach { stream ->
            try { audioManager.adjustStreamVolume(stream, android.media.AudioManager.ADJUST_MUTE, 0) } catch (_: Exception) {}
        }
    }

    private fun unmuteBeeps() {
        streamsToMute.forEach { stream ->
            try { audioManager.adjustStreamVolume(stream, android.media.AudioManager.ADJUST_UNMUTE, 0) } catch (_: Exception) {}
        }
    }

    // ── Lazy recognizer initialisation ──────────────────────────────────
    private fun getOrCreateRecognizer(): SpeechRecognizer? {
        if (speechRecognizer != null) return speechRecognizer

        if (!SpeechRecognizer.isRecognitionAvailable(context)) return null

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    isRecognitionActive = true
                    unmuteBeeps() // Safe to unmute — mic is now actively capturing
                }

                override fun onBeginningOfSpeech() {}

                override fun onRmsChanged(rmsdB: Float) {
                    _results.tryEmit(SpeechResult.VolumeUpdate(rmsdB))
                }

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    // Mute the auto-stop beep. This fires before onResults/onError.
                    muteBeeps()
                }

                override fun onError(error: Int) {
                    isRecognitionActive = false
                    val message = getErrorText(error)
                    _results.tryEmit(SpeechResult.Error(message))
                    mainHandler.postDelayed({ unmuteBeeps() }, 800)
                }

                override fun onResults(results: Bundle?) {
                    isRecognitionActive = false
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        _results.tryEmit(SpeechResult.Final(matches[0]))
                    }
                    mainHandler.postDelayed({ unmuteBeeps() }, 800)
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        _results.tryEmit(SpeechResult.Partial(matches[0]))
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }

        return speechRecognizer
    }

    // ── Public API ──────────────────────────────────────────────────────

    override fun startListening(language: String): Flow<SpeechResult> {
        // Mark the session start; muting only happens on first call
        isFirstListenInSession = true

        return _results
            .onStart { fireStartListening(language) }
            .onCompletion {
                // Flow collector cancelled — stop recognition but keep the instance alive
                mainHandler.post {
                    if (isRecognitionActive) {
                        speechRecognizer?.cancel()
                        isRecognitionActive = false
                    }
                    unmuteBeeps()
                }
            }
    }

    /**
     * Called internally to (re-)start the recognizer on the main thread.
     * The ViewModel's while loop calls collect on the flow returned by
     * startListening(). When it needs a restart (after Final/Error), it
     * calls this method again via restartListening().
     */
    fun fireStartListening(language: String = "en-US") {
        mainHandler.post {
            val recognizer = getOrCreateRecognizer()
            if (recognizer == null) {
                _results.tryEmit(SpeechResult.Error("Speech recognition not available on this device"))
                return@post
            }

            // Cancel any in-progress recognition (safe even if idle)
            if (isRecognitionActive) {
                recognizer.cancel()
                isRecognitionActive = false
            }

            // Mute beeps only on the very first start of a user session
            if (isFirstListenInSession) {
                muteBeeps()
                isFirstListenInSession = false
                // Fail-safe unmute in case onReadyForSpeech never fires
                mainHandler.postDelayed({ unmuteBeeps() }, 1500)
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, language)
                putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, true)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 15000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 15000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 15000L)
            }

            recognizer.startListening(intent)
        }
    }

    /**
     * Restart the recognizer for the next chunk within the same user session.
     * Does NOT reset the mute-once flag — internal restarts are silent.
     */
    fun restartListening(language: String = "en-US") {
        mainHandler.post {
            val recognizer = getOrCreateRecognizer() ?: return@post

            // Cancel any in-progress recognition
            if (isRecognitionActive) {
                recognizer.cancel()
                isRecognitionActive = false
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, language)
                putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, true)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 15000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 15000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 15000L)
            }

            recognizer.startListening(intent)
        }
    }

    override fun stopListening() {
        muteBeeps()
        mainHandler.post {
            if (isRecognitionActive) {
                speechRecognizer?.stopListening()
                isRecognitionActive = false
            }
            mainHandler.postDelayed({ unmuteBeeps() }, 800)
        }
    }

    /**
     * Permanently destroys the SpeechRecognizer instance.
     * Only call this when the ViewModel is being cleared (onCleared).
     */
    override fun release() {
        mainHandler.post {
            unmuteBeeps()
            if (isRecognitionActive) {
                speechRecognizer?.cancel()
                isRecognitionActive = false
            }
            speechRecognizer?.destroy()
            speechRecognizer = null
        }
    }

    @Deprecated("Use startListening() for real-time recognition")
    override suspend fun transcribeAudio(file: File): String {
        throw UnsupportedOperationException("NativeSpeechRepository does not support file-based transcription.")
    }

    private fun getErrorText(errorCode: Int): String {
        return when (errorCode) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client side error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
            SpeechRecognizer.ERROR_NETWORK -> "Network error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No match"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "RecognitionService busy"
            SpeechRecognizer.ERROR_SERVER -> "Error from server"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
            else -> "Didn't understand, please try again."
        }
    }
}
