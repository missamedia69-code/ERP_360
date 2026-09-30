package com.missa.b360.core.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Appareil autorisé localement ; la clé publique sera échangée lors du futur appairage. */
@Entity(tableName = "sync_devices", indices = [Index(value = ["deviceId"], unique = true)])
data class SyncDeviceEntity(
    @PrimaryKey val deviceId: String,
    val nom: String,
    @ColumnInfo(defaultValue = "''") val publicKey: String = "",
    @ColumnInfo(defaultValue = "1") val actif: Boolean = true,
    @ColumnInfo(defaultValue = "0") val creeLe: Long,
    @ColumnInfo(defaultValue = "0") val vuLe: Long,
)

/**
 * Événement local immuable à remettre au futur relais. L'identifiant UUID rend
 * les reprises et doubles livraisons idempotentes.
 */
@Entity(
    tableName = "sync_outbox",
    indices = [Index("statut"), Index("prochaineTentative"), Index(value = ["aggregateType", "aggregateId"])],
)
data class SyncOutboxEntity(
    @PrimaryKey val eventId: String,
    val deviceId: String,
    val entrepriseId: String,
    val aggregateType: String,
    val aggregateId: String,
    val operation: String,
    /** JSON canonique local ; il sera chiffré avant toute sortie de l'appareil. */
    val payload: String,
    @ColumnInfo(defaultValue = "0") val revision: Long,
    @ColumnInfo(defaultValue = "0") val creeLe: Long,
    @ColumnInfo(defaultValue = "'EN_ATTENTE'") val statut: String = SyncStatut.EN_ATTENTE.name,
    @ColumnInfo(defaultValue = "0") val tentatives: Int = 0,
    @ColumnInfo(defaultValue = "0") val prochaineTentative: Long = 0,
    val derniereErreur: String? = null,
)

/** Accusé de réception local empêchant d'appliquer deux fois un événement distant. */
@Entity(tableName = "sync_inbox", indices = [Index("recuLe")])
data class SyncInboxEntity(
    @PrimaryKey val eventId: String,
    val deviceIdSource: String,
    val aggregateType: String,
    val aggregateId: String,
    @ColumnInfo(defaultValue = "0") val revision: Long,
    @ColumnInfo(defaultValue = "0") val recuLe: Long,
    val appliqueLe: Long? = null,
)

/** Conflit conservé sans perte jusqu'à résolution explicite. */
@Entity(tableName = "sync_conflicts", indices = [Index("resolu"), Index(value = ["aggregateType", "aggregateId"])])
data class SyncConflictEntity(
    @PrimaryKey val conflictId: String,
    val eventIdDistant: String,
    val aggregateType: String,
    val aggregateId: String,
    @ColumnInfo(defaultValue = "0") val revisionLocale: Long,
    @ColumnInfo(defaultValue = "0") val revisionDistante: Long,
    val payloadLocal: String,
    val payloadDistant: String,
    @ColumnInfo(defaultValue = "0") val detecteLe: Long,
    @ColumnInfo(defaultValue = "0") val resolu: Boolean = false,
    val resolution: String? = null,
    val resoluLe: Long? = null,
)

enum class SyncStatut { EN_ATTENTE, EN_COURS, LIVRE, ECHEC }
enum class SyncOperation { CREER, MODIFIER, ARCHIVER, CONTRE_PASSER }
