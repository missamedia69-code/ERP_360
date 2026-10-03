package com.missa.b360

import com.missa.b360.core.data.datastore.SettingsStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Régression : `CURRENT_USER_ID` est écrit en String ; sa lecture typée Long levait ClassCastException. */
class SettingsStoreLongTest {
    @Test fun `un Long est lu tel quel`() {
        assertEquals(7L, SettingsStore.longDepuisValeurBrute(7L))
    }

    @Test fun `une String numerique de l ancien format est convertie`() {
        assertEquals(42L, SettingsStore.longDepuisValeurBrute("42"))
        assertEquals(42L, SettingsStore.longDepuisValeurBrute(" 42 "))
    }

    @Test fun `une valeur absente ou non numerique donne null`() {
        assertNull(SettingsStore.longDepuisValeurBrute(null))
        assertNull(SettingsStore.longDepuisValeurBrute("abc"))
        assertNull(SettingsStore.longDepuisValeurBrute(true))
    }
}
