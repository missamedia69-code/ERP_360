package com.missa.b360.ui.production

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import com.missa.b360.ui.components.MissaMenuDeroulant
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.missa.b360.core.data.entity.OperationRecordEntity
import com.missa.b360.core.data.entity.OperationStatus
import com.missa.b360.core.domain.model.ProductionCodec
import com.missa.b360.core.domain.model.ProductionComponent
import com.missa.b360.ui.components.MissaEmptyState
import com.missa.b360.ui.components.MissaTopAppBar
import com.missa.b360.ui.icons.Iv
import com.missa.b360.ui.navigation.AppModule
import com.missa.b360.ui.stock.ProductWithStock
import com.missa.b360.ui.stock.fmtQuantite
import com.missa.b360.ui.stock.fmtValeur
import com.missa.b360.ui.theme.MissaBorder
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.missa.b360.ui.components.*

/** Violet caractéristique du module Production — source unique : [AppModule.PRODUCTION]. */
private val VioletProduction: Color get() = AppModule.PRODUCTION.couleur

private enum class VueProduction { LISTE, NOUVEL_ORDRE }

@Composable
fun ProductionScreen(
    onBack: () -> Unit,
    openCreate: Boolean = false,
    vm: ProductionViewModel = hiltViewModel(),
) {
    val ordres by vm.ordres.collectAsStateWithLifecycle(initialValue = emptyList())
    val devise by vm.devise.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    var vue by remember { mutableStateOf(if (openCreate) VueProduction.NOUVEL_ORDRE else VueProduction.LISTE) }

    LaunchedEffect(openCreate) {
        if (openCreate) vm.nouveauOrdre()
    }

    LaunchedEffect(message) {
        if (message is ProductionViewModel.ActionMessage.Succes) {
            vue = VueProduction.LISTE
            kotlinx.coroutines.delay(3_000)
            vm.effacerMessage()
        }
    }

    when (vue) {
        VueProduction.LISTE -> ListeOrdres(
            ordres = ordres,
            devise = devise,
            message = message,
            onBack = onBack,
            onNouvelOrdre = { vm.nouveauOrdre(); vue = VueProduction.NOUVEL_ORDRE },
            onEditBrouillon = { order -> vm.ouvrirBrouillon(order); vue = VueProduction.NOUVEL_ORDRE },
        )
        VueProduction.NOUVEL_ORDRE -> FormulaireOrdreProduction(
            vm = vm,
            message = message,
            onBack = { vue = VueProduction.LISTE },
        )
    }
}

@Composable
private fun ListeOrdres(
    ordres: List<OperationRecordEntity>,
    devise: String,
    message: ProductionViewModel.ActionMessage?,
    onBack: () -> Unit,
    onNouvelOrdre: () -> Unit,
    onEditBrouillon: (OperationRecordEntity) -> Unit,
) {
    val lances = ordres.filter { it.status == OperationStatus.VALIDATED.name }

    Column(Modifier.fillMaxSize()) {
        MissaTopAppBar(
            title = stringResource(R.string.module_production),
            onBack = onBack,
            couleurFond = AppModule.PRODUCTION.couleurPale,
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = VioletProduction.copy(alpha = 0.16f),
                ) {
                    Column(Modifier.fillMaxWidth().padding(14.dp)) {
                        Text(
                            stringResource(R.string.pro_titre_synthese),
                            fontSize = 11.sp,
                            color = MissaMuted,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            stringResource(R.string.pro_ordres_termines, lances.size),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MissaInk,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            stringResource(R.string.pro_total_ordres, ordres.size),
                            fontSize = 11.sp,
                            color = MissaMuted,
                        )
                    }
                }
            }

            item {
                Button(
                    onClick = onNouvelOrdre,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = VioletProduction, contentColor = Color.White),
                ) {
                    Icon(painterResource(Iv.Add), null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.pro_nouvel_ordre), color = Color.White)
                }
            }

            message?.let { msg ->
                item {
                    val (texte, couleur) = when (msg) {
                        is ProductionViewModel.ActionMessage.Succes -> msg.texte to Color(0xFF15803D)
                        is ProductionViewModel.ActionMessage.Erreur -> msg.texte to Color(0xFFB91C1C)
                    }
                    Surface(shape = RoundedCornerShape(10.dp), color = couleur.copy(alpha = 0.12f)) {
                        Text(
                            texte,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = couleur,
                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                        )
                    }
                }
            }

            item {
                Text(
                    stringResource(R.string.pro_titre_historique),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MissaInk,
                )
            }

            if (ordres.isEmpty()) {
                item {
                    MissaEmptyState(
                        icon = Iv.Factory,
                        title = stringResource(R.string.pro_aucun_ordre),
                        description = stringResource(R.string.pro_aucun_ordre_desc),
                        modifier = Modifier.padding(16.dp),
                    )
                }
            } else {
                items(ordres, key = { it.id }) { ordre ->
                    CarteOrdre(ordre = ordre, devise = devise, onEdit = { onEditBrouillon(ordre) })
                }
            }
        }
    }
}

