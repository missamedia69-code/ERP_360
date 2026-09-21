package com.missa.b360.ui.livraison

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.missa.b360.R
import com.missa.b360.core.data.entity.ClientEntity
import com.missa.b360.core.domain.model.BonLivraison
import com.missa.b360.core.domain.model.EtapeLivraison
import com.missa.b360.ui.components.MissaEmptyState
import com.missa.b360.ui.components.MissaTopAppBar
import com.missa.b360.ui.icons.Iv
import com.missa.b360.ui.navigation.AppModule
import com.missa.b360.ui.theme.MissaBorder
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Bleu ciel caractéristique du module Livraison — source unique : [AppModule.LIVRAISON]. */
private val BleuCielLivraison: Color get() = AppModule.LIVRAISON.couleur

@Composable
fun LivraisonScreen(
    onBack: () -> Unit,
    onNaviguer: (String) -> Unit = {},
    openCreate: Boolean = false,
    vm: LivraisonViewModel = hiltViewModel(),
) {
    val etat by vm.etat.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val enCours by vm.enCours.collectAsStateWithLifecycle()
    val filtre by vm.filtre.collectAsStateWithLifecycle()

    var dialogueNouveauBL by remember { mutableStateOf(openCreate) }
    var bonAAnnuler by remember { mutableStateOf<BonLivraison?>(null) }

    LaunchedEffect(message) {
        if (message != null) {
            dialogueNouveauBL = false
            bonAAnnuler = null
            kotlinx.coroutines.delay(3_000)
            vm.effacerMessage()
        }
    }

    Column(Modifier.fillMaxSize()) {
        MissaTopAppBar(
            title = stringResource(R.string.module_livraison),
            onBack = onBack,
            couleurFond = AppModule.LIVRAISON.couleurPale,
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // --- Carte Synthèse Livraisons ---
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = BleuCielLivraison.copy(alpha = 0.2f),
                ) {
                    Column(Modifier.fillMaxWidth().padding(14.dp)) {
                        Text(
                            stringResource(R.string.liv_titre_synthese),
                            fontSize = 11.sp,
                            color = MissaMuted,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            stringResource(R.string.liv_en_cours, etat.enCours),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MissaInk,
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                stringResource(R.string.liv_taux_reussite, String.format(Locale.getDefault(), "%.1f", etat.tauxLivraison)),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MissaInk,
                            )
                            etat.delaiMoyenJours?.let { delai ->
                                Text(
                                    stringResource(R.string.liv_delai_moyen, String.format(Locale.getDefault(), "%.1f", delai)),
                                    fontSize = 11.sp,
                                    color = MissaMuted,
                                )
                            }
                        }
                    }
                }
            }

            // --- Action Rapide Nouveau BL ---
            item {
                Button(
                    onClick = { dialogueNouveauBL = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = BleuCielLivraison, contentColor = Color.White),
                ) {
                    Icon(painterResource(Iv.LocalShipping), null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.liv_nouveau_bl), color = Color.White)
                }
            }

            // --- Filtres Étapes ---
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    item {
                        FilterChip(
                            selected = filtre == null,
                            onClick = { vm.filtrer(null) },
                            label = { Text(stringResource(R.string.clients_flow_all_status), fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = BleuCielLivraison.copy(alpha = 0.25f)),
                        )
                    }
                    items(EtapeLivraison.entries.toTypedArray()) { etape ->
                        FilterChip(
                            selected = filtre == etape,
                            onClick = { vm.filtrer(etape) },
                            label = { Text(stringResource(etape.libelleRes), fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = BleuCielLivraison.copy(alpha = 0.25f)),
                        )
                    }
                }
            }

            // --- Message retour ---
            message?.let { msg ->
                item {
                    val (texte, couleur) = when (msg) {
                        is LivraisonViewModel.Message.Cree -> stringResource(R.string.liv_msg_cree, msg.reference) to Color(0xFF15803D)
                        LivraisonViewModel.Message.Avance -> stringResource(R.string.liv_msg_avance) to Color(0xFF15803D)
                        LivraisonViewModel.Message.Annule -> stringResource(R.string.liv_msg_annule) to Color(0xFFB91C1C)
                        LivraisonViewModel.Message.EtapeFinale -> stringResource(R.string.liv_msg_finale) to MissaMuted
                        else -> stringResource(R.string.ach_erreur) to Color(0xFFB91C1C)
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

            // --- Liste des Bons de Livraison ---
            item {
                Text(
                    stringResource(R.string.liv_titre_registre),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MissaInk,
                )
            }

            if (etat.bons.isEmpty()) {
                item {
                    MissaEmptyState(
                        icon = Iv.LocalShipping,
                        title = stringResource(R.string.liv_aucun_bl),
                        description = stringResource(R.string.liv_aucun_bl_desc),
                        modifier = Modifier.padding(16.dp),
                    )
                }
            } else {
                items(etat.bons, key = { it.id }) { bon ->
                    CarteBonLivraison(
                        bon = bon,
                        onAvancer = { vm.avancer(bon.id) },
                        onAnnuler = { bonAAnnuler = bon },
                    )
                }
            }
        }
    }

    if (dialogueNouveauBL) {
        DialogueNouveauBL(
            clients = etat.clients,
            enCours = enCours,
            onFermer = { dialogueNouveauBL = false },
            onValider = { cl, nom, adr, trans, col, ref ->
                vm.creer(cl, nom, adr, trans, col, ref)
            },
        )
    }

    bonAAnnuler?.let { bon ->
        AlertDialog(
            onDismissRequest = { bonAAnnuler = null },
            title = { Text(stringResource(R.string.ach_annuler), color = MissaInk) },
            text = { Text(stringResource(R.string.ach_confirmer_annulation, bon.reference), color = MissaInk) },
            confirmButton = {
                TextButton(onClick = { vm.annuler(bon.id) }) {
                    Text(stringResource(R.string.ach_annuler), color = Color(0xFFB91C1C))
                }
            },
            dismissButton = {
                TextButton(onClick = { bonAAnnuler = null }) {
                    Text(stringResource(R.string.st_annuler), color = MissaInk)
                }
            },
        )
    }
}

