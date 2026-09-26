package com.missa.b360.ui.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.matchParentSize
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.missa.b360.R
import com.missa.b360.ui.icons.Iv
import com.missa.b360.ui.theme.BrandBlue
import com.missa.b360.ui.theme.Blue90
import com.missa.b360.ui.theme.MissaBorder
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted
import com.missa.b360.ui.theme.MissaSurface
import com.missa.b360.ui.theme.ProfilePurple
import com.missa.b360.ui.theme.TendrePositive

/**
 * Écran 2 — Annonces : présenté juste après le choix de la langue (maquette).
 *
 * Refonte : le bandeau de marque garde le logo et le nom intégrés à
 * l'illustration ; l'application y superpose le slogan et l'accroche dans la
 * zone libre, avec un voile de lisibilité. En dessous, une carte unique « One
 * app, many benefits » liste les six avantages en pastilles tonales (les
 * accents métier restent assourdis, sans concurrencer le bleu de marque), un
 * rappel d'essai gratuit, et le bouton « Commencer » épousant le bas de
 * l'écran. Le lien « Passer » court-circuite la lecture mais conduit au même
 * endroit : la configuration initiale.
 */
@Composable
internal fun OnbAnnonces(viewModel: OnboardingViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MissaSurface),
    ) {
        // En-tête aux teintes exactes du haut de l'illustration : la sensation
        // que l'image commence depuis le tout début de l'écran.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF0C48DB), Color(0xFF165AE4)),
                    ),
                ),
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
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        painter = painterResource(Iv.ArrowBack),
                        contentDescription = stringResource(R.string.ob_retour),
                        tint = Color.White.copy(alpha = 0.92f),
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.weight(1f))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.White.copy(alpha = 0.14f))
                        .clickable(
                            onClickLabel = stringResource(R.string.obn_ann_passer),
                            role = Role.Button,
                            onClick = viewModel::suivant,
                        )
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.obn_ann_passer),
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        painter = painterResource(Iv.ChevronRight),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            OnbAnnoncesHero(langue = viewModel.langue)
            OnbAvantagesCarte()
            OnbEssaiBanniere()
        }

        // Barre basse : le bouton « Commencer » épouse le bas de l'écran —
        // le fond de la barre descend jusqu'au bord physique, la zone de
        // navigation système est absorbée dans la barre.
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .background(MissaSurface),
            shadowElevation = 4.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
                    .navigationBarsPadding(),
            ) {
                OnbAnnoncesBoutonCommencer(onClick = viewModel::suivant)
            }
        }
    }
}

/**
 * Bandeau de la page d'annonces : illustration de marque pleine largeur
 * (le logo, le nom de la marque et le trait signature sont intégrés à
 * l'image). Dans la zone libre en bas à gauche, le slogan et l'accroche,
 * gérés par l'application et traduits dans les cinq langues, se superposent
 * sur un voile progressif qui garantit la lisibilité.
 *
 * La zone de texte est ancrée en bas à gauche de l'écran dans toutes les
 * langues, y compris en arabe (RTL) où l'alignement « End » est physiquement
 * à gauche ; les textes conservent la direction de lecture de leur langue.
 */
@Composable
private fun OnbAnnoncesHero(langue: String) {
    // Parmi les cinq locales de l'application, seul l'arabe est RTL : la
    // détection par la langue est déterministe (la langue est appliquée en
    // direct à l'interface dès l'écran de bienvenue).
    val rtl = langue.substringBefore('-').lowercase() == "ar"
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1341f / 1173f),
    ) {
        Image(
            painter = painterResource(R.drawable.hero_annonce),
            contentDescription = stringResource(R.string.app_name),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        // Voile de lisibilité : transparent jusqu'au milieu de l'image,
        // voile sombre en bas — le texte gagne en contraste sans masquer
        // l'illustration.
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.42f),
                        ),
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .align(if (rtl) Alignment.BottomEnd else Alignment.BottomStart)
                .padding(
                    start = if (rtl) 0.dp else 26.dp,
                    end = if (rtl) 26.dp else 0.dp,
                    bottom = 26.dp,
                )
                .fillMaxWidth(0.66f),
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                text = stringResource(R.string.obn_ann_slogan),
                color = Color.White,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 25.sp,
                textAlign = TextAlign.Start,
            )
            Spacer(Modifier.height(9.dp))
            Text(
                text = stringResource(R.string.obn_ann_paragraphe),
                color = Color.White.copy(alpha = 0.88f),
                fontSize = 12.5.sp,
                lineHeight = 17.5.sp,
                textAlign = TextAlign.Start,
            )
        }
    }
}

