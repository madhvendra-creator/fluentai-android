package com.supereva.fluentai.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.toRoute
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.supereva.fluentai.domain.session.model.Difficulty
import com.supereva.fluentai.domain.session.model.SessionMode

/**
 * Root navigation host composing all nested graphs.
 *
 * Shows a bottom bar for tab destinations and hides it when
 * the user enters the full-screen speaking session.
 */
@Composable
fun MainNavHost() {
    val translationViewModel: com.supereva.fluentai.ui.translation.TranslationViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val sourceLanguage by translationViewModel.sourceLanguage.collectAsState()
    val targetLanguage by translationViewModel.targetLanguage.collectAsState()

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    // Hide bottom bar during speaking session
    val showBottomBar = currentDestination?.let { dest ->
        bottomNavItems.any { dest.hasRoute(it.route::class) }
    } ?: true

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.hasRoute(item.route::class) == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label
                                )
                            },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavGraphRoute,
            modifier = Modifier.padding(innerPadding),
            // Disable crossfade to prevent ExoPlayer SurfaceView glitches during tab switches
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None }
        ) {
            bottomNavGraph(
                onNavigateToSession = { topicId ->
                    navController.navigate(
                        SpeakingSessionRoute(
                            topicId = topicId,
                            difficulty = Difficulty.BEGINNER.name,
                            sessionMode = SessionMode.AI.name
                        )
                    )
                },
                onNavigateToActiveTranslation = {
                    navController.navigate(ActiveTranslationRoute)
                }
            )

            composable<ActiveTranslationRoute> {
                com.supereva.fluentai.ui.translation.ActiveTranslationScreen(
                    sourceLanguage = sourceLanguage,
                    targetLanguage = targetLanguage,
                    onBack = { navController.popBackStack() },
                    onOpenLanguagePicker = { isSource ->
                        navController.navigate(LanguagePickerRoute(isSource))
                    },
                    onSwapLanguages = { translationViewModel.swapLanguages() },
                    onStartPracticeCall = { src, tgt ->
                        navController.navigate(
                            SpeakingSessionRoute(
                                topicId = "translation_practice",
                                difficulty = Difficulty.BEGINNER.name,
                                sessionMode = SessionMode.TRANSLATION_PRACTICE.name,
                                sourceLang = src,
                                targetLang = tgt
                            )
                        )
                    }
                )
            }

            composable<LanguagePickerRoute> { backStackEntry ->
                val route = backStackEntry.toRoute<LanguagePickerRoute>()
                com.supereva.fluentai.ui.translation.LanguagePickerScreen(
                    isSource = route.isSource,
                    currentSelectedLanguage = if (route.isSource) sourceLanguage else targetLanguage,
                    disabledLanguage = if (route.isSource) targetLanguage else sourceLanguage,
                    onBack = { navController.popBackStack() },
                    onLanguageSelected = { language ->
                        translationViewModel.setLanguage(route.isSource, language)
                        navController.popBackStack()
                    }
                )
            }

            speakingSessionGraph(
                onBack = { navController.popBackStack() }
            )
        }
    }
}

// ── Bottom nav item definitions ─────────────────────────────────────────

private data class BottomNavItem<T : Any>(
    val route: T,
    val label: String,
    val icon: ImageVector
)

private val bottomNavItems = listOf(
    BottomNavItem(HomeRoute, "Home", Icons.Default.Home),
    BottomNavItem(TranslationRoute, "Translate", Icons.Default.Translate),
    BottomNavItem(ProgressRoute, "Progress", Icons.Default.Star),
    BottomNavItem(MembershipRoute, "Premium", Icons.Outlined.Info)
)
