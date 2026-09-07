package com.missa.b360

import com.missa.b360.core.data.entity.ClientEntity
import com.missa.b360.core.data.entity.OperationModule
import com.missa.b360.core.data.entity.OperationRecordEntity
import com.missa.b360.core.data.entity.OperationStatus
import com.missa.b360.core.domain.model.CrmRules
import com.missa.b360.core.domain.model.SegmentClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Segmentation commerciale du portefeuille client. */
class CrmRulesTest {

    private val maintenant = 1_000_000_000_000L
    private val jour = 86_400_000L
    private var sequence = 0L

    private fun client(
        nom: String,
        creeIlYaJours: Int = 200,
        actif: Boolean = true,
    ) = ClientEntity(
        id = ++sequence,
        code = "CLI-$sequence",
        nom = nom,
        telephone = "+237600000000",
        createdAt = maintenant - creeIlYaJours * jour,
        active = actif,
    )

    private fun vente(
        client: String,
        montant: Double,
        ilYaJours: Int,
        statut: OperationStatus = OperationStatus.VALIDATED,
        tiersId: Long? = null,
    ) = OperationRecordEntity(
        id = ++sequence,
        module = OperationModule.VENTE.name,
        reference = "VTE-$sequence",
        title = "Vente $sequence",
        counterpart = client,
        tiersId = tiersId,
        amount = montant,
        status = statut.name,
        createdAt = maintenant - ilYaJours * jour,
    )

    @Test
    fun `le rapprochement ignore la casse et les espaces superflus`() {
        assertEquals(
            CrmRules.cleRapprochement("  Boulangerie   DU Coin "),
            CrmRules.cleRapprochement("boulangerie du coin"),
        )
    }

    @Test
    fun `un client qui vient d acheter une fois est nouveau`() {
        val fiches = CrmRules.fiches(
            listOf(client("Alpha")),
            listOf(vente("Alpha", 50_000.0, ilYaJours = 5)),
            maintenant,
        )
        assertEquals(SegmentClient.NOUVEAU, fiches.first().segment)
        assertEquals(1, fiches.first().nombreAchats)
        assertEquals(5, fiches.first().joursDepuisAchat)
    }

    @Test
    fun `trois achats recents font un client fidele`() {
        val fiches = CrmRules.fiches(
            listOf(client("Beta")),
            listOf(
                vente("Beta", 10_000.0, ilYaJours = 5),
                vente("Beta", 20_000.0, ilYaJours = 20),
                vente("Beta", 30_000.0, ilYaJours = 40),
            ),
            maintenant,
        )
        assertEquals(SegmentClient.FIDELE, fiches.first().segment)
        assertEquals(60_000.0, fiches.first().chiffreAffaires, 0.001)
    }

    @Test
    fun `un silence de deux mois fait basculer en relance`() {
        val fiches = CrmRules.fiches(
            listOf(client("Gamma")),
            listOf(vente("Gamma", 10_000.0, ilYaJours = 70)),
            maintenant,
        )
        assertEquals(SegmentClient.A_RELANCER, fiches.first().segment)
    }

    @Test
    fun `un silence de quatre mois fait un client dormant`() {
        val fiches = CrmRules.fiches(
            listOf(client("Delta")),
            listOf(vente("Delta", 10_000.0, ilYaJours = 150)),
            maintenant,
        )
        assertEquals(SegmentClient.DORMANT, fiches.first().segment)
    }

    @Test
    fun `un prospect recent reste prospect, un prospect tiede passe en relance`() {
        val fiches = CrmRules.fiches(
            listOf(client("Frais", creeIlYaJours = 3), client("Tiede", creeIlYaJours = 40)),
            emptyList(),
            maintenant,
        )
        val parNom = fiches.associateBy { it.client.nom }
        assertEquals(SegmentClient.PROSPECT, parNom.getValue("Frais").segment)
        assertEquals(SegmentClient.A_RELANCER, parNom.getValue("Tiede").segment)
    }

    @Test
    fun `un compte desactive sort du cycle commercial`() {
        val fiches = CrmRules.fiches(
            listOf(client("Ferme", actif = false)),
            listOf(vente("Ferme", 10_000.0, ilYaJours = 2)),
            maintenant,
        )
        assertEquals(SegmentClient.INACTIF, fiches.first().segment)
    }

    @Test
    fun `un devis non valide ne compte pas comme un achat`() {
        val fiches = CrmRules.fiches(
            listOf(client("Epsilon")),
            listOf(vente("Epsilon", 900_000.0, ilYaJours = 2, statut = OperationStatus.DRAFT)),
            maintenant,
        )
        assertEquals(0, fiches.first().nombreAchats)
        assertEquals(0.0, fiches.first().chiffreAffaires, 0.001)
    }

