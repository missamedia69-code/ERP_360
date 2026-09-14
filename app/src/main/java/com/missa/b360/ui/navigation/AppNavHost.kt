package com.missa.b360.ui.navigation

import androidx.compose.runtime.CompositionLocalProvider
import com.missa.b360.ui.components.LocalBarreNavigation
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.rememberCoroutineScope
import com.missa.b360.core.util.DateUtils
import com.missa.b360.ui.home.HomeSupportDialogue
import com.missa.b360.ui.home.MissaBusinessDrawer
import kotlinx.coroutines.launch
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.missa.b360.R
import com.missa.b360.ui.components.MissaBarreModules
import com.missa.b360.ui.home.HomeViewModel
import com.missa.b360.ui.theme.BrandBlue
import com.missa.b360.ui.theme.MissaMuted
import com.missa.b360.ui.theme.MissaSoftBlue
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
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
import com.missa.b360.ui.admin.ReferentielsScreen
import com.missa.b360.ui.clients.ClientsScreen
import com.missa.b360.ui.comptabilite.ComptabiliteScreen
import com.missa.b360.ui.crm.CrmScreen
import com.missa.b360.ui.fournisseurs.FournisseursScreen
import com.missa.b360.ui.home.HomeScreen
import com.missa.b360.ui.livraison.LivraisonScreen
import com.missa.b360.ui.logistique.LogistiqueScreen
import com.missa.b360.ui.maintenance.MaintenanceScreen
import com.missa.b360.ui.notifications.NotificationsScreen
import com.missa.b360.ui.onboarding.OnboardingScreen
import com.missa.b360.ui.onboarding.PinLockScreen
import com.missa.b360.ui.operations.OperationFormScreen
import com.missa.b360.ui.operations.OperationModuleScreen
import com.missa.b360.ui.operations.ReportingScreen
import com.missa.b360.ui.production.ProductionScreen
import com.missa.b360.ui.purchases.PurchasesScreen
import com.missa.b360.ui.projets.ProjetsScreen
import com.missa.b360.ui.qualite.QualiteScreen
import com.missa.b360.ui.services.ServicesScreen
import com.missa.b360.ui.rh.RhScreen
import com.missa.b360.ui.sales.DevisCommandeScreen
import com.missa.b360.ui.sales.ReturnSaleScreen
import com.missa.b360.ui.sales.SalesScreen
import com.missa.b360.ui.stock.InventoryScreen
import com.missa.b360.ui.stock.ProductFormScreen
import com.missa.b360.ui.stock.StockMovementFormScreen
import com.missa.b360.ui.stock.StockAccueilScreen
import com.missa.b360.ui.stock.StockScreen
import com.missa.b360.ui.stock.StockTransferFormScreen
import com.missa.b360.ui.screens.SplashVideoScreen
import com.missa.b360.ui.tasks.TasksScreen
import com.missa.b360.ui.tresorerie.TresorerieScreen

