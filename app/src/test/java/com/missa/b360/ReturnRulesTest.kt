package com.missa.b360

import com.missa.b360.core.domain.model.PurchaseRecordCodec
import com.missa.b360.core.domain.model.PurchaseRecordPayload
import com.missa.b360.core.domain.model.InventoryRules
import com.missa.b360.core.domain.model.PurchaseLine
import com.missa.b360.core.domain.model.PurchaseStockEffects
import com.missa.b360.core.domain.model.SaleLine
import com.missa.b360.core.domain.model.SaleRecordCodec
import com.missa.b360.core.domain.model.SaleRecordPayload
import com.missa.b360.core.domain.model.ReturnRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Règles pures du retour de vente (spec §22), de l'achat (spec §6)
 * et de l'inventaire (spec §12) — testables sans base.
 */
class ReturnRulesTest {

    private fun facture(): SaleRecordPayload = SaleRecordPayload(
        clientId = 1,
        clientName = "Client",
        lines = listOf(
            SaleLine(id = 1, name = "A", unitPrice = 10.0, quantity = 5.0, productId = 7L),
            SaleLine(id = 2, name = "B", unitPrice = 20.0, quantity = 2.0),
        ),
        subtotal = 90.0,
        discount = 0.0,
        delivery = 0.0,
        taxRate = 0.0,
        taxAmount = 0.0,
        total = 90.0,
        paymentMethod = "Espèces",
        paidAmount = 40.0,
    )

    private fun avoir(lignes: List<SaleLine>): SaleRecordPayload = SaleRecordPayload(
        clientId = 1,
        clientName = "Client",
        lines = lignes,
        subtotal = lignes.sumOf { it.total },
        discount = 0.0,
        delivery = 0.0,
        taxRate = 0.0,
        taxAmount = 0.0,
        total = lignes.sumOf { it.total },
        paymentMethod = "Espèces",
        paidAmount = 0.0,
        sourceRecordId = 100L,
    )

    @Test
    fun `le restant retournable est la quantité vendue moins les avoirs precedents`() {
        val original = facture()
        val sansAvoirs = ReturnRules.restantParLigne(original, emptyList())
        assertEquals(5.0, sansAvoirs[ReturnRules.lineKey(original.lines[0])], 0.0001)
        assertEquals(2.0, sansAvoirs[ReturnRules.lineKey(original.lines[1])], 0.0001)

        val avecRetour = ReturnRules.restantParLigne(
            original,
            listOf(avoir(listOf(SaleLine(id = 9, name = "A", unitPrice = 10.0, quantity = 2.0, productId = 7L)))),
        )
        assertEquals(3.0, avecRetour[ReturnRules.lineKey(original.lines[0])], 0.0001)
    }

    @Test
    fun `un retour dans le restant est valide`() {
        val original = facture()
        val demande = mapOf(ReturnRules.lineKey(original.lines[0]) to 3.0)
        assertTrue(ReturnRules.retourEstValide(original, emptyList(), demande))
    }

    @Test
    fun `un retour au-dessus du restant est refuse`() {
        val original = facture()
        val demande = mapOf(ReturnRules.lineKey(original.lines[0]) to 5.01)
        assertFalse(ReturnRules.retourEstValide(original, emptyList(), demande))
    }

    @Test
    fun `une ligne inconnue de la facture est refusee`() {
        val original = facture()
        val demande = mapOf("N:inexistant" to 1.0)
        assertFalse(ReturnRules.retourEstValide(original, emptyList(), demande))
    }

    @Test
    fun `un retour vide est refuse`() {
        assertFalse(ReturnRules.retourEstValide(facture(), emptyList(), emptyMap()))
    }

    @Test
    fun `les avoirs reduisent le solde de la facture`() {
        val original = facture() // total 90, payé 40
        assertEquals(50.0, ReturnRules.soldeFacture(original, emptyList()), 0.0001)
        val avecAvoir = ReturnRules.soldeFacture(
            original,
            listOf(avoir(listOf(SaleLine(id = 9, name = "A", unitPrice = 10.0, quantity = 2.0, productId = 7L))))
        )
        assertEquals(30.0, avecAvoir, 0.0001)
    }

    @Test
    fun `les cles de ligne distinguent produits et libelles libres`() {
        val parProduit = ReturnRules.lineKey(SaleLine(1, "A", 10.0, 1.0, productId = 7L))
        val parNom = ReturnRules.lineKey(SaleLine(2, "A", 10.0, 1.0))
        assertTrue(parProduit != parNom)
        assertEquals(parNom, ReturnRules.lineKey(SaleLine(3, "  a ", 10.0, 1.0)))
    }

    // --- Achat (spec §6) ---

    @Test
    fun `les besoins de stock ne comptent que les lignes catalogue positives`() {
        val besoins = PurchaseStockEffects.besoinsParProduit(
            listOf(
                PurchaseLine(1, "A", 10.0, 2.0, productId = 7L),
                PurchaseLine(2, "A", 10.0, 1.5, productId = 7L),
                PurchaseLine(3, "Libre", 5.0, 4.0),
                PurchaseLine(4, "Nul", 5.0, 0.0, productId = 9L),
            ),
        )
        assertEquals(3.5, besoins[7L], 0.0001)
        assertEquals(1, besoins.size)
    }

    @Test
    fun `le codec achat est stable au round trip`() {
        val payload = PurchaseRecordPayload(
            supplierId = 4,
            supplierName = "Fournisseur",
            lines = listOf(PurchaseLine(1, "A", 12.0, 3.0, productId = 7L)),
            subtotal = 36.0,
            taxRate = 0.0,
            taxAmount = 0.0,
            total = 36.0,
            paymentMethod = "Espèces",
            paidAmount = 10.0,
        )
        val decode = PurchaseRecordCodec.decode(PurchaseRecordCodec.encode(payload))
        assertEquals(payload, decode)
        assertNull(PurchaseRecordCodec.decode(null))
    }

    @Test
    fun `le codec vente reste compatible sans sourceRecordId`() {
        val json = SaleRecordCodec.encode(facture())
        val decode = SaleRecordCodec.decode(json)
        assertEquals(facture(), decode)
        assertNull(SaleRecordCodec.decode("pas du json"))
    }

    // --- Inventaire (spec §12) ---

    @Test
    fun `lecart est signe et le seuil filtre les differences negligeables`() {
        assertEquals(2.0, InventoryRules.ecart(10.0, 12.0), 0.0001)
        assertEquals(-3.0, InventoryRules.ecart(10.0, 7.0), 0.0001)
        assertFalse(InventoryRules.ecartRequiertAjustement(0.0))
        assertFalse(InventoryRules.ecartRequiertAjustement(1e-12))
        assertTrue(InventoryRules.ecartRequiertAjustement(0.5))
        assertTrue(InventoryRules.ecartRequiertAjustement(-1.0))
    }

    @Test
    fun `le stock apres ajustement ne peut pas devenir negatif`() {
        assertTrue(InventoryRules.stockApresEstValide(10.0, -10.0))
        assertTrue(InventoryRules.stockApresEstValide(0.0, 5.0))
        assertFalse(InventoryRules.stockApresEstValide(4.0, -5.0))
    }
}
