package com.missa.b360

import com.missa.b360.core.data.entity.FournisseurEntity
import com.missa.b360.core.data.entity.FournisseurItemEntity
import com.missa.b360.core.data.entity.FournisseurScoreEntity
import com.missa.b360.core.data.entity.FournisseurStatus
import com.missa.b360.core.domain.model.FournisseurComparateurRules
import com.missa.b360.core.domain.model.FournisseurLigne
import com.missa.b360.core.domain.model.SourcingExclusion
import com.missa.b360.core.domain.model.SourcingRules
import com.missa.b360.core.domain.model.SupplierReadiness
import com.missa.b360.core.domain.model.SupplierReadinessLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FournisseurComparateurRulesTest {
    private val now = 1_000L * 86_400_000L

    private fun ligne(id: Long, nom: String, statut: FournisseurStatus = FournisseurStatus.ACTIF, note: Int? = null) =
        FournisseurLigne(
            fournisseur = FournisseurEntity(id = id, code = "FRN-$id", nom = nom, telephone = "650000000", statut = statut, createdAt = 0),
            aptitude = SupplierReadiness(SupplierReadinessLevel.PRET, emptyList(), emptyList()),
            balance = null,
            score = FournisseurScoreEntity(fournisseurId = id, score = note),
        )

    private fun liaison(fournisseur: Long, produit: Long, prix: Double, delai: Int = 0, actif: Boolean = true, fin: Long? = null) =
        FournisseurItemEntity(fournisseurId = fournisseur, productId = produit, prixUnitaire = prix, delaiJours = delai, actif = actif, finValidite = fin)

    @Test fun `un candidat par liaison active avec son prix et sa fiabilite`() {
        val c = FournisseurComparateurRules.candidats(
            lignes = listOf(ligne(1, "Atelier", note = 80), ligne(2, "Bois")),
            liaisons = listOf(liaison(1, 7, 10.0, 3), liaison(2, 7, 12.0, actif = false), liaison(9, 7, 5.0)),
            now = now,
        )
        assertEquals(1, c.size)
        assertEquals(10.0, c.single().prix!!, 0.001)
        assertEquals(80, c.single().fiabilite)
        assertEquals(3, c.single().delaiJours)
    }

    @Test fun `un prix hors validite n est pas devine`() {
        val c = FournisseurComparateurRules.candidats(listOf(ligne(1, "Atelier")), listOf(liaison(1, 7, 10.0, fin = now - 1)), now)
        assertNull(c.single().prix)
        val classement = SourcingRules.classer(c, 1.0, now)
        assertEquals(SourcingExclusion.SANS_PRIX, classement.single().exclusion)
    }

    @Test fun `un fournisseur bloque est ecarte et le moins cher passe en premier`() {
        val c = FournisseurComparateurRules.candidats(
            listOf(ligne(1, "Atelier"), ligne(2, "Bois"), ligne(3, "Zinc", statut = FournisseurStatus.BLOQUE)),
            listOf(liaison(1, 7, 10.0), liaison(2, 7, 9.0), liaison(3, 7, 1.0)),
            now,
        )
        val classement = SourcingRules.classer(c, 1.0, now)
        assertEquals(listOf("Bois", "Atelier", "Zinc"), classement.map { it.candidat.nom })
        assertEquals(SourcingExclusion.NON_COMMANDABLE, classement.last().exclusion)
    }

    @Test fun `le nombre de fournisseurs par article ignore les liaisons inactives`() {
        val m = FournisseurComparateurRules.fournisseursParArticle(
            listOf(liaison(1, 7, 1.0), liaison(2, 7, 1.0), liaison(2, 7, 2.0), liaison(3, 7, 1.0, actif = false), liaison(1, 8, 1.0)),
        )
        assertEquals(mapOf(7L to 2, 8L to 1), m)
    }
}
