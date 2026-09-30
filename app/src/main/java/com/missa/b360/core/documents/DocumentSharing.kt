package com.missa.b360.core.documents

import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/** Partage sécurisé commun à tous les PDF produits par l'application. */
object DocumentSharing {
    private const val MIME_PDF = "application/pdf"

    sealed interface Resultat {
        data object Lance : Resultat
        data class Enregistre(val octets: Long, val sha256: String) : Resultat
        data class Erreur(val raison: ErreurPartage) : Resultat
    }

    enum class ErreurPartage { FICHIER_ABSENT, FICHIER_VIDE, FORMAT_INVALIDE, DESTINATION_INACCESSIBLE, AUCUNE_APPLICATION }

    /** Partage vers WhatsApp, Drive, Bluetooth, messagerie ou toute cible compatible. */
    fun partager(context: Context, fichier: File, titre: String, message: String? = null): Resultat {
        DocumentFilePolicy.erreur(fichier)?.let { return Resultat.Erreur(it) }
        val uri = uriSecurisee(context, fichier)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = MIME_PDF
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, titre)
            message?.takeIf(String::isNotBlank)?.let { putExtra(Intent.EXTRA_TEXT, it) }
            clipData = ClipData.newUri(context.contentResolver, titre, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return lancer(context, Intent.createChooser(intent, titre))
    }

    /** Envoi par e-mail sans imposer une application ou un fournisseur précis. */
    fun envoyerEmail(context: Context, fichier: File, sujet: String, destinataire: String? = null, message: String? = null): Resultat {
        DocumentFilePolicy.erreur(fichier)?.let { return Resultat.Erreur(it) }
        val uri = uriSecurisee(context, fichier)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = MIME_PDF
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, sujet)
            destinataire?.takeIf(String::isNotBlank)?.let { putExtra(Intent.EXTRA_EMAIL, arrayOf(it)) }
            message?.takeIf(String::isNotBlank)?.let { putExtra(Intent.EXTRA_TEXT, it) }
            clipData = ClipData.newUri(context.contentResolver, sujet, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return lancer(context, Intent.createChooser(intent, sujet))
    }

    /** Plusieurs documents dans un seul envoi, par exemple facture + reçu + livraison. */
    fun partagerLot(context: Context, fichiers: List<File>, titre: String, message: String? = null): Resultat {
        if (fichiers.isEmpty()) return Resultat.Erreur(ErreurPartage.FICHIER_ABSENT)
        fichiers.firstNotNullOfOrNull(DocumentFilePolicy::erreur)?.let { return Resultat.Erreur(it) }
        val uris = ArrayList(fichiers.map { uriSecurisee(context, it) })
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = MIME_PDF
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            putExtra(Intent.EXTRA_SUBJECT, titre)
            message?.takeIf(String::isNotBlank)?.let { putExtra(Intent.EXTRA_TEXT, it) }
            clipData = ClipData.newUri(context.contentResolver, titre, uris.first()).apply {
                uris.drop(1).forEach { addItem(ClipData.Item(it)) }
            }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return lancer(context, Intent.createChooser(intent, titre))
    }

    /** Ouvre le PDF dans une visionneuse, sans exposer son chemin interne. */
    fun ouvrir(context: Context, fichier: File, titre: String): Resultat {
        DocumentFilePolicy.erreur(fichier)?.let { return Resultat.Erreur(it) }
        val uri = uriSecurisee(context, fichier)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, MIME_PDF)
            clipData = ClipData.newUri(context.contentResolver, titre, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return lancer(context, Intent.createChooser(intent, titre))
    }

    /** Lance le véritable dialogue Android d'impression. */
    fun imprimer(context: Context, fichier: File, titre: String): Resultat {
        DocumentFilePolicy.erreur(fichier)?.let { return Resultat.Erreur(it) }
        val manager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
            ?: return Resultat.Erreur(ErreurPartage.AUCUNE_APPLICATION)
        manager.print(
            DocumentFilePolicy.nomSain(titre),
            PdfFilePrintAdapter(fichier, titre),
            PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                .build(),
        )
        return Resultat.Lance
    }

