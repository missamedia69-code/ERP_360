package com.missa.b360

import com.missa.b360.core.data.entity.ProductType
import com.missa.b360.core.domain.model.ProduitRules
import com.missa.b360.core.domain.model.PurchaseLine
import com.missa.b360.core.domain.model.PurchaseStockEffects
import com.missa.b360.core.domain.model.ValorisationRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Règles pures de la réception d'achat (spec §6) : valorisation CUMP,
 * traçabilité lot/série/péremption, imputation directe des non-stockables.
 */
class ValorisationAchatTest {

    private fun ligne(
        id: Long,
        productId: Long? = null,
        quantity: Double = 1.0,
        unitPrice: Double = 100.0,
        lot: String? = null,
        numeroSerie: String? = null,
        datePeremption: Long? = null,
    ) = PurchaseLine(
        id = id,
        name = "Ligne $id",
        unitPrice = unitPrice,
        quantity = quantity,
        productId = productId,
        lot = lot,
        numeroSerie = numeroSerie,
        datePeremption = datePeremption,
    )

    // --- Valorisation CUMP ---

    @Test
    fun `la reception ajoute le cout d achat a la valeur du stock`() {
        // 10 unités à 1 000 de valeur, réception de 5 à 160 l'unité (800).
        assertEquals(1_800.0, ValorisationRules.nouvelleValeur(1_000.0, 800.0), 0.001)
    }

    @Test
    fun `le cump se deduit de la valeur et de la quantite`() {
        // 10 + 5 = 15 unités pour 1 800 → CUMP 120.
        assertEquals(120.0, ValorisationRules.cump(15.0, 1_800.0), 0.001)
    }

    @Test
    fun `un stock vide n a pas de cump et ne divise jamais par zero`() {
        assertEquals(0.0, ValorisationRules.cump(0.0, 500.0), 0.001)
    }

    @Test
    fun `la valeur ne devient jamais negative`() {
        assertEquals(0.0, ValorisationRules.nouvelleValeur(100.0, -500.0), 0.001)
    }

    // --- Traçabilité : un mouvement par ligne tracée, agrégation pour le reste ---

    @Test
    fun `une ligne avec lot ou serie ou peremption est tracee`() {
        val lignes = listOf(
            ligne(1, productId = 7, lot = "LOT-A"),
            ligne(2, productId = 7, numeroSerie = "SN-1"),
            ligne(3, productId = 7, datePeremption = 1_900_000_000_000L),
            ligne(4, productId = 7),
        )
        assertEquals(setOf(1L, 2L, 3L), PurchaseStockEffects.idsTracees(lignes))
    }

    @Test
    fun `les lignes libres ne sont jamais tracees`() {
        val lignes = listOf(ligne(1, productId = null, lot = "LOT-X"))
        assertTrue(PurchaseStockEffects.lignesTracees(lignes).isEmpty())
    }

    @Test
    fun `l agregation ignore les lignes deja tracees`() {
        val lignes = listOf(
            ligne(1, productId = 7, quantity = 2.0, lot = "LOT-A"),
            ligne(2, productId = 7, quantity = 3.0),
            ligne(3, productId = 7, quantity = 4.0),
        )
        // 2 unités tracées (mouvement propre) + 7 agrégées.
        assertEquals(mapOf(7L to 7.0), PurchaseStockEffects.besoinsParProduitSansTracees(lignes))
        assertEquals(mapOf(7L to 9.0), PurchaseStockEffects.besoinsParProduit(lignes))
    }

    @Test
    fun `le cout d achat est calcule par produit`() {
        val lignes = listOf(
            ligne(1, productId = 7, quantity = 2.0, unitPrice = 100.0),
            ligne(2, productId = 7, quantity = 1.0, unitPrice = 250.0),
            ligne(3, productId = null, quantity = 5.0, unitPrice = 999.0),
        )
        assertEquals(mapOf(7L to 450.0), PurchaseStockEffects.coutParProduit(lignes))
    }

    // --- Prestations : achetables mais non stockables, jamais valorisées ---

    @Test
    fun `une prestation s achete mais ne se stocke pas`() {
        assertTrue(ProduitRules.estAchetable(ProductType.PRESTATION))
        assertFalse(ProduitRules.estStockable(ProductType.PRESTATION))
        assertFalse(ProduitRules.estValorise(ProductType.PRESTATION))
    }

    @Test
    fun `les familles stockees sans valeur ne sont pas valorisees`() {
        assertFalse(ProduitRules.estValorise(ProductType.DECHET_NON_VALORISABLE))
        assertFalse(ProduitRules.estValorise(ProductType.CONSIGNATION))
        // Ce qui se stocke et s'achète porte une valeur.
        assertTrue(ProduitRules.estValorise(ProductType.ACHATE_REVENDU))
        assertTrue(ProduitRules.estValorise(ProductType.MATIERE_PREMIERE))
        assertTrue(ProduitRules.estValorise(ProductType.DECHET_VALORISABLE))
    }
}
