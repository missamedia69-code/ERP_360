package com.missa.b360.core.domain.model

/**
 * Règles pures du cycle commercial devis → commande → facture (spec §20).
 *
 * Les devis et commandes réutilisent le payload de vente [SaleRecordPayload]
 * (client, lignes, remises, taxes, total) : aucune donnée n'est dupliquée en
 * double modèle. La liaison entre pièces passe par `sourceRecordId` :
 *  - commande.sourceRecordId = id du devis d'origine ;
 *  - facture.sourceRecordId  = id de la commande facturationnée.
 */
object DevisCommandeRules {

    /**
     * Copie d'un payload pour la pièce suivante du cycle : identifiants de lignes
     * remis à zéro (les ids ne sont pas uniques entre pièces), rattachement à la
     * pièce d'origine, note vidée. Les montants sont conservés tels quels —
     * [com.missa.b360.core.domain.usecase.SaveSaleUseCase] recalcule les totaux
     * transactionnellement à la facturation.
     */
    fun payloadCopie(payload: SaleRecordPayload, sourceRecordId: Long?): SaleRecordPayload =
        payload.copy(
            lines = payload.lines.mapIndexed { index, line -> line.copy(id = (index + 1).toLong()) },
            note = null,
            sourceRecordId = sourceRecordId,
        )

    /**
     * Une commande est « facturée » dès qu'une pièce de vente validée lui est
     * rattachée via `sourceRecordId` — l'interdiction de double facturation.
     */
    fun estFacturee(ventes: List<SaleRecordPayload>, commandeId: Long): Boolean =
        ventes.any { it.sourceRecordId == commandeId && it.total > 0.0 }
}
