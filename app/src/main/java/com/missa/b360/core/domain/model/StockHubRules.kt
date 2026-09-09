package com.missa.b360.core.domain.model

import com.missa.b360.core.data.dao.StockMovementView
import com.missa.b360.core.data.entity.GroupeArticleEntity
import com.missa.b360.core.data.entity.ProductEntity
import com.missa.b360.core.data.entity.ProductStockEntity
import com.missa.b360.core.data.entity.SiteEntity
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

/** Un mouvement récent, prêt à l'affichage : nom du produit, sens et montant. */
data class LigneMouvementRecente(
    val produitNom: String,
    val produitCode: String,
    val type: StockMovementType,
    /** Quantité signée pour le sens (positive en entrée, négative en sortie). */
    val quantite: Double,
    val reference: String?,
    val horodatage: Long,
) {
    /** Vrai pour une sortie de stock. */
    val estSortie: Boolean get() = type == StockMovementType.SORTIE ||
        type == StockMovementType.TRANSFERT_SORTIE

    val estEntree: Boolean get() = type == StockMovementType.ENTREE ||
        type == StockMovementType.TRANSFERT_ENTREE
}

/** Répartition de la valeur et du nombre d'articles par groupe. */
data class LigneGroupeStock(
    val code: String,
    val nom: String,
    val nombreArticles: Int,
    val valeur: Double,
)

/**
 * Photographie du module Stock, pour son écran d'accueil.
 *
 * La valeur du stock, les alertes et les mouvements sont toujours calculés pour
 * le dépôt sélectionné ; les fiches articles restent communes à l'entreprise.
 */
data class StockHub(
    val valeurStock: Double = 0.0,
    val nombreArticles: Int = 0,
    val articlesSousSeuil: List<LigneSousSeuil> = emptyList(),
    val ruptures: Int = 0,
    val mouvementsDuJour: Int = 0,
    val mouvementsHier: Int = 0,
    val derniersMouvements: List<LigneMouvementRecente> = emptyList(),
    val groupes: List<LigneGroupeStock> = emptyList(),
    val quantiteTotale: Double = 0.0,
    val nomDepot: String? = null,
    /** Minutes écoulées depuis le dernier mouvement ; null si aucun mouvement. */
    val minutesDepuisActivite: Int? = null,
) {
    /** Ce qui appelle une action : ruptures et articles sous seuil. */
    val alertes: Int get() = articlesSousSeuil.size + ruptures

    /** Vrai quand un indicateur de tendance peut être affiché (mouvements connus). */
    val tendanceMouvements: Double? get() {
        if (mouvementsHier <= 0) return null
        return (mouvementsDuJour - mouvementsHier).toDouble() / mouvementsHier
    }
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
    private const val MINUTE_MS = 60_000L

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
    fun mouvementsDuJour(mouvements: List<StockMovementView>, maintenant: Long): Int {
        val debut = maintenant - (maintenant % JOUR_MS)
        return mouvements.count { it.horodatage >= debut }
    }

    /** Mouvements enregistrés hier (journée civile précédente). */
    fun mouvementsHier(mouvements: List<StockMovementView>, maintenant: Long): Int {
        val debutAujourdhui = maintenant - (maintenant % JOUR_MS)
        val debutHier = debutAujourdhui - JOUR_MS
        return mouvements.count { it.horodatage >= debutHier && it.horodatage < debutAujourdhui }
    }

    /**
     * Répartition par groupe d'articles : nombre de fiches actives rattachées au
     * groupe et valeur de leur stock. Les groupes vides restent visibles, car un
     * magasinier veut savoir qu'une famille n'a encore rien.
     */
    fun parGroupe(
        produits: List<ProductEntity>,
        stocks: List<ProductStockEntity>,
        groupes: List<GroupeArticleEntity>,
    ): List<LigneGroupeStock> {
        val parProduit = produits.associateBy { it.id }.filterValues { it.active }
        val stocksFiltres = stocks.filter { parProduit.containsKey(it.produitId) }
        val quantites = stocksFiltres.groupBy { it.produitId }
            .mapValues { (_, lignes) -> lignes.sumOf { it.quantite } }

        return groupes
            .filter { it.actif }
            .map { groupe ->
                val membres = parProduit.values.filter { it.itemGroupId == groupe.id }
                val valeur = membres.sumOf { produit ->
                    val cout = produit.prixRevient ?: produit.prixAchat ?: 0.0
                    cout * (quantites[produit.id] ?: 0.0)
                }
                LigneGroupeStock(
                    code = groupe.code,
                    nom = groupe.nom,
                    nombreArticles = membres.size,
                    valeur = valeur,
                )
            }
            .sortedWith(compareByDescending<LigneGroupeStock> { it.valeur })
    }

    /** Type de mouvement à partir de son nom stocké, avec repli sur ENTRÉE. */
    fun typeDe(nom: String?): StockMovementType = runCatching {
        StockMovementType.valueOf(nom.orEmpty())
    }.getOrDefault(StockMovementType.ENTREE)

    /** Minutes écoulées depuis le dernier mouvement ; null si aucun mouvement. */
    fun minutesDepuisDerniereActivite(
        mouvements: List<StockMovementView>,
        maintenant: Long,
    ): Int? {
        val derniere = mouvements.maxOfOrNull { it.horodatage } ?: return null
        return ((maintenant - derniere) / MINUTE_MS).toInt().coerceAtLeast(0)
    }

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
        mouvements: List<StockMovementView>,
        groupes: List<GroupeArticleEntity>,
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
            mouvementsHier = mouvementsHier(mouvementsFiltres, maintenant),
            derniersMouvements = mouvementsFiltres
                .take(RACCOURCIS_AFFICHES)
                .map { vue ->
                    LigneMouvementRecente(
                        produitNom = vue.produitNom,
                        produitCode = vue.produitCode,
                        type = typeDe(vue.type),
                        quantite = vue.quantite,
                        reference = vue.reference,
                        horodatage = vue.horodatage,
                    )
                },
            groupes = parGroupe(produits, stocksFiltres, groupes),
            quantiteTotale = stocksFiltres.sumOf { it.quantite },
            nomDepot = depotId?.let { id -> sites.firstOrNull { it.id == id }?.nom },
            minutesDepuisActivite = minutesDepuisDerniereActivite(mouvementsFiltres, maintenant),
        )
    }

    /** Six raccourcis suffisent : au-delà, la rangée devient un inventaire. */
    const val RACCOURCIS_AFFICHES = 6
}
