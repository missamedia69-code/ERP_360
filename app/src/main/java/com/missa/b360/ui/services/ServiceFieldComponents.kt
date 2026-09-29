package com.missa.b360.ui.services

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.missa.b360.R
import com.missa.b360.core.data.entity.ServiceRequestEntity
import com.missa.b360.core.data.entity.ServiceReportEntity
import com.missa.b360.core.data.entity.ServiceWorkOrderEntity
import com.missa.b360.core.domain.model.ServicePriority
import com.missa.b360.core.domain.model.ServiceWorkOrderStatus
import com.missa.b360.core.domain.model.ServiceWorkflowRules
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val ServicePink = Color(0xFFDB2777)

@Composable
internal fun ServiceFieldOverview(
    requests: List<ServiceRequestEntity>,
    workOrders: List<ServiceWorkOrderEntity>,
    onNewRequest: () -> Unit,
    onNewOrder: () -> Unit,
) {
    val openRequests = requests.count { it.status in setOf("OPEN", "QUALIFIED", "QUOTE_REQUIRED") }
    val activeOrders = workOrders.count {
        ServiceWorkflowRules.status(it.status) in setOf(
            ServiceWorkOrderStatus.SCHEDULED, ServiceWorkOrderStatus.EN_ROUTE,
            ServiceWorkOrderStatus.IN_PROGRESS, ServiceWorkOrderStatus.WAITING_CUSTOMER,
            ServiceWorkOrderStatus.WAITING_PARTS, ServiceWorkOrderStatus.WAITING_SUPPLIER,
        )
    }
    val urgent = requests.count { it.priority == ServicePriority.URGENT.name && it.status == "OPEN" } +
        workOrders.count { it.priority == ServicePriority.URGENT.name && it.status !in setOf("CLOSED", "CANCELLED") }
    Surface(shape = RoundedCornerShape(16.dp), color = ServicePink.copy(alpha = 0.09f)) {
        Column(Modifier.fillMaxWidth().padding(10.dp)) {
            Text(stringResource(R.string.srvf_titre_terrain), fontSize = 11.sp, color = Color(0xFF6B7280), fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FieldKpi(stringResource(R.string.srvf_kpi_demandes), openRequests.toString(), Modifier.weight(1f))
                FieldKpi(stringResource(R.string.srvf_kpi_ordres), activeOrders.toString(), Modifier.weight(1f))
                FieldKpi(stringResource(R.string.srvf_kpi_urgents), urgent.toString(), Modifier.weight(1f), if (urgent > 0) Color(0xFFB91C1C) else ServicePink)
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    onClick = onNewRequest,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ServicePink),
                ) { Text("+ " + stringResource(R.string.srvf_demande_client)) }
                OutlinedButton(
                    onClick = onNewOrder,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                ) { Text("+ " + stringResource(R.string.srvf_ordre_direct), color = ServicePink) }
            }
        }
    }
}

