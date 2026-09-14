package com.missa.b360.core.domain.model

import com.missa.b360.R
import com.missa.b360.core.data.entity.OperationModule
import com.missa.b360.core.data.entity.OperationRecordEntity
import com.missa.b360.core.data.entity.OperationStatus
import com.missa.b360.core.data.entity.ProductEntity
import com.missa.b360.core.data.entity.ProductStockEntity
import com.missa.b360.core.data.entity.SiteEntity
import com.missa.b360.core.data.entity.StockMovementEntity
import com.missa.b360.core.data.entity.StockMovementType

/** Implantation du stock sur un site. */
data class StockDuSite(
    val site: SiteEntity,
    val references: Int,
    val quantite: Double,
    val valeur: Double,
    val ruptures: Int,
)

/** État d'un transfert entre deux sites. */
enum class EtatTransfert(val libelleRes: Int) {
    /** Sortie et entrée appariées : la marchandise est arrivée. */
    RECU(R.string.log_transfert_recu),
    /** Sortie enregistrée, entrée manquante : la marchandise est en route… ou perdue. */
    EN_TRANSIT(R.string.log_transfert_transit),
    /** Entrée sans sortie : saisie incomplète, à corriger. */
    ORPHELIN(R.string.log_transfert_orphelin),
}

/** Un transfert inter-sites reconstitué à partir de ses mouvements. */
data class Transfert(
    val reference: String,
    val quantite: Double,
    val etat: EtatTransfert,
    val date: Long,
    val siteSource: Long?,
    val siteDestination: Long?,
)

/** Compte des livraisons par statut. */
data class SuiviLivraisons(
    val enPreparation: Int = 0,
    val effectuees: Int = 0,
    val annulees: Int = 0,
) {
    val total: Int get() = enPreparation + effectuees + annulees

    /** Part des livraisons menées à bien, hors annulations. */
    val tauxService: Double
        get() {
            val base = enPreparation + effectuees
            return if (base == 0) 0.0 else effectuees.toDouble() / base * 100.0
        }
}

/**
 * Règles du module Logistique — sans dépendance Android ni Room.
 *
 * La Logistique ne saisit ni produit ni mouvement : elle donne la vue
 * transverse qui manque aux modules Stock (mono-site par écran) et Ventes —
 * où est la marchandise, ce qui circule entre les sites, et ce qui n'est
 * jamais arrivé.
 */
object LogistiqueRules {

    /** Préfixe de référence posé sur les deux jambes d'un transfert. */
    const val PREFIXE_TRANSFERT = "TRF"

    fun suiviLivraisons(pieces: List<OperationRecordEntity>): SuiviLivraisons {
        val livraisons = pieces.filter { it.module == OperationModule.LIVRAISON.name }
        return SuiviLivraisons(
            enPreparation = livraisons.count { it.status == OperationStatus.DRAFT.name },
            effectuees = livraisons.count { it.status == OperationStatus.VALIDATED.name },
            annulees = livraisons.count { it.status == OperationStatus.CANCELLED.name },
        )
    }

    /**
     * Implantation du stock, site par site.
     *
     * La valeur retenue est le coût de revient, à défaut le prix d'achat : un
     * stock valorisé au prix de vente afficherait une marge qui n'est pas encore
     * réalisée.
     */
    fun stockParSite(
        sites: List<SiteEntity>,
        stocks: List<ProductStockEntity>,
        produits: List<ProductEntity>,
    ): List<StockDuSite> {
        val parProduit = produits.associateBy { it.id }
        val parSite = stocks.groupBy { it.siteId }
        return sites.map { site ->
            val lignes = parSite[site.id].orEmpty().filter { parProduit.containsKey(it.produitId) }
            StockDuSite(
                site = site,
                references = lignes.count { it.quantite > 0 },
                quantite = lignes.sumOf { it.quantite },
                valeur = lignes.sumOf { ligne ->
                    val produit = parProduit[ligne.produitId]
                    val cout = produit?.prixRevient ?: produit?.prixAchat ?: 0.0
                    ligne.quantite * cout
                },
                ruptures = lignes.count { it.quantite <= 0.0 },
            )
        }.sortedByDescending { it.valeur }
    }

    /**
     * Reconstitue les transferts à partir des mouvements appariés par
     * référence.
     *
     * Une sortie sans entrée correspondante n'est pas une anomalie de données :
     * c'est de la marchandise en route, ou égarée. C'est précisément ce que la
     * logistique doit rendre visible.
     */
    fun transferts(mouvements: List<StockMovementEntity>): List<Transfert> = mouvements
        .filter {
            it.type == StockMovementType.TRANSFERT_SORTIE ||
                it.type == StockMovementType.TRANSFERT_ENTREE
        }
        .groupBy { it.reference ?: "" }
        .filterKeys { it.isNotEmpty() }
        .map { (reference, lignes) ->
            val sortie = lignes.firstOrNull { it.type == StockMovementType.TRANSFERT_SORTIE }
            val entree = lignes.firstOrNull { it.type == StockMovementType.TRANSFERT_ENTREE }
            Transfert(
                reference = reference,
                quantite = sortie?.quantite ?: entree?.quantite ?: 0.0,
                etat = when {
                    sortie != null && entree != null -> EtatTransfert.RECU
                    sortie != null -> EtatTransfert.EN_TRANSIT
                    else -> EtatTransfert.ORPHELIN
                },
                date = lignes.maxOf { it.horodatage },
                siteSource = sortie?.siteId,
                siteDestination = entree?.siteId,
            )
        }
        .sortedWith(compareBy<Transfert> { it.etat.ordinal }.thenByDescending { it.date })

    /** Transferts qui n'ont jamais été réceptionnés. */
    fun enTransit(transferts: List<Transfert>): List<Transfert> =
        transferts.filter { it.etat == EtatTransfert.EN_TRANSIT }

    /**
     * Sites dont la valeur de stock dépasse la part attendue, signe d'une
     * concentration. Utile pour équilibrer un réseau de boutiques.
     */
    fun concentration(sites: List<StockDuSite>): Double {
        val total = sites.sumOf { it.valeur }
        if (total <= 0.0 || sites.size < 2) return 0.0
        return sites.maxOf { it.valeur } / total * 100.0
    }
}
