package com.missa.b360.ui.onboarding

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.missa.b360.R
import com.missa.b360.core.util.Fuseaux
import com.missa.b360.ui.components.MissaOption
import com.missa.b360.ui.components.MissaSelecteurLigne
import com.missa.b360.ui.theme.BrandBlue
import com.missa.b360.ui.theme.MissaBorder
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted
import com.missa.b360.ui.theme.MissaSurface
import com.missa.b360.ui.theme.Red40
import kotlinx.coroutines.delay

/**
 * Écran — Configuration initiale : fuseau horaire, format de date, style des
 * nombres, rétention du journal et sauvegardes automatiques. Chaque choix est
 * appliqué immédiatement à toute l'application (FormatPrefs) puis conservé.
 *
 * La langue se choisit désormais sur l'écran de bienvenue (drapeaux).
 * Tous les choix utilisent le sélecteur standard [MissaSelecteurLigne].
 */
@Composable
internal fun OnbConfigurationStep(viewModel: OnboardingViewModel) {
    OnbScaffold(
        titreRes = R.string.obn_config_titre,
        sousTitreRes = R.string.obn_config_sous,
        viewModel = viewModel,
        onRetour = viewModel::precedent,
    ) {
        // Cartes autonomes, espacées : chaque sélecteur a son propre cadre
        // depuis qu'il est encadré, empiler le tout dans une carte unique les
        // faisait se toucher.
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OnbFuseauLigne(
                selection = viewModel.fuseau,
                onSelect = viewModel::appliquerFuseau,
                enabled = !viewModel.enregistrementEnCours,
            )
            val formatsJours = listOf(
                MissaOption(cle = "dd/MM/yyyy", titre = "31/12/2025", badge = "JJ/MM/AAAA"),
                MissaOption(cle = "MM/dd/yyyy", titre = "12/31/2025", badge = "MM/JJ/AAAA"),
                MissaOption(cle = "dd.MM.yyyy", titre = "31.12.2025", badge = "JJ.MM.AAAA"),
                MissaOption(cle = "yyyy-MM-dd", titre = "2025-12-31", badge = "AAAA-MM-JJ"),
            )
            MissaSelecteurLigne(
                label = stringResource(R.string.obn_format_date),
                options = formatsJours,
                selectionCle = viewModel.formatJours,
                onSelection = viewModel::appliquerFormatDate,
                enabled = !viewModel.enregistrementEnCours,
            )
            val formatsNombres = listOf(
                MissaOption(
                    cle = "fr",
                    titre = "1 234 567,89",
                    sousTitre = stringResource(R.string.obn_format_nombres_fr),
                ),
                MissaOption(
                    cle = "en",
                    titre = "1,234,567.89",
                    sousTitre = stringResource(R.string.obn_format_nombres_en),
                ),
            )
            MissaSelecteurLigne(
                label = stringResource(R.string.obn_format_nombres),
                options = formatsNombres,
                selectionCle = viewModel.formatNombres,
                onSelection = viewModel::appliquerFormatNombres,
                enabled = !viewModel.enregistrementEnCours,
            )
            val retentions = listOf(
                MissaOption(
                    cle = "30",
                    titre = stringResource(R.string.obn_retention_30),
                    sousTitre = stringResource(R.string.obn_retention_30_sous),
                ),
                MissaOption(
                    cle = "90",
                    titre = stringResource(R.string.obn_retention_90),
                    sousTitre = stringResource(R.string.obn_retention_90_sous),
                ),
                MissaOption(
                    cle = "365",
                    titre = stringResource(R.string.obn_retention_365),
                    sousTitre = stringResource(R.string.obn_retention_365_sous),
                ),
            )
            MissaSelecteurLigne(
                label = stringResource(R.string.obn_retention),
                options = retentions,
                selectionCle = viewModel.retentionJournal.toString(),
                onSelection = { cle ->
                    cle.toIntOrNull()?.let(viewModel::appliquerRetentionJournal)
                },
                enabled = !viewModel.enregistrementEnCours,
            )
            // Même cadre que les sélecteurs voisins : une ligne nue au milieu
            // de cartes encadrées se lit comme un oubli.
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MissaSurface,
                border = BorderStroke(1.dp, MissaBorder),
            ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 13.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.obn_sauvegardes),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MissaInk,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.obn_sauvegardes_sous),
                        fontSize = 11.5.sp,
                        color = MissaMuted,
                    )
                }
                Switch(
                    checked = viewModel.sauvegardesActives,
                    onCheckedChange = { viewModel.appliquerSauvegardes(it) },
                    enabled = !viewModel.enregistrementEnCours,
                    colors = androidx.compose.material3.SwitchDefaults.colors(
                        checkedTrackColor = BrandBlue,
                    ),
                )
            }
            }
        }
        OnbRestaurationCarte(viewModel = viewModel)
    }
}