/** Un avantage mis en avant sur la page d'annonces (pastille tonale). */
private data class OnbAvantage(
    val icone: Int,
    val couleur: Color,
    val titreRes: Int,
    val descriptionRes: Int,
)

/**
 * Les six avantages. Les couleurs d'accent sont celles des profils métier,
 * mais appliquées en pastille tonale (fond 12 %, icône pleine couleur) :
 * l'œil suit la hiérarchie sans que les accents ne concurrencent le bleu.
 */
private val OnbAvantages = listOf(
    OnbAvantage(Iv.ShoppingCart, BrandBlue, R.string.obn_av_ventes_titre, R.string.obn_av_ventes_desc),
    OnbAvantage(Iv.Inventory2, TendrePositive, R.string.obn_av_stock_titre, R.string.obn_av_stock_desc),
    OnbAvantage(Iv.Group, ProfilePurple, R.string.obn_av_clients_titre, R.string.obn_av_clients_desc),
    OnbAvantage(Iv.Description, BrandBlue, R.string.obn_av_documents_titre, R.string.obn_av_documents_desc),
    OnbAvantage(Iv.Chat, TendrePositive, R.string.obn_av_commun_titre, R.string.obn_av_commun_desc),
    OnbAvantage(Iv.Security, ProfilePurple, R.string.obn_av_securite_titre, R.string.obn_av_securite_desc),
)

/**
 * Carte « One app, many benefits » : pastille étoile en dégradé de marque,
 * titre et sous-titre, puis les six avantages — chacun sur sa ligne, pastille
 * iconée tonale, titre et description.
 */
@Composable
private fun OnbAvantagesCarte() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp)
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, MissaBorder.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
            .background(MissaSurface)
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .shadow(
                        elevation = 6.dp,
                        shape = RoundedCornerShape(12.dp),
                        spotColor = BrandBlue.copy(alpha = 0.4f),
                    )
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF3E7BFA), BrandBlue),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(Iv.Star),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.width(11.dp))
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
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        OnbAvantages.forEachIndexed { index, avantage ->
            OnbAvantageLigne(avantage)
            if (index < OnbAvantages.lastIndex) {
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color(0xFFEFF3F9)),
                )
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

/**
 * Une ligne de la liste des avantages : pastille tonale (fond de la couleur
 * à 12 %, icône pleine couleur), titre, description.
 */
@Composable
private fun OnbAvantageLigne(avantage: OnbAvantage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(avantage.couleur.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(avantage.icone),
                contentDescription = null,
                tint = avantage.couleur,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(11.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(avantage.titreRes),
                color = MissaInk,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 17.sp,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(avantage.descriptionRes),
                color = MissaMuted,
                fontSize = 11.5.sp,
                lineHeight = 15.5.sp,
            )
        }
    }
}

/** Rappel de l'essai gratuit : bandeau bleu de marque sur fond tonal. */
@Composable
private fun OnbEssaiBanniere() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Blue90)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(BrandBlue.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(Iv.Calendar),
                contentDescription = null,
                tint = BrandBlue,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(11.dp))
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
                fontSize = 11.5.sp,
                lineHeight = 15.sp,
            )
        }
    }
}

/**
 * Gros bouton bleu « Commencer » : dégradé de marque, ombre bleue portée —
 * même destination que « Passer ».
 */
@Composable
private fun OnbAnnoncesBoutonCommencer(onClick: () -> Unit) {
    val libelle = stringResource(R.string.obn_commencer)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .shadow(
                elevation = 7.dp,
                shape = RoundedCornerShape(14.dp),
                spotColor = BrandBlue.copy(alpha = 0.45f),
            )
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF3E7BFA), BrandBlue),
                ),
            )
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
