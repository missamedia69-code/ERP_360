package com.missa.b360.core.documents

import com.missa.b360.core.data.entity.EnterpriseEntity
import com.missa.b360.core.data.entity.OperationRecordEntity
import com.missa.b360.core.data.entity.OperationStatus
import com.missa.b360.core.domain.model.SaleRecordCodec
import java.text.DateFormat
import java.util.Date
import java.util.Locale

/** Adaptateur unique des pièces Vente/Devis/Commande vers la charte PDF. */
object CommercialDocumentFactory {
    fun depuisVente(
        piece: OperationRecordEntity,
        entreprise: EnterpriseEntity,
        type: DocumentType,
        locale: Locale = Locale.getDefault(),
    ): DocumentDonnees? {
        require(type in setOf(DocumentType.DEVIS, DocumentType.BON_COMMANDE_CLIENT, DocumentType.FACTURE_CLIENT, DocumentType.NOTE_CREDIT))
        val payload = SaleRecordCodec.decode(piece.notes) ?: return null
        val montantHt = (payload.total - payload.taxAmount).coerceAtLeast(0.0)
        return DocumentDonnees(
            type = type,
            numero = piece.reference,
            dateEmission = DateFormat.getDateInstance(DateFormat.MEDIUM, locale).format(Date(piece.createdAt)),
            entreprise = entreprise,
            destinataire = DocumentPartie(
                titre = "Client",
                nom = payload.clientName.ifBlank { piece.counterpart.orEmpty().ifBlank { "Client comptoir" } },
            ),
            statut = when (piece.status) {
                OperationStatus.DRAFT.name -> "Brouillon"
                OperationStatus.VALIDATED.name -> if (payload.paidAmount >= payload.total) "Payé" else "Validé"
                OperationStatus.CANCELLED.name -> "Annulé"
                else -> piece.status
            },
            meta = buildList {
                add(DocumentChamp("Mode de paiement", payload.paymentMethod))
                if (payload.paidAmount > 0) add(DocumentChamp("Montant payé", "${payload.paidAmount} ${entreprise.devise}"))
                payload.sourceRecordId?.let { add(DocumentChamp("Pièce d’origine", it.toString())) }
            },
            lignes = payload.lines.map { ligne ->
                DocumentLigne(
                    designation = ligne.name,
                    quantite = ligne.quantity,
                    prixUnitaire = ligne.unitPrice,
                    taxePourcent = payload.taxRate,
                    montant = ligne.total,
                )
            },
            totaux = buildList {
                add(DocumentTotal("Sous-total", payload.subtotal))
                if (payload.discount > 0) add(DocumentTotal("Remise", -payload.discount))
                if (payload.delivery > 0) add(DocumentTotal("Livraison", payload.delivery))
                if (payload.taxAmount > 0) add(DocumentTotal("Taxes incluses (${payload.taxRate} %)", payload.taxAmount))
                add(DocumentTotal("Total TTC", payload.total, important = true))
                if (payload.paidAmount > 0.0 && payload.paidAmount < payload.total) add(DocumentTotal("Reste à payer", payload.total - payload.paidAmount, important = true))
            },
            notes = listOfNotNull(payload.note),
            signataires = when (type) {
                DocumentType.DEVIS -> listOf("Le commercial", "Bon pour accord — Client")
                DocumentType.BON_COMMANDE_CLIENT -> listOf("Le responsable", "Le client")
                DocumentType.NOTE_CREDIT -> listOf("Le responsable", "Le client")
                else -> listOf("Le responsable")
            },
        )
    }
}