/**
 * Ligne « Fuseau horaire » : le catalogue complet (UTC-12:00 → UTC+14:00) est
 * présenté par le sélecteur standard, avec l'heure locale de chaque fuseau en
 * pastille et l'heure du fuseau retenu rafraîchie sous la valeur courante.
 */
@Composable
private fun OnbFuseauLigne(
    selection: String,
    onSelect: (String) -> Unit,
    enabled: Boolean,
) {
    val fuseau = remember(selection) { Fuseaux.resoudre(selection) }
    val heure by produceState(Fuseaux.heureCourante(selection), selection) {
        while (true) {
            value = Fuseaux.heureCourante(selection)
            delay(20_000)
        }
    }
    val options = Fuseaux.catalogue.map { entree ->
        val nom = stringResource(entree.nomRes)
        MissaOption(
            cle = entree.id,
            titre = if (entree.code.isBlank()) nom else "$nom · ${entree.code}",
            sousTitre = entree.villes,
            badge = entree.libelleUtc,
            badgeSecondaire = Fuseaux.heureCourante(entree.id),
        )
    }
    MissaSelecteurLigne(
        label = stringResource(R.string.obn_fuseau),
        options = options,
        selectionCle = fuseau.id,
        onSelection = onSelect,
        enabled = enabled,
        titreDialogue = stringResource(R.string.fuseau_titre),
        indiceRecherche = stringResource(R.string.fuseau_recherche),
        detail = "${stringResource(R.string.fuseau_heure_locale)} $heure" +
            (if (fuseau.code.isNotBlank()) " · ${fuseau.code}" else ""),
    )
}

/**
 * Carte « Restaurer une sauvegarde » : reprise d'un appareil précédent ou d'une
 * réinstallation. Le fichier `.db` est choisi via le sélecteur système, confirmé
 * explicitement (les données actuelles sont remplacées), puis l'application
 * redémarre pour rouvrir la base restaurée.
 */
@Composable
private fun OnbRestaurationCarte(viewModel: OnboardingViewModel) {
    val contexte = LocalContext.current
    var fichierChoisi by remember { mutableStateOf<Uri?>(null) }
    val selecteur = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri -> if (uri != null) fichierChoisi = uri }

    // Redémarrage une fois la base remplacée : Room pointe encore sur l'ancien fichier.
    LaunchedEffect(viewModel.restaurationReussie) {
        if (viewModel.restaurationReussie) {
            delay(1_200)
            redemarrerApplication(contexte)
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MissaBorder),
        colors = CardDefaults.cardColors(containerColor = MissaSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Restore,
                    contentDescription = null,
                    tint = BrandBlue,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.obn_restaurer_titre),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MissaInk,
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.obn_restaurer_sous),
                fontSize = 11.5.sp,
                color = MissaMuted,
            )
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = { selecteur.launch(arrayOf("application/octet-stream", "*/*")) },
                enabled = !viewModel.restaurationEnCours &&
                    !viewModel.restaurationReussie &&
                    !viewModel.enregistrementEnCours,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(
                        if (viewModel.restaurationEnCours) {
                            R.string.obn_restaurer_en_cours
                        } else {
                            R.string.obn_restaurer_btn
                        },
                    ),
                    fontSize = 12.5.sp,
                )
            }
            if (viewModel.restaurationEnCours) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = BrandBlue,
                )
            }
            viewModel.restaurationMessageRes?.let { messageRes ->
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(messageRes),
                    fontSize = 11.5.sp,
                    color = if (viewModel.restaurationReussie) BrandBlue else Red40,
                )
            }
        }
    }

    fichierChoisi?.let { uri ->
        AlertDialog(
            onDismissRequest = { fichierChoisi = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        fichierChoisi = null
                        viewModel.restaurerSauvegarde(uri)
                    },
                ) {
                    Text(stringResource(R.string.obn_restaurer_confirmer), color = Red40)
                }
            },
            dismissButton = {
                TextButton(onClick = { fichierChoisi = null }) {
                    Text(stringResource(R.string.ops_cancel))
                }
            },
            title = {
                Text(
                    text = stringResource(R.string.obn_restaurer_confirme_titre),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MissaInk,
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.obn_restaurer_confirme_texte),
                    fontSize = 12.5.sp,
                    color = MissaMuted,
                )
            },
        )
    }
}

/** Relance l'application après une restauration (nouvelle base de données). */
private fun redemarrerApplication(contexte: Context) {
    val intention = contexte.packageManager.getLaunchIntentForPackage(contexte.packageName)
    if (intention != null) {
        intention.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        contexte.startActivity(intention)
    }
    Runtime.getRuntime().exit(0)
}
