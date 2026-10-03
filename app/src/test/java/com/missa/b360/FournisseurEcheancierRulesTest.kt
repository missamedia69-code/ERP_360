package com.missa.b360

import com.missa.b360.core.domain.model.EcheanceTranche
import com.missa.b360.core.domain.model.FournisseurEcheancierRules
import com.missa.b360.core.domain.model.FournisseurFactureOuverte
import com.missa.b360.core.domain.model.PaymentPlan
import com.missa.b360.core.domain.model.PlanStatut
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FournisseurEcheancierRulesTest {
    private val jour = 86_400_000L
    private val now = 1_000L * jour

    private fun facture(id: Long, echeance: Long, reste: Double, fournisseur: Long = 1) = FournisseurFactureOuverte(
        recordId = id, reference = "FA-$id", fournisseurId = fournisseur,
        issuedAt = 0, dueAt = echeance, total = reste, outstanding = reste,
    )

    private fun plan(id: Long, facture: Long, montant: Double, statut: PlanStatut = PlanStatut.PLANIFIE) =
        PaymentPlan(id = id, factureRecordId = facture, datePrevue = now + jour, montant = montant, statut = statut)

    @Test fun `les tranches suivent l urgence`() {
        assertEquals(EcheanceTranche.EN_RETARD, FournisseurEcheancierRules.tranche(now - 1, now))
        assertEquals(EcheanceTranche.SEMAINE, FournisseurEcheancierRules.tranche(now, now))
        assertEquals(EcheanceTranche.SEMAINE, FournisseurEcheancierRules.tranche(now + 7 * jour, now))
        assertEquals(EcheanceTranche.MOIS, FournisseurEcheancierRules.tranche(now + 7 * jour + 1, now))
        assertEquals(EcheanceTranche.MOIS, FournisseurEcheancierRules.tranche(now + 30 * jour, now))
        assertEquals(EcheanceTranche.PLUS_TARD, FournisseurEcheancierRules.tranche(now + 30 * jour + 1, now))
    }

    @Test fun `les groupes sont ordonnes et sans tranche vide`() {
        val groupes = FournisseurEcheancierRules.construire(
            factures = listOf(facture(1, now + 60 * jour, 10.0), facture(2, now - 5 * jour, 20.0), facture(3, now - jour, 30.0)),
            noms = mapOf(1L to "Atelier"),
            plans = emptyList(),
            now = now,
        )
        assertEquals(listOf(EcheanceTranche.EN_RETARD, EcheanceTranche.PLUS_TARD), groupes.map { it.tranche })
        assertEquals(listOf(2L, 3L), groupes.first().lignes.map { it.facture.recordId })
        assertEquals(50.0, groupes.first().total, 0.001)
        assertEquals("Atelier", groupes.first().lignes.first().fournisseurNom)
    }

    @Test fun `seuls les plans planifies reduisent le reste a planifier`() {
        val groupes = FournisseurEcheancierRules.construire(
            factures = listOf(facture(1, now + jour, 100.0)),
            noms = emptyMap(),
            plans = listOf(
                plan(1, 1, 30.0),
                plan(2, 1, 20.0, PlanStatut.ANNULE),
                plan(3, 1, 10.0, PlanStatut.PAYE),
                plan(4, 2, 99.0),
            ),
            now = now,
        )
        val ligne = groupes.single().lignes.single()
        assertEquals(listOf(1L), ligne.plans.map { it.id })
        assertEquals(70.0, ligne.resteAPlanifier, 0.001)
    }

    @Test fun `un fournisseur inconnu garde un nom de repli et sans facture rien n est affiche`() {
        val g = FournisseurEcheancierRules.construire(listOf(facture(1, now, 5.0, fournisseur = 9)), emptyMap(), emptyList(), now)
        assertEquals("#9", g.single().lignes.single().fournisseurNom)
        assertTrue(FournisseurEcheancierRules.construire(emptyList(), emptyMap(), emptyList(), now).isEmpty())
    }
}
