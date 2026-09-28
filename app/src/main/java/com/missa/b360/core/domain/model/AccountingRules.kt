package com.missa.b360.core.domain.model

import com.missa.b360.core.data.entity.AccountingAccountEntity
import com.missa.b360.core.data.entity.AccountingEntryLineEntity
import com.missa.b360.core.data.entity.AccountingPostingRuleEntity
import com.missa.b360.core.data.entity.AccountingSide
import kotlin.math.abs

/** Vérifications comptables pures communes aux écritures manuelles et aux événements modules. */
object AccountingRules {
    const val TOLERANCE_CENT = 0.005

    data class Totals(val debit: Double, val credit: Double) {
        val balanced: Boolean get() = debit.isFinite() && credit.isFinite() && abs(debit - credit) <= TOLERANCE_CENT
    }

    fun linesValid(lines: List<AccountingEntryLineEntity>): Boolean =
        lines.size >= 2 && lines.all { line ->
            line.label.trim().length >= 2 &&
                line.debitAmount.isFinite() && line.creditAmount.isFinite() &&
                line.debitAmount >= 0.0 && line.creditAmount >= 0.0 &&
                ((line.debitAmount > 0.0 && roundMoney(line.debitAmount) >= 0.01 && line.creditAmount == 0.0) ||
                    (line.creditAmount > 0.0 && roundMoney(line.creditAmount) >= 0.01 && line.debitAmount == 0.0))
        }

    fun totals(lines: List<AccountingEntryLineEntity>): Totals = Totals(
        debit = roundMoney(lines.sumOf { it.debitAmount }),
        credit = roundMoney(lines.sumOf { it.creditAmount }),
    )

    fun roundMoney(value: Double): Double = kotlin.math.round(value * 100.0) / 100.0

    /** Applique une règle de journal à un montant métier et produit des lignes équilibrées. */
    fun eventLines(
        rule: AccountingPostingRuleEntity,
        primary: AccountingAccountEntity,
        counterpart: AccountingAccountEntity,
        taxAccount: AccountingAccountEntity?,
        description: String,
        amount: Double,
        taxAmount: Double,
    ): List<AccountingEntryLineEntity>? {
        if (!amount.isFinite() || amount <= 0 || !taxAmount.isFinite() || taxAmount < 0 || description.trim().length < 2) return null
        if (primary.code != rule.primaryAccountCode || counterpart.code != rule.counterpartAccountCode ||
            primary.standard != rule.standard || counterpart.standard != rule.standard ||
            !primary.active || !primary.postable || !counterpart.active || !counterpart.postable || primary.id == counterpart.id
        ) return null
        val primarySide = runCatching { AccountingSide.valueOf(rule.primarySide) }.getOrNull() ?: return null
        val taxSide = rule.taxSide?.let { runCatching { AccountingSide.valueOf(it) }.getOrNull() }
        if (rule.taxAccountCode == null) {
            if (taxAccount != null || taxSide != null || taxAmount != 0.0) return null
        } else {
            if (taxAccount == null || taxSide == null || taxAccount.code != rule.taxAccountCode ||
                taxAccount.standard != rule.standard || !taxAccount.active || !taxAccount.postable ||
                taxAccount.id == primary.id || taxAccount.id == counterpart.id
            ) return null
        }
        val oppositeSide = if (primarySide == AccountingSide.DEBIT) AccountingSide.CREDIT else AccountingSide.DEBIT
        val counterpartAmount = when {
            taxAccount == null -> amount
            taxSide == primarySide -> amount + taxAmount
            taxSide == oppositeSide -> amount - taxAmount
            else -> return null
        }
        if (!counterpartAmount.isFinite() || counterpartAmount <= 0) return null
        val lines = mutableListOf<AccountingEntryLineEntity>()
        fun append(account: AccountingAccountEntity, side: AccountingSide, value: Double, label: String) {
            lines += AccountingEntryLineEntity(
                voucherId = 0,
                lineNumber = lines.size + 1,
                accountId = account.id,
                label = label,
                debitAmount = if (side == AccountingSide.DEBIT) value else 0.0,
                creditAmount = if (side == AccountingSide.CREDIT) value else 0.0,
            )
        }
        append(primary, primarySide, amount, description.trim())
        if (taxAccount != null && taxAmount > 0) append(taxAccount, taxSide!!, taxAmount, "Taxe — ${description.trim()}")
        append(counterpart, oppositeSide, counterpartAmount, description.trim())
        return lines.takeIf { linesValid(it) && totals(it).balanced }
    }

    fun classCode(accountCode: String): String? = accountCode.trim().firstOrNull()
        ?.takeIf { it in '1'..'9' }
        ?.toString()
}
