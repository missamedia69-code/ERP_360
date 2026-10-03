package com.missa.b360.ui.fournisseurs.action

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.missa.b360.R
import com.missa.b360.core.domain.model.ActionFacture
import com.missa.b360.core.domain.model.FournisseurLigne
import com.missa.b360.ui.components.MissaEmptyState
import com.missa.b360.ui.components.MissaTopAppBar
import com.missa.b360.ui.fournisseurs.components.BadgeAptitude
import com.missa.b360.ui.fournisseurs.components.FournisseurCouleurs
import com.missa.b360.ui.fournisseurs.components.formatDate
import com.missa.b360.ui.fournisseurs.components.libelleRes
import com.missa.b360.ui.icons.Iv
import com.missa.b360.ui.stock.fmtValeur
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted
import com.missa.b360.ui.theme.OnbConfigCard

/** Route `fournisseurs_action` : les paiements en retard ou proches, puis les dossiers à régulariser. */
@Composable
fun FournisseurActionScreen(
    onBack: () -> Unit,
    onOuvrirFournisseur: (Long) -> Unit,
    viewModel: FournisseurActionViewModel = hiltViewModel(),
) {
    val etat by viewModel.etat.collectAsStateWithLifecycle()
    val t = etat.tableau
    Column(Modifier.fillMaxSize().background(Color.White)) {
        MissaTopAppBar(title = stringResource(R.string.four_action_titre), onBack = onBack, titreCentre = false)
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 10.dp, end = 10.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Kpi(stringResource(R.string.four_kpi_dette), fmtValeur(t.detteTotale, etat.devise), Modifier.weight(1f))
                    Kpi(stringResource(R.string.four_kpi_retard), fmtValeur(t.enRetard, etat.devise), Modifier.weight(1f), t.enRetard > 0.0)
                    Kpi(stringResource(R.string.four_kpi_sous_7j), fmtValeur(t.aPayerSousSeptJours, etat.devise), Modifier.weight(1f))
                }
            }
            if (!etat.chargement && t.vide) {
                item {
                    MissaEmptyState(
                        icon = Iv.CheckCircle,
                        title = stringResource(R.string.four_action_rien),
                        description = stringResource(R.string.four_action_rien_desc),
                    )
                }
            }
            if (t.facturesEnRetard.isNotEmpty()) {
                item { Titre(stringResource(R.string.four_action_en_retard), t.facturesEnRetard.size) }
                items(t.facturesEnRetard, key = { "r${it.facture.recordId}" }) { f ->
                    LigneFacture(f, etat.devise, enRetard = true) { onOuvrirFournisseur(f.facture.fournisseurId) }
                }
            }
            if (t.facturesBientot.isNotEmpty()) {
                item { Titre(stringResource(R.string.four_action_bientot), t.facturesBientot.size) }
                items(t.facturesBientot, key = { "b${it.facture.recordId}" }) { f ->
                    LigneFacture(f, etat.devise, enRetard = false) { onOuvrirFournisseur(f.facture.fournisseurId) }
                }
            }
            if (t.aRegulariser.isNotEmpty()) {
                item { Titre(stringResource(R.string.four_action_regulariser), t.aRegulariser.size) }
                items(t.aRegulariser, key = { "a${it.fournisseur.id}" }) { l ->
                    LigneDossier(l) { onOuvrirFournisseur(l.fournisseur.id) }
                }
            }
            if (t.bloques.isNotEmpty()) {
                item { Titre(stringResource(R.string.four_action_bloques), t.bloques.size) }
                items(t.bloques, key = { "x${it.fournisseur.id}" }) { l ->
                    LigneDossier(l) { onOuvrirFournisseur(l.fournisseur.id) }
                }
            }
        }
    }
}

@Composable
private fun Kpi(titre: String, valeur: String, modifier: Modifier, alerte: Boolean = false) {
    Surface(shape = RoundedCornerShape(12.dp), color = OnbConfigCard, modifier = modifier) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(titre, fontSize = 10.sp, color = MissaMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                valeur,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (alerte) FournisseurCouleurs.Bloque else MissaInk,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun Titre(texte: String, nombre: Int) {
    Text(
        "$texte ($nombre)",
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = MissaInk,
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
    )
}

@Composable
private fun LigneFacture(f: ActionFacture, devise: String, enRetard: Boolean, onClick: () -> Unit) {
    Surface(shape = RoundedCornerShape(12.dp), color = OnbConfigCard, modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(f.fournisseurNom, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MissaInk, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(f.facture.reference, fontSize = 11.sp, color = MissaMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    if (enRetard) stringResource(R.string.four_action_retard_jours, f.joursRetard)
                    else stringResource(R.string.four_action_echeance, formatDate(f.facture.dueAt)),
                    fontSize = 11.sp,
                    color = if (enRetard) FournisseurCouleurs.Bloque else MissaMuted,
                )
            }
            Text(fmtValeur(f.facture.outstanding, devise), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MissaInk, maxLines = 1)
        }
    }
}

@Composable
private fun LigneDossier(l: FournisseurLigne, onClick: () -> Unit) {
    Surface(shape = RoundedCornerShape(12.dp), color = OnbConfigCard, modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(l.fournisseur.nom, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MissaInk, maxLines = 1, overflow = TextOverflow.Ellipsis)
                l.aptitude.motifs.forEach { motif ->
                    Text(stringResource(motif.libelleRes()), fontSize = 11.sp, color = MissaMuted)
                }
            }
            BadgeAptitude(l.aptitude.niveau)
        }
    }
}
