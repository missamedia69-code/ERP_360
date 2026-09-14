package com.missa.b360.ui.maintenance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.missa.b360.core.data.entity.EquipementEntity
import com.missa.b360.core.data.entity.InterventionEntity
import com.missa.b360.core.data.entity.TypeEquipement
import com.missa.b360.core.data.entity.TypeIntervention
import com.missa.b360.core.domain.model.EtatEquipement
import com.missa.b360.core.domain.model.QualiteMaintenanceRules
import com.missa.b360.core.domain.model.TresorerieRules
import com.missa.b360.core.domain.usecase.GetEnterpriseUseCase
import com.missa.b360.core.domain.usecase.MaintenanceUseCases
import com.missa.b360.core.domain.usecase.ResultatSaisie
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

/** État de l'écran Maintenance : parc, échéances et interventions. */
@HiltViewModel
class MaintenanceViewModel @Inject constructor(
    private val maintenance: MaintenanceUseCases,
    getEnterprise: GetEnterpriseUseCase,
) : ViewModel() {

    data class EtatMaintenance(
        val parc: List<EtatEquipement> = emptyList(),
        val enRetard: Int = 0,
        val tauxPreventif: Double = 0.0,
        val coutTotal: Double = 0.0,
        val interventions: List<InterventionEntity> = emptyList(),
        val equipements: List<EquipementEntity> = emptyList(),
    )

    sealed class Message {
        data object Enregistre : Message()
        data object LectureSeule : Message()
        data object Invalide : Message()
        data object NomDejaPris : Message()
        data object Erreur : Message()
    }

    private val _message = MutableStateFlow<Message?>(null)
    val message: StateFlow<Message?> = _message

    private val _enCours = MutableStateFlow(false)
    val enCours: StateFlow<Boolean> = _enCours

    val devise: StateFlow<String> = getEnterprise.observer()
        .map { it?.devise ?: Iso4217.DEVISE_REPLI }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Iso4217.DEVISE_REPLI)

    val etat: StateFlow<EtatMaintenance> = combine(
        maintenance.observerEquipements(),
        maintenance.observerInterventions(),
    ) { equipements, interventions ->
        val parc = QualiteMaintenanceRules.etatDuParc(
            equipements,
            interventions,
            System.currentTimeMillis(),
        )
        EtatMaintenance(
            parc = parc,
            enRetard = QualiteMaintenanceRules.enRetard(parc).size,
            tauxPreventif = QualiteMaintenanceRules.tauxPreventif(interventions),
            coutTotal = QualiteMaintenanceRules.coutMaintenance(interventions),
            interventions = interventions,
            equipements = equipements.filter { it.actif },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EtatMaintenance())

    fun effacerMessage() { _message.value = null }

    fun ajouterEquipement(
        nom: String,
        type: TypeEquipement,
        code: String,
        periodiciteTexte: String,
    ) {
        val periodicite = if (periodiciteTexte.isBlank()) 0 else periodiciteTexte.trim()
            .toIntOrNull() ?: run {
            _message.value = Message.Invalide
            return
        }
        lancer {
            traiter(
                maintenance.ajouterEquipement(
                    nom = nom,
                    type = type,
                    code = code,
                    periodiciteJours = periodicite,
                    dateMiseEnService = System.currentTimeMillis(),
                ),
            )
        }
    }

    fun enregistrerIntervention(
        equipementId: Long,
        type: TypeIntervention,
        description: String,
        coutTexte: String,
        dureeTexte: String,
        technicien: String,
    ) {
        val cout = if (coutTexte.isBlank()) 0.0 else TresorerieRules.montantSaisi(coutTexte) ?: run {
            _message.value = Message.Invalide
            return
        }
        val duree = if (dureeTexte.isBlank()) 0.0 else dureeTexte.trim().replace(',', '.')
            .toDoubleOrNull() ?: run {
            _message.value = Message.Invalide
            return
        }
        lancer {
            traiter(
                maintenance.enregistrerIntervention(
                    equipementId = equipementId,
                    type = type,
                    description = description,
                    cout = cout,
                    dureeHeures = duree,
                    technicien = technicien,
                ),
            )
        }
    }

    private fun traiter(resultat: ResultatSaisie) {
        _message.value = when (resultat) {
            is ResultatSaisie.Succes -> Message.Enregistre
            ResultatSaisie.LectureSeule -> Message.LectureSeule
            ResultatSaisie.Invalide -> Message.Invalide
            ResultatSaisie.NomDejaPris -> Message.NomDejaPris
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
