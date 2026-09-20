package com.missa.b360.core.domain.usecase

import com.missa.b360.core.data.dao.FournisseurCompteBancaireDao
import com.missa.b360.core.data.dao.FournisseurContactDao
import com.missa.b360.core.data.dao.FournisseurDao
import com.missa.b360.core.data.dao.FournisseurDocumentDao
import com.missa.b360.core.data.dao.FournisseurEvenementDao
import com.missa.b360.core.data.dao.FournisseurItemDao
import com.missa.b360.core.data.entity.FournisseurCompteBancaireEntity
import com.missa.b360.core.data.entity.FournisseurDocType
import com.missa.b360.core.data.entity.FournisseurDocumentEntity
import com.missa.b360.core.data.entity.FournisseurEntity
import com.missa.b360.core.data.entity.FournisseurEvenementEntity
import com.missa.b360.core.data.entity.FournisseurEvenementType
import com.missa.b360.core.data.entity.FournisseurItemEntity
import com.missa.b360.core.data.entity.FournisseurStatus
import com.missa.b360.core.data.entity.VerificationStatut
import com.missa.b360.core.domain.model.FournisseurRules
import com.missa.b360.core.journal.JournalManager
import com.missa.b360.core.licensing.LicenceManager
import com.missa.b360.core.notifications.AppNotifier
import com.missa.b360.core.numbering.DocType
import com.missa.b360.core.numbering.SequenceManager
import com.missa.b360.core.util.PieceJointeAchat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Anti-doublon étendu (spec Fournisseurs §1) : raison sociale, identifiant fiscal,
 * RCCM, téléphone, e-mail, pays + raison sociale. Retourne `ficheId → motifs`.
 */
class DetectDuplicateFournisseurUseCase @Inject constructor(
    private val fournisseurDao: FournisseurDao,
) {
    suspend operator fun invoke(candidat: FournisseurEntity): Map<Long, List<String>> {
        val existants = fournisseurDao.findDoublonsPotentiels(
            nom = candidat.nom,
            telephone = candidat.telephone,
            email = candidat.email.orEmpty(),
            identifiantFiscal = candidat.identifiantFiscal.orEmpty(),
            rccm = candidat.rccm.orEmpty(),
            pays = candidat.pays,
            saufId = candidat.id,
        )
        return FournisseurRules.motifsDoublon(candidat, existants)
    }
}

/**
 * Création d'un fournisseur — code `FRN-2026-0001` via SequenceManager, statut
 * initial BROUILLON (spec §11), anti-doublon extensible, journal d'audit.
 */
class CreateFournisseurUseCase @Inject constructor(
    private val fournisseurDao: FournisseurDao,
    private val evenementDao: FournisseurEvenementDao,
    private val sequenceManager: SequenceManager,
    private val licenceManager: LicenceManager,
    private val journalManager: JournalManager,
) {
    sealed class Result {
        data class Succes(val fournisseurId: Long, val code: String) : Result()
        data object LicenceExpiree : Result()
        data class DoublonPotentiel(val fiches: Map<Long, List<String>>) : Result()
        data class ChampsManquants(val champs: List<String>) : Result()
    }

    suspend operator fun invoke(
        brouillon: FournisseurEntity,
        doublonConfirme: Boolean = false,
        now: Long = System.currentTimeMillis(),
    ): Result {
        if (licenceManager.isReadOnly()) return Result.LicenceExpiree
        val manquants = FournisseurRules.manquantsPourSoumission(brouillon, contactPrincipalPresent = true)
            .filter { it == "raison_sociale" || it == "contact_tel_ou_email" }
        if (manquants.isNotEmpty()) return Result.ChampsManquants(manquants)

        if (!doublonConfirme) {
            val doublons = DetectDuplicateFournisseurUseCase(fournisseurDao).invoke(brouillon)
            if (doublons.isNotEmpty()) return Result.DoublonPotentiel(doublons)
        }

        val code = sequenceManager.next(DocType.FOURNISSEUR)
        val id = fournisseurDao.insert(
            brouillon.copy(
                code = code,
                statut = FournisseurStatus.BROUILLON,
                createdAt = now,
                updatedAt = now,
            ),
        )
        evenementDao.insert(
            FournisseurEvenementEntity(
                fournisseurId = id,
                date = now,
                type = FournisseurEvenementType.CREATION,
                details = "Fiche $code créée (brouillon)",
            ),
        )
        journalManager.log("FOURNISSEURS", "CREATION_FOURNISSEUR", "Fournisseur $code — ${brouillon.nom}")
        return Result.Succes(id, code)
    }
}

