package com.missa.b360

import com.missa.b360.core.journal.JournalManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Rétention du journal d'audit (RA-18) : 30 / 90 jours ou 12 mois. */
class JournalRetentionTest {

    @Test
    fun `les rétentions proposées sont 30, 90 et 365 jours`() {
        assertEquals(listOf(30, 90, 365), JournalManager.RETENTIONS_DISPONIBLES)
        assertTrue(JournalManager.RETENTION_DEFAUT_JOURS == 365)
    }

    @Test
    fun `une valeur enregistrée valide est respectée`() {
        assertEquals(30, JournalManager.retentionEnJours("30"))
        assertEquals(90, JournalManager.retentionEnJours("90"))
        assertEquals(365, JournalManager.retentionEnJours("365"))
        assertEquals(90, JournalManager.retentionEnJours(" 90 "))
    }

    @Test
    fun `une valeur absente ou hors catalogue retombe sur 12 mois`() {
        assertEquals(365, JournalManager.retentionEnJours(null))
        assertEquals(365, JournalManager.retentionEnJours(""))
        assertEquals(365, JournalManager.retentionEnJours("abc"))
        assertEquals(365, JournalManager.retentionEnJours("7"))
        assertEquals(365, JournalManager.retentionEnJours("-30"))
    }

    @Test
    fun `la durée par défaut vaut bien 365 jours en millisecondes`() {
        assertEquals(
            JournalManager.RETENTION_DEFAUT_JOURS * JournalManager.JOUR_MS,
            JournalManager.DUREE_RETENTION_MS,
        )
    }
}
