package com.missa.b360.ui.fournisseurs.echeancier

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.missa.b360.R
import com.missa.b360.core.domain.model.EcheanceTranche
import com.missa.b360.core.domain.model.EcheancierLigne
import com.missa.b360.core.domain.model.PaymentPlan
import com.missa.b360.ui.components.MissaEmptyState
import com.missa.b360.ui.components.MissaTopAppBar
import com.missa.b360.ui.fournisseurs.components.BoutonFournisseur
import com.missa.b360.ui.fournisseurs.components.CarteFournisseur
import com.missa.b360.ui.fournisseurs.components.FournisseurCouleurs
import com.missa.b360.ui.fournisseurs.components.LigneValeur
import com.missa.b360.ui.fournisseurs.components.MessageEtat
import com.missa.b360.ui.fournisseurs.components.formatDate
import com.missa.b360.ui.icons.Iv
import com.missa.b360.ui.stock.fmtValeur
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted

/** Route `fournisseurs_echeancier` : quoi payer, quand, et ce qui est déjà planifié. */
@Composable
fun FournisseurEcheancierScreen(
    onBack: () -> Unit,
    onOuvrirFournisseur: (Long) -> Unit,
    viewModel: FournisseurEcheancierViewModel = hiltViewModel(),
) {
    val etat by viewModel.etat.collectAsStateWithLifecycle()
    var aPlanifier by remember { mutableStateOf<EcheancierLigne?>(null) }
    var aReporter by remember { mutableStateOf<PaymentPlan?>(null) }
    LaunchedEffect(etat.message) {
        if (etat.message != null) {
            kotlinx.coroutines.delay(3_000)
            viewModel.effacerMessage()
        }
    }
    Column(Modifier.fillMaxSize().background(Color.White)) {
        MissaTopAppBar(title = stringResource(R.string.four_echeancier_titre), onBack = onBack, titreCentre = false)
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 10.dp, end = 10.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { MessageEtat(etat.message) }
            item {
                CarteFournisseur(stringResource(R.string.four_echeancier_titre)) {
                    LigneValeur(stringResource(R.string.four_kpi_dette), fmtValeur(etat.detteTotale, etat.devise))
                    LigneValeur(stringResource(R.string.four_echeancier_planifie), fmtValeur(etat.planifie, etat.devise))
                }
            }
            if (!etat.chargement && etat.groupes.isEmpty()) {
                item {
                    MissaEmptyState(
                        icon = Iv.CheckCircle,
                        title = stringResource(R.string.four_action_rien),
                        description = stringResource(R.string.four_echeancier_vide),
                    )
                }
            }
            etat.groupes.forEach { groupe ->
                item(key = "t${groupe.tranche.name}") {
                    Text(
                        stringResource(groupe.tranche.libelleRes()) + " · " + fmtValeur(groupe.total, etat.devise),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (groupe.tranche == EcheanceTranche.EN_RETARD) FournisseurCouleurs.Bloque else MissaInk,
                    )
                }
                groupe.lignes.forEach { ligne ->
                    item(key = "f${ligne.facture.recordId}") {
                        CarteEcheance(
                            ligne = ligne,
                            devise = etat.devise,
                            onFournisseur = { onOuvrirFournisseur(ligne.facture.fournisseurId) },
                            onPlanifier = { aPlanifier = ligne },
                            onReporter = { aReporter = it },
                            onAnnuler = { viewModel.annuler(it.id) },
                        )
                    }
                }
            }
        }
    }
    aPlanifier?.let { ligne ->
        DialoguePlanification(
            reste = ligne.resteAPlanifier,
            onConfirmer = { montant, date ->
                viewModel.planifier(ligne.facture.recordId, montant, date)
                aPlanifier = null
            },
            onAnnuler = { aPlanifier = null },
        )
    }
    aReporter?.let { plan ->
        DialogueReport(
            dateInitiale = plan.datePrevue,
            onConfirmer = { date ->
                viewModel.reporter(plan.id, date)
                aReporter = null
            },
            onAnnuler = { aReporter = null },
        )
    }
}

@Composable
private fun CarteEcheance(
    ligne: EcheancierLigne,
    devise: String,
    onFournisseur: () -> Unit,
    onPlanifier: () -> Unit,
    onReporter: (PaymentPlan) -> Unit,
    onAnnuler: (PaymentPlan) -> Unit,
) {
    val f = ligne.facture
    CarteFournisseur(ligne.fournisseurNom, Modifier.clickable(onClick = onFournisseur)) {
        LigneValeur(f.reference + " · " + stringResource(R.string.four_action_echeance, formatDate(f.dueAt)), fmtValeur(f.outstanding, devise))
        ligne.plans.forEach { plan ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    stringResource(R.string.four_echeancier_ligne_plan, fmtValeur(plan.montant, devise), formatDate(plan.datePrevue)),
                    fontSize = 11.5.sp, color = MissaInk, modifier = Modifier.weight(1f),
                )
                BoutonFournisseur(stringResource(R.string.four_echeancier_reporter), plein = false) { onReporter(plan) }
                BoutonFournisseur(stringResource(R.string.st_annuler), plein = false) { onAnnuler(plan) }
            }
        }
        if (ligne.resteAPlanifier > 0.005) {
            Text(
                stringResource(R.string.four_echeancier_reste, fmtValeur(ligne.resteAPlanifier, devise)),
                fontSize = 11.sp, color = MissaMuted,
            )
            BoutonFournisseur(stringResource(R.string.four_echeancier_planifier), Modifier.fillMaxWidth(), onClick = onPlanifier)
        }
    }
}

private fun EcheanceTranche.libelleRes(): Int = when (this) {
    EcheanceTranche.EN_RETARD -> R.string.four_tranche_en_retard
    EcheanceTranche.SEMAINE -> R.string.four_tranche_semaine
    EcheanceTranche.MOIS -> R.string.four_tranche_mois
    EcheanceTranche.PLUS_TARD -> R.string.four_tranche_plus_tard
}
