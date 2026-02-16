package com.supereva.fluentai.domain.session

import com.supereva.fluentai.domain.session.model.Difficulty
import com.supereva.fluentai.domain.session.model.SessionTurn
import com.supereva.fluentai.domain.session.model.TurnRole
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultSpeakingSessionCoordinatorTest {

    private lateinit var coordinator: DefaultSpeakingSessionCoordinator
    private lateinit var testScope: TestScope

    @Before
    fun setUp() {
        coordinator = DefaultSpeakingSessionCoordinator()
        testScope = TestScope()
    }

    // ── startSession ────────────────────────────────────────────────────

    @Test
    fun `startSession creates session with intro message and transitions to AiSpeaking then Listening`() {
        coordinator.startSession("greetings", Difficulty.BEGINNER, testScope)

        val session = coordinator.currentSession.value
        assertNotNull(session)
        assertEquals("greetings", session!!.topicId)
        assertEquals(Difficulty.BEGINNER, session.difficulty)

        // Intro AI turn is present
        assertEquals(1, session.messages.size)
        assertEquals(TurnRole.AI, session.messages[0].role)
        assertTrue(session.messages[0].transcript.contains("greetings"))

        // State is AiSpeaking immediately
        assertTrue(coordinator.sessionState.value is SessionState.AiSpeaking)

        // After delay, transitions to Listening
        testScope.advanceTimeBy(DefaultSpeakingSessionCoordinator.INTRO_DELAY_MS + 1)
        assertTrue(coordinator.sessionState.value is SessionState.Listening)
    }

    @Test
    fun `startSession is no-op when session already active`() {
        coordinator.startSession("topic1", Difficulty.BEGINNER, testScope)
        testScope.advanceTimeBy(DefaultSpeakingSessionCoordinator.INTRO_DELAY_MS + 1)

        coordinator.startSession("topic2", Difficulty.ADVANCED, testScope) // should be a no-op

        // Original session remains unchanged
        val session = coordinator.currentSession.value!!
        assertEquals("topic1", session.topicId)
        assertEquals(Difficulty.BEGINNER, session.difficulty)
    }

    // ── appendUserTurn ──────────────────────────────────────────────────

    @Test
    fun `appendUserTurn adds turn and updates score`() {
        coordinator.startSession("greetings", Difficulty.INTERMEDIATE, testScope)
        testScope.advanceTimeBy(DefaultSpeakingSessionCoordinator.INTRO_DELAY_MS + 1)

        val turn = SessionTurn(
            role = TurnRole.USER,
            transcript = "Hello, how are you?",
            correctedText = "Hello, how are you?",
            feedback = "Great pronunciation!",
            score = 85
        )
        coordinator.appendUserTurn(turn)

        val session = coordinator.currentSession.value!!
        // 1 intro + 1 user turn
        assertEquals(2, session.messages.size)
        assertEquals(85, session.scoreProgress.totalScore)
        assertEquals(1, session.scoreProgress.turnCount)
        assertEquals(85.0, session.scoreProgress.averageScore, 0.01)
    }

    @Test
    fun `appendUserTurn without score does not affect scoreProgress`() {
        coordinator.startSession("greetings", Difficulty.BEGINNER, testScope)
        testScope.advanceTimeBy(DefaultSpeakingSessionCoordinator.INTRO_DELAY_MS + 1)

        val turn = SessionTurn(
            role = TurnRole.USER,
            transcript = "Hi there",
            score = null
        )
        coordinator.appendUserTurn(turn)

        val session = coordinator.currentSession.value!!
        assertEquals(2, session.messages.size) // intro + user
        assertEquals(0, session.scoreProgress.turnCount)
    }

    @Test(expected = IllegalStateException::class)
    fun `appendUserTurn throws when no active session`() {
        coordinator.appendUserTurn(
            SessionTurn(role = TurnRole.USER, transcript = "hello")
        )
    }

    // ── appendAiTurn ────────────────────────────────────────────────────

    @Test
    fun `appendAiTurn adds turn without affecting score`() {
        coordinator.startSession("travel", Difficulty.ADVANCED, testScope)
        testScope.advanceTimeBy(DefaultSpeakingSessionCoordinator.INTRO_DELAY_MS + 1)

        coordinator.appendAiTurn(
            SessionTurn(
                role = TurnRole.AI,
                transcript = "Where would you like to travel?"
            )
        )

        val session = coordinator.currentSession.value!!
        // 1 intro + 1 appended AI turn
        assertEquals(2, session.messages.size)
        assertEquals(TurnRole.AI, session.messages[1].role)
        assertEquals(0, session.scoreProgress.turnCount)
    }

    // ── endSession ──────────────────────────────────────────────────────

    @Test
    fun `endSession transitions to Completed and clears current session`() {
        coordinator.startSession("food", Difficulty.BEGINNER, testScope)
        testScope.advanceTimeBy(DefaultSpeakingSessionCoordinator.INTRO_DELAY_MS + 1)

        coordinator.appendUserTurn(
            SessionTurn(role = TurnRole.USER, transcript = "I like pizza", score = 90)
        )
        coordinator.endSession()

        val state = coordinator.sessionState.value
        assertTrue(state is SessionState.Completed)
        val completedSession = (state as SessionState.Completed).session
        assertEquals(2, completedSession.messages.size) // intro + user

        // currentSession is cleared after ending
        assertNull(coordinator.currentSession.value)
    }

    @Test
    fun `endSession is no-op when no active session`() {
        // Should not throw — no-op when no session
        coordinator.endSession()

        // State remains Idle
        assertTrue(coordinator.sessionState.value is SessionState.Idle)
    }

    // ── transitionTo ────────────────────────────────────────────────────

    @Test
    fun `transitionTo changes state correctly`() {
        coordinator.startSession("weather", Difficulty.INTERMEDIATE, testScope)
        testScope.advanceTimeBy(DefaultSpeakingSessionCoordinator.INTRO_DELAY_MS + 1)

        coordinator.transitionTo(SessionState.Processing)
        assertTrue(coordinator.sessionState.value is SessionState.Processing)

        coordinator.transitionTo(SessionState.AiSpeaking)
        assertTrue(coordinator.sessionState.value is SessionState.AiSpeaking)

        coordinator.transitionTo(SessionState.Listening)
        assertTrue(coordinator.sessionState.value is SessionState.Listening)
    }

    // ── Multi-turn conversation ─────────────────────────────────────────

    @Test
    fun `full conversation flow produces correct score`() {
        coordinator.startSession("daily_routine", Difficulty.INTERMEDIATE, testScope)
        testScope.advanceTimeBy(DefaultSpeakingSessionCoordinator.INTRO_DELAY_MS + 1)

        coordinator.appendUserTurn(
            SessionTurn(role = TurnRole.USER, transcript = "I wake up at 7", score = 80)
        )
        coordinator.appendAiTurn(
            SessionTurn(role = TurnRole.AI, transcript = "What do you do next?")
        )
        coordinator.appendUserTurn(
            SessionTurn(role = TurnRole.USER, transcript = "I eat breakfast", score = 90)
        )

        val session = coordinator.currentSession.value!!
        assertEquals(4, session.messages.size) // intro + 2 user + 1 AI
        assertEquals(2, session.scoreProgress.turnCount)
        assertEquals(170, session.scoreProgress.totalScore)
        assertEquals(85.0, session.scoreProgress.averageScore, 0.01)
    }

    // ── Session can be restarted after ending ───────────────────────────

    @Test
    fun `can start new session after previous one ends`() {
        coordinator.startSession("topic1", Difficulty.BEGINNER, testScope)
        testScope.advanceTimeBy(DefaultSpeakingSessionCoordinator.INTRO_DELAY_MS + 1)
        coordinator.endSession()

        // Reset to Idle so a new session can start
        coordinator.transitionTo(SessionState.Idle)
        coordinator.startSession("topic2", Difficulty.ADVANCED, testScope)
        testScope.advanceTimeBy(DefaultSpeakingSessionCoordinator.INTRO_DELAY_MS + 1)

        val session = coordinator.currentSession.value!!
        assertEquals("topic2", session.topicId)
        assertEquals(Difficulty.ADVANCED, session.difficulty)
    }

    // ── Turn-taking ──────────────────────────────────────────────────────

    @Test
    fun `onUserTurnProcessed transitions to AiThinking`() {
        coordinator.startSession("greetings", Difficulty.BEGINNER, testScope)
        testScope.advanceTimeBy(DefaultSpeakingSessionCoordinator.INTRO_DELAY_MS + 1)

        coordinator.onUserTurnProcessed()

        assertTrue(coordinator.sessionState.value is SessionState.AiThinking)
    }

    @Test
    fun `onReplyGenerationStarted transitions to AiSpeaking`() {
        coordinator.startSession("greetings", Difficulty.BEGINNER, testScope)
        testScope.advanceTimeBy(DefaultSpeakingSessionCoordinator.INTRO_DELAY_MS + 1)

        coordinator.onReplyGenerationStarted()

        assertTrue(coordinator.sessionState.value is SessionState.AiSpeaking)
    }

    @Test
    fun `interruptAiSpeaking during AiSpeaking transitions to Listening`() {
        coordinator.startSession("greetings", Difficulty.BEGINNER, testScope)
        testScope.advanceTimeBy(DefaultSpeakingSessionCoordinator.INTRO_DELAY_MS + 1)
        coordinator.transitionTo(SessionState.AiSpeaking)

        coordinator.interruptAiSpeaking()

        // Transient UserInterrupting emitted then immediately replaced by Listening
        assertTrue(coordinator.sessionState.value is SessionState.Listening)
    }

    @Test
    fun `interruptAiSpeaking when not AiSpeaking is no-op`() {
        coordinator.startSession("greetings", Difficulty.BEGINNER, testScope)
        testScope.advanceTimeBy(DefaultSpeakingSessionCoordinator.INTRO_DELAY_MS + 1)
        // State is Listening after intro delay

        coordinator.interruptAiSpeaking()

        // State should remain Listening (no-op)
        assertTrue(coordinator.sessionState.value is SessionState.Listening)
    }

    @Test
    fun `full turn-taking flow follows correct state sequence`() {
        coordinator.startSession("daily_routine", Difficulty.INTERMEDIATE, testScope)
        assertTrue(coordinator.sessionState.value is SessionState.AiSpeaking)

        testScope.advanceTimeBy(DefaultSpeakingSessionCoordinator.INTRO_DELAY_MS + 1)
        assertTrue(coordinator.sessionState.value is SessionState.Listening)

        // Simulate recording stopped → processing
        coordinator.transitionTo(SessionState.Processing)
        assertTrue(coordinator.sessionState.value is SessionState.Processing)

        // User turn processed → AI thinking
        coordinator.onUserTurnProcessed()
        assertTrue(coordinator.sessionState.value is SessionState.AiThinking)

        // Reply generation started → AI speaking
        coordinator.onReplyGenerationStarted()
        assertTrue(coordinator.sessionState.value is SessionState.AiSpeaking)

        // Reply finished → back to listening
        coordinator.transitionTo(SessionState.Listening)
        assertTrue(coordinator.sessionState.value is SessionState.Listening)
    }

    // ── streamAiTurn ────────────────────────────────────────────────────

    @Test
    fun `streamAiTurn streams words incrementally and transitions to Listening`() {
        coordinator.startSession("greetings", Difficulty.BEGINNER, testScope)
        testScope.advanceTimeBy(DefaultSpeakingSessionCoordinator.INTRO_DELAY_MS + 1)

        val aiText = "Hello how are you"                           // 4 words
        coordinator.streamAiTurn(
            SessionTurn(role = TurnRole.AI, transcript = aiText, correctedText = aiText)
        )

        // Immediately after call: first word appended, AiSpeaking
        val session1 = coordinator.currentSession.value!!
        assertEquals(2, session1.messages.size)                   // intro + streaming turn
        assertEquals("Hello", session1.messages.last().transcript)
        assertTrue(session1.messages.last().isStreaming)
        assertTrue(coordinator.sessionState.value is SessionState.AiSpeaking)

        // After 1 tick → 2 words
        testScope.advanceTimeBy(DefaultSpeakingSessionCoordinator.STREAM_WORD_DELAY_MS + 1)
        assertEquals("Hello how", coordinator.currentSession.value!!.messages.last().transcript)
        assertTrue(coordinator.currentSession.value!!.messages.last().isStreaming)

        // After 2 ticks → 3 words
        testScope.advanceTimeBy(DefaultSpeakingSessionCoordinator.STREAM_WORD_DELAY_MS)
        assertEquals("Hello how are", coordinator.currentSession.value!!.messages.last().transcript)
        assertTrue(coordinator.currentSession.value!!.messages.last().isStreaming)

        // After 3 ticks → 4 words (complete)
        testScope.advanceTimeBy(DefaultSpeakingSessionCoordinator.STREAM_WORD_DELAY_MS)
        assertEquals("Hello how are you", coordinator.currentSession.value!!.messages.last().transcript)
        assertFalse(coordinator.currentSession.value!!.messages.last().isStreaming)
        assertTrue(coordinator.sessionState.value is SessionState.Listening)
    }

    @Test
    fun `streamAiTurn does not create multiple turns`() {
        coordinator.startSession("greetings", Difficulty.BEGINNER, testScope)
        testScope.advanceTimeBy(DefaultSpeakingSessionCoordinator.INTRO_DELAY_MS + 1)

        coordinator.streamAiTurn(
            SessionTurn(role = TurnRole.AI, transcript = "one two three")
        )

        // Advance through all streaming
        testScope.advanceTimeBy(DefaultSpeakingSessionCoordinator.STREAM_WORD_DELAY_MS * 3)

        // Should still be exactly 2 messages: intro + 1 streamed AI turn
        assertEquals(2, coordinator.currentSession.value!!.messages.size)
    }

    @Test
    fun `streamAiTurn handles single word`() {
        coordinator.startSession("greetings", Difficulty.BEGINNER, testScope)
        testScope.advanceTimeBy(DefaultSpeakingSessionCoordinator.INTRO_DELAY_MS + 1)

        coordinator.streamAiTurn(
            SessionTurn(role = TurnRole.AI, transcript = "Hello")
        )

        // Single-word: completes immediately in the coroutine
        testScope.advanceTimeBy(1)
        assertEquals("Hello", coordinator.currentSession.value!!.messages.last().transcript)
        assertFalse(coordinator.currentSession.value!!.messages.last().isStreaming)
        assertTrue(coordinator.sessionState.value is SessionState.Listening)
    }

    @Test
    fun `full flow with streamAiTurn follows correct state sequence`() {
        coordinator.startSession("daily_routine", Difficulty.INTERMEDIATE, testScope)
        testScope.advanceTimeBy(DefaultSpeakingSessionCoordinator.INTRO_DELAY_MS + 1)

        coordinator.transitionTo(SessionState.Processing)
        coordinator.onUserTurnProcessed()
        assertTrue(coordinator.sessionState.value is SessionState.AiThinking)

        coordinator.onReplyGenerationStarted()
        assertTrue(coordinator.sessionState.value is SessionState.AiSpeaking)

        // Stream the AI reply
        coordinator.streamAiTurn(
            SessionTurn(role = TurnRole.AI, transcript = "Good morning friend")
        )
        assertTrue(coordinator.sessionState.value is SessionState.AiSpeaking)

        // Complete streaming
        testScope.advanceTimeBy(DefaultSpeakingSessionCoordinator.STREAM_WORD_DELAY_MS * 3)
        assertTrue(coordinator.sessionState.value is SessionState.Listening)
        assertEquals("Good morning friend", coordinator.currentSession.value!!.messages.last().transcript)
    }

    // ── TTS integration ─────────────────────────────────────────────────

    /**
     * Lightweight fake that records calls to [speak] and [stop].
     */
    private class FakeTtsEngine : com.supereva.fluentai.domain.tts.TtsEngine {
        val spokenTexts = mutableListOf<String>()
        var stopCount = 0
        private val _isSpeaking = MutableStateFlow(false)
        override val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

        override suspend fun speak(text: String) {
            spokenTexts.add(text)
            _isSpeaking.value = true
        }

        override fun stop() {
            stopCount++
            _isSpeaking.value = false
        }
    }

    @Test
    fun `streamAiTurn calls ttsEngine speak with full text`() {
        val fake = FakeTtsEngine()
        val ttsCoordinator = DefaultSpeakingSessionCoordinator(ttsEngine = fake)
        val scope = TestScope()
        ttsCoordinator.startSession("greetings", Difficulty.BEGINNER, scope)
        scope.advanceTimeBy(DefaultSpeakingSessionCoordinator.INTRO_DELAY_MS + 1)

        ttsCoordinator.streamAiTurn(
            SessionTurn(role = TurnRole.AI, transcript = "Hello world")
        )
        scope.advanceTimeBy(DefaultSpeakingSessionCoordinator.STREAM_WORD_DELAY_MS + 1)

        // intro speak + streaming speak
        assertEquals(2, fake.spokenTexts.size)
        assertEquals("Hello world", fake.spokenTexts[1])
    }

    @Test
    fun `interruptAiSpeaking calls ttsEngine stop`() {
        val fake = FakeTtsEngine()
        val ttsCoordinator = DefaultSpeakingSessionCoordinator(ttsEngine = fake)
        val scope = TestScope()
        ttsCoordinator.startSession("greetings", Difficulty.BEGINNER, scope)
        scope.advanceTimeBy(DefaultSpeakingSessionCoordinator.INTRO_DELAY_MS + 1)

        ttsCoordinator.transitionTo(SessionState.AiSpeaking)
        ttsCoordinator.interruptAiSpeaking()

        assertTrue(fake.stopCount >= 1)
        assertTrue(ttsCoordinator.sessionState.value is SessionState.Listening)
    }

    @Test
    fun `endSession calls ttsEngine stop`() {
        val fake = FakeTtsEngine()
        val ttsCoordinator = DefaultSpeakingSessionCoordinator(ttsEngine = fake)
        val scope = TestScope()
        ttsCoordinator.startSession("greetings", Difficulty.BEGINNER, scope)
        scope.advanceTimeBy(DefaultSpeakingSessionCoordinator.INTRO_DELAY_MS + 1)

        ttsCoordinator.endSession()

        assertTrue(fake.stopCount >= 1)
    }
}
