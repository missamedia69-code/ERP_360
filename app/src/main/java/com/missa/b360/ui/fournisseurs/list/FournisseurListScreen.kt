package com.missa.b360.ui.fournisseurs.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.missa.b360.core.domain.model.FournisseurFiltre
import com.missa.b360.core.domain.model.FournisseurTri
import com.missa.b360.ui.components.MissaChampTexte
import com.missa.b360.ui.components.MissaEmptyState
import com.missa.b360.ui.components.MissaTopAppBar
import com.missa.b360.ui.fournisseurs.components.FournisseurCouleurs
import com.missa.b360.ui.fournisseurs.components.LigneFournisseur
import com.missa.b360.ui.fournisseurs.components.PuceFournisseur
import com.missa.b360.ui.icons.Iv
import com.missa.b360.ui.stock.fmtValeur
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted
import com.missa.b360.ui.theme.OnbConfigCard

/** Route `module_fournisseurs` : la liste lit les soldes et scores en cache (mêmes montants que la fiche). */
@Composable
fun FournisseurListScreen(
    onBack: () -> Unit,
    onOuvrirFournisseur: (Long) -> Unit,
    onNouveau: () -> Unit,
    onAction: () -> Unit,
    ouvrirCreation: Boolean = false,
    viewModel: FournisseurListViewModel = hiltViewModel(),
) {
    val etat by viewModel.etat.collectAsStateWithLifecycle()
    // `module_fournisseurs?create=true` (Accueil) ouvre la création une seule fois par entrée de pile.
    var creationConsommee by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(ouvrirCreation) {
        if (ouvrirCreation && !creationConsommee) {
            creationConsommee = true
            onNouveau()
        }
    }
    FournisseurListContent(
        etat = etat,
        onBack = onBack,
        onOuvrirFournisseur = onOuvrirFournisseur,
        onNouveau = onNouveau,
        onAction = onAction,
        onRequete = { viewModel.changerRequete(it) },
        onFiltre = { viewModel.changerFiltre(it) },
        onTri = { viewModel.changerTri(it) },
    )
}

@Composable
internal fun FournisseurListContent(
    etat: FournisseurListUiState,
    onBack: () -> Unit,
    onOuvrirFournisseur: (Long) -> Unit,
    onNouveau: () -> Unit,
    onAction: () -> Unit,
    onRequete: (String) -> Unit,
    onFiltre: (FournisseurFiltre) -> Unit,
    onTri: (FournisseurTri) -> Unit,
) {
    Column(Modifier.fillMaxSize().background(Color.White)) {
        MissaTopAppBar(
            title = stringResource(R.string.module_fournisseurs),
            onBack = onBack,
            titreCentre = false,
            actions = {
                IconButton(onClick = onAction) {
                    Icon(painterResource(Iv.Notifications), stringResource(R.string.four_action_ouvrir), tint = MissaInk)
                }
            },
        )
        LazyColumn(
            Modifier.fillMaxSize(),
            // Marge basse : la barre de modules flottante recouvre le bas de l'écran.
            contentPadding = PaddingValues(start = 10.dp, end = 10.dp, top = 4.dp, bottom = 132.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            item { ResumeFournisseurs(etat) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    MissaChampTexte(
                        valeur = etat.requete,
                        onValeur = onRequete,
                        libelle = stringResource(R.string.four_rechercher),
                        icone = Iv.Search,
                        modifier = Modifier.weight(1f),
                    )
                    Button(
                        onClick = onNouveau,
                        colors = ButtonDefaults.buttonColors(containerColor = FournisseurCouleurs.Nuit, contentColor = Color.White),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Icon(painterResource(Iv.Add), null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.four_btn_nouveau), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
            item { FiltresFournisseurs(etat, onFiltre) }
            item { TrisFournisseurs(etat.tri, onTri) }
            when {
                etat.aucunFournisseur -> item {
                    MissaEmptyState(
                        icon = Iv.Handshake,
                        title = stringResource(R.string.four_aucun),
                        description = stringResource(R.string.four_aucun_desc),
                    )
                }
                etat.lignes.isEmpty() -> item {
                    MissaEmptyState(icon = Iv.Search, title = stringResource(R.string.four_liste_aucun_resultat))
                }
                else -> items(etat.lignes, key = { it.fournisseur.id }) { ligne ->
                    LigneFournisseur(ligne, etat.devise) { onOuvrirFournisseur(ligne.fournisseur.id) }
                }
            }
        }
    }
}

@Composable
private fun ResumeFournisseurs(etat: FournisseurListUiState) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Cellule(stringResource(R.string.four_kpi_dette), fmtValeur(etat.detteTotale, etat.devise), Modifier.weight(1f))
        Cellule(
            stringResource(R.string.four_kpi_retard),
            fmtValeur(etat.enRetard, etat.devise),
            Modifier.weight(1f),
            alerte = etat.enRetard > 0.0,
        )
        Cellule(stringResource(R.string.four_kpi_a_regulariser), etat.aRegulariser.toString(), Modifier.weight(0.7f))
    }
}

@Composable
private fun Cellule(titre: String, valeur: String, modifier: Modifier, alerte: Boolean = false) {
    Surface(shape = RoundedCornerShape(12.dp), color = OnbConfigCard, modifier = modifier) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(titre, fontSize = 10.sp, color = MissaMuted, maxLines = 1)
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
private fun FiltresFournisseurs(etat: FournisseurListUiState, onFiltre: (FournisseurFiltre) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(FournisseurFiltre.entries) { filtre ->
            val nombre = etat.compteurs[filtre] ?: 0
            PuceFournisseur(
                libelle = "${stringResource(filtre.libelleRes())} ($nombre)",
                selectionnee = etat.filtre == filtre,
                onClick = { onFiltre(filtre) },
            )
        }
    }
}

@Composable
private fun TrisFournisseurs(tri: FournisseurTri, onTri: (FournisseurTri) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        item { Text(stringResource(R.string.four_tri_titre), fontSize = 11.sp, color = MissaMuted) }
        items(FournisseurTri.entries) { option ->
            PuceFournisseur(stringResource(option.libelleRes()), tri == option) { onTri(option) }
        }
    }
}

private fun FournisseurFiltre.libelleRes(): Int = when (this) {
    FournisseurFiltre.TOUS -> R.string.four_tous
    FournisseurFiltre.A_PAYER -> R.string.four_filtre_a_payer
    FournisseurFiltre.EN_RETARD -> R.string.four_filtre_en_retard
    FournisseurFiltre.A_REGULARISER -> R.string.four_filtre_a_regulariser
    FournisseurFiltre.BLOQUES -> R.string.four_filtre_bloques
    FournisseurFiltre.ARCHIVES -> R.string.four_filtre_archives
}

private fun FournisseurTri.libelleRes(): Int = when (this) {
    FournisseurTri.NOM -> R.string.four_tri_nom
    FournisseurTri.DETTE -> R.string.four_tri_dette
    FournisseurTri.RETARD -> R.string.four_tri_retard
    FournisseurTri.SCORE -> R.string.four_tri_score
}
