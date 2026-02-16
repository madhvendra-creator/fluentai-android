package com.supereva.fluentai.domain.usecase

import com.supereva.fluentai.domain.model.Topic
import com.supereva.fluentai.domain.repository.TopicRepository

/**
 * Fetches all topics and groups them by [Topic.category].
 *
 * Returns a map where keys are category names and values are
 * the topics belonging to that category, preserving insertion order.
 */
class GetTopicsUseCase(
    private val topicRepository: TopicRepository
) {
    suspend operator fun invoke(): Map<String, List<Topic>> {
        return topicRepository.getAllTopics()
            .groupBy { it.category }
    }
}
