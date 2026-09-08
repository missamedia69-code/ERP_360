package com.missa.b360.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.missa.b360.R
import com.missa.b360.core.domain.model.ModuleCode

/**
 * Illustrations d'arrière-plan des écrans.
 *
 * Chaque module reçoit un dessin en rapport avec son métier : un entrepôt pour
 * le stock, une caisse pour la vente, un coffre pour la trésorerie. Le trait
 * est le même partout — ligne fine gris-bleu, aucun aplat — pour que
 * l'ensemble reste une seule famille visuelle et non une collection d'images.
 *
 * Six dessins couvrent les dix-huit modules : au-delà, la variété deviendrait
 * du bruit, et plusieurs modules partagent de toute façon le même univers.
 */
enum class Filigrane(val ressource: Int) {
    ENTREPOT(R.drawable.fond_entrepot),
    DOCUMENTS(R.drawable.fond_documents),
    CAISSE(R.drawable.fond_caisse),
    COFFRE(R.drawable.fond_coffre),
    EQUIPE(R.drawable.fond_equipe),
    OUTILS(R.drawable.fond_outils),
    ;

    companion object {
        /** Dessin retenu pour un module métier. */
        fun pour(module: ModuleCode): Filigrane = when (module) {
            ModuleCode.STK, ModuleCode.LOG -> ENTREPOT
            ModuleCode.VEN, ModuleCode.ACH -> CAISSE
            ModuleCode.TRE -> COFFRE
            ModuleCode.CPT, ModuleCode.REP, ModuleCode.PRJ -> DOCUMENTS
            ModuleCode.CRM, ModuleCode.RH, ModuleCode.SER -> EQUIPE
            ModuleCode.MAI, ModuleCode.QUA, ModuleCode.PRO -> OUTILS
        }
    }
}

/**
 * Pose un filigrane derrière le contenu d'un écran.
 *
 * L'image est ancrée en bas à droite et très pâle : elle habille la zone vide
 * qu'un écran presque sans données laisse forcément, sans jamais concurrencer
 * le texte. C'est aussi pourquoi elle n'occupe qu'une fraction de la largeur —
 * un fond plein écran, même léger, fatigue la lecture.
 */
@Composable
fun MissaFondFiligrane(
    filigrane: Filigrane,
    modifier: Modifier = Modifier,
    alpha: Float = 0.22f,
    largeur: Float = 0.62f,
    alignement: Alignment = Alignment.BottomEnd,
    contenu: @Composable BoxScope.() -> Unit,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Image(
            painter = painterResource(filigrane.ressource),
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
            alpha = alpha,
            modifier = Modifier
                .align(alignement)
                .fillMaxWidth(largeur),
        )
        contenu()
    }
}
