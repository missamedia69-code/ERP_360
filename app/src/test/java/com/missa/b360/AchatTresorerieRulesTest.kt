package com.missa.b360

import com.missa.b360.core.data.entity.SensMouvement
import com.missa.b360.core.domain.model.AchatTresorerieRules
import com.missa.b360.core.domain.model.DecaissementAchat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Une facture d'achat réglée à la validation fait SORTIR l'argent (et jamais entrer). */
class AchatTresorerieRulesTest {
    private fun decaisser(
        paye: Double = 100.0, deja: Boolean = false, compte: Long? = 3, solde: Double? = 500.0,
    ) = AchatTresorerieRules.decaissementALaValidation(paye, deja, compte, solde)

    @Test fun `le montant paye sort de la tresorerie`() {
        val d = decaisser(paye = 100.0)
        assertTrue(d is DecaissementAchat.Sortie)
        d as DecaissementAchat.Sortie
        assertEquals(SensMouvement.OUT, d.sens)
        assertEquals(3L, d.compteId)
        assertEquals(100.0, d.montant, 0.0)
    }

    @Test fun `le montant est arrondi au centime et jamais egal au total`() {
        assertEquals(12.35, (decaisser(paye = 12.346, solde = 50.0) as DecaissementAchat.Sortie).montant, 0.0001)
    }

    @Test fun `un solde insuffisant refuse la sortie`() {
        assertEquals(DecaissementAchat.SoldeInsuffisant, decaisser(paye = 600.0, solde = 500.0))
        assertEquals(DecaissementAchat.SoldeInsuffisant, decaisser(solde = null))
        assertEquals(DecaissementAchat.SoldeInsuffisant, decaisser(solde = Double.NaN))
    }

    @Test fun `un solde exactement egal au montant suffit`() {
        assertTrue(decaisser(paye = 500.0, solde = 500.0) is DecaissementAchat.Sortie)
    }

    @Test fun `rien n est ecrit sans montant, sans compte ou si deja enregistre`() {
        assertEquals(DecaissementAchat.Aucun, decaisser(paye = 0.0))
        assertEquals(DecaissementAchat.Aucun, decaisser(paye = -5.0))
        assertEquals(DecaissementAchat.Aucun, decaisser(paye = Double.NaN))
        assertEquals(DecaissementAchat.Aucun, decaisser(compte = null))
        assertEquals(DecaissementAchat.Aucun, decaisser(deja = true))
    }
}
