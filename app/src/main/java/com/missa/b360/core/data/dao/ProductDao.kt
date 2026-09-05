package com.missa.b360.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.missa.b360.core.data.entity.ProductCategoryEntity
import com.missa.b360.core.data.entity.ProductEntity
import com.missa.b360.core.data.entity.ProductStockEntity
import com.missa.b360.core.data.entity.StockMovementEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO Produits (spec §7).
 * Convention C7 : aucune suppression physique — statut `DESACTIVE` uniquement.
 */
@Dao
interface ProductDao {
    @Query("SELECT * FROM products WHERE active = 1 ORDER BY nom")
    fun observeAll(): Flow<List<ProductEntity>>

    /** Liste complète destinée à la fiche produit, y compris les produits désactivés. */
    @Query("SELECT * FROM products ORDER BY nom")
    fun observeAllIncludingInactive(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<ProductEntity?>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ProductEntity?

    @Query("SELECT * FROM products WHERE code = :code LIMIT 1")
    suspend fun getByCode(code: String): ProductEntity?

    /** Recherche par code-barres (saisie ou scan) — premier actif trouvé. */
    @Query("SELECT * FROM products WHERE active = 1 AND barcode = :barcode LIMIT 1")
    suspend fun getByBarcode(barcode: String): ProductEntity?

    @Insert
    suspend fun insert(product: ProductEntity): Long

    @Update
    suspend fun update(product: ProductEntity)

    /** Désactivation (jamais de DELETE — C7). */
    @Query("UPDATE products SET statut = 'DESACTIVE', active = 0 WHERE id = :id")
    suspend fun desactiver(id: Long)

    @Query("SELECT COUNT(*) FROM products")
    suspend fun count(): Int

    // --- Catégories de produit (spec §31) ---
    @Query("SELECT * FROM product_categories ORDER BY nom")
    fun observeCategories(): Flow<List<ProductCategoryEntity>>

    @Query("SELECT * FROM product_categories WHERE id = :id LIMIT 1")
    suspend fun getCategorieById(id: Long): ProductCategoryEntity?

    @Insert
    suspend fun insertCategorie(categorie: ProductCategoryEntity): Long

    @Update
    suspend fun updateCategorie(categorie: ProductCategoryEntity)

    /** Suppression interdite si rattachée : le UseCase vérifie ce compteur. */
    @Query("SELECT COUNT(*) FROM products WHERE categorieId = :categorieId")
    suspend fun countProductsAvecCategorie(categorieId: Long): Int

    @Query("DELETE FROM product_categories WHERE id = :categorieId")
    suspend fun deleteCategorie(categorieId: Long)
}

/**
 * Stock courant — écriture par `ensureRow` + `remplacer` **dans une transaction**
 * (aucun upsert SQL) afin de rester compatible avec toutes les versions de SQLite.
 */
@Dao
interface ProductStockDao {
    /** Quantité dans un site, ou null si aucune ligne (à traiter comme 0.0). */
    @Query("SELECT quantite FROM product_stock WHERE produitId = :produitId AND siteId = :siteId LIMIT 1")
    suspend fun quantite(produitId: Long, siteId: Long): Double?

    /** Toutes les lignes de stock (listes produits → stock disponible). */
    @Query("SELECT * FROM product_stock")
    fun observeToutes(): Flow<List<ProductStockEntity>>

    /** Crée la ligne à 0 si elle n'existe pas (no-op sinon). */
    @Query("INSERT OR IGNORE INTO product_stock (produitId, siteId, quantite) VALUES (:produitId, :siteId, 0)")
    suspend fun ensureRow(produitId: Long, siteId: Long)

    /** Écrase la ligne — à appeler uniquement dans une transaction après relecture. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun remplacer(ligne: ProductStockEntity)

    /** Site ayant la plus grande quantité positive (sortie sans site principal). */
    @Query(
        "SELECT siteId FROM product_stock WHERE produitId = :produitId AND quantite > 0 " +
            "ORDER BY quantite DESC LIMIT 1",
    )
    suspend fun siteAvecPlusDeStock(produitId: Long): Long?

    /** Stock total multi-site d'un produit. */
    @Query("SELECT COALESCE(SUM(quantite), 0) FROM product_stock WHERE produitId = :produitId")
    suspend fun total(produitId: Long): Double
}

/**
 * Historique des mouvements de stock (lecture seule en UI — l'écriture passe par
 * les UseCases transactionnels, spec §43/§44).
 */
@Dao
interface StockMovementDao {
    @Insert
    suspend fun insert(movement: StockMovementEntity): Long

    @Query("SELECT * FROM stock_movements ORDER BY horodatage DESC, id DESC LIMIT :limit")
    fun observeRecent(limit: Int = 200): Flow<List<StockMovementEntity>>

    /** Mouvements joints au produit et au site, prêts à afficher. */
    @Query(
        "SELECT m.id, COALESCE(p.nom, 'Produit') AS produitNom, COALESCE(p.code, '') AS produitCode, " +
            "s.nom AS siteNom, m.type, m.quantite, m.motif, m.reference, m.commentaire, m.horodatage " +
            "FROM stock_movements m " +
            "LEFT JOIN products p ON p.id = m.produitId " +
            "LEFT JOIN sites s ON s.id = m.siteId " +
            "ORDER BY m.horodatage DESC, m.id DESC LIMIT :limit",
    )
    fun observeJoints(limit: Int = 200): Flow<List<StockMovementView>>
}

/** Ligne d'historique de mouvements, jointe pour l'affichage (pas de rechargement). */
data class StockMovementView(
    val id: Long,
    val produitNom: String,
    val produitCode: String,
    val siteNom: String?,
    /** [StockMovementType].name */
    val type: String,
    val quantite: Double,
    val motif: String,
    val reference: String?,
    val commentaire: String?,
    val horodatage: Long,
)
