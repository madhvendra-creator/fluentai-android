package com.supereva.fluentai.data.repository

import com.supereva.fluentai.domain.model.PracticeResult
import com.supereva.fluentai.domain.repository.AiRepository
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
 * Real [AiRepository] that calls the OpenAI Chat Completions API
 * to analyse the user's spoken text and return a dynamic
 * [PracticeResult] with corrected text, feedback, and score.
 */
class RealAiRepository(
    private val apiKey: String
) : AiRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    override suspend fun analyzeSpeech(text: String): PracticeResult {
        return withContext(Dispatchers.IO) {

            val systemPrompt = """
                You are an English language tutor. The user will give you a sentence they spoke.
                Respond ONLY with a JSON object (no markdown, no explanation) in this exact format:
                {
                  "correctedText": "the grammatically correct version of the sentence",
                  "feedback": "a short, encouraging explanation of what was wrong and how to fix it",
                  "score": 85
                }
                Rules:
                - "score" is an integer 0-100 rating the grammar and fluency.
                - If the sentence is already perfect, set correctedText equal to the original, feedback to a compliment, and score to 95-100.
                - Keep feedback under 2 sentences.
            """.trimIndent()

            val messagesArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", text)
                })
            }

            val body = JSONObject().apply {
                put("model", "gpt-4o-mini")
                put("messages", messagesArray)
                put("temperature", 0.3)
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
            val content = json
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
                .trim()

            // Strip markdown code fences if the model wraps in ```json ... ```
            val cleanJson = content
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val resultJson = JSONObject(cleanJson)

            PracticeResult(
                transcript = text,
                correctedText = resultJson.getString("correctedText"),
                feedback = resultJson.getString("feedback"),
                score = resultJson.getInt("score")
            )
        }
    }
}
