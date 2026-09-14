package com.missa.b360.core.domain.usecase

import com.missa.b360.core.data.dao.OperationRecordDao
import com.missa.b360.core.data.entity.OperationModule
import com.missa.b360.core.data.entity.OperationRecordEntity
import com.missa.b360.core.data.entity.OperationStatus
import com.missa.b360.core.domain.model.ModeFacturation
import com.missa.b360.core.domain.model.Prestation
import com.missa.b360.core.domain.model.PrestationCodec
import com.missa.b360.core.domain.model.PrestationPayload
import com.missa.b360.core.domain.model.PrestationRules
import com.missa.b360.core.journal.JournalManager
import com.missa.b360.core.licensing.LicenceManager
import com.missa.b360.core.numbering.DocType
import com.missa.b360.core.numbering.SequenceManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Prestations de services (module SER).
 *
 * Une prestation suit son propre cycle — planifiée, en cours, terminée — et
 * porte son mode de facturation. Elle s'appuie sur la table de pièces
 * générique, comme la facture et le bon de livraison : aucune migration.
 */
class PrestationUseCases @Inject constructor(
    private val dao: OperationRecordDao,
    private val sequenceManager: SequenceManager,
    private val licenceManager: LicenceManager,
    private val journalManager: JournalManager,
) {
    companion object { const val MODULE = "SERVICES" }

    sealed class Resultat {
        data class Succes(val id: Long, val reference: String) : Resultat()
        data object LectureSeule : Resultat()
        data object Invalide : Resultat()
        data object Introuvable : Resultat()
        data object EtapeFinale : Resultat()
    }

    fun observer(): Flow<List<Prestation>> = dao
        .observeByModule(OperationModule.SERVICES.name)
        .map { pieces ->
            PrestationRules.trier(
                pieces.mapNotNull { piece ->
                    PrestationCodec.decode(piece.notes)?.let { Prestation(piece, it) }
                },
            )
        }

    suspend fun creer(
        clientId: Long,
        clientNom: String,
        intitule: String,
        mode: ModeFacturation,
        tarif: Double,
        heures: Double = 0.0,
        intervenant: String? = null,
        lieu: String? = null,
        maintenant: Long = System.currentTimeMillis(),
    ): Resultat {
        val libelle = intitule.trim()
        if (!PrestationRules.intituleValide(libelle)) return Resultat.Invalide
        if (!tarif.isFinite() || tarif < 0.0) return Resultat.Invalide
        if (!heures.isFinite() || heures < 0.0) return Resultat.Invalide
        if (licenceManager.isReadOnly()) return Resultat.LectureSeule

        val payload = PrestationPayload(
            clientId = clientId,
            clientName = clientNom.trim(),
            intitule = libelle,
            intervenant = intervenant?.trim()?.ifEmpty { null },
            lieu = lieu?.trim()?.ifEmpty { null },
            mode = mode.name,
            tarif = tarif,
            heures = heures,
            datePrevue = maintenant,
        )
        val reference = sequenceManager.next(DocType.FACTURE)
        val id = dao.insert(
            OperationRecordEntity(
                module = OperationModule.SERVICES.name,
                reference = reference,
                title = libelle,
                counterpart = clientNom.trim().ifEmpty { null },
                tiersId = clientId.takeIf { it > 0 },
                amount = PrestationRules.montant(payload),
                quantity = heures.takeIf { it > 0 },
                status = OperationStatus.VALIDATED.name,
                notes = PrestationCodec.encode(payload),
                createdAt = maintenant,
            ),
        )
        journalManager.log(MODULE, "PRESTATION_CREEE", "$reference — $libelle")
        return Resultat.Succes(id, reference)
    }

    /** Fait avancer la prestation : planifiée → en cours → terminée. */
    suspend fun avancer(id: Long, maintenant: Long = System.currentTimeMillis()): Resultat {
        if (licenceManager.isReadOnly()) return Resultat.LectureSeule
        val piece = dao.getById(id) ?: return Resultat.Introuvable
        if (piece.status == OperationStatus.CANCELLED.name) return Resultat.EtapeFinale
        val payload = PrestationCodec.decode(piece.notes) ?: return Resultat.Introuvable
        val suivant = PrestationRules.avancer(payload, maintenant) ?: return Resultat.EtapeFinale
        dao.update(
            piece.copy(
                notes = PrestationCodec.encode(suivant),
                amount = PrestationRules.montant(suivant),
            ),
        )
        journalManager.log(MODULE, "PRESTATION_${suivant.etape}", piece.reference)
        return Resultat.Succes(id, piece.reference)
    }

    /**
     * Corrige le temps passé sur une prestation à l'heure : c'est lui qui fait
     * le montant, il doit rester modifiable jusqu'à la clôture.
     */
    suspend fun ajusterHeures(id: Long, heures: Double): Resultat {
        if (!heures.isFinite() || heures < 0.0) return Resultat.Invalide
        if (licenceManager.isReadOnly()) return Resultat.LectureSeule
        val piece = dao.getById(id) ?: return Resultat.Introuvable
        if (piece.status == OperationStatus.CANCELLED.name) return Resultat.EtapeFinale
        val payload = PrestationCodec.decode(piece.notes) ?: return Resultat.Introuvable
        val ajuste = payload.copy(heures = heures)
        dao.update(
            piece.copy(
                notes = PrestationCodec.encode(ajuste),
                amount = PrestationRules.montant(ajuste),
                quantity = heures.takeIf { it > 0 },
            ),
        )
        journalManager.log(MODULE, "PRESTATION_HEURES", "${piece.reference} — $heures h")
        return Resultat.Succes(id, piece.reference)
    }

    /** Annulation par compensation : la pièce reste, son statut change. */
    suspend fun annuler(id: Long): Resultat {
        if (licenceManager.isReadOnly()) return Resultat.LectureSeule
        val piece = dao.getById(id) ?: return Resultat.Introuvable
        if (piece.status == OperationStatus.CANCELLED.name) return Resultat.EtapeFinale
        dao.update(piece.copy(status = OperationStatus.CANCELLED.name))
        journalManager.log(MODULE, "PRESTATION_ANNULEE", piece.reference)
        return Resultat.Succes(id, piece.reference)
    }
}
