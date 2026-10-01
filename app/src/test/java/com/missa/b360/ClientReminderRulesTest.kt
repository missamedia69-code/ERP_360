package com.missa.b360

import com.missa.b360.core.data.entity.ClientBalanceEntity
import com.missa.b360.core.data.entity.ClientEntity
import com.missa.b360.core.data.entity.ClientFollowupEntity
import com.missa.b360.core.data.entity.ClientPaymentEntity
import com.missa.b360.core.data.entity.FollowupStatus
import com.missa.b360.core.data.entity.FollowupType
import com.missa.b360.core.domain.model.ActivityFilter
import com.missa.b360.core.domain.model.ActivityKind
import com.missa.b360.core.domain.model.ActivitySale
import com.missa.b360.core.domain.model.AgingBucket
import com.missa.b360.core.domain.model.ClientActivityRules
import com.missa.b360.core.domain.model.ClientListItem
import com.missa.b360.core.domain.model.ClientListRules
import com.missa.b360.core.domain.model.ClientReminderRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClientReminderRulesTest {
    private val jour = 86_400_000L
    private val now = 1_800_000_000_000L

    private fun client(id: Long) = ClientEntity(id = id, code = "C$id", nom = "Client $id", telephone = "+23769900000$id", createdAt = 1L)

    private fun compte(id: Long, enRetard: Double, jours: Int) =
        ClientBalanceEntity(clientId = id, encours = enRetard + 10.0, enRetard = enRetard, joursRetardMax = jours)

    private fun ligne(id: Long, enRetard: Double, jours: Int) = ClientListItem(client(id), compte(id, enRetard, jours))

    @Test fun `les tranches suivent 30 60 et 90 jours`() {
        assertEquals(AgingBucket.NON_ECHU, ClientReminderRules.tranche(0))
        assertEquals(AgingBucket.JOURS_1_30, ClientReminderRules.tranche(30))
        assertEquals(AgingBucket.JOURS_31_60, ClientReminderRules.tranche(31))
        assertEquals(AgingBucket.JOURS_61_90, ClientReminderRules.tranche(90))
        assertEquals(AgingBucket.PLUS_90, ClientReminderRules.tranche(91))
    }

    @Test fun `les retards sont groupes du plus ancien au plus recent puis par montant`() {
        val groupes = ClientReminderRules.groupes(
            listOf(ligne(1, 100.0, 10), ligne(2, 500.0, 95), ligne(3, 900.0, 10), ligne(4, 0.0, 0)),
        )
        assertEquals(listOf(AgingBucket.PLUS_90, AgingBucket.JOURS_1_30), groupes.map { it.first })
        assertEquals(listOf(3L, 1L), groupes[1].second.map { it.client.id })
        assertEquals(1500.0, ClientReminderRules.totalEnRetard(groupes.flatMap { it.second }), 1e-9)
    }

    @Test fun `seules les promesses non tenues sont retenues`() {
        fun promesse(id: Long, statut: FollowupStatus, date: Long) = ClientFollowupEntity(
            id = id, clientId = 1, type = FollowupType.PROMESSE, promesseDate = date, promesseMontant = 10.0, statut = statut, createdAt = 1L,
        )
        val liste = ClientReminderRules.promessesNonTenues(
            listOf(promesse(1, FollowupStatus.NON_TENU, 100), promesse(2, FollowupStatus.TENU, 300), promesse(3, FollowupStatus.NON_TENU, 200)),
        )
        assertEquals(listOf(3L, 1L), liste.map { it.id })
    }

    @Test fun `un client en retard sans relance recente est a relancer`() {
        val items = ClientListRules.construireItems(
            clients = listOf(client(1), client(2), client(3)),
            comptes = mapOf(1L to compte(1, 50.0, 40), 2L to compte(2, 50.0, 40), 3L to compte(3, 0.0, 0)),
            suivis = listOf(
                ClientFollowupEntity(clientId = 2, type = FollowupType.RELANCE, createdAt = now - 2 * jour),
            ),
            now = now,
        )
        assertTrue(items.first { it.client.id == 1L }.aRelancer)
        assertFalse(items.first { it.client.id == 2L }.aRelancer)
        assertFalse(items.first { it.client.id == 3L }.aRelancer)
    }

    @Test fun `la chronologie melange les sources et filtre`() {
        val paiement = ClientPaymentEntity(
            clientId = 1, montant = 40.0, modePaiement = "ESPECES", reference = "ENC-1", paiementAt = now - jour, createdAt = now - jour,
        )
        val relance = ClientFollowupEntity(clientId = 1, type = FollowupType.RELANCE, message = "Bonjour", createdAt = now)
        val entrees = ClientActivityRules.construire(
            ventes = listOf(ActivitySale("V-1", now - 3 * jour, 100.0, avoir = false), ActivitySale("A-1", now - 2 * jour, 20.0, avoir = true)),
            paiements = listOf(paiement),
            suivis = listOf(relance),
        )
        assertEquals(listOf(ActivityKind.RELANCE, ActivityKind.ENCAISSEMENT, ActivityKind.AVOIR, ActivityKind.VENTE), entrees.map { it.kind })
        assertEquals(2, ClientActivityRules.filtrer(entrees, ActivityFilter.VENTES).size)
        assertEquals(1, ClientActivityRules.filtrer(entrees, ActivityFilter.ENCAISSEMENTS).size)
        assertEquals(1, ClientActivityRules.filtrer(entrees, ActivityFilter.SUIVI).size)
        assertEquals(4, ClientActivityRules.filtrer(entrees, ActivityFilter.TOUT).size)
    }
}
