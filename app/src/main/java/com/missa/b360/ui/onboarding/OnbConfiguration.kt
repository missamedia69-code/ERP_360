package com.missa.b360.ui.onboarding

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.missa.b360.R
import com.missa.b360.core.util.FuseauHoraire
import com.missa.b360.core.util.Fuseaux
import com.missa.b360.ui.theme.BrandBlue
import com.missa.b360.ui.theme.MissaBorder
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted
import com.missa.b360.ui.theme.MissaSoftBlue
import com.missa.b360.ui.theme.MissaSurface
import com.missa.b360.ui.theme.Red40
import java.text.Normalizer
import kotlinx.coroutines.delay

/**
 * Écran — Configuration initiale : langue, fuseau horaire, format de date,
 * style des nombres et sauvegardes automatiques. Chaque choix est appliqué
 * immédiatement à toute l'application (FormatPrefs / locale) puis conservé.
 */
@Composable
internal fun OnbConfigurationStep(viewModel: OnboardingViewModel) {
    OnbScaffold(
        titreRes = R.string.obn_config_titre,
        sousTitreRes = R.string.obn_config_sous,
        viewModel = viewModel,
        onRetour = viewModel::precedent,
    ) {
        OnbLanguesDrapeaux(
            selection = viewModel.langue,
            onSelect = viewModel::appliquerLangue,
            enabled = !viewModel.enregistrementEnCours,
        )
        Card(
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MissaBorder),
            colors = CardDefaults.cardColors(containerColor = MissaSurface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                OnbFuseauLigne(
                    selection = viewModel.fuseau,
                    onSelect = viewModel::appliquerFuseau,
                    enabled = !viewModel.enregistrementEnCours,
                )
                HorizontalDivider(color = MissaBorder)
                val formatsJours = listOf(
                    "dd/MM/yyyy" to "31/12/2025",
                    "MM/dd/yyyy" to "12/31/2025",
                    "dd.MM.yyyy" to "31.12.2025",
                    "yyyy-MM-dd" to "2025-12-31",
                )
                OnbConfigLigne(
                    labelRes = R.string.obn_format_date,
                    options = formatsJours,
                    selectedKey = viewModel.formatJours,
                    optionKey = { it.first },
                    optionLabel = { it.second },
                    onPick = { viewModel.appliquerFormatDate(it.first) },
                    enabled = !viewModel.enregistrementEnCours,
                )
                HorizontalDivider(color = MissaBorder)
                val formatsNombres = listOf(
                    "fr" to "1 234 567,89",
                    "en" to "1,234,567.89",
                )
                OnbConfigLigne(
                    labelRes = R.string.obn_format_nombres,
                    options = formatsNombres,
                    selectedKey = viewModel.formatNombres,
                    optionKey = { it.first },
                    optionLabel = { it.second },
                    onPick = { viewModel.appliquerFormatNombres(it.first) },
                    enabled = !viewModel.enregistrementEnCours,
                )
                HorizontalDivider(color = MissaBorder)
                val retentions = listOf(
                    30 to stringResource(R.string.obn_retention_30),
                    90 to stringResource(R.string.obn_retention_90),
                    365 to stringResource(R.string.obn_retention_365),
                )
                OnbConfigLigne(
                    labelRes = R.string.obn_retention,
                    options = retentions,
                    selectedKey = viewModel.retentionJournal.toString(),
                    optionKey = { it.first.toString() },
                    optionLabel = { it.second },
                    onPick = { viewModel.appliquerRetentionJournal(it.first) },
                    enabled = !viewModel.enregistrementEnCours,
                )
                HorizontalDivider(color = MissaBorder)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 13.dp),
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

/** Langues proposées : code de locale + drapeau + libellé (accessibilité). */
private val OnbLangues = listOf(
    Triple("fr", "🇫🇷", R.string.langue_fr),
    Triple("en", "🇬🇧", R.string.langue_en),
    Triple("es", "🇪🇸", R.string.langue_es),
    Triple("ar", "🇸🇦", R.string.langue_ar),
    Triple("zh", "🇨🇳", R.string.langue_zh),
)

/**
 * Sélecteur de langue horizontal : une rangée de drapeaux cliquables placée
 * au-dessus de la carte de configuration. Le drapeau actif est mis en avant
 * (fond bleu clair + bordure de marque) et la langue s'applique immédiatement.
 */
@Composable
private fun OnbLanguesDrapeaux(
    selection: String,
    onSelect: (String) -> Unit,
    enabled: Boolean,
) {
    val courant = selection.substringBefore('-').lowercase()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OnbLangues.forEach { (code, drapeau, labelRes) ->
            val actif = courant == code
            val libelle = stringResource(labelRes)
            Box(
                modifier = Modifier
                    .size(width = 54.dp, height = 44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (actif) MissaSoftBlue else MissaSurface)
                    .border(
                        width = if (actif) 2.dp else 1.dp,
                        color = if (actif) BrandBlue else MissaBorder,
                        shape = RoundedCornerShape(12.dp),
                    )
                    .then(
                        if (enabled) {
                            Modifier.clickable(
                                enabled = true,
                                onClickLabel = libelle,
                                role = Role.RadioButton,
                            ) { onSelect(code) }
                        } else {
                            Modifier
                        },
                    )
                    .semantics {
                        contentDescription = libelle
                        selected = actif
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(text = drapeau, fontSize = 22.sp)
            }
        }
    }
}

/**
 * Ligne « Fuseau horaire » : décalage UTC, code usuel, libellé traduit et heure
 * courante recalculée en direct. Le clic ouvre le sélecteur complet du catalogue.
 */
@Composable
private fun OnbFuseauLigne(
    selection: String,
    onSelect: (String) -> Unit,
    enabled: Boolean,
) {
    var ouvert by remember { mutableStateOf(false) }
    val fuseau = remember(selection) { Fuseaux.resoudre(selection) }
    val heure by produceState(Fuseaux.heureCourante(selection), selection) {
        while (true) {
            value = Fuseaux.heureCourante(selection)
            delay(20_000)
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 13.dp)
            .then(if (enabled) Modifier.clickable { ouvert = true } else Modifier),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.obn_fuseau),
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = MissaInk,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${fuseau.libelleUtc} · ${stringResource(fuseau.nomRes)}",
                fontSize = 13.5.sp,
                color = MissaMuted,
            )
            Spacer(Modifier.height(1.dp))
            Text(
                text = "${stringResource(R.string.fuseau_heure_locale)} $heure" +
                    (if (fuseau.code.isNotBlank()) " · ${fuseau.code}" else ""),
                fontSize = 11.5.sp,
                color = BrandBlue,
            )
        }
        Icon(
            imageVector = Icons.Outlined.ArrowDropDown,
            contentDescription = null,
            tint = MissaMuted,
            modifier = Modifier.size(18.dp),
        )
    }
    if (ouvert) {
        OnbFuseauDialogue(
            selection = fuseau.id,
            onSelect = {
                onSelect(it)
                ouvert = false
            },
            onFermer = { ouvert = false },
        )
    }
}

