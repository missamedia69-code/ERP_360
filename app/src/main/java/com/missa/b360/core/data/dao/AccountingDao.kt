package com.missa.b360.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.missa.b360.core.data.entity.AccountingAccountEntity
import com.missa.b360.core.data.entity.AccountingEntryLineEntity
import com.missa.b360.core.data.entity.AccountingJournalEntity
import com.missa.b360.core.data.entity.AccountingPeriodEntity
import com.missa.b360.core.data.entity.AccountingPostingRuleEntity
import com.missa.b360.core.data.entity.AccountingSettingsEntity
import com.missa.b360.core.data.entity.AccountingVoucherEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountingDao {
    @Query("SELECT * FROM accounting_settings WHERE id = 1")
    fun observeSettings(): Flow<AccountingSettingsEntity?>

    @Query("SELECT * FROM accounting_settings WHERE id = 1")
    suspend fun getSettings(): AccountingSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: AccountingSettingsEntity)

    @Query("SELECT * FROM accounting_accounts WHERE standard = :standard ORDER BY code")
    fun observeAccounts(standard: String): Flow<List<AccountingAccountEntity>>

    @Query("SELECT * FROM accounting_accounts WHERE id = :id LIMIT 1")
    suspend fun getAccount(id: Long): AccountingAccountEntity?

    @Query("SELECT * FROM accounting_accounts WHERE standard = :standard AND code = :code LIMIT 1")
    suspend fun getAccountByCode(standard: String, code: String): AccountingAccountEntity?

    @Query("SELECT COUNT(*) FROM accounting_accounts WHERE standard = :standard")
    suspend fun countAccounts(standard: String): Int

    @Query("SELECT COUNT(*) FROM accounting_accounts")
    suspend fun countAllAccounts(): Int

    @Query("SELECT COUNT(*) FROM accounting_vouchers")
    suspend fun countAllVouchers(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAccounts(accounts: List<AccountingAccountEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAccount(account: AccountingAccountEntity): Long

    @Update
    suspend fun updateAccount(account: AccountingAccountEntity)

    @Query("SELECT * FROM accounting_posting_rules WHERE standard = :standard AND eventType = :eventType LIMIT 1")
    suspend fun getPostingRule(standard: String, eventType: String): AccountingPostingRuleEntity?

    @Query("SELECT * FROM accounting_posting_rules WHERE standard = :standard ORDER BY eventType")
    fun observePostingRules(standard: String): Flow<List<AccountingPostingRuleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun savePostingRule(rule: AccountingPostingRuleEntity)

    @Query("SELECT * FROM accounting_journals WHERE active = 1 ORDER BY code")
    fun observeJournals(): Flow<List<AccountingJournalEntity>>

    @Query("SELECT * FROM accounting_journals WHERE id = :id LIMIT 1")
    suspend fun getJournal(id: Long): AccountingJournalEntity?

    @Query("SELECT * FROM accounting_journals WHERE code = :code LIMIT 1")
    suspend fun getJournalByCode(code: String): AccountingJournalEntity?

    @Query("SELECT COUNT(*) FROM accounting_journals")
    suspend fun countJournals(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertJournals(journals: List<AccountingJournalEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertJournal(journal: AccountingJournalEntity): Long

    @Query("SELECT * FROM accounting_vouchers ORDER BY accountingDate DESC, id DESC")
    fun observeVouchers(): Flow<List<AccountingVoucherEntity>>

    @Query("SELECT * FROM accounting_vouchers WHERE id = :id LIMIT 1")
    suspend fun getVoucher(id: Long): AccountingVoucherEntity?

    @Query("SELECT * FROM accounting_vouchers WHERE sourceKey = :sourceKey LIMIT 1")
    suspend fun getVoucherBySourceKey(sourceKey: String): AccountingVoucherEntity?

    @Query("SELECT COUNT(*) FROM accounting_vouchers WHERE accountingDate BETWEEN :start AND :end AND status IN ('DRAFT', 'TO_VALIDATE')")
    suspend fun countPendingVouchers(start: Long, end: Long): Int

    @Query("SELECT COUNT(*) FROM accounting_vouchers WHERE accountingDate BETWEEN :start AND :end AND status = 'POSTED'")
    suspend fun countPostedVouchers(start: Long, end: Long): Int

    @Query("SELECT * FROM accounting_entry_lines WHERE voucherId = :voucherId ORDER BY lineNumber")
    fun observeLines(voucherId: Long): Flow<List<AccountingEntryLineEntity>>

    @Query("SELECT * FROM accounting_entry_lines WHERE voucherId = :voucherId ORDER BY lineNumber")
    suspend fun getLines(voucherId: Long): List<AccountingEntryLineEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertVoucher(voucher: AccountingVoucherEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertLines(lines: List<AccountingEntryLineEntity>)

    @Update
    suspend fun updateVoucher(voucher: AccountingVoucherEntity)

    @Query("SELECT * FROM accounting_periods WHERE year = :year AND month = :month LIMIT 1")
    suspend fun getPeriod(year: Int, month: Int): AccountingPeriodEntity?

    @Query("SELECT * FROM accounting_periods ORDER BY year DESC, month DESC")
    fun observePeriods(): Flow<List<AccountingPeriodEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPeriod(period: AccountingPeriodEntity): Long

    @Update
    suspend fun updatePeriod(period: AccountingPeriodEntity)


}
