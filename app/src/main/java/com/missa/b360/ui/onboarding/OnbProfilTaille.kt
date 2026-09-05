package com.missa.b360.ui.onboarding

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Construction
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Store
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Workspaces
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import com.missa.b360.R
import com.missa.b360.core.domain.model.PalierTaille
import com.missa.b360.core.domain.model.ProfilActivite
import com.missa.b360.ui.theme.BrandBlue
import com.missa.b360.ui.theme.MissaBorder
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted
import com.missa.b360.ui.theme.MissaSurface

/**
 * Écran 2 — Profil d'activité : les cinq familles de la maquette sélectionnent les
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
                titreRes = R.string.obn_profil_commerce,
                sousTitreRes = R.string.obn_profil_commerce_sous,
                icone = Icons.Outlined.ShoppingCart,
                selected = viewModel.profil == ProfilActivite.B,
                onClick = { viewModel.choisirProfil(ProfilActivite.B) },
            )
            OnbProfilCarte(
                titreRes = R.string.obn_profil_comstock,
                sousTitreRes = R.string.obn_profil_comstock_sous,
                icone = Icons.Outlined.Inventory2,
                selected = viewModel.profil == ProfilActivite.D,
                onClick = { viewModel.choisirProfil(ProfilActivite.D) },
            )
            OnbProfilCarte(
                titreRes = R.string.obn_profil_production,
                sousTitreRes = R.string.obn_profil_production_sous,
                icone = Icons.Outlined.Construction,
                selected = viewModel.profil == ProfilActivite.E,
                onClick = { viewModel.choisirProfil(ProfilActivite.E) },
            )
            OnbProfilCarte(
                titreRes = R.string.obn_profil_services,
                sousTitreRes = R.string.obn_profil_services_sous,
                icone = Icons.Outlined.Handshake,
                selected = viewModel.profil == ProfilActivite.F,
                onClick = { viewModel.choisirProfil(ProfilActivite.F) },
            )
            OnbProfilCarte(
                titreRes = R.string.obn_profil_autre,
                sousTitreRes = R.string.obn_profil_autre_sous,
                icone = Icons.Outlined.Workspaces,
                selected = viewModel.profil == ProfilActivite.H,
                onClick = { viewModel.choisirProfil(ProfilActivite.H) },
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
                                Text(
                                    text = stringResource(profile.labelRes),
                                    fontSize = 13.5.sp,
                                    color = MissaInk,
                                    modifier = Modifier.weight(1f),
                                )
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

/**
 * Écran 3 — Taille de l'entreprise : les six paliers existants (P1–P6) avec leurs
 * fourchettes d'effectif, présentés comme des options de la maquette.
 */
@Composable
internal fun OnbTailleStep(viewModel: OnboardingViewModel) {
    val icones = listOf(
        Icons.Outlined.Person,
        Icons.Outlined.Group,
        Icons.Outlined.Groups,
        Icons.Outlined.Store,
        Icons.Outlined.Storefront,
        Icons.Outlined.Business,
    )
    OnbScaffold(
        titreRes = R.string.obn_taille_titre,
        sousTitreRes = R.string.obn_taille_sous,
        viewModel = viewModel,
        boutonActive = viewModel.palier != null,
        onRetour = viewModel::precedent,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            for (index in PalierTaille.entries.indices) {
                val palier = PalierTaille.entries[index]
                Card(
                    onClick = { viewModel.choisirPalier(palier) },
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(
                        if (palier == viewModel.palier) 1.5.dp else 1.dp,
                        if (palier == viewModel.palier) BrandBlue else MissaBorder,
                    ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (palier == viewModel.palier) BrandBlue.copy(alpha = 0.045f) else MissaSurface,
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
                                    imageVector = icones.getOrElse(index) { Icons.Outlined.Business },
                                    contentDescription = null,
                                    tint = BrandBlue,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Text(
                            text = stringResource(palier.labelRes),
                            fontSize = 14.5.sp,
                            fontWeight = if (palier == viewModel.palier) FontWeight.SemiBold else FontWeight.Normal,
                            color = MissaInk,
                            modifier = Modifier.weight(1f),
                        )
                        if (palier == viewModel.palier) {
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
        }
    }
}
