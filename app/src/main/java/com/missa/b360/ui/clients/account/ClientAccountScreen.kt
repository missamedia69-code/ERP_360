package com.missa.b360.ui.clients.account

import com.missa.b360.ui.clients.components.ClientCouleurs
import com.missa.b360.ui.theme.OnbConfigCard
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import com.missa.b360.ui.clients.components.BoutonClientPlein as Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import com.missa.b360.ui.components.BoutonContourMissa as OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.missa.b360.R
import com.missa.b360.core.domain.model.MentionsLegales
import com.missa.b360.ui.clients.components.ClientHero
import com.missa.b360.ui.clients.components.ClientTitreSection
import com.missa.b360.ui.clients.components.ClientCompteur
import com.missa.b360.ui.clients.components.ClientEtatVide
import com.missa.b360.ui.clients.components.BoutonClientDoux
import com.missa.b360.ui.icons.Iv
import androidx.compose.foundation.layout.Box
import com.missa.b360.ui.clients.components.ClientNoticeEffect
import com.missa.b360.ui.clients.components.ClientTopBar
import com.missa.b360.ui.clients.components.CreditGauge
import com.missa.b360.ui.clients.components.EtatChargement
import com.missa.b360.ui.clients.components.EtatErreur
import com.missa.b360.ui.clients.components.RisqueCouleurs
import com.missa.b360.ui.clients.components.clientDate
import com.missa.b360.ui.clients.components.clientMoney
import com.missa.b360.ui.theme.MissaBorder
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Route `clients/{id}/compte` : jauge, balance âgée, factures ouvertes, encaissement, promesse et relevé PDF. */
@Composable
fun ClientAccountScreen(
    onBack: () -> Unit,
    viewModel: ClientAccountViewModel = hiltViewModel(),
) {
    val etat by viewModel.etat.collectAsState()
    val hote = remember { SnackbarHostState() }
    val contexte = LocalContext.current
    val portee = rememberCoroutineScope()
    val client = etat.client
    val compte = etat.compte
    val mentions = MentionsLegales.depuis(
        entreprise = etat.entreprise,
        libelleFiscalGenerique = stringResource(R.string.fisc_id_fiscal),
        libelleRegistreGenerique = stringResource(R.string.fisc_id_registre),
        nomParDefaut = stringResource(R.string.app_name),
    )
    val lanceurPdf = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri != null && client != null && compte != null) {
            portee.launch {
                val ok = withContext(Dispatchers.IO) {
                    contexte.writeClientStatementPdf(uri, mentions, client, etat.devise, compte, System.currentTimeMillis())
                }
                if (!ok) viewModel.signalerPdfErreur()
            }
        }
    }
    ClientNoticeEffect(etat.notice, hote, viewModel::noticeLue)

    Scaffold(
        containerColor = ClientCouleurs.Fond,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(hote) },
        topBar = { ClientTopBar(titre = stringResource(R.string.cli_compte_titre), onBack = onBack) },
    ) { padding ->
        when {
            etat.erreur -> EtatErreur(onReessayer = viewModel::reessayer, modifier = Modifier.padding(padding))
            etat.chargement -> EtatChargement(Modifier.padding(padding))
            etat.introuvable || client == null || compte == null -> EtatErreur(
                onReessayer = onBack,
                message = stringResource(R.string.cli_fiche_introuvable),
                modifier = Modifier.padding(padding),
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 13.dp, end = 13.dp, top = 10.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    ClientHero {
                        Text(
                            stringResource(R.string.cli_compte_de, client.nom),
                            color = Color(0xFFC3CBE0), fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1,
                        )
                        Text(
                            clientMoney(etat.balance?.encours ?: 0.0, etat.devise),
                            color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, maxLines = 1,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                        Text(stringResource(R.string.cli_reste_du_libelle), color = Color(0xFFC3CBE0), fontSize = 11.sp)
                        val enRetard = etat.balance?.enRetard ?: 0.0
                        Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(R.string.cli_kpi_en_retard), color = Color(0xFFC3CBE0), fontSize = 11.sp)
                                Text(
                                    clientMoney(enRetard, etat.devise),
                                    color = if (enRetard > 0.0) Color(0xFFFFB4A8) else Color.White,
                                    fontWeight = FontWeight.ExtraBold, fontSize = 15.sp,
                                )
                            }
                        }
                        val risque = etat.evaluation?.risque
                        if (risque != null) {
                            Box(Modifier.padding(top = 10.dp)) { CreditGauge(etat.evaluation?.utilisationPct, risque, surFonce = true) }
                        }
                    }
                }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { viewModel.ouvrirEncaissement() },
                            couleur = ClientCouleurs.Nuit,
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                        ) { Text(stringResource(R.string.cli_encaisser)) }
                        BoutonClientDoux(
                            onClick = viewModel::ouvrirPromesse,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(stringResource(R.string.cli_type_promesse), color = ClientCouleurs.Violet, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                        }
                    }
                }
                item {
                    OutlinedButton(
                        onClick = { lanceurPdf.launch("releve-${client.code}.pdf") },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    ) { Text(stringResource(R.string.cli_telecharger_releve)) }
                }
                val promesse = etat.promesse
                val dateAPromettre = promesse?.promesseDate
                val montantPromis = promesse?.promesseMontant
                if (promesse != null && dateAPromettre != null && montantPromis != null) {
                    item {
                        Card(
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = ClientCouleurs.Carte),
                            border = BorderStroke(1.dp, RisqueCouleurs.Attention),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                stringResource(R.string.cli_promesse_ouverte, clientMoney(montantPromis, etat.devise), clientDate(dateAPromettre)),
                                color = MissaInk, fontWeight = FontWeight.Medium, modifier = Modifier.padding(12.dp),
                            )
                        }
                    }
                }
                item { AgedBalanceBars(compte.balanceAgee, etat.devise) }
                item {
                    ClientTitreSection(Iv.Description, stringResource(R.string.cli_factures_ouvertes)) {
                        ClientCompteur(compte.factures.size.toString())
                    }
                }
                if (compte.factures.isEmpty()) {
                    item { ClientEtatVide(Iv.CheckCircle, stringResource(R.string.cli_aucune_facture_ouverte), "") }
                } else {
                    items(compte.factures.size) { index ->
                        val facture = compte.factures[index]
                        OpenInvoiceRow(facture, etat.devise, onEncaisser = { viewModel.ouvrirEncaissement(facture.recordId) })
                    }
                }
                item {
                    ClientTitreSection(Iv.Payments, stringResource(R.string.cli_encaissements)) {
                        ClientCompteur(compte.paiements.size.toString())
                    }
                }
                if (compte.paiements.isEmpty()) {
                    item { ClientEtatVide(Iv.Payments, stringResource(R.string.cli_aucun_encaissement), "") }
                } else {
                    items(compte.paiements.take(10).size) { index ->
                        val paiement = compte.paiements[index]
                        Row(Modifier.fillMaxWidth().heightIn(min = 40.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Text("${clientDate(paiement.paiementAt)} · ${paiement.modePaiement}", color = MissaMuted, fontSize = 14.sp, modifier = Modifier.weight(1f))
                            Text(clientMoney(paiement.montant, etat.devise), color = MissaInk, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }

    when (etat.dialogue) {
        AccountDialog.ENCAISSER -> {
            val cible = etat.factureCible?.let { id -> compte?.factures?.firstOrNull { it.recordId == id } }
            EncaissementDialog(
                montantInitial = cible?.outstanding ?: (etat.balance?.encours ?: 0.0),
                devise = etat.devise,
                cibleFacture = cible != null,
                onValider = viewModel::encaisser,
                onDismiss = viewModel::fermerDialogue,
            )
        }
        AccountDialog.PROMESSE -> PromesseDialog(
            encours = etat.balance?.encours ?: 0.0,
            devise = etat.devise,
            onValider = { montant, jours -> viewModel.promettre(montant, jours) },
            onDismiss = viewModel::fermerDialogue,
            sousTitre = client?.nom,
        )
        null -> Unit
    }
}
