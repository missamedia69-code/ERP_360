package com.missa.b360.core.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Non-conformité qualité : un écart constaté, sa gravité et son traitement.
 *
 * Rien n'est supprimé — une non-conformité se clôt, elle ne s'efface pas. Le
 * registre des écarts est précisément ce qu'un audit vient consulter.
 */
@Entity(
    tableName = "qualite_non_conformites",
    indices = [Index(value = ["statut"]), Index(value = ["date"])],
)
data class NonConformiteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,
    val titre: String,
    val description: String? = null,
    /** Nom stable de [GraviteNc]. */
    val gravite: String = GraviteNc.MINEURE.name,
    /** Nom stable de [OrigineNc]. */
    val origine: String = OrigineNc.INTERNE.name,
    /** Nom stable de [StatutNc]. */
    val statut: String = StatutNc.OUVERTE.name,
    /** Pièce concernée : lot de production, réception, vente… */
    val reference: String? = null,
    val responsable: String? = null,
    val actionCorrective: String? = null,
    /** Coût constaté de l'écart (rebut, retour, retouche). */
    val cout: Double = 0.0,
    val dateResolution: Long? = null,
    val createdAt: Long,
)

enum class GraviteNc { MINEURE, MAJEURE, CRITIQUE }

enum class OrigineNc { PRODUCTION, RECEPTION, CLIENT, INTERNE }

enum class StatutNc { OUVERTE, EN_COURS, RESOLUE }

/**
 * Équipement suivi en maintenance : machine, véhicule, installation.
 *
 * La périodicité en jours porte le plan préventif ; à zéro, l'équipement n'est
 * suivi qu'en curatif.
 */
@Entity(
    tableName = "maintenance_equipements",
    indices = [Index(value = ["nom"], unique = true)],
)
data class EquipementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nom: String,
    val code: String? = null,
    /** Nom stable de [TypeEquipement]. */
    val type: String = TypeEquipement.MACHINE.name,
    val siteId: Long? = null,
    val dateMiseEnService: Long? = null,
    /** Intervalle du plan préventif, en jours ; 0 = pas de préventif. */
    val periodiciteJours: Int = 0,
    val actif: Boolean = true,
    val notes: String? = null,
    val createdAt: Long,
)

enum class TypeEquipement { MACHINE, VEHICULE, INSTALLATION, OUTILLAGE, INFORMATIQUE }

/** Intervention réalisée ou planifiée sur un équipement. */
@Entity(
    tableName = "maintenance_interventions",
    indices = [Index(value = ["equipementId"]), Index(value = ["date"])],
)
data class InterventionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val equipementId: Long,
    val date: Long,
    /** Nom stable de [TypeIntervention]. */
    val type: String = TypeIntervention.PREVENTIVE.name,
    val description: String,
    val technicien: String? = null,
    val cout: Double = 0.0,
    /** Durée d'immobilisation, en heures. */
    val dureeHeures: Double = 0.0,
    /** Nom stable de [StatutIntervention]. */
    val statut: String = StatutIntervention.REALISEE.name,
    val createdAt: Long,
)

enum class TypeIntervention { PREVENTIVE, CORRECTIVE }

enum class StatutIntervention { PLANIFIEE, REALISEE, ANNULEE }
