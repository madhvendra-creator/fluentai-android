package com.supereva.fluentai.ui.home

/**
 * One-shot navigation events emitted by [HomeViewModel].
 *
 * Collected inside a `LaunchedEffect` so each event fires
 * exactly once, surviving configuration changes safely.
 */
sealed interface HomeNavigationEvent {

    /** Navigate to the speaking-session screen for [topicId]. */
    data class NavigateToSession(val topicId: String) : HomeNavigationEvent
}
