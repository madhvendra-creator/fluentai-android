package com.supereva.fluentai.domain.repository

import com.supereva.fluentai.domain.model.Topic

/**
 * Contract for fetching conversation topics.
 * Implementations live in the data layer.
 */
interface TopicRepository {
    suspend fun getAllTopics(): List<Topic>
    suspend fun getTopicsByCategory(category: String): List<Topic>
}
