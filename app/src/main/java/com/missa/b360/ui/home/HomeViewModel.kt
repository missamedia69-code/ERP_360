package com.missa.b360.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.missa.b360.core.data.dao.CompteTresorerieDao
import com.missa.b360.core.data.dao.MouvementTresorerieDao
import com.missa.b360.core.data.datastore.SettingsStore
import com.missa.b360.core.domain.model.ModuleCode
import com.missa.b360.core.domain.model.ModulesPersonnalises
import com.missa.b360.core.domain.model.TresorerieRules
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject
import com.missa.b360.core.util.Iso4217

/** Données de synthèse disponibles dès les modules actuellement implémentés. */
data class HomeUiState(
    val entrepriseNom: String = "",
    val devise: String = Iso4217.DEVISE_REPLI,
    val entrepriseLogoUri: String? = null,
    val profilActivite: String? = null,
    val palierTaille: String? = null,
    val prenomUtilisateur: String? = null,
    val nombreClients: Int = 0,
    val nombreFournisseurs: Int = 0,
    val derniereSauvegarde: Long? = null,
    val ventes: Double = 0.0,
    val achats: Double = 0.0,
    /** Marge brute du jour : ventes − achats, calculée une seule fois. */
    val marge: Double = 0.0,
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
    private val settingsStore: SettingsStore,
    compteTresorerieDao: CompteTresorerieDao,
    mouvementTresorerieDao: MouvementTresorerieDao,
    getEnterprise: GetEnterpriseUseCase,
    users: UserAdminUseCases,
    observeClients: ObserveClientsUseCase,
    observeFournisseurs: ObserveFournisseursUseCase,
    backups: BackupUseCases,
    operations: OperationUseCases,
) : ViewModel() {

    val notificationsNonLues: Flow<Int> = appNotifier.observeNonLues()

    /**
     * Modules retenus au moment du choix du pack. La liste pilote l'accueil :
     * un module non retenu n'a pas à encombrer le menu d'une TPE qui ne s'en
     * servira jamais.
     */
    /** Modules épinglés dans la barre du bas ; vide = disposition d'usine. */
    val modulesEpingles: StateFlow<List<String>> =
        settingsStore.observe(SettingsStore.Keys.BARRE_MODULES)
            .map { valeur ->
                valeur.orEmpty().split(',').map { it.trim() }.filter { it.isNotEmpty() }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Enregistre la sélection ; une liste vide rétablit la disposition d'usine. */
    fun epinglerModules(modules: List<String>) {
        viewModelScope.launch {
            settingsStore.set(SettingsStore.Keys.BARRE_MODULES, modules.joinToString(","))
        }
    }

    val modulesActifs: StateFlow<List<ModuleCode>> =
        settingsStore.observe(SettingsStore.Keys.MODULES_ACTIFS)
            .map { ModulesPersonnalises.deserialiser(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val baseState = combine(
        getEnterprise.observer(),
        users.observerUtilisateurs(),
        observeClients(),
        observeFournisseurs(),
        backups.historique(),
    ) { entreprise, utilisateurs, clients, fournisseurs, historiqueSauvegardes ->
        HomeUiState(
            entrepriseNom = entreprise?.nom.orEmpty(),
            devise = entreprise?.devise ?: Iso4217.DEVISE_REPLI,
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

    private val soldeTresorerie = combine(
        compteTresorerieDao.observeAll(),
        mouvementTresorerieDao.observeAll(),
    ) { comptes, mouvements -> TresorerieRules.soldeGlobal(comptes, mouvements) }

    val uiState: StateFlow<HomeUiState> = combine(
        baseState,
        operations.observeAll(),
        soldeTresorerie,
    ) { base, records, soldeComptes ->
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
        // Le solde affiché à l'accueil doit être celui du module Trésorerie, à
        // l'unité près : deux écrans qui annoncent deux soldes différents font
        // perdre confiance dans les deux. Même formule que le tableau de bord —
        // comptes de trésorerie plus anciennes pièces FINANCES.
        val fluxFinances = validated
            .filter { it.module == OperationModule.FINANCES.name }
            .sumOf { record ->
                when (record.direction) {
                    OperationDirection.IN.name -> record.amount ?: 0.0
                    OperationDirection.OUT.name -> -(record.amount ?: 0.0)
                    else -> 0.0
                }
            }
        val tresorerie = soldeComptes + fluxFinances
        val quantiteStock = validated
            .filter { it.module == OperationModule.STOCK.name }
            .sumOf { it.quantity ?: 0.0 }
        base.copy(
            ventes = ventes,
            achats = achats,
            marge = ventes - achats,
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
