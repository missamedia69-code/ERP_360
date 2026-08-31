package com.missa.b360

import com.missa.b360.core.domain.usecase.CheckCreditLimitUseCase
import com.missa.b360.core.numbering.DocType
import com.missa.b360.core.numbering.SequenceManager
import com.missa.b360.core.security.PinHasher
import com.missa.b360.core.util.Iso4217
import com.missa.b360.core.util.MoneyUtils
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tests unitaires des règles métier (Phase A — socle). */
class SocleUseCasesTest {

    // --- RA-01 : PIN hashé, format 4–6 chiffres ---
    @Test
    fun `PinHasher encode et vérifie correctement le PIN`() {
        val encoded = PinHasher.encode("1234")
        assertTrue(encoded.contains(':'))
        assertTrue(PinHasher.verify("1234", encoded))
        assertFalse(PinHasher.verify("9999", encoded))
    }

    @Test
    fun `RA-01 un PIN de 3 chiffres est refusé et 6 accepté`() {
        assertFalse(PinHasher.isValidFormat("123"))
        assertTrue(PinHasher.isValidFormat("123456"))
        assertFalse(PinHasher.isValidFormat("1234567"))
        assertFalse(PinHasher.isValidFormat("12a4"))
    }

    @Test
    fun `RA-01 le sel change, le hash aussi — jamais identique deux fois`() {
        assertTrue(PinHasher.encode("1234") != PinHasher.encode("1234"))
    }

    // --- RA-09 / RM-40 : format de numérotation F2026-0001 ---
    @Test
    fun `numérotation Lettre + Année + Séquence sur 4 chiffres`() {
        assertEquals("F2026-0001", SequenceManager.format("F", 2026, 1L))
        assertEquals("CLI2026-0042", SequenceManager.format("CLI", 2026, 42L))
        assertEquals("TRF2026-12345", SequenceManager.format("TRF", 2026, 12345L))
    }

    @Test
    fun `les 18 types de documents ont un préfixe non vide`() {
        assertTrue(DocType.entries.all { it.prefix.isNotBlank() })
    }

    // --- RC-05 : limite de crédit ---
    private val check = CheckCreditLimitUseCase()

    @Test
    fun `RC-05 vente autorisée sous la limite`() {
        assertEquals(
            CheckCreditLimitUseCase.Verdict.AUTORISE,
            check(100.0, 200.0, 500.0),
        )
    }

    @Test
    fun `RC-05 alerte dans la marge de 10 pour cent`() {
        assertEquals(
            CheckCreditLimitUseCase.Verdict.ALERTE,
            check(420.0, 100.0, 500.0), // 520 = 104 % de 500
        )
    }

    @Test
    fun `RC-05 validation Gérant requise au-delà de la marge`() {
        assertEquals(
            CheckCreditLimitUseCase.Verdict.VALIDATION_REQUISE,
            check(420.0, 200.0, 500.0), // 620 > 550
        )
    }

    @Test
    fun `RC-05 limite nulle signifie illimitée`() {
        assertEquals(
            CheckCreditLimitUseCase.Verdict.AUTORISE,
            check(10_000.0, 50_000.0, null),
        )
    }

    // --- RA-07 : montants formatés, jamais convertis ---
    @Test
    fun `RA-07 formatage brut à 2 décimales`() {
        assertEquals("1500.50", MoneyUtils.formatBrut(1500.5))
        assertEquals("0.00", MoneyUtils.formatBrut(0.0))
    }

    @Test
    fun `RA-07 formatage avec devise`() {
        val formatted = MoneyUtils.format(1500.5, "USD")
        assertTrue(formatted.endsWith("USD"))
        assertTrue(formatted.contains("1"))
    }

    @Test
    fun `la liste des pays couvre le catalogue ISO et suggere la taxe du Cameroun`() {
        val pays = Iso4217.paysDisponibles(Locale.FRENCH)
        assertTrue(pays.size >= 200)
        val cameroun = pays.firstOrNull { it.code == "CM" }
        assertEquals(19.25, cameroun?.tauxTaxeSuggere ?: -1.0, 0.0)
        assertTrue(pays.all { it.tauxTaxeSuggere >= 0.0 })
    }
}
