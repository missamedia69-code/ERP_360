package com.missa.b360.core.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Statut de licence (RA-04..06) : essai 7 j → active → expirée (lecture seule). */
enum class LicenceStatus { TRIAL, ACTIVE, EXPIRED }

/**
 * Entité Entreprise — table `enterprise`, id=1 unique.
 * Devise (ISO 4217) + langue obligatoires ; verrouillées au premier usage (RA-19).
 */
@Entity(tableName = "enterprise")
data class EnterpriseEntity(
    @PrimaryKey val id: Long = 1L,
    val nom: String,
    val secteur: String? = null,
    val adresse: String? = null,
    val telephone: String? = null,
    val email: String? = null,
    /** Devise ISO 4217 (défaut USD — D4), verrouillée au 1er usage. */
    val devise: String,
    /** Langue de l'interface : fr, en, es, ar, zh. */
    val langue: String,
    /** Pays suggérant taxes + référentiels comptable/paie. */
    val pays: String? = null,
    /** URI du logo choisi durant l'onboarding, avec droit de lecture persistant. */
    val logoUri: String? = null,
    /** Profil d'activité A–H (RA-20, modifiable). */
    val profilActivite: String? = null,
    /** Palier de taille P1–P6. */
    val palierTaille: String? = null,
    /** True une fois l'onboarding complet terminé. */
    val onboardingTermine: Boolean = false,
)

/** Site physique (multi-site : ≥ 1 site obligatoire). */
@Entity(tableName = "sites")
data class SiteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nom: String,
    /** boutique / entrepôt / usine / bureau… */
    val type: String,
    val adresse: String? = null,
    val principal: Boolean = false,
)

/** Utilisateur — email de secours unique obligatoire (RA-03). PIN jamais stocké en clair (RA-01). */
@Entity(
    tableName = "users",
    indices = [Index(value = ["emailSecours"], unique = true)],
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nom: String,
    val emailSecours: String,
    /** Hash PBKDF2 du PIN (format salt:hash). Jamais le PIN en clair. */
    val pinHash: String? = null,
    val roleId: Long? = null,
    val actif: Boolean = true,
    val createdAt: Long,
)

/** Rôle — 3 rôles SYSTEM (Propriétaire, Gérant, Consultation seule) + CUSTOM. */
@Entity(tableName = "roles")
data class RoleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nom: String,
    /** SYSTEM ou CUSTOM. */
    val type: String,
    /** Modèle pré-rempli : Caissier, Magasinier, Comptable, RH, Livreur, Chef de production, Chargé de projet. */
    val modele: String? = null,
)

/** Permission d'un rôle : Voir/Créer/Modifier/Supprimer/Valider × module (RA-14/24/25). */
@Entity(
    tableName = "role_permissions",
    primaryKeys = ["roleId", "module", "action"],
)
data class RolePermissionEntity(
    val roleId: Long,
    val module: String,
    /** VIEW, CREATE, EDIT, DELETE, VALIDATE. */
    val action: String,
    val granted: Boolean,
)

/** Licence — id=1 ; essai 7 j → code 1 appareil/1 an (D3). */
@Entity(tableName = "licence")
data class LicenceEntity(
    @PrimaryKey val id: Long = 1L,
    val statut: LicenceStatus = LicenceStatus.TRIAL,
    /** Début de l'essai (7 jours). */
    val dateDebutEssai: Long,
    val dateActivation: Long? = null,
    val dateExpiration: Long? = null,
    val code: String? = null,
    /** Identifiant appareil lié (1 code = 1 appareil). */
    val appareilId: String? = null,
    /** Désassociations d'appareil utilisées (max 3/an). */
    val desassociationsUtilisees: Int = 0,
)

/**
 * Séquence de numérotation — clé composite (type de document, année) (RA-09 / RM-40).
 * 9 documents de base + 9 séquences dédiées ; incrément transactionnel, aucun numéro réutilisé.
 */
@Entity(tableName = "sequences", primaryKeys = ["docType", "annee"])
data class SequenceEntity(
    val docType: String,
    val annee: Int,
    val compteur: Long = 0,
)

/** Taxe — figée au premier usage (verrou d'amont, RA-19). */
@Entity(tableName = "taxes")
data class TaxEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nom: String,
    /** Taux en % (ex. 19.25 pour le Cameroun). */
    val taux: Double,
    val parDefaut: Boolean = false,
)

/** Mode de paiement — figé au premier usage ; Mobile Money = moyen générique unique (D13). */
@Entity(tableName = "payment_methods")
data class PaymentMethodEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nom: String,
    val actif: Boolean = true,
)

/** Réglage clé/valeur avec verrou `locked` (refus d'écriture si verrouillé — RA-19). */
@Entity(tableName = "settings")
data class SettingEntity(
    @PrimaryKey val cle: String,
    val valeur: String,
    val locked: Boolean = false,
)

/** Historique de sauvegarde. */
@Entity(tableName = "backups")
data class BackupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,
    /** MANUAL / AUTO. */
    val type: String,
    val chemin: String,
    /** LOCAL / DRIVE / ICLOUD. */
    val plateforme: String,
)

/**
 * Entrée de journal — immuable (aucune UPDATE/DELETE métier, RA-18), purge 12 mois.
 */
@Entity(
    tableName = "journal",
    indices = [Index(value = ["horodatage"])],
)
data class JournalEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val horodatage: Long,
    val userId: Long? = null,
    val module: String,
    val action: String,
    val details: String,
)

/** Notification locale (RA-23) — lue/non lue, badge recalculé après lecture. */
@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    @ColumnInfo(name = "titre") val titre: String,
    val message: String,
    val date: Long,
    val lue: Boolean = false,
)
