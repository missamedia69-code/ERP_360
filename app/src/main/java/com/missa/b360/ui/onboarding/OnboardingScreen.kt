package com.missa.b360.ui.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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

    var presentationPage by rememberSaveable { mutableStateOf(0) }
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
        } else if (viewModel.step == OnboardingStep.LANGUE && presentationPage < PRESENTATION_PAGES) {
            PresentationStep(
                page = presentationPage,
                onNext = { presentationPage += 1 },
                onSkip = { presentationPage = PRESENTATION_PAGES },
            )
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

/** Étape profil : quatre familles fidèles à la maquette, avec les profils détaillés conservés. */
@Composable
private fun ProfileStep(viewModel: OnboardingViewModel) {
    val families = listOf(
        ProfileFamily(
            titleRes = R.string.ob_profil_producteur,
            descriptionRes = R.string.ob_profil_producteur_description,
            pictogram = "🌿",
            color = ProfileGreen,
            profiles = setOf(ProfilActivite.E),
            preferred = ProfilActivite.E,
        ),
        ProfileFamily(
            titleRes = R.string.ob_profil_commercant,
            descriptionRes = R.string.ob_profil_commercant_description,
            pictogram = "🛍",
            color = ProfileCommerceBlue,
            profiles = setOf(ProfilActivite.A, ProfilActivite.B, ProfilActivite.C, ProfilActivite.D),
            preferred = ProfilActivite.B,
        ),
        ProfileFamily(
            titleRes = R.string.ob_profil_prestataire,
            descriptionRes = R.string.ob_profil_prestataire_description,
            pictogram = "💼",
            color = ProfilePurple,
            profiles = setOf(ProfilActivite.F, ProfilActivite.G),
            preferred = ProfilActivite.F,
        ),
        ProfileFamily(
            titleRes = R.string.ob_profil_autre,
            descriptionRes = R.string.ob_profil_autre_description,
            pictogram = "•••",
            color = ProfileOrange,
            profiles = setOf(ProfilActivite.H),
            preferred = ProfilActivite.H,
        ),
    )
    var detailsVisibles by rememberSaveable { mutableStateOf(false) }
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
            families.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    row.forEach { family ->
                        ProfileFamilyCard(
                            family = family,
                            selected = viewModel.profil in family.profiles,
                            onClick = { viewModel.choisirProfil(family.preferred) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            TextButton(
                onClick = { detailsVisibles = !detailsVisibles },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text(
                    stringResource(
                        if (detailsVisibles) R.string.ob_profil_masquer_details
                        else R.string.ob_profil_voir_details,
                    ),
                )
            }
            if (detailsVisibles) {
                DetailedProfileChoices(
                    selected = viewModel.profil,
                    onSelected = viewModel::choisirProfil,
                )
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

private data class ProfileFamily(
    val titleRes: Int,
    val descriptionRes: Int,
    val pictogram: String,
    val color: Color,
    val profiles: Set<ProfilActivite>,
    val preferred: ProfilActivite,
)

@Composable
private fun ProfileFamilyCard(
    family: ProfileFamily,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) family.color else OnboardingBorder,
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) family.color.copy(alpha = 0.06f) else Color.White,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier.heightIn(min = 156.dp),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = family.color.copy(alpha = 0.11f),
                modifier = Modifier.size(42.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = family.pictogram, fontSize = 23.sp)
                }
            }
            Text(
                text = stringResource(family.titleRes),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(family.descriptionRes),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (selected) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = family.color,
                    modifier = Modifier.size(17.dp),
                )
            }
        }
    }
}

@Composable
private fun DetailedProfileChoices(
    selected: ProfilActivite?,
    onSelected: (ProfilActivite) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        ProfilActivite.entries.forEach { profile ->
            Card(
                onClick = { onSelected(profile) },
                shape = RoundedCornerShape(9.dp),
                border = BorderStroke(
                    if (profile == selected) 1.5.dp else 1.dp,
                    if (profile == selected) BrandBlue else OnboardingBorder,
                ),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(profile.labelRes),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                    )
                    if (profile == selected) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = BrandBlue,
                            modifier = Modifier.size(18.dp),
                        )
                    }
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

private const val PRESENTATION_PAGES = 2

