package com.missa.b360.core.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Les 6 types de client (spec 9.9). */
enum class ClientType { PARTICULIER, ENTREPRISE, ADMINISTRATION, ONG, REVENDEUR, PROSPECT }

/** Statut client — « Désactivé » unique (RC-03) ; jamais de suppression physique. */
enum class ClientStatus { ACTIF, DESACTIVE }

/** Badge de fidélité — paramétrable, remise automatique à la vente (RC-16). */
@Entity(tableName = "loyalty_badges")
data class BadgeLoyaltyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nom: String,
    /** Remise automatique en % appliquée à la vente. */
    val remisePct: Double,
    val actif: Boolean = true,
)

/** Catégorie de client — suppression interdite si rattachée à un client (RC). */
@Entity(tableName = "client_categories")
data class CategoryClientEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nom: String,
)

/**
 * Client — code `CLI-2026-0001` unique via SequenceManager ; téléphone indexé
 * pour la détection de doublons (RC-01) ; solde dérivé (RC-08, jamais saisi).
 */
@Entity(
    tableName = "clients",
    indices = [
        Index(value = ["code"], unique = true),
        Index(value = ["telephone"]),
        Index(value = ["nom"]),
    ],
)
data class ClientEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val code: String,
    val nom: String,
    val type: ClientType = ClientType.PARTICULIER,
    val telephone: String,
    val telephone2: String? = null,
    val email: String? = null,
    val adresse: String? = null,
    val categorieId: Long? = null,
    /** Remise par défaut en % (pré-remplie à la vente, RC-06). */
    val remiseDefautPct: Double = 0.0,
    /** Limite de crédit dans la devise de l'entreprise (RC-05) ; null = illimitée. */
    val limiteCredit: Double? = null,
    val statut: ClientStatus = ClientStatus.ACTIF,
    val badgeId: Long? = null,
    /** Prospect auto-converti à la 1re vente (RC-02). */
    val prospect: Boolean = false,
    /** Photo locale (PI jamais dans le cloud). */
    val photoPath: String? = null,
    val notes: String? = null,
    /** Site de rattachement (multi-site). */
    val siteId: Long? = null,
    /** Référence croisée client ↔ fournisseur (champ libre). */
    val codeFournisseur: String? = null,
    val createdAt: Long,
    val active: Boolean = true, // C7 : désactivation, jamais de DELETE
)

/** Prix spécifique client × produit (RC-07) — consommé par 9.6 Vente (RV-17). */
@Entity(
    tableName = "client_prices",
    indices = [Index(value = ["clientId", "produitId"], unique = true)],
)
data class PriceClientEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clientId: Long,
    val produitId: Long,
    val prix: Double,
)
