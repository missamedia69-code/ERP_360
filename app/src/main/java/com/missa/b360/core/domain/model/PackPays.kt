package com.missa.b360.core.domain.model

import androidx.annotation.StringRes
import com.missa.b360.R

/**
 * Nature de la taxe à la consommation du pays. Le sigle affiché est traduit :
 * « TVA » en français, « VAT » en anglais, « IVA » en espagnol…
 */
enum class TypeTaxe(@param:StringRes val libelleRes: Int) {
    TVA(R.string.fisc_taxe_tva),
    GST(R.string.fisc_taxe_gst),
    GST_HST(R.string.fisc_taxe_gst_hst),
    SST(R.string.fisc_taxe_sst),
    VENTES(R.string.fisc_taxe_ventes),
    CONSOMMATION(R.string.fisc_taxe_consommation),
    ICMS(R.string.fisc_taxe_icms),
    IGI(R.string.fisc_taxe_igi),
    ITBMS(R.string.fisc_taxe_itbms),
    AUCUNE(R.string.fisc_taxe_aucune),
}

/** Mode d'imposition des personnes physiques. */
enum class TypeImpotRevenu(@param:StringRes val libelleRes: Int) {
    PROGRESSIF(R.string.fisc_ir_progressif),
    FORFAITAIRE(R.string.fisc_ir_forfaitaire),
    AUCUN(R.string.fisc_ir_aucun),
}

/** Dispositif national de facturation électronique, quand il en existe un. */
data class EFacturation(
    val obligatoire: Boolean,
    val systeme: String,
    val format: String?,
)

/**
 * Le « pack pays » : ce que la seule sélection du pays permet de pré-remplir.
 *
 * Ni le taux de taxe standard ni sa nature ne sont répétés ici : ils vivent dans
 * [com.missa.b360.core.util.Iso4217.TAXES_SUGGEREES], seule source de vérité —
 * pas plus que la devise, déduite du référentiel ISO du système, ni la zone
 * fiscale et les identifiants légaux, portés par [ReferentielFiscal].
 *
 * @param tauxReduits taux réduits en vigueur, en pourcentage
 * @param seuilAssujettissement chiffre d'affaires à partir duquel la taxe est due, en devise locale
 * @param impotSocietes taux de droit commun de l'impôt sur les bénéfices, en pourcentage
 * @param impotSocietesMinimum imposition minimale exprimée en pourcentage du chiffre d'affaires
 * @param impotRevenuMax tranche la plus haute de l'impôt sur le revenu, en pourcentage
 */
data class PackPays(
    val code: String,
    val tauxReduits: List<Double> = emptyList(),
    val seuilAssujettissement: Long? = null,
    val impotSocietes: Double,
    val impotSocietesMinimum: Double? = null,
    val impotRevenu: TypeImpotRevenu,
    val impotRevenuMax: Double = 0.0,
    val eFacturation: EFacturation? = null,
)

/**
 * Référentiel des packs pays, transcrit du cadrage « fiscalité multi-zones »
 * (sources : PwC Tax Summaries, Commission européenne, directions générales
 * des impôts nationales). Les pays absents restent gérés par le socle commun :
 * devise ISO, taux suggéré du catalogue et zone fiscale de repli.
 */
object ReferentielPackPays {

