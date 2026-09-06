package com.missa.b360.core.domain.model

import androidx.annotation.StringRes
import com.missa.b360.R

/**
 * Zone fiscale d'appartenance d'un pays.
 *
 * La zone conditionne le vocabulaire fiscal affiché (identifiants, référentiel
 * comptable) et servira de socle aux évolutions suivantes : plans comptables,
 * déclarations périodiques et e-facturation.
 *
 * @property libelleRes libellé localisé de la zone.
 * @property referentielComptable référentiel comptable dominant, affiché tel quel
 *   (nom propre non traduit : SYSCOHADA, PCG…), ou `null` si non déterminé.
 */
enum class ZoneFiscale(
    @param:StringRes val libelleRes: Int,
    val referentielComptable: String? = null,
) {
    CEMAC(R.string.fisc_zone_cemac, "SYSCOHADA"),
    UEMOA(R.string.fisc_zone_uemoa, "SYSCOHADA"),
    OHADA(R.string.fisc_zone_ohada, "SYSCOHADA"),
    UE(R.string.fisc_zone_ue, "EN 16931"),
    EUROPE(R.string.fisc_zone_europe),
    GCC(R.string.fisc_zone_gcc),
    AFRIQUE_NORD(R.string.fisc_zone_afrique_nord),
    AFRIQUE(R.string.fisc_zone_afrique),
    AMERIQUE_NORD(R.string.fisc_zone_amerique_nord),
    AMERIQUE_LATINE(R.string.fisc_zone_amerique_latine),
    ASIE_PACIFIQUE(R.string.fisc_zone_asie_pacifique),
    AUTRE(R.string.fisc_zone_autre),
}

/**
 * Emplacement de stockage d'un identifiant légal dans la fiche entreprise.
 * Deux colonnes suffisent aujourd'hui : l'identifiant fiscal et l'immatriculation
 * au registre. Le nom affiché, lui, dépend du pays.
 */
enum class CleIdentifiant { FISCAL, REGISTRE }

/**
 * Règle d'un identifiant légal pour un pays donné (équivalent local d'une ligne
 * de `tax_identifier_rules`).
 *
 * @property cle colonne cible dans la fiche entreprise.
 * @property libelle désignation officielle locale (NIU, NINEA, ICE, TRN…) —
 *   nom propre volontairement non traduit.
 * @property motif expression régulière de contrôle, `null` si aucun format stable.
 * @property exemple valeur d'exemple affichée en filigrane.
 */
data class RegleIdentifiant(
    val cle: CleIdentifiant,
    val libelle: String,
    val motif: String?,
    val exemple: String,
) {
    /** Vide = non renseigné : le contrôle de format ne s'applique qu'à une saisie. */
    fun estValide(valeur: String): Boolean {
        val nettoye = valeur.trim()
        if (nettoye.isEmpty()) return true
        val motifCompile = motif ?: return true
        return runCatching { Regex(motifCompile).matches(nettoye.uppercase()) }.getOrDefault(true)
    }
}

/**
 * Référentiel fiscal par pays : zone d'appartenance et identifiants légaux exigés.
 *
 * Les données sont embarquées (application hors ligne) sous forme de table
 * compacte, à l'image du référentiel d'indicatifs téléphoniques. Un pays absent
 * de la table reçoit les libellés génériques de sa zone.
 */
object ReferentielFiscal {

