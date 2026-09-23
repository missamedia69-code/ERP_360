package com.missa.b360.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.missa.b360.R
import com.missa.b360.core.data.entity.SiteEntity
import com.missa.b360.ui.components.MissaEmptyState
import com.missa.b360.ui.components.MissaTopAppBar
import com.missa.b360.ui.icons.Iv
import com.missa.b360.ui.navigation.AppModule
import com.missa.b360.ui.theme.MissaBorder
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted

private val BleuSites: Color get() = AppModule.LOGISTIQUE.couleur

private data class TuileAdminSitesSpec(
    val icone: Int,
    val titre: String,
    val sousTitre: String,
    val estActif: Boolean,
    val onClick: () -> Unit,
)

@Composable
fun AdminSitesScreen(
    onBack: () -> Unit,
    vm: AdminSitesViewModel = hiltViewModel(),
) {
    val sites by vm.sites.collectAsStateWithLifecycle()
    val totalSites by vm.totalSites.collectAsStateWithLifecycle()
    val filtreType by vm.filtreType.collectAsStateWithLifecycle()

    var dialogueNouveauSite by remember { mutableStateOf(false) }
    var siteASupprimer by remember { mutableStateOf<SiteEntity?>(null) }

    val depotsCount = totalSites.count { it.type.contains("dépot", ignoreCase = true) || it.type.contains("depot", ignoreCase = true) || it.type.contains("entrepôt", ignoreCase = true) }
    val boutiquesCount = totalSites.count { it.type.contains("boutique", ignoreCase = true) || it.type.contains("magasin", ignoreCase = true) }

    Column(Modifier.fillMaxSize()) {
        MissaTopAppBar(
            title = stringResource(R.string.home_sites_sales),
            onBack = onBack,
            couleurFond = BleuSites.copy(alpha = 0.2f),
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // --- Hero Synthèse Sites & Dépôts ---
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = BleuSites.copy(alpha = 0.16f),
                ) {
                    Column(Modifier.fillMaxWidth().padding(14.dp)) {
                        Text(
                            stringResource(R.string.sites_titre_synthese),
                            fontSize = 11.sp,
                            color = MissaMuted,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            stringResource(R.string.sites_total_count, totalSites.size),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MissaInk,
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                stringResource(R.string.sites_depots_count, depotsCount),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BleuSites,
                            )
                            Text(
                                stringResource(R.string.sites_boutiques_count, boutiquesCount),
                                fontSize = 11.sp,
                                color = MissaMuted,
                            )
                        }
                    }
                }
            }

            // --- Structure Matricielle 4 Tuiles ---
            item {
                Text(
                    stringResource(R.string.sites_matrice_titre),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MissaInk,
                )
            }

            item {
                val tuiles = listOf(
                    TuileAdminSitesSpec(
                        icone = Iv.Warehouse,
                        titre = stringResource(R.string.sites_tuile_tous),
                        sousTitre = totalSites.size.toString(),
                        estActif = filtreType == null,
                        onClick = { vm.filtrer(null) },
                    ),
                    TuileAdminSitesSpec(
                        icone = Iv.Store,
                        titre = stringResource(R.string.sites_tuile_boutiques),
                        sousTitre = boutiquesCount.toString(),
                        estActif = filtreType == "Boutique",
                        onClick = { vm.filtrer("Boutique") },
                    ),
                    TuileAdminSitesSpec(
                        icone = Iv.LocalShipping,
                        titre = stringResource(R.string.sites_tuile_depots),
                        sousTitre = depotsCount.toString(),
                        estActif = filtreType == "Entrepôt",
                        onClick = { vm.filtrer("Entrepôt") },
                    ),
                    TuileAdminSitesSpec(
                        icone = Iv.Add,
                        titre = stringResource(R.string.sites_nouveau_site),
                        sousTitre = stringResource(R.string.st_creer),
                        estActif = false,
                        onClick = { dialogueNouveauSite = true },
                    ),
                )

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    tuiles.forEach { tuile ->
                        TuileSites(
                            icone = tuile.icone,
                            titre = tuile.titre,
                            sousTitre = tuile.sousTitre,
                            estActif = tuile.estActif,
                            modifier = Modifier.weight(1f),
                            onClick = tuile.onClick,
                        )
                    }
                }
            }

            // --- Registre des Sites ---
            item {
                Text(
                    stringResource(R.string.sites_titre_registre),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MissaInk,
                )
            }

            if (sites.isEmpty()) {
                item {
                    MissaEmptyState(
                        icon = Iv.Warehouse,
                        title = stringResource(R.string.sites_aucun),
                        description = stringResource(R.string.sites_aucun_desc),
                        modifier = Modifier.padding(16.dp),
                    )
                }
            } else {
                items(sites, key = { it.id }) { site ->
                    CarteSite(
                        site = site,
                        onSupprimer = { siteASupprimer = site },
                    )
                }
            }
        }
    }

    if (dialogueNouveauSite) {
        DialogueNouveauSite(
            onFermer = { dialogueNouveauSite = false },
            onValider = { nom, adr, type, princ ->
                vm.creer(nom, adr, type, princ)
                dialogueNouveauSite = false
            },
        )
    }

    siteASupprimer?.let { s ->
        AlertDialog(
            onDismissRequest = { siteASupprimer = null },
            title = { Text(stringResource(R.string.ach_annuler), color = MissaInk) },
            text = { Text(stringResource(R.string.sites_confirmer_suppression, s.nom), color = MissaInk) },
            confirmButton = {
                TextButton(onClick = {
                    vm.supprimer(s.id)
                    siteASupprimer = null
                }) { Text(stringResource(R.string.ops_delete), color = Color(0xFFB91C1C)) }
            },
            dismissButton = {
                TextButton(onClick = { siteASupprimer = null }) {
                    Text(stringResource(R.string.ops_cancel), color = MissaInk)
                }
            },
        )
    }
}

