package com.missa.b360.ui.operations

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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.missa.b360.ui.components.MissaTopAppBar
import com.missa.b360.ui.icons.Iv
import com.missa.b360.ui.navigation.AppModule
import com.missa.b360.ui.stock.fmtValeur
import com.missa.b360.ui.theme.MissaBorder
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted
import java.util.Locale

/** Cyan foncé caractéristique du module Reporting — source unique : [AppModule.REPORTING]. */
private val CyanReporting: Color get() = AppModule.REPORTING.couleur

private data class TuileMatriceSpec(
    val icone: Int,
    val titre: String,
    val sousTitre: String,
    val estActif: Boolean,
    val onClick: () -> Unit,
)

@Composable
fun ReportingScreen(
    onBack: () -> Unit,
    onNaviguer: (String) -> Unit = {},
    vm: ReportingViewModel = hiltViewModel(),
) {
    val tableau by vm.tableau.collectAsStateWithLifecycle()
    val devise by vm.devise.collectAsStateWithLifecycle()
    var sectionSelectionnee by remember { mutableStateOf(0) } // 0: Tous, 1: Ventes, 2: Dépenses, 3: Trésorerie

    Column(Modifier.fillMaxSize()) {
        MissaTopAppBar(
            title = stringResource(R.string.module_reporting),
            onBack = onBack,
            couleurFond = AppModule.REPORTING.couleurPale,
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // --- Hero Synthèse Cockpit Direction ---
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = CyanReporting.copy(alpha = 0.16f),
                ) {
                    Column(Modifier.fillMaxWidth().padding(14.dp)) {
                        Text(
                            stringResource(R.string.rep_titre_synthese),
                            fontSize = 11.sp,
                            color = MissaMuted,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            fmtValeur(tableau.ventes.chiffreAffaires, devise),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MissaInk,
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                stringResource(R.string.rep_marge_brute, fmtValeur(tableau.ventes.margeBrute, devise)),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyanReporting,
                            )
                            Text(
                                stringResource(R.string.rep_tresorerie_nette, fmtValeur(tableau.tresorerie.disponible, devise)),
                                fontSize = 11.sp,
                                color = MissaMuted,
                            )
                        }
                    }
                }
            }

            // --- Structure Matricielle 4 Tuiles ---
            item {
                Text(
                    stringResource(R.string.rep_matrice_titre),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MissaInk,
                )
            }

            item {
                val tuiles = listOf(
                    TuileMatriceSpec(
                        icone = Iv.Analytics,
                        titre = stringResource(R.string.rep_tuile_vue_globale),
                        sousTitre = stringResource(R.string.rep_indicateurs_count, 6),
                        estActif = sectionSelectionnee == 0,
                        onClick = { sectionSelectionnee = 0 },
                    ),
                    TuileMatriceSpec(
                        icone = Iv.ShoppingCart,
                        titre = stringResource(R.string.module_vente),
                        sousTitre = fmtValeur(tableau.ventes.chiffreAffaires, devise),
                        estActif = sectionSelectionnee == 1,
                        onClick = { sectionSelectionnee = 1 },
                    ),
                    TuileMatriceSpec(
                        icone = Iv.CartArrowDown,
                        titre = stringResource(R.string.module_achats),
                        sousTitre = fmtValeur(tableau.achats.totalEngage, devise),
                        estActif = sectionSelectionnee == 2,
                        onClick = { sectionSelectionnee = 2 },
                    ),
                    TuileMatriceSpec(
                        icone = Iv.Bank,
                        titre = stringResource(R.string.module_tresorerie),
                        sousTitre = fmtValeur(tableau.tresorerie.disponible, devise),
                        estActif = sectionSelectionnee == 3,
                        onClick = { sectionSelectionnee = 3 },
                    ),
                )

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    tuiles.forEach { tuile ->
                        TuileReporting(
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

            // --- Fiches Analytiques Détaillées ---
            item {
                Text(
                    stringResource(R.string.rep_titre_indicateurs),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MissaInk,
                )
            }

            if (sectionSelectionnee == 0 || sectionSelectionnee == 1) {
                item {
                    CarteIndicateurSection(
                        titre = stringResource(R.string.module_vente),
                        ligne1 = stringResource(R.string.rep_ca_realise) to fmtValeur(tableau.ventes.chiffreAffaires, devise),
                        ligne2 = stringResource(R.string.rep_marge_estimee) to fmtValeur(tableau.ventes.margeBrute, devise),
                        ligne3 = stringResource(R.string.rep_commandes_count) to "${tableau.ventes.nombreVentes}",
                        couleur = Color(0xFF2563EB),
                    )
                }
            }

            if (sectionSelectionnee == 0 || sectionSelectionnee == 2) {
                item {
                    CarteIndicateurSection(
                        titre = stringResource(R.string.module_achats),
                        ligne1 = stringResource(R.string.rep_achats_engages) to fmtValeur(tableau.achats.totalEngage, devise),
                        ligne2 = stringResource(R.string.rep_fournisseurs_actifs) to "${tableau.achats.fournisseursActifs}",
                        ligne3 = stringResource(R.string.rep_factures_achats) to "${tableau.achats.nombreFactures}",
                        couleur = Color(0xFFF59E0B),
                    )
                }
            }

            if (sectionSelectionnee == 0 || sectionSelectionnee == 3) {
                item {
                    CarteIndicateurSection(
                        titre = stringResource(R.string.module_tresorerie),
                        ligne1 = stringResource(R.string.rep_solde_tresorerie) to fmtValeur(tableau.tresorerie.disponible, devise),
                        ligne2 = stringResource(R.string.rep_flux_entrants) to fmtValeur(tableau.tresorerie.encaissements, devise),
                        ligne3 = stringResource(R.string.rep_flux_sortants) to fmtValeur(tableau.tresorerie.decaissements, devise),
                        couleur = Color(0xFF16A34A),
                    )
                }
            }
        }
    }
}

@Composable
private fun TuileReporting(
    icone: Int,
    titre: String,
    sousTitre: String,
    estActif: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (estActif) CyanReporting.copy(alpha = 0.15f) else Color.White,
        border = BorderStroke(1.dp, if (estActif) CyanReporting else MissaBorder),
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
private fun CarteIndicateurSection(
    titre: String,
    ligne1: Pair<String, String>,
    ligne2: Pair<String, String>,
    ligne3: Pair<String, String>,
    couleur: Color,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, MissaBorder),
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Text(titre, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = couleur)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(ligne1.first, fontSize = 12.sp, color = MissaMuted)
                Text(ligne1.second, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MissaInk)
            }
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(ligne2.first, fontSize = 12.sp, color = MissaMuted)
                Text(ligne2.second, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MissaInk)
            }
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(ligne3.first, fontSize = 12.sp, color = MissaMuted)
                Text(ligne3.second, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MissaInk)
            }
        }
    }
}
