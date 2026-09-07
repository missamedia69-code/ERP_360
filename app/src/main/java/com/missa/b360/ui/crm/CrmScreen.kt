package com.missa.b360.ui.crm

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.missa.b360.R
import com.missa.b360.core.domain.model.FicheCrm
import com.missa.b360.core.domain.model.SegmentClient
import com.missa.b360.core.util.DateUtils
import com.missa.b360.core.util.Iso4217
import com.missa.b360.core.util.MoneyUtils
import com.missa.b360.ui.theme.BrandBlue
import com.missa.b360.ui.theme.MissaBorder
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted
import com.missa.b360.ui.theme.MissaSoftBlue
import com.missa.b360.ui.theme.MissaSurface
import com.missa.b360.ui.theme.ProfileGreen
import com.missa.b360.ui.theme.ProfileOrange
import com.missa.b360.ui.theme.Red40

/**
 * Module CRM (CRM) — portefeuille segmenté, relances à passer et meilleurs
 * clients.
 *
 * Il n'ouvre aucune fiche en écriture : la saisie reste au module Clients. Ce
 * que le CRM apporte, c'est la lecture commerciale de données déjà là — qui
 * n'achète plus, qui pèse, qui n'a jamais commandé.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrmScreen(
    onBack: () -> Unit,
    viewModel: CrmViewModel = hiltViewModel(),
) {
    val etat by viewModel.etat.collectAsState()
    val devise by viewModel.devise.collectAsState()
    val filtre by viewModel.filtre.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.module_crm)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.ob_retour),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MissaSurface),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            item { CrmSyntheseCarte(etat, devise) }

            if (etat.compteurs.isNotEmpty()) {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        items(etat.compteurs, key = { it.segment.name }) { compteur ->
                            FilterChip(
                                selected = filtre == compteur.segment,
                                onClick = { viewModel.filtrer(compteur.segment) },
                                label = {
                                    Text(
                                        text = stringResource(compteur.segment.libelleRes) +
                                            " · ${compteur.nombre}",
                                        fontSize = 11.5.sp,
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BrandBlue.copy(alpha = 0.15f),
                                ),
                            )
                        }
                    }
                }
            }

            if (etat.relances.isNotEmpty() && filtre == null) {
                item { CrmRelancesCarte(etat.relances.size) }
            }

            if (etat.top.isNotEmpty() && filtre == null) {
                item {
                    CrmSectionTitre(R.string.crm_top_clients, Icons.Outlined.Star)
                }
                items(etat.top, key = { "top-" + it.client.id }) { fiche ->
                    CrmFicheLigne(fiche, devise, rang = etat.top.indexOf(fiche) + 1)
                }
            }

            item {
                CrmSectionTitre(
                    if (filtre == null) R.string.crm_portefeuille else R.string.crm_selection,
                    Icons.Outlined.Groups,
                )
            }

            if (etat.fiches.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.crm_aucun_client),
                        fontSize = 12.sp,
                        color = MissaMuted,
                    )
                }
            } else {
                items(etat.fiches, key = { it.client.id }) { fiche ->
                    CrmFicheLigne(fiche, devise)
                }
            }
        }
    }
}

@Composable
private fun CrmSyntheseCarte(etat: CrmViewModel.EtatCrm, devise: String) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MissaSoftBlue),
        border = BorderStroke(1.dp, BrandBlue.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(horizontal = 15.dp, vertical = 13.dp)) {
            CrmChiffre(
                libelleRes = R.string.crm_ca_portefeuille,
                valeur = MoneyUtils.format(etat.chiffreAffaires, devise),
                modifier = Modifier.weight(1.2f),
            )
            CrmChiffre(
                libelleRes = R.string.crm_taux_conversion,
                valeur = Iso4217.formatPourcentage(etat.tauxConversion),
                modifier = Modifier.weight(1f),
            )
            CrmChiffre(
                libelleRes = R.string.crm_panier_moyen,
                valeur = MoneyUtils.format(etat.panierMoyen, devise),
                modifier = Modifier.weight(1.2f),
            )
        }
    }
}

@Composable
private fun CrmChiffre(libelleRes: Int, valeur: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = valeur,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MissaInk,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = stringResource(libelleRes),
            fontSize = 10.sp,
            color = MissaMuted,
        )
    }
}

@Composable
private fun CrmRelancesCarte(nombre: Int) {
    Card(
        shape = RoundedCornerShape(13.dp),
        colors = CardDefaults.cardColors(containerColor = ProfileOrange.copy(alpha = 0.12f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.NotificationsActive,
                contentDescription = null,
                tint = ProfileOrange,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(9.dp))
            Text(
                text = stringResource(R.string.crm_relances_message, nombre),
                fontSize = 12.sp,
                color = MissaInk,
            )
        }
    }
}

@Composable
private fun CrmSectionTitre(titreRes: Int, icone: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
        Icon(
            imageVector = icone,
            contentDescription = null,
            tint = BrandBlue,
            modifier = Modifier.size(15.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = stringResource(titreRes),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MissaInk,
        )
    }
}

/** Une fiche du portefeuille : segment, chiffre d'affaires, dernier contact. */
@Composable
private fun CrmFicheLigne(fiche: FicheCrm, devise: String, rang: Int? = null) {
    val couleur = when (fiche.segment) {
        SegmentClient.A_RELANCER -> ProfileOrange
        SegmentClient.DORMANT -> Red40
        SegmentClient.FIDELE -> ProfileGreen
        SegmentClient.NOUVEAU -> BrandBlue
        else -> MissaMuted
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            color = couleur.copy(alpha = 0.13f),
            shape = CircleShape,
            modifier = Modifier.size(30.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = rang?.toString() ?: fiche.client.nom.take(1).uppercase(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = couleur,
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = fiche.client.nom,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Medium,
                color = MissaInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = listOfNotNull(
                    stringResource(fiche.segment.libelleRes),
                    fiche.dernierAchat?.let { DateUtils.formatDate(it) }
                        ?: stringResource(R.string.crm_jamais_achete),
                    fiche.nombreAchats.takeIf { it > 0 }
                        ?.let { stringResource(R.string.crm_nb_achats, it) },
                ).joinToString(" · "),
                fontSize = 10.5.sp,
                color = MissaMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (fiche.chiffreAffaires > 0) {
            Spacer(Modifier.width(8.dp))
            Text(
                text = MoneyUtils.format(fiche.chiffreAffaires, devise),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MissaInk,
            )
        }
    }
    HorizontalDivider(color = MissaBorder, modifier = Modifier.padding(top = 6.dp))
}
