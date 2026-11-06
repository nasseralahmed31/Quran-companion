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
import com.qurancompanion.app.ui.achievements.AchievementsScreen
import com.qurancompanion.app.ui.activity.ActivityLogScreen
import com.qurancompanion.app.ui.home.HomeScreen
import com.qurancompanion.app.ui.memorize.MemorizeFormScreen
import com.qurancompanion.app.ui.memorize.MemorizeScreen
import com.qurancompanion.app.ui.more.MoreScreen
import com.qurancompanion.app.ui.placeholder.PlaceholderScreen
import com.qurancompanion.app.ui.quran.QuranScreen
import com.qurancompanion.app.ui.quran.ReaderScreen

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

/** Routes that have a real screen; every other destination still shows the "under construction" page. */
private val implementedRoutes = setOf(
    Routes.HOME,
    Routes.MORE,
    Routes.QURAN,
    Routes.MEMORIZE,
    Routes.ACHIEVEMENTS,
    Routes.ACTIVITY,
)

@Composable
fun AppRoot() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = TopLevelDestinations.any { it.route == currentRoute }

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
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.MEMORIZE) {
                MemorizeScreen(
                    onOpenForm = { id -> navController.navigateTo(Routes.memorizeForm(id)) },
                    onOpenReader = { page, ayahId -> navController.navigateTo(Routes.reader(page, ayahId)) },
                )
            }
            composable(
                route = Routes.MEMORIZE_FORM,
                arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = 0L }),
            ) { entry ->
                MemorizeFormScreen(
                    entryId = entry.arguments?.getLong("id") ?: 0L,
                    onDone = { navController.popBackStack() },
                )
            }
            composable(Routes.ACHIEVEMENTS) { AchievementsScreen(onBack = { navController.popBackStack() }) }
            composable(Routes.ACTIVITY) { ActivityLogScreen(onBack = { navController.popBackStack() }) }

            (TopLevelDestinations + MoreDestinations)
                .filter { it.route !in implementedRoutes }
                .forEach { dest ->
                    val isTab = TopLevelDestinations.any { it.route == dest.route }
                    composable(dest.route) {
                        PlaceholderScreen(
                            title = dest.label,
                            phase = dest.phase,
                            icon = dest.icon,
                            onBack = if (isTab) null else ({ navController.popBackStack() }),
                        )
                    }
                }
        }
    }
}
