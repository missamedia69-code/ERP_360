package com.missa.b360.ui.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Construction
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Workspaces
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.missa.b360.R
import com.missa.b360.core.domain.model.ModuleCode
import com.missa.b360.core.domain.model.ModuleSousElements
import com.missa.b360.core.domain.model.ModulesPersonnalises
import com.missa.b360.core.domain.model.PalierTaille
import com.missa.b360.core.domain.model.ProfilActivite
import com.missa.b360.ui.components.MissaOption
import com.missa.b360.ui.components.MissaSelecteurBleu
import com.missa.b360.ui.theme.BrandBlue
import com.missa.b360.ui.theme.MissaBorder
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted
import com.missa.b360.ui.theme.MissaSurface
import com.missa.b360.ui.theme.Red40

/**
 * Écran — Profil d'activité : les cinq familles de la maquette sélectionnent les
 * profils détaillés existants (A–H) ; « Plus de détails » conserve le choix fin.
 */
@Composable
internal fun OnbProfilStep(viewModel: OnboardingViewModel) {
    OnbScaffold(
        titreRes = R.string.obn_profil_titre,
        sousTitreRes = R.string.obn_profil_sous,
        viewModel = viewModel,
        boutonActive = viewModel.profilEcranValide(),
        onRetour = viewModel::precedent,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            OnbProfilCarte(
                titreRes = R.string.obn_profil_av,
                sousTitreRes = R.string.obn_profil_av_sous,
                icone = Icons.Outlined.ShoppingCart,
                selected = viewModel.profil == ProfilActivite.AV,
                onClick = { viewModel.choisirProfil(ProfilActivite.AV) },
            )
            OnbProfilCarte(
                titreRes = R.string.obn_profil_asv,
                sousTitreRes = R.string.obn_profil_asv_sous,
                icone = Icons.Outlined.Inventory2,
                selected = viewModel.profil == ProfilActivite.ASV,
                onClick = { viewModel.choisirProfil(ProfilActivite.ASV) },
            )
            OnbProfilCarte(
                titreRes = R.string.obn_profil_apsv,
                sousTitreRes = R.string.obn_profil_apsv_sous,
                icone = Icons.Outlined.Construction,
                selected = viewModel.profil == ProfilActivite.APSV,
                onClick = { viewModel.choisirProfil(ProfilActivite.APSV) },
            )
            OnbProfilCarte(
                titreRes = R.string.obn_profil_ser,
                sousTitreRes = R.string.obn_profil_ser_sous,
                icone = Icons.Outlined.Handshake,
                selected = viewModel.profil == ProfilActivite.SER,
                onClick = { viewModel.choisirProfil(ProfilActivite.SER) },
            )
            OnbProfilCarte(
                titreRes = R.string.obn_profil_prj,
                sousTitreRes = R.string.obn_profil_prj_sous,
                icone = Icons.Outlined.Workspaces,
                selected = viewModel.profil == ProfilActivite.PRJ,
                onClick = { viewModel.choisirProfil(ProfilActivite.PRJ) },
            )
            OnbProfilCarte(
                titreRes = R.string.obn_profil_full,
                sousTitreRes = R.string.obn_profil_full_sous,
                icone = Icons.Outlined.Business,
                selected = viewModel.profil == ProfilActivite.FULL,
                onClick = { viewModel.choisirProfil(ProfilActivite.FULL) },
            )
            OnbProfilCarte(
                titreRes = R.string.profil_custom,
                sousTitreRes = R.string.obn_profil_perso_sous,
                icone = Icons.Outlined.Tune,
                selected = viewModel.profil == ProfilActivite.CUSTOM,
                onClick = viewModel::choisirPersonnalisation,
            )
            if (viewModel.profil == ProfilActivite.CUSTOM) {
                OnbModulesPersonnalises(viewModel = viewModel)
            }
            MissaSelecteurBleu(
                label = stringResource(R.string.obn_effectif_label),
                options = PalierTaille.entries.map { palier ->
                    MissaOption(cle = palier.name, titre = stringResource(palier.labelRes))
                },
                selectionCle = viewModel.palier?.name,
                onSelection = { cle ->
                    runCatching { PalierTaille.valueOf(cle) }.getOrNull()
                        ?.let(viewModel::choisirPalier)
                },
                icone = Icons.Outlined.Groups,
                enabled = !viewModel.enregistrementEnCours,
                placeholder = stringResource(R.string.obn_effectif_placeholder),
            )
        }
    }
}

/**
 * Personnalisation des modules (profil « Personnalisé ») : les 14 modules métier
 * cochables un à un, avec le nombre de fonctionnalités que chacun apporte. La
 * sélection est conservée immédiatement et pilote les modules actifs de l'app.
 */
@Composable
private fun OnbModulesPersonnalises(viewModel: OnboardingViewModel) {
    val selection = viewModel.modulesPersonnalises
    val tousCoches = selection.size == ModuleCode.entries.size
    Card(
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (selection.isEmpty()) Red40 else BrandBlue),
        colors = CardDefaults.cardColors(containerColor = MissaSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.obn_profil_perso_compteur, selection.size),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selection.isEmpty()) Red40 else MissaInk,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = viewModel::basculerTousLesModules) {
                    Text(
                        text = stringResource(
                            if (tousCoches) R.string.obn_profil_perso_rien
                            else R.string.obn_profil_perso_tout,
                        ),
                        fontSize = 12.sp,
                    )
                }
            }
            if (selection.isEmpty()) {
                Text(
                    text = stringResource(R.string.obn_profil_perso_vide),
                    fontSize = 11.5.sp,
                    color = Red40,
                )
            }
            Spacer(Modifier.height(4.dp))
            for (module in ModuleCode.entries) {
                val actif = module in selection
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.basculerModule(module) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = actif,
                        onCheckedChange = { viewModel.basculerModule(module) },
                        colors = CheckboxDefaults.colors(checkedColor = BrandBlue),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(ModulesPersonnalises.libelleRes(module)),
                            fontSize = 13.sp,
                            fontWeight = if (actif) FontWeight.SemiBold else FontWeight.Normal,
                            color = MissaInk,
                        )
                        Text(
                            text = stringResource(
                                R.string.obn_profil_perso_fonctions,
                                ModuleSousElements.pourModule(module).size,
                            ),
                            fontSize = 11.sp,
                            color = MissaMuted,
                        )
                    }
                }
            }
        }
    }
}

/** Carte d'option de la maquette : puce iconée, titre, sous-titre, chevron si sélectionné. */
@Composable
internal fun OnbProfilCarte(
    titreRes: Int,
    sousTitreRes: Int,
    icone: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            if (selected) 1.5.dp else 1.dp,
            if (selected) BrandBlue else MissaBorder,
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) BrandBlue.copy(alpha = 0.045f) else MissaSurface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 15.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = RoundedCornerShape(11.dp),
                color = BrandBlue.copy(alpha = 0.09f),
                modifier = Modifier.size(42.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icone,
                        contentDescription = null,
                        tint = BrandBlue,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(titreRes),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MissaInk,
                )
                Text(
                    text = stringResource(sousTitreRes),
                    fontSize = 12.5.sp,
                    color = MissaMuted,
                )
            }
            if (selected) {
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = BrandBlue,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}
