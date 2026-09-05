package com.missa.b360.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.missa.b360.core.data.entity.BackupEntity
import com.missa.b360.core.data.entity.EnterpriseEntity
import com.missa.b360.core.data.entity.JournalEntryEntity
import com.missa.b360.core.data.entity.LicenceEntity
import com.missa.b360.core.data.entity.NotificationEntity
import com.missa.b360.core.data.entity.PaymentMethodEntity
import com.missa.b360.core.data.entity.RoleEntity
import com.missa.b360.core.data.entity.RolePermissionEntity
import com.missa.b360.core.data.entity.SequenceEntity
import com.missa.b360.core.data.entity.SettingEntity
import com.missa.b360.core.data.entity.SiteEntity
import com.missa.b360.core.data.entity.TaxEntity
import com.missa.b360.core.data.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EnterpriseDao {
    @Query("SELECT * FROM enterprise WHERE id = 1")
    suspend fun get(): EnterpriseEntity?

    /** Flux réactif des informations affichées dans l'en-tête et le tableau de bord. */
    @Query("SELECT * FROM enterprise WHERE id = 1")
    fun observe(): Flow<EnterpriseEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(enterprise: EnterpriseEntity)
}

@Dao
interface SiteDao {
    @Query("SELECT * FROM sites ORDER BY nom")
    fun observeAll(): Flow<List<SiteEntity>>

    @Query("SELECT COUNT(*) FROM sites")
    suspend fun count(): Int

    @Query("SELECT nom FROM sites WHERE id = :id LIMIT 1")
    suspend fun getNomById(id: Long): String?

    @Insert
    suspend fun insert(site: SiteEntity): Long

    @Update
    suspend fun update(site: SiteEntity)

    @Query("DELETE FROM sites WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface UserDao {
    @Query("SELECT * FROM users ORDER BY id")
    fun observeAll(): Flow<List<UserEntity>>

    @Query("SELECT COUNT(*) FROM users")
    suspend fun count(): Int

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getById(id: Long): UserEntity?

    @Query("SELECT * FROM users WHERE emailSecours = :email LIMIT 1")
    suspend fun findByEmail(email: String): UserEntity?

    @Insert
    suspend fun insert(user: UserEntity): Long

    @Update
    suspend fun update(user: UserEntity)
}

@Dao
interface RoleDao {
    @Query("SELECT * FROM roles ORDER BY id")
    fun observeAll(): Flow<List<RoleEntity>>

    @Query("SELECT * FROM roles WHERE id = :id")
    suspend fun getById(id: Long): RoleEntity?

    @Query("SELECT * FROM roles WHERE nom = :nom LIMIT 1")
    suspend fun getByNom(nom: String): RoleEntity?

    @Insert
    suspend fun insert(role: RoleEntity): Long

    @Query("SELECT * FROM role_permissions WHERE roleId = :roleId")
    suspend fun permissionsFor(roleId: Long): List<RolePermissionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPermission(permission: RolePermissionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPermissions(permissions: List<RolePermissionEntity>)
}

@Dao
interface LicenceDao {
    @Query("SELECT * FROM licence WHERE id = 1")
    suspend fun get(): LicenceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(licence: LicenceEntity)
}

@Dao
interface SequenceDao {
    @Query("SELECT compteur FROM sequences WHERE docType = :docType AND annee = :annee")
    suspend fun compteur(docType: String, annee: Int): Long?

    @Query("INSERT INTO sequences (docType, annee, compteur) VALUES (:docType, :annee, 0) ON CONFLICT(docType, annee) DO NOTHING")
    suspend fun ensureRow(docType: String, annee: Int)

    @Query("UPDATE sequences SET compteur = compteur + 1 WHERE docType = :docType AND annee = :annee")
    suspend fun increment(docType: String, annee: Int)

    @Query("SELECT compteur FROM sequences WHERE docType = :docType AND annee = :annee")
    suspend fun compteurApresIncrement(docType: String, annee: Int): Long?
}

@Dao
interface TaxDao {
    @Query("SELECT * FROM taxes")
    fun observeAll(): Flow<List<TaxEntity>>

    /** Taxe proposée lors de l'onboarding, utilisée pour restaurer une reprise interrompue. */
    @Query("SELECT * FROM taxes WHERE parDefaut = 1 LIMIT 1")
    suspend fun getParDefaut(): TaxEntity?

    @Query("SELECT COUNT(*) FROM taxes")
    suspend fun count(): Int

    @Insert
    suspend fun insertAll(taxes: List<TaxEntity>)
}

@Dao
interface PaymentMethodDao {
    @Query("SELECT * FROM payment_methods")
    fun observeAll(): Flow<List<PaymentMethodEntity>>

    @Query("SELECT COUNT(*) FROM payment_methods")
    suspend fun count(): Int

    @Insert
    suspend fun insertAll(methods: List<PaymentMethodEntity>)
}

@Dao
interface SettingDao {
    @Query("SELECT * FROM settings WHERE cle = :cle")
    suspend fun get(cle: String): SettingEntity?

    @Query("SELECT * FROM settings")
    suspend fun getAll(): List<SettingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(setting: SettingEntity)
}

@Dao
interface BackupDao {
    @Query("SELECT * FROM backups ORDER BY date DESC")
    fun observeAll(): Flow<List<BackupEntity>>

    @Insert
    suspend fun insert(backup: BackupEntity): Long
}

@Dao
interface JournalDao {
    /** Écriture seule : le journal est immuable (RA-18) — aucune UPDATE. */
    @Insert
    suspend fun insert(entry: JournalEntryEntity): Long

    @Query("SELECT * FROM journal ORDER BY horodatage DESC LIMIT :limit")
    fun observeRecent(limit: Int = 200): Flow<List<JournalEntryEntity>>

    /** Purge automatique des entrées de plus de 12 mois (RA-18). */
    @Query("DELETE FROM journal WHERE horodatage < :limiteHorodatage")
    suspend fun purgeAvant(limiteHorodatage: Long): Int
}

@Dao
interface NotificationDao {
    @Insert
    suspend fun insert(notification: NotificationEntity): Long

    @Query("SELECT * FROM notifications ORDER BY date DESC")
    fun observeAll(): Flow<List<NotificationEntity>>

    @Query("SELECT COUNT(*) FROM notifications WHERE lue = 0")
    fun observeNonLues(): Flow<Int>

    @Query("UPDATE notifications SET lue = 1 WHERE id = :id")
    suspend fun marquerLue(id: Long)

    @Query("UPDATE notifications SET lue = 1")
    suspend fun marquerToutesLues()
}
