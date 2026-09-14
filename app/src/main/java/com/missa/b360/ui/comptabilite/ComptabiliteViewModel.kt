package com.missa.b360.ui.comptabilite

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.missa.b360.core.domain.usecase.GetEnterpriseUseCase
import com.missa.b360.core.domain.usecase.ObserverComptabiliteUseCase
import com.missa.b360.core.domain.usecase.SyntheseComptable
import com.missa.b360.core.util.Iso4217
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import javax.inject.Inject

/**
 * État de l'écran Comptabilité.
 *
 * La période est choisie par l'utilisateur ; changer de mois relance la
 * consolidation, jamais une saisie.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ComptabiliteViewModel @Inject constructor(
    observerComptabilite: ObserverComptabiliteUseCase,
    getEnterprise: GetEnterpriseUseCase,
) : ViewModel() {

    /** Fenêtres proposées : le mois en cours, le mois précédent, l'année. */
    enum class Periode { MOIS, MOIS_PRECEDENT, ANNEE }

    private val _periode = MutableStateFlow(Periode.MOIS)
    val periode: StateFlow<Periode> = _periode

    val devise: StateFlow<String> = getEnterprise.observer()
        .map { it?.devise ?: Iso4217.DEVISE_REPLI }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Iso4217.DEVISE_REPLI)

    val synthese: StateFlow<SyntheseComptable> = _periode
        .flatMapLatest { periode ->
            observerComptabilite({ debut(periode) }, { fin(periode) })
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SyntheseComptable())

    fun choisirPeriode(periode: Periode) {
        _periode.value = periode
    }

    private fun debut(periode: Periode): Long = Calendar.getInstance().apply {
        when (periode) {
            Periode.MOIS -> set(Calendar.DAY_OF_MONTH, 1)
            Periode.MOIS_PRECEDENT -> {
                add(Calendar.MONTH, -1)
                set(Calendar.DAY_OF_MONTH, 1)
            }
            Periode.ANNEE -> set(Calendar.DAY_OF_YEAR, 1)
        }
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun fin(periode: Periode): Long = Calendar.getInstance().apply {
        if (periode == Periode.MOIS_PRECEDENT) {
            // Dernier instant du mois précédent : on remonte d'un jour depuis le
            // premier du mois courant, plutôt que de compter les jours à la main.
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.DAY_OF_MONTH, -1)
        }
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }.timeInMillis
}
