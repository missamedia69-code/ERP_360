package com.missa.b360.core.domain.usecase

import com.missa.b360.core.data.dao.OperationRecordDao
import com.missa.b360.core.data.entity.OperationModule
import com.missa.b360.core.data.entity.OperationRecordEntity
import com.missa.b360.core.data.entity.OperationStatus
import com.missa.b360.core.domain.model.EtatProjet
import com.missa.b360.core.domain.model.Projet
import com.missa.b360.core.domain.model.ProjetCodec
import com.missa.b360.core.domain.model.ProjetPayload
import com.missa.b360.core.domain.model.ProjetRules
import com.missa.b360.core.journal.JournalManager
import com.missa.b360.core.licensing.LicenceManager
import com.missa.b360.core.numbering.DocType
import com.missa.b360.core.numbering.SequenceManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Projets (module PRJ) : budget, avancement et dérive.
 *
 * Le projet s'appuie sur la table de pièces générique, comme les autres
 * documents : la référence vient du compteur atomique et le métier vit dans un
 * payload typé.
 */
class ProjetUseCases @Inject constructor(
    private val dao: OperationRecordDao,
    private val sequenceManager: SequenceManager,
    private val licenceManager: LicenceManager,
    private val journalManager: JournalManager,
) {
    companion object { const val MODULE = "PROJETS" }

    sealed class Resultat {
        data class Succes(val id: Long, val reference: String) : Resultat()
        data object LectureSeule : Resultat()
        data object Invalide : Resultat()
        data object Introuvable : Resultat()
    }

    fun observer(): Flow<List<Projet>> = dao
        .observeByModule(OperationModule.PROJETS.name)
        .map { pieces ->
            ProjetRules.trier(
                pieces.mapNotNull { piece ->
                    ProjetCodec.decode(piece.notes)?.let { Projet(piece, it) }
                },
            )
        }

    suspend fun creer(
        nom: String,
        clientId: Long = 0,
        clientNom: String? = null,
        responsable: String? = null,
        budget: Double = 0.0,
        echeance: Long? = null,
        maintenant: Long = System.currentTimeMillis(),
    ): Resultat {
        val libelle = nom.trim()
        if (!ProjetRules.nomValide(libelle)) return Resultat.Invalide
        if (!budget.isFinite() || budget < 0.0) return Resultat.Invalide
        if (licenceManager.isReadOnly()) return Resultat.LectureSeule

        val payload = ProjetPayload(
            nom = libelle,
            clientId = clientId,
            clientName = clientNom?.trim()?.ifEmpty { null },
            responsable = responsable?.trim()?.ifEmpty { null },
            budget = budget,
            dateDebut = maintenant,
            echeance = echeance,
        )
        val reference = sequenceManager.next(DocType.FACTURE)
        val id = dao.insert(
            OperationRecordEntity(
                module = OperationModule.PROJETS.name,
                reference = reference,
                title = libelle,
                counterpart = clientNom?.trim()?.ifEmpty { null },
                tiersId = clientId.takeIf { it > 0 },
                amount = budget,
                status = OperationStatus.VALIDATED.name,
                notes = ProjetCodec.encode(payload),
                createdAt = maintenant,
            ),
        )
        journalManager.log(MODULE, "PROJET_CREE", "$reference — $libelle")
        return Resultat.Succes(id, reference)
    }

    /**
     * Met à jour l'avancement et le consommé — les deux valeurs qui font la
     * dérive. Elles se saisissent ensemble : mesurer l'une sans l'autre ne dit
     * rien de la santé du projet.
     */
    suspend fun actualiser(id: Long, avancement: Int, consomme: Double): Resultat {
        if (!consomme.isFinite() || consomme < 0.0) return Resultat.Invalide
        if (licenceManager.isReadOnly()) return Resultat.LectureSeule
        val piece = dao.getById(id) ?: return Resultat.Introuvable
        val payload = ProjetCodec.decode(piece.notes) ?: return Resultat.Introuvable
        val actualise = payload.copy(
            avancement = ProjetRules.avancementValide(avancement),
            consomme = consomme,
        )
        dao.update(piece.copy(notes = ProjetCodec.encode(actualise)))
        journalManager.log(MODULE, "PROJET_ACTUALISE", "${piece.reference} — $avancement %")
        return Resultat.Succes(id, piece.reference)
    }

    /** Change l'état du projet (préparation, en cours, livré, suspendu). */
    suspend fun changerEtat(id: Long, etat: EtatProjet): Resultat {
        if (licenceManager.isReadOnly()) return Resultat.LectureSeule
        val piece = dao.getById(id) ?: return Resultat.Introuvable
        val payload = ProjetCodec.decode(piece.notes) ?: return Resultat.Introuvable
        // Un projet livré est réputé achevé : son avancement passe à 100 %,
        // sans quoi les moyennes resteraient faussées par des projets terminés
        // affichant 80 %.
        val suivant = payload.copy(
            etat = etat.name,
            avancement = if (etat == EtatProjet.LIVRE) 100 else payload.avancement,
        )
        dao.update(piece.copy(notes = ProjetCodec.encode(suivant)))
        journalManager.log(MODULE, "PROJET_${etat.name}", piece.reference)
        return Resultat.Succes(id, piece.reference)
    }

    suspend fun annuler(id: Long): Resultat {
        if (licenceManager.isReadOnly()) return Resultat.LectureSeule
        val piece = dao.getById(id) ?: return Resultat.Introuvable
        dao.update(piece.copy(status = OperationStatus.CANCELLED.name))
        journalManager.log(MODULE, "PROJET_ANNULE", piece.reference)
        return Resultat.Succes(id, piece.reference)
    }
}
