package com.missa.b360.ui.fournisseurs

/**
 * Routes du module Fournisseurs. Un écran = une route : l'état de navigation vit dans la pile
 * (retour système, rotation, restauration), jamais dans une variable locale.
 *
 * `module_fournisseurs?create=true` reste l'entrée de l'Accueil : elle ouvre la liste puis la création.
 * Une route n'est déclarée qu'avec son écran : une route sans destination ferait planter `navigate`.
 */
object FournisseurRoutes {
    /** Identique à `AppModule.FOURNISSEURS.route` (barre de modules, Accueil, notifications) : garde-fou `FournisseurRoutesTest`. */
    const val RACINE = "module_fournisseurs"
    const val ARG_ID = "id"
    const val ARG_CREATE = "create"

    const val LISTE = "$RACINE?$ARG_CREATE={$ARG_CREATE}"
    const val ACTION = "fournisseurs_action"
    const val FICHE = "fournisseur_fiche/{$ARG_ID}"

    /** Dossier complet (contacts, articles, comptes, documents, journal, changements de statut). */
    const val DOSSIER = "fournisseur_dossier/{$ARG_ID}"
    const val COMPTE = "fournisseur_compte/{$ARG_ID}"
    const val CONFORMITE = "fournisseur_conformite/{$ARG_ID}"
    const val ECHEANCIER = "fournisseurs_echeancier"

    /** `id` 0 = création. */
    const val EDITION = "fournisseur_edition/{$ARG_ID}"

    fun fiche(id: Long): String = "fournisseur_fiche/$id"
    fun dossier(id: Long): String = "fournisseur_dossier/$id"
    fun compte(id: Long): String = "fournisseur_compte/$id"
    fun conformite(id: Long): String = "fournisseur_conformite/$id"
    fun edition(id: Long): String = "fournisseur_edition/$id"
    fun creation(): String = edition(0L)
}
