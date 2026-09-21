package com.missa.b360.ui.comptabilite

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.missa.b360.core.domain.model.EcritureComptable
import com.missa.b360.core.domain.model.RubriqueComptable
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

/** Vert compta caractéristique du module Comptabilité — source unique : [AppModule.COMPTABILITE]. */
private val VertCompta: Color get() = AppModule.COMPTABILITE.couleur

@Composable
fun ComptabiliteScreen(
    onBack: () -> Unit,
    onNaviguer: (String) -> Unit = {},
    vm: ComptabiliteViewModel = hiltViewModel(),
) {
    val synthese by vm.synthese.collectAsStateWithLifecycle()
    val devise by vm.devise.collectAsStateWithLifecycle()
    val periode by vm.periode.collectAsStateWithLifecycle()

    val res = synthese.resultat
    val resultatNet = res.resultat
    val estBenefice = resultatNet >= 0

    Column(Modifier.fillMaxSize()) {
        MissaTopAppBar(
            title = stringResource(R.string.module_comptabilite),
            onBack = onBack,
            couleurFond = AppModule.COMPTABILITE.couleurPale,
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // --- Filtre Période ---
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = periode == ComptabiliteViewModel.Periode.MOIS,
                        onClick = { vm.choisirPeriode(ComptabiliteViewModel.Periode.MOIS) },
                        label = { Text(stringResource(R.string.cpt_periode_mois_courant), fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = VertCompta.copy(alpha = 0.2f),
                        ),
                    )
                    FilterChip(
                        selected = periode == ComptabiliteViewModel.Periode.MOIS_PRECEDENT,
                        onClick = { vm.choisirPeriode(ComptabiliteViewModel.Periode.MOIS_PRECEDENT) },
                        label = { Text(stringResource(R.string.cpt_periode_mois_precedent), fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = VertCompta.copy(alpha = 0.2f),
                        ),
                    )
                    FilterChip(
                        selected = periode == ComptabiliteViewModel.Periode.ANNEE,
                        onClick = { vm.choisirPeriode(ComptabiliteViewModel.Periode.ANNEE) },
                        label = { Text(stringResource(R.string.cpt_periode_annee), fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = VertCompta.copy(alpha = 0.2f),
                        ),
                    )
                }
            }

            // --- Carte Compte de Résultat Simplifié ---
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = VertCompta.copy(alpha = 0.16f),
                ) {
                    Column(Modifier.fillMaxWidth().padding(14.dp)) {
                        Text(
                            stringResource(R.string.cpt_resultat_net),
                            fontSize = 11.sp,
                            color = MissaMuted,
                        )
                        Spacer(Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                fmtValeur(resultatNet, devise),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (estBenefice) Color(0xFF15803D) else Color(0xFFB91C1C),
                            )
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (estBenefice) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                            ) {
                                Text(
                                    "${String.format(Locale.getDefault(), "%.1f", res.marge)} %",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (estBenefice) Color(0xFF15803D) else Color(0xFFB91C1C),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(stringResource(R.string.cpt_total_produits), fontSize = 10.sp, color = MissaMuted)
                                Text(
                                    fmtValeur(res.totalProduits, devise),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF15803D),
                                )
                            }
                            Column {
                                Text(stringResource(R.string.cpt_total_charges), fontSize = 10.sp, color = MissaMuted)
                                Text(
                                    fmtValeur(res.totalCharges, devise),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFB91C1C),
                                )
                            }
                        }
                    }
                }
            }

            // --- Position Fiscale TVA ---
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, MissaBorder),
                ) {
                    Column(Modifier.fillMaxWidth().padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                stringResource(R.string.cpt_position_tva),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MissaInk,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                "Taux : ${synthese.tauxTva} %",
                                fontSize = 11.sp,
                                color = MissaMuted,
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(stringResource(R.string.cpt_tva_collectee), fontSize = 10.sp, color = MissaMuted)
                                Text(fmtValeur(synthese.tvaCollectee, devise), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MissaInk)
                            }
                            Column {
                                Text(stringResource(R.string.cpt_tva_deductible), fontSize = 10.sp, color = MissaMuted)
                                Text(fmtValeur(synthese.tvaDeductible, devise), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MissaInk)
                            }
                            Column {
                                Text(
                                    stringResource(if (synthese.tvaAPayer >= 0) R.string.cpt_tva_due else R.string.cpt_tva_credit),
                                    fontSize = 10.sp,
                                    color = MissaMuted,
                                )
                                Text(
                                    fmtValeur(synthese.tvaAPayer, devise),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (synthese.tvaAPayer >= 0) Color(0xFFB91C1C) else Color(0xFF15803D),
                                )
                            }
                        }
                    }
                }
            }

            // --- Journal Général des Écritures ---
            item {
                Text(
                    stringResource(R.string.cpt_journal_general),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MissaInk,
                )
            }

            if (synthese.journal.isEmpty()) {
                item {
                    MissaEmptyState(
                        icon = Iv.Calculator,
                        title = stringResource(R.string.cpt_aucun_journal),
                        description = stringResource(R.string.cpt_aucun_journal_desc),
                        modifier = Modifier.padding(16.dp),
                    )
                }
            } else {
                items(synthese.journal, key = { "${it.reference}-${it.date}" }) { ecriture ->
                    CarteEcriture(ecriture = ecriture, devise = devise)
                }
            }
        }
    }
}

@Composable
private fun CarteEcriture(ecriture: EcritureComptable, devise: String) {
    val dateStr = remember(ecriture.date) {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(ecriture.date))
    }
    val estProduit = ecriture.rubrique in listOf(RubriqueComptable.VENTES, RubriqueComptable.AUTRES_PRODUITS)

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, MissaBorder),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(ecriture.reference, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MissaInk)
                    Spacer(Modifier.width(6.dp))
                    Surface(shape = RoundedCornerShape(6.dp), color = VertCompta.copy(alpha = 0.15f)) {
                        Text(
                            stringResource(ecriture.rubrique.libelleRes),
                            fontSize = 10.sp,
                            color = VertCompta,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(ecriture.libelle, fontSize = 12.sp, color = MissaInk)
                Text(dateStr, fontSize = 10.sp, color = MissaMuted)
            }
            Text(
                (if (estProduit) "+ " else "- ") + fmtValeur(ecriture.montant, devise),
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (estProduit) Color(0xFF15803D) else Color(0xFFB91C1C),
            )
        }
    }
}
