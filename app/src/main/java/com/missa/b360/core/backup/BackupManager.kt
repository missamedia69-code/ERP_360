package com.missa.b360.core.backup

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import androidx.sqlite.db.SimpleSQLiteQuery
import com.missa.b360.core.data.dao.BackupDao
import com.missa.b360.core.data.db.AppDatabase
import com.missa.b360.core.data.entity.BackupEntity
import com.missa.b360.core.journal.JournalManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * BackupManager (RA-13) — export local (Drive/iCloud en Phase C).
 * Sauvegarde cohérente via `VACUUM INTO` (snapshot atomique de la base).
 * Aucune donnée de démo dans les sauvegardes : uniquement les données réelles.
 */
@Singleton
class BackupManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val database: AppDatabase,
    private val backupDao: BackupDao,
    private val journalManager: JournalManager,
) {
    /** Sauvegarde locale complète. @return le fichier créé, ou null en cas d'échec. */
    suspend fun sauvegarderLocalement(type: String = "MANUAL"): File? {
        return try {
            val dir = File(context.filesDir, "backups").apply { mkdirs() }
            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ROOT).format(Date())
            val fichier = File(dir, "missa_b360_$stamp.db")
            database.openHelper.writableDatabase.query(
                SimpleSQLiteQuery("VACUUM INTO ?", arrayOf(fichier.absolutePath)),
            ).use { it.moveToFirst() }
            backupDao.insert(
                BackupEntity(
                    date = System.currentTimeMillis(),
                    type = type,
                    chemin = fichier.absolutePath,
                    plateforme = "LOCAL",
                ),
            )
            journalManager.log("ADMIN", "SAUVEGARDE", "Sauvegarde locale : ${fichier.name}")
            fichier
        } catch (e: Exception) {
            journalManager.log("ADMIN", "SAUVEGARDE_ECHEC", "Erreur : ${e.message}")
            null
        }
    }

    fun observeHistorique() = backupDao.observeAll()

    /**
     * Restaure une sauvegarde choisie par l'utilisateur (fichier `.db` produit par
     * [sauvegarderLocalement]) — reprise d'un appareil précédent ou réinstallation.
     *
     * Déroulé sécurisé :
     * 1. copie du fichier choisi dans le cache (le SAF ne donne pas de chemin direct) ;
     * 2. contrôle qu'il s'agit bien d'une base Missa Business 360 lisible et pas plus
     *    récente que le schéma supporté ;
     * 3. copie de sécurité de la base courante (type `AVANT_RESTAURATION`) ;
     * 4. fermeture de Room puis remplacement du fichier de base (+ purge WAL/SHM).
     *
     * L'appelant doit **redémarrer l'application** après un [ResultatRestauration.Succes] :
     * l'instance Room en mémoire pointe encore sur l'ancien fichier.
     */
    suspend fun restaurerDepuis(source: Uri): ResultatRestauration = withContext(Dispatchers.IO) {
        val temporaire = File(context.cacheDir, "restauration_${System.currentTimeMillis()}.db")
        try {
            val copie = runCatching {
                context.contentResolver.openInputStream(source)?.use { entree ->
                    temporaire.outputStream().use { sortie -> entree.copyTo(sortie) }
                }
            }.getOrNull()
            if (copie == null || !temporaire.exists() || temporaire.length() == 0L) {
                return@withContext echec(ResultatRestauration.Motif.FICHIER_ILLISIBLE)
            }

            val version = versionSauvegarde(temporaire)
                ?: return@withContext echec(ResultatRestauration.Motif.FORMAT_INVALIDE)
            if (version > AppDatabase.VERSION_SCHEMA) {
                return@withContext echec(ResultatRestauration.Motif.VERSION_TROP_RECENTE)
            }

            // Filet de sécurité : la base actuelle est sauvegardée avant d'être remplacée.
            sauvegarderLocalement(type = "AVANT_RESTAURATION")
            journalManager.log("ADMIN", "RESTAURATION", "Restauration depuis $source")

            database.close()
            val destination = context.getDatabasePath(NOM_BASE)
            destination.parentFile?.mkdirs()
            temporaire.copyTo(destination, overwrite = true)
            File("${destination.path}-wal").delete()
            File("${destination.path}-shm").delete()
            ResultatRestauration.Succes
        } catch (e: Exception) {
            runCatching {
                journalManager.log("ADMIN", "RESTAURATION_ECHEC", "Erreur : ${e.message}")
            }
            echec(ResultatRestauration.Motif.ECHEC_COPIE)
        } finally {
            temporaire.delete()
        }
    }

    private suspend fun echec(motif: ResultatRestauration.Motif): ResultatRestauration {
        runCatching { journalManager.log("ADMIN", "RESTAURATION_REFUSEE", motif.name) }
        return ResultatRestauration.Echec(motif)
    }

    /**
     * Version de schéma (`user_version`) du fichier, ou null si ce n'est pas une base
     * SQLite produite par Room (absence de `room_master_table`).
     */
    private fun versionSauvegarde(fichier: File): Int? = runCatching {
        SQLiteDatabase.openDatabase(
            fichier.absolutePath,
            null,
            SQLiteDatabase.OPEN_READONLY,
        ).use { base ->
            val estRoom = base.rawQuery(
                "SELECT name FROM sqlite_master WHERE type='table' AND name='room_master_table'",
                null,
            ).use { it.moveToFirst() }
            val contientEntreprise = base.rawQuery(
                "SELECT name FROM sqlite_master WHERE type='table' AND name='entreprise'",
                null,
            ).use { it.moveToFirst() }
            if (estRoom || contientEntreprise) base.version else null
        }
    }.getOrNull()

    companion object {
        /** Nom du fichier Room (doit rester aligné sur DatabaseModule). */
        const val NOM_BASE = "missa_b360.db"
    }
}

/** Issue d'une restauration de sauvegarde. */
sealed class ResultatRestauration {

    /** Base remplacée : l'application doit redémarrer. */
    object Succes : ResultatRestauration()

    /** Restauration refusée ou interrompue : la base actuelle est intacte. */
    data class Echec(val motif: Motif) : ResultatRestauration()

    enum class Motif {
        /** Fichier introuvable, vide ou illisible. */
        FICHIER_ILLISIBLE,

        /** Ce n'est pas une sauvegarde Missa Business 360. */
        FORMAT_INVALIDE,

        /** Sauvegarde produite par une version plus récente de l'application. */
        VERSION_TROP_RECENTE,

        /** Erreur d'écriture pendant le remplacement du fichier. */
        ECHEC_COPIE,
    }
}