/**
 * Sélecteur complet des fuseaux (UTC-12:00 → UTC+14:00) : recherche par ville,
 * par code (PST, CET, EAT…) ou par décalage, liste défilante positionnée sur le
 * fuseau courant, et heure locale affichée pour chaque entrée.
 */
@Composable
private fun OnbFuseauDialogue(
    selection: String,
    onSelect: (String) -> Unit,
    onFermer: () -> Unit,
) {
    var requete by remember { mutableStateOf("") }
    val entrees = Fuseaux.catalogue.map { it to stringResource(it.nomRes) }
    val filtre = normaliserRecherche(requete)
    val visibles = if (filtre.isBlank()) {
        entrees
    } else {
        entrees.filter { (fuseau, nom) ->
            normaliserRecherche("${fuseau.libelleUtc} ${fuseau.code} $nom ${fuseau.villes}")
                .contains(filtre)
        }
    }
    val etatListe = rememberLazyListState()
    val indexCourant = visibles.indexOfFirst { it.first.id == selection }
    LaunchedEffect(Unit) {
        if (indexCourant > 1) etatListe.scrollToItem(indexCourant - 1)
    }
    AlertDialog(
        onDismissRequest = onFermer,
        confirmButton = {
            TextButton(onClick = onFermer) { Text(stringResource(R.string.ob_fermer)) }
        },
        title = {
            Text(
                text = stringResource(R.string.fuseau_titre),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MissaInk,
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = requete,
                    onValueChange = { requete = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.fuseau_recherche), fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                if (visibles.isEmpty()) {
                    Text(
                        text = stringResource(R.string.fuseau_aucun),
                        fontSize = 12.5.sp,
                        color = MissaMuted,
                        modifier = Modifier.padding(vertical = 12.dp),
                    )
                } else {
                    LazyColumn(
                        state = etatListe,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 380.dp),
                    ) {
                        items(visibles, key = { it.first.id }) { (fuseau, nom) ->
                            OnbFuseauItem(
                                fuseau = fuseau,
                                nom = nom,
                                actif = fuseau.id == selection,
                                onClick = { onSelect(fuseau.id) },
                            )
                        }
                    }
                }
            }
        },
    )
}

