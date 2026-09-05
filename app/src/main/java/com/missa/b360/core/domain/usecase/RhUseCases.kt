package com.missa.b360.core.domain.usecase

import com.missa.b360.core.data.dao.AbsenceDao
import com.missa.b360.core.data.dao.EmployeeDao
import com.missa.b360.core.data.dao.OperationRecordDao
import com.missa.b360.core.data.db.AppDatabase
import com.missa.b360.core.data.entity.AbsenceEntity
import com.missa.b360.core.data.entity.EmployeeEntity
import com.missa.b360.core.data.entity.OperationDirection
import com.missa.b360.core.data.entity.OperationModule
import com.missa.b360.core.data.entity.OperationRecordEntity
import com.missa.b360.core.data.entity.OperationStatus
import com.missa.b360.core.domain.model.AdvanceCodec
import com.missa.b360.core.domain.model.AdvancePayload
import com.missa.b360.core.domain.model.PaieRules
import com.missa.b360.core.domain.model.PayslipCodec
import com.missa.b360.core.domain.model.PayslipLine
import com.missa.b360.core.domain.model.PayslipPayload
import com.missa.b360.core.journal.JournalManager
import com.missa.b360.core.licensing.LicenceManager
import com.missa.b360.core.numbering.DocType
import com.missa.b360.core.numbering.SequenceManager
import java.util.Calendar
import javax.inject.Inject

/**
 * Employé (spec §RH) — création/édition. Code atomique `EMP2026-0001`.
 * Désactivation uniquement — jamais de DELETE (C7).
 */
class SaveEmployeeUseCase @Inject constructor(
    private val employeeDao: EmployeeDao,
    private val sequenceManager: SequenceManager,
    private val licenceManager: LicenceManager,
    private val journalManager: JournalManager,
) {
    sealed class Result {
        data class Succes(val employeeId: Long, val code: String) : Result()
        data object LectureSeule : Result()
        data object DonneesInvalides : Result()
        data object EmployeIntrouvable : Result()
    }

    suspend operator fun invoke(
        recordId: Long?,
        nom: String,
        telephone: String,
        poste: String?,
        salaireBase: Double,
        joursMensuels: Double,
        notes: String?,
        now: Long = System.currentTimeMillis(),
    ): Result {
        if (licenceManager.isReadOnly()) return Result.LectureSeule
        if (nom.isBlank() || telephone.isBlank()) return Result.DonneesInvalides
        if (salaireBase < 0.0 || joursMensuels !in 1.0..31.0) return Result.DonneesInvalides

        if (recordId == null) {
            val code = sequenceManager.next(DocType.EMPLOYE)
            val id = employeeDao.insert(
                EmployeeEntity(
                    code = code,
                    nom = nom.trim(),
                    telephone = telephone.trim(),
                    poste = poste?.trim()?.ifBlank { null },
                    salaireBase = salaireBase,
                    joursMensuels = joursMensuels,
                    notes = notes?.trim()?.ifBlank { null },
                    createdAt = now,
                ),
            )
            journalManager.log(OperationModule.RH.name, "EMPLOYE_CREE", "$code — ${nom.trim()}")
            return Result.Succes(id, code)
        }

        val existant = employeeDao.getById(recordId) ?: return Result.EmployeIntrouvable
        employeeDao.update(
            existant.copy(
                nom = nom.trim(),
                telephone = telephone.trim(),
                poste = poste?.trim()?.ifBlank { null },
                salaireBase = salaireBase,
                joursMensuels = joursMensuels,
                notes = notes?.trim()?.ifBlank { null },
            ),
        )
        journalManager.log(OperationModule.RH.name, "EMPLOYE_MODIFIE", "${existant.code} mis à jour")
        return Result.Succes(recordId, existant.code)
    }
}

class DesactivateEmployeeUseCase @Inject constructor(
    private val employeeDao: EmployeeDao,
    private val licenceManager: LicenceManager,
    private val journalManager: JournalManager,
) {
    sealed class Result {
        data object Succes : Result()
        data object LectureSeule : Result()
        data object Introuvable : Result()
    }

    suspend operator fun invoke(employeeId: Long): Result {
        if (licenceManager.isReadOnly()) return Result.LectureSeule
        val employee = employeeDao.getById(employeeId) ?: return Result.Introuvable
        employeeDao.desactiver(employeeId)
        journalManager.log(OperationModule.RH.name, "EMPLOYE_DESACTIVE", employee.code)
        return Result.Succes
    }
}

