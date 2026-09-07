package com.missa.b360.core.domain.usecase

import com.missa.b360.core.data.dao.ClientDao
import com.missa.b360.core.data.dao.CompteTresorerieDao
import com.missa.b360.core.data.dao.EmployeeDao
import com.missa.b360.core.data.dao.MouvementTresorerieDao
import com.missa.b360.core.data.dao.OperationRecordDao
import com.missa.b360.core.data.dao.ProductDao
import com.missa.b360.core.data.dao.ProductStockDao
import com.missa.b360.core.data.dao.StockMovementDao
import com.missa.b360.core.data.entity.ClientEntity
import com.missa.b360.core.data.entity.CompteTresorerieEntity
import com.missa.b360.core.data.entity.MouvementTresorerieEntity
import com.missa.b360.core.data.entity.OperationDirection
import com.missa.b360.core.data.entity.OperationModule
import com.missa.b360.core.data.entity.OperationRecordEntity
import com.missa.b360.core.data.entity.OperationStatus
import com.missa.b360.core.data.entity.ProductEntity
import com.missa.b360.core.data.entity.ProductStockEntity
import com.missa.b360.core.data.entity.StockMovementEntity
import com.missa.b360.core.data.entity.StockMovementType
import com.missa.b360.core.domain.model.AlerteCode
import com.missa.b360.core.domain.model.IndicateurCode
import com.missa.b360.core.domain.model.Indicateurs
import com.missa.b360.core.domain.model.TresorerieRules
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/** Valeur calculée d'un indicateur, avec sa variation par rapport à la période précédente. */
data class ValeurIndicateur(
    val code: IndicateurCode,
    val valeur: Double,
    /** Variation en % vs période précédente, ou null si non comparable. */
    val variation: Double? = null,
)

/** Alerte levée : nombre d'éléments concernés, montant éventuel et exemple parlant. */
data class ValeurAlerte(
    val code: AlerteCode,
    val nombre: Int = 0,
    val montant: Double? = null,
    val exemple: String? = null,
)

/** Photographie complète du pilotage, indépendante des modules activés. */
data class TableauDeBord(
    val indicateurs: Map<IndicateurCode, ValeurIndicateur> = emptyMap(),
    val alertes: List<ValeurAlerte> = emptyList(),
    val periodeJours: Int = Indicateurs.PERIODE_JOURS,
    /** Vrai quand aucune donnée exploitable n'existe encore. */
    val vide: Boolean = true,
)

/**
 * Moteur du tableau de bord : il ne saisit rien, il **relit** les données déjà
 * produites par les autres modules (pièces, produits, stocks, mouvements,
 * employés, clients) et en déduit indicateurs et alertes.
 *
 * Tout est calculé ; c'est l'écran qui ne retient que les indicateurs des
 * modules actifs — un même calcul sert donc à tous les profils.
 */
