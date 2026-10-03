package com.missa.b360

import com.missa.b360.core.domain.model.SourcingCandidate
import com.missa.b360.core.domain.model.SourcingExclusion
import com.missa.b360.core.domain.model.SourcingReason
import com.missa.b360.core.domain.model.SourcingRules
import com.missa.b360.core.domain.model.SupplierItemPrice
import com.missa.b360.core.domain.model.SupplierReadinessLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SourcingRulesTest {
    private val now = 1_000_000L

    private fun candidat(
        id: Long, nom: String = "F$id", prix: Double? = 100.0, delai: Int = 5, qteMin: Double = 0.0,
        fiabilite: Int? = 80, prefere: Boolean = false, peut: Boolean = true,
        aptitude: SupplierReadinessLevel = SupplierReadinessLevel.PRET,
    ) = SourcingCandidate(id, nom, peut, aptitude, prix, delai, qteMin, fiabilite, prefere)

    @Test
    fun `un seul candidat est recommande avec ses raisons`() {
        val r = SourcingRules.classer(listOf(candidat(1, prefere = true)), 10.0, now)
        assertEquals(1, r.size)
        assertEquals(1, r[0].rang)
        assertTrue(r[0].recommande)
        assertEquals(
            listOf(SourcingReason.MOINS_CHER, SourcingReason.PLUS_RAPIDE, SourcingReason.PLUS_FIABLE, SourcingReason.PREFERE),
            r[0].raisons,
        )
    }

    @Test
    fun `tous exclus donne une liste sans recommandation avec motif`() {
        val r = SourcingRules.classer(
            listOf(
                candidat(1, peut = false),
                candidat(2, aptitude = SupplierReadinessLevel.BLOQUE),
                candidat(3, qteMin = 50.0),
                candidat(4, prix = null),
            ),
            10.0, now,
        )
        assertTrue(r.none { it.recommande })
        assertEquals(
            mapOf(
                1L to SourcingExclusion.NON_COMMANDABLE,
                2L to SourcingExclusion.BLOQUE,
                3L to SourcingExclusion.QTE_MIN_NON_ATTEINTE,
                4L to SourcingExclusion.SANS_PRIX,
            ),
            r.associate { it.candidat.fournisseurId to it.exclusion },
        )
    }

    @Test
    fun `quantite minimale egale a la quantite voulue reste eligible`() {
        val r = SourcingRules.classer(listOf(candidat(1, qteMin = 10.0)), 10.0, now)
        assertTrue(r[0].recommande)
    }

    @Test
    fun `le moins cher peut perdre face a un fournisseur plus rapide et plus fiable`() {
        val bonMarche = candidat(1, prix = 90.0, delai = 20, fiabilite = 30)
        val sur = candidat(2, prix = 100.0, delai = 5, fiabilite = 95)
        val r = SourcingRules.classer(listOf(bonMarche, sur), 10.0, now)
        assertEquals(2L, r[0].candidat.fournisseurId)
        assertEquals(1, r[0].rang)
        assertEquals(2, r[1].rang)
        assertTrue(SourcingReason.PLUS_RAPIDE in r[0].raisons)
        assertTrue(SourcingReason.MOINS_CHER in r[1].raisons)
    }

    @Test
    fun `egalite parfaite departagee par prefere puis prix puis nom`() {
        val a = candidat(1, nom = "Zeta")
        val b = candidat(2, nom = "Alpha")
        assertEquals(listOf(2L, 1L), SourcingRules.classer(listOf(a, b), 1.0, now).map { it.candidat.fournisseurId })
        val prefere = a.copy(prefere = true)
        assertEquals(listOf(1L, 2L), SourcingRules.classer(listOf(prefere, b), 1.0, now).map { it.candidat.fournisseurId })
    }

    @Test
    fun `delai zero est inconnu et vaut cinquante`() {
        val inconnu = candidat(1, delai = 0, fiabilite = 50)
        val r = SourcingRules.classer(listOf(inconnu), 1.0, now)
        // 0,45 × 100 + 0,25 × 50 + 0,30 × 50
        assertEquals(72.5, r[0].score ?: -1.0, 0.001)
        assertFalse(SourcingReason.PLUS_RAPIDE in r[0].raisons)
    }

    @Test
    fun `fiabilite absente vaut cinquante`() {
        val r = SourcingRules.classer(listOf(candidat(1, fiabilite = null)), 1.0, now)
        assertEquals(0.45 * 100 + 0.25 * 100 + 0.30 * 50, r[0].score ?: -1.0, 0.001)
    }

    @Test
    fun `prix retenu liaison valide sinon dernier prix paye`() {
        val valide = SupplierItemPrice(80.0, debutValidite = now - 10, finValidite = now + 10)
        assertEquals(80.0, SourcingRules.prixRetenu(valide, 95.0, now) ?: -1.0, 0.0)
        val expire = valide.copy(finValidite = now - 1)
        assertEquals(95.0, SourcingRules.prixRetenu(expire, 95.0, now) ?: -1.0, 0.0)
        val futur = valide.copy(debutValidite = now + 1)
        assertEquals(95.0, SourcingRules.prixRetenu(futur, 95.0, now) ?: -1.0, 0.0)
        val inactif = valide.copy(actif = false)
        assertEquals(95.0, SourcingRules.prixRetenu(inactif, 95.0, now) ?: -1.0, 0.0)
        assertNull(SourcingRules.prixRetenu(expire, null, now))
        assertNull(SourcingRules.prixRetenu(null, 0.0, now))
        assertNull(SourcingRules.prixRetenu(valide.copy(prixUnitaire = Double.NaN), Double.NaN, now))
    }

    @Test
    fun `bornes de validite incluses`() {
        val p = SupplierItemPrice(80.0, debutValidite = now, finValidite = now)
        assertEquals(80.0, SourcingRules.prixRetenu(p, null, now) ?: -1.0, 0.0)
    }

    @Test
    fun `les non recommandables sont listes apres les classes`() {
        val r = SourcingRules.classer(listOf(candidat(1, peut = false, nom = "A"), candidat(2, nom = "B")), 1.0, now)
        assertEquals(listOf(2L, 1L), r.map { it.candidat.fournisseurId })
    }
}
