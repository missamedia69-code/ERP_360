package com.missa.b360

import com.missa.b360.core.data.entity.ClientAddressEntity
import com.missa.b360.core.data.entity.ClientContactEntity
import com.missa.b360.core.data.entity.ClientEntity
import com.missa.b360.core.data.entity.ClientType
import com.missa.b360.ui.clients.form.AddressDraft
import com.missa.b360.ui.clients.form.ClientDraft
import com.missa.b360.ui.clients.form.ClientDraftMapper
import com.missa.b360.ui.clients.form.ClientSection
import com.missa.b360.ui.clients.form.ContactDraft
import com.missa.b360.ui.clients.form.DraftField
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ClientDraftMapperTest {

    private fun valide() = ClientDraft(nom = "Boulangerie Atangana", codePays = "CM", telephoneLocal = "699 00 00 01")

    @Test fun `un brouillon valide produit une demande avec le telephone international`() {
        val controle = ClientDraftMapper.construire(valide(), clientId = 7)
        val demande = controle.demande
        assertNotNull(demande)
        assertEquals("+237699000001", demande!!.telephone)
        assertEquals(30, demande.profile.conditionPaiementJours)
        assertTrue(controle.erreurs.isEmpty())
    }

    @Test fun `nom trop court et telephone absent sans email sont signales`() {
        val controle = ClientDraftMapper.construire(ClientDraft(nom = "A"), clientId = 1)
        assertNull(controle.demande)
        assertTrue(DraftField.NOM in controle.erreurs)
        assertTrue(DraftField.TELEPHONE in controle.erreurs)
    }

    @Test fun `un email valide remplace le telephone`() {
        val controle = ClientDraftMapper.construire(ClientDraft(nom = "Alice Mbarga", email = "alice@exemple.cm"), clientId = 1)
        assertNotNull(controle.demande)
        assertEquals("", controle.demande!!.telephone)
    }

    @Test fun `delai limite et remise hors bornes sont refuses`() {
        val brouillon = valide().copy(delaiPaiement = "400", limiteCredit = "-5", remiseDefaut = "120", remiseMax = "x", tauxTva = "101")
        val erreurs = ClientDraftMapper.construire(brouillon, 1).erreurs
        assertTrue(DraftField.DELAI in erreurs)
        assertTrue(DraftField.LIMITE in erreurs)
        assertTrue(DraftField.REMISE in erreurs)
        assertTrue(DraftField.REMISE_MAX in erreurs)
        assertTrue(DraftField.TAUX_TVA in erreurs)
    }

    @Test fun `la virgule decimale est acceptee`() {
        val demande = ClientDraftMapper.construire(valide().copy(limiteCredit = "1500,50", remiseDefaut = "2,5"), 1).demande
        assertEquals(1500.5, demande!!.limiteCredit!!, 1e-9)
        assertEquals(2.5, demande.remiseDefautPct, 1e-9)
    }

    @Test fun `une exoneration sans motif est refusee`() {
        val erreurs = ClientDraftMapper.construire(valide().copy(exonereTva = true), 1).erreurs
        assertTrue(DraftField.MOTIF_EXONERATION in erreurs)
        assertEquals(ClientSection.FISCALITE, DraftField.MOTIF_EXONERATION.section)
    }

    @Test fun `un contact invalide est signale dans la section contacts`() {
        val brouillon = valide().copy(contacts = listOf(ContactDraft(nom = "B", telephone = "12")))
        val erreurs = ClientDraftMapper.construire(brouillon, 1).erreurs
        assertTrue(DraftField.CONTACTS in erreurs)
        assertEquals(ClientSection.CONTACTS, DraftField.CONTACTS.section)
    }

    @Test fun `une adresse trop courte est signalee`() {
        val brouillon = valide().copy(adresses = listOf(AddressDraft(adresse = "x")))
        assertTrue(DraftField.ADRESSES in ClientDraftMapper.construire(brouillon, 1).erreurs)
    }

    @Test fun `un seul contact et une seule adresse sont principaux`() {
        val contacts = ClientDraftMapper.contactsEntites(
            listOf(ContactDraft(nom = "Un", principal = true), ContactDraft(nom = "Deux", principal = true), ContactDraft()),
            clientId = 3,
        )
        assertEquals(2, contacts.size)
        assertEquals(1, contacts.count { it.principal })
        val adresses = ClientDraftMapper.adressesEntites(listOf(AddressDraft(adresse = "Rue 1"), AddressDraft(adresse = "Rue 2")), 3)
        assertEquals(1, adresses.count { it.principale })
        assertTrue(adresses.first().principale)
    }

    @Test fun `le brouillon fait l aller retour en json`() {
        val brouillon = valide().copy(
            type = ClientType.ENTREPRISE.name,
            contacts = listOf(ContactDraft(nom = "Chantal", principal = true)),
            adresses = listOf(AddressDraft(adresse = "Rue 1", ville = "Douala")),
        )
        val relu = ClientDraftMapper.depuisJson(ClientDraftMapper.versJson(brouillon))
        assertEquals(brouillon, relu)
        assertNull(ClientDraftMapper.depuisJson("pas du json"))
        assertNull(ClientDraftMapper.depuisJson(null))
    }

    @Test fun `la fiche existante se convertit en brouillon puis revient sans perte`() {
        val client = ClientEntity(
            id = 5, code = "CLI-1", nom = "Société Njoya", type = ClientType.ENTREPRISE,
            telephone = "+237699000001", email = "contact@njoya.cm", nif = "M1234", adresse = "Akwa",
            limiteCredit = 250000.0, remiseDefautPct = 2.5, conditionPaiementJours = 45, createdAt = 1L,
        )
        val contacts = listOf(ClientContactEntity(clientId = 5, nom = "Paul", telephone = "+237677000002", principal = true))
        val adresses = listOf(ClientAddressEntity(clientId = 5, libelle = "Siège", adresse = "Akwa", ville = "Douala", principale = true))
        val brouillon = ClientDraftMapper.depuis(client, contacts, adresses, "FR")
        assertEquals("CM", brouillon.codePays)
        assertEquals("699000001", brouillon.telephoneLocal)
        assertEquals("250000", brouillon.limiteCredit)
        assertEquals("2.5", brouillon.remiseDefaut)
        val demande = ClientDraftMapper.construire(brouillon, 5).demande
        assertNotNull(demande)
        assertEquals("+237699000001", demande!!.telephone)
        assertEquals(250000.0, demande.limiteCredit!!, 1e-9)
        assertEquals(45, demande.profile.conditionPaiementJours)
        assertEquals(1, demande.profile.contacts.size)
        assertFalse(demande.profile.addresses.isEmpty())
    }
}
