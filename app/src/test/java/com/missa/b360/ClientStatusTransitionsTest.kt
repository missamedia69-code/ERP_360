package com.missa.b360

import com.missa.b360.core.data.entity.ClientEntity
import com.missa.b360.core.data.entity.ClientStatus
import com.missa.b360.core.domain.usecase.ClientLifecycleRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClientStatusTransitionsTest {
    private fun client(statut: ClientStatus, nom: String = "Client Test") = ClientEntity(
        code = "CLI-TEST",
        nom = nom,
        telephone = "+237699000000",
        email = "client@example.cm",
        statut = statut,
        active = statut == ClientStatus.ACTIF,
        createdAt = 1L,
    )

    @Test fun `chaque statut a une entree dans la table`() {
        ClientStatus.entries.forEach { statut ->
            assertTrue("$statut absent de la table", ClientLifecycleRules.transitions.containsKey(statut))
        }
    }

    @Test fun `aucun statut ne transite vers lui meme`() {
        ClientStatus.entries.forEach { statut ->
            assertFalse(statut in ClientLifecycleRules.transitionsDepuis(statut))
        }
    }

    @Test fun `on ne revient jamais a brouillon ni au statut historique desactive`() {
        ClientLifecycleRules.transitions.values.forEach { cibles ->
            assertFalse(ClientStatus.BROUILLON in cibles)
            assertFalse(ClientStatus.DESACTIVE in cibles)
        }
    }

    @Test fun `un client archive ne peut etre que restaure en inactif`() {
        assertEquals(setOf(ClientStatus.INACTIF), ClientLifecycleRules.transitionsDepuis(ClientStatus.ARCHIVE))
        assertFalse(ClientLifecycleRules.peutTransiter(client(ClientStatus.ARCHIVE), ClientStatus.ACTIF))
    }

    @Test fun `un client actif ne peut pas etre archive directement`() {
        assertFalse(ClientLifecycleRules.peutTransiter(client(ClientStatus.ACTIF), ClientStatus.ARCHIVE))
        assertTrue(ClientLifecycleRules.peutTransiter(client(ClientStatus.ACTIF), ClientStatus.INACTIF))
    }

    @Test fun `les quatre statuts operationnels se transforment entre eux sans condition de fiche`() {
        val incomplet = client(ClientStatus.ACTIF, nom = "")
        assertTrue(ClientLifecycleRules.peutTransiter(incomplet, ClientStatus.SOUS_SURVEILLANCE))
        assertTrue(ClientLifecycleRules.peutTransiter(incomplet.copy(statut = ClientStatus.BLOQUE_CREDIT), ClientStatus.ACTIF))
        assertTrue(ClientLifecycleRules.peutTransiter(incomplet.copy(statut = ClientStatus.BLOQUE_ADMINISTRATIF), ClientStatus.BLOQUE_CREDIT))
    }

    @Test fun `la reactivation depuis un etat non operationnel exige une fiche activable`() {
        listOf(ClientStatus.BROUILLON, ClientStatus.A_COMPLETER, ClientStatus.INACTIF, ClientStatus.DESACTIVE).forEach { statut ->
            assertTrue("$statut -> ACTIF", ClientLifecycleRules.peutTransiter(client(statut), ClientStatus.ACTIF))
            assertFalse("$statut -> ACTIF sans nom", ClientLifecycleRules.peutTransiter(client(statut, nom = ""), ClientStatus.ACTIF))
        }
    }

    @Test fun `la creation rapide passe de a completer a actif ou inactif`() {
        assertEquals(
            setOf(ClientStatus.ACTIF, ClientStatus.INACTIF, ClientStatus.ARCHIVE),
            ClientLifecycleRules.transitionsDepuis(ClientStatus.A_COMPLETER),
        )
    }

    @Test fun `blocage inactivite et archive demandent une confirmation`() {
        listOf(
            ClientStatus.BLOQUE_CREDIT, ClientStatus.BLOQUE_ADMINISTRATIF,
            ClientStatus.INACTIF, ClientStatus.ARCHIVE,
        ).forEach { assertTrue(ClientLifecycleRules.confirmationRequise(it)) }
        listOf(ClientStatus.ACTIF, ClientStatus.SOUS_SURVEILLANCE, ClientStatus.A_COMPLETER)
            .forEach { assertFalse(ClientLifecycleRules.confirmationRequise(it)) }
    }
}
