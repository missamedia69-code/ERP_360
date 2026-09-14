package com.missa.b360

import com.missa.b360.core.data.dao.GroupeArticleComplet
import com.missa.b360.core.data.entity.ProductEntity
import com.missa.b360.core.data.entity.ProductStatus
import com.missa.b360.core.data.entity.ProductType
import com.missa.b360.core.domain.model.GroupesStandards
import com.missa.b360.core.domain.model.ModuleCode
import com.missa.b360.core.domain.model.ReglesGroupesArticles
import com.missa.b360.core.domain.model.ReglesGroupesArticles.estAchetable
import com.missa.b360.core.domain.model.ReglesGroupesArticles.estMaintenable
import com.missa.b360.core.domain.model.ReglesGroupesArticles.estProduisible
import com.missa.b360.core.domain.model.ReglesGroupesArticles.estService
import com.missa.b360.core.domain.model.ReglesGroupesArticles.estStocke
import com.missa.b360.core.domain.model.ReglesGroupesArticles.estValorise
import com.missa.b360.core.domain.model.ReglesGroupesArticles.estVendable
import com.missa.b360.core.domain.model.ReglesGroupesArticles.compteParDefaut
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Règles transverses des groupes d'articles (spec §5.2 et §8) : ce que chaque
 * module lit sur le groupe, et le pont article → groupe avec repli ProduitRules.
 */
class ReglesGroupesArticlesTest {

    private val maintenant = 1_000_000_000_000L

    /** Les sept familles standard, chacune avec un id distinct. */
    private fun groupes(referentiel: String? = "SYSCOHADA"): List<GroupeArticleComplet> =
        GroupesStandards.modeles(referentiel, maintenant).mapIndexed { index, modele ->
            val id = index + 1L
            GroupeArticleComplet(
                groupe = modele.groupe.copy(id = id),
                stock = modele.stock.copy(itemGroupId = id),
                achat = modele.achat.copy(itemGroupId = id),
                vente = modele.vente.copy(itemGroupId = id),
                production = modele.production.copy(itemGroupId = id),
                maintenance = modele.maintenance.copy(itemGroupId = id),
                comptabilite = modele.comptabilite.copy(itemGroupId = id),
            )
        }

    private fun parCode(code: String): GroupeArticleComplet = groupes().first { it.groupe.code == code }

    private fun article(
        type: ProductType = ProductType.ACHATE_REVENDU,
        groupeId: Long? = null,
        actif: Boolean = true,
    ) = ProductEntity(
        code = "PRD-$maintenant-$type-${groupeId ?: "SANS"}",
        nom = "Article test $type",
        type = type,
        itemGroupId = groupeId,
        statut = if (actif) ProductStatus.ACTIF else ProductStatus.DESACTIVE,
        active = actif,
        createdAt = maintenant,
    )

    private fun gruposVides(): List<GroupeArticleComplet> = emptyList()

    // --- Ce que chaque module lit sur le groupe. ---

    @Test
    fun `les marchandises s achetent se vendent et tiennent un stock valorise`() {
        val march = parCode("MARCH")
        assertTrue(march.estAchetable)
        assertTrue(march.estVendable)
        assertTrue(march.estStocke)
        assertTrue(march.estValorise)
        assertFalse(march.estService)
    }

    @Test
    fun `les comptes par défaut suivent la famille et le référentiel`() {
        // SYSCOHADA : marchandises 601 en charge, 701 en produit, 311 en stock.
        assertEquals("601", parCode("MARCH").compteParDefaut(ModuleCode.ACH))
        assertEquals("701", parCode("MARCH").compteParDefaut(ModuleCode.VEN))
        assertEquals("311", parCode("MARCH").compteParDefaut(ModuleCode.STK))
        assertEquals("311", parCode("MARCH").compteParDefaut(ModuleCode.CPT))
        // Un équipement s'immobilise : le compte d'achat devient 241.
        assertEquals("241", parCode("EQUIP").compteParDefaut(ModuleCode.ACH))
    }

