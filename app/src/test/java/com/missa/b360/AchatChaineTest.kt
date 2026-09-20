package com.missa.b360

import com.missa.b360.core.domain.model.AchatCommandeRules
import com.missa.b360.core.domain.model.AchatReportRules
import com.missa.b360.core.domain.model.CommandeAchatCodec
import com.missa.b360.core.domain.model.CommandeAchatLigne
import com.missa.b360.core.domain.model.CommandeAchatPayload
import com.missa.b360.core.domain.model.PurchaseRecordCodec
import com.missa.b360.core.domain.model.ReceptionCodec
import com.missa.b360.core.domain.model.ReceptionLigne
import com.missa.b360.core.domain.model.ReceptionPayload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Chaîne d'achat : commande → réception → facture → règlement → annulation.
 * Garde-fous purs et compatibilité des payloads.
 */
class AchatChaineTest {

    private fun reception(
        productId: Long,
        quantite: Double,
        commandee: Double = 0.0,
    ) = ReceptionLigne(
        productId = productId,
        name = "P$productId",
        quantiteCommandee = commandee,
        quantiteRecue = quantite,
    )

    // --- Réception bornée par la commande ---

    @Test
    fun `le reste a recevoir se calcule par produit`() {
        assertEquals(5.0, AchatCommandeRules.restantARecevoir(10.0, 5.0), 1e-9)
        assertEquals(0.0, AchatCommandeRules.restantARecevoir(10.0, 12.0), 1e-9)
    }

    @Test
    fun `une reception dans la limite de la commande est valide`() {
        assertTrue(
            AchatCommandeRules.receptionEstValide(
                commandeeParProduit = mapOf(7L to 10.0),
                dejaRecuParProduit = mapOf(7L to 4.0),
                reception = listOf(reception(7L, 6.0)),
            ),
        )
    }

    @Test
    fun `une reception qui depasse la commande est refusee`() {
        assertFalse(
            AchatCommandeRules.receptionEstValide(
                commandeeParProduit = mapOf(7L to 10.0),
                dejaRecuParProduit = mapOf(7L to 4.0),
                reception = listOf(reception(7L, 7.0)),
            ),
        )
    }

    @Test
    fun `plusieurs receptions partielles se cumulent`() {
        assertFalse(
            AchatCommandeRules.receptionEstValide(
                commandeeParProduit = mapOf(7L to 10.0),
                dejaRecuParProduit = mapOf(7L to 9.5),
                reception = listOf(reception(7L, 1.0)),
            ),
        )
    }

    @Test
    fun `une reception sans quantite positive est refusee`() {
        assertFalse(
            AchatCommandeRules.receptionEstValide(
                commandeeParProduit = mapOf(7L to 10.0),
                dejaRecuParProduit = emptyMap(),
                reception = listOf(reception(7L, 0.0)),
            ),
        )
    }

    @Test
    fun `une commande soldee ne laisse plus rien a recevoir`() {
        assertTrue(
            AchatCommandeRules.commandeSoldee(
                commandeeParProduit = mapOf(7L to 10.0, 8L to 2.0),
                recuParProduit = mapOf(7L to 10.0, 8L to 2.0),
            ),
        )
        assertFalse(
            AchatCommandeRules.commandeSoldee(
                commandeeParProduit = mapOf(7L to 10.0),
                recuParProduit = mapOf(7L to 9.0),
            ),
        )
    }

    // --- Règlements numérotés ---

    @Test
    fun `un reglement partiel puis le solde sont valides`() {
        assertTrue(AchatCommandeRules.reglementEstValide(total = 100.0, dejaRegle = 0.0, montant = 40.0))
        assertTrue(AchatCommandeRules.reglementEstValide(total = 100.0, dejaRegle = 40.0, montant = 60.0))
        assertEquals(60.0, AchatCommandeRules.restantARegler(100.0, 40.0), 1e-9)
    }

    @Test
    fun `un reglement excessif ou nul est refuse`() {
        assertFalse(AchatCommandeRules.reglementEstValide(total = 100.0, dejaRegle = 40.0, montant = 61.0))
        assertFalse(AchatCommandeRules.reglementEstValide(total = 100.0, dejaRegle = 0.0, montant = 0.0))
        assertFalse(AchatCommandeRules.reglementEstValide(total = 100.0, dejaRegle = 0.0, montant = -5.0))
    }

