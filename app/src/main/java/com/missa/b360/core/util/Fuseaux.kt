package com.missa.b360.core.util

import com.missa.b360.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

/**
 * Un fuseau du catalogue : décalage UTC fixe, code usuel (PST, CET, EAT…),
 * libellé traduit et exemples de villes.
 *
 * L'identifiant [id] est un fuseau à décalage fixe (« GMT+01:00 ») : il est
 * enregistré dans les préférences et rechargé par [FormatPrefs], si bien que
 * l'affichage des dates correspond exactement au décalage choisi.
 */
data class FuseauHoraire(
    val id: String,
    val offsetMinutes: Int,
    val code: String,
    val nomRes: Int,
    val villes: String,
) {
    /** « UTC+01:00 », « UTC-03:30 »… */
    val libelleUtc: String get() = Fuseaux.formaterOffset(offsetMinutes)

    fun timeZone(): TimeZone = TimeZone.getTimeZone(id)
}

/**
 * Catalogue complet des fuseaux horaires proposés à la configuration
 * (UTC-12:00 → UTC+14:00, décalages d'une demi-heure et d'un quart d'heure
 * compris). Le décalage retenu est l'heure normale (hors heure d'été).
 */
object Fuseaux {

    val catalogue: List<FuseauHoraire> = listOf(
        creer(-720, "", R.string.fuseau_m1200, "Île Baker, Île Howland (États-Unis)"),
        creer(-660, "SST", R.string.fuseau_m1100, "Samoa américaines, Îles Midway, Niue"),
        creer(-600, "HST", R.string.fuseau_m1000, "Honolulu, Tahiti, Îles Cook"),
        creer(-570, "MART", R.string.fuseau_m0930, "Îles Marquises (Polynésie française)"),
        creer(-540, "AKST", R.string.fuseau_m0900, "Anchorage, Îles Gambier"),
        creer(-480, "PST", R.string.fuseau_m0800, "Los Angeles, Vancouver, Tijuana, Îles Pitcairn"),
        creer(-420, "MST", R.string.fuseau_m0700, "Denver, Calgary, Chihuahua, Hermosillo"),
        creer(-360, "CST", R.string.fuseau_m0600, "Chicago, Mexico, Guatemala, San José"),
        creer(-300, "EST", R.string.fuseau_m0500, "New York, Toronto, Bogotá, Lima, La Havane"),
        creer(-240, "AST", R.string.fuseau_m0400, "Halifax, Santo Domingo, Caracas, La Paz, Barbade"),
        creer(-210, "NST", R.string.fuseau_m0330, "St. John's (Canada)"),
        creer(-180, "BRT / ART", R.string.fuseau_m0300, "São Paulo, Buenos Aires, Santiago, Montevideo"),
        creer(-120, "FNT", R.string.fuseau_m0200, "Fernando de Noronha (Brésil), Géorgie du Sud"),
        creer(-60, "AZOT / CVT", R.string.fuseau_m0100, "Açores (Portugal), Cap-Vert"),
        creer(0, "GMT / WET", R.string.fuseau_p0000, "Londres, Lisbonne, Casablanca, Accra, Reykjavik"),
        creer(60, "CET / WAT", R.string.fuseau_p0100, "Paris, Berlin, Rome, Madrid, Alger, Lagos, Douala"),
        creer(120, "EET / CAT / SAST", R.string.fuseau_p0200, "Le Caire, Athènes, Bucarest, Johannesburg, Kigali"),
        creer(180, "EAT / MSK", R.string.fuseau_p0300, "Nairobi, Addis-Abeba, Moscou, Istanbul, Riyad"),
        creer(210, "IRST", R.string.fuseau_p0330, "Téhéran"),
        creer(240, "GST / SAMT", R.string.fuseau_p0400, "Dubaï, Bakou, Maurice, La Réunion, Samara"),
        creer(270, "AFT", R.string.fuseau_p0430, "Kaboul"),
        creer(300, "PKT / YEKT", R.string.fuseau_p0500, "Karachi, Tachkent, Iekaterinbourg, Maldives"),
        creer(330, "IST", R.string.fuseau_p0530, "New Delhi, Colombo"),
        creer(345, "NPT", R.string.fuseau_p0545, "Katmandou"),
        creer(360, "BST / OMST", R.string.fuseau_p0600, "Dacca, Almaty, Omsk, Thimphou"),
        creer(390, "MMT", R.string.fuseau_p0630, "Yangon, Îles Cocos"),
        creer(420, "ICT / KRAT", R.string.fuseau_p0700, "Bangkok, Hanoï, Jakarta (ouest), Krasnoïarsk"),
        creer(480, "CST / AWST", R.string.fuseau_p0800, "Pékin, Singapour, Kuala Lumpur, Perth, Manille"),
        creer(525, "ACWST", R.string.fuseau_p0845, "Eucla (Australie)"),
        creer(540, "JST / YAKT", R.string.fuseau_p0900, "Tokyo, Séoul, Dili, Iakoutsk"),
        creer(570, "ACST", R.string.fuseau_p0930, "Adélaïde, Darwin, Broken Hill"),
        creer(600, "AEST / VLAT", R.string.fuseau_p1000, "Sydney, Brisbane, Guam, Vladivostok"),
        creer(630, "LHST", R.string.fuseau_p1030, "Île Lord Howe (Australie)"),
        creer(660, "SRET / NCT", R.string.fuseau_p1100, "Nouméa, Port-Vila, Îles Salomon, Kosrae"),
        creer(720, "NZST / PETT", R.string.fuseau_p1200, "Auckland, Fidji, Îles Marshall, Kamtchatka"),
        creer(765, "CHAST", R.string.fuseau_p1245, "Îles Chatham (Nouvelle-Zélande)"),
        creer(780, "TOT / WST", R.string.fuseau_p1300, "Nuku'alofa, Apia, Îles Phoenix"),
        creer(840, "LINT", R.string.fuseau_p1400, "Kiritimati (Kiribati)"),
    )

