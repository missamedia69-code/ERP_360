package com.missa.b360

import com.missa.b360.core.data.entity.OperationModule
import com.missa.b360.core.data.entity.OperationRecordEntity
import com.missa.b360.core.data.entity.OperationStatus
import com.missa.b360.core.domain.model.EtapePrestation
import com.missa.b360.core.domain.model.ModeFacturation
import com.missa.b360.core.domain.model.Prestation
import com.missa.b360.core.domain.model.PrestationCodec
import com.missa.b360.core.domain.model.PrestationPayload
import com.missa.b360.core.domain.model.PrestationRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Règles des prestations de services. */
class PrestationRulesTest {

    private val maintenant = 1_000_000_000_000L
    private var sequence = 0L

    private fun prestation(
        etape: EtapePrestation = EtapePrestation.PLANIFIEE,
        mode: ModeFacturation = ModeFacturation.FORFAIT,
        tarif: Double = 100_000.0,
        heures: Double = 0.0,
        annulee: Boolean = false,
    ): Prestation {
        val payload = PrestationPayload(
            clientName = "Client ${++sequence}",
            intitule = "Prestation $sequence",
            mode = mode.name,
            tarif = tarif,
            heures = heures,
            etape = etape.name,
        )
        return Prestation(
            record = OperationRecordEntity(
                id = sequence,
                module = OperationModule.SERVICES.name,
                reference = "S-$sequence",
                title = payload.intitule,
                status = if (annulee) OperationStatus.CANCELLED.name else OperationStatus.VALIDATED.name,
                notes = PrestationCodec.encode(payload),
                createdAt = maintenant,
            ),
            payload = payload,
        )
    }

    @Test
    fun `le payload survit a un aller-retour d encodage`() {
        val payload = PrestationPayload(
            clientId = 3,
            clientName = "Cabinet Sarl",
            intitule = "Audit annuel",
            mode = ModeFacturation.HORAIRE.name,
            tarif = 15_000.0,
            heures = 7.5,
        )
        assertEquals(payload, PrestationCodec.decode(PrestationCodec.encode(payload)))
    }

    @Test
    fun `un forfait ne varie pas avec les heures passees`() {
        val payload = PrestationPayload(
            clientName = "A",
            intitule = "Installation",
            mode = ModeFacturation.FORFAIT.name,
            tarif = 250_000.0,
            heures = 40.0,
        )
        assertEquals(250_000.0, PrestationRules.montant(payload), 0.001)
    }

    @Test
    fun `une prestation horaire suit le temps reellement passe`() {
        val payload = PrestationPayload(
            clientName = "B",
            intitule = "Dépannage",
            mode = ModeFacturation.HORAIRE.name,
            tarif = 12_500.0,
            heures = 3.5,
        )
        assertEquals(43_750.0, PrestationRules.montant(payload), 0.001)
    }

    @Test
    fun `une prestation horaire sans heures ne facture rien`() {
        val payload = PrestationPayload(
            clientName = "C",
            intitule = "Visite",
            mode = ModeFacturation.HORAIRE.name,
            tarif = 20_000.0,
            heures = 0.0,
        )
        assertEquals(0.0, PrestationRules.montant(payload), 0.001)
    }

    @Test
    fun `le montant est arrondi au centime`() {
        val payload = PrestationPayload(
            clientName = "D",
            intitule = "Conseil",
            mode = ModeFacturation.HORAIRE.name,
            tarif = 3_333.333,
            heures = 3.0,
        )
        assertEquals(9_999.999.let { Math.round(it * 100.0) / 100.0 }, PrestationRules.montant(payload), 0.001)
    }

    @Test
    fun `le cycle avance dans un seul sens`() {
        assertEquals(EtapePrestation.EN_COURS, PrestationRules.etapeSuivante(EtapePrestation.PLANIFIEE))
        assertEquals(EtapePrestation.TERMINEE, PrestationRules.etapeSuivante(EtapePrestation.EN_COURS))
        assertNull(PrestationRules.etapeSuivante(EtapePrestation.TERMINEE))
    }

    @Test
    fun `la date de realisation n est posee qu a la cloture`() {
        val depart = PrestationPayload(clientName = "E", intitule = "Pose")
        val enCours = PrestationRules.avancer(depart, maintenant)!!
        assertNull(enCours.dateRealisation)
        val terminee = PrestationRules.avancer(enCours, maintenant)!!
        assertEquals(maintenant, terminee.dateRealisation)
    }

    @Test
    fun `seules les prestations terminees comptent comme realisees`() {
        val liste = listOf(
            prestation(EtapePrestation.TERMINEE, tarif = 100_000.0),
            prestation(EtapePrestation.EN_COURS, tarif = 50_000.0),
            prestation(EtapePrestation.PLANIFIEE, tarif = 30_000.0),
        )
        assertEquals(100_000.0, PrestationRules.chiffreRealise(liste), 0.001)
        assertEquals(80_000.0, PrestationRules.carnet(liste), 0.001)
    }

    @Test
    fun `une prestation annulee ne compte ni au realise ni au carnet`() {
        val liste = listOf(
            prestation(EtapePrestation.TERMINEE, tarif = 100_000.0, annulee = true),
            prestation(EtapePrestation.PLANIFIEE, tarif = 40_000.0, annulee = true),
        )
        assertEquals(0.0, PrestationRules.chiffreRealise(liste), 0.001)
        assertEquals(0.0, PrestationRules.carnet(liste), 0.001)
    }

    @Test
    fun `les heures realisees ne comptent que les prestations terminees`() {
        val liste = listOf(
            prestation(EtapePrestation.TERMINEE, ModeFacturation.HORAIRE, 10_000.0, heures = 6.0),
            prestation(EtapePrestation.EN_COURS, ModeFacturation.HORAIRE, 10_000.0, heures = 20.0),
        )
        assertEquals(6.0, PrestationRules.heuresRealisees(liste), 0.001)
    }

    @Test
    fun `le tri place le travail a faire avant le travail fait`() {
        val liste = listOf(
            prestation(EtapePrestation.TERMINEE),
            prestation(EtapePrestation.PLANIFIEE),
            prestation(annulee = true),
        )
        val tries = PrestationRules.trier(liste)
        assertEquals(EtapePrestation.PLANIFIEE, tries.first().etape)
        assertTrue(tries.last().annulee)
    }

    @Test
    fun `les compteurs ignorent les prestations annulees`() {
        val liste = listOf(
            prestation(EtapePrestation.PLANIFIEE),
            prestation(EtapePrestation.PLANIFIEE, annulee = true),
        )
        assertEquals(1, PrestationRules.compteurs(liste).sumOf { it.nombre })
    }

    @Test
    fun `un intitule trop court est refuse`() {
        assertTrue(PrestationRules.intituleValide("Audit"))
        assertTrue(!PrestationRules.intituleValide(" "))
        assertTrue(!PrestationRules.intituleValide("x"))
    }

    @Test
    fun `une valeur inconnue en base retombe sur un defaut sur`() {
        assertEquals(EtapePrestation.PLANIFIEE, PrestationRules.etape("???"))
        assertEquals(ModeFacturation.FORFAIT, PrestationRules.mode(null))
    }

    @Test
    fun `chaque etape et chaque mode porte un libelle traduit`() {
        EtapePrestation.entries.forEach { assertTrue(it.libelleRes != 0) }
        ModeFacturation.entries.forEach { assertTrue(it.libelleRes != 0) }
    }
}
