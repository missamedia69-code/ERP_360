package com.missa.b360.core.domain.model

import com.missa.b360.core.data.entity.FournisseurEntity
import com.missa.b360.core.data.entity.FournisseurStatus
import com.missa.b360.core.data.entity.TypeFournisseur

/**
 * Règles pures du référentiel fournisseur (spécification module Fournisseurs) :
 * cycle de vie, droits par statut, exigences fiscales par pays, réapprobation des
 * modifications sensibles, alertes de documents et contrôles de soumission.
 *
 * Aucune dépendance Android/Room — testable en JVM.
 */
object FournisseurRules {

    private const val JOUR_MS = 24L * 60 * 60 * 1000

    /** Transitions autorisées du cycle de vie (spec §3). */
    fun transitions(de: FournisseurStatus): Set<FournisseurStatus> = when (de) {
        FournisseurStatus.BROUILLON -> setOf(FournisseurStatus.A_VALIDER, FournisseurStatus.ARCHIVE)
        FournisseurStatus.A_VALIDER ->
            setOf(FournisseurStatus.ACTIF, FournisseurStatus.BROUILLON, FournisseurStatus.ARCHIVE)
        FournisseurStatus.ACTIF -> setOf(FournisseurStatus.SUSPENDU, FournisseurStatus.BLOQUE, FournisseurStatus.ARCHIVE)
        FournisseurStatus.SUSPENDU ->
            setOf(FournisseurStatus.ACTIF, FournisseurStatus.BLOQUE, FournisseurStatus.ARCHIVE)
        FournisseurStatus.BLOQUE -> setOf(FournisseurStatus.SUSPENDU, FournisseurStatus.ARCHIVE)
        FournisseurStatus.ARCHIVE -> emptySet()
    }

    fun transitionAutorisee(de: FournisseurStatus, vers: FournisseurStatus): Boolean =
        vers in transitions(de)

    /** Un fournisseur ne peut recevoir une commande que s'il est ACTIF (approuvé). */
    fun peutCommander(statut: FournisseurStatus): Boolean = statut == FournisseurStatus.ACTIF

    /**
     * Paiement : ACTIF normalement ; SUSPENDU/BLOQUE uniquement pour honorer des
     * factures déjà validées (spec §3.1). BROUILLON/A_VALIDER/ARCHIVE : jamais.
     */
    fun peutEtrePaye(statut: FournisseurStatus): Boolean = statut in setOf(
        FournisseurStatus.ACTIF,
        FournisseurStatus.SUSPENDU,
        FournisseurStatus.BLOQUE,
    )

    /**
     * Identifiant fiscal exigé par pays (spec §6.3) — null si le pays n'impose rien
     * de connu. Les particuliers en sont dispensés.
     */
    fun identifiantFiscalRequis(pays: String): String? = when (pays.uppercase()) {
        "CM" -> "NIU"
        "SN" -> "NINEA"
        "MA" -> "ICE"
        "CI", "GA", "GN" -> "NIF"
        "FR" -> "SIRET"
        "AE" -> "TRN"
        "NG" -> "TIN"
        "KE" -> "PIN"
        "GH" -> "TIN"
        else -> null
    }

    fun identifiantFiscalObligatoire(pays: String, type: TypeFournisseur): Boolean =
        type != TypeFournisseur.PARTICULIER && identifiantFiscalRequis(pays) != null

    /**
     * Champs manquants pour la soumission à validation (spec §11). Retourne des
     * libellés stables que l'UI traduit ; liste vide = dossier complet.
     */
    fun manquantsPourSoumission(
        fournisseur: FournisseurEntity,
        contactPrincipalPresent: Boolean,
    ): List<String> = buildList {
        if (fournisseur.nom.isBlank()) add("raison_sociale")
        if (fournisseur.adresse.isNullOrBlank()) add("adresse")
        if (fournisseur.telephone.isBlank() && fournisseur.email.isNullOrBlank()) add("contact_tel_ou_email")
        if (!contactPrincipalPresent) add("contact_principal")
        if (identifiantFiscalObligatoire(fournisseur.pays, fournisseur.type) &&
            fournisseur.identifiantFiscal.isNullOrBlank()
        ) {
            add("identifiant_fiscal")
        }
        if (fournisseur.conditionsPaiement.isNullOrBlank()) add("conditions_paiement")
        if (fournisseur.categoriesFournies.isNullOrBlank()) add("categories_fournies")
    }

    /**
     * Modification sensible (spec §3.2) : toute évolution d'un fournisseur ACTIF sur
     * ces champs impose un retour en A_VALIDER (réapprobation).
     */
    fun reapprobationRequise(ancien: FournisseurEntity, nouveau: FournisseurEntity): Boolean {
        if (ancien.statut != FournisseurStatus.ACTIF) return false
        return ancien.identifiantFiscal != nouveau.identifiantFiscal ||
            ancien.nom.trim().equals(nouveau.nom.trim(), ignoreCase = true).not() ||
            ancien.pays != nouveau.pays ||
            ancien.conditionsPaiement != nouveau.conditionsPaiement ||
            ancien.modePaiementPrefere != nouveau.modePaiementPrefere ||
            ancien.joursEcheance != nouveau.joursEcheance
    }

    /** Gravité d'alerte d'un document selon le délai restant (spec §6.9). */
    enum class AlerteDocument { AUCUNE, INFO, ALERTE, FORTE, EXPIRE }

    fun alerteDocument(dateExpiration: Long?, now: Long): AlerteDocument {
        if (dateExpiration == null) return AlerteDocument.AUCUNE
        val jours = (dateExpiration - now) / JOUR_MS
        return when {
            jours < 0 -> AlerteDocument.EXPIRE
            jours <= 7 -> AlerteDocument.FORTE
            jours <= 30 -> AlerteDocument.ALERTE
            jours <= 90 -> AlerteDocument.INFO
            else -> AlerteDocument.AUCUNE
        }
    }

    /** Motifs de doublon potentiels entre une saisie et les fiches existantes. */
    fun motifsDoublon(
        candidat: FournisseurEntity,
        existants: List<FournisseurEntity>,
    ): Map<Long, List<String>> = existants.mapNotNull { existant ->
        val motifs = buildList {
            if (existant.nom.trim().equals(candidat.nom.trim(), ignoreCase = true)) add("raison_sociale")
            if (candidat.telephone.isNotBlank() && existant.telephone == candidat.telephone) add("telephone")
            val email = candidat.email?.takeIf { it.isNotBlank() }
            if (email != null && existant.email?.equals(email, ignoreCase = true) == true) add("email")
            val fiscal = candidat.identifiantFiscal?.takeIf { it.isNotBlank() }
            if (fiscal != null && existant.identifiantFiscal == fiscal) add("identifiant_fiscal")
            val rccm = candidat.rccm?.takeIf { it.isNotBlank() }
            if (rccm != null && existant.rccm == rccm) add("rccm")
            if (existant.pays == candidat.pays &&
                existant.nom.trim().equals(candidat.nom.trim(), ignoreCase = true)
            ) {
                add("pays_raison_sociale")
            }
        }
        if (motifs.isEmpty()) null else existant.id to motifs.distinct()
    }.toMap()

    /** Le blocage exige toujours un motif (spec §11). */
    fun blocageValide(motif: String?): Boolean = !motif.isNullOrBlank()
}
