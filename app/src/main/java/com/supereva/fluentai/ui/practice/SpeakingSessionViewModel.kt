package com.supereva.fluentai.ui.practice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.supereva.fluentai.di.SessionServiceLocator
import com.supereva.fluentai.domain.ai.AiConversationEngine
import com.supereva.fluentai.domain.repository.ConversationRepository
import com.supereva.fluentai.domain.session.SessionState
import com.supereva.fluentai.domain.session.SpeakingSessionCoordinator
import com.supereva.fluentai.domain.session.model.Difficulty
import com.supereva.fluentai.domain.session.model.SessionTurn
import com.supereva.fluentai.domain.session.model.SpeakingSession
import com.supereva.fluentai.domain.session.model.TurnRole
import com.supereva.fluentai.domain.usecase.ProcessSpeechUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

/**
 * Thin orchestrator that bridges the domain [SpeakingSessionCoordinator]
 * with the Compose UI layer.
 *
 * **Dual-path audio processing:**
 * - When [aiEngine] is non-null → streaming path.
 * - When [aiEngine] is null → classic fallback:
 *   1. [ProcessRecordingUseCase] transcribes + analyses.
 *   2. Correction turn appended.
 *   3. [ConversationRepository] generates a follow-up reply.
 *   4. AI conversation turn appended.
 */
