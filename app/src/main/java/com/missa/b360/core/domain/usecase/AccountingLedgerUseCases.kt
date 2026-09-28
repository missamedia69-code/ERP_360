package com.missa.b360.core.domain.usecase

import androidx.room.withTransaction
import com.missa.b360.core.data.dao.AccountingDao
import com.missa.b360.core.data.dao.UserDao
import com.missa.b360.core.data.datastore.SettingsStore
import com.missa.b360.core.data.db.AppDatabase
import com.missa.b360.core.data.entity.AccountingAccountEntity
import com.missa.b360.core.data.entity.AccountingEntryLineEntity
import com.missa.b360.core.data.entity.AccountingJournalEntity
import com.missa.b360.core.data.entity.AccountingPeriodEntity
import com.missa.b360.core.data.entity.AccountingPeriodStatus
import com.missa.b360.core.data.entity.AccountingPostingRuleEntity
import com.missa.b360.core.data.entity.AccountingSettingsEntity
import com.missa.b360.core.data.entity.AccountingSide
import com.missa.b360.core.data.entity.AccountingStandard
import com.missa.b360.core.data.entity.AccountingVoucherEntity
import com.missa.b360.core.data.entity.AccountingVoucherStatus
import com.missa.b360.core.domain.model.AccountingRules
import com.missa.b360.core.journal.JournalManager
import com.missa.b360.core.licensing.LicenceManager
import com.missa.b360.core.numbering.DocType
import com.missa.b360.core.numbering.SequenceManager
import com.missa.b360.core.permissions.PermissionChecker
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import java.util.Calendar
import javax.inject.Inject

class AccountingPermissionGate @Inject constructor(
    private val settingsStore: SettingsStore,
    private val userDao: UserDao,
    private val permissionChecker: PermissionChecker,
) {
    suspend fun currentUserId(): Long? {
        val id = settingsStore.getLong(SettingsStore.Keys.CURRENT_USER_ID) ?: return null
        return id.takeIf { userDao.getById(it)?.actif == true }
    }

    suspend fun autorise(action: PermissionChecker.Action): Boolean {
        val id = currentUserId() ?: return false
        val user = userDao.getById(id) ?: return false
        return permissionChecker.hasPermission(user.roleId, "COMPTABILITE", action)
    }
}

/** Proposition d'écriture venant d'un événement source. Le noyau CPT valide les comptes et l'équilibre. */
data class AccountingPostingEvent(
    val eventType: String,
    val idempotencyKey: String,
    val date: Long,
    val description: String,
    val currencyCode: String,
    val sourceModule: String,
    val sourceDocumentId: Long,
    /** Montant hors taxe de l'événement métier ; la règle CPT détermine les comptes et le journal. */
    val amount: Double,
    val taxAmount: Double = 0.0,
)

