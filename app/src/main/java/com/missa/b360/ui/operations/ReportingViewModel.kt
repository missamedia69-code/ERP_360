package com.missa.b360.ui.operations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.missa.b360.core.data.datastore.SettingsStore
import com.missa.b360.core.domain.model.ModuleCode
import com.missa.b360.core.domain.model.ModulesPersonnalises
import com.missa.b360.core.domain.usecase.GetEnterpriseUseCase
import com.missa.b360.core.domain.usecase.ObserveTableauDeBordUseCase
import com.missa.b360.core.domain.usecase.TableauDeBord
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Tableau de bord : croise les données de tous les modules et ne présente que
 * les indicateurs des modules réellement activés (clé `modules_actifs`).
 */
@HiltViewModel
class ReportingViewModel @Inject constructor(
    observeTableauDeBord: ObserveTableauDeBordUseCase,
    settingsStore: SettingsStore,
    getEnterprise: GetEnterpriseUseCase,
) : ViewModel() {

    val devise: StateFlow<String> = getEnterprise.observer()
        .map { it?.devise ?: "XAF" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "XAF")

    /**
     * Modules actifs de l'installation. Une installation antérieure à la
     * sélection des modules n'a pas encore la clé : on considère alors que tout
     * est actif plutôt que d'afficher un écran vide.
     */
    val modulesActifs: StateFlow<List<ModuleCode>> =
        settingsStore.observe(SettingsStore.Keys.MODULES_ACTIFS)
            .map { valeur ->
                ModulesPersonnalises.deserialiser(valeur).ifEmpty { ModuleCode.entries.toList() }
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                ModuleCode.entries.toList(),
            )

    val tableau: StateFlow<TableauDeBord> = observeTableauDeBord()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TableauDeBord())
}
