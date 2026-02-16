package com.supereva.fluentai.domain.session

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TurnTakingEngineTest {

    private lateinit var engine: TurnTakingEngine

    @Before
    fun setUp() {
        engine = TurnTakingEngine()
    }

    @Test
    fun `onUserTurnProcessed returns AiThinking`() {
        val result = engine.onUserTurnProcessed()
        assertTrue(result is SessionState.AiThinking)
    }

    @Test
    fun `onReplyGenerationStarted returns AiSpeaking`() {
        val result = engine.onReplyGenerationStarted()
        assertTrue(result is SessionState.AiSpeaking)
    }

    @Test
    fun `onUserInterrupted during AiSpeaking returns UserInterrupting`() {
        val result = engine.onUserInterrupted(SessionState.AiSpeaking)
        assertNotNull(result)
        assertTrue(result is SessionState.UserInterrupting)
    }

    @Test
    fun `onUserInterrupted during Listening returns null`() {
        val result = engine.onUserInterrupted(SessionState.Listening)
        assertNull(result)
    }

    @Test
    fun `onUserInterrupted during AiThinking returns null`() {
        val result = engine.onUserInterrupted(SessionState.AiThinking)
        assertNull(result)
    }

    @Test
    fun `onUserInterrupted during Processing returns null`() {
        val result = engine.onUserInterrupted(SessionState.Processing)
        assertNull(result)
    }

    @Test
    fun `onUserInterrupted during Idle returns null`() {
        val result = engine.onUserInterrupted(SessionState.Idle)
        assertNull(result)
    }

    @Test
    fun `onInterruptionHandled returns Listening`() {
        val result = engine.onInterruptionHandled()
        assertTrue(result is SessionState.Listening)
    }
}