data class AccountingLedgerSnapshot(
    val settings: AccountingSettingsEntity? = null,
    val accounts: List<AccountingAccountEntity> = emptyList(),
    val journals: List<AccountingJournalEntity> = emptyList(),
    val vouchers: List<AccountingVoucherEntity> = emptyList(),
    val periods: List<AccountingPeriodEntity> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class ObserverAccountingLedgerUseCase @Inject constructor(private val dao: AccountingDao) {
    fun observe(): Flow<AccountingLedgerSnapshot> = dao.observeSettings().flatMapLatest { settings ->
        combine(
            dao.observeAccounts(settings?.standard ?: AccountingStandard.SYSCOHADA_REVISE.name),
            dao.observeJournals(),
            dao.observeVouchers(),
            dao.observePeriods(),
        ) { accounts, journals, vouchers, periods ->
            AccountingLedgerSnapshot(settings, accounts, journals, vouchers, periods)
        }
    }
}

/** Installe un socle de comptes et journaux sans générer de soldes ou écritures fictifs. */
class InitializeAccountingPlanUseCase @Inject constructor(
    private val database: AppDatabase,
    private val dao: AccountingDao,
    private val gate: AccountingPermissionGate,
    private val licenceManager: LicenceManager,
    private val journalManager: JournalManager,
) {
    sealed interface Result {
        data class Success(val standard: AccountingStandard) : Result
        data object ReadOnly : Result
        data object PermissionDenied : Result
        data object InvalidStandard : Result
        data object AlreadyInitialized : Result
    }

    suspend operator fun invoke(standard: AccountingStandard): Result {
        if (licenceManager.isReadOnly()) return Result.ReadOnly
        if (!gate.autorise(PermissionChecker.Action.EDIT)) return Result.PermissionDenied
        // Le gabarit livré n'est disponible que pour le SYSCOHADA révisé et reste à compléter.
        if (standard != AccountingStandard.SYSCOHADA_REVISE) return Result.InvalidStandard
        val result = database.withTransaction {
            if (dao.countAccounts(standard.name) > 0) return@withTransaction Result.AlreadyInitialized
            val now = System.currentTimeMillis()
            val existing = dao.getSettings()
            if (existing != null && existing.standard != standard.name) return@withTransaction Result.InvalidStandard
            dao.saveSettings((existing ?: AccountingSettingsEntity(standard = standard.name, updatedAt = now))
                .copy(standard = standard.name, updatedAt = now))
            dao.insertAccounts(defaultAccounts(standard, now))
            if (dao.countJournals() == 0) dao.insertJournals(defaultJournals(now))
            Result.Success(standard)
        }
        if (result is Result.Success) {
            journalManager.log("COMPTABILITE", "PLAN_INITIALISE", standard.name, userId = gate.currentUserId())
        }
        return result
    }

    private fun defaultAccounts(standard: AccountingStandard, now: Long) = listOf(
        account(standard, "101", "Capital social", "1", "CREDIT", now),
        account(standard, "218", "Matériel et outillage", "2", "DEBIT", now),
        account(standard, "31", "Stocks de marchandises", "3", "DEBIT", now),
        account(standard, "401", "Fournisseurs", "4", "CREDIT", now, supplier = true),
        account(standard, "411", "Clients", "4", "DEBIT", now, customer = true),
        account(standard, "443", "État, TVA facturée", "4", "CREDIT", now),
        account(standard, "445", "État, TVA récupérable", "4", "DEBIT", now),
        account(standard, "521", "Banques locales", "5", "DEBIT", now),
        account(standard, "571", "Caisse", "5", "DEBIT", now),
        account(standard, "601", "Achats de marchandises", "6", "DEBIT", now),
        account(standard, "603", "Variations de stocks", "6", "DEBIT", now),
        account(standard, "622", "Locations et charges locatives", "6", "DEBIT", now),
        account(standard, "661", "Rémunérations directes versées au personnel", "6", "DEBIT", now),
        account(standard, "681", "Dotations aux amortissements", "6", "DEBIT", now),
        account(standard, "701", "Ventes de marchandises", "7", "CREDIT", now),
    )

    private fun account(
        standard: AccountingStandard, code: String, name: String, classCode: String,
        side: String, now: Long, customer: Boolean = false, supplier: Boolean = false,
    ) = AccountingAccountEntity(
        standard = standard.name, code = code, name = name, classCode = classCode,
        normalSide = side, customerAuxiliary = customer, supplierAuxiliary = supplier, createdAt = now,
    )

    private fun defaultJournals(now: Long) = listOf(
        journal("VE", "Ventes", "SALES", now), journal("AC", "Achats", "PURCHASES", now),
        journal("BQ", "Banque", "BANK", now), journal("CA", "Caisse", "CASH", now),
        journal("MM", "Mobile Money", "MOBILE_MONEY", now), journal("ST", "Stock", "STOCK", now),
        journal("PA", "Paie", "PAYROLL", now), journal("OD", "Opérations diverses", "GENERAL", now),
        journal("AN", "À-nouveaux", "OPENING", now), journal("CL", "Clôture", "CLOSING", now),
    )

    private fun journal(code: String, name: String, type: String, now: Long) =
        AccountingJournalEntity(code = code, name = name, type = type, createdAt = now)
}

/** Admission des brouillons manuels et des propositions idempotentes des modules métier. */
class CreateAccountingVoucherUseCase @Inject constructor(
    private val database: AppDatabase,
    private val dao: AccountingDao,
    private val gate: AccountingPermissionGate,
    private val licenceManager: LicenceManager,
    private val sequenceManager: SequenceManager,
    private val journalManager: JournalManager,
) {
    sealed interface Result {
        data class Success(val voucherId: Long, val reference: String) : Result
        data class AlreadyProcessed(val voucherId: Long, val reference: String) : Result
        data object Invalid : Result
        data object ReadOnly : Result
        data object PermissionDenied : Result
        data object JournalNotFound : Result
        data object AccountNotFound : Result
        data object PostingRuleNotFound : Result
        data object PeriodClosed : Result
    }

    suspend fun manual(
        journalCode: String, date: Long, description: String, currencyCode: String,
        lines: List<AccountingEntryLineEntity>,
    ): Result = create(
        journalCode, date, description, currencyCode, lines,
        "COMPTABILITE", "MANUAL_VOUCHER", null, null,
    )

    suspend fun fromEvent(event: AccountingPostingEvent): Result {
        if (event.eventType.isBlank() || event.idempotencyKey.isBlank() || event.idempotencyKey.length > 200 ||
            event.sourceModule.isBlank() || event.sourceDocumentId <= 0
        ) return Result.Invalid
        dao.getVoucherBySourceKey(event.idempotencyKey)?.let {
            return Result.AlreadyProcessed(it.id, it.reference)
        }
        if (!event.amount.isFinite() || event.amount <= 0 || !event.taxAmount.isFinite() || event.taxAmount < 0) return Result.Invalid
        val eventType = event.eventType.trim().uppercase()
        val standard = dao.getSettings()?.standard ?: return Result.Invalid
        val rule = dao.getPostingRule(standard, eventType) ?: return Result.PostingRuleNotFound
        if (!rule.active) return Result.PostingRuleNotFound
        val journal = dao.getJournalByCode(rule.journalCode)?.takeIf { it.active } ?: return Result.JournalNotFound
        val primary = dao.getAccountByCode(standard, rule.primaryAccountCode) ?: return Result.AccountNotFound
        val counterpart = dao.getAccountByCode(standard, rule.counterpartAccountCode) ?: return Result.AccountNotFound
        val taxAccount = rule.taxAccountCode?.let { dao.getAccountByCode(standard, it) ?: return Result.AccountNotFound }
        val eventLines = AccountingRules.eventLines(
            rule, primary, counterpart, taxAccount, event.description, event.amount, event.taxAmount,
        ) ?: return Result.Invalid
        return create(
            journal.code, event.date, event.description, event.currencyCode, eventLines,
            event.sourceModule.trim().uppercase(), eventType, event.sourceDocumentId, event.idempotencyKey,
        )
    }

    private suspend fun create(
        journalCode: String, date: Long, description: String, currencyCode: String,
        lines: List<AccountingEntryLineEntity>, sourceModule: String,
        sourceDocumentType: String, sourceDocumentId: Long?, sourceKey: String?,
    ): Result {
        if (licenceManager.isReadOnly()) return Result.ReadOnly
        if (!gate.autorise(PermissionChecker.Action.CREATE)) return Result.PermissionDenied
        if (description.trim().length < 2 || currencyCode.length != 3 ||
            !currencyCode.all { it in 'A'..'Z' || it in 'a'..'z' } || !AccountingRules.linesValid(lines)
        ) return Result.Invalid
        val config = dao.getSettings() ?: return Result.Invalid
        val journal = dao.getJournalByCode(journalCode)?.takeIf { it.active } ?: return Result.JournalNotFound
        val accounts = lines.map { dao.getAccount(it.accountId) ?: return Result.AccountNotFound }
        if (accounts.any { !it.active || !it.postable || it.standard != config.standard }) return Result.AccountNotFound
        val dateCalendar = Calendar.getInstance().apply { timeInMillis = date }
        val year = dateCalendar.get(Calendar.YEAR)
        val month = dateCalendar.get(Calendar.MONTH) + 1
        val initialPeriod = dao.getPeriod(year, month)
        if (isClosed(initialPeriod?.status)) return Result.PeriodClosed
        if (sourceKey != null) dao.getVoucherBySourceKey(sourceKey)?.let {
            return Result.AlreadyProcessed(it.id, it.reference)
        }
        val totals = AccountingRules.totals(lines)
        if (!totals.balanced) return Result.Invalid
        val reference = sequenceManager.next(DocType.COMPTABILITE, year)
        val userId = gate.currentUserId()
        val id = database.withTransaction {
            if (isClosed(dao.getPeriod(year, month)?.status)) return@withTransaction -1L
            if (sourceKey != null) dao.getVoucherBySourceKey(sourceKey)?.let {
                return@withTransaction -it.id - 1L
            }
            val currentAccounts = lines.map { dao.getAccount(it.accountId) ?: return@withTransaction -3L }
            if (currentAccounts.any { !it.active || !it.postable || it.standard != config.standard }) return@withTransaction -3L
            val voucherId = dao.insertVoucher(AccountingVoucherEntity(
                journalId = journal.id, reference = reference, accountingDate = date,
                sourceModule = sourceModule, sourceDocumentType = sourceDocumentType,
                sourceDocumentId = sourceDocumentId, sourceKey = sourceKey,
                description = description.trim(), currencyCode = currencyCode.uppercase(),
                totalDebit = totals.debit, totalCredit = totals.credit, createdBy = userId,
                createdAt = System.currentTimeMillis(),
            ))
            dao.insertLines(lines.mapIndexed { index, line ->
                line.copy(id = 0, voucherId = voucherId, lineNumber = index + 1)
            })
            voucherId
        }
        if (id == -1L) return Result.PeriodClosed
        if (id == -3L) return Result.AccountNotFound
        if (id <= -2L) {
            val existing = dao.getVoucher(-id - 1L) ?: return Result.Invalid
            return Result.AlreadyProcessed(existing.id, existing.reference)
        }
        journalManager.log("COMPTABILITE", "BROUILLON_CREE", "$reference — ${description.trim()}", userId = userId)
        return Result.Success(id, reference)
    }
}

/** Seule transition vers POSTED : valide période, comptes, pièce et totaux dans une transaction Room. */
class PostAccountingVoucherUseCase @Inject constructor(
    private val database: AppDatabase,
    private val dao: AccountingDao,
    private val gate: AccountingPermissionGate,
    private val licenceManager: LicenceManager,
    private val journalManager: JournalManager,
) {
    sealed interface Result {
        data object Success : Result
        data object NotFound : Result
        data object InvalidState : Result
        data object Unbalanced : Result
        data object AccountInvalid : Result
        data object PeriodClosed : Result
        data object SameUserApproval : Result
        data object PermissionDenied : Result
        data object ReadOnly : Result
    }

    suspend operator fun invoke(voucherId: Long): Result {
        if (licenceManager.isReadOnly()) return Result.ReadOnly
        if (!gate.autorise(PermissionChecker.Action.VALIDATE) || !gate.autorise(PermissionChecker.Action.EDIT)) {
            return Result.PermissionDenied
        }
        val userId = gate.currentUserId()
        val result = database.withTransaction {
            val voucher = dao.getVoucher(voucherId) ?: return@withTransaction Result.NotFound
            if (voucher.status != AccountingVoucherStatus.DRAFT.name && voucher.status != AccountingVoucherStatus.TO_VALIDATE.name) {
                return@withTransaction Result.InvalidState
            }
            if (isClosed(dao.getPeriod(voucher.accountingDate.year(), voucher.accountingDate.month())?.status)) {
                return@withTransaction Result.PeriodClosed
            }
            val journal = dao.getJournal(voucher.journalId) ?: return@withTransaction Result.AccountInvalid
            if (!journal.active) return@withTransaction Result.AccountInvalid
            if (journal.requiresApproval && voucher.createdBy != null && voucher.createdBy == userId) {
                return@withTransaction Result.SameUserApproval
            }
            val lines = dao.getLines(voucherId)
            if (!AccountingRules.linesValid(lines) || !AccountingRules.totals(lines).balanced) {
                return@withTransaction Result.Unbalanced
            }
            val standard = dao.getSettings()?.standard ?: return@withTransaction Result.AccountInvalid
            for (line in lines) {
                val account = dao.getAccount(line.accountId) ?: return@withTransaction Result.AccountInvalid
                if (!account.active || !account.postable || account.standard != standard) return@withTransaction Result.AccountInvalid
            }
            val totals = AccountingRules.totals(lines)
            dao.updateVoucher(voucher.copy(
                status = AccountingVoucherStatus.POSTED.name,
                totalDebit = totals.debit, totalCredit = totals.credit,
                validatedBy = userId, postedAt = System.currentTimeMillis(),
            ))
            Result.Success
        }
        if (result == Result.Success) {
            journalManager.log("COMPTABILITE", "PIECE_COMPTABILISEE", "Pièce id=$voucherId", userId = userId)
        }
        return result
    }
}

/** Corrige une pièce par extourne; les montants et lignes de la source ne sont jamais réécrits. */
class ReverseAccountingVoucherUseCase @Inject constructor(
    private val database: AppDatabase,
    private val dao: AccountingDao,
    private val gate: AccountingPermissionGate,
    private val licenceManager: LicenceManager,
    private val sequenceManager: SequenceManager,
    private val journalManager: JournalManager,
) {
    sealed interface Result {
        data class Success(val reversalId: Long, val reference: String) : Result
        data object NotFound : Result
        data object InvalidState : Result
        data object PermissionDenied : Result
        data object PeriodClosed : Result
        data object SameUserApproval : Result
        data object ReadOnly : Result
    }

    suspend operator fun invoke(voucherId: Long, date: Long = System.currentTimeMillis()): Result {
        if (licenceManager.isReadOnly()) return Result.ReadOnly
        if (!gate.autorise(PermissionChecker.Action.EDIT) || !gate.autorise(PermissionChecker.Action.VALIDATE)) return Result.PermissionDenied
        val source = dao.getVoucher(voucherId) ?: return Result.NotFound
        if (source.status != AccountingVoucherStatus.POSTED.name) return Result.InvalidState
        val sourceJournal = dao.getJournal(source.journalId) ?: return Result.InvalidState
        if (sourceJournal.requiresApproval) return Result.SameUserApproval
        val originalLines = dao.getLines(voucherId)
        if (!AccountingRules.linesValid(originalLines) || !AccountingRules.totals(originalLines).balanced) return Result.InvalidState
        val calendar = Calendar.getInstance().apply { timeInMillis = date }
        val year = calendar.get(Calendar.YEAR)
        if (isClosed(dao.getPeriod(year, calendar.get(Calendar.MONTH) + 1)?.status)) return Result.PeriodClosed
        val reference = sequenceManager.next(DocType.COMPTABILITE, year)
        val now = System.currentTimeMillis()
        val userId = gate.currentUserId()
        val reversalId = database.withTransaction {
            val current = dao.getVoucher(voucherId) ?: return@withTransaction 0L
            if (current.status != AccountingVoucherStatus.POSTED.name) return@withTransaction 0L
            if (isClosed(dao.getPeriod(date.year(), date.month())?.status)) return@withTransaction -1L
            val id = dao.insertVoucher(current.copy(
                id = 0, reference = reference, accountingDate = date, documentDate = null,
                sourceModule = "COMPTABILITE", sourceDocumentType = "REVERSAL",
                sourceDocumentId = current.id, sourceKey = null,
                status = AccountingVoucherStatus.DRAFT.name,
                description = "Extourne de ${current.reference} — ${current.description}",
                totalDebit = current.totalCredit, totalCredit = current.totalDebit,
                createdBy = userId, validatedBy = null, postedAt = null,
                reversedVoucherId = current.id, createdAt = now,
            ))
            dao.insertLines(originalLines.map { line ->
                line.copy(id = 0, voucherId = id, debitAmount = line.creditAmount, creditAmount = line.debitAmount)
            })
            val reversal = dao.getVoucher(id) ?: return@withTransaction 0L
            dao.updateVoucher(reversal.copy(status = AccountingVoucherStatus.POSTED.name, validatedBy = userId, postedAt = now))
            dao.updateVoucher(current.copy(status = AccountingVoucherStatus.REVERSED.name))
            id
        }
        if (reversalId == -1L) return Result.PeriodClosed
        if (reversalId <= 0) return Result.InvalidState
        journalManager.log("COMPTABILITE", "EXTOURNE_CREEE", "${source.reference} → $reference", userId = userId)
        return Result.Success(reversalId, reference)
    }
}

/** Verrouille une période comptable lorsqu'elle ne contient aucun brouillon à traiter. */
class CloseAccountingPeriodUseCase @Inject constructor(
    private val database: AppDatabase,
    private val dao: AccountingDao,
    private val gate: AccountingPermissionGate,
    private val licenceManager: LicenceManager,
    private val journalManager: JournalManager,
) {
    sealed interface Result {
        data object Success : Result
        data object InvalidPeriod : Result
        data object HasPendingVouchers : Result
        data object AlreadyClosed : Result
        data object PermissionDenied : Result
        data object ReadOnly : Result
    }

    suspend operator fun invoke(year: Int, month: Int): Result {
        if (year !in 2000..2200 || month !in 1..12) return Result.InvalidPeriod
        if (licenceManager.isReadOnly()) return Result.ReadOnly
        if (!gate.autorise(PermissionChecker.Action.EDIT) || !gate.autorise(PermissionChecker.Action.VALIDATE)) {
            return Result.PermissionDenied
        }
        val currentCalendar = Calendar.getInstance()
        if (year > currentCalendar.get(Calendar.YEAR) ||
            (year == currentCalendar.get(Calendar.YEAR) && month > currentCalendar.get(Calendar.MONTH) + 1)
        ) return Result.InvalidPeriod
        val userId = gate.currentUserId()
        val calendar = Calendar.getInstance().apply { clear(); set(year, month - 1, 1) }
        val start = calendar.timeInMillis
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23); calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59); calendar.set(Calendar.MILLISECOND, 999)
        val end = calendar.timeInMillis
        val result = database.withTransaction {
            val period = dao.getPeriod(year, month)
            if (isClosed(period?.status)) return@withTransaction Result.AlreadyClosed
            if (dao.countPendingVouchers(start, end) > 0) return@withTransaction Result.HasPendingVouchers
            val now = System.currentTimeMillis()
            if (period == null) {
                dao.insertPeriod(AccountingPeriodEntity(
                    year = year, month = month, status = AccountingPeriodStatus.CLOSED.name,
                    closedAt = now, closedBy = userId,
                ))
            } else {
                dao.updatePeriod(period.copy(status = AccountingPeriodStatus.CLOSED.name, closedAt = now, closedBy = userId))
            }
            Result.Success
        }
        if (result == Result.Success) journalManager.log("COMPTABILITE", "PERIODE_CLOTUREE", "$year-$month", userId = userId)
        return result
    }
}

