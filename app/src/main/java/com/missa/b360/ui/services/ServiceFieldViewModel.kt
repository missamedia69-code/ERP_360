package com.missa.b360.ui.services

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.missa.b360.core.data.entity.ClientEntity
import com.missa.b360.core.data.entity.ServiceRequestEntity
import com.missa.b360.core.data.entity.ServiceWorkOrderEntity
import com.missa.b360.core.domain.model.ServicePriority
import com.missa.b360.core.domain.model.ServiceRequestChannel
import com.missa.b360.core.domain.model.ServiceRequestType
import com.missa.b360.core.domain.model.ServiceWorkOrderStatus
import com.missa.b360.core.domain.usecase.ObserveClientsUseCase
import com.missa.b360.core.domain.usecase.ServiceWorkflowUseCases
import com.missa.b360.core.data.dao.EmployeeDao
import com.missa.b360.core.domain.model.ServiceWorkflowRules
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ServiceFieldViewModel @Inject constructor(
    private val services: ServiceWorkflowUseCases,
    observeClients: ObserveClientsUseCase,
) : ViewModel() {
    data class State(
        val requests: List<ServiceRequestEntity> = emptyList(),
        val workOrders: List<ServiceWorkOrderEntity> = emptyList(),
        val clients: List<ClientEntity> = emptyList(),
        val employees: List<com.missa.b360.core.data.entity.EmployeeEntity> = emptyList(),
    )

    sealed interface Message {
        data class Success(val text: String, val reference: String? = null) : Message
        data class Error(val reason: ServiceWorkflowUseCases.Result) : Message
    }

    private val _message = MutableStateFlow<Message?>(null)
    val message: StateFlow<Message?> = _message
    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy

    val state: StateFlow<State> = combine(
        services.observeRequests(), services.observeWorkOrders(), observeClients(), services.observeEmployees(),
    ) { requests, workOrders, clients, employees ->
        State(requests, workOrders, clients, employees)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), State())

    fun clearMessage() { _message.value = null }

    fun createRequest(
        client: ClientEntity?,
        description: String,
        requestType: ServiceRequestType = ServiceRequestType.OTHER,
        priority: ServicePriority = ServicePriority.NORMAL,
        channel: ServiceRequestChannel = ServiceRequestChannel.PHONE,
        contact: String? = null,
        phone: String? = null,
        address: String? = null,
    ) = run("Demande enregistrée") {
        if (client == null) return@run ServiceWorkflowUseCases.Result.Invalid
        services.createRequest(client.id, client.nom, description, requestType, priority, channel, contact, phone, address)
    }

    fun qualifyRequest(id: Long) = run("Demande qualifiée") { services.qualifyRequest(id) }

    fun convertRequest(id: Long) = run("Ordre d'intervention créé") { services.convertRequest(id) }

    fun createWorkOrder(client: ClientEntity?, description: String, priority: ServicePriority = ServicePriority.NORMAL) = run("Ordre d'intervention créé") {
        if (client == null) return@run ServiceWorkflowUseCases.Result.Invalid
        services.createWorkOrder(client.id, client.nom, description, priority = priority)
    }

    fun schedule(id: Long, technicianId: Long, startAt: Long, endAt: Long) = run("Intervention planifiée") {
        services.schedule(id, technicianId, startAt, endAt)
    }

    fun transition(id: Long, status: ServiceWorkOrderStatus) = run("Statut mis à jour") {
        services.transition(id, status)
    }

    fun submitReport(id: Long, diagnosis: String, work: String, resolved: Boolean, signer: String?, signatureUri: String?) = run("Rapport envoyé pour validation") {
        services.submitReport(id, diagnosis, work, customerResolved = resolved, customerSignerName = signer, customerSignatureUri = signatureUri)
    }

    fun addTimesheet(id: Long, technicianId: Long, minutes: Int) = run("Temps enregistré") {
        services.addTimesheet(id, technicianId, System.currentTimeMillis(), workMinutes = minutes)
    }

    fun addAttachment(id: Long, uri: String) = run("Photo ajoutée au dossier") {
        services.addAttachment(id, "PHOTO", uri)
    }

    fun approveReport(id: Long) = run("Rapport validé") { services.approveReport(id) }

    private fun run(successText: String, action: suspend () -> ServiceWorkflowUseCases.Result) {
        if (_busy.value) return
        viewModelScope.launch {
            _busy.value = true
            try {
                when (val result = action()) {
                    is ServiceWorkflowUseCases.Result.Success -> _message.value = Message.Success(successText, result.reference)
                    else -> _message.value = Message.Error(result)
                }
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (_: Exception) {
                _message.value = Message.Error(ServiceWorkflowUseCases.Result.Invalid)
            } finally {
                _busy.value = false
            }
        }
    }
}
