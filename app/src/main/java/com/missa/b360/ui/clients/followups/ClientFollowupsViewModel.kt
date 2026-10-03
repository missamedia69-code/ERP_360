package com.missa.b360.ui.clients.followups

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.missa.b360.core.data.dao.ClientBalanceDao
import com.missa.b360.core.data.dao.ClientFollowupDao
import com.missa.b360.core.data.entity.ClientBalanceEntity
import com.missa.b360.core.data.entity.ClientEntity
import com.missa.b360.core.data.entity.ClientFollowupEntity
import com.missa.b360.core.data.entity.FollowupChannel
import com.missa.b360.core.data.entity.FollowupType
import com.missa.b360.core.domain.model.AgingBucket
import com.missa.b360.core.domain.model.ClientListItem
import com.missa.b360.core.domain.model.ClientListRules
import com.missa.b360.core.domain.model.ClientReminderRules
import com.missa.b360.core.domain.usecase.ClientDefaultsUseCase
import com.missa.b360.core.domain.usecase.ClientFollowupUseCase
import com.missa.b360.core.domain.usecase.ObserveAllClientsUseCase
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

@Immutable
data class ClientFollowupsUiState(
    val chargement: Boolean = true,
    val erreur: Boolean = false,
    val groupes: List<Pair<AgingBucket, List<ClientListItem>>> = emptyList(),
    val totalEnRetard: Double = 0.0,
    val nonTenues: List<ClientFollowupEntity> = emptyList(),
    val noms: Map<Long, String> = emptyMap(),
    val devise: String = "",
    /** Client dont la fenêtre de relance est ouverte. */
    val relance: ClientListItem? = null,
    val notice: ClientNotice? = null,
) {
    val vide: Boolean get() = !chargement && !erreur && groupes.isEmpty() && nonTenues.isEmpty()
}

@HiltViewModel
class ClientFollowupsViewModel @Inject constructor(
    private val savedState: SavedStateHandle,
    observeAllClients: ObserveAllClientsUseCase,
    balanceDao: ClientBalanceDao,
    followupDao: ClientFollowupDao,
    private val followups: ClientFollowupUseCase,
    private val defaults: ClientDefaultsUseCase,
) : ViewModel() {

    private class Donnees(val clients: List<ClientEntity>, val comptes: Map<Long, ClientBalanceEntity>, val suivis: List<ClientFollowupEntity>)
    private class Local(val relanceId: Long?, val notice: ClientNotice?, val devise: String)

    private val relanceId = MutableStateFlow(savedState.get<Long>(CLE_RELANCE))
    private val notice = MutableStateFlow<ClientNotice?>(null)
    private val devise = MutableStateFlow("")
    private val nouvelEssai = MutableStateFlow(0)

    private val donnees: Flow<Donnees?> = nouvelEssai.flatMapLatest {
        combine(observeAllClients(), balanceDao.observeAll(), followupDao.observeAll()) { clients, comptes, suivis ->
            Donnees(clients, comptes.associateBy { it.clientId }, suivis)
        }.map<Donnees, Donnees?> { it }.catch { emit(null) }
    }

    private val local: Flow<Local> = combine(relanceId, notice, devise) { r, n, d -> Local(r, n, d) }

    val etat: StateFlow<ClientFollowupsUiState> = combine(donnees, local) { d, l -> construire(d, l) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ClientFollowupsUiState())

    init {
        viewModelScope.launch {
            try {
                devise.value = defaults().devise
                // Une promesse honorée ou échue change de statut avant l'affichage de l'écran.
                followups.reevaluerPromesses()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                Unit
            }
        }
    }

    private fun construire(d: Donnees?, l: Local): ClientFollowupsUiState {
        if (d == null) return ClientFollowupsUiState(chargement = false, erreur = true, devise = l.devise)
        val items = ClientListRules.construireItems(d.clients, d.comptes, d.suivis, System.currentTimeMillis())
        return ClientFollowupsUiState(
            chargement = false,
            groupes = ClientReminderRules.groupes(items),
            totalEnRetard = ClientReminderRules.totalEnRetard(items),
            nonTenues = ClientReminderRules.promessesNonTenues(d.suivis),
            noms = d.clients.associate { it.id to it.nom },
            devise = l.devise,
            relance = l.relanceId?.let { id -> items.firstOrNull { it.client.id == id } },
            notice = l.notice,
        )
    }

    fun ouvrirRelance(clientId: Long) {
        relanceId.value = clientId
        savedState[CLE_RELANCE] = clientId
    }

    fun fermerRelance() {
        relanceId.value = null
        savedState.remove<Long>(CLE_RELANCE)
    }

    fun reessayer() {
        nouvelEssai.value += 1
    }

    fun noticeLue() {
        notice.value = null
    }

    /** Appelée une fois le message parti : la relance est enregistrée sans autre geste. */
    fun enregistrerRelance(canal: FollowupChannel, message: String) {
        val id = relanceId.value ?: return
        fermerRelance()
        enregistrer(id, FollowupType.RELANCE, canal, message, ClientNotice.RELANCE_ENREGISTREE)
    }

    fun enregistrerAppel(clientId: Long) = enregistrer(clientId, FollowupType.APPEL, FollowupChannel.APPEL, null, null)

    private fun enregistrer(clientId: Long, type: FollowupType, canal: FollowupChannel, message: String?, succes: ClientNotice?) {
        viewModelScope.launch {
            val resultat = try {
                followups.enregistrer(clientId, type, canal, message)
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
        const val CLE_RELANCE = "relance"
    }
}
