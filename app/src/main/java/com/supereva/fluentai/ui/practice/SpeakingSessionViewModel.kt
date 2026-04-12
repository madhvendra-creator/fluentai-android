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
import com.supereva.fluentai.domain.session.model.TurnRole
import com.supereva.fluentai.domain.usecase.ProcessSpeechUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class SpeakingSessionViewModel(
    private val coordinator: SpeakingSessionCoordinator,
    private val aiEngine: AiConversationEngine?,
    private val processSpeechUseCase: ProcessSpeechUseCase,
    private val conversationRepository: ConversationRepository,
    private val speechRepository: com.supereva.fluentai.domain.repository.SpeechRepository,
    private val audioRecorder: com.supereva.fluentai.data.audio.AudioRecorder
) : ViewModel() {

    private val _currentVolume = kotlinx.coroutines.flow.MutableStateFlow(0f)
    private var accumulatedTranscript = java.lang.StringBuilder()
    private val _partialTranscript = kotlinx.coroutines.flow.MutableStateFlow("")
    private val _isAutocorrectEnabled = kotlinx.coroutines.flow.MutableStateFlow(false)
    private val _isMicHot = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isMicHot: kotlinx.coroutines.flow.StateFlow<Boolean> = _isMicHot

    private val _lastCorrectAnswer = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    val lastCorrectAnswer: kotlinx.coroutines.flow.StateFlow<String?> = _lastCorrectAnswer

    fun toggleAutocorrect() {
        _isAutocorrectEnabled.value = !_isAutocorrectEnabled.value
    }

    private var recordingJob: Job? = null
    private var processingJob: Job? = null

    private var currentChallengeSentence: String? = null
    private var currentSessionMode: String = "AI"
    private var currentSourceLang: String = "English"
    private var currentTargetLang: String = "Hindi"

    /** Maps language display names to BCP-47 locale codes for the speech recognizer. */
    private fun getLocaleForLanguage(language: String): String = when (language) {
        "Hindi"   -> "hi-IN"
        "Spanish" -> "es-ES"
        "French"  -> "fr-FR"
        "German"  -> "de-DE"
        else      -> "en-US"
    }

    /** Returns the recognizer locale to use: target language locale for translation, en-US otherwise. */
    private fun getRecognizerLocale(): String {
        return if (currentSessionMode == com.supereva.fluentai.domain.session.model.SessionMode.TRANSLATION_PRACTICE.name) {
            getLocaleForLanguage(currentTargetLang)
        } else {
            "en-US"
        }
    }

    // Starter sentences for translation practice — used to generate next challenges
    private val starterSentences = listOf(
        "How are you?",
        "Where are you going?",
        "I want to eat something.",
        "What time is it?",
        "Can you help me?",
        "I am going to sleep.",
        "What is your name?",
        "I like this place.",
        "Please open the door.",
        "The weather is nice today."
    )

    // Handled the 6-flow limitation by chaining combine
    val uiState: StateFlow<SessionUiState> = combine(
        coordinator.sessionState,
        coordinator.currentSession,
        _currentVolume,
        _partialTranscript,
        _isAutocorrectEnabled
    ) { state, session, volume, partial, autocorrect ->
        SessionUiState(
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
            error = (state as? SessionState.Error)?.message,
            partialTranscript = partial,
            isAutocorrectEnabled = autocorrect,
            isMicHot = false // Now tracked via independent StateFlow to prevent 6-flow limitations
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SessionUiState())

    init {
        aiEngine?.let { engine ->
            coordinator.subscribeToAiEngine(engine, viewModelScope)
            viewModelScope.launch {
                val session = coordinator.currentSession.value
                engine.startSession(
                    topicId = session?.topicId ?: "free_talk",
                    difficulty = session?.difficulty ?: Difficulty.BEGINNER
                )
            }
        }
    }

    fun startSession(
        topicId: String = "free_talk", 
        firstQuestion: String = "Hi! Let's talk about anything you want today.", 
        difficultyStr: String = "BEGINNER",
        sessionModeStr: String = "AI",
        sourceLang: String = "English",
        targetLang: String = "Hindi"
    ) {
        currentSessionMode = sessionModeStr
        currentSourceLang = sourceLang
        currentTargetLang = targetLang
        val difficulty = try { Difficulty.valueOf(difficultyStr) } catch (e: Exception) { Difficulty.BEGINNER }

        if (sessionModeStr == com.supereva.fluentai.domain.session.model.SessionMode.TRANSLATION_PRACTICE.name) {
            // Extract challenge from "Please translate: 'How are you?'" format
            val extractedChallenge = "'(.*?)'".toRegex().find(firstQuestion)?.groupValues?.get(1)
                ?: firstQuestion.removePrefix("Please translate: ").trim()
            currentChallengeSentence = extractedChallenge
            // Use English locale for the challenge prompt TTS
            SessionServiceLocator.ttsEngine?.setLanguage(java.util.Locale.US)
            coordinator.startSession(topicId, firstQuestion, difficulty, viewModelScope)
        } else {
            SessionServiceLocator.ttsEngine?.setLanguage(java.util.Locale.US)
            coordinator.startSession(topicId, firstQuestion, difficulty, viewModelScope)
        }
    }

    fun onRecordingStarted() {
        accumulatedTranscript.clear()
        _partialTranscript.value = ""
        _isMicHot.value = true // 🟢 TURN ON MIC UI (GREEN)
        coordinator.transitionTo(SessionState.Listening)

        recordingJob?.cancel()

        if (aiEngine != null) {
            try {
                audioRecorder.startRecording()
                recordingJob = viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        val bufferSize = audioRecorder.minBufferSize
                        val buffer = ByteArray(bufferSize)
                        while (isActive && _isMicHot.value) {
                            val readBytes = audioRecorder.read(buffer, bufferSize)
                            if (readBytes > 0) {
                                val chunk = if (readBytes == bufferSize) buffer else buffer.copyOfRange(0, readBytes)
                                aiEngine.sendStreamChunk(chunk)
                                _currentVolume.value = (Math.random() * 10).toFloat()
                            } else break
                        }
                    } finally {
                        audioRecorder.stopRecording()
                        _isMicHot.value = false
                    }
                }
            } catch (e: Exception) { e.printStackTrace(); _isMicHot.value = false }
        } else {
            // Cast to NativeSpeechRepository to access restartListening()
            val nativeRepo = speechRepository as? com.supereva.fluentai.data.repository.NativeSpeechRepository

            recordingJob = viewModelScope.launch {
                // Collect from the shared flow. startListening() fires the first
                // recognition; after Final/Error we call restartListening() to
                // re-trigger recognition on the SAME SpeechRecognizer instance.
                speechRepository.startListening(getRecognizerLocale()).collect { result ->
                    if (!_isMicHot.value) return@collect

                    when (result) {
                        is com.supereva.fluentai.domain.repository.SpeechResult.Partial -> {
                            val currentAcc = accumulatedTranscript.toString().trim()
                            _partialTranscript.value = if (currentAcc.isNotEmpty()) "$currentAcc ${result.text}" else result.text
                        }
                        is com.supereva.fluentai.domain.repository.SpeechResult.VolumeUpdate -> {
                            _currentVolume.value = result.rmsdB
                        }
                        is com.supereva.fluentai.domain.repository.SpeechResult.Final -> {
                            if (result.text.isNotBlank()) {
                                if (accumulatedTranscript.isNotEmpty()) accumulatedTranscript.append(" ")
                                accumulatedTranscript.append(result.text.trim())
                                _partialTranscript.value = accumulatedTranscript.toString()
                            }
                            // Breathing delay then seamlessly restart recognition
                            if (_isMicHot.value) {
                                delay(150)
                                nativeRepo?.restartListening(getRecognizerLocale())
                            }
                        }
                        is com.supereva.fluentai.domain.repository.SpeechResult.Error -> {
                            // Breathing delay then seamlessly restart recognition
                            if (_isMicHot.value) {
                                delay(150)
                                nativeRepo?.restartListening(getRecognizerLocale())
                            }
                        }
                    }
                }
            }
        }
    }

    fun onRecordingStopped() {
        _isMicHot.value = false // 🟢 TURN OFF MIC UI (ORANGE)
        recordingJob?.cancel()
        recordingJob = null

        if (aiEngine != null) {
            coordinator.transitionTo(SessionState.AiThinking)
            viewModelScope.launch { aiEngine.commitUserTurn() }
        } else {
            speechRepository.stopListening()
            val finalText = _partialTranscript.value.trim()
            if (finalText.isNotBlank()) {
                onSpeechFinalized(finalText)
            } else {
                coordinator.transitionTo(SessionState.Listening)
            }
        }
        _currentVolume.value = 0f
        _partialTranscript.value = ""
    }

    /**
     * Soft reset: clears the current transcript but keeps the mic active (Green).
     * Forces a new recognition window so deleted words don't reappear.
     */
    fun resetActiveRecording() {
        accumulatedTranscript.clear()
        _partialTranscript.value = ""
        // isMicHot stays TRUE — mic stays Green

        // Force a fresh recognition window to discard any buffered partials
        val nativeRepo = speechRepository as? com.supereva.fluentai.data.repository.NativeSpeechRepository
        nativeRepo?.restartListening(getRecognizerLocale())

        if (currentSessionMode == com.supereva.fluentai.domain.session.model.SessionMode.TRANSLATION_PRACTICE.name && currentChallengeSentence != null) {
            viewModelScope.launch {
                SessionServiceLocator.ttsEngine?.setLanguage(java.util.Locale.US)
                coordinator.speakText(currentChallengeSentence!!)
            }
        }
    }

    fun cancelRecording() {
        _isMicHot.value = false // 🟢 TURN OFF MIC UI (ORANGE)
        recordingJob?.cancel()
        recordingJob = null

        if (aiEngine != null) {
            audioRecorder.stopRecording()
        } else {
            speechRepository.stopListening()
        }

        _partialTranscript.value = ""
        _currentVolume.value = 0f
        coordinator.transitionTo(SessionState.Listening)
    }

    fun requestNextQuestion() {
        _lastCorrectAnswer.value = null
        accumulatedTranscript.clear()
        _partialTranscript.value = ""

        val sentencesForLang = when (currentSourceLang) {
            "Hindi" -> listOf(
                "मैं बाज़ार जा रहा हूँ।",
                "आपका नाम क्या है?",
                "क्या आप मेरी मदद कर सकते हैं?",
                "मुझे भूख लगी है।",
                "यह बहुत अच्छा है।"
            )
            "Spanish" -> listOf(
                "¿Cómo estás?", "Me llamo Juan.",
                "¿Dónde está el baño?", "Tengo hambre.", "Hace buen tiempo."
            )
            "French" -> listOf(
                "Comment allez-vous?", "Je m'appelle Marie.",
                "Où est la gare?", "J'ai faim.", "Il fait beau aujourd'hui."
            )
            "German" -> listOf(
                "Wie geht es Ihnen?", "Ich heiße Thomas.",
                "Wo ist der Bahnhof?", "Ich habe Hunger.", "Das Wetter ist schön."
            )
            else -> starterSentences  // English
        }
        val randomChallenge = sentencesForLang.random()
        currentChallengeSentence = randomChallenge
        val challengeText = "Please translate: '$randomChallenge'"

        viewModelScope.launch {
            coordinator.appendAiTurn(
                com.supereva.fluentai.domain.session.model.SessionTurn(
                    com.supereva.fluentai.domain.session.model.TurnRole.AI,
                    challengeText, challengeText, "", null
                )
            )
            SessionServiceLocator.ttsEngine?.setLanguage(java.util.Locale.US)
            coordinator.transitionTo(com.supereva.fluentai.domain.session.SessionState.AiSpeaking)
            coordinator.speakText(challengeText)
            waitForTtsToFinish()
            coordinator.transitionTo(com.supereva.fluentai.domain.session.SessionState.Listening)
        }
    }


    private fun onSpeechFinalized(transcript: String) {
        coordinator.transitionTo(SessionState.Processing)
        _partialTranscript.value = ""

        processingJob?.cancel()
        processingJob = viewModelScope.launch {
            try {
                val topicId = coordinator.currentSession.value?.topicId ?: "free_talk"
                val sessionId = coordinator.currentSession.value?.sessionId ?: "session-123"
                val previousAiText = coordinator.currentSession.value?.messages
                    ?.lastOrNull { it.role == TurnRole.AI }?.transcript

                val result = processSpeechUseCase(
                    transcript = transcript, 
                    challengeSentence = currentChallengeSentence, 
                    sessionMode = currentSessionMode,
                    topicId = topicId,
                    previousAiText = previousAiText
                )

                // Store the correct answer for UI display
                _lastCorrectAnswer.value = result.correctedText

                coordinator.appendUserTurn(SessionTurn(TurnRole.USER, result.transcript, result.correctedText, score = result.score))

                // ── Translation Practice: correction feedback + next challenge ──
                if (currentSessionMode == com.supereva.fluentai.domain.session.model.SessionMode.TRANSLATION_PRACTICE.name) {
                    val isIncorrect = result.correctedText != transcript && result.score < 80

                    // Build feedback text
                    val feedbackText = if (isIncorrect) {
                        "That's not quite right. You should say: '${result.correctedText}'"
                    } else {
                        "Great job! That's correct."
                    }

                    // Append feedback as AI turn
                    coordinator.appendAiTurn(SessionTurn(TurnRole.AI, feedbackText, feedbackText, result.feedback, result.score))
                    coordinator.onUserTurnProcessed()

                    // Speak the feedback in English
                    SessionServiceLocator.ttsEngine?.setLanguage(java.util.Locale.US)
                    coordinator.transitionTo(SessionState.AiSpeaking)
                    coordinator.speakText(feedbackText)
                    waitForTtsToFinish()

                    // STOP HERE — keep feedback + correct answer visible.
                    // The user will tap "Next →" to load the next challenge.
                    coordinator.transitionTo(SessionState.Listening)

                } else {
                    // ── Standard flow (free_talk, interview, etc.) ──
                    coordinator.appendAiTurn(SessionTurn(TurnRole.AI, result.correctedText, result.correctedText, result.feedback, result.score))
                    coordinator.onUserTurnProcessed()
                    coordinator.onReplyGenerationStarted()

                    val replyFlow = conversationRepository.streamReply(sessionId, topicId, transcript, result.correctedText, _isAutocorrectEnabled.value)
                    var accumulatedText = ""

                    replyFlow.collect { chunkText ->
                        accumulatedText += chunkText 
                        coordinator.appendOrUpdateAiTurn(
                            com.supereva.fluentai.domain.ai.AiChunk(accumulatedText, isFinal = false)
                        )
                    }

                    coordinator.appendOrUpdateAiTurn(com.supereva.fluentai.domain.ai.AiChunk(accumulatedText, isFinal = true))
                    
                    SessionServiceLocator.ttsEngine?.setLanguage(java.util.Locale.US)
                    coordinator.transitionTo(SessionState.AiSpeaking)
                    coordinator.speakText(accumulatedText)
                    waitForTtsToFinish()

                    coordinator.transitionTo(SessionState.Listening)
                }

            } catch (e: Exception) {
                coordinator.transitionTo(SessionState.Error(e.message ?: "Unknown error", e))
            }
        }
    }

    /** Waits for TTS playback to complete, with a 15-second safety timeout. */
    private suspend fun waitForTtsToFinish() {
        delay(500) // Give TTS time to start
        val tts = SessionServiceLocator.ttsEngine
        if (tts != null) {
            var timeoutCounter = 150 // 150 * 100ms = 15 seconds max
            while (tts.isSpeaking.value && timeoutCounter > 0) {
                delay(100)
                timeoutCounter--
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechRepository.release()
    }

    fun onMicTappedDuringAiSpeaking() = coordinator.interruptAiSpeaking()
    fun speakWord(word: String) = viewModelScope.launch { coordinator.speakText(word) }
    fun endSession() { viewModelScope.launch { aiEngine?.endSession() }; coordinator.endSession() }
    fun onScreenLeft() { viewModelScope.launch { aiEngine?.endSession() }; coordinator.endSession() }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SpeakingSessionViewModel(
                    coordinator = SessionServiceLocator.coordinator,
                    aiEngine = SessionServiceLocator.aiEngine,
                    processSpeechUseCase = SessionServiceLocator.processSpeechUseCase,
                    conversationRepository = SessionServiceLocator.conversationRepository,
                    speechRepository = SessionServiceLocator.speechRepository,
                    audioRecorder = SessionServiceLocator.audioRecorder
                ) as T
            }
        }
    }
}
