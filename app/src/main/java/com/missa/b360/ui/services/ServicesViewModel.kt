package com.missa.b360.ui.services

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.missa.b360.core.data.entity.ClientEntity
import com.missa.b360.core.domain.model.CompteurPrestation
import com.missa.b360.core.domain.model.EtapePrestation
import com.missa.b360.core.domain.model.ModeFacturation
import com.missa.b360.core.domain.model.Prestation
import com.missa.b360.core.domain.model.PrestationRules
import com.missa.b360.core.domain.model.TresorerieRules
import com.missa.b360.core.domain.usecase.GetEnterpriseUseCase
import com.missa.b360.core.domain.usecase.ObserveClientsUseCase
import com.missa.b360.core.domain.usecase.PrestationUseCases
import com.missa.b360.core.util.Iso4217
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** État de l'écran Prestations de services. */
@HiltViewModel
class ServicesViewModel @Inject constructor(
    private val prestations: PrestationUseCases,
    observeClients: ObserveClientsUseCase,
    getEnterprise: GetEnterpriseUseCase,
) : ViewModel() {

    data class EtatServices(
        val prestations: List<Prestation> = emptyList(),
        val compteurs: List<CompteurPrestation> = emptyList(),
        val enCours: Int = 0,
        val chiffreRealise: Double = 0.0,
        val carnet: Double = 0.0,
        val heuresRealisees: Double = 0.0,
        val clients: List<ClientEntity> = emptyList(),
    )

    sealed class Message {
        data class Creee(val reference: String) : Message()
        data object Avance : Message()
        data object Annulee : Message()
        data object HeuresAjustees : Message()
        data object LectureSeule : Message()
        data object Invalide : Message()
        data object EtapeFinale : Message()
        data object Erreur : Message()
    }

    private val _message = MutableStateFlow<Message?>(null)
    val message: StateFlow<Message?> = _message

    private val _enCours = MutableStateFlow(false)
    val enCours: StateFlow<Boolean> = _enCours

    private val _filtre = MutableStateFlow<EtapePrestation?>(null)
    val filtre: StateFlow<EtapePrestation?> = _filtre

    val devise: StateFlow<String> = getEnterprise.observer()
        .map { it?.devise ?: Iso4217.DEVISE_REPLI }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Iso4217.DEVISE_REPLI)

    val etat: StateFlow<EtatServices> = combine(
        prestations.observer(),
        observeClients(),
        _filtre,
    ) { liste, clients, filtre ->
        EtatServices(
            prestations = liste.filter { filtre == null || (it.etape == filtre && !it.annulee) },
            compteurs = PrestationRules.compteurs(liste),
            enCours = PrestationRules.enCours(liste).size,
            chiffreRealise = PrestationRules.chiffreRealise(liste),
            carnet = PrestationRules.carnet(liste),
            heuresRealisees = PrestationRules.heuresRealisees(liste),
            clients = clients,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EtatServices())

    fun filtrer(etape: EtapePrestation?) {
        _filtre.value = if (_filtre.value == etape) null else etape
    }

    fun effacerMessage() { _message.value = null }

    fun creer(
        client: ClientEntity?,
        intitule: String,
        mode: ModeFacturation,
        tarifTexte: String,
        heuresTexte: String,
        intervenant: String,
        lieu: String,
    ) {
        val tarif = TresorerieRules.montantSaisi(tarifTexte) ?: run {
            _message.value = Message.Invalide
            return
        }
        val heures = heuresTexte.lireHeures() ?: run {
            _message.value = Message.Invalide
            return
        }
        lancer {
            traiter(
                prestations.creer(
                    clientId = client?.id ?: 0L,
                    clientNom = client?.nom.orEmpty(),
                    intitule = intitule,
                    mode = mode,
                    tarif = tarif,
                    heures = heures,
                    intervenant = intervenant,
                    lieu = lieu,
                ),
            )
        }
    }

    fun avancer(id: Long) = lancer { traiter(prestations.avancer(id), avance = true) }

    fun annuler(id: Long) = lancer { traiter(prestations.annuler(id), annule = true) }

    fun ajusterHeures(id: Long, heuresTexte: String) {
        val heures = heuresTexte.lireHeures() ?: run {
            _message.value = Message.Invalide
            return
        }
        lancer { traiter(prestations.ajusterHeures(id, heures), heuresAjustees = true) }
    }

    /** Un champ vide vaut zéro heure ; une saisie illisible est refusée. */
    private fun String.lireHeures(): Double? {
        if (isBlank()) return 0.0
        return trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0.0 }
    }

    private fun traiter(
        resultat: PrestationUseCases.Resultat,
        avance: Boolean = false,
        annule: Boolean = false,
        heuresAjustees: Boolean = false,
    ) {
        _message.value = when (resultat) {
            is PrestationUseCases.Resultat.Succes -> when {
                annule -> Message.Annulee
                avance -> Message.Avance
                heuresAjustees -> Message.HeuresAjustees
                else -> Message.Creee(resultat.reference)
            }
            PrestationUseCases.Resultat.LectureSeule -> Message.LectureSeule
            PrestationUseCases.Resultat.Invalide -> Message.Invalide
            PrestationUseCases.Resultat.Introuvable -> Message.Erreur
            PrestationUseCases.Resultat.EtapeFinale -> Message.EtapeFinale
        }
    }

    private fun lancer(action: suspend () -> Unit) {
        if (_enCours.value) return
        viewModelScope.launch {
            _enCours.value = true
            try {
                action()
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                _message.value = Message.Erreur
            } finally {
                _enCours.value = false
            }
        }
    }
}
