package com.missa.b360.ui.stock

import com.missa.b360.ui.navigation.AppModule
import androidx.compose.foundation.layout.Arrangement

import com.missa.b360.ui.theme.MissaInk

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
import com.missa.b360.ui.components.*
import com.missa.b360.ui.icons.Iv

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
        if (ok) onBack()
    }

    val qteTransfert = quantite.toDoubleOrNull()
    val transfertComplet = produitId != null && qteTransfert != null && qteTransfert > 0 &&
        sourceId != null && destId != null && sourceId != destId

    MissaFormulaireTheme(AppModule.STOCK.couleur) {
    Column(modifier = Modifier.fillMaxSize()) {
        MissaTopAppBar(title = stringResource(R.string.st_transferts_stock), onBack = onBack, couleurFond = AppModule.STOCK.couleurPale)
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(8.dp))
            Row {
                StockChip(stringResource(R.string.st_nouveau_transfert), actif = !modeHistorique) { modeHistorique = false }
                Spacer(Modifier.width(6.dp))
                StockChip(stringResource(R.string.st_historique), actif = modeHistorique) { modeHistorique = true }
            }
            Spacer(Modifier.height(14.dp))
            if (modeHistorique) {
                HistoriqueTransferts()
            } else {
                val stockProduit = stockRows.filter { it.produitId == produitId }
                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    MissaFormSection(titre = stringResource(R.string.form_section_article), numero = 1) {
                        MissaChampListe(
                            libelle = stringResource(R.string.st_article),
                            options = produits.map { it.product.id to "${it.nom} (${fmtQuantite(it.stock)})" },
                            selection = produitId,
                            onSelection = { id ->
                                produitId = id
                                if (sourceId == null) sourceId = produits.firstOrNull { p -> p.product.id == id }?.product?.siteId
                            },
                            icone = Iv.Inventory2,
                            requis = true,
                        )
                        MissaChampTexte(quantite, { quantite = it }, stringResource(R.string.st_quantite), icone = Iv.Calculator, clavier = MissaClavier.DECIMAL, requis = true)
                    }
                    MissaFormSection(titre = stringResource(R.string.form_section_expedition), numero = 2) {
                        MissaChampListe(
                            libelle = stringResource(R.string.st_de),
                            options = sites.map { site ->
                                val q = stockProduit.firstOrNull { it.siteId == site.id }?.quantite ?: 0.0
                                site.id to "${site.nom} (${fmtQuantite(q)})"
                            },
                            selection = sourceId,
                            onSelection = { sourceId = it },
                            icone = Iv.Warehouse,
                            requis = true,
                        )
                        MissaChampListe(
                            libelle = stringResource(R.string.st_vers),
                            options = sites.filter { it.id != sourceId }.map { it.id to it.nom },
                            selection = destId,
                            onSelection = { destId = it },
                            icone = Iv.LocalShipping,
                            requis = true,
                        )
                        MissaChampTexte(observation, { observation = it }, stringResource(R.string.st_observation), icone = Iv.Description, placeholder = stringResource(R.string.st_obs_placeholder), lignes = 2)
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
        if (!modeHistorique) {
            MissaFormPied(
                texte = stringResource(R.string.st_valider_transfert),
                actif = transfertComplet,
                enCours = busy,
                onValider = {
                    val q = qteTransfert
                    if (transfertComplet && q != null) {
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
            )
        }
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
        // Un mouvement enregistré = opération terminée : on revient en arrière
        // (rester sur le formulaire rempli exposerait au double enregistrement).
        if (ok) onBack()
    }

    val qte = quantite.toDoubleOrNull()
    val mouvementComplet = produitId != null && qte != null && qte > 0

    MissaFormulaireTheme(AppModule.STOCK.couleur) {
    Column(modifier = Modifier.fillMaxSize()) {
        MissaTopAppBar(title = stringResource(R.string.st_nouveau_mouvement), onBack = onBack, couleurFond = AppModule.STOCK.couleurPale)
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            MissaFormSection(titre = stringResource(R.string.form_section_type), numero = 1) {
                MissaChoixTuiles(
                    options = listOf(
                        MissaTuile(StockMovementType.ENTREE, stringResource(R.string.st_mv_entree), Iv.TrendingUp),
                        MissaTuile(StockMovementType.SORTIE, stringResource(R.string.st_mv_sortie), Iv.TrendingDown),
                        // Le transfert ouvre son propre formulaire : ce n'est jamais une sélection.
                        MissaTuile(StockMovementType.TRANSFERT_SORTIE, stringResource(R.string.st_mv_transfert), Iv.SwapHoriz),
                    ),
                    selection = type,
                    onSelection = { choix -> if (choix == StockMovementType.TRANSFERT_SORTIE) onOpenTransfer() else type = choix },
                    colonnes = 3,
                )
            }
            MissaFormSection(titre = stringResource(R.string.form_section_article), numero = 2) {
                MissaChampListe(
                    libelle = stringResource(R.string.st_article),
                    options = produits.map { it.product.id to "${it.nom} (${fmtQuantite(it.stock)})" },
                    selection = produitId,
                    onSelection = { produitId = it },
                    icone = Iv.Inventory2,
                    requis = true,
                )
                MissaChampTexte(quantite, { quantite = it }, stringResource(R.string.st_quantite), icone = Iv.Calculator, clavier = MissaClavier.DECIMAL, requis = true)
                MissaChampTexte(motif, { motif = it }, stringResource(R.string.st_motif), icone = Iv.Description)
            }
        }
        MissaFormPied(
            texte = stringResource(R.string.st_valider),
            actif = mouvementComplet,
            enCours = busy,
            onValider = {
                val q = qte
                val id = produitId
                if (id != null && q != null && q > 0) {
                    vm.record(
                        produitId = id,
                        type = type,
                        quantite = q,
                        motif = motif.ifBlank { if (type == StockMovementType.ENTREE) texteEntree else texteSortie },
                        reference = "",
                        commentaire = "",
                    )
                }
            },
        )
    }
    }
}

