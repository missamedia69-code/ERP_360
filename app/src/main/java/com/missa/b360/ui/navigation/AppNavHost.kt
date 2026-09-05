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
import com.missa.b360.core.data.entity.StockMovementType
import com.missa.b360.ui.admin.AdminAProposScreen
import com.missa.b360.ui.admin.AdminJournalScreen
import com.missa.b360.ui.admin.AdminLicenceScreen
import com.missa.b360.ui.admin.AdminReglagesScreen
import com.missa.b360.ui.admin.AdminSauvegardeScreen
import com.missa.b360.ui.admin.AdminSitesScreen
import com.missa.b360.ui.admin.AdminUtilisateursScreen
import com.missa.b360.ui.clients.ClientsScreen
import com.missa.b360.ui.fournisseurs.FournisseursScreen
import com.missa.b360.ui.purchases.PurchasesScreen
import com.missa.b360.ui.home.HomeScreen
import com.missa.b360.ui.notifications.NotificationsScreen
import com.missa.b360.ui.operations.OperationFormScreen
import com.missa.b360.ui.operations.OperationModuleScreen
import com.missa.b360.ui.operations.ReportingScreen
import com.missa.b360.ui.sales.ReturnSaleScreen
import com.missa.b360.ui.sales.SalesScreen
import com.missa.b360.ui.stock.InventoryScreen
import com.missa.b360.ui.stock.ProductFormScreen
import com.missa.b360.ui.stock.StockMovementFormScreen
import com.missa.b360.ui.stock.StockScreen
import com.missa.b360.ui.stock.StockTransferFormScreen
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
        // Phase E — Module Stock : produits, mouvements et transferts (spec §7/§11/§13).
        composable(
            route = "${AppModule.STOCK.route}?create={create}&direction={direction}",
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
            val direction = OperationDirection.entries.firstOrNull {
                it.name == entry.arguments?.getString("direction")
            } ?: OperationDirection.NONE
            StockScreen(
                onBack = { navController.popBackStack() },
                onNavigate = { route -> navController.navigate(route) },
                initialMovement = if (entry.arguments?.getBoolean("create") == true) {
                    if (direction == OperationDirection.OUT) StockMovementType.SORTIE else StockMovementType.ENTREE
                } else {
                    null
                },
            )
        }
        composable(
            route = "${Routes.STOCK_PRODUCT_FORM}?productId={productId}",
            arguments = listOf(
                navArgument("productId") {
                    type = NavType.LongType
                    defaultValue = 0L
                },
            ),
        ) { entry ->
            ProductFormScreen(
                onBack = { navController.popBackStack() },
                productId = entry.arguments?.getLong("productId")?.takeIf { it > 0L },
            )
        }
        composable(
            route = "${Routes.STOCK_MOVEMENT_FORM}?type={type}",
            arguments = listOf(
                navArgument("type") {
                    type = NavType.StringType
                    defaultValue = "ENTREE"
                },
            ),
        ) { entry ->
            StockMovementFormScreen(
                onBack = { navController.popBackStack() },
                initialDirection = runCatching {
                    StockMovementType.valueOf(entry.arguments?.getString("type") ?: "ENTREE")
                }.getOrDefault(StockMovementType.ENTREE),
                onOpenTransfer = { navController.navigate(Routes.STOCK_TRANSFER_FORM) },
            )
        }
        composable(Routes.STOCK_TRANSFER_FORM) {
            StockTransferFormScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.STOCK_INVENTORY) {
            InventoryScreen(onBack = { navController.popBackStack() })
        }
        // Modules opérationnels : chacun a sa propre liste, création, validation et journalisation.
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
        // Achat — écran dédié (spec §6) : facture fournisseur, réception de stock et passif.
        composable(
            route = "${AppModule.ACHATS.route}?create={create}",
            arguments = listOf(
                navArgument("create") {
                    type = NavType.BoolType
                    defaultValue = false
                },
            ),
        ) { entry ->
            PurchasesScreen(
                onBack = { navController.popBackStack() },
                openCreate = entry.arguments?.getBoolean("create") == true,
            )
        }
        // Retour de vente + avoir (spec §22) — recordId optionnel : 0 = liste des factures retournables.
        composable(
            route = "${Routes.SALES_RETURN}?recordId={recordId}",
            arguments = listOf(
                navArgument("recordId") {
                    type = NavType.LongType
                    defaultValue = 0L
                },
            ),
        ) { entry ->
            ReturnSaleScreen(
                onBack = { navController.popBackStack() },
                recordId = entry.arguments?.getLong("recordId")?.takeIf { it > 0L },
            )
        }
        operationDestination(AppModule.FINANCES, OperationModule.FINANCES, navController)
        operationDestination(AppModule.LIVRAISON, OperationModule.LIVRAISON, navController)
        operationDestination(AppModule.PRODUCTION, OperationModule.PRODUCTION, navController)
        operationDestination(AppModule.SERVICES, OperationModule.SERVICES, navController)
        operationDestination(AppModule.RH, OperationModule.RH, navController)
        operationDestination(AppModule.PROJETS, OperationModule.PROJETS, navController)
        // Formulaire d'opération — page dédiée unique (spec §3.2) : [Retour | Titre] ... [Annuler][Enregistrer].
        composable(
            route = "${Routes.OPERATION_FORM}?module={module}&direction={direction}",
            arguments = listOf(
                navArgument("module") {
                    type = NavType.StringType
                },
                navArgument("direction") {
                    type = NavType.StringType
                    defaultValue = "NONE"
                },
            ),
        ) { entry ->
            val module = runCatching {
                OperationModule.valueOf(entry.arguments?.getString("module").orEmpty())
            }.getOrNull()
            if (module != null) {
                OperationFormScreen(
                    module = module,
                    initialDirection = OperationDirection.entries.firstOrNull {
                        it.name == entry.arguments?.getString("direction")
                    } ?: OperationDirection.NONE,
                    onBack = { navController.popBackStack() },
                )
            }
        }
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
            onNavigate = { route -> navController.navigate(route) },
            openCreate = entry.arguments?.getBoolean("create") == true,
            initialDirection = OperationDirection.entries.firstOrNull {
                it.name == entry.arguments?.getString("direction")
            } ?: OperationDirection.NONE,
        )
    }
}
