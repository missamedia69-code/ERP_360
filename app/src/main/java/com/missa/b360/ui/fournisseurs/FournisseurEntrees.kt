package com.missa.b360.ui.fournisseurs

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import com.missa.b360.ui.fournisseurs.dossier.DossierFournisseurEcran
import com.missa.b360.ui.fournisseurs.form.FormulaireFournisseur

/**
 * Route `fournisseur_dossier/{id}` : dossier complet (contacts, articles, comptes, documents, journal, statuts).
 * Chaque destination a son propre ViewModel : la fiche est ouverte à l'entrée de la route.
 */
@Composable
fun FournisseurDossierRoute(
    id: Long,
    onBack: () -> Unit,
    onModifier: (Long) -> Unit,
) {
    val vm: FournisseursViewModel = hiltViewModel()
    LaunchedEffect(id) { vm.ouvrirFiche(id) }
    DossierFournisseurEcran(vm = vm, onBack = onBack, onModifier = { fournisseur -> onModifier(fournisseur.id) })
}

/**
 * Route `fournisseur_edition/{id}` : formulaire en 7 étapes ; [id] 0 = création.
 * [onTermine] reçoit l'identifiant du fournisseur enregistré.
 */
@Composable
fun FournisseurEditionRoute(
    id: Long,
    onBack: () -> Unit,
    onTermine: (Long?) -> Unit,
) {
    val vm: FournisseursViewModel = hiltViewModel()
    LaunchedEffect(id) { vm.ouvrirEdition(id) }
    FormulaireFournisseur(vm = vm, onBack = onBack, onTermine = onTermine)
}
