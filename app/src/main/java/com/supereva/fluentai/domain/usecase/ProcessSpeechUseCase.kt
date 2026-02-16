package com.supereva.fluentai.domain.usecase

import com.supereva.fluentai.domain.model.PracticeResult
import com.supereva.fluentai.domain.repository.AiRepository

// We keep interfaces here so domain stays independent
import com.supereva.fluentai.domain.repository.SpeechRepository

/**
 * Analyses user speech text (already transcribed) to generate feedback.
 * Replaces ProcessRecordingUseCase for text-based flows.
 */
class ProcessSpeechUseCase(
    private val aiRepository: AiRepository
) {

    suspend operator fun invoke(transcript: String): PracticeResult {
        // Step 1 — AI analysis (direct text input)
        // Ensure transcript isn't empty before sending? 
        // For now, let AI repository handle it or assume valid input.
        val result = aiRepository.analyzeSpeech(transcript)

        return result
    }
}
