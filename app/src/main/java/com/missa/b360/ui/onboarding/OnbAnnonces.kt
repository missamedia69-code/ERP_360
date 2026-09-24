package com.missa.b360.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.missa.b360.R
import com.missa.b360.ui.icons.Iv
import com.missa.b360.ui.theme.BrandBlue
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaLime
import com.missa.b360.ui.theme.MissaMuted
import com.missa.b360.ui.theme.MissaSurface
import com.missa.b360.ui.theme.OnboardingHeroBlue

/**
 * Écran 2 — Annonces : présenté juste après le choix de la langue (maquette).
 *
 * Bandeau bleu de marque (logo, nom, slogan, accroche et illustration), carte
 * « Une application, plusieurs avantages » (six briques), rappel de l'essai
 * gratuit et bouton « Commencer ». Le lien « Passer » court-circuite la
 * lecture mais conduit au même endroit : la configuration initiale.
 */
@Composable
internal fun OnbAnnonces(viewModel: OnboardingViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MissaSurface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = viewModel::precedent,
                modifier = Modifier.size(38.dp),
            ) {
                Icon(
                    painter = painterResource(Iv.ArrowBack),
                    contentDescription = stringResource(R.string.ob_retour),
                    tint = MissaMuted,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.weight(1f))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(
                        onClickLabel = stringResource(R.string.obn_ann_passer),
                        role = Role.Button,
                        onClick = viewModel::suivant,
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.obn_ann_passer),
                    color = BrandBlue,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.width(3.dp))
                Icon(
                    painter = painterResource(Iv.ChevronRight),
                    contentDescription = null,
                    tint = BrandBlue,
                    modifier = Modifier.size(14.dp),
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            OnbAnnoncesHero()
            OnbAvantagesCarte()
            OnbEssaiBanniere()
            OnbAnnoncesBoutonCommencer(onClick = viewModel::suivant)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp, Alignment.CenterHorizontally),
        ) {
            OnbDots(
                total = OnboardingStep.TERMINE.ordinal,
                active = viewModel.step.ordinal,
            )
        }
    }
}

/**
 * Bandeau bleu : dégradé royal qui se fond dans le blanc de la page, tuile
 * logo, nom de la marque, slogan, trait vert signature et illustration des
 * écrans (ordinateur + téléphone + indicateur de progression).
 */
@Composable
private fun OnbAnnoncesHero() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(330.dp)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        OnboardingHeroBlue,
                        BrandBlue,
                        Color(0xFFDCE8FF),
                    ),
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 24.dp, top = 22.dp, end = 132.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF13229B)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.logo_missa),
                    contentDescription = stringResource(R.string.app_name),
                    tint = Color.White,
                    modifier = Modifier.size(32.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.app_name).uppercase(),
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
            )
            Text(
                text = stringResource(R.string.obn_ann_slogan),
                color = Color.White,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
            )
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MissaLime),
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = stringResource(R.string.obn_ann_paragraphe),
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 12.sp,
                lineHeight = 17.sp,
            )
        }
        // Illustration : écran d'ordinateur avec graphique en barres, téléphone
        // posé devant et indicateur de progression flottant — composé aux
        // couleurs de la marque (aucun asset binaire).
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 22.dp)
                .width(158.dp)
                .height(168.dp),
        ) {
        // Ordinateur : écran + socle.
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(y = 14.dp)
                .width(138.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .width(118.dp)
                    .height(74.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OnbAnnoncesBarre(14.dp, 20.dp, Color(0xFFAFC8FF))
                    OnbAnnoncesBarre(14.dp, 34.dp, Color(0xFF6E96F5))
                    OnbAnnoncesBarre(14.dp, 26.dp, Color(0xFF3D6DF0))
                    OnbAnnoncesBarre(14.dp, 44.dp, BrandBlue)
                }
            }
            Spacer(Modifier.height(2.dp))
            Box(
                modifier = Modifier
                    .width(138.dp)
                    .height(7.dp)
                    .clip(RoundedCornerShape(3.5.dp))
                    .background(Color(0xFFD9E6FF)),
            )
        }
        // Téléphone posé devant l'ordinateur.
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = -12.dp, y = -2.dp)
                .width(48.dp)
                .height(92.dp)
                .shadow(8.dp, RoundedCornerShape(13.dp))
                .clip(RoundedCornerShape(13.dp))
                .background(Color.White)
                .padding(7.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF22C55E)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(Iv.Check),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(10.dp),
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(1.5.dp))
                        .background(Color(0xFFDCE6F7)),
                )
                Box(
                    modifier = Modifier
                        .width(30.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(1.5.dp))
                        .background(Color(0xFFE6EEFA)),
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(Color(0xFFEAF1FF)),
                )
                Spacer(Modifier.weight(1f))
            }
        }
        // Indicateur de progression flottant.
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(34.dp)
                .shadow(8.dp, RoundedCornerShape(10.dp))
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(Iv.TrendingUp),
                contentDescription = null,
                tint = Color(0xFF16A34A),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
}

