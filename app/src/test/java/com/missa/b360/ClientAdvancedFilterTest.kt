package com.missa.b360

import com.missa.b360.core.data.entity.ClientBalanceEntity
import com.missa.b360.core.data.entity.ClientEntity
import com.missa.b360.core.data.entity.ClientStatus
import com.missa.b360.core.data.entity.ClientType
import com.missa.b360.core.domain.model.ClientAdvancedFilter
import com.missa.b360.core.domain.model.ClientListFilter
import com.missa.b360.core.domain.model.ClientListItem
import com.missa.b360.core.domain.model.ClientListRules
import org.junit.Assert.assertEquals
import org.junit.Test

class ClientAdvancedFilterTest {
    private val now = 1_800_000_000_000L

    private fun ligne(id: Long, statut: ClientStatus, type: ClientType, encours: Double, enRetard: Double) = ClientListItem(
        client = ClientEntity(id = id, code = "C$id", nom = "Client $id", telephone = "+23769900000$id", type = type, statut = statut, createdAt = 1L),
        balance = ClientBalanceEntity(clientId = id, encours = encours, enRetard = enRetard),
    )

    private val items = listOf(
        ligne(1, ClientStatus.ACTIF, ClientType.PARTICULIER, 0.0, 0.0),
        ligne(2, ClientStatus.ACTIF, ClientType.ENTREPRISE, 100.0, 0.0),
        ligne(3, ClientStatus.BLOQUE_CREDIT, ClientType.ENTREPRISE, 300.0, 200.0),
    )

    private fun ids(filtre: ClientAdvancedFilter) =
        ClientListRules.filtrer(items, "", ClientListFilter.TOUS, now, filtre).map { it.client.id }

    @Test fun `sans critere tout passe et aucun critere n est actif`() {
        assertEquals(listOf(1L, 2L, 3L), ids(ClientAdvancedFilter()))
        assertEquals(0, ClientAdvancedFilter().actifs)
    }

    @Test fun `le filtre de statut garde les statuts choisis`() {
        assertEquals(listOf(3L), ids(ClientAdvancedFilter(statuts = setOf(ClientStatus.BLOQUE_CREDIT))))
    }

    @Test fun `les criteres se cumulent`() {
        val filtre = ClientAdvancedFilter(types = setOf(ClientType.ENTREPRISE), avecEncours = true, enRetard = true)
        assertEquals(listOf(3L), ids(filtre))
        assertEquals(3, filtre.actifs)
    }

    @Test fun `avec encours exclut les comptes a zero`() {
        assertEquals(listOf(2L, 3L), ids(ClientAdvancedFilter(avecEncours = true)))
    }
}