    @Test
    fun `le filtrage par module suit le tableau spec 5 2`() {
        // ACH ne retient que les familles achetables.
        assertEquals(
            setOf("MARCH", "MP", "CONSO", "EQUIP"),
            ReglesGroupesArticles.utilisablesPour(ModuleCode.ACH, groupes()).map { it.groupe.code }.toSet(),
        )
        // VEN ne retient que les familles vendables.
        assertEquals(
            setOf("MARCH", "PF", "SE", "SERV"),
            ReglesGroupesArticles.utilisablesPour(ModuleCode.VEN, groupes()).map { it.groupe.code }.toSet(),
        )
        // MAI ne retient que les équipements.
        assertEquals(
            setOf("EQUIP"),
            ReglesGroupesArticles.utilisablesPour(ModuleCode.MAI, groupes()).map { it.groupe.code }.toSet(),
        )
        // PRO retient les produisibles et les familles achetées (matières).
        assertEquals(
            setOf("MARCH", "MP", "PF", "SE", "CONSO", "EQUIP"),
            ReglesGroupesArticles.utilisablesPour(ModuleCode.PRO, groupes()).map { it.groupe.code }.toSet(),
        )
        // SER ne retient que les prestations.
        assertEquals(
            setOf("SERV"),
            ReglesGroupesArticles.utilisablesPour(ModuleCode.SER, groupes()).map { it.groupe.code }.toSet(),
        )
    }

    @Test
    fun `les groupes inactifs disparaissent des référentiels`() {
        val groupes = groupes().map {
            it.copy(groupe = it.groupe.copy(actif = it.groupe.code != "MARCH"))
        }
        assertTrue(ReglesGroupesArticles.utilisablesPour(ModuleCode.ACH, groupes)
            .none { it.groupe.code == "MARCH" })
    }

    // --- Pont article → groupe : le groupe prime quand le lien existe. ---

    @Test
    fun `un article rattaché à un groupe hérite des règles du groupe`() {
        val groupes = groupes()
        val equip = parCode("EQUIP")
        // Même type « acheté-revendu », le rattachement au groupe Équipements
        // impose la règle du groupe : non vendable, maintenable.
        val article = article(groupeId = equip.groupe.id)
        assertTrue(ReglesGroupesArticles.estMaintenable(article, groupes))
        assertFalse(ReglesGroupesArticles.estVendable(article, groupes))
        assertEquals("241", ReglesGroupesArticles.compteParDefaut(article, ModuleCode.ACH, groupes))
    }

    @Test
    fun `sans groupe l article retombe sur son type historique`() {
        val vendeur = article(type = ProductType.ACHATE_REVENDU)
        assertTrue(ReglesGroupesArticles.estVendable(vendeur, gruposVides()))
        assertTrue(ReglesGroupesArticles.estAchetable(vendeur, gruposVides()))
        assertFalse(ReglesGroupesArticles.estMaintenable(vendeur, gruposVides()))

        val matiere = article(type = ProductType.MATIERE_PREMIERE, groupeId = null)
        assertFalse(ReglesGroupesArticles.estVendable(matiere, gruposVides()))

        val fabrique = article(type = ProductType.FABRIQUE)
        assertTrue(ReglesGroupesArticles.estProduisible(fabrique, gruposVides()))
    }

    @Test
    fun `un groupe inconnu de la liste est ignoré silencieusement`() {
        val article = article(groupeId = 999_999L)
        assertNull(ReglesGroupesArticles.pourArticle(article, gruposVides()))
    }

    @Test
    fun `le catalogue vente n expose que les actifs vendables`() {
        val marchGroupe = parCode("MARCH")
        val produits = listOf(
            article(groupeId = marchGroupe.groupe.id),
            article(groupeId = marchGroupe.groupe.id, actif = false),
            article(type = ProductType.MATIERE_PREMIERE),
        )
        assertEquals(1, ReglesGroupesArticles.vendables(produits, groupes()).size)
    }

    @Test
    fun `le catalogue maintenance ne retient que les équipements`() {
        val equipGroupe = parCode("EQUIP")
        val produits = listOf(
            article(groupeId = equipGroupe.groupe.id),
            article(type = ProductType.ACHATE_REVENDU),
        )
        assertEquals(1, ReglesGroupesArticles.maintenables(produits, groupes()).size)
    }
}