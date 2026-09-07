package com.missa.b360

import com.missa.b360.core.data.entity.OperationModule
import com.missa.b360.core.data.entity.OperationRecordEntity
import com.missa.b360.core.data.entity.OperationStatus
import com.missa.b360.core.domain.model.EtatProjet
import com.missa.b360.core.domain.model.Projet
import com.missa.b360.core.domain.model.ProjetCodec
import com.missa.b360.core.domain.model.ProjetPayload
import com.missa.b360.core.domain.model.ProjetRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Règles des projets : budget, avancement, dérive. */
class ProjetRulesTest {

    private val maintenant = 1_000_000_000_000L
    private val jour = 86_400_000L
    private var sequence = 0L

    private fun projet(
        etat: EtatProjet = EtatProjet.EN_COURS,
        budget: Double = 1_000_000.0,
        consomme: Double = 0.0,
        avancement: Int = 0,
        echeance: Long? = null,
        annule: Boolean = false,
    ): Projet {
        val payload = ProjetPayload(
            nom = "Projet ${++sequence}",
            budget = budget,
            consomme = consomme,
            avancement = avancement,
            echeance = echeance,
            etat = etat.name,
        )
        return Projet(
            record = OperationRecordEntity(
                id = sequence,
                module = OperationModule.PROJETS.name,
                reference = "P-$sequence",
                title = payload.nom,
                status = if (annule) OperationStatus.CANCELLED.name else OperationStatus.VALIDATED.name,
                notes = ProjetCodec.encode(payload),
                createdAt = maintenant,
            ),
            payload = payload,
        )
    }

    @Test
    fun `le payload survit a un aller-retour d encodage`() {
        val payload = ProjetPayload(
            nom = "Refonte boutique",
            clientId = 4,
            budget = 5_000_000.0,
            consomme = 1_250_000.0,
            avancement = 30,
        )
        assertEquals(payload, ProjetCodec.decode(ProjetCodec.encode(payload)))
    }

    @Test
    fun `la consommation du budget se calcule en pourcentage`() {
        val payload = ProjetPayload(nom = "A", budget = 200_000.0, consomme = 50_000.0)
        assertEquals(25.0, ProjetRules.consommationBudget(payload), 0.01)
    }

    @Test
    fun `sans budget defini la consommation ne divise pas par zero`() {
        val payload = ProjetPayload(nom = "B", budget = 0.0, consomme = 90_000.0)
        assertEquals(0.0, ProjetRules.consommationBudget(payload), 0.001)
    }

    @Test
    fun `la derive oppose le budget brule a l avancement declare`() {
        // 80 % du budget consommé pour 30 % d'avancement : 50 points de dérive.
        val payload = ProjetPayload(
            nom = "C",
            budget = 1_000_000.0,
            consomme = 800_000.0,
            avancement = 30,
        )
        assertEquals(50.0, ProjetRules.derive(payload), 0.01)
    }

    @Test
    fun `un projet en avance affiche une derive negative`() {
        val payload = ProjetPayload(
            nom = "D",
            budget = 1_000_000.0,
            consomme = 200_000.0,
            avancement = 60,
        )
        assertTrue(ProjetRules.derive(payload) < 0)
    }

    @Test
    fun `seuls les projets vivants et budgetes remontent en derive`() {
        val liste = listOf(
            projet(budget = 100_000.0, consomme = 90_000.0, avancement = 10),
            projet(EtatProjet.LIVRE, budget = 100_000.0, consomme = 100_000.0, avancement = 20),
            projet(budget = 0.0, consomme = 500_000.0, avancement = 0),
            projet(budget = 100_000.0, consomme = 90_000.0, avancement = 10, annule = true),
        )
        assertEquals(1, ProjetRules.enDerive(liste).size)
    }

    @Test
    fun `un projet livre n est jamais compte en retard`() {
        val liste = listOf(
            projet(echeance = maintenant - jour),
            projet(EtatProjet.LIVRE, echeance = maintenant - 10 * jour),
        )
        assertEquals(1, ProjetRules.enRetard(liste, maintenant).size)
    }

    @Test
    fun `une echeance future ne declenche aucun retard`() {
        val liste = listOf(projet(echeance = maintenant + 5 * jour))
        assertTrue(ProjetRules.enRetard(liste, maintenant).isEmpty())
    }

    @Test
    fun `l avancement est borne entre zero et cent`() {
        assertEquals(0, ProjetRules.avancementValide(-20))
        assertEquals(100, ProjetRules.avancementValide(150))
        assertEquals(45, ProjetRules.avancementValide(45))
    }

    @Test
    fun `un depassement de budget est detecte`() {
        val depasse = projet(budget = 100_000.0, consomme = 120_000.0)
        assertTrue(depasse.depasse)
        assertEquals(-20_000.0, depasse.resteBudget, 0.001)
    }

    @Test
    fun `les totaux ignorent les projets annules`() {
        val liste = listOf(
            projet(budget = 500_000.0, consomme = 100_000.0),
            projet(budget = 300_000.0, consomme = 50_000.0, annule = true),
        )
        assertEquals(500_000.0, ProjetRules.budgetTotal(liste), 0.001)
        assertEquals(100_000.0, ProjetRules.consommeTotal(liste), 0.001)
    }

    @Test
    fun `l avancement moyen ne retient que les projets vivants`() {
        val liste = listOf(
            projet(avancement = 20),
            projet(avancement = 40),
            projet(EtatProjet.LIVRE, avancement = 100),
            projet(avancement = 90, annule = true),
        )
        assertEquals(30.0, ProjetRules.avancementMoyen(liste), 0.01)
    }

    @Test
    fun `sans projet vivant la moyenne reste a zero`() {
        assertEquals(0.0, ProjetRules.avancementMoyen(emptyList()), 0.001)
        assertEquals(0.0, ProjetRules.avancementMoyen(listOf(projet(EtatProjet.LIVRE))), 0.001)
    }

    @Test
    fun `le tri met les projets en difficulte en tete`() {
        val sain = projet(budget = 100_000.0, consomme = 10_000.0, avancement = 50)
        val derive = projet(budget = 100_000.0, consomme = 95_000.0, avancement = 10)
        val tries = ProjetRules.trier(listOf(sain, derive))
        assertEquals(derive.record.id, tries.first().record.id)
    }

    @Test
    fun `un nom trop court est refuse`() {
        assertTrue(ProjetRules.nomValide("Site web"))
        assertTrue(!ProjetRules.nomValide(" "))
        assertTrue(!ProjetRules.nomValide("x"))
    }

    @Test
    fun `un etat inconnu retombe sur en preparation`() {
        assertEquals(EtatProjet.EN_PREPARATION, ProjetRules.etat("???"))
        assertEquals(EtatProjet.EN_PREPARATION, ProjetRules.etat(null))
    }

    @Test
    fun `un contenu illisible ne fait pas planter la lecture`() {
        assertNull(ProjetCodec.decode("pas du json"))
    }

    @Test
    fun `chaque etat porte un libelle traduit`() {
        EtatProjet.entries.forEach { assertTrue(it.libelleRes != 0) }
    }
}
