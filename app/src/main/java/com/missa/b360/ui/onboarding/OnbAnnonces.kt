package com.missa.b360.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.missa.b360.R
import com.missa.b360.ui.icons.Iv
import com.missa.b360.ui.theme.BrandBlue
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaLime
import com.missa.b360.ui.theme.MissaMuted
import com.missa.b360.ui.theme.MissaSurface

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
 * Bandeau de la page d'annonces : illustration de marque pleine largeur
 * (téléphone avec l'application, icônes de modules en orbite, dégradé bleu —
 * moitié gauche volontairement libre) sur laquelle se superposent les textes
 * gérés par l'application : tuile logo, nom de la marque avec « 360 » vert,
 * trait signature, slogan et accroche — traduits dans les cinq langues.
 */
@Composable
private fun OnbAnnoncesHero() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1104f / 960f),
    ) {
        Image(
            painter = painterResource(R.drawable.hero_annonce),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 24.dp, top = 22.dp)
                .fillMaxWidth(0.52f),
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .shadow(
                        elevation = 10.dp,
                        shape = RoundedCornerShape(17.dp),
                        spotColor = Color(0xFF000A2E).copy(alpha = 0.35f),
                    )
                    .clip(RoundedCornerShape(17.dp))
                    .background(Color(0xFF0544CE)),
            ) {
                Image(
                    painter = painterResource(R.drawable.logo_missa_mark),
                    contentDescription = stringResource(R.string.app_name),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Spacer(Modifier.height(16.dp))
            OnbAnnoncesTitre()
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .width(44.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MissaLime),
            )
            Spacer(Modifier.height(18.dp))
            Text(
                text = stringResource(R.string.obn_ann_slogan),
                color = Color.White,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 18.sp,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.obn_ann_paragraphe),
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 11.5.sp,
                lineHeight = 16.sp,
            )
        }
    }
}

/** Nom de la marque en capitales : le « 360 » final est surligné en vert. */
@Composable
private fun OnbAnnoncesTitre() {
    val nom = stringResource(R.string.app_name).uppercase()
    val titre = remember(nom) {
        buildAnnotatedString {
            val index = nom.lastIndexOf("360")
            if (index >= 0) {
                append(nom.substring(0, index))
                withStyle(SpanStyle(color = MissaLime)) {
                    append(nom.substring(index))
                }
            } else {
                append(nom)
            }
        }
    }
    Text(
        text = titre,
        color = Color.White,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 24.sp,
    )
}

/** Les six briques mises en avant sur la page d'annonces (maquette). */
private data class OnbAvantage(
    val icone: Int,
    val debut: Color,
    val fin: Color,
    val bordure: Color,
    val fond: Color,
    val titreRes: Int,
    val descriptionRes: Int,
)

private val OnbAvantages = listOf(
    OnbAvantage(Iv.ShoppingCart, Color(0xFF7CB0FF), Color(0xFF2563EB), Color(0xFFD9E7FC), Color(0xFFEFF5FF), R.string.obn_av_ventes_titre, R.string.obn_av_ventes_desc),
    OnbAvantage(Iv.Inventory2, Color(0xFF57D98A), Color(0xFF16A34A), Color(0xFFD7EFDF), Color(0xFFEFFAF3), R.string.obn_av_stock_titre, R.string.obn_av_stock_desc),
    OnbAvantage(Iv.Group, Color(0xFFB09CFF), Color(0xFF7C3AED), Color(0xFFE6E0FC), Color(0xFFF5F2FF), R.string.obn_av_clients_titre, R.string.obn_av_clients_desc),
    OnbAvantage(Iv.Description, Color(0xFF7CB0FF), Color(0xFF2563EB), Color(0xFFD9E7FC), Color(0xFFEFF5FF), R.string.obn_av_documents_titre, R.string.obn_av_documents_desc),
    OnbAvantage(Iv.Chat, Color(0xFF57D98A), Color(0xFF22C55E), Color(0xFFD7EFDF), Color(0xFFEFFAF3), R.string.obn_av_commun_titre, R.string.obn_av_commun_desc),
    OnbAvantage(Iv.Security, Color(0xFFB09CFF), Color(0xFF7C3AED), Color(0xFFE6E0FC), Color(0xFFF5F2FF), R.string.obn_av_securite_titre, R.string.obn_av_securite_desc),
)

/**
 * Carte « Une application, plusieurs avantages » : pastille étoile en dégradé
 * vert sur fond de halo, titre, sous-titre, filet tricolore de transition et
 * grille 2 × 3 de briques — chaque brique porte son icône en dégradé avec
 * halo coloré, sa bordure teintée et son fond en dégradé doux.
 */
@Composable
private fun OnbAvantagesCarte() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, Color(0xFFE4EBF6), RoundedCornerShape(24.dp))
            .background(Color.White)
            .padding(22.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .shadow(
                        elevation = 7.dp,
                        shape = RoundedCornerShape(16.dp),
                        spotColor = Color(0xFF16A34A).copy(alpha = 0.4f),
                    )
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF4ADE80), Color(0xFF16A34A)),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(Iv.Star),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = stringResource(R.string.obn_avantages_titre),
                    color = MissaInk,
                    fontSize = 16.5.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.obn_avantages_sous),
                    color = MissaMuted,
                    fontSize = 12.sp,
                    lineHeight = 16.5.sp,
                )
            }
        }
        Spacer(Modifier.height(15.dp))
        // Filet de transition : les trois couleurs de marque en dégradé.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(1.5.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(BrandBlue, Color(0xFF22C55E), Color(0xFF7C3AED)),
                    ),
                ),
        )
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
    val formeTuile = RoundedCornerShape(18.dp)
    Column(
        modifier = modifier
            .height(132.dp)
            .clip(formeTuile)
            .border(1.5.dp, avantage.bordure, formeTuile)
            .background(
                Brush.verticalGradient(
                    colors = listOf(avantage.fond, Color.White),
                ),
            )
            .padding(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .shadow(
                    elevation = 5.dp,
                    shape = RoundedCornerShape(15.dp),
                    spotColor = avantage.fin.copy(alpha = 0.35f),
                )
                .clip(RoundedCornerShape(15.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(avantage.debut, avantage.fin),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(avantage.icone),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(avantage.titreRes),
            color = MissaInk,
            fontSize = 13.sp,
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
