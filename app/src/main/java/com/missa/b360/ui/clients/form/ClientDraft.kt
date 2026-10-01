package com.missa.b360.ui.clients.form

import com.missa.b360.core.data.entity.ClientAddressEntity
import com.missa.b360.core.data.entity.ClientContactEntity
import com.missa.b360.core.data.entity.ClientEntity
import com.missa.b360.core.data.entity.ClientType
import com.missa.b360.core.domain.usecase.ClientProfileInput
import com.missa.b360.core.domain.usecase.ClientValidation
import com.missa.b360.core.util.Iso4217
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class ContactDraft(
    val nom: String = "",
    val fonction: String = "",
    val telephone: String = "",
    val email: String = "",
    val principal: Boolean = false,
)

@Serializable
data class AddressDraft(
    val libelle: String = "",
    val adresse: String = "",
    val ville: String = "",
    val principale: Boolean = false,
)

/**
 * Brouillon d'édition d'un client : que des textes et booléens, sérialisé en JSON dans le
 * `SavedStateHandle` pour survivre à la rotation et à l'arrêt du processus.
 */
@Serializable
data class ClientDraft(
    val nom: String = "",
    val type: String = ClientType.PARTICULIER.name,
    val codePays: String? = null,
    val telephoneLocal: String = "",
    val email: String = "",
    val adresse: String = "",
    val nif: String = "",
    val typeIdentifiantFiscal: String = "NIF",
    val numeroTva: String = "",
    val assujettiTva: Boolean = true,
    val exonereTva: Boolean = false,
    val motifExoneration: String = "",
    val tauxTva: String = "",
    val delaiPaiement: String = "30",
    val limiteCredit: String = "",
    val remiseDefaut: String = "0",
    val remiseMax: String = "",
    val commercial: String = "",
    val conditionsPaiement: String = "",
    val grilleTarifaire: String = "",
    val segment: String = "",
    val canalVente: String = "",
    val territoire: String = "",
    val compteComptable: String = "",
    val categorieId: Long? = null,
    val badgeId: Long? = null,
    val siteId: Long? = null,
    val notes: String = "",
    val contacts: List<ContactDraft> = emptyList(),
    val adresses: List<AddressDraft> = emptyList(),
)

/** Champs contrôlés ; chacun appartient à une section pliable pour ouvrir celle qui est en erreur. */
enum class DraftField(val section: ClientSection) {
    NOM(ClientSection.IDENTITE), TELEPHONE(ClientSection.IDENTITE), EMAIL(ClientSection.IDENTITE),
    ADRESSE(ClientSection.IDENTITE),
    TAUX_TVA(ClientSection.FISCALITE), MOTIF_EXONERATION(ClientSection.FISCALITE), FISCAL_TEXTE(ClientSection.FISCALITE),
    DELAI(ClientSection.CONDITIONS), LIMITE(ClientSection.CONDITIONS), REMISE(ClientSection.CONDITIONS),
    REMISE_MAX(ClientSection.CONDITIONS), CONDITIONS_TEXTE(ClientSection.CONDITIONS),
    CONTACTS(ClientSection.CONTACTS), ADRESSES(ClientSection.CONTACTS),
    NOTES(ClientSection.NOTES),
}

enum class ClientSection { IDENTITE, FISCALITE, CONDITIONS, CONTACTS, NOTES }

/** Demande d'enregistrement prête pour `UpdateClientUseCase`. */
data class ClientSaveRequest(
    val nom: String,
    val telephone: String,
    val type: ClientType,
    val email: String?,
    val adresse: String?,
    val categorieId: Long?,
    val siteId: Long?,
    val remiseDefautPct: Double,
    val limiteCredit: Double?,
    val badgeId: Long?,
    val notes: String?,
    val profile: ClientProfileInput,
)

/** Résultat du contrôle : soit une demande, soit la liste des champs en erreur. */
data class DraftCheck(val demande: ClientSaveRequest?, val erreurs: Set<DraftField>)

object ClientDraftMapper {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun versJson(draft: ClientDraft): String = json.encodeToString(ClientDraft.serializer(), draft)

    fun depuisJson(texte: String?): ClientDraft? =
        texte?.let { runCatching { json.decodeFromString(ClientDraft.serializer(), it) }.getOrNull() }

