package com.missa.b360

import com.missa.b360.core.data.entity.OperationModule
import com.missa.b360.core.data.entity.OperationRecordEntity
import com.missa.b360.core.data.entity.OperationStatus
import com.missa.b360.core.domain.model.BonLivraison
import com.missa.b360.core.domain.model.EtapeLivraison
import com.missa.b360.core.domain.model.LivraisonCodec
import com.missa.b360.core.domain.model.LivraisonPayload
import com.missa.b360.core.domain.model.LivraisonRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Cycle de vie des bons de livraison. */
class LivraisonRulesTest {

    private val maintenant = 1_000_000_000_000L
    private val jour = 86_400_000L
    private var sequence = 0L

    private fun bon(
        etape: EtapeLivraison = EtapeLivraison.A_PREPARER,
        annule: Boolean = false,
        creeIlYaJours: Int = 2,
        dateLivraison: Long? = null,
    ): BonLivraison {
        val cree = maintenant - creeIlYaJours * jour
        val payload = LivraisonPayload(
            clientName = "Client ${++sequence}",
            etape = etape.name,
            dateLivraison = dateLivraison,
        )
        return BonLivraison(
            record = OperationRecordEntity(
                id = sequence,
                module = OperationModule.LIVRAISON.name,
                reference = "L-$sequence",
                title = payload.clientName,
                status = if (annule) OperationStatus.CANCELLED.name else OperationStatus.VALIDATED.name,
                notes = LivraisonCodec.encode(payload),
                createdAt = cree,
            ),
            payload = payload,
        )
    }

    @Test
    fun `le payload survit a un aller-retour d encodage`() {
        val payload = LivraisonPayload(
            clientId = 7,
            clientName = "Boulangerie du Coin",
            adresseLivraison = "Rue 12, Douala",
            transporteur = "Express CM",
            nombreColis = 3,
            poidsKg = 12.5,
        )
        val relu = LivraisonCodec.decode(LivraisonCodec.encode(payload))
        assertEquals(payload, relu)
    }

    @Test
    fun `un contenu illisible ne fait pas planter la lecture`() {
        assertNull(LivraisonCodec.decode("ceci n'est pas du json"))
        assertNull(LivraisonCodec.decode(null))
    }

    @Test
    fun `le cycle avance dans un seul sens`() {
        assertEquals(EtapeLivraison.EXPEDIEE, LivraisonRules.etapeSuivante(EtapeLivraison.A_PREPARER))
        assertEquals(EtapeLivraison.LIVREE, LivraisonRules.etapeSuivante(EtapeLivraison.EXPEDIEE))
        assertNull(LivraisonRules.etapeSuivante(EtapeLivraison.LIVREE))
    }

    @Test
    fun `la date de remise n est posee qu a la livraison`() {
        val depart = LivraisonPayload(clientName = "Alpha")
        val expediee = LivraisonRules.avancer(depart, maintenant)!!
        assertEquals(EtapeLivraison.EXPEDIEE.name, expediee.etape)
        assertNull(expediee.dateLivraison)

        val livree = LivraisonRules.avancer(expediee, maintenant)!!
        assertEquals(EtapeLivraison.LIVREE.name, livree.etape)
        assertEquals(maintenant, livree.dateLivraison)
    }

    @Test
    fun `un bon livre ne peut plus avancer`() {
        val livre = LivraisonPayload(clientName = "Beta", etape = EtapeLivraison.LIVREE.name)
        assertNull(LivraisonRules.avancer(livre, maintenant))
    }

    @Test
    fun `les bons a traiter passent devant les bons termines`() {
        val bons = listOf(
            bon(EtapeLivraison.LIVREE, dateLivraison = maintenant),
            bon(EtapeLivraison.A_PREPARER),
            bon(EtapeLivraison.EXPEDIEE),
        )
        val tries = LivraisonRules.trier(bons)
        assertEquals(EtapeLivraison.A_PREPARER, tries[0].etape)
        assertEquals(EtapeLivraison.EXPEDIEE, tries[1].etape)
        assertEquals(EtapeLivraison.LIVREE, tries[2].etape)
    }

    @Test
    fun `un bon annule tombe en fin de liste`() {
        val tries = LivraisonRules.trier(
            listOf(bon(annule = true), bon(EtapeLivraison.LIVREE, dateLivraison = maintenant)),
        )
        assertTrue(tries.last().annule)
    }

    @Test
    fun `les compteurs ignorent les bons annules`() {
        val bons = listOf(
            bon(EtapeLivraison.A_PREPARER),
            bon(EtapeLivraison.A_PREPARER),
            bon(EtapeLivraison.A_PREPARER, annule = true),
        )
        val compteurs = LivraisonRules.compteurs(bons)
        assertEquals(1, compteurs.size)
        assertEquals(2, compteurs.first().nombre)
    }

    @Test
    fun `la file en cours exclut les bons livres et annules`() {
        val bons = listOf(
            bon(EtapeLivraison.A_PREPARER),
            bon(EtapeLivraison.EXPEDIEE),
            bon(EtapeLivraison.LIVREE, dateLivraison = maintenant),
            bon(annule = true),
        )
        assertEquals(2, LivraisonRules.enCours(bons).size)
    }

    @Test
    fun `le delai moyen se calcule sur les seules livraisons arrivees`() {
        val bons = listOf(
            bon(EtapeLivraison.LIVREE, creeIlYaJours = 4, dateLivraison = maintenant - 2 * jour),
            bon(EtapeLivraison.LIVREE, creeIlYaJours = 6, dateLivraison = maintenant - 2 * jour),
            bon(EtapeLivraison.A_PREPARER, creeIlYaJours = 30),
        )
        assertEquals(3.0, LivraisonRules.delaiMoyenJours(bons)!!, 0.01)
    }

    @Test
    fun `sans livraison arrivee aucun delai n est invente`() {
        assertNull(LivraisonRules.delaiMoyenJours(listOf(bon(EtapeLivraison.A_PREPARER))))
        assertNull(LivraisonRules.delaiMoyenJours(emptyList()))
    }

    @Test
    fun `le taux de livraison ecarte les annulations du denominateur`() {
        val bons = listOf(
            bon(EtapeLivraison.LIVREE, dateLivraison = maintenant),
            bon(EtapeLivraison.A_PREPARER),
            bon(annule = true),
        )
        assertEquals(50.0, LivraisonRules.tauxLivraison(bons), 0.01)
    }

    @Test
    fun `un portefeuille vide ne divise pas par zero`() {
        assertEquals(0.0, LivraisonRules.tauxLivraison(emptyList()), 0.001)
    }

    @Test
    fun `un destinataire trop court est refuse`() {
        assertTrue(LivraisonRules.destinataireValide("SARL Alpha"))
        assertTrue(!LivraisonRules.destinataireValide(" "))
        assertTrue(!LivraisonRules.destinataireValide("A"))
    }

    @Test
    fun `une etape inconnue en base retombe sur a preparer`() {
        assertEquals(EtapeLivraison.A_PREPARER, LivraisonRules.etape("INEXISTANT"))
        assertEquals(EtapeLivraison.A_PREPARER, LivraisonRules.etape(null))
    }

    @Test
    fun `chaque etape porte un libelle traduit`() {
        EtapeLivraison.entries.forEach { assertTrue(it.libelleRes != 0) }
        assertNotNull(EtapeLivraison.LIVREE.ordre)
    }
}
