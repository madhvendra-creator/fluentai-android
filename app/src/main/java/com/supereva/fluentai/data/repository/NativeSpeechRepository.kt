package com.supereva.fluentai.data.repository

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
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

    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val audioManager by lazy {
        context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
    }

    private val _results = MutableSharedFlow<SpeechResult>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    @Volatile private var isRecognitionActive = false
    @Volatile private var currentLanguage = "en-IN"

    // Store original system volume to restore later
    private var originalSystemVolume = -1
    private var pendingStart: Runnable? = null
    private var lastStartAtMs = 0L
    private val minStartGapMs = 220L

    // ── Volume control — instant setStreamVolume(0) not ADJUST_MUTE ──

    private fun silenceSystem() {
        try {
            if (originalSystemVolume < 0) {
                originalSystemVolume = audioManager.getStreamVolume(
                    android.media.AudioManager.STREAM_SYSTEM
                )
            }
            audioManager.setStreamVolume(
                android.media.AudioManager.STREAM_SYSTEM, 0, 0
            )
        } catch (_: Exception) {}
    }

    private fun restoreSystem() {
        try {
            val vol = if (originalSystemVolume >= 0) originalSystemVolume
            else audioManager.getStreamMaxVolume(
                android.media.AudioManager.STREAM_SYSTEM) / 2
            audioManager.setStreamVolume(
                android.media.AudioManager.STREAM_SYSTEM, vol, 0
            )
        } catch (_: Exception) {}
    }

    private fun getOrCreateRecognizer(): SpeechRecognizer? {
        if (speechRecognizer != null) return speechRecognizer
        if (!SpeechRecognizer.isRecognitionAvailable(context)) return null

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {

                override fun onReadyForSpeech(params: Bundle?) {
                    isRecognitionActive = true
                    // ONLY place we restore audio — mic is confirmed ready
                    restoreSystem()
                }

                override fun onBeginningOfSpeech() {}

                override fun onRmsChanged(rmsdB: Float) {
                    _results.tryEmit(SpeechResult.VolumeUpdate(rmsdB))
                }

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    // Immediately silence the stop beep
                    silenceSystem()
                }

                override fun onError(error: Int) {
                    isRecognitionActive = false
                    _results.tryEmit(SpeechResult.Error(getErrorText(error)))
                }

                override fun onResults(results: Bundle?) {
                    isRecognitionActive = false
                    val matches = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        _results.tryEmit(SpeechResult.Final(matches[0]))
                    } else {
                        _results.tryEmit(SpeechResult.Error("No match"))
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        _results.tryEmit(SpeechResult.Partial(matches[0]))
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
        return speechRecognizer
    }

    private fun buildIntent(language: String): Intent =
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, language)
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, true)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            // Balanced timeout for smoother conversational turn-taking
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1800L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 500L)
        }

    override fun startListening(language: String): Flow<SpeechResult> {
        currentLanguage = language
        return _results
            .onStart { startRecognition(language) }
            .onCompletion {
                mainHandler.post {
                    if (isRecognitionActive) {
                        speechRecognizer?.cancel()
                        isRecognitionActive = false
                    }
                    restoreSystem()
                }
            }
    }

    fun restartListening(language: String = "en-IN") {
        currentLanguage = language
        mainHandler.post { scheduleStartRecognition(language) }
    }

    fun forceRestartListening(language: String = "en-IN") {
        currentLanguage = language
        mainHandler.post {
            silenceSystem()
            if (isRecognitionActive) {
                speechRecognizer?.cancel()
                isRecognitionActive = false
            }
            mainHandler.postDelayed({ scheduleStartRecognition(language) }, 150)
        }
    }

    fun fireStartListening(language: String = "en-IN") {
        restartListening(language)
    }

    private fun startRecognition(language: String) {
        scheduleStartRecognition(language)
    }

    private fun scheduleStartRecognition(language: String) {
        pendingStart?.let { mainHandler.removeCallbacks(it) }
        val now = SystemClock.elapsedRealtime()
        val waitMs = (minStartGapMs - (now - lastStartAtMs)).coerceAtLeast(0L)
        val task = Runnable {
            pendingStart = null
            startRecognitionNow(language)
        }
        pendingStart = task
        if (waitMs == 0L) {
            task.run()
        } else {
            mainHandler.postDelayed(task, waitMs)
        }
    }

    private fun startRecognitionNow(language: String) {
        val recognizer = getOrCreateRecognizer() ?: run {
            _results.tryEmit(SpeechResult.Error("Not available"))
            return
        }
        if (isRecognitionActive) {
            recognizer.cancel()
            isRecognitionActive = false
        }
        // Silence BEFORE startListening — this is what prevents the beep
        silenceSystem()
        lastStartAtMs = SystemClock.elapsedRealtime()
        recognizer.startListening(buildIntent(language))
    }

    override fun stopListening() {
        silenceSystem()
        mainHandler.post {
            if (isRecognitionActive) {
                speechRecognizer?.stopListening()
                isRecognitionActive = false
            }
            pendingStart?.let {
                mainHandler.removeCallbacks(it)
                pendingStart = null
            }
            mainHandler.postDelayed({ restoreSystem() }, 500)
        }
    }

    override fun release() {
        mainHandler.post {
            if (isRecognitionActive) {
                speechRecognizer?.cancel()
                isRecognitionActive = false
            }
            pendingStart?.let {
                mainHandler.removeCallbacks(it)
                pendingStart = null
            }
            speechRecognizer?.destroy()
            speechRecognizer = null
            restoreSystem()
        }
    }

    @Deprecated("Not supported")
    override suspend fun transcribeAudio(file: File): String {
        throw UnsupportedOperationException("Not supported")
    }

    private fun getErrorText(errorCode: Int): String = when (errorCode) {
        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
        SpeechRecognizer.ERROR_CLIENT -> "Client side error"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
        SpeechRecognizer.ERROR_NETWORK -> "Network error"
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
        SpeechRecognizer.ERROR_NO_MATCH -> "No match"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "RecognitionService busy"
        SpeechRecognizer.ERROR_SERVER -> "Server error"
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
        else -> "Didn't understand, please try again."
    }
}