    /**
     * Une ligne par pays :
     * `code|zone|libellé fiscal|motif fiscal|exemple fiscal|libellé registre|motif registre|exemple`
     *
     * Le caractère `|` sépare les champs : les motifs n'utilisent donc jamais
     * d'alternance. Un motif est soit un alias de [MOTIFS], soit une expression
     * régulière sans ancres (ajoutées à la compilation). Champ vide = non applicable.
     */
    private val TABLE = """
        CM|CEMAC|NIU|AN|P0123456789X|RCCM|RCCM|RC/DLA/2024/B/1234
        GA|CEMAC|NIF|AN|123456 A|RCCM|RCCM|LBV/2024/B/1234
        CG|CEMAC|NIU|AN|M2024110000123|RCCM|RCCM|CG/BZV/2024/B/123
        CF|CEMAC|NIF|AN|123456789|RCCM|RCCM|BGI/2024/B/123
        TD|CEMAC|NIF|AN|123456789|RCCM|RCCM|NDJ/2024/B/123
        GQ|CEMAC|NIF|AN|123456789|RCCM|RCCM|MAL/2024/B/123
        CI|UEMOA|Numéro contribuable|AN|1234567 A|RCCM|RCCM|CI-ABJ-2024-B-1234
        SN|UEMOA|NINEA|AN|005678901 2G3|RCCM|RCCM|SN-DKR-2024-B-1234
        BJ|UEMOA|IFU|D13|3202412345678|RCCM|RCCM|RB/COT/24/B/1234
        BF|UEMOA|IFU|AN|00012345A|RCCM|RCCM|BF-OUA-2024-B-1234
        ML|UEMOA|NIF|AN|081234567890|RCCM|RCCM|MA-BKO-2024-B-1234
        NE|UEMOA|NIF|AN|12345678|RCCM|RCCM|NE-NIM-2024-B-1234
        TG|UEMOA|NIF|AN|1000123456|RCCM|RCCM|TG-LOM-2024-B-1234
        GW|UEMOA|NIF|AN|12345678|RCCM|RCCM|GW-BXO-2024-B-123
        CD|OHADA|Numéro impôt|AN|A1234567X|RCCM|RCCM|CD/KIN/RCCM/24-B-1234
        GN|OHADA|NIF|AN|123456789|RCCM|RCCM|GN/CKY/2024/B/1234
        KM|OHADA|NIF|AN|123456789|RCCM|RCCM|KM/MOR/2024/B/123
        MA|AFRIQUE_NORD|Identifiant fiscal (IF)|D8|12345678|ICE|D15|001234567000089
        TN|AFRIQUE_NORD|Matricule fiscal|AN|1234567AAM000|Registre national (RNE)|AN|1234567W
        DZ|AFRIQUE_NORD|NIF|D15|000216001234567|Registre du commerce|REG|16/00-1234567B24
        EG|AFRIQUE_NORD|Tax ID|D9|123456789|Commercial register|REG|123456
        LY|AFRIQUE_NORD|NIF|AN|123456789|Registre du commerce|REG|1234567
        NG|AFRIQUE|TIN|REG|12345678-0001|CAC number|REG|RC1234567
        GH|AFRIQUE|TIN|AN|C0001234567|Registration number|REG|CS123452024
        KE|AFRIQUE|KRA PIN|AN|P051234567M|Registration number|REG|PVT-ABC1234
        TZ|AFRIQUE|TIN|REG|123-456-789|BRELA number|REG|123456789
        UG|AFRIQUE|TIN|D10|1000123456|URSB number|REG|80020001234567
        RW|AFRIQUE|TIN|D9|100123456|RDB code|REG|103456789
        ZA|AFRIQUE|Income tax number|D10|9012345678|CIPC registration|REG|2024/123456/07
        ZM|AFRIQUE|TPIN|D10|1001234567|PACRA number|REG|120240012345
        MZ|AFRIQUE|NUIT|D9|400123456|Registo comercial|REG|100123456
        AO|AFRIQUE|NIF|AN|5417123456|Registo comercial|REG|1234-56/24
        MU|AFRIQUE|TAN|D8|20123456|BRN|AN|C12345678
        ET|AFRIQUE|TIN|D10|0012345678|Business licence|REG|MT/AA/1/0012345
        SA|GCC|VAT number|D15|300012345600003|Commercial register (CR)|D10|1010123456
        AE|GCC|TRN|D15|100123456700003|Trade licence|REG|CN-1234567
        QA|GCC|TIN|AN|500012345|Commercial register (CR)|REG|123456
        KW|GCC|Tax card number|AN|123456789|Commercial licence|REG|123456
        BH|GCC|VAT account number|D15|200012345600002|CR number|REG|123456-1
        OM|GCC|VATIN|OM[0-9]{10}|OM1100123456|CR number|REG|1234567
        FR|UE|N° TVA intracommunautaire|FR[A-Z0-9]{2}[0-9]{9}|FR12345678901|SIRET|D14|12345678900012
        BE|UE|N° TVA|BE0[0-9]{9}|BE0123456789|Numéro d'entreprise (BCE)|D10|0123456789
        DE|UE|USt-IdNr.|DE[0-9]{9}|DE123456789|Handelsregisternummer|AN|HRB 12345
        ES|UE|NIF-IVA|ES[A-Z0-9][0-9]{7}[A-Z0-9]|ESB12345674|CIF|AN|B12345674
        IT|UE|Partita IVA|IT[0-9]{11}|IT12345678901|Numero REA|REG|MI-1234567
        PT|UE|NIF|PT[0-9]{9}|PT123456789|NIPC|D9|123456789
        NL|UE|Btw-nummer|NL[0-9]{9}B[0-9]{2}|NL123456789B01|KvK-nummer|D8|12345678
        LU|UE|N° TVA|LU[0-9]{8}|LU12345678|RCS|AN|B123456
        IE|UE|VAT number|IE[0-9]{7}[A-Z]{1,2}|IE1234567FA|CRO number|REG|123456
        PL|UE|NIP|PL[0-9]{10}|PL1234567890|KRS|D10|0000123456
        RO|UE|CUI|RO[0-9]{2,10}|RO12345678|Nr. ORC|REG|J40/1234/2024
        GB|EUROPE|VAT number|GB[0-9]{9}|GB123456789|Company number|AN|12345678
        GR|UE|ΑΦΜ (VAT)|EL[0-9]{9}|EL123456789|Αριθμός ΓΕΜΗ|REG|123456789000
        FI|UE|ALV-numero|FI[0-9]{8}|FI12345678|Y-tunnus|[0-9]{7}-[0-9]|1234567-8
        SE|UE|Momsregistreringsnummer|SE[0-9]{12}|SE123456789001|Organisationsnummer|[0-9]{6}-[0-9]{4}|556123-4567
        DK|UE|Momsnummer|DK[0-9]{8}|DK12345678|CVR-nummer|D8|12345678
        HU|UE|Közösségi adószám|HU[0-9]{8}|HU12345678|Adószám|[0-9]{8}-[0-9]-[0-9]{2}|12345678-2-42
        CZ|UE|DIČ|CZ[0-9]{8,10}|CZ12345678|IČO|[0-9]{8}|12345678
        BG|UE|ДДС номер|BG[0-9]{9,10}|BG123456789|ЕИК|[0-9]{9,13}|123456789
        AD|EUROPE|NRT|[A-Z][0-9]{6}[A-Z]|A123456X|Registre de Societats|REG|12345
        MC|EUROPE|N° TVA|FR[A-Z0-9]{2}[0-9]{9}|FR12345678901|RCI|REG|24S12345
        CH|EUROPE|N° TVA (IDE)|CHE[0-9]{9}|CHE123456789|N° IDE|CHE[0-9]{9}|CHE123456789
        NO|EUROPE|MVA-nummer|NO[0-9]{9}MVA|NO123456789MVA|Organisasjonsnummer|D9|123456789
        US|AMERIQUE_NORD|EIN|[0-9]{2}-?[0-9]{7}|12-3456789|State registration|REG|C1234567
        CA|AMERIQUE_NORD|Business number (BN)|D9|123456789|GST/HST number|[0-9]{9}RT[0-9]{4}|123456789RT0001
        CR|AMERIQUE_LATINE|Cédula jurídica|[0-9]{9,12}|3101123456|Registro Nacional|REG|3-101-123456
        BS|AUTRE|TIN|[0-9]{6,12}|123456789|Company number|REG|123456 B
        BM|AUTRE|TIN|[0-9]{6,12}|123456789|Registrar of Companies|REG|12345
        KY|AUTRE|TIN|[0-9]{6,12}|123456789|Company number|REG|123456
        BR|AMERIQUE_LATINE|CNPJ|[0-9./-]{14,18}|12.345.678/0001-95|Inscrição estadual|REG|123.456.789.110
        MX|AMERIQUE_LATINE|RFC|[A-ZÑ&]{3,4}[0-9]{6}[A-Z0-9]{3}|ABC010203XY1|Folio mercantil|REG|N-2024012345
        CO|AMERIQUE_LATINE|NIT|[0-9]{9,10}-?[0-9]?|900123456-7|Matrícula mercantil|REG|01234567
        CL|AMERIQUE_LATINE|RUT|[0-9]{7,8}-[0-9K]|76123456-7|Registro de comercio|REG|1234-56
        AR|AMERIQUE_LATINE|CUIT|[0-9]{2}-?[0-9]{8}-?[0-9]|30-12345678-9|Inscripción IGJ|REG|1234567
        PE|AMERIQUE_LATINE|RUC|D11|20123456789|Partida registral|REG|11223344
        PA|AMERIQUE_LATINE|RUC|REG|155612345-2-2024|Folio mercantil|REG|155612345
        UY|AMERIQUE_LATINE|RUT|D12|211234560017|N° BPS|REG|1234567
        IN|ASIE_PACIFIQUE|GSTIN|GSTIN|27ABCDE1234F1Z5|PAN|[A-Z]{5}[0-9]{4}[A-Z]|ABCDE1234F
        CN|ASIE_PACIFIQUE|USCC|[0-9A-Z]{18}|91310000MA1K35Q12X|Business licence|AN|91310000MA1K35Q12X
        SG|ASIE_PACIFIQUE|GST registration number|AN|M91234567X|UEN|AN|202412345K
        MY|ASIE_PACIFIQUE|TIN|AN|C12345678901|SSM number|D12|202401012345
        TH|ASIE_PACIFIQUE|Tax ID|D13|0105561234567|Registration number|D13|0105561234567
        VN|ASIE_PACIFIQUE|Mã số thuế (MST)|REG|0312345678|Business registration|REG|0312345678
        ID|ASIE_PACIFIQUE|NPWP|REG|01.234.567.8-901.000|NIB|D13|1234567890123
        PH|ASIE_PACIFIQUE|TIN|REG|123-456-789-000|SEC registration|REG|CS202412345
        JP|ASIE_PACIFIQUE|Invoice registration number|T[0-9]{13}|T1234567890123|Corporate number|D13|1234567890123
        KR|ASIE_PACIFIQUE|Business registration number|REG|123-45-67890|Corporate registration number|REG|110111-1234567
        AU|ASIE_PACIFIQUE|ABN|D11|51824753556|ACN|D9|123456789
        NZ|ASIE_PACIFIQUE|IRD number|AN|123456789|NZBN|D13|9429012345678
    """.trimIndent()

