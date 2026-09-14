package com.missa.b360.core.domain.usecase

import com.missa.b360.core.data.dao.OperationRecordDao
import com.missa.b360.core.data.entity.OperationModule
import com.missa.b360.core.data.entity.OperationRecordEntity
import com.missa.b360.core.data.entity.OperationStatus
import com.missa.b360.core.domain.model.BonLivraison
import com.missa.b360.core.domain.model.LivraisonCodec
import com.missa.b360.core.domain.model.LivraisonPayload
import com.missa.b360.core.domain.model.LivraisonRules
import com.missa.b360.core.journal.JournalManager
import com.missa.b360.core.licensing.LicenceManager
import com.missa.b360.core.numbering.DocType
import com.missa.b360.core.numbering.SequenceManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Bons de livraison (module LOG / LIVRAISON).
 *
 * Le bon s'appuie sur la table de pièces générique : la référence vient du
 * compteur atomique, et le détail métier — destinataire, transporteur, colis,
 * articles — vit dans un payload typé. Aucune table supplémentaire, donc aucune
 * migration, et la pièce reste visible partout où les pièces le sont.
 */
class LivraisonUseCases @Inject constructor(
    private val dao: OperationRecordDao,
    private val sequenceManager: SequenceManager,
    private val licenceManager: LicenceManager,
    private val journalManager: JournalManager,
) {
    companion object { const val MODULE = "LIVRAISON" }

    sealed class Resultat {
        data class Succes(val id: Long, val reference: String) : Resultat()
        data object LectureSeule : Resultat()
        data object Invalide : Resultat()
        data object Introuvable : Resultat()
        /** Un bon déjà remis ou annulé ne bouge plus. */
        data object EtapeFinale : Resultat()
    }

    /** Bons de livraison, détail décodé, dans l'ordre de traitement. */
    fun observer(): Flow<List<BonLivraison>> = dao
        .observeByModule(OperationModule.LIVRAISON.name)
        .map { pieces ->
            LivraisonRules.trier(
                pieces.mapNotNull { piece ->
                    LivraisonCodec.decode(piece.notes)?.let { BonLivraison(piece, it) }
                },
            )
        }

    suspend fun creer(
        clientId: Long,
        clientNom: String,
        adresse: String? = null,
        contact: String? = null,
        transporteur: String? = null,
        nombreColis: Int = 0,
        poidsKg: Double = 0.0,
        referenceOrigine: String? = null,
        note: String? = null,
        maintenant: Long = System.currentTimeMillis(),
    ): Resultat {
        val destinataire = clientNom.trim()
        if (!LivraisonRules.destinataireValide(destinataire)) return Resultat.Invalide
        if (nombreColis < 0 || !poidsKg.isFinite() || poidsKg < 0.0) return Resultat.Invalide
        if (licenceManager.isReadOnly()) return Resultat.LectureSeule

        val payload = LivraisonPayload(
            clientId = clientId,
            clientName = destinataire,
            adresseLivraison = adresse?.trim()?.ifEmpty { null },
            contact = contact?.trim()?.ifEmpty { null },
            transporteur = transporteur?.trim()?.ifEmpty { null },
            nombreColis = nombreColis,
            poidsKg = poidsKg,
            referenceOrigine = referenceOrigine?.trim()?.ifEmpty { null },
            note = note?.trim()?.ifEmpty { null },
        )
        val reference = sequenceManager.next(DocType.LIVRAISON)
        val id = dao.insert(
            OperationRecordEntity(
                module = OperationModule.LIVRAISON.name,
                reference = reference,
                title = destinataire,
                counterpart = destinataire,
                tiersId = clientId.takeIf { it > 0 },
                quantity = nombreColis.toDouble(),
                status = OperationStatus.VALIDATED.name,
                notes = LivraisonCodec.encode(payload),
                createdAt = maintenant,
            ),
        )
        journalManager.log(MODULE, "BL_CREE", "$reference — $destinataire")
        return Resultat.Succes(id, reference)
    }

    /** Fait avancer le bon : à préparer → expédiée → livrée. */
    suspend fun avancer(id: Long, maintenant: Long = System.currentTimeMillis()): Resultat {
        if (licenceManager.isReadOnly()) return Resultat.LectureSeule
        val piece = dao.getById(id) ?: return Resultat.Introuvable
        if (piece.status == OperationStatus.CANCELLED.name) return Resultat.EtapeFinale
        val payload = LivraisonCodec.decode(piece.notes) ?: return Resultat.Introuvable
        val suivant = LivraisonRules.avancer(payload, maintenant) ?: return Resultat.EtapeFinale
        dao.update(piece.copy(notes = LivraisonCodec.encode(suivant)))
        journalManager.log(MODULE, "BL_${suivant.etape}", piece.reference)
        return Resultat.Succes(id, piece.reference)
    }

    /**
     * Annulation par compensation : la pièce passe à ANNULÉ, elle n'est jamais
     * supprimée — un bon de livraison est une trace de ce qui a quitté le dépôt.
     */
    suspend fun annuler(id: Long): Resultat {
        if (licenceManager.isReadOnly()) return Resultat.LectureSeule
        val piece = dao.getById(id) ?: return Resultat.Introuvable
        if (piece.status == OperationStatus.CANCELLED.name) return Resultat.EtapeFinale
        dao.update(piece.copy(status = OperationStatus.CANCELLED.name))
        journalManager.log(MODULE, "BL_ANNULE", piece.reference)
        return Resultat.Succes(id, piece.reference)
    }
}
