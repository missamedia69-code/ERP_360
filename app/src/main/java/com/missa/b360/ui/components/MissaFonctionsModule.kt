package com.missa.b360.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.missa.b360.R
import com.missa.b360.core.domain.model.ModuleCode
import com.missa.b360.ui.navigation.DestinationsFonctions
import com.missa.b360.ui.navigation.FonctionModule
import com.missa.b360.ui.theme.BrandBlue
import com.missa.b360.ui.theme.MissaBorder
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted
import com.missa.b360.ui.theme.MissaSurface
import com.missa.b360.ui.theme.ProfileGreen

/**
 * Sommaire des fonctionnalités d'un module, à placer en bas de son écran.
 *
 * Chaque module possède un catalogue de fonctionnalités défini au cahier des
 * charges. Cette section les présente **toutes**, en séparant celles qui
 * ouvrent aujourd'hui un écran de celles qui restent à livrer : masquer les
 * secondes ferait paraître le module plus pauvre qu'il n'est prévu, les rendre
 * cliquables mènerait à des impasses.
 *
 * S'ajoute à la fin d'une `LazyColumn` existante, sans rien déplacer de ce que
 * l'écran fait déjà.
 */
fun LazyListScope.sectionFonctionsModule(
    module: ModuleCode,
    onNaviguer: (String) -> Unit,
) {
    val disponibles = DestinationsFonctions.disponibles(module)
    val aVenir = DestinationsFonctions.aVenir(module)

    if (disponibles.isNotEmpty()) {
        item { TitreSectionModule(stringResource(R.string.mod_fonctions_disponibles)) }
        items(disponibles, key = { "fonction-" + it.libelle }) { fonction ->
            LigneFonction(fonction) { fonction.route?.let(onNaviguer) }
        }
    }
    if (aVenir.isNotEmpty()) {
        item {
            TitreSectionModule(stringResource(R.string.mod_fonctions_a_venir, aVenir.size))
        }
        item {
            // Un seul bloc de texte : une liste de lignes grisées se lirait
            // comme une série de boutons en panne.
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(13.dp),
                color = MissaSurface,
                border = BorderStroke(1.dp, MissaBorder),
            ) {
                Text(
                    text = aVenir.joinToString(" · ") { it.libelle },
                    fontSize = 11.5.sp,
                    color = MissaMuted,
                    modifier = Modifier.padding(13.dp),
                )
            }
        }
    }
}

@Composable
private fun LigneFonction(fonction: FonctionModule, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MissaSurface,
        border = BorderStroke(1.dp, MissaBorder),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = ProfileGreen,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = fonction.libelle,
                fontSize = 12.5.sp,
                color = MissaInk,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                contentDescription = null,
                tint = MissaMuted,
                modifier = Modifier.size(12.dp),
            )
        }
    }
}

@Composable
private fun TitreSectionModule(titre: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(width = 3.dp, height = 14.dp)
                .background(BrandBlue, RoundedCornerShape(2.dp)),
        )
        Spacer(Modifier.width(7.dp))
        Text(
            text = titre,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MissaInk,
        )
    }
}
