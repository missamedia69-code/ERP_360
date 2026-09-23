package com.missa.b360.core.data.repository

import com.missa.b360.core.data.dao.EnterpriseDao
import com.missa.b360.core.data.datastore.SettingsStore
import com.missa.b360.core.domain.model.ActivationProfil
import com.missa.b360.core.domain.model.ElementsPersonnalises
import com.missa.b360.core.domain.model.ModuleCode
import com.missa.b360.core.domain.model.ModulesPersonnalises
import com.missa.b360.core.domain.model.ModulesSocle
import com.missa.b360.core.domain.model.PalierTaille
import com.missa.b360.core.domain.model.ProfilActivite
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository central qui calcule et expose l'activation effective du profil
 * dans toute l'application.
 *
 * Chaque profil actif active réellement les modules et les éléments qui lui ont été
 * affectés par défaut OU par l'utilisateur. Ce repository est la source de vérité unique
 * observée par tous les ViewModels, écrans et guards.
 */
@Singleton
class ProfilActivationRepository @Inject constructor(
    private val settingsStore: SettingsStore,
    private val enterpriseDao: EnterpriseDao,
) {

    /**
     * Flow de l'activation effective, recalculé à chaque changement de réglage.
     *
     * Observe :
     * - PROFIL_ACTIVITE
     * - PALIER_TAILLE
     * - VENTE_SANS_STOCK
     * - MODULES_ACTIFS (tous modules actifs actuels, pour rétrocompatibilité)
     * - MODULES_SUPPORT (extras support)
     * - MODULES_ELEMENTS (nouveau : éléments personnalisés par module)
     */
    fun observeActivation(): Flow<ActivationProfil> {
        val profilFlow = settingsStore.observe(SettingsStore.Keys.PROFIL_ACTIVITE)
        val palierFlow = settingsStore.observe(SettingsStore.Keys.PALIER_TAILLE)
        val venteSansStockFlow = settingsStore.observe(SettingsStore.Keys.VENTE_SANS_STOCK)
        val modulesActifsFlow = settingsStore.observe(SettingsStore.Keys.MODULES_ACTIFS)
        val modulesSupportFlow = settingsStore.observe(SettingsStore.Keys.MODULES_SUPPORT)
        val modulesElementsFlow = settingsStore.observe(SettingsStore.Keys.MODULES_ELEMENTS)

        // Utilise combine à 5 puis combine avec le 6e pour compatibilité coroutines <1.8
        val cinq = combine(
            profilFlow,
            palierFlow,
            venteSansStockFlow,
            modulesActifsFlow,
            modulesSupportFlow,
        ) { profilStr, palierStr, venteSansStockStr, modulesActifsStr, modulesSupportStr ->
            arrayOf(profilStr, palierStr, venteSansStockStr, modulesActifsStr, modulesSupportStr)
        }
        return combine(cinq, modulesElementsFlow) { cinqArr, modulesElementsStr ->
            val profilStr = cinqArr[0] as String?
            val palierStr = cinqArr[1] as String?
            val venteSansStockStr = cinqArr[2] as String?
            val modulesActifsStr = cinqArr[3] as String?
            val modulesSupportStr = cinqArr[4] as String?

            val profil = profilStr?.let { runCatching { ProfilActivite.valueOf(it) }.getOrNull() }
            val palier = palierStr?.let { runCatching { PalierTaille.valueOf(it) }.getOrNull() }
            val venteSansStock = venteSansStockStr == "true"

            // Rétrocompatibilité : MODULES_ACTIFS contient tous les modules (métier + support)
            val tousActifs = ModulesPersonnalises.deserialiser(modulesActifsStr)
            val metierPersonnalises = ModulesSocle.filtrerMetier(tousActifs).toSet()

            // MODULES_SUPPORT contient les extras support (ajouts volontaires)
            val extrasSupport = ModulesPersonnalises.deserialiser(modulesSupportStr)
                .let { ModulesSocle.filtrerSupport(it) }
                .toSet()

            // Éléments personnalisés
            val elementsPerso = ElementsPersonnalises.deserialiser(modulesElementsStr)

            ActivationProfil.calculer(
                profil = profil,
                palier = palier,
                venteSansStock = venteSansStock,
                modulesPersonnalises = metierPersonnalises,
                extrasSupport = extrasSupport,
                elementsPersonnalises = elementsPerso,
            )
        }
    }

    /** Version synchrone (pour les UseCases qui ont besoin de l'état actuel) */
    suspend fun getActivation(): ActivationProfil {
        val profilStr = settingsStore.get(SettingsStore.Keys.PROFIL_ACTIVITE)
        val palierStr = settingsStore.get(SettingsStore.Keys.PALIER_TAILLE)
        val venteSansStockStr = settingsStore.get(SettingsStore.Keys.VENTE_SANS_STOCK)
        val modulesActifsStr = settingsStore.get(SettingsStore.Keys.MODULES_ACTIFS)
        val modulesSupportStr = settingsStore.get(SettingsStore.Keys.MODULES_SUPPORT)
        val modulesElementsStr = settingsStore.get(SettingsStore.Keys.MODULES_ELEMENTS)

        val profil = profilStr?.let { runCatching { ProfilActivite.valueOf(it) }.getOrNull() }
        val palier = palierStr?.let { runCatching { PalierTaille.valueOf(it) }.getOrNull() }
        val venteSansStock = venteSansStockStr == "true"

        val tousActifs = ModulesPersonnalises.deserialiser(modulesActifsStr)
        val metierPersonnalises = ModulesSocle.filtrerMetier(tousActifs).toSet()
        val extrasSupport = ModulesPersonnalises.deserialiser(modulesSupportStr)
            .let { ModulesSocle.filtrerSupport(it) }
            .toSet()
        val elementsPerso = ElementsPersonnalises.deserialiser(modulesElementsStr)

        return ActivationProfil.calculer(
            profil = profil,
            palier = palier,
            venteSansStock = venteSansStock,
            modulesPersonnalises = metierPersonnalises,
            extrasSupport = extrasSupport,
            elementsPersonnalises = elementsPerso,
        )
    }

    /**
     * Met à jour le profil et recalcule les modules actifs.
     * Persiste à la fois dans SettingsStore et dans l'entité Enterprise (si elle existe).
     * Met aussi à jour la barre du bas pour que le profil choisi place directement
     * ses modules sur la barre (max 3) et le reste dans Plus.
     */
    suspend fun mettreAJourProfil(
        profil: ProfilActivite,
        palier: PalierTaille? = null,
        venteSansStock: Boolean? = null,
        modulesPersonnalises: Set<ModuleCode>? = null,
        extrasSupport: Set<ModuleCode>? = null,
        elementsPersonnalises: Map<ModuleCode, Set<String>>? = null,
    ) {
        // Récupère l'état actuel pour les valeurs non fournies
        val actuel = getActivation()

        val nouveauProfil = profil
        val nouveauPalier = palier ?: actuel.palier
        val nouveauVenteSansStock = venteSansStock ?: actuel.venteSansStock
        val nouveauxMetierPerso = modulesPersonnalises ?: actuel.modulesPersonnalises
        val nouveauxExtras = extrasSupport ?: actuel.extrasSupport
        val nouveauxElements = elementsPersonnalises ?: actuel.elementsPersonnalises

        // Calcule la nouvelle activation effective
        val nouvelleActivation = ActivationProfil.calculer(
            profil = nouveauProfil,
            palier = nouveauPalier,
            venteSansStock = nouveauVenteSansStock,
            modulesPersonnalises = nouveauxMetierPerso,
            extrasSupport = nouveauxExtras,
            elementsPersonnalises = nouveauxElements,
        )

        // Persiste
        settingsStore.set(SettingsStore.Keys.PROFIL_ACTIVITE, nouveauProfil.name)
        nouveauPalier?.let { settingsStore.set(SettingsStore.Keys.PALIER_TAILLE, it.name) }
        settingsStore.set(SettingsStore.Keys.VENTE_SANS_STOCK, nouvelleActivation.venteSansStock.toString())

        // Modules actifs effectifs (métier + support) -> MODULES_ACTIFS
        val actifsSerialises = ModulesPersonnalises.serialiser(nouvelleActivation.modulesActifs)
        settingsStore.set(SettingsStore.Keys.MODULES_ACTIFS, actifsSerialises)

        // Extras support -> MODULES_SUPPORT
        val extrasSerialises = ModulesPersonnalises.serialiser(nouvelleActivation.extrasSupport)
        settingsStore.set(SettingsStore.Keys.MODULES_SUPPORT, extrasSerialises)

        // Éléments personnalisés -> MODULES_ELEMENTS
        val elementsSerialises = ElementsPersonnalises.serialiser(nouvelleActivation.elementsPersonnalises)
        settingsStore.set(SettingsStore.Keys.MODULES_ELEMENTS, elementsSerialises)

        // Barre du bas : place directement les modules du profil sur la barre (max 3)
        // L'utilisateur a demandé que choisir un profil (ex: Achat-Vente) mette
        // ses modules sur la barre du bas, le reste allant dans Plus.
        val barreModules = calculerBarrePourActivation(nouvelleActivation)
        settingsStore.set(SettingsStore.Keys.BARRE_MODULES, barreModules.joinToString(","))

        // Met à jour l'entreprise si elle existe
        val entreprise = enterpriseDao.get()
        if (entreprise != null) {
            enterpriseDao.upsert(
                entreprise.copy(
                    profilActivite = nouveauProfil.name,
                    palierTaille = nouveauPalier?.name ?: entreprise.palierTaille,
                ),
            )
        }
    }

    /**
     * Calcule les 3 modules à mettre sur la barre du bas pour une activation donnée.
     * Règle : prend les modules actifs dans l'ordre métier puis support, mappe chaque
     * ModuleCode vers son AppModule principal (ex: ACH->ACHATS, VEN->VENTE) et garde
     * les 3 premiers distincts. Cela garantit que choisir Achat-Vente met bien
     * Achats + Vente sur la barre, le reste (Compta, Tréso, Reporting) allant dans Plus.
     */
    private fun calculerBarrePourActivation(activation: ActivationProfil): List<String> {
        if (activation.modulesActifs.isEmpty()) return emptyList()
        // Ordre canonique : métier d'abord (ACH, STK, PRO, VEN, SER, PRJ) puis support
        // Mais pour l'UX, on veut VEN en premier pour un commerçant, donc on utilise
        // l'ordre de ModulesSocle.metier + support mais en priorisant VEN, ACH, STK
        val ordrePrioritaire = listOf(
            ModuleCode.VEN,
            ModuleCode.ACH,
            ModuleCode.STK,
            ModuleCode.TRE,
            ModuleCode.CPT,
            ModuleCode.PRO,
            ModuleCode.SER,
            ModuleCode.PRJ,
            ModuleCode.LOG,
            ModuleCode.CRM,
            ModuleCode.RH,
            ModuleCode.QUA,
            ModuleCode.MAI,
            ModuleCode.REP,
        )
        val tries = activation.modulesActifs.sortedBy { code ->
            val idx = ordrePrioritaire.indexOf(code)
            if (idx == -1) 99 else idx
        }
        // Map ModuleCode -> nom AppModule principal (celui qui apparaît dans la barre)
        val mapPrincipal = mapOf(
            ModuleCode.ACH to "ACHATS",
            ModuleCode.VEN to "VENTE",
            ModuleCode.STK to "STOCK",
            ModuleCode.PRO to "PRODUCTION",
            ModuleCode.SER to "SERVICES",
            ModuleCode.PRJ to "PROJETS",
            ModuleCode.RH to "RH",
            ModuleCode.CPT to "COMPTABILITE",
            ModuleCode.TRE to "TRESORERIE",
            ModuleCode.CRM to "CRM",
            ModuleCode.QUA to "QUALITE",
            ModuleCode.MAI to "MAINTENANCE",
            ModuleCode.LOG to "LOGISTIQUE",
            ModuleCode.REP to "REPORTING",
        )
        val result = mutableListOf<String>()
        val vus = mutableSetOf<ModuleCode>()
        for (code in tries) {
            if (code in vus) continue
            vus.add(code)
            val appName = mapPrincipal[code] ?: continue
            // Évite les doublons de ModuleCode déjà représentés (ex: VEN a VENTE et CLIENTS, on ne garde que VENTE)
            result.add(appName)
            if (result.size >= 3) break
        }
        return result
    }

    /**
     * Ajoute un module métier personnalisé
     */
    suspend fun ajouterModuleMetier(module: ModuleCode) {
        val actuel = getActivation()
        if (actuel.profil == null) return
        val nouveaux = actuel.modulesPersonnalises + module
        mettreAJourProfil(
            profil = actuel.profil,
            palier = actuel.palier,
            venteSansStock = actuel.venteSansStock,
            modulesPersonnalises = nouveaux,
            extrasSupport = actuel.extrasSupport,
            elementsPersonnalises = actuel.elementsPersonnalises,
        )
    }

    suspend fun retirerModuleMetier(module: ModuleCode) {
        val actuel = getActivation()
        if (actuel.profil == null) return
        // Ne pas retirer si c'est dans le pack du profil (verrouillé)
        if (module in ModulesSocle.metierDuPack(actuel.profil)) return
        val nouveaux = actuel.modulesPersonnalises - module
        mettreAJourProfil(
            profil = actuel.profil,
            palier = actuel.palier,
            venteSansStock = actuel.venteSansStock,
            modulesPersonnalises = nouveaux,
            extrasSupport = actuel.extrasSupport,
            elementsPersonnalises = actuel.elementsPersonnalises,
        )
    }

    suspend fun ajouterModuleSupport(module: ModuleCode) {
        val actuel = getActivation()
        if (actuel.profil == null) return
        // Si déjà recommandé, pas besoin de l'ajouter en extra
        val recommandes = ModulesSocle.recommandes(actuel.profil, actuel.palier, actuel.modulesMetierEffectifs)
        if (module in recommandes) return
        val nouveaux = actuel.extrasSupport + module
        mettreAJourProfil(
            profil = actuel.profil,
            palier = actuel.palier,
            venteSansStock = actuel.venteSansStock,
            modulesPersonnalises = actuel.modulesPersonnalises,
            extrasSupport = nouveaux,
            elementsPersonnalises = actuel.elementsPersonnalises,
        )
    }

    suspend fun retirerModuleSupport(module: ModuleCode) {
        val actuel = getActivation()
        if (actuel.profil == null) return
        // Ne pas retirer si recommandé (verrouillé)
        val recommandes = ModulesSocle.recommandes(actuel.profil, actuel.palier, actuel.modulesMetierEffectifs)
        if (module in recommandes) return
        val nouveaux = actuel.extrasSupport - module
        mettreAJourProfil(
            profil = actuel.profil,
            palier = actuel.palier,
            venteSansStock = actuel.venteSansStock,
            modulesPersonnalises = actuel.modulesPersonnalises,
            extrasSupport = nouveaux,
            elementsPersonnalises = actuel.elementsPersonnalises,
        )
    }

    suspend fun mettreAJourElements(module: ModuleCode, elements: Set<String>) {
        val actuel = getActivation()
        if (actuel.profil == null) return
        val nouvelleMap = actuel.elementsPersonnalises.toMutableMap()
        if (elements.isEmpty()) {
            nouvelleMap.remove(module)
        } else {
            nouvelleMap[module] = elements
        }
        mettreAJourProfil(
            profil = actuel.profil,
            palier = actuel.palier,
            venteSansStock = actuel.venteSansStock,
            modulesPersonnalises = actuel.modulesPersonnalises,
            extrasSupport = actuel.extrasSupport,
            elementsPersonnalises = nouvelleMap,
        )
    }

    suspend fun basculerVenteSansStock() {
        val actuel = getActivation()
        if (actuel.profil == null) return
        mettreAJourProfil(
            profil = actuel.profil,
            palier = actuel.palier,
            venteSansStock = !actuel.venteSansStock,
            modulesPersonnalises = actuel.modulesPersonnalises,
            extrasSupport = actuel.extrasSupport,
            elementsPersonnalises = actuel.elementsPersonnalises,
        )
    }
}
