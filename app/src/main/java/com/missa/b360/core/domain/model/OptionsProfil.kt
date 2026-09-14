package com.missa.b360.core.domain.model

/**
 * Options globales de configuration d'un profil (spec §7.1).
 *
 * Aujourd'hui une seule option existe : **Vente sans stock**. Elle se règle à
 * l'onboarding (`SettingsStore.VENTE_SANS_STOCK`) et conditionne la dépendance
 * `VEN → STK` (voir [DependancesModules]). Le type est conçu pour accueillir de
 * futures options (ex. `production_sans_stock`) sans changer les signatures.
 */
data class OptionsConfigProfil(
    /** `true` : VEN peut fonctionner sans STK (drop, services, produits digitaux). */
    val venteSansStock: Boolean = false,
)

/** Clés canoniques des options — celles que la spec nomme `profile_options`. */
object OptionsProfil {
    const val VENTE_SANS_STOCK = "vente_sans_stock"

    /** Clés connues : utile pour ignorer des clés inconnues lors d'une relecture. */
    val clesConnues: Set<String> = setOf(VENTE_SANS_STOCK)

    /** Relecture tolérante d'une table d'options (clé → booléen). */
    fun depuis(map: Map<String, Boolean>): OptionsConfigProfil =
        OptionsConfigProfil(
            venteSansStock = map[VENTE_SANS_STOCK] == true,
        )

    /** Relecture tolérante depuis une table de chaînes (bas de données, préférences). */
    fun depuisChaines(map: Map<String, String>): OptionsConfigProfil =
        OptionsConfigProfil(
            venteSansStock = map[VENTE_SANS_STOCK]?.equals("true", ignoreCase = true) == true,
        )
}