package com.missa.b360.ui.stock

/** Action rapide demandée à l'ouverture du module Stock depuis l'accueil. */
enum class StockOpenAction { ENTRY, EXIT, TRANSFER, ADJUSTMENT, PRODUCT;

    companion object {
        fun parse(value: String?): StockOpenAction? =
            entries.firstOrNull { it.name.equals(value?.trim(), ignoreCase = true) }
    }
}
