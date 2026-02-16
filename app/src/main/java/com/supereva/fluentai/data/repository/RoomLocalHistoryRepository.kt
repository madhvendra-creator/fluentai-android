package com.supereva.fluentai.data.repository

import com.supereva.fluentai.data.database.dao.SessionDao
import com.supereva.fluentai.data.database.entity.SessionEntity
import com.supereva.fluentai.domain.repository.LocalHistoryRepository
import com.supereva.fluentai.domain.session.model.SpeakingSession
import com.supereva.fluentai.domain.session.model.TurnRole
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class RoomLocalHistoryRepository(
    private val sessionDao: SessionDao
) : LocalHistoryRepository {

    override suspend fun saveSession(session: SpeakingSession) {
        val feedbacks = session.messages
            .filter { it.role == TurnRole.AI && it.feedback.isNotEmpty() }
            .map { it.feedback }
        
        val feedbackJson = Json.encodeToString(feedbacks)

        val entity = SessionEntity(
            sessionId = session.sessionId,
            topicId = session.topicId,
            averageScore = session.scoreProgress.averageScore,
            turnCount = session.scoreProgress.turnCount,
            timestamp = System.currentTimeMillis(), // Or use session updated time
            feedbackJson = feedbackJson
        )

        sessionDao.insertSession(entity)
    }

    override fun getAllSessions(): kotlinx.coroutines.flow.Flow<List<SessionEntity>> {
        return sessionDao.getAllSessions()
    }
}
