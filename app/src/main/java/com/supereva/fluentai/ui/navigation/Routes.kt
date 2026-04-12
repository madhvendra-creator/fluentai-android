package com.supereva.fluentai.ui.navigation

import com.supereva.fluentai.domain.session.model.Difficulty
import com.supereva.fluentai.domain.session.model.SessionMode
import kotlinx.serialization.Serializable

/**
 * Type-safe route definitions for the entire app.
 *
 * Each route is a `@Serializable` class / object so that Navigation
 * Compose 2.8+ can encode/decode arguments automatically.
 * Deep-link URIs can be derived from these same types.
 */

// ── Bottom-nav tab routes ───────────────────────────────────────────────

@Serializable
data object HomeRoute

@Serializable
data object TranslationRoute

@Serializable
data object MembershipRoute

@Serializable
data object ProgressRoute

@Serializable
data object ActiveTranslationRoute

// ── Speaking session (full-screen overlay) ──────────────────────────────

@Serializable
data class SpeakingSessionRoute(
    val topicId: String,
    val difficulty: String = Difficulty.BEGINNER.name,
    val sessionMode: String = SessionMode.AI.name,
    val sourceLang: String? = null,
    val targetLang: String? = null
) {
    /** Convenience: parse [difficulty] back to the enum. */
    fun parseDifficulty(): Difficulty =
        Difficulty.valueOf(difficulty)

    /** Convenience: parse [sessionMode] back to the enum. */
    fun parseSessionMode(): SessionMode =
        SessionMode.valueOf(sessionMode)
}

@Serializable
data class LanguagePickerRoute(val isSource: Boolean)

// ── Graph-level route markers (used for nested nav graphs) ──────────────

@Serializable
data object BottomNavGraphRoute

@Serializable
data object SpeakingSessionGraphRoute
