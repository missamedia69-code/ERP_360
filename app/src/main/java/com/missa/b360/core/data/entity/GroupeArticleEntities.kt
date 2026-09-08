package com.missa.b360.core.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Groupe d'articles — cœur transverse du référentiel.
 *
 * Un groupe porte les règles communes à une famille d'articles : est-elle
 * stockée, valorisée, vendable, maintenable… Jusqu'ici ces règles étaient
 * codées en dur dans `ProductType`, ce qui obligeait à modifier l'application
 * pour créer une nouvelle famille. Elles deviennent des données.
 *
 * Les caractéristiques propres à chaque métier vivent dans des tables
 * d'extension séparées ([GroupeStockEntity], [GroupeAchatEntity]…). Cette
 * séparation coûte une jointure, mais permet d'ajouter un module sans toucher
 * à la table centrale ni migrer les groupes existants.
 */
@Entity(
    tableName = "item_groups",
    indices = [Index(value = ["code"], unique = true)],
)
data class GroupeArticleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val code: String,
    val nom: String,
    val description: String? = null,
    val actif: Boolean = true,
    /** L'article de ce groupe tient-il un stock physique ? */
    val stocke: Boolean = true,
    /** Le stock de ce groupe entre-t-il dans la valorisation comptable ? */
    val valorise: Boolean = true,
    /** Le groupe désigne-t-il une immobilisation plutôt qu'un consommable ? */
    val immobilisation: Boolean = false,
    /** Nom stable de [MethodeValorisation] ; null = méthode de l'entité. */
    val methodeValorisation: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

/** Méthodes de valorisation du stock. */
enum class MethodeValorisation { CUMP, FIFO, STANDARD }

/** Extension « stock » d'un groupe d'articles. */
@Entity(
    tableName = "item_groups_stock",
    indices = [Index(value = ["itemGroupId"], unique = true)],
    foreignKeys = [
        ForeignKey(
            entity = GroupeArticleEntity::class,
            parentColumns = ["id"],
            childColumns = ["itemGroupId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class GroupeStockEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemGroupId: Long,
    /** Compte de stock du plan comptable en vigueur (SYSCOHADA, PCG…). */
    val compteStock: String? = null,
    val compteEcartInventaire: String? = null,
    val gestionLotObligatoire: Boolean = false,
    val gestionSerieObligatoire: Boolean = false,
    val gestionPeremptionObligatoire: Boolean = false,
    /** Autoriser un solde négatif : à réserver aux cas de saisie différée. */
    val stockNegatifAutorise: Boolean = false,
    val seuilReapproDefaut: Double = 0.0,
)

/** Extension « achats » d'un groupe d'articles. */
@Entity(
    tableName = "item_groups_purchase",
    indices = [Index(value = ["itemGroupId"], unique = true)],
    foreignKeys = [
        ForeignKey(
            entity = GroupeArticleEntity::class,
            parentColumns = ["id"],
            childColumns = ["itemGroupId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class GroupeAchatEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemGroupId: Long,
    val compteCharge: String? = null,
    val compteImmobilisation: String? = null,
    val achetable: Boolean = true,
    val consommable: Boolean = false,
    /** L'achat s'immobilise-t-il au lieu de passer en charge ? */
    val immobilisable: Boolean = false,
    val delaiLivraisonJours: Int = 0,
)

/** Extension « ventes » d'un groupe d'articles. */
@Entity(
    tableName = "item_groups_sales",
    indices = [Index(value = ["itemGroupId"], unique = true)],
    foreignKeys = [
        ForeignKey(
            entity = GroupeArticleEntity::class,
            parentColumns = ["id"],
            childColumns = ["itemGroupId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class GroupeVenteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemGroupId: Long,
    val compteProduit: String? = null,
    val vendable: Boolean = true,
    val service: Boolean = false,
    val soumisTaxe: Boolean = true,
    val livraisonRequise: Boolean = true,
    val garantieApplicable: Boolean = false,
    val garantieMois: Int = 0,
)

/** Extension « production » d'un groupe d'articles. */
@Entity(
    tableName = "item_groups_production",
    indices = [Index(value = ["itemGroupId"], unique = true)],
    foreignKeys = [
        ForeignKey(
            entity = GroupeArticleEntity::class,
            parentColumns = ["id"],
            childColumns = ["itemGroupId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class GroupeProductionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemGroupId: Long,
    val produisible: Boolean = false,
    /** Article fantôme : consommé sans jamais être stocké. */
    val fantome: Boolean = false,
    val coProduit: Boolean = false,
    val sousProduit: Boolean = false,
    val gammeRequise: Boolean = false,
    val nomenclatureRequise: Boolean = false,
    val soustraitable: Boolean = false,
)

/** Extension « maintenance » d'un groupe d'articles. */
@Entity(
    tableName = "item_groups_maintenance",
    indices = [Index(value = ["itemGroupId"], unique = true)],
    foreignKeys = [
        ForeignKey(
            entity = GroupeArticleEntity::class,
            parentColumns = ["id"],
            childColumns = ["itemGroupId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class GroupeMaintenanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemGroupId: Long,
    val equipementMaintenable: Boolean = false,
    val planRequis: Boolean = false,
    val suiviHeures: Boolean = false,
    val suiviCompteur: Boolean = false,
    val critiqueSecurite: Boolean = false,
    val etalonnageRequis: Boolean = false,
    /** Temps moyen entre pannes, en heures ; 0 = inconnu. */
    val mtbfHeures: Double = 0.0,
)

/** Extension « comptabilité » d'un groupe d'articles. */
@Entity(
    tableName = "item_groups_accounting",
    indices = [Index(value = ["itemGroupId"], unique = true)],
    foreignKeys = [
        ForeignKey(
            entity = GroupeArticleEntity::class,
            parentColumns = ["id"],
            childColumns = ["itemGroupId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class GroupeComptabiliteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemGroupId: Long,
    val categorieTaxe: String? = null,
    val amortissable: Boolean = false,
    val methodeAmortissement: String? = null,
    val dureeAmortissementAnnees: Int = 0,
    val stockValorise: Boolean = true,
    val centreCout: String? = null,
    val centreProfit: String? = null,
)
