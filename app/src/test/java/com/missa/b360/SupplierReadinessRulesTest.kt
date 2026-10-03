package com.missa.b360

import com.missa.b360.core.data.entity.FournisseurEntity
import com.missa.b360.core.data.entity.FournisseurStatus
import com.missa.b360.core.data.entity.VerificationStatut
import com.missa.b360.core.domain.model.SupplierAccountInfo
import com.missa.b360.core.domain.model.SupplierDocumentInfo
import com.missa.b360.core.domain.model.SupplierReadinessLevel
import com.missa.b360.core.domain.model.SupplierReadinessReason
import com.missa.b360.core.domain.model.SupplierReadinessRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SupplierReadinessRulesTest {
    private val jour = 86_400_000L
    private val now = 1_000L * jour

    private fun fournisseur(
        statut: FournisseurStatus = FournisseurStatus.ACTIF,
        paiementBloque: Boolean = false,
        complet: Boolean = true,
    ) = FournisseurEntity(
        code = "FRN-2026-0001",
        nom = "Atelier Nord",
        telephone = "650000000",
        adresse = if (complet) "Rue 1" else null,
        identifiantFiscal = "M123",
        conditionsPaiement = if (complet) "30 jours" else null,
        categoriesFournies = if (complet) "Matières" else null,
        statut = statut,
        paiementBloque = paiementBloque,
        createdAt = 0,
    )

    private fun evaluer(
        f: FournisseurEntity,
        contact: Boolean = true,
        documents: List<SupplierDocumentInfo> = emptyList(),
        comptes: List<SupplierAccountInfo> = emptyList(),
    ) = SupplierReadinessRules.evaluer(f, contact, documents, comptes, now)

    @Test
    fun `un fournisseur actif sans anomalie est pret`() {
        val r = evaluer(fournisseur())
        assertEquals(SupplierReadinessLevel.PRET, r.niveau)
        assertTrue(r.motifs.isEmpty())
        assertTrue(r.manquants.isEmpty())
    }

    @Test
    fun `statut bloque ou archive donne bloque avec son motif`() {
        val bloque = evaluer(fournisseur(FournisseurStatus.BLOQUE))
        assertEquals(SupplierReadinessLevel.BLOQUE, bloque.niveau)
        assertEquals(listOf(SupplierReadinessReason.STATUT_BLOQUE), bloque.motifs)
        val archive = evaluer(fournisseur(FournisseurStatus.ARCHIVE))
        assertEquals(SupplierReadinessLevel.BLOQUE, archive.niveau)
        assertEquals(listOf(SupplierReadinessReason.STATUT_ARCHIVE), archive.motifs)
    }

    @Test
    fun `brouillon a valider et suspendu sont a regulariser`() {
        listOf(FournisseurStatus.BROUILLON, FournisseurStatus.A_VALIDER, FournisseurStatus.SUSPENDU).forEach { statut ->
            val r = evaluer(fournisseur(statut))
            assertEquals(statut.name, SupplierReadinessLevel.A_REGULARISER, r.niveau)
            assertTrue(statut.name, SupplierReadinessReason.STATUT_NON_ACTIF in r.motifs)
        }
    }

    @Test
    fun `paiement bloque rend a regulariser`() {
        val r = evaluer(fournisseur(paiementBloque = true))
        assertEquals(SupplierReadinessLevel.A_REGULARISER, r.niveau)
        assertEquals(listOf(SupplierReadinessReason.PAIEMENT_BLOQUE), r.motifs)
    }

    @Test
    fun `dossier incomplet liste les manquants seulement avant validation`() {
        val brouillon = evaluer(fournisseur(FournisseurStatus.BROUILLON, complet = false), contact = false)
        assertTrue(SupplierReadinessReason.DOSSIER_INCOMPLET in brouillon.motifs)
        assertTrue("adresse" in brouillon.manquants)
        assertTrue("contact_principal" in brouillon.manquants)
        val actif = evaluer(fournisseur(FournisseurStatus.ACTIF, complet = false), contact = false)
        assertEquals(SupplierReadinessLevel.PRET, actif.niveau)
        assertTrue(actif.manquants.isEmpty())
    }

    @Test
    fun `document expire rejete ou archive`() {
        val expire = SupplierDocumentInfo(now - jour, VerificationStatut.VERIFIE)
        assertTrue(SupplierReadinessReason.DOCUMENT_EXPIRE in evaluer(fournisseur(), documents = listOf(expire)).motifs)

        val archive = expire.copy(archive = true)
        assertEquals(SupplierReadinessLevel.PRET, evaluer(fournisseur(), documents = listOf(archive)).niveau)

        val rejete = SupplierDocumentInfo(null, VerificationStatut.REJETE)
        assertTrue(SupplierReadinessReason.DOCUMENT_REJETE in evaluer(fournisseur(), documents = listOf(rejete)).motifs)
        assertEquals(SupplierReadinessLevel.PRET, evaluer(fournisseur(), documents = listOf(rejete.copy(archive = true))).niveau)
    }

    @Test
    fun `un document qui expire bientot n altere pas laptitude`() {
        listOf(2L, 20L, 80L).forEach { jours ->
            val doc = SupplierDocumentInfo(now + jours * jour, VerificationStatut.VERIFIE)
            assertEquals("J+$jours", SupplierReadinessLevel.PRET, evaluer(fournisseur(), documents = listOf(doc)).niveau)
        }
    }

    @Test
    fun `aucun compte bancaire reste valide`() {
        assertEquals(SupplierReadinessLevel.PRET, evaluer(fournisseur(), comptes = emptyList()).niveau)
    }

    @Test
    fun `comptes sans aucun verifie sont a regulariser`() {
        val ancien = now - 30 * jour
        val r = evaluer(
            fournisseur(),
            comptes = listOf(SupplierAccountInfo(VerificationStatut.A_VERIFIER, ancien), SupplierAccountInfo(VerificationStatut.REJETE, ancien)),
        )
        assertEquals(listOf(SupplierReadinessReason.COMPTE_NON_VERIFIE), r.motifs)

        val avecVerifie = evaluer(
            fournisseur(),
            comptes = listOf(SupplierAccountInfo(VerificationStatut.VERIFIE, ancien), SupplierAccountInfo(VerificationStatut.A_VERIFIER, ancien)),
        )
        assertEquals(SupplierReadinessLevel.PRET, avecVerifie.niveau)
    }

    @Test
    fun `compte non verifie modifie il y a moins de sept jours`() {
        fun avec(joursEcoules: Long) = evaluer(
            fournisseur(),
            comptes = listOf(
                SupplierAccountInfo(VerificationStatut.VERIFIE, now - 100 * jour),
                SupplierAccountInfo(VerificationStatut.A_VERIFIER, now - joursEcoules * jour),
            ),
        )
        assertTrue(SupplierReadinessReason.COMPTE_RECENT_NON_VERIFIE in avec(6).motifs)
        assertEquals(SupplierReadinessLevel.PRET, avec(7).niveau)
        assertEquals(SupplierReadinessLevel.PRET, avec(8).niveau)
    }

    @Test
    fun `motifs cumules dans un ordre stable`() {
        val r = evaluer(
            fournisseur(FournisseurStatus.SUSPENDU, paiementBloque = true),
            documents = listOf(SupplierDocumentInfo(now - jour, VerificationStatut.REJETE)),
        )
        assertEquals(
            listOf(
                SupplierReadinessReason.STATUT_NON_ACTIF,
                SupplierReadinessReason.PAIEMENT_BLOQUE,
                SupplierReadinessReason.DOCUMENT_EXPIRE,
                SupplierReadinessReason.DOCUMENT_REJETE,
            ),
            r.motifs,
        )
    }
}
