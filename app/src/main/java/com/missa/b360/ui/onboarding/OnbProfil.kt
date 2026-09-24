package com.missa.b360.ui.onboarding

import com.missa.b360.ui.icons.Iv
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.missa.b360.R
import com.missa.b360.core.domain.model.ModuleCode
import com.missa.b360.core.domain.model.ModulesPersonnalises
import com.missa.b360.core.domain.model.ModulesSocle
import com.missa.b360.core.domain.model.PalierTaille
import com.missa.b360.core.domain.model.ProfilActivite
import com.missa.b360.ui.components.MissaOption
import com.missa.b360.ui.components.MissaSelecteurBleu
import com.missa.b360.ui.theme.BrandBlue
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted
import com.missa.b360.ui.theme.MissaSoftBlue
import com.missa.b360.ui.theme.MissaSurface
import com.missa.b360.ui.theme.OnbConfigCard
import com.missa.b360.ui.theme.Red40

/** Un pack de la matrice : profil ciblé, libellés et icône (fiche + dialogue). */
private data class OnbProfilCarteInfo(
    val profil: ProfilActivite,
    val titreRes: Int,
    val sousTitreRes: Int,
    val icone: Int,
)

/**
 * Écran — Profil d'activité (structure matricielle) : en haut, une rangée de
 * petits carrés numérotés (1 → 6), chaque numéro correspondant à un pack ; en
 * bas, la fiche complète du pack sélectionné — modules verrouillés, modules
 * ajoutables, option vente sans stock. L'info « i » de la fiche ouvre la boîte
 * de détail du profil.
 */
@Composable
internal fun OnbProfilStep(viewModel: OnboardingViewModel) {
    val cartes = listOf(
        OnbProfilCarteInfo(
            ProfilActivite.ASV,
            R.string.obn_profil_asv,
            R.string.obn_profil_asv_sous,
            Iv.Inventory2,
        ),
        OnbProfilCarteInfo(
            ProfilActivite.APSV,
            R.string.obn_profil_apsv,
            R.string.obn_profil_apsv_sous,
            Iv.Construction,
        ),
        OnbProfilCarteInfo(
            ProfilActivite.AV,
            R.string.obn_profil_av,
            R.string.obn_profil_av_sous,
            Iv.ShoppingCart,
        ),
        OnbProfilCarteInfo(
            ProfilActivite.SER,
            R.string.obn_profil_ser,
            R.string.obn_profil_ser_sous,
            Iv.Handshake,
        ),
        OnbProfilCarteInfo(
            ProfilActivite.PRJ,
            R.string.obn_profil_prj,
            R.string.obn_profil_prj_sous,
            Iv.Workspaces,
        ),
        OnbProfilCarteInfo(
            ProfilActivite.FULL,
            R.string.obn_profil_full,
            R.string.obn_profil_full_sous,
            Iv.Business,
        ),
    )
    var detailProfil by rememberSaveable { mutableStateOf<String?>(null) }
    OnbScaffold(
        titreRes = R.string.obn_profil_titre,
        sousTitreRes = R.string.obn_profil_sous,
        viewModel = viewModel,
        boutonActive = viewModel.profilEcranValide(),
        onRetour = viewModel::precedent,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            // --- Matrice des packs : petits carrés numérotés (1 = ASV … 6 = FULL) ---
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                cartes.forEachIndexed { index, carte ->
                    OnbProfilCarre(
                        numero = index + 1,
                        code = carte.profil.name,
                        selected = viewModel.profil == carte.profil,
                        onClick = { viewModel.choisirProfil(carte.profil) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            MissaSelecteurBleu(
                label = stringResource(R.string.obn_effectif_label),
                options = PalierTaille.entries.map { palier ->
                    MissaOption(
                        cle = palier.name,
                        titre = stringResource(palier.labelRes),
                        sousTitre = stringResource(palier.impactRes),
                        badge = palier.tranche,
                        badgeSecondaire = "${palier.emoji} ${palier.modulesDebloques}",
                    )
                },
                selectionCle = viewModel.palier?.name,
                onSelection = { cle ->
                    runCatching { PalierTaille.valueOf(cle) }.getOrNull()
                        ?.let(viewModel::choisirPalier)
                },
                icone = Iv.Groups,
                enabled = !viewModel.enregistrementEnCours,
                placeholder = stringResource(R.string.obn_effectif_placeholder),
                titreDialogue = stringResource(R.string.palier_choisir_titre),
            )

            // --- Détail du pack sélectionné, sous la matrice ---
            val profilChoisi = viewModel.profil
            val infoChoisi = cartes.firstOrNull { it.profil == profilChoisi }
            if (profilChoisi != null && infoChoisi != null) {
                OnbDetailPackEntete(
                    titreRes = infoChoisi.titreRes,
                    sousTitreRes = infoChoisi.sousTitreRes,
                    icone = infoChoisi.icone,
                    onInfo = { detailProfil = infoChoisi.profil.name },
                )
                OnbModulesDuPack(viewModel = viewModel)
            }
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
                viewModel.choisirProfil(carteDetaillee.profil)
                detailProfil = null
            },
            onFermer = { detailProfil = null },
        )
    }
}

/**
 * Petit carré numéroté de la matrice : chaque numéro correspond à un pack
 * (1 = ASV, 2 = APSV, 3 = AV, 4 = SER, 5 = PRJ, 6 = FULL). Le clic sélectionne
 * le pack et affiche sa fiche complète (modules) sous la matrice ; le code du
 * pack, sous le carré, rappelle quel numéro est le bon sans avoir à cliquer.
 */
@Composable
private fun OnbProfilCarre(
    numero: Int,
    code: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (selected) BrandBlue else OnbConfigCard)
                .clickable(onClick = onClick),
        ) {
            Text(
                text = numero.toString(),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = if (selected) Color.White else BrandBlue,
            )
        }
        Spacer(Modifier.height(5.dp))
        Text(
            text = code,
            fontSize = 10.5.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) BrandBlue else MissaMuted,
            maxLines = 1,
        )
    }
}

