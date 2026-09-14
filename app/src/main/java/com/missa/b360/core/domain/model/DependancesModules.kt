package com.missa.b360.core.domain.model

/**
 * Condition d'application d'une dépendance de modules.
 *
 * La spec §9 donne aux dépendances un attribut `condition` : certaines s'appliquent
 * dans tous les cas, d'autres seulement selon une option de configuration du profil.
 */
enum class ConditionDependance {
    /** La dépendance s'applique toujours, quelle que soit la configuration. */
    TOUJOURS,

    /** La dépendance tombe quand l'option « Vente sans stock » est activée. */
    SAUF_SI_VENTE_SANS_STOCK,
}

/**
 * Dépendance déclarative entre deux modules (spec §9 — `ModuleDependency`).
 *
 * Exemples :
 * - `ACH → STK` : tout achat alimente un stock, donc STK est requis avec ACH.
 * - `PRO → STK` : la production consomme et produit du stock.
 * - `VEN → STK` : requis par défaut, sauf si « Vente sans stock » est activée.
 */
data class ModuleDependency(
    val from: ModuleCode,
    val to: ModuleCode,
    /** `true` : la violation doit bloquer l'enregistrement de la configuration. */
    val obligatoire: Boolean = true,
    val condition: ConditionDependance = ConditionDependance.TOUJOURS,
) {
    /** Libellés courts, lisibles dans les messages d'erreur. */
    val libelleFrom: String get() = "${from.nom} (${from.code})"
    val libelleTo: String get() = "${to.nom} (${to.code})"
}

/**
 * Registre central des dépendances entre modules (spec §9 — `moduleDependencies`).
 *
 * Le lecteur le plus efficace reste `ModulesSocle.avecDependances()`, qui ajoute
 * automatiquement les modules manquants. Ce registre sert, lui, à **valider** une
 * configuration saisie à la main (profil CUSTOM) et à expliquer la règle d'or :
 * *« Tu achètes ou tu produis → t'as forcément un stock. »*
 *
 * L'exception AV (legacy, « Achat-Vente sans stock ») n'est volontairement pas
 * représentée ici : elle est un profil figé, pas une configuration à valider —
 * la validation concerne les packs modifiables (CUSTOM et ajustements manuels).
 */
object DependancesModules {

    /** Les dépendances de la règle d'or, dans l'ordre du flux métier. */
    val liste: List<ModuleDependency> = listOf(
        ModuleDependency(
            from = ModuleCode.ACH,
            to = ModuleCode.STK,
            condition = ConditionDependance.TOUJOURS,
        ),
        ModuleDependency(
            from = ModuleCode.PRO,
            to = ModuleCode.STK,
            condition = ConditionDependance.TOUJOURS,
        ),
        ModuleDependency(
            from = ModuleCode.VEN,
            to = ModuleCode.STK,
            condition = ConditionDependance.SAUF_SI_VENTE_SANS_STOCK,
        ),
    )

    /** Dépendance déclarée pour un module, ou `null`. */
    fun de(from: ModuleCode): ModuleDependency? = liste.firstOrNull { it.from == from }

    /**
     * Une dépendance est respectée si son module d'origine n'est pas activé, si
     * sa cible l'est, ou si sa condition ne s'applique pas à la configuration.
     */
    fun estRespectee(
        dependance: ModuleDependency,
        modules: Collection<ModuleCode>,
        options: OptionsConfigProfil = OptionsConfigProfil(),
    ): Boolean {
        if (dependance.from !in modules) return true
        if (dependance.to in modules) return true
        return dependance.condition == ConditionDependance.SAUF_SI_VENTE_SANS_STOCK &&
            options.venteSansStock
    }

    /** Dépendances violées par la configuration proposée. */
    fun violations(
        modules: Collection<ModuleCode>,
        options: OptionsConfigProfil = OptionsConfigProfil(),
    ): List<ModuleDependency> = liste.filter { !estRespectee(it, modules, options) }
}