data class AccountingAccountDraft(
    val code: String,
    val name: String,
    val normalSide: String,
    val parentCode: String? = null,
    val postable: Boolean = true,
    val customerAuxiliary: Boolean = false,
    val supplierAuxiliary: Boolean = false,
)

/** Configure pays, régime déclaratif librement étiqueté et début d'exercice avant saisie. */
class ConfigureAccountingProfileUseCase @Inject constructor(
    private val database: AppDatabase,
    private val dao: AccountingDao,
    private val gate: AccountingPermissionGate,
    private val licenceManager: LicenceManager,
    private val journalManager: JournalManager,
) {
    sealed interface Result {
        data object Success : Result
        data object Invalid : Result
        data object AlreadyInUse : Result
        data object PermissionDenied : Result
        data object ReadOnly : Result
    }

    suspend operator fun invoke(
        standard: AccountingStandard,
        countryCode: String,
        taxRegime: String?,
        exerciceStartMonth: Int,
    ): Result {
        if (licenceManager.isReadOnly()) return Result.ReadOnly
        if (!gate.autorise(PermissionChecker.Action.EDIT)) return Result.PermissionDenied
        val country = countryCode.trim().uppercase()
        val regime = taxRegime?.trim()?.takeIf { it.isNotEmpty() }
        if (!country.matches(Regex("[A-Z]{2}")) || exerciceStartMonth !in 1..12 || (regime?.length ?: 0) > 80) {
            return Result.Invalid
        }
        val userId = gate.currentUserId()
        val result = database.withTransaction {
            if (dao.countAllAccounts() > 0 || dao.countAllVouchers() > 0) return@withTransaction Result.AlreadyInUse
            val current = dao.getSettings() ?: AccountingSettingsEntity(updatedAt = System.currentTimeMillis())
            dao.saveSettings(current.copy(
                standard = standard.name,
                countryCode = country,
                taxRegime = regime,
                exerciceStartMonth = exerciceStartMonth,
                updatedAt = System.currentTimeMillis(),
            ))
            Result.Success
        }
        if (result == Result.Success) journalManager.log("COMPTABILITE", "PROFIL_COMPTABLE_CONFIGURE", "$country / ${regime ?: "sans régime précisé"}", userId = userId)
        return result
    }
}