@Composable
private fun OnbAnnoncesBarre(largeur: androidx.compose.ui.unit.Dp, hauteur: androidx.compose.ui.unit.Dp, couleur: Color) {
    Box(
        modifier = Modifier
            .width(largeur)
            .height(hauteur)
            .clip(RoundedCornerShape(4.dp))
            .background(couleur),
    )
}

/** Les six briques mises en avant sur la page d'annonces (maquette). */
private data class OnbAvantage(
    val icone: Int,
    val fond: Color,
    val couleur: Color,
    val titreRes: Int,
    val descriptionRes: Int,
)

private val OnbAvantages = listOf(
    OnbAvantage(Iv.ShoppingCart, Color(0xFFE3F0FE), Color(0xFF2563EB), R.string.obn_av_ventes_titre, R.string.obn_av_ventes_desc),
    OnbAvantage(Iv.Inventory2, Color(0xFFE8F7EC), Color(0xFF16A34A), R.string.obn_av_stock_titre, R.string.obn_av_stock_desc),
    OnbAvantage(Iv.Group, Color(0xFFF0EBFB), Color(0xFF7C3AED), R.string.obn_av_clients_titre, R.string.obn_av_clients_desc),
    OnbAvantage(Iv.Description, Color(0xFFE3F0FE), Color(0xFF2563EB), R.string.obn_av_documents_titre, R.string.obn_av_documents_desc),
    OnbAvantage(Iv.Chat, Color(0xFFE8F7EC), Color(0xFF22C55E), R.string.obn_av_commun_titre, R.string.obn_av_commun_desc),
    OnbAvantage(Iv.Security, Color(0xFFF0EBFB), Color(0xFF7C3AED), R.string.obn_av_securite_titre, R.string.obn_av_securite_desc),
)

/**
 * Carte « Une application, plusieurs avantages » : badge étoile, titre,
 * sous-titre et grille 2 × 3 de briques (icônes colorées, titres, textes).
 */
@Composable
private fun OnbAvantagesCarte() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, Color(0xFFE4EBF6), RoundedCornerShape(20.dp))
            .background(Color.White)
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF22C55E)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(Iv.Star),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = stringResource(R.string.obn_avantages_titre),
                    color = MissaInk,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = stringResource(R.string.obn_avantages_sous),
                    color = MissaMuted,
                    fontSize = 11.5.sp,
                    lineHeight = 16.sp,
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        for (ligne in 0 until 3) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OnbAvantageTuile(OnbAvantages[ligne * 2], Modifier.weight(1f))
                OnbAvantageTuile(OnbAvantages[ligne * 2 + 1], Modifier.weight(1f))
            }
            if (ligne < 2) Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun OnbAvantageTuile(avantage: OnbAvantage, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .height(130.dp)
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, Color(0xFFE8EEF7), RoundedCornerShape(14.dp))
            .background(Color(0xFFF7FAFF))
            .padding(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(avantage.fond),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(avantage.icone),
                contentDescription = null,
                tint = avantage.couleur,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(avantage.titreRes),
            color = MissaInk,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 16.sp,
            maxLines = 2,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(avantage.descriptionRes),
            color = MissaMuted,
            fontSize = 10.5.sp,
            lineHeight = 14.5.sp,
        )
    }
}

/** Rappel de l'essai gratuit : bandeau bleu clair avec calendrier (maquette). */
@Composable
private fun OnbEssaiBanniere() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFEAF2FE))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(Color(0xFFCFE1FC)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(Iv.Calendar),
                contentDescription = null,
                tint = BrandBlue,
                modifier = Modifier.size(19.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.obn_essai_titre),
                color = BrandBlue,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = stringResource(R.string.obn_ann_essai_desc),
                color = Color(0xFF5B6B84),
                fontSize = 11.sp,
                lineHeight = 15.sp,
            )
        }
    }
}

/** Gros bouton bleu « Commencer » — même destination que « Passer ». */
@Composable
private fun OnbAnnoncesBoutonCommencer(onClick: () -> Unit) {
    val libelle = stringResource(R.string.obn_commencer)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(BrandBlue)
            .clickable(onClickLabel = libelle, role = Role.Button, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = libelle,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.width(8.dp))
        Icon(
            painter = painterResource(Iv.ArrowForward),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(18.dp),
        )
    }
}
