package com.missa.b360.core.domain.model

import com.missa.b360.core.data.entity.FournisseurDocumentEntity
import com.missa.b360.core.data.entity.VerificationStatut

/** Synthèse de conformité d'un fournisseur (documents non archivés uniquement). */
data class ConformiteResume(
    val total: Int = 0,
    val expires: Int = 0,
    /** Expirent dans 30 jours ou moins (alertes « forte » et « alerte »). */
    val aExpirer: Int = 0,
    val rejetes: Int = 0,
    val aVerifier: Int = 0,
) {
    val conforme: Boolean get() = expires == 0 && rejetes == 0
}

object FournisseurConformiteRules {

    fun alerte(document: FournisseurDocumentEntity, now: Long): FournisseurRules.AlerteDocument =
        FournisseurRules.alerteDocument(document.dateExpiration, now)

    fun resumer(documents: List<FournisseurDocumentEntity>, now: Long): ConformiteResume {
        val actifs = documents.filterNot { it.archive }
        return ConformiteResume(
            total = actifs.size,
            expires = actifs.count { alerte(it, now) == FournisseurRules.AlerteDocument.EXPIRE },
            aExpirer = actifs.count {
                val a = alerte(it, now)
                a == FournisseurRules.AlerteDocument.FORTE || a == FournisseurRules.AlerteDocument.ALERTE
            },
            rejetes = actifs.count { it.verification == VerificationStatut.REJETE },
            aVerifier = actifs.count { it.verification == VerificationStatut.A_VERIFIER },
        )
    }

    /** Expirés d'abord, puis par date d'expiration croissante ; sans échéance en dernier. */
    fun trier(documents: List<FournisseurDocumentEntity>): List<FournisseurDocumentEntity> =
        documents.filterNot { it.archive }
            .sortedWith(compareBy<FournisseurDocumentEntity> { it.dateExpiration == null }.thenBy { it.dateExpiration ?: 0L }.thenBy { it.id })

    /**
     * Documents à renouveler : non archivés, avec une échéance, expirés ou arrivant à terme dans
     * [horizonJours] jours ; les plus urgents (déjà expirés, puis les plus proches) en premier.
     */
    fun aRenouveler(
        documents: List<FournisseurDocumentEntity>,
        now: Long,
        horizonJours: Int = 90,
    ): List<FournisseurDocumentEntity> {
        val limite = now + horizonJours * 86_400_000L
        return documents
            .filter { !it.archive && (it.dateExpiration ?: Long.MAX_VALUE) <= limite }
            .sortedWith(compareBy<FournisseurDocumentEntity> { it.dateExpiration ?: 0L }.thenBy { it.id })
    }
}