@Composable
private fun CarteOrdre(ordre: OperationRecordEntity, devise: String, onEdit: () -> Unit) {
    val dateStr = remember(ordre.createdAt) {
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(ordre.createdAt))
    }
    val payload = remember(ordre.notes) { ProductionCodec.decode(ordre.notes) }
    val estValide = ordre.status == OperationStatus.VALIDATED.name

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, MissaBorder),
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(ordre.reference, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MissaInk)
                Spacer(Modifier.weight(1f))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (estValide) Color(0xFFDCFCE7) else Color(0xFFF3F4F6),
                ) {
                    Text(
                        stringResource(if (estValide) R.string.ach_valide else R.string.ach_brouillon),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (estValide) Color(0xFF15803D) else MissaMuted,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                payload?.let { "${it.produitNom} (x${fmtQuantite(it.quantite)})" } ?: ordre.title,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Medium,
                color = MissaInk,
            )
            payload?.composants?.let { comps ->
                Text(
                    stringResource(
                        if (ordre.status == OperationStatus.DRAFT.name) R.string.pro_composants_prevus else R.string.pro_composants_consommes,
                        comps.size,
                    ),
                    fontSize = 11.sp,
                    color = VioletProduction,
                )
            }
            payload?.coutMatieres?.let { cout ->
                Text("Coût matières (CUMP) : ${fmtValeur(cout, devise)}", fontSize = 11.sp, color = MissaMuted)
            }
            Text(dateStr, fontSize = 10.sp, color = MissaMuted)
            if (ordre.status == OperationStatus.DRAFT.name) {
                TextButton(onClick = onEdit) { Text(stringResource(R.string.pro_modifier_brouillon), color = VioletProduction, fontSize = 11.sp) }
            }
        }
    }
}

