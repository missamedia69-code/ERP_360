package com.missa.b360

import com.missa.b360.core.domain.model.AgedBalanceRules
import com.missa.b360.core.domain.model.AgingBucket
import com.missa.b360.core.domain.model.ClientLedgerItem
import com.missa.b360.core.domain.model.ClientMetricsRules
import org.junit.Assert.assertEquals
import org.junit.Test

class AgedBalanceRulesTest {
    private val day = ClientMetricsRules.DAY_MS
    private val now = 2_000_000_000_000L

    @Test fun `jours de retard arrondis par exces et nuls avant lecheance`() {
        assertEquals(0, AgedBalanceRules.joursDeRetard(now, now))
        assertEquals(0, AgedBalanceRules.joursDeRetard(now + 1, now))
        assertEquals(1, AgedBalanceRules.joursDeRetard(now - 1, now))
        assertEquals(1, AgedBalanceRules.joursDeRetard(now - day, now))
        assertEquals(2, AgedBalanceRules.joursDeRetard(now - day - 1, now))
        assertEquals(365, AgedBalanceRules.joursDeRetard(now - 365 * day, now))
    }

    @Test fun `ecart extreme ne deborde pas`() {
        assertEquals(Int.MAX_VALUE, AgedBalanceRules.joursDeRetard(Long.MIN_VALUE, now))
        assertEquals(0, AgedBalanceRules.joursDeRetard(Long.MAX_VALUE, now))
    }

    @Test fun `bornes des tranches`() {
        fun tranche(jours: Long, extra: Long = 0) = AgedBalanceRules.tranche(now - jours * day - extra, now)
        assertEquals(AgingBucket.NON_ECHU, AgedBalanceRules.tranche(now, now))
        assertEquals(AgingBucket.JOURS_1_30, tranche(1))
        assertEquals(AgingBucket.JOURS_1_30, tranche(30))
        assertEquals(AgingBucket.JOURS_31_60, tranche(30, extra = 1))
        assertEquals(AgingBucket.JOURS_31_60, tranche(60))
        assertEquals(AgingBucket.JOURS_61_90, tranche(60, extra = 1))
        assertEquals(AgingBucket.JOURS_61_90, tranche(90))
        assertEquals(AgingBucket.PLUS_90, tranche(90, extra = 1))
        assertEquals(AgingBucket.PLUS_90, tranche(365))
    }

    @Test fun `aucune facture donne cinq tranches a zero`() {
        val balance = AgedBalanceRules.calculer(emptyList<ClientLedgerItem>(), 30, now)
        assertEquals(AgingBucket.entries.size, balance.montants.size)
        assertEquals(0.0, balance.total, 0.0)
        assertEquals(0.0, balance.enRetard, 0.0)
    }

    @Test fun `repartit les factures ouvertes par tranche avec delai de 30 jours`() {
        val items = listOf(
            ClientLedgerItem(50.0, 0.0, now - 10 * day, recordId = 1), // échéance dans 20 j
            ClientLedgerItem(100.0, 0.0, now - 35 * day, recordId = 2), // 5 j de retard
            ClientLedgerItem(80.0, 20.0, now - 75 * day, recordId = 3), // 45 j, reste 60
            ClientLedgerItem(200.0, 0.0, now - 100 * day, recordId = 4), // 70 j
            ClientLedgerItem(10.0, 0.0, now - 200 * day, recordId = 5), // 170 j
        )
        val balance = AgedBalanceRules.calculer(items, 30, now)
        assertEquals(50.0, balance.montant(AgingBucket.NON_ECHU), 1e-9)
        assertEquals(100.0, balance.montant(AgingBucket.JOURS_1_30), 1e-9)
        assertEquals(60.0, balance.montant(AgingBucket.JOURS_31_60), 1e-9)
        assertEquals(200.0, balance.montant(AgingBucket.JOURS_61_90), 1e-9)
        assertEquals(10.0, balance.montant(AgingBucket.PLUS_90), 1e-9)
        assertEquals(420.0, balance.total, 1e-9)
        assertEquals(370.0, balance.enRetard, 1e-9)
    }

    @Test fun `un avoir rapproche reduit la tranche de sa facture source`() {
        val items = listOf(
            ClientLedgerItem(100.0, 0.0, now - 50 * day, recordId = 10), // 20 j de retard
            ClientLedgerItem(200.0, 0.0, now - 20 * day, recordId = 20), // non échue
            ClientLedgerItem(150.0, 0.0, now, creditNote = true, sourceRecordId = 20),
        )
        val balance = AgedBalanceRules.calculer(items, 30, now)
        assertEquals(50.0, balance.montant(AgingBucket.NON_ECHU), 1e-9)
        assertEquals(100.0, balance.montant(AgingBucket.JOURS_1_30), 1e-9)
        assertEquals(150.0, balance.total, 1e-9)
    }

    @Test fun `un avoir total solde la facture et la balance est vide`() {
        val items = listOf(
            ClientLedgerItem(100.0, 0.0, now - 50 * day, recordId = 1),
            ClientLedgerItem(100.0, 0.0, now, creditNote = true, sourceRecordId = 1),
        )
        assertEquals(0.0, AgedBalanceRules.calculer(items, 30, now).total, 1e-9)
    }

    @Test fun `delai nul la facture est echue des la milliseconde suivante`() {
        val a = AgedBalanceRules.calculer(listOf(ClientLedgerItem(10.0, 0.0, now)), 0, now)
        assertEquals(10.0, a.montant(AgingBucket.NON_ECHU), 1e-9)
        val b = AgedBalanceRules.calculer(listOf(ClientLedgerItem(10.0, 0.0, now - 1)), 0, now)
        assertEquals(10.0, b.montant(AgingBucket.JOURS_1_30), 1e-9)
    }

    @Test fun `delai de 365 jours puis valeurs hors bornes ramenees`() {
        val items = listOf(ClientLedgerItem(10.0, 0.0, now - 364 * day))
        assertEquals(10.0, AgedBalanceRules.calculer(items, 365, now).montant(AgingBucket.NON_ECHU), 1e-9)
        // 1000 jours est ramené à 365 : la facture de 364 jours reste non échue.
        assertEquals(10.0, AgedBalanceRules.calculer(items, 1000, now).montant(AgingBucket.NON_ECHU), 1e-9)
    }

    @Test fun `valeurs invalides ignorees`() {
        val items = listOf(
            ClientLedgerItem(Double.NaN, 0.0, now - 50 * day),
            ClientLedgerItem(10.0, 20.0, now - 50 * day),
            ClientLedgerItem(40.0, 0.0, now - 50 * day),
        )
        assertEquals(40.0, AgedBalanceRules.calculer(items, 30, now).total, 1e-9)
    }

    @Test fun `la balance concorde avec encours et retards des metriques`() {
        val items = listOf(
            ClientLedgerItem(100.0, 0.0, now - 90 * day, recordId = 1),
            ClientLedgerItem(60.0, 10.0, now - 40 * day, recordId = 2),
            ClientLedgerItem(75.0, 0.0, now - 5 * day, recordId = 3),
            ClientLedgerItem(30.0, 0.0, now - 1 * day, creditNote = true, sourceRecordId = 1),
        )
        val balance = AgedBalanceRules.calculer(items, 30, now)
        val metrics = ClientMetricsRules.calculate(items, 30, now)
        assertEquals(metrics.outstanding, balance.total, 1e-9)
        assertEquals(metrics.overdueAmount, balance.enRetard, 1e-9)
        assertEquals(60, metrics.joursRetardMax)
    }
}
