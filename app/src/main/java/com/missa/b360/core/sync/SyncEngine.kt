package com.missa.b360.core.sync

import android.os.Build
import androidx.room.withTransaction
import com.missa.b360.core.data.dao.SyncDao
import com.missa.b360.core.data.datastore.SettingsStore
import com.missa.b360.core.data.db.AppDatabase
import com.missa.b360.core.data.entity.SyncConflictEntity
import com.missa.b360.core.data.entity.SyncDeviceEntity
import com.missa.b360.core.data.entity.SyncInboxEntity
import com.missa.b360.core.data.entity.SyncOperation
import com.missa.b360.core.data.entity.SyncOutboxEntity
import com.missa.b360.core.data.entity.SyncStatut
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

/** Règles déterministes et testables du futur transport de synchronisation. */
object SyncPolicy {
    const val LOT_MAX = 100
    const val DELAI_MAX_MS = 6 * 60 * 60 * 1_000L

    /** 5 s, 10 s, 20 s… plafonné à 6 h, avec valeur sûre pour tout compteur. */
    fun delaiNouvelEssai(tentatives: Int): Long {
        val exposant = tentatives.coerceIn(0, 20)
        return min(5_000L * (1L shl exposant), DELAI_MAX_MS)
    }

    fun conflit(revisionLocale: Long, revisionDistante: Long, modificationsLocales: Boolean): Boolean =
        modificationsLocales && revisionDistante >= revisionLocale
}

/**
 * Moteur local-first, volontairement indépendant de tout fournisseur réseau.
 * Le futur client HTTP ne fera que prendre [lotAExpedier], envoyer des enveloppes
 * chiffrées, puis appeler [confirmerLivraison] ou [signalerEchec].
 */
@Singleton
class SyncEngine @Inject constructor(
    private val dao: SyncDao,
    private val database: AppDatabase,
    private val settings: SettingsStore,
) {
    val nombreEnAttente: Flow<Int> = dao.observerNombreEnAttente()
    val conflits: Flow<List<SyncConflictEntity>> = dao.observerConflits()

    suspend fun appareilLocal(): SyncDeviceEntity {
        val existant = settings.get(SettingsStore.Keys.SYNC_DEVICE_ID)
        val id = existant ?: UUID.randomUUID().toString().also {
            settings.set(SettingsStore.Keys.SYNC_DEVICE_ID, it)
        }
        dao.appareil(id)?.let { return it }
        val nouveau = SyncDeviceEntity(
            deviceId = id,
            nom = listOf(Build.MANUFACTURER, Build.MODEL).filter { it.isNotBlank() }.joinToString(" ").ifBlank { "Android" },
            creeLe = System.currentTimeMillis(),
            vuLe = System.currentTimeMillis(),
        )
        dao.insererAppareil(nouveau)
        return nouveau
    }

    suspend fun enregistrerModification(
        entrepriseId: String,
        aggregateType: String,
        aggregateId: String,
        operation: SyncOperation,
        payloadJson: String,
        revision: Long,
        maintenant: Long = System.currentTimeMillis(),
    ): String {
        require(entrepriseId.isNotBlank() && aggregateType.isNotBlank() && aggregateId.isNotBlank())
        require(revision >= 0 && payloadJson.isNotBlank())
        val eventId = UUID.randomUUID().toString()
        dao.ajouterSortant(
            SyncOutboxEntity(
                eventId = eventId,
                deviceId = appareilLocal().deviceId,
                entrepriseId = entrepriseId,
                aggregateType = aggregateType,
                aggregateId = aggregateId,
                operation = operation.name,
                payload = payloadJson,
                revision = revision,
                creeLe = maintenant,
            ),
        )
        return eventId
    }

    suspend fun lotAExpedier(maintenant: Long = System.currentTimeMillis()): List<SyncOutboxEntity> {
        val lot = dao.lotAEnvoyer(maintenant, SyncPolicy.LOT_MAX)
        lot.forEach { dao.modifierSortant(it.copy(statut = SyncStatut.EN_COURS.name)) }
        return lot
    }

    suspend fun confirmerLivraison(eventId: String) = dao.marquerLivre(eventId)

    suspend fun signalerEchec(eventId: String, erreur: String, maintenant: Long = System.currentTimeMillis()) {
        val event = dao.sortant(eventId) ?: return
        val essais = event.tentatives + 1
        dao.modifierSortant(
            event.copy(
                statut = SyncStatut.ECHEC.name,
                tentatives = essais,
                prochaineTentative = maintenant + SyncPolicy.delaiNouvelEssai(essais),
                derniereErreur = erreur.take(500),
            ),
        )
    }

    /**
     * Réception atomique et idempotente. Le callback applique le payload au dépôt
     * métier concerné ; en cas de conflit, aucune version n'est écrasée.
     */
    suspend fun recevoir(
        event: SyncOutboxEntity,
        revisionLocale: Long,
        payloadLocal: String,
        modificationsLocales: Boolean,
        appliquer: suspend (SyncOutboxEntity) -> Unit,
        maintenant: Long = System.currentTimeMillis(),
    ): ReceptionResultat = database.withTransaction {
        if (dao.dejaRecu(event.eventId)) return@withTransaction ReceptionResultat.DEJA_RECU
        dao.ajouterRecu(
            SyncInboxEntity(
                eventId = event.eventId,
                deviceIdSource = event.deviceId,
                aggregateType = event.aggregateType,
                aggregateId = event.aggregateId,
                revision = event.revision,
                recuLe = maintenant,
            ),
        )
        if (SyncPolicy.conflit(revisionLocale, event.revision, modificationsLocales)) {
            dao.ajouterConflit(
                SyncConflictEntity(
                    conflictId = UUID.randomUUID().toString(),
                    eventIdDistant = event.eventId,
                    aggregateType = event.aggregateType,
                    aggregateId = event.aggregateId,
                    revisionLocale = revisionLocale,
                    revisionDistante = event.revision,
                    payloadLocal = payloadLocal,
                    payloadDistant = event.payload,
                    detecteLe = maintenant,
                ),
            )
            ReceptionResultat.CONFLIT
        } else {
            appliquer(event)
            dao.marquerApplique(event.eventId, maintenant)
            ReceptionResultat.APPLIQUE
        }
    }

    suspend fun resoudreConflit(id: String, resolution: String): Boolean =
        dao.resoudreConflit(id, resolution.take(100), System.currentTimeMillis()) == 1
}

enum class ReceptionResultat { APPLIQUE, DEJA_RECU, CONFLIT }