@Composable
private fun FormulaireOrdreProduction(
    vm: ProductionViewModel,
    message: ProductionViewModel.ActionMessage?,
    onBack: () -> Unit,
) {
    val ui by vm.uiState.collectAsStateWithLifecycle()
    val fabricables by vm.fabricables.collectAsStateWithLifecycle()
    val composantsDispo by vm.composantsDisponibles.collectAsStateWithLifecycle()
    val enregistrement by vm.enregistrement.collectAsStateWithLifecycle()

    var dialogueComposant by remember { mutableStateOf(false) }
    // Texte saisi conservé tel quel (sinon impossible d'effacer le champ pour retaper).
    var quantiteTexte by remember(ui.editingOrderId) { mutableStateOf(
            ui.quantiteAProduire.let { q -> if (q % 1.0 == 0.0) q.toLong().toString() else q.toString() },
        ) }

    MissaFormulaireTheme(AppModule.PRODUCTION.couleur) {
    Column(Modifier.fillMaxSize()) {
        MissaTopAppBar(
            title = stringResource(if (ui.editingOrderId == null) R.string.pro_nouvel_ordre else R.string.pro_modifier_ordre),
            onBack = onBack,
            couleurFond = AppModule.PRODUCTION.couleurPale,
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // ① Produit fini et quantité
            item { MissaFormSectionTitre(stringResource(R.string.form_section_article), numero = 1) }
            item {
                MissaChampListe(
                    libelle = stringResource(R.string.pro_produit_a_fabriquer),
                    options = fabricables.map { it to it.nom },
                    selection = ui.selectedProduct,
                    onSelection = vm::selectFabricable,
                    icone = Iv.Inventory2,
                    requis = true,
                )
            }
            item {
                MissaChampTexte(
                    valeur = quantiteTexte,
                    onValeur = { saisie ->
                        quantiteTexte = saisie
                        saisie.toDoubleOrNull()?.let(vm::setQuantiteAProduire)
                    },
                    libelle = stringResource(R.string.pro_quantite_a_fabriquer),
                    icone = Iv.LineWeight,
                    clavier = MissaClavier.DECIMAL,
                    requis = true,
                )
            }

            // ② Nomenclature (BOM)
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    MissaFormSectionTitre(
                        stringResource(R.string.pro_titre_nomenclature),
                        numero = 2,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { dialogueComposant = true }) {
                        Text("+ " + stringResource(R.string.pro_ajouter_composant), fontSize = 11.sp, color = VioletProduction)
                    }
                }
            }

            if (ui.composants.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.pro_aucun_composant),
                        fontSize = 11.sp,
                        color = MissaMuted,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
            } else {
                items(ui.composants, key = { it.productId }) { comp ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, MissaBorder),
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            val available = composantsDispo.firstOrNull { it.product.id == comp.productId }?.total ?: 0.0
                            Column(Modifier.weight(1f)) {
                                Text(comp.nom, fontSize = 12.sp, color = MissaInk)
                                Text(
                                    stringResource(R.string.pro_disponibilite_ligne, fmtQuantite(comp.quantite), fmtQuantite(available)),
                                    fontSize = 10.sp,
                                    color = if (available >= comp.quantite) Color(0xFF15803D) else Color(0xFFB91C1C),
                                )
                            }
                            IconButton(onClick = { vm.removeComposant(comp.productId) }) {
                                Icon(painterResource(Iv.DeleteOutline), null, tint = MissaInk, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            message?.let { msg ->
                item {
                    val (texte, couleur) = when (msg) {
                        is ProductionViewModel.ActionMessage.Succes -> msg.texte to Color(0xFF15803D)
                        is ProductionViewModel.ActionMessage.Erreur -> msg.texte to Color(0xFFB91C1C)
                    }
                    Surface(shape = RoundedCornerShape(10.dp), color = couleur.copy(alpha = 0.12f)) {
                        Text(
                            texte,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = couleur,
                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                        )
                    }
                }
            }
        }

        // Barre de lancement
        val ordreComplet = ui.selectedProduct != null && ui.composants.isNotEmpty() &&
            (quantiteTexte.toDoubleOrNull() ?: 0.0) > 0.0
        MissaFormPied(
            texte = stringResource(R.string.pro_lancer_fabrication),
            onValider = { vm.lancerOrdre(draft = false) },
            actif = ordreComplet,
            enCours = enregistrement,
            secondaire = stringResource(R.string.ach_brouillon) to { vm.lancerOrdre(draft = true) },
            secondaireActif = ordreComplet,
        )
    }
    }

    if (dialogueComposant) {
        DialogueAjoutComposant(
            options = composantsDispo,
            onFermer = { dialogueComposant = false },
            onAjouter = { comp, qte ->
                vm.addComposant(comp, qte)
                dialogueComposant = false
            },
        )
    }
}


@Composable
private fun DialogueAjoutComposant(
    options: List<ProductWithStock>,
    onFermer: () -> Unit,
    onAjouter: (ProductWithStock, Double) -> Unit,
) {
    var selectionne by remember { mutableStateOf(options.firstOrNull()) }
    var quantite by remember { mutableStateOf("1") }

    MissaFormDialogue(
        titre = stringResource(R.string.pro_ajouter_composant),
        icone = Iv.Inventory2,
        couleur = AppModule.PRODUCTION.couleur,
        onFermer = onFermer,
        libelleValider = stringResource(R.string.ops_save),
        validerActif = selectionne != null && (quantite.toDoubleOrNull() ?: 0.0) > 0.0,
        onValider = {
            selectionne?.let { prod -> onAjouter(prod, quantite.toDoubleOrNull() ?: 1.0) }
        },
    ) {
        MissaChampListe(
            libelle = stringResource(R.string.pro_choisir_matiere),
            options = options.map { it to stringResource(R.string.pro_composant_stock_total, it.nom, fmtQuantite(it.total)) },
            selection = selectionne,
            onSelection = { selectionne = it },
            icone = Iv.Inventory2,
            requis = true,
        )
        MissaChampTexte(quantite, { quantite = it }, stringResource(R.string.pro_quantite_requise), icone = Iv.LineWeight, clavier = MissaClavier.DECIMAL, requis = true)
    }
}