/** En-tête du détail sous la matrice : pack choisi + lien « i ». */
@Composable
private fun OnbDetailPackEntete(
    titreRes: Int,
    sousTitreRes: Int,
    icone: Int,
    onInfo: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = BrandBlue.copy(alpha = 0.12f),
            modifier = Modifier.size(34.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(icone),
                    contentDescription = null,
                    tint = BrandBlue,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(titreRes),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MissaInk,
            )
            Text(
                text = stringResource(sousTitreRes),
                fontSize = 12.sp,
                color = MissaMuted,
            )
        }
        Surface(
            shape = CircleShape,
            color = OnbConfigCard,
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .clickable(onClick = onInfo),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(Iv.Info),
                    contentDescription = stringResource(R.string.obn_profil_info),
                    tint = BrandBlue,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

/**
 * Composition du pack, dépliée sous la tranche choisie.
 *
 * Deux temps, et une règle : **ce que le pack apporte est verrouillé**. Les
 * modules métier du profil et les briques transverses recommandées forment un
 * ensemble cohérent — un « Achat-Vente » sans Vente, ou des ventes sans
 * Comptabilité, ne produiraient qu'une installation bancale. Ils sont donc
 * montrés en pastilles cadenassées, pour information.
 *
 * En dessous, tout le reste du catalogue est librement cochable, modules métier
 * compris : un prestataire de services qui tient malgré tout un petit stock
 * ajoute Stock sans quitter son profil.
 */
@Composable
private fun OnbModulesDuPack(viewModel: OnboardingViewModel) {
    val profil = viewModel.profil
    val metierDuPack = ModulesSocle.metierDuPack(profil)
    val supportRecommande = viewModel.socleRecommande()
    val verrouilles = ModuleCode.entries.filter { it in metierDuPack || it in supportRecommande }
    val metierAjoutable = ModulesSocle.metier.filterNot { it in metierDuPack }
    val supportAjoutable = ModulesSocle.support.filterNot { it in supportRecommande }
    val metierChoisi = viewModel.modulesPersonnalises
    val actifs = verrouilles.size +
        metierAjoutable.count { it in metierChoisi } +
        supportAjoutable.count { it in viewModel.modulesSupport }
    val selectionVide = profil == ProfilActivite.CUSTOM && metierChoisi.isEmpty()

    Card(
        shape = RoundedCornerShape(16.dp),
        border = if (selectionVide) BorderStroke(1.dp, Red40) else null,
        colors = CardDefaults.cardColors(containerColor = OnbConfigCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.obn_pack_inclus),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MissaInk,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = stringResource(R.string.obn_pack_compteur, actifs),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BrandBlue,
                )
            }
            Text(
                text = stringResource(R.string.obn_pack_inclus_note),
                fontSize = 11.sp,
                color = MissaMuted,
            )
            Spacer(Modifier.height(7.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                for (module in verrouilles) {
                    OnbModulePastille(module)
                }
            }
            if (selectionVide) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.obn_profil_perso_vide),
                    fontSize = 11.5.sp,
                    color = Red40,
                )
            }

            if (metierAjoutable.isEmpty() && supportAjoutable.isEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.obn_pack_complet),
                    fontSize = 11.5.sp,
                    color = MissaMuted,
                )
                return@Column
            }

            Spacer(Modifier.height(11.dp))
            HorizontalDivider(color = BrandBlue.copy(alpha = 0.18f))
            Spacer(Modifier.height(9.dp))
            Text(
                text = stringResource(R.string.obn_pack_ajouter),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MissaInk,
            )
            Text(
                text = stringResource(R.string.obn_pack_ajouter_note),
                fontSize = 11.sp,
                color = MissaMuted,
            )
            Spacer(Modifier.height(4.dp))
            if (metierAjoutable.isNotEmpty()) {
                OnbModulesGroupe(titreRes = R.string.obn_profil_perso_metier)
                for (module in metierAjoutable) {
                    OnbModuleAjoutable(
                        module = module,
                        coche = module in metierChoisi,
                        onBascule = { viewModel.basculerModule(module) },
                    )
                }
            }
            if (supportAjoutable.isNotEmpty()) {
                OnbModulesGroupe(titreRes = R.string.obn_socle_titre)
                for (module in supportAjoutable) {
                    OnbModuleAjoutable(
                        module = module,
                        coche = module in viewModel.modulesSupport,
                        onBascule = { viewModel.basculerSupport(module) },
                    )
                }
            }
            if (viewModel.socleAjuste || metierChoisi.any { it !in metierDuPack }) {
                TextButton(
                    onClick = viewModel::reinitialiserSocle,
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text(text = stringResource(R.string.obn_socle_defaut), fontSize = 12.sp)
                }
            }
            if (viewModel.dependancesActives.isNotEmpty()) {
                Spacer(Modifier.height(9.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(9.dp),
                    color = MissaSoftBlue,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painter = painterResource(Iv.Info),
                            contentDescription = null,
                            tint = BrandBlue,
                            modifier = Modifier.size(15.dp),
                        )
                        Spacer(Modifier.width(7.dp))
                        Text(
                            text = stringResource(R.string.obn_regle_dor),
                            fontSize = 11.sp,
                            color = MissaInk,
                        )
                    }
                }
            }
            Spacer(Modifier.height(9.dp))
            HorizontalDivider(color = BrandBlue.copy(alpha = 0.18f))
            Spacer(Modifier.height(9.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.obn_vente_sans_stock),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MissaInk,
                    )
                    Text(
                        text = stringResource(R.string.obn_vente_sans_stock_sous),
                        fontSize = 11.sp,
                        color = MissaMuted,
                    )
                }
                Spacer(Modifier.width(10.dp))
                Switch(
                    checked = viewModel.venteSansStock,
                    onCheckedChange = { viewModel.basculerVenteSansStock() },
                    colors = SwitchDefaults.colors(checkedTrackColor = BrandBlue),
                )
            }
            if (viewModel.erreurRes == R.string.obn_stock_requis) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.obn_stock_requis),
                    fontSize = 11.5.sp,
                    color = Red40,
                )
            }
        }
    }
}

