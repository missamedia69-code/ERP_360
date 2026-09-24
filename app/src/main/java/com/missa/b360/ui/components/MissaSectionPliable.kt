package com.missa.b360.ui.components

import com.missa.b360.R

import androidx.compose.ui.res.painterResource

import com.missa.b360.ui.icons.Iv
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.missa.b360.ui.theme.BrandBlue
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted
import com.missa.b360.ui.theme.MissaSurface
import com.missa.b360.ui.theme.OnbConfigCard
import com.missa.b360.ui.theme.Red40
import com.missa.b360.ui.theme.Red80

/**
 * Section repliable standard (style maquette onboarding) : carte bleu clair
 * pleine sans bordure, titre + résumé, étiquette d'état et chevron. L'en-tête
 * s'ouvre et se referme au clic.
 *
 * Elle sert à découper un écran long en blocs qui tiennent tous à l'écran une
 * fois refermés : replié, chaque bloc dit en une ligne ce qu'il contient.
 *
 * @param resume ligne d'état affichée sous le titre (valeurs déjà saisies)
 * @param etiquette pastille de droite : « Facultatif », « À compléter »…
 * @param etiquetteEnErreur passe la pastille et le contour en rouge
 * @param ouvrirDOffice force l'ouverture quand la valeur devient vraie
 */
@Composable
fun MissaSectionPliable(
    titre: String,
    icone: Int,
    modifier: Modifier = Modifier,
    resume: String? = null,
    etiquette: String? = null,
    etiquetteEnErreur: Boolean = false,
    ouvertParDefaut: Boolean = false,
    ouvrirDOffice: Boolean = false,
    contenu: @Composable ColumnScope.() -> Unit,
) {
    var ouvert by rememberSaveable(titre) { mutableStateOf(ouvertParDefaut) }
    LaunchedEffect(ouvrirDOffice) {
        if (ouvrirDOffice) ouvert = true
    }
    Card(
        shape = RoundedCornerShape(16.dp),
        border = if (etiquetteEnErreur) BorderStroke(1.dp, Red40) else null,
        colors = CardDefaults.cardColors(containerColor = OnbConfigCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { ouvert = !ouvert }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(color = BrandBlue.copy(alpha = 0.12f), shape = RoundedCornerShape(10.dp)) {
                Icon(
                    painter = painterResource(icone),
                    contentDescription = null,
                    tint = BrandBlue,
                    modifier = Modifier.padding(6.dp).size(17.dp),
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = titre,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MissaInk,
                )
                if (!resume.isNullOrBlank()) {
                    Text(
                        text = resume,
                        fontSize = 12.sp,
                        color = MissaMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (etiquette != null) {
                Spacer(Modifier.width(8.dp))
                Surface(
                    color = if (etiquetteEnErreur) Red80 else MissaSurface,
                    shape = RoundedCornerShape(7.dp),
                ) {
                    Text(
                        text = etiquette,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (etiquetteEnErreur) Red40 else BrandBlue,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
            Icon(
                painter = painterResource(if (ouvert) Iv.ExpandLess else Iv.ExpandMore),
                contentDescription = null,
                tint = MissaMuted,
                modifier = Modifier.padding(start = 4.dp).size(20.dp),
            )
        }
        AnimatedVisibility(visible = ouvert) {
            Column(modifier = Modifier.fillMaxWidth()) {
                HorizontalDivider(color = BrandBlue.copy(alpha = 0.14f))
                Spacer(Modifier.height(12.dp))
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    content = contenu,
                )
                Spacer(Modifier.height(14.dp))
            }
        }
    }
}
