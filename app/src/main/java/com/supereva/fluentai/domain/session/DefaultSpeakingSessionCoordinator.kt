package com.supereva.fluentai.domain.session

import com.supereva.fluentai.domain.ai.AiChunk
import com.supereva.fluentai.domain.ai.AiConversationEngine
import com.supereva.fluentai.domain.session.model.Difficulty
import com.supereva.fluentai.domain.session.model.ScoreProgress
import com.supereva.fluentai.domain.session.model.SessionTurn
import com.supereva.fluentai.domain.session.model.SpeakingSession
import com.supereva.fluentai.domain.session.model.TurnRole
import com.supereva.fluentai.domain.tts.TtsEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Thread-safe, production-ready implementation of [SpeakingSessionCoordinator].
 *
 * Every mutation:
 * 1. Uses `@Synchronized` on methods that perform read-modify-write operations.
 * 2. Produces an **immutable copy** of the session via [SpeakingSession.copy].
 * 3. Emits the new snapshot through [MutableStateFlow].
 *
 * Supports **realtime AI streaming**: call [subscribeToAiEngine] to
 * auto-collect [AiChunk]s, drive [SessionState.AiSpeaking] ↔
 * [SessionState.Listening] transitions, and grow the chat transcript
 * incrementally.
 *
 * The class has **zero Android dependencies** and can be unit-tested on the JVM.
 */