    /** Motifs réutilisés par plusieurs pays, sans ancres (ajoutées à la compilation). */
    private val MOTIFS = mapOf(
        "RCCM" to "[A-Z0-9/ .-]{6,30}",
        "REG" to "[A-Z0-9/ .-]{4,25}",
        "AN" to "[A-Z0-9 .-]{5,25}",
        "GSTIN" to "[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][A-Z0-9]{3}",
        "D8" to "[0-9]{8}",
        "D9" to "[0-9]{9}",
        "D10" to "[0-9]{10}",
        "D11" to "[0-9]{11}",
        "D12" to "[0-9]{12}",
        "D13" to "[0-9]{13}",
        "D14" to "[0-9]{14}",
        "D15" to "[0-9]{15}",
    )

    /** États membres de l'Union européenne, pour le repli de zone. */
    private val PAYS_UE = setOf(
        "AT", "BE", "BG", "CY", "CZ", "DE", "DK", "EE", "ES", "FI", "FR", "GR", "HR", "HU",
        "IE", "IT", "LT", "LU", "LV", "MT", "NL", "PL", "PT", "RO", "SE", "SI", "SK",
    )

    private val PAYS_EUROPE = setOf(
        "AD", "AL", "BA", "BY", "CH", "GB", "GE", "GI", "IS", "LI", "MC", "MD", "ME", "MK",
        "NO", "RS", "RU", "SM", "UA", "VA", "XK",
    )

