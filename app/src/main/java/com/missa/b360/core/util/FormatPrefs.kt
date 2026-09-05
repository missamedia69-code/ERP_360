package com.missa.b360.core.util

import com.missa.b360.core.data.datastore.SettingsStore
import java.text.DecimalFormatSymbols
import java.util.Locale
import java.util.TimeZone

/**
 * FormatPrefs — réglages d'affichage choisis à l'onboarding (fuseau horaire,
 * format de date, style des nombres), chargés au démarrage et appliqués à
 * toute l'application (MoneyUtils, DateUtils) — harmonisation 9.1.
 *
 * Les valeurs métier (montants, horodatages) restent inchangées : seul
 * l'affichage est piloté par ces préférences.
 */
object FormatPrefs {

    /** Fuseau d'affichage des dates (défaut : appareil). */
    var fuseau: TimeZone = TimeZone.getDefault()
        private set

    /** Motif de date : dd/MM/yyyy, MM/dd/yyyy, dd.MM.yyyy, yyyy-MM-dd. */
    var motifDate: String = "dd/MM/yyyy"
        private set

    /** Style des nombres : « fr » (1 234 567,89) ou « en » (1,234,567.89). */
    var styleNombres: String = "fr"
        private set

    /** Symboles décimaux du style de nombres choisi (montants, MoneyUtils). */
    fun symboles(): DecimalFormatSymbols =
        if (styleNombres == "en") DecimalFormatSymbols(Locale.US)
        else DecimalFormatSymbols(Locale.FRENCH)

    /** Applique des valeurs fraîches (onboarding) — les null sont ignorées. */
    fun appliquer(fuseauId: String?, motif: String?, style: String?) {
        fuseauId?.takeIf { it.isNotBlank() }?.let { fuseau = TimeZone.getTimeZone(it) }
        motif?.takeIf { it.isNotBlank() }?.let { motifDate = it }
        style?.takeIf { it.isNotBlank() }?.let { styleNombres = it }
    }

    /** Charge les valeurs enregistrées (démarrage, reprise d'onboarding). */
    suspend fun charger(store: SettingsStore) {
        appliquer(
            store.get(SettingsStore.Keys.FUSEAU_HORAIRE),
            store.get(SettingsStore.Keys.FORMAT_DATE),
            store.get(SettingsStore.Keys.FORMAT_NOMBRES),
        )
    }
}