/** Absence (spec §Paie) — déduite du bulletin par prorata journalier. */
class SaveAbsenceUseCase @Inject constructor(
    private val absenceDao: AbsenceDao,
    private val employeeDao: EmployeeDao,
    private val licenceManager: LicenceManager,
    private val journalManager: JournalManager,
) {
    sealed class Result {
        data object Succes : Result()
        data object LectureSeule : Result()
        data object DonneesInvalides : Result()
        data object EmployeIntrouvable : Result()
    }

    suspend operator fun invoke(
        employeeId: Long,
        type: String,
        dateDebut: Long,
        dureeJours: Double,
        motif: String?,
        now: Long = System.currentTimeMillis(),
    ): Result {
        if (licenceManager.isReadOnly()) return Result.LectureSeule
        if (dureeJours <= 0.0 || !dureeJours.isFinite()) return Result.DonneesInvalides
        val employee = employeeDao.getById(employeeId) ?: return Result.EmployeIntrouvable
        absenceDao.insert(
            AbsenceEntity(
                employeeId = employeeId,
                type = type,
                dateDebut = dateDebut,
                dureeJours = dureeJours,
                motif = motif?.trim()?.ifBlank { null },
                createdAt = now,
            ),
        )
        journalManager.log(OperationModule.RH.name, "ABSENCE_ENREGISTREE", "${employee.code} — $type, $dureeJours j")
        return Result.Succes
    }
}

class DeleteAbsenceUseCase @Inject constructor(
    private val absenceDao: AbsenceDao,
    private val licenceManager: LicenceManager,
    private val journalManager: JournalManager,
) {
    sealed class Result {
        data object Succes : Result()
        data object LectureSeule : Result()
    }

    /** Correction de saisie — l'absence est une donnée, pas une pièce financière. */
    suspend operator fun invoke(absenceId: Long): Result {
        if (licenceManager.isReadOnly()) return Result.LectureSeule
        absenceDao.delete(absenceId)
        journalManager.log(OperationModule.RH.name, "ABSENCE_SUPPRIMEE", "id $absenceId")
        return Result.Succes
    }
}

/**
 * Avance de salaire (doc AV) — pièce RH validée ; déduite du bulletin du
 * mois de création. Aucune écriture de caisse (le versement est Finance).
 */
class SaveAdvanceUseCase @Inject constructor(
    private val operationDao: OperationRecordDao,
    private val employeeDao: EmployeeDao,
    private val database: AppDatabase,
    private val sequenceManager: SequenceManager,
    private val licenceManager: LicenceManager,
    private val journalManager: JournalManager,
) {
    sealed class Result {
        data class Succes(val recordId: Long, val reference: String) : Result()
        data object LectureSeule : Result()
        data object DonneesInvalides : Result()
        data object EmployeIntrouvable : Result()
    }

    suspend operator fun invoke(
        employeeId: Long,
        montant: Double,
        motif: String?,
        now: Long = System.currentTimeMillis(),
    ): Result {
        if (licenceManager.isReadOnly()) return Result.LectureSeule
        if (montant <= 0.0 || !montant.isFinite()) return Result.DonneesInvalides
        val employee = employeeDao.getById(employeeId) ?: return Result.EmployeIntrouvable

        return database.withTransaction {
            val reference = sequenceManager.next(DocType.AVANCE_SALAIRE)
            val id = operationDao.insert(
                OperationRecordEntity(
                    module = OperationModule.RH.name,
                    reference = reference,
                    title = "Avance $reference — ${employee.nom}",
                    counterpart = employee.nom,
                    amount = montant,
                    direction = OperationDirection.NONE.name,
                    status = OperationStatus.VALIDATED.name,
                    notes = AdvanceCodec.encode(
                        AdvancePayload(employeeId = employeeId, employeeNom = employee.nom, motif = motif),
                    ),
                    createdAt = now,
                ),
            )
            journalManager.log(
                OperationModule.RH.name,
                "AVANCE_SALAIRE",
                "$reference — ${employee.nom} ($montant)",
            )
            Result.Succes(id, reference)
        }
    }
}

