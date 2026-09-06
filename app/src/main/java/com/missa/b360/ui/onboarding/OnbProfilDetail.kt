package com.missa.b360.ui.onboarding

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.missa.b360.R
import com.missa.b360.core.domain.model.ModuleCode
import com.missa.b360.core.domain.model.ModuleFonctions
import com.missa.b360.core.domain.model.ModuleSousElements
import com.missa.b360.core.domain.model.ModulesPersonnalises
import com.missa.b360.core.domain.model.ProfilActivite
import com.missa.b360.core.domain.model.ProfilConfiguration
import com.missa.b360.ui.theme.BrandBlue
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted
import com.missa.b360.ui.theme.MissaSoftBlue

/**
 * Boîte de détail d'un profil d'activité (bouton « i » des cartes) : elle liste
 * les modules réellement activés par ce profil et, pour chacun, les
 * fonctionnalités correspondantes — toutes traduites via [ModuleFonctions].
 *
 * Le profil « Personnalisé » n'active rien par défaut : la boîte présente alors
 * le catalogue complet des 14 modules, à cocher ensuite dans l'écran.
 */
@Composable
internal fun OnbProfilDetailDialogue(
    profil: ProfilActivite,
    titreRes: Int,
    sousTitreRes: Int,
    icone: ImageVector,
    dejaChoisi: Boolean,
    onChoisir: () -> Unit,
    onFermer: () -> Unit,
) {
    val catalogueComplet = profil == ProfilActivite.CUSTOM
    val modules: List<ModuleCode> = if (catalogueComplet) {
        ModuleCode.entries.toList()
    } else {
        ProfilConfiguration.modulesPourProfil(profil)
    }
    val blocs: List<Pair<ModuleCode, List<String>>> = modules.map { module ->
        // Un module activé « en entier » par le profil (valeur null dans la
        // configuration) doit lister tout son catalogue ; le repli protège
        // aussi d'une configuration incomplète — jamais de module vide.
        module to if (catalogueComplet) {
            ModuleSousElements.pourModule(module)
        } else {
            ProfilConfiguration.sousElementsPourModule(profil, module)
                .ifEmpty { ModuleSousElements.pourModule(module) }
        }
    }
    val totalFonctions = blocs.sumOf { it.second.size }
    AlertDialog(
        onDismissRequest = onFermer,
        icon = {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = BrandBlue.copy(alpha = 0.09f),
                modifier = Modifier.size(44.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icone,
                        contentDescription = null,
                        tint = BrandBlue,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        },
        title = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(titreRes),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MissaInk,
                )
                Text(
                    text = stringResource(sousTitreRes),
                    fontSize = 12.5.sp,
                    color = MissaMuted,
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    shape = RoundedCornerShape(9.dp),
                    color = MissaSoftBlue,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(
                            R.string.obn_profil_detail_resume,
                            modules.size,
                            totalFonctions,
                        ),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BrandBlue,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(
                        if (catalogueComplet) R.string.obn_profil_detail_custom
                        else R.string.obn_profil_detail_modifiable,
                    ),
                    fontSize = 11.5.sp,
                    color = MissaMuted,
                )
                Spacer(Modifier.height(6.dp))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 340.dp),
                ) {
                    items(blocs, key = { it.first.name }) { (module, fonctions) ->
                        OnbProfilDetailModule(module = module, fonctions = fonctions)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onChoisir, enabled = !dejaChoisi) {
                Text(
                    text = stringResource(
                        if (dejaChoisi) R.string.obn_profil_detail_actif
                        else R.string.obn_profil_detail_choisir,
                    ),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onFermer) {
                Text(text = stringResource(R.string.ob_fermer), fontSize = 13.sp)
            }
        },
    )
}

/** Un module de la boîte : puce, nom traduit, compteur et fonctionnalités listées. */
@Composable
private fun OnbProfilDetailModule(module: ModuleCode, fonctions: List<String>) {
    val complet = fonctions.size == ModuleSousElements.pourModule(module).size
    val libelles = ArrayList<String>(fonctions.size)
    for (nom in fonctions) {
        val res = ModuleFonctions.libelleRes(nom)
        libelles.add(if (res != null) stringResource(res) else nom)
    }
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(BrandBlue),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(ModulesPersonnalises.libelleRes(module)),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MissaInk,
                modifier = Modifier.weight(1f),
            )
            if (complet) {
                Surface(shape = RoundedCornerShape(6.dp), color = MissaSoftBlue) {
                    Text(
                        text = stringResource(R.string.obn_profil_detail_complet),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BrandBlue,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                    )
                }
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text = stringResource(R.string.obn_profil_perso_fonctions, libelles.size),
                fontSize = 11.sp,
                color = MissaMuted,
            )
        }
        if (libelles.isNotEmpty()) {
            Text(
                text = libelles.joinToString(" · "),
                fontSize = 11.5.sp,
                color = MissaMuted,
                modifier = Modifier.padding(start = 15.dp, top = 1.dp),
            )
        }
    }
}