/** Intertitre « Modules métier » / « Modules support » de la liste des ajouts. */
@Composable
private fun OnbModulesGroupe(titreRes: Int) {
    Text(
        text = stringResource(titreRes),
        fontSize = 10.5.sp,
        fontWeight = FontWeight.SemiBold,
        color = MissaMuted,
        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp),
    )
}

/** Module fourni par le pack : pastille cadenassée, sans interrupteur. */
@Composable
private fun OnbModulePastille(module: ModuleCode) {
    Surface(
        shape = RoundedCornerShape(7.dp),
        color = MissaSurface,
        border = BorderStroke(1.dp, BrandBlue.copy(alpha = 0.35f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(Iv.Lock),
                contentDescription = null,
                tint = BrandBlue,
                modifier = Modifier.size(11.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = stringResource(ModulesPersonnalises.libelleRes(module)),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = MissaInk,
            )
        }
    }
}

/** Module hors pack : case à cocher, libellé et rôle en une ligne. */
@Composable
private fun OnbModuleAjoutable(module: ModuleCode, coche: Boolean, onBascule: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onBascule)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = coche,
            onCheckedChange = { onBascule() },
            colors = CheckboxDefaults.colors(checkedColor = BrandBlue),
            modifier = Modifier.size(34.dp),
        )
        Spacer(Modifier.width(6.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(ModulesPersonnalises.libelleRes(module)),
                fontSize = 12.5.sp,
                fontWeight = if (coche) FontWeight.SemiBold else FontWeight.Normal,
                color = MissaInk,
            )
            Text(
                text = stringResource(ModulesSocle.descriptionRes(module)),
                fontSize = 10.5.sp,
                color = MissaMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
