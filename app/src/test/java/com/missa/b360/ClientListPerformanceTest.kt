package com.missa.b360

import com.missa.b360.core.data.entity.ClientBalanceEntity
import com.missa.b360.core.data.entity.ClientEntity
import com.missa.b360.core.data.entity.ClientStatus
import com.missa.b360.core.data.entity.ClientType
import com.missa.b360.core.domain.model.ClientListFilter
import com.missa.b360.core.domain.model.ClientListRules
import com.missa.b360.core.domain.model.ClientListSort
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Garde-fou de volumétrie : 5 000 clients, construction + recherche + tri + compteurs. */
class ClientListPerformanceTest {
    private val now = 2_000_000_000_000L
    private val nombre = 5_000

    private val clients = (1..nombre).map { i ->
        ClientEntity(
            id = i.toLong(), code = "CLI-$i", nom = "Client numero $i", type = ClientType.PARTICULIER,
            telephone = "+2376${(10_000_000 + i)}", statut = ClientStatus.ACTIF, createdAt = now - i * 86_400_000L,
        )
    }
    private val comptes = clients.associate { c ->
        c.id to ClientBalanceEntity(clientId = c.id, encours = (c.id % 97).toDouble(), enRetard = (c.id % 7).toDouble())
    }

    @Test fun `la liste de 5000 clients se construit, se filtre et se trie en moins de deux secondes`() {
        val debut = System.nanoTime()
        val items = ClientListRules.construireItems(clients, comptes, emptyList(), now)
        val trouves = ClientListRules.filtrer(items, "numero 49", ClientListFilter.TOUS, now)
        val tries = ClientListRules.trier(items, ClientListSort.ENCOURS)
        val compteurs = ClientListRules.compteurs(items, now)
        val ms = (System.nanoTime() - debut) / 1_000_000

        assertEquals(nombre, items.size)
        assertEquals(nombre, compteurs.total)
        assertEquals(nombre, tries.size)
        assertTrue(trouves.isNotEmpty() && trouves.all { it.client.nom.contains("numero 49") })
        assertTrue("trop lent : $ms ms", ms < 2_000)
    }
}
