package com.supereva.fluentai.data.repository

import kotlinx.coroutines.delay
import java.io.File

import com.supereva.fluentai.domain.repository.SpeechRepository

class FakeSpeechRepository : SpeechRepository {

    override fun startListening(language: String): kotlinx.coroutines.flow.Flow<com.supereva.fluentai.domain.repository.SpeechResult> {
        return kotlinx.coroutines.flow.flow {
            delay(1000)
            emit(com.supereva.fluentai.domain.repository.SpeechResult.Final("I am practice English every day"))
        }
    }

    override fun stopListening() {
        // No-op
    }

    override suspend fun transcribeAudio(file: File): String {
        delay(1500) // simulate network call
        return "I am practice English every day"
    }
}