/**
 * Mise à jour complète de la fiche. Toute modification sensible sur un fournisseur
 * ACTIF (fiscal, raison sociale, pays, conditions de paiement) le renvoie en
 * A_VALIDER : réapprobation obligatoire (spec §3.2).
 */
class UpdateFournisseurUseCase @Inject constructor(
    private val fournisseurDao: FournisseurDao,
    private val evenementDao: FournisseurEvenementDao,
    private val licenceManager: LicenceManager,
    private val journalManager: JournalManager,
    private val notifier: AppNotifier,
) {
    suspend operator fun invoke(modifie: FournisseurEntity, now: Long = System.currentTimeMillis()): Boolean {
        if (licenceManager.isReadOnly()) return false
        val existant = fournisseurDao.getById(modifie.id) ?: return false
        val reapprobation = FournisseurRules.reapprobationRequise(existant, modifie)
        val aEnregistrer = modifie.copy(
            statut = if (reapprobation) FournisseurStatus.A_VALIDER else modifie.statut,
            soumisLe = if (reapprobation) now else modifie.soumisLe,
            updatedAt = now,
        )
        fournisseurDao.update(aEnregistrer)
        evenementDao.insert(
            FournisseurEvenementEntity(
                fournisseurId = modifie.id,
                date = now,
                type = if (reapprobation) {
                    FournisseurEvenementType.REAPPROBATION_REQUISE
                } else {
                    FournisseurEvenementType.MISE_A_JOUR
                },
                details = if (reapprobation) "Modification sensible — réapprobation requise" else "Fiche mise à jour",
            ),
        )
        if (reapprobation) {
            notifier.notifier(
                "FOURNISSEUR",
                "Réapprobation requise",
                "${existant.code} — ${existant.nom} : modification sensible, validation nécessaire.",
            )
        }
        journalManager.log("FOURNISSEURS", "MODIFICATION_FOURNISSEUR", "Fournisseur ${existant.code} modifié")
        return true
    }
}

/** Soumission à validation : dossier complet obligatoire (spec §11). */
class SoumettreFournisseurUseCase @Inject constructor(
    private val fournisseurDao: FournisseurDao,
    private val contactDao: FournisseurContactDao,
    private val evenementDao: FournisseurEvenementDao,
    private val licenceManager: LicenceManager,
    private val notifier: AppNotifier,
) {
    sealed class Result {
        data object Succes : Result()
        data class ChampsManquants(val champs: List<String>) : Result()
        data object TransitionRefusee : Result()
    }

    suspend operator fun invoke(id: Long, now: Long = System.currentTimeMillis()): Result {
        if (licenceManager.isReadOnly()) return Result.TransitionRefusee
        val fournisseur = fournisseurDao.getById(id) ?: return Result.TransitionRefusee
        if (!FournisseurRules.transitionAutorisee(fournisseur.statut, FournisseurStatus.A_VALIDER)) {
            return Result.TransitionRefusee
        }
        val contactPrincipal = contactDao.compterActifs(id) > 0
        val manquants = FournisseurRules.manquantsPourSoumission(fournisseur, contactPrincipal)
        if (manquants.isNotEmpty()) return Result.ChampsManquants(manquants)
        fournisseurDao.update(fournisseur.copy(statut = FournisseurStatus.A_VALIDER, soumisLe = now, updatedAt = now))
        evenementDao.insert(
            FournisseurEvenementEntity(id, now, FournisseurEvenementType.SOUMISSION, "Dossier soumis à validation"),
        )
        notifier.notifier("FOURNISSEUR", "Fournisseur à valider", "${fournisseur.code} — ${fournisseur.nom}")
        return Result.Succes
    }
}

/**
 * Changement de statut du cycle de vie : approbation, suspension, blocage (motif
 * obligatoire), réactivation, archivage. Chaque transition est auditée.
 */
