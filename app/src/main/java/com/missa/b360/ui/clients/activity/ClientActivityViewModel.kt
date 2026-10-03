package com.missa.b360.ui.clients.activity

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.missa.b360.core.data.dao.ClientBalanceDao
import com.missa.b360.core.data.dao.ClientDao
import com.missa.b360.core.data.dao.ClientFollowupDao
import com.missa.b360.core.data.dao.ClientPaymentDao
import com.missa.b360.core.data.dao.OperationRecordDao
import com.missa.b360.core.data.entity.ClientFollowupEntity
import com.missa.b360.core.data.entity.ClientPaymentEntity
import com.missa.b360.core.domain.model.ActivityEntry
import com.missa.b360.core.domain.model.ActivityFilter
import com.missa.b360.core.domain.model.ActivitySale
import com.missa.b360.core.domain.model.ClientActivityRules
import com.missa.b360.core.domain.model.SaleRecordCodec
import com.missa.b360.core.domain.usecase.ClientDefaultsUseCase
import com.missa.b360.ui.clients.ClientRoutes
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
data class ClientActivityUiState(
    val chargement: Boolean = true,
    val erreur: Boolean = false,
    val introuvable: Boolean = false,
    val nomClient: String = "",
    val filtre: ActivityFilter = ActivityFilter.TOUT,
    val entrees: List<ActivityEntry> = emptyList(),
    val devise: String = "",
)

@HiltViewModel
class ClientActivityViewModel @Inject constructor(
    private val savedState: SavedStateHandle,
    private val clientDao: ClientDao,
    private val operationDao: OperationRecordDao,
    balanceDao: ClientBalanceDao,
    paymentDao: ClientPaymentDao,
    followupDao: ClientFollowupDao,
    private val defaults: ClientDefaultsUseCase,
) : ViewModel() {

    private class Donnees(val nom: String?, val entrees: List<ActivityEntry>)

    val clientId: Long = savedState.get<Long>(ClientRoutes.ARG_ID) ?: -1L

    private val filtre = MutableStateFlow(
        savedState.get<String>(CLE_FILTRE)?.let { nom -> ActivityFilter.entries.firstOrNull { it.name == nom } } ?: ActivityFilter.TOUT,
    )
    private val devise = MutableStateFlow("")
    private val nouvelEssai = MutableStateFlow(0)

    private val donnees: Flow<Donnees?> = nouvelEssai.flatMapLatest {
        // Le compte change à chaque vente, avoir ou encaissement : il relance la lecture des ventes.
        combine(
            clientDao.observeById(clientId),
            balanceDao.observe(clientId),
            paymentDao.observeByClient(clientId),
            followupDao.observeByClient(clientId),
        ) { client, _, paiements, suivis -> Triple(client?.nom, paiements, suivis) }
            .map<Triple<String?, List<ClientPaymentEntity>, List<ClientFollowupEntity>>, Donnees?> { (nom, paiements, suivis) ->
                if (nom == null) {
                    Donnees(null, emptyList())
                } else {
                    Donnees(nom, ClientActivityRules.construire(ventes(), paiements, suivis))
                }
            }
            .catch { emit(null) }
    }

    val etat: StateFlow<ClientActivityUiState> = combine(donnees, filtre, devise) { d, f, dev ->
        when {
            d == null -> ClientActivityUiState(chargement = false, erreur = true, filtre = f, devise = dev)
            d.nom == null -> ClientActivityUiState(chargement = false, introuvable = true, filtre = f, devise = dev)
            else -> ClientActivityUiState(
                chargement = false,
                nomClient = d.nom,
                filtre = f,
                entrees = ClientActivityRules.filtrer(d.entrees, f),
                devise = dev,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ClientActivityUiState())

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

    private suspend fun ventes(): List<ActivitySale> =
        operationDao.getVentesValideesPourClient(clientId).mapNotNull { record ->
            val payload = SaleRecordCodec.decode(record.notes) ?: return@mapNotNull null
            ActivitySale(record.reference, record.createdAt, payload.total, avoir = payload.sourceRecordId != null)
        }

    fun choisirFiltre(valeur: ActivityFilter) {
        filtre.value = valeur
        savedState[CLE_FILTRE] = valeur.name
    }

    fun reessayer() {
        nouvelEssai.value += 1
    }

    private companion object {
        const val CLE_FILTRE = "filtre"
    }
}
