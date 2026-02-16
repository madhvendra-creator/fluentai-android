package com.supereva.fluentai.ui.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import androidx.navigation.toRoute
import com.supereva.fluentai.ui.practice.PracticeScreen

/**
 * Nested graph for the speaking-session flow.
 *
 * Extracts [SpeakingSessionRoute] args (topicId, difficulty,
 * sessionMode) and passes them to [PracticeScreen].
 *
 * The [SpeakingSessionViewModel] is scoped to the **nav graph**
 * backstack entry, so it survives recomposition and bottom-nav
 * switches but is destroyed when the graph is popped.
 */
fun NavGraphBuilder.speakingSessionGraph(
    onBack: () -> Unit
) {
    navigation<SpeakingSessionGraphRoute>(
        startDestination = SpeakingSessionRoute(topicId = "free_talk")
    ) {
        composable<SpeakingSessionRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<SpeakingSessionRoute>()

            // Scope ViewModel to the nav-graph backstack entry
            // so it survives child-composable recompositions.
            PracticeScreen(
                onBackClick = onBack,
                viewModelStoreOwner = backStackEntry
            )
        }
    }
}