class ChangerStatutFournisseurUseCase @Inject constructor(
    private val fournisseurDao: FournisseurDao,
    private val evenementDao: FournisseurEvenementDao,
    private val licenceManager: LicenceManager,
    private val journalManager: JournalManager,
    private val notifier: AppNotifier,
) {
    sealed class Result {
        data object Succes : Result()
        data object TransitionRefusee : Result()
        data object MotifObligatoire : Result()
        data object Introuvable : Result()
    }

    suspend operator fun invoke(
        id: Long,
        vers: FournisseurStatus,
        motif: String? = null,
        now: Long = System.currentTimeMillis(),
    ): Result {
        if (licenceManager.isReadOnly()) return Result.TransitionRefusee
        val fournisseur = fournisseurDao.getById(id) ?: return Result.Introuvable
        if (!FournisseurRules.transitionAutorisee(fournisseur.statut, vers)) return Result.TransitionRefusee
        if (vers == FournisseurStatus.BLOQUE && !FournisseurRules.blocageValide(motif)) {
            return Result.MotifObligatoire
        }
        fournisseurDao.update(
            fournisseur.copy(
                statut = vers,
                motifBlocage = if (vers == FournisseurStatus.BLOQUE) motif?.trim() else null,
                approuve = if (vers == FournisseurStatus.ACTIF) true else fournisseur.approuve,
                approuveLe = if (vers == FournisseurStatus.ACTIF) now else fournisseur.approuveLe,
                updatedAt = now,
            ),
        )
        val type = when (vers) {
            FournisseurStatus.ACTIF -> FournisseurEvenementType.APPROBATION
            FournisseurStatus.SUSPENDU -> FournisseurEvenementType.SUSPENSION
            FournisseurStatus.BLOQUE -> FournisseurEvenementType.BLOCAGE
            FournisseurStatus.ARCHIVE -> FournisseurEvenementType.ARCHIVAGE
            else -> FournisseurEvenementType.REACTIVATION
        }
        evenementDao.insert(
            FournisseurEvenementEntity(
                fournisseurId = id,
                date = now,
                type = type,
                details = buildString {
                    append("Statut → ${vers.name}")
                    if (!motif.isNullOrBlank()) append(" — $motif")
                },
            ),
        )
        when (vers) {
            FournisseurStatus.ACTIF -> notifier.notifier(
                "FOURNISSEUR",
                "Fournisseur approuvé",
                "${fournisseur.code} — ${fournisseur.nom} est actif : commandes autorisées.",
            )
            FournisseurStatus.BLOQUE -> notifier.notifier(
                "FOURNISSEUR",
                "Fournisseur bloqué",
                "${fournisseur.code} — ${fournisseur.nom} : ${motif?.trim()}",
            )
            else -> Unit
        }
        journalManager.log("FOURNISSEURS", "STATUT_FOURNISSEUR", "${fournisseur.code} → ${vers.name}")
        return Result.Succes
    }
}

/** Ajout d'un compte bancaire / Mobile Money : principal unique, à vérifier. */
class AjouterCompteBancaireUseCase @Inject constructor(
    private val compteDao: FournisseurCompteBancaireDao,
    private val evenementDao: FournisseurEvenementDao,
    private val licenceManager: LicenceManager,
    private val notifier: AppNotifier,
) {
    suspend operator fun invoke(
        compte: FournisseurCompteBancaireEntity,
        now: Long = System.currentTimeMillis(),
    ): Long? {
        if (licenceManager.isReadOnly()) return null
        if (compte.titulaire.isBlank()) return null
        val numeroRenseigne = !compte.numeroCompte.isNullOrBlank() ||
            !compte.iban.isNullOrBlank() ||
            !compte.numeroMobile.isNullOrBlank()
        if (!numeroRenseigne) return null
        if (compte.principal) compteDao.retirerComptePrincipal(compte.fournisseurId)
        val id = compteDao.insert(
            compte.copy(verification = VerificationStatut.A_VERIFIER, verifieLe = null),
        )
        evenementDao.insert(
            FournisseurEvenementEntity(
                fournisseurId = compte.fournisseurId,
                date = now,
                type = FournisseurEvenementType.COMPTE_AJOUTE,
                details = "Compte ${compte.banque ?: compte.operateurMobile ?: "bancaire"} ajouté — à vérifier",
            ),
        )
        notifier.notifier(
            "FOURNISSEUR",
            "RIB à vérifier",
            "Nouveau compte de paiement en attente de vérification.",
        )
        return id
    }
}

