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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.qurancompanion.app.ui.home.HomeScreen
import com.qurancompanion.app.ui.more.MoreScreen
import com.qurancompanion.app.ui.placeholder.PlaceholderScreen

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

            TopLevelDestinations
                .filter { it.route != Routes.HOME && it.route != Routes.MORE }
                .forEach { dest ->
                    composable(dest.route) {
                        PlaceholderScreen(dest.label, dest.phase, dest.icon, onBack = null)
                    }
                }
            MoreDestinations.forEach { dest ->
                composable(dest.route) {
                    PlaceholderScreen(dest.label, dest.phase, dest.icon, onBack = { navController.popBackStack() })
                }
            }
        }
    }
}
