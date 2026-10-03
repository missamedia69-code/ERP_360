package com.missa.b360.ui.clients.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.missa.b360.ui.clients.components.BoutonClient
import com.missa.b360.ui.clients.components.ClientCouleurs
import com.missa.b360.ui.clients.components.ClientRecherche
import com.missa.b360.ui.clients.components.ClientEtatVide
import com.missa.b360.ui.theme.MissaInk
import androidx.compose.foundation.layout.heightIn
import com.missa.b360.ui.components.BoutonContourMissa as OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import com.missa.b360.core.domain.model.ClientAdvancedFilter
import com.missa.b360.ui.clients.components.appeler
import com.missa.b360.ui.clients.components.ouvrirWhatsApp
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.missa.b360.ui.clients.components.BoutonClientPlein
import com.missa.b360.ui.theme.MissaMuted
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
        containerColor = ClientCouleurs.Fond,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            ClientTopBar(titre = stringResource(R.string.clients_flow_list_title), onBack = onBack, titreCentre = false) {
                IconButton(onClick = onRelances) {
                    Icon(painterResource(Iv.Notifications), stringResource(R.string.cli_ouvrir_relances), tint = MissaInk)
                }
                IconButton(onClick = onImporter) {
                    Icon(painterResource(Iv.UploadSimple), stringResource(R.string.clients_flow_import), tint = MissaInk)
                }
            }
        },
        bottomBar = {
            // Bouton « Nouveau client » pleine largeur ; la marge basse laisse la place à la barre de modules flottante.
            Column(Modifier.fillMaxWidth().background(Color.White)) {
                HorizontalDivider(color = ClientCouleurs.Trait)
                BoutonClientPlein(
                    onClick = onNouveau,
                    modifier = Modifier.fillMaxWidth().padding(start = 13.dp, end = 13.dp, top = 9.dp).heightIn(min = 48.dp),
                ) {
                    Icon(painterResource(Iv.Add), contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.clients_nouveau), fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                }
                Spacer(Modifier.height(64.dp))
            }
        },
    ) { padding ->
        when {
            etat.erreur -> EtatErreur(onReessayer = onReessayer, modifier = Modifier.padding(padding))
            etat.chargement -> EtatChargement(Modifier.padding(padding))
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 13.dp, end = 13.dp, top = 10.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item { ClientListSummary(etat) }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        ClientRecherche(
                            valeur = etat.requete,
                            onValeur = onRequete,
                            indication = stringResource(R.string.clients_recherche),
                            modifier = Modifier.weight(1f),
                        )
                        val actifs = etat.avance.actifs
                        val titreFiltres = stringResource(R.string.cli_filtres_titre) + if (actifs > 0) " ($actifs)" else ""
                        BoutonClient(
                            onClick = { filtresOuverts = true },
                            modifier = Modifier.size(48.dp).semantics { contentDescription = titreFiltres },
                            plein = actifs > 0,
                        ) {
                            Icon(
                                painterResource(Iv.Sliders), contentDescription = null,
                                tint = if (actifs > 0) Color.White else ClientCouleurs.Violet, modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
                item { ClientFilterChips(etat, onFiltre) }
                item { ClientListeEntete(etat, onTri) }
                when {
                    etat.aucunClient -> item {
                        ClientEtatVide(
                            icone = Iv.Group,
                            titre = stringResource(R.string.cli_liste_vide_titre),
                            description = stringResource(R.string.cli_liste_vide_desc),
                        )
                    }
                    etat.aucunResultat -> item {
                        ClientEtatVide(
                            icone = Iv.Search,
                            titre = stringResource(R.string.cli_aucun_resultat),
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
                if (etat.lignes.isNotEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.cli_liste_astuce),
                            color = MissaMuted, fontSize = 10.sp, textAlign = TextAlign.Start,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
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