/** Vérification (ou rejet) d'un compte de paiement — tracée dans l'audit. */
class VerifierCompteBancaireUseCase @Inject constructor(
    private val compteDao: FournisseurCompteBancaireDao,
    private val evenementDao: FournisseurEvenementDao,
    private val licenceManager: LicenceManager,
) {
    suspend operator fun invoke(
        compteId: Long,
        fournisseurId: Long,
        approuve: Boolean,
        now: Long = System.currentTimeMillis(),
    ): Boolean {
        if (licenceManager.isReadOnly()) return false
        compteDao.majVerification(
            id = compteId,
            statut = if (approuve) VerificationStatut.VERIFIE else VerificationStatut.REJETE,
            date = now,
        )
        evenementDao.insert(
            FournisseurEvenementEntity(
                fournisseurId = fournisseurId,
                date = now,
                type = if (approuve) {
                    FournisseurEvenementType.COMPTE_VERIFIE
                } else {
                    FournisseurEvenementType.COMPTE_REJETE
                },
                details = if (approuve) "Compte de paiement vérifié" else "Compte de paiement rejeté",
            ),
        )
        return true
    }
}

/** Ajout d'un document de conformité (fichier déjà copié via PieceJointeAchat). */
class AjouterDocumentFournisseurUseCase @Inject constructor(
    private val documentDao: FournisseurDocumentDao,
    private val evenementDao: FournisseurEvenementDao,
    private val licenceManager: LicenceManager,
) {
    suspend operator fun invoke(
        fournisseurId: Long,
        typeDocument: FournisseurDocType,
        reference: String?,
        cheminFichier: String?,
        dateEmission: Long?,
        dateExpiration: Long?,
        now: Long = System.currentTimeMillis(),
    ): Long? {
        if (licenceManager.isReadOnly()) return null
        val id = documentDao.insert(
            FournisseurDocumentEntity(
                fournisseurId = fournisseurId,
                typeDocument = typeDocument,
                reference = reference?.trim()?.ifBlank { null },
                cheminFichier = cheminFichier,
                dateEmission = dateEmission,
                dateExpiration = dateExpiration,
            ),
        )
        evenementDao.insert(
            FournisseurEvenementEntity(
                fournisseurId = fournisseurId,
                date = now,
                type = FournisseurEvenementType.DOCUMENT_AJOUTE,
                details = "Document ${typeDocument.name} ajouté",
            ),
        )
        return id
    }
}

/** Retrait d'un document (seule suppression physique — l'audit conserve la trace). */
class SupprimerDocumentFournisseurUseCase @Inject constructor(
    private val documentDao: FournisseurDocumentDao,
    private val evenementDao: FournisseurEvenementDao,
    private val licenceManager: LicenceManager,
) {
    suspend operator fun invoke(documentId: Long, now: Long = System.currentTimeMillis()): Boolean {
        if (licenceManager.isReadOnly()) return false
        val document = documentDao.getById(documentId) ?: return false
        document.cheminFichier?.let(PieceJointeAchat::supprimer)
        documentDao.deleteById(documentId)
        evenementDao.insert(
            FournisseurEvenementEntity(
                fournisseurId = document.fournisseurId,
                date = now,
                type = FournisseurEvenementType.DOCUMENT_SUPPRIME,
                details = "Document ${document.typeDocument.name} retiré",
            ),
        )
        return true
    }
}

/** Liaison fournisseur ↔ article : prix, délai, quantité minimum, préféré. */
class LierArticleFournisseurUseCase @Inject constructor(
    private val itemDao: FournisseurItemDao,
    private val evenementDao: FournisseurEvenementDao,
    private val licenceManager: LicenceManager,
) {
    suspend operator fun invoke(liaison: FournisseurItemEntity, now: Long = System.currentTimeMillis()): Long? {
        if (licenceManager.isReadOnly()) return null
        if (liaison.prixUnitaire < 0.0) return null
        val existante = itemDao.getLiaison(liaison.fournisseurId, liaison.productId)
        val id = if (existante == null) {
            itemDao.insert(liaison)
        } else {
            itemDao.update(existante.copy(
                reference = liaison.reference,
                prixUnitaire = liaison.prixUnitaire,
                delaiJours = liaison.delaiJours,
                quantiteMin = liaison.quantiteMin,
                prefere = liaison.prefere,
                debutValidite = liaison.debutValidite,
                finValidite = liaison.finValidite,
                actif = true,
            ))
            existante.id
        }
        if (liaison.prefere) itemDao.retirerPreferenceProduit(liaison.productId, liaison.fournisseurId)
        evenementDao.insert(
            FournisseurEvenementEntity(
                fournisseurId = liaison.fournisseurId,
                date = now,
                type = FournisseurEvenementType.ARTICLE_LIE,
                details = "Article #${liaison.productId} lié (prix ${liaison.prixUnitaire}, délai ${liaison.delaiJours} j)",
            ),
        )
        return id
    }
}

