package com.missa.b360.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.missa.b360.core.domain.usecase.BackupUseCases
import com.missa.b360.core.domain.usecase.GetEnterpriseUseCase
import com.missa.b360.core.domain.usecase.ObserveClientsUseCase
import com.missa.b360.core.domain.usecase.ObserveFournisseursUseCase
import com.missa.b360.core.domain.usecase.OperationUseCases
import com.missa.b360.core.domain.usecase.UserAdminUseCases
import com.missa.b360.core.data.entity.OperationDirection
import com.missa.b360.core.data.entity.OperationModule
import com.missa.b360.core.data.entity.OperationRecordEntity
import com.missa.b360.core.data.entity.OperationStatus
import com.missa.b360.core.notifications.AppNotifier
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import javax.inject.Inject

/** Données de synthèse disponibles dès les modules actuellement implémentés. */
data class HomeUiState(
    val entrepriseNom: String = "",
    val devise: String = "XAF",
    val entrepriseLogoUri: String? = null,
    val profilActivite: String? = null,
    val palierTaille: String? = null,
    val prenomUtilisateur: String? = null,
    val nombreClients: Int = 0,
    val nombreFournisseurs: Int = 0,
    val derniereSauvegarde: Long? = null,
    val ventes: Double = 0.0,
    val achats: Double = 0.0,
    val tresorerie: Double = 0.0,
    val quantiteStock: Double = 0.0,
    val recentOperations: List<OperationRecordEntity> = emptyList(),
)

/**
 * ViewModel de l'accueil : badge (RA-23) et résumé réactif de l'entreprise.
 * Les indicateurs reposent exclusivement sur les pièces locales validées, sans données fictives.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val appNotifier: AppNotifier,
    getEnterprise: GetEnterpriseUseCase,
    users: UserAdminUseCases,
    observeClients: ObserveClientsUseCase,
    observeFournisseurs: ObserveFournisseursUseCase,
    backups: BackupUseCases,
    operations: OperationUseCases,
) : ViewModel() {

    val notificationsNonLues: Flow<Int> = appNotifier.observeNonLues()

    private val baseState = combine(
        getEnterprise.observer(),
        users.observerUtilisateurs(),
        observeClients(),
        observeFournisseurs(),
        backups.historique(),
    ) { entreprise, utilisateurs, clients, fournisseurs, historiqueSauvegardes ->
        HomeUiState(
            entrepriseNom = entreprise?.nom.orEmpty(),
            devise = entreprise?.devise ?: "XAF",
            entrepriseLogoUri = entreprise?.logoUri,
            profilActivite = entreprise?.profilActivite,
            palierTaille = entreprise?.palierTaille,
            prenomUtilisateur = utilisateurs.firstOrNull { it.actif }
                ?.nom
                ?.trim()
                ?.substringBefore(' ')
                ?.takeIf { it.isNotBlank() },
            nombreClients = clients.size,
            nombreFournisseurs = fournisseurs.size,
            derniereSauvegarde = historiqueSauvegardes.firstOrNull()?.date,
        )
    }

    val uiState: StateFlow<HomeUiState> = combine(baseState, operations.observeAll()) { base, records ->
        val validated = records.filter { it.status == OperationStatus.VALIDATED.name }
        // Les deux cartes libellées « Aujourd’hui » ne doivent jamais agréger les
        // opérations des jours précédents. La trésorerie reste, elle, un solde cumulé.
        val startOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val validatedToday = validated.filter { it.createdAt >= startOfToday }
        val ventes = validatedToday.amountFor(OperationModule.VENTE)
        val achats = validatedToday.amountFor(OperationModule.ACHATS)
        val tresorerie = validated
            .filter { it.module == OperationModule.FINANCES.name }
            .sumOf { record ->
                when (record.direction) {
                    OperationDirection.IN.name -> record.amount ?: 0.0
                    OperationDirection.OUT.name -> -(record.amount ?: 0.0)
                    else -> 0.0
                }
            }
        val quantiteStock = validated
            .filter { it.module == OperationModule.STOCK.name }
            .sumOf { it.quantity ?: 0.0 }
        base.copy(
            ventes = ventes,
            achats = achats,
            tresorerie = tresorerie,
            quantiteStock = quantiteStock,
            recentOperations = records.take(3),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )
}

private fun List<OperationRecordEntity>.amountFor(module: OperationModule): Double =
    filter { it.module == module.name }.sumOf { it.amount ?: 0.0 }
