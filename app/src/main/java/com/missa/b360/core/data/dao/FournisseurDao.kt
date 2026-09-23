package com.missa.b360.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.missa.b360.core.data.entity.FournisseurCompteBancaireEntity
import com.missa.b360.core.data.entity.FournisseurContactEntity
import com.missa.b360.core.data.entity.FournisseurDocumentEntity
import com.missa.b360.core.data.entity.FournisseurEntity
import com.missa.b360.core.data.entity.FournisseurEvenementEntity
import com.missa.b360.core.data.entity.FournisseurItemEntity
import com.missa.b360.core.data.entity.FournisseurStatus
import com.missa.b360.core.data.entity.VerificationStatut
import kotlinx.coroutines.flow.Flow

/**
 * DAO Fournisseurs — fiche maître du cycle d'achat.
 * Convention C7 : aucune suppression physique — archivage uniquement.
 */
@Dao
interface FournisseurDao {
    /** Fournisseurs utilisables dans le cycle d'achat (commandes, factures). */
    @Query("SELECT * FROM fournisseurs WHERE statut = 'ACTIF' ORDER BY nom")
    fun observeAll(): Flow<List<FournisseurEntity>>

    /** Tous les statuts, pour le hub et la liste filtrable. */
    @Query("SELECT * FROM fournisseurs ORDER BY nom")
    fun observeTous(): Flow<List<FournisseurEntity>>

    @Query("SELECT * FROM fournisseurs WHERE statut = :statut ORDER BY nom")
    fun observeParStatut(statut: FournisseurStatus): Flow<List<FournisseurEntity>>

    @Query(
        "SELECT * FROM fournisseurs WHERE statut = :statut AND " +
            "(LOWER(nom) LIKE '%' || LOWER(:query) || '%' OR LOWER(code) LIKE '%' || LOWER(:query) || '%' " +
            "OR telephone LIKE '%' || :query || '%') ORDER BY nom",
    )
    fun rechercheParStatut(statut: FournisseurStatus, query: String): Flow<List<FournisseurEntity>>

    @Query("SELECT COUNT(*) FROM fournisseurs WHERE statut = :statut")
    fun observeCountParStatut(statut: FournisseurStatus): Flow<Int>

    @Query("SELECT * FROM fournisseurs WHERE id = :id")
    suspend fun getById(id: Long): FournisseurEntity?

    @Query("SELECT * FROM fournisseurs WHERE code = :code LIMIT 1")
    suspend fun getByCode(code: String): FournisseurEntity?

    /**
     * Anti-doublon étendu (spec §1) : raison sociale, identifiant fiscal, RCCM,
     * téléphone, e-mail, pays + raison sociale.
     */
    @Query(
        "SELECT * FROM fournisseurs WHERE statut != 'ARCHIVE' AND id != :saufId AND (" +
            "LOWER(TRIM(nom)) = LOWER(TRIM(:nom)) " +
            "OR (:telephone != '' AND telephone = :telephone) " +
            "OR (:email != '' AND email IS NOT NULL AND LOWER(email) = LOWER(:email)) " +
            "OR (:identifiantFiscal != '' AND identifiantFiscal IS NOT NULL AND identifiantFiscal = :identifiantFiscal) " +
            "OR (:rccm != '' AND rccm IS NOT NULL AND rccm = :rccm) " +
            "OR (LOWER(TRIM(nom)) = LOWER(TRIM(:nom)) AND pays = :pays)" +
            ")",
    )
    suspend fun findDoublonsPotentiels(
        nom: String,
        telephone: String,
        email: String,
        identifiantFiscal: String,
        rccm: String,
        pays: String,
        saufId: Long = 0,
    ): List<FournisseurEntity>

    @Insert
    suspend fun insert(fournisseur: FournisseurEntity): Long

    @Update
    suspend fun update(fournisseur: FournisseurEntity)

    /** Archivage (jamais de DELETE — C7). */
    @Query("UPDATE fournisseurs SET statut = 'ARCHIVE' WHERE id = :id")
    suspend fun archiver(id: Long)

    /** Compatibilité : désactiver = archiver. */
    @Query("UPDATE fournisseurs SET statut = 'ARCHIVE' WHERE id = :id")
    suspend fun desactiver(id: Long)

    @Query("SELECT COUNT(*) FROM fournisseurs")
    suspend fun count(): Int
}

/** Contacts fournisseur — plusieurs rôles par contact, désactivation sans suppression. */
@Dao
interface FournisseurContactDao {
    @Query("SELECT * FROM fournisseur_contacts WHERE fournisseurId = :fournisseurId AND actif = 1 ORDER BY principal DESC, nom")
    fun observeParFournisseur(fournisseurId: Long): Flow<List<FournisseurContactEntity>>

    @Insert
    suspend fun insert(contact: FournisseurContactEntity): Long

    @Update
    suspend fun update(contact: FournisseurContactEntity)

    @Query("UPDATE fournisseur_contacts SET principal = 0 WHERE fournisseurId = :fournisseurId")
    suspend fun retirerRôlePrincipal(fournisseurId: Long)

