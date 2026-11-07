package com.qurancompanion.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.qurancompanion.app.domain.model.QuizType
import com.qurancompanion.app.ui.achievements.AchievementsScreen
import com.qurancompanion.app.ui.backup.BackupScreen
import com.qurancompanion.app.ui.activity.ActivityLogScreen
import com.qurancompanion.app.ui.goals.GoalFormScreen
import com.qurancompanion.app.ui.goals.GoalsScreen
import com.qurancompanion.app.ui.home.HomeScreen
import com.qurancompanion.app.ui.map.MapScreen
import com.qurancompanion.app.ui.memorize.MemorizeFormScreen
import com.qurancompanion.app.ui.memorize.MemorizeScreen
import com.qurancompanion.app.ui.more.MoreScreen
import com.qurancompanion.app.ui.quiz.QuizScreen
import com.qurancompanion.app.ui.quran.QuranScreen
import com.qurancompanion.app.ui.quran.ReaderScreen
import com.qurancompanion.app.ui.review.ReviewScreen
import com.qurancompanion.app.ui.review.ReviewSessionScreen
import com.qurancompanion.app.ui.settings.SettingsScreen
import com.qurancompanion.app.ui.similar.SimilarGroupScreen
import com.qurancompanion.app.ui.similar.SimilarScreen
import com.qurancompanion.app.ui.stats.StatsScreen

private fun NavHostController.navigateTo(route: String) {
    if (TopLevelDestinations.any { it.route == route }) {
        navigate(route) {
            popUpTo(graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    } else {
        navigate(route) { launchSingleTop = true }
    }
}

@Composable
fun AppRoot() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = TopLevelDestinations.any { it.route == currentRoute }
    val back: () -> Unit = { navController.popBackStack() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    TopLevelDestinations.forEach { dest ->
                        NavigationBarItem(
                            selected = currentRoute == dest.route,
                            onClick = { navController.navigateTo(dest.route) },
                            icon = { Icon(dest.icon, contentDescription = dest.label) },
                            label = { Text(dest.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            ),
                        )
                    }
                }
            }
        },
    ) { inner ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(inner),
        ) {
            composable(Routes.HOME) { HomeScreen(onNavigate = { navController.navigateTo(it) }) }
            composable(Routes.MORE) { MoreScreen(onNavigate = { navController.navigateTo(it) }) }

            // Quran
            composable(Routes.QURAN) {
                QuranScreen(onOpenReader = { page, ayahId -> navController.navigateTo(Routes.reader(page, ayahId)) })
            }
            composable(
                route = Routes.READER,
                arguments = listOf(
                    navArgument("page") { type = NavType.IntType },
                    navArgument("ayah") { type = NavType.IntType; defaultValue = 0 },
                ),
            ) { entry ->
                ReaderScreen(
                    initialPage = entry.arguments?.getInt("page") ?: 1,
                    highlightAyahId = entry.arguments?.getInt("ayah") ?: 0,
                    onBack = back,
                )
            }

            // Memorization
            composable(Routes.MEMORIZE) {
                MemorizeScreen(
                    onOpenForm = { id -> navController.navigateTo(Routes.memorizeForm(id)) },
                    onOpenReader = { page, ayahId -> navController.navigateTo(Routes.reader(page, ayahId)) },
                )
            }
            composable(
                route = Routes.MEMORIZE_FORM,
                arguments = listOf(
                    navArgument("id") { type = NavType.LongType; defaultValue = 0L },
                    navArgument("start") { type = NavType.IntType; defaultValue = 0 },
                    navArgument("end") { type = NavType.IntType; defaultValue = 0 },
                ),
            ) { entry ->
                MemorizeFormScreen(
                    entryId = entry.arguments?.getLong("id") ?: 0L,
                    presetStart = entry.arguments?.getInt("start") ?: 0,
                    presetEnd = entry.arguments?.getInt("end") ?: 0,
                    onDone = back,
                )
            }

            // Review
            composable(Routes.REVIEW) {
                ReviewScreen(onStart = { id -> navController.navigateTo(Routes.reviewSession(id)) })
            }
            composable(
                route = Routes.REVIEW_SESSION,
                arguments = listOf(navArgument("entryId") { type = NavType.LongType }),
            ) { entry ->
                ReviewSessionScreen(
                    entryId = entry.arguments?.getLong("entryId") ?: 0L,
                    onDone = back,
                    onEditEntry = { id -> navController.navigateTo(Routes.memorizeForm(id)) },
                )
            }

            // Quiz and similar ayahs
            composable(
                route = Routes.QUIZ_PATTERN,
                arguments = listOf(navArgument("type") { type = NavType.StringType; defaultValue = "" }),
            ) { entry ->
                val typeName = entry.arguments?.getString("type").orEmpty()
                QuizScreen(initialType = QuizType.entries.firstOrNull { it.name == typeName }, onBack = back)
            }
            composable(Routes.SIMILAR) {
                SimilarScreen(
                    onBack = back,
                    onOpenGroup = { id -> navController.navigateTo(Routes.similarGroup(id)) },
                    onOpenQuiz = { navController.navigateTo(Routes.quiz(QuizType.SIMILAR.name)) },
                )
            }
            composable(
                route = Routes.SIMILAR_GROUP,
                arguments = listOf(navArgument("id") { type = NavType.IntType }),
            ) { entry ->
                SimilarGroupScreen(
                    groupId = entry.arguments?.getInt("id") ?: 0,
                    onBack = back,
                    onOpenReader = { page, ayahId -> navController.navigateTo(Routes.reader(page, ayahId)) },
                )
            }

            // Goals, statistics, map
            composable(Routes.GOALS) {
                GoalsScreen(
                    onBack = back,
                    onNewGoal = { navController.navigateTo(Routes.GOAL_FORM) },
                    onRecord = { start, end -> navController.navigateTo(Routes.memorizeForm(0L, start, end)) },
                )
            }
            composable(Routes.GOAL_FORM) { GoalFormScreen(onDone = back) }
            composable(Routes.STATS) { StatsScreen(onBack = back) }
            composable(Routes.MAP) {
                MapScreen(
                    onBack = back,
                    onOpenReader = { page, ayahId -> navController.navigateTo(Routes.reader(page, ayahId)) },
                )
            }

            composable(Routes.ACHIEVEMENTS) { AchievementsScreen(onBack = back) }
            composable(Routes.ACTIVITY) { ActivityLogScreen(onBack = back) }

            composable(Routes.SETTINGS) { SettingsScreen(onBack = back) }
            composable(Routes.BACKUP) { BackupScreen(onBack = back) }
        }
    }
}
