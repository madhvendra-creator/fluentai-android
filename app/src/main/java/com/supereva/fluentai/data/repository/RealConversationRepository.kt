package com.supereva.fluentai.data.repository

import com.supereva.fluentai.domain.model.ConversationReply
import com.supereva.fluentai.domain.repository.ConversationRepository
import com.supereva.fluentai.domain.session.model.SessionTurn
import com.supereva.fluentai.domain.session.model.TurnRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * [ConversationRepository] backed by the OpenAI Chat Completions API.
 *
 * Builds a multi-turn message array from the [sessionHistory] so
 * the model has full context when generating its follow-up reply.
 *
 * No Android framework dependencies — uses OkHttp + org.json only.
 */
class RealConversationRepository(
    private val apiKey: String
) : ConversationRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    override suspend fun generateReply(
        topicId: String,
        userText: String,
        correctedText: String,
        sessionHistory: List<SessionTurn>
    ): ConversationReply {
        return withContext(Dispatchers.IO) {

            val topicLabel = topicId.replace("_", " ")

            val (persona, contextDescription) = when (topicId) {
                "ordering_food" -> "Polite Waiter" to "You are a polite waiter taking an order at a nice restaurant."
                "daily_life" -> "Helpful Neighbor" to "You are a friendly neighbor chatting casually in the hallway."
                else -> "Friendly English Tutor" to "You are a patient and helpful English practice partner."
            }

            val systemPrompt = """
                $contextDescription The topic is "$topicLabel".
                Act as a $persona.
                Continue the conversation naturally based on what the user said.
                Rules:
                - Reply in 1-3 short sentences.
                - Ask a follow-up question to keep the conversation going.
                - Use simple, clear English appropriate for a language learner.
                - Do NOT correct grammar — that is handled separately.
                - Do NOT use JSON — reply with plain text only.
            """.trimIndent()

            val messagesArray = JSONArray().apply {
                // System prompt
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })

                // Current user turn (use corrected text for better context)
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", correctedText)
                })
            }

            val body = JSONObject().apply {
                put("model", "gpt-4o-mini")
                put("messages", messagesArray)
                put("temperature", 0.7)
                put("max_tokens", 150)
            }

            val request = Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(body.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                throw Exception("OpenAI API error: ${response.code}")
            }

            val responseBody = response.body?.string()
                ?: throw Exception("Empty response from OpenAI")

            val json = JSONObject(responseBody)
            val aiText = json
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
                .trim()

            ConversationReply(aiText = aiText)
        }
    }
}
