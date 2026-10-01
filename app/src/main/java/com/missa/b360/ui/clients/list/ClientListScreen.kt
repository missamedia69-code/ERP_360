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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import com.missa.b360.core.domain.model.ClientAdvancedFilter
import com.missa.b360.ui.clients.components.appeler
import com.missa.b360.ui.clients.components.ouvrirWhatsApp
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
    val contexte = LocalContext.current
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
        onAvance = viewModel::changerAvance,
        onAppeler = { telephone -> contexte.appeler(telephone) },
        onWhatsApp = { telephone -> contexte.ouvrirWhatsApp(telephone) },
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
    onAvance: (ClientAdvancedFilter) -> Unit,
    onAppeler: (String) -> Unit,
    onWhatsApp: (String) -> Unit,
    onReessayer: () -> Unit,
) {
    var filtresOuverts by rememberSaveable { mutableStateOf(false) }
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
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        MissaChampTexte(
                            valeur = etat.requete,
                            onValeur = onRequete,
                            libelle = stringResource(R.string.clients_recherche),
                            icone = Iv.Search,
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedButton(onClick = { filtresOuverts = true }, modifier = Modifier.heightIn(min = 52.dp)) {
                            val actifs = etat.avance.actifs
                            Text(stringResource(R.string.cli_filtres_titre) + if (actifs > 0) " ($actifs)" else "")
                        }
                    }
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
                        ClientSwipeRow(
                            ligne = ligne,
                            devise = etat.devise,
                            onClick = { onOuvrirClient(ligne.client.id) },
                            onAppeler = { onAppeler(ligne.client.telephone) },
                            onWhatsApp = { onWhatsApp(ligne.client.telephone) },
                        )
                    }
                }
            }
        }
    }
    if (filtresOuverts) {
        ClientFilterSheet(etat.avance, onAvance, onClose = { filtresOuverts = false })
    }
}
