package com.supereva.fluentai.domain.repository

import kotlinx.coroutines.flow.Flow
import java.io.File

sealed class SpeechResult {
    data class Partial(val text: String) : SpeechResult()
    data class Final(val text: String) : SpeechResult()
    data class Error(val message: String) : SpeechResult()
    data class VolumeUpdate(val rmsdB: Float) : SpeechResult() // Added this
}

interface SpeechRepository {
    fun startListening(language: String = "en-US"): Flow<SpeechResult>
    fun stopListening()
    suspend fun transcribeAudio(file: File): String
}