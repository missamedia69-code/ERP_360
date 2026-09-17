package com.missa.b360.ui.stock

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.missa.b360.R
import com.missa.b360.core.data.entity.StockMovementType
import com.missa.b360.ui.components.MissaTopAppBar
import com.missa.b360.ui.theme.BrandBlue
import com.missa.b360.core.data.dao.StockMovementView

/** Maquette 9 — transferts entre sites + historique ; validation via TransferStockUseCase. */
@Composable
fun StockTransferFormScreen(onBack: () -> Unit) {
    val vm: StockOpsViewModel = hiltViewModel()
    val produits by vm.products.collectAsStateWithLifecycle()
    val sites by vm.sites.collectAsStateWithLifecycle()
    val stockRows by vm.stockRows.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val outcome by vm.outcome.collectAsStateWithLifecycle()
    val contexte = LocalContext.current

    var modeHistorique by remember { mutableStateOf(false) }
    var produitId by remember { mutableStateOf<Long?>(null) }
    var quantite by remember { mutableStateOf("") }
    var sourceId by remember { mutableStateOf<Long?>(null) }
    var destId by remember { mutableStateOf<Long?>(null) }
    var observation by remember { mutableStateOf("") }

    val texteChampsRequis = stringResource(R.string.st_champs_requis)
    val texteTransfertMotif = stringResource(R.string.st_mv_transfert)
    val texteTransfertOk = stringResource(R.string.st_transfert_ok)
    val texteTransfertKo = stringResource(R.string.st_transfert_ko)
    val texteMouvementOk = stringResource(R.string.st_mouvement_ok)
    val texteMouvementKo = stringResource(R.string.st_mouvement_ko)
    val texteEntree = stringResource(R.string.st_mv_entree)
    val texteSortie = stringResource(R.string.st_mv_sortie)

    outcome?.let { o ->
        val ok = when (o) {
            is StockOpsViewModel.MovementOutcome.Transfer -> o.result is com.missa.b360.core.domain.usecase.TransferStockUseCase.Result.Succes
            else -> false
        }
        Toast.makeText(contexte, if (ok) texteTransfertOk else texteTransfertKo, Toast.LENGTH_SHORT).show()
        vm.clearOutcome()
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
    ) {
        MissaTopAppBar(title = stringResource(R.string.st_transferts_stock), onBack = onBack)
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(8.dp))
            Row {
                StockChip(stringResource(R.string.st_nouveau_transfert), actif = !modeHistorique) { modeHistorique = false }
                Spacer(Modifier.width(6.dp))
                StockChip(stringResource(R.string.st_historique), actif = modeHistorique) { modeHistorique = true }
            }
            Spacer(Modifier.height(12.dp))
            if (modeHistorique) {
                HistoriqueTransferts()
            } else {
                val stockProduit = stockRows.filter { it.produitId == produitId }
                DropdownChamp(
                    libelle = stringResource(R.string.st_article),
                    options = produits.map { it.product.id to "${it.nom} (${fmtQuantite(it.stock)})" },
                    selection = produitId,
                    onSelection = { produitId = it; if (sourceId == null) sourceId = produits.firstOrNull { p -> p.product.id == it }?.product?.siteId },
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = quantite,
                    onValueChange = { quantite = it },
                    label = { Text("${stringResource(R.string.st_quantite)} *", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                )
                Spacer(Modifier.height(10.dp))
                DropdownChamp(
                    libelle = "${stringResource(R.string.st_de)} *",
                    options = sites.map { s ->
                        val q = stockProduit.firstOrNull { it.siteId == s.id }?.quantite ?: 0.0
                        s.id to "${s.nom} (${fmtQuantite(q)})"
                    },
                    selection = sourceId,
                    onSelection = { sourceId = it },
                )
                Spacer(Modifier.height(10.dp))
                DropdownChamp(
                    libelle = "${stringResource(R.string.st_vers)} *",
                    options = sites.filter { it.id != sourceId }.map { it.id to it.nom },
                    selection = destId,
                    onSelection = { destId = it },
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = observation,
                    onValueChange = { observation = it },
                    label = { Text(stringResource(R.string.st_observation), fontSize = 11.sp) },
                    placeholder = { Text(stringResource(R.string.st_obs_placeholder), fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth().height(84.dp),
                    shape = RoundedCornerShape(12.dp),
                )
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = {
                        val q = quantite.toDoubleOrNull()
                        if (produitId == null || q == null || q <= 0 || sourceId == null || destId == null || sourceId == destId) {
                            Toast.makeText(contexte, texteChampsRequis, Toast.LENGTH_SHORT).show()
                        } else {
                            vm.transfer(
                                produitId = produitId!!,
                                siteSourceId = sourceId!!,
                                siteDestId = destId!!,
                                quantite = q,
                                motif = texteTransfertMotif,
                                commentaire = observation,
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                    enabled = !busy,
                ) {
                    Text(stringResource(R.string.st_valider_transfert), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun HistoriqueTransferts() {
    val vmMv: StockMouvementsViewModel = hiltViewModel()
    val groupes by vmMv.etat.collectAsStateWithLifecycle()
    val transferts = groupes.flatMap { it.lignes }.filter { it.type == "TRANSFERT_SORTIE" }
    if (transferts.isEmpty()) {
        Text(stringResource(R.string.st_aucun_resultat), fontSize = 12.sp, color = com.missa.b360.ui.theme.MissaMuted)
    } else {
        transferts.forEach { mv ->
            CarteStock {
                Text(mv.produitNom, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = com.missa.b360.ui.theme.MissaInk)
                Text(
                    "${mv.siteNom.orEmpty()} · ${fmtQuantite(mv.quantite)}",
                    fontSize = 10.5.sp,
                    color = com.missa.b360.ui.theme.MissaMuted,
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

/** Formulaire rapide « Nouveau mouvement » (entrée / sortie) — piste d'audit §43. */
@Composable
fun StockMovementFormScreen(
    onBack: () -> Unit,
    initialDirection: StockMovementType = StockMovementType.ENTREE,
    onOpenTransfer: () -> Unit = {},
) {
    val vm: StockOpsViewModel = hiltViewModel()
    val produits by vm.products.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val outcome by vm.outcome.collectAsStateWithLifecycle()
    val contexte = LocalContext.current

    var type by remember { mutableStateOf(if (initialDirection == StockMovementType.SORTIE) StockMovementType.SORTIE else StockMovementType.ENTREE) }
    var produitId by remember { mutableStateOf<Long?>(null) }
    var quantite by remember { mutableStateOf("") }
    var motif by remember { mutableStateOf("") }

    val texteChampsRequis = stringResource(R.string.st_champs_requis)
    val texteMouvementOk = stringResource(R.string.st_mouvement_ok)
    val texteMouvementKo = stringResource(R.string.st_mouvement_ko)
    val texteEntree = stringResource(R.string.st_mv_entree)
    val texteSortie = stringResource(R.string.st_mv_sortie)

    outcome?.let { o ->
        val ok = when (o) {
            is StockOpsViewModel.MovementOutcome.Result -> o.result is com.missa.b360.core.domain.usecase.StockMovementResult.Succes
            else -> false
        }
        Toast.makeText(contexte, if (ok) texteMouvementOk else texteMouvementKo, Toast.LENGTH_SHORT).show()
        vm.clearOutcome()
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        MissaTopAppBar(title = stringResource(R.string.st_nouveau_mouvement), onBack = onBack)
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(8.dp))
            Row {
                StockChip(stringResource(R.string.st_mv_entree), actif = type == StockMovementType.ENTREE) { type = StockMovementType.ENTREE }
                Spacer(Modifier.width(6.dp))
                StockChip(stringResource(R.string.st_mv_sortie), actif = type == StockMovementType.SORTIE) { type = StockMovementType.SORTIE }
                Spacer(Modifier.width(6.dp))
                StockChip(stringResource(R.string.st_mv_transfert), actif = false, onClick = onOpenTransfer)
            }
            Spacer(Modifier.height(12.dp))
            DropdownChamp(
                libelle = stringResource(R.string.st_article),
                options = produits.map { it.product.id to "${it.nom} (${fmtQuantite(it.stock)})" },
                selection = produitId,
                onSelection = { produitId = it },
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = quantite,
                onValueChange = { quantite = it },
                label = { Text("${stringResource(R.string.st_quantite)} *", fontSize = 11.sp) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = motif,
                onValueChange = { motif = it },
                label = { Text(stringResource(R.string.st_motif), fontSize = 11.sp) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
            )
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = {
                    val q = quantite.toDoubleOrNull()
                    if (produitId == null || q == null || q <= 0) {
                        Toast.makeText(contexte, texteChampsRequis, Toast.LENGTH_SHORT).show()
                    } else {
                        vm.record(
                            produitId = produitId!!,
                            type = type,
                            quantite = q,
                            motif = motif.ifBlank { if (type == StockMovementType.ENTREE) texteEntree else texteSortie },
                            reference = "",
                            commentaire = "",
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().height(46.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                enabled = !busy,
            ) {
                Text(stringResource(R.string.st_valider), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

