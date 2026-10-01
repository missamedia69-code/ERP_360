package com.missa.b360.ui.clients

/**
 * Routes du module Clients. Un écran = une route : l'état de navigation vit dans la pile de
 * navigation (retour système, rotation et restauration après arrêt du processus inclus), jamais
 * dans une variable locale.
 *
 * `clients?create=true` reste l'entrée de l'Accueil : elle ouvre la liste puis la feuille de création.
 */
object ClientRoutes {
    const val RACINE = "clients"
    const val ARG_ID = "id"
    const val ARG_CREATE = "create"

    const val LISTE = "$RACINE?$ARG_CREATE={$ARG_CREATE}"
    const val FICHE = "$RACINE/{$ARG_ID}"
    const val COMPTE = "$RACINE/{$ARG_ID}/compte"
    const val ACTIVITE = "$RACINE/{$ARG_ID}/activite"
    const val RELANCES = "$RACINE/relances"
    const val IMPORT = "$RACINE/import"
    const val NOUVEAU = "$RACINE/new"
    const val EDITION = "$RACINE/{$ARG_ID}/edit"

    fun liste(creer: Boolean = false): String = if (creer) "$RACINE?$ARG_CREATE=true" else RACINE
    fun fiche(id: Long): String = "$RACINE/$id"
    fun compte(id: Long): String = "$RACINE/$id/compte"
    fun activite(id: Long): String = "$RACINE/$id/activite"
    fun edition(id: Long): String = "$RACINE/$id/edit"
}
