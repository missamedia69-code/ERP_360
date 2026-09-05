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
        val df = DecimalFormat("#,##0.00", FormatPrefs.symboles())
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

    /** Pays affichable dans le sélecteur d'indicatif téléphonique. */
    data class PaysAvecIndicatif(
        val code: String,
        val nom: String,
        val indicatif: String,
    )

    // Référentiel E.164 : 245 territoires disposant d'un indicatif téléphonique public.
    private val INDICATIFS_TELEPHONIQUES: Map<String, String> = """
        AC:+247,AD:+376,AE:+971,AF:+93,AG:+1,AI:+1,AL:+355,AM:+374,AO:+244,AR:+54
        AS:+1,AT:+43,AU:+61,AW:+297,AX:+358,AZ:+994,BA:+387,BB:+1,BD:+880,BE:+32
        BF:+226,BG:+359,BH:+973,BI:+257,BJ:+229,BL:+590,BM:+1,BN:+673,BO:+591,BQ:+599
        BR:+55,BS:+1,BT:+975,BW:+267,BY:+375,BZ:+501,CA:+1,CC:+61,CD:+243,CF:+236
        CG:+242,CH:+41,CI:+225,CK:+682,CL:+56,CM:+237,CN:+86,CO:+57,CR:+506,CU:+53
        CV:+238,CW:+599,CX:+61,CY:+357,CZ:+420,DE:+49,DJ:+253,DK:+45,DM:+1,DO:+1
        DZ:+213,EC:+593,EE:+372,EG:+20,EH:+212,ER:+291,ES:+34,ET:+251,FI:+358,FJ:+679
        FK:+500,FM:+691,FO:+298,FR:+33,GA:+241,GB:+44,GD:+1,GE:+995,GF:+594,GG:+44
        GH:+233,GI:+350,GL:+299,GM:+220,GN:+224,GP:+590,GQ:+240,GR:+30,GT:+502,GU:+1
        GW:+245,GY:+592,HK:+852,HN:+504,HR:+385,HT:+509,HU:+36,ID:+62,IE:+353,IL:+972
        IM:+44,IN:+91,IO:+246,IQ:+964,IR:+98,IS:+354,IT:+39,JE:+44,JM:+1,JO:+962
        JP:+81,KE:+254,KG:+996,KH:+855,KI:+686,KM:+269,KN:+1,KP:+850,KR:+82,KW:+965
        KY:+1,KZ:+7,LA:+856,LB:+961,LC:+1,LI:+423,LK:+94,LR:+231,LS:+266,LT:+370
        LU:+352,LV:+371,LY:+218,MA:+212,MC:+377,MD:+373,ME:+382,MF:+590,MG:+261,MH:+692
        MK:+389,ML:+223,MM:+95,MN:+976,MO:+853,MP:+1,MQ:+596,MR:+222,MS:+1,MT:+356
        MU:+230,MV:+960,MW:+265,MX:+52,MY:+60,MZ:+258,NA:+264,NC:+687,NE:+227,NF:+672
        NG:+234,NI:+505,NL:+31,NO:+47,NP:+977,NR:+674,NU:+683,NZ:+64,OM:+968,PA:+507
        PE:+51,PF:+689,PG:+675,PH:+63,PK:+92,PL:+48,PM:+508,PR:+1,PS:+970,PT:+351
        PW:+680,PY:+595,QA:+974,RE:+262,RO:+40,RS:+381,RU:+7,RW:+250,SA:+966,SB:+677
        SC:+248,SD:+249,SE:+46,SG:+65,SH:+290,SI:+386,SJ:+47,SK:+421,SL:+232,SM:+378
        SN:+221,SO:+252,SR:+597,SS:+211,ST:+239,SV:+503,SX:+1,SY:+963,SZ:+268,TA:+290
        TC:+1,TD:+235,TG:+228,TH:+66,TJ:+992,TK:+690,TL:+670,TM:+993,TN:+216,TO:+676
        TR:+90,TT:+1,TV:+688,TW:+886,TZ:+255,UA:+380,UG:+256,US:+1,UY:+598,UZ:+998
        VA:+39,VC:+1,VE:+58,VG:+1,VI:+1,VN:+84,VU:+678,WF:+681,WS:+685,XK:+383
        YE:+967,YT:+262,ZA:+27,ZM:+260,ZW:+263
    """.trimIndent()
        .lines()
        .flatMap { it.split(',') }
        .associate { item ->
            item.substringBefore(':') to item.substringAfter(':')
        }

    fun indicatifTelephone(codePays: String?): String? =
        codePays?.trim()?.uppercase()?.let(INDICATIFS_TELEPHONIQUES::get)

    fun paysAvecIndicatif(locale: Locale): List<PaysAvecIndicatif> =
        paysDisponibles(locale).mapNotNull { pays ->
            indicatifTelephone(pays.code)?.let { indicatif ->
                PaysAvecIndicatif(code = pays.code, nom = pays.nom, indicatif = indicatif)
            }
        }

    /** Retrouve le code ISO même si le pays a été sauvegardé dans une autre langue. */
    fun codePaysDepuisNom(nomPays: String?): String? {
        val recherche = nomPays?.trim().orEmpty()
        if (recherche.isEmpty()) return null
        recherche.uppercase().takeIf(INDICATIFS_TELEPHONIQUES::containsKey)?.let { return it }
        val locales = listOf(
            Locale.FRENCH,
            Locale.ENGLISH,
            Locale("es"),
            Locale("ar"),
            Locale.SIMPLIFIED_CHINESE,
        )
        return INDICATIFS_TELEPHONIQUES.keys.firstOrNull { code ->
            val pays = Locale.Builder().setRegion(code).build()
            locales.any { locale -> pays.getDisplayCountry(locale).equals(recherche, ignoreCase = true) }
        }
    }

    /**
     * Déduit un pays d'un numéro E.164 uniquement pour préremplir l'éditeur.
     * Les indicatifs partagés (par exemple +1) gardent le premier territoire de référence.
     */
    fun codePaysDepuisTelephone(telephone: String?): String? {
        val saisie = telephone?.trim().orEmpty()
        if (!saisie.startsWith('+')) return null
        val normalise = "+" + saisie.filter { it in '0'..'9' }
        return INDICATIFS_TELEPHONIQUES
            .entries
            .sortedByDescending { it.value.length }
            .firstOrNull { normalise.startsWith(it.value) }
            ?.key
    }

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
    private fun formatteur(avecHeure: Boolean): SimpleDateFormat {
        val motif = if (avecHeure) "${FormatPrefs.motifDate} HH:mm" else FormatPrefs.motifDate
        return SimpleDateFormat(motif, Locale.getDefault()).apply {
            timeZone = FormatPrefs.fuseau
        }
    }

    /** Date au format choisi à l'onboarding, dans le fuseau choisi. */
    fun formatDate(timestamp: Long): String = formatteur(false).format(Date(timestamp))

    /** Date + heure au format choisi à l'onboarding, dans le fuseau choisi. */
    fun formatDateHeure(timestamp: Long): String = formatteur(true).format(Date(timestamp))
}
