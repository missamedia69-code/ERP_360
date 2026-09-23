package com.missa.b360.core.domain.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * État effectif d'activation d'un profil dans toute l'application.
 *
 * C'est la source de vérité unique : chaque profil actif (AV, ASV, APSV, SER, PRJ, FULL, CUSTOM)
 * active réellement les modules et les éléments (sous-fonctionnalités) qui lui ont été affectés
 * par défaut OU par l'utilisateur. Ce modèle est calculé à partir :
 * - du profil choisi
 * - du palier de taille
 * - de l'option venteSansStock
 * - des modules métier personnalisés ajoutés par l'utilisateur
 * - des modules support supplémentaires ajoutés
 * - des éléments (fonctions) personnalisés par module
 *
 * Il doit être effectif dans toute l'application, dans tous les systèmes :
 * navigation, accueil, modules, reporting, guards, etc.
 */
data class ActivationProfil(
    val profil: ProfilActivite?,
    val palier: PalierTaille?,
    val venteSansStock: Boolean,
    /** Modules métier ajoutés manuellement hors pack (ex. Stock ajouté à SER) */
    val modulesPersonnalises: Set<ModuleCode> = emptySet(),
    /** Briques support ajoutées manuellement hors recommandations */
    val extrasSupport: Set<ModuleCode> = emptySet(),
    /** Éléments personnalisés par module : module -> éléments choisis par l'utilisateur (override/additif) */
    val elementsPersonnalises: Map<ModuleCode, Set<String>> = emptyMap(),
    /** Modules métier effectifs (pack + perso + dépendances) */
    val modulesMetierEffectifs: Set<ModuleCode> = emptySet(),
    /** Modules support effectifs (recommandés + extras) */
    val modulesSupportEffectifs: Set<ModuleCode> = emptySet(),
    /** Tous les modules actifs (métier + support) */
    val modulesActifs: Set<ModuleCode> = emptySet(),
    /** Éléments effectifs par module (après fusion défaut + perso) */
    val elementsParModule: Map<ModuleCode, Set<String>> = emptyMap(),
) {
    /** Vérifie si un module est actif */
    fun isModuleActif(module: ModuleCode): Boolean = module in modulesActifs

    /** Vérifie si un élément d'un module est actif */
    fun isElementActif(module: ModuleCode, element: String): Boolean {
        if (!isModuleActif(module)) return false
        val actifs = elementsParModule[module] ?: return false
        return element in actifs
    }

    /** Éléments actifs pour un module donné (vide si module inactif) */
    fun elementsActifsPour(module: ModuleCode): Set<String> =
        if (isModuleActif(module)) elementsParModule[module].orEmpty() else emptySet()

    /** Liste triée des modules actifs dans l'ordre canonique */
    fun modulesActifsTries(): List<ModuleCode> =
        ModuleCode.entries.filter { it in modulesActifs }

    companion object {
        /** Activation vide : aucun profil, aucun module (état initial avant onboarding) */
        val VIDE = ActivationProfil(
            profil = null,
            palier = null,
            venteSansStock = false,
        )

        /**
         * Calcule l'activation effective à partir des entrées brutes.
         *
         * Règles :
         * - FULL : tous les modules, tous les éléments
         * - CUSTOM : modules = personnalisés + extras, éléments = personnalisés ou tous si non spécifié
         * - Standard (AV, ASV, APSV, SER, PRJ) :
         *   - modules métier = ModulesSocle.metierActifs(profil, personnalises, venteSansStock)
         *   - support recommandé = ModulesSocle.recommandes(profil, palier, metier)
         *   - support effectif = recommandé + extrasSupport
         *   - modules actifs = métier + support
         *   - éléments :
         *     - si module dans config du profil :
         *       - null => tous les éléments du module
         *       - liste => éléments par défaut du profil
         *       - fusion avec éléments personnalisés (union) si présents
         *     - si module ajouté par utilisateur (hors config) :
         *       - si éléments personnalisés présents => ces éléments
         *       - sinon tous les éléments du module
         */
        fun calculer(
            profil: ProfilActivite?,
            palier: PalierTaille?,
            venteSansStock: Boolean,
            modulesPersonnalises: Set<ModuleCode>,
            extrasSupport: Set<ModuleCode>,
            elementsPersonnalises: Map<ModuleCode, Set<String>>,
        ): ActivationProfil {
            if (profil == null) {
                return VIDE.copy(
                    profil = null,
                    palier = palier,
                    venteSansStock = venteSansStock,
                    modulesPersonnalises = modulesPersonnalises,
                    extrasSupport = extrasSupport,
                    elementsPersonnalises = elementsPersonnalises,
                )
            }

            // 1. Modules métier effectifs
            val metierEffectifs = when (profil) {
                ProfilActivite.FULL -> ModuleCode.entries.filter { ModulesSocle.type(it) == TypeModule.METIER }.toSet()
                ProfilActivite.CUSTOM -> ModulesSocle.filtrerMetier(modulesPersonnalises).toSet()
                else -> ModulesSocle.metierActifs(profil, modulesPersonnalises, venteSansStock).toSet()
            }

            // 2. Modules support effectifs
            val supportRecommande = when (profil) {
                ProfilActivite.FULL -> ModulesSocle.support.toSet()
                ProfilActivite.CUSTOM -> emptySet()
                else -> ModulesSocle.recommandes(profil, palier, metierEffectifs)
            }
            val supportEffectifs = when (profil) {
                ProfilActivite.FULL -> ModulesSocle.support.toSet()
                ProfilActivite.CUSTOM -> ModulesSocle.filtrerSupport(modulesPersonnalises + extrasSupport).toSet()
                else -> (supportRecommande + extrasSupport).let { ModulesSocle.filtrerSupport(it).toSet() }
            }

            // 3. Tous modules actifs
            val actifs = when (profil) {
                ProfilActivite.FULL -> ModuleCode.entries.toSet()
                ProfilActivite.CUSTOM -> (metierEffectifs + supportEffectifs)
                else -> (metierEffectifs + supportEffectifs)
            }

            // 4. Éléments par module
            val elementsMap = mutableMapOf<ModuleCode, Set<String>>()
            for (module in actifs) {
                val elementsEffectifs: Set<String> = when (profil) {
                    ProfilActivite.FULL -> ModuleSousElements.pourModule(module).toSet()
                    ProfilActivite.CUSTOM -> {
                        val custom = elementsPersonnalises[module]
                        if (custom != null && custom.isNotEmpty()) custom
                        else ModuleSousElements.pourModule(module).toSet()
                    }
                    else -> {
                        val config = ProfilConfiguration.configurations[profil]
                        val contient = config?.containsKey(module) == true
                        if (contient) {
                            val defaut = config?.get(module) // null => tous
                            val defautSet = if (defaut == null) {
                                ModuleSousElements.pourModule(module).toSet()
                            } else {
                                defaut.toSet()
                            }
                            val custom = elementsPersonnalises[module]
                            if (custom != null && custom.isNotEmpty()) {
                                // Union : défaut + custom (permet d'ajouter des éléments hors pack)
                                // Si l'utilisateur veut restreindre, il peut passer par une map qui remplace totalement
                                // On considère que si custom est présent, on fait union sauf si le module est hors pack initial
                                // Pour permettre la suppression, on utilise custom comme override si le module est dans CUSTOM list?
                                // Ici : union pour les modules du pack, override pour les modules ajoutés
                                // On distingue : si module est dans le pack de base, union, sinon override
                                val estDansPack = module in (ProfilConfiguration.modulesPourProfil(profil))
                                if (estDansPack) defautSet + custom else custom
                            } else {
                                defautSet
                            }
                        } else {
                            // Module ajouté par l'utilisateur hors profil
                            val custom = elementsPersonnalises[module]
                            if (custom != null && custom.isNotEmpty()) custom
                            else ModuleSousElements.pourModule(module).toSet()
                        }
                    }
                }
                elementsMap[module] = elementsEffectifs
            }

            return ActivationProfil(
                profil = profil,
                palier = palier,
                venteSansStock = venteSansStock,
                modulesPersonnalises = modulesPersonnalises,
                extrasSupport = extrasSupport,
                elementsPersonnalises = elementsPersonnalises,
                modulesMetierEffectifs = metierEffectifs,
                modulesSupportEffectifs = supportEffectifs,
                modulesActifs = actifs,
                elementsParModule = elementsMap,
            )
        }
    }
}

