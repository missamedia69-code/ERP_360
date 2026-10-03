package com.missa.b360.core.domain.model

import com.missa.b360.core.data.entity.FournisseurBalanceEntity
import com.missa.b360.core.data.entity.FournisseurCompteBancaireEntity
import com.missa.b360.core.data.entity.FournisseurDocumentEntity
import com.missa.b360.core.data.entity.FournisseurEntity
import com.missa.b360.core.data.entity.FournisseurScoreEntity
import com.missa.b360.core.data.entity.FournisseurStatus
import java.util.Locale

/** Un fournisseur du portefeuille avec ce qu'il faut pour décider : aptitude, dette, fiabilité. */
data class FournisseurLigne(
    val fournisseur: FournisseurEntity,
    val aptitude: SupplierReadiness,
    val balance: FournisseurBalanceEntity?,
    val score: FournisseurScoreEntity?,
) {
    val dette: Double get() = balance?.dette ?: 0.0
    val enRetard: Double get() = balance?.enRetard ?: 0.0
    val joursRetardMax: Int get() = balance?.joursRetardMax ?: 0
    val note: Int? get() = score?.score
}

enum class FournisseurFiltre { TOUS, A_PAYER, EN_RETARD, A_REGULARISER, BLOQUES, ARCHIVES }

enum class FournisseurTri { NOM, DETTE, RETARD, SCORE }

fun FournisseurDocumentEntity.versInfo(): SupplierDocumentInfo =
    SupplierDocumentInfo(dateExpiration = dateExpiration, verification = verification, archive = archive)

/** Construit, filtre et trie la liste des fournisseurs — sans accès aux données (testable). */
object FournisseurPortefeuilleRules {
    private const val EPS = 0.005

    fun lignes(
        fournisseurs: List<FournisseurEntity>,
        avecContactPrincipal: Set<Long>,
        comptes: List<FournisseurCompteBancaireEntity>,
        documents: List<FournisseurDocumentEntity>,
        balances: List<FournisseurBalanceEntity>,
        scores: List<FournisseurScoreEntity>,
        now: Long,
    ): List<FournisseurLigne> {
        val comptesParFournisseur = comptes.groupBy { it.fournisseurId }
        val documentsParFournisseur = documents.groupBy { it.fournisseurId }
        val balanceParFournisseur = balances.associateBy { it.fournisseurId }
        val scoreParFournisseur = scores.associateBy { it.fournisseurId }
        return fournisseurs.map { f ->
            FournisseurLigne(
                fournisseur = f,
                aptitude = SupplierReadinessRules.evaluer(
                    f = f,
                    contactPrincipalPresent = f.id in avecContactPrincipal,
                    documents = documentsParFournisseur[f.id].orEmpty().map { it.versInfo() },
                    comptes = comptesParFournisseur[f.id].orEmpty().map { it.versInfo() },
                    now = now,
                ),
                balance = balanceParFournisseur[f.id],
                score = scoreParFournisseur[f.id],
            )
        }
    }

    fun correspond(f: FournisseurEntity, requete: String): Boolean {
        val terme = requete.trim()
        if (terme.isEmpty()) return true
        return listOfNotNull(
            f.nom, f.nomCommercial, f.code, f.telephone, f.telephone2, f.email, f.adresse,
            f.identifiantFiscal, f.typeIdentifiantFiscal, f.rccm, f.numTva, f.categoriesFournies,
        ).any { it.contains(terme, ignoreCase = true) }
    }

    fun correspondFiltre(l: FournisseurLigne, filtre: FournisseurFiltre): Boolean {
        val statut = l.fournisseur.statut
        return when (filtre) {
            FournisseurFiltre.TOUS -> statut != FournisseurStatus.ARCHIVE
            FournisseurFiltre.A_PAYER -> statut != FournisseurStatus.ARCHIVE && l.dette > EPS
            FournisseurFiltre.EN_RETARD -> statut != FournisseurStatus.ARCHIVE && l.enRetard > EPS
            FournisseurFiltre.A_REGULARISER ->
                statut != FournisseurStatus.ARCHIVE && l.aptitude.niveau == SupplierReadinessLevel.A_REGULARISER
            FournisseurFiltre.BLOQUES -> statut == FournisseurStatus.BLOQUE
            FournisseurFiltre.ARCHIVES -> statut == FournisseurStatus.ARCHIVE
        }
    }

    fun compter(lignes: List<FournisseurLigne>, filtre: FournisseurFiltre): Int =
        lignes.count { correspondFiltre(it, filtre) }

    fun filtrer(lignes: List<FournisseurLigne>, filtre: FournisseurFiltre, requete: String): List<FournisseurLigne> =
        lignes.filter { correspondFiltre(it, filtre) && correspond(it.fournisseur, requete) }

    fun trier(lignes: List<FournisseurLigne>, tri: FournisseurTri): List<FournisseurLigne> {
        val parNom = compareBy<FournisseurLigne> { it.fournisseur.nom.lowercase(Locale.ROOT) }
        return when (tri) {
            FournisseurTri.NOM -> lignes.sortedWith(parNom)
            FournisseurTri.DETTE -> lignes.sortedWith(compareByDescending<FournisseurLigne> { it.dette }.then(parNom))
            FournisseurTri.RETARD -> lignes.sortedWith(compareByDescending<FournisseurLigne> { it.joursRetardMax }.then(parNom))
            FournisseurTri.SCORE -> lignes.sortedWith(
                compareByDescending<FournisseurLigne> { it.note != null }.thenByDescending { it.note ?: 0 }.then(parNom),
            )
        }
    }
}
