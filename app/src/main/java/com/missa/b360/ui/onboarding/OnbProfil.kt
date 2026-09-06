package com.missa.b360.ui.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Construction
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Workspaces
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.missa.b360.R
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
import com.missa.b360.ui.theme.Red40

/** Une carte de l'écran : profil ciblé, libellés et icône de la maquette. */
private data class OnbProfilCarteInfo(
    val profil: ProfilActivite,
    val titreRes: Int,
    val sousTitreRes: Int,
    val icone: ImageVector,
)

/**
 * Écran — Profil d'activité : les six familles de la maquette plus l'option
 * « Personnalisé ». Chaque carte porte un bouton « i » qui ouvre une boîte
 * détaillant les modules et les fonctionnalités réellement activés par ce choix.
 */
@Composable
internal fun OnbProfilStep(viewModel: OnboardingViewModel) {
    val cartes = listOf(
        OnbProfilCarteInfo(
            ProfilActivite.AV,
            R.string.obn_profil_av,
            R.string.obn_profil_av_sous,
            Icons.Outlined.ShoppingCart,
        ),
        OnbProfilCarteInfo(
            ProfilActivite.ASV,
            R.string.obn_profil_asv,
            R.string.obn_profil_asv_sous,
            Icons.Outlined.Inventory2,
        ),
        OnbProfilCarteInfo(
            ProfilActivite.APSV,
            R.string.obn_profil_apsv,
            R.string.obn_profil_apsv_sous,
            Icons.Outlined.Construction,
        ),
        OnbProfilCarteInfo(
            ProfilActivite.SER,
            R.string.obn_profil_ser,
            R.string.obn_profil_ser_sous,
            Icons.Outlined.Handshake,
        ),
        OnbProfilCarteInfo(
            ProfilActivite.PRJ,
            R.string.obn_profil_prj,
            R.string.obn_profil_prj_sous,
            Icons.Outlined.Workspaces,
        ),
        OnbProfilCarteInfo(
            ProfilActivite.FULL,
            R.string.obn_profil_full,
            R.string.obn_profil_full_sous,
            Icons.Outlined.Business,
        ),
        OnbProfilCarteInfo(
            ProfilActivite.CUSTOM,
            R.string.profil_custom,
            R.string.obn_profil_perso_sous,
            Icons.Outlined.Tune,
        ),
    )
    var detailProfil by rememberSaveable { mutableStateOf<String?>(null) }
    val choisir: (ProfilActivite) -> Unit = { profil ->
        if (profil == ProfilActivite.CUSTOM) {
            viewModel.choisirPersonnalisation()
        } else {
            viewModel.choisirProfil(profil)
        }
    }
    OnbScaffold(
        titreRes = R.string.obn_profil_titre,
        sousTitreRes = R.string.obn_profil_sous,
        viewModel = viewModel,
        boutonActive = viewModel.profilEcranValide(),
        onRetour = viewModel::precedent,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            for (carte in cartes) {
                OnbProfilCarte(
                    titreRes = carte.titreRes,
                    sousTitreRes = carte.sousTitreRes,
                    icone = carte.icone,
                    selected = viewModel.profil == carte.profil,
                    onClick = { choisir(carte.profil) },
                    onInfo = { detailProfil = carte.profil.name },
                )
                if (viewModel.profil == carte.profil) {
                    if (carte.profil == ProfilActivite.CUSTOM) {
                        OnbModulesPersonnalises(viewModel = viewModel)
                    }
                    OnbModulesSocle(viewModel = viewModel, profilTitreRes = carte.titreRes)
                }
            }
            MissaSelecteurBleu(
                label = stringResource(R.string.obn_effectif_label),
                options = PalierTaille.entries.map { palier ->
                    MissaOption(cle = palier.name, titre = stringResource(palier.labelRes))
                },
                selectionCle = viewModel.palier?.name,
                onSelection = { cle ->
                    runCatching { PalierTaille.valueOf(cle) }.getOrNull()
                        ?.let(viewModel::choisirPalier)
                },
                icone = Icons.Outlined.Groups,
                enabled = !viewModel.enregistrementEnCours,
                placeholder = stringResource(R.string.obn_effectif_placeholder),
            )
        }
    }
    val carteDetaillee = cartes.firstOrNull { it.profil.name == detailProfil }
    if (carteDetaillee != null) {
        OnbProfilDetailDialogue(
            profil = carteDetaillee.profil,
            titreRes = carteDetaillee.titreRes,
            sousTitreRes = carteDetaillee.sousTitreRes,
            icone = carteDetaillee.icone,
            palier = viewModel.palier,
            dejaChoisi = viewModel.profil == carteDetaillee.profil,
            onChoisir = {
                choisir(carteDetaillee.profil)
                detailProfil = null
            },
            onFermer = { detailProfil = null },
        )
    }
}

/**
 * Personnalisation du niveau 1 (profil « Personnalisé ») : les six modules
 * **métier** cochables un à un, avec le nombre de fonctionnalités que chacun
 * apporte. Les briques transverses restent gérées par la carte « Modules support ».
 */
