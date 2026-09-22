package com.missa.b360.ui.logistique

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.missa.b360.R
import com.missa.b360.core.domain.model.EtatTransfert
import com.missa.b360.core.domain.model.StockDuSite
import com.missa.b360.core.domain.model.Transfert
import com.missa.b360.ui.components.MissaEmptyState
import com.missa.b360.ui.components.MissaTopAppBar
import com.missa.b360.ui.icons.Iv
import com.missa.b360.ui.navigation.AppModule
import com.missa.b360.ui.stock.fmtValeur
import com.missa.b360.ui.theme.MissaBorder
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Vert olive caractéristique du module Logistique — source unique : [AppModule.LOGISTIQUE]. */
private val VertLogistique: Color get() = AppModule.LOGISTIQUE.couleur

private data class TuileMatriceSpec(
    val icone: Int,
    val titre: String,
    val sousTitre: String,
    val estActif: Boolean,
    val onClick: () -> Unit,
)

@Composable
fun LogistiqueScreen(
    onBack: () -> Unit,
    onNaviguer: (String) -> Unit = {},
    vm: LogistiqueViewModel = hiltViewModel(),
) {
    val etat by vm.etat.collectAsStateWithLifecycle()
    val devise by vm.devise.collectAsStateWithLifecycle()

    var filtreTransfert by remember { mutableStateOf<EtatTransfert?>(null) }
    var ongletSites by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        MissaTopAppBar(
            title = stringResource(R.string.module_logistique),
            onBack = onBack,
            couleurFond = AppModule.LOGISTIQUE.couleurPale,
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // --- Carte Synthèse Logistique & Stock multi-sites ---
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = VertLogistique.copy(alpha = 0.16f),
                ) {
                    Column(Modifier.fillMaxWidth().padding(14.dp)) {
                        Text(
                            stringResource(R.string.log_titre_synthese),
                            fontSize = 11.sp,
                            color = MissaMuted,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            fmtValeur(etat.valeurTotale, devise),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MissaInk,
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                stringResource(R.string.log_taux_service, String.format(Locale.ROOT, "%.1f%%", etat.livraisons.tauxService)),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = VertLogistique,
                            )
                            Text(
                                stringResource(R.string.log_en_transit_count, etat.enTransit),
                                fontSize = 11.sp,
                                color = if (etat.enTransit > 0) Color(0xFFD97706) else MissaMuted,
                                fontWeight = if (etat.enTransit > 0) FontWeight.Bold else FontWeight.Normal,
                            )
                        }
                    }
                }
            }

            // --- Structure Matricielle 4 Tuiles ---
            item {
                Text(
                    stringResource(R.string.log_matrice_titre),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MissaInk,
                )
            }

            item {
                val tuiles = listOf(
                    TuileMatriceSpec(
                        icone = Iv.LocalShipping,
                        titre = stringResource(R.string.log_tuile_transferts),
                        sousTitre = stringResource(R.string.log_nb_count, etat.transferts.size),
                        estActif = !ongletSites && filtreTransfert == null,
                        onClick = {
                            ongletSites = false
                            filtreTransfert = null
                        },
                    ),
                    TuileMatriceSpec(
                        icone = Iv.Warehouse,
                        titre = stringResource(R.string.log_tuile_sites),
                        sousTitre = stringResource(R.string.log_nb_count, etat.sites.size),
                        estActif = ongletSites,
                        onClick = { ongletSites = true },
                    ),
                    TuileMatriceSpec(
                        icone = Iv.Schedule,
                        titre = stringResource(R.string.log_transfert_transit),
                        sousTitre = stringResource(R.string.log_nb_count, etat.enTransit),
                        estActif = !ongletSites && filtreTransfert == EtatTransfert.EN_TRANSIT,
                        onClick = {
                            ongletSites = false
                            filtreTransfert = EtatTransfert.EN_TRANSIT
                        },
                    ),
                    TuileMatriceSpec(
                        icone = Iv.Add,
                        titre = stringResource(R.string.log_nouveau_transfert),
                        sousTitre = stringResource(R.string.st_creer),
                        estActif = false,
                        onClick = { onNaviguer(AppModule.STOCK.route) },
                    ),
                )

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    tuiles.forEach { tuile ->
                        TuileLogistique(
                            icone = tuile.icone,
                            titre = tuile.titre,
                            sousTitre = tuile.sousTitre,
                            estActif = tuile.estActif,
                            modifier = Modifier.weight(1f),
                            onClick = tuile.onClick,
                        )
                    }
                }
            }

            // --- Liste dynamique : Sites ou Transferts ---
            item {
                Text(
                    stringResource(if (ongletSites) R.string.log_titre_sites else R.string.log_titre_transferts),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MissaInk,
                )
            }

            if (ongletSites) {
                if (etat.sites.isEmpty()) {
                    item {
                        MissaEmptyState(
                            icon = Iv.Warehouse,
                            title = stringResource(R.string.log_aucun_site),
                            description = stringResource(R.string.log_aucun_site_desc),
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                } else {
                    items(etat.sites, key = { it.site.id }) { site ->
                        CarteStockSite(site = site, devise = devise)
                    }
                }
            } else {
                val transfertsAffiches = etat.transferts.filter { filtreTransfert == null || it.etat == filtreTransfert }
                if (transfertsAffiches.isEmpty()) {
                    item {
                        MissaEmptyState(
                            icon = Iv.LocalShipping,
                            title = stringResource(R.string.log_aucun_transfert),
                            description = stringResource(R.string.log_aucun_transfert_desc),
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                } else {
                    items(transfertsAffiches, key = { it.reference }) { transfert ->
                        CarteTransfert(transfert = transfert)
                    }
                }
            }
        }
    }
}

@Composable
private fun TuileLogistique(
    icone: Int,
    titre: String,
    sousTitre: String,
    estActif: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (estActif) VertLogistique.copy(alpha = 0.15f) else Color.White,
        border = BorderStroke(1.dp, if (estActif) VertLogistique else MissaBorder),
        modifier = modifier
            .height(82.dp)
            .clickable(onClick = onClick),
    ) {
        Column(
            Modifier.padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(painterResource(icone), null, tint = MissaInk, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(4.dp))
            Text(titre, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MissaInk, maxLines = 1)
            Text(sousTitre, fontSize = 9.sp, color = MissaMuted, maxLines = 1)
        }
    }
}

