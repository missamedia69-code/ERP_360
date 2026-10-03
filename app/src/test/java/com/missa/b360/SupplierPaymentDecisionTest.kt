package com.missa.b360

import com.missa.b360.core.data.entity.FournisseurCompteBancaireEntity
import com.missa.b360.core.data.entity.FournisseurEntity
import com.missa.b360.core.data.entity.FournisseurStatus
import com.missa.b360.core.data.entity.VerificationStatut
import com.missa.b360.core.domain.model.PaymentDecision
import com.missa.b360.core.domain.model.PaymentGuardReason
import com.missa.b360.core.domain.model.PaymentMode
import com.missa.b360.core.domain.model.SupplierAccountRules
import com.missa.b360.core.domain.model.SupplierPaymentDecision
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SupplierPaymentDecisionTest {
    private val jour = 86_400_000L
    private val now = 1_000L * jour

    private fun fournisseur(
        statut: FournisseurStatus = FournisseurStatus.ACTIF,
        plafond: Double = 0.0,
        bloque: Boolean = false,
    ) = FournisseurEntity(
        id = 7, code = "FRN-2026-0001", nom = "Atelier Nord", telephone = "650000000",
        statut = statut, plafondPaiement = plafond, paiementBloque = bloque, createdAt = 0,
    )

    private fun banque(
        id: Long = 1, v: VerificationStatut = VerificationStatut.VERIFIE, ilYaJours: Long = 90, principal: Boolean = false,
    ) = FournisseurCompteBancaireEntity(
        id = id, fournisseurId = 7, titulaire = "Atelier Nord", iban = "CM2110002000300040005000$id",
        verification = v, modifieLe = now - ilYaJours * jour, principal = principal,
    )

    private fun mobile(id: Long = 2, v: VerificationStatut = VerificationStatut.VERIFIE) = FournisseurCompteBancaireEntity(
        id = id, fournisseurId = 7, titulaire = "Atelier Nord", numeroMobile = "670000000", verification = v, modifieLe = 0,
    )

    private fun decider(
        mode: String = "Espèces",
        montant: Double = 100.0,
        comptes: List<FournisseurCompteBancaireEntity> = emptyList(),
        f: FournisseurEntity = fournisseur(),
        compteId: Long? = null,
        confirme: Boolean = false,
    ) = SupplierPaymentDecision.decider(f, montant, mode, comptes, compteId, confirme, now)

    @Test fun `un paiement en especes sans compte est autorise`() {
        assertEquals(PaymentDecision.Proceed(), decider())
    }

    @Test fun `un virement exige un compte du fournisseur`() {
        assertEquals(PaymentDecision.Reject(PaymentGuardReason.COMPTE_REQUIS), decider(mode = "Virement"))
        assertEquals(
            PaymentDecision.Reject(PaymentGuardReason.COMPTE_REQUIS),
            decider(mode = "Virement", comptes = listOf(mobile())),
        )
        assertEquals(PaymentDecision.Proceed(), decider(mode = "Virement", comptes = listOf(banque())))
    }

    @Test fun `le mobile money exige un numero mobile`() {
        assertEquals(PaymentDecision.Reject(PaymentGuardReason.COMPTE_REQUIS), decider(mode = "Mobile money", comptes = listOf(banque())))
        assertEquals(PaymentDecision.Proceed(), decider(mode = "Mobile money", comptes = listOf(mobile())))
    }

    @Test fun `un compte non verifie ancien demande confirmation puis passe`() {
        val comptes = listOf(banque(v = VerificationStatut.A_VERIFIER))
        assertEquals(
            PaymentDecision.NeedConfirmation(PaymentGuardReason.COMPTE_NON_VERIFIE),
            decider(mode = "Virement", comptes = comptes),
        )
        assertEquals(
            PaymentDecision.Proceed(PaymentGuardReason.COMPTE_NON_VERIFIE),
            decider(mode = "Virement", comptes = comptes, confirme = true),
        )
    }

    @Test fun `un compte recent non verifie est refuse meme avec confirmation`() {
        val comptes = listOf(banque(v = VerificationStatut.A_VERIFIER, ilYaJours = 2))
        assertEquals(
            PaymentDecision.Reject(PaymentGuardReason.COMPTE_RECENT_NON_VERIFIE),
            decider(mode = "Virement", comptes = comptes, confirme = true),
        )
    }

    @Test fun `un compte rejete est refuse meme avec confirmation`() {
        assertEquals(
            PaymentDecision.Reject(PaymentGuardReason.COMPTE_REJETE),
            decider(mode = "Virement", comptes = listOf(banque(v = VerificationStatut.REJETE)), confirme = true),
        )
    }

    @Test fun `le depassement du plafond demande confirmation`() {
        val f = fournisseur(plafond = 500.0)
        assertEquals(PaymentDecision.Proceed(), decider(montant = 500.0, f = f))
        assertEquals(PaymentDecision.NeedConfirmation(PaymentGuardReason.AU_DESSUS_PLAFOND), decider(montant = 500.01, f = f))
        assertEquals(PaymentDecision.Proceed(PaymentGuardReason.AU_DESSUS_PLAFOND), decider(montant = 900.0, f = f, confirme = true))
    }

    @Test fun `fournisseur bloque ou statut interdit sont refuses meme avec confirmation`() {
        assertEquals(PaymentDecision.Reject(PaymentGuardReason.PAIEMENT_BLOQUE), decider(f = fournisseur(bloque = true), confirme = true))
        assertEquals(
            PaymentDecision.Reject(PaymentGuardReason.STATUT_INTERDIT),
            decider(f = fournisseur(statut = FournisseurStatus.ARCHIVE), confirme = true),
        )
    }

    @Test fun `un montant invalide est refuse`() {
        assertEquals(PaymentDecision.Reject(PaymentGuardReason.MONTANT_INVALIDE), decider(montant = Double.NaN))
    }

    // --- Choix du compte bénéficiaire ---

    @Test fun `le compte principal est prefere puis le plus fiable`() {
        val comptes = listOf(
            banque(id = 1, v = VerificationStatut.A_VERIFIER),
            banque(id = 2, v = VerificationStatut.VERIFIE),
            banque(id = 3, v = VerificationStatut.REJETE, principal = true),
        )
        assertEquals(3L, SupplierAccountRules.compteBeneficiaire(comptes, PaymentMode.VIREMENT)?.id)
        assertEquals(2L, SupplierAccountRules.compteBeneficiaire(comptes.filter { !it.principal }, PaymentMode.VIREMENT)?.id)
    }

    @Test fun `un compte explicite doit etre un candidat du mode`() {
        val comptes = listOf(banque(id = 1), mobile(id = 2))
        assertEquals(1L, SupplierAccountRules.compteBeneficiaire(comptes, PaymentMode.VIREMENT, compteId = 1)?.id)
        assertNull(SupplierAccountRules.compteBeneficiaire(comptes, PaymentMode.VIREMENT, compteId = 2))
        assertNull(SupplierAccountRules.compteBeneficiaire(comptes, PaymentMode.VIREMENT, compteId = 99))
        assertEquals(2L, SupplierAccountRules.compteBeneficiaire(comptes, PaymentMode.MOBILE_MONEY)?.id)
    }

    @Test fun `les especes et autres modes n ont pas de compte beneficiaire`() {
        val comptes = listOf(banque())
        assertNull(SupplierAccountRules.compteBeneficiaire(comptes, PaymentMode.ESPECES))
        assertNull(SupplierAccountRules.compteBeneficiaire(comptes, PaymentMode.AUTRE))
    }
}
