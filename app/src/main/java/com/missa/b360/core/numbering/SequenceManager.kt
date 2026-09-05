package com.missa.b360.core.numbering

import androidx.room.withTransaction
import com.missa.b360.core.data.dao.SequenceDao
import com.missa.b360.core.data.db.AppDatabase
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Types de documents numérotés (RA-09 / RM-40) :
 * **9 documents de base + 9 séquences dédiées**, lettre + année + séquence (F2026-0001).
 */
enum class DocType(val prefix: String) {
    // 9 documents de base
    DEVIS("D"),
    COMMANDE_CLIENT("C"),
    FACTURE("F"),
    TICKET("T"),
    AVOIR("A"),
    BON_COMMANDE("B"),
    BON_RECEPTION("R"),
    LIVRAISON("L"),
    BULLETIN_PAIE("P"),

    // 9 séquences dédiées
    CLIENT("CLI"),
    TRANSFERT("TRF"),
    INVENTAIRE("INV"),
    FACTURE_FOURNISSEUR("FFR"),
    RETOUR_FOURNISSEUR("RRT"),
    ORDRE_PRODUCTION("OP"),
    DEVIS_PRESTATION("DP"),
    ORDRE_SERVICE("OS"),
    AVANCE_SALAIRE("AV"),
    PROJET("PRJ"),

    // Séquence dédiée fournisseur (module 9.3)
    FOURNISSEUR("FRN"),

    // Séquence dédiée produit (module Stock, spec §7)
    PRODUIT("PRD"),

    // RH — employés, rappels de paiement (spec §RH/§22)
    EMPLOYE("EMP"),
    RAPPEL("RPP"),
}

/**
 * SequenceManager — numérotation **atomique** (RA-09 / RM-40).
 *
 * L'incrément se fait dans une transaction Room : aucun numéro réutilisé,
 * même en cas d'annulation de la pièce (C7 — compensation, pas de DELETE).
 */
@Singleton
class SequenceManager @Inject constructor(
    private val database: AppDatabase,
    private val sequenceDao: SequenceDao,
) {
    /**
     * Réserve et retourne le prochain numéro pour [docType].
     * À appeler **dans la même transaction** que la création du document si possible,
     * sinon immédiatement avant l'insertion (le numéro n'est jamais réutilisé).
     */
    suspend fun next(docType: DocType, annee: Int = anneeCourante()): String {
        val compteur = database.withTransaction {
            sequenceDao.ensureRow(docType.name, annee)
            sequenceDao.increment(docType.name, annee)
            sequenceDao.compteurApresIncrement(docType.name, annee) ?: 1L
        }
        return format(docType.prefix, annee, compteur)
    }

    companion object {
        fun anneeCourante(): Int = Calendar.getInstance().get(Calendar.YEAR)

        /** Format Lettre + Année + Séquence : F2026-0001. */
        fun format(prefix: String, annee: Int, compteur: Long): String =
            "$prefix$annee-${compteur.toString().padStart(4, '0')}"
    }
}
