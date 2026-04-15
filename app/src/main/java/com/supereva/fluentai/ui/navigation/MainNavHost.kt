package com.supereva.fluentai.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
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
                FloatingBottomNavBar(
                    items = bottomNavItems,
                    currentDestination = currentDestination,
                    onItemClick = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
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
                },
                onNavigateToLesson = { lessonId ->
                    navController.navigate(LessonRoute(lessonId))
                },
                onNavigateToMenu = {
                    navController.navigate(MenuRoute)
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

            composable<LessonRoute> { backStackEntry ->
                val route = backStackEntry.toRoute<LessonRoute>()
                val learnViewModel: com.supereva.fluentai.ui.learn.LearnViewModel =
                    androidx.lifecycle.viewmodel.compose.viewModel(factory = com.supereva.fluentai.ui.learn.LearnViewModel.Factory)
                com.supereva.fluentai.ui.learn.LessonScreen(
                    lessonId = route.lessonId,
                    onClose = { navController.popBackStack() },
                    onComplete = { lessonId ->
                        learnViewModel.completeLesson(lessonId)
                        navController.popBackStack()
                    }
                )
            }

            composable<MenuRoute> {
                com.supereva.fluentai.ui.menu.MenuScreen(
                    onBack = { navController.popBackStack() }
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
    BottomNavItem(LearnRoute, "Learn", Icons.Default.School),
    BottomNavItem(ProgressRoute, "Progress", Icons.Default.Star),
    BottomNavItem(MembershipRoute, "Premium", Icons.Default.WorkspacePremium)
)

// ── Floating bottom nav composables ────────────────────────────────────

@Composable
private fun FloatingBottomNavBar(
    items: List<BottomNavItem<*>>,
    currentDestination: NavDestination?,
    onItemClick: (Any) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(bottom = 8.dp, top = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(elevation = 24.dp, shape = RoundedCornerShape(28.dp), ambientColor = Color.Black, spotColor = Color.Black)
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xFF1A1A2E))
                .padding(horizontal = 8.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    val selected = currentDestination?.hasRoute(item.route::class) == true
                    FloatingNavItem(
                        item = item,
                        selected = selected,
                        onClick = { onItemClick(item.route) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FloatingNavItem(
    item: BottomNavItem<*>,
    selected: Boolean,
    onClick: () -> Unit
) {
    val scale = remember { Animatable(1f) }
    LaunchedEffect(selected) {
        if (selected) {
            scale.animateTo(1.18f, tween(80, easing = FastOutSlowInEasing))
            scale.animateTo(
                1f,
                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
            )
        }
    }

    Box(
        modifier = Modifier
            .scale(scale.value)
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) Color(0xFF4CAF50) else Color.Transparent)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() }
            .padding(horizontal = if (selected) 14.dp else 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.label,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    text = item.label,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        } else {
            Icon(
                imageVector = item.icon,
                contentDescription = item.label,
                tint = Color(0xFF666666),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
