package com.missa.b360.core.domain.model

/**
 * Violation d'une règle de dépendance lors de la configuration d'un profil.
 *
 * Chaque violation correspond à une dépendance de la règle d'or
 * ([DependancesModules]). Sa [message] est rédigée pour être affichée telle quelle
 * à l'utilisateur ; l'UI pourra la traduire via un code stable si besoin.
 */
enum class ViolationProfil(val from: ModuleCode, val to: ModuleCode) {
    ACH_SANS_STOCK(ModuleCode.ACH, ModuleCode.STK),
    PRO_SANS_STOCK(ModuleCode.PRO, ModuleCode.STK),
    VEN_SANS_STOCK(ModuleCode.VEN, ModuleCode.STK);

    /** Message francophone clair, prêt à l'affichage. */
    val message: String
        get() = when (this) {
            ACH_SANS_STOCK ->
                "Le module ${from.nom} (${from.code}) exige le module Stock (${to.code}) : " +
                    "tout achat alimente un stock."
            PRO_SANS_STOCK ->
                "Le module ${from.nom} (${from.code}) exige le module Stock (${to.code}) : " +
                    "la production consomme et produit du stock."
            VEN_SANS_STOCK ->
                "Le module ${from.nom} (${from.code}) exige le module Stock (${to.code}) " +
                    "tant que l'option « Vente sans stock » est désactivée."
        }
}

/**
 * Validation des configurations de profils (spec §7.2 et §9 — `validateProfileConfig`).
 *
 * La spec prévoit que **toute violation bloque l'enregistrement** de la configuration.
 * Exception : le profil AV (legacy « Achat-Vente sans stock ») — c'est un profil figé
 * et non une configuration à valider ; les packs modifiables, dont CUSTOM, n'y
 * échappent pas.
 *
 * Deux formes sont offertes :
 * - `validateProfileConfig` : la forme exacte de la spec, retourne les messages.
 * - `violations` : la forme programmatique, retourne des [ViolationProfil] typés.
 */
object ValidationProfil {

    /** Forme canonique de la spec §9. */
    fun validateProfileConfig(
        modules: Set<ModuleCode>,
        options: Map<String, Boolean> = emptyMap(),
    ): List<String> =
        violations(modules, OptionsProfil.depuis(options)).map { it.message }

    /** Forme programmatique : violations typées d'une configuration proposée. */
    fun violations(
        modules: Collection<ModuleCode>,
        options: OptionsConfigProfil = OptionsConfigProfil(),
    ): List<ViolationProfil> {
        val set = modules.toSet()
        val resultat = mutableListOf<ViolationProfil>()
        if (ModuleCode.ACH in set && ModuleCode.STK !in set) resultat += ViolationProfil.ACH_SANS_STOCK
        if (ModuleCode.PRO in set && ModuleCode.STK !in set) resultat += ViolationProfil.PRO_SANS_STOCK
        if (ModuleCode.VEN in set && ModuleCode.STK !in set && !options.venteSansStock) {
            resultat += ViolationProfil.VEN_SANS_STOCK
        }
        return resultat
    }

    /** `true` si la configuration proposée est valide au regard de la règle d'or. */
    fun estValide(
        modules: Collection<ModuleCode>,
        options: OptionsConfigProfil = OptionsConfigProfil(),
    ): Boolean = violations(modules, options).isEmpty()
}