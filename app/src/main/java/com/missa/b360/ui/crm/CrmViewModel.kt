package com.missa.b360.ui.crm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.missa.b360.core.data.dao.ClientDao
import com.missa.b360.core.data.dao.OperationRecordDao
import com.missa.b360.core.domain.model.CompteurSegment
import com.missa.b360.core.domain.model.CrmRules
import com.missa.b360.core.domain.model.FicheCrm
import com.missa.b360.core.domain.model.SegmentClient
import com.missa.b360.core.domain.usecase.GetEnterpriseUseCase
import com.missa.b360.core.util.Iso4217
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * État de l'écran CRM : segmentation du portefeuille, relances à passer et
 * meilleurs clients. Aucune saisie ici — les fiches clients restent gérées par
 * le module Clients, le CRM ne fait que les éclairer.
 */
@HiltViewModel
class CrmViewModel @Inject constructor(
    clientDao: ClientDao,
    operationDao: OperationRecordDao,
    getEnterprise: GetEnterpriseUseCase,
) : ViewModel() {

    data class EtatCrm(
        val fiches: List<FicheCrm> = emptyList(),
        val compteurs: List<CompteurSegment> = emptyList(),
        val relances: List<FicheCrm> = emptyList(),
        val top: List<FicheCrm> = emptyList(),
        val tauxConversion: Double = 0.0,
        val chiffreAffaires: Double = 0.0,
        val panierMoyen: Double = 0.0,
    )

    private val _filtre = MutableStateFlow<SegmentClient?>(null)
    val filtre: StateFlow<SegmentClient?> = _filtre

    val devise: StateFlow<String> = getEnterprise.observer()
        .map { it?.devise ?: Iso4217.DEVISE_REPLI }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Iso4217.DEVISE_REPLI)

    val etat: StateFlow<EtatCrm> = combine(
        clientDao.observeAllIncludingInactive(),
        operationDao.observeAll(),
        _filtre,
    ) { clients, pieces, filtre ->
        val fiches = CrmRules.fiches(clients, pieces, System.currentTimeMillis())
        EtatCrm(
            fiches = fiches.filter { filtre == null || it.segment == filtre },
            compteurs = CrmRules.compteurs(fiches),
            relances = CrmRules.aRelancer(fiches),
            top = CrmRules.top(fiches),
            tauxConversion = CrmRules.tauxConversion(fiches),
            chiffreAffaires = CrmRules.chiffreAffairesTotal(fiches),
            panierMoyen = CrmRules.panierMoyen(fiches),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EtatCrm())

    /** Un second appui sur le même segment retire le filtre. */
    fun filtrer(segment: SegmentClient?) {
        _filtre.value = if (_filtre.value == segment) null else segment
    }
}
