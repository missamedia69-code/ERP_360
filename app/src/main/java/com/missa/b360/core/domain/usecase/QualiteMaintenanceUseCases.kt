package com.missa.b360.core.domain.usecase

import com.missa.b360.core.data.dao.EquipementDao
import com.missa.b360.core.data.dao.InterventionDao
import com.missa.b360.core.data.dao.NonConformiteDao
import com.missa.b360.core.data.entity.EquipementEntity
import com.missa.b360.core.data.entity.GraviteNc
import com.missa.b360.core.data.entity.InterventionEntity
import com.missa.b360.core.data.entity.NonConformiteEntity
import com.missa.b360.core.data.entity.OrigineNc
import com.missa.b360.core.data.entity.StatutIntervention
import com.missa.b360.core.data.entity.StatutNc
import com.missa.b360.core.data.entity.TypeEquipement
import com.missa.b360.core.data.entity.TypeIntervention
import com.missa.b360.core.domain.model.TresorerieRules
import com.missa.b360.core.journal.JournalManager
import com.missa.b360.core.licensing.LicenceManager
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Résultat commun aux écritures Qualité et Maintenance. */
sealed class ResultatSaisie {
    data class Succes(val id: Long) : ResultatSaisie()
    data object LectureSeule : ResultatSaisie()
    data object Invalide : ResultatSaisie()
    data object NomDejaPris : ResultatSaisie()
}

/**
 * Module Qualité (QUA) — registre des non-conformités et suivi de leur
 * traitement. Une non-conformité ne se supprime pas : elle se clôture, avec sa
 * date, pour que le délai de résolution reste mesurable.
 */
class QualiteUseCases @Inject constructor(
    private val dao: NonConformiteDao,
    private val licenceManager: LicenceManager,
    private val journalManager: JournalManager,
) {
    companion object { const val MODULE = "QUALITE" }

    fun observer(): Flow<List<NonConformiteEntity>> = dao.observeAll()

    suspend fun declarer(
        titre: String,
        gravite: GraviteNc,
        origine: OrigineNc,
        description: String? = null,
        reference: String? = null,
        responsable: String? = null,
        cout: Double = 0.0,
        date: Long = System.currentTimeMillis(),
    ): ResultatSaisie {
        val libelle = titre.trim()
        if (!TresorerieRules.libelleValide(libelle)) return ResultatSaisie.Invalide
        if (!cout.isFinite() || cout < 0.0) return ResultatSaisie.Invalide
        if (licenceManager.isReadOnly()) return ResultatSaisie.LectureSeule
        val id = dao.insert(
            NonConformiteEntity(
                date = date,
                titre = libelle,
                description = description?.trim()?.ifEmpty { null },
                gravite = gravite.name,
                origine = origine.name,
                reference = reference?.trim()?.ifEmpty { null },
                responsable = responsable?.trim()?.ifEmpty { null },
                cout = cout,
                createdAt = System.currentTimeMillis(),
            ),
        )
        journalManager.log(MODULE, "NC_DECLAREE", "$libelle (${gravite.name})")
        return ResultatSaisie.Succes(id)
    }

    /** Avance la non-conformité d'un cran : ouverte → en cours → résolue. */
    suspend fun avancer(
        id: Long,
        actionCorrective: String? = null,
        maintenant: Long = System.currentTimeMillis(),
    ): ResultatSaisie {
        if (licenceManager.isReadOnly()) return ResultatSaisie.LectureSeule
        val nc = dao.getById(id) ?: return ResultatSaisie.Invalide
        val suivant = when (nc.statut) {
            StatutNc.OUVERTE.name -> StatutNc.EN_COURS
            StatutNc.EN_COURS.name -> StatutNc.RESOLUE
            else -> return ResultatSaisie.Invalide
        }
        dao.update(
            nc.copy(
                statut = suivant.name,
                actionCorrective = actionCorrective?.trim()?.ifEmpty { null }
                    ?: nc.actionCorrective,
                // La date de résolution n'est posée qu'à la clôture : c'est elle
                // qui donne le délai de traitement.
                dateResolution = if (suivant == StatutNc.RESOLUE) maintenant else null,
            ),
        )
        journalManager.log(MODULE, "NC_${suivant.name}", nc.titre)
        return ResultatSaisie.Succes(id)
    }
}

/**
 * Module Maintenance (MAI) — parc d'équipements et interventions préventives
 * ou correctives.
 */
class MaintenanceUseCases @Inject constructor(
    private val equipementDao: EquipementDao,
    private val interventionDao: InterventionDao,
    private val licenceManager: LicenceManager,
    private val journalManager: JournalManager,
) {
    companion object { const val MODULE = "MAINTENANCE" }

    fun observerEquipements(): Flow<List<EquipementEntity>> = equipementDao.observeAll()

    fun observerInterventions(): Flow<List<InterventionEntity>> = interventionDao.observeAll()

    suspend fun ajouterEquipement(
        nom: String,
        type: TypeEquipement,
        code: String? = null,
        periodiciteJours: Int = 0,
        dateMiseEnService: Long? = null,
    ): ResultatSaisie {
        val libelle = nom.trim()
        if (!TresorerieRules.libelleValide(libelle)) return ResultatSaisie.Invalide
        if (periodiciteJours < 0 || periodiciteJours > MAX_PERIODICITE_JOURS) {
            return ResultatSaisie.Invalide
        }
        if (licenceManager.isReadOnly()) return ResultatSaisie.LectureSeule
        if (equipementDao.compterHomonymes(libelle) > 0) return ResultatSaisie.NomDejaPris
        val id = equipementDao.insert(
            EquipementEntity(
                nom = libelle,
                code = code?.trim()?.ifEmpty { null },
                type = type.name,
                periodiciteJours = periodiciteJours,
                dateMiseEnService = dateMiseEnService,
                createdAt = System.currentTimeMillis(),
            ),
        )
        journalManager.log(MODULE, "EQUIPEMENT_AJOUTE", libelle)
        return ResultatSaisie.Succes(id)
    }

    suspend fun enregistrerIntervention(
        equipementId: Long,
        type: TypeIntervention,
        description: String,
        cout: Double = 0.0,
        dureeHeures: Double = 0.0,
        technicien: String? = null,
        statut: StatutIntervention = StatutIntervention.REALISEE,
        date: Long = System.currentTimeMillis(),
    ): ResultatSaisie {
        val libelle = description.trim()
        if (!TresorerieRules.libelleValide(libelle)) return ResultatSaisie.Invalide
        if (!cout.isFinite() || cout < 0.0) return ResultatSaisie.Invalide
        if (!dureeHeures.isFinite() || dureeHeures < 0.0) return ResultatSaisie.Invalide
        if (licenceManager.isReadOnly()) return ResultatSaisie.LectureSeule
        val equipement = equipementDao.getById(equipementId) ?: return ResultatSaisie.Invalide
        val id = interventionDao.insert(
            InterventionEntity(
                equipementId = equipementId,
                date = date,
                type = type.name,
                description = libelle,
                technicien = technicien?.trim()?.ifEmpty { null },
                cout = cout,
                dureeHeures = dureeHeures,
                statut = statut.name,
                createdAt = System.currentTimeMillis(),
            ),
        )
        journalManager.log(MODULE, "INTERVENTION_${type.name}", "${equipement.nom} — $libelle")
        return ResultatSaisie.Succes(id)
    }

    private companion object {
        /** Dix ans : au-delà, la périodicité saisie est une erreur de frappe. */
        const val MAX_PERIODICITE_JOURS = 3_650
    }
}
