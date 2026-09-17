package com.missa.b360.ui.stock

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.missa.b360.R
import com.missa.b360.core.data.entity.ProductEquipementEntity
import com.missa.b360.core.data.entity.StatutEquipement
import com.missa.b360.ui.components.MissaTopAppBar
import com.missa.b360.ui.navigation.Routes
import com.missa.b360.ui.theme.Blue90
import com.missa.b360.ui.theme.BrandBlue
import com.missa.b360.ui.theme.Green60
import com.missa.b360.ui.theme.Green90
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted
import com.missa.b360.ui.theme.ProfileOrange
import com.missa.b360.ui.theme.Red40
import com.missa.b360.ui.theme.Red80

/** Badge de statut d'un équipement : En service / Maintenance / Hors service. */
@Composable
fun BadgeStatutEquipement(equipement: ProductEquipementEntity?) {
    val statut = equipement?.statut ?: StatutEquipement.EN_SERVICE
    val (texteRes, teinte, fond) = when (statut) {
        StatutEquipement.EN_SERVICE -> Triple(R.string.st_en_service, Green60, Green90)
        StatutEquipement.MAINTENANCE -> Triple(R.string.st_maintenance, ProfileOrange, Color(0xFFFFF4E5))
        StatutEquipement.HORS_SERVICE -> Triple(R.string.st_hors_service, Red40, Red80)
    }
    Surface(shape = RoundedCornerShape(8.dp), color = fond) {
        Text(
            text = stringResource(texteRes),
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            color = teinte,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
        )
    }
}

/** Maquette 5 — liste des équipements avec recherche, filtres de statut et compteurs. */
@Composable
fun StockEquipementsScreen(onBack: () -> Unit, onNavigate: (String) -> Unit = {}) {
    val vm: StockEquipementsViewModel = hiltViewModel()
    val etat by vm.etat.collectAsStateWithLifecycle()
    val requete by vm.requete.collectAsStateWithLifecycle()
    val filtreStatut by vm.filtreStatut.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        MissaTopAppBar(title = stringResource(R.string.st_equipements), onBack = onBack)
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(8.dp))
            androidx.compose.material3.OutlinedTextField(
                value = requete,
                onValueChange = vm::chercher,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.st_rechercher_equipement), fontSize = 12.sp, color = MissaMuted) },
                leadingIcon = {
                    Icon(
                        StockIv.Search,
                        null,
                        tint = MissaMuted,
                        modifier = Modifier.size(18.dp),
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                StockChip(stringResource(R.string.st_tous), actif = filtreStatut == 0) { vm.setStatut(0) }
                StockChip(stringResource(R.string.st_en_service), actif = filtreStatut == 1) { vm.setStatut(1) }
                StockChip(stringResource(R.string.st_maintenance), actif = filtreStatut == 2) { vm.setStatut(2) }
                StockChip(stringResource(R.string.st_hors_service), actif = filtreStatut == 3) { vm.setStatut(3) }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile(
                    icone = StockIv.CheckCircle,
                    teinte = Green60,
                    fond = Green90,
                    valeur = etat.enService.toString(),
                    libelle = stringResource(R.string.st_en_service),
                    modifier = Modifier.weight(1f),
                )
                StatTile(
                    icone = StockIv.Build,
                    teinte = ProfileOrange,
                    fond = Color(0xFFFFF4E5),
                    valeur = etat.maintenance.toString(),
                    libelle = stringResource(R.string.st_maintenance),
                    modifier = Modifier.weight(1f),
                )
                StatTile(
                    icone = StockIv.Error,
                    teinte = Red40,
                    fond = Red80,
                    valeur = etat.horsService.toString(),
                    libelle = stringResource(R.string.st_hors_service),
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.st_equipements_count, etat.lignes.size),
                fontSize = 11.sp,
                color = MissaMuted,
                modifier = Modifier.padding(vertical = 6.dp),
            )
            LazyColumn {
                items(etat.lignes, key = { it.product.id }) { ligne ->
                    CarteStock(onClick = { onNavigate(Routes.stockDetail(ligne.product.id)) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(modifier = Modifier.size(44.dp), shape = RoundedCornerShape(12.dp), color = Blue90) {
                                androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                                    Icon(ligne.product.type.icone(), null, tint = BrandBlue, modifier = Modifier.size(20.dp))
                                }
                            }
                            ProduitImage(photoPath = ligne.product.photoPath)
                            Spacer(Modifier.width(11.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(ligne.product.nom, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = MissaInk, maxLines = 1)
                                Text(
                                    text = ligne.product.reference?.takeIf { it.isNotBlank() } ?: ligne.product.code,
                                    fontSize = 10.5.sp,
                                    color = MissaMuted,
                                )
                            }
                            BadgeStatutEquipement(ligne.equipement)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}
