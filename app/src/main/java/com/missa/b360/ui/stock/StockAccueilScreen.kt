package com.missa.b360.ui.stock

import androidx.compose.runtime.setValue

import androidx.compose.runtime.remember

import androidx.compose.runtime.mutableStateOf

import androidx.compose.material3.OutlinedTextField

import androidx.compose.material3.TextButton

import androidx.compose.material3.AlertDialog

import androidx.compose.ui.text.style.TextAlign

import androidx.compose.ui.text.style.TextOverflow

import com.missa.b360.ui.theme.MissaBorder

import androidx.compose.foundation.BorderStroke

import com.missa.b360.ui.navigation.AppModule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.missa.b360.R
import com.missa.b360.ui.navigation.Routes
import com.missa.b360.ui.theme.Blue90
import com.missa.b360.ui.theme.Green60
import com.missa.b360.ui.theme.MissaCanvas
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted
import com.missa.b360.ui.theme.Red40
import com.missa.b360.ui.theme.Red80
import kotlin.math.max
import kotlin.math.round

/** Maquette 1 — accueil du module Stock : catégories, tableau de bord, inventaire. */
@Composable
fun StockAccueilScreen(onBack: () -> Unit, onNaviguer: (String) -> Unit = {}) {
    val vm: StockAccueilViewModel = hiltViewModel()
    val etat by vm.etat.collectAsStateWithLifecycle()
    var dialogueCategorie by remember { mutableStateOf(false) }

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MissaCanvas)
            .windowInsetsPadding(WindowInsets.statusBars)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        // Header du module : bandeau légèrement teinté de la couleur Stock.
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = AppModule.STOCK.couleurPale,
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Text(text = stringResource(R.string.module_stock), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = MissaInk)
                Text(text = stringResource(R.string.st_sous_titre), fontSize = 11.5.sp, color = MissaMuted)
            }
        }
        Spacer(Modifier.height(12.dp))

        // Catégories en petits onglets matriciels : accès direct aux listes.
        StockSectionTitle(
            titre = stringResource(R.string.st_categories_titre),
            action = stringResource(R.string.st_voir_tout),
            onAction = { onNaviguer(Routes.STOCK_CATEGORIES) },
        )
        // Matrice unifiée : types d'articles + catégories utilisateur + création.
        val tuiles = buildList {
            etat.categories.forEach { cat ->
                add(
                    TuileCatSpec(
                        icone = cat.type.icone(),
                        nom = stringResource(cat.nomRes),
                        sous = stringResource(R.string.st_articles_count, cat.nombre),
                    ) {
                        onNaviguer(
                            if (TYPES_EQUIPEMENTS.contains(cat.type)) Routes.STOCK_EQUIPEMENTS
                            else Routes.stockListe(cat.type.name),
                        )
                    },
                )
            }
            etat.categoriesLibres.forEach { cat ->
                add(
                    TuileCatSpec(
                        icone = StockIv.Category,
                        nom = cat.nom,
                        sous = stringResource(R.string.st_articles_count, cat.nombre),
                    ) { onNaviguer(Routes.stockListe(null, cat.id)) },
                )
            }
            add(
                TuileCatSpec(
                    icone = StockIv.Add,
                    nom = stringResource(R.string.st_nouvelle_categorie),
                    sous = stringResource(R.string.st_creer),
                ) { dialogueCategorie = true },
            )
        }
        tuiles.chunked(4).forEach { ligneTuiles ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ligneTuiles.forEach { tuile ->
                    TuileCategorieMatrice(
                        icone = tuile.icone,
                        nom = tuile.nom,
                        nombre = tuile.sous,
                        modifier = Modifier.weight(1f),
                        onClick = tuile.clic,
                    )
                }
                repeat(4 - ligneTuiles.size) { Spacer(Modifier.weight(1f)) }
            }
            Spacer(Modifier.height(8.dp))
        }
        Spacer(Modifier.height(6.dp))

        // ——— Tableau de bord : valorisation, flux, alertes et activité regroupés. ———
        StockSectionTitle(titre = stringResource(R.string.st_tableau_bord))

        // Carte héro : valeur du stock, tendance et actions rapides.
        CarteStock(onClick = { onNaviguer(Routes.stockListe(null)) }) {
            Column {
                Text(stringResource(R.string.st_valeur_totale), fontSize = 11.sp, color = MissaMuted)
                Spacer(Modifier.height(2.dp))
                Text(
                    text = fmtValeur(etat.valeur, etat.devise),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MissaInk,
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    etat.tendance?.let { t ->
                        Icon(
                            painterResource(if (t >= 0) StockIv.TrendingUp else StockIv.TrendingDown),
                            null,
                            tint = MissaInk,
                            modifier = Modifier.size(13.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.st_tendance, "%+.1f %%".format(t * 100)),
                            fontSize = 10.5.sp,
                            color = if (t >= 0) Green60 else Red40,
                        )
                        Spacer(Modifier.width(10.dp))
                    }
                    Text(
                        text = stringResource(R.string.st_resume_hero, etat.nbArticles, etat.nbCategories),
                        fontSize = 10.5.sp,
                        color = MissaMuted,
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        onClick = { onNaviguer(Routes.stockProductForm()) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = MissaInk,
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 9.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(painterResource(StockIv.Add), null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.st_article), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                    Surface(
                        onClick = { onNaviguer(Routes.STOCK_MOVEMENT_FORM) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = MissaCanvas,
                        border = BorderStroke(1.dp, MissaBorder),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 9.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(painterResource(StockIv.Sync), null, tint = MissaInk, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.st_mouvement), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MissaInk)
                        }
                    }
                }
            }
        }

        // Grille 2×2 : flux 30 jours et alertes.
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            KpiDash(
                icone = StockIv.TrendingUp,
                libelle = stringResource(R.string.st_entrees),
                valeur = groupe(round(etat.entrees30j).toLong()),
                sous = stringResource(R.string.st_30_jours),
                fond = AppModule.STOCK.couleurPale,
                modifier = Modifier.weight(1f),
                onClick = { onNaviguer(Routes.STOCK_MOUVEMENTS) },
            )
            KpiDash(
                icone = StockIv.TrendingDown,
                libelle = stringResource(R.string.st_sorties),
                valeur = groupe(round(etat.sorties30j).toLong()),
                sous = stringResource(R.string.st_30_jours),
                fond = Color.White,
                modifier = Modifier.weight(1f),
                onClick = { onNaviguer(Routes.STOCK_MOUVEMENTS) },
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            KpiDash(
                icone = StockIv.Warning,
                libelle = stringResource(R.string.st_stock_critique),
                valeur = etat.critiques.toString(),
                sous = null,
                fond = Color(0xFFFFF4E5),
                modifier = Modifier.weight(1f),
                onClick = { onNaviguer(Routes.STOCK_ALERTES) },
            )
            KpiDash(
                icone = StockIv.Error,
                libelle = stringResource(R.string.st_ruptures),
                valeur = etat.ruptures.toString(),
                sous = null,
                fond = Red80,
                modifier = Modifier.weight(1f),
                onClick = { onNaviguer(Routes.STOCK_ALERTES) },
            )
        }

        // Valeur par catégorie : les trois catégories les plus valorisées.
        if (etat.topCategories.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            StockSectionTitle(titre = stringResource(R.string.st_valeur_par_categorie))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                etat.topCategories.forEach { cat ->
                    val pct = if (etat.valeur > 0) round(cat.valeur / etat.valeur * 100).toInt() else 0
                    Surface(
                        onClick = {
                            onNaviguer(
                                if (TYPES_EQUIPEMENTS.contains(cat.type)) Routes.STOCK_EQUIPEMENTS
                                else Routes.stockListe(cat.type.name),
                            )
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, MissaBorder),
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(painterResource(cat.type.icone()), null, tint = MissaInk, modifier = Modifier.size(13.dp))
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    text = stringResource(cat.nomRes),
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MissaInk,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            Spacer(Modifier.height(5.dp))
                            Text(
                                text = fmtValeur(cat.valeur, etat.devise),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MissaInk,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = stringResource(R.string.st_resume_cat, cat.nombre, pct),
                                fontSize = 9.sp,
                                color = MissaMuted,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }

        // Activité : barres empilées entrées/sorties sur 6 mois.
        Spacer(Modifier.height(14.dp))
        StockSectionTitle(titre = stringResource(R.string.st_activite_mois))
        CarteStock(onClick = { onNaviguer(Routes.STOCK_MOUVEMENTS) }) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LegendePoint(Green60, stringResource(R.string.st_entrees))
                    Spacer(Modifier.width(12.dp))
                    LegendePoint(Red40, stringResource(R.string.st_sorties))
                }
                Spacer(Modifier.height(8.dp))
                val maxTotal = max(0.0001, etat.activite.maxOfOrNull { it.entrees + it.sorties } ?: 0.0)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    etat.activite.forEach { mois ->
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Column(
                                modifier = Modifier.height(96.dp).fillMaxWidth().clip(RoundedCornerShape(5.dp)),
                                verticalArrangement = Arrangement.Bottom,
                            ) {
                                val reste = maxTotal - (mois.entrees + mois.sorties)
                                if (reste > 0) Spacer(Modifier.weight(reste.toFloat()))
                                if (mois.entrees > 0) {
                                    Box(
                                        Modifier
                                            .weight(max(0.06f, (mois.entrees / maxTotal).toFloat()))
                                            .fillMaxWidth()
                                            .background(Green60),
                                    )
                                }
                                if (mois.sorties > 0) {
                                    Box(
                                        Modifier
                                            .weight(max(0.06f, (mois.sorties / maxTotal).toFloat()))
                                            .fillMaxWidth()
                                            .background(Red40),
                                    )
                                }
                                if (mois.entrees <= 0 && mois.sorties <= 0) {
                                    Box(Modifier.weight(0.06f).fillMaxWidth().background(MissaBorder))
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(mois.label, fontSize = 8.5.sp, color = MissaMuted, maxLines = 1)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.st_resume_jour, etat.entreesJour, etat.sortiesJour, etat.transfertsJour),
                    fontSize = 10.sp,
                    color = MissaMuted,
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        // Accès inventaire physique.
        CarteStock(onClick = { onNaviguer(Routes.STOCK_INVENTORY) }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(modifier = Modifier.size(44.dp), shape = RoundedCornerShape(12.dp), color = Blue90) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(painterResource(StockIv.ClipboardText), null, tint = MissaInk, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(Modifier.width(11.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.st_inventaire_titre), fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = MissaInk)
                    Text(stringResource(R.string.st_demarrer_inventaire), fontSize = 10.5.sp, color = MissaMuted)
                }
                Icon(painterResource(StockIv.ChevronRight), null, tint = MissaInk, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

/** Pastille de légende du graphique d'activité. */
@Composable
private fun LegendePoint(couleur: Color, libelle: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).background(couleur, RoundedCornerShape(2.dp)))
        Spacer(Modifier.width(4.dp))
        Text(libelle, fontSize = 10.sp, color = MissaMuted)
    }
}

/** Tuile KPI du tableau de bord : libellé, valeur, sous-titre et icône. */
@Composable
private fun KpiDash(
    icone: Int,
    libelle: String,
    valeur: String,
    sous: String?,
    fond: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = fond,
        border = BorderStroke(1.dp, MissaBorder),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(libelle, fontSize = 11.sp, color = MissaMuted, modifier = Modifier.weight(1f))
                Surface(modifier = Modifier.size(22.dp), shape = RoundedCornerShape(8.dp), color = MissaCanvas) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(painterResource(icone), null, tint = MissaInk, modifier = Modifier.size(12.dp))
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(valeur, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = MissaInk)
            if (sous != null) {
                Text(sous, fontSize = 9.5.sp, color = MissaMuted)
            }
        }
    }
}

/** Petit onglet matriciel d'une catégorie : icône, nom et nombre d'articles. */
@Composable
private fun TuileCategorieMatrice(
    icone: Int,
    nom: String,
    nombre: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, MissaBorder),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 9.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                modifier = Modifier.size(30.dp),
                shape = RoundedCornerShape(9.dp),
                color = AppModule.STOCK.couleurDouce,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(painterResource(icone), null, tint = MissaInk, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(Modifier.height(5.dp))
            Text(
                text = nom,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                color = MissaInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            Text(text = nombre, fontSize = 9.sp, color = MissaMuted, maxLines = 1)
        }
    }
}

/** Spécification d'une tuile de la matrice des catégories. */
private class TuileCatSpec(
    val icone: Int,
    val nom: String,
    val sous: String,
    val clic: () -> Unit,
)
