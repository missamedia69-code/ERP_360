package com.missa.b360.core.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Pièce opérationnelle générique des modules livrés après le socle.
 * Une pièce n'est jamais supprimée : son statut peut évoluer vers ANNULÉ, ce qui respecte
 * la convention de compensation métier et préserve la piste d'audit.
 */
@Entity(
    tableName = "operation_records",
    indices = [
        Index(value = ["module"]),
        Index(value = ["createdAt"]),
        Index(value = ["reference"], unique = true),
    ],
)
data class OperationRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Nom stable de [OperationModule]. */
    val module: String,
    /** Référence atomique obtenue via [com.missa.b360.core.numbering.SequenceManager]. */
    val reference: String,
    /** Libellé saisi par l'utilisateur. */
    val title: String,
    /** Client, fournisseur, collaborateur ou tiers libre selon le module. */
    val counterpart: String? = null,
    /** Montant hors formatage, dans la devise verrouillée de l'entreprise. */
    val amount: Double? = null,
    /** Quantité concernée (stock, production, service, livraison…). */
    val quantity: Double? = null,
    /** NONE / IN / OUT — utilisé pour le solde de trésorerie. */
    val direction: String = OperationDirection.NONE.name,
    /** DRAFT / VALIDATED / CANCELLED. */
    val status: String = OperationStatus.DRAFT.name,
    val notes: String? = null,
    val createdAt: Long,
)

/** Modules qui s'appuient sur les pièces opérationnelles du MVP. */
enum class OperationModule {
    STOCK,
    VENTE,
    ACHATS,
    FINANCES,
    LIVRAISON,
    PRODUCTION,
    SERVICES,
    RH,
    PROJETS,
}

enum class OperationStatus { DRAFT, VALIDATED, CANCELLED }
enum class OperationDirection { NONE, IN, OUT }
