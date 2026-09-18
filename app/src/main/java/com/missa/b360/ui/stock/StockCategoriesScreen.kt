package com.missa.b360.ui.stock

import androidx.compose.runtime.setValue

import androidx.compose.runtime.remember

import androidx.compose.runtime.mutableStateOf

import androidx.compose.material3.OutlinedTextField

import androidx.compose.material3.TextButton

import androidx.compose.material3.AlertDialog

import com.missa.b360.ui.navigation.AppModule

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import android.widget.Toast
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.missa.b360.R
import com.missa.b360.core.data.entity.ProductType
import com.missa.b360.core.domain.usecase.CategorieProduitUseCases
import com.missa.b360.ui.components.MissaTopAppBar
import com.missa.b360.ui.navigation.Routes
import com.missa.b360.ui.theme.Blue90
import com.missa.b360.ui.theme.BrandBlue
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted

/** Maquette 2 — toutes les catégories de stock (types d'article) avec compteurs et valeurs. */
@Composable
fun StockCategoriesScreen(onBack: () -> Unit, onNaviguer: (String) -> Unit = {}) {
    val vm: StockAccueilViewModel = hiltViewModel()
    val etat by vm.etat.collectAsStateWithLifecycle()
    var dialogueCategorie by remember { mutableStateOf(false) }
    val contexte = androidx.compose.ui.platform.LocalContext.current
    val suppCat by vm.suppressionCategorie.collectAsStateWithLifecycle()

    suppCat?.let { r ->
        when (r) {
            is CategorieProduitUseCases.SuppressionResult.CategorieUtilisee ->
                Toast.makeText(contexte, stringResource(R.string.st_categorie_utilisee), Toast.LENGTH_LONG).show()
            is CategorieProduitUseCases.SuppressionResult.LectureSeule ->
                Toast.makeText(contexte, stringResource(R.string.clients_lecture_seule), Toast.LENGTH_LONG).show()
            else -> Unit
        }
        vm.clearSuppressionCategorie()
    }

    if (dialogueCategorie) {
        var nomCategorie by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { dialogueCategorie = false },
            confirmButton = {
                TextButton(onClick = {
                    if (nomCategorie.isNotBlank()) {
                        vm.creerCategorie(nomCategorie)
                        dialogueCategorie = false
                    }
                }) { Text(stringResource(R.string.st_creer)) }
            },
            dismissButton = {
                TextButton(onClick = { dialogueCategorie = false }) {
                    Text(stringResource(R.string.st_annuler))
                }
            },
            title = { Text(stringResource(R.string.st_nouvelle_categorie)) },
            text = {
                OutlinedTextField(
                    value = nomCategorie,
                    onValueChange = { nomCategorie = it },
                    label = { Text(stringResource(R.string.st_nom_categorie)) },
                    singleLine = true,
                )
            },
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        MissaTopAppBar(title = stringResource(R.string.st_categories_titre), onBack = onBack, couleurFond = AppModule.STOCK.couleurPale)
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        ) {
            item { Spacer(Modifier.height(8.dp)) }
            items(ProductType.entries.toList(), key = { it.name }) { type ->
                val ligne = etat.categories.firstOrNull { it.type == type }
                CarteStock(onClick = { onNaviguer(if (TYPES_EQUIPEMENTS.contains(type)) Routes.STOCK_EQUIPEMENTS else Routes.stockListe(type.name)) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(modifier = Modifier.size(38.dp), shape = RoundedCornerShape(11.dp), color = Blue90) {
                            androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                                Icon(painterResource(type.icone()), null, tint = MissaInk, modifier = Modifier.size(19.dp))
                            }
                        }
                        Spacer(Modifier.width(11.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(type.libelleCatRes()),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MissaInk,
                            )
                            Text(
                                text = stringResource(R.string.st_articles_count, ligne?.nombre ?: 0),
                                fontSize = 10.5.sp,
                                color = MissaMuted,
                            )
                        }
                        Text(
                            text = fmtValeur(ligne?.valeur ?: 0.0, etat.devise),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MissaMuted,
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            painterResource(StockIv.ChevronRight),
                            null,
                            tint = MissaInk,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            // Catégories créées par l'utilisateur.
            item {
                StockSectionTitle(titre = stringResource(R.string.st_categories_perso))
            }
            items(etat.categoriesLibres, key = { "libre_" + it.id }) { cat ->
                CarteStock(onClick = { onNaviguer(Routes.stockListe(null, cat.id)) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(modifier = Modifier.size(38.dp), shape = RoundedCornerShape(11.dp), color = Blue90) {
                            androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                                Icon(painterResource(StockIv.Category), null, tint = MissaInk, modifier = Modifier.size(19.dp))
                            }
                        }
                        Spacer(Modifier.width(11.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = cat.nom, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MissaInk)
                            Text(text = stringResource(R.string.st_articles_count, cat.nombre), fontSize = 10.5.sp, color = MissaMuted)
                        }
                        IconButton(onClick = { vm.supprimerCategorie(cat.id) }, modifier = Modifier.size(40.dp)) {
                            Icon(painterResource(StockIv.Trash), null, tint = MissaInk, modifier = Modifier.size(16.dp))
                        }
                        Icon(painterResource(StockIv.ChevronRight), null, tint = MissaInk, modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            item {
                CarteStock(onClick = { dialogueCategorie = true }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(modifier = Modifier.size(38.dp), shape = RoundedCornerShape(11.dp), color = Blue90) {
                            androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                                Icon(painterResource(StockIv.Add), null, tint = MissaInk, modifier = Modifier.size(19.dp))
                            }
                        }
                        Spacer(Modifier.width(11.dp))
                        Text(text = stringResource(R.string.st_nouvelle_categorie), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MissaInk)
                    }
                }
            }
            item { Spacer(Modifier.height(12.dp)) }
        }
    }
}
