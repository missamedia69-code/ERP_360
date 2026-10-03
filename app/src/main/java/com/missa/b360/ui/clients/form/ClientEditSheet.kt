package com.missa.b360.ui.clients.form

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.missa.b360.R
import com.missa.b360.core.data.entity.ClientType
import com.missa.b360.core.domain.usecase.ClientLifecycleRules
import com.missa.b360.ui.clients.components.BoutonClientPlein as Button
import com.missa.b360.ui.clients.components.BoutonClient
import androidx.compose.material3.IconButton
import com.missa.b360.ui.clients.components.ClientCarte
import com.missa.b360.ui.clients.components.ClientCouleurs
import com.missa.b360.ui.clients.components.ClientNoticeEffect
import com.missa.b360.ui.clients.components.ClientTopBar
import com.missa.b360.ui.clients.components.EtatChargement
import com.missa.b360.ui.clients.components.EtatErreur
import com.missa.b360.ui.icons.Iv
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted

/**
 * Route `clients/{id}/edit` : formulaire en cinq étapes (identité, fiscalité, conditions, contacts, notes) avec une
 * carte de progression ; le brouillon survit à la rotation et au redémarrage du processus. Aucune étape n'est bloquante :
 * l'enregistrement a lieu à la dernière étape et renvoie à la première étape en erreur.
 */
