package com.missa.b360.core.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Référentiel comptable retenu par l'entreprise. */
@Entity(tableName = "accounting_settings")
data class AccountingSettingsEntity(
    @PrimaryKey val id: Long = 1,
    val standard: String = AccountingStandard.SYSCOHADA_REVISE.name,
    val countryCode: String = "CM",
    val taxRegime: String? = null,
    val exerciceStartMonth: Int = 1,
    val updatedAt: Long,
)

enum class AccountingStandard { SYSCOHADA_REVISE, SYSCOHADA_PME, PCG_FR, IFRS_LOCAL, CUSTOM }
enum class AccountingSide { DEBIT, CREDIT }
enum class AccountingVoucherStatus { DRAFT, TO_VALIDATE, POSTED, REVERSED }
enum class AccountingPeriodStatus { OPEN, IN_REVIEW, CLOSED, LOCKED }

/** Compte du plan choisi. Un compte non imputable sert uniquement de regroupement. */
@Entity(
    tableName = "accounting_accounts",
    indices = [Index(value = ["standard", "code"], unique = true), Index(value = ["classCode"])],
)
data class AccountingAccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val standard: String,
    val code: String,
    val name: String,
    val classCode: String,
    val normalSide: String,
    val parentCode: String? = null,
    val postable: Boolean = true,
    val active: Boolean = true,
    val customerAuxiliary: Boolean = false,
    val supplierAuxiliary: Boolean = false,
    val createdAt: Long,
)

/** Journal de saisie et de numérotation des pièces comptables. */
@Entity(
    tableName = "accounting_posting_rules",
    indices = [Index(value = ["standard", "eventType"], unique = true)],
)
data class AccountingPostingRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val standard: String,
    val eventType: String,
    val journalCode: String,
    val primaryAccountCode: String,
    val counterpartAccountCode: String,
    val primarySide: String,
    val taxAccountCode: String? = null,
    val taxSide: String? = null,
    val active: Boolean = true,
    val createdAt: Long,
)

@Entity(
    tableName = "accounting_journals",
    indices = [Index(value = ["code"], unique = true)],
)
data class AccountingJournalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val code: String,
    val name: String,
    val type: String,
    val active: Boolean = true,
    val requiresApproval: Boolean = false,
    val createdAt: Long,
)

/** Une période comptable clôturée est immuable en écriture. */
@Entity(
    tableName = "accounting_periods",
    indices = [Index(value = ["year", "month"], unique = true)],
)
data class AccountingPeriodEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val year: Int,
    /** 0 signifie période annuelle, 1..12 un mois. */
    val month: Int,
    val status: String = AccountingPeriodStatus.OPEN.name,
    val closedAt: Long? = null,
    val closedBy: Long? = null,
)

/** En-tête d'une pièce équilibrée ou en cours de saisie. */
@Entity(
    tableName = "accounting_vouchers",
    foreignKeys = [
        ForeignKey(
            entity = AccountingJournalEntity::class,
            parentColumns = ["id"],
            childColumns = ["journalId"],
            onDelete = ForeignKey.NO_ACTION,
        ),
    ],
    indices = [
        Index(value = ["reference"], unique = true),
        Index(value = ["journalId", "accountingDate"]),
        Index(value = ["sourceKey"], unique = true),
        Index(value = ["status", "accountingDate"]),
    ],
)
data class AccountingVoucherEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val journalId: Long,
    val reference: String,
    val accountingDate: Long,
    val documentDate: Long? = null,
    val sourceModule: String? = null,
    val sourceDocumentType: String? = null,
    val sourceDocumentId: Long? = null,
    /** Clé d'idempotence fournie par le module source. */
    val sourceKey: String? = null,
    val status: String = AccountingVoucherStatus.DRAFT.name,
    val description: String,
    val currencyCode: String,
    val exchangeRate: Double = 1.0,
    val totalDebit: Double = 0.0,
    val totalCredit: Double = 0.0,
    val createdBy: Long? = null,
    val validatedBy: Long? = null,
    val postedAt: Long? = null,
    val reversedVoucherId: Long? = null,
    val createdAt: Long,
)

/** Ligne comptable. Après comptabilisation, seules des écritures d'extourne corrigent la pièce. */
@Entity(
    tableName = "accounting_entry_lines",
    foreignKeys = [
        ForeignKey(
            entity = AccountingVoucherEntity::class,
            parentColumns = ["id"],
            childColumns = ["voucherId"],
            onDelete = ForeignKey.NO_ACTION,
        ),
        ForeignKey(
            entity = AccountingAccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.NO_ACTION,
        ),
    ],
    indices = [Index(value = ["voucherId", "lineNumber"], unique = true), Index(value = ["accountId"])],
)
data class AccountingEntryLineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val voucherId: Long,
    val lineNumber: Int,
    val accountId: Long,
    val label: String,
    val debitAmount: Double = 0.0,
    val creditAmount: Double = 0.0,
    val currencyAmount: Double? = null,
    val currencyCode: String? = null,
    val customerId: Long? = null,
    val supplierId: Long? = null,
    val treasuryAccountId: Long? = null,
    val taxCodeId: Long? = null,
    val projectId: Long? = null,
    val costCenterId: Long? = null,
    val dueDate: Long? = null,
    val matchingReference: String? = null,
)
