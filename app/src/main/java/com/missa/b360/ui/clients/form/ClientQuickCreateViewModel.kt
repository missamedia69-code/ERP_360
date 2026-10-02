package com.missa.b360.ui.clients.form

import androidx.compose.runtime.Immutable
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.missa.b360.core.data.entity.ClientEntity
import com.missa.b360.core.data.entity.ClientStatus
import com.missa.b360.core.domain.usecase.ClientBalanceUseCase
import com.missa.b360.core.domain.usecase.ClientDefaultsUseCase
import com.missa.b360.core.domain.usecase.ClientValidation
import com.missa.b360.core.domain.usecase.CreateClientUseCase
import com.missa.b360.core.domain.usecase.DetectDuplicateClientUseCase
import com.missa.b360.core.util.Iso4217
import com.missa.b360.ui.clients.components.ClientNotice
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class ClientQuickCreateUiState(
    val nom: String = "",
    val telephoneLocal: String = "",
    val codePays: String? = null,
    val erreurNom: Boolean = false,
    val erreurTelephone: Boolean = false,
    val doublon: ClientEntity? = null,
    val enCours: Boolean = false,
    val notice: ClientNotice? = null,
    /** Identifiant du client créé : l'écran navigue vers sa fiche, une seule fois. */
    val creeId: Long? = null,
    /** Cause technique de l'échec (nom de l'exception ou du résultat), affichée sous le bouton. */
    val detailErreur: String? = null,
)

/** Création rapide : nom + téléphone seulement ; la fiche naît « à compléter ». */
@HiltViewModel
class ClientQuickCreateViewModel @Inject constructor(
    private val savedState: SavedStateHandle,
    private val create: CreateClientUseCase,
    private val detecterDoublon: DetectDuplicateClientUseCase,
    private val defaults: ClientDefaultsUseCase,
    private val clientBalance: ClientBalanceUseCase,
) : ViewModel() {

    private val _etat = MutableStateFlow(
        ClientQuickCreateUiState(
            nom = savedState.get<String>(CLE_NOM).orEmpty(),
            telephoneLocal = savedState.get<String>(CLE_TEL).orEmpty(),
            codePays = savedState.get<String>(CLE_PAYS),
        ),
    )
    val etat: StateFlow<ClientQuickCreateUiState> = _etat.asStateFlow()

    init {
        if (_etat.value.codePays == null) {
            viewModelScope.launch {
                try {
                    val pays = defaults().codePays
                    _etat.update { if (it.codePays == null) it.copy(codePays = pays) else it }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    Unit
                }
            }
        }
    }

    fun changerNom(valeur: String) {
        val propre = valeur.take(ClientValidation.LONGUEUR_NOM_MAX)
        savedState[CLE_NOM] = propre
        _etat.update { it.copy(nom = propre, erreurNom = false) }
    }

    fun changerTelephone(valeur: String) {
        val propre = ClientValidation.filtrerTelephoneLocalPourSaisie(valeur)
        savedState[CLE_TEL] = propre
        _etat.update { it.copy(telephoneLocal = propre, erreurTelephone = false) }
    }

    fun changerCodePays(code: String) {
        savedState[CLE_PAYS] = code
        _etat.update { it.copy(codePays = code, erreurTelephone = false) }
    }

    fun noticeLue() = _etat.update { it.copy(notice = null) }

    fun fermerDoublon() = _etat.update { it.copy(doublon = null) }

    fun creeConsomme() = _etat.update { it.copy(creeId = null) }

    fun enregistrer(doublonConfirme: Boolean = false) {
        val courant = _etat.value
        if (courant.enCours) return
        val telephone = ClientValidation.telephoneAvecIndicatif(courant.telephoneLocal, Iso4217.indicatifTelephone(courant.codePays))
        val nomValide = ClientValidation.nomEstValide(courant.nom)
        val telephoneValide = ClientValidation.telephoneEstValide(telephone)
        if (!nomValide || !telephoneValide) {
            _etat.update { it.copy(erreurNom = !nomValide, erreurTelephone = !telephoneValide) }
            return
        }
        _etat.update { it.copy(enCours = true, doublon = null, detailErreur = null) }
        viewModelScope.launch {
            try {
                if (!doublonConfirme) {
                    val existant = detecterDoublon(telephone, courant.nom).firstOrNull()
                    if (existant != null) {
                        _etat.update { it.copy(enCours = false, doublon = existant) }
                        return@launch
                    }
                }
                val resultat = create(
                    nom = courant.nom,
                    telephone = telephone,
                    doublonConfirme = true,
                    statutInitial = ClientStatus.A_COMPLETER,
                )
                when (resultat) {
                    is CreateClientUseCase.Result.Succes -> {
                        // La liste lit client_balances : la ligne existe dès la création, à zéro.
                        runCatching { clientBalance.recalculer(resultat.clientId) }
                        _etat.update { it.copy(enCours = false, creeId = resultat.clientId) }
                    }
                    else -> _etat.update { it.copy(enCours = false, detailErreur = resultat.toString(), notice = resultat.enNotice(), erreurNom = resultat is CreateClientUseCase.Result.NomInvalide || resultat is CreateClientUseCase.Result.NomObligatoire, erreurTelephone = resultat is CreateClientUseCase.Result.TelephoneInvalide || resultat is CreateClientUseCase.Result.TelephoneObligatoire) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("ClientQuickCreate", "Création du client impossible", e)
                val detail = (e::class.java.simpleName + ": " + (e.message ?: "")).take(300)
                _etat.update { it.copy(enCours = false, detailErreur = detail, notice = ClientNotice.ERREUR) }
            }
        }
    }

    private fun CreateClientUseCase.Result.enNotice(): ClientNotice = when (this) {
        CreateClientUseCase.Result.LicenceExpiree -> ClientNotice.LICENCE_EXPIREE
        CreateClientUseCase.Result.PermissionRefusee -> ClientNotice.PERMISSION_REFUSEE
        else -> ClientNotice.ERREUR
    }

    private companion object {
        const val CLE_NOM = "nom"
        const val CLE_TEL = "tel"
        const val CLE_PAYS = "pays"
    }
}