/** Dé-liaison d'un article (désactivation — jamais de DELETE). */
class DelierArticleFournisseurUseCase @Inject constructor(
    private val itemDao: FournisseurItemDao,
    private val evenementDao: FournisseurEvenementDao,
    private val licenceManager: LicenceManager,
) {
    suspend operator fun invoke(liaisonId: Long, fournisseurId: Long, now: Long = System.currentTimeMillis()): Boolean {
        if (licenceManager.isReadOnly()) return false
        itemDao.desactiver(liaisonId)
        evenementDao.insert(
            FournisseurEvenementEntity(
                fournisseurId = fournisseurId,
                date = now,
                type = FournisseurEvenementType.ARTICLE_DELIE,
                details = "Liaison article #$liaisonId retirée",
            ),
        )
        return true
    }
}

/** Évaluation manuelle : note /5 + commentaire (score simple). */
class EvaluerFournisseurUseCase @Inject constructor(
    private val fournisseurDao: FournisseurDao,
    private val evenementDao: FournisseurEvenementDao,
    private val licenceManager: LicenceManager,
) {
    suspend operator fun invoke(
        id: Long,
        note: Double,
        commentaire: String?,
        now: Long = System.currentTimeMillis(),
    ): Boolean {
        if (licenceManager.isReadOnly()) return false
        if (note < 0.0 || note > 5.0) return false
        val fournisseur = fournisseurDao.getById(id) ?: return false
        fournisseurDao.update(
            fournisseur.copy(
                noteEvaluation = note,
                commentaireEvaluation = commentaire?.trim()?.ifBlank { null },
                dateEvaluation = now,
                updatedAt = now,
            ),
        )
        evenementDao.insert(
            FournisseurEvenementEntity(
                fournisseurId = id,
                date = now,
                type = FournisseurEvenementType.EVALUATION,
                details = "Note : ${"%.1f".format(note)} / 5",
            ),
        )
        return true
    }
}

/**
 * Scan des documents expirants (J-90 / J-30 / J-7 / expiré) — alimente le hub
 * « À traiter » et les notifications.
 */
class ScannerDocumentsExpirantsUseCase @Inject constructor(
    private val documentDao: FournisseurDocumentDao,
) {
    suspend fun documentsExpirants(
        horizon: Long = System.currentTimeMillis() + 90L * 24 * 60 * 60 * 1000,
    ): List<FournisseurDocumentEntity> = documentDao.observeExpirants(horizon).first()
}

/** Désactivation = archivage (C7 — jamais de DELETE). */
class DesactiverFournisseurUseCase @Inject constructor(
    private val fournisseurDao: FournisseurDao,
    private val evenementDao: FournisseurEvenementDao,
    private val licenceManager: LicenceManager,
    private val journalManager: JournalManager,
) {
    suspend operator fun invoke(id: Long, now: Long = System.currentTimeMillis()): Boolean {
        if (licenceManager.isReadOnly()) return false
        val fournisseur = fournisseurDao.getById(id) ?: return false
        fournisseurDao.desactiver(id)
        evenementDao.insert(
            FournisseurEvenementEntity(
                fournisseurId = id,
                date = now,
                type = FournisseurEvenementType.ARCHIVAGE,
                details = "Fournisseur archivé",
            ),
        )
        journalManager.log("FOURNISSEURS", "DESACTIVATION_FOURNISSEUR", "Fournisseur ${fournisseur.code} archivé")
        return true
    }
}

/** Lecture de la liste des fournisseurs actifs (cycle d'achat). */
class ObserveFournisseursUseCase @Inject constructor(
    private val fournisseurDao: FournisseurDao,
) {
    operator fun invoke(): Flow<List<FournisseurEntity>> = fournisseurDao.observeAll()
}
