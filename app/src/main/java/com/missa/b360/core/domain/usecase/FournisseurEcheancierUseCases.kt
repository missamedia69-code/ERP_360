package com.missa.b360.core.domain.usecase

import androidx.room.withTransaction
import com.missa.b360.core.data.dao.FournisseurDocumentDao
import com.missa.b360.core.data.dao.FournisseurEvenementDao
import com.missa.b360.core.data.dao.FournisseurPaiementPlanifieDao
import com.missa.b360.core.data.dao.OperationRecordDao
import com.missa.b360.core.data.db.AppDatabase
import com.missa.b360.core.data.entity.FournisseurEvenementEntity
import com.missa.b360.core.data.entity.FournisseurEvenementType
import com.missa.b360.core.data.entity.FournisseurPaiementPlanifieEntity
import com.missa.b360.core.data.entity.VerificationStatut
import com.missa.b360.core.domain.model.FournisseurAchatMetrics
import com.missa.b360.core.domain.model.PaymentPlan
import com.missa.b360.core.domain.model.PaymentPlanRules
import com.missa.b360.core.domain.model.PlanStatut
import com.missa.b360.core.licensing.LicenceManager
import javax.inject.Inject

fun FournisseurPaiementPlanifieEntity.versPlan(): PaymentPlan =
    PaymentPlan(id = id, factureRecordId = factureRecordId, datePrevue = datePrevue, montant = montant, statut = statut)

enum class PlanResultat { OK, LECTURE_SEULE, FACTURE_INTROUVABLE, MONTANT_INVALIDE, DATE_INVALIDE, PLAN_INTROUVABLE, DEJA_TRAITE }

/**
 * Échéancier : planifier, reporter ou annuler un paiement fournisseur. Une ligne n'est jamais
 * supprimée (report = ancienne ligne REPORTE + nouvelle ligne ; annulation = statut ANNULE).
 * Planifier ne paie rien : le règlement passe toujours par la garde de paiement.
 */
class PlanifierPaiementUseCase @Inject constructor(
    private val planDao: FournisseurPaiementPlanifieDao,
    private val operationDao: OperationRecordDao,
    private val evenementDao: FournisseurEvenementDao,
    private val database: AppDatabase,
    private val licenceManager: LicenceManager,
) {
    suspend fun planifier(
        factureRecordId: Long,
        montant: Double,
        datePrevue: Long,
        now: Long = System.currentTimeMillis(),
    ): PlanResultat {
        if (licenceManager.isReadOnly()) return PlanResultat.LECTURE_SEULE
        if (datePrevue <= 0L) return PlanResultat.DATE_INVALIDE
        val piece = operationDao.getById(factureRecordId) ?: return PlanResultat.FACTURE_INTROUVABLE
        val facture = FournisseurAchatMetrics.facturesOuvertes(listOf(piece)).firstOrNull()
            ?: return PlanResultat.FACTURE_INTROUVABLE
        return database.withTransaction {
            val existants = planDao.getParFacture(factureRecordId).map { it.versPlan() }
            if (!PaymentPlanRules.planifiable(facture.outstanding, existants, montant)) {
                return@withTransaction PlanResultat.MONTANT_INVALIDE
            }
            planDao.insert(
                FournisseurPaiementPlanifieEntity(
                    fournisseurId = facture.fournisseurId,
                    factureRecordId = factureRecordId,
                    datePrevue = datePrevue,
                    montant = Math.round(montant * 100.0) / 100.0,
                    createdAt = now,
                ),
            )
            evenementDao.insert(
                FournisseurEvenementEntity(
                    fournisseurId = facture.fournisseurId,
                    date = now,
                    type = FournisseurEvenementType.MISE_A_JOUR,
                    details = "Paiement planifié ${facture.reference} : $montant",
                ),
            )
            PlanResultat.OK
        }
    }

    suspend fun reporter(planId: Long, nouvelleDate: Long, now: Long = System.currentTimeMillis()): PlanResultat {
        if (licenceManager.isReadOnly()) return PlanResultat.LECTURE_SEULE
        if (nouvelleDate <= 0L) return PlanResultat.DATE_INVALIDE
        return database.withTransaction {
            val plan = planDao.getById(planId) ?: return@withTransaction PlanResultat.PLAN_INTROUVABLE
            val (ancien, nouveau) = PaymentPlanRules.reporter(plan.versPlan(), nouvelleDate)
                ?: return@withTransaction PlanResultat.DEJA_TRAITE
            planDao.update(plan.copy(statut = ancien.statut))
            planDao.insert(plan.copy(id = 0, datePrevue = nouveau.datePrevue, statut = PlanStatut.PLANIFIE, createdAt = now))
            PlanResultat.OK
        }
    }

    suspend fun annuler(planId: Long, now: Long = System.currentTimeMillis()): PlanResultat {
        if (licenceManager.isReadOnly()) return PlanResultat.LECTURE_SEULE
        return database.withTransaction {
            val plan = planDao.getById(planId) ?: return@withTransaction PlanResultat.PLAN_INTROUVABLE
            val annule = PaymentPlanRules.annuler(plan.versPlan()) ?: return@withTransaction PlanResultat.DEJA_TRAITE
            planDao.update(plan.copy(statut = annule.statut))
            evenementDao.insert(
                FournisseurEvenementEntity(
                    fournisseurId = plan.fournisseurId,
                    date = now,
                    type = FournisseurEvenementType.MISE_A_JOUR,
                    details = "Paiement planifié annulé : ${plan.montant}",
                ),
            )
            PlanResultat.OK
        }
    }
}

/** Vérification (ou rejet) d'un document de conformité ; l'ancien état reste dans le journal. */
class VerifierDocumentFournisseurUseCase @Inject constructor(
    private val documentDao: FournisseurDocumentDao,
    private val evenementDao: FournisseurEvenementDao,
    private val licenceManager: LicenceManager,
) {
    suspend operator fun invoke(
        documentId: Long,
        fournisseurId: Long,
        approuve: Boolean,
        now: Long = System.currentTimeMillis(),
    ): Boolean {
        if (licenceManager.isReadOnly()) return false
        val document = documentDao.getById(documentId) ?: return false
        if (document.fournisseurId != fournisseurId || document.archive) return false
        val statut = if (approuve) VerificationStatut.VERIFIE else VerificationStatut.REJETE
        if (document.verification == statut) return false
        documentDao.update(document.copy(verification = statut))
        evenementDao.insert(
            FournisseurEvenementEntity(
                fournisseurId = fournisseurId,
                date = now,
                type = FournisseurEvenementType.MISE_A_JOUR,
                details = "Document ${document.typeDocument.name} " + if (approuve) "vérifié" else "rejeté",
            ),
        )
        return true
    }
}
