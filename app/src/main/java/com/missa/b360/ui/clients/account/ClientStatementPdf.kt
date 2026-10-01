package com.missa.b360.ui.clients.account

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.missa.b360.R
import com.missa.b360.core.data.entity.ClientEntity
import com.missa.b360.core.domain.model.AgingBucket
import com.missa.b360.core.domain.model.MentionsLegales
import com.missa.b360.core.domain.usecase.ClientAccount
import com.missa.b360.core.util.DateUtils
import com.missa.b360.ui.clients.components.clientMoney

private class LigneReleve(val gauche: String, val droite: String = "", val gras: Boolean = false, val avant: Float = 0f)

private const val LARGEUR = 595
private const val HAUTEUR = 842
private const val MARGE = 45f
private const val BAS = 800f
private const val INTERLIGNE = 18f

/**
 * Relevé de compte PDF : balance âgée, factures ouvertes et encaissements. Le contenu vient du
 * même [ClientAccount] que l'écran, donc les montants du relevé sont ceux que voit l'utilisateur.
 * Pagine automatiquement ; retourne `false` si le fichier n'a pas pu être écrit.
 */
internal fun Context.writeClientStatementPdf(
    uri: Uri,
    mentions: MentionsLegales,
    client: ClientEntity,
    devise: String,
    compte: ClientAccount,
    now: Long,
): Boolean = runCatching {
    val lignes = mutableListOf<LigneReleve>()
    lignes += LigneReleve(getString(R.string.cli_releve_titre), DateUtils.formatDate(now), gras = true)
    lignes += LigneReleve(client.nom, client.code, gras = true, avant = 6f)
    lignes += LigneReleve(client.telephone)
    lignes += LigneReleve(getString(R.string.cli_balance_agee), clientMoney(compte.balanceAgee.total, devise), gras = true, avant = 14f)
    for (tranche in AgingBucket.entries) {
        lignes += LigneReleve(getString(tranche.libelleReleve()), clientMoney(compte.balanceAgee.montant(tranche), devise))
    }
    lignes += LigneReleve(getString(R.string.cli_factures_ouvertes), "", gras = true, avant = 14f)
    if (compte.factures.isEmpty()) lignes += LigneReleve(getString(R.string.cli_aucune_facture_ouverte))
    for (facture in compte.factures) {
        val retard = if (facture.joursRetard > 0) "  (" + getString(R.string.cli_retard_jours, facture.joursRetard) + ")" else ""
        lignes += LigneReleve(
            DateUtils.formatDate(facture.issuedAt) + "  " + facture.reference.take(22) + retard,
            clientMoney(facture.outstanding, devise),
        )
    }
    lignes += LigneReleve(getString(R.string.cli_encaissements), "", gras = true, avant = 14f)
    if (compte.paiements.isEmpty()) lignes += LigneReleve(getString(R.string.cli_aucun_encaissement))
    for (paiement in compte.paiements) {
        lignes += LigneReleve(
            DateUtils.formatDate(paiement.paiementAt) + "  " + paiement.modePaiement,
            clientMoney(paiement.montant, devise),
        )
    }

    val titre = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(16, 28, 67); textSize = 20f; typeface = Typeface.DEFAULT_BOLD }
    val corps = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(16, 28, 67); textSize = 11f }
    val gras = Paint(corps).apply { typeface = Typeface.DEFAULT_BOLD }
    val petit = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(108, 122, 155); textSize = 10f }

    val document = PdfDocument()
    try {
        var numero = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(LARGEUR, HAUTEUR, numero).create())
        var y = MARGE + 14f
        page.canvas.drawText(mentions.nom, MARGE, y, titre)
        y += 18f
        for (ligne in mentions.lignes) {
            page.canvas.drawText(ligne, MARGE, y, petit)
            y += 13f
        }
        y += 14f
        for (ligne in lignes) {
            y += ligne.avant
            if (y > BAS) {
                document.finishPage(page)
                numero += 1
                page = document.startPage(PdfDocument.PageInfo.Builder(LARGEUR, HAUTEUR, numero).create())
                y = MARGE + 14f
            }
            val peinture = if (ligne.gras) gras else corps
            page.canvas.drawText(ligne.gauche, MARGE, y, peinture)
            if (ligne.droite.isNotEmpty()) {
                page.canvas.drawText(ligne.droite, LARGEUR - MARGE - peinture.measureText(ligne.droite), y, peinture)
            }
            y += INTERLIGNE
        }
        document.finishPage(page)
        contentResolver.openOutputStream(uri)?.use { document.writeTo(it) } ?: error("Flux de sortie indisponible")
    } finally {
        document.close()
    }
}.isSuccess

internal fun AgingBucket.libelleReleve(): Int = when (this) {
    AgingBucket.NON_ECHU -> R.string.cli_tranche_non_echu
    AgingBucket.JOURS_1_30 -> R.string.cli_tranche_1_30
    AgingBucket.JOURS_31_60 -> R.string.cli_tranche_31_60
    AgingBucket.JOURS_61_90 -> R.string.cli_tranche_61_90
    AgingBucket.PLUS_90 -> R.string.cli_tranche_plus_90
}
