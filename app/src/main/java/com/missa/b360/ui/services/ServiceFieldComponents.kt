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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.missa.b360.core.data.entity.ServiceRequestEntity
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
    Surface(shape = RoundedCornerShape(18.dp), color = ServicePink.copy(alpha = 0.09f)) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Text("INTERVENTIONS TERRAIN", fontSize = 11.sp, color = Color(0xFF6B7280), fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FieldKpi("Demandes à traiter", openRequests.toString(), Modifier.weight(1f))
                FieldKpi("Ordres actifs", activeOrders.toString(), Modifier.weight(1f))
                FieldKpi("Urgents", urgent.toString(), Modifier.weight(1f), if (urgent > 0) Color(0xFFB91C1C) else ServicePink)
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onNewRequest,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ServicePink),
                ) { Text("+ Demande client") }
                OutlinedButton(
                    onClick = onNewOrder,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                ) { Text("+ Ordre direct", color = ServicePink) }
            }
        }
    }
}

@Composable
private fun FieldKpi(label: String, value: String, modifier: Modifier = Modifier, color: Color = ServicePink) {
    Column(modifier) {
        Text(value, color = color, fontWeight = FontWeight.Bold, fontSize = 18.sp)
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
    Surface(shape = RoundedCornerShape(14.dp), color = Color.White, tonalElevation = 1.dp) {
        Column(Modifier.fillMaxWidth().padding(13.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(request.reference, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ServicePink, modifier = Modifier.weight(1f))
                Text(request.priority.toPriorityLabel(), fontSize = 10.sp, color = priorityColor, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(3.dp))
            Text(request.customerName, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1F2937))
            Text(request.description, fontSize = 12.sp, color = Color(0xFF4B5563), lineHeight = 17.sp)
            if (!request.address.isNullOrBlank()) Text(request.address, fontSize = 11.sp, color = Color(0xFF6B7280))
            if (ServiceWorkflowRules.isOverdue(request.resolutionDeadlineAt, System.currentTimeMillis(), request.status in setOf("RESOLVED", "CLOSED", "CANCELLED", "CONVERTED"))) {
                Text("SLA de résolution dépassé", fontSize = 10.sp, color = Color(0xFFB91C1C), fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(7.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("${request.requestType.toTypeLabel()} · ${request.status.toStatusLabel()}", fontSize = 10.sp, color = Color(0xFF6B7280))
                when (request.status) {
                    "OPEN" -> OutlinedButton(onClick = onQualify, shape = RoundedCornerShape(10.dp)) { Text("Qualifier", color = ServicePink, fontSize = 11.sp) }
                    "QUALIFIED" -> OutlinedButton(onClick = onConvert, shape = RoundedCornerShape(10.dp)) { Text("Créer l'ordre", color = ServicePink, fontSize = 11.sp) }
                }
            }
        }
    }
}

@Composable
internal fun ServiceWorkOrderCard(
    order: ServiceWorkOrderEntity,
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
    Surface(shape = RoundedCornerShape(14.dp), color = Color.White, tonalElevation = 1.dp) {
        Column(Modifier.fillMaxWidth().padding(13.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(order.reference, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ServicePink, modifier = Modifier.weight(1f))
                Text(status.toLabel(), fontSize = 10.sp, color = statusColor, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(3.dp))
            Text(order.customerName, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1F2937))
            Text(order.description, fontSize = 12.sp, color = Color(0xFF4B5563), lineHeight = 17.sp)
            val schedule = order.plannedStartAt?.let { dateFormat.format(Date(it)) }
            Text(
                listOfNotNull(schedule, technicianName?.let { "Technicien·ne : $it" }, order.address).joinToString(" · ").ifBlank { "À planifier" },
                fontSize = 10.sp,
                color = Color(0xFF6B7280),
            )
            if (ServiceWorkflowRules.isOverdue(order.resolutionDeadlineAt, System.currentTimeMillis(), status in setOf(ServiceWorkOrderStatus.READY_TO_BILL, ServiceWorkOrderStatus.CLOSED, ServiceWorkOrderStatus.CANCELLED))) {
                Text("SLA de résolution dépassé", fontSize = 10.sp, color = Color(0xFFB91C1C), fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                when (status) {
                    ServiceWorkOrderStatus.TO_PLAN -> Action("Planifier", onSchedule)
                    ServiceWorkOrderStatus.SCHEDULED -> {
                        Action("En route", { onTransition(ServiceWorkOrderStatus.EN_ROUTE) })
                        Action("Démarrer", { onTransition(ServiceWorkOrderStatus.IN_PROGRESS) })
                    }
                    ServiceWorkOrderStatus.EN_ROUTE -> Action("Démarrer", { onTransition(ServiceWorkOrderStatus.IN_PROGRESS) })
                    ServiceWorkOrderStatus.IN_PROGRESS -> {
                        Action("Temps", onTimesheet)
                        Action("Rapport", onReport)
                    }
                    ServiceWorkOrderStatus.WAITING_CUSTOMER,
                    ServiceWorkOrderStatus.WAITING_PARTS,
                    ServiceWorkOrderStatus.WAITING_SUPPLIER -> Action("Reprendre", { onTransition(ServiceWorkOrderStatus.IN_PROGRESS) })
                    ServiceWorkOrderStatus.AWAITING_VALIDATION -> Action("Valider le rapport", onApprove)
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
private fun String.toPriorityLabel() = when (this) { "URGENT" -> "URGENT"; "HIGH" -> "HAUTE"; "LOW" -> "BASSE"; else -> "NORMALE" }
private fun String.toTypeLabel() = when (this) { "BREAKDOWN" -> "Panne"; "MAINTENANCE" -> "Maintenance"; "INSTALLATION" -> "Installation"; "WARRANTY" -> "Garantie"; "CUSTOMER_QUERY" -> "Demande client"; else -> "Autre" }
private fun String.toStatusLabel() = when (this) { "OPEN" -> "Ouverte"; "QUALIFIED" -> "Qualifiée"; "QUOTE_REQUIRED" -> "Devis requis"; "CONVERTED" -> "Convertie"; "CLOSED" -> "Clôturée"; else -> this }
private fun ServiceWorkOrderStatus.toLabel() = when (this) {
    ServiceWorkOrderStatus.TO_PLAN -> "À planifier"
    ServiceWorkOrderStatus.SCHEDULED -> "Planifiée"
    ServiceWorkOrderStatus.EN_ROUTE -> "En route"
    ServiceWorkOrderStatus.IN_PROGRESS -> "En cours"
    ServiceWorkOrderStatus.WAITING_CUSTOMER -> "Attente client"
    ServiceWorkOrderStatus.WAITING_PARTS -> "Attente pièces"
    ServiceWorkOrderStatus.WAITING_SUPPLIER -> "Attente fournisseur"
    ServiceWorkOrderStatus.AWAITING_VALIDATION -> "À valider"
    ServiceWorkOrderStatus.READY_TO_BILL -> "Prête à facturer"
    ServiceWorkOrderStatus.CLOSED -> "Clôturée"
    ServiceWorkOrderStatus.CANCELLED -> "Annulée"
}
