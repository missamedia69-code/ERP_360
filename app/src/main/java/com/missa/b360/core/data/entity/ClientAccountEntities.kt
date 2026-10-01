package com.missa.b360.core.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Nature d'une entrée du journal de suivi client (jamais supprimée). */
enum class FollowupType { RELANCE, APPEL, PROMESSE, NOTE }

/** Canal d'une relance ou d'un appel. */
enum class FollowupChannel { WHATSAPP, SMS, APPEL, EMAIL, VISITE, AUTRE }

/** Cycle d'une entrée de suivi ; `TENU`, `NON_TENU` et `CLOS` sont définitifs. */
enum class FollowupStatus { OUVERT, TENU, NON_TENU, CLOS }

/**
 * Situation de compte d'un client, lue par la liste, la fiche et le compte (une seule source).
 * Elle est **dérivée** des pièces de vente et des encaissements par `ClientBalanceUseCase` :
 * jamais saisie, toujours reconstructible.
 */
@Entity(
    tableName = "client_balances",
    foreignKeys = [ForeignKey(entity = ClientEntity::class, parentColumns = ["id"], childColumns = ["clientId"])],
    indices = [Index(value = ["encours"]), Index(value = ["enRetard"])],
)
data class ClientBalanceEntity(
    @PrimaryKey val clientId: Long,
    @ColumnInfo(defaultValue = "0") val encours: Double = 0.0,
    @ColumnInfo(defaultValue = "0") val enRetard: Double = 0.0,
    @ColumnInfo(defaultValue = "0") val joursRetardMax: Int = 0,
    @ColumnInfo(defaultValue = "0") val ca12Mois: Double = 0.0,
    val derniereVenteAt: Long? = null,
    @ColumnInfo(defaultValue = "0") val nbVentes: Int = 0,
    @ColumnInfo(defaultValue = "0") val majAt: Long = 0,
)

/**
 * Journal de suivi d'un client : relances, appels, promesses de paiement, notes.
 * Jamais supprimé ; une promesse change seulement de statut (OUVERT → TENU / NON_TENU / CLOS).
 */
@Entity(
    tableName = "client_followups",
    foreignKeys = [ForeignKey(entity = ClientEntity::class, parentColumns = ["id"], childColumns = ["clientId"])],
    indices = [Index(value = ["clientId", "createdAt"]), Index(value = ["type", "statut"])],
)
data class ClientFollowupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clientId: Long,
    val type: FollowupType,
    val canal: FollowupChannel? = null,
    val message: String? = null,
    val promesseDate: Long? = null,
    val promesseMontant: Double? = null,
    @ColumnInfo(defaultValue = "'OUVERT'") val statut: FollowupStatus = FollowupStatus.OUVERT,
    val createdAt: Long,
)

/**
 * Encaissement d'un client postérieur à la facture (la part payée à la vente reste dans la
 * pièce de vente). Un encaissement est imputé à une facture ([invoiceRecordId]) ou, à défaut,
 * aux plus anciennes échéances. On ne le supprime jamais : l'annulation est une ligne de montant
 * négatif qui référence l'original ([contrePassationDe]).
 */
@Entity(
    tableName = "client_payments",
    foreignKeys = [ForeignKey(entity = ClientEntity::class, parentColumns = ["id"], childColumns = ["clientId"])],
    indices = [
        Index(value = ["clientId", "paiementAt"]),
        Index(value = ["reference"], unique = true),
        Index(value = ["invoiceRecordId"]),
    ],
)
data class ClientPaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clientId: Long,
    val invoiceRecordId: Long? = null,
    /** Positif pour un encaissement, négatif pour sa contre-passation. */
    val montant: Double,
    val modePaiement: String,
    val reference: String,
    val paiementAt: Long,
    val contrePassationDe: Long? = null,
    val note: String? = null,
    val createdAt: Long,
)
