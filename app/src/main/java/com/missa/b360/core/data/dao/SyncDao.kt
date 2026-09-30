package com.missa.b360.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.missa.b360.core.data.entity.SyncConflictEntity
import com.missa.b360.core.data.entity.SyncDeviceEntity
import com.missa.b360.core.data.entity.SyncInboxEntity
import com.missa.b360.core.data.entity.SyncOutboxEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncDao {
    @Insert suspend fun insererAppareil(appareil: SyncDeviceEntity)
    @Query("SELECT * FROM sync_devices WHERE deviceId = :id LIMIT 1") suspend fun appareil(id: String): SyncDeviceEntity?
    @Query("SELECT * FROM sync_devices WHERE actif = 1 ORDER BY nom") fun observerAppareils(): Flow<List<SyncDeviceEntity>>

    @Insert suspend fun ajouterSortant(evenement: SyncOutboxEntity)
    @Update suspend fun modifierSortant(evenement: SyncOutboxEntity)
    @Query("SELECT * FROM sync_outbox WHERE statut IN ('EN_ATTENTE','ECHEC') AND prochaineTentative <= :maintenant ORDER BY creeLe LIMIT :limite")
    suspend fun lotAEnvoyer(maintenant: Long, limite: Int): List<SyncOutboxEntity>
    @Query("SELECT * FROM sync_outbox WHERE eventId = :id LIMIT 1") suspend fun sortant(id: String): SyncOutboxEntity?
    @Query("SELECT COUNT(*) FROM sync_outbox WHERE statut IN ('EN_ATTENTE','EN_COURS','ECHEC')") fun observerNombreEnAttente(): Flow<Int>
    @Query("UPDATE sync_outbox SET statut = 'LIVRE', derniereErreur = NULL WHERE eventId = :id") suspend fun marquerLivre(id: String)

    @Query("SELECT EXISTS(SELECT 1 FROM sync_inbox WHERE eventId = :id)") suspend fun dejaRecu(id: String): Boolean
    @Insert suspend fun ajouterRecu(evenement: SyncInboxEntity)
    @Query("UPDATE sync_inbox SET appliqueLe = :date WHERE eventId = :id") suspend fun marquerApplique(id: String, date: Long)

    @Insert suspend fun ajouterConflit(conflit: SyncConflictEntity)
    @Query("SELECT * FROM sync_conflicts WHERE resolu = 0 ORDER BY detecteLe DESC") fun observerConflits(): Flow<List<SyncConflictEntity>>
    @Query("UPDATE sync_conflicts SET resolu = 1, resolution = :resolution, resoluLe = :date WHERE conflictId = :id AND resolu = 0")
    suspend fun resoudreConflit(id: String, resolution: String, date: Long): Int
}