class CreateAccountingAccountUseCase @Inject constructor(
    private val database: AppDatabase,
    private val dao: AccountingDao,
    private val gate: AccountingPermissionGate,
    private val licenceManager: LicenceManager,
    private val journalManager: JournalManager,
) {
    sealed interface Result {
        data class Success(val accountId: Long) : Result
        data object Invalid : Result
        data object DuplicateCode : Result
        data object ParentNotFound : Result
        data object NoPlan : Result
        data object PermissionDenied : Result
        data object ReadOnly : Result
    }

    suspend operator fun invoke(draft: AccountingAccountDraft): Result {
        if (licenceManager.isReadOnly()) return Result.ReadOnly
        if (!gate.autorise(PermissionChecker.Action.CREATE) || !gate.autorise(PermissionChecker.Action.EDIT)) return Result.PermissionDenied
        val code = draft.code.trim()
        val name = draft.name.trim()
        if (!code.matches(Regex("[1-9][0-9]{0,7}")) || name.length < 2 ||
            draft.normalSide !in AccountingSide.entries.map { it.name } ||
            (draft.customerAuxiliary && draft.supplierAuxiliary) ||
            (draft.parentCode != null && !draft.parentCode.matches(Regex("[1-9][0-9]{0,7}")))
        ) return Result.Invalid
        val settings = dao.getSettings() ?: return Result.NoPlan
        val userId = gate.currentUserId()
        val result = database.withTransaction {
            if (dao.getAccountByCode(settings.standard, code) != null) return@withTransaction Result.DuplicateCode
            if (draft.parentCode != null && dao.getAccountByCode(settings.standard, draft.parentCode) == null) {
                return@withTransaction Result.ParentNotFound
            }
            val id = dao.insertAccount(AccountingAccountEntity(
                standard = settings.standard, code = code, name = name,
                classCode = code.first().toString(), normalSide = draft.normalSide,
                parentCode = draft.parentCode, postable = draft.postable,
                customerAuxiliary = draft.customerAuxiliary, supplierAuxiliary = draft.supplierAuxiliary,
                createdAt = System.currentTimeMillis(),
            ))
            Result.Success(id)
        }
        if (result is Result.Success) journalManager.log("COMPTABILITE", "COMPTE_CREE", "$code — $name", userId = userId)
        return result
    }
}