@Composable
private fun CarteBonLivraison(
    bon: BonLivraison,
    onAvancer: () -> Unit,
    onAnnuler: () -> Unit,
) {
    val dateStr = remember(bon.record.createdAt) {
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(bon.record.createdAt))
    }
    val etapeSuivante = bon.etapeSuivante

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, MissaBorder),
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(bon.reference, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MissaInk)
                Spacer(Modifier.weight(1f))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = when {
                        bon.annule -> Color(0xFFFEE2E2)
                        bon.etape == EtapeLivraison.LIVREE -> Color(0xFFDCFCE7)
                        bon.etape == EtapeLivraison.EXPEDIEE -> BleuCielLivraison.copy(alpha = 0.25f)
                        else -> Color(0xFFF3F4F6)
                    },
                ) {
                    Text(
                        stringResource(if (bon.annule) R.string.ach_annulee else bon.etape.libelleRes),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            bon.annule -> Color(0xFFB91C1C)
                            bon.etape == EtapeLivraison.LIVREE -> Color(0xFF15803D)
                            bon.etape == EtapeLivraison.EXPEDIEE -> Color(0xFF0284C7)
                            else -> MissaMuted
                        },
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(bon.clientNom, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MissaInk)
            bon.adresse?.let {
                Text(it, fontSize = 11.sp, color = MissaMuted)
            }
            if (bon.nombreColis > 0) {
                Text(
                    stringResource(R.string.liv_nb_colis, bon.nombreColis) + (bon.transporteur?.let { " · $it" } ?: ""),
                    fontSize = 11.sp,
                    color = BleuCielLivraison,
                )
            }
            Text(dateStr, fontSize = 10.sp, color = MissaMuted)

            if (!bon.annule && etapeSuivante != null) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = onAvancer,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = BleuCielLivraison, contentColor = Color.White),
                    ) {
                        Text(
                            stringResource(R.string.liv_passer_etape, stringResource(etapeSuivante.libelleRes)),
                            fontSize = 11.sp,
                            color = Color.White,
                        )
                    }
                    IconButton(onClick = onAnnuler, modifier = Modifier.size(36.dp)) {
                        Icon(painterResource(Iv.DeleteOutline), null, tint = Color(0xFFB91C1C), modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun DialogueNouveauBL(
    clients: List<ClientEntity>,
    enCours: Boolean,
    onFermer: () -> Unit,
    onValider: (ClientEntity?, String, String, String, String, String) -> Unit,
) {
    var clientChoisi by remember { mutableStateOf<ClientEntity?>(clients.firstOrNull()) }
    var nomLibre by remember { mutableStateOf("") }
    var adresse by remember { mutableStateOf("") }
    var transporteur by remember { mutableStateOf("") }
    var colis by remember { mutableStateOf("1") }
    var refOrigine by remember { mutableStateOf("") }
    var ouvert by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onFermer,
        title = { Text(stringResource(R.string.liv_nouveau_bl), fontSize = 16.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = clientChoisi?.nom ?: stringResource(R.string.sales_select_client),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.clients_flow_contact), fontSize = 11.sp) },
                        trailingIcon = { Icon(painterResource(Iv.ArrowDropDown), null, tint = MissaInk) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Box(Modifier.matchParentSize().clickable { ouvert = true })
                    DropdownMenu(expanded = ouvert, onDismissRequest = { ouvert = false }) {
                        clients.forEach { cl ->
                            DropdownMenuItem(
                                text = { Text(cl.nom, color = MissaInk) },
                                onClick = {
                                    clientChoisi = cl
                                    adresse = cl.adresse.orEmpty()
                                    ouvert = false
                                },
                            )
                        }
                    }
                }
                if (clientChoisi == null) {
                    OutlinedTextField(
                        value = nomLibre,
                        onValueChange = { nomLibre = it },
                        label = { Text(stringResource(R.string.clients_flow_company_name), fontSize = 11.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                OutlinedTextField(
                    value = adresse,
                    onValueChange = { adresse = it },
                    label = { Text(stringResource(R.string.clients_flow_address), fontSize = 11.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = transporteur,
                    onValueChange = { transporteur = it },
                    label = { Text(stringResource(R.string.liv_champ_transporteur), fontSize = 11.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = colis,
                    onValueChange = { colis = it.filter { c -> c.isDigit() } },
                    label = { Text(stringResource(R.string.liv_champ_nb_colis), fontSize = 11.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onValider(clientChoisi, nomLibre, adresse, transporteur, colis, refOrigine) },
                enabled = (clientChoisi != null || nomLibre.isNotBlank()) && !enCours,
            ) { Text(stringResource(R.string.ops_save)) }
        },
        dismissButton = {
            TextButton(onClick = onFermer) { Text(stringResource(R.string.ops_cancel)) }
        },
    )
}
