package com.missa.b360.core.documents

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.max

/**
 * Moteur PDF A4 commun à tous les modules. L'émetteur est exclusivement
 * l'entreprise cliente ; Missa n'apparaît que dans la mention discrète du pied.
 */
object ProfessionalDocumentPdf {
    private const val W = 595f
    private const val H = 842f
    private const val M = 32f
    private const val FOOTER_Y = 812f
    private const val CONTENT_BOTTOM = 775f

    data class Resultat(val fichier: File, val pages: Int, val avertissements: List<String>)

    fun generer(context: Context, donnees: DocumentDonnees, options: DocumentOptions = DocumentOptions()): Resultat {
        val erreurs = DocumentValidation.erreurs(donnees)
        require(erreurs.isEmpty()) { erreurs.joinToString("; ") }
        val pagesLignes = DocumentValidation.paginer(donnees.lignes)
        val pdf = PdfDocument()
        val logo = if (options.afficherLogo) chargerLogo(context, donnees.entreprise.logoUri) else null
        val locale = runCatching { Locale.forLanguageTag(options.localeTag) }.getOrDefault(Locale.FRENCH)
        val nombres = NumberFormat.getNumberInstance(locale).apply { minimumFractionDigits = 0; maximumFractionDigits = 2 }
        val avertissements = buildList {
            if (options.afficherLogo && donnees.entreprise.logoUri != null && logo == null) add("Logo inaccessible : document généré sans logo")
            if (donnees.entreprise.numeroFiscal.isNullOrBlank()) add("Identifiant fiscal non renseigné")
            if (donnees.entreprise.registreCommerce.isNullOrBlank()) add("Registre du commerce non renseigné")
        }

        pagesLignes.forEachIndexed { pageIndex, lignesPage ->
            val page = pdf.startPage(PdfDocument.PageInfo.Builder(W.toInt(), H.toInt(), pageIndex + 1).create())
            val canvas = page.canvas
            entete(canvas, donnees, options, logo, pageIndex > 0)
            var y = if (pageIndex == 0) blocParties(canvas, donnees, options) else 171f
            y = tableau(canvas, lignesPage, donnees, options, nombres, y)
            if (pageIndex == pagesLignes.lastIndex) {
                y = finDocument(canvas, donnees, options, nombres, y)
            } else {
                texte(canvas, "Suite page ${pageIndex + 2}", W - M, CONTENT_BOTTOM - 5f, 8f, 0xFF718096.toInt(), align = Paint.Align.RIGHT)
            }
            pied(canvas, donnees, options, pageIndex + 1, pagesLignes.size)
            pdf.finishPage(page)
        }

        val dossier = File(context.cacheDir, "documents_pdf").apply { mkdirs() }
        val nom = "${donnees.type.prefixe}-${nettoyerNom(donnees.numero)}-${System.currentTimeMillis()}.pdf"
        val fichier = File(dossier, nom)
        FileOutputStream(fichier).use(pdf::writeTo)
        pdf.close()
        logo?.recycle()
        return Resultat(fichier, pagesLignes.size, avertissements)
    }

    private fun entete(c: Canvas, d: DocumentDonnees, o: DocumentOptions, logo: Bitmap?, continuation: Boolean) {
        c.drawRect(0f, 0f, W, 7f, paint(o.couleurPrincipale))
        var x = M
        if (logo != null) {
            val ratio = logo.width.toFloat() / max(1, logo.height)
            val h = 52f
            val w = (h * ratio).coerceIn(42f, 100f)
            c.drawBitmap(logo, null, RectF(M, 25f, M + w, 25f + h), paint(Color.WHITE))
            x += w + 11f
        }
        texte(c, d.entreprise.nom, x, 42f, 15f, o.couleurPrincipale, true)
        d.entreprise.secteur?.takeIf(String::isNotBlank)?.let { texte(c, it, x, 58f, 7.5f, 0xFF667085.toInt()) }
        val coords = listOfNotNull(d.entreprise.adresse, d.entreprise.telephone, d.entreprise.email).filter(String::isNotBlank)
        coords.take(2).forEachIndexed { i, value -> texte(c, value, x, 71f + i * 11f, 7f, 0xFF667085.toInt()) }

        val titre = d.titrePersonnalise ?: d.type.titreDefaut
        texte(c, if (continuation) "$titre — SUITE" else titre, W - M, 43f, 17f, o.couleurPrincipale, true, Paint.Align.RIGHT)
        texte(c, "N° ${d.numero}", W - M, 63f, 8.5f, 0xFF344054.toInt(), align = Paint.Align.RIGHT)
        d.statut?.takeIf(String::isNotBlank)?.let {
            val largeur = (mesure(it.uppercase(), 7.5f, true) + 18f).coerceAtLeast(60f)
            c.drawRoundRect(RectF(W - M - largeur, 72f, W - M, 91f), 9f, 9f, paint(0xFFE6F6F2.toInt()))
            texte(c, it.uppercase(), W - M - largeur / 2f, 85f, 7.5f, o.couleurAccent, true, Paint.Align.CENTER)
        }
        c.drawLine(M, 105f, W - M, 105f, paint(0xFFD9E2E8.toInt(), stroke = 0.7f))
    }

