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
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.missa.b360.R
import com.missa.b360.ui.theme.BrandBlue
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaLime
import com.missa.b360.ui.theme.MissaMuted
import com.missa.b360.ui.theme.MissaSurface
import com.missa.b360.ui.theme.OnboardingHeroBlue
import com.missa.b360.ui.theme.OnboardingStepGray

/**
 * Hôte de l'onboarding — maquette Missa Business 360 :
 * bienvenue → profil d'activité → taille → informations entreprise (+ logo)
 * → configuration initiale → code PIN → récapitulatif.
 *
 * Les validations métier (entreprise transactionnelle, PIN, propriétaire, clôture)
 * restent portées par le ViewModel et les use cases existants.
 */
@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val termine = viewModel.onboardingTermine
    LaunchedEffect(termine) {
        if (termine) onFinished()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MissaSurface,
    ) {
        if (!viewModel.initialisationTerminee) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = BrandBlue)
            }
        } else {
            when (viewModel.step) {
                OnboardingStep.BIENVENUE -> WelcomeStep(onCommencer = viewModel::suivant)
                OnboardingStep.CONFIGURATION -> OnbConfigurationStep(viewModel)
                OnboardingStep.PROFIL -> OnbProfilStep(viewModel)
                OnboardingStep.TAILLE -> OnbTailleStep(viewModel)
                OnboardingStep.ENTREPRISE -> OnbEntrepriseStep(viewModel)
                OnboardingStep.PIN -> OnbPinStep(viewModel)
                OnboardingStep.TERMINE -> OnbTermineStep(viewModel)
            }
        }
    }
}

/**
 * Gabarit commun des étapes : titre centré, sous-titre, retour discret, contenu
 * défilable, bannière d'erreur et barre basse [points de progression + bouton].
 */
@Composable
internal fun OnbScaffold(
    titreRes: Int,
    sousTitreRes: Int,
    viewModel: OnboardingViewModel,
    boutonLabelRes: Int = R.string.ob_suivant,
    boutonActive: Boolean = true,
    boutonPleineLargeur: Boolean = false,
    onRetour: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MissaSurface)
            .windowInsetsPadding(androidx.compose.foundation.layout.WindowInsets.safeDrawing),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onRetour != null) {
                IconButton(onClick = onRetour, enabled = !viewModel.enregistrementEnCours, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = stringResource(R.string.ob_retour),
                        tint = MissaMuted,
                        modifier = Modifier.size(22.dp),
                    )
                }
            } else {
                Spacer(Modifier.size(40.dp))
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(titreRes),
                color = MissaInk,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(sousTitreRes),
                color = MissaMuted,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
            )
        }

        val erreurActuelle = viewModel.erreurRes
        if (erreurActuelle != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                shape = RoundedCornerShape(10.dp),
                color = com.missa.b360.ui.theme.Red80,
            ) {
                Text(
                    text = stringResource(erreurActuelle),
                    color = com.missa.b360.ui.theme.Red40,
                    fontSize = 12.5.sp,
                    modifier = Modifier.padding(10.dp),
                )
            }
        }
        if (viewModel.enregistrementEnCours) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = BrandBlue,
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            content()
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .background(MissaSurface),
            shadowElevation = 4.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OnbDots(
                        total = OnboardingStep.TERMINE.ordinal,
                        active = viewModel.step.ordinal,
                    )
                }
                Button(
                    onClick = viewModel::suivant,
                    enabled = boutonActive && !viewModel.enregistrementEnCours,
                    modifier = Modifier
                        .then(
                            if (boutonPleineLargeur) Modifier.fillMaxWidth()
                            else Modifier.width(150.dp),
                        )
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandBlue,
                        disabledContainerColor = BrandBlue.copy(alpha = 0.35f),
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp),
                ) {
                    Text(
                        text = stringResource(boutonLabelRes),
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(17.dp),
                    )
                }
            }
        }
    }
}

/** Points de progression bas d'écran : le point actif est agrandi et bleu. */
@Composable
internal fun OnbDots(total: Int, active: Int) {
    for (index in 0 until total) {
        Box(
            modifier = Modifier
                .size(if (index == active) 9.dp else 7.dp)
                .background(if (index == active) BrandBlue else OnboardingStepGray, CircleShape),
        )
    }
}

/**
 * Écran 1 — Bienvenue : fond bleu roi, monogramme de la marque, accroche et
 * bouton « Commencer » vert signature (maquette).
 */
@Composable
private fun WelcomeStep(onCommencer: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OnboardingHeroBlue)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            OnbLogoMark(modifier = Modifier.size(112.dp))
            Spacer(Modifier.height(18.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "MISSA\nBUSINESS",
                    color = Color.White,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 33.sp,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "360",
                    color = MissaLime,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 34.sp,
                )
            }
            Spacer(Modifier.height(44.dp))
            Text(
                text = stringResource(R.string.obn_bienvenue_titre),
                color = Color.White.copy(alpha = 0.78f),
                fontSize = 15.sp,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Missa Business 360",
                color = MissaLime,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.obn_bienvenue_sous),
                color = Color.White.copy(alpha = 0.72f),
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 19.sp,
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 18.dp),
        ) {
            Button(
                onClick = onCommencer,
                modifier = Modifier
                    .width(250.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MissaLime),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp),
            ) {
                Text(
                    text = stringResource(R.string.obn_commencer),
                    color = OnboardingHeroBlue,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                )
                Spacer(Modifier.width(10.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = null,
                    tint = OnboardingHeroBlue,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                for (index in 0 until 7) {
                    Box(
                        modifier = Modifier
                            .size(if (index == 0) 9.dp else 7.dp)
                            .background(
                                if (index == 0) Color.White else Color.White.copy(alpha = 0.35f),
                                CircleShape,
                            ),
                    )
                }
            }
        }
    }
}

/** Monogramme vectoriel de la marque sur fond bleu : anneau blanc, repère vert signature. */
@Composable
private fun OnbLogoMark(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = size.width.coerceAtMost(size.height) * 0.11f
        drawArc(
            color = Color.White,
            startAngle = 40f,
            sweepAngle = 300f,
            useCenter = false,
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
        val start = Offset(size.width * 0.36f, size.height * 0.63f)
        val middle = Offset(size.width * 0.59f, size.height * 0.40f)
        val end = Offset(size.width * 0.79f, size.height * 0.19f)
        drawLine(Color.White, start, middle, strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(MissaLime, middle, end, strokeWidth = stroke, cap = StrokeCap.Round)
        drawCircle(MissaLime, radius = stroke * 0.8f, center = end)
    }
}
