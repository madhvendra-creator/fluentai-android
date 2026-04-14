package com.supereva.fluentai.domain.repository

import com.supereva.fluentai.domain.model.PracticeResult

interface AiRepository {
    suspend fun analyzeSpeech(
        text: String,
        challengeSentence: String? = null,
        sessionMode: String = "AI",
        topicId: String? = null,
        previousAiText: String? = null,
        sourceLang: String = "English",
        targetLanguage: String = "Hindi"
    ): PracticeResult
}
