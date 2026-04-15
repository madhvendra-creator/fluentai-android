package com.supereva.fluentai.data.repository

import com.supereva.fluentai.domain.learn.Lesson
import com.supereva.fluentai.domain.learn.LearnLevel
import com.supereva.fluentai.domain.learn.LearnUnit
import com.supereva.fluentai.domain.learn.LessonType

class LearnRepository {

    fun getUnits(): List<LearnUnit> = listOf(
        LearnUnit(
            id = "unit_interview",
            title = "Naukri ka interview",
            subtitle = "500+ sentences seekhein",
            emoji = "📋",
            levels = listOf(
                LearnLevel(
                    id = "level_intro",
                    levelNumber = 1,
                    title = "Apna introduction do",
                    totalLessons = 5,
                    completedLessons = 0,
                    isUnlocked = true,
                    lessons = listOf(
                        Lesson(
                            id = "lesson_intro_read",
                            title = "Reading",
                            description = "Is lesson mein 11 sentences seekho",
                            type = LessonType.READING,
                            isCompleted = false
                        ),
                        Lesson(
                            id = "lesson_intro_listen",
                            title = "Listening",
                            description = "Sentences sunein aur samjhein",
                            type = LessonType.LISTENING,
                            isCompleted = false
                        ),
                        Lesson(
                            id = "lesson_intro_arrange",
                            title = "Arrange Words",
                            description = "Words ko sahi order mein lagao",
                            type = LessonType.ARRANGE,
                            isCompleted = false
                        ),
                        Lesson(
                            id = "lesson_intro_speak",
                            title = "Speaking",
                            description = "Bolne ki practice karo",
                            type = LessonType.SPEAKING,
                            isCompleted = false
                        ),
                        Lesson(
                            id = "lesson_intro_final",
                            title = "Final Challenge",
                            description = "Sab kuch ek saath practice karo",
                            type = LessonType.ARRANGE,
                            isCompleted = false
                        )
                    )
                ),
                LearnLevel(
                    id = "level_common_q",
                    levelNumber = 2,
                    title = "Common interview questions",
                    totalLessons = 5,
                    completedLessons = 0,
                    isUnlocked = false,
                    lessons = listOf(
                        Lesson(
                            id = "lesson_cq_read",
                            title = "Reading",
                            description = "Common questions ko padhein",
                            type = LessonType.READING,
                            isCompleted = false
                        ),
                        Lesson(
                            id = "lesson_cq_listen",
                            title = "Listening",
                            description = "HR ke questions sunein",
                            type = LessonType.LISTENING,
                            isCompleted = false
                        ),
                        Lesson(
                            id = "lesson_cq_arrange",
                            title = "Arrange Words",
                            description = "Answer ke words arrange karo",
                            type = LessonType.ARRANGE,
                            isCompleted = false
                        ),
                        Lesson(
                            id = "lesson_cq_speak",
                            title = "Speaking",
                            description = "Answers bolne ki practice karo",
                            type = LessonType.SPEAKING,
                            isCompleted = false
                        ),
                        Lesson(
                            id = "lesson_cq_final",
                            title = "Final Challenge",
                            description = "Mock interview practice karo",
                            type = LessonType.ARRANGE,
                            isCompleted = false
                        )
                    )
                )
            )
        ),
        LearnUnit(
            id = "unit_daily",
            title = "Daily Conversation",
            subtitle = "300+ sentences seekhein",
            emoji = "💬",
            levels = listOf(
                LearnLevel(
                    id = "level_greetings",
                    levelNumber = 1,
                    title = "Greetings and basics",
                    totalLessons = 4,
                    completedLessons = 0,
                    isUnlocked = false,
                    lessons = listOf(
                        Lesson(
                            id = "lesson_greet_read",
                            title = "Reading",
                            description = "Basic greetings padhein",
                            type = LessonType.READING,
                            isCompleted = false
                        ),
                        Lesson(
                            id = "lesson_greet_listen",
                            title = "Listening",
                            description = "Greetings sunein",
                            type = LessonType.LISTENING,
                            isCompleted = false
                        ),
                        Lesson(
                            id = "lesson_greet_arrange",
                            title = "Arrange Words",
                            description = "Greeting sentences banao",
                            type = LessonType.ARRANGE,
                            isCompleted = false
                        ),
                        Lesson(
                            id = "lesson_greet_speak",
                            title = "Speaking",
                            description = "Greetings bolne ki practice",
                            type = LessonType.SPEAKING,
                            isCompleted = false
                        )
                    )
                ),
                LearnLevel(
                    id = "level_shopping",
                    levelNumber = 2,
                    title = "Shopping and travel",
                    totalLessons = 4,
                    completedLessons = 0,
                    isUnlocked = false,
                    lessons = listOf(
                        Lesson(
                            id = "lesson_shop_read",
                            title = "Reading",
                            description = "Shopping phrases padhein",
                            type = LessonType.READING,
                            isCompleted = false
                        ),
                        Lesson(
                            id = "lesson_shop_listen",
                            title = "Listening",
                            description = "Travel phrases sunein",
                            type = LessonType.LISTENING,
                            isCompleted = false
                        ),
                        Lesson(
                            id = "lesson_shop_arrange",
                            title = "Arrange Words",
                            description = "Shopping sentences banao",
                            type = LessonType.ARRANGE,
                            isCompleted = false
                        ),
                        Lesson(
                            id = "lesson_shop_speak",
                            title = "Speaking",
                            description = "Travel mein baat karna seekho",
                            type = LessonType.SPEAKING,
                            isCompleted = false
                        )
                    )
                )
            )
        )
    )
}