/** Les deux écrans de découverte précèdent la configuration sans modifier l'état métier. */
@Composable
private fun PresentationStep(page: Int, onNext: () -> Unit, onSkip: () -> Unit) {
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
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onSkip) {
                Text(
                    text = stringResource(R.string.ob_passer),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (page == 0) {
                WelcomeBrand()
                Spacer(Modifier.height(28.dp))
                BusinessHeroArtwork()
                Spacer(Modifier.height(24.dp))
                Text(
                    text = stringResource(R.string.ob_intro_tagline),
                    style = MaterialTheme.typography.labelMedium,
                    color = BrandBlue,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(20.dp))
                Text(
                    text = stringResource(R.string.ob_intro_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
            } else {
                FeatureArtwork()
                Spacer(Modifier.height(28.dp))
                PresentationFeature(
                    titleRes = R.string.ob_intro_business_title,
                    descriptionRes = R.string.ob_intro_business_description,
                    color = BrandBlue,
                )
                PresentationFeature(
                    titleRes = R.string.ob_intro_secure_title,
                    descriptionRes = R.string.ob_intro_secure_description,
                    color = ProfileGreen,
                )
                PresentationFeature(
                    titleRes = R.string.ob_intro_offline_title,
                    descriptionRes = R.string.ob_intro_offline_description,
                    color = ProfilePurple,
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PresentationProgress(page)
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
            ) {
                Text(stringResource(R.string.ob_suivant), fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null)
            }
        }
    }
}

@Composable
private fun WelcomeBrand() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        MissaLogoMark(modifier = Modifier.size(70.dp))
        Column(modifier = Modifier.padding(start = 10.dp)) {
            Text(
                text = "MISSA\nBUSINESS",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                lineHeight = 19.sp,
            )
            Text(
                text = "360",
                color = ProfileGreen,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 25.sp,
            )
        }
    }
}

/** Monogramme vectoriel inspiré du logo : cible bleue et repère vert, sans image bitmap. */
@Composable
private fun MissaLogoMark(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = size.width.coerceAtMost(size.height) * 0.13f
        drawArc(
            color = BrandBlue,
            startAngle = 40f,
            sweepAngle = 300f,
            useCenter = false,
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
        val start = Offset(size.width * 0.36f, size.height * 0.63f)
        val middle = Offset(size.width * 0.59f, size.height * 0.40f)
        val end = Offset(size.width * 0.79f, size.height * 0.19f)
        drawLine(BrandBlue, start, middle, strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(ProfileGreen, middle, end, strokeWidth = stroke, cap = StrokeCap.Round)
        drawCircle(ProfileGreen, radius = stroke * 0.78f, center = end)
    }
}

/** Illustration Compose légère : tableau de bord mobile, colis et indicateurs de croissance. */
@Composable
private fun BusinessHeroArtwork() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(230.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(verticalArrangement = Arrangement.Bottom) {
            Surface(
                color = ProfileOrange.copy(alpha = 0.82f),
                shape = RoundedCornerShape(5.dp),
                modifier = Modifier.size(width = 48.dp, height = 48.dp),
            ) {
                Box(contentAlignment = Alignment.Center) { Text("▦", color = Color.White, fontSize = 24.sp) }
            }
            Spacer(Modifier.height(6.dp))
            Surface(
                color = ProfileOrange.copy(alpha = 0.6f),
                shape = RoundedCornerShape(5.dp),
                modifier = Modifier.size(width = 38.dp, height = 38.dp),
            ) {}
        }
        Spacer(Modifier.size(10.dp))
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)),
            shadowElevation = 8.dp,
            modifier = Modifier.size(width = 142.dp, height = 220.dp),
        ) {
            Column(
                modifier = Modifier.padding(13.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("MissaBusiness", style = MaterialTheme.typography.labelSmall, color = BrandBlue)
                Surface(color = BrandBlue, shape = RoundedCornerShape(8.dp)) {
                    Column(Modifier.padding(10.dp)) {
                        Text(
                            text = stringResource(R.string.ob_intro_dashboard_label),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.82f),
                        )
                        Text("360", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    MiniMetric("↗", ProfileGreen, Modifier.weight(1f))
                    MiniMetric("▣", ProfilePurple, Modifier.weight(1f))
                }
                Surface(
                    color = BrandBlue.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(7.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("⌁ 〰", color = BrandBlue, fontSize = 20.sp)
                    }
                }
            }
        }
        Spacer(Modifier.size(10.dp))
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            listOf(46.dp, 76.dp, 108.dp).forEach { height ->
                Surface(
                    color = ProfileGreen,
                    shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp),
                    modifier = Modifier.size(width = 20.dp, height = height),
                ) {}
            }
        }
    }
}

@Composable
private fun MiniMetric(symbol: String, color: Color, modifier: Modifier) {
    Surface(color = color.copy(alpha = 0.12f), shape = RoundedCornerShape(7.dp), modifier = modifier) {
        Text(
            text = symbol,
            color = color,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 8.dp),
        )
    }
}

@Composable
private fun FeatureArtwork() {
    Surface(
        color = BrandBlue.copy(alpha = 0.08f),
        shape = CircleShape,
        modifier = Modifier.size(106.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Outlined.Business,
                contentDescription = null,
                tint = BrandBlue,
                modifier = Modifier.size(58.dp),
            )
        }
    }
}

@Composable
private fun PresentationFeature(titleRes: Int, descriptionRes: Int, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Surface(color = color.copy(alpha = 0.10f), shape = CircleShape, modifier = Modifier.size(52.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = color)
            }
        }
        Column(modifier = Modifier.padding(start = 14.dp).weight(1f)) {
            Text(
                text = stringResource(titleRes),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(descriptionRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PresentationProgress(page: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        repeat(5) { index ->
            Box(
                modifier = Modifier
                    .size(if (index == page) 8.dp else 7.dp)
                    .background(if (index == page) BrandBlue else OnboardingBorder, CircleShape),
            )
        }
    }
}
