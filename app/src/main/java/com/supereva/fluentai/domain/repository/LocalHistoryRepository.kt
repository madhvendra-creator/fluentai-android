package com.supereva.fluentai.domain.repository

import com.supereva.fluentai.domain.session.model.SpeakingSession

interface LocalHistoryRepository {
    suspend fun saveSession(session: SpeakingSession)
    fun getAllSessions(): kotlinx.coroutines.flow.Flow<List<com.supereva.fluentai.data.database.entity.SessionEntity>>
}
