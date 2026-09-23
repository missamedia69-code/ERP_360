package com.missa.b360

import com.missa.b360.core.data.entity.FournisseurEntity
import com.missa.b360.core.data.entity.FournisseurStatus
import com.missa.b360.core.data.entity.TypeFournisseur
import com.missa.b360.core.domain.model.FournisseurRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Référentiel fournisseur : cycle de vie, droits par statut, fiscalité par pays,
 * réapprobation des modifications sensibles et alertes de documents.
 */
class FournisseurRulesTest {

    private val jour = 24L * 60 * 60 * 1000

    private fun base(
        statut: FournisseurStatus = FournisseurStatus.ACTIF,
        type: TypeFournisseur = TypeFournisseur.ENTREPRISE,
        pays: String = "CM",
        nom: String = "Société Alpha",
        telephone: String = "+237600000000",
        identifiant: String? = "M0123456789",
    ) = FournisseurEntity(
        code = "FRN-2026-0001",
        nom = nom,
        telephone = telephone,
        adresse = "Douala, Bonapriso",
        statut = statut,
        createdAt = 0,
        type = type,
        pays = pays,
        identifiantFiscal = identifiant,
        conditionsPaiement = "30 jours fin de mois",
        categoriesFournies = "Matières premières",
    )

    // --- Cycle de vie (spec §3) ---

    @Test
    fun `le cycle de vie suit le workflow defini`() {
        assertTrue(FournisseurRules.transitionAutorisee(FournisseurStatus.BROUILLON, FournisseurStatus.A_VALIDER))
        assertFalse(FournisseurRules.transitionAutorisee(FournisseurStatus.BROUILLON, FournisseurStatus.ACTIF))
        assertTrue(FournisseurRules.transitionAutorisee(FournisseurStatus.A_VALIDER, FournisseurStatus.ACTIF))
        assertTrue(FournisseurRules.transitionAutorisee(FournisseurStatus.A_VALIDER, FournisseurStatus.BROUILLON))
        assertFalse(FournisseurRules.transitionAutorisee(FournisseurStatus.ACTIF, FournisseurStatus.BROUILLON))
        assertTrue(FournisseurRules.transitionAutorisee(FournisseurStatus.ACTIF, FournisseurStatus.SUSPENDU))
        assertTrue(FournisseurRules.transitionAutorisee(FournisseurStatus.ACTIF, FournisseurStatus.BLOQUE))
        assertTrue(FournisseurRules.transitionAutorisee(FournisseurStatus.SUSPENDU, FournisseurStatus.ACTIF))
        assertTrue(FournisseurRules.transitionAutorisee(FournisseurStatus.BLOQUE, FournisseurStatus.SUSPENDU))
        assertFalse(FournisseurRules.transitionAutorisee(FournisseurStatus.BLOQUE, FournisseurStatus.ACTIF))
        assertTrue(FournisseurRules.transitions(FournisseurStatus.ARCHIVE).isEmpty())
    }

    @Test
    fun `seul un fournisseur actif peut recevoir une commande`() {
        FournisseurStatus.entries.forEach { statut ->
            assertEquals(
                statut == FournisseurStatus.ACTIF,
                FournisseurRules.peutCommander(statut),
            )
        }
    }

    @Test
    fun `le paiement reste possible pour honorer les factures existantes`() {
        assertTrue(FournisseurRules.peutEtrePaye(FournisseurStatus.ACTIF))
        assertTrue(FournisseurRules.peutEtrePaye(FournisseurStatus.SUSPENDU))
        assertTrue(FournisseurRules.peutEtrePaye(FournisseurStatus.BLOQUE))
        assertFalse(FournisseurRules.peutEtrePaye(FournisseurStatus.BROUILLON))
        assertFalse(FournisseurRules.peutEtrePaye(FournisseurStatus.A_VALIDER))
        assertFalse(FournisseurRules.peutEtrePaye(FournisseurStatus.ARCHIVE))
    }

    // --- Fiscalité par pays (spec §6.3) ---

    @Test
    fun `l identifiant fiscal depend du pays`() {
        assertEquals("NIU", FournisseurRules.identifiantFiscalRequis("CM"))
        assertEquals("NINEA", FournisseurRules.identifiantFiscalRequis("SN"))
        assertEquals("ICE", FournisseurRules.identifiantFiscalRequis("MA"))
        assertEquals("SIRET", FournisseurRules.identifiantFiscalRequis("FR"))
        assertEquals("TRN", FournisseurRules.identifiantFiscalRequis("AE"))
        assertNull(FournisseurRules.identifiantFiscalRequis("US"))
    }

