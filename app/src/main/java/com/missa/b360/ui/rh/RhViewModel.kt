package com.missa.b360.ui.rh

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.missa.b360.core.data.dao.AbsenceDao
import com.missa.b360.core.data.dao.EmployeeDao
import com.missa.b360.core.data.entity.AbsenceEntity
import com.missa.b360.core.data.entity.EmployeeEntity
import com.missa.b360.core.domain.usecase.CreatePayslipUseCase
import com.missa.b360.core.domain.usecase.DesactivateEmployeeUseCase
import com.missa.b360.core.domain.usecase.GetEnterpriseUseCase
import com.missa.b360.core.domain.usecase.SaveAbsenceUseCase
import com.missa.b360.core.domain.usecase.SaveAdvanceUseCase
import com.missa.b360.core.domain.usecase.SaveEmployeeUseCase
import com.missa.b360.core.util.Iso4217
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class RhViewModel @Inject constructor(
    private val employeeDao: EmployeeDao,
    private val absenceDao: AbsenceDao,
    private val saveEmployee: SaveEmployeeUseCase,
    private val desactivateEmployee: DesactivateEmployeeUseCase,
    private val saveAbsence: SaveAbsenceUseCase,
    private val saveAdvance: SaveAdvanceUseCase,
    private val createPayslip: CreatePayslipUseCase,
    getEnterprise: GetEnterpriseUseCase,
) : ViewModel() {

    sealed interface ActionMessage {
        data class Succes(val texte: String) : ActionMessage
        data class Erreur(val texte: String) : ActionMessage
    }

    val employees: StateFlow<List<EmployeeEntity>> = employeeDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val absences: StateFlow<List<AbsenceEntity>> = absenceDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val devise: StateFlow<String> = getEnterprise.observer()
        .map { it?.devise ?: Iso4217.DEVISE_REPLI }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Iso4217.DEVISE_REPLI)

    private val _message = MutableStateFlow<ActionMessage?>(null)
    val message: StateFlow<ActionMessage?> = _message

    fun creerEmploye(nom: String, telephone: String, poste: String?, salaireBase: Double, joursMensuels: Double) {
        viewModelScope.launch {
            when (val res = saveEmployee(null, nom, telephone, poste, salaireBase, joursMensuels, null)) {
                is SaveEmployeeUseCase.Result.Succes -> _message.value = ActionMessage.Succes("Employé créé : ${res.code}")
                SaveEmployeeUseCase.Result.LectureSeule -> _message.value = ActionMessage.Erreur("Licence en lecture seule")
                else -> _message.value = ActionMessage.Erreur("Données invalides")
            }
        }
    }

    fun desactiverEmploye(id: Long) {
        viewModelScope.launch {
            when (desactivateEmployee(id)) {
                DesactivateEmployeeUseCase.Result.Succes -> _message.value = ActionMessage.Succes("Employé désactivé")
                else -> _message.value = ActionMessage.Erreur("Action impossible")
            }
        }
    }

    fun declarerAbsence(employeeId: Long, type: String, dureeJours: Double, motif: String?) {
        viewModelScope.launch {
            when (saveAbsence(employeeId, type, System.currentTimeMillis(), dureeJours, motif)) {
                SaveAbsenceUseCase.Result.Succes -> _message.value = ActionMessage.Succes("Absence enregistrée")
                else -> _message.value = ActionMessage.Erreur("Impossible d'enregistrer l'absence")
            }
        }
    }

    fun verserAvance(employeeId: Long, montant: Double, motif: String?) {
        viewModelScope.launch {
            when (val res = saveAdvance(employeeId, montant, motif)) {
                is SaveAdvanceUseCase.Result.Succes -> _message.value = ActionMessage.Succes("Avance validée : ${res.reference}")
                else -> _message.value = ActionMessage.Erreur("Erreur lors de l'avance")
            }
        }
    }

    fun genererBulletinPaie() {
        val cal = Calendar.getInstance()
        val mois = cal.get(Calendar.MONTH) + 1
        val annee = cal.get(Calendar.YEAR)
        viewModelScope.launch {
            when (val res = createPayslip(mois, annee)) {
                is CreatePayslipUseCase.Result.Succes -> _message.value = ActionMessage.Succes("Paie $mois/$annee générée (${res.employes} employés)")
                CreatePayslipUseCase.Result.DejaExistante -> _message.value = ActionMessage.Erreur("La paie de ce mois a déjà été générée")
                CreatePayslipUseCase.Result.AucunEmploye -> _message.value = ActionMessage.Erreur("Aucun employé actif")
                else -> _message.value = ActionMessage.Erreur("Erreur lors du calcul de la paie")
            }
        }
    }

    fun effacerMessage() {
        _message.value = null
    }
}
