package com.missa.b360

import com.missa.b360.core.data.entity.AccountingAccountEntity
import com.missa.b360.core.data.entity.AccountingEntryLineEntity
import com.missa.b360.core.data.entity.AccountingPostingRuleEntity
import com.missa.b360.core.domain.model.AccountingRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountingRulesTest {
    private fun line(
        number: Int,
        debit: Double = 0.0,
        credit: Double = 0.0,
        accountId: Long = number.toLong(),
    ) = AccountingEntryLineEntity(
        voucherId = 1,
        lineNumber = number,
        accountId = accountId,
        label = "Écriture $number",
        debitAmount = debit,
        creditAmount = credit,
    )

    @Test fun `une pièce équilibrée à deux lignes est valide`() {
        val lines = listOf(line(1, debit = 1250.25), line(2, credit = 1250.25))
        assertTrue(AccountingRules.linesValid(lines))
        assertEquals(1250.25, AccountingRules.totals(lines).debit, 0.0)
        assertTrue(AccountingRules.totals(lines).balanced)
    }

    @Test fun `un écart supérieur au centime est détecté`() {
        val totals = AccountingRules.totals(listOf(line(1, debit = 100.0), line(2, credit = 99.99)))
        assertFalse(totals.balanced)
    }

    @Test fun `une demi-unité d'arrondi est tolérée à la précision centime`() {
        val totals = AccountingRules.Totals(debit = 100.004, credit = 100.0)
        assertTrue(totals.balanced)
    }

    @Test fun `les lignes à double sens ou négatives sont rejetées`() {
        assertFalse(AccountingRules.linesValid(listOf(line(1, debit = 2.0, credit = 1.0), line(2, credit = 1.0))))
        assertFalse(AccountingRules.linesValid(listOf(line(1, debit = -1.0), line(2, credit = -1.0))))
        assertFalse(AccountingRules.linesValid(listOf(line(1, debit = 1.0), line(2))))
    }

    @Test fun `les montants non finis sont rejetés`() {
        assertFalse(AccountingRules.linesValid(listOf(line(1, debit = Double.NaN), line(2, credit = Double.NaN))))
        assertFalse(AccountingRules.Totals(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY).balanced)
    }

    @Test fun `le compte appartient à une classe OHADA numérotée`() {
        assertEquals("4", AccountingRules.classCode("411"))
        assertEquals("7", AccountingRules.classCode(" 701 "))
        assertEquals(null, AccountingRules.classCode("ABC"))
        assertEquals(null, AccountingRules.classCode("0"))
    }

    @Test fun `une règle de vente produit débit client crédit vente et TVA`() {
        val rule = AccountingPostingRuleEntity(
            standard = "SYSCOHADA_REVISE", eventType = "SALE_POSTED", journalCode = "VE",
            primaryAccountCode = "701", counterpartAccountCode = "411", primarySide = "CREDIT",
            taxAccountCode = "443", taxSide = "CREDIT", createdAt = 1,
        )
        val lines = AccountingRules.eventLines(
            rule, account(1, "701"), account(2, "411"), account(3, "443"), "Vente validée", 100.0, 19.25,
        )!!
        assertEquals(3, lines.size)
        assertEquals(119.25, AccountingRules.totals(lines).debit, 0.0)
        assertEquals(119.25, AccountingRules.totals(lines).credit, 0.0)
        assertTrue(lines.any { it.accountId == 2L && it.debitAmount == 119.25 })
        assertTrue(lines.any { it.accountId == 3L && it.creditAmount == 19.25 })
    }

    @Test fun `une règle d'achat poste la taxe récupérable au débit`() {
        val rule = AccountingPostingRuleEntity(
            standard = "SYSCOHADA_REVISE", eventType = "PURCHASE_POSTED", journalCode = "AC",
            primaryAccountCode = "601", counterpartAccountCode = "401", primarySide = "DEBIT",
            taxAccountCode = "445", taxSide = "DEBIT", createdAt = 1,
        )
        val lines = AccountingRules.eventLines(
            rule, account(1, "601"), account(2, "401"), account(3, "445"), "Achat validé", 80.0, 15.2,
        )!!
        assertTrue(AccountingRules.totals(lines).balanced)
        assertEquals(95.2, lines.single { it.accountId == 2L }.creditAmount, 0.0)
        assertEquals(15.2, lines.single { it.accountId == 3L }.debitAmount, 0.0)
    }

    @Test fun `le moteur rejette une taxe sans compte ni solde`() {
        val rule = AccountingPostingRuleEntity(
            standard = "SYSCOHADA_REVISE", eventType = "SALE_POSTED", journalCode = "VE",
            primaryAccountCode = "701", counterpartAccountCode = "411", primarySide = "CREDIT", createdAt = 1,
        )
        assertEquals(null, AccountingRules.eventLines(
            rule, account(1, "701"), account(2, "411"), null, "Vente", 100.0, 19.25,
        ))
    }

    private fun account(id: Long, code: String) = AccountingAccountEntity(
        id = id, standard = "SYSCOHADA_REVISE", code = code, name = code,
        classCode = code.first().toString(), normalSide = "DEBIT", createdAt = 1,
    )
}
