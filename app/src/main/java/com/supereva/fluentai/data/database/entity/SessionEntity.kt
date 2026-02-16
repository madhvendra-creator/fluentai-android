package com.supereva.fluentai.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val sessionId: String,
    val topicId: String,
    val averageScore: Double,
    val turnCount: Int,
    val timestamp: Long,
    val feedbackJson: String // Serialized list of feedback strings
)
