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
import com.missa.b360.ui.components.ModuleInactifScreen
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
import com.missa.b360.core.domain.model.ActivationProfil
import com.missa.b360.core.domain.model.ModuleCode
import com.missa.b360.ui.admin.AdminAProposScreen
import com.missa.b360.ui.admin.AdminJournalScreen
import com.missa.b360.ui.admin.AdminLicenceScreen
import com.missa.b360.ui.admin.AdminReglagesScreen
import com.missa.b360.ui.admin.AdminSauvegardeScreen
import com.missa.b360.ui.admin.AdminSitesScreen
import com.missa.b360.ui.admin.AdminUtilisateursScreen
import com.missa.b360.ui.admin.ReferentielsScreen
import com.missa.b360.ui.clients.ClientsPlaceholderScreen
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
import com.missa.b360.ui.stock.StockAlertesScreen
import com.missa.b360.ui.stock.StockCategoriesScreen
import com.missa.b360.ui.stock.StockDetailScreen
import com.missa.b360.ui.stock.StockEquipementsScreen
import com.missa.b360.ui.stock.StockMouvementsScreen
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

    if (!startup.introVue) {
        SplashVideoScreen(onFinished = startup::marquerIntroVue)
        return
    }

    when (state) {
        StartupState.Chargement -> Box(Modifier.fillMaxSize())
        StartupState.Onboarding -> OnboardingScreen(onFinished = startup::evaluer)
        StartupState.VerrouPin -> PinLockScreen(onUnlocked = startup::deverrouiller)
        StartupState.Pret -> MainNavHost()
    }
}