/** Une entrée du catalogue : décalage, code, libellé traduit et villes repères. */
@Composable
private fun OnbFuseauItem(
    fuseau: FuseauHoraire,
    nom: String,
    actif: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (actif) MissaSoftBlue else MissaSurface)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.width(74.dp)) {
            Text(
                text = fuseau.libelleUtc,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (actif) BrandBlue else MissaInk,
            )
            Text(
                text = Fuseaux.heureCourante(fuseau.id),
                fontSize = 11.sp,
                color = MissaMuted,
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (fuseau.code.isBlank()) nom else "$nom · ${fuseau.code}",
                fontSize = 12.5.sp,
                fontWeight = if (actif) FontWeight.SemiBold else FontWeight.Normal,
                color = MissaInk,
            )
            Text(
                text = fuseau.villes,
                fontSize = 11.sp,
                color = MissaMuted,
            )
        }
        if (actif) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                tint = BrandBlue,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/** Recherche insensible à la casse et aux accents (Douala = douala = doualá). */
private fun normaliserRecherche(texte: String): String =
    Normalizer.normalize(texte.lowercase(), Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .trim()

/**
 * Ligne de réglage de la maquette : libellé au-dessus de la valeur courante,
 * chevron de droite et menu déroulant ancré sur la ligne (DropdownMenu
 * standard, ancré sur le conteneur de la ligne).
 */
@Composable
private fun <T> OnbConfigLigne(
    labelRes: Int,
    options: List<T>,
    selectedKey: String,
    optionKey: (T) -> String,
    optionLabel: ((T) -> String)? = null,
    onPick: (T) -> Unit,
    enabled: Boolean,
) {
    var ouvert by remember { mutableStateOf(false) }
    val choisie = options.firstOrNull { optionKey(it) == selectedKey }
    val libelle = if (choisie != null) (optionLabel?.invoke(choisie) ?: optionKey(choisie)) else selectedKey
    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 13.dp)
                .then(if (enabled) Modifier.clickable { ouvert = !ouvert } else Modifier),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(labelRes),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MissaInk,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = libelle,
                    fontSize = 13.5.sp,
                    color = MissaMuted,
                )
            }
            Icon(
                imageVector = Icons.Outlined.ArrowDropDown,
                contentDescription = null,
                tint = MissaMuted,
                modifier = Modifier.size(18.dp),
            )
        }
        DropdownMenu(
            expanded = ouvert,
            onDismissRequest = { ouvert = false },
        ) {
            for (option in options) {
                DropdownMenuItem(
                    text = {
                        Text(
                            (optionLabel?.invoke(option) ?: optionKey(option)),
                            fontSize = 13.sp,
                        )
                    },
                    onClick = {
                        onPick(option)
                        ouvert = false
                    },
                )
            }
        }
    }
}
