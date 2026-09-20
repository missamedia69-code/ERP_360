package com.missa.b360.core.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Cycle de vie fournisseur (spécification module Fournisseurs §3) :
 * BROUILLON → A_VALIDER → ACTIF → SUSPENDU / BLOQUE → ARCHIVE.
 * Jamais de suppression physique (C7) — l'archivage tient lieu de fin de vie.
 */
enum class FournisseurStatus { BROUILLON, A_VALIDER, ACTIF, SUSPENDU, BLOQUE, ARCHIVE }

/** Type de fournisseur — pilote les sections affichées dans le formulaire. */
enum class TypeFournisseur {
    ENTREPRISE,
    PARTICULIER,
    PRESTATAIRE,
    ARTISAN,
    TRANSPORTEUR,
    SOUS_TRAITANT,
    FOURNISSEUR_EQUIPEMENT,
    FOURNISSEUR_MATIERES,
    FOURNISSEUR_SERVICES,
    COLLECTEUR_DECHETS,
    AUTRE,
}

/** Types de documents rattachables à un fournisseur (contrats, attestations, certificats…). */
enum class FournisseurDocType {
    CONTRAT,
    CONVENTION,
    BON_COMMANDE_TYPE,
    DEVIS,
    ATTESTATION_FISCALE,
    IDENTIFIANT_FISCAL,
    RCCM,
    RIB,
    CERTIFICAT_QUALITE,
    ASSURANCE,
    FICHE_SECURITE,
    CERTIFICAT_ORIGINE,
    LICENCE,
    AUTRE,
}

/** Statut de vérification (compte bancaire, document). */
enum class VerificationStatut { A_VERIFIER, VERIFIE, REJETE }

/** Actions tracées dans le journal d'audit fournisseur. */
enum class FournisseurEvenementType {
    CREATION,
    MISE_A_JOUR,
    SOUMISSION,
    APPROBATION,
    SUSPENSION,
    BLOCAGE,
    REACTIVATION,
    ARCHIVAGE,
    COMPTE_AJOUTE,
    COMPTE_VERIFIE,
    COMPTE_REJETE,
    DOCUMENT_AJOUTE,
    DOCUMENT_SUPPRIME,
    ARTICLE_LIE,
    ARTICLE_DELIE,
    EVALUATION,
    REAPPROBATION_REQUISE,
}

/**
 * Fournisseur — fiche maître unique du cycle d'achat. Code `FRN-2026-0001` via
 * SequenceManager ; anti-doublon étendu (raison sociale, identifiant fiscal, RCCM,
 * téléphone, e-mail, pays) ; jamais de DELETE (C7).
 *
 * Les colonnes ont toutes une valeur par défaut : la migration 16→17 les ajoute par
 * `ALTER TABLE ADD COLUMN` sans réécriture de la table.
 */
@Entity(
    tableName = "fournisseurs",
    indices = [
        Index(value = ["code"], unique = true),
        Index(value = ["telephone"]),
        Index(value = ["nom"]),
    ],
)
data class FournisseurEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val code: String,
    val nom: String,
    val telephone: String,
    val telephone2: String? = null,
    val email: String? = null,
    val adresse: String? = null,
    val siteId: Long? = null,
    val notes: String? = null,
    val statut: FournisseurStatus = FournisseurStatus.BROUILLON,
    val createdAt: Long,
    // --- Général (spec §6.1) ---
    val type: TypeFournisseur = TypeFournisseur.ENTREPRISE,
    val nomCommercial: String? = null,
    val pays: String = "CM",
    val devise: String = "XAF",
    val langue: String? = null,
    val siteWeb: String? = null,
    val description: String? = null,
    val motifBlocage: String? = null,
    val soumisLe: Long? = null,
    val approuveLe: Long? = null,
    // --- Fiscalité et conformité (spec §6.3) ---
    val typeIdentifiantFiscal: String? = null,
    val identifiantFiscal: String? = null,
    val rccm: String? = null,
    val numTva: String? = null,
    val assujettiTva: Boolean = true,
    val tauxRetenue: Double = 0.0,
    val exonere: Boolean = false,
    val dateValidationFiscale: Long? = null,
    // --- Paiement et banque (spec §6.5) ---
    val conditionsPaiement: String? = null,
    val joursEcheance: Int = 0,
    val modePaiementPrefere: String? = null,
    val paiementBloque: Boolean = false,
    /** Plafond de paiement sans validation supplémentaire ; 0 = pas de plafond. */
    val plafondPaiement: Double = 0.0,
    // --- Achats et approvisionnement (spec §6.4) ---
    val approuve: Boolean = false,
    val delaiMoyenJours: Int = 0,
    val quantiteMinCommande: Double = 0.0,
    val montantMinCommande: Double = 0.0,
    /** Noms de groupes d'articles fournis, séparés par des virgules. */
    val categoriesFournies: String? = null,
    val incoterm: String? = null,
    val depotLivraisonId: Long? = null,
    // --- Évaluation manuelle (score simple) ---
    val noteEvaluation: Double? = null,
    val commentaireEvaluation: String? = null,
    val dateEvaluation: Long? = null,
    val updatedAt: Long = 0,
)