    fun depuis(
        client: ClientEntity,
        contacts: List<ClientContactEntity>,
        adresses: List<ClientAddressEntity>,
        codePaysParDefaut: String?,
    ): ClientDraft {
        val pays = Iso4217.codePaysDepuisTelephone(client.telephone) ?: codePaysParDefaut
        return ClientDraft(
            nom = client.nom,
            type = client.type.name,
            codePays = pays,
            telephoneLocal = ClientValidation.telephoneSansIndicatif(client.telephone, Iso4217.indicatifTelephone(pays)),
            email = client.email.orEmpty(),
            adresse = client.adresse.orEmpty(),
            nif = client.nif.orEmpty(),
            typeIdentifiantFiscal = client.typeIdentifiantFiscal ?: "NIF",
            numeroTva = client.numeroTva.orEmpty(),
            assujettiTva = client.assujettiTva,
            exonereTva = client.exonereTva,
            motifExoneration = client.motifExoneration.orEmpty(),
            tauxTva = client.tauxTva?.let { texteDecimal(it) }.orEmpty(),
            delaiPaiement = client.conditionPaiementJours.toString(),
            limiteCredit = client.limiteCredit?.let { texteDecimal(it) }.orEmpty(),
            remiseDefaut = texteDecimal(client.remiseDefautPct),
            remiseMax = client.remiseMaxPct?.let { texteDecimal(it) }.orEmpty(),
            commercial = client.commercial.orEmpty(),
            conditionsPaiement = client.conditionsPaiement.orEmpty(),
            grilleTarifaire = client.grilleTarifaire.orEmpty(),
            segment = client.segment.orEmpty(),
            canalVente = client.canalVente.orEmpty(),
            territoire = client.territoire.orEmpty(),
            compteComptable = client.compteComptable.orEmpty(),
            categorieId = client.categorieId,
            badgeId = client.badgeId,
            siteId = client.siteId,
            notes = client.notes.orEmpty(),
            contacts = contacts.map { ContactDraft(it.nom, it.fonction.orEmpty(), it.telephone.orEmpty(), it.email.orEmpty(), it.principal) },
            adresses = adresses.map { AddressDraft(it.libelle, it.adresse, it.ville.orEmpty(), it.principale) },
        )
    }

