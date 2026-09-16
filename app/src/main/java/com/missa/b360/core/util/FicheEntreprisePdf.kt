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
 * structuré : lettre à en-tête (logo + nom + titre + date), trait de
 * séparation, sections titrées au trait bleu, éléments en puces bleues
 * « Libellé : valeur » avec retour à la ligne propre et pagination automatique.
 * Généré 100 % hors-ligne via `android.graphics.pdf`, aucune dépendance tierce.
 */
object FicheEntreprisePdf {

    data class Ligne(val libelle: String, val valeur: String)
    data class Section(val titre: String, val lignes: List<Ligne>)

    /** A5 portrait en points (72 dpi) : 148 × 210 mm. */
    private const val LARGEUR = 420f
    private const val HAUTEUR = 595f
    private const val MARGE = 36f

    private const val ENCRE = 0xFF101C43.toInt()
    private const val BLEU = 0xFF1554E8.toInt()
    private const val GRIS = 0xFF65718F.toInt()

    fun generer(
        context: Context,
        nomEntreprise: String,
        titreDoc: String,
        dateTexte: String,
        sections: List<Section>,
        logo: Bitmap? = null,
    ): File {
        val document = PdfDocument()
        var numero = 1
        var page = demarrerPage(document, numero)
        var canvas = page.canvas
        var y = MARGE

        fun nouvellePage() {
            document.finishPage(page)
            numero += 1
            page = demarrerPage(document, numero)
            canvas = page.canvas
            y = MARGE
        }

        fun espace(besoin: Float) {
            if (y + besoin > HAUTEUR - MARGE) nouvellePage()
        }

        val paintTitre = Paint().apply {
            color = ENCRE
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val paintSousTitre = Paint().apply {
            color = BLEU
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val paintDate = Paint().apply { color = GRIS; textSize = 8.5f; isAntiAlias = true }
        val paintSection = Paint().apply {
            color = BLEU
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val paintLigne = TextPaint().apply { color = ENCRE; textSize = 9.5f; isAntiAlias = true }
        val paintTrait = Paint().apply { color = BLEU; strokeWidth = 1f; isAntiAlias = true }
        val paintPuce = Paint().apply { color = BLEU; style = Paint.Style.FILL; isAntiAlias = true }

        // En-tête façon papier à lettres : logo de l'entreprise à gauche,
        // nom + titre + date à droite, trait de séparation dessous.
        val coteLogo = 40f
        val xTexte = if (logo != null) MARGE + coteLogo + 10f else MARGE
        if (logo != null) {
            val dst = RectF(MARGE, y, MARGE + coteLogo, y + coteLogo)
            val arrondi = Path().apply { addRoundRect(dst, 9f, 9f, Path.Direction.CCW) }
            canvas.save()
            canvas.clipPath(arrondi)
            canvas.drawBitmap(logo, null, dst, null)
            canvas.restore()
        }
        canvas.drawText(nomEntreprise, xTexte, y + 15f, paintTitre)
        canvas.drawText(titreDoc, xTexte, y + 27f, paintSousTitre)
        canvas.drawText(dateTexte, xTexte, y + 38f, paintDate)
        y += coteLogo + 10f
        canvas.drawLine(MARGE, y, LARGEUR - MARGE, y, paintTrait)
        y += 14f

        for (section in sections) {
            espace(36f)
            canvas.drawText(section.titre, MARGE, y + 11f, paintSection)
            y += 15f
            canvas.drawLine(MARGE, y, MARGE + 110f, y, paintTrait)
            y += 11f
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
                val largeurDispo = (LARGEUR - MARGE * 2 - 12f).toInt()
                val bloc = StaticLayout.Builder
                    .obtain(span, 0, span.length, paintLigne, largeurDispo)
                    .build()
                val hauteurBloc = bloc.height + 4f
                espace(hauteurBloc + 6f)
                canvas.drawCircle(MARGE + 2.5f, y + 5f, 2f, paintPuce)
                canvas.save()
                canvas.translate(MARGE + 12f, y)
                bloc.draw(canvas)
                canvas.restore()
                y += hauteurBloc
            }
            y += 8f
        }

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
