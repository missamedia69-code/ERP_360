package com.missa.b360

import com.missa.b360.core.data.entity.EnterpriseEntity
import com.missa.b360.core.documents.DocumentDonnees
import com.missa.b360.core.documents.DocumentLigne
import com.missa.b360.core.documents.DocumentType
import com.missa.b360.core.documents.DocumentValidation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DocumentModelsTest {
    private val entreprise = EnterpriseEntity(nom = "Entreprise cliente", devise = "XAF", langue = "fr")

    @Test
    fun `le catalogue couvre tous les domaines documentaires`() {
        assertTrue(DocumentType.entries.size >= 26)
        assertEquals(DocumentType.entries.size, DocumentType.entries.map { it.prefixe }.distinct().size)
        assertTrue(DocumentType.entries.all { it.titreDefaut.isNotBlank() && it.prefixe.isNotBlank() })
    }

    @Test
    fun `la pagination ne perd ni ne duplique aucune ligne`() {
        val lignes = (1..57).map { DocumentLigne("Article $it") }
        val pages = DocumentValidation.paginer(lignes)
        assertEquals(listOf(12, 18, 18, 9), pages.map { it.size })
        assertEquals(lignes, pages.flatten())
    }

    @Test
    fun `un document vide conserve une page imprimable`() {
        assertEquals(listOf(emptyList<DocumentLigne>()), DocumentValidation.paginer(emptyList()))
    }

    @Test
    fun `les donnees legales et montants invalides sont refuses`() {
        val document = DocumentDonnees(
            type = DocumentType.FACTURE_CLIENT,
            numero = "",
            dateEmission = "",
            entreprise = entreprise.copy(nom = ""),
            lignes = listOf(DocumentLigne("", quantite = -1.0, montant = Double.NaN)),
        )
        val erreurs = DocumentValidation.erreurs(document)
        assertTrue(erreurs.size >= 5)
    }
}
