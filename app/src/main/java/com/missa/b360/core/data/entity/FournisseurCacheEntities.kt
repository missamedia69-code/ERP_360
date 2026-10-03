package com.missa.b360.core.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.missa.b360.core.domain.model.PlanStatut

/**
 * Situation de compte d'un fournisseur : **cache dérivé** des pièces d'achat validées, jamais
 * saisi. La source de vérité reste les pièces ; `FournisseurBalanceRefresher` recalcule la ligne
 * dans la transaction qui modifie une pièce, et `reconstruireTout` peut la reconstruire entièrement.
 */
@Entity(
    tableName = "fournisseur_balances",
    foreignKeys = [ForeignKey(entity = FournisseurEntity::class, parentColumns = ["id"], childColumns = ["fournisseurId"])],
)
data class FournisseurBalanceEntity(
    @PrimaryKey val fournisseurId: Long,
    @ColumnInfo(defaultValue = "0") val dette: Double = 0.0,
    @ColumnInfo(defaultValue = "0") val enRetard: Double = 0.0,
    @ColumnInfo(defaultValue = "0") val joursRetardMax: Int = 0,
    @ColumnInfo(defaultValue = "0") val nbFacturesOuvertes: Int = 0,
    @ColumnInfo(defaultValue = "0") val nbCommandesOuvertes: Int = 0,
    @ColumnInfo(defaultValue = "0") val achats12Mois: Double = 0.0,
    val derniereFactureAt: Long? = null,
    val prochaineEcheanceAt: Long? = null,
    @ColumnInfo(defaultValue = "0") val majAt: Long = 0,
)

/**
 * Fiabilité **mesurée** d'un fournisseur (cache dérivé des réceptions, voir `SupplierScorecard`).
 * `score` vaut `null` sous trois commandes mesurées.
 */
@Entity(
    tableName = "fournisseur_scores",
    foreignKeys = [ForeignKey(entity = FournisseurEntity::class, parentColumns = ["id"], childColumns = ["fournisseurId"])],
)
data class FournisseurScoreEntity(
    @PrimaryKey val fournisseurId: Long,
    val ponctualite: Double? = null,
    val conformite: Double? = null,
    val prix: Double? = null,
    val score: Int? = null,
    @ColumnInfo(defaultValue = "0") val nbCommandesMesurees: Int = 0,
    /** Moyenne des jours entre la commande et sa réception complète. */
    val delaiMoyenReelJours: Double? = null,
    @ColumnInfo(defaultValue = "0") val majAt: Long = 0,
)

/**
 * Paiement prévu d'une facture fournisseur. Jamais supprimé : un report ajoute une ligne et
 * passe l'ancienne à `REPORTE`, une annulation passe à `ANNULE`.
 */
@Entity(
    tableName = "fournisseur_paiements_planifies",
    foreignKeys = [ForeignKey(entity = FournisseurEntity::class, parentColumns = ["id"], childColumns = ["fournisseurId"])],
    indices = [Index(value = ["fournisseurId", "datePrevue"]), Index(value = ["statut", "datePrevue"])],
)
data class FournisseurPaiementPlanifieEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fournisseurId: Long,
    val factureRecordId: Long,
    val datePrevue: Long,
    val montant: Double,
    @ColumnInfo(defaultValue = "'PLANIFIE'") val statut: PlanStatut = PlanStatut.PLANIFIE,
    val referenceMouvement: String? = null,
    val createdAt: Long,
)
