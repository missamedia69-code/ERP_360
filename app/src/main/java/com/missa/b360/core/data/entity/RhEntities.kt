package com.missa.b360.core.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class EmployeeStatus { ACTIF, DESACTIVE }

/**
 * Employé (spec §RH) — base de la paie (P), des absences et des avances (AV).
 * Désactivation (jamais de DELETE — C7).
 */
@Entity(tableName = "employees")
data class EmployeeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val code: String,
    val nom: String,
    val telephone: String,
    val poste: String? = null,
    /** Salaire de base mensuel, devise de l'entreprise. */
    val salaireBase: Double = 0.0,
    /** Jours travaillés par mois — sert au prorata des absences. */
    val joursMensuels: Double = 26.0,
    val statut: String = EmployeeStatus.ACTIF.name,
    val notes: String? = null,
    val createdAt: Long,
)

enum class AbsenceType { MALADIE, CONGE, AUTRE }

/** Absence d'un employé — déduite de la paie par prorata journalier (spec §Paie). */
@Entity(tableName = "absences")
data class AbsenceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val employeeId: Long,
    /** [AbsenceType] en texte. */
    val type: String,
    val dateDebut: Long,
    val dureeJours: Double,
    val motif: String? = null,
    val createdAt: Long,
)

enum class TaskStatus { A_FAIRE, EN_COURS, FAITE }

/** Tâche de suivi (spec §Tâches) — jamais de suppression : on passe en FAITE. */
@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val titre: String,
    val notes: String? = null,
    /** [TaskStatus] en texte. */
    val statut: String = TaskStatus.A_FAIRE.name,
    val echeance: Long? = null,
    val createdAt: Long,
)
