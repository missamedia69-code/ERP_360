package com.missa.b360.ui.operations

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.missa.b360.R
import com.missa.b360.core.domain.model.FormatIndicateur
import com.missa.b360.core.domain.model.IndicateurCode
import com.missa.b360.core.domain.model.Indicateurs
import com.missa.b360.core.domain.model.ModuleCode
import com.missa.b360.core.domain.model.ModulesPersonnalises
import com.missa.b360.core.domain.model.SensIndicateur
import com.missa.b360.core.domain.usecase.ValeurAlerte
import com.missa.b360.core.domain.usecase.ValeurIndicateur
import com.missa.b360.core.util.MoneyUtils
import com.missa.b360.ui.components.MissaEmptyState
import com.missa.b360.ui.components.MissaLayout
import com.missa.b360.ui.components.MissaPanel
import com.missa.b360.ui.components.MissaSectionTitle
import com.missa.b360.ui.components.MissaTopAppBar
import com.missa.b360.ui.theme.BrandBlue
import com.missa.b360.ui.theme.Green60
import com.missa.b360.ui.theme.MissaCanvas
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted
import com.missa.b360.ui.theme.MissaSoftBlue
import com.missa.b360.ui.theme.Red40
import com.missa.b360.ui.theme.Red80
import java.util.Locale

/**
 * Tableau de bord — le module Reporting ne saisit rien : il relit les données
 * des autres modules et les transforme en indicateurs et en alertes.
 *
 * Seuls les indicateurs des modules actifs sont affichés : un négoce ne voit pas
 * « quantité produite », une société de services ne voit pas « rotation du stock ».
 */
@Composable
fun ReportingScreen(
    onBack: () -> Unit,
    viewModel: ReportingViewModel = hiltViewModel(),
) {
    val tableau by viewModel.tableau.collectAsState()
    val devise by viewModel.devise.collectAsState()
    val modules by viewModel.modulesActifs.collectAsState()
    val groupes = Indicateurs.grouperParModule(modules)
    val alertes = tableau.alertes.filter { it.code.module in modules }

    Scaffold(
        containerColor = MissaCanvas,
        topBar = {
            MissaTopAppBar(title = stringResource(R.string.module_reporting), onBack = onBack)
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(
                horizontal = MissaLayout.screenHorizontal,
                vertical = MissaLayout.screenVertical,
            ),
            verticalArrangement = Arrangement.spacedBy(MissaLayout.itemGap),
        ) {
            item {
                MissaSectionTitle(
                    title = stringResource(R.string.kpi_titre_alertes),
                    subtitle = stringResource(R.string.kpi_sous_titre),
                )
            }
            if (alertes.isEmpty()) {
                item {
                    MissaPanel(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = stringResource(R.string.kpi_aucune_alerte),
                            color = MissaMuted,
                            fontSize = 12.sp,
                        )
                    }
                }
            } else {
                items(alertes, key = { it.code.name }) { alerte ->
                    AlerteCarte(alerte = alerte, devise = devise)
                }
            }
            item {
                MissaSectionTitle(
                    title = stringResource(R.string.kpi_titre_indicateurs),
                    subtitle = stringResource(R.string.kpi_periode, tableau.periodeJours),
                )
            }
            if (groupes.isEmpty()) {
                item {
                    MissaEmptyState(
                        icon = Icons.Outlined.Assessment,
                        title = stringResource(R.string.kpi_aucun_module),
                    )
                }
            } else {
                items(groupes, key = { it.first.name }) { (module, codes) ->
                    ModuleIndicateursCarte(
                        module = module,
                        codes = codes,
                        tableau = tableau.indicateurs,
                        devise = devise,
                    )
                }
            }
        }
    }
}

/** Détail chiffré d'une alerte, formaté selon sa nature (nombre, montant, taux). */
@Composable
internal fun detailAlerte(alerte: ValeurAlerte, devise: String): String {
    val exemple = alerte.exemple
    return if (alerte.code.avecMontant) {
        val montant = alerte.montant ?: 0.0
        val texte = if (alerte.code.detailRes == R.string.alerte_marge_d) {
            String.format(Locale.getDefault(), "%.1f %%", montant)
        } else {
            MoneyUtils.format(montant, devise)
        }
        stringResource(alerte.code.detailRes, texte)
    } else if (exemple != null) {
        stringResource(alerte.code.detailRes, alerte.nombre, exemple)
    } else {
        stringResource(alerte.code.detailRes, alerte.nombre)
    }
}

