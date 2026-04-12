package com.supereva.fluentai.data.auth

import android.content.Context
import android.content.SharedPreferences
import com.supereva.fluentai.domain.auth.AuthManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class RealAuthManager(private val context: Context) : AuthManager {
    private val client = OkHttpClient()
    private val prefs: SharedPreferences = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    private val deviceId: String by lazy {
        prefs.getString("device_id", null) ?: run {
            val newId = java.util.UUID.randomUUID().toString()
            prefs.edit().putString("device_id", newId).apply()
            newId
        }
    }

    override suspend fun authenticateDevice(): String = withContext(Dispatchers.IO) {
        val existingToken = getAuthToken()
        if (existingToken != null) return@withContext existingToken

        val json = JSONObject().apply {
            put("userId", deviceId)
        }
        val requestBody = json.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("https://fluentai-backend-production-6a57.up.railway.app/api/auth/login")
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("Failed to authenticate")
            val responseBody = response.body?.string() ?: throw Exception("Empty response body")
            val responseJson = JSONObject(responseBody)
            
            // Extract the token (assuming the key is "token")
            val token = responseJson.getString("token")
            prefs.edit().putString("jwt_token", token).apply()
            token
        }
    }

    override fun getAuthToken(): String? {
        return prefs.getString("jwt_token", null)
    }
}
