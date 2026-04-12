package com.supereva.fluentai.data.repository

import com.supereva.fluentai.domain.model.PracticeResult
import com.supereva.fluentai.domain.repository.AiRepository
import kotlinx.coroutines.delay

class FakeAiRepository : AiRepository {

    override suspend fun analyzeSpeech(
        text: String,
        challengeSentence: String?,
        sessionMode: String,
        topicId: String?,
        previousAiText: String?
    ): PracticeResult {
        delay(1500) // simulate AI processing

        return PracticeResult(
            transcript = text,
            correctedText = "I practice English every day",
            feedback = "Use present simple: 'I practice' instead of 'I am practice'.",
            score = 82
        )
    }
}
