package com.missa.b360.ui.stock

import com.missa.b360.ui.navigation.AppModule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.missa.b360.R
import com.missa.b360.ui.components.MissaTopAppBar
import com.missa.b360.ui.theme.Blue90
import com.missa.b360.ui.theme.BrandBlue
import com.missa.b360.ui.theme.Green60
import com.missa.b360.ui.theme.Green90
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted
import com.missa.b360.ui.theme.ProfileOrange
import com.missa.b360.ui.theme.Red40
import com.missa.b360.ui.theme.Red80

/**
 * Maquette 8 — Inventaire : session en cours (progression), comptage physique
 * par article, écarts détectés, clôture qui applique les ajustements au stock.
 */
@Composable
fun InventoryScreen(onBack: () -> Unit) {
    val vm: InventaireViewModel = hiltViewModel()
    val etat by vm.etat.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        MissaTopAppBar(title = stringResource(R.string.st_inventaire_titre), onBack = onBack)

        if (etat.session == null) {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Surface(shape = RoundedCornerShape(20.dp), color = Blue90, modifier = Modifier.size(64.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(painterResource(StockIv.Unarchive), null, tint = AppModule.STOCK.couleur, modifier = Modifier.size(30.dp))
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.st_aucun_inventaire), fontSize = 12.sp, color = MissaMuted, textAlign = TextAlign.Center)
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = vm::demarrer,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                ) {
                    Text(stringResource(R.string.st_demarrer_inventaire), fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            var onglet by remember { mutableStateOf(0) }
            var recherche by remember { mutableStateOf("") }
            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                Spacer(Modifier.height(8.dp))
                CarteStock {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = RoundedCornerShape(12.dp), color = Green90, modifier = Modifier.size(42.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(painterResource(StockIv.Inventory2), null, tint = AppModule.STOCK.couleur, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(Modifier.width(11.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.st_inventaire_en_cours), fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = MissaInk)
                            Text(
                                text = etat.siteNom ?: "",
                                fontSize = 10.5.sp,
                                color = MissaMuted,
                            )
                            Spacer(Modifier.height(5.dp))
                            LinearProgressIndicator(
                                progress = { if (etat.total == 0) 0f else etat.comptes.toFloat() / etat.total },
                                modifier = Modifier.fillMaxWidth().height(5.dp),
                                color = BrandBlue,
                                trackColor = Blue90,
                                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
                            )
                            Text(
                                text = "${etat.comptes}/${etat.total}",
                                fontSize = 10.sp,
                                color = MissaMuted,
                                modifier = Modifier.padding(top = 3.dp),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatTile(
                        icone = StockIv.CheckCircle,
                        teinte = AppModule.STOCK.couleur,
                        fond = Green90,
                        valeur = etat.comptes.toString(),
                        libelle = stringResource(R.string.st_articles_comptes),
                        modifier = Modifier.weight(1f),
                    )
                    StatTile(
                        icone = StockIv.Warning,
                        teinte = AppModule.STOCK.couleur,
                        fond = Color(0xFFFFF4E5),
                        valeur = etat.ecarts.size.toString(),
                        libelle = stringResource(R.string.st_ecarts_detectes),
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(12.dp))
                StockOnglets(
                    onglets = listOf(
                        stringResource(R.string.st_tab_general),
                        stringResource(R.string.st_tab_physique),
                        stringResource(R.string.st_tab_ecart),
                    ),
                    selection = onglet,
                    onSelection = { onglet = it },
                )
                Spacer(Modifier.height(10.dp))
                if (onglet == 1) {
                    OutlinedTextField(
                        value = recherche,
                        onValueChange = { recherche = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(stringResource(R.string.st_rechercher_article), fontSize = 12.sp, color = MissaMuted) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                }
                val lignes = etat.lignes.filter {
                    when (onglet) {
                        0 -> false
                        1 -> recherche.isBlank() || it.nom.contains(recherche, ignoreCase = true)
                        else -> it.ecart != null && it.ecart != 0.0
                    }
                }
                LazyColumn(modifier = Modifier.weight(1f)) {
                    if (onglet == 0) {
                        item {
                            CarteStock {
                                Text(
                                    text = stringResource(R.string.st_articles_comptes),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MissaInk,
                                )
                                Spacer(Modifier.height(6.dp))
                                LigneInfo(stringResource(R.string.st_attendu), etat.lignes.sumOf { it.attendu }.toString())
                                LigneInfo(
                                    stringResource(R.string.st_compte),
                                    etat.lignes.filter { it.compte != null }.sumOf { it.compte ?: 0.0 }.toString(),
                                )
                                LigneInfo(stringResource(R.string.st_ecarts_detectes), etat.ecarts.size.toString())
                            }
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = stringResource(R.string.st_ecarts_detectes),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MissaMuted,
                            )
                            Spacer(Modifier.height(6.dp))
                            if (etat.ecarts.isEmpty()) {
                                Text(stringResource(R.string.st_aucun_resultat), fontSize = 11.5.sp, color = MissaMuted)
                            } else {
                                etat.ecarts.forEach { ligne ->
                                    CarteStock(onClick = {}) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(ligne.nom, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = MissaInk, maxLines = 1)
                                                Text(
                                                    text = "${stringResource(R.string.st_attendu)} ${ligne.attendu} → ${stringResource(R.string.st_compte)} ${ligne.compte ?: 0}",
                                                    fontSize = 10.5.sp,
                                                    color = MissaMuted,
                                                )
                                            }
                                            val ecart = ligne.ecart ?: 0.0
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (ecart > 0) Green90 else Red80,
                                            ) {
                                                Text(
                                                    text = "${if (ecart > 0) "+" else ""}$ecart",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (ecart > 0) Green60 else Red40,
                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                                )
                                            }
                                        }
                                    }
                                    Spacer(Modifier.height(8.dp))
                                }
                            }
                        }
                    } else {
                        items(lignes, key = { it.produitId }) { ligne ->
                            CarteStock {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(ligne.nom, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = MissaInk, maxLines = 1)
                                        Text(
                                            text = "${ligne.reference} — ${stringResource(R.string.st_attendu)}: ${ligne.attendu}",
                                            fontSize = 10.5.sp,
                                            color = MissaMuted,
                                        )
                                    }
                                    if (onglet == 1) {
                                        var saisie by remember(ligne.produitId, ligne.compte) {
                                            mutableStateOf(ligne.compte?.toString() ?: "")
                                        }
                                        OutlinedTextField(
                                            value = saisie,
                                            onValueChange = {
                                                saisie = it
                                                vm.enregistrerCompte(ligne.produitId, it, ligne.attendu)
                                            },
                                            modifier = Modifier.width(96.dp),
                                            label = { Text(stringResource(R.string.st_compte), fontSize = 9.sp) },
                                            singleLine = true,
                                            shape = RoundedCornerShape(10.dp),
                                        )
                                    } else {
                                        val ecart = ligne.ecart ?: 0.0
                                        Text(
                                            text = "${if (ecart > 0) "+" else ""}$ecart",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (ecart > 0) Green60 else Red40,
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                    item { Spacer(Modifier.height(12.dp)) }
                }
                Button(
                    onClick = { vm.cloturer(onBack) },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                ) {
                    Text(stringResource(R.string.st_cloturer), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
