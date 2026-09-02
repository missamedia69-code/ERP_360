package com.missa.b360.core.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Statut fournisseur — « Désactivé » unique ; jamais de suppression physique (C7). */
enum class FournisseurStatus { ACTIF, DESACTIVE }

/**
 * Fournisseur — code `FRN-2026-0001` via SequenceManager ; téléphone indexé pour la
 * détection de doublons (RF-01) ; désactivation (C7), jamais de DELETE.
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
    val statut: FournisseurStatus = FournisseurStatus.ACTIF,
    val createdAt: Long,
)