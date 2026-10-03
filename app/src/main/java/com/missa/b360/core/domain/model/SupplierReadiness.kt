package com.missa.b360.core.domain.model

import com.missa.b360.core.data.entity.FournisseurEntity
import com.missa.b360.core.data.entity.FournisseurStatus
import com.missa.b360.core.data.entity.VerificationStatut

enum class SupplierReadinessLevel { PRET, A_REGULARISER, BLOQUE }

enum class SupplierReadinessReason {
    STATUT_BLOQUE,
    STATUT_ARCHIVE,
    STATUT_NON_ACTIF,
    PAIEMENT_BLOQUE,
    DOSSIER_INCOMPLET,
    DOCUMENT_EXPIRE,
    DOCUMENT_REJETE,
    COMPTE_NON_VERIFIE,
    COMPTE_RECENT_NON_VERIFIE,
}

data class SupplierReadiness(
    val niveau: SupplierReadinessLevel,
    val motifs: List<SupplierReadinessReason>,
    val manquants: List<String>,
)

/** Vue minimale d'un document de conformité : seule la règle d'aptitude en a besoin. */
data class SupplierDocumentInfo(
    val dateExpiration: Long?,
    val verification: VerificationStatut,
    /** Un document retiré est archivé : il reste en base mais ne compte plus. */
    val archive: Boolean = false,
)

/** Vue minimale d'un compte de paiement (banque ou mobile money). */
data class SupplierAccountInfo(
    val verification: VerificationStatut,
    /** Dernière modification des coordonnées ; 0 = inconnue (donc ancienne). */
    val modifieLe: Long = 0L,
)

/**
 * Aptitude d'un fournisseur : un résumé d'affichage (Prêt / À régulariser / Bloqué).
 * Elle ne remplace pas [FournisseurRules.peutCommander], qui reste la seule autorité pour
 * autoriser une commande.
 */
object SupplierReadinessRules {

    const val DELAI_COMPTE_RECENT_JOURS = 7
    private const val JOUR_MS = 86_400_000L

    fun compteRecent(compte: SupplierAccountInfo, now: Long): Boolean =
        now - compte.modifieLe < DELAI_COMPTE_RECENT_JOURS * JOUR_MS && compte.modifieLe > 0L

    fun evaluer(
        f: FournisseurEntity,
        contactPrincipalPresent: Boolean,
        documents: List<SupplierDocumentInfo>,
        comptes: List<SupplierAccountInfo>,
        now: Long,
    ): SupplierReadiness {
        if (f.statut == FournisseurStatus.BLOQUE) {
            return SupplierReadiness(SupplierReadinessLevel.BLOQUE, listOf(SupplierReadinessReason.STATUT_BLOQUE), emptyList())
        }
        if (f.statut == FournisseurStatus.ARCHIVE) {
            return SupplierReadiness(SupplierReadinessLevel.BLOQUE, listOf(SupplierReadinessReason.STATUT_ARCHIVE), emptyList())
        }

        val motifs = mutableListOf<SupplierReadinessReason>()
        var manquants = emptyList<String>()

        if (f.statut != FournisseurStatus.ACTIF) motifs += SupplierReadinessReason.STATUT_NON_ACTIF
        if (f.paiementBloque) motifs += SupplierReadinessReason.PAIEMENT_BLOQUE

        if (f.statut == FournisseurStatus.BROUILLON || f.statut == FournisseurStatus.A_VALIDER) {
            manquants = FournisseurRules.manquantsPourSoumission(f, contactPrincipalPresent)
            if (manquants.isNotEmpty()) motifs += SupplierReadinessReason.DOSSIER_INCOMPLET
        }

        val actifs = documents.filterNot { it.archive }
        if (actifs.any { FournisseurRules.alerteDocument(it.dateExpiration, now) == FournisseurRules.AlerteDocument.EXPIRE }) {
            motifs += SupplierReadinessReason.DOCUMENT_EXPIRE
        }
        if (actifs.any { it.verification == VerificationStatut.REJETE }) {
            motifs += SupplierReadinessReason.DOCUMENT_REJETE
        }

        if (comptes.isNotEmpty() && comptes.none { it.verification == VerificationStatut.VERIFIE }) {
            motifs += SupplierReadinessReason.COMPTE_NON_VERIFIE
        }
        if (comptes.any { it.verification == VerificationStatut.A_VERIFIER && compteRecent(it, now) }) {
            motifs += SupplierReadinessReason.COMPTE_RECENT_NON_VERIFIE
        }

        val niveau = if (motifs.isEmpty()) SupplierReadinessLevel.PRET else SupplierReadinessLevel.A_REGULARISER
        return SupplierReadiness(niveau, motifs.toList(), manquants)
    }
}
