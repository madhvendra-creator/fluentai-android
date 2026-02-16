package com.supereva.fluentai.data.repository

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.supereva.fluentai.domain.repository.SpeechRepository
import com.supereva.fluentai.domain.repository.SpeechResult
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.io.File
import java.util.Locale

class NativeSpeechRepository(
    private val context: Context
) : SpeechRepository {

    private var speechRecognizer: SpeechRecognizer? = null

    override fun startListening(language: String): Flow<SpeechResult> = callbackFlow {
        // Run on main thread is required for SpeechRecognizer
        val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
        
        mainHandler.post {
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {}
                        override fun onBeginningOfSpeech() {}
                        override fun onRmsChanged(rmsdB: Float) {
                            trySend(SpeechResult.VolumeUpdate(rmsdB))
                        }
                        override fun onBufferReceived(buffer: ByteArray?) {}
                        override fun onEndOfSpeech() {}

                        override fun onError(error: Int) {
                            val message = getErrorText(error)
                            trySend(SpeechResult.Error(message))
                            // Don't close implicitly; let ViewModel handle retry if needed, 
                            // but typical behavior is error ends session or requires restart.
                            close()
                        }

                        override fun onResults(results: Bundle?) {
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            if (!matches.isNullOrEmpty()) {
                                trySend(SpeechResult.Final(matches[0]))
                            }
                            close()
                        }

                        override fun onPartialResults(partialResults: Bundle?) {
                            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            if (!matches.isNullOrEmpty()) {
                                trySend(SpeechResult.Partial(matches[0]))
                            }
                        }

                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                }

                speechRecognizer?.startListening(intent)
            } else {
                trySend(SpeechResult.Error("Speech recognition not available on this device"))
                close()
            }
        }

        awaitClose {
            mainHandler.post {
                speechRecognizer?.destroy()
                speechRecognizer = null
            }
        }
    }

    override fun stopListening() {
        val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
        mainHandler.post {
            speechRecognizer?.stopListening()
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
