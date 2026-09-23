package com.missa.b360.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Pièces jointes d'une facture fournisseur : photos compressées et PDF,
 * stockés dans le stockage interne (`filesDir/achats/`), jamais en base.
 *
 * Même philosophie que [ImageProduit] : décodage borné, mise à l'échelle,
 * JPEG compact ; les PDF sont simplement copiés (plafond [PDF_MAX_OCTETS]).
 */
object PieceJointeAchat {

    private const val TAILLE_MAX = 1024
    private const val QUALITE = 70
    private const val PDF_MAX_OCTETS = 8L * 1024 * 1024

    @Volatile
    var derniereErreur: String? = null
        private set

    private fun dossier(context: Context): File = File(context.filesDir, "achats")

    fun estPdf(path: String): Boolean = path.endsWith(".pdf", ignoreCase = true)

    /**
     * Enregistre la pièce pointée par [uri] (photo ou PDF).
     * @return le chemin absolu du fichier écrit, ou null en cas d'échec.
     */
    suspend fun enregistrer(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val mime = context.contentResolver.getType(uri).orEmpty()
            val destination = if (mime == "application/pdf" || uri.lastPathSegment?.endsWith(".pdf") == true) {
                copierPdf(context, uri)
            } else {
                compresserPhoto(context, uri)
            }
            destination?.also { derniereErreur = null }
        }.onFailure { e ->
            derniereErreur = e.javaClass.simpleName + ": " + e.message
        }.getOrNull()
    }

    private fun copierPdf(context: Context, uri: Uri): String? {
        val taille = context.contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize } ?: -1L
        if (taille > PDF_MAX_OCTETS) {
            derniereErreur = "PDF trop lourd"
            return null
        }
        val cible = File(dossier(context), "${UUID.randomUUID()}.pdf")
        cible.parentFile?.mkdirs()
        context.contentResolver.openInputStream(uri)?.use { entree ->
            FileOutputStream(cible).use { sortie -> entree.copyTo(sortie) }
        } ?: return null
        return cible.absolutePath
    }

    private fun compresserPhoto(context: Context, uri: Uri): String? {
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
        val fichier = File(dossier(context), "${UUID.randomUUID()}.jpg")
        fichier.parentFile?.mkdirs()
        FileOutputStream(fichier).use { sortie ->
            cible.compress(Bitmap.CompressFormat.JPEG, QUALITE, sortie)
        }
        if (cible !== source) cible.recycle()
        source.recycle()
        return fichier.absolutePath
    }

    /** Vignette d'une photo jointe (null pour un PDF). */
    suspend fun charger(path: String): Bitmap? = withContext(Dispatchers.IO) {
        if (estPdf(path)) return@withContext null
        runCatching { BitmapFactory.decodeFile(path) }.getOrNull()
    }

    fun supprimer(path: String) {
        runCatching { File(path).takeIf { it.exists() }?.delete() }
    }
}
