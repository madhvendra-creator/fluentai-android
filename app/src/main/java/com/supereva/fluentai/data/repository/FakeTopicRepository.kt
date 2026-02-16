package com.supereva.fluentai.data.repository

import com.supereva.fluentai.domain.model.Topic
import com.supereva.fluentai.domain.repository.TopicRepository
import com.supereva.fluentai.domain.session.model.Difficulty
import kotlinx.coroutines.delay

/**
 * In-memory topic repository with realistic placeholder data.
 * Replace with a real API / Room data source later.
 */
class FakeTopicRepository : TopicRepository {

    private val topics = listOf(
        // ── Job Interview ───────────────────────────────────────
        Topic(
            id = "ji_01",
            title = "Tell Me About Yourself",
            category = "Job Interview",
            durationMinutes = 5,
            difficulty = Difficulty.BEGINNER
        ),
        Topic(
            id = "ji_02",
            title = "Strengths & Weaknesses",
            category = "Job Interview",
            durationMinutes = 7,
            difficulty = Difficulty.INTERMEDIATE
        ),
        Topic(
            id = "ji_03",
            title = "Behavioral Questions",
            category = "Job Interview",
            durationMinutes = 10,
            difficulty = Difficulty.ADVANCED
        ),
        Topic(
            id = "ji_04",
            title = "Salary Negotiation",
            category = "Job Interview",
            durationMinutes = 8,
            difficulty = Difficulty.ADVANCED
        ),

        // ── Office Talk ─────────────────────────────────────────
        Topic(
            id = "ot_01",
            title = "Water Cooler Chat",
            category = "Office Talk",
            durationMinutes = 3,
            difficulty = Difficulty.BEGINNER
        ),
        Topic(
            id = "ot_02",
            title = "Team Stand-up",
            category = "Office Talk",
            durationMinutes = 5,
            difficulty = Difficulty.INTERMEDIATE
        ),
        Topic(
            id = "ot_03",
            title = "Presenting Ideas",
            category = "Office Talk",
            durationMinutes = 8,
            difficulty = Difficulty.INTERMEDIATE
        ),
        Topic(
            id = "ot_04",
            title = "Email Follow-ups",
            category = "Office Talk",
            durationMinutes = 4,
            difficulty = Difficulty.BEGINNER
        ),

        // ── Daily English ───────────────────────────────────────
        Topic(
            id = "de_01",
            title = "Ordering Food",
            category = "Daily English",
            durationMinutes = 3,
            difficulty = Difficulty.BEGINNER
        ),
        Topic(
            id = "de_02",
            title = "Asking for Directions",
            category = "Daily English",
            durationMinutes = 4,
            difficulty = Difficulty.BEGINNER
        ),
        Topic(
            id = "de_03",
            title = "Shopping Conversations",
            category = "Daily English",
            durationMinutes = 5,
            difficulty = Difficulty.INTERMEDIATE
        ),
        Topic(
            id = "de_04",
            title = "Making Plans with Friends",
            category = "Daily English",
            durationMinutes = 6,
            difficulty = Difficulty.INTERMEDIATE
        ),
        Topic(
            id = "de_05",
            title = "Doctor's Appointment",
            category = "Daily English",
            durationMinutes = 7,
            difficulty = Difficulty.ADVANCED
        )
    )

    override suspend fun getAllTopics(): List<Topic> {
        delay(300) // simulate network latency
        return topics
    }

    override suspend fun getTopicsByCategory(category: String): List<Topic> {
        delay(200)
        return topics.filter { it.category == category }
    }
}