class ObserveTableauDeBordUseCase @Inject constructor(
    private val operationDao: OperationRecordDao,
    private val productDao: ProductDao,
    private val stockDao: ProductStockDao,
    private val movementDao: StockMovementDao,
    private val employeeDao: EmployeeDao,
    private val clientDao: ClientDao,
    private val compteTresorerieDao: CompteTresorerieDao,
    private val mouvementTresorerieDao: MouvementTresorerieDao,
) {

    private data class Sources(
        val pieces: List<OperationRecordEntity>,
        val produits: List<ProductEntity>,
        val stocks: List<ProductStockEntity>,
        val mouvements: List<StockMovementEntity>,
        val employes: Int,
        val comptes: List<CompteTresorerieEntity> = emptyList(),
        val mouvementsTresorerie: List<MouvementTresorerieEntity> = emptyList(),
    )

    operator fun invoke(maintenant: () -> Long = System::currentTimeMillis): Flow<TableauDeBord> {
        val socle = combine(
            operationDao.observeAll(),
            productDao.observeAll(),
            stockDao.observeToutes(),
            movementDao.observeRecent(LIMITE_MOUVEMENTS),
            employeeDao.observeActifs(),
        ) { pieces, produits, stocks, mouvements, employes ->
            Sources(pieces, produits, stocks, mouvements, employes.size)
        }
        return combine(
            socle,
            clientDao.observeAllIncludingInactive(),
            compteTresorerieDao.observeAll(),
            mouvementTresorerieDao.observeAll(),
        ) { sources, clients, comptes, mouvementsTresorerie ->
            calculer(
                sources.copy(comptes = comptes, mouvementsTresorerie = mouvementsTresorerie),
                clients,
                maintenant(),
            )
        }
    }

    private fun calculer(sources: Sources, clients: List<ClientEntity>, maintenant: Long): TableauDeBord {
        val jour = 86_400_000L
        val debut = maintenant - Indicateurs.PERIODE_JOURS * jour
        val debutPrecedent = maintenant - 2L * Indicateurs.PERIODE_JOURS * jour
        val debutDormance = maintenant - Indicateurs.JOURS_DORMANT * jour

        val validees = sources.pieces.filter { it.status == OperationStatus.VALIDATED.name }
        val courant = validees.filter { it.createdAt >= debut }
        val precedent = validees.filter { it.createdAt in debutPrecedent until debut }

        fun montant(pieces: List<OperationRecordEntity>, module: OperationModule) =
            pieces.filter { it.module == module.name }.sumOf { it.amount ?: 0.0 }

        fun nombre(pieces: List<OperationRecordEntity>, module: OperationModule) =
            pieces.count { it.module == module.name }.toDouble()

        fun quantite(pieces: List<OperationRecordEntity>, module: OperationModule) =
            pieces.filter { it.module == module.name }.sumOf { it.quantity ?: 0.0 }

        fun tresorerie(pieces: List<OperationRecordEntity>, sens: OperationDirection) = pieces
            .filter { it.module == OperationModule.FINANCES.name && it.direction == sens.name }
            .sumOf { it.amount ?: 0.0 }

        // --- Ventes / achats / marge ---
        val ca = montant(courant, OperationModule.VENTE)
        val caPrecedent = montant(precedent, OperationModule.VENTE)
        val nbVentes = nombre(courant, OperationModule.VENTE)
        val nbVentesPrecedent = nombre(precedent, OperationModule.VENTE)
        val achats = montant(courant, OperationModule.ACHATS)
        val achatsPrecedent = montant(precedent, OperationModule.ACHATS)
        val marge = ca - achats
        val margePrecedente = caPrecedent - achatsPrecedent
        val tauxMarge = if (ca > 0) marge / ca * 100.0 else 0.0

        val devisOuverts = sources.pieces.filter {
            it.module == OperationModule.DEVIS.name && it.status == OperationStatus.DRAFT.name
        }
        val brouillons = sources.pieces.count { it.status == OperationStatus.DRAFT.name }
        val devisAnciens = devisOuverts.count {
            it.createdAt < maintenant - Indicateurs.JOURS_DEVIS_ANCIEN * jour
        }

        // --- Trésorerie ---
        // Deux gisements coexistent : les comptes du module Trésorerie (source
        // principale depuis la v9) et les anciennes pièces FINANCES saisies
        // avant lui. Ce sont des enregistrements distincts : les additionner ne
        // double aucun montant, et ignorer les seconds ferait disparaître de
        // l'argent des installations existantes.
        val soldeComptes = TresorerieRules.soldeGlobal(
            sources.comptes,
            sources.mouvementsTresorerie,
        )
        val fluxTresorerie = TresorerieRules.flux(
            sources.mouvementsTresorerie,
            debut,
            maintenant,
        )
        val fluxPrecedent = TresorerieRules.flux(
            sources.mouvementsTresorerie,
            debutPrecedent,
            debut - 1,
        )
        val entrees = tresorerie(validees, OperationDirection.IN)
        val sorties = tresorerie(validees, OperationDirection.OUT)
        val solde = soldeComptes + entrees - sorties
        val entreesPeriode = tresorerie(courant, OperationDirection.IN) + fluxTresorerie.entrees
        val sortiesPeriode = tresorerie(courant, OperationDirection.OUT) + fluxTresorerie.sorties

        // --- Stock ---
        val quantiteParProduit = sources.stocks.groupBy { it.produitId }
            .mapValues { (_, lignes) -> lignes.sumOf { it.quantite } }
        val produitsActifs = sources.produits.filter { it.active }
        val valeurStock = produitsActifs.sumOf { produit ->
            val quantiteEnStock = quantiteParProduit[produit.id] ?: 0.0
            val cout = produit.prixRevient ?: produit.prixAchat ?: 0.0
            quantiteEnStock * cout
        }
        val sousSeuil = produitsActifs.filter { produit ->
            produit.stockMin > 0.0 && (quantiteParProduit[produit.id] ?: 0.0) < produit.stockMin
        }
        val dernierMouvement = sources.mouvements
            .groupBy { it.produitId }
            .mapValues { (_, liste) -> liste.maxOf { it.horodatage } }
        val dormants = produitsActifs.filter { produit ->
            val enStock = (quantiteParProduit[produit.id] ?: 0.0) > 0.0
            val dernier = dernierMouvement[produit.id] ?: produit.createdAt
            enStock && dernier < debutDormance
        }
        val sortiesRecentes = sources.mouvements
            .filter {
                it.horodatage >= debutDormance &&
                    (it.type == StockMovementType.SORTIE || it.type == StockMovementType.TRANSFERT_SORTIE)
            }
            .sumOf { it.quantite }
        val stockTotal = quantiteParProduit.values.sum()
        // Sorties du trimestre rapportées au stock détenu, annualisées.
        val rotation = if (stockTotal > 0) sortiesRecentes / stockTotal * 4.0 else 0.0

        // --- Autres modules ---
        val projetsActifs = sources.pieces.count {
            it.module == OperationModule.PROJETS.name && it.status != OperationStatus.CANCELLED.name
        }.toDouble()
        val clientsActifs = clients.count { it.active && !it.prospect }.toDouble()
        val nouveauxClients = clients.count { it.createdAt >= debut }.toDouble()
        val nouveauxPrecedent = clients.count { it.createdAt in debutPrecedent until debut }.toDouble()

        val valeurs = listOf(
            valeur(IndicateurCode.CA_PERIODE, ca, caPrecedent),
            valeur(
                IndicateurCode.PANIER_MOYEN,
                if (nbVentes > 0) ca / nbVentes else 0.0,
                if (nbVentesPrecedent > 0) caPrecedent / nbVentesPrecedent else 0.0,
            ),
            valeur(IndicateurCode.DEVIS_ATTENTE, devisOuverts.sumOf { it.amount ?: 0.0 }),
            valeur(IndicateurCode.ACHATS_PERIODE, achats, achatsPrecedent),
            valeur(IndicateurCode.MARGE_BRUTE, marge, margePrecedente),
            valeur(IndicateurCode.TAUX_MARGE, tauxMarge),
            valeur(IndicateurCode.PIECES_BROUILLON, brouillons.toDouble()),
            valeur(IndicateurCode.SOLDE_TRESORERIE, solde),
            valeur(
                IndicateurCode.ENCAISSEMENTS,
                entreesPeriode,
                tresorerie(precedent, OperationDirection.IN) + fluxPrecedent.entrees,
            ),
            valeur(
                IndicateurCode.DECAISSEMENTS,
                sortiesPeriode,
                tresorerie(precedent, OperationDirection.OUT) + fluxPrecedent.sorties,
            ),
            valeur(IndicateurCode.VALEUR_STOCK, valeurStock),
            valeur(IndicateurCode.ARTICLES_SOUS_SEUIL, sousSeuil.size.toDouble()),
            valeur(IndicateurCode.ARTICLES_DORMANTS, dormants.size.toDouble()),
            valeur(IndicateurCode.ROTATION_STOCK, rotation),
            valeur(
                IndicateurCode.LIVRAISONS,
                nombre(courant, OperationModule.LIVRAISON),
                nombre(precedent, OperationModule.LIVRAISON),
            ),
            valeur(
                IndicateurCode.ORDRES_FABRICATION,
                nombre(courant, OperationModule.PRODUCTION),
                nombre(precedent, OperationModule.PRODUCTION),
            ),
            valeur(
                IndicateurCode.QUANTITE_PRODUITE,
                quantite(courant, OperationModule.PRODUCTION),
                quantite(precedent, OperationModule.PRODUCTION),
            ),
            valeur(
                IndicateurCode.INTERVENTIONS,
                nombre(courant, OperationModule.SERVICES),
                nombre(precedent, OperationModule.SERVICES),
            ),
            valeur(IndicateurCode.PROJETS_ACTIFS, projetsActifs),
            valeur(IndicateurCode.EFFECTIF_ACTIF, sources.employes.toDouble()),
            valeur(IndicateurCode.CLIENTS_ACTIFS, clientsActifs),
            valeur(IndicateurCode.NOUVEAUX_CLIENTS, nouveauxClients, nouveauxPrecedent),
            valeur(
                IndicateurCode.PIECES_VALIDEES,
                courant.size.toDouble(),
                precedent.size.toDouble(),
            ),
        ).associateBy { it.code }

        val alertes = buildList {
            if (sousSeuil.isNotEmpty()) {
                add(
                    ValeurAlerte(
                        code = AlerteCode.STOCK_SOUS_SEUIL,
                        nombre = sousSeuil.size,
                        exemple = sousSeuil.first().nom,
                    ),
                )
            }
            if (dormants.isNotEmpty()) {
                add(
                    ValeurAlerte(
                        code = AlerteCode.STOCK_DORMANT,
                        nombre = dormants.size,
                        exemple = dormants.first().nom,
                    ),
                )
            }
            if (solde < 0) {
                add(ValeurAlerte(code = AlerteCode.TRESORERIE_NEGATIVE, montant = solde))
            }
            if (brouillons > 0) {
                add(ValeurAlerte(code = AlerteCode.PIECES_A_VALIDER, nombre = brouillons))
            }
            if (devisAnciens > 0) {
                add(ValeurAlerte(code = AlerteCode.DEVIS_A_RELANCER, nombre = devisAnciens))
            }
            if (ca > 0 && tauxMarge < Indicateurs.SEUIL_MARGE_FAIBLE) {
                add(ValeurAlerte(code = AlerteCode.MARGE_FAIBLE, montant = tauxMarge))
            }
        }

        val vide = sources.pieces.isEmpty() && sources.produits.isEmpty() &&
            clients.isEmpty() && sources.comptes.isEmpty()
        return TableauDeBord(
            indicateurs = valeurs,
            alertes = alertes,
            periodeJours = Indicateurs.PERIODE_JOURS,
            vide = vide,
        )
    }

    private fun valeur(code: IndicateurCode, valeur: Double, precedent: Double? = null) =
        ValeurIndicateur(
            code = code,
            valeur = valeur,
            variation = if (precedent != null && precedent > 0.0) {
                (valeur - precedent) / precedent * 100.0
            } else {
                null
            },
        )

    private companion object {
        /** Historique de mouvements relu pour la dormance et la rotation. */
        const val LIMITE_MOUVEMENTS = 1_000
    }
}