/** Une alerte : bandeau rouge, intitulé et détail chiffré. */
@Composable
private fun AlerteCarte(alerte: ValeurAlerte, devise: String) {
    val detail = detailAlerte(alerte, devise)
    MissaPanel(modifier = Modifier.fillMaxWidth(), accent = Red40) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(9.dp), color = Red80, modifier = Modifier.size(32.dp)) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.WarningAmber,
                        contentDescription = null,
                        tint = Red40,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(alerte.code.libelleRes),
                    color = MissaInk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.5.sp,
                )
                Text(text = detail, color = MissaMuted, fontSize = 11.sp)
            }
        }
    }
}

/** Les indicateurs d'un module, regroupés dans une carte. */
@Composable
private fun ModuleIndicateursCarte(
    module: ModuleCode,
    codes: List<IndicateurCode>,
    tableau: Map<IndicateurCode, ValeurIndicateur>,
    devise: String,
) {
    MissaPanel(modifier = Modifier.fillMaxWidth(), accent = BrandBlue) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = BrandBlue,
                modifier = Modifier.size(width = 3.dp, height = 14.dp),
            ) {}
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(ModulesPersonnalises.libelleRes(module)),
                color = MissaInk,
                fontWeight = FontWeight.Bold,
                fontSize = 12.5.sp,
            )
        }
        Spacer(Modifier.height(2.dp))
        for (code in codes) {
            IndicateurLigne(valeur = tableau[code], code = code, devise = devise)
        }
    }
}

/** Une ligne : libellé + formule à gauche, valeur et variation à droite. */
@Composable
private fun IndicateurLigne(valeur: ValeurIndicateur?, code: IndicateurCode, devise: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(code.libelleRes),
                color = MissaInk,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
            )
            Text(
                text = stringResource(code.formuleRes),
                color = MissaMuted,
                fontSize = 10.sp,
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = formaterValeur(valeur?.valeur ?: 0.0, code.format, devise),
                color = MissaInk,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
            )
            val variation = valeur?.variation
            if (variation != null && kotlin.math.abs(variation) >= 0.5) {
                VariationPastille(variation = variation, sens = code.sens)
            }
        }
    }
}

/** Pastille de variation : verte si l'évolution va dans le bon sens, rouge sinon. */
@Composable
private fun VariationPastille(variation: Double, sens: SensIndicateur) {
    val hausse = variation > 0
    val favorable = when (sens) {
        SensIndicateur.HAUT_BON -> hausse
        SensIndicateur.BAS_BON -> !hausse
        SensIndicateur.NEUTRE -> true
    }
    val couleur = when {
        sens == SensIndicateur.NEUTRE -> MissaMuted
        favorable -> Green60
        else -> Red40
    }
    val fond = when {
        sens == SensIndicateur.NEUTRE -> MissaSoftBlue
        favorable -> MissaSoftBlue
        else -> Red80
    }
    val pourcent = String.format(Locale.getDefault(), "%.0f %%", kotlin.math.abs(variation))
    val texte = if (hausse) {
        stringResource(R.string.kpi_variation_hausse, pourcent)
    } else {
        stringResource(R.string.kpi_variation_baisse, "-$pourcent")
    }
    Text(
        text = texte,
        color = couleur,
        fontSize = 9.5.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .padding(top = 1.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(fond)
            .padding(horizontal = 5.dp, vertical = 1.dp),
    )
}

private fun formaterValeur(valeur: Double, format: FormatIndicateur, devise: String): String =
    when (format) {
        FormatIndicateur.MONNAIE -> MoneyUtils.format(valeur, devise)
        FormatIndicateur.POURCENT -> String.format(Locale.getDefault(), "%.1f %%", valeur)
        FormatIndicateur.DECIMAL -> String.format(Locale.getDefault(), "%.1f", valeur)
        FormatIndicateur.NOMBRE -> valeur.toLong().toString()
    }