    private fun creer(offsetMinutes: Int, code: String, nomRes: Int, villes: String) =
        FuseauHoraire(
            id = identifiant(offsetMinutes),
            offsetMinutes = offsetMinutes,
            code = code,
            nomRes = nomRes,
            villes = villes,
        )

    /** Identifiant java.util.TimeZone à décalage fixe : « GMT+05:45 ». */
    fun identifiant(offsetMinutes: Int): String {
        val signe = if (offsetMinutes < 0) "-" else "+"
        val absolu = abs(offsetMinutes)
        return String.format(Locale.US, "GMT%s%02d:%02d", signe, absolu / 60, absolu % 60)
    }

    /** Libellé lisible du décalage : « UTC-03:30 ». */
    fun formaterOffset(offsetMinutes: Int): String {
        val signe = if (offsetMinutes < 0) "-" else "+"
        val absolu = abs(offsetMinutes)
        return String.format(Locale.US, "UTC%s%02d:%02d", signe, absolu / 60, absolu % 60)
    }

    /** Fuseau du catalogue portant exactement cet identifiant. */
    fun parId(id: String?): FuseauHoraire? =
        id?.let { valeur -> catalogue.firstOrNull { it.id.equals(valeur, ignoreCase = true) } }

    /**
     * Retrouve un fuseau du catalogue à partir de n'importe quelle valeur
     * enregistrée : identifiant fixe (« GMT+01:00 ») ou identifiant IANA
     * historique (« Africa/Douala »), ramené à son décalage d'heure normale.
     */
    fun resoudre(id: String?): FuseauHoraire {
        if (id.isNullOrBlank()) return parDefaut()
        parId(id)?.let { return it }
        val zone = TimeZone.getTimeZone(id)
        // getTimeZone renvoie GMT pour un identifiant inconnu : on retombe alors
        // sur le fuseau de l'appareil plutôt que d'imposer UTC par erreur.
        if (zone.id == "GMT" && !id.startsWith("GMT", ignoreCase = true) &&
            !id.equals("UTC", ignoreCase = true)
        ) {
            return parDefaut()
        }
        return parOffset(zone.rawOffset / 60_000)
    }

    /** Fuseau du catalogue le plus proche d'un décalage donné (en minutes). */
    fun parOffset(offsetMinutes: Int): FuseauHoraire =
        catalogue.firstOrNull { it.offsetMinutes == offsetMinutes }
            ?: catalogue.minByOrNull { abs(it.offsetMinutes - offsetMinutes) }
            ?: catalogue.first { it.offsetMinutes == 0 }

    /** Fuseau déduit de l'appareil (heure normale, hors heure d'été). */
    fun parDefaut(): FuseauHoraire {
        val zone = TimeZone.getDefault()
        return parOffset(zone.rawOffset / 60_000)
    }

    /** Identifiant à enregistrer par défaut au premier lancement. */
    fun idParDefaut(): String = parDefaut().id

    /** Heure courante « 14:32 » dans le fuseau demandé. */
    fun heureCourante(id: String?): String {
        val fuseau = resoudre(id)
        val format = SimpleDateFormat("HH:mm", Locale.getDefault())
        format.timeZone = fuseau.timeZone()
        return format.format(Date())
    }
}
