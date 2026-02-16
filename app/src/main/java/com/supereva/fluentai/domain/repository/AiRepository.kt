package com.supereva.fluentai.domain.repository

import com.supereva.fluentai.domain.model.PracticeResult

interface AiRepository {
    suspend fun analyzeSpeech(text: String): PracticeResult
}
