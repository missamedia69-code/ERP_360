package com.missa.b360.core.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Le type du produit détermine les sections affichées du formulaire (spec §7). */
enum class ProductType {
    /** Produit acheté puis revendu (commerce). */
    ACHATE_REVENDU,
    /** Matière première consommée par la production. */
    MATIERE_PREMIERE,
    /** Produit fini issu d'un ordre de production. */
    FABRIQUE,
    /** Produit assemblé à partir de composants. */
    COMPOSE,
    /** Consommable non revendable. */
    CONNOMMABLE,
}

/** Statut produit — « Désactivé » unique ; jamais de suppression physique (C7). */
enum class ProductStatus { ACTIF, DESACTIVE }

/**
 * Catégorie de produit (spec §31) — suppression verrouillée si rattachée
 * à au moins un produit (même règle que les catégories clients).
 */
@Entity(tableName = "product_categories")
data class ProductCategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nom: String,
    /** PRODUIT / SERVICE / DEPENSE / AUTRE. */
    val type: String = "PRODUIT",
    val parentId: Long? = null,
    val description: String? = null,
    val actif: Boolean = true,
)

/**
 * Produit — code `PRD-2026-0001` unique via SequenceManager ; code-barres indexé
 * pour la recherche à la vente ; le stock courant n'est **jamais** stocké ici :
 * il provient exclusivement des mouvements de stock (spec §43).
 */
@Entity(
    tableName = "products",
    indices = [
        Index(value = ["code"], unique = true),
        Index(value = ["barcode"]),
        Index(value = ["nom"]),
    ],
)
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val code: String,
    val nom: String,
    val type: ProductType = ProductType.ACHATE_REVENDU,
    val reference: String? = null,
    /** Code-barres saisi ou scanné. */
    val barcode: String? = null,
    val sku: String? = null,
    val categorieId: Long? = null,
    val marque: String? = null,
    /** Unité de vente (ex. pièce, carton, litre). */
    val unite: String? = null,
    /** Photo locale (PI jamais dans le cloud). */
    val photoPath: String? = null,
    val prixAchat: Double? = null,
    val prixVente: Double? = null,
    val prixRevient: Double? = null,
    val prixMinimum: Double? = null,
    /** Remise maximale autorisée en % (spec §7). */
    val remiseMaxPct: Double = 0.0,
    /** Seuils d'alerte — le stock lui-même provient des mouvements de stock. */
    val stockMin: Double = 0.0,
    val stockMax: Double? = null,
    val stockSecurite: Double = 0.0,
    /** Entrepôt principal de stockage. */
    val siteId: Long? = null,
    val emplacement: String? = null,
    val fournisseurId: Long? = null,
    val refFournisseur: String? = null,
    val description: String? = null,
    /** Poids en kilogrammes. */
    val poids: Double? = null,
    /** Volume en litres. */
    val volume: Double? = null,
    val origine: String? = null,
    val notes: String? = null,
    val statut: ProductStatus = ProductStatus.ACTIF,
    val active: Boolean = true,
    val createdAt: Long,
)

/**
 * Stock courant d'un produit dans un site — mis à jour exclusivement par les
 * mouvements de stock (spec §43 : ne jamais modifier arbitrairement un champ stock).
 */
@Entity(tableName = "product_stock", primaryKeys = ["produitId", "siteId"])
data class ProductStockEntity(
    val produitId: Long,
    val siteId: Long,
    val quantite: Double = 0.0,
)

/**
 * Type de mouvement — le sens est porté par le type, jamais par un signe masqué.
 * TRANSFERT_* ne s'emploie que par paires (sortie source + entrée destination,
 * même référence TRF).
 */
enum class StockMovementType {
    ENTREE,
    SORTIE,
    /** Correction : écart signé appliqué au stock (inventaire, casse comptée…). */
    AJUSTEMENT,
    TRANSFERT_SORTIE,
    TRANSFERT_ENTREE,
}

/**
 * Mouvement de stock — piste d'audit : chaque variation de
 * [ProductStockEntity.quantite] est accompagnée de son mouvement (spec §38/§43).
 */
@Entity(
    tableName = "stock_movements",
    indices = [
        Index(value = ["produitId"]),
        Index(value = ["siteId"]),
        Index(value = ["horodatage"]),
    ],
)
data class StockMovementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val produitId: Long,
    val siteId: Long,
    val type: StockMovementType,
    /** Quantité positive pour ENTRÉE/SORTIE/TRANSFERT ; écart signé pour AJUSTEMENT. */
    val quantite: Double,
    val motif: String,
    /** Référence du document d'origine (vente, achat, transfert TRF…). */
    val reference: String? = null,
    val commentaire: String? = null,
    val horodatage: Long,
)