    private fun blocParties(c: Canvas, d: DocumentDonnees, o: DocumentOptions): Float {
        var y = 125f
        d.destinataire?.let { p ->
            texte(c, p.titre.uppercase(), M, y, 7f, 0xFF667085.toInt(), true)
            texte(c, p.nom, M, y + 20f, 11f, o.couleurPrincipale, true)
            p.lignes.take(3).forEachIndexed { i, ligne -> texte(c, ligne, M, y + 34f + 11f * i, 7.5f, 0xFF667085.toInt()) }
        }
        val meta = (listOf(DocumentChamp("Date", d.dateEmission)) + d.meta).take(5)
        val boxH = 25f + meta.size * 18f
        c.drawRoundRect(RectF(350f, y - 12f, W - M, y - 12f + boxH), 5f, 5f, paint(0xFFF6F8FA.toInt()))
        meta.forEachIndexed { i, champ ->
            val ly = y + 5f + i * 18f
            texte(c, champ.libelle, 363f, ly, 7f, 0xFF667085.toInt())
            texte(c, champ.valeur, W - M - 10f, ly, 7.3f, o.couleurPrincipale, true, Paint.Align.RIGHT)
        }
        return max(222f, y - 1f + boxH)
    }

    private fun tableau(c: Canvas, lignes: List<DocumentLigne>, d: DocumentDonnees, o: DocumentOptions, nf: NumberFormat, debut: Float): Float {
        var y = debut
        if (lignes.isEmpty()) return y
        c.drawRoundRect(RectF(M, y, W - M, y + 27f), 4f, 4f, paint(o.couleurPrincipale))
        texte(c, "DÉSIGNATION", M + 9f, y + 18f, 7f, Color.WHITE, true)
        texte(c, "QTÉ", 367f, y + 18f, 7f, Color.WHITE, true, Paint.Align.CENTER)
        texte(c, "PU (${d.devise})", 462f, y + 18f, 7f, Color.WHITE, true, Paint.Align.RIGHT)
        texte(c, "TOTAL", W - M - 8f, y + 18f, 7f, Color.WHITE, true, Paint.Align.RIGHT)
        y += 27f
        lignes.forEachIndexed { index, l ->
            val h = if (l.description.isNullOrBlank()) 31f else 43f
            if (index % 2 == 1) c.drawRect(M, y, W - M, y + h, paint(0xFFF8FAFB.toInt()))
            texte(c, l.designation.take(65), M + 9f, y + 17f, 8f, o.couleurPrincipale, true)
            l.description?.takeIf(String::isNotBlank)?.let { texte(c, it.take(75), M + 9f, y + 31f, 6.8f, 0xFF667085.toInt()) }
            val qte = l.quantite?.let(nf::format).orEmpty() + l.unite?.let { " $it" }.orEmpty()
            texte(c, qte, 367f, y + 18f, 7.5f, 0xFF344054.toInt(), align = Paint.Align.CENTER)
            texte(c, l.prixUnitaire?.let(nf::format).orEmpty(), 462f, y + 18f, 7.5f, 0xFF344054.toInt(), align = Paint.Align.RIGHT)
            texte(c, l.montant?.let(nf::format).orEmpty(), W - M - 8f, y + 18f, 7.5f, o.couleurPrincipale, true, Paint.Align.RIGHT)
            y += h
            c.drawLine(M, y, W - M, y, paint(0xFFE4E7EC.toInt(), stroke = 0.4f))
        }
        return y + 12f
    }

