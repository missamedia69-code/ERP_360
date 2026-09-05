package com.missa.b360.ui.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.missa.b360.R
import com.missa.b360.core.util.Iso4217
import com.missa.b360.ui.theme.BrandBlue
import com.missa.b360.ui.theme.Green60
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaLime
import com.missa.b360.ui.theme.MissaMuted
import com.missa.b360.ui.theme.MissaSurface
import com.missa.b360.ui.theme.OnboardingHeroGreen
import com.missa.b360.ui.theme.ProfileGreen
import com.missa.b360.ui.theme.ProfileOrange
import com.missa.b360.ui.theme.ProfilePurple

/**
 * Écran 7 — Tout est prêt : récapitulatif de la configuration et accès à
 * l'application (clôture RA-11 ; l'app démarrera sur le verrou PIN).
 */
@Composable
internal fun OnbTermineStep(viewModel: OnboardingViewModel) {
    val profilActuel = viewModel.profil
    val tailleActuelle = viewModel.palier
    val profilLabel = if (profilActuel != null) stringResource(profilActuel.labelRes) else "—"
    val tailleLabel = if (tailleActuelle != null) stringResource(tailleActuelle.labelRes) else "—"
    val deviseChoisie = Iso4217.COMMUNES.firstOrNull { it.code == viewModel.devise }
    val deviseLabel = if (deviseChoisie != null) "${deviseChoisie.code} · ${deviseChoisie.nom}" else viewModel.devise

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OnboardingHeroGreen)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        ConfettiDots()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))
            Surface(
                color = Green60,
                shape = CircleShape,
                modifier = Modifier.size(88.dp),
                shadowElevation = 10.dp,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(44.dp),
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
            Text(
                text = stringResource(R.string.obn_terminer_titre),
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold,
                color = MissaInk,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.obn_terminer_sous),
                fontSize = 13.5.sp,
                color = MissaMuted,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(22.dp))

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MissaSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = stringResource(R.string.obn_recap),
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MissaInk,
                    )
                    OnbRecapLigne(R.string.obn_recap_profil, profilLabel)
                    OnbRecapLigne(R.string.obn_recap_taille, tailleLabel)
                    OnbRecapLigne(R.string.obn_recap_entreprise, viewModel.nomEntreprise)
                    OnbRecapLigne(R.string.obn_recap_devise, deviseLabel)
                }
            }
            Spacer(Modifier.weight(1f))

            Button(
                onClick = viewModel::suivant,
                enabled = !viewModel.enregistrementEnCours,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
            ) {
                Text(
                    text = stringResource(R.string.obn_acceder),
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.5.sp,
                )
                Spacer(Modifier.size(9.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun OnbRecapLigne(labelRes: Int, valeur: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(labelRes),
            fontSize = 12.5.sp,
            color = MissaMuted,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = valeur,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MissaInk,
        )
    }
}

/**
 * Confettis décoratifs du fond vert : positions fixes (aucun aléa entre
 * recompositions) dans les couleurs de la marque.
 */
private data class ConfettiPoint(val x: Float, val y: Float, val couleur: Color, val rayon: Float)

@Composable
private fun ConfettiDots() {
    val points = listOf(
        ConfettiPoint(0.08f, 0.10f, MissaLime, 5f),
        ConfettiPoint(0.92f, 0.14f, BrandBlue, 4f),
        ConfettiPoint(0.85f, 0.05f, ProfileOrange, 3.5f),
        ConfettiPoint(0.05f, 0.30f, ProfilePurple, 3f),
        ConfettiPoint(0.95f, 0.38f, ProfileGreen, 4f),
        ConfettiPoint(0.07f, 0.62f, BrandBlue, 3f),
        ConfettiPoint(0.93f, 0.70f, MissaLime, 5f),
        ConfettiPoint(0.10f, 0.88f, ProfileOrange, 3.5f),
        ConfettiPoint(0.90f, 0.92f, ProfilePurple, 3f),
    )
    Canvas(modifier = Modifier.fillMaxSize()) {
        points.forEach { point ->
            drawCircle(
                color = point.couleur.copy(alpha = 0.5f),
                radius = point.rayon * density,
                center = androidx.compose.ui.geometry.Offset(point.x * size.width, point.y * size.height),
            )
        }
    }
}
