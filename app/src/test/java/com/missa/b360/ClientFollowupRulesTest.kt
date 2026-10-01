package com.missa.b360

import com.missa.b360.core.domain.model.ClientFollowupRules
import com.missa.b360.core.data.entity.FollowupStatus
import com.missa.b360.core.domain.model.PromiseCheck
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClientFollowupRulesTest {
    private val day = 86_400_000L
    private val today = 2_000_000_000_000L - 2_000_000_000_000L % day // début de journée
    private val now = today + 10 * 3_600_000L

    @Test fun `promesse valide`() {
        assertEquals(PromiseCheck.VALIDE, ClientFollowupRules.verifierPromesse(100.0, today, 100.0, today))
        assertEquals(PromiseCheck.VALIDE, ClientFollowupRules.verifierPromesse(40.0, today + 3 * day, 100.0, today))
    }

    @Test fun `promesse invalide pour montant date ou depassement de lencours`() {
        listOf(0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY).forEach {
            assertEquals(PromiseCheck.MONTANT_INVALIDE, ClientFollowupRules.verifierPromesse(it, today, 100.0, today))
        }
        assertEquals(PromiseCheck.MONTANT_SUPERIEUR_ENCOURS, ClientFollowupRules.verifierPromesse(100.01, today, 100.0, today))
        assertEquals(PromiseCheck.MONTANT_SUPERIEUR_ENCOURS, ClientFollowupRules.verifierPromesse(10.0, today, 0.0, today))
        assertEquals(PromiseCheck.DATE_INVALIDE, ClientFollowupRules.verifierPromesse(50.0, today - 1, 100.0, today))
    }

    @Test fun `promesse ouverte tant que la journee promise nest pas ecoulee`() {
        val statut = ClientFollowupRules.statutPromesse(FollowupStatus.OUVERT, today, 100.0, 0.0, now)
        assertEquals(FollowupStatus.OUVERT, statut)
        assertEquals(
            FollowupStatus.OUVERT,
            ClientFollowupRules.statutPromesse(FollowupStatus.OUVERT, today, 100.0, 0.0, today + day - 1),
        )
    }

    @Test fun `promesse non tenue des que la journee promise est ecoulee`() {
        assertEquals(
            FollowupStatus.NON_TENU,
            ClientFollowupRules.statutPromesse(FollowupStatus.OUVERT, today, 100.0, 0.0, today + day),
        )
        // un encaissement partiel ne suffit pas
        assertEquals(
            FollowupStatus.NON_TENU,
            ClientFollowupRules.statutPromesse(FollowupStatus.OUVERT, today, 100.0, 99.0, today + 2 * day),
        )
    }

    @Test fun `promesse tenue des que les encaissements couvrent le montant meme avant la date`() {
        assertEquals(
            FollowupStatus.TENU,
            ClientFollowupRules.statutPromesse(FollowupStatus.OUVERT, today + 5 * day, 100.0, 100.0, now),
        )
        assertEquals(
            FollowupStatus.TENU,
            ClientFollowupRules.statutPromesse(FollowupStatus.OUVERT, today, 100.0, 150.0, today + 9 * day),
        )
    }

    @Test fun `un statut definitif nevolue plus`() {
        listOf(FollowupStatus.TENU, FollowupStatus.NON_TENU, FollowupStatus.CLOS).forEach { statut ->
            assertEquals(statut, ClientFollowupRules.statutPromesse(statut, today, 100.0, 0.0, today + 30 * day))
            assertEquals(statut, ClientFollowupRules.statutPromesse(statut, today, 100.0, 500.0, now))
        }
    }

    @Test fun `encaissement non fini est ignore`() {
        assertEquals(
            FollowupStatus.OUVERT,
            ClientFollowupRules.statutPromesse(FollowupStatus.OUVERT, today + day, 100.0, Double.NaN, now),
        )
    }

    @Test fun `client sans montant echu nest jamais a relancer`() {
        assertFalse(ClientFollowupRules.aRelancer(0.0, null, null, now))
        assertFalse(ClientFollowupRules.aRelancer(Double.NaN, null, null, now))
    }

    @Test fun `client en retard jamais relance est a relancer`() {
        assertTrue(ClientFollowupRules.aRelancer(50.0, null, null, now))
    }

    @Test fun `promesse ouverte dans les delais suspend la relance`() {
        assertFalse(ClientFollowupRules.aRelancer(50.0, today + 2 * day, null, now))
        // la journée promise écoulée, la relance redevient possible
        assertTrue(ClientFollowupRules.aRelancer(50.0, today - day, null, today + day))
    }

    @Test fun `delai minimal entre deux relances`() {
        assertFalse(ClientFollowupRules.aRelancer(50.0, null, now - 6 * day, now))
        assertTrue(ClientFollowupRules.aRelancer(50.0, null, now - 7 * day, now))
        assertTrue(ClientFollowupRules.aRelancer(50.0, null, now - 2 * day, now, delaiJours = 1))
    }
}
