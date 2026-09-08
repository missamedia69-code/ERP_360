package com.missa.b360.ui.comptabilite

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.missa.b360.R
import com.missa.b360.core.domain.model.EcritureComptable
import com.missa.b360.core.domain.model.ResultatPeriode
import com.missa.b360.core.domain.model.TotalRubrique
import com.missa.b360.core.domain.model.ModuleCode
import com.missa.b360.core.util.DateUtils
import com.missa.b360.core.util.Iso4217
import com.missa.b360.core.util.MoneyUtils
import com.missa.b360.ui.components.Filigrane
import com.missa.b360.ui.components.MissaFondFiligrane
import com.missa.b360.ui.components.sectionFonctionsModule
import com.missa.b360.ui.theme.BrandBlue
import com.missa.b360.ui.theme.MissaBorder
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted
import com.missa.b360.ui.theme.MissaSoftBlue
import com.missa.b360.ui.theme.MissaSurface
import com.missa.b360.ui.theme.ProfileGreen
import com.missa.b360.ui.theme.Red40

/**
 * Module Comptabilité (CPT) — compte de résultat simplifié, position de TVA et
 * journal consolidé de la période.
 *
 * Aucune saisie : tout provient des ventes, achats et mouvements de trésorerie
 * déjà enregistrés. C'est la garantie qu'un chiffre affiché ici correspond
 * exactement à une pièce existante ailleurs.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComptabiliteScreen(
    onBack: () -> Unit,
    onNaviguer: (String) -> Unit,
    viewModel: ComptabiliteViewModel = hiltViewModel(),
) {
    val synthese by viewModel.synthese.collectAsState()
    val devise by viewModel.devise.collectAsState()
    val periode by viewModel.periode.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.module_comptabilite)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.ob_retour),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MissaSurface),
            )
        },
    ) { padding ->
        MissaFondFiligrane(
            filigrane = Filigrane.pour(ModuleCode.CPT),
            modifier = Modifier.padding(padding),
        ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    ComptabiliteViewModel.Periode.entries.forEach { candidate ->
                        FilterChip(
                            selected = periode == candidate,
                            onClick = { viewModel.choisirPeriode(candidate) },
                            label = {
                                Text(
                                    stringResource(
                                        when (candidate) {
                                            ComptabiliteViewModel.Periode.MOIS ->
                                                R.string.cpt_periode_mois
                                            ComptabiliteViewModel.Periode.MOIS_PRECEDENT ->
                                                R.string.cpt_periode_mois_precedent
                                            ComptabiliteViewModel.Periode.ANNEE ->
                                                R.string.cpt_periode_annee
                                        },
                                    ),
                                    fontSize = 11.5.sp,
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BrandBlue.copy(alpha = 0.15f),
                            ),
                        )
                    }
                }
            }

            item { CptResultatCarte(synthese.resultat, devise) }

            if (synthese.tauxTva > 0.0) {
                item {
                    CptTvaCarte(
                        taux = synthese.tauxTva,
                        collectee = synthese.tvaCollectee,
                        deductible = synthese.tvaDeductible,
                        aPayer = synthese.tvaAPayer,
                        devise = devise,
                    )
                }
            }

            item {
                Text(
                    text = stringResource(R.string.cpt_journal),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MissaInk,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }

            if (synthese.journal.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.cpt_journal_vide),
                        fontSize = 12.sp,
                        color = MissaMuted,
                    )
                }
            } else {
                items(synthese.journal, key = { it.date.toString() + it.libelle + it.montant }) {
                    CptEcritureLigne(it, devise)
                }
            }

            // Sommaire des fonctionnalités du module, disponibles et prévues.
            sectionFonctionsModule(ModuleCode.CPT) { route -> onNaviguer(route) }
        }
        }
    }
}

/** Produits, charges et résultat net de la période. */
@Composable
private fun CptResultatCarte(resultat: ResultatPeriode, devise: String) {
    val benefice = resultat.resultat >= 0
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MissaSurface),
        border = BorderStroke(1.dp, MissaBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(horizontal = 15.dp, vertical = 13.dp)) {
            Text(
                text = stringResource(R.string.cpt_resultat_titre),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MissaInk,
            )
            Spacer(Modifier.height(9.dp))
            CptTotalLigne(
                libelle = stringResource(R.string.cpt_total_produits),
                montant = resultat.totalProduits,
                devise = devise,
                couleur = ProfileGreen,
                gras = true,
            )
            resultat.produits.forEach { CptSousLigne(it, devise) }
            Spacer(Modifier.height(7.dp))
            CptTotalLigne(
                libelle = stringResource(R.string.cpt_total_charges),
                montant = resultat.totalCharges,
                devise = devise,
                couleur = Red40,
                gras = true,
            )
            resultat.charges.forEach { CptSousLigne(it, devise) }
            Spacer(Modifier.height(9.dp))
            HorizontalDivider(color = MissaBorder)
            Spacer(Modifier.height(9.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (benefice) {
                        Icons.AutoMirrored.Outlined.TrendingUp
                    } else {
                        Icons.AutoMirrored.Outlined.TrendingDown
                    },
                    contentDescription = null,
                    tint = if (benefice) ProfileGreen else Red40,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    text = stringResource(
                        if (benefice) R.string.cpt_benefice else R.string.cpt_perte,
                    ),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MissaInk,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = MoneyUtils.format(resultat.resultat, devise),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (benefice) ProfileGreen else Red40,
                )
            }
            if (resultat.totalProduits > 0) {
                Text(
                    text = stringResource(
                        R.string.cpt_taux_marge,
                        Iso4217.formatPourcentage(resultat.marge),
                    ),
                    fontSize = 11.sp,
                    color = MissaMuted,
                )
            }
        }
    }
}

