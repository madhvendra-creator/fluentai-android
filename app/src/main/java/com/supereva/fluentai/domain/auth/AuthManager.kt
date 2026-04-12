package com.supereva.fluentai.domain.auth

interface AuthManager {
    suspend fun authenticateDevice(): String
    fun getAuthToken(): String?
}
