package com.missa.b360.ui.projets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.missa.b360.core.data.entity.ClientEntity
import com.missa.b360.core.domain.model.CompteurProjet
import com.missa.b360.core.domain.model.EtatProjet
import com.missa.b360.core.domain.model.Projet
import com.missa.b360.core.domain.model.ProjetRules
import com.missa.b360.core.domain.model.TresorerieRules
import com.missa.b360.core.domain.usecase.GetEnterpriseUseCase
import com.missa.b360.core.domain.usecase.ObserveClientsUseCase
import com.missa.b360.core.domain.usecase.ProjetUseCases
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

/** État de l'écran Projets. */
@HiltViewModel
class ProjetsViewModel @Inject constructor(
    private val projets: ProjetUseCases,
    observeClients: ObserveClientsUseCase,
    getEnterprise: GetEnterpriseUseCase,
) : ViewModel() {

    data class EtatProjets(
        val projets: List<Projet> = emptyList(),
        val compteurs: List<CompteurProjet> = emptyList(),
        val budgetTotal: Double = 0.0,
        val consommeTotal: Double = 0.0,
        val avancementMoyen: Double = 0.0,
        val enDerive: Int = 0,
        val enRetard: Int = 0,
        val clients: List<ClientEntity> = emptyList(),
    )

    sealed class Message {
        data class Cree(val reference: String) : Message()
        data object Actualise : Message()
        data object EtatChange : Message()
        data object Annule : Message()
        data object LectureSeule : Message()
        data object Invalide : Message()
        data object Erreur : Message()
    }

    private val _message = MutableStateFlow<Message?>(null)
    val message: StateFlow<Message?> = _message

    private val _enCours = MutableStateFlow(false)
    val enCours: StateFlow<Boolean> = _enCours

    private val _filtre = MutableStateFlow<EtatProjet?>(null)
    val filtre: StateFlow<EtatProjet?> = _filtre

    val devise: StateFlow<String> = getEnterprise.observer()
        .map { it?.devise ?: Iso4217.DEVISE_REPLI }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Iso4217.DEVISE_REPLI)

    val etat: StateFlow<EtatProjets> = combine(
        projets.observer(),
        observeClients(),
        _filtre,
    ) { liste, clients, filtre ->
        EtatProjets(
            projets = liste.filter { filtre == null || (it.etat == filtre && !it.annule) },
            compteurs = ProjetRules.compteurs(liste),
            budgetTotal = ProjetRules.budgetTotal(liste),
            consommeTotal = ProjetRules.consommeTotal(liste),
            avancementMoyen = ProjetRules.avancementMoyen(liste),
            enDerive = ProjetRules.enDerive(liste).size,
            enRetard = ProjetRules.enRetard(liste, System.currentTimeMillis()).size,
            clients = clients,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EtatProjets())

    fun filtrer(etat: EtatProjet?) {
        _filtre.value = if (_filtre.value == etat) null else etat
    }

    fun effacerMessage() { _message.value = null }

    fun creer(client: ClientEntity?, nom: String, responsable: String, budgetTexte: String) {
        val budget = if (budgetTexte.isBlank()) 0.0 else TresorerieRules.montantSaisi(budgetTexte)
            ?: run {
                _message.value = Message.Invalide
                return
            }
        lancer {
            traiter(
                projets.creer(
                    nom = nom,
                    clientId = client?.id ?: 0L,
                    clientNom = client?.nom,
                    responsable = responsable,
                    budget = budget,
                ),
            )
        }
    }

    fun actualiser(id: Long, avancementTexte: String, consommeTexte: String) {
        val avancement = avancementTexte.trim().toIntOrNull() ?: run {
            _message.value = Message.Invalide
            return
        }
        val consomme = if (consommeTexte.isBlank()) 0.0 else TresorerieRules.montantSaisi(consommeTexte)
            ?: run {
                _message.value = Message.Invalide
                return
            }
        lancer { traiter(projets.actualiser(id, avancement, consomme), actualise = true) }
    }

    fun changerEtat(id: Long, etat: EtatProjet) =
        lancer { traiter(projets.changerEtat(id, etat), etatChange = true) }

    fun annuler(id: Long) = lancer { traiter(projets.annuler(id), annule = true) }

    private fun traiter(
        resultat: ProjetUseCases.Resultat,
        actualise: Boolean = false,
        etatChange: Boolean = false,
        annule: Boolean = false,
    ) {
        _message.value = when (resultat) {
            is ProjetUseCases.Resultat.Succes -> when {
                annule -> Message.Annule
                actualise -> Message.Actualise
                etatChange -> Message.EtatChange
                else -> Message.Cree(resultat.reference)
            }
            ProjetUseCases.Resultat.LectureSeule -> Message.LectureSeule
            ProjetUseCases.Resultat.Invalide -> Message.Invalide
            ProjetUseCases.Resultat.Introuvable -> Message.Erreur
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
