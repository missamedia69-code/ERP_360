package com.missa.b360

import com.missa.b360.core.domain.model.DevisCommandeRules
import com.missa.b360.core.domain.model.SaleLine
import com.missa.b360.core.domain.model.SaleRecordPayload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Règles pures du cycle devis → commande → facture (spec §20) —
 * testables sans base.
 */
class DevisCommandeRulesTest {

    private fun devis(): SaleRecordPayload = SaleRecordPayload(
        clientId = 5,
        clientName = "Client",
        lines = listOf(
            SaleLine(id = 1, name = "A", unitPrice = 10.0, quantity = 2.0, productId = 7L),
            SaleLine(id = 9, name = "Livraison lib", unitPrice = 5.0, quantity = 1.0),
        ),
        subtotal = 25.0,
        discount = 5.0,
        delivery = 0.0,
        taxRate = 19.25,
        taxAmount = 3.22,
        total = 23.22,
        paymentMethod = "",
        paidAmount = 0.0,
        note = "devis",
    )

    @Test
    fun `la copie remplace les identifiants de lignes et rattache a la piece source`() {
        val source = devis()
        val copie = DevisCommandeRules.payloadCopie(source, sourceRecordId = 42L)

        assertEquals(source.clientId, copie.clientId)
        assertEquals(source.lines.size, copie.lines.size)
        assertEquals(listOf(1L, 2L), copie.lines.map { it.id })
        assertEquals(42L, copie.sourceRecordId)
        assertNull(copie.note)
        // Contenu commercial conservé.
        assertEquals(source.subtotal, copie.subtotal, 0.0001)
        assertEquals(source.discount, copie.discount, 0.0001)
        assertEquals(source.taxRate, copie.taxRate, 0.0001)
        assertEquals(source.total, copie.total, 0.0001)
        // La source n'est pas mutée.
        assertEquals(listOf(1L, 9L), source.lines.map { it.id })
        assertNull(source.sourceRecordId)
    }

    @Test
    fun `sans source le champ reste vide`() {
        val copie = DevisCommandeRules.payloadCopie(devis(), sourceRecordId = null)
        assertNull(copie.sourceRecordId)
        assertNotEquals(9L, copie.lines[1].id)
    }

    @Test
    fun `une commande est facturee des quune vente y est rattachee`() {
        val ventes = listOf(
            SaleRecordPayload(
                clientId = 5,
                clientName = "Client",
                lines = listOf(SaleLine(id = 1, name = "A", unitPrice = 10.0, quantity = 2.0, productId = 7L)),
                subtotal = 20.0,
                discount = 0.0,
                delivery = 0.0,
                taxRate = 0.0,
                taxAmount = 0.0,
                total = 20.0,
                paymentMethod = "Espèces",
                paidAmount = 20.0,
                sourceRecordId = 77L,
            ),
        )
        assertTrue(DevisCommandeRules.estFacturee(ventes, 77L))
        assertFalse(DevisCommandeRules.estFacturee(ventes, 78L))
        assertFalse(DevisCommandeRules.estFacturee(emptyList(), 77L))
    }
}
