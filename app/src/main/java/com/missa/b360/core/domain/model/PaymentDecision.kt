package com.missa.b360.core.domain.model

import com.missa.b360.core.data.entity.FournisseurCompteBancaireEntity
import com.missa.b360.core.data.entity.FournisseurEntity

/** Issue de la garde de paiement une fois la confirmation de l'utilisateur prise en compte. */
sealed interface PaymentDecision {
    /** Le règlement peut être enregistré ; [confirmeMotif] vaut le motif d'une confirmation donnée. */
    data class Proceed(val confirmeMotif: PaymentGuardReason? = null) : PaymentDecision

    /** À reproposer à l'utilisateur avec [confirme] = true. */
    data class NeedConfirmation(val motif: PaymentGuardReason) : PaymentDecision

    /** Aucune confirmation ne lève ce refus. */
    data class Reject(val motif: PaymentGuardReason) : PaymentDecision
}

/** Applique [PaymentGuard] à un règlement : choix du compte bénéficiaire, puis confirmation. */
object SupplierPaymentDecision {

    fun decider(
        fournisseur: FournisseurEntity,
        montant: Double,
        modePaiement: String?,
        comptes: List<FournisseurCompteBancaireEntity>,
        compteId: Long?,
        confirme: Boolean,
        now: Long,
    ): PaymentDecision {
        val mode = PaymentMode.depuis(modePaiement)
        val beneficiaire = SupplierAccountRules.compteBeneficiaire(comptes, mode, compteId)
        return when (val verdict = PaymentGuard.evaluer(fournisseur, montant, mode, beneficiaire?.versInfo(), now)) {
            PaymentVerdict.Autorise -> PaymentDecision.Proceed()
            is PaymentVerdict.Refuser -> PaymentDecision.Reject(verdict.motif)
            is PaymentVerdict.Confirmer ->
                if (confirme) PaymentDecision.Proceed(verdict.motif) else PaymentDecision.NeedConfirmation(verdict.motif)
        }
    }
}
