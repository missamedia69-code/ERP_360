package com.missa.b360.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Images de produits : un JPEG compact par article dans le stockage interne.
 *
 * L'image fournie à l'enregistrement est réduite à [TAILLE_MAX] px et
 * recompressée (qualité [QUALITE]) — quelques dizaines de Ko au lieu des
 * plusieurs Mo d'une photo de téléphone, tout en restant nette en vignette
 * comme en fiche. Aucun BLOB en base : le fichier est nommé d'après
 * l'identifiant produit, donc aucune migration de schéma.
 */
object ImageProduit {

    private const val TAILLE_MAX = 512
    private const val QUALITE = 70

    fun fichier(context: Context, produitId: Long): File =
        File(context.filesDir, "produits/$produitId.jpg")

    fun existe(context: Context, produitId: Long): Boolean = fichier(context, produitId).exists()

    /** Décode le fichier compact, ou null si absent. */
    suspend fun charger(context: Context, produitId: Long): Bitmap? = withContext(Dispatchers.IO) {
        val f = fichier(context, produitId)
        if (!f.exists()) return@withContext null
        runCatching { BitmapFactory.decodeFile(f.absolutePath) }.getOrNull()
    }

    /**
     * Enregistre l'image pointée par [uri] : décodage borné (jamais le bitmap
     * pleine taille en mémoire), mise à l'échelle, compression JPEG.
     * @return le chemin du fichier écrit, ou null en cas d'échec.
     */
    suspend fun enregistrer(context: Context, produitId: Long, uri: Uri): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                // 1) Dimensions seules, pour calculer l'échantillonnage.
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, options)
                } ?: return@runCatching null
                var echantillon = 1
                while (maxOf(options.outWidth, options.outHeight) / (echantillon * 2) >= TAILLE_MAX) {
                    echantillon *= 2
                }
                // 2) Décodage borné.
                val source = context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = echantillon })
                } ?: return@runCatching null
                // 3) Mise à l'échelle finale au plus grand côté ≤ TAILLE_MAX.
                val plusGrand = maxOf(source.width, source.height)
                val cible = if (plusGrand > TAILLE_MAX) {
                    val ratio = TAILLE_MAX.toFloat() / plusGrand
                    Bitmap.createScaledBitmap(
                        source,
                        (source.width * ratio).toInt().coerceAtLeast(1),
                        (source.height * ratio).toInt().coerceAtLeast(1),
                        true,
                    )
                } else {
                    source
                }
                // 4) Écriture JPEG compacte.
                val destination = fichier(context, produitId)
                destination.parentFile?.mkdirs()
                FileOutputStream(destination).use { sortie ->
                    cible.compress(Bitmap.CompressFormat.JPEG, QUALITE, sortie)
                }
                if (cible !== source) cible.recycle()
                source.recycle()
                destination.absolutePath
            }.getOrNull()
        }

    /** Décode une image sélectionnée (aperçu avant enregistrement), bornée à [TAILLE_MAX]. */
    suspend fun decoderUri(context: Context, uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        runCatching {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            }
            var echantillon = 1
            while (maxOf(options.outWidth, options.outHeight) / (echantillon * 2) >= TAILLE_MAX) {
                echantillon *= 2
            }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = echantillon })
            }
        }.getOrNull()
    }

    /**
     * Compresse l'image sélectionnée dans un fichier temporaire (cache) —
     * appelé dès la sélection, avant que l'identifiant produit existe.
     * @return le chemin temporaire, ou null si l'image est illisible.
     */
    suspend fun enregistrerTemp(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val bitmap = decoderBitmap(context, uri) ?: return@runCatching null
            val temp = File(context.cacheDir, "images/pending_${System.currentTimeMillis()}.jpg")
            temp.parentFile?.mkdirs()
            FileOutputStream(temp).use { sortie ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, QUALITE, sortie)
            }
            bitmap.recycle()
            temp.absolutePath
        }.getOrNull()
    }

    /** Déplace un fichier temporaire vers l'emplacement définitif du produit. */
    suspend fun promouvoirTemp(context: Context, produitId: Long, tempPath: String): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                val temp = File(tempPath)
                if (!temp.exists()) return@runCatching null
                val destination = fichier(context, produitId)
                destination.parentFile?.mkdirs()
                temp.copyTo(destination, overwrite = true)
                temp.delete()
                destination.absolutePath
            }.getOrNull()
        }

    private fun decoderBitmap(context: Context, uri: Uri): Bitmap? {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: return null
        var echantillon = 1
        while (maxOf(options.outWidth, options.outHeight) / (echantillon * 2) >= TAILLE_MAX) {
            echantillon *= 2
        }
        val source = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = echantillon })
        } ?: return null
        val plusGrand = maxOf(source.width, source.height)
        if (plusGrand <= TAILLE_MAX) return source
        val ratio = TAILLE_MAX.toFloat() / plusGrand
        val cible = Bitmap.createScaledBitmap(
            source,
            (source.width * ratio).toInt().coerceAtLeast(1),
            (source.height * ratio).toInt().coerceAtLeast(1),
            true,
        )
        source.recycle()
        return cible
    }

    suspend fun supprimer(context: Context, produitId: Long) = withContext(Dispatchers.IO) {
        runCatching { fichier(context, produitId).delete() }
    }
}
