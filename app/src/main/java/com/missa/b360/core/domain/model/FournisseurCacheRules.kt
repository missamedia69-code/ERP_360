package com.missa.b360.core.domain.model

import com.missa.b360.core.data.entity.FournisseurBalanceEntity
import com.missa.b360.core.data.entity.FournisseurEntity
import com.missa.b360.core.data.entity.FournisseurScoreEntity
import com.missa.b360.core.data.entity.OperationRecordEntity

/**
 * Calcul pur des deux caches dérivés d'un fournisseur. Le refresher n'ajoute que de la
 * persistance : le cache est donc, par construction, égal au résultat de ce calculateur.
 */
object FournisseurCacheRules {

    private const val FENETRE_ACHATS_JOURS = 365L

    fun balance(fournisseur: FournisseurEntity, pieces: List<OperationRecordEntity>, now: Long): FournisseurBalanceEntity {
        val dette = FournisseurAchatMetrics.dette(
            pieces = pieces,
            now = now,
            fournisseurId = fournisseur.id,
            joursEcheance = { fournisseur.joursEcheance },
        )
        val debut = now - FENETRE_ACHATS_JOURS * FournisseurAchatMetrics.DAY_MS
        val factures = FournisseurAchatMetrics.facturesDuFournisseur(pieces, fournisseur.id)
        return FournisseurBalanceEntity(
            fournisseurId = fournisseur.id,
            dette = dette.dette,
            enRetard = dette.enRetard,
            joursRetardMax = dette.joursRetardMax,
            nbFacturesOuvertes = dette.nbFacturesOuvertes,
            nbCommandesOuvertes = FournisseurAchatMetrics.commandesOuvertes(pieces, fournisseur.id),
            achats12Mois = factures.filter { (piece, _) -> piece.createdAt >= debut && piece.createdAt <= now }
                .sumOf { (_, facture) -> facture.total.takeIf { it.isFinite() && it > 0.0 } ?: 0.0 },
            derniereFactureAt = factures.maxOfOrNull { (piece, _) -> piece.createdAt },
            prochaineEcheanceAt = dette.prochaineEcheanceAt,
            majAt = now,
        )
    }

    fun score(fournisseurId: Long, pieces: List<OperationRecordEntity>, now: Long): FournisseurScoreEntity {
        val s = SupplierScorecard.calculer(fournisseurId, pieces, now)
        return FournisseurScoreEntity(
            fournisseurId = fournisseurId,
            ponctualite = s.ponctualite,
            conformite = s.conformite,
            prix = s.prix,
            score = s.score,
            nbCommandesMesurees = s.nbCommandesMesurees,
            delaiMoyenReelJours = s.delaiMoyenReelJours,
            majAt = now,
        )
    }

    /** Deux balances sont équivalentes pour l'argent et les compteurs (hors dates et retards, qui avancent seuls). */
    fun memesMontants(a: FournisseurBalanceEntity?, b: FournisseurBalanceEntity): Boolean =
        a != null &&
            kotlin.math.abs(a.dette - b.dette) < 0.005 &&
            a.nbFacturesOuvertes == b.nbFacturesOuvertes &&
            a.nbCommandesOuvertes == b.nbCommandesOuvertes
}