/**
 * Génération du bulletin de paie du mois (doc P) — **transactionnelle** :
 * un seul bulletin par mois (interdiction de double paie), lignes par employé
 * actif (base − absences au prorata − avances du mois), journalisée.
 */
class CreatePayslipUseCase @Inject constructor(
    private val employeeDao: EmployeeDao,
    private val absenceDao: AbsenceDao,
    private val operationDao: OperationRecordDao,
    private val database: AppDatabase,
    private val sequenceManager: SequenceManager,
    private val licenceManager: LicenceManager,
    private val journalManager: JournalManager,
) {
    sealed class Result {
        data class Succes(val recordId: Long, val reference: String, val employes: Int) : Result()
        data object LectureSeule : Result()
        data object DonneesInvalides : Result()
        data object AucunEmploye : Result()
        data object DejaExistante : Result()
    }

    /** mois/année 1-based. */
    suspend operator fun invoke(mois: Int, annee: Int, now: Long = System.currentTimeMillis()): Result {
        if (licenceManager.isReadOnly()) return Result.LectureSeule
        if (mois !in 1..12 || annee !in 2000..2100) return Result.DonneesInvalides
        val employes = employeeDao.listActifs()
        if (employes.isEmpty()) return Result.AucunEmploye

        return database.withTransaction {
            // Interdiction de double bulletin pour le même mois.
            val existants = operationDao.getByModule(OperationModule.RH.name)
                .mapNotNull { PayslipCodec.decode(it.notes) }
            if (existants.any { it.mois == mois && it.annee == annee }) {
                return@withTransaction Result.DejaExistante
            }

            // Avances du mois (pièces AV validées, mois de création).
            val cal = Calendar.getInstance().apply {
                clear()
                set(annee, mois - 1, 1)
            }
            val debutMois = cal.timeInMillis
            cal.add(Calendar.MONTH, 1)
            val finMois = cal.timeInMillis
            val avances = operationDao.getByModule(OperationModule.RH.name)
                .filter { it.status == OperationStatus.VALIDATED.name && it.createdAt in debutMois until finMois }
                .mapNotNull { record ->
                    AdvanceCodec.decode(record.notes)?.let { it.employeeId to (record.amount ?: 0.0) }
                }
                .groupBy({ it.first }, { it.second })
                .mapValues { (_, montants) -> montants.sum() }

            val lignes = mutableListOf<PayslipLine>()
            for (employee in employes) {
                val absences = absenceDao.byEmployee(employee.id)
                val joursAbs = PaieRules.absencesDuMois(absences, mois, annee)
                val jour = PaieRules.salaireJour(employee.salaireBase, employee.joursMensuels)
                val avancement = avances[employee.id] ?: 0.0
                val net = PaieRules.net(employee.salaireBase, 0.0, 0.0, jour * joursAbs, avancement)
                lignes += PayslipLine(
                    employeeId = employee.id,
                    nom = employee.nom,
                    code = employee.code,
                    base = employee.salaireBase,
                    absencesJours = joursAbs,
                    absencesMontant = jour * joursAbs,
                    avancement = avancement,
                    net = net,
                )
            }
            val totalNet = lignes.sumOf { it.net }
            val reference = sequenceManager.next(DocType.BULLETIN_PAIE)
            val id = operationDao.insert(
                OperationRecordEntity(
                    module = OperationModule.RH.name,
                    reference = reference,
                    title = "Paie $annee-$mois",
                    counterpart = "RH",
                    amount = totalNet,
                    direction = OperationDirection.NONE.name,
                    status = OperationStatus.VALIDATED.name,
                    notes = PayslipCodec.encode(
                        PayslipPayload(mois = mois, annee = annee, lignes = lignes, totalNet = totalNet),
                    ),
                    createdAt = now,
                ),
            )
            journalManager.log(
                OperationModule.RH.name,
                "PAIE_GENEREE",
                "$reference — ${lignes.size} employé(s), net total $totalNet",
            )
            Result.Succes(id, reference, lignes.size)
        }
    }
}