    /** Copie vers l'URI choisie par ACTION_CREATE_DOCUMENT et vérifie le contenu écrit. */
    fun enregistrerCopie(context: Context, fichier: File, destination: Uri): Resultat {
        DocumentFilePolicy.erreur(fichier)?.let { return Resultat.Erreur(it) }
        return runCatching {
            val digest = MessageDigest.getInstance("SHA-256")
            var total = 0L
            context.contentResolver.openOutputStream(destination, "w")?.use { sortie ->
                FileInputStream(fichier).use { entree ->
                    val tampon = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val lus = entree.read(tampon)
                        if (lus < 0) break
                        sortie.write(tampon, 0, lus)
                        digest.update(tampon, 0, lus)
                        total += lus
                    }
                    sortie.flush()
                }
            } ?: error("Destination inaccessible")
            check(total == fichier.length())
            Resultat.Enregistre(total, digest.digest().joinToString("") { "%02x".format(it) })
        }.getOrElse { Resultat.Erreur(ErreurPartage.DESTINATION_INACCESSIBLE) }
    }

    /** Les PDF du cache sont temporaires ; les récents restent partageables. */
    fun nettoyerCache(context: Context, retentionJours: Int = 7): Int {
        val limite = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(retentionJours.coerceAtLeast(1).toLong())
        return listOf("documents_pdf", "fiches_pdf").sumOf { dossier ->
            File(context.cacheDir, dossier).listFiles()?.count { it.isFile && it.lastModified() < limite && it.delete() } ?: 0
        }
    }

    private fun uriSecurisee(context: Context, fichier: File) =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", fichier)

    private fun lancer(context: Context, intent: Intent): Resultat = runCatching {
        if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        Resultat.Lance
    }.getOrElse { Resultat.Erreur(ErreurPartage.AUCUNE_APPLICATION) }
}

/** Règles pures, également utilisées par les tests JVM. */
object DocumentFilePolicy {
    fun erreur(fichier: File): DocumentSharing.ErreurPartage? = when {
        !fichier.exists() || !fichier.isFile -> DocumentSharing.ErreurPartage.FICHIER_ABSENT
        fichier.length() <= 4L -> DocumentSharing.ErreurPartage.FICHIER_VIDE
        fichier.extension.lowercase() != "pdf" -> DocumentSharing.ErreurPartage.FORMAT_INVALIDE
        runCatching {
            FileInputStream(fichier).use { flux ->
                val signature = ByteArray(4)
                flux.read(signature) == 4 && signature.contentEquals("%PDF".encodeToByteArray())
            }
        }.getOrDefault(false).not() -> DocumentSharing.ErreurPartage.FORMAT_INVALIDE
        else -> null
    }

    fun nomSain(value: String): String = value.trim()
        .replace(Regex("[^A-Za-z0-9À-ÿ._ -]"), "-")
        .replace(Regex("\\s+"), " ")
        .take(80)
        .ifBlank { "Document" }
}

private class PdfFilePrintAdapter(private val fichier: File, private val titre: String) : PrintDocumentAdapter() {
    override fun onLayout(oldAttributes: PrintAttributes?, newAttributes: PrintAttributes?, cancellationSignal: CancellationSignal, callback: LayoutResultCallback, extras: Bundle?) {
        if (cancellationSignal.isCanceled) return callback.onLayoutCancelled()
        callback.onLayoutFinished(
            PrintDocumentInfo.Builder("${DocumentFilePolicy.nomSain(titre)}.pdf")
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .build(),
            oldAttributes != newAttributes,
        )
    }

    override fun onWrite(pages: Array<out android.print.PageRange>, destination: ParcelFileDescriptor, cancellationSignal: CancellationSignal, callback: WriteResultCallback) {
        try {
            FileInputStream(fichier).use { entree ->
                FileOutputStream(destination.fileDescriptor).use { sortie ->
                    val tampon = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (!cancellationSignal.isCanceled) {
                        val lus = entree.read(tampon)
                        if (lus < 0) break
                        sortie.write(tampon, 0, lus)
                    }
                }
            }
            if (cancellationSignal.isCanceled) callback.onWriteCancelled()
            else callback.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))
        } catch (e: Exception) {
            callback.onWriteFailed(e.localizedMessage)
        }
    }
}
