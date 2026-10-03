package com.missa.b360

import com.missa.b360.core.domain.model.ClientCsvImport
import com.missa.b360.core.domain.model.ClientImportError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClientCsvImportTest {
    @Test fun `un fichier avec en-tete et separateur point virgule est lu`() {
        val texte = "nom;telephone;email;adresse\nAlice Ngo;+237699000001;alice@exemple.cm;Douala\nBob Eto;+237699000002;;"
        val apercu = ClientCsvImport.analyser(texte)
        assertEquals(2, apercu.valides.size)
        assertTrue(apercu.erreurs.isEmpty())
        assertEquals("Alice Ngo", apercu.valides[0].nom)
        assertEquals("alice@exemple.cm", apercu.valides[0].email)
        assertEquals("Douala", apercu.valides[0].adresse)
        assertEquals(null, apercu.valides[1].email)
        assertEquals(2, apercu.lignesLues)
    }

    @Test fun `le separateur virgule et l absence d en-tete sont detectes`() {
        val apercu = ClientCsvImport.analyser("Alice Ngo,+237699000001\nBob Eto,+237699000002")
        assertEquals(2, apercu.valides.size)
        assertEquals(2, apercu.lignesLues)
    }

    @Test fun `un champ entre guillemets peut contenir le separateur`() {
        val apercu = ClientCsvImport.analyser("nom;telephone\n\"Ngo; Alice\";+237699000001")
        assertEquals("Ngo; Alice", apercu.valides.single().nom)
    }

    @Test fun `le rapport d erreurs indique la ligne du fichier et le motif`() {
        val texte = listOf(
            "nom;telephone;email",
            "Alice Ngo;+237699000001;",
            ";+237699000002;",
            "X;+237699000003;",
            "Carl Eto;;",
            "Dora Eto;12;",
            "Eve Eto;+237699000005;pas-un-email",
            "Fred Eto;+237699000001;",
        ).joinToString("\n")
        val apercu = ClientCsvImport.analyser(texte)
        assertEquals(1, apercu.valides.size)
        assertEquals(
            listOf(
                3 to ClientImportError.NOM_MANQUANT,
                4 to ClientImportError.NOM_INVALIDE,
                5 to ClientImportError.TELEPHONE_MANQUANT,
                6 to ClientImportError.TELEPHONE_INVALIDE,
                7 to ClientImportError.EMAIL_INVALIDE,
                8 to ClientImportError.DOUBLON_FICHIER,
            ),
            apercu.erreurs.map { it.ligne to it.erreur },
        )
    }

    @Test fun `l import est partiel les lignes valides restent importables`() {
        val apercu = ClientCsvImport.analyser("nom;telephone\nAlice Ngo;+237699000001\n;;\nBob Eto;+237699000002")
        assertEquals(2, apercu.valides.size)
        assertEquals(1, apercu.erreurs.size)
    }

    @Test fun `l indicatif par defaut complete un numero local`() {
        val apercu = ClientCsvImport.analyser("nom;telephone\nAlice Ngo;699000001", indicatifTelephone = "+237")
        assertEquals("+237699000001", apercu.valides.single().telephone)
    }

    @Test fun `un numero deja international n est pas modifie`() {
        val apercu = ClientCsvImport.analyser("nom;telephone\nAlice Ngo;+33612345678", indicatifTelephone = "+237")
        assertEquals("+33612345678", apercu.valides.single().telephone)
    }

    @Test fun `le doublon est detecte meme avec une mise en forme differente`() {
        val apercu = ClientCsvImport.analyser("nom;telephone\nAlice Ngo;+237 699 00 00 01\nAlice Bis;+237699000001")
        assertEquals(1, apercu.valides.size)
        assertEquals(ClientImportError.DOUBLON_FICHIER, apercu.erreurs.single().erreur)
    }

    @Test fun `un fichier vide ou blanc ne produit rien`() {
        assertTrue(ClientCsvImport.analyser("").valides.isEmpty())
        assertTrue(ClientCsvImport.analyser("\n  \n").erreurs.isEmpty())
    }

    @Test fun `le BOM d un export tableur est ignore`() {
        val apercu = ClientCsvImport.analyser("\uFEFFnom;telephone\nAlice Ngo;+237699000001")
        assertEquals(1, apercu.valides.size)
    }

    @Test fun `au dela de la limite une seule erreur est signalee`() {
        val lignes = (1..ClientCsvImport.MAX_LIGNES + 5).joinToString("\n") { "Client $it;+2376${(10_000_000 + it)}" }
        val apercu = ClientCsvImport.analyser("nom;telephone\n$lignes")
        assertEquals(ClientCsvImport.MAX_LIGNES, apercu.valides.size)
        assertEquals(listOf(ClientImportError.TROP_DE_LIGNES), apercu.erreurs.map { it.erreur })
    }
}
