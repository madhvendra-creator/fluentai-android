package com.supereva.fluentai.data.repository

import com.supereva.fluentai.domain.model.PracticeResult
import com.supereva.fluentai.domain.repository.AiRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Real [AiRepository] that calls the Fastify backend 
 * to analyse the user's spoken text and return a dynamic
 * [PracticeResult] with corrected text, feedback, and score.
 */
class RealAiRepository(
    private val authManager: com.supereva.fluentai.domain.auth.AuthManager
) : AiRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    override suspend fun analyzeSpeech(
        text: String, 
        challengeSentence: String?,
        sessionMode: String,
        topicId: String?,
        previousAiText: String?
    ): PracticeResult {
        return withContext(Dispatchers.IO) {

            val body = JSONObject().apply {
                put("message", text)
                put("sessionMode", sessionMode)
                if (challengeSentence != null) {
                    put("challengeSentence", challengeSentence)
                }
                if (topicId != null) {
                    put("topicId", topicId)
                }
                if (previousAiText != null) {
                    put("previousAiText", previousAiText)
                }
            }

            val token = authManager.getAuthToken() ?: authManager.authenticateDevice()

            val request = Request.Builder()
                .url("https://fluentai-backend-production-6a57.up.railway.app/chat/evaluate")
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Content-Type", "application/json")
                .post(body.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                throw Exception("Backend API error: ${response.code}")
            }

            val responseBody = response.body?.string()
                ?: throw Exception("Empty response from backend")

            try {
                val resultJson = JSONObject(responseBody)

                PracticeResult(
                    transcript = text,
                    correctedText = resultJson.getString("correctedText"),
                    feedback = resultJson.getString("feedback"),
                    score = resultJson.getInt("score")
                )
            } catch (e: Exception) {
                android.util.Log.e("RealAiRepository", "Error parsing AI response: ${e.message}")
                android.util.Log.e("RealAiRepository", "Raw response: $responseBody")

                PracticeResult(
                    transcript = text,
                    correctedText = text, // Assume original is okay if we can't process
                    feedback = "Good effort! I couldn't process specific feedback this time, but keep practicing.",
                    score = 80
                )
            }
        }
    }
}