@Composable
private fun MainNavHost() {
    val navController = rememberNavController()
    val accueilViewModel: HomeViewModel = hiltViewModel()
    val activation by accueilViewModel.activation.collectAsState()
    val modulesEpingles by accueilViewModel.modulesEpingles.collectAsState()
    val routeCourante = navController.currentBackStackEntryAsState().value?.destination?.route
    var plusDeModules by remember { mutableStateOf(false) }
    var assistance by remember { mutableStateOf(false) }
    var ficheEntreprise by remember { mutableStateOf(false) }
    val ficheViewModel: com.missa.b360.ui.components.FicheEntrepriseViewModel = hiltViewModel()
    val ficheEtat by ficheViewModel.etat.collectAsState()
    val etatTiroir = rememberDrawerState(DrawerValue.Closed)
    val portee = rememberCoroutineScope()
    val etatAccueil by accueilViewModel.uiState.collectAsState()

    val barreDemandee = remember { mutableStateOf(true) }
    // Reset la demande de masquage à chaque changement de route : si un écran a masqué
    // la barre via LocalBarreNavigation, elle doit réapparaître en sortant.
    androidx.compose.runtime.LaunchedEffect(routeCourante) {
        barreDemandee.value = true
    }
    // Barre visible partout sauf sur les formulaires plein écran.
    // Important pour que le changement de profil dans Réglages se répercute
    // directement sur la barre du bas et le menu Plus sans retour arrière.
    val estFormulairePleinEcran = routeCourante?.let { r ->
        r.startsWith(Routes.STOCK_PRODUCT_FORM) ||
            r.startsWith(Routes.STOCK_MOVEMENT_FORM) ||
            r.startsWith(Routes.STOCK_TRANSFER_FORM) ||
            r.startsWith(Routes.OPERATION_FORM) ||
            r.startsWith(Routes.SALES_RETURN)
    } == true
    val afficherBarre = (
        !estFormulairePleinEcran && (
            AppModule.barreVisibleSur(routeCourante) ||
                routeCourante == Routes.HOME ||
                routeCourante?.startsWith("admin_") == true ||
                routeCourante == Routes.TASKS ||
                routeCourante == Routes.ADMIN_REFERENTIELS ||
                routeCourante == Routes.NOTIFICATIONS
            )
        ) && barreDemandee.value

    val nomEntreprise = etatAccueil.entrepriseNom.ifBlank {
        stringResource(R.string.home_company_placeholder)
    }
    val etatSauvegarde = etatAccueil.derniereSauvegarde?.let {
        stringResource(R.string.home_backup_date, DateUtils.formatDateHeure(it))
    } ?: stringResource(R.string.home_backup_never)

    val nonLues by accueilViewModel.notificationsNonLues.collectAsState(initial = 0)
    val isHome = routeCourante == Routes.HOME || routeCourante?.startsWith(Routes.HOME) == true

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
                onCompanyFiche = {
                    portee.launch { etatTiroir.close() }
                    ficheEntreprise = true
                },
                onSupport = {
                    portee.launch { etatTiroir.close() }
                    assistance = true
                },
            )
        },
    ) {
    Scaffold(
        containerColor = com.missa.b360.ui.theme.MissaCanvas,
        topBar = {
            // Spec: Header fixe 64dp + statusBars — global pour Home, autres écrans ont leur propre MissaTopAppBar 64dp
            // Pour éviter double header, on affiche MissaAppHeader seulement sur HOME
            if (isHome && !estFormulairePleinEcran) {
                com.missa.b360.ui.components.MissaAppHeader(
                    companyLogoUri = etatAccueil.entrepriseLogoUri,
                    notificationCount = nonLues,
                    isHome = true,
                    onMenuClick = { portee.launch { etatTiroir.open() } },
                    onBackClick = { navController.popBackStack() },
                    onNotificationClick = { navController.navigate(Routes.NOTIFICATIONS) },
                    onProfileClick = { ficheEntreprise = true },
                )
            }
        },
        bottomBar = {
            if (afficherBarre) {
                MissaBarreModules(
                    modules = AppModule.barreBas(activation, modulesEpingles),
                    routeCourante = routeCourante,
                    onAccueil = { navController.naviguerVers(Routes.HOME) },
                    onModule = { navController.naviguerVers(it.route) },
                    onPlus = { plusDeModules = true },
                )
            }
        },
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
    ) { padding ->
    // Spec: Content entre Header 64dp et BottomNav 80dp, scrollable seul, respecte WindowInsets
    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
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

        // Administration
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

        // Clients & Fournisseurs
        composable(
            route = "${AppModule.CLIENTS.route}?create={create}",
            arguments = listOf(navArgument("create") { type = NavType.BoolType; defaultValue = false }),
        ) { entry ->
            GuardedModule(AppModule.CLIENTS, activation, navController) {
                ClientsPlaceholderScreen(
                    onBack = { navController.popBackStack() },
                    openCreate = entry.arguments?.getBoolean("create") == true,
                    onNavigate = { route -> navController.navigate(route) },
                )
            }
        }
        composable(
            route = "${AppModule.FOURNISSEURS.route}?create={create}",
            arguments = listOf(navArgument("create") { type = NavType.BoolType; defaultValue = false }),
        ) { entry ->
            GuardedModule(AppModule.FOURNISSEURS, activation, navController) {
                FournisseursScreen(
                    onBack = { navController.popBackStack() },
                    openCreate = entry.arguments?.getBoolean("create") == true,
                )
            }
        }
        // Stock
        composable(
            route = "${AppModule.STOCK.route}?create={create}&direction={direction}",
            arguments = listOf(
                navArgument("create") { type = NavType.BoolType; defaultValue = false },
                navArgument("direction") { type = NavType.StringType; defaultValue = "NONE" },
            ),
        ) {
            GuardedModule(AppModule.STOCK, activation, navController) {
                StockAccueilScreen(
                    onBack = { navController.popBackStack() },
                    onNaviguer = { route -> navController.naviguerVers(route) },
                )
            }
        }
        composable(Routes.STOCK_ARTICLES) {
            GuardedModule(AppModule.STOCK, activation, navController) {
                StockScreen(
                    onBack = { navController.popBackStack() },
                    onNavigate = { route -> navController.navigate(route) },
                    initialMovement = null,
                )
            }
        }

        composable(Routes.STOCK_CATEGORIES) {
            GuardedModule(AppModule.STOCK, activation, navController) {
                StockCategoriesScreen(
                    onBack = { navController.popBackStack() },
                    onNaviguer = { route -> navController.naviguerVers(route) },
                )
            }
        }
        composable(
            route = "${Routes.STOCK_LISTE}?type={type}",
            arguments = listOf(navArgument("type") { type = NavType.StringType; nullable = true; defaultValue = null }),
        ) {
            GuardedModule(AppModule.STOCK, activation, navController) {
                StockScreen(
                    onBack = { navController.popBackStack() },
                    onNavigate = { route -> navController.navigate(route) },
                    initialMovement = null,
                )
            }
        }
        composable(
            route = Routes.STOCK_DETAIL,
            arguments = listOf(navArgument("id") { type = NavType.LongType }),
        ) {
            GuardedModule(AppModule.STOCK, activation, navController) {
                StockDetailScreen(
                    onBack = { navController.popBackStack() },
                    onNavigate = { route -> navController.navigate(route) },
                )
            }
        }
        composable(Routes.STOCK_EQUIPEMENTS) {
            GuardedModule(AppModule.STOCK, activation, navController) {
                StockEquipementsScreen(
                    onBack = { navController.popBackStack() },
                    onNavigate = { route -> navController.navigate(route) },
                )
            }
        }
        composable(Routes.STOCK_MOUVEMENTS) {
            GuardedModule(AppModule.STOCK, activation, navController) {
                StockMouvementsScreen(
                    onBack = { navController.popBackStack() },
                    onNavigate = { route -> navController.navigate(route) },
                )
            }
        }
        composable(Routes.STOCK_ALERTES) {
            GuardedModule(AppModule.STOCK, activation, navController) {
                StockAlertesScreen(
                    onBack = { navController.popBackStack() },
                    onNaviguer = { route -> navController.naviguerVers(route) },
                )
            }
        }
        composable(
            route = "${Routes.STOCK_PRODUCT_FORM}?productId={productId}",
            arguments = listOf(navArgument("productId") { type = NavType.LongType; defaultValue = 0L }),
        ) { entry ->
            GuardedModule(AppModule.STOCK, activation, navController) {
                ProductFormScreen(
                    onBack = { navController.popBackStack() },
                    productId = entry.arguments?.getLong("productId")?.takeIf { it > 0L },
                )
            }
        }
        composable(
            route = "${Routes.STOCK_MOVEMENT_FORM}?type={type}",
            arguments = listOf(navArgument("type") { type = NavType.StringType; defaultValue = "ENTREE" }),
        ) { entry ->
            GuardedModule(AppModule.STOCK, activation, navController) {
                StockMovementFormScreen(
                    onBack = { navController.popBackStack() },
                    initialDirection = runCatching {
                        StockMovementType.valueOf(entry.arguments?.getString("type") ?: "ENTREE")
                    }.getOrDefault(StockMovementType.ENTREE),
                    onOpenTransfer = { navController.navigate(Routes.STOCK_TRANSFER_FORM) },
                )
            }
        }
        composable(Routes.STOCK_TRANSFER_FORM) {
            GuardedModule(AppModule.STOCK, activation, navController) {
                StockTransferFormScreen(onBack = { navController.popBackStack() })
            }
        }
        composable(Routes.STOCK_INVENTORY) {
            GuardedModule(AppModule.STOCK, activation, navController) {
                InventoryScreen(onBack = { navController.popBackStack() })
            }
        }
        // RH
        composable(
            route = "${AppModule.RH.route}?create={create}&direction={direction}",
            arguments = listOf(
                navArgument("create") { type = NavType.BoolType; defaultValue = false },
                navArgument("direction") { type = NavType.StringType; defaultValue = "NONE" },
            ),
        ) { entry ->
            GuardedModule(AppModule.RH, activation, navController) {
                RhScreen(
                    onBack = { navController.popBackStack() },
                    openCreate = entry.arguments?.getBoolean("create") == true,
                )
            }
        }
        // Production
        composable(
            route = "${AppModule.PRODUCTION.route}?create={create}&direction={direction}",
            arguments = listOf(
                navArgument("create") { type = NavType.BoolType; defaultValue = false },
                navArgument("direction") { type = NavType.StringType; defaultValue = "NONE" },
            ),
        ) { entry ->
            GuardedModule(AppModule.PRODUCTION, activation, navController) {
                ProductionScreen(
                    onBack = { navController.popBackStack() },
                    openCreate = entry.arguments?.getBoolean("create") == true,
                )
            }
        }
        // Vente
        composable(
            route = "${AppModule.VENTE.route}?create={create}",
            arguments = listOf(navArgument("create") { type = NavType.BoolType; defaultValue = false }),
        ) { entry ->
            GuardedModule(AppModule.VENTE, activation, navController) {
                SalesScreen(
                    onNavigate = { route -> navController.navigate(route) },
                    onOpenClientCreate = { navController.navigate("${AppModule.CLIENTS.route}?create=true") },
                    openCreate = entry.arguments?.getBoolean("create") == true,
                )
            }
        }
        // Achats
        composable(
            route = "${AppModule.ACHATS.route}?create={create}",
            arguments = listOf(navArgument("create") { type = NavType.BoolType; defaultValue = false }),
        ) { entry ->
            GuardedModule(AppModule.ACHATS, activation, navController) {
                PurchasesScreen(
                    onBack = { navController.popBackStack() },
                    openCreate = entry.arguments?.getBoolean("create") == true,
                )
            }
        }
        // Devis & commandes
        composable(Routes.DEVIS_COMMANDE) {
            // Devis appartient à VEN
            GuardedModule(AppModule.VENTE, activation, navController) {
                DevisCommandeScreen(onBack = { navController.popBackStack() })
            }
        }
        // Retour de vente
        composable(
            route = "${Routes.SALES_RETURN}?recordId={recordId}",
            arguments = listOf(navArgument("recordId") { type = NavType.LongType; defaultValue = 0L }),
        ) { entry ->
            GuardedModule(AppModule.VENTE, activation, navController) {
                ReturnSaleScreen(
                    onBack = { navController.popBackStack() },
                    recordId = entry.arguments?.getLong("recordId")?.takeIf { it > 0L },
                )
            }
        }
        operationDestination(AppModule.FINANCES, OperationModule.FINANCES, navController, activation)
        composable(
            route = "${AppModule.LIVRAISON.route}?create={create}",
            arguments = listOf(navArgument("create") { type = NavType.BoolType; defaultValue = false }),
        ) { entry ->
            GuardedModule(AppModule.LIVRAISON, activation, navController) {
                LivraisonScreen(
                    onBack = { navController.popBackStack() },
                    onNaviguer = { route -> navController.naviguerVers(route) },
                    openCreate = entry.arguments?.getBoolean("create") == true,
                )
            }
        }
        composable(
            route = "${AppModule.SERVICES.route}?create={create}",
            arguments = listOf(navArgument("create") { type = NavType.BoolType; defaultValue = false }),
        ) { entry ->
            GuardedModule(AppModule.SERVICES, activation, navController) {
                ServicesScreen(
                    onBack = { navController.popBackStack() },
                    onNaviguer = { route -> navController.naviguerVers(route) },
                    openCreate = entry.arguments?.getBoolean("create") == true,
                )
            }
        }
        composable(
            route = "${AppModule.PROJETS.route}?create={create}",
            arguments = listOf(navArgument("create") { type = NavType.BoolType; defaultValue = false }),
        ) { entry ->
            GuardedModule(AppModule.PROJETS, activation, navController) {
                ProjetsScreen(
                    onBack = { navController.popBackStack() },
                    onNaviguer = { route -> navController.naviguerVers(route) },
                    openCreate = entry.arguments?.getBoolean("create") == true,
                )
            }
        }
        composable(AppModule.COMPTABILITE.route) {
            GuardedModule(AppModule.COMPTABILITE, activation, navController) {
                ComptabiliteScreen(
                    onBack = { navController.popBackStack() },
                    onNaviguer = { route -> navController.naviguerVers(route) },
                )
            }
        }
        composable(AppModule.TRESORERIE.route) {
            GuardedModule(AppModule.TRESORERIE, activation, navController) {
                TresorerieScreen(
                    onBack = { navController.popBackStack() },
                    onNaviguer = { route -> navController.naviguerVers(route) },
                )
            }
        }
        composable(AppModule.CRM.route) {
            GuardedModule(AppModule.CRM, activation, navController) {
                CrmScreen(
                    onBack = { navController.popBackStack() },
                    onNaviguer = { route -> navController.naviguerVers(route) },
                )
            }
        }
        composable(AppModule.QUALITE.route) {
            GuardedModule(AppModule.QUALITE, activation, navController) {
                QualiteScreen(
                    onBack = { navController.popBackStack() },
                    onNaviguer = { route -> navController.naviguerVers(route) },
                )
            }
        }
        composable(AppModule.MAINTENANCE.route) {
            GuardedModule(AppModule.MAINTENANCE, activation, navController) {
                MaintenanceScreen(
                    onBack = { navController.popBackStack() },
                    onNaviguer = { route -> navController.naviguerVers(route) },
                )
            }
        }
        composable(AppModule.LOGISTIQUE.route) {
            GuardedModule(AppModule.LOGISTIQUE, activation, navController) {
                LogistiqueScreen(
                    onBack = { navController.popBackStack() },
                    onNaviguer = { route -> navController.naviguerVers(route) },
                )
            }
        }
        composable(Routes.ADMIN_REFERENTIELS) {
            ReferentielsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.TASKS) {
            TasksScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = "${Routes.OPERATION_FORM}?module={module}&direction={direction}",
            arguments = listOf(
                navArgument("module") { type = NavType.StringType },
                navArgument("direction") { type = NavType.StringType; defaultValue = "NONE" },
            ),
        ) { entry ->
            val module = runCatching {
                OperationModule.valueOf(entry.arguments?.getString("module").orEmpty())
            }.getOrNull()
            if (module != null) {
                // Guard par module opérationnel
                val appModule = when (module) {
                    OperationModule.VENTE -> AppModule.VENTE
                    OperationModule.ACHATS -> AppModule.ACHATS
                    OperationModule.STOCK -> AppModule.STOCK
                    OperationModule.FINANCES -> AppModule.FINANCES
                    else -> null
                }
                if (appModule != null && activation.modulesActifs.isNotEmpty() && !activation.isModuleActif(appModule.moduleCode)) {
                    ModuleInactifScreen(module = appModule, activation = activation, onBack = { navController.popBackStack() }, onActiver = { navController.naviguerVers(Routes.ADMIN_REGLAGES) })
                } else {
                    OperationFormScreen(
                        module = module,
                        initialDirection = OperationDirection.entries.firstOrNull {
                            it.name == entry.arguments?.getString("direction")
                        } ?: OperationDirection.NONE,
                        onBack = { navController.popBackStack() },
                    )
                }
            }
        }
        composable(AppModule.REPORTING.route) {
            GuardedModule(AppModule.REPORTING, activation, navController) {
                ReportingScreen(onBack = { navController.popBackStack() })
            }
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

    if (ficheEntreprise) {
        com.missa.b360.ui.components.FicheEntrepriseDialog(
            etat = ficheEtat,
            onDismiss = { ficheEntreprise = false },
        )
    }

    if (plusDeModules) {
        PlusDeModulesFeuille(
            modules = AppModule.secondaires(activation, modulesEpingles),
            onFermer = { plusDeModules = false },
            onModule = { module ->
                plusDeModules = false
                navController.naviguerVers(module.route)
            },
        )
    }
}

@Composable
private fun GuardedModule(
    appModule: AppModule,
    activation: ActivationProfil,
    navController: NavController,
    content: @Composable () -> Unit,
) {
    if (activation.modulesActifs.isEmpty() || activation.isModuleActif(appModule.moduleCode)) {
        content()
    } else {
        ModuleInactifScreen(
            module = appModule,
            activation = activation,
            onBack = { navController.popBackStack() },
            onActiver = { navController.naviguerVers(Routes.ADMIN_REGLAGES) },
        )
    }
}

/** Route commune aux opérations : le paramètre crée un document immédiatement si demandé. */
private fun NavGraphBuilder.operationDestination(
    appModule: AppModule,
    operationModule: OperationModule,
    navController: NavController,
    activation: ActivationProfil,
) {
    composable(
        route = "${appModule.route}?create={create}&direction={direction}",
        arguments = listOf(
            navArgument("create") { type = NavType.BoolType; defaultValue = false },
            navArgument("direction") { type = NavType.StringType; defaultValue = "NONE" },
        ),
    ) { entry ->
        GuardedModule(appModule, activation, navController) {
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
}

private fun NavController.naviguerVers(route: String) {
    navigate(route) {
        popUpTo(Routes.HOME) { inclusive = route == Routes.HOME }
        launchSingleTop = true
        restoreState = true
    }
}

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
