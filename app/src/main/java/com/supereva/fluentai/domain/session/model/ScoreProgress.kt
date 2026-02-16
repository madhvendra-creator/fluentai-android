package com.supereva.fluentai.domain.session.model

/**
 * Running score summary for a session.
 *
 * [totalScore] is the cumulative raw score across all scored turns.
 * [turnCount]  is the number of turns that contributed a score.
 * [averageScore] is derived; returns 0.0 when no scored turns exist.
 */
data class ScoreProgress(
    val totalScore: Int = 0,
    val turnCount: Int = 0
) {
    val averageScore: Double
        get() = if (turnCount > 0) totalScore.toDouble() / turnCount else 0.0
}
