package com.missa.b360.ui.clients.csvimport

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.missa.b360.R
import com.missa.b360.core.domain.model.ClientImportError
import com.missa.b360.core.domain.model.ClientImportIssue
import com.missa.b360.ui.clients.components.ClientTopBar
import com.missa.b360.ui.clients.components.RisqueCouleurs
import com.missa.b360.ui.theme.MissaBorder
import com.missa.b360.ui.theme.MissaCanvas
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted

private const val ERREURS_AFFICHEES = 200

private fun ClientImportError.libelle(): Int = when (this) {
    ClientImportError.NOM_MANQUANT -> R.string.cli_import_err_nom_manquant
    ClientImportError.NOM_INVALIDE -> R.string.cli_import_err_nom_invalide
    ClientImportError.TELEPHONE_MANQUANT -> R.string.cli_import_err_tel_manquant
    ClientImportError.TELEPHONE_INVALIDE -> R.string.cli_import_err_tel_invalide
    ClientImportError.EMAIL_INVALIDE -> R.string.cli_import_err_email
    ClientImportError.ADRESSE_INVALIDE -> R.string.cli_import_err_adresse
    ClientImportError.DOUBLON_FICHIER -> R.string.cli_import_err_doublon
    ClientImportError.TROP_DE_LIGNES -> R.string.cli_import_err_trop
}

/** Route `clients/import` : choix du fichier, aperçu avec rapport d'erreurs, confirmation, bilan. */
@Composable
fun ClientImportScreen(
    onBack: () -> Unit,
    onVoirListe: () -> Unit,
    viewModel: ClientImportViewModel = hiltViewModel(),
) {
    val etat by viewModel.etat.collectAsState()
    val lanceur = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.choisirFichier(uri)
    }
    val types = arrayOf("text/*", "application/csv", "application/vnd.ms-excel", "application/octet-stream")

    Scaffold(
        containerColor = MissaCanvas,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { ClientTopBar(titre = stringResource(R.string.cli_import_titre), onBack = onBack) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 140.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            when (etat.etape) {
                ImportEtape.CHOIX -> {
                    item {
                        Carte {
                            Text(stringResource(R.string.cli_import_intro), color = MissaInk, fontSize = 15.sp)
                            Text(stringResource(R.string.cli_import_colonnes), color = MissaMuted, fontSize = 14.sp)
                            if (etat.lectureImpossible) {
                                Text(stringResource(R.string.cli_import_lecture_impossible), color = RisqueCouleurs.Eleve, fontWeight = FontWeight.Medium)
                            }
                            Button(onClick = { lanceur.launch(types) }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                                Text(stringResource(R.string.cli_import_choisir))
                            }
                        }
                    }
                }
                ImportEtape.ANALYSE -> item { Attente(stringResource(R.string.cli_import_analyse), null) }
                ImportEtape.IMPORT -> item {
                    Attente(
                        stringResource(R.string.cli_import_en_cours, etat.fait, etat.total),
                        if (etat.total > 0) etat.fait.toFloat() / etat.total else null,
                    )
                }
                ImportEtape.APERCU -> {
                    item {
                        Carte {
                            Text(stringResource(R.string.cli_import_lignes_lues, etat.apercu.lignesLues), color = MissaMuted, fontSize = 14.sp)
                            Text(
                                stringResource(R.string.cli_import_valides, etat.apercu.valides.size),
                                color = RisqueCouleurs.Normal, fontWeight = FontWeight.Bold, fontSize = 17.sp,
                            )
                            if (etat.apercu.erreurs.isNotEmpty()) {
                                Text(
                                    stringResource(R.string.cli_import_erreurs, etat.apercu.erreurs.size),
                                    color = RisqueCouleurs.Eleve, fontWeight = FontWeight.Bold, fontSize = 17.sp,
                                )
                                Text(stringResource(R.string.cli_import_partiel), color = MissaMuted, fontSize = 14.sp)
                            }
                            Button(
                                onClick = viewModel::confirmer,
                                enabled = etat.apercu.valides.isNotEmpty(),
                                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                            ) { Text(stringResource(R.string.cli_import_confirmer, etat.apercu.valides.size)) }
                            OutlinedButton(onClick = { lanceur.launch(types) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                                Text(stringResource(R.string.cli_import_autre_fichier))
                            }
                        }
                    }
                    val erreurs = etat.apercu.erreurs
                    items(minOf(erreurs.size, ERREURS_AFFICHEES)) { index -> LigneErreur(erreurs[index]) }
                    if (erreurs.size > ERREURS_AFFICHEES) {
                        item { Text(stringResource(R.string.cli_import_erreurs_masquees, erreurs.size - ERREURS_AFFICHEES), color = MissaMuted) }
                    }
                }
                ImportEtape.TERMINE -> {
                    val rapport = etat.rapport
                    item {
                        Carte {
                            Text(stringResource(R.string.cli_import_termine), color = MissaInk, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            if (rapport != null) {
                                Text(stringResource(R.string.cli_import_crees, rapport.crees), color = RisqueCouleurs.Normal, fontWeight = FontWeight.SemiBold)
                                Text(stringResource(R.string.cli_import_doublons, rapport.doublons), color = MissaInk)
                                Text(stringResource(R.string.cli_import_echecs, rapport.echecs), color = MissaInk)
                                if (rapport.interrompu) {
                                    Text(stringResource(R.string.cli_import_interrompu), color = RisqueCouleurs.Eleve, fontWeight = FontWeight.Medium)
                                }
                            }
                            Button(onClick = onVoirListe, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                                Text(stringResource(R.string.cli_import_voir_liste))
                            }
                            OutlinedButton(onClick = viewModel::recommencer, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                                Text(stringResource(R.string.cli_import_autre_fichier))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Carte(contenu: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, MissaBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { contenu() }
    }
}

@Composable
private fun Attente(message: String, avancement: Float?) {
    Carte {
        Text(message, color = MissaInk, fontWeight = FontWeight.Medium)
        if (avancement == null) {
            CircularProgressIndicator()
        } else {
            LinearProgressIndicator(progress = { avancement }, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun LigneErreur(erreur: ClientImportIssue) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, MissaBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(10.dp)) {
            Text(
                stringResource(R.string.cli_import_ligne_numero, erreur.ligne) + " · " + stringResource(erreur.erreur.libelle()),
                color = RisqueCouleurs.Eleve, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
            )
            if (erreur.extrait.isNotBlank()) Text(erreur.extrait, color = MissaMuted, fontSize = 13.sp)
        }
    }
}
