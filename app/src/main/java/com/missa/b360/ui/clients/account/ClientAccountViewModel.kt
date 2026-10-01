package com.missa.b360.ui.clients.account

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.missa.b360.core.data.dao.ClientBalanceDao
import com.missa.b360.core.data.dao.ClientDao
import com.missa.b360.core.data.dao.ClientFollowupDao
import com.missa.b360.core.data.dao.ClientPaymentDao
import com.missa.b360.core.data.entity.ClientBalanceEntity
import com.missa.b360.core.data.entity.ClientEntity
import com.missa.b360.core.data.entity.ClientFollowupEntity
import com.missa.b360.core.data.entity.EnterpriseEntity
import com.missa.b360.core.data.entity.FollowupStatus
import com.missa.b360.core.data.entity.FollowupType
import com.missa.b360.core.domain.model.CreditAssessment
import com.missa.b360.core.domain.model.CreditInput
import com.missa.b360.core.domain.model.CreditPolicy
import com.missa.b360.core.domain.usecase.ClientAccount
import com.missa.b360.core.domain.usecase.ClientAccountUseCase
import com.missa.b360.core.domain.usecase.ClientDefaultsUseCase
import com.missa.b360.core.domain.usecase.ClientFollowupUseCase
import com.missa.b360.core.domain.usecase.ClientPaymentUseCase
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

enum class AccountDialog { ENCAISSER, PROMESSE }

@Immutable
data class ClientAccountUiState(
    val chargement: Boolean = true,
    val erreur: Boolean = false,
    val introuvable: Boolean = false,
    val client: ClientEntity? = null,
    val balance: ClientBalanceEntity? = null,
    val evaluation: CreditAssessment? = null,
    val compte: ClientAccount? = null,
    val promesse: ClientFollowupEntity? = null,
    val entreprise: EnterpriseEntity? = null,
    val devise: String = "",
    val dialogue: AccountDialog? = null,
    /** Facture visée par l'encaissement ; `null` = imputation aux plus anciennes échéances. */
    val factureCible: Long? = null,
    val notice: ClientNotice? = null,
)

