package com.missa.b360.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.missa.b360.core.data.dao.AbsenceDao
import com.missa.b360.core.data.dao.AccountingDao
import com.missa.b360.core.data.dao.BackupDao
import com.missa.b360.core.data.dao.ClientBalanceDao
import com.missa.b360.core.data.dao.ClientDao
import com.missa.b360.core.data.dao.ClientFollowupDao
import com.missa.b360.core.data.dao.ClientPaymentDao
import com.missa.b360.core.data.dao.CompteTresorerieDao
import com.missa.b360.core.data.dao.EmployeeDao
import com.missa.b360.core.data.dao.EquipementDao
import com.missa.b360.core.data.dao.GroupeArticleDao
import com.missa.b360.core.data.dao.EnterpriseDao
import com.missa.b360.core.data.dao.FournisseurBalanceDao
import com.missa.b360.core.data.dao.FournisseurCompteBancaireDao
import com.missa.b360.core.data.dao.FournisseurContactDao
import com.missa.b360.core.data.dao.FournisseurDao
import com.missa.b360.core.data.dao.FournisseurDocumentDao
import com.missa.b360.core.data.dao.FournisseurEvenementDao
import com.missa.b360.core.data.dao.FournisseurItemDao
import com.missa.b360.core.data.dao.FournisseurPaiementPlanifieDao
import com.missa.b360.core.data.dao.FournisseurScoreDao
import com.missa.b360.core.data.dao.JournalDao
import com.missa.b360.core.data.dao.LicenceDao
import com.missa.b360.core.data.dao.MouvementTresorerieDao
import com.missa.b360.core.data.dao.InterventionDao
import com.missa.b360.core.data.dao.NonConformiteDao
import com.missa.b360.core.data.dao.NotificationDao
import com.missa.b360.core.data.dao.OperationRecordDao
import com.missa.b360.core.data.dao.PaymentMethodDao
import com.missa.b360.core.data.dao.ProductDao
import com.missa.b360.core.data.dao.ProductExtrasDao
import com.missa.b360.core.data.dao.ProductStockDao
import com.missa.b360.core.data.dao.ProductEquipementDao
import com.missa.b360.core.data.dao.StockMovementDao
import com.missa.b360.core.data.dao.TaskDao
import com.missa.b360.core.data.dao.RoleDao
import com.missa.b360.core.data.dao.SequenceDao
import com.missa.b360.core.data.dao.SettingDao
import com.missa.b360.core.data.dao.SiteDao
import com.missa.b360.core.data.dao.SyncDao
import com.missa.b360.core.data.dao.ServiceWorkflowDao
import com.missa.b360.core.data.dao.TaxDao
import com.missa.b360.core.data.dao.UserDao
import com.missa.b360.core.data.entity.AbsenceEntity
import com.missa.b360.core.data.entity.AccountingAccountEntity
import com.missa.b360.core.data.entity.AccountingEntryLineEntity
import com.missa.b360.core.data.entity.AccountingJournalEntity
import com.missa.b360.core.data.entity.AccountingPeriodEntity
import com.missa.b360.core.data.entity.AccountingPostingRuleEntity
import com.missa.b360.core.data.entity.AccountingSettingsEntity
import com.missa.b360.core.data.entity.AccountingVoucherEntity
import com.missa.b360.core.data.entity.BackupEntity
import com.missa.b360.core.data.entity.BadgeLoyaltyEntity
import com.missa.b360.core.data.entity.CategoryClientEntity
import com.missa.b360.core.data.entity.ClientAddressEntity
import com.missa.b360.core.data.entity.ClientBalanceEntity
import com.missa.b360.core.data.entity.ClientContactEntity
import com.missa.b360.core.data.entity.ClientEntity
import com.missa.b360.core.data.entity.ClientFollowupEntity
import com.missa.b360.core.data.entity.ClientPaymentEntity
import com.missa.b360.core.data.entity.CompteTresorerieEntity
import com.missa.b360.core.data.entity.KitComposantEntity
import com.missa.b360.core.data.entity.ProductConsignationEntity
import com.missa.b360.core.data.entity.ProductDechetEntity
import com.missa.b360.core.data.entity.ProductEmballageEntity
import com.missa.b360.core.data.entity.ProductKitEntity
import com.missa.b360.core.data.entity.EmployeeEntity
import com.missa.b360.core.data.entity.EquipementEntity
import com.missa.b360.core.data.entity.GroupeAchatEntity
import com.missa.b360.core.data.entity.GroupeArticleEntity
import com.missa.b360.core.data.entity.GroupeComptabiliteEntity
import com.missa.b360.core.data.entity.GroupeMaintenanceEntity
import com.missa.b360.core.data.entity.GroupeProductionEntity
import com.missa.b360.core.data.entity.GroupeStockEntity
import com.missa.b360.core.data.entity.GroupeVenteEntity
import com.missa.b360.core.data.entity.EnterpriseEntity
import com.missa.b360.core.data.entity.FournisseurBalanceEntity
import com.missa.b360.core.data.entity.FournisseurCompteBancaireEntity
import com.missa.b360.core.data.entity.FournisseurContactEntity
import com.missa.b360.core.data.entity.FournisseurDocumentEntity
import com.missa.b360.core.data.entity.FournisseurEntity
import com.missa.b360.core.data.entity.FournisseurEvenementEntity
import com.missa.b360.core.data.entity.FournisseurItemEntity
import com.missa.b360.core.data.entity.FournisseurPaiementPlanifieEntity
import com.missa.b360.core.data.entity.FournisseurScoreEntity
import com.missa.b360.core.data.entity.JournalEntryEntity
import com.missa.b360.core.data.entity.LicenceEntity
import com.missa.b360.core.data.entity.MouvementTresorerieEntity
import com.missa.b360.core.data.entity.InterventionEntity
import com.missa.b360.core.data.entity.NonConformiteEntity
import com.missa.b360.core.data.entity.NotificationEntity
import com.missa.b360.core.data.entity.OperationRecordEntity
import com.missa.b360.core.data.entity.PaymentMethodEntity
import com.missa.b360.core.data.entity.ProductCategoryEntity
import com.missa.b360.core.data.entity.ProductEntity
import com.missa.b360.core.data.entity.ProductStockEntity
import com.missa.b360.core.data.entity.InventaireEntity
import com.missa.b360.core.data.entity.InventaireLigneEntity
import com.missa.b360.core.data.dao.InventaireDao
import com.missa.b360.core.data.entity.ProductEquipementEntity
import com.missa.b360.core.data.entity.StockMovementEntity
import com.missa.b360.core.data.entity.PriceClientEntity
import com.missa.b360.core.data.entity.RoleEntity
import com.missa.b360.core.data.entity.RolePermissionEntity
import com.missa.b360.core.data.entity.SequenceEntity
import com.missa.b360.core.data.entity.SettingEntity
import com.missa.b360.core.data.entity.SiteEntity
import com.missa.b360.core.data.entity.ServiceContractEntity
import com.missa.b360.core.data.entity.CustomerServiceAssetEntity
import com.missa.b360.core.data.entity.ServiceRequestEntity
import com.missa.b360.core.data.entity.ServiceWorkOrderEntity
import com.missa.b360.core.data.entity.ServiceTimesheetEntity
import com.missa.b360.core.data.entity.ServiceReportEntity
import com.missa.b360.core.data.entity.ServiceAttachmentEntity
import com.missa.b360.core.data.entity.TaskEntity
import com.missa.b360.core.data.entity.SyncDeviceEntity
import com.missa.b360.core.data.entity.SyncOutboxEntity
import com.missa.b360.core.data.entity.SyncInboxEntity
import com.missa.b360.core.data.entity.SyncConflictEntity
import com.missa.b360.core.data.entity.TaxEntity
import com.missa.b360.core.data.entity.UserEntity

