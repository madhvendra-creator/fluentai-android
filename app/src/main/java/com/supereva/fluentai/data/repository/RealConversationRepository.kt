package com.supereva.fluentai.data.repository

import com.supereva.fluentai.domain.repository.ConversationRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * [ConversationRepository] backed by the Fastify backend SSE endpoint.
 *
 * Connects to the `/chat/reply` endpoint and streams the reply word-by-word.
 */
class RealConversationRepository(
    private val authManager: com.supereva.fluentai.domain.auth.AuthManager
) : ConversationRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val eventSourceFactory = EventSources.createFactory(client)

    override suspend fun streamReply(
        sessionId: String,
        topicId: String,
        userText: String,
        correctedText: String,
        isAutocorrectEnabled: Boolean
    ): Flow<String> = callbackFlow {
        
        val body = JSONObject().apply {
            put("sessionId", sessionId)
            put("topicId", topicId) // CRITICAL: Added this so backend knows the context
            put("message", userText)
            put("correctedText", correctedText)
            put("isAutocorrectEnabled", isAutocorrectEnabled)
        }

        val token = authManager.getAuthToken() ?: authManager.authenticateDevice()

        // Connect to local Fastify server
        val request = Request.Builder()
            .url("https://fluentai-backend-production-6a57.up.railway.app/chat/reply")
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Accept", "text/event-stream")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val eventSource = eventSourceFactory.newEventSource(request, object : EventSourceListener() {
            override fun onOpen(eventSource: EventSource, response: Response) {
                super.onOpen(eventSource, response)
                android.util.Log.d("SSE", "Connection opened")
            }

            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                super.onEvent(eventSource, id, type, data)
                try {
                    val json = JSONObject(data)
                    if (json.has("text")) {
                        val text = json.getString("text")
                        trySend(text)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("SSE", "Error parsing chunk", e)
                }
            }

            override fun onClosed(eventSource: EventSource) {
                super.onClosed(eventSource)
                android.util.Log.d("SSE", "Connection closed")
                close()
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                super.onFailure(eventSource, t, response)
                android.util.Log.e("SSE", "Connection failed", t)
                close(t)
            }
        })

        awaitClose {
            eventSource.cancel()
        }
    }
}
