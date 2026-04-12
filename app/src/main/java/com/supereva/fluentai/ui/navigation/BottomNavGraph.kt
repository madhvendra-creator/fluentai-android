package com.supereva.fluentai.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.supereva.fluentai.ui.home.HomeScreen

/**
 * Nested graph for bottom-navigation tab screens.
 *
 * The graph is Hilt-ready — just swap `composable` →
 * `hiltComposable` when Hilt is introduced.
 */
fun NavGraphBuilder.bottomNavGraph(
    onNavigateToSession: (topicId: String) -> Unit,
    onNavigateToActiveTranslation: () -> Unit
) {
    navigation<BottomNavGraphRoute>(startDestination = HomeRoute) {

        composable<HomeRoute> {
            HomeScreen(
                onNavigateToSession = onNavigateToSession
            )
        }

        composable<TranslationRoute> {
            com.supereva.fluentai.ui.translation.TranslationScreen(
                onStartTranslation = onNavigateToActiveTranslation
            )
        }

        composable<ProgressRoute> {
            com.supereva.fluentai.ui.progress.ProgressScreen()
        }

        composable<MembershipRoute> {
            PlaceholderScreen(title = "Membership")
        }
    }
}

// ── Placeholder screens (replace with real implementations) ─────────────

@Composable
internal fun PlaceholderScreen(title: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
