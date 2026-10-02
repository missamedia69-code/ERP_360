package com.missa.b360.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.missa.b360.ui.theme.BrandBlue
import com.missa.b360.ui.theme.MissaBorder
import com.missa.b360.ui.theme.MissaInk

/*
 * Boutons par défaut de l'application (design de référence « Informations sur votre entreprise ») :
 * rayon 12 dp, hauteur minimale 48 dp, bleu nuit plein sans ombre, désactivé = 35 % de la couleur.
 *
 * Les écrans importent ces fonctions sous les noms Material :
 *   import com.missa.b360.ui.components.BoutonMissa as Button
 *   import com.missa.b360.ui.components.BoutonContourMissa as OutlinedButton
 * ce qui applique le modèle partout sans réécrire chaque appel ; les paramètres restent surchargeables.
 */

/** Rayon commun des boutons et cartes de paramètres. */
val RayonBoutonMissa = RoundedCornerShape(12.dp)

@Composable
fun BoutonMissa(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RayonBoutonMissa,
    colors: ButtonColors = ButtonDefaults.buttonColors(
        containerColor = BrandBlue,
        contentColor = Color.White,
        disabledContainerColor = BrandBlue.copy(alpha = 0.35f),
        disabledContentColor = Color.White.copy(alpha = 0.9f),
    ),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(
        defaultElevation = 0.dp,
        pressedElevation = 0.dp,
        focusedElevation = 0.dp,
        hoveredElevation = 0.dp,
        disabledElevation = 0.dp,
    ),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content,
    )
}

@Composable
fun BoutonContourMissa(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RayonBoutonMissa,
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(contentColor = MissaInk),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = BorderStroke(1.dp, MissaBorder),
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content,
    )
}
