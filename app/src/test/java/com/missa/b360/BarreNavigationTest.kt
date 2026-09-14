package com.missa.b360

import com.missa.b360.core.domain.model.ModuleCode
import com.missa.b360.ui.navigation.AppModule
import com.missa.b360.ui.navigation.Routes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Présence de la barre de navigation, écran par écran.
 *
 * Ces tests figent la règle qui avait dérivé : un module épinglable doit
 * afficher la barre quand on l'ouvre, sinon l'onglet mène à un cul-de-sac.
 */
class BarreNavigationTest {

    @Test
    fun `tout module epinglable dans la barre affiche la barre une fois ouvert`() {
        val actifs = ModuleCode.entries.toList()
        AppModule.barreBas(actifs).forEach { module ->
            assertTrue(
                "L'onglet ${module.name} disparaîtrait une fois ouvert",
                AppModule.barreVisibleSur(module.route),
            )
        }
    }

    @Test
    fun `la barre reste visible malgre les arguments de route`() {
        assertTrue(AppModule.barreVisibleSur("module_vente?create=true"))
        assertTrue(AppModule.barreVisibleSur("module_stock?create=false&direction=NONE"))
    }

    @Test
    fun `les ecrans-liste des modules livres affichent la barre`() {
        listOf(
            AppModule.TRESORERIE,
            AppModule.COMPTABILITE,
            AppModule.CRM,
            AppModule.LOGISTIQUE,
            AppModule.QUALITE,
            AppModule.MAINTENANCE,
            AppModule.LIVRAISON,
            AppModule.SERVICES,
            AppModule.PROJETS,
            AppModule.CLIENTS,
            AppModule.STOCK,
            AppModule.VENTE,
        ).forEach { assertTrue(it.name, AppModule.barreVisibleSur(it.route)) }
    }

    @Test
    fun `la barre suit le metier du pack`() {
        // Un négoce voit sa vente et son stock en tête ; Clients, qui relève
        // du même module que la vente, complète la troisième place.
        assertEquals(
            listOf(AppModule.VENTE, AppModule.STOCK, AppModule.CLIENTS),
            AppModule.barreBas(listOf(ModuleCode.VEN, ModuleCode.STK)),
        )
        // …un prestataire ses services, sans stock ni production.
        val service = AppModule.barreBas(listOf(ModuleCode.SER, ModuleCode.VEN))
        assertTrue(AppModule.SERVICES in service)
        assertTrue(AppModule.STOCK !in service)
        // …un bureau d'études ses projets.
        assertTrue(AppModule.PROJETS in AppModule.barreBas(listOf(ModuleCode.PRJ, ModuleCode.VEN)))
    }

    @Test
    fun `la barre et le menu Plus se partagent le pack sans doublon`() {
        val actifs = ModuleCode.entries.toList()
        val barre = AppModule.barreBas(actifs)
        val plus = AppModule.secondaires(actifs)
        assertTrue("un module figure dans les deux", barre.intersect(plus.toSet()).isEmpty())
        assertEquals(AppModule.visibles(actifs).size, barre.size + plus.size)
    }

    @Test
    fun `epingler un module le retire du menu Plus`() {
        val actifs = ModuleCode.entries.toList()
        val epingles = listOf(AppModule.QUALITE.name, AppModule.CRM.name)
        assertEquals(
            listOf(AppModule.QUALITE, AppModule.CRM),
            AppModule.barreBas(actifs, epingles),
        )
        val plus = AppModule.secondaires(actifs, epingles)
        assertTrue(AppModule.QUALITE !in plus)
        assertTrue(AppModule.CRM !in plus)
        // Les anciens onglets reviennent dans le menu.
        assertTrue(AppModule.VENTE in plus)
    }

    @Test
    fun `un module dont l ecran masque la barre n est jamais epinglable`() {
        val actifs = ModuleCode.entries.toList()
        assertTrue(AppModule.FINANCES !in AppModule.epinglables(actifs))
        assertTrue(AppModule.ACHATS !in AppModule.epinglables(actifs))
        assertTrue(AppModule.FINANCES !in AppModule.barreBas(actifs, listOf(AppModule.FINANCES.name)))
    }

    @Test
    fun `l accueil affiche la barre`() {
        // L'accueil est la destination racine : la barre y est posée par le
        // graphe, comme sur tous les écrans-liste.
        assertTrue(AppModule.barreVisibleSur(AppModule.VENTE.route))
    }

    @Test
    fun `un formulaire en surimpression n affiche pas la barre`() {
        assertFalse(AppModule.barreVisibleSur(AppModule.FINANCES.route))
        assertFalse(AppModule.barreVisibleSur(AppModule.ACHATS.route))
    }

    @Test
    fun `les ecrans hors modules n affichent pas la barre`() {
        listOf(
            Routes.ADMIN_REGLAGES,
            Routes.ADMIN_JOURNAL,
            Routes.NOTIFICATIONS,
            Routes.TASKS,
            Routes.STOCK_PRODUCT_FORM,
            Routes.DEVIS_COMMANDE,
            null,
            "",
        ).forEach { assertFalse("route=$it", AppModule.barreVisibleSur(it)) }
    }

    @Test
    fun `la disposition d usine tient dans le nombre d onglets`() {
        val defaut = AppModule.barreBas(ModuleCode.entries.toList())
        assertTrue(defaut.size <= AppModule.MAX_ONGLETS)
        assertTrue(defaut.isNotEmpty())
    }

    @Test
    fun `un module epingle puis retire du pack est ignore`() {
        val actifs = listOf(ModuleCode.VEN)
        val choisis = AppModule.barreBas(actifs, listOf(AppModule.QUALITE.name, AppModule.VENTE.name))
        assertEquals(listOf(AppModule.VENTE), choisis)
    }

    @Test
    fun `sans epinglage valide la barre retombe sur la disposition d usine`() {
        val actifs = ModuleCode.entries.toList()
        val choisis = AppModule.barreBas(actifs, listOf("MODULE_INEXISTANT"))
        assertEquals(AppModule.barreBas(actifs), choisis)
    }
}
