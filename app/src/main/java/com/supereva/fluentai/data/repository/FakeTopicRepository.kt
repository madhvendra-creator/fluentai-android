package com.supereva.fluentai.data.repository

import com.supereva.fluentai.R
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
            difficulty = Difficulty.BEGINNER,
            firstQuestion = "Hi! Let's practice for your interview. To start, could you tell me a little bit about yourself?"
        ),
        Topic(
            id = "ji_02",
            title = "Strengths & Weaknesses",
            category = "Job Interview",
            durationMinutes = 7,
            difficulty = Difficulty.INTERMEDIATE,
            firstQuestion = "Hi! Let's practice for your interview. To start, what would you say is your greatest strength?",
            imageResId = R.drawable.strength
        ),
        Topic(
            id = "ji_03",
            title = "Behavioral Questions",
            category = "Job Interview",
            durationMinutes = 10,
            difficulty = Difficulty.ADVANCED,
            firstQuestion = "Hello! For this interview sequence, could you describe a time when you faced a significant challenge at work and how you overcame it?"
        ),
        Topic(
            id = "ji_04",
            title = "Salary Negotiation",
            category = "Job Interview",
            durationMinutes = 8,
            difficulty = Difficulty.ADVANCED,
            firstQuestion = "Welcome to the final stage. We would love to offer you the position. What are your salary expectations?",
            imageResId = R.drawable.salary
        ),

        // ── Office Talk ─────────────────────────────────────────
        Topic(
            id = "ot_01",
            title = "Water Cooler Chat",
            category = "Office Talk",
            durationMinutes = 3,
            difficulty = Difficulty.BEGINNER,
            firstQuestion = "Hey there! How was your weekend? Do anything fun?",
            imageResId = R.drawable.watercooler
        ),
        Topic(
            id = "ot_02",
            title = "Team Stand-up",
            category = "Office Talk",
            durationMinutes = 5,
            difficulty = Difficulty.INTERMEDIATE,
            firstQuestion = "Good morning everyone. Let's start the stand-up. What did you work on yesterday and what's on your plate today?",
            imageResId = R.drawable.teamstandup
        ),
        Topic(
            id = "ot_03",
            title = "Presenting Ideas",
            category = "Office Talk",
            durationMinutes = 8,
            difficulty = Difficulty.INTERMEDIATE,
            firstQuestion = "Thanks for setting up this meeting. We're eager to hear your proposal. Whenever you're ready, the floor is yours.",
            imageResId = R.drawable.presentingideas
        ),
        Topic(
            id = "ot_04",
            title = "Email Follow-ups",
            category = "Office Talk",
            durationMinutes = 4,
            difficulty = Difficulty.BEGINNER,
            firstQuestion = "Hi! I just wanted to touch base regarding the email I sent yesterday. Have you had a chance to review the document?",
            imageResId = R.drawable.emailfollowups
        ),

        // ── Daily English ───────────────────────────────────────
        Topic(
            id = "de_01",
            title = "Ordering Food",
            category = "Daily English",
            durationMinutes = 3,
            difficulty = Difficulty.BEGINNER,
            firstQuestion = "Welcome to our restaurant! Are you ready to order, or do you need a few more minutes?",
            imageResId = R.drawable.orderingfood
        ),
        Topic(
            id = "de_02",
            title = "Asking for Directions",
            category = "Daily English",
            durationMinutes = 4,
            difficulty = Difficulty.BEGINNER,
            firstQuestion = "Excuse me, I'm a bit lost. Could you help me find the nearest train station?",
            imageResId = R.drawable.direction
        ),
        Topic(
            id = "de_03",
            title = "Shopping Conversations",
            category = "Daily English",
            durationMinutes = 5,
            difficulty = Difficulty.INTERMEDIATE,
            firstQuestion = "Hi there! Let me know if you need help finding a particular size or style today.",
            imageResId = R.drawable.shoppingconversation
        ),
        Topic(
            id = "de_04",
            title = "Making Plans with Friends",
            category = "Daily English",
            durationMinutes = 6,
            difficulty = Difficulty.INTERMEDIATE,
            firstQuestion = "Hey! It's been a while. Are you free this weekend to grab some coffee and catch up?",
            imageResId = R.drawable.planswithfriends
        ),
        Topic(
            id = "de_05",
            title = "Doctor's Appointment",
            category = "Daily English",
            durationMinutes = 7,
            difficulty = Difficulty.ADVANCED,
            firstQuestion = "Hello, please come in and have a seat. What seems to be bringing you into the clinic today?",
            imageResId = R.drawable.doctorsappointment
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
