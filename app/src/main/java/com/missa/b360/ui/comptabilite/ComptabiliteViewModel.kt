package com.missa.b360.ui.comptabilite

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.missa.b360.core.data.entity.AccountingEntryLineEntity
import com.missa.b360.core.data.entity.AccountingStandard
import com.missa.b360.core.domain.usecase.AccountingLedgerSnapshot
import com.missa.b360.core.domain.usecase.CreateAccountingVoucherUseCase
import com.missa.b360.core.domain.usecase.ConfigureAccountingProfileUseCase
import com.missa.b360.core.domain.usecase.GetEnterpriseUseCase
import com.missa.b360.core.domain.usecase.InitializeAccountingPlanUseCase
import com.missa.b360.core.domain.usecase.ObserverAccountingLedgerUseCase
import com.missa.b360.core.domain.usecase.ObserverComptabiliteUseCase
import com.missa.b360.core.domain.usecase.PostAccountingVoucherUseCase
import com.missa.b360.core.domain.usecase.SyntheseComptable
import com.missa.b360.core.util.Iso4217
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

/** Écran CPT : indicateurs opérationnels historiques + registre comptable persistant. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ComptabiliteViewModel @Inject constructor(
    observerComptabilite: ObserverComptabiliteUseCase,
    observerLedger: ObserverAccountingLedgerUseCase,
    getEnterprise: GetEnterpriseUseCase,
    private val initializePlan: InitializeAccountingPlanUseCase,
    private val configureProfile: ConfigureAccountingProfileUseCase,
    private val createVoucher: CreateAccountingVoucherUseCase,
    private val postVoucher: PostAccountingVoucherUseCase,
) : ViewModel() {
    enum class Periode { MOIS, MOIS_PRECEDENT, ANNEE }

    private val _periode = MutableStateFlow(Periode.MOIS)
    val periode: StateFlow<Periode> = _periode
    private val _feedback = MutableStateFlow<String?>(null)
    val feedback = _feedback.asStateFlow()

    val devise: StateFlow<String> = getEnterprise.observer()
        .map { it?.devise ?: Iso4217.DEVISE_REPLI }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Iso4217.DEVISE_REPLI)

    val synthese: StateFlow<SyntheseComptable> = _periode
        .flatMapLatest { periode -> observerComptabilite({ debut(periode) }, { fin(periode) }) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SyntheseComptable())

    val registre: StateFlow<AccountingLedgerSnapshot> = observerLedger.observe()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AccountingLedgerSnapshot())

    fun choisirPeriode(periode: Periode) { _periode.value = periode }

    fun initialiserPlan() = viewModelScope.launch {
        _feedback.value = when (initializePlan(AccountingStandard.SYSCOHADA_REVISE)) {
            is InitializeAccountingPlanUseCase.Result.Success -> "Socle SYSCOHADA initialisé. Complétez et validez votre plan avant toute déclaration."
            InitializeAccountingPlanUseCase.Result.AlreadyInitialized -> "Un plan est déjà initialisé."
            InitializeAccountingPlanUseCase.Result.PermissionDenied -> "Permission de configuration comptable refusée."
            InitializeAccountingPlanUseCase.Result.ReadOnly -> "Licence en lecture seule."
            InitializeAccountingPlanUseCase.Result.InvalidStandard -> "Référentiel non disponible."
        }
    }

    fun configurerProfil(countryCode: String, taxRegime: String, exerciceStartMonth: Int) = viewModelScope.launch {
        _feedback.value = when (configureProfile(
            standard = AccountingStandard.SYSCOHADA_REVISE,
            countryCode = countryCode,
            taxRegime = taxRegime,
            exerciceStartMonth = exerciceStartMonth,
        )) {
            ConfigureAccountingProfileUseCase.Result.Success -> "Pays, régime et début d'exercice enregistrés. Ces paramètres ne certifient pas la conformité fiscale locale."
            ConfigureAccountingProfileUseCase.Result.Invalid -> "Pays ou mois d'exercice invalide."
            ConfigureAccountingProfileUseCase.Result.AlreadyInUse -> "Le profil ne peut plus être modifié après l'ajout de comptes ou de pièces."
            ConfigureAccountingProfileUseCase.Result.PermissionDenied -> "Permission de configuration comptable refusée."
            ConfigureAccountingProfileUseCase.Result.ReadOnly -> "Licence en lecture seule."
        }
    }

    fun creerEcriture(description: String, debitAccountId: Long, creditAccountId: Long, amount: Double, currency: String) = viewModelScope.launch {
        val label = description.trim()
        if (label.length < 2 || debitAccountId == creditAccountId || !amount.isFinite() || amount <= 0) {
            _feedback.value = "Saisie invalide : choisissez deux comptes différents et un montant positif."
            return@launch
        }
        val journal = registre.value.journals.firstOrNull { it.code == "OD" }
        if (journal == null) {
            _feedback.value = "Le journal OD est absent : initialisez d'abord le référentiel."
            return@launch
        }
        val lines = listOf(
            AccountingEntryLineEntity(voucherId = 0, lineNumber = 1, accountId = debitAccountId,
                label = label, debitAmount = amount),
            AccountingEntryLineEntity(voucherId = 0, lineNumber = 2, accountId = creditAccountId,
                label = label, creditAmount = amount),
        )
        when (val result = createVoucher.manual(journal.code, System.currentTimeMillis(), label, currency, lines)) {
            is CreateAccountingVoucherUseCase.Result.Success -> {
                _feedback.value = "Brouillon ${result.reference} créé. Il doit être validé avant comptabilisation."
            }
            is CreateAccountingVoucherUseCase.Result.AlreadyProcessed -> {
                _feedback.value = "Cette pièce existe déjà : ${result.reference}."
            }
            CreateAccountingVoucherUseCase.Result.Invalid -> _feedback.value = "Écriture invalide : montants ou informations non conformes."
            CreateAccountingVoucherUseCase.Result.ReadOnly -> _feedback.value = "Licence en lecture seule."
            CreateAccountingVoucherUseCase.Result.PermissionDenied -> _feedback.value = "Permission de création refusée."
            CreateAccountingVoucherUseCase.Result.JournalNotFound -> _feedback.value = "Journal OD introuvable."
            CreateAccountingVoucherUseCase.Result.AccountNotFound -> _feedback.value = "Un compte est inactif ou n'appartient pas au référentiel courant."
            CreateAccountingVoucherUseCase.Result.PeriodClosed -> _feedback.value = "La période de cette écriture est clôturée."
        }
    }

    fun comptabiliser(voucherId: Long) = viewModelScope.launch {
        _feedback.value = when (postVoucher(voucherId)) {
            PostAccountingVoucherUseCase.Result.Success -> "Pièce comptabilisée. Elle est désormais immuable."
            PostAccountingVoucherUseCase.Result.NotFound -> "Pièce introuvable."
            PostAccountingVoucherUseCase.Result.InvalidState -> "Seuls les brouillons peuvent être comptabilisés."
            PostAccountingVoucherUseCase.Result.Unbalanced -> "Pièce non équilibrée : débit et crédit doivent être égaux."
            PostAccountingVoucherUseCase.Result.AccountInvalid -> "La pièce contient un compte invalide ou inactif."
            PostAccountingVoucherUseCase.Result.PeriodClosed -> "La période comptable est clôturée."
            PostAccountingVoucherUseCase.Result.PermissionDenied -> "Permission de validation ou modification refusée."
            PostAccountingVoucherUseCase.Result.SameUserApproval -> "Un autre utilisateur doit valider cette pièce (séparation des rôles)."
            PostAccountingVoucherUseCase.Result.ReadOnly -> "Licence en lecture seule."
        }
    }

    fun effacerFeedback() { _feedback.value = null }

    private fun debut(periode: Periode): Long = Calendar.getInstance().apply {
        when (periode) {
            Periode.MOIS -> set(Calendar.DAY_OF_MONTH, 1)
            Periode.MOIS_PRECEDENT -> { add(Calendar.MONTH, -1); set(Calendar.DAY_OF_MONTH, 1) }
            Periode.ANNEE -> set(Calendar.DAY_OF_YEAR, 1)
        }
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun fin(periode: Periode): Long = Calendar.getInstance().apply {
        if (periode == Periode.MOIS_PRECEDENT) { set(Calendar.DAY_OF_MONTH, 1); add(Calendar.DAY_OF_MONTH, -1) }
        set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999)
    }.timeInMillis
}