@Composable
private fun OnbModulesPersonnalises(viewModel: OnboardingViewModel) {
    val selection = viewModel.modulesPersonnalises
    val tousCoches = selection.size == ModulesSocle.metier.size
    Card(
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (selection.isEmpty()) Red40 else BrandBlue),
        colors = CardDefaults.cardColors(containerColor = MissaSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(
                text = stringResource(R.string.obn_profil_perso_metier),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MissaInk,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.obn_profil_perso_compteur, selection.size),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selection.isEmpty()) Red40 else MissaInk,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = viewModel::basculerTousLesModules) {
                    Text(
                        text = stringResource(
                            if (tousCoches) R.string.obn_profil_perso_rien
                            else R.string.obn_profil_perso_tout,
                        ),
                        fontSize = 12.sp,
                    )
                }
            }
            if (selection.isEmpty()) {
                Text(
                    text = stringResource(R.string.obn_profil_perso_vide),
                    fontSize = 11.5.sp,
                    color = Red40,
                )
            }
            Spacer(Modifier.height(4.dp))
            for (module in ModulesSocle.metier) {
                val actif = module in selection
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.basculerModule(module) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = actif,
                        onCheckedChange = { viewModel.basculerModule(module) },
                        colors = CheckboxDefaults.colors(checkedColor = BrandBlue),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(ModulesPersonnalises.libelleRes(module)),
                            fontSize = 13.sp,
                            fontWeight = if (actif) FontWeight.SemiBold else FontWeight.Normal,
                            color = MissaInk,
                        )
                        Text(
                            text = stringResource(
                                R.string.obn_profil_perso_fonctions,
                                ModuleSousElements.pourModule(module).size,
                            ),
                            fontSize = 11.sp,
                            color = MissaMuted,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Niveau 2 — options socle, dépliées **sous la tranche métier choisie** : les
 * briques transverses (Comptabilité, Trésorerie, Logistique, Reporting, CRM, RH,
 * Qualité, Maintenance) s'ajoutent au profil sélectionné juste au-dessus. Elles sont pré-cochées selon des règles simples (comptabilité et
 * reporting systématiques, trésorerie dès qu'il y a achat ou vente, logistique
 * avec le stock, qualité et maintenance avec la production, RH selon
 * l'effectif) ; l'utilisateur reste libre de les activer ou non.
 */
@Composable
private fun OnbModulesSocle(viewModel: OnboardingViewModel, profilTitreRes: Int) {
    val selection = viewModel.modulesSupport
    val recommandes = viewModel.socleRecommande()
    Card(
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, BrandBlue.copy(alpha = 0.45f)),
        colors = CardDefaults.cardColors(containerColor = MissaSoftBlue),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.obn_socle_titre),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MissaInk,
                    )
                    Text(
                        text = stringResource(
                            R.string.obn_socle_pour,
                            stringResource(profilTitreRes),
                        ),
                        fontSize = 11.5.sp,
                        color = MissaMuted,
                    )
                }
                Text(
                    text = stringResource(R.string.obn_socle_compteur, selection.size),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BrandBlue,
                )
            }
            Spacer(Modifier.height(6.dp))
            for (module in ModulesSocle.support) {
                val actif = module in selection
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.basculerSupport(module) }
                        .padding(vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(ModulesPersonnalises.libelleRes(module)),
                                fontSize = 13.sp,
                                fontWeight = if (actif) FontWeight.SemiBold else FontWeight.Normal,
                                color = MissaInk,
                            )
                            if (module in recommandes) {
                                Spacer(Modifier.width(6.dp))
                                Surface(shape = RoundedCornerShape(6.dp), color = MissaSurface) {
                                    Text(
                                        text = stringResource(R.string.obn_socle_recommande),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = BrandBlue,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                    )
                                }
                            }
                        }
                        Text(
                            text = stringResource(ModulesSocle.descriptionRes(module)),
                            fontSize = 11.sp,
                            color = MissaMuted,
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Switch(
                        checked = actif,
                        onCheckedChange = { viewModel.basculerSupport(module) },
                        colors = SwitchDefaults.colors(checkedTrackColor = BrandBlue),
                    )
                }
            }
            if (viewModel.socleAjuste) {
                TextButton(
                    onClick = viewModel::reinitialiserSocle,
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text(text = stringResource(R.string.obn_socle_defaut), fontSize = 12.sp)
                }
            }
        }
    }
}

/** Carte d'option de la maquette : puce iconée, titre, sous-titre, chevron si sélectionné. */
@Composable
internal fun OnbProfilCarte(
    titreRes: Int,
    sousTitreRes: Int,
    icone: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    onInfo: (() -> Unit)? = null,
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            if (selected) 1.5.dp else 1.dp,
            if (selected) BrandBlue else MissaBorder,
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) BrandBlue.copy(alpha = 0.045f) else MissaSurface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 15.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = RoundedCornerShape(11.dp),
                color = BrandBlue.copy(alpha = 0.09f),
                modifier = Modifier.size(42.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icone,
                        contentDescription = null,
                        tint = BrandBlue,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(titreRes),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MissaInk,
                )
                Text(
                    text = stringResource(sousTitreRes),
                    fontSize = 12.5.sp,
                    color = MissaMuted,
                )
            }
            if (selected) {
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = BrandBlue,
                    modifier = Modifier.size(20.dp),
                )
            }
            if (onInfo != null) {
                Spacer(Modifier.width(2.dp))
                Surface(
                    shape = CircleShape,
                    color = if (selected) BrandBlue else MissaSoftBlue,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onInfo),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = stringResource(R.string.obn_profil_info),
                            tint = if (selected) Color.White else BrandBlue,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }
    }
}
