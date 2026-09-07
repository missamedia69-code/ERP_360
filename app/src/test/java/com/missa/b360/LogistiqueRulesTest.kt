package com.missa.b360

import com.missa.b360.core.data.entity.OperationModule
import com.missa.b360.core.data.entity.OperationRecordEntity
import com.missa.b360.core.data.entity.OperationStatus
import com.missa.b360.core.data.entity.ProductEntity
import com.missa.b360.core.data.entity.ProductStockEntity
import com.missa.b360.core.data.entity.SiteEntity
import com.missa.b360.core.data.entity.StockMovementEntity
import com.missa.b360.core.data.entity.StockMovementType
import com.missa.b360.core.domain.model.EtatTransfert
import com.missa.b360.core.domain.model.LogistiqueRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Vue transverse de la logistique : implantation, transferts, livraisons. */
class LogistiqueRulesTest {

    private var sequence = 0L

    private fun site(id: Long, nom: String = "Site $id") =
        SiteEntity(id = id, nom = nom, type = "boutique")

    private fun produit(id: Long, prixRevient: Double?, prixAchat: Double? = null) = ProductEntity(
        id = id,
        code = "ART-$id",
        nom = "Article $id",
        prixVente = 10_000.0,
        prixAchat = prixAchat,
        prixRevient = prixRevient,
        createdAt = 0L,
    )

    private fun stock(produitId: Long, siteId: Long, quantite: Double) =
        ProductStockEntity(produitId = produitId, siteId = siteId, quantite = quantite)

    private fun mouvement(
        type: StockMovementType,
        siteId: Long,
        quantite: Double,
        reference: String?,
        horodatage: Long = 1_000L,
    ) = StockMovementEntity(
        id = ++sequence,
        produitId = 1L,
        siteId = siteId,
        type = type,
        quantite = quantite,
        motif = "Test",
        reference = reference,
        horodatage = horodatage,
    )

    private fun livraison(statut: OperationStatus) = OperationRecordEntity(
        id = ++sequence,
        module = OperationModule.LIVRAISON.name,
        reference = "BL-$sequence",
        title = "Livraison $sequence",
        status = statut.name,
        createdAt = 1_000L,
    )

    @Test
    fun `le stock est valorise au cout de revient, pas au prix de vente`() {
        val resultat = LogistiqueRules.stockParSite(
            listOf(site(1)),
            listOf(stock(1, 1, 10.0)),
            listOf(produit(1, prixRevient = 3_000.0)),
        )
        assertEquals(30_000.0, resultat.first().valeur, 0.001)
    }

    @Test
    fun `sans cout de revient le prix d achat prend le relais`() {
        val resultat = LogistiqueRules.stockParSite(
            listOf(site(1)),
            listOf(stock(1, 1, 4.0)),
            listOf(produit(1, prixRevient = null, prixAchat = 2_500.0)),
        )
        assertEquals(10_000.0, resultat.first().valeur, 0.001)
    }

    @Test
    fun `les sites sont classes par valeur decroissante`() {
        val resultat = LogistiqueRules.stockParSite(
            listOf(site(1, "Petit"), site(2, "Gros")),
            listOf(stock(1, 1, 1.0), stock(1, 2, 50.0)),
            listOf(produit(1, prixRevient = 1_000.0)),
        )
        assertEquals("Gros", resultat.first().site.nom)
    }

    @Test
    fun `les ruptures et les references sont comptees separement`() {
        val resultat = LogistiqueRules.stockParSite(
            listOf(site(1)),
            listOf(stock(1, 1, 5.0), stock(2, 1, 0.0), stock(3, 1, 2.0)),
            listOf(
                produit(1, 100.0),
                produit(2, 100.0),
                produit(3, 100.0),
            ),
        )
        assertEquals(2, resultat.first().references)
        assertEquals(1, resultat.first().ruptures)
    }

    @Test
    fun `un stock qui reference un produit supprime est ignore`() {
        val resultat = LogistiqueRules.stockParSite(
            listOf(site(1)),
            listOf(stock(99, 1, 10.0)),
            listOf(produit(1, 100.0)),
        )
        assertEquals(0.0, resultat.first().valeur, 0.001)
        assertEquals(0, resultat.first().references)
    }

