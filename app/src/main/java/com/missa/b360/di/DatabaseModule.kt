package com.missa.b360.di

import android.content.Context
import androidx.room.Room
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
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4)
            .fallbackToDestructiveMigration(false)
            .build()

    @Provides fun provideEnterpriseDao(db: AppDatabase): EnterpriseDao = db.enterpriseDao()
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
    @Provides fun provideOperationRecordDao(db: AppDatabase): OperationRecordDao = db.operationRecordDao()
}
