package com.missa.b360.ui.clients.list

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.missa.b360.core.data.dao.ClientBalanceDao
import com.missa.b360.core.data.dao.ClientFollowupDao
import com.missa.b360.core.data.entity.ClientBalanceEntity
import com.missa.b360.core.data.entity.ClientEntity
import com.missa.b360.core.data.entity.ClientFollowupEntity
import com.missa.b360.core.domain.model.ClientListCounters
import com.missa.b360.core.domain.model.ClientListFilter
import com.missa.b360.core.domain.model.ClientListItem
import com.missa.b360.core.domain.model.ClientListRules
import com.missa.b360.core.domain.model.ClientListSort
import com.missa.b360.core.domain.usecase.ClientDefaultsUseCase
import com.missa.b360.core.domain.usecase.ObserveAllClientsUseCase
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

/** État immuable de la liste Clients ; `lignes` est déjà filtrée et triée. */
@Immutable
data class ClientListUiState(
    val chargement: Boolean = true,
    val erreur: Boolean = false,
    val lignes: List<ClientListItem> = emptyList(),
    val compteurs: ClientListCounters = ClientListCounters(),
    val requete: String = "",
    val filtre: ClientListFilter = ClientListFilter.TOUS,
    val tri: ClientListSort = ClientListSort.ENCOURS,
    val devise: String = "",
    val encoursTotal: Double = 0.0,
    val enRetardTotal: Double = 0.0,
) {
    val aucunClient: Boolean get() = !chargement && !erreur && compteurs.total == 0
    val aucunResultat: Boolean get() = !chargement && !erreur && compteurs.total > 0 && lignes.isEmpty()
}

@HiltViewModel
class ClientListViewModel @Inject constructor(
    private val savedState: SavedStateHandle,
    observeAllClients: ObserveAllClientsUseCase,
    balanceDao: ClientBalanceDao,
    followupDao: ClientFollowupDao,
    private val defaults: ClientDefaultsUseCase,
) : ViewModel() {

    private class Donnees(
        val clients: List<ClientEntity>,
        val comptes: Map<Long, ClientBalanceEntity>,
        val suivis: List<ClientFollowupEntity>,
    )

    // Recherche, puce et tri survivent à l'arrêt du processus via SavedStateHandle.
    private val requete = MutableStateFlow(savedState.get<String>(CLE_REQUETE).orEmpty())
    private val filtre = MutableStateFlow(lire(CLE_FILTRE, ClientListFilter.TOUS))
    private val tri = MutableStateFlow(lire(CLE_TRI, ClientListSort.ENCOURS))
    private val devise = MutableStateFlow("")
    private val nouvelEssai = MutableStateFlow(0)

    private val donnees: Flow<Donnees?> = nouvelEssai.flatMapLatest {
        combine(observeAllClients(), balanceDao.observeAll(), followupDao.observeAll()) { clients, comptes, suivis ->
            Donnees(clients, comptes.associateBy { it.clientId }, suivis)
        }.map<Donnees, Donnees?> { it }.catch { emit(null) }
    }

    val etat: StateFlow<ClientListUiState> = combine(donnees, requete, filtre, tri, devise) { d, q, f, t, dev ->
        construire(d, q, f, t, dev)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ClientListUiState())

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

    fun changerRequete(valeur: String) {
        val propre = valeur.take(80)
        requete.value = propre
        savedState[CLE_REQUETE] = propre
    }

    fun changerFiltre(valeur: ClientListFilter) {
        // Toucher la puce active la désactive : retour à « Tous ».
        val nouveau = if (filtre.value == valeur) ClientListFilter.TOUS else valeur
        filtre.value = nouveau
        savedState[CLE_FILTRE] = nouveau.name
    }

    fun changerTri(valeur: ClientListSort) {
        tri.value = valeur
        savedState[CLE_TRI] = valeur.name
    }

    fun reessayer() {
        nouvelEssai.value += 1
    }

    private fun construire(
        d: Donnees?,
        q: String,
        f: ClientListFilter,
        t: ClientListSort,
        dev: String,
    ): ClientListUiState {
        if (d == null) return ClientListUiState(chargement = false, erreur = true, requete = q, filtre = f, tri = t, devise = dev)
        val now = System.currentTimeMillis()
        val items = ClientListRules.construireItems(d.clients, d.comptes, d.suivis, now)
        return ClientListUiState(
            chargement = false,
            lignes = ClientListRules.trier(ClientListRules.filtrer(items, q, f, now), t),
            compteurs = ClientListRules.compteurs(items, now),
            requete = q,
            filtre = f,
            tri = t,
            devise = dev,
            encoursTotal = items.sumOf { it.encours },
            enRetardTotal = items.sumOf { it.enRetard },
        )
    }

    private inline fun <reified E : Enum<E>> lire(cle: String, defaut: E): E =
        savedState.get<String>(cle)?.let { nom -> enumValues<E>().firstOrNull { it.name == nom } } ?: defaut

    private companion object {
        const val CLE_REQUETE = "requete"
        const val CLE_FILTRE = "filtre"
        const val CLE_TRI = "tri"
    }
}
