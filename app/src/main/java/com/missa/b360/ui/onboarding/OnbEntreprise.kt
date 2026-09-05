package com.missa.b360.ui.onboarding

import android.content.Intent
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.missa.b360.R
import com.missa.b360.core.util.Iso4217
import com.missa.b360.ui.components.CompanyLogo
import com.missa.b360.ui.theme.BrandBlue
import com.missa.b360.ui.theme.MissaBorder
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted
import com.missa.b360.ui.theme.MissaSurface
import com.missa.b360.ui.theme.Red40

private val IMAGE_MIME_TYPES = arrayOf("image/png", "image/jpeg", "image/webp")

private const val LOGO_MAX_BYTES = 2L * 1024 * 1024

/**
 * Écran 4 — Informations sur votre entreprise : identité, pays, devise et
 * **logo de l'entreprise (fusionné dans cet écran selon la maquette)**.
 * Le site principal et le taux de TVA/D5 restent accessibles en « Plus de détails ».
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun OnbEntrepriseStep(viewModel: OnboardingViewModel) {
    var siteModifieManuellement by remember { mutableStateOf(viewModel.nomSitePrincipal.isNotBlank()) }
    var detailsVisibles by remember { mutableStateOf(false) }
    val locale = LocalConfiguration.current.locales[0]
    val paysListe = remember(locale) { Iso4217.paysDisponibles(locale) }
    var deviseOuvert by remember { mutableStateOf(false) }
    var paysOuvert by remember { mutableStateOf(false) }
    var saisiePaysManuelle by remember { mutableStateOf(false) }
    var recherchePays by remember { mutableStateOf("") }
    val deviseChoisie = Iso4217.COMMUNES.firstOrNull { it.code == viewModel.devise }
    val paysFiltres = remember(paysListe, recherchePays) {
        val requete = recherchePays.trim()
        if (requete.isEmpty()) paysListe else paysListe.filter { pays ->
            pays.nom.contains(requete, ignoreCase = true) ||
                pays.code.contains(requete, ignoreCase = true)
        }
    }
    val tauxTaxeInvalide = !viewModel.tauxTaxeEstValide()

    OnbScaffold(
        titreRes = R.string.obn_entreprise_titre,
        sousTitreRes = R.string.obn_entreprise_sous,
        viewModel = viewModel,
        boutonPleineLargeur = true,
        boutonActive = !viewModel.enregistrementEnCours,
        onRetour = viewModel::precedent,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // --- Identité de l'entreprise ---
            Card(
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MissaBorder),
                colors = CardDefaults.cardColors(containerColor = MissaSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    OnbChamp(
                        icone = Icons.Outlined.Business,
                        labelRes = R.string.ob_nom_entreprise,
                    ) {
                        OutlinedTextField(
                            value = viewModel.nomEntreprise,
                            onValueChange = { nom ->
                                val ancienNom = viewModel.nomEntreprise
                                viewModel.nomEntreprise = nom
                                if (!siteModifieManuellement || viewModel.nomSitePrincipal == ancienNom) {
                                    viewModel.nomSitePrincipal = nom
                                }
                            },
                            placeholder = { Text(stringResource(R.string.obn_nom_ex)) },
                            singleLine = true,
                            enabled = !viewModel.enregistrementEnCours,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    OnbChamp(
                        icone = Icons.Outlined.Category,
                        labelRes = R.string.obn_secteur,
                    ) {
                        OutlinedTextField(
                            value = viewModel.secteur,
                            onValueChange = { viewModel.secteur = it },
                            placeholder = { Text(stringResource(R.string.obn_secteur_ex)) },
                            singleLine = true,
                            enabled = !viewModel.enregistrementEnCours,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    OnbChamp(
                        icone = Icons.Outlined.Storefront,
                        labelRes = R.string.ob_pays,
                    ) {
                        ExposedDropdownMenuBox(
                            expanded = paysOuvert,
                            onExpandedChange = { ouvert ->
                                paysOuvert = ouvert
                                if (ouvert) recherchePays = ""
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            OutlinedTextField(
                                value = if (paysOuvert) recherchePays else viewModel.pays,
                                onValueChange = {
                                    recherchePays = it
                                    paysOuvert = true
                                },
                                placeholder = { Text(stringResource(R.string.ob_selectionne)) },
                                readOnly = viewModel.pays.isNotEmpty() && !paysOuvert,
                                singleLine = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = paysOuvert) },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                            )
                            ExposedDropdownMenu(
                                expanded = paysOuvert,
                                onDismissRequest = {
                                    paysOuvert = false
                                    recherchePays = ""
                                },
                            ) {
                                if (paysFiltres.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.ob_pays_aucun_resultat)) },
                                        onClick = {},
                                        enabled = false,
                                    )
                                } else {
                                    for (pays in paysFiltres) {
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    "${pays.nom} (${pays.code}) · " +
                                                        "${stringResource(R.string.ob_tva_gst)} ${pays.libelleTaxe}",
                                                )
                                            },
                                            onClick = {
                                                viewModel.choisirPays(pays.nom, pays.code, pays.tauxTaxeSuggere)
                                                recherchePays = ""
                                                paysOuvert = false
                                            },
                                        )
                                    }
                                }
                            }
                        }
                        TextButton(
                            onClick = { saisiePaysManuelle = !saisiePaysManuelle },
                            modifier = Modifier.align(Alignment.Start),
                        ) {
                            Text(stringResource(R.string.ob_pays_saisie_manuelle), fontSize = 12.5.sp)
                        }
                        if (saisiePaysManuelle) {
                            OutlinedTextField(
                                value = viewModel.pays,
                                onValueChange = viewModel::modifierPaysManuel,
                                label = { Text(stringResource(R.string.ob_pays_personnalise)) },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    OnbChamp(
                        icone = Icons.Outlined.Payments,
                        labelRes = R.string.obn_devise_principale,
                    ) {
                        ExposedDropdownMenuBox(
                            expanded = deviseOuvert,
                            onExpandedChange = { deviseOuvert = it },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            OutlinedTextField(
                                value = deviseChoisie?.let { "${it.code} · ${it.nom}" } ?: viewModel.devise,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = deviseOuvert) },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                            )
                            ExposedDropdownMenu(
                                expanded = deviseOuvert,
                                onDismissRequest = { deviseOuvert = false },
                            ) {
                                for (devise in Iso4217.COMMUNES) {
                                    DropdownMenuItem(
                                        text = { Text("${devise.code} · ${devise.nom}") },
                                        onClick = {
                                            viewModel.devise = devise.code
                                            deviseOuvert = false
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // --- Logo de l'entreprise (jumeau de l'écran informations) ---
            OnbLogoCard(
                logoUri = viewModel.logoUri,
                enabled = !viewModel.enregistrementEnCours,
                onLogoSelected = viewModel::definirLogoUri,
                onLogoCleared = { viewModel.definirLogoUri(null) },
            )

            // --- Plus de détails : site principal + taux de taxe ---
            TextButton(
                onClick = { detailsVisibles = !detailsVisibles },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text(
                    stringResource(
                        if (detailsVisibles) R.string.ob_profil_masquer_details else R.string.obn_detaux,
                    ),
                    fontSize = 13.sp,
                )
            }
            if (detailsVisibles) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MissaBorder),
                    colors = CardDefaults.cardColors(containerColor = MissaSurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        OutlinedTextField(
                            value = viewModel.nomSitePrincipal,
                            onValueChange = {
                                siteModifieManuellement = true
                                viewModel.nomSitePrincipal = it
                            },
                            label = { Text(stringResource(R.string.ob_site_principal)) },
                            singleLine = true,
                            enabled = !viewModel.enregistrementEnCours,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = viewModel.tauxTaxeTexte,
                            onValueChange = viewModel::modifierTauxTaxe,
                            label = { Text(stringResource(R.string.ob_taux_taxe)) },
                            singleLine = true,
                            isError = tauxTaxeInvalide,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            supportingText = if (tauxTaxeInvalide) {
                                { Text(stringResource(R.string.ob_erreur_taux_taxe)) }
                            } else {
                                null
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Bloc champ de la maquette : icône + libellé au-dessus du champ, dans la même carte.
 */