/** Position de TVA de la période : collectée, déductible, solde. */
@Composable
private fun CptTvaCarte(
    taux: Double,
    collectee: Double,
    deductible: Double,
    aPayer: Double,
    devise: String,
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MissaSoftBlue),
        border = BorderStroke(1.dp, BrandBlue.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Receipt,
                    contentDescription = null,
                    tint = BrandBlue,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    text = stringResource(
                        R.string.cpt_tva_titre,
                        Iso4217.formatPourcentage(taux),
                    ),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MissaInk,
                )
            }
            Spacer(Modifier.height(7.dp))
            CptTotalLigne(stringResource(R.string.cpt_tva_collectee), collectee, devise, MissaInk)
            CptTotalLigne(stringResource(R.string.cpt_tva_deductible), deductible, devise, MissaInk)
            Spacer(Modifier.height(4.dp))
            CptTotalLigne(
                libelle = stringResource(
                    if (aPayer >= 0) R.string.cpt_tva_a_payer else R.string.cpt_tva_credit,
                ),
                montant = kotlin.math.abs(aPayer),
                devise = devise,
                couleur = if (aPayer >= 0) Red40 else ProfileGreen,
                gras = true,
            )
            Text(
                text = stringResource(R.string.cpt_tva_note),
                fontSize = 10.5.sp,
                color = MissaMuted,
            )
        }
    }
}

@Composable
private fun CptTotalLigne(
    libelle: String,
    montant: Double,
    devise: String,
    couleur: Color,
    gras: Boolean = false,
) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
        Text(
            text = libelle,
            fontSize = 12.sp,
            fontWeight = if (gras) FontWeight.SemiBold else FontWeight.Normal,
            color = MissaInk,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = MoneyUtils.format(montant, devise),
            fontSize = 12.sp,
            fontWeight = if (gras) FontWeight.SemiBold else FontWeight.Normal,
            color = couleur,
        )
    }
}

@Composable
private fun CptSousLigne(total: TotalRubrique, devise: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(start = 12.dp, top = 1.dp)) {
        Text(
            text = stringResource(total.rubrique.libelleRes),
            fontSize = 11.sp,
            color = MissaMuted,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = MoneyUtils.format(total.montant, devise),
            fontSize = 11.sp,
            color = MissaMuted,
        )
    }
}

/** Une écriture du journal : date, libellé, rubrique, montant. */
@Composable
private fun CptEcritureLigne(ecriture: EcritureComptable, devise: String) {
    val produit = ecriture.rubrique.produit
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(30.dp)
                .background(
                    if (produit) ProfileGreen else Red40,
                    RoundedCornerShape(2.dp),
                ),
        )
        Spacer(Modifier.width(9.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = ecriture.libelle,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Medium,
                color = MissaInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = listOfNotNull(
                    DateUtils.formatDate(ecriture.date),
                    stringResource(ecriture.rubrique.libelleRes),
                    ecriture.reference,
                ).joinToString(" · "),
                fontSize = 10.5.sp,
                color = MissaMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = (if (produit) "+ " else "− ") + MoneyUtils.format(ecriture.montant, devise),
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (produit) ProfileGreen else Red40,
        )
        if (ecriture.tresorerie) {
            Spacer(Modifier.width(5.dp))
            Icon(
                imageVector = Icons.Outlined.AccountBalanceWallet,
                contentDescription = stringResource(R.string.cpt_origine_tresorerie),
                tint = MissaMuted,
                modifier = Modifier.size(13.dp),
            )
        }
    }
}