@Composable
private fun TuileSites(
    icone: Int,
    titre: String,
    sousTitre: String,
    estActif: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (estActif) BleuSites.copy(alpha = 0.15f) else Color.White,
        border = BorderStroke(1.dp, if (estActif) BleuSites else MissaBorder),
        modifier = modifier
            .height(82.dp)
            .clickable(onClick = onClick),
    ) {
        Column(
            Modifier.padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(painterResource(icone), null, tint = MissaInk, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(4.dp))
            Text(titre, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MissaInk, maxLines = 1)
            Text(sousTitre, fontSize = 9.sp, color = MissaMuted, maxLines = 1)
        }
    }
}

@Composable
private fun CarteSite(
    site: SiteEntity,
    onSupprimer: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, MissaBorder),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(site.nom, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MissaInk)
                    if (site.principal) {
                        Spacer(Modifier.padding(horizontal = 4.dp))
                        Surface(shape = RoundedCornerShape(6.dp), color = BleuSites.copy(alpha = 0.15f)) {
                            Text(
                                stringResource(R.string.sites_badge_principal),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = BleuSites,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(site.type, fontSize = 11.5.sp, color = MissaMuted)
                site.adresse?.let {
                    Text(it, fontSize = 10.5.sp, color = MissaMuted)
                }
            }
            if (!site.principal) {
                IconButton(onClick = onSupprimer, modifier = Modifier.size(36.dp)) {
                    Icon(painterResource(Iv.DeleteOutline), null, tint = Color(0xFFB91C1C), modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun DialogueNouveauSite(
    onFermer: () -> Unit,
    onValider: (String, String?, String, Boolean) -> Unit,
) {
    var nom by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("Boutique") }
    var adresse by remember { mutableStateOf("") }
    var principal by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onFermer,
        title = { Text(stringResource(R.string.sites_nouveau_site), fontSize = 16.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = nom,
                    onValueChange = { nom = it },
                    label = { Text(stringResource(R.string.sites_champ_nom), fontSize = 11.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = type,
                    onValueChange = { type = it },
                    label = { Text(stringResource(R.string.sites_champ_type), fontSize = 11.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = adresse,
                    onValueChange = { adresse = it },
                    label = { Text(stringResource(R.string.sites_champ_adresse), fontSize = 11.sp) },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = principal, onCheckedChange = { principal = it })
                    Text(stringResource(R.string.sites_champ_principal), fontSize = 12.sp, color = MissaInk)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onValider(nom, adresse, type, principal) },
                enabled = nom.isNotBlank(),
            ) { Text(stringResource(R.string.ops_save)) }
        },
        dismissButton = {
            TextButton(onClick = onFermer) { Text(stringResource(R.string.ops_cancel)) }
        },
    )
}
