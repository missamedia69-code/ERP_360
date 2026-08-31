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

    /** Pays ISO 3166 proposé à l'onboarding, avec son taux numérique et son libellé officiel. */
    data class Pays(
        val code: String,
        val nom: String,
        val tauxTaxeSuggere: Double,
        val libelleTaxe: String,
    )

    /**
     * Certains territoires appliquent une taxe composée, variable ou aucune TVA nationale.
     * Le libellé conserve l'information métier fournie, tandis que [tauxParDefaut] alimente
     * le champ éditable de l'entreprise.
     */
    data class TaxeReference(
        val tauxParDefaut: Double,
        val libelle: String,
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

    private val TAUX_PAR_DEFAUT = TaxeReference(0.0, "0 %")

    /**
     * Référentiel TVA/GST 2026 transmis pour l'application.
     * Les clés ISO 3166-1 alpha-2 rendent le taux indépendant de la langue utilisée
     * pour afficher le pays dans l'interface.
     */
    val TAXES_SUGGEREES: Map<String, TaxeReference> = mapOf(
        // Europe
        "DE" to TaxeReference(19.0, "19 %"),
        "AT" to TaxeReference(20.0, "20 %"),
        "BE" to TaxeReference(21.0, "21 %"),
        "BG" to TaxeReference(20.0, "20 %"),
        "CY" to TaxeReference(19.0, "19 %"),
        "HR" to TaxeReference(25.0, "25 %"),
        "DK" to TaxeReference(25.0, "25 %"),
        "ES" to TaxeReference(21.0, "21 %"),
        "EE" to TaxeReference(24.0, "24 %"),
        "FI" to TaxeReference(25.5, "25,50 %"),
        "FR" to TaxeReference(20.0, "20 %"),
        "GR" to TaxeReference(24.0, "24 %"),
        "HU" to TaxeReference(27.0, "27 %"),
        "IE" to TaxeReference(23.0, "23 %"),
        "IT" to TaxeReference(22.0, "22 %"),
        "LV" to TaxeReference(21.0, "21 %"),
        "LT" to TaxeReference(21.0, "21 %"),
        "LU" to TaxeReference(17.0, "17 %"),
        "MT" to TaxeReference(18.0, "18 %"),
        "NL" to TaxeReference(21.0, "21 %"),
        "PL" to TaxeReference(23.0, "23 %"),
        "PT" to TaxeReference(23.0, "23 %"),
        "CZ" to TaxeReference(21.0, "21 %"),
        "RO" to TaxeReference(21.0, "21 %"),
        "SK" to TaxeReference(23.0, "23 %"),
        "SI" to TaxeReference(22.0, "22 %"),
        "SE" to TaxeReference(25.0, "25 %"),
        "GB" to TaxeReference(20.0, "20 %"),
        "CH" to TaxeReference(8.1, "8,10 %"),
        "NO" to TaxeReference(25.0, "25 %"),
        "IS" to TaxeReference(24.0, "24 %"),
        "TR" to TaxeReference(20.0, "20 %"),
        "AL" to TaxeReference(20.0, "20 %"),
        "AD" to TaxeReference(4.5, "4,50 %"),
        "BY" to TaxeReference(20.0, "20 %"),
        "BA" to TaxeReference(17.0, "17 %"),
        "GE" to TaxeReference(18.0, "18 %"),
        "GI" to TaxeReference(15.0, "15 %"),
        "MD" to TaxeReference(20.0, "20 %"),
        "ME" to TaxeReference(21.0, "21 %"),
        "MK" to TaxeReference(18.0, "18 %"),
        "RU" to TaxeReference(20.0, "20 %"),
        "RS" to TaxeReference(20.0, "20 %"),
        "UA" to TaxeReference(20.0, "20 %"),
        "MC" to TaxeReference(0.0, "Aucune (TVA française de facto)"),

        // Afrique
        "DZ" to TaxeReference(19.0, "19 %"),
        "AO" to TaxeReference(14.0, "14 %"),
        "BJ" to TaxeReference(18.0, "18 %"),
        "BF" to TaxeReference(18.0, "18 %"),
        "CM" to TaxeReference(19.25, "19,25 %"),
        "CD" to TaxeReference(16.0, "16 %"),
        "CG" to TaxeReference(0.0, "TVA sur services numériques"),
        "CI" to TaxeReference(18.0, "18 %"),
        "EG" to TaxeReference(14.0, "14 %"),
        "ET" to TaxeReference(15.0, "15 %"),
        "GA" to TaxeReference(18.0, "18 %"),
        "GH" to TaxeReference(20.0, "~20 %"),
        "GN" to TaxeReference(18.0, "18 %"),
        "KE" to TaxeReference(16.0, "16 %"),
        "LR" to TaxeReference(0.0, "GST (variable)"),
        "ML" to TaxeReference(18.0, "18 %"),
        "MA" to TaxeReference(20.0, "20 %"),
        "MW" to TaxeReference(17.5, "17,50 %"),
        "NE" to TaxeReference(19.0, "19 %"),
        "NG" to TaxeReference(7.5, "7,50 %"),
        "RW" to TaxeReference(18.0, "18 %"),
        "SN" to TaxeReference(18.0, "18 %"),
        "SO" to TaxeReference(0.0, "Aucune"),
        "TD" to TaxeReference(18.0, "18 %"),
        "TG" to TaxeReference(18.0, "18 %"),
        "TN" to TaxeReference(19.0, "19 %"),
        "TZ" to TaxeReference(18.0, "18 %"),
        "UG" to TaxeReference(18.0, "18 %"),
        "ZA" to TaxeReference(15.0, "15 %"),
        "ZM" to TaxeReference(16.0, "16 %"),
        "ZW" to TaxeReference(15.5, "15,50 %"),
        "EH" to TaxeReference(0.0, "Aucune"),

        // Amériques
        "AR" to TaxeReference(21.0, "21 %"),
        "BO" to TaxeReference(13.0, "13 %"),
        "BR" to TaxeReference(0.0, "Système dual IBS/CBS"),
        "BS" to TaxeReference(0.0, "Aucune"),
        "BM" to TaxeReference(0.0, "Aucune"),
        "CA" to TaxeReference(5.0, "5 % (GST) + HST 13–15 %"),
        "CL" to TaxeReference(19.0, "19 %"),
        "CO" to TaxeReference(19.0, "19 %"),
        "CR" to TaxeReference(13.0, "13 %"),
        "DO" to TaxeReference(18.0, "18 %"),
        "EC" to TaxeReference(15.0, "15 %"),
        "KY" to TaxeReference(0.0, "Aucune"),
        "MX" to TaxeReference(16.0, "16 %"),
        "PA" to TaxeReference(7.0, "7 %"),
        "PE" to TaxeReference(18.0, "18 %"),
        "PY" to TaxeReference(10.0, "10 %"),
        "TC" to TaxeReference(0.0, "Aucune"),
        "US" to TaxeReference(0.0, "Aucune (Sales Tax par État)"),
        "UY" to TaxeReference(22.0, "22 %"),
        "VG" to TaxeReference(0.0, "Aucune"),
        "AI" to TaxeReference(0.0, "Aucune"),

        // Moyen-Orient
        "AE" to TaxeReference(5.0, "5 %"),
        "BH" to TaxeReference(10.0, "10 %"),
        "IL" to TaxeReference(18.0, "18 %"),
        "IR" to TaxeReference(9.0, "9 %"),
        "JO" to TaxeReference(16.0, "16 %"),
        "KW" to TaxeReference(0.0, "Aucune"),
        "LB" to TaxeReference(11.0, "11 %"),
        "OM" to TaxeReference(5.0, "5 %"),
        "QA" to TaxeReference(0.0, "Aucune"),
        "SA" to TaxeReference(15.0, "15 %"),
        "YE" to TaxeReference(5.0, "5 %"),

        // Asie-Océanie
        "AU" to TaxeReference(10.0, "10 % (GST)"),
        "BD" to TaxeReference(15.0, "15 %"),
        "BT" to TaxeReference(5.0, "5 % (GST)"),
        "CN" to TaxeReference(13.0, "13 %"),
        "FJ" to TaxeReference(15.0, "15 %"),
        "HK" to TaxeReference(0.0, "Aucune"),
        "ID" to TaxeReference(11.0, "11 %"),
        "IN" to TaxeReference(18.0, "18 % (GST)"),
        "JP" to TaxeReference(10.0, "10 %"),
        "KH" to TaxeReference(10.0, "10 %"),
        "KR" to TaxeReference(10.0, "10 %"),
        "LK" to TaxeReference(18.0, "18 %"),
        "MO" to TaxeReference(0.0, "Aucune"),
        "MV" to TaxeReference(0.0, "Aucune"),
        "MY" to TaxeReference(6.0, "6–10 % (SST)"),
        "NP" to TaxeReference(13.0, "13 %"),
        "NZ" to TaxeReference(15.0, "15 % (GST)"),
        "PH" to TaxeReference(12.0, "12 %"),
        "PK" to TaxeReference(18.0, "18 %"),
        "SG" to TaxeReference(9.0, "9 % (GST)"),
        "TH" to TaxeReference(7.0, "7 % (10 % dès oct. 2026)"),
        "TW" to TaxeReference(5.0, "5 %"),
        "VN" to TaxeReference(10.0, "10 %"),
        "VU" to TaxeReference(0.0, "Aucune"),
    )

    /**
     * Catalogue complet des pays et territoires ISO 3166-1 connus de l'appareil.
     * Les noms et le tri suivent la langue active de l'interface ; le taux est associé
     * au code ISO, et non au nom traduit du pays.
     */
    fun paysDisponibles(locale: Locale): List<Pays> =
        Locale.getISOCountries()
            .mapNotNull { code ->
                val countryLocale = Locale.Builder().setRegion(code).build()
                val nom = countryLocale.getDisplayCountry(locale)
                nom.takeIf { it.isNotBlank() }?.let {
                    val taxe = TAXES_SUGGEREES[code] ?: TAUX_PAR_DEFAUT
                    Pays(
                        code = code,
                        nom = it,
                        tauxTaxeSuggere = taxe.tauxParDefaut,
                        libelleTaxe = taxe.libelle,
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
