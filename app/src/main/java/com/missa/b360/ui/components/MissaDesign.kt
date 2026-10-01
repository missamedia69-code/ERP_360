package com.missa.b360.ui.components

import com.missa.b360.ui.icons.Iv
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.missa.b360.R
import com.missa.b360.ui.theme.MissaBorder
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted
import com.missa.b360.ui.theme.MissaSoftBlue

/**
 * Fondations visuelles communes aux écrans Missa Business 360.
 *
 * Les maquettes emploient systématiquement un canevas bleu très clair, des surfaces
 * blanches bordées et un bleu cobalt. Ces composants évitent que chaque module dérive
 * vers une interprétation différente de cette direction artistique.
 */
object MissaLayout {
    val screenHorizontal = 16.dp // Spec: 16dp marges
    val screenVertical = 12.dp
    val itemGap = 12.dp // Spec grille 4/8/12/16/20/24/32
    val sectionGap = 16.dp
    val fieldHeight = 52.dp
    val actionHeight = 48.dp // Spec: zone tactile 48dp minimum
    val cardRadius = 14.dp
}

/** Espacements cohérents basés sur la grille 4 dp. */
object Spacing {
    val xxxSmall = 4.dp
    val xxSmall = 8.dp
    val xSmall = 12.dp
    val small = 16.dp
    val medium = 24.dp
    val large = 32.dp
    val xLarge = 48.dp
}

/** Petit logo de marque utilisable dans les en-têtes sans alourdir les écrans métier. */
@Composable
fun MissaBrandMark(
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
) {
    Surface(
        modifier = modifier.size(size),
        shape = CircleShape,
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFF0288D1).copy(alpha = 0.25f)),
    ) {
        Image(
            painter = painterResource(R.drawable.logo_missa),
            contentDescription = stringResource(R.string.app_name),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().clip(CircleShape),
        )
    }
}

/**
 * Barre haute — Spec UI MISSA BUSINESS 360
 * - Hauteur contenu 64dp + statusBar inset séparé
 * - Padding horizontal 16dp minimum
 * - Zones tactiles 48x48, icônes 24dp, logo 40dp, titre 15-16sp
 * - Responsive: dp/sp, WindowInsets, pas de px
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissaTopAppBar(
    title: String,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    couleurFond: Color = Color.White,
    actions: @Composable RowScope.() -> Unit = {},
) {
    CenterAlignedTopAppBar(
        modifier = modifier,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MissaBrandMark(size = 40.dp)
                Spacer(Modifier.width(Spacing.xxSmall))
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack, modifier = Modifier.size(Spacing.actionHeight)) {
                    Icon(
                        painter = painterResource(Iv.ArrowBack),
                        contentDescription = stringResource(R.string.back_button),
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = couleurFond,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        windowInsets = androidx.compose.foundation.layout.WindowInsets.statusBars,
    )
}

/** Carte de contenu neutre, compacte et lisible employée dans les modules génériques. */
@Composable
fun MissaPanel(
    modifier: Modifier = Modifier,
    accent: Color? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(MissaLayout.cardRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, accent?.copy(alpha = .30f) ?: MissaBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(Spacing.medium),
            verticalArrangement = Arrangement.spacedBy(Spacing.xSmall),
            content = content,
        )
    }
}

/** En-tête de section à employer au-dessus d'un groupe dense de données ou d'actions. */
@Composable
fun MissaSectionTitle(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            subtitle?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(Spacing.xxxSmall))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        trailing?.let {
            Spacer(Modifier.width(Spacing.xxSmall))
            it()
        }
    }
}

/** État vide cohérent : pictogramme doux, titre, explication et action éventuelle. */
@Composable
fun MissaEmptyState(
    icon: Int,
    title: String,
    description: String? = null,
    modifier: Modifier = Modifier,
    action: @Composable (() -> Unit)? = null,
) {
    MissaPanel(modifier = modifier, accent = MaterialTheme.colorScheme.primary) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xxSmall),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.xxSmall),
        ) {
            Surface(
                modifier = Modifier.size(Spacing.xLarge), // 42.dp -> 48.dp? Let's use xLarge (48.dp) for consistency
                shape = CircleShape,
                color = MissaSoftBlue,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(painterResource(icon), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(Spacing.xLarge))
                }
            }
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center,
            )
            description?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                )
            }
            action?.invoke()
        }
    }
}