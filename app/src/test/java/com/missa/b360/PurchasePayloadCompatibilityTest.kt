package com.missa.b360

import com.missa.b360.core.domain.model.CommandeAchatCodec
import com.missa.b360.core.domain.model.CommandeAchatLigne
import com.missa.b360.core.domain.model.CommandeAchatPayload
import com.missa.b360.core.domain.model.PurchaseLine
import com.missa.b360.core.domain.model.PurchaseRecordCodec
import com.missa.b360.core.domain.model.PurchaseRecordPayload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PurchasePayloadCompatibilityTest {

    @Test
    fun `une ancienne facture sans echeance se decode avec null`() {
        val ancien = """{"schemaVersion":1,"supplierId":7,"supplierName":"F","lines":[{"id":1,"name":"x","unitPrice":5.0,"quantity":2.0}],""" +
            """"subtotal":10.0,"taxRate":0.0,"taxAmount":0.0,"total":10.0,"paymentMethod":"Espèces","paidAmount":0.0}"""
        val payload = PurchaseRecordCodec.decode(ancien)
        assertNotNull(payload)
        assertNull(payload?.dateEcheance)
        assertEquals(10.0, payload?.total ?: -1.0, 0.0)
    }

    @Test
    fun `une ancienne commande sans livraison prevue se decode avec null`() {
        val ancien = """{"schemaVersion":1,"supplierId":7,"supplierName":"F","lines":[{"id":1,"name":"x","quantity":2.0,"unitPrice":5.0}]}"""
        val payload = CommandeAchatCodec.decode(ancien)
        assertNotNull(payload)
        assertNull(payload?.dateLivraisonPrevue)
    }

    @Test
    fun `les nouveaux champs font l aller retour sans toucher au reste`() {
        val facture = PurchaseRecordPayload(
            supplierId = 7, supplierName = "F",
            lines = listOf(PurchaseLine(id = 1, name = "x", unitPrice = 5.0, quantity = 2.0)),
            subtotal = 10.0, taxRate = 0.0, taxAmount = 0.0, total = 10.0, paymentMethod = "Espèces", paidAmount = 2.0,
            dateEcheance = 1_700_000_000_000L,
        )
        val relue = PurchaseRecordCodec.decode(PurchaseRecordCodec.encode(facture))
        assertEquals(facture, relue)
        assertEquals(1, relue?.schemaVersion)

        val commande = CommandeAchatPayload(
            supplierId = 7, supplierName = "F",
            lines = listOf(CommandeAchatLigne(id = 1, name = "x", quantity = 2.0, unitPrice = 5.0)),
            dateLivraisonPrevue = 1_700_000_000_000L,
        )
        assertEquals(commande, CommandeAchatCodec.decode(CommandeAchatCodec.encode(commande)))
    }

    @Test
    fun `un json avec une cle inconnue reste lisible`() {
        val futur = """{"schemaVersion":1,"supplierId":7,"supplierName":"F","lines":[],"cleFuture":42,"dateLivraisonPrevue":5}"""
        assertEquals(5L, CommandeAchatCodec.decode(futur)?.dateLivraisonPrevue)
    }
}