    @Test
    fun `un particulier est dispense d identifiant fiscal`() {
        assertTrue(FournisseurRules.identifiantFiscalObligatoire("CM", TypeFournisseur.ENTREPRISE))
        assertFalse(FournisseurRules.identifiantFiscalObligatoire("CM", TypeFournisseur.PARTICULIER))
        assertFalse(FournisseurRules.identifiantFiscalObligatoire("US", TypeFournisseur.ENTREPRISE))
    }

    // --- Contrôles de soumission (spec §11) ---

    @Test
    fun `un dossier complet ne remonte aucun manquant`() {
        assertTrue(
            FournisseurRules.manquantsPourSoumission(base(statut = FournisseurStatus.BROUILLON), contactPrincipalPresent = true)
                .isEmpty(),
        )
    }

    @Test
    fun `les champs obligatoires sont detects a la soumission`() {
        val incomplet = base(statut = FournisseurStatus.BROUILLON).copy(
            identifiantFiscal = null,
            conditionsPaiement = null,
            adresse = null,
        )
        val manquants = FournisseurRules.manquantsPourSoumission(incomplet, contactPrincipalPresent = false)
        assertTrue(manquants.containsAll(listOf("adresse", "contact_principal", "identifiant_fiscal", "conditions_paiement")))
    }

    // --- Réapprobation (spec §3.2) ---

    @Test
    fun `une modification sensible renvoie un actif en validation`() {
        val actif = base()
        assertTrue(FournisseurRules.reapprobationRequise(actif, actif.copy(identifiantFiscal = "M9999999999")))
        assertTrue(FournisseurRules.reapprobationRequise(actif, actif.copy(nom = "Autre SARL")))
        assertTrue(FournisseurRules.reapprobationRequise(actif, actif.copy(pays = "SN")))
        assertTrue(FournisseurRules.reapprobationRequise(actif, actif.copy(conditionsPaiement = "Comptant")))
        assertFalse(FournisseurRules.reapprobationRequise(actif, actif.copy(description = "Nouvelle note")))
        assertFalse(
            FournisseurRules.reapprobationRequise(
                base(statut = FournisseurStatus.BROUILLON),
                base(statut = FournisseurStatus.BROUILLON).copy(nom = "Autre"),
            ),
        )
    }

    // --- Alertes documents (spec §6.9) ---

    @Test
    fun `les alertes de documents suivent les paliers`() {
        val now = 1_700_000_000_000L
        assertEquals(FournisseurRules.AlerteDocument.AUCUNE, FournisseurRules.alerteDocument(null, now))
        assertEquals(FournisseurRules.AlerteDocument.EXPIRE, FournisseurRules.alerteDocument(now - jour, now))
        assertEquals(FournisseurRules.AlerteDocument.FORTE, FournisseurRules.alerteDocument(now + 5 * jour, now))
        assertEquals(FournisseurRules.AlerteDocument.ALERTE, FournisseurRules.alerteDocument(now + 20 * jour, now))
        assertEquals(FournisseurRules.AlerteDocument.INFO, FournisseurRules.alerteDocument(now + 60 * jour, now))
        assertEquals(FournisseurRules.AlerteDocument.AUCUNE, FournisseurRules.alerteDocument(now + 200 * jour, now))
    }

    // --- Anti-doublon (spec §1) ---

    @Test
    fun `les motifs de doublon sont explicites`() {
        val candidat = base(nom = "Société Alpha", telephone = "+237600000000", identifiant = "M0123456789")
        val homonyme = base(nom = "société alpha ", telephone = "+237699999999", identifiant = "X1").copy(id = 1)
        val memeTel = base(nom = "Bêta SARL", telephone = "+237600000000", identifiant = "X2").copy(id = 2)
        val motifs = FournisseurRules.motifsDoublon(candidat, listOf(homonyme, memeTel))
        assertEquals(2, motifs.size)
        assertTrue(motifs[1L].orEmpty().contains("raison_sociale"))
        assertTrue(motifs[1L].orEmpty().contains("pays_raison_sociale"))
        assertTrue(motifs[2L].orEmpty().contains("telephone"))
        assertTrue(
            FournisseurRules.motifsDoublon(
                candidat,
                listOf(base(nom = "Gamma", telephone = "+999", identifiant = "X3").copy(id = 3)),
            ).isEmpty(),
        )
    }

    @Test
    fun `le blocage exige un motif`() {
        assertTrue(FournisseurRules.blocageValide("Fraude suspectée"))
        assertFalse(FournisseurRules.blocageValide(null))
        assertFalse(FournisseurRules.blocageValide("  "))
    }
}
