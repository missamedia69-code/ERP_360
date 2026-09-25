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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.missa.b360.R
import com.missa.b360.ui.icons.Iv
import com.missa.b360.ui.theme.BrandBlue
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted
import com.missa.b360.ui.theme.MissaSurface

/**
 * Écran 2 — Annonces : présenté juste après le choix de la langue (maquette).
 *
 * Bandeau bleu de marque : l'illustration intègre le logo, le nom de la marque
 * et le trait signature ; le slogan et l'accroche, gérés par l'application,
 * se superposent dans la zone libre. Carte « Une application, de nombreux
 * avantages » où les six avantages sont présentés en liste, rappel de l'essai
 * gratuit et bouton « Commencer » qui épouse le bas de l'écran. Le lien
 * « Passer » court-circuite la lecture mais conduit au même endroit : la
 * configuration initiale.
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
 * (téléphone avec l'application, icônes de modules en orbite, dégradé bleu ;
 * le logo, le nom de la marque et le trait signature sont intégrés à
 * l'image). Dans la zone libre en bas à gauche, les textes gérés par
 * l'application — slogan et accroche — se superposent, traduits dans les
 * cinq langues.
 *
 * L'illustration est un actif de marque à mise en page physique : la zone
 * de texte reste en bas à gauche de l'écran dans toutes les langues, y
 * compris en arabe (RTL) où l'alignement « End » est physiquement à gauche ;
 * les textes conservent la direction de lecture de leur langue.
 */
@Composable
private fun OnbAnnoncesHero() {
    val rtl = LocalLayoutDirection.current == LayoutDirection.RTL
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
        Column(
            modifier = Modifier
                .align(if (rtl) Alignment.BottomEnd else Alignment.BottomStart)
                .padding(
                    start = if (rtl) 0.dp else 24.dp,
                    end = if (rtl) 24.dp else 0.dp,
                    bottom = 26.dp,
                )
                .fillMaxWidth(0.52f),
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                text = stringResource(R.string.obn_ann_slogan),
                color = Color.White,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 18.sp,
                textAlign = TextAlign.Start,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.obn_ann_paragraphe),
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 11.5.sp,
                lineHeight = 16.sp,
                textAlign = TextAlign.Start,
            )
        }
    }
}

/** Les six avantages mis en avant sur la page d'annonces (maquette). */
private data class OnbAvantage(
    val icone: Int,
    val debut: Color,
    val fin: Color,
    val titreRes: Int,
    val descriptionRes: Int,
)

private val OnbAvantages = listOf(
    OnbAvantage(Iv.ShoppingCart, Color(0xFF7CB0FF), Color(0xFF2563EB), R.string.obn_av_ventes_titre, R.string.obn_av_ventes_desc),
    OnbAvantage(Iv.Inventory2, Color(0xFF57D98A), Color(0xFF16A34A), R.string.obn_av_stock_titre, R.string.obn_av_stock_desc),
    OnbAvantage(Iv.Group, Color(0xFFB09CFF), Color(0xFF7C3AED), R.string.obn_av_clients_titre, R.string.obn_av_clients_desc),
    OnbAvantage(Iv.Description, Color(0xFF7CB0FF), Color(0xFF2563EB), R.string.obn_av_documents_titre, R.string.obn_av_documents_desc),
    OnbAvantage(Iv.Chat, Color(0xFF57D98A), Color(0xFF22C55E), R.string.obn_av_commun_titre, R.string.obn_av_commun_desc),
    OnbAvantage(Iv.Security, Color(0xFFB09CFF), Color(0xFF7C3AED), R.string.obn_av_securite_titre, R.string.obn_av_securite_desc),
)

/**
 * Carte « One app, many benefits » : pastille étoile en dégradé vert sur fond
 * de halo, titre, sous-titre, filet tricolore de transition, puis les six
 * avantages en liste simple — chacun sur sa ligne, icône en dégradé avec halo
 * coloré, titre et description.
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
        Spacer(Modifier.height(14.dp))
        OnbAvantages.forEachIndexed { index, avantage ->
            OnbAvantageLigne(avantage)
            if (index < OnbAvantages.lastIndex) {
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color(0xFFEFF3F9)),
                )
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

/** Une ligne de la liste des avantages : puce iconée en dégradé, titre, description. */
@Composable
private fun OnbAvantageLigne(avantage: OnbAvantage) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .shadow(
                    elevation = 5.dp,
                    shape = RoundedCornerShape(13.dp),
                    spotColor = avantage.fin.copy(alpha = 0.35f),
                )
                .clip(RoundedCornerShape(13.dp))
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
                modifier = Modifier.size(19.dp),
            )
        }
        Spacer(Modifier.width(11.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(avantage.titreRes),
                color = MissaInk,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 16.sp,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(avantage.descriptionRes),
                color = MissaMuted,
                fontSize = 11.sp,
                lineHeight = 14.5.sp,
            )
        }
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
