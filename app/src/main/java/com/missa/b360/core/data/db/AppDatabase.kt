package com.missa.b360.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.missa.b360.core.data.dao.BackupDao
import com.missa.b360.core.data.dao.ClientDao
import com.missa.b360.core.data.dao.EnterpriseDao
import com.missa.b360.core.data.dao.FournisseurDao
import com.missa.b360.core.data.dao.JournalDao
import com.missa.b360.core.data.dao.LicenceDao
import com.missa.b360.core.data.dao.NotificationDao
import com.missa.b360.core.data.dao.OperationRecordDao
import com.missa.b360.core.data.dao.PaymentMethodDao
import com.missa.b360.core.data.dao.RoleDao
import com.missa.b360.core.data.dao.SequenceDao
import com.missa.b360.core.data.dao.SettingDao
import com.missa.b360.core.data.dao.SiteDao
import com.missa.b360.core.data.dao.StockDao
import com.missa.b360.core.data.dao.TaxDao
import com.missa.b360.core.data.dao.UserDao
import com.missa.b360.core.data.entity.BackupEntity
import com.missa.b360.core.data.entity.BadgeLoyaltyEntity
import com.missa.b360.core.data.entity.CategoryClientEntity
import com.missa.b360.core.data.entity.ClientAddressEntity
import com.missa.b360.core.data.entity.ClientContactEntity
import com.missa.b360.core.data.entity.ClientEntity
import com.missa.b360.core.data.entity.EnterpriseEntity
import com.missa.b360.core.data.entity.FournisseurEntity
import com.missa.b360.core.data.entity.JournalEntryEntity
import com.missa.b360.core.data.entity.LicenceEntity
import com.missa.b360.core.data.entity.NotificationEntity
import com.missa.b360.core.data.entity.OperationRecordEntity
import com.missa.b360.core.data.entity.PaymentMethodEntity
import com.missa.b360.core.data.entity.PriceClientEntity
import com.missa.b360.core.data.entity.RoleEntity
import com.missa.b360.core.data.entity.RolePermissionEntity
import com.missa.b360.core.data.entity.SequenceEntity
import com.missa.b360.core.data.entity.SettingEntity
import com.missa.b360.core.data.entity.SiteEntity
import com.missa.b360.core.data.entity.StockCategoryEntity
import com.missa.b360.core.data.entity.StockInventoryEntity
import com.missa.b360.core.data.entity.StockInventoryLineEntity
import com.missa.b360.core.data.entity.StockMovementEntity
import com.missa.b360.core.data.entity.StockProductEntity
import com.missa.b360.core.data.entity.StockWarehouseEntity
import com.missa.b360.core.data.entity.TaxEntity
import com.missa.b360.core.data.entity.UserEntity

/**
 * Base de données offline-first Missa Business 360 (cahier de charge §8).
 * Aucune donnée de démo : la base démarre vide (l'onboarding crée entreprise, PIN, licence).
 */
