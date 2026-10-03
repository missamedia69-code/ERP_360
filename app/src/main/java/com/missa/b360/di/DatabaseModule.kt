package com.missa.b360.di

import android.content.Context
import androidx.room.Room
import com.missa.b360.core.data.dao.AbsenceDao
import com.missa.b360.core.data.dao.AccountingDao
import com.missa.b360.core.data.dao.BackupDao
import com.missa.b360.core.data.dao.ClientBalanceDao
import com.missa.b360.core.data.dao.ClientDao
import com.missa.b360.core.data.dao.ClientFollowupDao
import com.missa.b360.core.data.dao.ClientPaymentDao
import com.missa.b360.core.data.dao.CompteTresorerieDao
import com.missa.b360.core.data.dao.EquipementDao
import com.missa.b360.core.data.dao.GroupeArticleDao
import com.missa.b360.core.data.dao.InterventionDao
import com.missa.b360.core.data.dao.NonConformiteDao
import com.missa.b360.core.data.dao.EmployeeDao
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
import com.missa.b360.core.data.dao.NotificationDao
import com.missa.b360.core.data.dao.OperationRecordDao
import com.missa.b360.core.data.dao.PaymentMethodDao
import com.missa.b360.core.data.dao.ProductDao
import com.missa.b360.core.data.dao.ProductExtrasDao
import com.missa.b360.core.data.dao.ProductStockDao
import com.missa.b360.core.data.dao.RoleDao
import com.missa.b360.core.data.dao.StockMovementDao
import com.missa.b360.core.data.dao.SequenceDao
import com.missa.b360.core.data.dao.SettingDao
import com.missa.b360.core.data.dao.SiteDao
import com.missa.b360.core.data.dao.SyncDao
import com.missa.b360.core.data.dao.ServiceWorkflowDao
import com.missa.b360.core.data.dao.TaskDao
import com.missa.b360.core.data.dao.TaxDao
import com.missa.b360.core.data.dao.UserDao
import com.missa.b360.core.data.db.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Fournit la base Room offline et les DAOs (aucun accès direct à la BD hors couche data). */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "missa_b360.db")
            .addMigrations(*AppDatabase.ALL_MIGRATIONS)
            .build()

    @Provides fun provideSyncDao(db: AppDatabase): SyncDao = db.syncDao()
    @Provides fun provideClientBalanceDao(db: AppDatabase): ClientBalanceDao = db.clientBalanceDao()
    @Provides fun provideFournisseurBalanceDao(db: AppDatabase): FournisseurBalanceDao = db.fournisseurBalanceDao()
    @Provides fun provideFournisseurScoreDao(db: AppDatabase): FournisseurScoreDao = db.fournisseurScoreDao()
    @Provides fun provideFournisseurPaiementPlanifieDao(db: AppDatabase): FournisseurPaiementPlanifieDao =
        db.fournisseurPaiementPlanifieDao()
    @Provides fun provideClientFollowupDao(db: AppDatabase): ClientFollowupDao = db.clientFollowupDao()
    @Provides fun provideClientPaymentDao(db: AppDatabase): ClientPaymentDao = db.clientPaymentDao()
    @Provides fun provideGroupeArticleDao(db: AppDatabase): GroupeArticleDao = db.groupeArticleDao()
    @Provides fun provideNonConformiteDao(db: AppDatabase): NonConformiteDao = db.nonConformiteDao()
    @Provides fun provideEquipementDao(db: AppDatabase): EquipementDao = db.equipementDao()
    @Provides fun provideProductExtrasDao(db: AppDatabase): ProductExtrasDao = db.productExtrasDao()
    @Provides fun provideInterventionDao(db: AppDatabase): InterventionDao = db.interventionDao()
    @Provides fun provideCompteTresorerieDao(db: AppDatabase): CompteTresorerieDao =
        db.compteTresorerieDao()
    @Provides fun provideMouvementTresorerieDao(db: AppDatabase): MouvementTresorerieDao =
        db.mouvementTresorerieDao()
    @Provides fun provideEnterpriseDao(db: AppDatabase): EnterpriseDao = db.enterpriseDao()
    @Provides fun provideAccountingDao(db: AppDatabase): AccountingDao = db.accountingDao()
    @Provides fun provideSiteDao(db: AppDatabase): SiteDao = db.siteDao()
    @Provides fun provideUserDao(db: AppDatabase): UserDao = db.userDao()
    @Provides fun provideRoleDao(db: AppDatabase): RoleDao = db.roleDao()
    @Provides fun provideLicenceDao(db: AppDatabase): LicenceDao = db.licenceDao()
    @Provides fun provideSequenceDao(db: AppDatabase): SequenceDao = db.sequenceDao()
    @Provides fun provideTaxDao(db: AppDatabase): TaxDao = db.taxDao()
    @Provides fun providePaymentMethodDao(db: AppDatabase): PaymentMethodDao = db.paymentMethodDao()
    @Provides fun provideSettingDao(db: AppDatabase): SettingDao = db.settingDao()
    @Provides fun provideBackupDao(db: AppDatabase): BackupDao = db.backupDao()
    @Provides fun provideJournalDao(db: AppDatabase): JournalDao = db.journalDao()
    @Provides fun provideNotificationDao(db: AppDatabase): NotificationDao = db.notificationDao()
    @Provides fun provideClientDao(db: AppDatabase): ClientDao = db.clientDao()
    @Provides fun provideFournisseurDao(db: AppDatabase): FournisseurDao = db.fournisseurDao()
    @Provides fun provideFournisseurContactDao(db: AppDatabase): FournisseurContactDao =
        db.fournisseurContactDao()
    @Provides fun provideFournisseurCompteBancaireDao(db: AppDatabase): FournisseurCompteBancaireDao =
        db.fournisseurCompteBancaireDao()
    @Provides fun provideFournisseurDocumentDao(db: AppDatabase): FournisseurDocumentDao =
        db.fournisseurDocumentDao()
    @Provides fun provideFournisseurItemDao(db: AppDatabase): FournisseurItemDao =
        db.fournisseurItemDao()
    @Provides fun provideFournisseurEvenementDao(db: AppDatabase): FournisseurEvenementDao =
        db.fournisseurEvenementDao()
    @Provides fun provideOperationRecordDao(db: AppDatabase): OperationRecordDao = db.operationRecordDao()
    @Provides fun provideServiceWorkflowDao(db: AppDatabase): ServiceWorkflowDao = db.serviceWorkflowDao()
    @Provides fun provideProductDao(db: AppDatabase): ProductDao = db.productDao()
    @Provides fun provideProductStockDao(db: AppDatabase): ProductStockDao = db.productStockDao()
    @Provides fun provideStockMovementDao(db: AppDatabase): StockMovementDao = db.stockMovementDao()
    @Provides fun provideProductEquipementDao(db: AppDatabase): com.missa.b360.core.data.dao.ProductEquipementDao = db.productEquipementDao()
    @Provides fun provideInventaireDao(db: AppDatabase): com.missa.b360.core.data.dao.InventaireDao = db.inventaireDao()
    @Provides fun provideEmployeeDao(db: AppDatabase): EmployeeDao = db.employeeDao()
    @Provides fun provideAbsenceDao(db: AppDatabase): AbsenceDao = db.absenceDao()
    @Provides fun provideTaskDao(db: AppDatabase): TaskDao = db.taskDao()
}
