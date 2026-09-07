package com.missa.b360

import com.missa.b360.core.domain.model.ModuleCode
import com.missa.b360.ui.navigation.AppModule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Visibilité des modules à l'accueil selon le pack retenu à l'onboarding. */
class ModuleRegistryTest {

    @Test
    fun `une configuration inconnue montre tous les modules`() {
        assertEquals(AppModule.entries.size, AppModule.visibles(emptyList()).size)
    }

    @Test
    fun `seuls les modules du pack sont visibles`() {
        val visibles = AppModule.visibles(listOf(ModuleCode.VEN, ModuleCode.STK))
        assertTrue(AppModule.VENTE in visibles)
        assertTrue(AppModule.CLIENTS in visibles)
        assertTrue(AppModule.STOCK in visibles)
        assertTrue(AppModule.PRODUCTION !in visibles)
        assertTrue(AppModule.MAINTENANCE !in visibles)
    }

    @Test
    fun `la barre du bas ne propose que des modules actifs`() {
        val barre = AppModule.barreBas(listOf(ModuleCode.VEN))
        assertTrue(barre.all { it.bottomBarDefault })
        assertTrue(barre.none { it.moduleCode == ModuleCode.STK })
    }

    @Test
    fun `les secondaires excluent la barre du bas sans rien perdre`() {
        val actifs = listOf(ModuleCode.VEN, ModuleCode.STK, ModuleCode.TRE, ModuleCode.CPT)
        val visibles = AppModule.visibles(actifs)
        val barre = AppModule.barreBas(actifs)
        val secondaires = AppModule.secondaires(actifs)
        assertEquals(visibles.size, barre.size + secondaires.size)
        assertTrue(barre.intersect(secondaires.toSet()).isEmpty())
    }

    @Test
    fun `un pack complet rend chaque module accessible`() {
        val visibles = AppModule.visibles(ModuleCode.entries.toList())
        assertEquals(AppModule.entries.size, visibles.size)
    }

    @Test
    fun `chaque module declare une route unique`() {
        val routes = AppModule.entries.map { it.route }
        assertEquals(routes.size, routes.distinct().size)
    }

    @Test
    fun `chaque module porte un libelle traduit`() {
        AppModule.entries.forEach { assertTrue(it.titleRes != 0) }
    }
}
