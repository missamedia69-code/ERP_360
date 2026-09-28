package com.missa.b360.core.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Types métier client définis par le référentiel maître Clients. */
enum class ClientType {
    PARTICULIER,
    ENTREPRISE,
    ADMINISTRATION,
    ONG,
    REVENDEUR,
    GROSSISTE,
    DISTRIBUTEUR,
    CLIENT_EXPORT,
    CLIENT_PROJET,
    PROSPECT,
}

/** Cycle de vie Client 360°. Les valeurs historiques ACTIF/DESACTIVE restent lisibles. */
enum class ClientStatus {
    BROUILLON, A_COMPLETER, ACTIF, SOUS_SURVEILLANCE,
    BLOQUE_CREDIT, BLOQUE_ADMINISTRATIF, INACTIF, ARCHIVE,
    /** Compatibilité avec les enregistrements antérieurs au cycle de vie complet. */
    DESACTIVE,
}

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
    /** Identifiant fiscal / NIF indiqué par le client. */
    val nif: String? = null,
    /** Commercial référent, saisi ou choisi selon l'organisation. */
    val commercial: String? = null,
    /** Délai de règlement convenu, en jours. */
    val conditionPaiementJours: Int = 30,
    val categorieId: Long? = null,
    /** Remise par défaut en % (pré-remplie à la vente, RC-06). */
    val remiseDefautPct: Double = 0.0,
    /** Limite de crédit dans la devise de l'entreprise (RC-05) ; null = illimitée. */
    val limiteCredit: Double? = null,
    val statut: ClientStatus = ClientStatus.BROUILLON,
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
    /** Type d'identifiant fiscal national (NIF, NIU, NINEA, TVA, autre). */
    val typeIdentifiantFiscal: String? = null,
    val numeroTva: String? = null,
    @ColumnInfo(defaultValue = "1") val assujettiTva: Boolean = true,
    @ColumnInfo(defaultValue = "0") val exonereTva: Boolean = false,
    val motifExoneration: String? = null,
    val tauxTva: Double? = null,
    /** Règles commerciales du client. */
    val grilleTarifaire: String? = null,
    val remiseMaxPct: Double? = null,
    val segment: String? = null,
    val canalVente: String? = null,
    val territoire: String? = null,
    val compteComptable: String? = null,
    val conditionsPaiement: String? = null,
    val createdAt: Long,
    val active: Boolean = false, // Un nouveau client reste en brouillon jusqu’à activation explicite
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


/** Contact rattaché au client. Le principal est choisi par l'utilisateur, sans donnée fictive. */
@Entity(
    tableName = "client_contacts",
    indices = [Index(value = ["clientId"])],
)
data class ClientContactEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clientId: Long,
    val nom: String,
    val fonction: String? = null,
    val telephone: String? = null,
    val email: String? = null,
    val principal: Boolean = false,
)

/** Une adresse de livraison ou de facturation rattachée au client. */
@Entity(
    tableName = "client_addresses",
    indices = [Index(value = ["clientId"])],
)
data class ClientAddressEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clientId: Long,
    val libelle: String = "",
    val adresse: String,
    val ville: String? = null,
    val principale: Boolean = false,
)