@Composable
private fun CarteStockSite(
    site: StockDuSite,
    devise: String,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, MissaBorder),
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(site.site.nom, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MissaInk)
                Spacer(Modifier.weight(1f))
                Text(
                    fmtValeur(site.valeur, devise),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = VertLogistique,
                )
            }
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    stringResource(R.string.log_site_articles_count, site.references),
                    fontSize = 11.5.sp,
                    color = MissaMuted,
                )
                if (site.ruptures > 0) {
                    Text(
                        stringResource(R.string.log_site_ruptures_count, site.ruptures),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFDC2626),
                    )
                }
            }
        }
    }
}

@Composable
private fun CarteTransfert(
    transfert: Transfert,
) {
    val dateStr = remember(transfert.date) {
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(transfert.date))
    }

    val (badgeCouleur, badgeFond) = when (transfert.etat) {
        EtatTransfert.RECU -> Color(0xFF15803D) to Color(0xFFDCFCE7)
        EtatTransfert.EN_TRANSIT -> Color(0xFFD97706) to Color(0xFFFEF3C7)
        EtatTransfert.ORPHELIN -> Color(0xFFB91C1C) to Color(0xFFFEE2E2)
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, MissaBorder),
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(transfert.reference, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MissaInk)
                Spacer(Modifier.weight(1f))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = badgeFond,
                ) {
                    Text(
                        stringResource(transfert.etat.libelleRes),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeCouleur,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    stringResource(R.string.log_quantite_transfert, String.format(Locale.ROOT, "%.1f", transfert.quantite)),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MissaInk,
                )
                Text(dateStr, fontSize = 10.sp, color = MissaMuted)
            }
        }
    }
}
