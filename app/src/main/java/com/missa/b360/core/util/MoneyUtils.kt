package com.missa.b360.core.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * MoneyUtils — formatage des montants dans la devise de l'entreprise (RA-07).
 * Les montants ne sont **jamais convertis** (RA-07) : Double + devise ISO 4217.
 */
object MoneyUtils {

    /** Formate un montant : séparateurs de milliers, 2 décimales, symbole devise. */
    fun format(montant: Double, devise: String): String {
        val symbols = DecimalFormatSymbols(Locale.getDefault())
        val df = DecimalFormat("#,##0.00", symbols)
        return "${df.format(montant)} $devise"
    }

    /** Formate sans symbole (saisis, exports CSV/Excel). */
    fun formatBrut(montant: Double): String {
        val df = DecimalFormat("0.00", DecimalFormatSymbols(Locale.ROOT))
        return df.format(montant)
    }
}

/** Devises ISO 4217 courantes (réglage 9.1, verrou au premier usage — D4, défaut USD). */
object Iso4217 {
    data class Devise(val code: String, val nom: String)

    /** Pays ISO 3166 proposé à l'onboarding, avec une taxe éventuellement suggérée. */
    data class Pays(
        val code: String,
        val nom: String,
        val tauxTaxeSuggere: Double?,
    )

    val DEFAUT = "USD"

    val COMMUNES = listOf(
        Devise("USD", "Dollar américain"),
        Devise("EUR", "Euro"),
        Devise("XAF", "Franc CFA (BEAC)"),
        Devise("XOF", "Franc CFA (BCEAO)"),
        Devise("GBP", "Livre sterling"),
        Devise("CHF", "Franc suisse"),
        Devise("CAD", "Dollar canadien"),
        Devise("CNY", "Yuan (RMB)"),
        Devise("MAD", "Dirham marocain"),
        Devise("NGN", "Naira"),
        Devise("BRL", "Réal brésilien"),
        Devise("INR", "Roupie indienne"),
    )

    /**
     * Taux de taxe suggérés pour les pays pris en charge par le référentiel métier (D5).
     * Les clés ISO évitent de dépendre de la langue d'affichage des pays.
     */
    val TAXES_SUGGEREES = mapOf(
        "BJ" to 18.0, // Bénin
        "BF" to 18.0, // Burkina Faso
        "CM" to 19.25, // Cameroun
        "CA" to 5.0, // Canada
        "CG" to 18.0, // Congo (Brazzaville)
        "CD" to 16.0, // Congo (Kinshasa)
        "CI" to 18.0, // Côte d'Ivoire
        "ES" to 21.0, // Espagne
        "FR" to 20.0, // France
        "GA" to 18.0, // Gabon
        "GN" to 18.0, // Guinée
        "ML" to 18.0, // Mali
        "MA" to 20.0, // Maroc
        "NE" to 19.0, // Niger
        "SN" to 18.0, // Sénégal
        "TD" to 18.0, // Tchad
        "TG" to 18.0, // Togo
        "TN" to 19.0, // Tunisie
    )

    /**
     * Catalogue complet des pays et territoires ISO 3166-1 connus de l'appareil.
     * Les noms et le tri suivent la langue active de l'interface ; les données stockées
     * conservent le libellé choisi, tandis que les règles de taxe utilisent le code ISO.
     */
    fun paysDisponibles(locale: Locale): List<Pays> =
        Locale.getISOCountries()
            .mapNotNull { code ->
                val countryLocale = Locale.Builder().setRegion(code).build()
                val nom = countryLocale.getDisplayCountry(locale)
                nom.takeIf { it.isNotBlank() }?.let {
                    Pays(
                        code = code,
                        nom = it,
                        tauxTaxeSuggere = TAXES_SUGGEREES[code],
                    )
                }
            }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.nom })
}

/** Utilitaires de dates (horodatages en epoch ms). */
object DateUtils {
    private val FORMAT_DATE = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val FORMAT_DATE_HEURE = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    fun formatDate(timestamp: Long): String = FORMAT_DATE.format(Date(timestamp))

    fun formatDateHeure(timestamp: Long): String = FORMAT_DATE_HEURE.format(Date(timestamp))
}
