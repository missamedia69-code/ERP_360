package com.missa.b360.ui.livraison

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.missa.b360.core.data.entity.ClientEntity
import com.missa.b360.core.domain.model.BonLivraison
import com.missa.b360.core.domain.model.CompteurEtape
import com.missa.b360.core.domain.model.EtapeLivraison
import com.missa.b360.core.domain.model.LivraisonRules
import com.missa.b360.core.domain.usecase.GetEnterpriseUseCase
import com.missa.b360.core.domain.usecase.LivraisonUseCases
import com.missa.b360.core.domain.usecase.ObserveClientsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** État de l'écran Bons de livraison. */
@HiltViewModel
class LivraisonViewModel @Inject constructor(
    private val livraisons: LivraisonUseCases,
    observeClients: ObserveClientsUseCase,
    getEnterprise: GetEnterpriseUseCase,
) : ViewModel() {

    data class EtatLivraison(
        val bons: List<BonLivraison> = emptyList(),
        val compteurs: List<CompteurEtape> = emptyList(),
        val enCours: Int = 0,
        val tauxLivraison: Double = 0.0,
        val delaiMoyenJours: Double? = null,
        val clients: List<ClientEntity> = emptyList(),
    )

    sealed class Message {
        data class Cree(val reference: String) : Message()
        data object Avance : Message()
        data object Annule : Message()
        data object LectureSeule : Message()
        data object Invalide : Message()
        data object EtapeFinale : Message()
        data object Erreur : Message()
    }

    private val _message = MutableStateFlow<Message?>(null)
    val message: StateFlow<Message?> = _message

    private val _enCours = MutableStateFlow(false)
    val enCours: StateFlow<Boolean> = _enCours

    private val _filtre = MutableStateFlow<EtapeLivraison?>(null)
    val filtre: StateFlow<EtapeLivraison?> = _filtre

    val entreprise = getEnterprise.observer()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val etat: StateFlow<EtatLivraison> = combine(
        livraisons.observer(),
        observeClients(),
        _filtre,
    ) { bons, clients, filtre ->
        EtatLivraison(
            bons = bons.filter { filtre == null || (it.etape == filtre && !it.annule) },
            compteurs = LivraisonRules.compteurs(bons),
            enCours = LivraisonRules.enCours(bons).size,
            tauxLivraison = LivraisonRules.tauxLivraison(bons),
            delaiMoyenJours = LivraisonRules.delaiMoyenJours(bons),
            clients = clients,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EtatLivraison())

    fun filtrer(etape: EtapeLivraison?) {
        _filtre.value = if (_filtre.value == etape) null else etape
    }

    fun effacerMessage() { _message.value = null }

    fun creer(
        client: ClientEntity?,
        nomLibre: String,
        adresse: String,
        transporteur: String,
        colisTexte: String,
        referenceOrigine: String,
    ) {
        // Un client de la base l'emporte sur la saisie libre : c'est lui qui
        // porte l'identifiant, donc le rattachement durable de la pièce.
        val nom = client?.nom ?: nomLibre
        val colis = if (colisTexte.isBlank()) 0 else colisTexte.trim().toIntOrNull() ?: run {
            _message.value = Message.Invalide
            return
        }
        lancer {
            traiter(
                livraisons.creer(
                    clientId = client?.id ?: 0L,
                    clientNom = nom,
                    adresse = adresse.ifBlank { client?.adresse },
                    contact = client?.telephone,
                    transporteur = transporteur,
                    nombreColis = colis,
                    referenceOrigine = referenceOrigine,
                ),
            )
        }
    }

    fun avancer(id: Long) = lancer { traiter(livraisons.avancer(id), avance = true) }

    fun annuler(id: Long) = lancer { traiter(livraisons.annuler(id), annule = true) }

    private fun traiter(
        resultat: LivraisonUseCases.Resultat,
        avance: Boolean = false,
        annule: Boolean = false,
    ) {
        _message.value = when (resultat) {
            is LivraisonUseCases.Resultat.Succes -> when {
                annule -> Message.Annule
                avance -> Message.Avance
                else -> Message.Cree(resultat.reference)
            }
            LivraisonUseCases.Resultat.LectureSeule -> Message.LectureSeule
            LivraisonUseCases.Resultat.Invalide -> Message.Invalide
            LivraisonUseCases.Resultat.Introuvable -> Message.Erreur
            LivraisonUseCases.Resultat.EtapeFinale -> Message.EtapeFinale
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
