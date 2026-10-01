package com.missa.b360.core.domain.model

import com.missa.b360.core.data.entity.ClientFollowupEntity
import com.missa.b360.core.data.entity.ClientPaymentEntity
import com.missa.b360.core.data.entity.FollowupType

/** Vente ou avoir validé, réduit à ce qu'affiche la chronologie. */
data class ActivitySale(val reference: String, val date: Long, val total: Double, val avoir: Boolean)

enum class ActivityKind { VENTE, AVOIR, ENCAISSEMENT, RELANCE, APPEL, PROMESSE, NOTE }

/** Groupes proposés comme filtres de l'écran Activité. */
enum class ActivityFilter { TOUT, VENTES, ENCAISSEMENTS, SUIVI }

data class ActivityEntry(
    val date: Long,
    val kind: ActivityKind,
    val reference: String? = null,
    val montant: Double? = null,
    val detail: String? = null,
    val followup: ClientFollowupEntity? = null,
)

/** Chronologie d'un client : ventes, avoirs, encaissements et journal de suivi, du plus récent au plus ancien. */
object ClientActivityRules {
    fun construire(
        ventes: List<ActivitySale>,
        paiements: List<ClientPaymentEntity>,
        suivis: List<ClientFollowupEntity>,
    ): List<ActivityEntry> {
        val entrees = ArrayList<ActivityEntry>(ventes.size + paiements.size + suivis.size)
        ventes.forEach {
            entrees += ActivityEntry(it.date, if (it.avoir) ActivityKind.AVOIR else ActivityKind.VENTE, it.reference, it.total)
        }
        paiements.forEach {
            entrees += ActivityEntry(
                date = it.paiementAt,
                kind = ActivityKind.ENCAISSEMENT,
                reference = it.reference,
                montant = it.montant,
                detail = it.modePaiement,
            )
        }
        suivis.forEach {
            val kind = when (it.type) {
                FollowupType.RELANCE -> ActivityKind.RELANCE
                FollowupType.APPEL -> ActivityKind.APPEL
                FollowupType.PROMESSE -> ActivityKind.PROMESSE
                FollowupType.NOTE -> ActivityKind.NOTE
            }
            entrees += ActivityEntry(it.createdAt, kind, montant = it.promesseMontant, detail = it.message, followup = it)
        }
        return entrees.sortedByDescending { it.date }
    }

    fun filtrer(entrees: List<ActivityEntry>, filtre: ActivityFilter): List<ActivityEntry> = when (filtre) {
        ActivityFilter.TOUT -> entrees
        ActivityFilter.VENTES -> entrees.filter { it.kind == ActivityKind.VENTE || it.kind == ActivityKind.AVOIR }
        ActivityFilter.ENCAISSEMENTS -> entrees.filter { it.kind == ActivityKind.ENCAISSEMENT }
        ActivityFilter.SUIVI -> entrees.filter {
            it.kind == ActivityKind.RELANCE || it.kind == ActivityKind.APPEL ||
                it.kind == ActivityKind.PROMESSE || it.kind == ActivityKind.NOTE
        }
    }
}
