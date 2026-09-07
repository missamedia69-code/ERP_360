package com.missa.b360

import com.missa.b360.core.data.entity.CategorieTresorerie
import com.missa.b360.core.data.entity.MouvementTresorerieEntity
import com.missa.b360.core.data.entity.OperationDirection
import com.missa.b360.core.data.entity.OperationModule
import com.missa.b360.core.data.entity.OperationRecordEntity
import com.missa.b360.core.data.entity.OperationStatus
import com.missa.b360.core.data.entity.SensMouvement
import com.missa.b360.core.domain.model.ComptabiliteRules
import com.missa.b360.core.domain.model.RubriqueComptable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Consolidation comptable : journal, résultat et TVA. */
class ComptabiliteRulesTest {

    private var sequence = 0L

    private fun piece(
        module: OperationModule,
        montant: Double?,
        statut: OperationStatus = OperationStatus.VALIDATED,
        direction: OperationDirection = OperationDirection.NONE,
        date: Long = 1_000L,
    ) = OperationRecordEntity(
        id = ++sequence,
        module = module.name,
        reference = "REF-$sequence",
        title = "Pièce $sequence",
        amount = montant,
        direction = direction.name,
        status = statut.name,
        createdAt = date,
    )

    private fun mouvement(
        sens: SensMouvement,
        montant: Double,
        categorie: CategorieTresorerie,
        date: Long = 1_000L,
        transfertId: String? = null,
    ) = MouvementTresorerieEntity(
        id = ++sequence,
        compteId = 1L,
        date = date,
        sens = sens.name,
        montant = montant,
        categorie = categorie.name,
        libelle = "Mouvement $sequence",
        transfertId = transfertId,
        createdAt = date,
    )

    @Test
    fun `un brouillon n entre pas au resultat`() {
        val pieces = listOf(
            piece(OperationModule.VENTE, 100_000.0),
            piece(OperationModule.VENTE, 500_000.0, statut = OperationStatus.DRAFT),
        )
        val journal = ComptabiliteRules.journal(pieces, emptyList(), 0L, 10_000L)
        assertEquals(1, journal.size)
        assertEquals(100_000.0, journal.first().montant, 0.001)
    }

    @Test
    fun `une piece annulee est ignoree`() {
        val pieces = listOf(
            piece(OperationModule.VENTE, 80_000.0, statut = OperationStatus.CANCELLED),
        )
        assertTrue(ComptabiliteRules.journal(pieces, emptyList(), 0L, 10_000L).isEmpty())
    }

    @Test
    fun `devis commandes et livraisons ne creent ni produit ni charge`() {
        val pieces = listOf(
            piece(OperationModule.DEVIS, 300_000.0),
            piece(OperationModule.COMMANDE, 300_000.0),
            piece(OperationModule.LIVRAISON, 300_000.0),
            piece(OperationModule.STOCK, 300_000.0),
        )
        assertTrue(ComptabiliteRules.journal(pieces, emptyList(), 0L, 10_000L).isEmpty())
    }

    @Test
    fun `ventes et achats alimentent le compte de resultat`() {
        val pieces = listOf(
            piece(OperationModule.VENTE, 500_000.0),
            piece(OperationModule.ACHATS, 200_000.0),
            piece(OperationModule.RH, 150_000.0),
        )
        val resultat = ComptabiliteRules.resultat(
            ComptabiliteRules.journal(pieces, emptyList(), 0L, 10_000L),
        )
        assertEquals(500_000.0, resultat.totalProduits, 0.001)
        assertEquals(350_000.0, resultat.totalCharges, 0.001)
        assertEquals(150_000.0, resultat.resultat, 0.001)
        assertEquals(30.0, resultat.marge, 0.01)
    }

    @Test
    fun `un reglement encaisse ne double pas une vente deja facturee`() {
        val pieces = listOf(piece(OperationModule.VENTE, 500_000.0))
        val mouvements = listOf(
            mouvement(SensMouvement.IN, 500_000.0, CategorieTresorerie.VENTE),
        )
        val journal = ComptabiliteRules.journal(pieces, mouvements, 0L, 10_000L)
        assertEquals(1, journal.size)
        assertEquals(500_000.0, ComptabiliteRules.resultat(journal).totalProduits, 0.001)
    }

    @Test
    fun `sans piece de vente l encaissement devient le produit`() {
        val mouvements = listOf(
            mouvement(SensMouvement.IN, 90_000.0, CategorieTresorerie.VENTE),
        )
        val journal = ComptabiliteRules.journal(emptyList(), mouvements, 0L, 10_000L)
        assertEquals(1, journal.size)
        assertEquals(RubriqueComptable.VENTES, journal.first().rubrique)
        assertTrue(journal.first().tresorerie)
    }

    @Test
    fun `un virement interne n apparait jamais au journal`() {
        val mouvements = listOf(
            mouvement(SensMouvement.OUT, 100_000.0, CategorieTresorerie.TRANSFERT, transfertId = "TRF-1"),
            mouvement(SensMouvement.IN, 100_000.0, CategorieTresorerie.TRANSFERT, transfertId = "TRF-1"),
        )
        assertTrue(ComptabiliteRules.journal(emptyList(), mouvements, 0L, 10_000L).isEmpty())
    }

