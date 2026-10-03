package com.missa.b360

import com.missa.b360.core.data.entity.FournisseurEntity
import com.missa.b360.core.data.entity.FournisseurStatus
import com.missa.b360.core.data.entity.VerificationStatut
import com.missa.b360.core.domain.model.PaymentGuard
import com.missa.b360.core.domain.model.PaymentGuardReason
import com.missa.b360.core.domain.model.PaymentMode
import com.missa.b360.core.domain.model.PaymentVerdict
import com.missa.b360.core.domain.model.SupplierAccountInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class PaymentGuardTest {
    private val jour = 86_400_000L
    private val now = 1_000L * jour

    private fun fournisseur(
        statut: FournisseurStatus = FournisseurStatus.ACTIF,
        plafond: Double = 0.0,
        bloque: Boolean = false,
    ) = FournisseurEntity(
        code = "FRN-2026-0001", nom = "Atelier Nord", telephone = "650000000",
        statut = statut, plafondPaiement = plafond, paiementBloque = bloque, createdAt = 0,
    )

    private fun compte(v: VerificationStatut, ilYaJours: Long = 90) = SupplierAccountInfo(v, now - ilYaJours * jour)

    private fun garde(
        f: FournisseurEntity = fournisseur(),
        montant: Double = 100.0,
        mode: PaymentMode = PaymentMode.ESPECES,
        c: SupplierAccountInfo? = null,
    ) = PaymentGuard.evaluer(f, montant, mode, c, now)

    @Test
    fun `montant invalide est refuse en premier`() {
        listOf(Double.NaN, Double.POSITIVE_INFINITY, 0.0, -5.0).forEach {
            assertEquals(PaymentVerdict.Refuser(PaymentGuardReason.MONTANT_INVALIDE), garde(montant = it))
        }
        assertEquals(
            PaymentVerdict.Refuser(PaymentGuardReason.MONTANT_INVALIDE),
            garde(f = fournisseur(FournisseurStatus.ARCHIVE), montant = Double.NaN),
        )
    }

    @Test
    fun `statut interdit pour brouillon a valider et archive`() {
        listOf(FournisseurStatus.BROUILLON, FournisseurStatus.A_VALIDER, FournisseurStatus.ARCHIVE).forEach {
            assertEquals(it.name, PaymentVerdict.Refuser(PaymentGuardReason.STATUT_INTERDIT), garde(f = fournisseur(it)))
        }
    }

    @Test
    fun `actif suspendu et bloque peuvent etre payes pour honorer une facture validee`() {
        listOf(FournisseurStatus.ACTIF, FournisseurStatus.SUSPENDU, FournisseurStatus.BLOQUE).forEach {
            assertEquals(it.name, PaymentVerdict.Autorise, garde(f = fournisseur(it)))
        }
    }

    @Test
    fun `paiement bloque est refuse`() {
        assertEquals(PaymentVerdict.Refuser(PaymentGuardReason.PAIEMENT_BLOQUE), garde(f = fournisseur(bloque = true)))
    }

    @Test
    fun `especes et autre n exigent aucun compte`() {
        assertEquals(PaymentVerdict.Autorise, garde(mode = PaymentMode.ESPECES, c = null))
        assertEquals(PaymentVerdict.Autorise, garde(mode = PaymentMode.AUTRE, c = null))
    }

    @Test
    fun `virement ou mobile money sans compte est refuse`() {
        assertEquals(PaymentVerdict.Refuser(PaymentGuardReason.COMPTE_REQUIS), garde(mode = PaymentMode.VIREMENT))
        assertEquals(PaymentVerdict.Refuser(PaymentGuardReason.COMPTE_REQUIS), garde(mode = PaymentMode.MOBILE_MONEY))
    }

    @Test
    fun `compte rejete est refuse`() {
        assertEquals(
            PaymentVerdict.Refuser(PaymentGuardReason.COMPTE_REJETE),
            garde(mode = PaymentMode.VIREMENT, c = compte(VerificationStatut.REJETE)),
        )
    }

    @Test
    fun `compte verifie autorise`() {
        assertEquals(PaymentVerdict.Autorise, garde(mode = PaymentMode.VIREMENT, c = compte(VerificationStatut.VERIFIE)))
    }

    @Test
    fun `compte non verifie recent est refuse a six jours et confirme a sept et huit`() {
        fun verdict(jours: Long) = garde(mode = PaymentMode.MOBILE_MONEY, c = compte(VerificationStatut.A_VERIFIER, jours))
        assertEquals(PaymentVerdict.Refuser(PaymentGuardReason.COMPTE_RECENT_NON_VERIFIE), verdict(6))
        assertEquals(PaymentVerdict.Confirmer(PaymentGuardReason.COMPTE_NON_VERIFIE), verdict(7))
        assertEquals(PaymentVerdict.Confirmer(PaymentGuardReason.COMPTE_NON_VERIFIE), verdict(8))
    }

    @Test
    fun `compte de date inconnue est traite comme ancien`() {
        assertEquals(
            PaymentVerdict.Confirmer(PaymentGuardReason.COMPTE_NON_VERIFIE),
            garde(mode = PaymentMode.VIREMENT, c = SupplierAccountInfo(VerificationStatut.A_VERIFIER, 0L)),
        )
    }

    @Test
    fun `plafond zero signifie aucun plafond`() {
        assertEquals(PaymentVerdict.Autorise, garde(f = fournisseur(plafond = 0.0), montant = 9_999_999.0))
    }

    @Test
    fun `plafond egal autorise et au dessus demande confirmation`() {
        val f = fournisseur(plafond = 500.0)
        assertEquals(PaymentVerdict.Autorise, garde(f = f, montant = 500.0))
        assertEquals(PaymentVerdict.Confirmer(PaymentGuardReason.AU_DESSUS_PLAFOND), garde(f = f, montant = 500.01))
    }

    @Test
    fun `moyen de paiement libre devient un mode`() {
        assertEquals(PaymentMode.ESPECES, PaymentMode.depuis("Espèces"))
        assertEquals(PaymentMode.ESPECES, PaymentMode.depuis("Caisse"))
        assertEquals(PaymentMode.MOBILE_MONEY, PaymentMode.depuis("Mobile Money"))
        assertEquals(PaymentMode.MOBILE_MONEY, PaymentMode.depuis("Orange Money"))
        assertEquals(PaymentMode.VIREMENT, PaymentMode.depuis("Virement bancaire"))
        assertEquals(PaymentMode.AUTRE, PaymentMode.depuis("Chèque"))
        assertEquals(PaymentMode.AUTRE, PaymentMode.depuis(null))
        assertEquals(PaymentMode.AUTRE, PaymentMode.depuis("Troc"))
    }
}
