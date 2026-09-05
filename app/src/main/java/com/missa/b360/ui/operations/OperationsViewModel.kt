package com.missa.b360.ui.operations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.missa.b360.core.data.entity.OperationDirection
import com.missa.b360.core.data.entity.OperationModule
import com.missa.b360.core.data.entity.OperationRecordEntity
import com.missa.b360.core.data.entity.OperationStatus
import com.missa.b360.core.domain.usecase.GetEnterpriseUseCase
import com.missa.b360.core.domain.usecase.OperationUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OperationsViewModel @Inject constructor(
    private val operations: OperationUseCases,
    getEnterprise: GetEnterpriseUseCase,
) : ViewModel() {
    sealed class Result {
        data class Created(val reference: String) : Result()
        data object Invalid : Result()
        data object ReadOnly : Result()
        data object Error : Result()
    }

    private val _result = MutableStateFlow<Result?>(null)
    val result: StateFlow<Result?> = _result

    /** Anti double-soumission : création en cours (spec §3 SAUVEGARDE). */
    private val _enCours = MutableStateFlow(false)
    val enCours: StateFlow<Boolean> = _enCours

    val devise: StateFlow<String> = getEnterprise.observer()
        .map { it?.devise ?: "XAF" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "XAF")

    fun records(module: OperationModule): Flow<List<OperationRecordEntity>> = operations.observe(module)
    fun allRecords(): Flow<List<OperationRecordEntity>> = operations.observeAll()

    fun create(
        module: OperationModule,
        title: String,
        counterpart: String,
        amountText: String,
        quantityText: String,
        direction: OperationDirection,
        notes: String,
    ) {
        if (_enCours.value) return
        val amount = amountText.toAmountOrNull()
        val quantity = quantityText.toAmountOrNull()
        // Une saisie non vide non numérique doit être refusée plutôt que convertie silencieusement.
        if ((amountText.isNotBlank() && amount == null) || (quantityText.isNotBlank() && quantity == null)) {
            _result.value = Result.Invalid
            return
        }
        viewModelScope.launch {
            _enCours.value = true
            try {
                _result.value = when (
                    val result = operations.create(
                        OperationUseCases.CreateParams(
                            module = module,
                            title = title,
                            counterpart = counterpart,
                            amount = amount,
                            quantity = quantity,
                            direction = direction,
                            notes = notes,
                        ),
                    )
                ) {
                    is OperationUseCases.CreateResult.Success -> Result.Created(result.reference)
                    OperationUseCases.CreateResult.Invalid -> Result.Invalid
                    OperationUseCases.CreateResult.ReadOnly -> Result.ReadOnly
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                _result.value = Result.Error
            } finally {
                _enCours.value = false
            }
        }
    }

    fun setStatus(id: Long, status: OperationStatus) {
        viewModelScope.launch {
            try {
                if (!operations.setStatus(id, status)) _result.value = Result.Error
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                _result.value = Result.Error
            }
        }
    }

    fun clearResult() {
        _result.value = null
    }
}

private fun String.toAmountOrNull(): Double? = trim()
    .takeIf { it.isNotEmpty() }
    ?.replace(',', '.')
    ?.toDoubleOrNull()
