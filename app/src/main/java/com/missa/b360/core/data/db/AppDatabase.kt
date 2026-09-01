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
import com.missa.b360.core.data.dao.PaymentMethodDao
import com.missa.b360.core.data.dao.RoleDao
import com.missa.b360.core.data.dao.SequenceDao
import com.missa.b360.core.data.dao.SettingDao
import com.missa.b360.core.data.dao.SiteDao
import com.missa.b360.core.data.dao.TaxDao
import com.missa.b360.core.data.dao.UserDao
import com.missa.b360.core.data.entity.BackupEntity
import com.missa.b360.core.data.entity.BadgeLoyaltyEntity
import com.missa.b360.core.data.entity.CategoryClientEntity
import com.missa.b360.core.data.entity.ClientEntity
import com.missa.b360.core.data.entity.EnterpriseEntity
import com.missa.b360.core.data.entity.FournisseurEntity
import com.missa.b360.core.data.entity.JournalEntryEntity
import com.missa.b360.core.data.entity.LicenceEntity
import com.missa.b360.core.data.entity.NotificationEntity
import com.missa.b360.core.data.entity.PaymentMethodEntity
import com.missa.b360.core.data.entity.PriceClientEntity
import com.missa.b360.core.data.entity.RoleEntity
import com.missa.b360.core.data.entity.RolePermissionEntity
import com.missa.b360.core.data.entity.SequenceEntity
import com.missa.b360.core.data.entity.SettingEntity
import com.missa.b360.core.data.entity.SiteEntity
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
        CategoryClientEntity::class,
        PriceClientEntity::class,
        BadgeLoyaltyEntity::class,
        FournisseurEntity::class,
    ],
    version = 3,
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
    }
}