    private val PAYS_CEMAC = setOf("CM", "CF", "CG", "GA", "GQ", "TD")

    private val PAYS_UEMOA = setOf("BF", "BJ", "CI", "GW", "ML", "NE", "SN", "TG")

    /** Membres de l'OHADA hors CEMAC et UEMOA. */
    private val PAYS_OHADA = setOf("CD", "GN", "KM")

    private val PAYS_GCC = setOf("AE", "BH", "KW", "OM", "QA", "SA")

    private val PAYS_AFRIQUE_NORD = setOf("DZ", "EG", "LY", "MA", "MR", "SD", "TN")

    private val PAYS_AMERIQUE_NORD = setOf("CA", "US")

    private val PAYS_AMERIQUE_LATINE = setOf(
        "AR", "BO", "BR", "CL", "CO", "CR", "CU", "DO", "EC", "GT", "HN", "HT", "JM", "MX",
        "NI", "PA", "PE", "PY", "SV", "TT", "UY", "VE",
    )

    private val PAYS_ASIE_PACIFIQUE = setOf(
        "AU", "BD", "BN", "CN", "FJ", "HK", "ID", "IN", "JP", "KH", "KR", "LA", "LK", "MM",
        "MN", "MO", "MV", "MY", "NP", "NZ", "PG", "PH", "PK", "SG", "TH", "TW", "VN",
    )

    /** Territoires d'Afrique subsaharienne non couverts par une zone plus précise. */
    private val PAYS_AFRIQUE = setOf(
        "AO", "BI", "BW", "CV", "DJ", "ER", "ET", "GH", "GM", "KE", "LR", "LS", "MG", "MU",
        "MW", "MZ", "NA", "NG", "RW", "SC", "SL", "SO", "SS", "SZ", "TZ", "UG", "ZA", "ZM", "ZW",
    )

    /** Résout un alias de [MOTIFS] ou accepte une expression régulière brute. */
    private fun motif(champ: String): String? =
        champ.trim().ifBlank { null }?.let { MOTIFS[it] ?: it }