/** Contact fournisseur (spec §6.2) — plusieurs rôles possibles par contact. */
@Entity(
    tableName = "fournisseur_contacts",
    indices = [Index(value = ["fournisseurId"])],
)
data class FournisseurContactEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fournisseurId: Long,
    val nom: String,
    val prenom: String? = null,
    val fonction: String? = null,
    val service: String? = null,
    val telephone: String? = null,
    val whatsapp: String? = null,
    val email: String? = null,
    val principal: Boolean = false,
    val roleAchats: Boolean = false,
    val roleCompta: Boolean = false,
    val roleLivraison: Boolean = false,
    val roleUrgence: Boolean = false,
    val actif: Boolean = true,
)

/**
 * Compte bancaire ou Mobile Money (spec §6.5). Le compte principal est unique par
 * fournisseur ; un compte non vérifié ne doit pas recevoir de paiement quand le
 * contrôle est actif — vérification tracée dans le journal d'audit.
 */
@Entity(
    tableName = "fournisseur_comptes_bancaires",
    indices = [Index(value = ["fournisseurId"])],
)
data class FournisseurCompteBancaireEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fournisseurId: Long,
    val titulaire: String,
    val banque: String? = null,
    val paysBanque: String? = null,
    val numeroCompte: String? = null,
    val iban: String? = null,
    val bicSwift: String? = null,
    val operateurMobile: String? = null,
    val numeroMobile: String? = null,
    val principal: Boolean = false,
    val verification: VerificationStatut = VerificationStatut.A_VERIFIER,
    val verifieLe: Long? = null,
    val notes: String? = null,
)

/** Document de conformité (spec §6.9) — échéance suivie : J-90 / J-30 / J-7 / expiré. */
@Entity(
    tableName = "fournisseur_documents",
    indices = [Index(value = ["fournisseurId"])],
)
data class FournisseurDocumentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fournisseurId: Long,
    val typeDocument: FournisseurDocType = FournisseurDocType.AUTRE,
    val reference: String? = null,
    /** Chemin interne (fichiers compressés via PieceJointeAchat). */
    val cheminFichier: String? = null,
    val dateEmission: Long? = null,
    val dateExpiration: Long? = null,
    val verification: VerificationStatut = VerificationStatut.A_VERIFIER,
    val notes: String? = null,
)

/**
 * Liaison fournisseur ↔ article Stock (spec §6.4) : référence fournisseur, prix,
 * délai, quantité minimum, fournisseur préféré et période de validité du prix.
 */
@Entity(
    tableName = "fournisseur_items",
    indices = [Index(value = ["fournisseurId", "productId"], unique = true)],
)
data class FournisseurItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fournisseurId: Long,
    val productId: Long,
    val reference: String? = null,
    val prixUnitaire: Double = 0.0,
    val delaiJours: Int = 0,
    val quantiteMin: Double = 0.0,
    val prefere: Boolean = false,
    val debutValidite: Long? = null,
    val finValidite: Long? = null,
    val actif: Boolean = true,
)

/** Piste d'audit fournisseur (spec §1 et §6.10) — journal append-only. */
@Entity(
    tableName = "fournisseur_evenements",
    indices = [Index(value = ["fournisseurId"])],
)
data class FournisseurEvenementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fournisseurId: Long,
    val date: Long,
    val type: FournisseurEvenementType,
    val details: String? = null,
)
