package com.missabusiness.app.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.missabusiness.app.ui.screens.PlaceholderScreen

/**
 * Coquille de navigation de l'application.
 * Pour l'instant tous les écrans sont des placeholders en attendant
 * la validation des maquettes (docs/).
 */
@Composable
fun ErpApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            ErpBottomBar(
                currentRoute = currentRoute,
                onNavigate = { destination ->
                    navController.navigate(destination.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
            )
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = ErpDestination.DASHBOARD.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            ErpDestination.entries.forEach { destination ->
                composable(destination.route) {
                    PlaceholderScreen(titleRes = destination.titleRes)
                }
            }
        }
    }
}
