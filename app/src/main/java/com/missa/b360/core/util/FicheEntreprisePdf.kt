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
 * titrées en bandeau bleu clair, éléments en puces bleues « Libellé : valeur »,
 * pied de page (note + pagination).
 *
 * **Une seule page garantie** : la hauteur totale est d'abord mesurée, puis une
 * échelle (≤ 1, plancher 0,5) est appliquée à toutes les tailles jusqu'à ce que
 * le document tienne sur la page A5. La pagination reste en filet de sécurité.
 * Généré 100 % hors-ligne via `android.graphics.pdf`.
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
        // --- Mesure : hauteur exacte du document pour une échelle donnée. ---
        fun ligneSpannee(ligne: Ligne): SpannableString {
            val texte = "${ligne.libelle} : ${ligne.valeur}"
            return SpannableString(texte).apply {
                setSpan(
                    StyleSpan(Typeface.BOLD),
                    0,
                    (ligne.libelle.length + 3).coerceAtMost(texte.length),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                )
            }
        }

        fun blocLigne(ligne: Ligne, echelle: Float): StaticLayout = StaticLayout.Builder
            .obtain(
                ligneSpannee(ligne),
                0,
                ligneSpannee(ligne).length,
                TextPaint().apply { color = ENCRE; textSize = 10.5f * echelle; isAntiAlias = true },
                (LARGEUR - MARGE * 2 - 14f * echelle).toInt(),
            )
            .build()

        fun hauteurDocument(echelle: Float): Float {
            // En-tête : logo + retour + trait + respiration.
            var h = 48f * echelle + 12f * echelle + 16f * echelle
            for (section in sections) {
                h += 24f * echelle // bandeau + espacement
                for (ligne in section.lignes) {
                    h += blocLigne(ligne, echelle).height + 6f * echelle
                }
                h += 10f * echelle // respiration inter-sections
            }
            return h
        }

        // Réduction progressive jusqu'à tenir sur la page (plancher 0,5).
        val disponible = HAUTEUR - MARGE - 44f
        var echelle = 1f
        while (hauteurDocument(echelle) > disponible && echelle > 0.5f) {
            echelle = (echelle - 0.05f).coerceAtLeast(0.5f)
        }
        val f = echelle

        // --- Rendu. ---
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
            textSize = 18f * f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val paintSousTitre = Paint().apply {
            color = BLEU
            textSize = 10.5f * f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val paintDate = Paint().apply { color = GRIS; textSize = 9f * f; isAntiAlias = true }
        val paintSection = Paint().apply {
            color = BLEU
            textSize = 11.5f * f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val paintFondSection = Paint().apply {
            color = BLEU_CLAIR
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val paintLigne = TextPaint().apply { color = ENCRE; textSize = 10.5f * f; isAntiAlias = true }
        val paintTrait = Paint().apply { color = BLEU; strokeWidth = 1.2f; isAntiAlias = true }
        val paintPuce = Paint().apply { color = BLEU; style = Paint.Style.FILL; isAntiAlias = true }

        // En-tête papier à lettres.
        val coteLogo = 48f * f
        val xTexte = if (logo != null) MARGE + coteLogo + 12f * f else MARGE
        if (logo != null) {
            val dst = RectF(MARGE, y, MARGE + coteLogo, y + coteLogo)
            val arrondi = Path().apply { addRoundRect(dst, 11f * f, 11f * f, Path.Direction.CCW) }
            canvas.save()
            canvas.clipPath(arrondi)
            canvas.drawBitmap(logo, null, dst, null)
            canvas.restore()
        }
        canvas.drawText(nomEntreprise, xTexte, y + 17f * f, paintTitre)
        canvas.drawText(titreDoc, xTexte, y + 31f * f, paintSousTitre)
        canvas.drawText(dateTexte, xTexte, y + 44f * f, paintDate)
        y += coteLogo + 12f * f
        canvas.drawLine(MARGE, y, LARGEUR - MARGE, y, paintTrait)
        y += 16f * f

        for (section in sections) {
            espace(44f * f)
            val bandeau = RectF(MARGE, y, LARGEUR - MARGE, y + 18f * f)
            canvas.drawRoundRect(bandeau, 4f * f, 4f * f, paintFondSection)
            canvas.drawText(section.titre, MARGE + 7f * f, y + 13f * f, paintSection)
            y += 24f * f
            for (ligne in section.lignes) {
                val span = ligneSpannee(ligne)
                val bloc = StaticLayout.Builder
                    .obtain(span, 0, span.length, paintLigne, (LARGEUR - MARGE * 2 - 14f * f).toInt())
                    .build()
                val hauteurBloc = bloc.height + 6f * f
                espace(hauteurBloc + 6f * f)
                canvas.drawCircle(MARGE + 3f * f, y + 5.5f * f, 2.2f * f, paintPuce)
                canvas.save()
                canvas.translate(MARGE + 14f * f, y)
                bloc.draw(canvas)
                canvas.restore()
                y += hauteurBloc
            }
            y += 10f * f
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