class DefaultSpeakingSessionCoordinator(
    private val ttsEngine: TtsEngine? = null,
    private val localHistoryRepository: com.supereva.fluentai.domain.repository.LocalHistoryRepository? = null
) : SpeakingSessionCoordinator {

    private val turnTakingEngine = TurnTakingEngine()

    private val _sessionState = MutableStateFlow<SessionState>(SessionState.Idle)
    override val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    private val _currentSession = MutableStateFlow<SpeakingSession?>(null)
    override val currentSession: StateFlow<SpeakingSession?> = _currentSession.asStateFlow()

    /** Active chunk-collection job; cancelled on session end or re-subscribe. */
    private var chunkCollectionJob: Job? = null

    /** Active intro-speaking job; cancelled on session end. */
    private var introJob: Job? = null

    /** Active streaming job for simulated word-by-word AI reply; cancelled on session end. */
    private var streamingJob: Job? = null

    /** Scope provided by the caller of [startSession]; used for streaming coroutines. */
    private var sessionScope: CoroutineScope? = null

    companion object {
        /** Simulated speaking duration for the AI intro message. */
        internal const val INTRO_DELAY_MS = 1_000L

        /** Delay between each word during simulated AI streaming. */
        internal const val STREAM_WORD_DELAY_MS = 60L
    }

    // ── Lifecycle ────────────────────────────────────────────────────────

    @Synchronized
    override fun startSession(topicId: String, firstQuestion: String, difficulty: Difficulty, scope: CoroutineScope) {
        val introText = firstQuestion

        // Resilient: silently no-op if a session is already active.
        if (_sessionState.value !is SessionState.Idle &&
            _sessionState.value !is SessionState.Completed
        ) return

        val introTurn = SessionTurn(
            role = TurnRole.AI,
            transcript = introText,
            correctedText = introText
        )

        val session = SpeakingSession(
            sessionId = UUID.randomUUID().toString(),
            topicId = topicId,
            difficulty = difficulty,
            messages = listOf(introTurn)
        )

        _currentSession.value = session
        _sessionState.value = SessionState.AiSpeaking
        sessionScope = scope

        // Launch the actual TTS monitoring outside the lock
        introJob = scope.launch {
            ttsEngine?.speak(introText)
            
            // 1. Wait for TTS engine to start (timeout after 2 seconds)
            var waitStart = 0
            while (ttsEngine != null && !ttsEngine.isSpeaking.value && waitStart < 2000) {
                delay(50)
                waitStart += 50
            }
            // 2. Wait for TTS to finish speaking the entire sentence
            while (ttsEngine != null && ttsEngine.isSpeaking.value) {
                delay(100)
            }
            
            _sessionState.value = SessionState.Listening
        }
    }

    @Synchronized
    override fun ensureFreshSession(topicId: String, firstQuestion: String, difficulty: Difficulty, scope: CoroutineScope) {
        val current = _sessionState.value
        if (current !is SessionState.Idle && current !is SessionState.Completed) {
            // Tear down the in-flight session first
            chunkCollectionJob?.cancel()
            chunkCollectionJob = null
            introJob?.cancel()
            introJob = null
            streamingJob?.cancel()
            streamingJob = null
            sessionScope = null
            _sessionState.value = SessionState.Idle
            _currentSession.value = null
        }
        // Now safe to start
        startSession(topicId, firstQuestion, difficulty, scope)
    }

    private val coordinatorScope = CoroutineScope(kotlinx.coroutines.Dispatchers.IO + kotlinx.coroutines.SupervisorJob())

    @Synchronized
    override fun endSession() {
        var sessionToSave: SpeakingSession? = null
        chunkCollectionJob?.cancel()
        chunkCollectionJob = null
        introJob?.cancel()
        introJob = null
        streamingJob?.cancel()
        streamingJob = null
        sessionScope = null
        ttsEngine?.stop()
        val session = _currentSession.value ?: return
        _sessionState.value = SessionState.Completed(session)
        
        // Capture session for saving outside the lock
        sessionToSave = session
         
        _currentSession.value = null

        // Save session to local database
        sessionToSave?.let { s ->
            coordinatorScope.launch {
                localHistoryRepository?.saveSession(s)
            }
        }
    }

    // ── AI Engine subscription ───────────────────────────────────────────

    override fun subscribeToAiEngine(engine: AiConversationEngine, scope: CoroutineScope) {
        chunkCollectionJob?.cancel()

        chunkCollectionJob = scope.launch {
            engine.observeAiResponses().collect { chunk ->
                if (!chunk.isFinal) {
                    transitionTo(SessionState.AiSpeaking)
                }

                // Streaming audio playback
                chunk.audioPCM?.let { pcm ->
                    ttsEngine?.playPcm(pcm)
                }

                appendOrUpdateAiTurn(chunk)
                
                if (chunk.isFinal) {
                    transitionTo(SessionState.Listening)
                }
            }
        }
    }

    // ── Turn management ─────────────────────────────────────────────────

    @Synchronized
    override fun appendUserTurn(turn: SessionTurn) {
        val session = requireActiveSession()
        val updatedMessages = session.messages + turn
        val updatedScore = recalculateScore(session.scoreProgress, turn)

        _currentSession.value = session.copy(
            messages = updatedMessages,
            scoreProgress = updatedScore,
            updatedAt = System.currentTimeMillis()
        )
    }

    @Synchronized
    override fun appendAiTurn(turn: SessionTurn) {
        val session = requireActiveSession()
        val updatedMessages = session.messages + turn

        _currentSession.value = session.copy(
            messages = updatedMessages,
            updatedAt = System.currentTimeMillis()
        )
    }

    @Synchronized
    override fun streamAiTurn(turn: SessionTurn) {
        val scope = sessionScope ?: error("No session scope. Call startSession() first.")
        val words = turn.transcript.split(" ").filter { it.isNotEmpty() }
        if (words.isEmpty()) return

        // Append the initial turn with just the first word and isStreaming = true
        val session = requireActiveSession()
        val streamingTurn = turn.copy(
            transcript = words.first(),
            correctedText = words.first(),
            isStreaming = true
        )
        _currentSession.value = session.copy(
            messages = session.messages + streamingTurn,
            updatedAt = System.currentTimeMillis()
        )
        _sessionState.value = SessionState.AiSpeaking

        // Stream remaining words word-by-word + start TTS
        streamingJob?.cancel()
        streamingJob = scope.launch {
            // Fire-and-forget TTS for the full text (non-blocking)
            ttsEngine?.speak(turn.transcript)

            for (i in 1 until words.size) {
                delay(STREAM_WORD_DELAY_MS)
                synchronized(this@DefaultSpeakingSessionCoordinator) {
                    val currentSession = _currentSession.value ?: return@synchronized
                    val messages = currentSession.messages.toMutableList()
                    val last = messages.lastOrNull() ?: return@synchronized

                    val partialText = words.subList(0, i + 1).joinToString(" ")
                    val isLast = (i == words.size - 1)

                    messages[messages.lastIndex] = last.copy(
                        transcript = partialText,
                        correctedText = partialText,
                        isStreaming = !isLast
                    )

                    _currentSession.value = currentSession.copy(
                        messages = messages,
                        updatedAt = System.currentTimeMillis()
                    )

                    if (isLast) {
                        _sessionState.value = SessionState.Listening
                    }
                }
            }

            // Handle single-word edge case
            if (words.size == 1) {
                synchronized(this@DefaultSpeakingSessionCoordinator) {
                    val currentSession = _currentSession.value ?: return@synchronized
                    val messages = currentSession.messages.toMutableList()
                    val last = messages.lastOrNull() ?: return@synchronized
                    messages[messages.lastIndex] = last.copy(isStreaming = false)
                    _currentSession.value = currentSession.copy(
                        messages = messages,
                        updatedAt = System.currentTimeMillis()
                    )
                    _sessionState.value = SessionState.Listening
                }
            }
        }
    }

    @Synchronized
    override fun appendOrUpdateAiTurn(chunk: AiChunk) {
        val session = requireActiveSession()
        val messages = session.messages.toMutableList()
        val last = messages.lastOrNull()

        if (last != null && last.role == TurnRole.AI && last.isStreaming) {
            // Update the existing streaming turn in-place
            messages[messages.lastIndex] = last.copy(
                transcript = chunk.textPartial,
                correctedText = chunk.textPartial,
                audioPath = chunk.audioPartialPath ?: last.audioPath,
                isStreaming = !chunk.isFinal
            )
        } else {
            // Start a new AI streaming turn
            messages.add(
                SessionTurn(
                    role = TurnRole.AI,
                    transcript = chunk.textPartial,
                    correctedText = chunk.textPartial,
                    audioPath = chunk.audioPartialPath,
                    isStreaming = !chunk.isFinal
                )
            )
        }

        _currentSession.value = session.copy(
            messages = messages,
            updatedAt = System.currentTimeMillis()
        )
    }

    // ── Turn-taking ─────────────────────────────────────────────────────

    override fun onUserTurnProcessed() {
        _sessionState.value = turnTakingEngine.onUserTurnProcessed()
    }

    override fun onReplyGenerationStarted() {
        _sessionState.value = turnTakingEngine.onReplyGenerationStarted()
    }

    @Synchronized
    override fun interruptAiSpeaking() {
        val next = turnTakingEngine.onUserInterrupted(_sessionState.value) ?: return
        _sessionState.value = next
        streamingJob?.cancel()
        streamingJob = null
        ttsEngine?.stop()
        // Transient signal emitted — immediately transition to Listening
        _sessionState.value = turnTakingEngine.onInterruptionHandled()
    }

    override suspend fun speakText(text: String) {
        // Fire-and-forget TTS
        ttsEngine?.speak(text)
    }

    // ── State transitions ───────────────────────────────────────────────

    override fun transitionTo(state: SessionState) {
        _sessionState.value = state
    }

    // ── Internals ───────────────────────────────────────────────────────

    private fun requireActiveSession(): SpeakingSession {
        return _currentSession.value
            ?: error("No active session. Call startSession() first.")
    }

    private fun recalculateScore(
        current: ScoreProgress,
        turn: SessionTurn
    ): ScoreProgress {
        val score = turn.score ?: return current
        return current.copy(
            totalScore = current.totalScore + score,
            turnCount = current.turnCount + 1
        )
    }
}
