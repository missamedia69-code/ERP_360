package com.missa.b360.ui.admin

import com.missa.b360.ui.icons.Iv
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.missa.b360.R
import com.missa.b360.core.domain.model.ModuleCode
import com.missa.b360.core.domain.model.ModuleSousElements
import com.missa.b360.core.domain.model.ModulesPersonnalises
import com.missa.b360.core.domain.model.ModulesSocle
import com.missa.b360.core.domain.model.PalierTaille
import com.missa.b360.core.domain.model.ProfilActivite
import com.missa.b360.ui.components.MissaOption
import com.missa.b360.ui.components.MissaSelecteurBleu
import com.missa.b360.ui.theme.BrandBlue
import com.missa.b360.ui.theme.MissaBorder
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted
import com.missa.b360.ui.theme.MissaSoftBlue
import com.missa.b360.ui.theme.MissaSurface

/**
 * Écran Réglages — activation effective des profils dans toute l'application.
 *
 * Chaque profil actif active réellement les modules et les éléments qui lui ont été
 * affectés par défaut OU par l'utilisateur. Cet écran est la porte d'entrée pour
 * gérer cette activation de manière centralisée.
 */
@Composable
fun AdminReglagesScreen(
    onBack: () -> Unit,
    viewModel: ActivationViewModel = hiltViewModel(),
) {
    val activation by viewModel.activation.collectAsState()
    val entreprise by viewModel.entreprise.collectAsState()
    var showChangerProfil by remember { mutableStateOf(false) }
    var moduleDetail by remember { mutableStateOf<ModuleCode?>(null) }

    AdminScaffold(
        titreRes = R.string.activation_titre,
        onBack = onBack,
    ) {
        // En-tête explicatif
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MissaSoftBlue),
            border = BorderStroke(1.dp, BrandBlue.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(Iv.Settings), contentDescription = null, tint = BrandBlue, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.activation_titre), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MissaInk)
                }
                Spacer(Modifier.height(6.dp))
                Text(stringResource(R.string.activation_sous_titre), fontSize = 12.sp, color = MissaMuted)
            }
        }

        // Profil actuel
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MissaSurface),
            border = BorderStroke(1.dp, MissaBorder),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(14.dp)) {
                Text(stringResource(R.string.activation_profil_actuel), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandBlue)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        val profil = activation.profil
                        Text(
                            text = profil?.let { stringResource(ModulesPersonnalises.libelleRes(it)) } ?: stringResource(R.string.home_not_configured),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MissaInk,
                        )
                        Text(
                            text = profil?.description ?: "",
                            fontSize = 11.sp,
                            color = MissaMuted,
                        )
                    }
                    TextButton(onClick = { showChangerProfil = true }) {
                        Text(stringResource(R.string.activation_changer_profil), fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = MissaBorder)
                Spacer(Modifier.height(10.dp))
                // Palier — amélioré avec badges et descriptions d'impact
                MissaSelecteurBleu(
                    label = stringResource(R.string.activation_palier),
                    options = PalierTaille.entries.map { palier ->
                        MissaOption(
                            cle = palier.name,
                            titre = stringResource(palier.labelRes),
                            sousTitre = stringResource(palier.impactRes),
                            badge = palier.tranche,
                            badgeSecondaire = "${palier.emoji} ${palier.modulesDebloques}",
                        )
                    },
                    selectionCle = activation.palier?.name,
                    onSelection = { cle -> runCatching { PalierTaille.valueOf(cle) }.getOrNull()?.let { viewModel.changerPalier(it) } },
                    placeholder = stringResource(R.string.obn_effectif_placeholder),
                    titreDialogue = stringResource(R.string.palier_choisir_titre),
                )
                Spacer(Modifier.height(10.dp))
                // Vente sans stock
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.activation_vente_sans_stock), fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = MissaInk)
                        Text(stringResource(R.string.activation_vente_sans_stock_desc), fontSize = 11.sp, color = MissaMuted)
                    }
                    Spacer(Modifier.width(10.dp))
                    Switch(
                        checked = activation.venteSansStock,
                        onCheckedChange = { viewModel.basculerVenteSansStock() },
                        colors = SwitchDefaults.colors(checkedTrackColor = BrandBlue),
                    )
                }
            }
        }

        // Modules actifs compteur
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, MissaBorder),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(14.dp)) {
                Text(
                    text = stringResource(R.string.activation_modules_actifs, activation.modulesActifs.size),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MissaInk,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(
                        R.string.activation_modules_repartition,
                        activation.modulesMetierEffectifs.size,
                        activation.modulesSupportEffectifs.size,
                    ),
                    fontSize = 11.sp,
                    color = MissaMuted,
                )
            }
        }

        // Modules métier
        ActivationSectionModules(
            titre = stringResource(R.string.obn_profil_perso_metier),
            modules = ModuleCode.entries.filter { ModulesSocle.type(it) == com.missa.b360.core.domain.model.TypeModule.METIER },
            activation = activation,
            onToggle = { viewModel.basculerModuleMetier(it) },
            onDetail = { moduleDetail = it },
        )

        // Modules support
        ActivationSectionModules(
            titre = stringResource(R.string.obn_socle_titre),
            modules = ModuleCode.entries.filter { ModulesSocle.type(it) == com.missa.b360.core.domain.model.TypeModule.SUPPORT },
            activation = activation,
            onToggle = { viewModel.basculerModuleSupport(it) },
            onDetail = { moduleDetail = it },
        )

        // Bouton réinitialiser
        TextButton(
            onClick = { viewModel.reinitialiser() },
            modifier = Modifier.align(Alignment.End),
        ) {
            Text(stringResource(R.string.obn_socle_defaut), fontSize = 12.sp)
        }

        // Infos entreprise (ancien)
        if (entreprise.charge) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MissaSurface),
                border = BorderStroke(1.dp, MissaBorder),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.activation_entreprise_info), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MissaInk)
                    Text(stringResource(R.string.activation_entreprise_nom, entreprise.nom), fontSize = 12.sp, color = MissaInk)
                    Text(stringResource(R.string.activation_entreprise_devise, entreprise.devise), fontSize = 11.sp, color = MissaMuted)
                }
            }
        }
    }

    if (showChangerProfil) {
        ActivationChangerProfilDialogue(
            actuel = activation.profil,
            onChoisir = { profil ->
                viewModel.changerProfil(profil)
                showChangerProfil = false
            },
            onFermer = { showChangerProfil = false },
        )
    }

    moduleDetail?.let { module ->
        ActivationElementsDialogue(
            module = module,
            activation = activation,
            onToggleElement = { elem -> viewModel.basculerElement(module, elem) },
            onFermer = { moduleDetail = null },
        )
    }
}

