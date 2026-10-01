package com.missa.b360.core.domain.model

import com.missa.b360.core.domain.usecase.ClientValidation

/** Ligne du fichier jugée importable. [ligne] est le numéro dans le fichier (1 = première ligne). */
data class ClientImportRow(
    val ligne: Int,
    val nom: String,
    val telephone: String,
    val email: String? = null,
    val adresse: String? = null,
)

enum class ClientImportError {
    NOM_MANQUANT,
    NOM_INVALIDE,
    TELEPHONE_MANQUANT,
    TELEPHONE_INVALIDE,
    EMAIL_INVALIDE,
    ADRESSE_INVALIDE,
    DOUBLON_FICHIER,
    TROP_DE_LIGNES,
}

data class ClientImportIssue(val ligne: Int, val erreur: ClientImportError, val extrait: String)

/** Prévisualisation : rien n'est écrit en base avant la confirmation de l'utilisateur. */
data class ClientImportPreview(
    val valides: List<ClientImportRow> = emptyList(),
    val erreurs: List<ClientImportIssue> = emptyList(),
    val lignesLues: Int = 0,
)

/** Lecture et validation pures d'un fichier CSV de clients (aucun accès base ni fichier). */
object ClientCsvImport {
    const val MAX_LIGNES = 5_000
    private val enTetesNom = setOf("nom", "name", "client")
    private val enTetesTelephone = setOf("telephone", "téléphone", "phone", "tel", "tél")
    private val enTetesEmail = setOf("email", "e-mail", "mail")
    private val enTetesAdresse = setOf("adresse", "address")

    /** Découpe une ligne en respectant les champs entre guillemets (`"Dupont; Jean";699...`). */
    fun decouper(ligne: String, separateur: Char): List<String> {
        val champs = mutableListOf<String>()
        val courant = StringBuilder()
        var entreGuillemets = false
        var i = 0
        while (i < ligne.length) {
            val c = ligne[i]
            when {
                c == '"' && entreGuillemets && i + 1 < ligne.length && ligne[i + 1] == '"' -> {
                    courant.append('"')
                    i++
                }
                c == '"' -> entreGuillemets = !entreGuillemets
                c == separateur && !entreGuillemets -> {
                    champs += courant.toString().trim()
                    courant.clear()
                }
                else -> courant.append(c)
            }
            i++
        }
        champs += courant.toString().trim()
        return champs
    }

    /** [indicatifTelephone] (ex. « +237 ») complète les numéros saisis sans indicatif. */
    fun analyser(texte: String, indicatifTelephone: String? = null): ClientImportPreview {
        val lignes = texte.removePrefix("\uFEFF").lines().withIndex().filter { it.value.isNotBlank() }
        if (lignes.isEmpty()) return ClientImportPreview()
        val premiere = lignes.first().value
        val separateur = if (premiere.count { it == ';' } >= premiere.count { it == ',' }) ';' else ','
        val entetes = decouper(premiere, separateur).map { it.lowercase() }
        val aEntete = entetes.any { it in enTetesNom || it in enTetesTelephone }
        val iNom = entetes.indexOfFirst { it in enTetesNom }.takeIf { it >= 0 } ?: 0
        val iTel = entetes.indexOfFirst { it in enTetesTelephone }.takeIf { it >= 0 } ?: 1
        val iEmail = entetes.indexOfFirst { it in enTetesEmail }
        val iAdresse = entetes.indexOfFirst { it in enTetesAdresse }

        val donnees = if (aEntete) lignes.drop(1) else lignes
        val valides = mutableListOf<ClientImportRow>()
        val erreurs = mutableListOf<ClientImportIssue>()
        val vus = HashSet<String>()
        donnees.forEachIndexed { rang, (indexBrut, brut) ->
            val numero = indexBrut + 1
            if (rang >= MAX_LIGNES) {
                if (rang == MAX_LIGNES) erreurs += ClientImportIssue(numero, ClientImportError.TROP_DE_LIGNES, "")
                return@forEachIndexed
            }
            val colonnes = decouper(brut, separateur)
            val nom = ClientValidation.normaliseNom(colonnes.getOrNull(iNom).orEmpty())
            val saisieTel = ClientValidation.filtrerTelephonePourSaisie(colonnes.getOrNull(iTel).orEmpty())
            val telephone = when {
                saisieTel.isEmpty() -> ""
                saisieTel.startsWith("+") || indicatifTelephone.isNullOrBlank() -> saisieTel
                else -> ClientValidation.telephoneAvecIndicatif(saisieTel, indicatifTelephone)
            }
            val email = if (iEmail >= 0) ClientValidation.normaliseEmail(colonnes.getOrNull(iEmail)) else null
            val adresse = if (iAdresse >= 0) ClientValidation.normaliseTexte(colonnes.getOrNull(iAdresse)) else null
            val extrait = brut.take(60)
            val erreur = when {
                nom.isEmpty() -> ClientImportError.NOM_MANQUANT
                !ClientValidation.nomEstValide(nom) -> ClientImportError.NOM_INVALIDE
                telephone.isEmpty() -> ClientImportError.TELEPHONE_MANQUANT
                !ClientValidation.telephoneEstValide(telephone) -> ClientImportError.TELEPHONE_INVALIDE
                email != null && !ClientValidation.emailEstValide(email) -> ClientImportError.EMAIL_INVALIDE
                !ClientValidation.adresseEstValide(adresse) -> ClientImportError.ADRESSE_INVALIDE
                !vus.add(ClientValidation.normaliseTelephone(telephone)) -> ClientImportError.DOUBLON_FICHIER
                else -> null
            }
            if (erreur != null) erreurs += ClientImportIssue(numero, erreur, extrait)
            else valides += ClientImportRow(numero, nom, telephone, email, adresse)
        }
        return ClientImportPreview(valides, erreurs, donnees.size)
    }
}