/** Hôte de navigation de l'application (RA-22 + démarrage Phase B). */
@Composable
fun AppNavHost() {
    val startup: StartupViewModel = hiltViewModel()
    val state by startup.state.collectAsState()

    // L'introduction de marque ouvre l'application, une fois par lancement.
    // Son drapeau vit dans le ViewModel : un changement de langue recrée
    // l'activité, et la vidéo se rejouait — c'était le bref écran noir.
    if (!startup.introVue) {
        SplashVideoScreen(onFinished = startup::marquerIntroVue)
        return
    }

    when (state) {
        StartupState.Chargement -> Box(Modifier.fillMaxSize())

        // 1re ouverture : parcours d'onboarding, à partir de l'écran bleu.
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
    val accueilViewModel: HomeViewModel = hiltViewModel()
    val modulesActifs by accueilViewModel.modulesActifs.collectAsState()
    val modulesEpingles by accueilViewModel.modulesEpingles.collectAsState()
    val routeCourante = navController.currentBackStackEntryAsState().value?.destination?.route
    var plusDeModules by remember { mutableStateOf(false) }
    var assistance by remember { mutableStateOf(false) }
    val etatTiroir = rememberDrawerState(DrawerValue.Closed)
    val portee = rememberCoroutineScope()
    val etatAccueil by accueilViewModel.uiState.collectAsState()

    // Barre unique, déclarée ici et nulle part ailleurs. L'accueil en avait sa
    // propre copie : selon l'écran, on en voyait une, deux superposées, ou
    // aucune.
    // Un écran peut réclamer tout le bas de l'écran le temps d'une saisie.
    val barreDemandee = remember { mutableStateOf(true) }
    val afficherBarre = (
        AppModule.barreVisibleSur(routeCourante) || routeCourante == Routes.HOME
        ) && barreDemandee.value

    val nomEntreprise = etatAccueil.entrepriseNom.ifBlank {
        stringResource(R.string.home_company_placeholder)
    }
    val etatSauvegarde = etatAccueil.derniereSauvegarde?.let {
        stringResource(R.string.home_backup_date, DateUtils.formatDateHeure(it))
    } ?: stringResource(R.string.home_backup_never)

    CompositionLocalProvider(LocalBarreNavigation provides barreDemandee) {
    ModalNavigationDrawer(
        drawerState = etatTiroir,
        drawerContent = {
            MissaBusinessDrawer(
                companyName = nomEntreprise,
                logoUri = etatAccueil.entrepriseLogoUri,
                backupStatus = etatSauvegarde,
                currentRoute = routeCourante,
                onClose = { portee.launch { etatTiroir.close() } },
                onNavigate = { route ->
                    portee.launch { etatTiroir.close() }
                    navController.navigate(route)
                },
                onSupport = {
                    portee.launch { etatTiroir.close() }
                    assistance = true
                },
            )
        },
    ) {
    Scaffold(
        bottomBar = {
            if (afficherBarre) {
                MissaBarreModules(
                    modules = AppModule.barreBas(modulesActifs, modulesEpingles),
                    routeCourante = routeCourante,
                    onAccueil = { navController.naviguerVers(Routes.HOME) },
                    onModule = { navController.naviguerVers(it.route) },
                    onPlus = { plusDeModules = true },
                )
            }
        },
    ) { padding ->
    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        modifier = Modifier.padding(bottom = padding.calculateBottomPadding()),
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                navController = navController,
                onOuvrirMenu = { portee.launch { etatTiroir.open() } },
            )
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
        ) {
            StockAccueilScreen(
                onBack = { navController.popBackStack() },
                onNaviguer = { route -> navController.navigate(route) },
            )
        }
        composable(Routes.STOCK_ARTICLES) {
            StockScreen(
                onBack = { navController.popBackStack() },
                onNavigate = { route -> navController.navigate(route) },
                initialMovement = null,
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
        // RH (spec §RH/§Paie) — écran dédié : employés, absences, paie, avances.
        composable(
            route = "${AppModule.RH.route}?create={create}&direction={direction}",
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
            RhScreen(
                onBack = { navController.popBackStack() },
                openCreate = entry.arguments?.getBoolean("create") == true,
            )
        }
        // Production (spec §Production) — écran dédié : ordres de production (OP).
        composable(
            route = "${AppModule.PRODUCTION.route}?create={create}&direction={direction}",
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
            ProductionScreen(
                onBack = { navController.popBackStack() },
                openCreate = entry.arguments?.getBoolean("create") == true,
            )
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
        // Devis & commandes (spec §20) — cycle commercial avant facturation.
        composable(Routes.DEVIS_COMMANDE) {
            DevisCommandeScreen(onBack = { navController.popBackStack() })
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
        composable(
            route = "${'$'}{AppModule.LIVRAISON.route}?create={create}",
            arguments = listOf(
                navArgument("create") {
                    type = NavType.BoolType
                    defaultValue = false
                },
            ),
        ) { entry ->
            LivraisonScreen(
                onBack = { navController.popBackStack() },
                onNaviguer = { route -> navController.naviguerVers(route) },
                openCreate = entry.arguments?.getBoolean("create") == true,
            )
        }
        composable(
            route = "${'$'}{AppModule.SERVICES.route}?create={create}",
            arguments = listOf(
                navArgument("create") {
                    type = NavType.BoolType
                    defaultValue = false
                },
            ),
        ) { entry ->
            ServicesScreen(
                onBack = { navController.popBackStack() },
                onNaviguer = { route -> navController.naviguerVers(route) },
                openCreate = entry.arguments?.getBoolean("create") == true,
            )
        }
        composable(
            route = "${'$'}{AppModule.PROJETS.route}?create={create}",
            arguments = listOf(
                navArgument("create") {
                    type = NavType.BoolType
                    defaultValue = false
                },
            ),
        ) { entry ->
            ProjetsScreen(
                onBack = { navController.popBackStack() },
                onNaviguer = { route -> navController.naviguerVers(route) },
                openCreate = entry.arguments?.getBoolean("create") == true,
            )
        }
        // Nouveaux modules (structure ERP 360 complète)
        composable(AppModule.COMPTABILITE.route) {
            ComptabiliteScreen(
                onBack = { navController.popBackStack() },
                onNaviguer = { route -> navController.naviguerVers(route) },
            )
        }
        composable(AppModule.TRESORERIE.route) {
            TresorerieScreen(
                onBack = { navController.popBackStack() },
                onNaviguer = { route -> navController.naviguerVers(route) },
            )
        }
        composable(AppModule.CRM.route) {
            CrmScreen(
                onBack = { navController.popBackStack() },
                onNaviguer = { route -> navController.naviguerVers(route) },
            )
        }
        composable(AppModule.QUALITE.route) {
            QualiteScreen(
                onBack = { navController.popBackStack() },
                onNaviguer = { route -> navController.naviguerVers(route) },
            )
        }
        composable(AppModule.MAINTENANCE.route) {
            MaintenanceScreen(
                onBack = { navController.popBackStack() },
                onNaviguer = { route -> navController.naviguerVers(route) },
            )
        }
        composable(AppModule.LOGISTIQUE.route) {
            LogistiqueScreen(
                onBack = { navController.popBackStack() },
                onNaviguer = { route -> navController.naviguerVers(route) },
            )
        }
        // Référentiels (spec §30) — moyens de paiement, taxes, unités.
        composable(Routes.ADMIN_REFERENTIELS) {
            ReferentielsScreen(onBack = { navController.popBackStack() })
        }
        // Tâches de suivi (spec §Tâches).
        composable(Routes.TASKS) {
            TasksScreen(onBack = { navController.popBackStack() })
        }
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

    }
    }

    if (assistance) {
        HomeSupportDialogue(
            entrepriseNom = etatAccueil.entrepriseNom,
            onFermer = { assistance = false },
        )
    }

    if (plusDeModules) {
        PlusDeModulesFeuille(
            modules = AppModule.secondaires(modulesActifs, modulesEpingles),
            onFermer = { plusDeModules = false },
            onModule = { module ->
                plusDeModules = false
                navController.naviguerVers(module.route)
            },
        )
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

/**
 * Navigation par onglet : sans ces garde-fous, chaque appui empile un écran de
 * plus et le retour arrière devient interminable.
 */
private fun NavController.naviguerVers(route: String) {
    navigate(route) {
        popUpTo(Routes.HOME) { inclusive = route == Routes.HOME }
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * Liste des modules hors barre, ouverte par le bouton « Plus ».
 *
 * Elle est ici, au niveau du graphe, et non dans l'accueil : sinon changer de
 * module depuis un module obligerait à repasser par l'accueil.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlusDeModulesFeuille(
    modules: List<AppModule>,
    onFermer: () -> Unit,
    onModule: (AppModule) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onFermer) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 18.dp, bottom = 24.dp),
        ) {
            Text(
                text = stringResource(R.string.more_modules),
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.home_more_modules_description),
                fontSize = 12.5.sp,
                color = MissaMuted,
            )
            Spacer(Modifier.height(12.dp))
            modules.forEach { module ->
                ListItem(
                    headlineContent = {
                        Text(stringResource(module.titleRes), fontWeight = FontWeight.SemiBold)
                    },
                    leadingContent = {
                        Surface(
                            modifier = Modifier.size(42.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = MissaSoftBlue,
                        ) {
                            Icon(
                                imageVector = module.icon,
                                contentDescription = null,
                                tint = BrandBlue,
                                modifier = Modifier.padding(10.dp),
                            )
                        }
                    },
                    modifier = Modifier.clickable { onModule(module) },
                )
            }
        }
    }
}