@HiltViewModel
class ClientAccountViewModel @Inject constructor(
    private val savedState: SavedStateHandle,
    clientDao: ClientDao,
    balanceDao: ClientBalanceDao,
    paymentDao: ClientPaymentDao,
    followupDao: ClientFollowupDao,
    private val account: ClientAccountUseCase,
    private val payments: ClientPaymentUseCase,
    private val followups: ClientFollowupUseCase,
    private val defaults: ClientDefaultsUseCase,
) : ViewModel() {

    private class Donnees(val client: ClientEntity?, val balance: ClientBalanceEntity?, val compte: ClientAccount?)
    private class Local(val dialogue: AccountDialog?, val cible: Long?, val notice: ClientNotice?, val devise: String, val entreprise: EnterpriseEntity?)

    val clientId: Long = savedState.get<Long>(ClientRoutes.ARG_ID) ?: -1L

    private val dialogue = MutableStateFlow(
        savedState.get<String>(CLE_DIALOGUE)?.let { nom -> AccountDialog.entries.firstOrNull { it.name == nom } },
    )
    private val cible = MutableStateFlow(savedState.get<Long>(CLE_CIBLE))
    private val notice = MutableStateFlow<ClientNotice?>(null)
    private val devise = MutableStateFlow("")
    private val entreprise = MutableStateFlow<EnterpriseEntity?>(null)
    private val nouvelEssai = MutableStateFlow(0)

    private val donnees: Flow<Donnees?> = nouvelEssai.flatMapLatest {
        // Tout changement du client, du compte ou des encaissements relance le calcul du détail.
        combine(
            clientDao.observeById(clientId),
            balanceDao.observe(clientId),
            paymentDao.observeByClient(clientId),
        ) { client, balance, _ -> Pair(client, balance) }
            .map<Pair<ClientEntity?, ClientBalanceEntity?>, Donnees?> { (client, balance) ->
                Donnees(client, balance, if (client == null) null else account(clientId))
            }
            .catch { emit(null) }
    }

    private val promesse: Flow<ClientFollowupEntity?> = followupDao.observeByClient(clientId).map { liste ->
        liste.firstOrNull { it.type == FollowupType.PROMESSE && it.statut == FollowupStatus.OUVERT }
    }

    private val local: Flow<Local> = combine(dialogue, cible, notice, devise, entreprise) { d, c, n, dev, e -> Local(d, c, n, dev, e) }

    val etat: StateFlow<ClientAccountUiState> = combine(donnees, promesse, local) { d, p, l -> construire(d, p, l) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ClientAccountUiState())

    init {
        viewModelScope.launch {
            try {
                val valeurs = defaults()
                devise.value = valeurs.devise
                entreprise.value = valeurs.entreprise
                // Les promesses échues ou honorées sont réévaluées dès l'ouverture du compte.
                followups.reevaluerPromesses()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                Unit
            }
        }
    }

    private fun construire(d: Donnees?, p: ClientFollowupEntity?, l: Local): ClientAccountUiState {
        if (d == null) return ClientAccountUiState(chargement = false, erreur = true, devise = l.devise)
        val client = d.client ?: return ClientAccountUiState(chargement = false, introuvable = true, devise = l.devise)
        val evaluation = CreditPolicy.evaluate(
            CreditInput(
                statut = client.statut,
                limiteCredit = client.limiteCredit,
                encours = d.balance?.encours ?: 0.0,
                enRetard = d.balance?.enRetard ?: 0.0,
                joursRetardMax = d.balance?.joursRetardMax ?: 0,
            ),
        )
        return ClientAccountUiState(
            chargement = false,
            client = client,
            balance = d.balance,
            evaluation = evaluation,
            compte = d.compte,
            promesse = p,
            entreprise = l.entreprise,
            devise = l.devise,
            dialogue = l.dialogue,
            factureCible = l.cible,
            notice = l.notice,
        )
    }

    fun ouvrirEncaissement(factureId: Long? = null) {
        cible.value = factureId
        savedState[CLE_CIBLE] = factureId
        ouvrir(AccountDialog.ENCAISSER)
    }

    fun ouvrirPromesse() = ouvrir(AccountDialog.PROMESSE)

    fun fermerDialogue() {
        dialogue.value = null
        savedState.remove<String>(CLE_DIALOGUE)
    }

    fun reessayer() {
        nouvelEssai.value += 1
    }

    fun noticeLue() {
        notice.value = null
    }

    fun signalerPdfErreur() {
        notice.value = ClientNotice.PDF_ERREUR
    }

    fun encaisser(montant: String, mode: String, note: String) {
        val valeur = decimal(montant)
        val factureId = cible.value
        viewModelScope.launch {
            notice.value = try {
                if (valeur == null) {
                    ClientNotice.ENCAISSEMENT_INVALIDE
                } else {
                    when (payments.encaisser(clientId, valeur, mode, factureId, note)) {
                        is ClientPaymentUseCase.Result.Succes -> {
                            fermerDialogue()
                            ClientNotice.ENCAISSEMENT_ENREGISTRE
                        }
                        ClientPaymentUseCase.Result.LicenceExpiree -> ClientNotice.LICENCE_EXPIREE
                        ClientPaymentUseCase.Result.PermissionRefusee -> ClientNotice.PERMISSION_REFUSEE
                        ClientPaymentUseCase.Result.ClientIntrouvable -> ClientNotice.ERREUR
                        ClientPaymentUseCase.Result.MontantInvalide,
                        ClientPaymentUseCase.Result.MontantSuperieurEncours,
                        ClientPaymentUseCase.Result.ModeInvalide -> ClientNotice.ENCAISSEMENT_INVALIDE
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                ClientNotice.ERREUR
            }
        }
    }

    /** Promesse de paiement à [joursAvant] jours d'aujourd'hui (le jour promis court jusqu'à minuit). */
    fun promettre(montant: String, joursAvant: Int, now: Long = System.currentTimeMillis()) {
        val valeur = decimal(montant)
        viewModelScope.launch {
            notice.value = try {
                if (valeur == null) {
                    ClientNotice.PROMESSE_INVALIDE
                } else {
                    val date = now + joursAvant.coerceAtLeast(0) * JOUR_MS
                    when (followups.enregistrer(clientId, FollowupType.PROMESSE, null, null, date, valeur, now)) {
                        is ClientFollowupUseCase.Result.Succes -> {
                            fermerDialogue()
                            ClientNotice.PROMESSE_ENREGISTREE
                        }
                        is ClientFollowupUseCase.Result.PromesseInvalide -> ClientNotice.PROMESSE_INVALIDE
                        ClientFollowupUseCase.Result.LicenceExpiree -> ClientNotice.LICENCE_EXPIREE
                        ClientFollowupUseCase.Result.PermissionRefusee -> ClientNotice.PERMISSION_REFUSEE
                        else -> ClientNotice.ERREUR
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                ClientNotice.ERREUR
            }
        }
    }

    private fun ouvrir(valeur: AccountDialog) {
        dialogue.value = valeur
        savedState[CLE_DIALOGUE] = valeur.name
    }

    private fun decimal(texte: String): Double? = texte.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }

    private companion object {
        const val CLE_DIALOGUE = "dialogue"
        const val CLE_CIBLE = "cible"
        const val JOUR_MS = 86_400_000L
    }
}