    @Query("SELECT COUNT(*) FROM fournisseur_contacts WHERE fournisseurId = :fournisseurId AND actif = 1")
    suspend fun compterActifs(fournisseurId: Long): Int
}

/** Comptes bancaires / Mobile Money — compte principal unique, vérification tracée. */
@Dao
interface FournisseurCompteBancaireDao {
    @Query("SELECT * FROM fournisseur_comptes_bancaires WHERE fournisseurId = :fournisseurId ORDER BY principal DESC, id")
    fun observeParFournisseur(fournisseurId: Long): Flow<List<FournisseurCompteBancaireEntity>>

    @Query("SELECT COUNT(*) FROM fournisseur_comptes_bancaires WHERE verification = 'A_VERIFIER'")
    fun observeComptesAVerifier(): Flow<Int>

    @Insert
    suspend fun insert(compte: FournisseurCompteBancaireEntity): Long

    @Update
    suspend fun update(compte: FournisseurCompteBancaireEntity)

    @Query("UPDATE fournisseur_comptes_bancaires SET principal = 0 WHERE fournisseurId = :fournisseurId")
    suspend fun retirerComptePrincipal(fournisseurId: Long)

    @Query("UPDATE fournisseur_comptes_bancaires SET verification = :statut, verifieLe = :date WHERE id = :id")
    suspend fun majVerification(id: Long, statut: VerificationStatut, date: Long)
}

/** Documents de conformité — échéances suivies pour les alertes d'expiration. */
@Dao
interface FournisseurDocumentDao {
    @Query("SELECT * FROM fournisseur_documents WHERE fournisseurId = :fournisseurId ORDER BY dateExpiration IS NULL, dateExpiration")
    fun observeParFournisseur(fournisseurId: Long): Flow<List<FournisseurDocumentEntity>>

    /** Documents expirant avant :horizon (ms) ou déjà expirés — pour le hub « À traiter ». */
    @Query(
        "SELECT d.* FROM fournisseur_documents d INNER JOIN fournisseurs f ON f.id = d.fournisseurId " +
            "WHERE d.dateExpiration IS NOT NULL AND d.dateExpiration <= :horizon AND f.statut NOT IN ('ARCHIVE', 'BLOQUE') " +
            "ORDER BY d.dateExpiration",
    )
    fun observeExpirants(horizon: Long): Flow<List<FournisseurDocumentEntity>>

    @Insert
    suspend fun insert(document: FournisseurDocumentEntity): Long

    @Update
    suspend fun update(document: FournisseurDocumentEntity)

    @Query("SELECT * FROM fournisseur_documents WHERE id = :id")
    suspend fun getById(id: Long): FournisseurDocumentEntity?

    /** Seule exception à C7 : un document joint peut être retiré — l'audit garde la trace. */
    @Query("DELETE FROM fournisseur_documents WHERE id = :id")
    suspend fun deleteById(id: Long)
}

/** Liaison fournisseur ↔ article : prix, délai, quantité minimum, préféré. */
@Dao
interface FournisseurItemDao {
    @Query(
        "SELECT * FROM fournisseur_items WHERE fournisseurId = :fournisseurId AND actif = 1 ORDER BY reference",
    )
    fun observeParFournisseur(fournisseurId: Long): Flow<List<FournisseurItemEntity>>

    @Query("SELECT * FROM fournisseur_items WHERE fournisseurId = :fournisseurId AND productId = :productId AND actif = 1 LIMIT 1")
    suspend fun getLiaison(fournisseurId: Long, productId: Long): FournisseurItemEntity?

    /** Toutes les liaisons actives d'un fournisseur, indexables par produit côté UI. */
    @Query("SELECT * FROM fournisseur_items WHERE fournisseurId = :fournisseurId AND actif = 1")
    suspend fun listeParFournisseur(fournisseurId: Long): List<FournisseurItemEntity>

    @Query("SELECT * FROM fournisseur_items WHERE productId = :productId AND actif = 1 AND prefere = 1 LIMIT 1")
    suspend fun getPreferePourProduit(productId: Long): FournisseurItemEntity?

    @Insert
    suspend fun insert(item: FournisseurItemEntity): Long

    @Update
    suspend fun update(item: FournisseurItemEntity)

    @Query("UPDATE fournisseur_items SET prefere = 0 WHERE productId = :productId AND fournisseurId != :saufFournisseurId")
    suspend fun retirerPreferenceProduit(productId: Long, saufFournisseurId: Long)

    @Query("UPDATE fournisseur_items SET actif = 0 WHERE id = :id")
    suspend fun desactiver(id: Long)
}

/** Journal d'audit fournisseur — append-only. */
@Dao
interface FournisseurEvenementDao {
    @Query("SELECT * FROM fournisseur_evenements WHERE fournisseurId = :fournisseurId ORDER BY date DESC LIMIT :limite")
    fun observeParFournisseur(fournisseurId: Long, limite: Int = 50): Flow<List<FournisseurEvenementEntity>>

    @Insert
    suspend fun insert(evenement: FournisseurEvenementEntity): Long
}
