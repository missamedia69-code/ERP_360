package com.missa.b360

import com.missa.b360.core.documents.DocumentFilePolicy
import com.missa.b360.core.documents.DocumentSharing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File
import kotlin.io.path.createTempDirectory

class DocumentFilePolicyTest {
    @Test
    fun `un fichier absent ou vide nest jamais partage`() {
        val dossier = createTempDirectory("document-partage-").toFile()
        assertEquals(DocumentSharing.ErreurPartage.FICHIER_ABSENT, DocumentFilePolicy.erreur(File(dossier, "absent.pdf")))
        val vide = File(dossier, "vide.pdf").apply { writeBytes(byteArrayOf()) }
        assertEquals(DocumentSharing.ErreurPartage.FICHIER_VIDE, DocumentFilePolicy.erreur(vide))
        dossier.deleteRecursively()
    }

    @Test
    fun `extension et signature PDF sont toutes deux controlees`() {
        val dossier = createTempDirectory("document-partage-").toFile()
        val faux = File(dossier, "faux.pdf").apply { writeText("contenu non pdf") }
        val mauvaiseExtension = File(dossier, "document.txt").apply { writeText("%PDF-1.4") }
        val correct = File(dossier, "document.pdf").apply { writeText("%PDF-1.4\ncontenu") }
        assertEquals(DocumentSharing.ErreurPartage.FORMAT_INVALIDE, DocumentFilePolicy.erreur(faux))
        assertEquals(DocumentSharing.ErreurPartage.FORMAT_INVALIDE, DocumentFilePolicy.erreur(mauvaiseExtension))
        assertNull(DocumentFilePolicy.erreur(correct))
        dossier.deleteRecursively()
    }

    @Test
    fun `le nom de tache dimpression est nettoye et borne`() {
        assertEquals("FAC-2026-0042-client", DocumentFilePolicy.nomSain("FAC:2026/0042*client"))
        assertEquals("Document", DocumentFilePolicy.nomSain("///"))
        assertEquals(80, DocumentFilePolicy.nomSain("a".repeat(120)).length)
    }
}
