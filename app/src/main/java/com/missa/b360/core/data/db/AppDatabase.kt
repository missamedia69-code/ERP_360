package com.missa.b360.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.missa.b360.core.data.dao.AbsenceDao
import com.missa.b360.core.data.dao.BackupDao
import com.missa.b360.core.data.dao.ClientDao
import com.missa.b360.core.data.dao.EmployeeDao
import com.missa.b360.core.data.dao.EnterpriseDao
import com.missa.b360.core.data.dao.FournisseurDao
import com.missa.b360.core.data.dao.JournalDao
import com.missa.b360.core.data.dao.LicenceDao
import com.missa.b360.core.data.dao.NotificationDao
import com.missa.b360.core.data.dao.OperationRecordDao
import com.missa.b360.core.data.dao.PaymentMethodDao
import com.missa.b360.core.data.dao.ProductDao
import com.missa.b360.core.data.dao.ProductStockDao
import com.missa.b360.core.data.dao.StockMovementDao
import com.missa.b360.core.data.dao.TaskDao
import com.missa.b360.core.data.dao.RoleDao
import com.missa.b360.core.data.dao.SequenceDao
import com.missa.b360.core.data.dao.SettingDao
import com.missa.b360.core.data.dao.SiteDao
import com.missa.b360.core.data.dao.TaxDao
import com.missa.b360.core.data.dao.UserDao
import com.missa.b360.core.data.entity.AbsenceEntity
import com.missa.b360.core.data.entity.BackupEntity
import com.missa.b360.core.data.entity.BadgeLoyaltyEntity
import com.missa.b360.core.data.entity.CategoryClientEntity
import com.missa.b360.core.data.entity.ClientAddressEntity
import com.missa.b360.core.data.entity.ClientContactEntity
import com.missa.b360.core.data.entity.ClientEntity
import com.missa.b360.core.data.entity.EmployeeEntity
import com.missa.b360.core.data.entity.EnterpriseEntity
import com.missa.b360.core.data.entity.FournisseurEntity
import com.missa.b360.core.data.entity.JournalEntryEntity
import com.missa.b360.core.data.entity.LicenceEntity
import com.missa.b360.core.data.entity.NotificationEntity
import com.missa.b360.core.data.entity.OperationRecordEntity
import com.missa.b360.core.data.entity.PaymentMethodEntity
import com.missa.b360.core.data.entity.ProductCategoryEntity
import com.missa.b360.core.data.entity.ProductEntity
import com.missa.b360.core.data.entity.ProductStockEntity
import com.missa.b360.core.data.entity.StockMovementEntity
import com.missa.b360.core.data.entity.PriceClientEntity
import com.missa.b360.core.data.entity.RoleEntity
import com.missa.b360.core.data.entity.RolePermissionEntity
import com.missa.b360.core.data.entity.SequenceEntity
import com.missa.b360.core.data.entity.SettingEntity
import com.missa.b360.core.data.entity.SiteEntity
import com.missa.b360.core.data.entity.TaskEntity
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
        ProductCategoryEntity::class,
        ProductEntity::class,
        ProductStockEntity::class,
        StockMovementEntity::class,
        EmployeeEntity::class,
        AbsenceEntity::class,
        TaskEntity::class,
    ],
    version = 7,
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
    abstract fun productDao(): ProductDao
    abstract fun productStockDao(): ProductStockDao
    abstract fun stockMovementDao(): StockMovementDao
    abstract fun employeeDao(): EmployeeDao
    abstract fun absenceDao(): AbsenceDao
    abstract fun taskDao(): TaskDao

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

        /** v5 → v6 : produits, catégories, stock courant et mouvements de stock. */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `product_categories` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `nom` TEXT NOT NULL, " +
                        "`type` TEXT NOT NULL, `parentId` INTEGER, `description` TEXT, " +
                        "`actif` INTEGER NOT NULL)",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `products` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `code` TEXT NOT NULL, " +
                        "`nom` TEXT NOT NULL, `type` TEXT NOT NULL, `reference` TEXT, `barcode` TEXT, " +
                        "`sku` TEXT, `categorieId` INTEGER, `marque` TEXT, `unite` TEXT, `photoPath` TEXT, " +
                        "`prixAchat` REAL, `prixVente` REAL, `prixRevient` REAL, `prixMinimum` REAL, " +
                        "`remiseMaxPct` REAL NOT NULL, `stockMin` REAL NOT NULL, `stockMax` REAL, " +
                        "`stockSecurite` REAL NOT NULL, `siteId` INTEGER, `emplacement` TEXT, " +
                        "`fournisseurId` INTEGER, `refFournisseur` TEXT, `description` TEXT, " +
                        "`poids` REAL, `volume` REAL, `origine` TEXT, `notes` TEXT, " +
                        "`statut` TEXT NOT NULL, `active` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)",
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_products_code` ON `products` (`code`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_products_barcode` ON `products` (`barcode`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_products_nom` ON `products` (`nom`)",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `product_stock` (" +
                        "`produitId` INTEGER NOT NULL, `siteId` INTEGER NOT NULL, " +
                        "`quantite` REAL NOT NULL, PRIMARY KEY (`produitId`, `siteId`))",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `stock_movements` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `produitId` INTEGER NOT NULL, " +
                        "`siteId` INTEGER NOT NULL, `type` TEXT NOT NULL, `quantite` REAL NOT NULL, " +
                        "`motif` TEXT NOT NULL, `reference` TEXT, `commentaire` TEXT, " +
                        "`horodatage` INTEGER NOT NULL)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_stock_movements_produitId` " +
                        "ON `stock_movements` (`produitId`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_stock_movements_siteId` " +
                        "ON `stock_movements` (`siteId`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_stock_movements_horodatage` " +
                        "ON `stock_movements` (`horodatage`)",
                )
            }
        }

        /** v6 → v7 : RH (employés, absences) + tâches de suivi. */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `employees` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `code` TEXT NOT NULL, " +
                        "`nom` TEXT NOT NULL, `telephone` TEXT NOT NULL, `poste` TEXT, " +
                        "`salaireBase` REAL NOT NULL, `joursMensuels` REAL NOT NULL, " +
                        "`statut` TEXT NOT NULL, `notes` TEXT, `createdAt` INTEGER NOT NULL)",
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_employees_code` ON `employees` (`code`)",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `absences` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `employeeId` INTEGER NOT NULL, " +
                        "`type` TEXT NOT NULL, `dateDebut` INTEGER NOT NULL, `dureeJours` REAL NOT NULL, " +
                        "`motif` TEXT, `createdAt` INTEGER NOT NULL)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_absences_employeeId` ON `absences` (`employeeId`)",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `tasks` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `titre` TEXT NOT NULL, " +
                        "`notes` TEXT, `statut` TEXT NOT NULL, `echeance` INTEGER, `createdAt` INTEGER NOT NULL)",
                )
            }
        }
    }
}