@Composable
private fun OnbChamp(
    icone: ImageVector,
    labelRes: Int,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = icone,
            contentDescription = null,
            tint = BrandBlue,
            modifier = Modifier
                .padding(top = 15.dp)
                .size(18.dp),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp),
        ) {
            Text(
                text = stringResource(labelRes),
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = MissaInk,
            )
            Spacer(Modifier.height(5.dp))
            content()
        }
    }
}

/**
 * Zone de logo de la maquette : cadre pointillé, nuage de dépôt, bouton
 * « Parcourir », formats acceptés et limite 2MB vérifiée avant acceptation.
 */
@Composable
private fun OnbLogoCard(
    logoUri: String?,
    enabled: Boolean,
    onLogoSelected: (String) -> Unit,
    onLogoCleared: () -> Unit,
) {
    val context = LocalContext.current
    var logoTropGrand by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let {
            // OpenDocument fournit un droit persistant. Un fournisseur non compatible ne
            // bloque pas le choix : le droit temporaire permet tout de même l'aperçu en cours.
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            val taille = runCatching {
                context.contentResolver.query(it, null, null, null, null)?.use { cursor ->
                    val idx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (idx >= 0 && cursor.moveToFirst()) cursor.getLong(idx) else -1L
                } ?: -1L
            }.getOrDefault(-1L)
            if (taille in 1..LOGO_MAX_BYTES) {
                logoTropGrand = false
                onLogoSelected(it.toString())
            } else {
                logoTropGrand = true
            }
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MissaBorder),
        colors = CardDefaults.cardColors(containerColor = MissaSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.obn_logo_titre),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MissaInk,
            )
            Text(
                text = stringResource(R.string.obn_logo_optionnel),
                fontSize = 12.sp,
                color = MissaMuted,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.obn_logo_description),
                fontSize = 12.5.sp,
                color = MissaMuted,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))

            if (logoUri == null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .dashedBorder(1.2.dp, MissaBorder, 14.dp),
                    color = BrandBlue.copy(alpha = 0.025f),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Backup,
                            contentDescription = null,
                            tint = BrandBlue,
                            modifier = Modifier.size(38.dp),
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.obn_logo_deposer),
                            fontSize = 13.sp,
                            color = MissaInk,
                        )
                        Text(
                            text = stringResource(R.string.obn_logo_ou),
                            fontSize = 12.sp,
                            color = MissaMuted,
                        )
                        Spacer(Modifier.height(6.dp))
                        OutlinedButton(
                            onClick = { launcher.launch(IMAGE_MIME_TYPES) },
                            enabled = enabled,
                            shape = RoundedCornerShape(9.dp),
                        ) {
                            Text(stringResource(R.string.obn_logo_parcourir), fontSize = 13.sp)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.obn_logo_formats),
                    fontSize = 11.sp,
                    color = MissaMuted,
                    textAlign = TextAlign.Center,
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CompanyLogo(
                        logoUri = logoUri,
                        contentDescription = stringResource(R.string.ob_logo_apercu),
                        fallbackIcon = Icons.Outlined.Backup,
                        modifier = Modifier.size(84.dp),
                        size = 72.dp,
                        shape = RoundedCornerShape(14.dp),
                        fallbackTint = BrandBlue,
                        fallbackBackground = BrandBlue.copy(alpha = 0.07f),
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        OutlinedButton(
                            onClick = { launcher.launch(IMAGE_MIME_TYPES) },
                            enabled = enabled,
                            shape = RoundedCornerShape(9.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(stringResource(R.string.ob_logo_modifier), fontSize = 13.sp)
                        }
                        TextButton(onClick = onLogoCleared, enabled = enabled) {
                            Text(stringResource(R.string.ob_logo_supprimer), fontSize = 13.sp, color = Red40)
                        }
                    }
                }
            }
            if (logoTropGrand) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.obn_logo_trop_grand),
                    fontSize = 12.sp,
                    color = Red40,
                )
            }
        }
    }
}

/** Bordure pointillée du cadre de dépôt de logo (maquette). */
private fun Modifier.dashedBorder(width: androidx.compose.ui.unit.Dp, color: androidx.compose.ui.graphics.Color, radius: androidx.compose.ui.unit.Dp): Modifier =
    this.drawBehind {
        val r = radius.toPx()
        val path = Path().apply {
            addRoundRect(
                androidx.compose.ui.geometry.Rect(Offset.Zero, Offset(size.width, size.height)),
                r,
                r,
            )
        }
        drawPath(
            path = path,
            color = SolidColor(color),
            style = Stroke(
                width = width.toPx(),
                pathEffect = PathEffect.dashPathEffect(
                    floatArrayOf(7f * density, 6f * density),
                    0f,
                ),
            ),
        )
    }
