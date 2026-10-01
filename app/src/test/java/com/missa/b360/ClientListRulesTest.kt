package com.missa.b360

import com.missa.b360.core.data.entity.ClientBalanceEntity
import com.missa.b360.core.data.entity.ClientEntity
import com.missa.b360.core.data.entity.ClientStatus
import com.missa.b360.core.data.entity.ClientType
import com.missa.b360.core.domain.model.ClientListFilter
import com.missa.b360.core.domain.model.ClientListItem
import com.missa.b360.core.domain.model.ClientListRules
import com.missa.b360.core.domain.model.ClientListSort
import com.missa.b360.core.domain.model.ClientMetricsRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClientListRulesTest {
    private val day = ClientMetricsRules.DAY_MS
    private val now = 2_000_000_000_000L

    private fun client(
        id: Long,
        nom: String = "Client $id",
        statut: ClientStatus = ClientStatus.ACTIF,
        telephone: String = "+23769900000$id",
        creeIlYa: Long = 200,
        type: ClientType = ClientType.PARTICULIER,
        nif: String? = null,
        adresse: String? = null,
        email: String? = null,
    ) = ClientEntity(
        id = id, code = "CLI-$id", nom = nom, type = type, telephone = telephone, nif = nif,
        adresse = adresse, email = email, statut = statut, createdAt = now - creeIlYa * day,
    )

    private fun item(
        client: ClientEntity,
        encours: Double = 0.0,
        derniereVenteIlYa: Long? = null,
        aRelancer: Boolean = false,
    ) = ClientListItem(
        client = client,
        balance = ClientBalanceEntity(
            clientId = client.id,
            encours = encours,
            derniereVenteAt = derniereVenteIlYa?.let { now - it * day },
        ),
        aRelancer = aRelancer,
    )

    @Test fun `la recherche couvre nom code telephone et NIF`() {
        val c = client(1, nom = "Boulangerie Mbeki", nif = "M0123456789")
        assertTrue(ClientListRules.correspond(c, "mbeki"))
        assertTrue(ClientListRules.correspond(c, "cli-1"))
        assertTrue(ClientListRules.correspond(c, "9000001"))
        assertTrue(ClientListRules.correspond(c, "m0123"))
        assertFalse(ClientListRules.correspond(c, "introuvable"))
        assertTrue(ClientListRules.correspond(c, "   "))
    }

    @Test fun `la recherche telephone ignore les espaces et tirets`() {
        val c = client(1, telephone = "+237699000001")
        assertTrue(ClientListRules.correspond(c, "699 00-00 01"))
        assertFalse(ClientListRules.correspond(c, "69"))
    }

    @Test fun `un client bloque credit ou administratif est bloque`() {
        assertTrue(ClientListRules.estBloque(client(1, statut = ClientStatus.BLOQUE_CREDIT)))
        assertTrue(ClientListRules.estBloque(client(2, statut = ClientStatus.BLOQUE_ADMINISTRATIF)))
        assertFalse(ClientListRules.estBloque(client(3, statut = ClientStatus.SOUS_SURVEILLANCE)))
    }

    @Test fun `une fiche a completer ou sans coordonnees est incomplete`() {
        assertTrue(ClientListRules.estIncomplet(client(1, statut = ClientStatus.A_COMPLETER)))
        assertTrue(ClientListRules.estIncomplet(client(2, statut = ClientStatus.BROUILLON)))
        assertTrue(ClientListRules.estIncomplet(client(3, telephone = "")))
        assertTrue(ClientListRules.estIncomplet(client(4, type = ClientType.ENTREPRISE)))
        assertFalse(
            ClientListRules.estIncomplet(client(5, type = ClientType.ENTREPRISE, nif = "N1", adresse = "Douala")),
        )
        assertFalse(ClientListRules.estIncomplet(client(6)))
    }

    @Test fun `un client est nouveau pendant 30 jours`() {
        assertTrue(ClientListRules.estNouveau(client(1, creeIlYa = 30), now))
        assertFalse(ClientListRules.estNouveau(client(2, creeIlYa = 31), now))
    }

    @Test fun `sans achat 90 jours repose sur la derniere vente puis sur la creation`() {
        assertTrue(ClientListRules.sansAchat90j(item(client(1), derniereVenteIlYa = 91), now))
        assertFalse(ClientListRules.sansAchat90j(item(client(2), derniereVenteIlYa = 90), now))
        assertTrue(ClientListRules.sansAchat90j(item(client(3, creeIlYa = 120)), now))
        assertFalse(ClientListRules.sansAchat90j(item(client(4, creeIlYa = 20)), now))
    }

    @Test fun `sans achat 90 jours ignore les clients hors service`() {
        listOf(ClientStatus.BROUILLON, ClientStatus.INACTIF, ClientStatus.ARCHIVE, ClientStatus.BLOQUE_CREDIT).forEach {
            assertFalse(it.name, ClientListRules.sansAchat90j(item(client(1, statut = it), derniereVenteIlYa = 400), now))
        }
    }

    @Test fun `les filtres retiennent les bonnes lignes`() {
        val items = listOf(
            item(client(1), aRelancer = true),
            item(client(2, statut = ClientStatus.BLOQUE_CREDIT)),
            item(client(3, creeIlYa = 5)),
            item(client(4, statut = ClientStatus.A_COMPLETER)),
        )
        fun ids(f: ClientListFilter) = ClientListRules.filtrer(items, "", f, now).map { it.client.id }
        assertEquals(listOf(1L), ids(ClientListFilter.A_RELANCER))
        assertEquals(listOf(2L), ids(ClientListFilter.BLOQUES))
        assertEquals(listOf(3L), ids(ClientListFilter.NOUVEAUX))
        assertEquals(listOf(4L), ids(ClientListFilter.INCOMPLETS))
        assertEquals(4, ids(ClientListFilter.TOUS).size)
    }

    @Test fun `le filtre se combine avec la recherche`() {
        val items = listOf(
            item(client(1, nom = "Alpha"), aRelancer = true),
            item(client(2, nom = "Beta"), aRelancer = true),
        )
        val resultat = ClientListRules.filtrer(items, "alp", ClientListFilter.A_RELANCER, now)
        assertEquals(listOf(1L), resultat.map { it.client.id })
    }

    @Test fun `le tri par encours est decroissant puis alphabetique`() {
        val items = listOf(
            item(client(1, nom = "Zoe"), encours = 10.0),
            item(client(2, nom = "Abel"), encours = 10.0),
            item(client(3, nom = "Max"), encours = 500.0),
            ClientListItem(client(4, nom = "Sans compte"), balance = null),
        )
        assertEquals(listOf(3L, 2L, 1L, 4L), ClientListRules.trier(items, ClientListSort.ENCOURS).map { it.client.id })
    }

    @Test fun `le tri par nom ignore la casse`() {
        val items = listOf(item(client(1, nom = "bravo")), item(client(2, nom = "Alpha")), item(client(3, nom = "Charlie")))
        assertEquals(listOf(2L, 1L, 3L), ClientListRules.trier(items, ClientListSort.NOM).map { it.client.id })
    }

    @Test fun `le tri par derniere vente place les clients sans vente en dernier`() {
        val items = listOf(
            item(client(1, nom = "A"), derniereVenteIlYa = 50),
            item(client(2, nom = "B")),
            item(client(3, nom = "C"), derniereVenteIlYa = 2),
        )
        assertEquals(listOf(3L, 1L, 2L), ClientListRules.trier(items, ClientListSort.DERNIERE_VENTE).map { it.client.id })
    }

    @Test fun `les compteurs correspondent aux filtres`() {
        val items = listOf(
            item(client(1), aRelancer = true, derniereVenteIlYa = 100),
            item(client(2, statut = ClientStatus.BLOQUE_ADMINISTRATIF)),
            item(client(3, creeIlYa = 3), derniereVenteIlYa = 1),
            item(client(4, statut = ClientStatus.A_COMPLETER, creeIlYa = 3)),
        )
        val compteurs = ClientListRules.compteurs(items, now)
        ClientListFilter.entries.forEach { filtre ->
            assertEquals(filtre.name, ClientListRules.filtrer(items, "", filtre, now).size, compteurs.pour(filtre))
        }
        assertEquals(4, compteurs.total)
    }
}
