package com.missa.b360.ui.clients

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.navArgument
import com.missa.b360.ui.clients.account.ClientAccountScreen
import com.missa.b360.ui.clients.activity.ClientActivityScreen
import com.missa.b360.ui.clients.csvimport.ClientImportScreen
import com.missa.b360.ui.clients.detail.ClientDetailScreen
import com.missa.b360.ui.clients.followups.ClientFollowupsScreen
import com.missa.b360.ui.clients.form.ClientEditSheet
import com.missa.b360.ui.clients.form.ClientQuickCreateSheet
import com.missa.b360.ui.clients.list.ClientListScreen
import com.missa.b360.ui.clients.components.ClientCouleurs
import com.missa.b360.ui.components.LocalCouleurSelection
import com.missa.b360.ui.navigation.AppModule

private val ARGUMENT_ID = listOf(navArgument(ClientRoutes.ARG_ID) { type = NavType.LongType })

/**
 * Graphe du module Clients : un écran = une route, donc retour système, rotation et restauration
 * après arrêt du processus passent par la pile de navigation. [garde] applique la protection du
 * module (activation du profil) autour de chaque écran ; les feuilles sont des destinations `dialog`.
 */
fun NavGraphBuilder.clientsGraph(
    navController: NavController,
    garde: @Composable (contenu: @Composable () -> Unit) -> Unit,
) {
    // La sélection (listes, sélecteurs) est violette dans tout le module, sans toucher aux boutons d'action.
    val gardeViolette: @Composable (@Composable () -> Unit) -> Unit = { contenu ->
        garde { CompositionLocalProvider(LocalCouleurSelection provides ClientCouleurs.Violet, content = contenu) }
    }
    val retour: () -> Unit = { navController.popBackStack() }

    composable(
        route = ClientRoutes.LISTE,
        arguments = listOf(navArgument(ClientRoutes.ARG_CREATE) { type = NavType.BoolType; defaultValue = false }),
    ) { entree ->
        gardeViolette {
            ClientListScreen(
                onBack = retour,
                onOuvrirClient = { id -> navController.navigate(ClientRoutes.fiche(id)) },
                onNouveau = { navController.navigate(ClientRoutes.NOUVEAU) { launchSingleTop = true } },
                onImporter = { navController.navigate(ClientRoutes.IMPORT) { launchSingleTop = true } },
                onRelances = { navController.navigate(ClientRoutes.RELANCES) { launchSingleTop = true } },
                ouvrirCreation = entree.arguments?.getBoolean(ClientRoutes.ARG_CREATE) == true,
            )
        }
    }
    composable(route = ClientRoutes.FICHE, arguments = ARGUMENT_ID) {
        gardeViolette {
            ClientDetailScreen(
                onBack = retour,
                onModifier = { id -> navController.navigate(ClientRoutes.edition(id)) { launchSingleTop = true } },
                onCompte = { id -> navController.navigate(ClientRoutes.compte(id)) },
                onActivite = { id -> navController.navigate(ClientRoutes.activite(id)) },
                onVendre = { id -> navController.navigate("${AppModule.VENTE.route}?create=true&clientId=$id") },
            )
        }
    }
    composable(route = ClientRoutes.COMPTE, arguments = ARGUMENT_ID) {
        gardeViolette { ClientAccountScreen(onBack = retour) }
    }
    composable(route = ClientRoutes.ACTIVITE, arguments = ARGUMENT_ID) {
        gardeViolette { ClientActivityScreen(onBack = retour) }
    }
    composable(route = ClientRoutes.RELANCES) {
        gardeViolette {
            ClientFollowupsScreen(
                onBack = retour,
                onOuvrirClient = { id -> navController.navigate(ClientRoutes.fiche(id)) },
            )
        }
    }
    composable(route = ClientRoutes.IMPORT) {
        gardeViolette {
            ClientImportScreen(
                onBack = retour,
                onVoirListe = { navController.popBackStack(ClientRoutes.LISTE, inclusive = false) },
            )
        }
    }
    dialog(route = ClientRoutes.NOUVEAU, dialogProperties = DialogProperties(usePlatformDefaultWidth = false)) {
        gardeViolette {
            ClientQuickCreateSheet(
                onClose = retour,
                onCree = { id ->
                    navController.navigate(ClientRoutes.fiche(id)) { popUpTo(ClientRoutes.NOUVEAU) { inclusive = true } }
                },
                onOuvrirExistant = { id ->
                    navController.navigate(ClientRoutes.fiche(id)) { popUpTo(ClientRoutes.NOUVEAU) { inclusive = true } }
                },
            )
        }
    }
    dialog(
        route = ClientRoutes.EDITION,
        arguments = ARGUMENT_ID,
        dialogProperties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        gardeViolette { ClientEditSheet(onClose = retour, onSauve = retour) }
    }
}