/**
 * Base de données offline-first Missa Business 360 (cahier de charge §8).
 * Aucune donnée de démo : la base démarre vide (l'onboarding crée entreprise, PIN, licence).
 */
@Database(
    entities = [
        EnterpriseEntity::class,
        AccountingSettingsEntity::class,
        AccountingAccountEntity::class,
        AccountingJournalEntity::class,
        AccountingPostingRuleEntity::class,
        AccountingPeriodEntity::class,
        AccountingVoucherEntity::class,
        AccountingEntryLineEntity::class,
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
        CompteTresorerieEntity::class,
        MouvementTresorerieEntity::class,
        NonConformiteEntity::class,
        EquipementEntity::class,
        InterventionEntity::class,
        GroupeArticleEntity::class,
        GroupeStockEntity::class,
        GroupeAchatEntity::class,
        GroupeVenteEntity::class,
        GroupeProductionEntity::class,
        GroupeMaintenanceEntity::class,
        GroupeComptabiliteEntity::class,
        ProductCategoryEntity::class,
        ProductEntity::class,
        ProductStockEntity::class,
        StockMovementEntity::class,
        ProductEquipementEntity::class,
        ProductDechetEntity::class,
        ProductEmballageEntity::class,
        ProductConsignationEntity::class,
        ProductKitEntity::class,
        KitComposantEntity::class,
        InventaireEntity::class,
        InventaireLigneEntity::class,
        EmployeeEntity::class,
        AbsenceEntity::class,
        TaskEntity::class,
        FournisseurContactEntity::class,
        FournisseurCompteBancaireEntity::class,
        FournisseurDocumentEntity::class,
        FournisseurItemEntity::class,
        FournisseurEvenementEntity::class,
        ServiceContractEntity::class,
        CustomerServiceAssetEntity::class,
        ServiceRequestEntity::class,
        ServiceWorkOrderEntity::class,
        ServiceTimesheetEntity::class,
        ServiceReportEntity::class,
        ServiceAttachmentEntity::class,
        SyncDeviceEntity::class,
        SyncOutboxEntity::class,
        SyncInboxEntity::class,
        SyncConflictEntity::class,
        ClientBalanceEntity::class,
        ClientFollowupEntity::class,
        ClientPaymentEntity::class,
        FournisseurBalanceEntity::class,
        FournisseurScoreEntity::class,
        FournisseurPaiementPlanifieEntity::class,
    ],
    version = 24,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun enterpriseDao(): EnterpriseDao
    abstract fun accountingDao(): AccountingDao
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
    abstract fun fournisseurContactDao(): FournisseurContactDao
    abstract fun fournisseurCompteBancaireDao(): FournisseurCompteBancaireDao
    abstract fun fournisseurDocumentDao(): FournisseurDocumentDao
    abstract fun fournisseurItemDao(): FournisseurItemDao
    abstract fun fournisseurEvenementDao(): FournisseurEvenementDao
    abstract fun operationRecordDao(): OperationRecordDao
    abstract fun serviceWorkflowDao(): ServiceWorkflowDao
    abstract fun productDao(): ProductDao
    abstract fun productExtrasDao(): ProductExtrasDao
    abstract fun productStockDao(): ProductStockDao
    abstract fun stockMovementDao(): StockMovementDao
    abstract fun productEquipementDao(): ProductEquipementDao
    abstract fun inventaireDao(): InventaireDao
    abstract fun employeeDao(): EmployeeDao
    abstract fun absenceDao(): AbsenceDao
    abstract fun taskDao(): TaskDao
    abstract fun compteTresorerieDao(): CompteTresorerieDao
    abstract fun mouvementTresorerieDao(): MouvementTresorerieDao
    abstract fun nonConformiteDao(): NonConformiteDao
    abstract fun equipementDao(): EquipementDao
    abstract fun interventionDao(): InterventionDao
    abstract fun groupeArticleDao(): GroupeArticleDao
    abstract fun syncDao(): SyncDao
    abstract fun clientBalanceDao(): ClientBalanceDao
    abstract fun clientFollowupDao(): ClientFollowupDao
    abstract fun clientPaymentDao(): ClientPaymentDao
    abstract fun fournisseurBalanceDao(): FournisseurBalanceDao
    abstract fun fournisseurScoreDao(): FournisseurScoreDao
    abstract fun fournisseurPaiementPlanifieDao(): FournisseurPaiementPlanifieDao

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

        /**
         * v7 → v8 : identifiants légaux de l'entreprise (numéro fiscal NIU/NIF et
         * registre du commerce RCCM), obligatoires sur les pièces de vente.
         * Colonnes nullables : aucune donnée existante n'est perdue.
         */
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `enterprise` ADD COLUMN `numeroFiscal` TEXT")
                db.execSQL("ALTER TABLE `enterprise` ADD COLUMN `registreCommerce` TEXT")
            }
        }

        /**
         * v8 → v9 : module Trésorerie — comptes (caisse, banque, mobile money)
         * et mouvements. Deux tables neuves : aucune donnée existante n'est
         * touchée, la migration est donc sans risque de perte.
         */
        /**
         * v9 → v10 : modules Qualité et Maintenance — registre des
         * non-conformités, parc d'équipements et interventions. Trois tables
         * neuves, aucune donnée existante touchée.
         */
        /**
         * v10 → v11 : les pièces portent l'identifiant du tiers concerné.
         *
         * Les pièces existantes gardent `tiersId` à NULL ; le rapprochement par
         * nom reste actif pour elles, ce qui préserve l'historique déjà saisi.
         */
        /**
         * v11 → v12 : groupes d'articles et leurs extensions métier.
         *
         * Les règles jusqu'ici codées en dur dans `ProductType` deviennent des
         * données : chaque groupe déclare s'il est stocké, valorisé, vendable,
         * maintenable, et porte ses comptes selon le référentiel comptable en
         * vigueur. Les articles reçoivent un rattachement facultatif, ce qui
         * laisse fonctionner les fiches déjà saisies.
         */
        /**
         * v14 → v15 : drapeaux article (vendable/achetable/stockable, spec §14)
         * et extensions par famille (déchets, emballages, consignations, kits).
         */
        val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Référentiel fournisseur étendu (spec module Fournisseurs) :
                // identité, fiscalité, paiement, achats, évaluation.
                val colonnes = listOf(
                    "type TEXT NOT NULL DEFAULT 'ENTREPRISE'",
                    "nomCommercial TEXT",
                    "pays TEXT NOT NULL DEFAULT 'CM'",
                    "devise TEXT NOT NULL DEFAULT 'XAF'",
                    "langue TEXT",
                    "siteWeb TEXT",
                    "description TEXT",
                    "motifBlocage TEXT",
                    "soumisLe INTEGER",
                    "approuveLe INTEGER",
                    "typeIdentifiantFiscal TEXT",
                    "identifiantFiscal TEXT",
                    "rccm TEXT",
                    "numTva TEXT",
                    "assujettiTva INTEGER NOT NULL DEFAULT 1",
                    "tauxRetenue REAL NOT NULL DEFAULT 0",
                    "exonere INTEGER NOT NULL DEFAULT 0",
                    "dateValidationFiscale INTEGER",
                    "conditionsPaiement TEXT",
                    "joursEcheance INTEGER NOT NULL DEFAULT 0",
                    "modePaiementPrefere TEXT",
                    "paiementBloque INTEGER NOT NULL DEFAULT 0",
                    "plafondPaiement REAL NOT NULL DEFAULT 0",
                    "approuve INTEGER NOT NULL DEFAULT 0",
                    "delaiMoyenJours INTEGER NOT NULL DEFAULT 0",
                    "quantiteMinCommande REAL NOT NULL DEFAULT 0",
                    "montantMinCommande REAL NOT NULL DEFAULT 0",
                    "categoriesFournies TEXT",
                    "incoterm TEXT",
                    "depotLivraisonId INTEGER",
                    "noteEvaluation REAL",
                    "commentaireEvaluation TEXT",
                    "dateEvaluation INTEGER",
                    "updatedAt INTEGER NOT NULL DEFAULT 0",
                )
                colonnes.forEach { db.execSQL("ALTER TABLE fournisseurs ADD COLUMN $it") }
                // L'ancien statut « désactivé » devient un archivage (cycle de vie complet).
                db.execSQL("UPDATE fournisseurs SET statut = 'ARCHIVE' WHERE statut = 'DESACTIVE'")

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `fournisseur_contacts` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`fournisseurId` INTEGER NOT NULL, `nom` TEXT NOT NULL, `prenom` TEXT, " +
                        "`fonction` TEXT, `service` TEXT, `telephone` TEXT, `whatsapp` TEXT, " +
                        "`email` TEXT, `principal` INTEGER NOT NULL, `roleAchats` INTEGER NOT NULL, " +
                        "`roleCompta` INTEGER NOT NULL, `roleLivraison` INTEGER NOT NULL, " +
                        "`roleUrgence` INTEGER NOT NULL, `actif` INTEGER NOT NULL)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_fournisseur_contacts_fournisseurId` " +
                        "ON `fournisseur_contacts` (`fournisseurId`)",
                )

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `fournisseur_comptes_bancaires` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`fournisseurId` INTEGER NOT NULL, `titulaire` TEXT NOT NULL, `banque` TEXT, " +
                        "`paysBanque` TEXT, `numeroCompte` TEXT, `iban` TEXT, `bicSwift` TEXT, " +
                        "`operateurMobile` TEXT, `numeroMobile` TEXT, `principal` INTEGER NOT NULL, " +
                        "`verification` TEXT NOT NULL, `verifieLe` INTEGER, `notes` TEXT)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_fournisseur_comptes_bancaires_fournisseurId` " +
                        "ON `fournisseur_comptes_bancaires` (`fournisseurId`)",
                )

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `fournisseur_documents` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`fournisseurId` INTEGER NOT NULL, `typeDocument` TEXT NOT NULL, " +
                        "`reference` TEXT, `cheminFichier` TEXT, `dateEmission` INTEGER, " +
                        "`dateExpiration` INTEGER, `verification` TEXT NOT NULL, `notes` TEXT)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_fournisseur_documents_fournisseurId` " +
                        "ON `fournisseur_documents` (`fournisseurId`)",
                )

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `fournisseur_items` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`fournisseurId` INTEGER NOT NULL, `productId` INTEGER NOT NULL, " +
                        "`reference` TEXT, `prixUnitaire` REAL NOT NULL, `delaiJours` INTEGER NOT NULL, " +
                        "`quantiteMin` REAL NOT NULL, `prefere` INTEGER NOT NULL, " +
                        "`debutValidite` INTEGER, `finValidite` INTEGER, `actif` INTEGER NOT NULL)",
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_fournisseur_items_fournisseurId_productId` " +
                        "ON `fournisseur_items` (`fournisseurId`, `productId`)",
                )

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `fournisseur_evenements` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`fournisseurId` INTEGER NOT NULL, `date` INTEGER NOT NULL, " +
                        "`type` TEXT NOT NULL, `details` TEXT)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_fournisseur_evenements_fournisseurId` " +
                        "ON `fournisseur_evenements` (`fournisseurId`)",
                )
            }
        }

        /** v18 → v19 : plan, journaux, périodes et pièces à double entrée. */
        val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `accounting_settings` (`id` INTEGER NOT NULL, `standard` TEXT NOT NULL, `countryCode` TEXT NOT NULL, `taxRegime` TEXT, `exerciceStartMonth` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
                db.execSQL("CREATE TABLE IF NOT EXISTS `accounting_accounts` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `standard` TEXT NOT NULL, `code` TEXT NOT NULL, `name` TEXT NOT NULL, `classCode` TEXT NOT NULL, `normalSide` TEXT NOT NULL, `parentCode` TEXT, `postable` INTEGER NOT NULL, `active` INTEGER NOT NULL, `customerAuxiliary` INTEGER NOT NULL, `supplierAuxiliary` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_accounting_accounts_standard_code` ON `accounting_accounts` (`standard`, `code`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_accounting_accounts_classCode` ON `accounting_accounts` (`classCode`)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `accounting_journals` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `code` TEXT NOT NULL, `name` TEXT NOT NULL, `type` TEXT NOT NULL, `active` INTEGER NOT NULL, `requiresApproval` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_accounting_journals_code` ON `accounting_journals` (`code`)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `accounting_periods` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `year` INTEGER NOT NULL, `month` INTEGER NOT NULL, `status` TEXT NOT NULL, `closedAt` INTEGER, `closedBy` INTEGER)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_accounting_periods_year_month` ON `accounting_periods` (`year`, `month`)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `accounting_vouchers` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `journalId` INTEGER NOT NULL, `reference` TEXT NOT NULL, `accountingDate` INTEGER NOT NULL, `documentDate` INTEGER, `sourceModule` TEXT, `sourceDocumentType` TEXT, `sourceDocumentId` INTEGER, `sourceKey` TEXT, `status` TEXT NOT NULL, `description` TEXT NOT NULL, `currencyCode` TEXT NOT NULL, `exchangeRate` REAL NOT NULL, `totalDebit` REAL NOT NULL, `totalCredit` REAL NOT NULL, `createdBy` INTEGER, `validatedBy` INTEGER, `postedAt` INTEGER, `reversedVoucherId` INTEGER, `createdAt` INTEGER NOT NULL, FOREIGN KEY(`journalId`) REFERENCES `accounting_journals`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_accounting_vouchers_reference` ON `accounting_vouchers` (`reference`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_accounting_vouchers_journalId_accountingDate` ON `accounting_vouchers` (`journalId`, `accountingDate`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_accounting_vouchers_sourceKey` ON `accounting_vouchers` (`sourceKey`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_accounting_vouchers_status_accountingDate` ON `accounting_vouchers` (`status`, `accountingDate`)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `accounting_entry_lines` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `voucherId` INTEGER NOT NULL, `lineNumber` INTEGER NOT NULL, `accountId` INTEGER NOT NULL, `label` TEXT NOT NULL, `debitAmount` REAL NOT NULL, `creditAmount` REAL NOT NULL, `currencyAmount` REAL, `currencyCode` TEXT, `customerId` INTEGER, `supplierId` INTEGER, `treasuryAccountId` INTEGER, `taxCodeId` INTEGER, `projectId` INTEGER, `costCenterId` INTEGER, `dueDate` INTEGER, `matchingReference` TEXT, FOREIGN KEY(`voucherId`) REFERENCES `accounting_vouchers`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION, FOREIGN KEY(`accountId`) REFERENCES `accounting_accounts`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_accounting_entry_lines_voucherId_lineNumber` ON `accounting_entry_lines` (`voucherId`, `lineNumber`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_accounting_entry_lines_accountId` ON `accounting_entry_lines` (`accountId`)")
                db.execSQL("CREATE TRIGGER IF NOT EXISTS `accounting_lines_insert_only_draft` BEFORE INSERT ON `accounting_entry_lines` WHEN COALESCE((SELECT `status` FROM `accounting_vouchers` WHERE `id` = NEW.`voucherId`), '') != 'DRAFT' BEGIN SELECT RAISE(ABORT, 'Accounting lines can only be added to a draft'); END")
                db.execSQL("CREATE TRIGGER IF NOT EXISTS `accounting_lines_update_only_draft` BEFORE UPDATE ON `accounting_entry_lines` WHEN COALESCE((SELECT `status` FROM `accounting_vouchers` WHERE `id` = OLD.`voucherId`), '') != 'DRAFT' OR COALESCE((SELECT `status` FROM `accounting_vouchers` WHERE `id` = NEW.`voucherId`), '') != 'DRAFT' BEGIN SELECT RAISE(ABORT, 'Posted accounting lines are immutable'); END")
                db.execSQL("CREATE TRIGGER IF NOT EXISTS `accounting_lines_delete_only_draft` BEFORE DELETE ON `accounting_entry_lines` WHEN COALESCE((SELECT `status` FROM `accounting_vouchers` WHERE `id` = OLD.`voucherId`), '') != 'DRAFT' BEGIN SELECT RAISE(ABORT, 'Posted accounting lines are immutable'); END")
                db.execSQL("CREATE TRIGGER IF NOT EXISTS `accounting_vouchers_immutable` BEFORE UPDATE ON `accounting_vouchers` WHEN OLD.`status` IN ('POSTED', 'REVERSED') AND ((OLD.`status` = 'REVERSED' AND NEW.`status` != 'REVERSED') OR (OLD.`status` = 'POSTED' AND NEW.`status` NOT IN ('POSTED', 'REVERSED')) OR OLD.`id` IS NOT NEW.`id` OR OLD.`journalId` IS NOT NEW.`journalId` OR OLD.`reference` IS NOT NEW.`reference` OR OLD.`accountingDate` IS NOT NEW.`accountingDate` OR OLD.`documentDate` IS NOT NEW.`documentDate` OR OLD.`sourceModule` IS NOT NEW.`sourceModule` OR OLD.`sourceDocumentType` IS NOT NEW.`sourceDocumentType` OR OLD.`sourceDocumentId` IS NOT NEW.`sourceDocumentId` OR OLD.`sourceKey` IS NOT NEW.`sourceKey` OR OLD.`description` IS NOT NEW.`description` OR OLD.`currencyCode` IS NOT NEW.`currencyCode` OR OLD.`exchangeRate` IS NOT NEW.`exchangeRate` OR OLD.`totalDebit` IS NOT NEW.`totalDebit` OR OLD.`totalCredit` IS NOT NEW.`totalCredit` OR OLD.`createdBy` IS NOT NEW.`createdBy` OR OLD.`validatedBy` IS NOT NEW.`validatedBy` OR OLD.`postedAt` IS NOT NEW.`postedAt` OR OLD.`reversedVoucherId` IS NOT NEW.`reversedVoucherId` OR OLD.`createdAt` IS NOT NEW.`createdAt`) BEGIN SELECT RAISE(ABORT, 'Posted accounting vouchers are immutable'); END")
                db.execSQL("CREATE TRIGGER IF NOT EXISTS `accounting_vouchers_no_delete` BEFORE DELETE ON `accounting_vouchers` WHEN OLD.`status` IN ('POSTED', 'REVERSED') BEGIN SELECT RAISE(ABORT, 'Posted accounting vouchers cannot be deleted'); END")
                db.execSQL("CREATE TRIGGER IF NOT EXISTS `accounting_account_identity_immutable_when_used` BEFORE UPDATE OF `code`, `standard`, `name` ON `accounting_accounts` WHEN EXISTS (SELECT 1 FROM `accounting_entry_lines` l JOIN `accounting_vouchers` v ON v.`id` = l.`voucherId` WHERE l.`accountId` = OLD.`id` AND v.`status` IN ('POSTED', 'REVERSED')) BEGIN SELECT RAISE(ABORT, 'Used accounting account codes are immutable'); END")
            }
        }

        val MIGRATION_21_22 = object : Migration(21, 22) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `sync_devices` (`deviceId` TEXT NOT NULL, `nom` TEXT NOT NULL, `publicKey` TEXT NOT NULL DEFAULT '', `actif` INTEGER NOT NULL DEFAULT 1, `creeLe` INTEGER NOT NULL DEFAULT 0, `vuLe` INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(`deviceId`))")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_sync_devices_deviceId` ON `sync_devices` (`deviceId`)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `sync_outbox` (`eventId` TEXT NOT NULL, `deviceId` TEXT NOT NULL, `entrepriseId` TEXT NOT NULL, `aggregateType` TEXT NOT NULL, `aggregateId` TEXT NOT NULL, `operation` TEXT NOT NULL, `payload` TEXT NOT NULL, `revision` INTEGER NOT NULL DEFAULT 0, `creeLe` INTEGER NOT NULL DEFAULT 0, `statut` TEXT NOT NULL DEFAULT 'EN_ATTENTE', `tentatives` INTEGER NOT NULL DEFAULT 0, `prochaineTentative` INTEGER NOT NULL DEFAULT 0, `derniereErreur` TEXT, PRIMARY KEY(`eventId`))")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sync_outbox_statut` ON `sync_outbox` (`statut`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sync_outbox_prochaineTentative` ON `sync_outbox` (`prochaineTentative`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sync_outbox_aggregateType_aggregateId` ON `sync_outbox` (`aggregateType`, `aggregateId`)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `sync_inbox` (`eventId` TEXT NOT NULL, `deviceIdSource` TEXT NOT NULL, `aggregateType` TEXT NOT NULL, `aggregateId` TEXT NOT NULL, `revision` INTEGER NOT NULL DEFAULT 0, `recuLe` INTEGER NOT NULL DEFAULT 0, `appliqueLe` INTEGER, PRIMARY KEY(`eventId`))")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sync_inbox_recuLe` ON `sync_inbox` (`recuLe`)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `sync_conflicts` (`conflictId` TEXT NOT NULL, `eventIdDistant` TEXT NOT NULL, `aggregateType` TEXT NOT NULL, `aggregateId` TEXT NOT NULL, `revisionLocale` INTEGER NOT NULL DEFAULT 0, `revisionDistante` INTEGER NOT NULL DEFAULT 0, `payloadLocal` TEXT NOT NULL, `payloadDistant` TEXT NOT NULL, `detecteLe` INTEGER NOT NULL DEFAULT 0, `resolu` INTEGER NOT NULL DEFAULT 0, `resolution` TEXT, `resoluLe` INTEGER, PRIMARY KEY(`conflictId`))")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sync_conflicts_resolu` ON `sync_conflicts` (`resolu`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sync_conflicts_aggregateType_aggregateId` ON `sync_conflicts` (`aggregateType`, `aggregateId`)")
            }
        }

        val MIGRATION_20_21 = object : Migration(20, 21) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `service_contracts` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `reference` TEXT NOT NULL, `customerId` INTEGER NOT NULL, `name` TEXT NOT NULL, `type` TEXT NOT NULL, `status` TEXT NOT NULL, `startAt` INTEGER NOT NULL, `endAt` INTEGER, `autoRenew` INTEGER NOT NULL, `responseSlaMinutes` INTEGER, `resolutionSlaMinutes` INTEGER, `includedHours` REAL NOT NULL, `includedInterventions` INTEGER NOT NULL, `fixedFee` REAL NOT NULL, `hourlyRate` REAL NOT NULL, `coversParts` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `createdBy` INTEGER, FOREIGN KEY(`customerId`) REFERENCES `clients`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_service_contracts_reference` ON `service_contracts` (`reference`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_service_contracts_customerId_status` ON `service_contracts` (`customerId`, `status`)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `customer_service_assets` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `customerId` INTEGER NOT NULL, `name` TEXT NOT NULL, `type` TEXT NOT NULL, `brand` TEXT, `model` TEXT, `serialNumber` TEXT, `installedAt` INTEGER, `warrantyEndAt` INTEGER, `contractId` INTEGER, `status` TEXT NOT NULL, `notes` TEXT, `createdAt` INTEGER NOT NULL, FOREIGN KEY(`customerId`) REFERENCES `clients`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION, FOREIGN KEY(`contractId`) REFERENCES `service_contracts`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_customer_service_assets_customerId_status` ON `customer_service_assets` (`customerId`, `status`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_customer_service_assets_contractId` ON `customer_service_assets` (`contractId`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_customer_service_assets_serialNumber` ON `customer_service_assets` (`serialNumber`)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `service_requests` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `reference` TEXT NOT NULL, `customerId` INTEGER NOT NULL, `customerName` TEXT NOT NULL, `contactName` TEXT, `contactPhone` TEXT, `address` TEXT, `requestType` TEXT NOT NULL, `channel` TEXT NOT NULL, `priority` TEXT NOT NULL, `status` TEXT NOT NULL, `description` TEXT NOT NULL, `customerAssetId` INTEGER, `contractId` INTEGER, `projectReference` TEXT, `requestedAt` INTEGER, `responseDeadlineAt` INTEGER, `resolutionDeadlineAt` INTEGER, `qualifiedAt` INTEGER, `convertedWorkOrderId` INTEGER, `closedAt` INTEGER, `createdAt` INTEGER NOT NULL, `createdBy` INTEGER, FOREIGN KEY(`customerId`) REFERENCES `clients`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION, FOREIGN KEY(`contractId`) REFERENCES `service_contracts`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION, FOREIGN KEY(`customerAssetId`) REFERENCES `customer_service_assets`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_service_requests_reference` ON `service_requests` (`reference`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_service_requests_status_priority_createdAt` ON `service_requests` (`status`, `priority`, `createdAt`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_service_requests_customerId_createdAt` ON `service_requests` (`customerId`, `createdAt`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_service_requests_contractId` ON `service_requests` (`contractId`)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `service_work_orders` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `reference` TEXT NOT NULL, `requestId` INTEGER, `customerId` INTEGER NOT NULL, `customerName` TEXT NOT NULL, `customerAssetId` INTEGER, `contractId` INTEGER, `interventionType` TEXT NOT NULL, `priority` TEXT NOT NULL, `status` TEXT NOT NULL, `technicianId` INTEGER, `teamName` TEXT, `plannedStartAt` INTEGER, `plannedEndAt` INTEGER, `actualStartAt` INTEGER, `actualEndAt` INTEGER, `responseDeadlineAt` INTEGER, `resolutionDeadlineAt` INTEGER, `address` TEXT, `description` TEXT NOT NULL, `diagnosis` TEXT, `resolution` TEXT, `isBillable` INTEGER NOT NULL, `billingMethod` TEXT NOT NULL, `fixedFee` REAL NOT NULL, `hourlyRate` REAL NOT NULL, `salesInvoiceId` INTEGER, `createdAt` INTEGER NOT NULL, `createdBy` INTEGER, `updatedAt` INTEGER NOT NULL, FOREIGN KEY(`customerId`) REFERENCES `clients`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION, FOREIGN KEY(`requestId`) REFERENCES `service_requests`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION, FOREIGN KEY(`contractId`) REFERENCES `service_contracts`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION, FOREIGN KEY(`customerAssetId`) REFERENCES `customer_service_assets`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION, FOREIGN KEY(`technicianId`) REFERENCES `employees`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_service_work_orders_reference` ON `service_work_orders` (`reference`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_service_work_orders_status_plannedStartAt` ON `service_work_orders` (`status`, `plannedStartAt`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_service_work_orders_technicianId_plannedStartAt_plannedEndAt` ON `service_work_orders` (`technicianId`, `plannedStartAt`, `plannedEndAt`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_service_work_orders_requestId` ON `service_work_orders` (`requestId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_service_work_orders_customerId_createdAt` ON `service_work_orders` (`customerId`, `createdAt`)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `service_timesheets` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `workOrderId` INTEGER NOT NULL, `technicianId` INTEGER NOT NULL, `activityDate` INTEGER NOT NULL, `travelMinutes` INTEGER NOT NULL, `workMinutes` INTEGER NOT NULL, `adminMinutes` INTEGER NOT NULL, `costRate` REAL NOT NULL, `billRate` REAL NOT NULL, `isBillable` INTEGER NOT NULL, `status` TEXT NOT NULL, `note` TEXT, `createdAt` INTEGER NOT NULL, `createdBy` INTEGER, FOREIGN KEY(`workOrderId`) REFERENCES `service_work_orders`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION, FOREIGN KEY(`technicianId`) REFERENCES `employees`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_service_timesheets_workOrderId_activityDate` ON `service_timesheets` (`workOrderId`, `activityDate`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_service_timesheets_technicianId_activityDate` ON `service_timesheets` (`technicianId`, `activityDate`)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `service_reports` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `workOrderId` INTEGER NOT NULL, `status` TEXT NOT NULL, `diagnosis` TEXT NOT NULL, `workPerformed` TEXT NOT NULL, `recommendations` TEXT, `customerResolved` INTEGER NOT NULL, `customerComment` TEXT, `customerSignerName` TEXT, `customerSignatureUri` TEXT, `technicianSignatureUri` TEXT, `signedAt` INTEGER, `submittedAt` INTEGER, `approvedBy` INTEGER, `approvedAt` INTEGER, `pdfUri` TEXT, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, FOREIGN KEY(`workOrderId`) REFERENCES `service_work_orders`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_service_reports_workOrderId` ON `service_reports` (`workOrderId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_service_reports_status_updatedAt` ON `service_reports` (`status`, `updatedAt`)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `service_attachments` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `workOrderId` INTEGER NOT NULL, `kind` TEXT NOT NULL, `localUri` TEXT NOT NULL, `caption` TEXT, `capturedAt` INTEGER NOT NULL, `capturedBy` INTEGER, FOREIGN KEY(`workOrderId`) REFERENCES `service_work_orders`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_service_attachments_workOrderId_capturedAt` ON `service_attachments` (`workOrderId`, `capturedAt`)")
            }
        }

        val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `accounting_posting_rules` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `standard` TEXT NOT NULL, `eventType` TEXT NOT NULL, `journalCode` TEXT NOT NULL, `primaryAccountCode` TEXT NOT NULL, `counterpartAccountCode` TEXT NOT NULL, `primarySide` TEXT NOT NULL, `taxAccountCode` TEXT, `taxSide` TEXT, `active` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_accounting_posting_rules_standard_eventType` ON `accounting_posting_rules` (`standard`, `eventType`)")
            }
        }

        val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE clients ADD COLUMN typeIdentifiantFiscal TEXT")
                db.execSQL("ALTER TABLE clients ADD COLUMN numeroTva TEXT")
                db.execSQL("ALTER TABLE clients ADD COLUMN assujettiTva INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE clients ADD COLUMN exonereTva INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE clients ADD COLUMN motifExoneration TEXT")
                db.execSQL("ALTER TABLE clients ADD COLUMN tauxTva REAL")
                db.execSQL("ALTER TABLE clients ADD COLUMN grilleTarifaire TEXT")
                db.execSQL("ALTER TABLE clients ADD COLUMN remiseMaxPct REAL")
                db.execSQL("ALTER TABLE clients ADD COLUMN segment TEXT")
                db.execSQL("ALTER TABLE clients ADD COLUMN canalVente TEXT")
                db.execSQL("ALTER TABLE clients ADD COLUMN territoire TEXT")
                db.execSQL("ALTER TABLE clients ADD COLUMN compteComptable TEXT")
                db.execSQL("ALTER TABLE clients ADD COLUMN conditionsPaiement TEXT")
            }
        }

        val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE product_stock ADD COLUMN valeur REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE stock_movements ADD COLUMN lot TEXT")
                db.execSQL("ALTER TABLE stock_movements ADD COLUMN numeroSerie TEXT")
                db.execSQL("ALTER TABLE stock_movements ADD COLUMN datePeremption INTEGER")
            }
        }

        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE products ADD COLUMN vendable INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE products ADD COLUMN achetable INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE products ADD COLUMN stockable INTEGER NOT NULL DEFAULT 1")
                db.execSQL(
                    "UPDATE products SET vendable = 0 WHERE type IN " +
                        "('MATIERE_PREMIERE','CONNOMMABLE','PIECE_MAINTENANCE','EQUIPEMENT','MATERIEL','AUTRE_BIEN')",
                )
                db.execSQL("UPDATE products SET achetable = 0 WHERE type IN ('FABRIQUE','COMPOSE')")
                db.execSQL("UPDATE products SET stockable = 0 WHERE type IN ('EQUIPEMENT','MATERIEL')")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `product_dechets` (" +
                        "`produitId` INTEGER NOT NULL PRIMARY KEY, `typeDechet` TEXT, " +
                        "`codeReglementaire` TEXT, `dangereux` INTEGER NOT NULL, " +
                        "`valorisable` INTEGER NOT NULL, `origine` TEXT, `modeElimination` TEXT, " +
                        "`prestataire` TEXT, `coutElimination` REAL, `filiereRecyclage` TEXT, " +
                        "`zoneStockage` TEXT)",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `product_emballages` (" +
                        "`produitId` INTEGER NOT NULL PRIMARY KEY, `typeEmballage` TEXT, " +
                        "`matiere` TEXT, `dimensions` TEXT, `poidsKg` REAL, `capacite` REAL, " +
                        "`reutilisable` INTEGER NOT NULL, `consigne` INTEGER NOT NULL, " +
                        "`reutilisationsMax` INTEGER)",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `product_consignations` (" +
                        "`produitId` INTEGER NOT NULL PRIMARY KEY, `proprietaire` TEXT, " +
                        "`referenceContrat` TEXT, `dateDebut` INTEGER, `dateFin` INTEGER, " +
                        "`conditionsRetour` TEXT)",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `product_kits` (" +
                        "`produitId` INTEGER NOT NULL PRIMARY KEY, `methode` TEXT NOT NULL)",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `kit_composants` (" +
                        "`kitId` INTEGER NOT NULL, `composantId` INTEGER NOT NULL, " +
                        "`quantite` REAL NOT NULL, PRIMARY KEY(`kitId`, `composantId`))",
                )
            }
        }

        /** v13 → v14 : inventaires physiques (sessions + lignes). */
        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `inventaires` (" +
                        "`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                        "`siteId` INTEGER NOT NULL, `debut` INTEGER NOT NULL, " +
                        "`fin` INTEGER, `statut` TEXT NOT NULL)",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `inventaire_lignes` (" +
                        "`inventaireId` INTEGER NOT NULL, `produitId` INTEGER NOT NULL, " +
                        "`attendu` REAL NOT NULL, `compte` REAL, " +
                        "PRIMARY KEY(`inventaireId`, `produitId`))",
                )
            }
        }

        /** v12 → v13 : extension immobilisation des produits (équipements). */
        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `product_equipements` (" +
                        "`produitId` INTEGER NOT NULL PRIMARY KEY, " +
                        "`modele` TEXT, `numeroSerie` TEXT, `dateAcquisition` INTEGER, " +
                        "`prixAquisition` REAL, `fournisseurNom` TEXT, `responsable` TEXT, " +
                        "`garantieDebut` INTEGER, `garantieFin` INTEGER, " +
                        "`statut` TEXT NOT NULL)",
                )
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `item_groups` (" +
                        "`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                        "`code` TEXT NOT NULL, `nom` TEXT NOT NULL, `description` TEXT, " +
                        "`actif` INTEGER NOT NULL, `stocke` INTEGER NOT NULL, " +
                        "`valorise` INTEGER NOT NULL, `immobilisation` INTEGER NOT NULL, " +
                        "`methodeValorisation` TEXT, `createdAt` INTEGER NOT NULL, " +
                        "`updatedAt` INTEGER NOT NULL)",
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_item_groups_code` " +
                        "ON `item_groups` (`code`)",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `item_groups_stock` (" +
                        "`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                        "`itemGroupId` INTEGER NOT NULL, `compteStock` TEXT, `compteEcartInventaire` TEXT, `gestionLotObligatoire` INTEGER NOT NULL, `gestionSerieObligatoire` INTEGER NOT NULL, `gestionPeremptionObligatoire` INTEGER NOT NULL, `stockNegatifAutorise` INTEGER NOT NULL, `seuilReapproDefaut` REAL NOT NULL, " +
                        "FOREIGN KEY(`itemGroupId`) REFERENCES `item_groups`(`id`) " +
                        "ON UPDATE NO ACTION ON DELETE CASCADE)",
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_item_groups_stock_itemGroupId` " +
                        "ON `item_groups_stock` (`itemGroupId`)",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `item_groups_purchase` (" +
                        "`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                        "`itemGroupId` INTEGER NOT NULL, `compteCharge` TEXT, `compteImmobilisation` TEXT, `achetable` INTEGER NOT NULL, `consommable` INTEGER NOT NULL, `immobilisable` INTEGER NOT NULL, `delaiLivraisonJours` INTEGER NOT NULL, " +
                        "FOREIGN KEY(`itemGroupId`) REFERENCES `item_groups`(`id`) " +
                        "ON UPDATE NO ACTION ON DELETE CASCADE)",
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_item_groups_purchase_itemGroupId` " +
                        "ON `item_groups_purchase` (`itemGroupId`)",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `item_groups_sales` (" +
                        "`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                        "`itemGroupId` INTEGER NOT NULL, `compteProduit` TEXT, `vendable` INTEGER NOT NULL, `service` INTEGER NOT NULL, `soumisTaxe` INTEGER NOT NULL, `livraisonRequise` INTEGER NOT NULL, `garantieApplicable` INTEGER NOT NULL, `garantieMois` INTEGER NOT NULL, " +
                        "FOREIGN KEY(`itemGroupId`) REFERENCES `item_groups`(`id`) " +
                        "ON UPDATE NO ACTION ON DELETE CASCADE)",
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_item_groups_sales_itemGroupId` " +
                        "ON `item_groups_sales` (`itemGroupId`)",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `item_groups_production` (" +
                        "`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                        "`itemGroupId` INTEGER NOT NULL, `produisible` INTEGER NOT NULL, `fantome` INTEGER NOT NULL, `coProduit` INTEGER NOT NULL, `sousProduit` INTEGER NOT NULL, `gammeRequise` INTEGER NOT NULL, `nomenclatureRequise` INTEGER NOT NULL, `soustraitable` INTEGER NOT NULL, " +
                        "FOREIGN KEY(`itemGroupId`) REFERENCES `item_groups`(`id`) " +
                        "ON UPDATE NO ACTION ON DELETE CASCADE)",
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_item_groups_production_itemGroupId` " +
                        "ON `item_groups_production` (`itemGroupId`)",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `item_groups_maintenance` (" +
                        "`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                        "`itemGroupId` INTEGER NOT NULL, `equipementMaintenable` INTEGER NOT NULL, `planRequis` INTEGER NOT NULL, `suiviHeures` INTEGER NOT NULL, `suiviCompteur` INTEGER NOT NULL, `critiqueSecurite` INTEGER NOT NULL, `etalonnageRequis` INTEGER NOT NULL, `mtbfHeures` REAL NOT NULL, " +
                        "FOREIGN KEY(`itemGroupId`) REFERENCES `item_groups`(`id`) " +
                        "ON UPDATE NO ACTION ON DELETE CASCADE)",
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_item_groups_maintenance_itemGroupId` " +
                        "ON `item_groups_maintenance` (`itemGroupId`)",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `item_groups_accounting` (" +
                        "`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                        "`itemGroupId` INTEGER NOT NULL, `categorieTaxe` TEXT, `amortissable` INTEGER NOT NULL, `methodeAmortissement` TEXT, `dureeAmortissementAnnees` INTEGER NOT NULL, `stockValorise` INTEGER NOT NULL, `centreCout` TEXT, `centreProfit` TEXT, " +
                        "FOREIGN KEY(`itemGroupId`) REFERENCES `item_groups`(`id`) " +
                        "ON UPDATE NO ACTION ON DELETE CASCADE)",
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_item_groups_accounting_itemGroupId` " +
                        "ON `item_groups_accounting` (`itemGroupId`)",
                )
                db.execSQL("ALTER TABLE `products` ADD COLUMN `itemGroupId` INTEGER")
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `operation_records` ADD COLUMN `tiersId` INTEGER")
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_operation_records_tiersId` " +
                        "ON `operation_records` (`tiersId`)",
                )
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `qualite_non_conformites` (" +
                        "`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                        "`date` INTEGER NOT NULL, `titre` TEXT NOT NULL, `description` TEXT, " +
                        "`gravite` TEXT NOT NULL, `origine` TEXT NOT NULL, `statut` TEXT NOT NULL, " +
                        "`reference` TEXT, `responsable` TEXT, `actionCorrective` TEXT, " +
                        "`cout` REAL NOT NULL, `dateResolution` INTEGER, `createdAt` INTEGER NOT NULL)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_qualite_non_conformites_statut` " +
                        "ON `qualite_non_conformites` (`statut`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_qualite_non_conformites_date` " +
                        "ON `qualite_non_conformites` (`date`)",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `maintenance_equipements` (" +
                        "`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                        "`nom` TEXT NOT NULL, `code` TEXT, `type` TEXT NOT NULL, " +
                        "`siteId` INTEGER, `dateMiseEnService` INTEGER, " +
                        "`periodiciteJours` INTEGER NOT NULL, `actif` INTEGER NOT NULL, " +
                        "`notes` TEXT, `createdAt` INTEGER NOT NULL)",
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_maintenance_equipements_nom` " +
                        "ON `maintenance_equipements` (`nom`)",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `maintenance_interventions` (" +
                        "`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                        "`equipementId` INTEGER NOT NULL, `date` INTEGER NOT NULL, " +
                        "`type` TEXT NOT NULL, `description` TEXT NOT NULL, `technicien` TEXT, " +
                        "`cout` REAL NOT NULL, `dureeHeures` REAL NOT NULL, " +
                        "`statut` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_maintenance_interventions_equipementId` " +
                        "ON `maintenance_interventions` (`equipementId`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_maintenance_interventions_date` " +
                        "ON `maintenance_interventions` (`date`)",
                )
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `tresorerie_comptes` (" +
                        "`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                        "`nom` TEXT NOT NULL, `type` TEXT NOT NULL, `etablissement` TEXT, " +
                        "`numero` TEXT, `soldeInitial` REAL NOT NULL, " +
                        "`actif` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)",
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_tresorerie_comptes_nom` " +
                        "ON `tresorerie_comptes` (`nom`)",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `tresorerie_mouvements` (" +
                        "`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                        "`compteId` INTEGER NOT NULL, `date` INTEGER NOT NULL, " +
                        "`sens` TEXT NOT NULL, `montant` REAL NOT NULL, " +
                        "`categorie` TEXT NOT NULL, `libelle` TEXT NOT NULL, " +
                        "`tiers` TEXT, `modePaiement` TEXT, `reference` TEXT, " +
                        "`transfertId` TEXT, `rapproche` INTEGER NOT NULL, " +
                        "`notes` TEXT, `createdAt` INTEGER NOT NULL)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_tresorerie_mouvements_compteId` " +
                        "ON `tresorerie_mouvements` (`compteId`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_tresorerie_mouvements_date` " +
                        "ON `tresorerie_mouvements` (`date`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_tresorerie_mouvements_transfertId` " +
                        "ON `tresorerie_mouvements` (`transfertId`)",
                )
            }
        }

        /**
         * v22 → v23 (module Clients) : situation de compte dérivée, journal de suivi et
         * encaissements postérieurs à la facture. Aucune donnée existante n'est modifiée ;
         * `client_balances` est remplie au premier lancement par `ClientBalanceUseCase`.
         */
        val MIGRATION_22_23 = object : Migration(22, 23) {
            override fun migrate(db: SupportSQLiteDatabase) {
                CLIENT_ACCOUNT_STATEMENTS.forEach { db.execSQL(it) }
            }
        }

        /** Instructions de [MIGRATION_22_23], exposées pour le test de conformité au schéma exporté. */
        val CLIENT_ACCOUNT_STATEMENTS: List<String> = listOf(
            "CREATE TABLE IF NOT EXISTS `client_balances` (" +
                "`clientId` INTEGER NOT NULL, " +
                "`encours` REAL NOT NULL DEFAULT 0, " +
                "`enRetard` REAL NOT NULL DEFAULT 0, " +
                "`joursRetardMax` INTEGER NOT NULL DEFAULT 0, " +
                "`ca12Mois` REAL NOT NULL DEFAULT 0, " +
                "`derniereVenteAt` INTEGER, " +
                "`nbVentes` INTEGER NOT NULL DEFAULT 0, " +
                "`majAt` INTEGER NOT NULL DEFAULT 0, " +
                "PRIMARY KEY(`clientId`), " +
                "FOREIGN KEY(`clientId`) REFERENCES `clients`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION )",
            "CREATE INDEX IF NOT EXISTS `index_client_balances_encours` ON `client_balances` (`encours`)",
            "CREATE INDEX IF NOT EXISTS `index_client_balances_enRetard` ON `client_balances` (`enRetard`)",
            "CREATE TABLE IF NOT EXISTS `client_followups` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`clientId` INTEGER NOT NULL, " +
                "`type` TEXT NOT NULL, " +
                "`canal` TEXT, " +
                "`message` TEXT, " +
                "`promesseDate` INTEGER, " +
                "`promesseMontant` REAL, " +
                "`statut` TEXT NOT NULL DEFAULT 'OUVERT', " +
                "`createdAt` INTEGER NOT NULL, " +
                "FOREIGN KEY(`clientId`) REFERENCES `clients`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION )",
            "CREATE INDEX IF NOT EXISTS `index_client_followups_clientId_createdAt` " +
                "ON `client_followups` (`clientId`, `createdAt`)",
            "CREATE INDEX IF NOT EXISTS `index_client_followups_type_statut` " +
                "ON `client_followups` (`type`, `statut`)",
            "CREATE TABLE IF NOT EXISTS `client_payments` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`clientId` INTEGER NOT NULL, " +
                "`invoiceRecordId` INTEGER, " +
                "`montant` REAL NOT NULL, " +
                "`modePaiement` TEXT NOT NULL, " +
                "`reference` TEXT NOT NULL, " +
                "`paiementAt` INTEGER NOT NULL, " +
                "`contrePassationDe` INTEGER, " +
                "`note` TEXT, " +
                "`createdAt` INTEGER NOT NULL, " +
                "FOREIGN KEY(`clientId`) REFERENCES `clients`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION )",
            "CREATE INDEX IF NOT EXISTS `index_client_payments_clientId_paiementAt` " +
                "ON `client_payments` (`clientId`, `paiementAt`)",
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_client_payments_reference` ON `client_payments` (`reference`)",
            "CREATE INDEX IF NOT EXISTS `index_client_payments_invoiceRecordId` " +
                "ON `client_payments` (`invoiceRecordId`)",
        )

        /**
         * 23 → 24 (module Fournisseurs) : trois nouvelles tables dérivées (`fournisseur_balances`,
         * `fournisseur_scores`, `fournisseur_paiements_planifies`) et deux colonnes ajoutées avec une
         * valeur par défaut (`fournisseur_documents.archive`, `fournisseur_comptes_bancaires.modifieLe`).
         * Aucune table existante n'est réécrite ni supprimée ; les caches sont remplis au premier
         * lancement par `FournisseurCacheUseCase`.
         */
        val MIGRATION_23_24 = object : Migration(23, 24) {
            override fun migrate(db: SupportSQLiteDatabase) {
                FOURNISSEUR_STATEMENTS.forEach { db.execSQL(it) }
            }
        }

        /** Instructions de [MIGRATION_23_24], exposées pour le test de conformité au schéma exporté. */
        val FOURNISSEUR_STATEMENTS: List<String> = listOf(
            "ALTER TABLE `fournisseur_documents` ADD COLUMN `archive` INTEGER NOT NULL DEFAULT 0",
            "ALTER TABLE `fournisseur_comptes_bancaires` ADD COLUMN `modifieLe` INTEGER NOT NULL DEFAULT 0",
            "CREATE TABLE IF NOT EXISTS `fournisseur_balances` (" +
                "`fournisseurId` INTEGER NOT NULL, " +
                "`dette` REAL NOT NULL DEFAULT 0, " +
                "`enRetard` REAL NOT NULL DEFAULT 0, " +
                "`joursRetardMax` INTEGER NOT NULL DEFAULT 0, " +
                "`nbFacturesOuvertes` INTEGER NOT NULL DEFAULT 0, " +
                "`nbCommandesOuvertes` INTEGER NOT NULL DEFAULT 0, " +
                "`achats12Mois` REAL NOT NULL DEFAULT 0, " +
                "`derniereFactureAt` INTEGER, " +
                "`prochaineEcheanceAt` INTEGER, " +
                "`majAt` INTEGER NOT NULL DEFAULT 0, " +
                "PRIMARY KEY(`fournisseurId`), " +
                "FOREIGN KEY(`fournisseurId`) REFERENCES `fournisseurs`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION )",
            "CREATE TABLE IF NOT EXISTS `fournisseur_scores` (" +
                "`fournisseurId` INTEGER NOT NULL, " +
                "`ponctualite` REAL, " +
                "`conformite` REAL, " +
                "`prix` REAL, " +
                "`score` INTEGER, " +
                "`nbCommandesMesurees` INTEGER NOT NULL DEFAULT 0, " +
                "`delaiMoyenReelJours` REAL, " +
                "`majAt` INTEGER NOT NULL DEFAULT 0, " +
                "PRIMARY KEY(`fournisseurId`), " +
                "FOREIGN KEY(`fournisseurId`) REFERENCES `fournisseurs`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION )",
            "CREATE TABLE IF NOT EXISTS `fournisseur_paiements_planifies` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`fournisseurId` INTEGER NOT NULL, " +
                "`factureRecordId` INTEGER NOT NULL, " +
                "`datePrevue` INTEGER NOT NULL, " +
                "`montant` REAL NOT NULL, " +
                "`statut` TEXT NOT NULL DEFAULT 'PLANIFIE', " +
                "`referenceMouvement` TEXT, " +
                "`createdAt` INTEGER NOT NULL, " +
                "FOREIGN KEY(`fournisseurId`) REFERENCES `fournisseurs`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION )",
            "CREATE INDEX IF NOT EXISTS `index_fournisseur_paiements_planifies_fournisseurId_datePrevue` " +
                "ON `fournisseur_paiements_planifies` (`fournisseurId`, `datePrevue`)",
            "CREATE INDEX IF NOT EXISTS `index_fournisseur_paiements_planifies_statut_datePrevue` " +
                "ON `fournisseur_paiements_planifies` (`statut`, `datePrevue`)",
        )


        /** Toutes les migrations, dans l'ordre : `DatabaseModule` les enregistre telles quelles. */
        val ALL_MIGRATIONS: Array<Migration> = arrayOf(
            MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7,
            MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12,
            MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17,
            MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20, MIGRATION_20_21, MIGRATION_21_22,
            MIGRATION_22_23, MIGRATION_23_24,
        )
    }
}