@Database(
    entities = [
        EnterpriseEntity::class,
        SiteEntity::class,
        UserEntity::class,
        RoleEntity::class,
        RolePermissionEntity::class,
        LicenceEntity::class,
        SequenceEntity::class,
        TaxEntity::class,
        PaymentMethodEntity::class,
        SettingEntity::class,
        BackupEntity::class,
        JournalEntryEntity::class,
        NotificationEntity::class,
        ClientEntity::class,
        ClientContactEntity::class,
        ClientAddressEntity::class,
        CategoryClientEntity::class,
        PriceClientEntity::class,
        BadgeLoyaltyEntity::class,
        FournisseurEntity::class,
        OperationRecordEntity::class,
        StockCategoryEntity::class,
        StockWarehouseEntity::class,
        StockProductEntity::class,
        StockMovementEntity::class,
        StockInventoryEntity::class,
        StockInventoryLineEntity::class,
    ],
    version = 6,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun enterpriseDao(): EnterpriseDao
    abstract fun siteDao(): SiteDao
    abstract fun userDao(): UserDao
    abstract fun roleDao(): RoleDao
    abstract fun licenceDao(): LicenceDao
    abstract fun sequenceDao(): SequenceDao
    abstract fun taxDao(): TaxDao
    abstract fun paymentMethodDao(): PaymentMethodDao
    abstract fun settingDao(): SettingDao
    abstract fun backupDao(): BackupDao
    abstract fun journalDao(): JournalDao
    abstract fun notificationDao(): NotificationDao
    abstract fun clientDao(): ClientDao
    abstract fun fournisseurDao(): FournisseurDao
    abstract fun operationRecordDao(): OperationRecordDao
    abstract fun stockDao(): StockDao

    companion object {
        /** v1 → v2 (Phase D) : table fournisseurs. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `fournisseurs` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`code` TEXT NOT NULL, `nom` TEXT NOT NULL, `telephone` TEXT NOT NULL, " +
                        "`telephone2` TEXT, `email` TEXT, `adresse` TEXT, `siteId` INTEGER, " +
                        "`notes` TEXT, `statut` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)",
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_fournisseurs_code` ON `fournisseurs` (`code`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_fournisseurs_telephone` ON `fournisseurs` (`telephone`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_fournisseurs_nom` ON `fournisseurs` (`nom`)",
                )
            }
        }

        /** v2 → v3 : conserve le logo choisi pour l'entreprise. */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `enterprise` ADD COLUMN `logoUri` TEXT")
            }
        }

        /** v3 → v4 : pièces opérationnelles des modules Stock à Projets. */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `operation_records` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`module` TEXT NOT NULL, `reference` TEXT NOT NULL, `title` TEXT NOT NULL, " +
                        "`counterpart` TEXT, `amount` REAL, `quantity` REAL, " +
                        "`direction` TEXT NOT NULL, `status` TEXT NOT NULL, `notes` TEXT, " +
                        "`createdAt` INTEGER NOT NULL)",
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_operation_records_reference` " +
                        "ON `operation_records` (`reference`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_operation_records_module` " +
                        "ON `operation_records` (`module`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_operation_records_createdAt` " +
                        "ON `operation_records` (`createdAt`)",
                )
            }
        }

        /** v4 → v5 : profil client détaillé (NIF, contacts et adresses multiples). */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `clients` ADD COLUMN `nif` TEXT")
                db.execSQL("ALTER TABLE `clients` ADD COLUMN `commercial` TEXT")
                db.execSQL(
                    "ALTER TABLE `clients` ADD COLUMN `conditionPaiementJours` INTEGER NOT NULL DEFAULT 30",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `client_contacts` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`clientId` INTEGER NOT NULL, `nom` TEXT NOT NULL, `fonction` TEXT, " +
                        "`telephone` TEXT, `email` TEXT, `principal` INTEGER NOT NULL)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_client_contacts_clientId` " +
                        "ON `client_contacts` (`clientId`)",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `client_addresses` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`clientId` INTEGER NOT NULL, `libelle` TEXT NOT NULL, `adresse` TEXT NOT NULL, " +
                        "`ville` TEXT, `principale` INTEGER NOT NULL)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_client_addresses_clientId` " +
                        "ON `client_addresses` (`clientId`)",
                )
            }
        }

        /** v5 → v6 : module Stock dédié (catégories, entrepôts, produits, mouvements, inventaires). */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `stock_categories` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `nom` TEXT NOT NULL, " +
                        "`couleur` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)",
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_stock_categories_nom` " +
                        "ON `stock_categories` (`nom`)",
                )

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `stock_warehouses` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `nom` TEXT NOT NULL, " +
                        "`adresse` TEXT, `principal` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)",
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_stock_warehouses_nom` " +
                        "ON `stock_warehouses` (`nom`)",
                )

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `stock_products` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `code` TEXT NOT NULL, " +
                        "`nom` TEXT NOT NULL, `categorieId` INTEGER, `warehouseId` INTEGER, " +
                        "`unite` TEXT NOT NULL, `prixAchat` REAL NOT NULL, `prixVente` REAL NOT NULL, " +
                        "`seuilMin` REAL NOT NULL, `seuilMax` REAL NOT NULL, " +
                        "`quantiteInitiale` REAL NOT NULL, `quantite` REAL NOT NULL, " +
                        "`actif` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)",
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_stock_products_code` " +
                        "ON `stock_products` (`code`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_stock_products_categorieId` " +
                        "ON `stock_products` (`categorieId`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_stock_products_warehouseId` " +
                        "ON `stock_products` (`warehouseId`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_stock_products_nom` " +
                        "ON `stock_products` (`nom`)",
                )

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `stock_movements` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `reference` TEXT NOT NULL, " +
                        "`type` TEXT NOT NULL, `productId` INTEGER NOT NULL, " +
                        "`sourceWarehouseId` INTEGER, `targetWarehouseId` INTEGER, " +
                        "`quantity` REAL NOT NULL, `delta` REAL NOT NULL, `price` REAL, " +
                        "`counterpart` TEXT, `status` TEXT NOT NULL, `date` INTEGER NOT NULL, " +
                        "`notes` TEXT, `createdAt` INTEGER NOT NULL)",
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_stock_movements_reference` " +
                        "ON `stock_movements` (`reference`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_stock_movements_productId` " +
                        "ON `stock_movements` (`productId`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_stock_movements_type` " +
                        "ON `stock_movements` (`type`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_stock_movements_date` " +
                        "ON `stock_movements` (`date`)",
                )

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `stock_inventories` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `reference` TEXT NOT NULL, " +
                        "`warehouseId` INTEGER, `status` TEXT NOT NULL, `date` INTEGER NOT NULL, " +
                        "`notes` TEXT, `createdAt` INTEGER NOT NULL, `validatedAt` INTEGER, " +
                        "`completedAt` INTEGER)",
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_stock_inventories_reference` " +
                        "ON `stock_inventories` (`reference`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_stock_inventories_status` " +
                        "ON `stock_inventories` (`status`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_stock_inventories_date` " +
                        "ON `stock_inventories` (`date`)",
                )

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `stock_inventory_lines` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `inventoryId` INTEGER NOT NULL, " +
                        "`productId` INTEGER NOT NULL, `expectedQuantity` REAL NOT NULL, " +
                        "`countedQuantity` REAL NOT NULL, `ecart` REAL NOT NULL, `notes` TEXT)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_stock_inventory_lines_inventoryId` " +
                        "ON `stock_inventory_lines` (`inventoryId`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_stock_inventory_lines_productId` " +
                        "ON `stock_inventory_lines` (`productId`)",
                )
            }
        }
    }
}