    /** Valide le brouillon ; `demande` n'existe que si aucun champ n'est en erreur. */
    fun construire(draft: ClientDraft, clientId: Long): DraftCheck {
        val erreurs = mutableSetOf<DraftField>()
        val type = ClientType.entries.firstOrNull { it.name == draft.type } ?: ClientType.PARTICULIER
        val telephone = if (draft.telephoneLocal.isBlank()) {
            ""
        } else {
            ClientValidation.telephoneAvecIndicatif(draft.telephoneLocal, Iso4217.indicatifTelephone(draft.codePays))
        }
        val email = draft.email.trim().ifBlank { null }
        if (!ClientValidation.nomEstValide(draft.nom)) erreurs += DraftField.NOM
        if (telephone.isNotEmpty() && !ClientValidation.telephoneEstValide(telephone)) erreurs += DraftField.TELEPHONE
        if (telephone.isEmpty() && email == null) erreurs += DraftField.TELEPHONE
        if (!ClientValidation.emailEstValide(email)) erreurs += DraftField.EMAIL
        if (!ClientValidation.adresseEstValide(draft.adresse)) erreurs += DraftField.ADRESSE
        if (!ClientValidation.notesSontValides(draft.notes)) erreurs += DraftField.NOTES

        val delai = draft.delaiPaiement.trim().toIntOrNull()
        if (delai == null || delai !in 0..365) erreurs += DraftField.DELAI
        val limite = decimal(draft.limiteCredit)
        if (draft.limiteCredit.isNotBlank() && (limite == null || limite < 0.0)) erreurs += DraftField.LIMITE
        val remise = if (draft.remiseDefaut.isBlank()) 0.0 else decimal(draft.remiseDefaut)
        if (remise == null || remise !in 0.0..100.0) erreurs += DraftField.REMISE
        val remiseMax = decimal(draft.remiseMax)
        if (draft.remiseMax.isNotBlank() && (remiseMax == null || remiseMax !in 0.0..100.0)) erreurs += DraftField.REMISE_MAX
        val taux = decimal(draft.tauxTva)
        if (draft.tauxTva.isNotBlank() && (taux == null || taux !in 0.0..100.0)) erreurs += DraftField.TAUX_TVA
        if (draft.exonereTva && draft.motifExoneration.isBlank()) erreurs += DraftField.MOTIF_EXONERATION

        val contacts = contactsEntites(draft.contacts, clientId)
        val adresses = adressesEntites(draft.adresses, clientId)
        val profil = ClientProfileInput(
            nif = draft.nif,
            typeIdentifiantFiscal = draft.typeIdentifiantFiscal,
            numeroTva = draft.numeroTva,
            assujettiTva = draft.assujettiTva,
            exonereTva = draft.exonereTva,
            motifExoneration = draft.motifExoneration,
            tauxTva = taux,
            commercial = draft.commercial,
            conditionPaiementJours = delai ?: 0,
            conditionsPaiement = draft.conditionsPaiement,
            grilleTarifaire = draft.grilleTarifaire,
            remiseMaxPct = remiseMax,
            segment = draft.segment,
            canalVente = draft.canalVente,
            territoire = draft.territoire,
            compteComptable = draft.compteComptable,
            contacts = contacts,
            addresses = adresses,
            replaceRelations = true,
        )
        if (erreurs.none { it == DraftField.NOM || it == DraftField.TELEPHONE || it == DraftField.EMAIL } &&
            !ClientValidation.profilEstValide(profil)
        ) {
            val contactsOk = contacts.all {
                ClientValidation.nomEstValide(it.nom) &&
                    (it.telephone.isNullOrBlank() || ClientValidation.telephoneEstValide(it.telephone)) &&
                    ClientValidation.emailEstValide(it.email)
            }
            when {
                !contactsOk -> erreurs += DraftField.CONTACTS
                !adresses.all { it.adresse.trim().length in 2..ClientValidation.LONGUEUR_ADRESSE_MAX } -> erreurs += DraftField.ADRESSES
                else -> if (erreurs.none { it.section == ClientSection.FISCALITE }) erreurs += DraftField.FISCAL_TEXTE
            }
        }
        if (erreurs.isNotEmpty()) return DraftCheck(null, erreurs)
        return DraftCheck(
            ClientSaveRequest(
                nom = draft.nom,
                telephone = telephone,
                type = type,
                email = email,
                adresse = draft.adresse.ifBlank { null },
                categorieId = draft.categorieId,
                siteId = draft.siteId,
                remiseDefautPct = remise ?: 0.0,
                limiteCredit = limite,
                badgeId = draft.badgeId,
                notes = draft.notes.ifBlank { null },
                profile = profil,
            ),
            emptySet(),
        )
    }

    /** Un seul contact principal : le premier marqué, sinon le premier de la liste. */
    fun contactsEntites(brouillons: List<ContactDraft>, clientId: Long): List<ClientContactEntity> {
        val utiles = brouillons.filter { it.nom.isNotBlank() || it.telephone.isNotBlank() || it.email.isNotBlank() }
        val principal = utiles.indexOfFirst { it.principal }.takeIf { it >= 0 } ?: 0
        return utiles.mapIndexed { index, c ->
            ClientContactEntity(
                clientId = clientId,
                nom = c.nom.trim(),
                fonction = c.fonction.trim().ifBlank { null },
                telephone = c.telephone.trim().ifBlank { null },
                email = c.email.trim().ifBlank { null },
                principal = index == principal,
            )
        }
    }

    fun adressesEntites(brouillons: List<AddressDraft>, clientId: Long): List<ClientAddressEntity> {
        val utiles = brouillons.filter { it.adresse.isNotBlank() || it.libelle.isNotBlank() || it.ville.isNotBlank() }
        val principale = utiles.indexOfFirst { it.principale }.takeIf { it >= 0 } ?: 0
        return utiles.mapIndexed { index, a ->
            ClientAddressEntity(
                clientId = clientId,
                libelle = a.libelle.trim(),
                adresse = a.adresse.trim(),
                ville = a.ville.trim().ifBlank { null },
                principale = index == principale,
            )
        }
    }

    private fun decimal(texte: String): Double? =
        texte.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }

    private fun texteDecimal(valeur: Double): String =
        if (valeur == valeur.toLong().toDouble()) valeur.toLong().toString() else valeur.toString()
}
