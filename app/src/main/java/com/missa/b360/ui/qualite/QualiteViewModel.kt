package com.missa.b360.ui.qualite

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.missa.b360.core.data.entity.GraviteNc
import com.missa.b360.core.data.entity.NonConformiteEntity
import com.missa.b360.core.data.entity.OrigineNc
import com.missa.b360.core.domain.model.BilanQualite
import com.missa.b360.core.domain.model.QualiteMaintenanceRules
import com.missa.b360.core.domain.model.TresorerieRules
import com.missa.b360.core.domain.usecase.GetEnterpriseUseCase
import com.missa.b360.core.domain.usecase.QualiteUseCases
import com.missa.b360.core.domain.usecase.ResultatSaisie
import com.missa.b360.core.util.Iso4217
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** État de l'écran Qualité : bilan des écarts et file de traitement. */
@HiltViewModel
class QualiteViewModel @Inject constructor(
    private val qualite: QualiteUseCases,
    getEnterprise: GetEnterpriseUseCase,
) : ViewModel() {

    data class EtatQualite(
        val bilan: BilanQualite = BilanQualite(),
        val aTraiter: List<NonConformiteEntity> = emptyList(),
        val toutes: List<NonConformiteEntity> = emptyList(),
    )

    sealed class Message {
        data object Enregistre : Message()
        data object LectureSeule : Message()
        data object Invalide : Message()
        data object Erreur : Message()
    }

    private val _message = MutableStateFlow<Message?>(null)
    val message: StateFlow<Message?> = _message

    private val _enCours = MutableStateFlow(false)
    val enCours: StateFlow<Boolean> = _enCours

    val devise: StateFlow<String> = getEnterprise.observer()
        .map { it?.devise ?: Iso4217.DEVISE_REPLI }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Iso4217.DEVISE_REPLI)

    val etat: StateFlow<EtatQualite> = qualite.observer()
        .map { liste ->
            EtatQualite(
                bilan = QualiteMaintenanceRules.bilan(liste),
                aTraiter = QualiteMaintenanceRules.aTraiter(liste),
                toutes = liste,
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EtatQualite())

    fun effacerMessage() { _message.value = null }

    fun declarer(
        titre: String,
        gravite: GraviteNc,
        origine: OrigineNc,
        description: String,
        responsable: String,
        coutTexte: String,
    ) {
        val cout = if (coutTexte.isBlank()) 0.0 else TresorerieRules.montantSaisi(coutTexte) ?: run {
            _message.value = Message.Invalide
            return
        }
        lancer {
            traiter(
                qualite.declarer(
                    titre = titre,
                    gravite = gravite,
                    origine = origine,
                    description = description,
                    responsable = responsable,
                    cout = cout,
                ),
            )
        }
    }

    fun avancer(id: Long) {
        lancer { traiter(qualite.avancer(id)) }
    }

    private fun traiter(resultat: ResultatSaisie) {
        _message.value = when (resultat) {
            is ResultatSaisie.Succes -> Message.Enregistre
            ResultatSaisie.LectureSeule -> Message.LectureSeule
            ResultatSaisie.Invalide, ResultatSaisie.NomDejaPris -> Message.Invalide
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
