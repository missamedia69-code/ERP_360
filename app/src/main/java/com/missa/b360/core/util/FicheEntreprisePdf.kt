package com.missa.b360.core.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.SpannableString
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.StyleSpan
import java.io.File

/**
 * Génère la fiche entreprise en PDF A4 portrait via `android.graphics.pdf`
 * (aucune dépendance tierce) : en-tête nom + titre + date, sections titrées
 * au trait bleu, éléments en puces bleues « Libellé : valeur » avec retour à
 * la ligne propre (StaticLayout) et pagination automatique.
 */
object FicheEntreprisePdf {

    data class Ligne(val libelle: String, val valeur: String)
    data class Section(val titre: String, val lignes: List<Ligne>)

    /** A4 portrait en points (72 dpi). */
    private const val LARGEUR = 595f
    private const val HAUTEUR = 842f
    private const val MARGE = 48f

    private const val ENCRE = 0xFF101C43.toInt()
    private const val BLEU = 0xFF1554E8.toInt()
    private const val GRIS = 0xFF65718F.toInt()

    fun generer(
        context: Context,
        nomEntreprise: String,
        titreDoc: String,
        dateTexte: String,
        sections: List<Section>,
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
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val paintSousTitre = Paint().apply {
            color = BLEU
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val paintDate = Paint().apply { color = GRIS; textSize = 9.5f; isAntiAlias = true }
        val paintSection = Paint().apply {
            color = BLEU
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val paintLigne = TextPaint().apply { color = ENCRE; textSize = 10.5f; isAntiAlias = true }
        val paintTrait = Paint().apply { color = BLEU; strokeWidth = 1.2f; isAntiAlias = true }
        val paintPuce = Paint().apply { color = BLEU; style = Paint.Style.FILL; isAntiAlias = true }

        // En-tête du document : nom, titre, date, trait de séparation.
        canvas.drawText(nomEntreprise, MARGE, y + 20f, paintTitre)
        y += 26f
        canvas.drawText(titreDoc, MARGE, y + 11f, paintSousTitre)
        y += 16f
        canvas.drawText(dateTexte, MARGE, y + 9.5f, paintDate)
        y += 18f
        canvas.drawLine(MARGE, y, LARGEUR - MARGE, y, paintTrait)
        y += 18f

        for (section in sections) {
            espace(44f)
            canvas.drawText(section.titre, MARGE, y + 13f, paintSection)
            y += 18f
            canvas.drawLine(MARGE, y, MARGE + 140f, y, paintTrait)
            y += 14f
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
                espace(hauteurBloc + 8f)
                canvas.drawCircle(MARGE + 3f, y + 6f, 2.4f, paintPuce)
                canvas.save()
                canvas.translate(MARGE + 14f, y)
                bloc.draw(canvas)
                canvas.restore()
                y += hauteurBloc
            }
            y += 10f
        }

        document.finishPage(page)

        val dossier = File(context.cacheDir, "fiches_pdf").apply { mkdirs() }
        val fichier = File(dossier, "fiche-${System.currentTimeMillis()}.pdf")
        val flux = java.io.FileOutputStream(fichier)
        try {
            document.write(flux)
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
