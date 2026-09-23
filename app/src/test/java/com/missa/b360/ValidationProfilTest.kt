package com.missa.b360

import com.missa.b360.core.domain.model.ConditionDependance
import com.missa.b360.core.domain.model.DependancesModules
import com.missa.b360.core.domain.model.ModuleCode
import com.missa.b360.core.domain.model.OptionsConfigProfil
import com.missa.b360.core.domain.model.OptionsProfil
import com.missa.b360.core.domain.model.ValidationProfil
import com.missa.b360.core.domain.model.ViolationProfil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Règle d'or et validation des configurations de profils (spec §7.2, §9).
 */
class ValidationProfilTest {

    private val venteSansStock = OptionsConfigProfil(venteSansStock = true)

    // --- DependancesModules : registre déclaratif. ---

    @Test
    fun `la règle d'or est déclarée en ACH-to-STK PRO-to-STK et VEN-to-STK conditionnel`() {
        val codes = DependancesModules.liste.map { it.from to it.to }
        assertEquals(
            listOf(ModuleCode.ACH to ModuleCode.STK, ModuleCode.PRO to ModuleCode.STK, ModuleCode.VEN to ModuleCode.STK),
            codes,
        )
        assertEquals(
            ConditionDependance.SAUF_SI_VENTE_SANS_STOCK,
            DependancesModules.de(ModuleCode.VEN)?.condition,
        )
        assertTrue(DependancesModules.liste.all { it.obligatoire })
    }

    @Test
    fun `une dépendance sans son module d'origine est respectée`() {
        assertTrue(DependancesModules.estRespectee(
            DependancesModules.de(ModuleCode.ACH)!!,
            listOf(ModuleCode.VEN, ModuleCode.STK),
        ))
    }

    // --- Validation : la configuration proposée. ---

    @Test
    fun `le pack ASV respecte la règle d'or`() {
        val actifs = setOf(ModuleCode.ACH, ModuleCode.STK, ModuleCode.VEN)
        assertTrue(ValidationProfil.estValide(actifs))
        assertTrue(ValidationProfil.validateProfileConfig(actifs).isEmpty())
    }

    @Test
    fun `Achat sans Stock viole la règle d'or`() {
        val actifs = setOf(ModuleCode.ACH, ModuleCode.VEN)
        assertEquals(
            listOf(ViolationProfil.ACH_SANS_STOCK, ViolationProfil.VEN_SANS_STOCK),
            ValidationProfil.violations(actifs),
        )
    }

    @Test
    fun `Production sans Stock viole la règle d'or`() {
        assertEquals(
            listOf(ViolationProfil.PRO_SANS_STOCK),
            ValidationProfil.violations(setOf(ModuleCode.PRO)),
        )
    }

    @Test
    fun `Vente sans Stock est bloquée par défaut et autorisée avec l option`() {
        assertEquals(
            listOf(ViolationProfil.VEN_SANS_STOCK),
            ValidationProfil.violations(setOf(ModuleCode.VEN)),
        )
        assertTrue(ValidationProfil.estValide(setOf(ModuleCode.VEN), venteSansStock))
    }

    @Test
    fun `l option vente_sans_stock ne débloque que la dépendance VEN`() {
        val actifs = setOf(ModuleCode.ACH, ModuleCode.VEN)
        // ACH→STK reste exigée même avec « vente sans stock ».
        assertEquals(
            listOf(ViolationProfil.ACH_SANS_STOCK),
            ValidationProfil.violations(actifs, venteSansStock),
        )
    }

    @Test
    fun `la forme Map de la spec accepte une table d options`() {
        assertTrue(
            ValidationProfil.validateProfileConfig(
                setOf(ModuleCode.VEN),
                mapOf(OptionsProfil.VENTE_SANS_STOCK to true),
            ).isEmpty(),
        )
        val messages = ValidationProfil.validateProfileConfig(
            setOf(ModuleCode.ACH, ModuleCode.VEN),
            mapOf(OptionsProfil.VENTE_SANS_STOCK to false),
        )
        assertEquals(2, messages.size)
        assertTrue(messages.any { it.contains(ModuleCode.ACH.code) })
        assertTrue(messages.any { it.contains(ModuleCode.VEN.code) })
    }

    @Test
    fun `un pack support seul ne viole aucune dépendance métier`() {
        assertTrue(ValidationProfil.estValide(emptySet()))
        assertTrue(ValidationProfil.estValide(setOf(ModuleCode.SER, ModuleCode.CPT)))
        // Le profil FULL, complet, ne peut pas violer la règle d'or.
        assertTrue(ValidationProfil.estValide(ModuleCode.entries.toSet()))
    }

    @Test
    fun `la mauvaise clé d option est ignorée sans fausser la validation`() {
        assertEquals(
            OptionsConfigProfil(),
            OptionsProfil.depuis(mapOf("production_sans_stock" to true)),
        )
        assertEquals(
            listOf(ViolationProfil.VEN_SANS_STOCK),
            ValidationProfil.violations(
                setOf(ModuleCode.VEN),
                OptionsProfil.depuis(mapOf("cle_inconnue" to true)),
            ),
        )
    }

    @Test
    fun `les messages de violation sont explicites et nomment les modules`() {
        ViolationProfil.entries.forEach { violation ->
            assertNotNull(violation.message)
            assertTrue(violation.message.isNotBlank())
            assertTrue(violation.message.contains(violation.from.code))
            assertTrue(violation.message.contains(violation.to.code))
        }
    }

    /** AV (legacy) est un profil figé : à l'appelant de l'exclure de la validation. */
    @Test
    fun `le profil AV legacy viole la règle d or et l appelant l exclut`() {
        val av = setOf(ModuleCode.ACH, ModuleCode.VEN)
        assertFalse(ValidationProfil.estValide(av))
        // La spec §6.2 et §7.2 : AV est maintenu pour rétro-compatibilité mais à migrer.
        assertTrue(ViolationProfil.ACH_SANS_STOCK in ValidationProfil.violations(av))
    }
}