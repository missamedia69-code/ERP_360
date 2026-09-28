package com.missa.b360

import com.missa.b360.core.domain.model.ProductionCodec
import com.missa.b360.core.domain.model.ProductionComponent
import com.missa.b360.core.domain.model.ProductionRecordPayload
import com.missa.b360.core.domain.model.ProductionRules
import com.missa.b360.core.domain.model.ProductionStockLocation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductionRulesTest {
    private fun payload(
        quantity: Double = 10.0,
        components: List<ProductionComponent> = listOf(ProductionComponent(2, "Bois", 40.0)),
    ) = ProductionRecordPayload(produitId = 1, produitNom = "Table", quantite = quantity, composants = components)

    @Test fun `OF valide exige produit quantité et au moins un composant positif`() {
        assertTrue(ProductionRules.payloadIsValid(payload()))
        assertFalse(ProductionRules.payloadIsValid(payload(quantity = 0.0)))
        assertFalse(ProductionRules.payloadIsValid(payload(components = emptyList())))
        assertFalse(ProductionRules.payloadIsValid(payload(components = listOf(ProductionComponent(2, "Bois", Double.NaN)))))
        assertFalse(ProductionRules.payloadIsValid(payload(components = listOf(ProductionComponent(1, "Table", 1.0)))))
    }

    @Test fun `les besoins en doublon sont agrégés avant la sortie Stock`() {
        val needs = ProductionRules.besoinsParComposant(
            payload(components = listOf(
                ProductionComponent(2, "Bois", 40.0),
                ProductionComponent(2, "Bois", 10.0),
                ProductionComponent(3, "Vis", 80.0),
            )),
        )
        assertEquals(mapOf(2L to 50.0, 3L to 80.0), needs)
    }

    @Test fun `un coût matière et un dépôt sérialisés doivent rester valides`() {
        assertTrue(ProductionRules.payloadIsValid(payload().copy(coutMatieres = 15_000.0, siteDestinationId = 4)))
        assertFalse(ProductionRules.payloadIsValid(payload().copy(coutMatieres = Double.NaN)))
        assertFalse(ProductionRules.payloadIsValid(payload().copy(siteDestinationId = 0)))
    }

    @Test fun `une matière peut être prélevée sur plusieurs dépôts avec le site principal en priorité`() {
        val allocations = ProductionRules.allocateAcrossSites(
            locations = listOf(
                ProductionStockLocation(siteId = 2, quantity = 3.0),
                ProductionStockLocation(siteId = 1, quantity = 4.0),
            ),
            required = 5.0,
            preferredSiteId = 1,
        )
        assertEquals(2, allocations?.size)
        assertEquals(1L, allocations?.first()?.siteId)
        assertEquals(4.0, allocations?.first()?.quantity ?: 0.0, 0.0)
        assertEquals(1.0, allocations?.last()?.quantity ?: 0.0, 0.0)
    }

    @Test fun `une disponibilité totale insuffisante ne planifie aucune sortie`() {
        assertEquals(
            null,
            ProductionRules.allocateAcrossSites(
                locations = listOf(ProductionStockLocation(siteId = 1, quantity = 2.0), ProductionStockLocation(siteId = 2, quantity = 2.0)),
                required = 5.0,
                preferredSiteId = 1,
            ),
        )
    }

    @Test fun `le codec accepte les anciens brouillons sans champs de valorisation`() {
        val oldPayload = """{"schemaVersion":1,"produitId":1,"produitNom":"Table","quantite":10.0,"composants":[{"productId":2,"nom":"Bois","quantite":40.0}]}"""
        val decoded = ProductionCodec.decode(oldPayload)
        assertTrue(decoded != null)
        assertEquals(null, decoded?.coutMatieres)
        assertEquals(null, decoded?.siteDestinationId)
        assertTrue(ProductionRules.payloadIsValid(decoded!!))
    }
}
