package com.missa.b360

import com.missa.b360.core.data.entity.CategorieTresorerie
import com.missa.b360.core.data.entity.CompteTresorerieEntity
import com.missa.b360.core.data.entity.MouvementTresorerieEntity
import com.missa.b360.core.data.entity.SensMouvement
import com.missa.b360.core.data.entity.TypeCompteTresorerie
import com.missa.b360.core.domain.model.TresorerieRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Règles du module Trésorerie : soldes, flux, virements et saisie des montants. */
class TresorerieRulesTest {

    private var sequence = 0L

    private fun compte(
        id: Long,
        nom: String = "Compte $id",
        soldeInitial: Double = 0.0,
        actif: Boolean = true,
    ) = CompteTresorerieEntity(
        id = id,
        nom = nom,
        type = TypeCompteTresorerie.CAISSE.name,
        soldeInitial = soldeInitial,
        actif = actif,
        createdAt = 0L,
    )

    private fun mouvement(
        compteId: Long,
        sens: SensMouvement,
        montant: Double,
        date: Long = 1_000L,
        categorie: CategorieTresorerie = CategorieTresorerie.AUTRE,
        transfertId: String? = null,
        rapproche: Boolean = false,
    ) = MouvementTresorerieEntity(
        id = ++sequence,
        compteId = compteId,
        date = date,
        sens = sens.name,
        montant = montant,
        categorie = categorie.name,
        libelle = "Écriture $sequence",
        transfertId = transfertId,
        rapproche = rapproche,
        createdAt = date,
    )

    @Test
    fun `le solde part du solde d ouverture et suit les deux sens`() {
        val mouvements = listOf(
            mouvement(1, SensMouvement.IN, 150_000.0),
            mouvement(1, SensMouvement.OUT, 40_000.0),
            mouvement(1, SensMouvement.OUT, 10_000.0),
        )
        assertEquals(125_000.0, TresorerieRules.solde(25_000.0, mouvements), 0.001)
    }

    @Test
    fun `un compte sans mouvement conserve son solde d ouverture`() {
        assertEquals(75_000.0, TresorerieRules.solde(75_000.0, emptyList()), 0.001)
    }

    @Test
    fun `chaque compte ne recoit que ses propres mouvements`() {
        val comptes = listOf(compte(1, soldeInitial = 10_000.0), compte(2, soldeInitial = 0.0))
        val mouvements = listOf(
            mouvement(1, SensMouvement.IN, 5_000.0),
            mouvement(2, SensMouvement.IN, 80_000.0),
            mouvement(2, SensMouvement.OUT, 30_000.0),
        )
        val soldes = TresorerieRules.soldes(comptes, mouvements)
        assertEquals(15_000.0, soldes.first { it.compte.id == 1L }.solde, 0.001)
        assertEquals(50_000.0, soldes.first { it.compte.id == 2L }.solde, 0.001)
        assertEquals(1, soldes.first { it.compte.id == 1L }.nombreMouvements)
        assertEquals(2, soldes.first { it.compte.id == 2L }.nombreMouvements)
    }

    @Test
    fun `un virement interne ne change pas le solde global`() {
        val comptes = listOf(compte(1, soldeInitial = 100_000.0), compte(2))
        val transfert = "TRF-1"
        val mouvements = listOf(
            mouvement(1, SensMouvement.OUT, 60_000.0, transfertId = transfert),
            mouvement(2, SensMouvement.IN, 60_000.0, transfertId = transfert),
        )
        assertEquals(100_000.0, TresorerieRules.soldeGlobal(comptes, mouvements), 0.001)
        val soldes = TresorerieRules.soldes(comptes, mouvements)
        assertEquals(40_000.0, soldes.first { it.compte.id == 1L }.solde, 0.001)
        assertEquals(60_000.0, soldes.first { it.compte.id == 2L }.solde, 0.001)
    }

    @Test
    fun `les virements sont exclus des encaissements et decaissements`() {
        val mouvements = listOf(
            mouvement(1, SensMouvement.IN, 200_000.0, date = 500L),
            mouvement(1, SensMouvement.OUT, 50_000.0, date = 600L),
            mouvement(1, SensMouvement.OUT, 90_000.0, date = 700L, transfertId = "TRF-9"),
            mouvement(2, SensMouvement.IN, 90_000.0, date = 700L, transfertId = "TRF-9"),
        )
        val flux = TresorerieRules.flux(mouvements, debut = 0L, fin = 1_000L)
        assertEquals(200_000.0, flux.entrees, 0.001)
        assertEquals(50_000.0, flux.sorties, 0.001)
        assertEquals(150_000.0, flux.net, 0.001)
    }

    @Test
    fun `les bornes de la periode sont incluses et filtrantes`() {
        val mouvements = listOf(
            mouvement(1, SensMouvement.IN, 1_000.0, date = 99L),
            mouvement(1, SensMouvement.IN, 2_000.0, date = 100L),
            mouvement(1, SensMouvement.IN, 4_000.0, date = 200L),
            mouvement(1, SensMouvement.IN, 8_000.0, date = 201L),
        )
        val flux = TresorerieRules.flux(mouvements, debut = 100L, fin = 200L)
        assertEquals(6_000.0, flux.entrees, 0.001)
    }

    @Test
    fun `la repartition classe les postes de depense du plus lourd au plus leger`() {
        val mouvements = listOf(
            mouvement(1, SensMouvement.OUT, 30_000.0, categorie = CategorieTresorerie.LOYER),
            mouvement(1, SensMouvement.OUT, 90_000.0, categorie = CategorieTresorerie.SALAIRE),
            mouvement(1, SensMouvement.OUT, 20_000.0, categorie = CategorieTresorerie.SALAIRE),
            mouvement(1, SensMouvement.IN, 500_000.0, categorie = CategorieTresorerie.VENTE),
        )
        val repartition = TresorerieRules.repartitionSorties(mouvements, 0L, 10_000L)
        assertEquals(2, repartition.size)
        assertEquals(CategorieTresorerie.SALAIRE, repartition[0].first)
        assertEquals(110_000.0, repartition[0].second, 0.001)
        assertEquals(CategorieTresorerie.LOYER, repartition[1].first)
    }

