package com.supereva.fluentai.data.ai

import com.supereva.fluentai.domain.ai.AiChunk
import com.supereva.fluentai.domain.ai.AiConversationEngine
import com.supereva.fluentai.domain.session.model.Difficulty
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import java.io.File

class OpenAiRealtimeEngine : AiConversationEngine {
    override suspend fun startSession(topicId: String, difficulty: Difficulty) {}
    override suspend fun sendStreamChunk(chunk: ByteArray) {}
    override suspend fun sendUserAudio(file: File) {}
    override fun observeAiResponses(): Flow<AiChunk> = emptyFlow()
    override suspend fun endSession() {}
    override suspend fun commitUserTurn() {}
}
