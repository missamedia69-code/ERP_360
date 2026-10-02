package com.missa.b360.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.missa.b360.ui.theme.BrandBlue
import com.missa.b360.ui.theme.OnbConfigCard
import com.missa.b360.ui.theme.Red40

/**
 * Carte de section du kit de formulaires — la grammaire visuelle de l'écran « Informations sur
 * votre entreprise » : fond gris clair, titre numéroté ① ② ③, pastille « À compléter » à droite,
 * champs blancs contournés, toujours visibles (pas de section repliable).
 *
 * @param etiquette pastille à droite du titre (ex. « À compléter ») ; null = aucune
 * @param etiquetteEnErreur pastille rouge au lieu de la pastille de marque
 */
@Composable
fun MissaCarteSection(
    titre: String,
    modifier: Modifier = Modifier,
    numero: Int? = null,
    icone: Int? = null,
    sousTitre: String? = null,
    etiquette: String? = null,
    etiquetteEnErreur: Boolean = false,
    contenu: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        color = OnbConfigCard,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MissaFormSectionTitre(
                    titre = titre,
                    numero = numero,
                    sousTitre = sousTitre,
                    icone = if (numero == null) icone else null,
                    modifier = Modifier.weight(1f),
                )
                if (etiquette != null) {
                    Surface(
                        color = if (etiquetteEnErreur) Red40.copy(alpha = 0.08f) else BrandBlue.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(5.dp),
                    ) {
                        Text(
                            text = etiquette,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (etiquetteEnErreur) Red40 else BrandBlue,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.5.dp),
                        )
                    }
                }
            }
            contenu()
        }
    }
}