/**
 * Sérialisation des éléments personnalisés : Map<ModuleCode, Set<String>> <-> String
 *
 * Format JSON : {"ACH":["Fournisseurs","Commandes fournisseurs"],"VEN":["Clients"]}
 * Tolérant : ignore les codes inconnus et les éléments vides.
 */
object ElementsPersonnalises {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false }

    fun serialiser(elements: Map<ModuleCode, Set<String>>): String {
        if (elements.isEmpty()) return ""
        // Convert to Map<String, List<String>> for serialization
        val mapString = elements.mapKeys { it.key.name }
            .mapValues { it.value.toList() }
        return try {
            json.encodeToString(mapString)
        } catch (_: Exception) {
            // Fallback simple format
            mapString.entries.joinToString(";") { (k, v) -> "$k:${v.joinToString("|")}" }
        }
    }

    fun deserialiser(valeur: String?): Map<ModuleCode, Set<String>> {
        if (valeur.isNullOrBlank()) return emptyMap()
        // Try JSON first
        try {
            val decoded = json.decodeFromString<Map<String, List<String>>>(valeur)
            return decoded.mapNotNull { (k, v) ->
                runCatching { ModuleCode.valueOf(k) }.getOrNull()?.let { code ->
                    code to v.filter { it.isNotBlank() }.toSet()
                }
            }.toMap()
        } catch (_: Exception) {
            // Try legacy format "ACH:elem1|elem2;VEN:elem1"
            return try {
                valeur.split(';')
                    .mapNotNull { part ->
                        val idx = part.indexOf(':')
                        if (idx <= 0) return@mapNotNull null
                        val codeStr = part.substring(0, idx).trim()
                        val elemsStr = part.substring(idx + 1)
                        val code = runCatching { ModuleCode.valueOf(codeStr) }.getOrNull() ?: return@mapNotNull null
                        val elems = elemsStr.split('|').map { it.trim() }.filter { it.isNotEmpty() }.toSet()
                        if (elems.isEmpty()) null else code to elems
                    }.toMap()
            } catch (_: Exception) {
                emptyMap()
            }
        }
    }
}
