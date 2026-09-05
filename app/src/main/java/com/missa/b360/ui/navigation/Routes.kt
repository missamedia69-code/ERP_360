package com.missa.b360.ui.navigation

/** Routes hors modules métier (socle + administration). */
object Routes {
    const val HOME = "home"
    const val NOTIFICATIONS = "notifications"

    // Module Stock — formulaires dédiés (Phase E : produits & mouvements)
    const val STOCK_PRODUCT_FORM = "stock_product_form"
    const val STOCK_MOVEMENT_FORM = "stock_movement_form"
    const val STOCK_TRANSFER_FORM = "stock_transfer_form"

    // ☰ Administration & Paramétrage (module 9.1, Phase C)
    const val ADMIN_REGLAGES = "admin_reglages"
    const val ADMIN_LICENCE = "admin_licence"
    const val ADMIN_SAUVEGARDE = "admin_sauvegarde"
    const val ADMIN_JOURNAL = "admin_journal"
    const val ADMIN_UTILISATEURS = "admin_utilisateurs"
    const val ADMIN_MULTISITE = "admin_multisite"
    const val ADMIN_A_PROPOS = "admin_a_propos"
}
