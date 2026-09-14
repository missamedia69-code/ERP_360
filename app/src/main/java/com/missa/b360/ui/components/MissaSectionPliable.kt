package com.missa.b360.ui.components

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.missa.b360.ui.theme.BrandBlue
import com.missa.b360.ui.theme.MissaBorder
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted
import com.missa.b360.ui.theme.MissaSoftBlue
import com.missa.b360.ui.theme.MissaSurface
import com.missa.b360.ui.theme.Red40
import com.missa.b360.ui.theme.Red80

/**
 * Section repliable standard : une carte dont l'en-tête — icône de marque,
 * titre, résumé de ce qui est déjà rempli, étiquette d'état — s'ouvre et se
 * referme au clic.
 *
 * Elle sert à découper un écran long en blocs qui tiennent tous à l'écran une
 * fois refermés : replié, chaque bloc dit en une ligne ce qu'il contient, ce
 * qui évite de faire défiler une page entière pour retrouver un champ.
 *
 * @param resume ligne d'état affichée sous le titre (valeurs déjà saisies)
 * @param etiquette pastille de droite : « Facultatif », « À compléter »…
 * @param etiquetteEnErreur passe la pastille en rouge (saisie à corriger)
 * @param ouvrirDOffice force l'ouverture quand la valeur devient vraie : le
 *   contenu fautif ne peut pas rester caché derrière un en-tête replié
 */
@Composable
fun MissaSectionPliable(
    titre: String,
    icone: ImageVector,
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
        border = BorderStroke(1.dp, if (etiquetteEnErreur) Red40 else MissaBorder),
        colors = CardDefaults.cardColors(containerColor = MissaSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { ouvert = !ouvert }
                .padding(horizontal = 13.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(color = MissaSoftBlue, shape = RoundedCornerShape(9.dp)) {
                Icon(
                    imageVector = icone,
                    contentDescription = null,
                    tint = BrandBlue,
                    modifier = Modifier.padding(6.dp).size(17.dp),
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = titre,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MissaInk,
                )
                if (!resume.isNullOrBlank()) {
                    Text(
                        text = resume,
                        fontSize = 11.5.sp,
                        color = MissaMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (etiquette != null) {
                Spacer(Modifier.width(8.dp))
                Surface(
                    color = if (etiquetteEnErreur) Red80 else MissaSoftBlue,
                    shape = RoundedCornerShape(6.dp),
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
                imageVector = if (ouvert) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                contentDescription = null,
                tint = MissaMuted,
                modifier = Modifier.padding(start = 4.dp).size(20.dp),
            )
        }
        AnimatedVisibility(visible = ouvert) {
            Column(modifier = Modifier.fillMaxWidth()) {
                HorizontalDivider(color = MissaBorder)
                Spacer(Modifier.height(12.dp))
                Column(
                    modifier = Modifier.padding(horizontal = 13.dp),
                    content = contenu,
                )
                Spacer(Modifier.height(13.dp))
            }
        }
    }
}
