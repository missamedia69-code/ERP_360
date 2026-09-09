package com.missa.b360

import com.missa.b360.core.data.entity.OperationDirection
import com.missa.b360.core.data.entity.OperationModule
import com.missa.b360.core.data.entity.OperationRecordEntity
import com.missa.b360.core.data.entity.OperationStatus
import com.missa.b360.core.domain.model.CockpitRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/** Règles du cockpit compact de l'accueil — logique pure, sans base. */
class CockpitRulesTest {

    private val maintenant: Long = Calendar.getInstance().timeInMillis

    private fun piece(
        module: OperationModule,
        montant: Double,
        createdAt: Long,
        status: OperationStatus = OperationStatus.VALIDATED,
        direction: OperationDirection = OperationDirection.IN,
    ) = OperationRecordEntity(
        module = module.name,
        reference = "REF",
        title = "Pièce",
        amount = montant,
        quantity = 0.0,
        direction = direction.name,
        status = status.name,
        createdAt = createdAt,
    )

    @Test
    fun `variationPct renvoie null quand la valeur de reference est nulle`() {
        assertNull(CockpitRules.variationPct(10.0, 0.0))
        assertEquals(50.0, CockpitRules.variationPct(150.0, 100.0)!!, 0.0001)
        assertEquals(-50.0, CockpitRules.variationPct(50.0, 100.0)!!, 0.0001)
    }

    @Test
    fun `encaissements ne compte que les ventes validees du jour`() {
        val jour = CockpitRules.debutJour(maintenant)
        val avant = piece(OperationModule.VENTE, 1000.0, jour - 1000)
        val duJour = piece(OperationModule.VENTE, 2500.0, jour + 60000)
        val autreModule = piece(OperationModule.ACHATS, 500.0, jour + 60000)
        val brouillon = piece(
            OperationModule.VENTE, 900.0, jour + 60000,
            status = OperationStatus.DRAFT,
        )
        assertEquals(
            2500.0,
            CockpitRules.encaissements(listOf(avant, duJour, autreModule, brouillon), jour),
            0.0001,
        )
    }

    @Test
    fun `margeJour est la difference ventes achats`() {
        assertEquals(50.0, CockpitRules.margeJour(150.0, 100.0), 0.0001)
    }

    @Test
    fun `performanceMensuelle couvre six mois et garde le mois courant`() {
        // Une vente au mois précédent.
        val cal = Calendar.getInstance().apply { timeInMillis = maintenant }
        cal.add(Calendar.MONTH, -1)
        cal.set(Calendar.DAY_OF_MONTH, 15)
        val ventesPrecedentes = piece(OperationModule.VENTE, 4200.0, cal.timeInMillis)
        val serie = CockpitRules.performanceMensuelle(listOf(ventesPrecedentes), maintenant)
        assertEquals(6, serie.size)
        // Mois le plus récent = mois courant, toujours présent (1..12).
        assertTrue(serie.last().moisIndex in 1..12)
        // Le mois précédent porte bien le montant saisi.
        assertTrue(serie.any { it.montant == 4200.0 })
    }

    @Test
    fun `fluxJour distingue entrees et sorties`() {
        val jour = CockpitRules.debutJour(maintenant)
        val entree = piece(OperationModule.FINANCES, 800.0, jour + 5000, direction = OperationDirection.IN)
        val sortie = piece(OperationModule.FINANCES, 300.0, jour + 9000, direction = OperationDirection.OUT)
        val sansDirection = piece(OperationModule.STOCK, 100.0, jour + 9000, direction = OperationDirection.NONE)
        assertEquals(500.0, CockpitRules.fluxJour(listOf(entree, sortie, sansDirection), jour), 0.0001)
    }
}
