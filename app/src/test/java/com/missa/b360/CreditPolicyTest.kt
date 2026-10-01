package com.missa.b360

import com.missa.b360.core.data.entity.ClientStatus
import com.missa.b360.core.domain.model.CreditInput
import com.missa.b360.core.domain.model.CreditPolicy
import com.missa.b360.core.domain.model.CreditPolicyConfig
import com.missa.b360.core.domain.model.CreditReason
import com.missa.b360.core.domain.model.RiskLevel
import com.missa.b360.core.domain.model.SaleVerdict
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CreditPolicyTest {
    private fun input(
        statut: ClientStatus = ClientStatus.ACTIF,
        limite: Double? = 1000.0,
        encours: Double = 0.0,
        enRetard: Double = 0.0,
        jours: Int = 0,
    ) = CreditInput(statut, limite, encours, enRetard, jours)

    private fun risque(i: CreditInput) = CreditPolicy.evaluate(i).risque

    @Test fun `client sans encours ni retard est normal`() {
        val result = CreditPolicy.evaluate(input(encours = 100.0))
        assertEquals(RiskLevel.NORMAL, result.risque)
        assertEquals(10.0, result.utilisationPct!!, 1e-9)
        assertTrue(result.raisons.isEmpty())
    }

    @Test fun `seuils dutilisation de la limite 80 et 100 pour cent`() {
        assertEquals(RiskLevel.NORMAL, risque(input(encours = 799.0)))
        assertEquals(RiskLevel.ATTENTION, risque(input(encours = 800.0)))
        assertEquals(RiskLevel.ATTENTION, risque(input(encours = 999.0)))
        assertEquals(RiskLevel.ELEVE, risque(input(encours = 1000.0)))
        assertEquals(RiskLevel.ELEVE, risque(input(encours = 5000.0)))
    }

    @Test fun `seuils de retard 30 et 60 jours sont stricts`() {
        assertEquals(RiskLevel.NORMAL, risque(input(enRetard = 10.0, jours = 30)))
        assertEquals(RiskLevel.ATTENTION, risque(input(enRetard = 10.0, jours = 31)))
        assertEquals(RiskLevel.ATTENTION, risque(input(enRetard = 10.0, jours = 60)))
        assertEquals(RiskLevel.ELEVE, risque(input(enRetard = 10.0, jours = 61)))
    }

    @Test fun `des jours de retard sans montant echu sont ignores`() {
        assertEquals(RiskLevel.NORMAL, risque(input(enRetard = 0.0, jours = 120)))
    }

    @Test fun `limite illimitee ne produit aucune utilisation mais le retard compte`() {
        val gros = CreditPolicy.evaluate(input(limite = null, encours = 1e9))
        assertNull(gros.utilisationPct)
        assertEquals(RiskLevel.NORMAL, gros.risque)
        assertEquals(RiskLevel.ATTENTION, risque(input(limite = null, enRetard = 5.0, jours = 45)))
    }

    @Test fun `limite nulle est saturee des quil y a un encours`() {
        val vide = CreditPolicy.evaluate(input(limite = 0.0, encours = 0.0))
        assertEquals(RiskLevel.NORMAL, vide.risque)
        assertEquals(0.0, vide.utilisationPct!!, 1e-9)
        val use = CreditPolicy.evaluate(input(limite = 0.0, encours = 1.0))
        assertEquals(RiskLevel.ELEVE, use.risque)
        assertEquals(Double.POSITIVE_INFINITY, use.utilisationPct!!, 0.0)
    }

    @Test fun `statuts de blocage donnent le niveau bloque quel que soit lencours`() {
        val credit = CreditPolicy.evaluate(input(statut = ClientStatus.BLOQUE_CREDIT))
        assertEquals(RiskLevel.BLOQUE, credit.risque)
        assertEquals(listOf(CreditReason.STATUT_BLOQUE_CREDIT), credit.raisons)
        val admin = CreditPolicy.evaluate(input(statut = ClientStatus.BLOQUE_ADMINISTRATIF))
        assertEquals(RiskLevel.BLOQUE, admin.risque)
        assertEquals(listOf(CreditReason.STATUT_BLOQUE_ADMINISTRATIF), admin.raisons)
    }

    @Test fun `sous surveillance est au moins en attention et les raisons vont de la plus grave`() {
        assertEquals(RiskLevel.ATTENTION, risque(input(statut = ClientStatus.SOUS_SURVEILLANCE)))
        val result = CreditPolicy.evaluate(
            input(statut = ClientStatus.SOUS_SURVEILLANCE, encours = 850.0, enRetard = 10.0, jours = 70),
        )
        assertEquals(RiskLevel.ELEVE, result.risque)
        assertEquals(
            listOf(CreditReason.RETARD_CRITIQUE, CreditReason.LIMITE_PROCHE, CreditReason.SOUS_SURVEILLANCE),
            result.raisons,
        )
    }

    @Test fun `montants non finis ou negatifs sont traites comme zero`() {
        assertEquals(RiskLevel.NORMAL, risque(input(encours = Double.NaN)))
        assertEquals(RiskLevel.NORMAL, risque(input(encours = -50.0)))
        assertEquals(RiskLevel.NORMAL, risque(input(enRetard = Double.NaN, jours = 200)))
    }

    @Test fun `config personnalisee modifie les seuils`() {
        val config = CreditPolicyConfig(attentionUtilisationPct = 50.0, eleveUtilisationPct = 90.0)
        assertEquals(RiskLevel.ATTENTION, CreditPolicy.evaluate(input(encours = 500.0), config).risque)
        assertEquals(RiskLevel.ELEVE, CreditPolicy.evaluate(input(encours = 900.0), config).risque)
    }

    @Test fun `vente refusee pour un montant invalide`() {
        listOf(0.0, -5.0, Double.NaN, Double.POSITIVE_INFINITY).forEach { montant ->
            assertEquals(SaleVerdict.Block(CreditReason.MONTANT_INVALIDE), CreditPolicy.canSell(input(), montant))
        }
        assertEquals(SaleVerdict.Block(CreditReason.MONTANT_INVALIDE), CreditPolicy.canSell(input(), 100.0, -1.0))
        assertEquals(SaleVerdict.Block(CreditReason.MONTANT_INVALIDE), CreditPolicy.canSell(input(), 100.0, 101.0))
    }

    @Test fun `vente refusee pour un client inactif ou archive meme payee comptant`() {
        listOf(ClientStatus.INACTIF, ClientStatus.ARCHIVE, ClientStatus.DESACTIVE).forEach { statut ->
            assertEquals(
                SaleVerdict.Block(CreditReason.STATUT_INACTIF),
                CreditPolicy.canSell(input(statut = statut), 100.0, 100.0),
            )
        }
    }

    @Test fun `blocage administratif refuse tout, blocage credit accepte seulement le comptant`() {
        val admin = input(statut = ClientStatus.BLOQUE_ADMINISTRATIF)
        assertEquals(SaleVerdict.Block(CreditReason.STATUT_BLOQUE_ADMINISTRATIF), CreditPolicy.canSell(admin, 100.0, 100.0))
        val credit = input(statut = ClientStatus.BLOQUE_CREDIT)
        assertEquals(SaleVerdict.Allow, CreditPolicy.canSell(credit, 100.0, 100.0))
        assertEquals(SaleVerdict.Block(CreditReason.STATUT_BLOQUE_CREDIT), CreditPolicy.canSell(credit, 100.0, 40.0))
        assertEquals(SaleVerdict.Block(CreditReason.STATUT_BLOQUE_CREDIT), CreditPolicy.canSell(credit, 100.0))
    }

    @Test fun `fiche en brouillon ou a completer ne peut pas acheter a credit`() {
        listOf(ClientStatus.BROUILLON, ClientStatus.A_COMPLETER).forEach { statut ->
            val i = input(statut = statut)
            assertEquals(SaleVerdict.Block(CreditReason.FICHE_INCOMPLETE), CreditPolicy.canSell(i, 100.0))
            assertEquals(SaleVerdict.Allow, CreditPolicy.canSell(i, 100.0, 100.0))
        }
    }

    @Test fun `vente a credit dans la limite est autorisee jusqua la limite incluse`() {
        assertEquals(SaleVerdict.Allow, CreditPolicy.canSell(input(encours = 100.0), 200.0))
        assertEquals(SaleVerdict.Allow, CreditPolicy.canSell(input(encours = 100.0), 900.0))
    }

    @Test fun `vente qui depasse la limite avertit sans bloquer`() {
        val verdict = CreditPolicy.canSell(input(encours = 100.0), 901.0)
        assertEquals(SaleVerdict.Warn(CreditReason.LIMITE_DEPASSEE_PAR_VENTE, RiskLevel.ELEVE), verdict)
    }

    @Test fun `seule la part a credit compte pour la limite`() {
        val i = input(encours = 900.0)
        assertTrue(CreditPolicy.canSell(i, 500.0, 0.0) is SaleVerdict.Warn)
        assertTrue(CreditPolicy.canSell(i, 500.0, 450.0).let { it is SaleVerdict.Warn && it.raison == CreditReason.LIMITE_PROCHE })
        assertEquals(SaleVerdict.Allow, CreditPolicy.canSell(i, 500.0, 500.0))
    }

    @Test fun `risque attention ou eleve avertit avec la raison la plus grave`() {
        val attention = CreditPolicy.canSell(input(encours = 100.0, enRetard = 5.0, jours = 40), 50.0)
        assertEquals(SaleVerdict.Warn(CreditReason.RETARD_MODERE, RiskLevel.ATTENTION), attention)
        val eleve = CreditPolicy.canSell(input(encours = 100.0, enRetard = 5.0, jours = 90), 50.0)
        assertEquals(SaleVerdict.Warn(CreditReason.RETARD_CRITIQUE, RiskLevel.ELEVE), eleve)
    }

    @Test fun `client sans limite et sans retard peut toujours acheter a credit`() {
        assertEquals(SaleVerdict.Allow, CreditPolicy.canSell(input(limite = null, encours = 1e9), 1e6))
    }
}
