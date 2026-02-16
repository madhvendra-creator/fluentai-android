package com.supereva.fluentai.data.ai

import android.util.Log
import com.supereva.fluentai.BuildConfig
import com.supereva.fluentai.domain.ai.AiChunk
import com.supereva.fluentai.domain.ai.AiConversationEngine
import com.supereva.fluentai.domain.session.model.Difficulty
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString.Companion.toByteString
import java.io.File
import java.util.Base64

class OpenAiRealtimeEngine(
    private val client: OkHttpClient = OkHttpClient()
) : AiConversationEngine {

    private val scope = CoroutineScope(Dispatchers.IO)
    private var webSocket: WebSocket? = null
    private val json = Json { ignoreUnknownKeys = true }

    // SharedFlow to emit AI responses to the UI
    private val _responses = MutableSharedFlow<AiChunk>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    override suspend fun startSession(topicId: String, difficulty: Difficulty) {
        // 1. Build the Request
        val request = Request.Builder()
            .url("wss://api.openai.com/v1/realtime?model=gpt-4o-realtime-preview-2024-10-01")
            .addHeader("Authorization", "Bearer ${BuildConfig.OPENAI_API_KEY}")
            .addHeader("OpenAI-Beta", "realtime=v1")
            .build()

        // 2. Create the WebSocketListener
        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("OpenAiRealtime", "Connected to OpenAI Realtime API")
                // Optional: Send initial configuration or session update here if needed
                // For now, we just establish the connection.
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleMessage(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("OpenAiRealtime", "WebSocket Failure: ${t.message}", t)
                // You might want to emit an error state or reconnect logic here
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("OpenAiRealtime", "WebSocket Closing: $code / $reason")
            }
        }

        // 3. Open the connection
        webSocket = client.newWebSocket(request, listener)
    }

    override suspend fun sendStreamChunk(chunk: ByteArray) {
        val b64 = Base64.getEncoder().encodeToString(chunk)
        val event = InputAudioBufferAppend(audio = b64)
        val jsonString = json.encodeToString(InputAudioBufferAppend.serializer(), event)
        webSocket?.send(jsonString)
    }

    override suspend fun sendUserAudio(file: File) {
        scope.launch {
            if (file.exists()) {
                val bytes = file.readBytes()
                val b64 = Base64.getEncoder().encodeToString(bytes)
                val event = InputAudioBufferAppend(audio = b64)
                val jsonString = json.encodeToString(InputAudioBufferAppend.serializer(), event)
                webSocket?.send(jsonString)
                
                 // After sending audio, we might want to commit the buffer to trigger generation
                 // explicitly if the server doesn't auto-commit (VAD mode dependent).
                 // For this implementation, we assume server VAD or simple append triggers.
                 // If specific commit is needed:
                 // val commitEvent = mapOf("type" to "input_audio_buffer.commit")
                 // webSocket?.send(JSONObject(commitEvent).toString())
            }
        }
    }

    override fun observeAiResponses(): Flow<AiChunk> = _responses.asSharedFlow()

    override suspend fun endSession() {
        webSocket?.close(1000, "Session ended by user")
        webSocket = null
    }

    private fun handleMessage(text: String) {
        try {
            val element = json.parseToJsonElement(text)
            val jsonObject = element as? JsonObject ?: return
            val type = jsonObject["type"]?.jsonPrimitive?.contentOrNull ?: return

            when (type) {
                "response.audio.delta" -> {
                    val delta = jsonObject["delta"]?.jsonPrimitive?.contentOrNull
                    if (delta != null) {
                        try {
                            val bytes = Base64.getDecoder().decode(delta)
                            _responses.tryEmit(
                                AiChunk(
                                    textPartial = "", // Audio delta usually implies no text or separate text event
                                    audioPCM = bytes,
                                    isFinal = false
                                )
                            )
                        } catch (e: Exception) {
                            Log.e("OpenAiRealtime", "Failed to decode audio delta", e)
                        }
                    }
                }
                "response.audio.transcript.delta" -> {
                     // If we want to stream text as well
                     val transcript = jsonObject["delta"]?.jsonPrimitive?.contentOrNull
                     if (transcript != null) {
                         _responses.tryEmit(
                             AiChunk(
                                 textPartial = transcript,
                                 isFinal = false
                             )
                         )
                     }
                }
                "response.audio.done" -> {
                    // Audio stream for this turn is finished
                     _responses.tryEmit(
                         AiChunk(
                             textPartial = "",
                             isFinal = true
                         )
                     )
                }
                "response.done" -> {
                     // The entire response turn is done
                     // Handled by response.audio.done for now as per requirements
                }
                "error" -> {
                    Log.e("OpenAiRealtime", "Server Error: $text")
                }
            }
        } catch (e: Exception) {
            Log.e("OpenAiRealtime", "Error parsing message: $text", e)
        }
    }

    // Serializable Event Classes
    
    @Serializable
    data class InputAudioBufferAppend(
        val type: String = "input_audio_buffer.append",
        val audio: String
    )
}
