package com.missa.b360.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.SpannableString
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.StyleSpan
import java.io.File

/**
 * Aperçu d'impression de la fiche entreprise au **format A5 portrait**, avec le
 * logo de l'entreprise en en-tête (jamais celui de MISSA) et un document bien
 * structuré : en-tête papier à lettres (logo + nom + titre + date), sections
 * titrées en bandeau bleu clair, éléments en puces bleues « Libellé : valeur »
 * avec retour à la ligne propre, pied de page (note + pagination) et pagination
 * automatique. Généré 100 % hors-ligne via `android.graphics.pdf`.
 */
object FicheEntreprisePdf {

    data class Ligne(val libelle: String, val valeur: String)
    data class Section(val titre: String, val lignes: List<Ligne>)

    /** A5 portrait en points (72 dpi) : 148 × 210 mm. */
    private const val LARGEUR = 420f
    private const val HAUTEUR = 595f
    private const val MARGE = 32f

    private const val ENCRE = 0xFF101C43.toInt()
    private const val BLEU = 0xFF1554E8.toInt()
    private const val GRIS = 0xFF65718F.toInt()
    private const val BLEU_CLAIR = 0xFFEFF4FF.toInt()
    private const val TRAIT_LEGER = 0xFFE1E8F5.toInt()

    fun generer(
        context: Context,
        nomEntreprise: String,
        titreDoc: String,
        dateTexte: String,
        sections: List<Section>,
        logo: Bitmap? = null,
        piedNote: String? = null,
    ): File {
        val document = PdfDocument()
        var numero = 1
        var page = demarrerPage(document, numero)
        var canvas = page.canvas
        var y = MARGE

        val paintPied = Paint().apply { color = GRIS; textSize = 7.5f; isAntiAlias = true }
        val paintTraitPied = Paint().apply { color = TRAIT_LEGER; strokeWidth = 0.8f; isAntiAlias = true }

        fun piedDePage(c: android.graphics.Canvas, num: Int) {
            c.drawLine(MARGE, HAUTEUR - 26f, LARGEUR - MARGE, HAUTEUR - 26f, paintTraitPied)
            piedNote?.let { c.drawText(it, MARGE, HAUTEUR - 14f, paintPied) }
            val pageTexte = "Page $num"
            c.drawText(pageTexte, LARGEUR - MARGE - paintPied.measureText(pageTexte), HAUTEUR - 14f, paintPied)
        }

        fun nouvellePage() {
            piedDePage(canvas, numero)
            document.finishPage(page)
            numero += 1
            page = demarrerPage(document, numero)
            canvas = page.canvas
            y = MARGE
        }

        fun espace(besoin: Float) {
            if (y + besoin > HAUTEUR - 44f) nouvellePage()
        }

        val paintTitre = Paint().apply {
            color = ENCRE
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val paintSousTitre = Paint().apply {
            color = BLEU
            textSize = 10.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val paintDate = Paint().apply { color = GRIS; textSize = 9f; isAntiAlias = true }
        val paintSection = Paint().apply {
            color = BLEU
            textSize = 11.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val paintFondSection = Paint().apply {
            color = BLEU_CLAIR
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val paintLigne = TextPaint().apply { color = ENCRE; textSize = 10.5f; isAntiAlias = true }
        val paintTrait = Paint().apply { color = BLEU; strokeWidth = 1.2f; isAntiAlias = true }
        val paintPuce = Paint().apply { color = BLEU; style = Paint.Style.FILL; isAntiAlias = true }

        // En-tête papier à lettres : logo de l'entreprise à gauche,
        // nom + titre + date à droite, trait de séparation dessous.
        val coteLogo = 48f
        val xTexte = if (logo != null) MARGE + coteLogo + 12f else MARGE
        if (logo != null) {
            val dst = RectF(MARGE, y, MARGE + coteLogo, y + coteLogo)
            val arrondi = Path().apply { addRoundRect(dst, 11f, 11f, Path.Direction.CCW) }
            canvas.save()
            canvas.clipPath(arrondi)
            canvas.drawBitmap(logo, null, dst, null)
            canvas.restore()
        }
        canvas.drawText(nomEntreprise, xTexte, y + 17f, paintTitre)
        canvas.drawText(titreDoc, xTexte, y + 31f, paintSousTitre)
        canvas.drawText(dateTexte, xTexte, y + 44f, paintDate)
        y += coteLogo + 12f
        canvas.drawLine(MARGE, y, LARGEUR - MARGE, y, paintTrait)
        y += 16f

        for (section in sections) {
            espace(44f)
            // Titre de section en bandeau bleu clair pleine largeur.
            val bandeau = RectF(MARGE, y, LARGEUR - MARGE, y + 18f)
            canvas.drawRoundRect(bandeau, 4f, 4f, paintFondSection)
            canvas.drawText(section.titre, MARGE + 7f, y + 13f, paintSection)
            y += 24f
            for (ligne in section.lignes) {
                val texte = "${ligne.libelle} : ${ligne.valeur}"
                val span = SpannableString(texte).apply {
                    setSpan(
                        StyleSpan(Typeface.BOLD),
                        0,
                        (ligne.libelle.length + 3).coerceAtMost(texte.length),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                    )
                }
                val largeurDispo = (LARGEUR - MARGE * 2 - 14f).toInt()
                val bloc = StaticLayout.Builder
                    .obtain(span, 0, span.length, paintLigne, largeurDispo)
                    .build()
                val hauteurBloc = bloc.height + 6f
                espace(hauteurBloc + 6f)
                canvas.drawCircle(MARGE + 3f, y + 5.5f, 2.2f, paintPuce)
                canvas.save()
                canvas.translate(MARGE + 14f, y)
                bloc.draw(canvas)
                canvas.restore()
                y += hauteurBloc
            }
            y += 10f
        }

        piedDePage(canvas, numero)
        document.finishPage(page)

        val dossier = File(context.cacheDir, "fiches_pdf").apply { mkdirs() }
        val fichier = File(dossier, "fiche-${System.currentTimeMillis()}.pdf")
        val flux = java.io.FileOutputStream(fichier)
        try {
            document.writeTo(flux)
        } finally {
            flux.close()
        }
        document.close()
        return fichier
    }

    private fun demarrerPage(document: PdfDocument, numero: Int): PdfDocument.Page =
        document.startPage(
            PdfDocument.PageInfo.Builder(LARGEUR.toInt(), HAUTEUR.toInt(), numero).create(),
        )
}
