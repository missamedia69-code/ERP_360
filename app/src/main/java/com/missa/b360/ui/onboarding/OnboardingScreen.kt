package com.missa.b360.ui.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.missa.b360.R
import com.missa.b360.core.domain.model.PalierTaille
import com.missa.b360.core.domain.model.ProfilActivite
import com.missa.b360.ui.theme.BrandBlue
import com.missa.b360.ui.theme.OnboardingBorder
import com.missa.b360.ui.theme.ProfileCommerceBlue
import com.missa.b360.ui.theme.ProfileGreen
import com.missa.b360.ui.theme.ProfileOrange
import com.missa.b360.ui.theme.ProfilePurple

/**
 * Hôte de l'onboarding : langue → profil/effectif → entreprise → PIN → email → licence.
 * Les étapes et les validations métier restent inchangées ; seule leur présentation adopte
 * le parcours mobile de la direction artistique Missa Business 360.
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
        color = Color.White,
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
                OnboardingStep.LANGUE -> LanguageStep(viewModel)
                OnboardingStep.PROFIL -> ProfileStep(viewModel)
                OnboardingStep.ENTREPRISE -> EnterpriseStep(viewModel)
                OnboardingStep.PIN -> PinSetupStep(viewModel)
                OnboardingStep.EMAIL -> EmailStep(viewModel)
                OnboardingStep.LICENCE -> LicenceStep(viewModel)
                OnboardingStep.CHECKLIST -> ChecklistStep(viewModel)
            }
        }
    }
}

/**
 * Gabarit mobile du parcours : zones système protégées, en-tête léger, contenu défilable,
 * progression et action primaire toujours visible en bas d'écran.
 */
@Composable
internal fun StepScaffold(
    title: String,
    viewModel: OnboardingViewModel,
    subtitle: String? = null,
    boutonSuivantRes: Int = R.string.ob_suivant,
    suivantActive: Boolean = true,
    navigationActive: Boolean = true,
    showSkip: Boolean = false,
    onSkip: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(52.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (viewModel.step != OnboardingStep.LANGUE) {
                IconButton(
                    onClick = viewModel::precedent,
                    enabled = navigationActive,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = stringResource(R.string.ob_retour),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            } else {
                Spacer(Modifier.size(48.dp))
            }
            Spacer(Modifier.weight(1f))
            if (showSkip) {
                TextButton(
                    onClick = onSkip ?: viewModel::suivant,
                    enabled = navigationActive,
                ) {
                    Text(
                        text = stringResource(R.string.ob_plus_tard),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            } else {
                Spacer(Modifier.size(48.dp))
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            subtitle?.let {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (viewModel.enregistrementEnCours) {
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = BrandBlue,
                )
            }
            viewModel.erreurRes?.let { erreur ->
                Spacer(Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(erreur),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            content()
            Spacer(Modifier.height(20.dp))
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            OnboardingProgress(step = viewModel.step)
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = viewModel::suivant,
                enabled = suivantActive && navigationActive,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandBlue,
                    disabledContainerColor = BrandBlue.copy(alpha = 0.35f),
                ),
            ) {
                Text(
                    text = stringResource(boutonSuivantRes),
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.weight(1f))
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = null,
                )
            }
        }
    }
}

@Composable
private fun OnboardingProgress(step: OnboardingStep) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OnboardingStep.entries.forEachIndexed { index, _ ->
            val active = index == step.ordinal
            Box(
                modifier = Modifier
                    .size(if (active) 8.dp else 7.dp)
                    .background(
                        color = if (active) BrandBlue else OnboardingBorder,
                        shape = CircleShape,
                    ),
            )
        }
    }
}

/** Étape langue : cartes à bordure fine, nom natif et sélection lisible. */
@Composable
private fun LanguageStep(viewModel: OnboardingViewModel) {
    val langues = listOf(
        LanguageChoice("fr", R.string.langue_fr, "🇫🇷"),
        LanguageChoice("en", R.string.langue_en, "🇬🇧"),
        LanguageChoice("es", R.string.langue_es, "🇪🇸"),
        LanguageChoice("ar", R.string.langue_ar, "🇸🇦"),
        LanguageChoice("zh", R.string.langue_zh, "🇨🇳"),
    )
    StepScaffold(
        title = stringResource(R.string.ob_langue_title),
        subtitle = stringResource(R.string.ob_langue_subtitle),
        viewModel = viewModel,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            langues.forEach { langue ->
                LanguageChoiceCard(
                    choice = langue,
                    selected = viewModel.langue == langue.code,
                    onClick = { viewModel.choisirLangue(langue.code) },
                )
            }
        }
    }
}

private data class LanguageChoice(val code: String, val labelRes: Int, val flag: String)

@Composable
private fun LanguageChoiceCard(
    choice: LanguageChoice,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) BrandBlue else OnboardingBorder,
        ),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = choice.flag, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.size(12.dp))
            Text(
                text = stringResource(choice.labelRes),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            RadioButton(
                selected = selected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(selectedColor = BrandBlue),
            )
        }
    }
}

/** Étape profil : les 8 profils requis restent disponibles dans des cartes visuelles compactes. */
@Composable
private fun ProfileStep(viewModel: OnboardingViewModel) {
    StepScaffold(
        title = stringResource(R.string.ob_profil_title),
        subtitle = stringResource(R.string.ob_profil_subtitle),
        viewModel = viewModel,
        suivantActive = viewModel.profil != null && viewModel.palier != null,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ProfilActivite.entries.toList().chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    row.forEach { profil ->
                        ProfileChoiceCard(
                            profil = profil,
                            selected = viewModel.profil == profil,
                            onClick = { viewModel.choisirProfil(profil) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }

            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.ob_palier_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            PalierTaille.entries.toList().chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    row.forEach { palier ->
                        PalierChoiceCard(
                            palier = palier,
                            selected = viewModel.palier == palier,
                            onClick = { viewModel.choisirPalier(palier) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileChoiceCard(
    profil: ProfilActivite,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val color = profilColor(profil)
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) color else OnboardingBorder,
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) color.copy(alpha = 0.06f) else Color.White,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier.heightIn(min = 126.dp),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = color.copy(alpha = 0.12f),
                modifier = Modifier.size(38.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Business,
                        contentDescription = null,
                        tint = color,
                    )
                }
            }
            Text(
                text = stringResource(profil.labelRes),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
            if (selected) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.size(4.dp))
                    Text(
                        text = stringResource(R.string.ob_selectionne),
                        style = MaterialTheme.typography.labelSmall,
                        color = color,
                    )
                }
            }
        }
    }
}

@Composable
private fun PalierChoiceCard(
    palier: PalierTaille,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) BrandBlue else OnboardingBorder,
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) BrandBlue.copy(alpha = 0.06f) else Color.White,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier,
    ) {
        Text(
            text = stringResource(palier.labelRes),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
        )
    }
}

private fun profilColor(profil: ProfilActivite): Color = when (profil) {
    ProfilActivite.E -> ProfileGreen
    ProfilActivite.F, ProfilActivite.H -> ProfilePurple
    ProfilActivite.G -> ProfileOrange
    else -> ProfileCommerceBlue
}
