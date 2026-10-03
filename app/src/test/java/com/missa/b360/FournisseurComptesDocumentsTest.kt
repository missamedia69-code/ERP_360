package com.missa.b360

import com.missa.b360.core.data.dao.FournisseurCompteBancaireDao
import com.missa.b360.core.data.dao.FournisseurDocumentDao
import com.missa.b360.core.data.dao.FournisseurEvenementDao
import com.missa.b360.core.data.entity.FournisseurCompteBancaireEntity
import com.missa.b360.core.data.entity.FournisseurDocType
import com.missa.b360.core.data.entity.FournisseurDocumentEntity
import com.missa.b360.core.data.entity.FournisseurEvenementEntity
import com.missa.b360.core.data.entity.FournisseurEvenementType
import com.missa.b360.core.data.entity.VerificationStatut
import com.missa.b360.core.domain.usecase.FournisseurCompteEditor
import com.missa.b360.core.domain.usecase.FournisseurDocumentArchiver
import java.lang.reflect.Proxy
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FournisseurComptesDocumentsTest {
    private val now = 5_000_000L

    private inline fun <reified T> faux(crossinline reponse: (String, Array<Any?>) -> Any?): T =
        Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { _, m, args ->
            reponse(m.name, args ?: emptyArray())
        } as T

    private class Journal {
        val evenements = mutableListOf<FournisseurEvenementEntity>()
    }

    private fun evenementDao(j: Journal) = faux<FournisseurEvenementDao> { nom, args ->
        if (nom == "insert") { j.evenements += args[0] as FournisseurEvenementEntity; 1L } else error("appel inattendu $nom")
    }

    // --- Documents ---

    private class Documents(var document: FournisseurDocumentEntity?) {
        var archivages = 0
    }

    private fun documentDao(d: Documents) = faux<FournisseurDocumentDao> { nom, args ->
        when (nom) {
            "getById" -> d.document?.takeIf { it.id == args[0] as Long }
            "archiver" -> {
                d.archivages++
                d.document = d.document?.copy(archive = true)
                1
            }
            else -> error("appel inattendu $nom")
        }
    }

    @Test fun `retirer un document l archive et le journalise sans le supprimer`() = runBlocking {
        val docs = Documents(FournisseurDocumentEntity(id = 3, fournisseurId = 7, typeDocument = FournisseurDocType.AUTRE, cheminFichier = "f.jpg"))
        val j = Journal()
        assertTrue(FournisseurDocumentArchiver(documentDao(docs), evenementDao(j)).archiver(3, now))
        assertTrue(docs.document!!.archive)
        assertEquals("f.jpg", docs.document!!.cheminFichier)
        assertEquals(1, j.evenements.size)
        assertEquals(7L, j.evenements[0].fournisseurId)
        assertEquals(FournisseurEvenementType.DOCUMENT_SUPPRIME, j.evenements[0].type)
    }

    @Test fun `un document inconnu ou deja archive n est pas archive deux fois`() = runBlocking {
        val j = Journal()
        val vide = Documents(null)
        assertFalse(FournisseurDocumentArchiver(documentDao(vide), evenementDao(j)).archiver(3, now))
        val deja = Documents(FournisseurDocumentEntity(id = 3, fournisseurId = 7, archive = true))
        assertFalse(FournisseurDocumentArchiver(documentDao(deja), evenementDao(j)).archiver(3, now))
        assertEquals(0, deja.archivages)
        assertTrue(j.evenements.isEmpty())
    }

    // --- Comptes ---

    private class Comptes(var compte: FournisseurCompteBancaireEntity?) {
        var principalRetireDe: Long? = null
    }

    private fun compteDao(c: Comptes) = faux<FournisseurCompteBancaireDao> { nom, args ->
        when (nom) {
            "getById" -> c.compte?.takeIf { it.id == args[0] as Long }
            "update" -> { c.compte = args[0] as FournisseurCompteBancaireEntity; Unit }
            "retirerComptePrincipal" -> { c.principalRetireDe = args[0] as Long; Unit }
            else -> error("appel inattendu $nom")
        }
    }

    private val verifie = FournisseurCompteBancaireEntity(
        id = 1, fournisseurId = 7, titulaire = "Société Alpha", banque = "Banque X", iban = "CM2110002000300040005000678",
        verification = VerificationStatut.VERIFIE, verifieLe = 1_000L, modifieLe = 500L,
    )

    @Test fun `changer l iban remet le compte a verifier et date la modification`() = runBlocking {
        val c = Comptes(verifie)
        val j = Journal()
        val resultat = FournisseurCompteEditor(compteDao(c), evenementDao(j))
            .modifier(verifie.copy(iban = "CM2110002000300040005000999"), now)
        assertEquals(true, resultat)
        assertEquals(VerificationStatut.A_VERIFIER, c.compte!!.verification)
        assertNull(c.compte!!.verifieLe)
        assertEquals(now, c.compte!!.modifieLe)
        assertEquals(FournisseurEvenementType.COMPTE_MODIFIE, j.evenements.single().type)
    }

    @Test fun `modifier la banque ou les notes conserve la verification`() = runBlocking {
        val c = Comptes(verifie)
        val j = Journal()
        val resultat = FournisseurCompteEditor(compteDao(c), evenementDao(j))
            .modifier(verifie.copy(banque = "Autre banque", notes = "agence centrale"), now)
        assertEquals(false, resultat)
        assertEquals(VerificationStatut.VERIFIE, c.compte!!.verification)
        assertEquals(1_000L, c.compte!!.verifieLe)
        assertEquals(500L, c.compte!!.modifieLe)
        assertEquals("Autre banque", c.compte!!.banque)
    }

    @Test fun `l audit ne contient jamais de numero de compte`() = runBlocking {
        val c = Comptes(verifie)
        val j = Journal()
        FournisseurCompteEditor(compteDao(c), evenementDao(j))
            .modifier(verifie.copy(iban = "CM2110002000300040005000999"), now)
        val details = j.evenements.single().details.orEmpty()
        assertFalse(details.contains("999"))
        assertFalse(details.contains("CM21"))
    }

    @Test fun `un compte invalide ou d un autre fournisseur est refuse`() = runBlocking {
        val c = Comptes(verifie)
        val j = Journal()
        val editor = FournisseurCompteEditor(compteDao(c), evenementDao(j))
        assertNull(editor.modifier(verifie.copy(titulaire = ""), now))
        assertNull(editor.modifier(verifie.copy(fournisseurId = 8), now))
        assertNull(editor.modifier(verifie.copy(id = 99), now))
        assertEquals(verifie, c.compte)
        assertTrue(j.evenements.isEmpty())
    }

    @Test fun `un compte principal retire le statut aux autres comptes du fournisseur`() = runBlocking {
        val c = Comptes(verifie)
        FournisseurCompteEditor(compteDao(c), evenementDao(Journal())).modifier(verifie.copy(principal = true), now)
        assertEquals(7L, c.principalRetireDe)
        assertTrue(c.compte!!.principal)
    }
}