class SpeakingSessionViewModel(
    private val coordinator: SpeakingSessionCoordinator,
    private val aiEngine: AiConversationEngine?,
    private val processSpeechUseCase: ProcessSpeechUseCase,
    private val conversationRepository: ConversationRepository,
    private val speechRepository: com.supereva.fluentai.domain.repository.SpeechRepository
) : ViewModel() {

    // ── UI State ────────────────────────────────────────────────────────

    private val _currentVolume = kotlinx.coroutines.flow.MutableStateFlow(0f)

    val uiState: StateFlow<SessionUiState> =
        combine(
            coordinator.sessionState,
            coordinator.currentSession,
            _currentVolume
        ) { state, session, volume ->
            mapToUiState(state, session, volume)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SessionUiState()
        )

    // ── Public actions (called by PracticeScreen) ───────────────────────

    fun startSession(
        topicId: String = "free_talk",
        difficulty: Difficulty = Difficulty.BEGINNER
    ) {
        coordinator.startSession(topicId, difficulty, viewModelScope)

        // Streaming path: subscribe to AI engine if available
        aiEngine?.let { engine ->
            coordinator.subscribeToAiEngine(engine, viewModelScope)
            viewModelScope.launch {
                engine.startSession(topicId, difficulty)
            }
        }
    }

    fun onRecordingStarted() {
        coordinator.transitionTo(SessionState.Listening)
        
        viewModelScope.launch {
            speechRepository.startListening()
                .collect { result ->
                    when (result) {
                        is com.supereva.fluentai.domain.repository.SpeechResult.Partial -> {
                            // Optional: Could update UI with partial text here
                            android.util.Log.d("Speech", "Partial: ${result.text}")
                        }
                        is com.supereva.fluentai.domain.repository.SpeechResult.VolumeUpdate -> {
                            _currentVolume.value = result.rmsdB
                        }
                        is com.supereva.fluentai.domain.repository.SpeechResult.Final -> {
                            onSpeechFinalized(result.text)
                        }
                        is com.supereva.fluentai.domain.repository.SpeechResult.Error -> {
                            coordinator.transitionTo(
                                SessionState.Error(result.message)
                            )
                        }
                    }
                }
        }
    }

    fun onRecordingStopped() {
        // Native speech repository handles stop internally or via stopListening
        speechRepository.stopListening()
        _currentVolume.value = 0f
    }

    private fun onSpeechFinalized(transcript: String) {
        coordinator.transitionTo(SessionState.Processing)
        viewModelScope.launch {
            try {
                if (aiEngine != null) {
                    // ── Streaming path ──────────────────────────────────
                    // Native streaming not yet fully bridged to AiEngine in this step
                    // but assuming we'd send text or audio buffer. 
                    // For now, let's treat it same as classic or log warning.
                    android.util.Log.w("Session", "AiEngine streaming with native speech not yet implemented")
                } else {
                    // ── Classic fallback path ───────────────────────────

                    // Step 1: Analyse text directly
                    val result = processSpeechUseCase(transcript)

                    android.util.Log.d("AI_RESULT", "Corrected = ${result.correctedText}")
                    android.util.Log.d("AI_RESULT", "Feedback = ${result.feedback}")
                    android.util.Log.d("AI_RESULT", "Score = ${result.score}")

                    // Step 2: Append user turn (transcript + correction)
                    coordinator.appendUserTurn(
                        SessionTurn(
                            role = TurnRole.USER,
                            transcript = result.transcript,
                            correctedText = result.correctedText,
                            score = result.score
                        )
                    )

                    // Step 3: Append correction turn (feedback + score)
                    coordinator.appendAiTurn(
                        SessionTurn(
                            role = TurnRole.AI,
                            transcript = result.correctedText,
                            correctedText = result.correctedText,
                            feedback = result.feedback,
                            score = result.score
                        )
                    )

                    // Step 3.5: Signal user turn fully processed → AiThinking
                    coordinator.onUserTurnProcessed()

                    // Step 4: Generate conversational AI reply
                    val topicId = coordinator.currentSession.value?.topicId ?: "free_talk"
                    val history = coordinator.currentSession.value?.messages.orEmpty()

                    // Step 4: Signal reply generation starting → AiSpeaking
                    coordinator.onReplyGenerationStarted()

                    val reply = conversationRepository.generateReply(
                        topicId = topicId,
                        userText = result.transcript,
                        correctedText = result.correctedText,
                        sessionHistory = history
                    )

                    android.util.Log.d("AI_RESULT", "Reply = ${reply.aiText}")

                    // Step 5: Stream AI conversation turn word-by-word
                    coordinator.streamAiTurn(
                        SessionTurn(
                            role = TurnRole.AI,
                            transcript = reply.aiText,
                            correctedText = reply.aiText
                        )
                    )
                }
            } catch (e: Exception) {
                coordinator.transitionTo(
                    SessionState.Error(
                        message = e.message ?: "Unknown error",
                        cause = e
                    )
                )
            }
        }
    }

    fun onMicTappedDuringAiSpeaking() {
        coordinator.interruptAiSpeaking()
    }

    fun speakWord(word: String) {
        viewModelScope.launch {
            coordinator.speakText(word)
        }
    }

    fun endSession() {
        viewModelScope.launch {
            aiEngine?.endSession()
        }
        coordinator.endSession()
    }

    fun onScreenLeft() {
        viewModelScope.launch {
            aiEngine?.endSession()
        }
        coordinator.endSession()
    }

    // ── State mapping (pure function) ───────────────────────────────────

    private fun mapToUiState(
        state: SessionState,
        session: SpeakingSession?,
        volume: Float
    ): SessionUiState {
        return SessionUiState(
            topicId = session?.topicId.orEmpty(),
            isSessionActive = session != null,
            isListening = state is SessionState.Listening,
            isProcessing = state is SessionState.Processing,
            isAiThinking = state is SessionState.AiThinking,
            isAiSpeaking = state is SessionState.AiSpeaking,
            isUserInterrupting = state is SessionState.UserInterrupting,
            isCompleted = state is SessionState.Completed,
            messages = session?.messages.orEmpty(),
            averageScore = session?.scoreProgress?.averageScore ?: 0.0,
            turnCount = session?.scoreProgress?.turnCount ?: 0,
            currentVolume = volume,
            error = (state as? SessionState.Error)?.message
        )
    }

    // ── Factory ─────────────────────────────────────────────────────────

    companion object {
        val Factory: ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SpeakingSessionViewModel(
                        coordinator = SessionServiceLocator.coordinator,
                        aiEngine = SessionServiceLocator.aiEngine,
                        processSpeechUseCase = SessionServiceLocator.processSpeechUseCase,
                        conversationRepository = SessionServiceLocator.conversationRepository,
                        speechRepository = SessionServiceLocator.speechRepository
                    ) as T
                }
            }
    }
}