@Composable
private fun ActivationSectionModules(
    titre: String,
    modules: List<ModuleCode>,
    activation: com.missa.b360.core.domain.model.ActivationProfil,
    onToggle: (ModuleCode) -> Unit,
    onDetail: (ModuleCode) -> Unit,
) {
    var expanded by remember { mutableStateOf(true) }
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, MissaBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(titre, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MissaInk, modifier = Modifier.weight(1f))
                Text(
                    stringResource(
                        R.string.activation_modules_compteur,
                        modules.count { activation.isModuleActif(it) },
                        modules.size,
                    ),
                    fontSize = 11.sp,
                    color = MissaMuted,
                )
                Spacer(Modifier.width(6.dp))
                Icon(
                    painter = painterResource(if (expanded) Iv.ExpandLess else Iv.ExpandMore),
                    contentDescription = null,
                    tint = MissaMuted,
                    modifier = Modifier.size(18.dp),
                )
            }
            if (expanded) {
                Spacer(Modifier.height(8.dp))
                for (module in modules) {
                    ActivationModuleLigne(
                        module = module,
                        activation = activation,
                        onToggle = { onToggle(module) },
                        onDetail = { onDetail(module) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ActivationModuleLigne(
    module: ModuleCode,
    activation: com.missa.b360.core.domain.model.ActivationProfil,
    onToggle: () -> Unit,
    onDetail: () -> Unit,
) {
    val isActif = activation.isModuleActif(module)
    val profil = activation.profil
    val isVerrouille = profil != null && (
        module in ModulesSocle.metierDuPack(profil) ||
            module in ModulesSocle.recommandes(profil, activation.palier, activation.modulesMetierEffectifs)
        )
    val isRecommande = profil != null && module in ModulesSocle.recommandes(profil, activation.palier, activation.modulesMetierEffectifs)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = isActif,
            onCheckedChange = { if (!isVerrouille) onToggle() },
            enabled = !isVerrouille,
            colors = CheckboxDefaults.colors(checkedColor = BrandBlue),
            modifier = Modifier.size(32.dp),
        )
        Spacer(Modifier.width(4.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(ModulesPersonnalises.libelleRes(module)),
                    fontSize = 12.5.sp,
                    fontWeight = if (isActif) FontWeight.SemiBold else FontWeight.Normal,
                    color = MissaInk,
                )
                Spacer(Modifier.width(6.dp))
                if (isVerrouille) {
                    Surface(shape = RoundedCornerShape(5.dp), color = MissaSoftBlue, border = BorderStroke(0.5.dp, BrandBlue.copy(alpha = 0.3f))) {
                        Row(Modifier.padding(horizontal = 5.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(painterResource(Iv.Lock), contentDescription = null, tint = BrandBlue, modifier = Modifier.size(10.dp))
                            Spacer(Modifier.width(3.dp))
                            Text(stringResource(R.string.activation_verrouille), fontSize = 9.sp, color = BrandBlue)
                        }
                    }
                } else if (isRecommande) {
                    Surface(shape = RoundedCornerShape(5.dp), color = com.missa.b360.ui.theme.Blue90) {
                        Text(stringResource(R.string.activation_recommande), fontSize = 9.sp, color = BrandBlue, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                    }
                } else if (isActif) {
                    Surface(shape = RoundedCornerShape(5.dp), color = com.missa.b360.ui.theme.Green90) {
                        Text(stringResource(R.string.activation_personnalise), fontSize = 9.sp, color = com.missa.b360.ui.theme.TendrePositive, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                    }
                }
            }
            Text(
                text = stringResource(ModulesSocle.descriptionRes(module)),
                fontSize = 10.5.sp,
                color = MissaMuted,
            )
            // Compteur éléments
            if (isActif) {
                val elems = activation.elementsActifsPour(module)
                Text(
                    text = stringResource(
                        R.string.activation_elements_compteur,
                        stringResource(R.string.activation_elements_actifs),
                        elems.size,
                        ModuleSousElements.pourModule(module).size,
                    ),
                    fontSize = 10.sp,
                    color = MissaMuted,
                )
            }
        }
        if (isActif) {
            Icon(
                painter = painterResource(Iv.Info),
                contentDescription = stringResource(R.string.activation_elements_actifs),
                tint = BrandBlue,
                modifier = Modifier
                    .size(22.dp)
                    .clickable(onClick = onDetail)
                    .padding(2.dp),
            )
        }
    }
}

@Composable
private fun ActivationChangerProfilDialogue(
    actuel: ProfilActivite?,
    onChoisir: (ProfilActivite) -> Unit,
    onFermer: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onFermer,
        title = { Text(stringResource(R.string.activation_changer_profil), fontSize = 15.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.activation_confirmer_changement), fontSize = 12.sp, color = MissaMuted)
                Spacer(Modifier.height(8.dp))
                // Le pack Achat-Vente n'est plus proposé (supprimé de la
                // matrice) : seules les installations AV existantes le
                // conservent. Le reste du catalogue, dont le pack Personnel.
                for (profil in ProfilActivite.entries.filter { it != ProfilActivite.AV }) {
                    val selected = profil == actuel
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) BrandBlue else MissaBorder),
                        colors = CardDefaults.cardColors(containerColor = if (selected) MissaSoftBlue else Color.White),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onChoisir(profil) },
                    ) {
                        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(ModulesPersonnalises.libelleRes(profil)), fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = MissaInk)
                                Text(profil.description, fontSize = 10.5.sp, color = MissaMuted)
                            }
                            if (selected) {
                                Icon(painterResource(Iv.CheckCircle), contentDescription = null, tint = BrandBlue, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onFermer) { Text(stringResource(R.string.ob_fermer)) }
        },
    )
}

