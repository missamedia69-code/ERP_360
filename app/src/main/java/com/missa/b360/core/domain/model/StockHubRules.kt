package com.missa.b360.core.domain.model

import com.missa.b360.core.data.entity.ProductEntity
import com.missa.b360.core.data.entity.ProductStockEntity
import com.missa.b360.core.data.entity.SiteEntity
import com.missa.b360.core.data.entity.StockMovementEntity
import com.missa.b360.core.data.entity.StockMovementType

/** Article dont la quantité est passée sous son seuil de réapprovisionnement. */
data class LigneSousSeuil(
    val produit: ProductEntity,
    val quantite: Double,
    val seuil: Double,
) {
    /** Ce qu'il faudrait commander pour repasser au-dessus du seuil. */
    val manque: Double get() = (seuil - quantite).coerceAtLeast(0.0)
}

/** Photographie du module Stock, pour son écran d'accueil. */
data class StockHub(
    val valeurStock: Double = 0.0,
    val nombreArticles: Int = 0,
    val articlesSousSeuil: List<LigneSousSeuil> = emptyList(),
    val ruptures: Int = 0,
    val mouvementsDuJour: Int = 0,
    val derniersMouvements: List<StockMovementEntity> = emptyList(),
    val quantiteTotale: Double = 0.0,
    val nomDepot: String? = null,
) {
    /** Ce qui appelle une action : ruptures et articles sous seuil. */
    val alertes: Int get() = articlesSousSeuil.size + ruptures
}

/**
 * Calculs de l'accueil du module Stock — sans dépendance Android ni Room.
 *
 * Tout part du dépôt sélectionné : un magasinier qui ouvre son dépôt veut ses
 * chiffres à lui, pas ceux du réseau. Sans sélection, l'ensemble des sites est
 * agrégé.
 */
object StockHubRules {

    private const val JOUR_MS = 86_400_000L

    /**
     * Valeur du stock au coût de revient, à défaut au prix d'achat.
     *
     * Valoriser au prix de vente afficherait une marge non réalisée : tant que
     * la marchandise est en rayon, elle ne vaut que ce qu'elle a coûté.
     */
    fun valeur(produits: List<ProductEntity>, stocks: List<ProductStockEntity>): Double {
        val parProduit = produits.associateBy { it.id }
        return stocks.sumOf { ligne ->
            val produit = parProduit[ligne.produitId]
            val cout = produit?.prixRevient ?: produit?.prixAchat ?: 0.0
            ligne.quantite * cout
        }
    }

    /**
     * Articles sous leur seuil, du plus critique au moins urgent.
     *
     * Un article sans seuil défini n'est jamais signalé : l'utilisateur n'a pas
     * exprimé d'attente, une alerte serait du bruit.
     */
    fun sousSeuil(
        produits: List<ProductEntity>,
        stocks: List<ProductStockEntity>,
    ): List<LigneSousSeuil> {
        val quantites = stocks.groupBy { it.produitId }
            .mapValues { (_, lignes) -> lignes.sumOf { it.quantite } }
        return produits
            .filter { it.active && it.stockMin > 0.0 }
            .mapNotNull { produit ->
                val quantite = quantites[produit.id] ?: 0.0
                if (quantite < produit.stockMin) {
                    LigneSousSeuil(produit, quantite, produit.stockMin)
                } else {
                    null
                }
            }
            .sortedByDescending { it.manque }
    }

    /** Articles actifs dont la quantité est nulle ou négative. */
    fun ruptures(produits: List<ProductEntity>, stocks: List<ProductStockEntity>): Int {
        val quantites = stocks.groupBy { it.produitId }
            .mapValues { (_, lignes) -> lignes.sumOf { it.quantite } }
        return produits.count { it.active && (quantites[it.id] ?: 0.0) <= 0.0 }
    }

    /** Mouvements enregistrés depuis le début de la journée. */
    fun mouvementsDuJour(mouvements: List<StockMovementEntity>, maintenant: Long): Int {
        val debut = maintenant - (maintenant % JOUR_MS)
        return mouvements.count { it.horodatage >= debut }
    }

    /** Libellé court d'un type de mouvement, pour les raccourcis. */
    fun natureMouvement(type: StockMovementType): String = type.name

    /**
     * Assemble la photographie complète du module pour un dépôt donné.
     *
     * Le filtre s'applique aux soldes et aux mouvements ; les fiches articles,
     * elles, restent communes à toute l'entreprise.
     */
    @Suppress("LongParameterList")
    fun construire(
        produits: List<ProductEntity>,
        stocks: List<ProductStockEntity>,
        mouvements: List<StockMovementEntity>,
        sites: List<SiteEntity>,
        depotId: Long?,
        maintenant: Long,
    ): StockHub {
        val stocksFiltres = stocks.filter { depotId == null || it.siteId == depotId }
        val mouvementsFiltres = mouvements.filter { depotId == null || it.siteId == depotId }
        val actifs = produits.filter { it.active }
        return StockHub(
            valeurStock = valeur(produits, stocksFiltres),
            nombreArticles = actifs.size,
            articlesSousSeuil = sousSeuil(produits, stocksFiltres),
            ruptures = ruptures(produits, stocksFiltres),
            mouvementsDuJour = mouvementsDuJour(mouvementsFiltres, maintenant),
            derniersMouvements = mouvementsFiltres.take(RACCOURCIS_AFFICHES),
            quantiteTotale = stocksFiltres.sumOf { it.quantite },
            nomDepot = depotId?.let { id -> sites.firstOrNull { it.id == id }?.nom },
        )
    }

    /** Quatre raccourcis suffisent : au-delà, la rangée devient un inventaire. */
    const val RACCOURCIS_AFFICHES = 6
}