    @Test
    fun `un financement reste hors compte de resultat`() {
        val mouvements = listOf(
            mouvement(SensMouvement.IN, 2_000_000.0, CategorieTresorerie.FINANCEMENT),
        )
        assertTrue(ComptabiliteRules.journal(emptyList(), mouvements, 0L, 10_000L).isEmpty())
    }

    @Test
    fun `les charges de tresorerie se rangent dans les bonnes rubriques`() {
        val mouvements = listOf(
            mouvement(SensMouvement.OUT, 100_000.0, CategorieTresorerie.SALAIRE),
            mouvement(SensMouvement.OUT, 50_000.0, CategorieTresorerie.LOYER),
            mouvement(SensMouvement.OUT, 20_000.0, CategorieTresorerie.ENERGIE),
            mouvement(SensMouvement.OUT, 30_000.0, CategorieTresorerie.TAXE),
        )
        val journal = ComptabiliteRules.journal(emptyList(), mouvements, 0L, 10_000L)
        val charges = ComptabiliteRules.totaux(journal, produits = false)
            .associate { it.rubrique to it.montant }
        assertEquals(100_000.0, charges.getValue(RubriqueComptable.SALAIRES), 0.001)
        assertEquals(70_000.0, charges.getValue(RubriqueComptable.CHARGES_EXTERNES), 0.001)
        assertEquals(30_000.0, charges.getValue(RubriqueComptable.IMPOTS_TAXES), 0.001)
    }

    @Test
    fun `la periode filtre les ecritures par leurs bornes incluses`() {
        val pieces = listOf(
            piece(OperationModule.VENTE, 10_000.0, date = 99L),
            piece(OperationModule.VENTE, 20_000.0, date = 100L),
            piece(OperationModule.VENTE, 40_000.0, date = 200L),
            piece(OperationModule.VENTE, 80_000.0, date = 201L),
        )
        val journal = ComptabiliteRules.journal(pieces, emptyList(), 100L, 200L)
        assertEquals(60_000.0, journal.sumOf { it.montant }, 0.001)
    }

    @Test
    fun `le journal est trie du plus recent au plus ancien`() {
        val pieces = listOf(
            piece(OperationModule.VENTE, 1_000.0, date = 100L),
            piece(OperationModule.VENTE, 2_000.0, date = 300L),
            piece(OperationModule.VENTE, 3_000.0, date = 200L),
        )
        val dates = ComptabiliteRules.journal(pieces, emptyList(), 0L, 10_000L).map { it.date }
        assertEquals(listOf(300L, 200L, 100L), dates)
    }

    @Test
    fun `la tva se deduit des montants toutes taxes comprises`() {
        val pieces = listOf(
            piece(OperationModule.VENTE, 1_192_500.0),
            piece(OperationModule.ACHATS, 596_250.0),
        )
        val journal = ComptabiliteRules.journal(pieces, emptyList(), 0L, 10_000L)
        // 1 192 500 TTC à 19,25 % = 1 000 000 HT, soit 192 500 de taxe.
        assertEquals(192_500.0, ComptabiliteRules.tvaCollectee(journal, 19.25), 0.5)
        assertEquals(96_250.0, ComptabiliteRules.tvaDeductible(journal, 19.25), 0.5)
        assertEquals(96_250.0, ComptabiliteRules.tvaAPayer(journal, 19.25), 0.5)
    }

    @Test
    fun `sans taux de taxe la tva reste nulle`() {
        val journal = ComptabiliteRules.journal(
            listOf(piece(OperationModule.VENTE, 500_000.0)),
            emptyList(),
            0L,
            10_000L,
        )
        assertEquals(0.0, ComptabiliteRules.tvaCollectee(journal, 0.0), 0.001)
        assertEquals(0.0, ComptabiliteRules.tvaAPayer(journal, 0.0), 0.001)
    }

    @Test
    fun `plus d achats que de ventes donne un credit de tva`() {
        val pieces = listOf(
            piece(OperationModule.VENTE, 119_250.0),
            piece(OperationModule.ACHATS, 596_250.0),
        )
        val journal = ComptabiliteRules.journal(pieces, emptyList(), 0L, 10_000L)
        assertTrue(ComptabiliteRules.tvaAPayer(journal, 19.25) < 0)
    }

    @Test
    fun `un montant absent ou nul ne cree pas d ecriture`() {
        val pieces = listOf(
            piece(OperationModule.VENTE, null),
            piece(OperationModule.VENTE, 0.0),
            piece(OperationModule.VENTE, -5_000.0),
        )
        assertTrue(ComptabiliteRules.journal(pieces, emptyList(), 0L, 10_000L).isEmpty())
    }

    @Test
    fun `chaque rubrique porte un libelle traduit`() {
        RubriqueComptable.entries.forEach { assertTrue(it.libelleRes != 0) }
    }
}
