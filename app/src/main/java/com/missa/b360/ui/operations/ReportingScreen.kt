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
import androidx.compose.foundation.lazy.items
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
import com.missa.b360.core.domain.model.FormatIndicateur
import com.missa.b360.core.domain.model.IndicateurCode
import com.missa.b360.core.domain.model.ModuleCode
import com.missa.b360.core.domain.model.SensIndicateur
import com.missa.b360.core.domain.model.ValeurIndicateur
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
    var moduleSelectionne by remember { mutableStateOf<ModuleCode?>(null) }

    val caValeur = tableau.indicateurs[IndicateurCode.CA_PERIODE]?.valeur ?: 0.0
    val margeValeur = tableau.indicateurs[IndicateurCode.MARGE_BRUTE]?.valeur ?: 0.0
    val tresoValeur = tableau.indicateurs[IndicateurCode.TRESORERIE_NETTE]?.valeur ?: 0.0

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
                            fmtValeur(caValeur, devise),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MissaInk,
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                stringResource(R.string.rep_marge_brute, fmtValeur(margeValeur, devise)),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyanReporting,
                            )
                            Text(
                                stringResource(R.string.rep_tresorerie_nette, fmtValeur(tresoValeur, devise)),
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
                        sousTitre = stringResource(R.string.rep_indicateurs_count, tableau.indicateurs.size),
                        estActif = moduleSelectionne == null,
                        onClick = { moduleSelectionne = null },
                    ),
                    TuileMatriceSpec(
                        icone = Iv.ShoppingCart,
                        titre = stringResource(R.string.module_vente),
                        sousTitre = fmtValeur(caValeur, devise),
                        estActif = moduleSelectionne == ModuleCode.VEN,
                        onClick = { moduleSelectionne = ModuleCode.VEN },
                    ),
                    TuileMatriceSpec(
                        icone = Iv.CartArrowDown,
                        titre = stringResource(R.string.module_achats),
                        sousTitre = fmtValeur(tableau.indicateurs[IndicateurCode.ACHATS_PERIODE]?.valeur ?: 0.0, devise),
                        estActif = moduleSelectionne == ModuleCode.ACH,
                        onClick = { moduleSelectionne = ModuleCode.ACH },
                    ),
                    TuileMatriceSpec(
                        icone = Iv.Bank,
                        titre = stringResource(R.string.module_tresorerie),
                        sousTitre = fmtValeur(tresoValeur, devise),
                        estActif = moduleSelectionne == ModuleCode.TRE,
                        onClick = { moduleSelectionne = ModuleCode.TRE },
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

            // --- Registre des Indicateurs Filtrés ---
            item {
                Text(
                    stringResource(R.string.rep_titre_indicateurs),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MissaInk,
                )
            }

            val indicateursAffiches = tableau.indicateurs.entries
                .filter { moduleSelectionne == null || it.key.module == moduleSelectionne }
                .sortedBy { it.key.name }

            items(indicateursAffiches, key = { it.key.name }) { (code, kpi) ->
                CarteKpiReporting(
                    code = code,
                    kpi = kpi,
                    devise = devise,
                )
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
private fun CarteKpiReporting(
    code: IndicateurCode,
    kpi: ValeurIndicateur,
    devise: String,
) {
    val valeurFormatee = when (code.format) {
        FormatIndicateur.MONNAIE -> fmtValeur(kpi.valeur, devise)
        FormatIndicateur.POURCENT -> String.format(Locale.ROOT, "%.1f%%", kpi.valeur)
        FormatIndicateur.JOURS -> String.format(Locale.ROOT, "%.0f j", kpi.valeur)
        FormatIndicateur.ENTIER -> String.format(Locale.ROOT, "%.0f", kpi.valeur)
        FormatIndicateur.DECIMAL -> String.format(Locale.ROOT, "%.1f", kpi.valeur)
    }

    val couleurSens = when (code.sens) {
        SensIndicateur.HAUT_BON -> if (kpi.valeur > 0) Color(0xFF15803D) else Color(0xFFB91C1C)
        SensIndicateur.BAS_BON -> if (kpi.valeur > 0) Color(0xFFB91C1C) else Color(0xFF15803D)
        SensIndicateur.NEUTRE -> MissaInk
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, MissaBorder),
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(code.libelleRes),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MissaInk,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    valeurFormatee,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = couleurSens,
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(code.formuleRes),
                fontSize = 11.sp,
                color = MissaMuted,
            )
        }
    }
}
