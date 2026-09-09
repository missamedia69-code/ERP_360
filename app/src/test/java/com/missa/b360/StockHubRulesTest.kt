package com.missa.b360

import com.missa.b360.core.data.dao.StockMovementView
import com.missa.b360.core.data.entity.GroupeArticleEntity
import com.missa.b360.core.data.entity.ProductEntity
import com.missa.b360.core.data.entity.ProductStockEntity
import com.missa.b360.core.data.entity.StockMovementType
import com.missa.b360.core.data.entity.SiteEntity
import com.missa.b360.core.domain.model.StockHubRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Règles de l'accueil Stock (réunion de données pour le hub) — logique pure. */
class StockHubRulesTest {

    private val maintenant: Long = 1000L * 60 * 60 * 24 * 30 // 30 jours en ms

    private fun produit(id: Long, nom: String, groupeId: Long? = null, cout: Double = 10.0) =
        ProductEntity(
            id = id,
            code = "PRD-$id",
            nom = nom,
            itemGroupId = groupeId,
            prixRevient = cout,
            stockMin = 0.0,
            createdAt = 0L,
        )

    private fun groupe(id: Long, code: String, nom: String) = GroupeArticleEntity(
        id = id,
        code = code,
        nom = nom,
        createdAt = 0L,
        updatedAt = 0L,
    )

    private fun mouvement(
        id: Long,
        siteId: Long,
        produitNom: String,
        type: StockMovementType,
        quantite: Double,
        horodatage: Long,
    ) = StockMovementView(
        id = id,
        siteId = siteId,
        produitNom = produitNom,
        produitCode = "PRD-$id",
        siteNom = "Site $siteId",
        type = type.name,
        quantite = quantite,
        motif = type.name,
        reference = null,
        commentaire = null,
        horodatage = horodatage,
    )

    @Test
    fun `parGroupe compte les fiches et valorise le stock du groupe`() {
        val marchandises = groupe(1, "MARCH", "Marchandises")
        val matieres = groupe(2, "MP", "Matières premières")
        val produits = listOf(
            produit(10, "Bière", groupeId = 1, cout = 500.0),
            produit(11, "Huile", groupeId = 1, cout = 200.0),
            produit(12, "Cacao", groupeId = 2, cout = 800.0),
            produit(13, "Farine", groupeId = 2, cout = 300.0),
        )
        val stocks = listOf(
            ProductStockEntity(produitId = 10, siteId = 1, quantite = 4.0),
            ProductStockEntity(produitId = 11, siteId = 1, quantite = 6.0),
            ProductStockEntity(produitId = 12, siteId = 1, quantite = 2.0),
            ProductStockEntity(produitId = 13, siteId = 1, quantite = 3.0),
        )
        val resultat = StockHubRules.parGroupe(produits, stocks, listOf(marchandises, matieres))
        assertEquals(2, resultat.size)
        // Marchandises : 2 fiches, 4*500 + 6*200 = 3 200 ; triées par valeur.
        val marchX = resultat.first { it.code == "MARCH" }
        assertEquals(2, marchX.nombreArticles)
        assertEquals(3200.0, marchX.valeur, 0.0001)
        val matX = resultat.first { it.code == "MP" }
        assertEquals(2, matX.nombreArticles)
        assertEquals(2200.0, matX.valeur, 0.0001)
        assertTrue(resultat[0].valeur >= resultat[1].valeur)
    }

    @Test
    fun `construire renvoie les derniers mouvements avec le nom du produit`() {
        val site = SiteEntity(id = 1, nom = "Principal", type = "entrepôt")
        val produits = listOf(produit(10, "Bière", groupeId = 1))
        val stocks = listOf(ProductStockEntity(produitId = 10, siteId = 1, quantite = 5.0))
        val groupes = listOf(groupe(1, "MARCH", "Marchandises"))
        val mouvements = listOf(
            mouvement(7, 1, "Bière", StockMovementType.SORTIE, 1.0, maintenant - 10_000),
            mouvement(8, 1, "Huile", StockMovementType.ENTREE, 2.0, maintenant - 5_000),
        )
        val hub = StockHubRules.construire(
            produits = produits,
            stocks = stocks,
            mouvements = mouvements,
            groupes = groupes,
            sites = listOf(site),
            depotId = null,
            maintenant = maintenant,
        )
        assertEquals("Bière", hub.derniersMouvements[0].produitNom)
        assertEquals("Huile", hub.derniersMouvements[1].produitNom)
        assertEquals(StockMovementType.SORTIE, hub.derniersMouvements[0].type)
        assertEquals(1, hub.groupes.size)
    }

    @Test
    fun `les mouvements du jour et d hier distinguent la journée civile`() {
        val debutAujourdhui = maintenant - (maintenant % 86_400_000L)
        val aujourdhui = mouvement(1, 1, "A", StockMovementType.ENTREE, 1.0, debutAujourdhui + 1000)
        val hier = mouvement(2, 1, "B", StockMovementType.SORTIE, 1.0, debutAujourdhui - 86_400_000L + 1000)
        val avantHier = mouvement(3, 1, "C", StockMovementType.ENTREE, 1.0, debutAujourdhui - 2 * 86_400_000L)
        assertEquals(1, StockHubRules.mouvementsDuJour(listOf(aujourdhui, hier, avantHier), maintenant))
        assertEquals(1, StockHubRules.mouvementsHier(listOf(aujourdhui, hier, avantHier), maintenant))
    }

    @Test
    fun `la tendance des mouvements est nulle sans activité la veille`() {
        val debutAujourdhui = maintenant - (maintenant % 86_400_000L)
        val hub = StockHubRules.construire(
            produits = emptyList(),
            stocks = emptyList(),
            mouvements = listOf(
                mouvement(1, 1, "A", StockMovementType.ENTREE, 1.0, debutAujourdhui + 1000),
            ),
            groupes = emptyList(),
            sites = emptyList(),
            depotId = null,
            maintenant = maintenant,
        )
        assertNull(hub.tendanceMouvements)
        // Avec une activité hier identique, la tendance vaut 0 %.
        val hubAvecHier = StockHubRules.construire(
            produits = emptyList(),
            stocks = emptyList(),
            mouvements = listOf(
                mouvement(1, 1, "A", StockMovementType.ENTREE, 1.0, debutAujourdhui + 1000),
                mouvement(2, 1, "A", StockMovementType.ENTREE, 1.0, debutAujourdhui - 86_400_000L + 1000),
            ),
            groupes = emptyList(),
            sites = emptyList(),
            depotId = null,
            maintenant = maintenant,
        )
        assertEquals(0.0, hubAvecHier.tendanceMouvements!!, 0.0001)
    }

    @Test
    fun `minutesDepuisActivite est nul sans mouvement et mesure l écart sinon`() {
        assertNull(StockHubRules.minutesDepuisDerniereActivite(emptyList(), maintenant))
        val mouvement = mouvement(1, 1, "A", StockMovementType.ENTREE, 1.0, maintenant - 120_000)
        assertEquals(2, StockHubRules.minutesDepuisDerniereActivite(listOf(mouvement), maintenant))
    }
}
