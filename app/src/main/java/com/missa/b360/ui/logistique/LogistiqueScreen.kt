package com.missa.b360.ui.logistique

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Store
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
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
import com.missa.b360.core.domain.model.EtatTransfert
import com.missa.b360.core.domain.model.StockDuSite
import com.missa.b360.core.domain.model.Transfert
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
 * Module Logistique (LOG) — implantation du stock par site, transferts
 * inter-sites et suivi des livraisons.
 *
 * Sa raison d'être : le module Stock montre un site à la fois. La logistique
 * montre le réseau, et surtout ce qui est parti d'un site sans jamais arriver
 * à l'autre.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogistiqueScreen(
    onBack: () -> Unit,
    viewModel: LogistiqueViewModel = hiltViewModel(),
) {
    val etat by viewModel.etat.collectAsState()
    val devise by viewModel.devise.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.module_logistique)) },
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
            item { LogSyntheseCarte(etat, devise) }

            if (etat.enTransit > 0) {
                item { LogAlerteTransit(etat.enTransit) }
            }

            item { LogSectionTitre(R.string.log_implantation, Icons.Outlined.Store) }

            if (etat.sites.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.log_aucun_site),
                        fontSize = 12.sp,
                        color = MissaMuted,
                    )
                }
            } else {
                items(etat.sites, key = { it.site.id }) { site ->
                    LogSiteLigne(site, devise, etat.valeurTotale)
                }
            }

            item { LogSectionTitre(R.string.log_transferts, Icons.Outlined.SwapHoriz) }

            if (etat.transferts.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.log_aucun_transfert),
                        fontSize = 12.sp,
                        color = MissaMuted,
                    )
                }
            } else {
                items(etat.transferts, key = { it.reference }) { transfert ->
                    LogTransfertLigne(transfert, etat.sites)
                }
            }
        }
    }
}

@Composable
private fun LogSyntheseCarte(etat: LogistiqueViewModel.EtatLogistique, devise: String) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MissaSoftBlue),
        border = BorderStroke(1.dp, BrandBlue.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(horizontal = 15.dp, vertical = 13.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                LogChiffre(
                    R.string.log_valeur_stock,
                    MoneyUtils.format(etat.valeurTotale, devise),
                    Modifier.weight(1.3f),
                )
                LogChiffre(
                    R.string.log_sites,
                    etat.sites.size.toString(),
                    Modifier.weight(0.7f),
                )
                LogChiffre(
                    R.string.log_taux_service,
                    Iso4217.formatPourcentage(etat.livraisons.tauxService),
                    Modifier.weight(1f),
                )
            }
            if (etat.livraisons.total > 0) {
                Spacer(Modifier.size(8.dp))
                HorizontalDivider(color = BrandBlue.copy(alpha = 0.2f))
                Spacer(Modifier.size(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.LocalShipping,
                        contentDescription = null,
                        tint = BrandBlue,
                        modifier = Modifier.size(15.dp),
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(
                        text = stringResource(
                            R.string.log_livraisons_detail,
                            etat.livraisons.effectuees,
                            etat.livraisons.enPreparation,
                        ),
                        fontSize = 11.5.sp,
                        color = MissaInk,
                    )
                }
            }
        }
    }
}

@Composable
private fun LogChiffre(libelleRes: Int, valeur: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = valeur,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MissaInk,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(text = stringResource(libelleRes), fontSize = 10.sp, color = MissaMuted)
    }
}

@Composable
private fun LogAlerteTransit(nombre: Int) {
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
                imageVector = Icons.Outlined.Warning,
                contentDescription = null,
                tint = ProfileOrange,
                modifier = Modifier.size(17.dp),
            )
            Spacer(Modifier.width(9.dp))
            Text(
                text = stringResource(R.string.log_alerte_transit, nombre),
                fontSize = 12.sp,
                color = MissaInk,
            )
        }
    }
}

@Composable
private fun LogSectionTitre(titreRes: Int, icone: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
        Icon(icone, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            text = stringResource(titreRes),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MissaInk,
        )
    }
}

/** Un site et sa part du stock total, barre de proportion comprise. */
@Composable
private fun LogSiteLigne(site: StockDuSite, devise: String, valeurTotale: Double) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = site.site.nom,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = MissaInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = listOfNotNull(
                        stringResource(R.string.log_references, site.references),
                        site.ruptures.takeIf { it > 0 }
                            ?.let { stringResource(R.string.log_ruptures, it) },
                    ).joinToString(" · "),
                    fontSize = 10.5.sp,
                    color = if (site.ruptures > 0) Red40 else MissaMuted,
                )
            }
            Text(
                text = MoneyUtils.format(site.valeur, devise),
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = MissaInk,
            )
        }
        HorizontalDivider(color = MissaBorder, modifier = Modifier.padding(top = 6.dp))
    }
}

/** Un transfert et son état : reçu, en transit, ou saisie incomplète. */
@Composable
private fun LogTransfertLigne(transfert: Transfert, sites: List<StockDuSite>) {
    fun nomSite(id: Long?): String? = sites.firstOrNull { it.site.id == id }?.site?.nom
    val couleur = when (transfert.etat) {
        EtatTransfert.RECU -> ProfileGreen
        EtatTransfert.EN_TRANSIT -> ProfileOrange
        EtatTransfert.ORPHELIN -> Red40
    }
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Outlined.SwapHoriz,
            contentDescription = null,
            tint = couleur,
            modifier = Modifier.size(17.dp),
        )
        Spacer(Modifier.width(9.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = listOfNotNull(nomSite(transfert.siteSource), nomSite(transfert.siteDestination))
                    .joinToString(" → ")
                    .ifBlank { transfert.reference },
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Medium,
                color = MissaInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = listOf(
                    transfert.reference,
                    DateUtils.formatDate(transfert.date),
                    stringResource(transfert.etat.libelleRes),
                ).joinToString(" · "),
                fontSize = 10.5.sp,
                color = couleur,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = MoneyUtils.formatBrut(transfert.quantite),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MissaInk,
        )
    }
}
