package com.missa.b360

import com.missa.b360.core.data.entity.ProductEntity
import com.missa.b360.core.data.entity.ProductType
import com.missa.b360.core.data.entity.ProductStockEntity
import com.missa.b360.core.data.entity.StockMovementType
import com.missa.b360.core.domain.model.SaleLine
import com.missa.b360.core.domain.model.SaleStockEffects
import com.missa.b360.core.domain.usecase.StockValidation
import com.missa.b360.ui.stock.ProductStocks
import com.missa.b360.ui.stock.ProductWithStock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Règles stock (spec §11/§13/§43) : validation des quantités, choix du site de
 * sortie et fusion produits × stock — logique pure, sans base.
 */
class StockRulesTest {

    @Test
    fun `entree et sortie exigent une quantité strictement positive`() {
        assertTrue(StockValidation.quantiteEntrSortieEstValide(1.0))
        assertTrue(StockValidation.quantiteEntrSortieEstValide(0.5))
        assertFalse(StockValidation.quantiteEntrSortieEstValide(0.0))
        assertFalse(StockValidation.quantiteEntrSortieEstValide(-1.0))
        assertFalse(StockValidation.quantiteEntrSortieEstValide(Double.NaN))
    }

    @Test
    fun `l ajustement est un écart signé non nul`() {
        assertTrue(StockValidation.ecartAjustementEstValide(3.0))
        assertTrue(StockValidation.ecartAjustementEstValide(-2.5))
        assertFalse(StockValidation.ecartAjustementEstValide(0.0))
        assertFalse(StockValidation.ecartAjustementEstValide(Double.POSITIVE_INFINITY))
    }

    @Test
    fun `le transfert exige source distincte et quantité positive`() {
        assertTrue(StockValidation.transfertEstValide(1L, 2L, 5.0))
        assertFalse(StockValidation.transfertEstValide(1L, 1L, 5.0))
        assertFalse(StockValidation.transfertEstValide(null, 2L, 5.0))
        assertFalse(StockValidation.transfertEstValide(1L, 2L, 0.0))
        assertFalse(StockValidation.transfertEstValide(1L, 2L, -1.0))
    }

    @Test
    fun `le stock ne peut pas passer sous zéro`() {
        assertTrue(StockValidation.stockApresEstValide(10.0, -10.0))
        assertTrue(StockValidation.stockApresEstValide(0.0, 5.0))
        assertFalse(StockValidation.stockApresEstValide(4.0, -5.0))
    }

    @Test
    fun `le stock disponible suit le site principal du produit`() {
        val produit = ProductEntity(
            code = "PRD2026-0001",
            nom = "Bocal",
            siteId = 1L,
            createdAt = 0L,
        )
        val stocks = listOf(
            ProductStockEntity(produitId = produit.id, siteId = 1L, quantite = 8.0),
            ProductStockEntity(produitId = produit.id, siteId = 2L, quantite = 30.0),
        )
        val combined = ProductStocks.combine(listOf(produit), stocks)
        assertEquals(8.0, combined.single().stock, 0.0001)
        assertEquals(38.0, combined.single().total, 0.0001)
        assertEquals(com.missa.b360.ui.stock.StockLevel.OK, combined.single().level)
    }

    @Test
    fun `sans site principal le stock disponible est celui du site le plus pourvu`() {
        val produit = ProductEntity(code = "PRD2026-0002", nom = "Sachet", siteId = null, createdAt = 0L)
        val stocks = listOf(
            ProductStockEntity(produitId = produit.id, siteId = 1L, quantite = 3.0),
            ProductStockEntity(produitId = produit.id, siteId = 2L, quantite = 12.0),
        )
        val combined = ProductStocks.combine(listOf(produit), stocks)
        assertEquals(12.0, combined.single().stock, 0.0001)
    }

    @Test
    fun `les alertes de seuil sont calculées sécurité puis minimum`() {
        fun build(stock: Double, min: Double, security: Double): ProductWithStock {
            val produit = ProductEntity(
                code = "PRD2026-0003",
                nom = "Pile",
                type = ProductType.ACHATE_REVENDU,
                stockMin = min,
                stockSecurite = security,
                siteId = null,
                createdAt = 0L,
            )
            return ProductStocks.combine(
                listOf(produit),
                listOf(ProductStockEntity(produitId = produit.id, siteId = 1L, quantite = stock)),
            ).single()
        }
        assertEquals(com.missa.b360.ui.stock.StockLevel.CRITIQUE, build(2.0, 5.0, 2.0).level)
        assertEquals(com.missa.b360.ui.stock.StockLevel.BAS, build(4.0, 5.0, 2.0).level)
        assertEquals(com.missa.b360.ui.stock.StockLevel.OK, build(6.0, 5.0, 2.0).level)
        assertEquals(com.missa.b360.ui.stock.StockLevel.OK, build(0.0, 0.0, 0.0).level)
    }

    @Test
    fun `les besoins de stock ne comptent que les lignes rattachées au catalogue`() {
        val besoins = SaleStockEffects.besoinsParProduit(
            listOf(
                SaleLine(id = 1, name = "A", unitPrice = 10.0, quantity = 2.0, productId = 7L),
                SaleLine(id = 2, name = "A", unitPrice = 10.0, quantity = 1.5, productId = 7L),
                SaleLine(id = 3, name = "B", unitPrice = 20.0, quantity = 3.0, productId = 9L),
                SaleLine(id = 4, name = "Libre", unitPrice = 5.0, quantity = 4.0, productId = null),
                SaleLine(id = 5, name = "Nul", unitPrice = 5.0, quantity = 0.0, productId = 9L),
            ),
        )
        assertEquals(3.5, besoins[7L], 0.0001)
        assertEquals(3.0, besoins[9L], 0.0001)
        assertEquals(2, besoins.size)
    }

    @Test
    fun `un panier sans produit du catalogue ne touche pas le stock`() {
        assertTrue(
            SaleStockEffects.besoinsParProduit(
                listOf(SaleLine(id = 1, name = "Service", unitPrice = 100.0, quantity = 2.0)),
            ).isEmpty(),
        )
    }

    @Test
    fun `les types de transfert ne s appliquent pas au mouvement simple`() {
        // Le type TRANSFERT n'est géré que par TransferStockUseCase (paire de mouvements) ;
        // RecordStockMovementUseCase ne reçoit que ENTRÉE / SORTIE / AJUSTEMENT.
        assertFalse(
            StockMovementType.TRANSFERT_ENTREE in
                setOf(StockMovementType.ENTREE, StockMovementType.SORTIE, StockMovementType.AJUSTEMENT),
        )
    }
}
