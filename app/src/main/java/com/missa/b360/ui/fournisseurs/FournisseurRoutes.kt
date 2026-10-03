package com.missa.b360.ui.fournisseurs

/**
 * Routes du module Fournisseurs. Un écran = une route : l'état de navigation vit dans la pile
 * (retour système, rotation, restauration), jamais dans une variable locale.
 *
 * `module_fournisseurs?create=true` reste l'entrée de l'Accueil : elle ouvre la liste puis la création.
 * Les routes des écrans de F5 à F7 (compte, conformité, échéancier, comparateur, documents) sont
 * ajoutées avec leur écran : une route déclarée sans destination ferait planter `navigate`.
 */
object FournisseurRoutes {
    /** Identique à `AppModule.FOURNISSEURS.route` (barre de modules, Accueil, notifications) : garde-fou `FournisseurRoutesTest`. */
    const val RACINE = "module_fournisseurs"
    const val ARG_ID = "id"
    const val ARG_CREATE = "create"

    const val LISTE = "$RACINE?$ARG_CREATE={$ARG_CREATE}"
    const val ACTION = "fournisseurs_action"
    const val FICHE = "fournisseur_fiche/{$ARG_ID}"

    /** `id` 0 = création. */
    const val EDITION = "fournisseur_edition/{$ARG_ID}"

    fun fiche(id: Long): String = "fournisseur_fiche/$id"
    fun edition(id: Long): String = "fournisseur_edition/$id"
    fun creation(): String = edition(0L)
}