    private fun finDocument(c: Canvas, d: DocumentDonnees, o: DocumentOptions, nf: NumberFormat, debut: Float): Float {
        var y = debut
        if (d.totaux.isNotEmpty()) {
            val debutTotaux = max(y, 525f)
            d.totaux.forEachIndexed { index, total ->
                val ty = debutTotaux + index * 22f
                if (total.important) {
                    c.drawRoundRect(RectF(342f, ty - 15f, W - M, ty + 10f), 4f, 4f, paint(o.couleurPrincipale))
                    texte(c, total.libelle.uppercase(), 352f, ty + 2f, 8f, Color.WHITE, true)
                    texte(c, "${nf.format(total.montant)} ${d.devise}", W - M - 9f, ty + 2f, 9f, Color.WHITE, true, Paint.Align.RIGHT)
                } else {
                    texte(c, total.libelle, 352f, ty, 7.5f, 0xFF667085.toInt())
                    texte(c, "${nf.format(total.montant)} ${d.devise}", W - M - 9f, ty, 8f, o.couleurPrincipale, true, Paint.Align.RIGHT)
                }
            }
            y = debutTotaux + d.totaux.size * 22f + 12f
        }
        if (o.afficherMontantEnLettres) d.montantEnLettres?.takeIf(String::isNotBlank)?.let {
            texte(c, "Arrêté le présent document à la somme de :", M, y, 7f, 0xFF667085.toInt())
            texte(c, it.take(115), M, y + 15f, 7.5f, o.couleurPrincipale, true)
            y += 34f
        }
        if (d.notes.isNotEmpty() && y < 700f) {
            texte(c, "OBSERVATIONS", M, y, 7f, o.couleurPrincipale, true)
            d.notes.take(4).forEachIndexed { i, note -> texte(c, "• ${note.take(115)}", M, y + 14f + i * 12f, 7f, 0xFF475467.toInt()) }
            y += 22f + d.notes.take(4).size * 12f
        }
        d.sections.take(3).forEach { (titre, lignes) ->
            if (y < 715f) {
                texte(c, titre.uppercase(), M, y, 7f, o.couleurPrincipale, true)
                lignes.take(4).forEachIndexed { i, ligne -> texte(c, ligne.take(120), M, y + 14f + i * 12f, 7f, 0xFF475467.toInt()) }
                y += 22f + lignes.take(4).size * 12f
            }
        }
        if (d.signataires.isNotEmpty() && y < 730f) {
            val largeur = (W - M * 2) / d.signataires.size
            d.signataires.forEachIndexed { i, s -> texte(c, s, M + largeur * i + largeur / 2, max(y + 30f, 720f), 7f, o.couleurPrincipale, true, Paint.Align.CENTER) }
        }
        return y
    }

    private fun pied(c: Canvas, d: DocumentDonnees, o: DocumentOptions, page: Int, pages: Int) {
        c.drawLine(M, 785f, W - M, 785f, paint(0xFFD9E2E8.toInt(), stroke = 0.6f))
        val legal = listOfNotNull(
            d.entreprise.numeroFiscal?.takeIf(String::isNotBlank)?.let { "N° fiscal : $it" },
            d.entreprise.registreCommerce?.takeIf(String::isNotBlank)?.let { "Registre : $it" },
        ).joinToString("  ·  ")
        texte(c, legal, M, 800f, 6.5f, 0xFF667085.toInt())
        texte(c, "Page $page/$pages", W / 2, FOOTER_Y, 6.5f, 0xFF98A2B3.toInt(), align = Paint.Align.CENTER)
        if (o.afficherMentionMissa) texte(c, o.mentionMissa, W - M, FOOTER_Y, 6.5f, 0xFF98A2B3.toInt(), align = Paint.Align.RIGHT)
    }

    private fun chargerLogo(context: Context, valeur: String?): Bitmap? {
        if (valeur.isNullOrBlank()) return null
        return runCatching {
            context.contentResolver.openInputStream(Uri.parse(valeur))?.use { flux ->
                BitmapFactory.decodeStream(flux)?.let { bitmap ->
                    if (bitmap.width > 800 || bitmap.height > 800) {
                        val echelle = 800f / max(bitmap.width, bitmap.height)
                        Bitmap.createScaledBitmap(
                            bitmap,
                            (bitmap.width * echelle).toInt().coerceAtLeast(1),
                            (bitmap.height * echelle).toInt().coerceAtLeast(1),
                            true,
                        ).also { bitmap.recycle() }
                    } else bitmap
                }
            }
        }.getOrNull()
    }

    private fun texte(c: Canvas, value: String, x: Float, y: Float, size: Float, color: Int, bold: Boolean = false, align: Paint.Align = Paint.Align.LEFT) {
        c.drawText(value, x, y, paint(color, size, bold, align))
    }
    private fun mesure(value: String, size: Float, bold: Boolean) = paint(Color.BLACK, size, bold).measureText(value)
    private fun paint(color: Int, size: Float = 8f, bold: Boolean = false, align: Paint.Align = Paint.Align.LEFT, stroke: Float = 0f) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color; textSize = size; textAlign = align
        typeface = Typeface.create(Typeface.DEFAULT, if (bold) Typeface.BOLD else Typeface.NORMAL)
        if (stroke > 0f) { style = Paint.Style.STROKE; strokeWidth = stroke }
    }
    private fun nettoyerNom(value: String) = value.replace(Regex("[^A-Za-z0-9._-]"), "-").take(60)
}
