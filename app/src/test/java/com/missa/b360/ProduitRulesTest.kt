package com.missa.b360

import com.missa.b360.core.data.entity.ProductEntity
import com.missa.b360.core.data.entity.ProductType
import com.missa.b360.core.domain.model.ProduitRules
import com.missa.b360.core.domain.model.ProfilActivite
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Ce qu'un article peut faire selon sa nature.
 *
 * Ces règles ferment des portes qui étaient ouvertes : vendre une matière
 * première, commander au fournisseur un produit qu'on fabrique soi-même, ou
 * consommer un produit fini comme composant.
 */
class ProduitRulesTest {

    private var sequence = 0L

    private fun article(type: ProductType, actif: Boolean = true) = ProductEntity(
        id = ++sequence,
        code = "ART-$sequence",
        nom = "Article $sequence",
        type = type,
        prixVente = 1_000.0,
        createdAt = 0L,
        active = actif,
    )

    @Test
    fun `une matiere premiere ne se vend pas`() {
        assertFalse(ProduitRules.estVendable(ProductType.MATIERE_PREMIERE))
        assertTrue(ProduitRules.estAchetable(ProductType.MATIERE_PREMIERE))
    }

    @Test
    fun `un produit fabrique se vend mais ne s achete pas`() {
        assertTrue(ProduitRules.estVendable(ProductType.FABRIQUE))
        assertFalse(ProduitRules.estAchetable(ProductType.FABRIQUE))
    }

    @Test
    fun `un consommable ne se vend pas et n entre pas en production`() {
        assertFalse(ProduitRules.estVendable(ProductType.CONNOMMABLE))
        assertFalse(ProduitRules.estComposant(ProductType.CONNOMMABLE))
        assertTrue(ProduitRules.estAchetable(ProductType.CONNOMMABLE))
    }

    @Test
    fun `un article achete-revendu couvre la vente, l achat et la production`() {
        assertTrue(ProduitRules.estVendable(ProductType.ACHATE_REVENDU))
        assertTrue(ProduitRules.estAchetable(ProductType.ACHATE_REVENDU))
        assertTrue(ProduitRules.estComposant(ProductType.ACHATE_REVENDU))
    }

    @Test
    fun `un produit fini n entre pas dans sa propre fabrication`() {
        assertTrue(ProduitRules.estFabricable(ProductType.FABRIQUE))
        assertFalse(ProduitRules.estComposant(ProductType.FABRIQUE))
    }

    @Test
    fun `un sous-ensemble compose se fabrique et se consomme`() {
        assertTrue(ProduitRules.estFabricable(ProductType.COMPOSE))
        assertTrue(ProduitRules.estComposant(ProductType.COMPOSE))
    }

    @Test
    fun `le catalogue de vente ecarte matieres et consommables`() {
        val catalogue = listOf(
            article(ProductType.ACHATE_REVENDU),
            article(ProductType.MATIERE_PREMIERE),
            article(ProductType.FABRIQUE),
            article(ProductType.CONNOMMABLE),
        )
        val vendables = ProduitRules.vendables(catalogue)
        assertEquals(2, vendables.size)
        assertTrue(vendables.none { it.type == ProductType.MATIERE_PREMIERE })
    }

    @Test
    fun `le catalogue d achat ecarte ce qui se fabrique`() {
        val catalogue = listOf(
            article(ProductType.ACHATE_REVENDU),
            article(ProductType.MATIERE_PREMIERE),
            article(ProductType.FABRIQUE),
            article(ProductType.COMPOSE),
        )
        val achetables = ProduitRules.achetables(catalogue)
        assertEquals(2, achetables.size)
        assertTrue(achetables.none { it.type == ProductType.FABRIQUE })
    }

    @Test
    fun `un article desactive ne figure dans aucun catalogue`() {
        val catalogue = listOf(
            article(ProductType.ACHATE_REVENDU, actif = false),
            article(ProductType.MATIERE_PREMIERE, actif = false),
            article(ProductType.FABRIQUE, actif = false),
        )
        assertTrue(ProduitRules.vendables(catalogue).isEmpty())
        assertTrue(ProduitRules.achetables(catalogue).isEmpty())
        assertTrue(ProduitRules.composants(catalogue).isEmpty())
        assertTrue(ProduitRules.fabricables(catalogue).isEmpty())
    }

    @Test
    fun `chaque nature sait au moins entrer ou sortir du stock`() {
        // Aucun type ne doit être inutilisable partout : ce serait un article
        // qu'on ne peut ni acheter, ni vendre, ni produire, ni consommer.
        ProductType.entries.forEach { type ->
            val utile = ProduitRules.estVendable(type) ||
                ProduitRules.estAchetable(type) ||
                ProduitRules.estComposant(type) ||
                ProduitRules.estFabricable(type)
            assertTrue("Nature inutilisable : $type", utile)
        }
    }

    @Test
    fun `un fabricant achete de la matiere, un commercant de la marchandise`() {
        assertEquals(
            ProductType.MATIERE_PREMIERE,
            ProduitRules.natureAchatParDefaut(ProfilActivite.APSV),
        )
        assertEquals(
            ProductType.ACHATE_REVENDU,
            ProduitRules.natureAchatParDefaut(ProfilActivite.ASV),
        )
        assertEquals(ProductType.ACHATE_REVENDU, ProduitRules.natureAchatParDefaut(null))
    }
}
