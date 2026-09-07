package com.missa.b360.core.domain.usecase

import com.missa.b360.core.data.dao.CompteTresorerieDao
import com.missa.b360.core.data.dao.MouvementTresorerieDao
import com.missa.b360.core.data.entity.CategorieTresorerie
import com.missa.b360.core.data.entity.CompteTresorerieEntity
import com.missa.b360.core.data.entity.MouvementTresorerieEntity
import com.missa.b360.core.data.entity.SensMouvement
import com.missa.b360.core.data.entity.TypeCompteTresorerie
import com.missa.b360.core.data.db.AppDatabase
import com.missa.b360.core.domain.model.TresorerieRules
import com.missa.b360.core.journal.JournalManager
import com.missa.b360.core.licensing.LicenceManager
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject

/**
 * Module Trésorerie (TRE) — comptes, mouvements, virements internes et
 * rapprochement.
 *
 * Toute écriture passe par le garde de licence (RA-05 : lecture seule à
 * l'expiration) et laisse une trace au journal d'audit, comme les autres
 * modules métier.
 */
class TresorerieUseCases @Inject constructor(
    private val database: AppDatabase,
    private val comptesDao: CompteTresorerieDao,
    private val mouvementsDao: MouvementTresorerieDao,
    private val licenceManager: LicenceManager,
    private val journalManager: JournalManager,
) {
    companion object {
        const val MODULE = "TRESORERIE"
    }

    sealed class Resultat {
        data class Succes(val id: Long) : Resultat()
        /** Licence expirée : consultation et export seulement. */
        data object LectureSeule : Resultat()
        /** Saisie refusée (montant, libellé, compte fermé…). */
        data object Invalide : Resultat()
        /** Un compte porte déjà ce nom. */
        data object NomDejaPris : Resultat()
        /** Virement : compte source et destination identiques ou introuvables. */
        data object ComptesIncoherents : Resultat()
    }

    fun observerComptes(): Flow<List<CompteTresorerieEntity>> = comptesDao.observeAll()

    fun observerMouvements(): Flow<List<MouvementTresorerieEntity>> = mouvementsDao.observeAll()

    fun observerMouvements(compteId: Long): Flow<List<MouvementTresorerieEntity>> =
        mouvementsDao.observeByCompte(compteId)

    suspend fun creerCompte(
        nom: String,
        type: TypeCompteTresorerie,
        etablissement: String? = null,
        numero: String? = null,
        soldeInitial: Double = 0.0,
        maintenant: Long = System.currentTimeMillis(),
    ): Resultat {
        val nomNettoye = nom.trim()
        if (!TresorerieRules.libelleValide(nomNettoye)) return Resultat.Invalide
        if (!soldeInitial.isFinite() || soldeInitial < 0.0) return Resultat.Invalide
        if (licenceManager.isReadOnly()) return Resultat.LectureSeule
        if (comptesDao.compterHomonymes(nomNettoye) > 0) return Resultat.NomDejaPris

        val id = comptesDao.insert(
            CompteTresorerieEntity(
                nom = nomNettoye,
                type = type.name,
                etablissement = etablissement?.trim()?.ifEmpty { null },
                numero = numero?.trim()?.ifEmpty { null },
                soldeInitial = soldeInitial,
                createdAt = maintenant,
            ),
        )
        journalManager.log(MODULE, "COMPTE_CREE", "$nomNettoye (${type.name})")
        return Resultat.Succes(id)
    }

    /** Ferme ou rouvre un compte ; les mouvements passés restent consultables. */
    suspend fun basculerActivite(compteId: Long): Resultat {
        if (licenceManager.isReadOnly()) return Resultat.LectureSeule
        val compte = comptesDao.getById(compteId) ?: return Resultat.Invalide
        comptesDao.update(compte.copy(actif = !compte.actif))
        journalManager.log(
            MODULE,
            if (compte.actif) "COMPTE_FERME" else "COMPTE_ROUVERT",
            compte.nom,
        )
        return Resultat.Succes(compteId)
    }

    data class MouvementParams(
        val compteId: Long,
        val sens: SensMouvement,
        val montant: Double,
        val libelle: String,
        val categorie: CategorieTresorerie = CategorieTresorerie.AUTRE,
        val tiers: String? = null,
        val modePaiement: String? = null,
        val reference: String? = null,
        val date: Long = System.currentTimeMillis(),
        val notes: String? = null,
    )

    suspend fun enregistrerMouvement(
        params: MouvementParams,
        maintenant: Long = System.currentTimeMillis(),
    ): Resultat {
        val libelle = params.libelle.trim()
        if (!TresorerieRules.libelleValide(libelle)) return Resultat.Invalide
        if (!params.montant.isFinite() ||
            params.montant <= 0.0 ||
            params.montant > TresorerieRules.MONTANT_MAX
        ) {
            return Resultat.Invalide
        }
        if (licenceManager.isReadOnly()) return Resultat.LectureSeule
        val compte = comptesDao.getById(params.compteId) ?: return Resultat.Invalide
        // Un compte fermé n'accepte plus rien : sinon son solde de clôture bougerait.
        if (!compte.actif) return Resultat.Invalide

        val id = mouvementsDao.insert(
            MouvementTresorerieEntity(
                compteId = params.compteId,
                date = params.date,
                sens = params.sens.name,
                montant = params.montant,
                categorie = params.categorie.name,
                libelle = libelle,
                tiers = params.tiers?.trim()?.ifEmpty { null },
                modePaiement = params.modePaiement?.trim()?.ifEmpty { null },
                reference = params.reference?.trim()?.ifEmpty { null },
                notes = params.notes?.trim()?.ifEmpty { null },
                createdAt = maintenant,
            ),
        )
        journalManager.log(
            MODULE,
            if (params.sens == SensMouvement.IN) "ENCAISSEMENT" else "DECAISSEMENT",
            "$libelle — ${params.montant} (${compte.nom})",
        )
        return Resultat.Succes(id)
    }

    /**
     * Virement interne : une sortie sur la source et une entrée sur la
     * destination, écrites dans la même transaction et reliées par un
     * identifiant commun. Sans transaction, une coupure entre les deux
     * insertions ferait disparaître de l'argent.
     */
    suspend fun virementInterne(
        sourceId: Long,
        destinationId: Long,
        montant: Double,
        libelle: String,
        date: Long = System.currentTimeMillis(),
        maintenant: Long = System.currentTimeMillis(),
    ): Resultat {
        if (sourceId == destinationId) return Resultat.ComptesIncoherents
        val texte = libelle.trim()
        if (!TresorerieRules.libelleValide(texte)) return Resultat.Invalide
        if (!montant.isFinite() || montant <= 0.0 || montant > TresorerieRules.MONTANT_MAX) {
            return Resultat.Invalide
        }
        if (licenceManager.isReadOnly()) return Resultat.LectureSeule
        val source = comptesDao.getById(sourceId) ?: return Resultat.ComptesIncoherents
        val destination = comptesDao.getById(destinationId) ?: return Resultat.ComptesIncoherents
        if (!source.actif || !destination.actif) return Resultat.Invalide

        val transfert = "${TresorerieRules.PREFIXE_TRANSFERT}-${UUID.randomUUID()}"
        val idSortie = database.withTransaction {
            val sortie = mouvementsDao.insert(
                MouvementTresorerieEntity(
                    compteId = sourceId,
                    date = date,
                    sens = SensMouvement.OUT.name,
                    montant = montant,
                    categorie = CategorieTresorerie.TRANSFERT.name,
                    libelle = texte,
                    tiers = destination.nom,
                    transfertId = transfert,
                    createdAt = maintenant,
                ),
            )
            mouvementsDao.insert(
                MouvementTresorerieEntity(
                    compteId = destinationId,
                    date = date,
                    sens = SensMouvement.IN.name,
                    montant = montant,
                    categorie = CategorieTresorerie.TRANSFERT.name,
                    libelle = texte,
                    tiers = source.nom,
                    transfertId = transfert,
                    createdAt = maintenant,
                ),
            )
            sortie
        }
        journalManager.log(
            MODULE,
            "VIREMENT",
            "${source.nom} → ${destination.nom} : $montant",
        )
        return Resultat.Succes(idSortie)
    }

    /** Pointage d'un mouvement contre le relevé bancaire. */
    suspend fun basculerRapprochement(mouvementId: Long): Resultat {
        if (licenceManager.isReadOnly()) return Resultat.LectureSeule
        val mouvement = mouvementsDao.getById(mouvementId) ?: return Resultat.Invalide
        mouvementsDao.marquerRapproche(mouvementId, !mouvement.rapproche)
        return Resultat.Succes(mouvementId)
    }
}