@Composable
private fun FieldKpi(label: String, value: String, modifier: Modifier = Modifier, color: Color = ServicePink) {
    Column(modifier) {
        Text(value, color = color, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(label, color = Color(0xFF6B7280), fontSize = 10.sp, lineHeight = 13.sp)
    }
}

@Composable
internal fun ServiceRequestCard(
    request: ServiceRequestEntity,
    onQualify: () -> Unit,
    onConvert: () -> Unit,
) {
    val priorityColor = priorityColor(request.priority)
    Surface(shape = RoundedCornerShape(12.dp), color = Color.White, tonalElevation = 1.dp) {
        Column(Modifier.fillMaxWidth().padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(request.reference, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ServicePink, modifier = Modifier.weight(1f))
                Text(stringResource(priorityLabelRes(request.priority)).uppercase(), fontSize = 10.sp, color = priorityColor, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(3.dp))
            Text(request.customerName, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1F2937))
            Text(request.description, fontSize = 12.sp, color = Color(0xFF4B5563), lineHeight = 17.sp)
            if (!request.address.isNullOrBlank()) Text(request.address, fontSize = 11.sp, color = Color(0xFF6B7280))
            if (ServiceWorkflowRules.isOverdue(request.resolutionDeadlineAt, System.currentTimeMillis(), request.status in setOf("RESOLVED", "CLOSED", "CANCELLED", "CONVERTED"))) {
                Text(stringResource(R.string.srvf_sla_depasse), fontSize = 10.sp, color = Color(0xFFB91C1C), fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(5.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("${stringResource(typeLabelRes(request.requestType))} · ${statusLabelRes(request.status)?.let { stringResource(it) } ?: request.status}", fontSize = 10.sp, color = Color(0xFF6B7280))
                when (request.status) {
                    "OPEN" -> OutlinedButton(onClick = onQualify, shape = RoundedCornerShape(10.dp)) { Text(stringResource(R.string.srvf_qualifier), color = ServicePink, fontSize = 11.sp) }
                    "QUALIFIED" -> OutlinedButton(onClick = onConvert, shape = RoundedCornerShape(10.dp)) { Text(stringResource(R.string.srvf_creer_ordre), color = ServicePink, fontSize = 11.sp) }
                }
            }
        }
    }
}

@Composable
internal fun ServiceWorkOrderCard(
    order: ServiceWorkOrderEntity,
    report: ServiceReportEntity?,
    technicianName: String?,
    onSchedule: () -> Unit,
    onTransition: (ServiceWorkOrderStatus) -> Unit,
    onReport: () -> Unit,
    onTimesheet: () -> Unit,
    onApprove: () -> Unit,
) {
    val status = ServiceWorkflowRules.status(order.status)
    val statusColor = when (status) {
        ServiceWorkOrderStatus.IN_PROGRESS, ServiceWorkOrderStatus.EN_ROUTE -> Color(0xFF15803D)
        ServiceWorkOrderStatus.WAITING_CUSTOMER, ServiceWorkOrderStatus.WAITING_PARTS, ServiceWorkOrderStatus.WAITING_SUPPLIER -> Color(0xFFD97706)
        ServiceWorkOrderStatus.CANCELLED -> Color(0xFFB91C1C)
        ServiceWorkOrderStatus.CLOSED, ServiceWorkOrderStatus.READY_TO_BILL -> Color(0xFF475569)
        else -> ServicePink
    }
    Surface(shape = RoundedCornerShape(12.dp), color = Color.White, tonalElevation = 1.dp) {
        Column(Modifier.fillMaxWidth().padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(order.reference, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ServicePink, modifier = Modifier.weight(1f))
                Text(stringResource(status.toLabelRes()), fontSize = 10.sp, color = statusColor, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(3.dp))
            Text(order.customerName, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1F2937))
            Text(order.description, fontSize = 12.sp, color = Color(0xFF4B5563), lineHeight = 17.sp)
            val schedule = order.plannedStartAt?.let { dateFormat.format(Date(it)) }
            Text(
                listOfNotNull(schedule, technicianName?.let { stringResource(R.string.srvf_technicien_x, it) }, order.address).joinToString(" · ").ifBlank { stringResource(R.string.srvf_st_a_planifier) },
                fontSize = 10.sp,
                color = Color(0xFF6B7280),
            )
            if (ServiceWorkflowRules.isOverdue(order.resolutionDeadlineAt, System.currentTimeMillis(), status in setOf(ServiceWorkOrderStatus.READY_TO_BILL, ServiceWorkOrderStatus.CLOSED, ServiceWorkOrderStatus.CANCELLED))) {
                Text(stringResource(R.string.srvf_sla_depasse), fontSize = 10.sp, color = Color(0xFFB91C1C), fontWeight = FontWeight.Bold)
            }
            if (status == ServiceWorkOrderStatus.AWAITING_VALIDATION && report != null) {
                Spacer(Modifier.height(6.dp))
                Text(stringResource(R.string.srvf_diagnostic_x, report.diagnosis), fontSize = 11.sp, color = Color(0xFF374151))
                Text(stringResource(R.string.srvf_travaux_x, report.workPerformed), fontSize = 11.sp, color = Color(0xFF374151))
                Text(
                    if (report.customerSignatureUri.isNullOrBlank()) stringResource(R.string.srvf_signature_manquante) else stringResource(R.string.srvf_signe_par, report.customerSignerName.orEmpty()),
                    fontSize = 10.sp,
                    color = if (report.customerSignatureUri.isNullOrBlank()) Color(0xFFB91C1C) else Color(0xFF15803D),
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                when (status) {
                    ServiceWorkOrderStatus.TO_PLAN -> Action(stringResource(R.string.srvf_planifier), onSchedule)
                    ServiceWorkOrderStatus.SCHEDULED -> {
                        Action(stringResource(R.string.srvf_st_en_route), { onTransition(ServiceWorkOrderStatus.EN_ROUTE) })
                        Action(stringResource(R.string.srvf_demarrer), { onTransition(ServiceWorkOrderStatus.IN_PROGRESS) })
                    }
                    ServiceWorkOrderStatus.EN_ROUTE -> Action(stringResource(R.string.srvf_demarrer), { onTransition(ServiceWorkOrderStatus.IN_PROGRESS) })
                    ServiceWorkOrderStatus.IN_PROGRESS -> {
                        Action(stringResource(R.string.srvf_temps), onTimesheet)
                        Action(stringResource(R.string.srvf_rapport), onReport)
                    }
                    ServiceWorkOrderStatus.WAITING_CUSTOMER,
                    ServiceWorkOrderStatus.WAITING_PARTS,
                    ServiceWorkOrderStatus.WAITING_SUPPLIER -> Action(stringResource(R.string.srvf_reprendre), { onTransition(ServiceWorkOrderStatus.IN_PROGRESS) })
                    ServiceWorkOrderStatus.AWAITING_VALIDATION -> Action(stringResource(R.string.srvf_valider_rapport), onApprove)
                    else -> Unit
                }
            }
        }
    }
}

@Composable
private fun RowScope.Action(label: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, shape = RoundedCornerShape(10.dp), modifier = Modifier.weight(1f)) {
        Text(label, fontSize = 10.sp, color = ServicePink, maxLines = 1)
    }
}

private val dateFormat get() = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
private fun priorityColor(value: String) = if (value == "URGENT") Color(0xFFB91C1C) else if (value == "HIGH") Color(0xFFD97706) else ServicePink
internal fun priorityLabelRes(value: String): Int = when (value) {
    "URGENT" -> R.string.srvf_prio_urgente
    "HIGH" -> R.string.srvf_prio_haute
    "LOW" -> R.string.srvf_prio_basse
    else -> R.string.srvf_prio_normale
}
internal fun typeLabelRes(value: String): Int = when (value) {
    "BREAKDOWN" -> R.string.srvf_type_panne
    "MAINTENANCE" -> R.string.srvf_type_maintenance
    "INSTALLATION" -> R.string.srvf_type_installation
    "WARRANTY" -> R.string.srvf_type_garantie
    "CUSTOMER_QUERY" -> R.string.srvf_type_demande
    else -> R.string.srvf_type_autre
}
private fun statusLabelRes(value: String): Int? = when (value) {
    "OPEN" -> R.string.srvf_dem_ouverte
    "QUALIFIED" -> R.string.srvf_dem_qualifiee
    "QUOTE_REQUIRED" -> R.string.srvf_dem_devis
    "CONVERTED" -> R.string.srvf_dem_convertie
    "CLOSED" -> R.string.srvf_st_cloturee
    else -> null
}
private fun ServiceWorkOrderStatus.toLabelRes(): Int = when (this) {
    ServiceWorkOrderStatus.TO_PLAN -> R.string.srvf_st_a_planifier
    ServiceWorkOrderStatus.SCHEDULED -> R.string.srvf_st_planifiee
    ServiceWorkOrderStatus.EN_ROUTE -> R.string.srvf_st_en_route
    ServiceWorkOrderStatus.IN_PROGRESS -> R.string.srvf_st_en_cours
    ServiceWorkOrderStatus.WAITING_CUSTOMER -> R.string.srvf_st_attente_client
    ServiceWorkOrderStatus.WAITING_PARTS -> R.string.srvf_st_attente_pieces
    ServiceWorkOrderStatus.WAITING_SUPPLIER -> R.string.srvf_st_attente_fournisseur
    ServiceWorkOrderStatus.AWAITING_VALIDATION -> R.string.srvf_st_a_valider
    ServiceWorkOrderStatus.READY_TO_BILL -> R.string.srvf_st_a_facturer
    ServiceWorkOrderStatus.CLOSED -> R.string.srvf_st_cloturee
    ServiceWorkOrderStatus.CANCELLED -> R.string.srvf_st_annulee
}