class CreateAccountingJournalUseCase @Inject constructor(
    private val database: AppDatabase,
    private val dao: AccountingDao,
    private val gate: AccountingPermissionGate,
    private val licenceManager: LicenceManager,
    private val journalManager: JournalManager,
) {
    sealed interface Result {
        data class Success(val journalId: Long) : Result
        data object Invalid : Result
        data object DuplicateCode : Result
        data object PermissionDenied : Result
        data object ReadOnly : Result
    }

    suspend operator fun invoke(code: String, name: String, type: String, requiresApproval: Boolean = false): Result {
        if (licenceManager.isReadOnly()) return Result.ReadOnly
        if (!gate.autorise(PermissionChecker.Action.CREATE) || !gate.autorise(PermissionChecker.Action.EDIT)) return Result.PermissionDenied
        val normalizedCode = code.trim().uppercase()
        val normalizedName = name.trim()
        if (!normalizedCode.matches(Regex("[A-Z0-9]{2,5}")) || normalizedName.length < 2 || type.isBlank()) return Result.Invalid
        val userId = gate.currentUserId()
        val result = database.withTransaction {
            if (dao.getJournalByCode(normalizedCode) != null) return@withTransaction Result.DuplicateCode
            val id = dao.insertJournal(AccountingJournalEntity(
                code = normalizedCode, name = normalizedName, type = type.trim().uppercase(),
                requiresApproval = requiresApproval, createdAt = System.currentTimeMillis(),
            ))
            Result.Success(id)
        }
        if (result is Result.Success) {
            journalManager.log("COMPTABILITE", "JOURNAL_CREE", "$normalizedCode — $normalizedName", userId = userId)
        }
        return result
    }
}

