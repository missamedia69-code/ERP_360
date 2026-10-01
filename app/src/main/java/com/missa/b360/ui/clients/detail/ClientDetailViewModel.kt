package com.missa.b360.ui.clients.detail

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.missa.b360.core.data.dao.ClientBalanceDao
import com.missa.b360.core.data.dao.ClientDao
import com.missa.b360.core.data.dao.ClientFollowupDao
import com.missa.b360.core.data.entity.ClientAddressEntity
import com.missa.b360.core.data.entity.ClientBalanceEntity
import com.missa.b360.core.data.entity.ClientContactEntity
import com.missa.b360.core.data.entity.ClientEntity
import com.missa.b360.core.data.entity.ClientFollowupEntity
import com.missa.b360.core.data.entity.ClientStatus
import com.missa.b360.core.data.entity.FollowupChannel
import com.missa.b360.core.data.entity.FollowupType
import com.missa.b360.core.data.entity.PriceClientEntity
import com.missa.b360.core.domain.model.CreditAssessment
import com.missa.b360.core.domain.model.CreditInput
import com.missa.b360.core.domain.model.CreditPolicy
import com.missa.b360.core.domain.usecase.ActiverClientUseCase
import com.missa.b360.core.domain.usecase.ChangerStatutClientUseCase
import com.missa.b360.core.domain.usecase.ClientDefaultsUseCase
import com.missa.b360.core.domain.usecase.ClientFollowupUseCase
import com.missa.b360.core.domain.usecase.ClientLifecycleRules
import com.missa.b360.core.domain.usecase.ClientProfileUseCase
import com.missa.b360.ui.clients.ClientRoutes
import com.missa.b360.ui.clients.components.ClientNotice
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Onglets de la fiche 360 ; l'onglet courant survit à la rotation et à l'arrêt du processus. */
enum class ClientTab { ACTIVITE, COMPTE, CONTACTS, CONDITIONS, NOTES }

@Immutable
data class ClientDetailUiState(
    val chargement: Boolean = true,
    val erreur: Boolean = false,
    val introuvable: Boolean = false,
    val client: ClientEntity? = null,
    val balance: ClientBalanceEntity? = null,
    val evaluation: CreditAssessment? = null,
    val devise: String = "",
    val onglet: ClientTab = ClientTab.ACTIVITE,
    val suivis: List<ClientFollowupEntity> = emptyList(),
    val contacts: List<ClientContactEntity> = emptyList(),
    val adresses: List<ClientAddressEntity> = emptyList(),
    val prix: List<PriceClientEntity> = emptyList(),
    val transitions: List<ClientStatus> = emptyList(),
    val notice: ClientNotice? = null,
)

