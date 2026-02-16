package com.supereva.fluentai.domain.model

import com.supereva.fluentai.domain.session.model.Difficulty

/**
 * Domain model representing a conversation topic.
 *
 * Pure Kotlin — no Android or UI dependencies.
 */
data class Topic(
    val id: String,
    val title: String,
    val category: String,
    val durationMinutes: Int,
    val difficulty: Difficulty,
    val imageUrl: String = ""
)
