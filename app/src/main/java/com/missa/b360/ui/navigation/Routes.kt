package com.missa.b360.ui.navigation

/** Routes hors modules métier (socle + administration). */
object Routes {
    const val HOME = "home"
    const val NOTIFICATIONS = "notifications"

    // Module Stock — formulaires dédiés (Phase E : produits & mouvements)
    const val STOCK_PRODUCT_FORM = "stock_product_form"
    const val STOCK_MOVEMENT_FORM = "stock_movement_form"
    const val STOCK_TRANSFER_FORM = "stock_transfer_form"
    const val STOCK_INVENTORY = "stock_inventory"
    const val OPERATION_FORM = "operation_form"

    // Module Vente — retour de vente et avoir (spec §22)
    const val SALES_RETURN = "sales_return"

    // Module Vente — devis & commandes (spec §20 : devis → commande → facture)
    const val DEVIS_COMMANDE = "devis_commande"

    // ☰ Administration & Paramétrage (module 9.1, Phase C)
    const val ADMIN_REGLAGES = "admin_reglages"
    const val ADMIN_LICENCE = "admin_licence"
    const val ADMIN_SAUVEGARDE = "admin_sauvegarde"
    const val ADMIN_JOURNAL = "admin_journal"
    const val ADMIN_UTILISATEURS = "admin_utilisateurs"
    const val ADMIN_MULTISITE = "admin_multisite"
    const val ADMIN_A_PROPOS = "admin_a_propos"

    // Référentiels (spec §30) — moyens de paiement, taxes, unités.
    const val ADMIN_REFERENTIELS = "admin_referentiels"

    // Tâches de suivi (spec §Tâches).
    const val TASKS = "tasks"
}
