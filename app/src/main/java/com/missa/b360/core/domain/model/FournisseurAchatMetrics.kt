package com.missa.b360.core.domain.model

import com.missa.b360.core.data.entity.OperationModule
import com.missa.b360.core.data.entity.OperationRecordEntity
import com.missa.b360.core.data.entity.OperationStatus

/** Agrégats achats affichés dans le hub et la fiche fournisseur. */
data class FournisseurAchatSummary(
    val montantAchete: Double = 0.0,
    val solde: Double = 0.0,
    val nombreCommandes: Int = 0,
    val derniereFacture: Long? = null,
)

/**
 * Calculs basés sur le contenu typé des pièces, jamais sur leur référence lisible
 * (dont les préfixes varient selon le type de pièce et la séquence configurée).
 */
object FournisseurAchatMetrics {

    private fun piecesAchatsValidees(pieces: List<OperationRecordEntity>): List<OperationRecordEntity> =
        pieces.filter {
            it.module == OperationModule.ACHATS.name && it.status == OperationStatus.VALIDATED.name
        }

    fun soldeTotal(pieces: List<OperationRecordEntity>): Double =
        piecesAchatsValidees(pieces).sumOf { piece ->
            val facture = PurchaseRecordCodec.decode(piece.notes) ?: return@sumOf 0.0
            (facture.total - facture.paidAmount).takeIf { it.isFinite() }?.coerceAtLeast(0.0) ?: 0.0
        }

    fun commandesOuvertes(pieces: List<OperationRecordEntity>): Int {
        val validees = piecesAchatsValidees(pieces)
        val receptionsParCommande = validees.mapNotNull { piece ->
            val reception = ReceptionCodec.decode(piece.notes) ?: return@mapNotNull null
            val commandeId = reception.commandeRecordId ?: return@mapNotNull null
            commandeId to reception
        }.groupBy({ it.first }, { it.second })

        return validees.count { piece ->
            val commande = CommandeAchatCodec.decode(piece.notes) ?: return@count false
            val commandees = commande.lines
                .filter { it.productId != null && it.quantity > 0.0 && it.quantity.isFinite() }
                .groupBy { it.productId!! }
                .mapValues { (_, lignes) -> lignes.sumOf { it.quantity } }
            val receptions = receptionsParCommande[piece.id].orEmpty()
            val recues = receptions.flatMap { it.lignes }
                .filter { it.quantiteRecue > 0.0 && it.quantiteRecue.isFinite() }
                .groupBy { it.productId }
                .mapValues { (_, lignes) -> lignes.sumOf { it.quantiteRecue } }

            // Les commandes de lignes libres ne disposent pas d'identifiants produit ;
            // faute de quantités rapprochables, elles restent ouvertes jusqu'à réception.
            if (commandees.isEmpty()) receptions.isEmpty()
            else !AchatCommandeRules.commandeSoldee(commandees, recues)
        }
    }

    fun pourFournisseur(fournisseurId: Long, pieces: List<OperationRecordEntity>): FournisseurAchatSummary {
        val validees = piecesAchatsValidees(pieces)
        val factures = validees.mapNotNull { piece ->
            val payload = PurchaseRecordCodec.decode(piece.notes) ?: return@mapNotNull null
            if (payload.supplierId != fournisseurId) return@mapNotNull null
            piece to payload
        }
        val commandes = validees.count { piece ->
            CommandeAchatCodec.decode(piece.notes)?.supplierId == fournisseurId
        }
        return FournisseurAchatSummary(
            montantAchete = factures.sumOf { (_, facture) -> facture.total.takeIf { it.isFinite() } ?: 0.0 },
            solde = factures.sumOf { (_, facture) ->
                (facture.total - facture.paidAmount).takeIf { it.isFinite() }?.coerceAtLeast(0.0) ?: 0.0
            },
            nombreCommandes = commandes,
            derniereFacture = factures.maxOfOrNull { (piece, _) -> piece.createdAt },
        )
    }
}
