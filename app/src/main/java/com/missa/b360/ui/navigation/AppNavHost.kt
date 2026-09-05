package com.missa.b360.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.missa.b360.core.data.entity.OperationDirection
import com.missa.b360.core.data.entity.OperationModule
import com.missa.b360.ui.admin.AdminAProposScreen
import com.missa.b360.ui.admin.AdminJournalScreen
import com.missa.b360.ui.admin.AdminLicenceScreen
import com.missa.b360.ui.admin.AdminReglagesScreen
import com.missa.b360.ui.admin.AdminSauvegardeScreen
import com.missa.b360.ui.admin.AdminSitesScreen
import com.missa.b360.ui.admin.AdminUtilisateursScreen
import com.missa.b360.ui.clients.ClientsScreen
import com.missa.b360.ui.fournisseurs.FournisseursScreen
import com.missa.b360.ui.home.HomeScreen
import com.missa.b360.ui.notifications.NotificationsScreen
import com.missa.b360.ui.operations.OperationModuleScreen
import com.missa.b360.ui.operations.ReportingScreen
import com.missa.b360.ui.sales.SalesScreen
import com.missa.b360.ui.stock.StockOpenAction
import com.missa.b360.ui.stock.StockScreen
import com.missa.b360.ui.onboarding.OnboardingScreen
import com.missa.b360.ui.onboarding.PinLockScreen

/** Hôte de navigation de l'application (RA-22 + démarrage Phase B). */
@Composable
fun AppNavHost() {
    val startup: StartupViewModel = hiltViewModel()
    val state by startup.state.collectAsState()

    when (state) {
        StartupState.Chargement -> Box(Modifier.fillMaxSize())

        // 1re ouverture : parcours d'onboarding complet (Phase B)
        StartupState.Onboarding -> OnboardingScreen(onFinished = startup::evaluer)

        // RA-01 : verrou PIN demandé à chaque ouverture
        StartupState.VerrouPin -> PinLockScreen(onUnlocked = startup::deverrouiller)

        // Accueil + modules métier
        StartupState.Pret -> MainNavHost()
    }
}

@Composable
private fun MainNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(navController)
        }
        composable(Routes.NOTIFICATIONS) {
            NotificationsScreen(onBack = { navController.popBackStack() })
        }

        // ☰ Administration & Paramétrage (module 9.1 — Phase C)
        composable(Routes.ADMIN_REGLAGES) {
            AdminReglagesScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.ADMIN_LICENCE) {
            AdminLicenceScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.ADMIN_SAUVEGARDE) {
            AdminSauvegardeScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.ADMIN_JOURNAL) {
            AdminJournalScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.ADMIN_UTILISATEURS) {
            AdminUtilisateursScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.ADMIN_MULTISITE) {
            AdminSitesScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.ADMIN_A_PROPOS) {
            AdminAProposScreen(onBack = { navController.popBackStack() })
        }

// Phase D — Clients & Fournisseurs (9.2/9.3)
        composable(
            route = "${AppModule.CLIENTS.route}?create={create}",
            arguments = listOf(
                navArgument("create") {
                    type = NavType.BoolType
                    defaultValue = false
                },
            ),
        ) { entry ->
            ClientsScreen(
                onBack = { navController.popBackStack() },
                openCreate = entry.arguments?.getBoolean("create") == true,
                onNavigate = { route -> navController.navigate(route) },
            )
        }
        composable(
            route = "${AppModule.FOURNISSEURS.route}?create={create}",
            arguments = listOf(
                navArgument("create") {
                    type = NavType.BoolType
                    defaultValue = false
                },
            ),
        ) { entry ->
            FournisseursScreen(
                onBack = { navController.popBackStack() },
                openCreate = entry.arguments?.getBoolean("create") == true,
            )
        }
        // Modules opérationnels : chacun a sa propre liste, création, validation et journalisation.
        composable(
            route = "${AppModule.STOCK.route}?create={create}&stock={stock}",
            arguments = listOf(
                navArgument("create") {
                    type = NavType.BoolType
                    defaultValue = false
                },
                navArgument("stock") {
                    type = NavType.StringType
                    defaultValue = ""
                },
            ),
        ) { entry ->
            StockScreen(
                onBack = { navController.popBackStack() },
                openCreate = entry.arguments?.getBoolean("create") == true,
                initialAction = StockOpenAction.parse(entry.arguments?.getString("stock")),
            )
        }
        composable(
            route = "${AppModule.VENTE.route}?create={create}",
            arguments = listOf(
                navArgument("create") {
                    type = NavType.BoolType
                    defaultValue = false
                },
            ),
        ) { entry ->
            SalesScreen(
                onNavigate = { route -> navController.navigate(route) },
                onOpenClientCreate = {
                    navController.navigate("${AppModule.CLIENTS.route}?create=true")
                },
                openCreate = entry.arguments?.getBoolean("create") == true,
            )
        }
        operationDestination(AppModule.ACHATS, OperationModule.ACHATS, navController)
        operationDestination(AppModule.FINANCES, OperationModule.FINANCES, navController)
        operationDestination(AppModule.LIVRAISON, OperationModule.LIVRAISON, navController)
        operationDestination(AppModule.PRODUCTION, OperationModule.PRODUCTION, navController)
        operationDestination(AppModule.SERVICES, OperationModule.SERVICES, navController)
        operationDestination(AppModule.RH, OperationModule.RH, navController)
        operationDestination(AppModule.PROJETS, OperationModule.PROJETS, navController)
        composable(AppModule.REPORTING.route) {
            ReportingScreen(onBack = { navController.popBackStack() })
        }
    }
}

/** Route commune aux opérations : le paramètre crée un document immédiatement si demandé. */
private fun NavGraphBuilder.operationDestination(
    appModule: AppModule,
    operationModule: OperationModule,
    navController: androidx.navigation.NavController,
) {
    composable(
        route = "${appModule.route}?create={create}&direction={direction}",
        arguments = listOf(
            navArgument("create") {
                type = NavType.BoolType
                defaultValue = false
            },
            navArgument("direction") {
                type = NavType.StringType
                defaultValue = "NONE"
            },
        ),
    ) { entry ->
        OperationModuleScreen(
            module = operationModule,
            onBack = { navController.popBackStack() },
            openCreate = entry.arguments?.getBoolean("create") == true,
            initialDirection = OperationDirection.entries.firstOrNull {
                it.name == entry.arguments?.getString("direction")
            } ?: OperationDirection.NONE,
        )
    }
}
