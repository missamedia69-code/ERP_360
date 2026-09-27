package com.missa.b360

import com.missa.b360.core.data.entity.ClientEntity
import com.missa.b360.core.data.entity.ClientStatus
import com.missa.b360.core.data.entity.ClientType
import com.missa.b360.core.domain.usecase.ClientLifecycleRules
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClientLifecycleRulesTest {
    private fun client(
        status: ClientStatus = ClientStatus.BROUILLON,
        active: Boolean = false,
        type: ClientType = ClientType.PARTICULIER,
        nif: String? = null,
        address: String? = null,
    ) = ClientEntity(
        code = "CLI-TEST",
        nom = "Client Test",
        telephone = "+237699000000",
        email = "client@example.cm",
        type = type,
        statut = status,
        active = active,
        nif = nif,
        adresse = address,
        createdAt = 1L,
    )

    @Test fun brouillonValidePeutEtreActiveMaisPasSilCoordonneesManquent() {
        assertTrue(ClientLifecycleRules.peutActiver(client()))
        assertFalse(ClientLifecycleRules.peutActiver(client().copy(telephone = "", email = null)))
    }

    @Test fun unEmailValideSuffitQuandAucunTelephoneNestFourni() {
        assertTrue(ClientLifecycleRules.peutActiver(client().copy(telephone = "")))
        assertFalse(ClientLifecycleRules.peutActiver(client().copy(telephone = "", email = "pas-un-email")))
    }

    @Test fun activationEntrepriseExigeInformationsFiscalesEtAdresse() {
        assertFalse(ClientLifecycleRules.peutActiver(client(type = ClientType.ENTREPRISE)))
        assertTrue(ClientLifecycleRules.peutActiver(client(type = ClientType.ENTREPRISE, nif = "M012345678901A", address = "Douala")))
    }

    @Test fun brouillonEtBlocageCreditAcceptentUniquementUnReglementIntegral() {
        assertTrue(ClientLifecycleRules.venteAutorisee(client(), montant = 100.0, regle = 100.0))
        assertFalse(ClientLifecycleRules.venteAutorisee(client(), montant = 100.0, regle = 99.0))
        val bloque = client(status = ClientStatus.BLOQUE_CREDIT, active = true)
        assertTrue(ClientLifecycleRules.venteAutorisee(bloque, montant = 100.0, regle = 100.0))
        assertFalse(ClientLifecycleRules.venteAutorisee(bloque, montant = 100.0, regle = 50.0))
    }

    @Test fun statutsAdministratifsEtArchivesRefusentLaVente() {
        listOf(ClientStatus.BLOQUE_ADMINISTRATIF, ClientStatus.INACTIF, ClientStatus.ARCHIVE, ClientStatus.DESACTIVE)
            .forEach { status ->
                assertFalse(ClientLifecycleRules.venteAutorisee(client(status, active = false), 100.0, 100.0))
            }
    }
}