    @Test
    fun `le reste a rapprocher ignore les lignes deja pointees`() {
        val mouvements = listOf(
            mouvement(1, SensMouvement.IN, 10_000.0, rapproche = true),
            mouvement(1, SensMouvement.IN, 7_000.0),
            mouvement(1, SensMouvement.OUT, 2_000.0),
        )
        assertEquals(5_000.0, TresorerieRules.resteARapprocher(mouvements), 0.001)
    }

    @Test
    fun `un montant saisi accepte la virgule et les espaces de milliers`() {
        assertEquals(1_250_000.5, TresorerieRules.montantSaisi("1 250 000,50")!!, 0.001)
        assertEquals(1_250_000.5, TresorerieRules.montantSaisi("1\u00A0250\u00A0000.50")!!, 0.001)
        assertEquals(3_000.0, TresorerieRules.montantSaisi(" 3000 ")!!, 0.001)
    }

    @Test
    fun `un montant nul negatif ou illisible est refuse`() {
        assertNull(TresorerieRules.montantSaisi(""))
        assertNull(TresorerieRules.montantSaisi("0"))
        assertNull(TresorerieRules.montantSaisi("-500"))
        assertNull(TresorerieRules.montantSaisi("abc"))
        assertNull(TresorerieRules.montantSaisi("12,,5"))
        assertNull(TresorerieRules.montantSaisi("999999999999999"))
    }

    @Test
    fun `le montant est arrondi au centime`() {
        assertEquals(10.13, TresorerieRules.montantSaisi("10,126")!!, 0.0001)
    }

    @Test
    fun `un libelle trop court est refuse`() {
        assertTrue(TresorerieRules.libelleValide("Loyer"))
        assertTrue(!TresorerieRules.libelleValide(" "))
        assertTrue(!TresorerieRules.libelleValide("x"))
    }

    @Test
    fun `une valeur inconnue en base retombe sur un defaut sur`() {
        assertEquals(CategorieTresorerie.AUTRE, TresorerieRules.categorie("INEXISTANT"))
        assertEquals(CategorieTresorerie.AUTRE, TresorerieRules.categorie(null))
        assertEquals(SensMouvement.IN, TresorerieRules.sens(null))
        assertEquals(TypeCompteTresorerie.CAISSE, TresorerieRules.typeCompte("???"))
    }

    @Test
    fun `le poste transfert n est jamais propose a la saisie manuelle`() {
        val proposees = TresorerieRules.categoriesPour(SensMouvement.IN) +
            TresorerieRules.categoriesPour(SensMouvement.OUT)
        assertTrue(CategorieTresorerie.TRANSFERT !in proposees)
        assertTrue(CategorieTresorerie.VENTE in TresorerieRules.categoriesPour(SensMouvement.IN))
        assertTrue(CategorieTresorerie.SALAIRE in TresorerieRules.categoriesPour(SensMouvement.OUT))
    }

    @Test
    fun `un compte ferme garde son argent dans le solde global`() {
        val comptes = listOf(compte(1, soldeInitial = 20_000.0, actif = false), compte(2))
        assertEquals(20_000.0, TresorerieRules.soldeGlobal(comptes, emptyList()), 0.001)
    }

    @Test
    fun `une vente payee alimente la tresorerie une seule fois`() {
        val premier = TresorerieRules.encaissementAEnregistrer(
            montantPaye = 150_000.0,
            dejaEnregistre = false,
            compteDisponible = true,
        )
        assertEquals(150_000.0, premier!!, 0.001)
        // Revalidation de la même facture : le mouvement existe déjà.
        assertNull(
            TresorerieRules.encaissementAEnregistrer(
                montantPaye = 150_000.0,
                dejaEnregistre = true,
                compteDisponible = true,
            ),
        )
    }

    @Test
    fun `sans compte ouvert aucun encaissement n est ecrit`() {
        assertNull(
            TresorerieRules.encaissementAEnregistrer(
                montantPaye = 90_000.0,
                dejaEnregistre = false,
                compteDisponible = false,
            ),
        )
    }

    @Test
    fun `une vente non reglee ne credite pas la caisse`() {
        assertNull(
            TresorerieRules.encaissementAEnregistrer(
                montantPaye = 0.0,
                dejaEnregistre = false,
                compteDisponible = true,
            ),
        )
        assertNull(
            TresorerieRules.encaissementAEnregistrer(
                montantPaye = -10.0,
                dejaEnregistre = false,
                compteDisponible = true,
            ),
        )
    }

    @Test
    fun `l encaissement est arrondi au centime`() {
        val montant = TresorerieRules.encaissementAEnregistrer(
            montantPaye = 33_333.339,
            dejaEnregistre = false,
            compteDisponible = true,
        )
        assertEquals(33_333.34, montant!!, 0.0001)
    }

    @Test
    fun `la reference d encaissement reprend le numero de facture`() {
        assertEquals("FAC-2026-0001", TresorerieRules.referenceEncaissement(" FAC-2026-0001 "))
    }

    @Test
    fun `chaque categorie et chaque type possede un libelle traduit`() {
        CategorieTresorerie.entries.forEach {
            assertTrue(TresorerieRules.libelleCategorie(it) != 0)
        }
        TypeCompteTresorerie.entries.forEach {
            assertTrue(TresorerieRules.libelleType(it) != 0)
        }
    }
}