    private data class FicheFiscale(val zone: ZoneFiscale, val regles: List<RegleIdentifiant>)

    private val FICHES: Map<String, FicheFiscale> = TABLE.lines()
        .map(String::trim)
        .filter(String::isNotEmpty)
        .mapNotNull { ligne ->
            val champs = ligne.split('|')
            if (champs.size < 8) return@mapNotNull null
            val zone = runCatching { ZoneFiscale.valueOf(champs[1]) }.getOrDefault(ZoneFiscale.AUTRE)
            val regles = buildList {
                add(
                    RegleIdentifiant(
                        cle = CleIdentifiant.FISCAL,
                        libelle = champs[2],
                        motif = motif(champs[3]),
                        exemple = champs[4],
                    ),
                )
                if (champs[5].isNotBlank()) {
                    add(
                        RegleIdentifiant(
                            cle = CleIdentifiant.REGISTRE,
                            libelle = champs[5],
                            motif = motif(champs[6]),
                            exemple = champs[7],
                        ),
                    )
                }
            }
            champs[0] to FicheFiscale(zone, regles)
        }
        .toMap()

    /** Zone fiscale du pays, déterminée d'abord par la table puis par appartenance. */
    fun zone(codePays: String?): ZoneFiscale {
        val code = codePays?.trim()?.uppercase().orEmpty()
        if (code.isEmpty()) return ZoneFiscale.AUTRE
        FICHES[code]?.let { return it.zone }
        return when (code) {
            in PAYS_CEMAC -> ZoneFiscale.CEMAC
            in PAYS_UEMOA -> ZoneFiscale.UEMOA
            in PAYS_OHADA -> ZoneFiscale.OHADA
            in PAYS_UE -> ZoneFiscale.UE
            in PAYS_EUROPE -> ZoneFiscale.EUROPE
            in PAYS_GCC -> ZoneFiscale.GCC
            in PAYS_AFRIQUE_NORD -> ZoneFiscale.AFRIQUE_NORD
            in PAYS_AFRIQUE -> ZoneFiscale.AFRIQUE
            in PAYS_AMERIQUE_NORD -> ZoneFiscale.AMERIQUE_NORD
            in PAYS_AMERIQUE_LATINE -> ZoneFiscale.AMERIQUE_LATINE
            in PAYS_ASIE_PACIFIQUE -> ZoneFiscale.ASIE_PACIFIQUE
            else -> ZoneFiscale.AUTRE
        }
    }

    /**
     * Règles d'identifiants du pays. Un pays non détaillé reçoit les libellés
     * génériques de sa zone, fournis par l'appelant via [libelleFiscalGenerique]
     * et [libelleRegistreGenerique] (chaînes traduites).
     */
    fun regles(
        codePays: String?,
        libelleFiscalGenerique: String,
        libelleRegistreGenerique: String,
    ): List<RegleIdentifiant> {
        val code = codePays?.trim()?.uppercase().orEmpty()
        FICHES[code]?.let { return it.regles }
        val zone = zone(code)
        val libelleFiscal = when (zone) {
            ZoneFiscale.UE -> "N° TVA intracommunautaire"
            ZoneFiscale.GCC -> "TRN"
            ZoneFiscale.CEMAC, ZoneFiscale.UEMOA, ZoneFiscale.OHADA -> "NIF"
            else -> libelleFiscalGenerique
        }
        val libelleRegistre = when (zone) {
            ZoneFiscale.CEMAC, ZoneFiscale.UEMOA, ZoneFiscale.OHADA -> "RCCM"
            else -> libelleRegistreGenerique
        }
        return listOf(
            RegleIdentifiant(CleIdentifiant.FISCAL, libelleFiscal, motif = null, exemple = ""),
            RegleIdentifiant(CleIdentifiant.REGISTRE, libelleRegistre, motif = null, exemple = ""),
        )
    }

    /** Règle d'un emplacement précis, ou `null` si le pays n'en attend pas. */
    fun regle(
        codePays: String?,
        cle: CleIdentifiant,
        libelleFiscalGenerique: String,
        libelleRegistreGenerique: String,
    ): RegleIdentifiant? =
        regles(codePays, libelleFiscalGenerique, libelleRegistreGenerique)
            .firstOrNull { it.cle == cle }

    /** Nombre de pays disposant d'une fiche détaillée (diagnostic / tests). */
    val paysDetailles: Int get() = FICHES.size

    /** Codes des pays disposant d'une fiche détaillée (diagnostic / tests). */
    val codesDetailles: Set<String> get() = FICHES.keys
}
