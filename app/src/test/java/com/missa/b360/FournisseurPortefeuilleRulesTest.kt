package com.missa.b360

import com.missa.b360.core.data.entity.FournisseurBalanceEntity
import com.missa.b360.core.data.entity.FournisseurEntity
import com.missa.b360.core.data.entity.FournisseurScoreEntity
import com.missa.b360.core.data.entity.FournisseurStatus
import com.missa.b360.core.domain.model.FournisseurFiltre
import com.missa.b360.core.domain.model.FournisseurLigne
import com.missa.b360.core.domain.model.FournisseurPortefeuilleRules
import com.missa.b360.core.domain.model.FournisseurTri
import com.missa.b360.core.domain.model.SupplierReadinessLevel
import com.missa.b360.core.domain.model.SupplierReadinessReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FournisseurPortefeuilleRulesTest {
    private val now = 1_000L * 86_400_000L

    private fun f(
        id: Long,
        nom: String,
        statut: FournisseurStatus = FournisseurStatus.ACTIF,
        paiementBloque: Boolean = false,
        telephone: String = "650000000",
    ) = FournisseurEntity(
        id = id,
        code = "FRN-2026-%04d".format(id),
        nom = nom,
        telephone = telephone,
        statut = statut,
        paiementBloque = paiementBloque,
        createdAt = 0,
    )

    private fun balance(id: Long, dette: Double, retard: Double = 0.0, jours: Int = 0) =
        FournisseurBalanceEntity(fournisseurId = id, dette = dette, enRetard = retard, joursRetardMax = jours)

    private fun score(id: Long, valeur: Int?) = FournisseurScoreEntity(fournisseurId = id, score = valeur)

    private val fournisseurs = listOf(
        f(1, "Atelier Nord"),
        f(2, "bois du Sud", paiementBloque = true),
        f(3, "Comptoir Est", statut = FournisseurStatus.BLOQUE),
        f(4, "Dépôt Ouest", statut = FournisseurStatus.ARCHIVE),
        f(5, "Zinc Express", telephone = "699111222"),
    )

    private val lignes: List<FournisseurLigne> = FournisseurPortefeuilleRules.lignes(
        fournisseurs = fournisseurs,
        avecContactPrincipal = setOf(1L, 2L, 3L, 4L, 5L),
        comptes = emptyList(),
        documents = emptyList(),
        balances = listOf(balance(1, 500.0, 200.0, 12), balance(2, 100.0), balance(3, 900.0, 900.0, 40), balance(4, 50.0)),
        scores = listOf(score(1, 80), score(2, null), score(5, 95)),
        now = now,
    )

    private fun noms(l: List<FournisseurLigne>) = l.map { it.fournisseur.nom }

    @Test fun `chaque fournisseur recoit son aptitude son solde et son score`() {
        val parId = lignes.associateBy { it.fournisseur.id }
        assertEquals(SupplierReadinessLevel.PRET, parId.getValue(1).aptitude.niveau)
        assertEquals(listOf(SupplierReadinessReason.PAIEMENT_BLOQUE), parId.getValue(2).aptitude.motifs)
        assertEquals(SupplierReadinessLevel.A_REGULARISER, parId.getValue(2).aptitude.niveau)
        assertEquals(SupplierReadinessLevel.BLOQUE, parId.getValue(3).aptitude.niveau)
        assertEquals(500.0, parId.getValue(1).dette, 0.0)
        assertEquals(200.0, parId.getValue(1).enRetard, 0.0)
        assertEquals(12, parId.getValue(1).joursRetardMax)
        assertEquals(80, parId.getValue(1).note)
        assertEquals(0.0, parId.getValue(5).dette, 0.0)
        assertEquals(null, parId.getValue(2).note)
    }

    @Test fun `le filtre Tous masque les archives`() {
        assertEquals(
            listOf("Atelier Nord", "bois du Sud", "Comptoir Est", "Zinc Express"),
            noms(FournisseurPortefeuilleRules.filtrer(lignes, FournisseurFiltre.TOUS, "")),
        )
    }

    @Test fun `les filtres de dette de retard de regularisation de blocage et d archive`() {
        assertEquals(
            listOf("Atelier Nord", "bois du Sud", "Comptoir Est"),
            noms(FournisseurPortefeuilleRules.filtrer(lignes, FournisseurFiltre.A_PAYER, "")),
        )
        assertEquals(
            listOf("Atelier Nord", "Comptoir Est"),
            noms(FournisseurPortefeuilleRules.filtrer(lignes, FournisseurFiltre.EN_RETARD, "")),
        )
        assertEquals(
            listOf("bois du Sud"),
            noms(FournisseurPortefeuilleRules.filtrer(lignes, FournisseurFiltre.A_REGULARISER, "")),
        )
        assertEquals(
            listOf("Comptoir Est"),
            noms(FournisseurPortefeuilleRules.filtrer(lignes, FournisseurFiltre.BLOQUES, "")),
        )
        assertEquals(
            listOf("Dépôt Ouest"),
            noms(FournisseurPortefeuilleRules.filtrer(lignes, FournisseurFiltre.ARCHIVES, "")),
        )
    }

    @Test fun `les compteurs suivent les filtres`() {
        assertEquals(4, FournisseurPortefeuilleRules.compter(lignes, FournisseurFiltre.TOUS))
        assertEquals(3, FournisseurPortefeuilleRules.compter(lignes, FournisseurFiltre.A_PAYER))
        assertEquals(1, FournisseurPortefeuilleRules.compter(lignes, FournisseurFiltre.ARCHIVES))
    }

    @Test fun `la recherche ignore la casse et couvre nom code et telephone`() {
        assertEquals(listOf("bois du Sud"), noms(FournisseurPortefeuilleRules.filtrer(lignes, FournisseurFiltre.TOUS, " BOIS ")))
        assertEquals(listOf("Zinc Express"), noms(FournisseurPortefeuilleRules.filtrer(lignes, FournisseurFiltre.TOUS, "699111")))
        assertEquals(listOf("Atelier Nord"), noms(FournisseurPortefeuilleRules.filtrer(lignes, FournisseurFiltre.TOUS, "2026-0001")))
        assertTrue(FournisseurPortefeuilleRules.filtrer(lignes, FournisseurFiltre.TOUS, "introuvable").isEmpty())
        assertTrue(FournisseurPortefeuilleRules.correspond(fournisseurs[0], "   "))
        assertFalse(FournisseurPortefeuilleRules.correspond(fournisseurs[0], "zzz"))
    }

    @Test fun `le tri par nom ignore la casse`() {
        assertEquals(
            listOf("Atelier Nord", "bois du Sud", "Comptoir Est", "Dépôt Ouest", "Zinc Express"),
            noms(FournisseurPortefeuilleRules.trier(lignes.reversed(), FournisseurTri.NOM)),
        )
    }

    @Test fun `le tri par dette et par retard est decroissant`() {
        assertEquals(
            listOf("Comptoir Est", "Atelier Nord", "bois du Sud", "Dépôt Ouest", "Zinc Express"),
            noms(FournisseurPortefeuilleRules.trier(lignes, FournisseurTri.DETTE)),
        )
        assertEquals(
            listOf("Comptoir Est", "Atelier Nord"),
            noms(FournisseurPortefeuilleRules.trier(lignes, FournisseurTri.RETARD)).take(2),
        )
    }

    @Test fun `le tri par fiabilite place les scores absents en dernier`() {
        assertEquals(
            listOf("Zinc Express", "Atelier Nord", "bois du Sud", "Comptoir Est", "Dépôt Ouest"),
            noms(FournisseurPortefeuilleRules.trier(lignes, FournisseurTri.SCORE)),
        )
    }

    @Test fun `sans contact principal un fournisseur actif reste pret`() {
        val sans = FournisseurPortefeuilleRules.lignes(
            listOf(f(1, "Atelier Nord")), emptySet(), emptyList(), emptyList(), emptyList(), emptyList(), now,
        )
        assertEquals(SupplierReadinessLevel.PRET, sans.single().aptitude.niveau)
    }
}