    val TABLE: Map<String, PackPays> by lazy {
        mapOf(
            "BJ" to PackPays(
                code = "BJ",
                tauxReduits = listOf(5.0, 10.0),
                impotSocietes = 30.0,
                impotRevenu = TypeImpotRevenu.PROGRESSIF,
                impotRevenuMax = 45.0,
                eFacturation = EFacturation(obligatoire = false, systeme = "e-MECeF", format = "XML"),
            ),
            "BF" to PackPays(
                code = "BF",
                tauxReduits = listOf(5.0, 10.0),
                impotSocietes = 27.5,
                impotRevenu = TypeImpotRevenu.PROGRESSIF,
                impotRevenuMax = 45.0,
            ),
            "CI" to PackPays(
                code = "CI",
                tauxReduits = listOf(5.0, 9.0),
                seuilAssujettissement = 50_000_000,
                impotSocietes = 25.0,
                impotRevenu = TypeImpotRevenu.PROGRESSIF,
                impotRevenuMax = 48.0,
                eFacturation = EFacturation(obligatoire = true, systeme = "FNE", format = "XML/JSON"),
            ),
            "ML" to PackPays(
                code = "ML",
                tauxReduits = listOf(5.0, 10.0),
                impotSocietes = 30.0,
                impotRevenu = TypeImpotRevenu.PROGRESSIF,
                impotRevenuMax = 40.0,
            ),
            "NE" to PackPays(
                code = "NE",
                tauxReduits = listOf(5.0, 10.0),
                impotSocietes = 30.0,
                impotRevenu = TypeImpotRevenu.PROGRESSIF,
                impotRevenuMax = 40.0,
            ),
            "SN" to PackPays(
                code = "SN",
                tauxReduits = listOf(5.0, 10.0),
                impotSocietes = 30.0,
                impotRevenu = TypeImpotRevenu.PROGRESSIF,
                impotRevenuMax = 40.0,
            ),
            "TG" to PackPays(
                code = "TG",
                tauxReduits = listOf(5.0, 10.0),
                impotSocietes = 27.0,
                impotRevenu = TypeImpotRevenu.PROGRESSIF,
                impotRevenuMax = 45.0,
            ),
            "CM" to PackPays(
                code = "CM",
                tauxReduits = listOf(10.0),
                impotSocietes = 33.0,
                impotRevenu = TypeImpotRevenu.PROGRESSIF,
                impotRevenuMax = 45.0,
            ),
            "GA" to PackPays(
                code = "GA",
                tauxReduits = listOf(10.0),
                impotSocietes = 30.0,
                impotRevenu = TypeImpotRevenu.PROGRESSIF,
                impotRevenuMax = 45.0,
            ),
            "CG" to PackPays(
                code = "CG",
                tauxReduits = listOf(5.0),
                impotSocietes = 28.0,
                impotRevenu = TypeImpotRevenu.PROGRESSIF,
                impotRevenuMax = 45.0,
            ),
            "CF" to PackPays(
                code = "CF",
                tauxReduits = listOf(10.0),
                seuilAssujettissement = 30_000_000,
                impotSocietes = 30.0,
                impotRevenu = TypeImpotRevenu.PROGRESSIF,
                impotRevenuMax = 45.0,
            ),
            "TD" to PackPays(
                code = "TD",
                tauxReduits = listOf(9.0),
                impotSocietes = 30.0,
                impotRevenu = TypeImpotRevenu.PROGRESSIF,
                impotRevenuMax = 45.0,
                eFacturation = EFacturation(obligatoire = true, systeme = "FEN", format = "XML"),
            ),
            "GQ" to PackPays(
                code = "GQ",
                tauxReduits = listOf(6.0),
                impotSocietes = 25.0,
                impotSocietesMinimum = 1.5,
                impotRevenu = TypeImpotRevenu.PROGRESSIF,
                impotRevenuMax = 35.0,
            ),
            "KM" to PackPays(
                code = "KM",
                tauxReduits = listOf(3.0, 5.0, 7.5),
                impotSocietes = 35.0,
                impotRevenu = TypeImpotRevenu.PROGRESSIF,
                impotRevenuMax = 40.0,
            ),
            "GN" to PackPays(
                code = "GN",
                tauxReduits = listOf(10.0),
                seuilAssujettissement = 50_000_000,
                impotSocietes = 25.0,
                impotSocietesMinimum = 0.5,
                impotRevenu = TypeImpotRevenu.PROGRESSIF,
                impotRevenuMax = 45.0,
                eFacturation = EFacturation(obligatoire = false, systeme = "eTax", format = "Web"),
            ),
            "CD" to PackPays(
                code = "CD",
                tauxReduits = listOf(5.0, 10.0),
                impotSocietes = 30.0,
                impotRevenu = TypeImpotRevenu.PROGRESSIF,
                impotRevenuMax = 40.0,
                eFacturation = EFacturation(obligatoire = false, systeme = "DSF", format = "XML"),
            ),
            "MA" to PackPays(
                code = "MA",
                tauxReduits = listOf(7.0, 10.0, 14.0),
                seuilAssujettissement = 500_000,
                impotSocietes = 20.0,
                impotRevenu = TypeImpotRevenu.PROGRESSIF,
                impotRevenuMax = 38.0,
                eFacturation = EFacturation(obligatoire = false, systeme = "Fatora", format = "XML/JSON"),
            ),
            "TN" to PackPays(
                code = "TN",
                tauxReduits = listOf(7.0, 13.0),
                impotSocietes = 20.0,
                impotRevenu = TypeImpotRevenu.PROGRESSIF,
                impotRevenuMax = 35.0,
            ),
            "DZ" to PackPays(
                code = "DZ",
                tauxReduits = listOf(9.0),
                impotSocietes = 26.0,
                impotRevenu = TypeImpotRevenu.PROGRESSIF,
                impotRevenuMax = 35.0,
            ),
            "EG" to PackPays(
                code = "EG",
                tauxReduits = listOf(5.0),
                seuilAssujettissement = 500_000,
                impotSocietes = 22.5,
                impotRevenu = TypeImpotRevenu.PROGRESSIF,
                impotRevenuMax = 25.0,
                eFacturation = EFacturation(obligatoire = true, systeme = "ETA", format = "XML/JSON"),
            ),
            "KE" to PackPays(
                code = "KE",
                tauxReduits = listOf(8.0),
                seuilAssujettissement = 5_000_000,
                impotSocietes = 30.0,
                impotRevenu = TypeImpotRevenu.PROGRESSIF,
                impotRevenuMax = 30.0,
                eFacturation = EFacturation(obligatoire = false, systeme = "eTIMS", format = "JSON"),
            ),
            "TZ" to PackPays(
                code = "TZ",
                tauxReduits = listOf(5.0),
                impotSocietes = 30.0,
                impotRevenu = TypeImpotRevenu.PROGRESSIF,
                impotRevenuMax = 30.0,
            ),
            "UG" to PackPays(
                code = "UG",
                tauxReduits = listOf(5.0),
                impotSocietes = 30.0,
                impotRevenu = TypeImpotRevenu.PROGRESSIF,
                impotRevenuMax = 40.0,
                eFacturation = EFacturation(obligatoire = true, systeme = "EFRIS", format = "XML"),
            ),
            "NG" to PackPays(
                code = "NG",
                tauxReduits = listOf(5.0),
                seuilAssujettissement = 25_000_000,
                impotSocietes = 30.0,
                impotRevenu = TypeImpotRevenu.PROGRESSIF,
                impotRevenuMax = 24.0,
            ),
            "GH" to PackPays(
                code = "GH",
                tauxReduits = listOf(3.0, 5.0),
                impotSocietes = 25.0,
                impotRevenu = TypeImpotRevenu.PROGRESSIF,
                impotRevenuMax = 30.0,
            ),
            "ZA" to PackPays(
                code = "ZA",
                tauxReduits = listOf(5.0),
                seuilAssujettissement = 1_000_000,
                impotSocietes = 27.0,
                impotRevenu = TypeImpotRevenu.PROGRESSIF,
                impotRevenuMax = 45.0,
                eFacturation = EFacturation(obligatoire = false, systeme = "SARS", format = "Web"),
            ),
            "FR" to PackPays(
                code = "FR",
                tauxReduits = listOf(2.1, 5.5, 10.0),
                impotSocietes = 25.0,
                impotRevenu = TypeImpotRevenu.PROGRESSIF,
                impotRevenuMax = 45.0,
                eFacturation = EFacturation(obligatoire = true, systeme = "Chorus Pro", format = "Factur-X"),
            ),
            "DE" to PackPays(
                code = "DE",
                tauxReduits = listOf(7.0),
                impotSocietes = 15.0,
                impotRevenu = TypeImpotRevenu.PROGRESSIF,
                impotRevenuMax = 45.0,
                eFacturation = EFacturation(obligatoire = true, systeme = "XRechnung", format = "XRechnung/ZUGFeRD"),
            ),
            "IT" to PackPays(
                code = "IT",
                tauxReduits = listOf(5.0, 10.0),
                impotSocietes = 24.0,
                impotRevenu = TypeImpotRevenu.PROGRESSIF,
                impotRevenuMax = 43.0,
                eFacturation = EFacturation(obligatoire = true, systeme = "SdI", format = "XML"),
            ),
            "ES" to PackPays(
                code = "ES",
                tauxReduits = listOf(4.0, 10.0),
                impotSocietes = 25.0,
                impotRevenu = TypeImpotRevenu.PROGRESSIF,
                impotRevenuMax = 47.0,
                eFacturation = EFacturation(obligatoire = true, systeme = "Veri*factu", format = "XML/JSON"),
            ),
            "NL" to PackPays(
                code = "NL",
                tauxReduits = listOf(9.0),
                impotSocietes = 19.0,
                impotRevenu = TypeImpotRevenu.PROGRESSIF,
                impotRevenuMax = 49.5,
                eFacturation = EFacturation(obligatoire = false, systeme = "Peppol", format = "Peppol BIS"),
            ),
            "BE" to PackPays(
                code = "BE",
                tauxReduits = listOf(6.0, 12.0),
                impotSocietes = 25.0,
                impotRevenu = TypeImpotRevenu.PROGRESSIF,
                impotRevenuMax = 50.0,
                eFacturation = EFacturation(obligatoire = false, systeme = "Mercurius", format = "Peppol BIS"),
            ),
            "PT" to PackPays(
                code = "PT",
                tauxReduits = listOf(6.0, 13.0),
                impotSocietes = 21.0,
                impotRevenu = TypeImpotRevenu.PROGRESSIF,
                impotRevenuMax = 48.0,
                eFacturation = EFacturation(obligatoire = true, systeme = "AT", format = "XML"),
            ),
            "IE" to PackPays(
                code = "IE",
                tauxReduits = listOf(4.8, 9.0, 13.5),
                impotSocietes = 12.5,
                impotRevenu = TypeImpotRevenu.PROGRESSIF,
                impotRevenuMax = 48.0,
                eFacturation = EFacturation(obligatoire = false, systeme = "Peppol", format = "Peppol BIS"),
            ),
            "FI" to PackPays(
                code = "FI",
                tauxReduits = listOf(10.0, 14.0),
                impotSocietes = 20.0,
                impotRevenu = TypeImpotRevenu.PROGRESSIF,
                impotRevenuMax = 57.65,
                eFacturation = EFacturation(obligatoire = false, systeme = "Peppol", format = "Peppol BIS"),
            ),
            "GR" to PackPays(
                code = "GR",
                tauxReduits = listOf(6.0, 13.0),
                impotSocietes = 22.0,
                impotRevenu = TypeImpotRevenu.PROGRESSIF,
                impotRevenuMax = 44.0,
                eFacturation = EFacturation(obligatoire = true, systeme = "myDATA", format = "XML/JSON"),
            ),
            "PL" to PackPays(
                code = "PL",
                tauxReduits = listOf(5.0, 8.0),
                impotSocietes = 19.0,
                impotRevenu = TypeImpotRevenu.PROGRESSIF,
                impotRevenuMax = 32.0,
                eFacturation = EFacturation(obligatoire = true, systeme = "KSeF", format = "XML"),
            ),
            "CZ" to PackPays(
                code = "CZ",
                tauxReduits = listOf(12.0),
                impotSocietes = 21.0,
                impotRevenu = TypeImpotRevenu.PROGRESSIF,
                impotRevenuMax = 23.0,
                eFacturation = EFacturation(obligatoire = false, systeme = "Peppol", format = "Peppol BIS"),
            ),
            "SE" to PackPays(
                code = "SE",
                tauxReduits = listOf(6.0, 12.0),
                impotSocietes = 20.6,
                impotRevenu = TypeImpotRevenu.PROGRESSIF,
                impotRevenuMax = 52.0,
                eFacturation = EFacturation(obligatoire = false, systeme = "Peppol", format = "Peppol BIS"),
            ),
            "DK" to PackPays(
                code = "DK",
                impotSocietes = 22.0,
                impotRevenu = TypeImpotRevenu.PROGRESSIF,
                impotRevenuMax = 56.0,
                eFacturation = EFacturation(obligatoire = true, systeme = "OIOUBL", format = "XML"),
            ),
            "HU" to PackPays(
                code = "HU",
                tauxReduits = listOf(5.0, 18.0),
                impotSocietes = 9.0,
                impotRevenu = TypeImpotRevenu.FORFAITAIRE,
                impotRevenuMax = 15.0,
                eFacturation = EFacturation(obligatoire = true, systeme = "NAV", format = "XML"),
            ),
            "RO" to PackPays(
                code = "RO",
                tauxReduits = listOf(5.0, 9.0),
                impotSocietes = 16.0,
                impotRevenu = TypeImpotRevenu.FORFAITAIRE,
                impotRevenuMax = 10.0,
                eFacturation = EFacturation(obligatoire = true, systeme = "RO e-Factura", format = "XML"),
            ),
            "BG" to PackPays(
                code = "BG",
                tauxReduits = listOf(9.0),
                impotSocietes = 10.0,
                impotRevenu = TypeImpotRevenu.FORFAITAIRE,
                impotRevenuMax = 10.0,
                eFacturation = EFacturation(obligatoire = false, systeme = "Peppol", format = "Peppol BIS"),
            ),
            "GB" to PackPays(
                code = "GB",
                tauxReduits = listOf(5.0),
                seuilAssujettissement = 90_000,
                impotSocietes = 25.0,
                impotRevenu = TypeImpotRevenu.PROGRESSIF,
                impotRevenuMax = 45.0,
                eFacturation = EFacturation(obligatoire = false, systeme = "Peppol", format = "Peppol BIS"),
            ),
            "CH" to PackPays(
                code = "CH",
                tauxReduits = listOf(2.5, 3.8),
                seuilAssujettissement = 100_000,
                impotSocietes = 11.9,
                impotRevenu = TypeImpotRevenu.PROGRESSIF,
                impotRevenuMax = 40.0,
                eFacturation = EFacturation(obligatoire = false, systeme = "Peppol", format = "Peppol BIS"),
            ),
            "MC" to PackPays(
                code = "MC",
                tauxReduits = listOf(2.1, 5.5, 10.0),
                impotSocietes = 25.0,
                impotRevenu = TypeImpotRevenu.AUCUN,
            ),
            "AD" to PackPays(
                code = "AD",
                tauxReduits = listOf(1.0, 2.5),
                impotSocietes = 10.0,
                impotRevenu = TypeImpotRevenu.PROGRESSIF,
                impotRevenuMax = 10.0,
            ),
            "AE" to PackPays(
                code = "AE",
                seuilAssujettissement = 375_000,
                impotSocietes = 9.0,
                impotRevenu = TypeImpotRevenu.AUCUN,
                eFacturation = EFacturation(obligatoire = true, systeme = "PINT-AE", format = "Peppol PINT-AE"),
            ),
            "SA" to PackPays(
                code = "SA",
                seuilAssujettissement = 375_000,
                impotSocietes = 20.0,
                impotRevenu = TypeImpotRevenu.AUCUN,
                eFacturation = EFacturation(obligatoire = true, systeme = "ZATCA", format = "XML/JSON"),
            ),
            "OM" to PackPays(
                code = "OM",
                impotSocietes = 15.0,
                impotRevenu = TypeImpotRevenu.AUCUN,
                eFacturation = EFacturation(obligatoire = true, systeme = "Fawtara", format = "Peppol PINT-OM"),
            ),
            "QA" to PackPays(
                code = "QA",
                impotSocietes = 10.0,
                impotRevenu = TypeImpotRevenu.AUCUN,
            ),
            "KW" to PackPays(
                code = "KW",
                impotSocietes = 15.0,
                impotRevenu = TypeImpotRevenu.AUCUN,
            ),
            "BH" to PackPays(
                code = "BH",
                impotSocietes = 10.0,
                impotRevenu = TypeImpotRevenu.AUCUN,
            ),
            "CO" to PackPays(
                code = "CO",
                tauxReduits = listOf(5.0),
                impotSocietes = 35.0,
                impotRevenu = TypeImpotRevenu.PROGRESSIF,
                impotRevenuMax = 39.0,
                eFacturation = EFacturation(obligatoire = true, systeme = "DIAN", format = "XML/JSON"),
            ),
            "BR" to PackPays(
                code = "BR",
                tauxReduits = listOf(7.0, 12.0),
                impotSocietes = 34.0,
                impotRevenu = TypeImpotRevenu.PROGRESSIF,
                impotRevenuMax = 27.5,
                eFacturation = EFacturation(obligatoire = true, systeme = "NF-e", format = "XML"),
            ),
            "MX" to PackPays(
                code = "MX",
                tauxReduits = listOf(8.0),
                impotSocietes = 30.0,
                impotRevenu = TypeImpotRevenu.PROGRESSIF,
                impotRevenuMax = 35.0,
                eFacturation = EFacturation(obligatoire = true, systeme = "CFDI", format = "XML"),
            ),
            "AR" to PackPays(
                code = "AR",
                tauxReduits = listOf(10.5),
                impotSocietes = 30.0,
                impotRevenu = TypeImpotRevenu.PROGRESSIF,
                impotRevenuMax = 35.0,
                eFacturation = EFacturation(obligatoire = true, systeme = "AFIP", format = "XML"),
            ),
            "CL" to PackPays(
                code = "CL",
                impotSocietes = 27.0,
                impotRevenu = TypeImpotRevenu.PROGRESSIF,
                impotRevenuMax = 40.0,
                eFacturation = EFacturation(obligatoire = true, systeme = "SII", format = "XML"),
            ),
            "PE" to PackPays(
                code = "PE",
                impotSocietes = 29.5,
                impotRevenu = TypeImpotRevenu.PROGRESSIF,
                impotRevenuMax = 30.0,
                eFacturation = EFacturation(obligatoire = true, systeme = "SUNAT", format = "XML"),
            ),
            "UY" to PackPays(
                code = "UY",
                tauxReduits = listOf(10.0),
                impotSocietes = 25.0,
                impotRevenu = TypeImpotRevenu.PROGRESSIF,
                impotRevenuMax = 36.0,
                eFacturation = EFacturation(obligatoire = true, systeme = "DGI", format = "XML"),
            ),
            "PA" to PackPays(
                code = "PA",
                tauxReduits = listOf(5.0),
                impotSocietes = 25.0,
                impotRevenu = TypeImpotRevenu.PROGRESSIF,
                impotRevenuMax = 25.0,
            ),
            "CR" to PackPays(
                code = "CR",
                tauxReduits = listOf(1.0, 2.0, 4.0),
                impotSocietes = 30.0,
                impotRevenu = TypeImpotRevenu.PROGRESSIF,
                impotRevenuMax = 25.0,
                eFacturation = EFacturation(obligatoire = true, systeme = "Hacienda", format = "XML"),
            ),
            "IN" to PackPays(
                code = "IN",
                tauxReduits = listOf(5.0, 12.0, 28.0),
                seuilAssujettissement = 4_000_000,
                impotSocietes = 25.0,
                impotRevenu = TypeImpotRevenu.PROGRESSIF,
                impotRevenuMax = 30.0,
                eFacturation = EFacturation(obligatoire = true, systeme = "GSTN", format = "JSON"),
            ),
            "SG" to PackPays(
                code = "SG",
                seuilAssujettissement = 1_000_000,
                impotSocietes = 17.0,
                impotRevenu = TypeImpotRevenu.PROGRESSIF,
                impotRevenuMax = 22.0,
                eFacturation = EFacturation(obligatoire = false, systeme = "Peppol", format = "Peppol BIS"),
            ),
            "MY" to PackPays(
                code = "MY",
                seuilAssujettissement = 500_000,
                impotSocietes = 24.0,
                impotRevenu = TypeImpotRevenu.PROGRESSIF,
                impotRevenuMax = 30.0,
                eFacturation = EFacturation(obligatoire = true, systeme = "LHDN", format = "XML/JSON"),
            ),
            "TH" to PackPays(
                code = "TH",
                seuilAssujettissement = 1_800_000,
                impotSocietes = 20.0,
                impotRevenu = TypeImpotRevenu.PROGRESSIF,
                impotRevenuMax = 35.0,
                eFacturation = EFacturation(obligatoire = false, systeme = "RD", format = "XML/JSON"),
            ),
            "VN" to PackPays(
                code = "VN",
                tauxReduits = listOf(5.0, 8.0),
                impotSocietes = 20.0,
                impotRevenu = TypeImpotRevenu.PROGRESSIF,
                impotRevenuMax = 35.0,
                eFacturation = EFacturation(obligatoire = true, systeme = "GDT", format = "XML"),
            ),
            "ID" to PackPays(
                code = "ID",
                seuilAssujettissement = 4_800_000_000,
                impotSocietes = 22.0,
                impotRevenu = TypeImpotRevenu.PROGRESSIF,
                impotRevenuMax = 35.0,
                eFacturation = EFacturation(obligatoire = true, systeme = "DJP", format = "XML"),
            ),
            "CN" to PackPays(
                code = "CN",
                tauxReduits = listOf(6.0, 9.0),
                impotSocietes = 25.0,
                impotRevenu = TypeImpotRevenu.PROGRESSIF,
                impotRevenuMax = 45.0,
                eFacturation = EFacturation(obligatoire = true, systeme = "SAT", format = "XML/JSON"),
            ),
            "JP" to PackPays(
                code = "JP",
                tauxReduits = listOf(8.0),
                impotSocietes = 23.2,
                impotRevenu = TypeImpotRevenu.PROGRESSIF,
                impotRevenuMax = 55.95,
            ),
            "KR" to PackPays(
                code = "KR",
                impotSocietes = 24.0,
                impotRevenu = TypeImpotRevenu.PROGRESSIF,
                impotRevenuMax = 49.5,
                eFacturation = EFacturation(obligatoire = true, systeme = "NTS", format = "XML"),
            ),
            "AU" to PackPays(
                code = "AU",
                seuilAssujettissement = 75_000,
                impotSocietes = 30.0,
                impotRevenu = TypeImpotRevenu.PROGRESSIF,
                impotRevenuMax = 45.0,
                eFacturation = EFacturation(obligatoire = false, systeme = "Peppol", format = "Peppol BIS"),
            ),
            "NZ" to PackPays(
                code = "NZ",
                seuilAssujettissement = 60_000,
                impotSocietes = 28.0,
                impotRevenu = TypeImpotRevenu.PROGRESSIF,
                impotRevenuMax = 39.0,
                eFacturation = EFacturation(obligatoire = false, systeme = "Peppol", format = "Peppol BIS"),
            ),
            "US" to PackPays(
                code = "US",
                impotSocietes = 21.0,
                impotRevenu = TypeImpotRevenu.PROGRESSIF,
                impotRevenuMax = 37.0,
            ),
            "CA" to PackPays(
                code = "CA",
                seuilAssujettissement = 30_000,
                impotSocietes = 15.0,
                impotRevenu = TypeImpotRevenu.PROGRESSIF,
                impotRevenuMax = 33.0,
            ),
            "BS" to PackPays(
                code = "BS",
                impotSocietes = 0.0,
                impotRevenu = TypeImpotRevenu.AUCUN,
            ),
            "BM" to PackPays(
                code = "BM",
                impotSocietes = 0.0,
                impotRevenu = TypeImpotRevenu.AUCUN,
            ),
            "KY" to PackPays(
                code = "KY",
                impotSocietes = 0.0,
                impotRevenu = TypeImpotRevenu.AUCUN,
            ),
        )
    }

    /** Pack du pays, ou `null` lorsque le référentiel détaillé ne le couvre pas. */
    fun pack(codePays: String?): PackPays? =
        codePays?.trim()?.uppercase()?.let(TABLE::get)

    /** Pays couverts par le référentiel détaillé. */
    val paysCouverts: Set<String> get() = TABLE.keys
}
