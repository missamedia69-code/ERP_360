package com.missa.b360

import com.missa.b360.core.domain.model.PaymentPlan
import com.missa.b360.core.domain.model.PaymentPlanRules
import com.missa.b360.core.domain.model.PlanStatut
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PaymentPlanRulesTest {
    private fun plan(id: Long, montant: Double, date: Long = 100, statut: PlanStatut = PlanStatut.PLANIFIE, facture: Long = 1) =
        PaymentPlan(id, facture, date, montant, statut)

    @Test
    fun `un plan ne peut pas depasser le reste du`() {
        assertTrue(PaymentPlanRules.planifiable(1000.0, emptyList(), 1000.0))
        assertFalse(PaymentPlanRules.planifiable(1000.0, emptyList(), 1000.01))
        assertFalse(PaymentPlanRules.planifiable(1000.0, emptyList(), 0.0))
        assertFalse(PaymentPlanRules.planifiable(1000.0, emptyList(), Double.NaN))
    }

    @Test
    fun `plusieurs plans dans la limite du reste du`() {
        val existants = listOf(plan(1, 400.0), plan(2, 300.0))
        assertEquals(300.0, PaymentPlanRules.resteAPlanifier(1000.0, existants), 0.001)
        assertTrue(PaymentPlanRules.planifiable(1000.0, existants, 300.0))
        assertFalse(PaymentPlanRules.planifiable(1000.0, existants, 300.5))
    }

    @Test
    fun `les plans payes reportes ou annules ne reservent plus de montant`() {
        val existants = listOf(
            plan(1, 400.0, statut = PlanStatut.PAYE),
            plan(2, 300.0, statut = PlanStatut.REPORTE),
            plan(3, 200.0, statut = PlanStatut.ANNULE),
        )
        assertEquals(600.0, PaymentPlanRules.resteAPlanifier(600.0, existants), 0.001)
    }

    @Test
    fun `report conserve l ancienne date et cree une nouvelle ligne`() {
        val (ancien, nouveau) = PaymentPlanRules.reporter(plan(5, 250.0, date = 100), 200)!!
        assertEquals(PlanStatut.REPORTE, ancien.statut)
        assertEquals(100L, ancien.datePrevue)
        assertEquals(5L, ancien.id)
        assertEquals(PlanStatut.PLANIFIE, nouveau.statut)
        assertEquals(200L, nouveau.datePrevue)
        assertEquals(0L, nouveau.id)
        assertEquals(250.0, nouveau.montant, 0.0)
    }

    @Test
    fun `report impossible si deja traite ou meme date`() {
        assertNull(PaymentPlanRules.reporter(plan(1, 10.0, statut = PlanStatut.PAYE), 200))
        assertNull(PaymentPlanRules.reporter(plan(1, 10.0, statut = PlanStatut.REPORTE), 200))
        assertNull(PaymentPlanRules.reporter(plan(1, 10.0, date = 100), 100))
    }

    @Test
    fun `annulation seulement d un plan planifie`() {
        assertEquals(PlanStatut.ANNULE, PaymentPlanRules.annuler(plan(1, 10.0))?.statut)
        assertNull(PaymentPlanRules.annuler(plan(1, 10.0, statut = PlanStatut.PAYE)))
    }

    @Test
    fun `un reglement solde les plans qu il couvre dans l ordre des dates`() {
        val plans = listOf(
            plan(1, 300.0, date = 300),
            plan(2, 200.0, date = 100),
            plan(3, 150.0, date = 200),
            plan(4, 50.0, date = 50, facture = 2),
        )
        assertEquals(listOf(2L, 3L), PaymentPlanRules.plansPayes(plans, 1, 350.0))
        assertEquals(listOf(2L), PaymentPlanRules.plansPayes(plans, 1, 349.0))
        assertEquals(listOf(2L, 3L, 1L), PaymentPlanRules.plansPayes(plans, 1, 650.0))
        assertTrue(PaymentPlanRules.plansPayes(plans, 1, 199.0).isEmpty())
        assertTrue(PaymentPlanRules.plansPayes(plans, 1, Double.NaN).isEmpty())
    }
}