/** Règles de comptabilisation configurées exclusivement dans CPT. */
class ConfigureAccountingPostingRuleUseCase @Inject constructor(
    private val database: AppDatabase,
    private val dao: AccountingDao,
    private val gate: AccountingPermissionGate,
    private val licenceManager: LicenceManager,
    private val journalManager: JournalManager,
) {
    sealed interface Result {
        data object Success : Result
        data object Invalid : Result
        data object JournalNotFound : Result
        data object AccountNotFound : Result
        data object PermissionDenied : Result
        data object ReadOnly : Result
    }

    suspend operator fun invoke(rule: AccountingPostingRuleEntity): Result {
        if (licenceManager.isReadOnly()) return Result.ReadOnly
        if (!gate.autorise(PermissionChecker.Action.EDIT)) return Result.PermissionDenied
        val eventType = rule.eventType.trim().uppercase()
        val journalCode = rule.journalCode.trim().uppercase()
        val primaryCode = rule.primaryAccountCode.trim()
        val counterpartCode = rule.counterpartAccountCode.trim()
        val taxCode = rule.taxAccountCode?.trim()?.takeIf { it.isNotEmpty() }
        if (!eventType.matches(Regex("[A-Z][A-Z0-9_]{1,63}")) ||
            rule.primarySide !in AccountingSide.entries.map { it.name } ||
            !journalCode.matches(Regex("[A-Z0-9]{2,5}")) ||
            primaryCode.isBlank() || counterpartCode.isBlank() || primaryCode == counterpartCode ||
            (taxCode != null && (taxCode == primaryCode || taxCode == counterpartCode)) ||
            (taxCode == null && rule.taxSide != null) ||
            (taxCode != null && (rule.taxSide == null || rule.taxSide !in AccountingSide.entries.map { it.name }))
        ) return Result.Invalid
        val userId = gate.currentUserId()
        val result = database.withTransaction {
            val settings = dao.getSettings() ?: return@withTransaction Result.Invalid
            val journal = dao.getJournalByCode(journalCode)?.takeIf { it.active }
                ?: return@withTransaction Result.JournalNotFound
            val primary = dao.getAccountByCode(settings.standard, primaryCode)
                ?.takeIf { it.active && it.postable } ?: return@withTransaction Result.AccountNotFound
            val counterpart = dao.getAccountByCode(settings.standard, counterpartCode)
                ?.takeIf { it.active && it.postable } ?: return@withTransaction Result.AccountNotFound
            if (primary.id == counterpart.id) return@withTransaction Result.Invalid
            if (taxCode != null) {
                val tax = dao.getAccountByCode(settings.standard, taxCode)
                    ?.takeIf { it.active && it.postable } ?: return@withTransaction Result.AccountNotFound
                if (tax.id == primary.id || tax.id == counterpart.id) return@withTransaction Result.Invalid
            }
            dao.savePostingRule(rule.copy(
                id = 0,
                standard = settings.standard,
                eventType = eventType,
                journalCode = journal.code,
                primaryAccountCode = primary.code,
                counterpartAccountCode = counterpart.code,
                taxAccountCode = taxCode,
                createdAt = System.currentTimeMillis(),
            ))
            Result.Success
        }
        if (result == Result.Success) {
            journalManager.log("COMPTABILITE", "REGLE_COMPTABLE_CONFIGUREE", eventType, userId = userId)
        }
        return result
    }
}

private fun isClosed(status: String?): Boolean =
    status == AccountingPeriodStatus.CLOSED.name || status == AccountingPeriodStatus.LOCKED.name

private fun Long.year(): Int = Calendar.getInstance().apply { timeInMillis = this@year }.get(Calendar.YEAR)
private fun Long.month(): Int = Calendar.getInstance().apply { timeInMillis = this@month }.get(Calendar.MONTH) + 1