    @Test
    fun `une sortie appariee a son entree donne un transfert recu`() {
        val transferts = LogistiqueRules.transferts(
            listOf(
                mouvement(StockMovementType.TRANSFERT_SORTIE, 1, 5.0, "TRF-1"),
                mouvement(StockMovementType.TRANSFERT_ENTREE, 2, 5.0, "TRF-1"),
            ),
        )
        assertEquals(1, transferts.size)
        assertEquals(EtatTransfert.RECU, transferts.first().etat)
        assertEquals(1L, transferts.first().siteSource)
        assertEquals(2L, transferts.first().siteDestination)
    }

    @Test
    fun `une sortie sans entree reste en transit`() {
        val transferts = LogistiqueRules.transferts(
            listOf(mouvement(StockMovementType.TRANSFERT_SORTIE, 1, 8.0, "TRF-2")),
        )
        assertEquals(EtatTransfert.EN_TRANSIT, transferts.first().etat)
        assertEquals(1, LogistiqueRules.enTransit(transferts).size)
    }

    @Test
    fun `une entree sans sortie signale une saisie incomplete`() {
        val transferts = LogistiqueRules.transferts(
            listOf(mouvement(StockMovementType.TRANSFERT_ENTREE, 2, 3.0, "TRF-3")),
        )
        assertEquals(EtatTransfert.ORPHELIN, transferts.first().etat)
    }

    @Test
    fun `les mouvements ordinaires ne sont pas des transferts`() {
        val transferts = LogistiqueRules.transferts(
            listOf(
                mouvement(StockMovementType.ENTREE, 1, 10.0, "ACH-1"),
                mouvement(StockMovementType.SORTIE, 1, 4.0, "VTE-1"),
                mouvement(StockMovementType.AJUSTEMENT, 1, -1.0, "INV-1"),
            ),
        )
        assertTrue(transferts.isEmpty())
    }

    @Test
    fun `un transfert sans reference ne peut pas etre reconstitue`() {
        val transferts = LogistiqueRules.transferts(
            listOf(mouvement(StockMovementType.TRANSFERT_SORTIE, 1, 5.0, null)),
        )
        assertTrue(transferts.isEmpty())
    }

    @Test
    fun `les transferts non recus remontent en tete`() {
        val transferts = LogistiqueRules.transferts(
            listOf(
                mouvement(StockMovementType.TRANSFERT_SORTIE, 1, 5.0, "TRF-A", horodatage = 5_000L),
                mouvement(StockMovementType.TRANSFERT_ENTREE, 2, 5.0, "TRF-A", horodatage = 5_100L),
                mouvement(StockMovementType.TRANSFERT_SORTIE, 1, 2.0, "TRF-B", horodatage = 1_000L),
            ),
        )
        assertEquals("TRF-A", transferts.first().reference)
        assertEquals(EtatTransfert.EN_TRANSIT, transferts.last().etat)
    }

    @Test
    fun `le taux de service ignore les livraisons annulees`() {
        val suivi = LogistiqueRules.suiviLivraisons(
            listOf(
                livraison(OperationStatus.VALIDATED),
                livraison(OperationStatus.VALIDATED),
                livraison(OperationStatus.VALIDATED),
                livraison(OperationStatus.DRAFT),
                livraison(OperationStatus.CANCELLED),
            ),
        )
        assertEquals(3, suivi.effectuees)
        assertEquals(1, suivi.enPreparation)
        assertEquals(1, suivi.annulees)
        assertEquals(75.0, suivi.tauxService, 0.01)
    }

    @Test
    fun `sans livraison le taux de service ne divise pas par zero`() {
        assertEquals(0.0, LogistiqueRules.suiviLivraisons(emptyList()).tauxService, 0.001)
    }

    @Test
    fun `la concentration mesure la part du site le plus charge`() {
        val sites = LogistiqueRules.stockParSite(
            listOf(site(1), site(2)),
            listOf(stock(1, 1, 75.0), stock(1, 2, 25.0)),
            listOf(produit(1, 1_000.0)),
        )
        assertEquals(75.0, LogistiqueRules.concentration(sites), 0.01)
    }

    @Test
    fun `un site unique n indique aucune concentration a corriger`() {
        val sites = LogistiqueRules.stockParSite(
            listOf(site(1)),
            listOf(stock(1, 1, 10.0)),
            listOf(produit(1, 1_000.0)),
        )
        assertEquals(0.0, LogistiqueRules.concentration(sites), 0.001)
    }

    @Test
    fun `chaque etat de transfert porte un libelle traduit`() {
        EtatTransfert.entries.forEach { assertTrue(it.libelleRes != 0) }
    }
}