@HiltViewModel
class ClientDetailViewModel @Inject constructor(
    private val savedState: SavedStateHandle,
    clientDao: ClientDao,
    balanceDao: ClientBalanceDao,
    followupDao: ClientFollowupDao,
    profil: ClientProfileUseCase,
    private val defaults: ClientDefaultsUseCase,
    private val activer: ActiverClientUseCase,
    private val statutUseCase: ChangerStatutClientUseCase,
    private val followups: ClientFollowupUseCase,
) : ViewModel() {

    private class Fiche(
        val client: ClientEntity?,
        val balance: ClientBalanceEntity?,
        val suivis: List<ClientFollowupEntity>,
        val contacts: List<ClientContactEntity>,
        val adresses: List<ClientAddressEntity>,
        val prix: List<PriceClientEntity>,
    )

    private class Local(val onglet: ClientTab, val notice: ClientNotice?, val devise: String)

    val clientId: Long = savedState.get<Long>(ClientRoutes.ARG_ID) ?: -1L

    private val onglet = MutableStateFlow(
        savedState.get<String>(CLE_ONGLET)?.let { nom -> ClientTab.entries.firstOrNull { it.name == nom } } ?: ClientTab.ACTIVITE,
    )
    private val notice = MutableStateFlow<ClientNotice?>(null)
    private val devise = MutableStateFlow("")
    private val nouvelEssai = MutableStateFlow(0)

    private val fiche: Flow<Fiche?> = nouvelEssai.flatMapLatest {
        val relations = combine(
            profil.observeContacts(clientId),
            profil.observeAddresses(clientId),
            profil.observePrices(clientId),
        ) { contacts, adresses, prix -> Triple(contacts, adresses, prix) }
        combine(
            clientDao.observeById(clientId),
            balanceDao.observe(clientId),
            followupDao.observeByClient(clientId),
            relations,
        ) { client, balance, suivis, rel -> Fiche(client, balance, suivis, rel.first, rel.second, rel.third) }
            .map<Fiche, Fiche?> { it }
            .catch { emit(null) }
    }

    private val local: Flow<Local> = combine(onglet, notice, devise) { o, n, d -> Local(o, n, d) }

    val etat: StateFlow<ClientDetailUiState> = combine(fiche, local) { f, l -> construire(f, l) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ClientDetailUiState())

    init {
        viewModelScope.launch {
            try {
                devise.value = defaults().devise
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                Unit
            }
        }
    }

    private fun construire(f: Fiche?, l: Local): ClientDetailUiState {
        if (f == null) return ClientDetailUiState(chargement = false, erreur = true, onglet = l.onglet, devise = l.devise)
        val client = f.client ?: return ClientDetailUiState(chargement = false, introuvable = true, onglet = l.onglet, devise = l.devise)
        val evaluation = CreditPolicy.evaluate(
            CreditInput(
                statut = client.statut,
                limiteCredit = client.limiteCredit,
                encours = f.balance?.encours ?: 0.0,
                enRetard = f.balance?.enRetard ?: 0.0,
                joursRetardMax = f.balance?.joursRetardMax ?: 0,
            ),
        )
        return ClientDetailUiState(
            chargement = false,
            client = client,
            balance = f.balance,
            evaluation = evaluation,
            devise = l.devise,
            onglet = l.onglet,
            suivis = f.suivis,
            contacts = f.contacts,
            adresses = f.adresses,
            prix = f.prix,
            transitions = ClientLifecycleRules.transitionsDepuis(client.statut)
                .filter { ClientLifecycleRules.peutTransiter(client, it) }
                .sortedBy { it.ordinal },
            notice = l.notice,
        )
    }

    fun changerOnglet(valeur: ClientTab) {
        onglet.value = valeur
        savedState[CLE_ONGLET] = valeur.name
    }

    fun reessayer() {
        nouvelEssai.value += 1
    }

    fun noticeLue() {
        notice.value = null
    }

    /** Changement de statut ; l'écran a déjà demandé la confirmation pour les blocages et l'archivage. */
    fun changerStatut(vers: ClientStatus) {
        val client = etat.value.client ?: return
        viewModelScope.launch {
            notice.value = try {
                if (vers == ClientStatus.ACTIF && !client.active) {
                    when (activer(client.id)) {
                        ActiverClientUseCase.Result.Succes -> ClientNotice.STATUT_CHANGE
                        ActiverClientUseCase.Result.CoordonneesManquantes -> ClientNotice.COORDONNEES_MANQUANTES
                        ActiverClientUseCase.Result.InformationsFiscalesManquantes -> ClientNotice.FISCAL_MANQUANT
                        ActiverClientUseCase.Result.LicenceExpiree -> ClientNotice.LICENCE_EXPIREE
                        ActiverClientUseCase.Result.PermissionRefusee -> ClientNotice.PERMISSION_REFUSEE
                        ActiverClientUseCase.Result.Introuvable -> ClientNotice.ERREUR
                    }
                } else {
                    when (statutUseCase(client.id, vers)) {
                        ChangerStatutClientUseCase.Result.Succes -> ClientNotice.STATUT_CHANGE
                        ChangerStatutClientUseCase.Result.TransitionInterdite -> ClientNotice.TRANSITION_INTERDITE
                        ChangerStatutClientUseCase.Result.LicenceExpiree -> ClientNotice.LICENCE_EXPIREE
                        ChangerStatutClientUseCase.Result.PermissionRefusee -> ClientNotice.PERMISSION_REFUSEE
                        ChangerStatutClientUseCase.Result.Introuvable -> ClientNotice.ERREUR
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                ClientNotice.ERREUR
            }
        }
    }

    /** Enregistre la relance une fois le message envoyé par WhatsApp ou SMS. */
    fun enregistrerRelance(canal: FollowupChannel, message: String) =
        enregistrer(FollowupType.RELANCE, canal, message, ClientNotice.RELANCE_ENREGISTREE)

    fun enregistrerAppel() = enregistrer(FollowupType.APPEL, FollowupChannel.APPEL, null, null)

    fun ajouterNote(texte: String) = enregistrer(FollowupType.NOTE, null, texte, ClientNotice.NOTE_ENREGISTREE)

    private fun enregistrer(type: FollowupType, canal: FollowupChannel?, message: String?, succes: ClientNotice?) {
        val id = etat.value.client?.id ?: return
        viewModelScope.launch {
            val resultat = try {
                followups.enregistrer(id, type, canal, message)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            }
            notice.value = when (resultat) {
                is ClientFollowupUseCase.Result.Succes -> succes
                ClientFollowupUseCase.Result.LicenceExpiree -> ClientNotice.LICENCE_EXPIREE
                ClientFollowupUseCase.Result.PermissionRefusee -> ClientNotice.PERMISSION_REFUSEE
                else -> ClientNotice.ERREUR
            }
        }
    }

    private companion object {
        const val CLE_ONGLET = "onglet"
    }
}
