package com.missa.b360

import com.missa.b360.core.util.Fuseaux
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tests du catalogue des fuseaux horaires (configuration initiale). */
class FuseauxTest {

    @Test
    fun `le catalogue couvre UTC-12 a UTC+14 sans doublon`() {
        val offsets = Fuseaux.catalogue.map { it.offsetMinutes }
        assertEquals(38, Fuseaux.catalogue.size)
        assertEquals(-720, offsets.first())
        assertEquals(840, offsets.last())
        assertEquals(offsets.sorted(), offsets)
        assertEquals(offsets.distinct().size, offsets.size)
        assertEquals(Fuseaux.catalogue.map { it.id }.distinct().size, Fuseaux.catalogue.size)
    }

    @Test
    fun `chaque identifiant produit le decalage attendu`() {
        Fuseaux.catalogue.forEach { fuseau ->
            val zone = TimeZone.getTimeZone(fuseau.id)
            assertEquals(fuseau.id, fuseau.offsetMinutes * 60_000, zone.rawOffset)
        }
    }

    @Test
    fun `les libelles UTC sont formates avec deux chiffres`() {
        assertEquals("UTC+05:45", Fuseaux.formaterOffset(345))
        assertEquals("UTC-03:30", Fuseaux.formaterOffset(-210))
        assertEquals("UTC+00:00", Fuseaux.formaterOffset(0))
        assertEquals("GMT+05:45", Fuseaux.identifiant(345))
        assertEquals("GMT-03:30", Fuseaux.identifiant(-210))
    }

    @Test
    fun `un identifiant IANA enregistre est ramene a son decalage`() {
        assertEquals(60, Fuseaux.resoudre("Africa/Douala").offsetMinutes)
        assertEquals(0, Fuseaux.resoudre("UTC").offsetMinutes)
        assertEquals(330, Fuseaux.resoudre("Asia/Kolkata").offsetMinutes)
        assertEquals("GMT+04:00", Fuseaux.resoudre("Asia/Dubai").id)
    }

    @Test
    fun `une valeur vide ou inconnue retombe sur le fuseau de l appareil`() {
        val defaut = Fuseaux.parDefaut()
        assertEquals(defaut.id, Fuseaux.resoudre(null).id)
        assertEquals(defaut.id, Fuseaux.resoudre("").id)
        assertEquals(defaut.id, Fuseaux.resoudre("Zone/Inconnue").id)
        assertNotNull(Fuseaux.parId(defaut.id))
    }

    @Test
    fun `l heure courante suit le fuseau demande`() {
        val heure = Fuseaux.heureCourante("GMT+09:00")
        assertTrue(heure.matches(Regex("\\d{2}:\\d{2}")))
    }
}
