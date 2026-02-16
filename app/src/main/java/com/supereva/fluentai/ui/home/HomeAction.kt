package com.supereva.fluentai.ui.home

/**
 * One-shot user intentions on the Home screen.
 *
 * Dispatched to [HomeViewModel.onAction] — keeps composables
 * free of business logic.
 */
sealed interface HomeAction {

    /** User tapped the "Start Free Talk" AI practice card. */
    data object StartAiPractice : HomeAction

    /** User tapped a specific topic card. */
    data class OpenTopic(val topicId: String) : HomeAction
}
