package com.missa.b360.ui.clients.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.missa.b360.R
import com.missa.b360.ui.clients.components.ClientTopBar
import com.missa.b360.ui.clients.components.EtatChargement
import com.missa.b360.ui.clients.components.EtatErreur
import com.missa.b360.ui.components.MissaChampTexte
import com.missa.b360.ui.components.MissaEmptyState
import com.missa.b360.ui.icons.Iv
import com.missa.b360.ui.navigation.AppModule
import com.missa.b360.ui.theme.MissaCanvas

/** Route `clients` : la liste lit uniquement `client_balances` (montants identiques à la fiche et au compte). */
@Composable
fun ClientListScreen(
    onBack: () -> Unit,
    onOuvrirClient: (Long) -> Unit,
    onNouveau: () -> Unit,
    onImporter: () -> Unit,
    onRelances: () -> Unit,
    ouvrirCreation: Boolean = false,
    viewModel: ClientListViewModel = hiltViewModel(),
) {
    val etat by viewModel.etat.collectAsState()
    // `clients?create=true` (Accueil) ouvre la feuille de création une seule fois par entrée de pile.
    var creationConsommee by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(ouvrirCreation) {
        if (ouvrirCreation && !creationConsommee) {
            creationConsommee = true
            onNouveau()
        }
    }
    ClientListContent(
        etat = etat,
        onBack = onBack,
        onOuvrirClient = onOuvrirClient,
        onNouveau = onNouveau,
        onImporter = onImporter,
        onRelances = onRelances,
        onRequete = viewModel::changerRequete,
        onFiltre = viewModel::changerFiltre,
        onTri = viewModel::changerTri,
        onReessayer = viewModel::reessayer,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ClientListContent(
    etat: ClientListUiState,
    onBack: () -> Unit,
    onOuvrirClient: (Long) -> Unit,
    onNouveau: () -> Unit,
    onImporter: () -> Unit,
    onRelances: () -> Unit,
    onRequete: (String) -> Unit,
    onFiltre: (com.missa.b360.core.domain.model.ClientListFilter) -> Unit,
    onTri: (com.missa.b360.core.domain.model.ClientListSort) -> Unit,
    onReessayer: () -> Unit,
) {
    Scaffold(
        containerColor = MissaCanvas,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            ClientTopBar(titre = stringResource(R.string.clients_flow_list_title), onBack = onBack) {
                IconButton(onClick = onRelances) {
                    Icon(painterResource(Iv.Notifications), stringResource(R.string.cli_ouvrir_relances), tint = AppModule.CLIENTS.couleur)
                }
                IconButton(onClick = onImporter) {
                    Icon(painterResource(Iv.UploadSimple), stringResource(R.string.clients_flow_import), tint = AppModule.CLIENTS.couleur)
                }
            }
        },
        floatingActionButton = {
            // Marge basse : la barre de modules flottante recouvre les 56 dp du bas de l'écran.
            FloatingActionButton(
                onClick = onNouveau,
                containerColor = AppModule.CLIENTS.couleur,
                contentColor = Color.White,
                modifier = Modifier.padding(bottom = 56.dp),
            ) {
                Icon(painterResource(Iv.Add), stringResource(R.string.clients_nouveau))
            }
        },
    ) { padding ->
        when {
            etat.erreur -> EtatErreur(onReessayer = onReessayer, modifier = Modifier.padding(padding))
            etat.chargement -> EtatChargement(Modifier.padding(padding))
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 140.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item { ClientListSummary(etat) }
                item {
                    MissaChampTexte(
                        valeur = etat.requete,
                        onValeur = onRequete,
                        libelle = stringResource(R.string.clients_recherche),
                        icone = Iv.Search,
                    )
                }
                item { ClientFilterChips(etat, onFiltre) }
                item { ClientSortChips(etat.tri, onTri) }
                when {
                    etat.aucunClient -> item {
                        MissaEmptyState(
                            icon = Iv.Group,
                            title = stringResource(R.string.cli_liste_vide_titre),
                            description = stringResource(R.string.cli_liste_vide_desc),
                        )
                    }
                    etat.aucunResultat -> item {
                        MissaEmptyState(
                            icon = Iv.Search,
                            title = stringResource(R.string.cli_aucun_resultat),
                            description = stringResource(R.string.cli_aucun_resultat_desc),
                        )
                    }
                    else -> items(etat.lignes, key = { it.client.id }) { ligne ->
                        ClientListRow(ligne, etat.devise, onClick = { onOuvrirClient(ligne.client.id) })
                    }
                }
            }
        }
    }
}
