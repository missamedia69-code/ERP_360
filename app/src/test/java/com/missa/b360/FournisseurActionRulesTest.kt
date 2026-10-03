package com.missa.b360

import com.missa.b360.core.data.entity.FournisseurBalanceEntity
import com.missa.b360.core.data.entity.FournisseurEntity
import com.missa.b360.core.data.entity.FournisseurStatus
import com.missa.b360.core.domain.model.FournisseurActionRules
import com.missa.b360.core.domain.model.FournisseurFactureOuverte
import com.missa.b360.core.domain.model.FournisseurLigne
import com.missa.b360.core.domain.model.FournisseurPortefeuilleRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FournisseurActionRulesTest {
    private val jour = 86_400_000L
    private val now = 1_000L * jour

    private fun f(id: Long, nom: String, statut: FournisseurStatus = FournisseurStatus.ACTIF, paiementBloque: Boolean = false) =
        FournisseurEntity(
            id = id, code = "FRN-$id", nom = nom, telephone = "650000000",
            statut = statut, paiementBloque = paiementBloque, createdAt = 0,
        )

    private fun facture(id: Long, fournisseur: Long, echeance: Long, reste: Double) = FournisseurFactureOuverte(
        recordId = id, reference = "FA-$id", fournisseurId = fournisseur,
        issuedAt = 0, dueAt = echeance, total = reste, outstanding = reste,
    )

    private val lignes: List<FournisseurLigne> = FournisseurPortefeuilleRules.lignes(
        fournisseurs = listOf(
            f(1, "Atelier Nord"),
            f(2, "Bois du Sud", paiementBloque = true),
            f(3, "Comptoir Est", statut = FournisseurStatus.BLOQUE),
            f(4, "Depot Ouest", statut = FournisseurStatus.ARCHIVE),
        ),
        avecContactPrincipal = setOf(1L, 2L, 3L, 4L),
        comptes = emptyList(),
        documents = emptyList(),
        balances = listOf(FournisseurBalanceEntity(fournisseurId = 3, dette = 900.0)),
        scores = emptyList(),
        now = now,
    )

    private val factures = listOf(
        facture(10, 1, now - 2 * jour, 100.0),
        facture(11, 2, now - 10 * jour, 300.0),
        facture(12, 1, now, 40.0),
        facture(13, 2, now + 7 * jour, 60.0),
        facture(14, 1, now + 7 * jour + 1, 500.0),
        facture(15, 3, now + 3 * jour, 25.0),
    )

    private val tableau = FournisseurActionRules.construire(lignes, factures, now)

    @Test fun `les factures en retard sont triees par retard decroissant`() {
        assertEquals(listOf(11L, 10L), tableau.facturesEnRetard.map { it.facture.recordId })
        assertEquals(listOf(10, 2), tableau.facturesEnRetard.map { it.joursRetard })
        assertEquals("Bois du Sud", tableau.facturesEnRetard.first().fournisseurNom)
    }

    @Test fun `le jour d echeance n est pas en retard et l horizon de sept jours est inclus`() {
        assertEquals(listOf(12L, 15L, 13L), tableau.facturesBientot.map { it.facture.recordId })
        assertFalse(tableau.facturesBientot.any { it.facture.recordId == 14L })
        assertTrue(tableau.facturesEnRetard.none { it.facture.recordId == 12L })
    }

    @Test fun `les indicateurs additionnent les restes a payer`() {
        assertEquals(1025.0, tableau.detteTotale, 0.001)
        assertEquals(400.0, tableau.enRetard, 0.001)
        assertEquals(125.0, tableau.aPayerSousSeptJours, 0.001)
    }

    @Test fun `les dossiers a regulariser et les bloques sont separes`() {
        assertEquals(listOf("Bois du Sud"), tableau.aRegulariser.map { it.fournisseur.nom })
        assertEquals(listOf("Comptoir Est"), tableau.bloques.map { it.fournisseur.nom })
    }

    @Test fun `un fournisseur archive n apparait ni a regulariser ni bloque`() {
        assertTrue((tableau.aRegulariser + tableau.bloques).none { it.fournisseur.id == 4L })
    }

    @Test fun `un tableau sans rien a faire est vide`() {
        val vide = FournisseurActionRules.construire(emptyList(), emptyList(), now)
        assertTrue(vide.vide)
        assertFalse(tableau.vide)
        assertEquals(0.0, vide.detteTotale, 0.0)
    }

    @Test fun `une facture d un fournisseur inconnu garde un nom de repli`() {
        val t = FournisseurActionRules.construire(emptyList(), listOf(facture(1, 99, now - jour, 10.0)), now)
        assertEquals("#99", t.facturesEnRetard.single().fournisseurNom)
    }
}