@Composable
private fun ActivationElementsDialogue(
    module: ModuleCode,
    activation: com.missa.b360.core.domain.model.ActivationProfil,
    onToggleElement: (String) -> Unit,
    onFermer: () -> Unit,
) {
    val tousElements = ModuleSousElements.pourModule(module)
    val actifs = activation.elementsActifsPour(module)
    val profil = activation.profil
    val defautElements = if (profil != null && profil != ProfilActivite.FULL && profil != ProfilActivite.CUSTOM) {
        com.missa.b360.core.domain.model.ProfilConfiguration.sousElementsPourModule(profil, module).toSet()
    } else {
        emptySet()
    }

    AlertDialog(
        onDismissRequest = onFermer,
        title = {
            Column {
                Text(stringResource(R.string.activation_elements_titre, stringResource(ModulesPersonnalises.libelleRes(module))), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.activation_elements_sous), fontSize = 11.sp, color = MissaMuted)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    for (elem in tousElements) {
                        val isActif = elem in actifs
                        val isDefaut = elem in defautElements
                        Surface(
                            shape = RoundedCornerShape(7.dp),
                            color = if (isActif) MissaSoftBlue else MissaSurface,
                            border = BorderStroke(1.dp, if (isActif) BrandBlue.copy(alpha = 0.5f) else MissaBorder),
                            modifier = Modifier.clickable { onToggleElement(elem) },
                        ) {
                            Row(Modifier.padding(horizontal = 8.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = isActif,
                                    onCheckedChange = { onToggleElement(elem) },
                                    modifier = Modifier.size(18.dp),
                                    colors = CheckboxDefaults.colors(checkedColor = BrandBlue),
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(elem, fontSize = 11.sp, color = MissaInk)
                                if (isDefaut) {
                                    Spacer(Modifier.width(4.dp))
                                    Icon(painterResource(Iv.Lock), contentDescription = null, tint = BrandBlue, modifier = Modifier.size(10.dp))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onFermer) { Text(stringResource(R.string.ob_fermer)) }
        },
    )
}