    @Test
    fun `les fiches urgentes remontent en tete de liste`() {
        val fiches = CrmRules.fiches(
            listOf(client("Fidele"), client("Perdu")),
            listOf(
                vente("Fidele", 10_000.0, ilYaJours = 2),
                vente("Fidele", 10_000.0, ilYaJours = 8),
                vente("Fidele", 10_000.0, ilYaJours = 15),
                vente("Perdu", 5_000.0, ilYaJours = 90),
            ),
            maintenant,
        )
        assertEquals(SegmentClient.A_RELANCER, fiches.first().segment)
        assertEquals("Perdu", fiches.first().client.nom)
    }

    @Test
    fun `le top classe par chiffre d affaires decroissant`() {
        val fiches = CrmRules.fiches(
            listOf(client("Petit"), client("Gros"), client("Moyen")),
            listOf(
                vente("Petit", 1_000.0, ilYaJours = 5),
                vente("Gros", 900_000.0, ilYaJours = 5),
                vente("Moyen", 50_000.0, ilYaJours = 5),
            ),
            maintenant,
        )
        val top = CrmRules.top(fiches, combien = 2)
        assertEquals(listOf("Gros", "Moyen"), top.map { it.client.nom })
    }

    @Test
    fun `le taux de conversion ignore les comptes desactives`() {
        val fiches = CrmRules.fiches(
            listOf(client("Acheteur"), client("Jamais"), client("Ferme", actif = false)),
            listOf(vente("Acheteur", 10_000.0, ilYaJours = 5)),
            maintenant,
        )
        assertEquals(50.0, CrmRules.tauxConversion(fiches), 0.01)
    }

    @Test
    fun `le panier moyen rapporte le chiffre au nombre d achats`() {
        val fiches = CrmRules.fiches(
            listOf(client("Zeta")),
            listOf(
                vente("Zeta", 30_000.0, ilYaJours = 3),
                vente("Zeta", 10_000.0, ilYaJours = 9),
            ),
            maintenant,
        )
        assertEquals(20_000.0, CrmRules.panierMoyen(fiches), 0.001)
    }

    @Test
    fun `un portefeuille vide ne divise jamais par zero`() {
        assertEquals(0.0, CrmRules.tauxConversion(emptyList()), 0.001)
        assertEquals(0.0, CrmRules.panierMoyen(emptyList()), 0.001)
        assertEquals(0.0, CrmRules.chiffreAffairesTotal(emptyList()), 0.001)
    }

    @Test
    fun `les compteurs couvrent chaque fiche une seule fois`() {
        val fiches = CrmRules.fiches(
            listOf(client("A"), client("B"), client("C", actif = false)),
            listOf(vente("A", 10_000.0, ilYaJours = 3)),
            maintenant,
        )
        assertEquals(fiches.size, CrmRules.compteurs(fiches).sumOf { it.nombre })
    }

    @Test
    fun `une vente identifiee suit le client meme apres un changement de nom`() {
        val client = client("Nouvelle raison sociale")
        val fiches = CrmRules.fiches(
            listOf(client),
            listOf(vente("Ancienne raison sociale", 75_000.0, ilYaJours = 3, tiersId = client.id)),
            maintenant,
        )
        assertEquals(1, fiches.first().nombreAchats)
        assertEquals(75_000.0, fiches.first().chiffreAffaires, 0.001)
    }

    @Test
    fun `les ventes identifiees et les anciennes ventes par nom se cumulent`() {
        val client = client("Alpha")
        val fiches = CrmRules.fiches(
            listOf(client),
            listOf(
                vente("Alpha", 10_000.0, ilYaJours = 10),
                vente("Alpha", 20_000.0, ilYaJours = 5, tiersId = client.id),
            ),
            maintenant,
        )
        assertEquals(2, fiches.first().nombreAchats)
        assertEquals(30_000.0, fiches.first().chiffreAffaires, 0.001)
    }

    @Test
    fun `une vente identifiee n est jamais comptee deux fois`() {
        val client = client("Beta")
        val fiches = CrmRules.fiches(
            listOf(client),
            listOf(vente("Beta", 40_000.0, ilYaJours = 4, tiersId = client.id)),
            maintenant,
        )
        assertEquals(1, fiches.first().nombreAchats)
        assertEquals(40_000.0, fiches.first().chiffreAffaires, 0.001)
    }

    @Test
    fun `une vente rattachee a un autre client n entre pas dans la fiche`() {
        val alpha = client("Alpha")
        val beta = client("Beta")
        val fiches = CrmRules.fiches(
            listOf(alpha, beta),
            listOf(vente("Alpha", 10_000.0, ilYaJours = 3, tiersId = beta.id)),
            maintenant,
        ).associateBy { it.client.nom }
        assertEquals(0, fiches.getValue("Alpha").nombreAchats)
        assertEquals(1, fiches.getValue("Beta").nombreAchats)
    }

    @Test
    fun `chaque segment porte un libelle traduit`() {
        SegmentClient.entries.forEach { assertTrue(it.libelleRes != 0) }
    }
}
