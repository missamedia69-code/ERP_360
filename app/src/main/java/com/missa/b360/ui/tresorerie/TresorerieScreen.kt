package com.missa.b360.ui.tresorerie

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.missa.b360.R
import com.missa.b360.core.data.entity.CompteTresorerieEntity
import com.missa.b360.core.data.entity.MouvementTresorerieEntity
import com.missa.b360.core.data.entity.SensMouvement
import com.missa.b360.core.data.entity.TypeCompteTresorerie
import com.missa.b360.core.domain.model.SoldeCompte
import com.missa.b360.core.domain.model.TresorerieRules
import com.missa.b360.core.util.MoneyUtils
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

/** Vert monétaire caractéristique du module Trésorerie — source unique : [AppModule.TRESORERIE]. */
private val VertTresorerie: Color get() = AppModule.TRESORERIE.couleur

@Composable
fun TresorerieScreen(
    onBack: () -> Unit,
    onNaviguer: (String) -> Unit = {},
    vm: TresorerieViewModel = hiltViewModel(),
) {
    val etat by vm.etat.collectAsStateWithLifecycle()
    val devise by vm.devise.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val enCours by vm.enCours.collectAsStateWithLifecycle()
    val compteFiltre by vm.compteFiltre.collectAsStateWithLifecycle()

    var dialogueNouveauCompte by remember { mutableStateOf(false) }
    var dialogueMouvement by remember { mutableStateOf<SensMouvement?>(null) }
    var dialogueVirement by remember { mutableStateOf(false) }

    LaunchedEffect(message) {
        if (message != null) {
            dialogueNouveauCompte = false
            dialogueMouvement = null
            dialogueVirement = false
            kotlinx.coroutines.delay(3_000)
            vm.effacerMessage()
        }
    }

    Column(Modifier.fillMaxSize()) {
        MissaTopAppBar(
            title = stringResource(R.string.module_tresorerie),
            onBack = onBack,
            couleurFond = AppModule.TRESORERIE.couleurPale,
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // --- Carte Synthèse Trésorerie ---
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = VertTresorerie.copy(alpha = 0.16f),
                ) {
                    Column(Modifier.fillMaxWidth().padding(14.dp)) {
                        Text(
                            stringResource(R.string.tre_solde_global),
                            fontSize = 11.sp,
                            color = MissaMuted,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            fmtValeur(etat.soldeGlobal, devise),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MissaInk,
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(stringResource(R.string.tre_flux_entrees), fontSize = 10.sp, color = MissaMuted)
                                Text(
                                    fmtValeur(etat.fluxDuMois.entrees, devise),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF15803D),
                                )
                            }
                            Column {
                                Text(stringResource(R.string.tre_flux_sorties), fontSize = 10.sp, color = MissaMuted)
                                Text(
                                    fmtValeur(etat.fluxDuMois.sorties, devise),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFB91C1C),
                                )
                            }
                            Column {
                                Text(stringResource(R.string.tre_flux_net), fontSize = 10.sp, color = MissaMuted)
                                Text(
                                    fmtValeur(etat.fluxDuMois.net, devise),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (etat.fluxDuMois.net >= 0) Color(0xFF15803D) else Color(0xFFB91C1C),
                                )
                            }
                        }
                    }
                }
            }

            // --- Actions Rapides ---
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { dialogueMouvement = SensMouvement.IN },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = VertTresorerie, contentColor = Color.White),
                    ) {
                        Icon(painterResource(Iv.TrendingUp), null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.tre_sens_entree), fontSize = 11.sp, color = Color.White)
                    }
                    Button(
                        onClick = { dialogueMouvement = SensMouvement.OUT },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB91C1C), contentColor = Color.White),
                    ) {
                        Icon(painterResource(Iv.TrendingDown), null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.tre_sens_sortie), fontSize = 11.sp, color = Color.White)
                    }
                    OutlinedButton(
                        onClick = { dialogueVirement = true },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(painterResource(Iv.CompareArrows), null, tint = MissaInk, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.tre_virement), fontSize = 11.sp, color = MissaInk)
                    }
                }
            }

            // --- Comptes de Trésorerie (Horizontal) ---
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.tre_titre_comptes),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MissaInk,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { dialogueNouveauCompte = true }) {
                        Text("+ " + stringResource(R.string.tre_nouveau_compte), fontSize = 11.sp, color = VertTresorerie)
                    }
                }
            }

            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    items(etat.comptes, key = { it.compte.id }) { soldeCompte ->
                        val estSelectionne = compteFiltre == soldeCompte.compte.id
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (estSelectionne) VertTresorerie.copy(alpha = 0.2f) else Color.White,
                            border = BorderStroke(1.dp, if (estSelectionne) VertTresorerie else MissaBorder),
                            modifier = Modifier
                                .width(170.dp)
                                .clickable { vm.filtrerCompte(soldeCompte.compte.id) },
                        ) {
                            Column(Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        painterResource(
                                            when (TresorerieRules.typeCompte(soldeCompte.compte.type)) {
                                                TypeCompteTresorerie.CAISSE -> Iv.Payments
                                                TypeCompteTresorerie.BANQUE -> Iv.AccountBalance
                                                TypeCompteTresorerie.MOBILE_MONEY -> Iv.Smartphone
                                            },
                                        ),
                                        contentDescription = null,
                                        tint = MissaInk,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        soldeCompte.compte.nom,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MissaInk,
                                        maxLines = 1,
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    fmtValeur(soldeCompte.solde, devise),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (soldeCompte.solde >= 0) MissaInk else Color(0xFFB91C1C),
                                )
                                Text(
                                    "${soldeCompte.nombreMouvements} op.",
                                    fontSize = 10.sp,
                                    color = MissaMuted,
                                )
                            }
                        }
                    }
                }
            }

            // --- Registre des Derniers Mouvements ---
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.tre_titre_mouvements),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MissaInk,
                    )
                    if (compteFiltre != null) {
                        Spacer(Modifier.width(8.dp))
                        Surface(shape = RoundedCornerShape(8.dp), color = VertTresorerie.copy(alpha = 0.15f)) {
                            Text(
                                etat.comptes.firstOrNull { it.compte.id == compteFiltre }?.compte?.nom ?: "",
                                fontSize = 10.sp,
                                color = VertTresorerie,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
            }

            if (etat.mouvements.isEmpty()) {
                item {
                    MissaEmptyState(
                        icon = Iv.AccountBalance,
                        title = stringResource(R.string.tre_aucun_mouvement),
                        description = stringResource(R.string.tre_aucun_mouvement_desc),
                        modifier = Modifier.padding(16.dp),
                    )
                }
            } else {
                items(etat.mouvements.take(50), key = { it.id }) { mouvement ->
                    CarteMouvement(mouvement = mouvement, devise = devise)
                }
            }
        }
    }

    // --- Dialogues ---
    if (dialogueNouveauCompte) {
        TreCompteDialogue(
            enCours = enCours,
            onFermer = { dialogueNouveauCompte = false },
            onValider = { nom, type, etab, num, solde ->
                vm.creerCompte(nom, type, etab, num, solde)
            },
        )
    }

    dialogueMouvement?.let { sens ->
        TreMouvementDialogue(
            comptes = etat.comptes.map { it.compte }.filter { it.actif },
            sensInitial = sens,
            enCours = enCours,
            onFermer = { dialogueMouvement = null },
            onNouveauCompte = { dialogueNouveauCompte = true },
            onValider = { compteId, s, mt, lib, cat, tiers, mode, ref ->
                vm.enregistrerMouvement(compteId, s, mt, lib, cat, tiers, mode, ref, System.currentTimeMillis())
            },
        )
    }

    if (dialogueVirement) {
        TreVirementDialogue(
            comptes = etat.comptes.map { it.compte }.filter { it.actif },
            devise = devise,
            soldes = etat.comptes,
            enCours = enCours,
            onFermer = { dialogueVirement = false },
            onValider = { src, dst, mt, lib ->
                vm.virement(src, dst, mt, lib)
            },
        )
    }
}

@Composable
private fun CarteMouvement(mouvement: MouvementTresorerieEntity, devise: String) {
    val dateStr = remember(mouvement.date) {
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(mouvement.date))
    }
    val estEntree = mouvement.sens == SensMouvement.IN.name
    val montantSigne = if (estEntree) "+ ${fmtValeur(mouvement.montant, devise)}" else "- ${fmtValeur(mouvement.montant, devise)}"

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, MissaBorder),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painterResource(if (estEntree) Iv.TrendingUp else Iv.TrendingDown),
                contentDescription = null,
                tint = if (estEntree) Color(0xFF15803D) else Color(0xFFB91C1C),
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(mouvement.libelle, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = MissaInk)
                Text(
                    dateStr + if (!mouvement.tiers.isNullOrBlank()) " · ${mouvement.tiers}" else "",
                    fontSize = 10.5.sp,
                    color = MissaMuted,
                )
            }
            Text(
                montantSigne,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (estEntree) Color(0xFF15803D) else Color(0xFFB91C1C),
            )
        }
    }
}