@Composable
fun ClientEditSheet(
    onClose: () -> Unit,
    onSauve: () -> Unit,
    viewModel: ClientEditViewModel = hiltViewModel(),
) {
    val etat by viewModel.etat.collectAsState()
    val hote = remember { SnackbarHostState() }
    ClientNoticeEffect(etat.notice, hote, viewModel::noticeLue)
    LaunchedEffect(etat.sauve) { if (etat.sauve) onSauve() }
    val sections = ClientSection.entries
    var etape by rememberSaveable { mutableStateOf(0) }
    // Une validation en échec renvoie à la première étape qui contient une erreur.
    LaunchedEffect(etat.erreurs) {
        etat.erreurs.minOfOrNull { it.section.ordinal }?.let { etape = it }
    }
    val abandonner = {
        viewModel.abandonner()
        onClose()
    }
    BackHandler(enabled = etape > 0) { etape -= 1 }
    val pret = !etat.erreur && !etat.chargement && !etat.introuvable
    val derniere = etape == sections.lastIndex
    Scaffold(
        containerColor = ClientCouleurs.Fond,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(hote) },
        topBar = {
            ClientTopBar(titre = stringResource(R.string.clients_modifier), onBack = abandonner) {
                IconButton(onClick = abandonner) {
                    Icon(painterResource(Iv.Close), stringResource(R.string.cli_fermer), tint = MissaInk, modifier = Modifier.size(18.dp))
                }
            }
        },
        bottomBar = {
            if (pret) {
                Column(Modifier.fillMaxWidth().background(Color.White).navigationBarsPadding()) {
                    HorizontalDivider(color = ClientCouleurs.Trait)
                    Row(Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BoutonClient(
                            onClick = { if (etape == 0) abandonner() else etape -= 1 },
                            modifier = Modifier.weight(0.8f),
                            plein = false,
                        ) {
                            if (etape > 0) Icon(painterResource(Iv.ArrowBack), contentDescription = null, tint = ClientCouleurs.Violet, modifier = Modifier.size(15.dp))
                            Text(stringResource(if (etape == 0) R.string.cli_annuler else R.string.cli_precedent), color = MissaInk)
                        }
                        Button(
                            onClick = { if (derniere) viewModel.enregistrer() else etape += 1 },
                            enabled = !etat.enCours,
                            modifier = Modifier.weight(1.2f).heightIn(min = 48.dp),
                        ) {
                            Text(stringResource(if (derniere) R.string.cli_enregistrer else R.string.cli_continuer))
                            Icon(painterResource(if (derniere) Iv.Check else Iv.ChevronRight), contentDescription = null, modifier = Modifier.size(15.dp))
                        }
                    }
                }
            }
        },
    ) { padding ->
        when {
            etat.erreur -> EtatErreur(onReessayer = viewModel::charger, modifier = Modifier.padding(padding))
            etat.chargement -> EtatChargement(Modifier.padding(padding))
            etat.introuvable -> Text(stringResource(R.string.cli_fiche_introuvable), modifier = Modifier.padding(padding).padding(16.dp))
            else -> {
                val section = sections[etape.coerceIn(0, sections.lastIndex)]
                val enErreur = etat.erreurs.any { it.section == section }
                val aCompleter = stringResource(R.string.obn_section_a_completer)
                val invalide = stringResource(R.string.form_valeur_invalide)
                val sousTitreUn = stringResource(R.string.cli_form_sous_titre_1)
                val sousTitreN = stringResource(R.string.cli_form_sous_titre_n)
                Column(
                    Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                        .padding(start = 13.dp, end = 13.dp, top = 10.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    CarteProgression(
                        etape = etape,
                        total = sections.size,
                        titre = stringResource(section.titre()),
                        etapesEnErreur = sections.indices.filter { i -> etat.erreurs.any { it.section == sections[i] } },
                        onEtape = { etape = it },
                    )
                    ClientCarte {
                        Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                                Box(
                                    Modifier.size(29.dp).clip(RoundedCornerShape(10.dp)).background(ClientCouleurs.Violet),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text((etape + 1).toString(), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                                }
                                Column(Modifier.weight(1f)) {
                                    Text(stringResource(section.titre()), color = MissaInk, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                                    val sousTitre = when {
                                        enErreur -> invalide
                                        section.essentielManque(etat.draft) -> aCompleter
                                        etape == 0 -> sousTitreUn
                                        else -> sousTitreN
                                    }
                                    if (sousTitre != null) {
                                        Text(
                                            sousTitre, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                            color = when (sousTitre) {
                                                invalide -> com.missa.b360.ui.clients.components.RisqueCouleurs.Eleve
                                                aCompleter -> ClientCouleurs.Alerte
                                                else -> MissaMuted
                                            },
                                        )
                                    }
                                }
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                when (section) {
                                    ClientSection.IDENTITE -> SectionIdentite(etat, viewModel::modifier)
                                    ClientSection.FISCALITE -> SectionFiscalite(etat, viewModel::modifier)
                                    ClientSection.CONDITIONS -> SectionConditions(etat, viewModel::modifier)
                                    ClientSection.CONTACTS -> SectionContacts(etat, viewModel::modifier)
                                    ClientSection.NOTES -> SectionNotes(etat, viewModel::modifier)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Carte de progression : titre de l'étape, « Étape n sur N », barre violette et pastilles numérotées ou cochées. */
@Composable
private fun CarteProgression(
    etape: Int,
    total: Int,
    titre: String,
    etapesEnErreur: List<Int>,
    onEtape: (Int) -> Unit,
) {
    ClientCarte {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(titre, color = MissaInk, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, modifier = Modifier.weight(1f))
                Text(stringResource(R.string.cli_etape_n_sur, etape + 1, total), color = ClientCouleurs.Violet, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
            }
            Box(Modifier.fillMaxWidth().padding(top = 4.dp).height(5.dp).clip(CircleShape).background(Color(0xFFECEEF4))) {
                Box(
                    Modifier
                        .fillMaxWidth((etape + 1).toFloat() / total)
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(Brush.horizontalGradient(listOf(ClientCouleurs.Violet, Color(0xFFA679F2)))),
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                for (i in 0 until total) {
                    val courant = i == etape
                    val fait = i < etape
                    val erreur = i in etapesEnErreur
                    Box(
                        Modifier
                            .size(width = 26.dp, height = 48.dp)
                            .clickable(role = Role.Tab) { onEtape(i) },
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Box(
                            Modifier
                                .size(20.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(
                                    when {
                                        erreur -> Color(0xFFFDE8E8)
                                        courant || fait -> ClientCouleurs.Violet
                                        else -> Color(0xFFEDF0F5)
                                    },
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (fait && !erreur) {
                                Icon(painterResource(Iv.Check), contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                            } else {
                                Text(
                                    (i + 1).toString(), fontSize = 9.sp, fontWeight = FontWeight.ExtraBold,
                                    color = when {
                                        erreur -> com.missa.b360.ui.clients.components.RisqueCouleurs.Eleve
                                        courant -> Color.White
                                        else -> Color(0xFF7C879C)
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Indication « À compléter » : l'essentiel de l'étape manque (jamais bloquant). */
private fun ClientSection.essentielManque(d: ClientDraft): Boolean = when (this) {
    ClientSection.IDENTITE -> d.nom.isBlank() || (d.telephoneLocal.isBlank() && d.email.isBlank())
    ClientSection.FISCALITE -> {
        val type = ClientType.entries.firstOrNull { it.name == d.type } ?: ClientType.PARTICULIER
        ClientLifecycleRules.informationsFiscalesRequises(type) && d.nif.isBlank()
    }
    else -> false
}

private fun ClientSection.titre(): Int = when (this) {
    ClientSection.IDENTITE -> R.string.cli_section_identite
    ClientSection.FISCALITE -> R.string.cli_section_fiscalite
    ClientSection.CONDITIONS -> R.string.cli_section_conditions
    ClientSection.CONTACTS -> R.string.cli_section_contacts
    ClientSection.NOTES -> R.string.cli_onglet_notes
}
