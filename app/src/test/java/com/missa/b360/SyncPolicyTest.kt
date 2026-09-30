package com.missa.b360

import com.missa.b360.core.sync.SyncPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncPolicyTest {
    @Test
    fun `les reprises utilisent un recul exponentiel plafonne`() {
        assertEquals(5_000L, SyncPolicy.delaiNouvelEssai(0))
        assertEquals(10_000L, SyncPolicy.delaiNouvelEssai(1))
        assertEquals(20_000L, SyncPolicy.delaiNouvelEssai(2))
        assertEquals(SyncPolicy.DELAI_MAX_MS, SyncPolicy.delaiNouvelEssai(99))
        assertEquals(5_000L, SyncPolicy.delaiNouvelEssai(-4))
    }

    @Test
    fun `une version distante necrase jamais une modification locale concurrente`() {
        assertTrue(SyncPolicy.conflit(revisionLocale = 10, revisionDistante = 10, modificationsLocales = true))
        assertTrue(SyncPolicy.conflit(revisionLocale = 10, revisionDistante = 11, modificationsLocales = true))
        assertFalse(SyncPolicy.conflit(revisionLocale = 10, revisionDistante = 9, modificationsLocales = true))
        assertFalse(SyncPolicy.conflit(revisionLocale = 10, revisionDistante = 11, modificationsLocales = false))
    }
}