    @Test
    fun `chaque reglement porte une reference unique`() {
        assertEquals("FFR2026-0001", AchatCommandeRules.referenceReglement("FFR2026-0001", 1))
        assertEquals("FFR2026-0001-R2", AchatCommandeRules.referenceReglement("FFR2026-0001", 2))
        assertEquals("FFR2026-0001-R3", AchatCommandeRules.referenceReglement("FFR2026-0001", 3))
        assertEquals("FFR2026-0001-ANN", AchatCommandeRules.referenceAnnulation("FFR2026-0001"))
    }

    // --- Reporting ---

    @Test
    fun `les depenses sont bornees par la fenetre`() {
        val donnees = listOf(100L to 10.0, 200L to 20.0, 300L to 30.0)
        assertEquals(30.0, AchatReportRules.depenses(donnees, 100L, 300L), 1e-9)
        assertEquals(60.0, AchatReportRules.depenses(donnees, 0L, 400L), 1e-9)
    }

    @Test
    fun `l evolution est nulle sans reference`() {
        assertNull(AchatReportRules.evolution(50.0, 0.0))
        assertEquals(50.0, AchatReportRules.evolution(150.0, 100.0)!!, 1e-9)
        assertEquals(-25.0, AchatReportRules.evolution(75.0, 100.0)!!, 1e-9)
    }

    @Test
    fun `le top fournisseurs est trie et limite`() {
        val top = AchatReportRules.topFournisseurs(
            listOf("A" to 10.0, "B" to 90.0, "A" to 20.0, "C" to 5.0, "D" to 1.0, "E" to 2.0, "F" to 3.0),
            limite = 3,
        )
        // B (90) > A (10 + 20 = 30) > C (5) — F (3), E (2) et D (1) sont hors du top 3.
        assertEquals(listOf("B", "A", "C"), top.map { it.nom })
        assertEquals(30.0, top[1].total, 1e-9)
        assertEquals(2, top[1].nombre)
    }

    @Test
    fun `la repartition par mois suit les fenetres`() {
        val points = AchatReportRules.parMois(
            listOf(50L to 10.0, 150L to 20.0, 250L to 30.0),
            listOf(0L to 100L, 100L to 200L, 200L to 300L),
        )
        assertEquals(listOf(10.0, 20.0, 30.0), points.map { it.total })
    }

    // --- Payloads : aller-retour et compatibilité ascendante ---

    @Test
    fun `les payloads commande et reception survivent a l encodage`() {
        val commande = CommandeAchatPayload(
            supplierId = 3,
            supplierName = "Missa SARL",
            lines = listOf(CommandeAchatLigne(id = 1, name = "Ciment", quantity = 50.0, unitPrice = 5_000.0, productId = 7)),
            note = "Livraison lundi",
        )
        assertEquals(commande, CommandeAchatCodec.decode(CommandeAchatCodec.encode(commande)))

        val reception = ReceptionPayload(
            commandeRecordId = 12,
            commandeReference = "B2026-0001",
            supplierId = 3,
            supplierName = "Missa SARL",
            lignes = listOf(reception(7L, 48.0, commandee = 50.0).copy(lot = "LOT-9", datePeremption = 1_900_000_000_000L)),
        )
        assertEquals(reception, ReceptionCodec.decode(ReceptionCodec.encode(reception)))
    }

    @Test
    fun `une ancienne facture sans nouveaux champs se decode toujours`() {
        // Payload encodé avant la chaîne commande/réception et les pièces jointes.
        val ancien = """{"supplierId":1,"supplierName":"S","lines":[],"subtotal":100.0,""" +
            """"taxRate":19.25,"taxAmount":16.17,"total":100.0,"paymentMethod":"Espèces","paidAmount":100.0}"""
        val payload = PurchaseRecordCodec.decode(ancien)
        assertNotNull(payload)
        assertEquals(emptyList<String>(), payload!!.attachments)
        assertNull(payload.receptionRecordId)
        assertNull(payload.commandeRecordId)
        assertEquals(100.0, payload.paidAmount, 1e-9)
    }
}
