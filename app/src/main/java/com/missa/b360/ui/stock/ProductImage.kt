package com.missa.b360.ui.stock

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Photo compacte d'un produit à côté du logo de catégorie.
 * Ne rend rien si l'article n'a pas d'image (`photoPath` nul ou fichier absent).
 */
@Composable
fun ProduitImage(
    photoPath: String?,
    modifier: Modifier = Modifier,
    taille: Dp = 44.dp,
    arrondi: Dp = 12.dp,
) {
    val photo = rememberPhotoProduit(photoPath) ?: return
    Image(
        bitmap = photo,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(taille)
            .clip(RoundedCornerShape(arrondi)),
    )
}
