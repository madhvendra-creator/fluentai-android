package com.supereva.fluentai.domain.learn

data class LearnUnit(
    val id: String,
    val title: String,
    val subtitle: String,
    val emoji: String,
    val levels: List<LearnLevel>
)

data class LearnLevel(
    val id: String,
    val levelNumber: Int,
    val title: String,
    val totalLessons: Int,
    val completedLessons: Int,
    val isUnlocked: Boolean,
    val lessons: List<Lesson>
)

data class Lesson(
    val id: String,
    val title: String,
    val description: String,
    val type: LessonType,
    val isCompleted: Boolean
)

enum class LessonType { READING, LISTENING, SPEAKING, ARRANGE }
