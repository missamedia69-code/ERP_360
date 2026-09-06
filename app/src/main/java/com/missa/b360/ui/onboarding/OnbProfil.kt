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
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Construction
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Workspaces
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.missa.b360.R
import com.missa.b360.core.domain.model.PalierTaille
import com.missa.b360.core.domain.model.ProfilActivite
import com.missa.b360.ui.theme.BrandBlue
import com.missa.b360.ui.theme.MissaBorder
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted
import com.missa.b360.ui.theme.MissaSurface

/**
 * Écran — Profil d'activité : les cinq familles de la maquette sélectionnent les
 * profils détaillés existants (A–H) ; « Plus de détails » conserve le choix fin.
 */
@Composable
internal fun OnbProfilStep(viewModel: OnboardingViewModel) {
    var detailsVisibles by rememberSaveable { mutableStateOf(false) }
    OnbScaffold(
        titreRes = R.string.obn_profil_titre,
        sousTitreRes = R.string.obn_profil_sous,
        viewModel = viewModel,
        boutonActive = viewModel.profil != null,
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
            TextButton(
                onClick = { detailsVisibles = !detailsVisibles },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text(
                    stringResource(
                        if (detailsVisibles) R.string.ob_profil_masquer_details
                        else R.string.obn_detaux,
                    ),
                    fontSize = 13.sp,
                )
            }
            if (detailsVisibles) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (profile in ProfilActivite.entries) {
                        Card(
                            onClick = { viewModel.choisirProfil(profile) },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(
                                if (profile == viewModel.profil) 1.5.dp else 1.dp,
                                if (profile == viewModel.profil) BrandBlue else MissaBorder,
                            ),
                            colors = CardDefaults.cardColors(containerColor = MissaSurface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(profile.labelRes),
                                        fontSize = 13.5.sp,
                                        color = MissaInk,
                                    )
                                    Text(
                                        text = profile.description,
                                        fontSize = 11.sp,
                                        color = MissaMuted,
                                    )
                                }
                                if (profile == viewModel.profil) {
                                    Icon(
                                        imageVector = Icons.Outlined.ChevronRight,
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
            OnbEffectifChamp(
                selection = viewModel.palier,
                onSelect = viewModel::choisirPalier,
                enabled = !viewModel.enregistrementEnCours,
            )
        }
    }
}

/**
 * Champ bleu « Nombre d'employés » en bas de l'écran : liste déroulante des six
 * paliers d'effectif (P1–P6). Le choix remplace l'ancien écran dédié : il est
 * conservé immédiatement et repris sur la fiche entreprise.
 */
@Composable
private fun OnbEffectifChamp(
    selection: PalierTaille?,
    onSelect: (PalierTaille) -> Unit,
    enabled: Boolean,
) {
    var ouvert by remember { mutableStateOf(false) }
    val libelle = selection?.let { stringResource(it.labelRes) }
        ?: stringResource(R.string.obn_effectif_placeholder)
    Box(modifier = Modifier.fillMaxWidth()) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = BrandBlue,
            modifier = Modifier
                .fillMaxWidth()
                .then(if (enabled) Modifier.clickable { ouvert = !ouvert } else Modifier),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 15.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Groups,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.obn_effectif_label),
                        fontSize = 11.5.sp,
                        color = Color.White.copy(alpha = 0.75f),
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = libelle,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                    )
                }
                Icon(
                    imageVector = Icons.Outlined.ArrowDropDown,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        DropdownMenu(
            expanded = ouvert,
            onDismissRequest = { ouvert = false },
        ) {
            for (palier in PalierTaille.entries) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(palier.labelRes),
                            fontSize = 13.sp,
                            fontWeight = if (palier == selection) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (palier == selection) BrandBlue else MissaInk,
                        )
                    },
                    onClick = {
                        onSelect(palier)
                        ouvert = false
                    },
                )
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
