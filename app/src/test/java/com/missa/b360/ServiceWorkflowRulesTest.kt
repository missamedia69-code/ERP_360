package com.missa.b360

import com.missa.b360.core.domain.model.ServiceReportStatus
import com.missa.b360.core.domain.model.ServiceWorkOrderStatus
import com.missa.b360.core.domain.model.ServiceWorkflowRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ServiceWorkflowRulesTest {
    @Test fun `une demande doit avoir un client et une description exploitable`() {
        assertTrue(ServiceWorkflowRules.requestIsValid(3, "La pompe est en panne"))
        assertFalse(ServiceWorkflowRules.requestIsValid(0, "La pompe est en panne"))
        assertFalse(ServiceWorkflowRules.requestIsValid(3, "abc"))
    }

    @Test fun `la planification refuse une durée nulle ou passée`() {
        assertTrue(ServiceWorkflowRules.scheduleIsValid(2_000, 3_000))
        assertFalse(ServiceWorkflowRules.scheduleIsValid(2_000, 2_000))
        assertFalse(ServiceWorkflowRules.scheduleIsValid(4_000, 3_000))
        assertFalse(ServiceWorkflowRules.scheduleIsValid(2_000, 3_000, now = 3_500))
    }

    @Test fun `les créneaux qui se chevauchent sont détectés sans pénaliser les bornes adjacentes`() {
        assertTrue(ServiceWorkflowRules.overlaps(10, 20, 19, 30))
        assertFalse(ServiceWorkflowRules.overlaps(10, 20, 20, 30))
        assertFalse(ServiceWorkflowRules.overlaps(20, 30, 10, 20))
    }

    @Test fun `les transitions opérationnelles sont explicites et une clôture ne se réouvre pas`() {
        assertTrue(ServiceWorkflowRules.mayTransition(ServiceWorkOrderStatus.TO_PLAN, ServiceWorkOrderStatus.SCHEDULED))
        assertFalse(ServiceWorkflowRules.mayTransition(ServiceWorkOrderStatus.TO_PLAN, ServiceWorkOrderStatus.CLOSED))
        assertTrue(ServiceWorkflowRules.mayTransition(ServiceWorkOrderStatus.IN_PROGRESS, ServiceWorkOrderStatus.AWAITING_VALIDATION))
        assertFalse(ServiceWorkflowRules.mayTransition(ServiceWorkOrderStatus.CLOSED, ServiceWorkOrderStatus.IN_PROGRESS))
        assertTrue(ServiceWorkflowRules.mayTransition(ServiceWorkOrderStatus.SCHEDULED, ServiceWorkOrderStatus.CANCELLED))
    }

    @Test fun `les délais SLA sont calculés et les demandes échues signalées`() {
        assertEquals(1_120_000L, ServiceWorkflowRules.responseDeadline(1_000_000L, 2))
        assertEquals(null, ServiceWorkflowRules.responseDeadline(1_000_000L, null))
        assertTrue(ServiceWorkflowRules.isOverdue(10L, 11L, terminal = false))
        assertFalse(ServiceWorkflowRules.isOverdue(10L, 11L, terminal = true))
    }

    @Test fun `le temps exige des durées positives et des tarifs finis non négatifs`() {
        assertTrue(ServiceWorkflowRules.timesheetIsValid(30, 5, 0, 100.0, 120.0))
        assertFalse(ServiceWorkflowRules.timesheetIsValid(0, 0, 0, 100.0, 120.0))
        assertFalse(ServiceWorkflowRules.timesheetIsValid(30, -1, 0, 100.0, 120.0))
        assertFalse(ServiceWorkflowRules.timesheetIsValid(30, 0, 0, Double.NaN, 120.0))
    }

    @Test fun `un rapport exige une signature client et une validation distincte de la création`() {
        assertTrue(ServiceWorkflowRules.reportCanBeApproved(ServiceReportStatus.SUBMITTED, "M. client", "content://sig", 10L, 7L, 8L))
        assertFalse(ServiceWorkflowRules.reportCanBeApproved(ServiceReportStatus.SUBMITTED, "M. client", null, 10L, 7L, 8L))
        assertFalse(ServiceWorkflowRules.reportCanBeApproved(ServiceReportStatus.SUBMITTED, "M. client", "content://sig", 10L, 8L, 8L))
        assertFalse(ServiceWorkflowRules.reportCanBeApproved(ServiceReportStatus.DRAFT, "M. client", "content://sig", 10L, 7L, 8L))
    }
}
