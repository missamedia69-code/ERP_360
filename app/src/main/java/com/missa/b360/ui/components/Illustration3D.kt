package com.missa.b360.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.missa.b360.R
import com.missa.b360.ui.navigation.AppModule

/**
 * Illustration 3D unifiée – Charte Missa Business 360
 *
 * Style : isométrique minimaliste, socle blanc #FFFFFF, lumière haut-gauche, 30°,
 * palette Bleu Missa #007BFF + accents module (vert Stock, orange Production...)
 *
 * Les PNG sont issus de docs/charte-3d-*.png, copiés en drawable/illustration_*.png
 * pour intégration directe. Quand un module évoluera, son illustration sera déjà prête.
 */
object Illustration3D {

    @DrawableRes
    fun forModule(module: AppModule): Int? = when (module) {
        AppModule.VENTE -> R.drawable.illustration_ventes
        AppModule.STOCK -> R.drawable.illustration_stock
        AppModule.PRODUCTION -> R.drawable.illustration_production
        AppModule.FINANCES -> R.drawable.illustration_compta
        AppModule.COMPTABILITE -> R.drawable.illustration_compta
        AppModule.TRESORERIE -> R.drawable.illustration_tresorerie
        AppModule.CLIENTS -> R.drawable.illustration_clients
        AppModule.ACHATS -> R.drawable.illustration_achats
        AppModule.FOURNISSEURS -> R.drawable.illustration_fournisseurs
        AppModule.LIVRAISON -> R.drawable.illustration_logistique
        AppModule.LOGISTIQUE -> R.drawable.illustration_logistique
        AppModule.SERVICES -> R.drawable.illustration_services
        AppModule.RH -> R.drawable.illustration_rh
        AppModule.PROJETS -> R.drawable.illustration_projets
        AppModule.CRM -> R.drawable.illustration_crm
        AppModule.QUALITE -> R.drawable.illustration_qualite
        AppModule.MAINTENANCE -> R.drawable.illustration_maintenance
        AppModule.REPORTING -> R.drawable.illustration_reporting
        else -> null
    }

    @DrawableRes
    fun forDashboardKpiVentes(): Int = R.drawable.illustration_ventes
    @DrawableRes
    fun forDashboardKpiAchats(): Int = R.drawable.illustration_stock
    @DrawableRes
    fun forDashboardKpiTresorerie(): Int = R.drawable.illustration_tresorerie
    @DrawableRes
    fun forDashboardKpiClients(): Int = R.drawable.illustration_clients
}

/**
 * Fond 3D plein cadre – remplit tout l'arrière-plan et rogne les parties hors-cadre.
 * Usage : dans une Box, mettre en premier avec Modifier.matchParentSize()
 * Ex: Image avec ContentScale.Crop qui couvre toute la carte KPI.
 */
@Composable
fun IllustrationFond(
    @DrawableRes drawableRes: Int,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    alpha: Float = 0.14f,
) {
    Image(
        painter = painterResource(id = drawableRes),
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop, // remplit le cadre, rogne ce qui dépasse
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .alpha(alpha),
    )
}

/**
 * Alias historique – même comportement plein cadre avec crop.
 * Gardé pour compatibilité, préférer IllustrationFond.
 */
@Composable
fun IllustrationCrop(
    @DrawableRes drawableRes: Int,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    alpha: Float = 0.14f,
    cropSize: androidx.compose.ui.unit.Dp = 88.dp, // ignoré, conservé pour compat API
    alignment: androidx.compose.ui.Alignment = androidx.compose.ui.Alignment.TopEnd, // ignoré
) {
    IllustrationFond(
        drawableRes = drawableRes,
        contentDescription = contentDescription,
        modifier = modifier,
        alpha = alpha,
    )
}
