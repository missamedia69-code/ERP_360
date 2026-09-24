package com.missa.b360

import com.missa.b360.core.domain.model.RestaurationRules
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Ce qui rend une sauvegarde restaurable.
 *
 * Ces tests viennent après une panne réelle : le plafond de version était
 * recopié à la main en parallèle de l'annotation `@Database`, il avait dérivé,
 * et l'application refusait ses propres sauvegardes. La borne est donc épinglée
 * ici, en particulier le cas ordinaire — la version égale, qui doit passer.
 */
class RestaurationRulesTest {

    @Test
    fun `une sauvegarde de la version courante est acceptee`() {
        assertTrue(RestaurationRules.versionAcceptable(17, 17))
    }

    @Test
    fun `une sauvegarde ancienne est acceptee et sera migree`() {
        assertTrue(RestaurationRules.versionAcceptable(1, 17))
        assertTrue(RestaurationRules.versionAcceptable(7, 17))
        assertTrue(RestaurationRules.versionAcceptable(16, 17))
    }

    @Test
    fun `une sauvegarde plus recente que le schema est refusee`() {
        assertFalse(RestaurationRules.versionAcceptable(18, 17))
    }

    @Test
    fun `le refus ne se declenche qu au dessus de la version courante`() {
        // La frontière exacte : une version de plus est refusée, la même passe.
        assertTrue(RestaurationRules.versionAcceptable(4, 5))
        assertFalse(RestaurationRules.versionAcceptable(6, 5))
    }
}
