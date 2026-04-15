package com.supereva.fluentai.data.repository

import android.content.Context
import com.supereva.fluentai.data.audio.AudioRecorder
import com.supereva.fluentai.domain.auth.AuthManager
import com.supereva.fluentai.domain.repository.SpeechRepository
import com.supereva.fluentai.domain.repository.SpeechResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.TimeUnit

class NativeSpeechRepository(
    private val context: Context,
    private val audioRecorder: AudioRecorder,
    private val authManager: AuthManager
) : SpeechRepository {

    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var listeningJob: Job? = null
    private var transcribeJob: Job? = null
    private val pendingSegments = Channel<AudioRecorder.SegmentFile>(capacity = Channel.UNLIMITED)

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .build()

    private val _results = MutableSharedFlow<SpeechResult>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    @Volatile private var isStreaming = false
    @Volatile private var currentLanguage = "en-IN"

    private var inSpeech = false
    private var utteranceStartMs = 0L
    private var lastSpeechMs = 0L
    private val utteranceBuffer = ByteArrayOutputStream()

    private val endpointSilenceMs = 1500L
    private val minSpeechMs = 250L
    private val maxUtteranceBytes = AudioRecorder.SAMPLE_RATE * 2 * 20 // 20 seconds cap

    override fun startListening(language: String): Flow<SpeechResult> {
        currentLanguage = language
        return _results
            .onStart { startContinuousStreaming(language) }
            .onCompletion { stopListening() }
    }

    private fun startContinuousStreaming(language: String) {
        if (isStreaming) return
        isStreaming = true
        currentLanguage = language

        if (transcribeJob == null) {
            transcribeJob = repoScope.launch {
                for (segment in pendingSegments) {
                    try {
                        val text = transcribeAudio(segment.file).trim()
                        if (text.isNotBlank()) {
                            _results.tryEmit(SpeechResult.Final(text))
                        } else {
                            _results.tryEmit(SpeechResult.Error("No clear speech detected"))
                        }
                    } catch (e: Exception) {
                        _results.tryEmit(SpeechResult.Error(e.message ?: "Transcription failed"))
                    } finally {
                        segment.file.delete()
                    }
                }
            }
        }

        listeningJob = repoScope.launch {
            try {
                audioRecorder.streamVadFrames(
                    AudioRecorder.VadConfig(
                        frameDurationMs = 20,
                        speechThresholdDbOffset = 9f,
                        minThresholdDb = -45f
                    )
                ).collect { frame ->
                    if (!isStreaming) return@collect
                    _results.tryEmit(SpeechResult.VolumeUpdate(frame.rmsDb))
                    consumeVadFrame(frame, currentLanguage)
                }
            } catch (e: Exception) {
                _results.tryEmit(SpeechResult.Error(e.message ?: "Mic stream failed"))
                isStreaming = false
            }
        }
    }

    private fun consumeVadFrame(frame: AudioRecorder.VadFrame, language: String) {
        val now = frame.timestampMs
        if (frame.isSpeech) {
            if (!inSpeech) {
                inSpeech = true
                utteranceStartMs = now
                utteranceBuffer.reset()
            }
            lastSpeechMs = now
            appendToUtterance(frame.pcm, frame.bytesRead)
            return
        }

        if (!inSpeech) return

        appendToUtterance(frame.pcm, frame.bytesRead)
        val silenceFor = now - lastSpeechMs
        val speechDuration = lastSpeechMs - utteranceStartMs
        val reachedMaxBytes = utteranceBuffer.size() >= maxUtteranceBytes

        if ((silenceFor >= endpointSilenceMs && speechDuration >= minSpeechMs) || reachedMaxBytes) {
            val bytes = utteranceBuffer.toByteArray()
            if (bytes.isNotEmpty()) {
                val segment = audioRecorder.writePcmToWav(bytes, "segment_${language}")
                pendingSegments.trySend(segment)
            }
            inSpeech = false
            utteranceStartMs = 0L
            lastSpeechMs = 0L
            utteranceBuffer.reset()
        }
    }

    private fun appendToUtterance(bytes: ByteArray, size: Int) {
        if (size <= 0) return
        utteranceBuffer.write(bytes, 0, size)
    }

    override fun stopListening() {
        isStreaming = false
        listeningJob?.cancel()
        listeningJob = null
        inSpeech = false
        utteranceStartMs = 0L
        lastSpeechMs = 0L
        utteranceBuffer.reset()
        audioRecorder.stopHotMic(releaseRecorder = false)
    }

    override fun release() {
        stopListening()
        transcribeJob?.cancel()
        transcribeJob = null
        pendingSegments.close()
        audioRecorder.release()
    }

    override suspend fun transcribeAudio(file: File): String = withContext(Dispatchers.IO) {
        val token = authManager.getAuthToken() ?: authManager.authenticateDevice()
        val endpoints = listOf(
            "https://fluentai-backend-production-6a57.up.railway.app/chat/transcribe",
            "https://fluentai-backend-production-6a57.up.railway.app/speech/transcribe",
            "https://fluentai-backend-production-6a57.up.railway.app/stt/transcribe"
        )

        var lastError = "Transcription endpoint not reachable"
        for (url in endpoints) {
            try {
                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("language", currentLanguage)
                    .addFormDataPart(
                        "audio",
                        file.name,
                        file.asRequestBody("audio/wav".toMediaType())
                    )
                    .build()

                val request = Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer $token")
                    .post(requestBody)
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        lastError = "Transcription failed: HTTP ${response.code}"
                        return@use
                    }
                    val payload = response.body?.string().orEmpty()
                    if (payload.isBlank()) {
                        lastError = "Transcription returned empty response"
                        return@use
                    }
                    val json = JSONObject(payload)
                    val transcript = when {
                        json.has("text") -> json.optString("text")
                        json.has("transcript") -> json.optString("transcript")
                        json.has("transcription") -> json.optString("transcription")
                        else -> ""
                    }.trim()
                    if (transcript.isNotBlank()) return@withContext transcript
                    lastError = "Transcription text missing in response"
                }
            } catch (e: Exception) {
                lastError = e.message ?: "Transcription request failed"
            }
        }
        throw IllegalStateException(lastError)
    }
}
