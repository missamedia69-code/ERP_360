package com.missa.b360

import com.missa.b360.core.data.entity.AbsenceEntity
import com.missa.b360.core.domain.model.PaieRules
import com.missa.b360.core.domain.model.ProductionComponent
import com.missa.b360.core.domain.model.ProductionRecordPayload
import com.missa.b360.core.domain.model.ProductionRules
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

/** Règles paie (§Paie) — prorata journalier des absences et net. */
class PaieRulesTest {

    private fun date(annee: Int, mois: Int, jour: Int): Long =
        Calendar.getInstance().apply {
            clear()
            set(annee, mois - 1, jour, 12, 0, 0)
        }.timeInMillis

    private fun absence(dateDebut: Long, duree: Double): AbsenceEntity =
        AbsenceEntity(
            employeeId = 1L,
            type = "MALADIE",
            dateDebut = dateDebut,
            dureeJours = duree,
            createdAt = dateDebut,
        )

    @Test
    fun salaireJour_base_sur_jours_mensuels() {
        assertEquals(100.0, PaieRules.salaireJour(2600.0, 26.0), 0.0001)
    }

    @Test
    fun salaireJour_jours_invalides_donne_zero() {
        assertEquals(0.0, PaieRules.salaireJour(2600.0, 0.0), 0.0001)
    }

    @Test
    fun absence_entierement_dans_le_mois() {
        val a = absence(date(2026, 6, 10), 3.0)
        assertEquals(3.0, PaieRules.absencesDuMois(listOf(a), mois = 6, annee = 2026), 0.0001)
        assertEquals(0.0, PaieRules.absencesDuMois(listOf(a), mois = 7, annee = 2026), 0.0001)
    }

    @Test
    fun absence_traversant_un_mois_est_proratisée() {
        // 5 jours à partir du 30/05 au midi : 2 jours en mai, 3 jours en juin.
        val a = absence(date(2026, 5, 30), 5.0)
        assertEquals(2.0, PaieRules.absencesDuMois(listOf(a), mois = 5, annee = 2026), 0.0001)
        assertEquals(3.0, PaieRules.absencesDuMois(listOf(a), mois = 6, annee = 2026), 0.0001)
    }

    @Test
    fun duree_partielle_est_arrondie_au_moins_un_jour() {
        val a = absence(date(2026, 6, 10), 0.4)
        assertEquals(1.0, PaieRules.absencesDuMois(listOf(a), mois = 6, annee = 2026), 0.0001)
    }

    @Test
    fun net_base_primes_retenues_absences_avancement() {
        assertEquals(
            2300.0,
            PaieRules.net(base = 2600.0, primes = 100.0, retenues = 50.0, absencesMontant = 200.0, avancement = 150.0),
            0.0001,
        )
    }

    @Test
    fun net_peut_rester_negatif() {
        assertEquals(
            -100.0,
            PaieRules.net(base = 0.0, primes = 0.0, retenues = 0.0, absencesMontant = 50.0, avancement = 50.0),
            0.0001,
        )
    }
}

/** Règles production (§Production) — agrégation des besoins par composant. */
class ProductionRulesTest {

    private fun payload(vararg composants: ProductionComponent): ProductionRecordPayload =
        ProductionRecordPayload(
            produitId = 10L,
            produitNom = "Produit fini",
            quantite = 2.0,
            composants = composants.toList(),
        )

    @Test
    fun besoins_somme_par_composant() {
        val besoins = ProductionRules.besoinsParComposant(
            payload(
                ProductionComponent(1L, "Farine", 2.0),
                ProductionComponent(2L, "Eau", 1.5),
                ProductionComponent(1L, "Farine", 1.0),
            ),
        )
        assertEquals(3.0, besoin(besoins, 1L), 0.0001)
        assertEquals(1.5, besoin(besoins, 2L), 0.0001)
    }

    @Test
    fun besoins_exclut_quantites_non_positives() {
        val besoins = ProductionRules.besoinsParComposant(
            payload(
                ProductionComponent(1L, "Farine", 0.0),
                ProductionComponent(2L, "Eau", -1.0),
                ProductionComponent(3L, "Sel", 0.5),
            ),
        )
        assertEquals(1, besoins.size)
        assertEquals(0.5, besoin(besoins, 3L), 0.0001)
    }

    @Test
    fun besoins_liste_vide() {
        assertEquals(emptyMap<Long, Double>(), ProductionRules.besoinsParComposant(payload()))
    }

    private fun besoin(map: Map<Long, Double>, id: Long): Double =
        map[id] ?: throw AssertionError("composant $id absent des besoins")
}
