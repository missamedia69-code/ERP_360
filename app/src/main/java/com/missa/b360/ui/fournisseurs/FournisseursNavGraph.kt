package com.missa.b360.ui.fournisseurs

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.missa.b360.ui.fournisseurs.action.FournisseurActionScreen
import com.missa.b360.ui.fournisseurs.list.FournisseurListScreen

private val ARGUMENT_ID = listOf(navArgument(FournisseurRoutes.ARG_ID) { type = NavType.LongType })

/**
 * Graphe du module Fournisseurs. [garde] applique la protection du module (activation du profil)
 * autour de chaque écran.
 */
fun NavGraphBuilder.fournisseursGraph(
    navController: NavController,
    garde: @Composable (contenu: @Composable () -> Unit) -> Unit,
) {
    val retour: () -> Unit = { navController.popBackStack() }

    composable(
        route = FournisseurRoutes.LISTE,
        arguments = listOf(navArgument(FournisseurRoutes.ARG_CREATE) { type = NavType.BoolType; defaultValue = false }),
    ) { entree ->
        garde {
            FournisseurListScreen(
                onBack = retour,
                onOuvrirFournisseur = { id -> navController.navigate(FournisseurRoutes.fiche(id)) },
                onNouveau = { navController.navigate(FournisseurRoutes.creation()) { launchSingleTop = true } },
                onAction = { navController.navigate(FournisseurRoutes.ACTION) { launchSingleTop = true } },
                ouvrirCreation = entree.arguments?.getBoolean(FournisseurRoutes.ARG_CREATE) == true,
            )
        }
    }
    composable(route = FournisseurRoutes.ACTION) {
        garde {
            FournisseurActionScreen(
                onBack = retour,
                onOuvrirFournisseur = { id -> navController.navigate(FournisseurRoutes.fiche(id)) },
            )
        }
    }
    composable(route = FournisseurRoutes.FICHE, arguments = ARGUMENT_ID) { entree ->
        val id = entree.arguments?.getLong(FournisseurRoutes.ARG_ID) ?: 0L
        garde {
            FournisseurFicheRoute(
                id = id,
                onBack = retour,
                onModifier = { cible -> navController.navigate(FournisseurRoutes.edition(cible)) { launchSingleTop = true } },
            )
        }
    }
    composable(route = FournisseurRoutes.EDITION, arguments = ARGUMENT_ID) { entree ->
        val id = entree.arguments?.getLong(FournisseurRoutes.ARG_ID) ?: 0L
        garde {
            FournisseurEditionRoute(
                id = id,
                onBack = retour,
                onTermine = { enregistre ->
                    if (id == 0L && enregistre != null) {
                        // Création : la fiche remplace le formulaire dans la pile.
                        navController.navigate(FournisseurRoutes.fiche(enregistre)) {
                            popUpTo(FournisseurRoutes.EDITION) { inclusive = true }
                        }
                    } else {
                        navController.popBackStack()
                    }
                },
            )
        }
    }
}
