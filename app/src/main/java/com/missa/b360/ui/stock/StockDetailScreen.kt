package com.missa.b360.ui.stock

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.missa.b360.R
import com.missa.b360.ui.components.MissaTopAppBar
import com.missa.b360.ui.navigation.Routes
import com.missa.b360.ui.theme.Blue90
import com.missa.b360.ui.theme.BrandBlue
import com.missa.b360.ui.theme.Green60
import com.missa.b360.ui.theme.Green90
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted
import com.missa.b360.ui.theme.ProfileOrange

/** Maquette 4 — détail d'un article : en-tête, onglets, tuiles de stock, bouton Modifier. */
@Composable
fun StockDetailScreen(onBack: () -> Unit, onNavigate: (String) -> Unit = {}) {
    val vm: StockDetailViewModel = hiltViewModel()
    val etat by vm.etat.collectAsStateWithLifecycle()
    val categorieNom by vm.categorieNom.collectAsStateWithLifecycle()
    val produit = etat.product

    Column(modifier = Modifier.fillMaxSize()) {
        MissaTopAppBar(title = stringResource(R.string.st_detail_article), onBack = onBack)
        if (produit == null) {
            Box(Modifier.fillMaxSize())
            return
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            // En-tête article.
            CarteStock {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val photo = rememberPhotoProduit(produit.photoPath)
                    Surface(modifier = Modifier.size(56.dp), shape = RoundedCornerShape(14.dp), color = Blue90) {
                        Box(contentAlignment = Alignment.Center) {
                            if (photo != null) {
                                Image(
                                    bitmap = photo,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            } else {
                                Icon(produit.type.icone(), null, tint = BrandBlue, modifier = Modifier.size(26.dp))
                            }
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(produit.nom, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = MissaInk)
                        Text(
                            text = produit.reference?.takeIf { it.isNotBlank() } ?: produit.code,
                            fontSize = 11.sp,
                            color = MissaMuted,
                        )
                        Spacer(Modifier.height(4.dp))
                        Surface(shape = RoundedCornerShape(8.dp), color = Green90) {
                            Text(
                                text = stringResource(produit.type.libelleTypeRes()),
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Green60,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            var onglet by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(0) }
            StockOnglets(
                onglets = listOf(
                    stringResource(R.string.st_tab_general),
                    stringResource(R.string.st_tab_stock),
                    stringResource(R.string.st_tab_prix),
                    stringResource(R.string.st_tab_fournisseur),
                ),
                selection = onglet,
                onSelection = { onglet = it },
            )
            Spacer(Modifier.height(12.dp))
            when (onglet) {
                0 -> OngletGeneral(etat, produit, categorieNom)
                1 -> OngletStock(etat, produit)
                2 -> OngletPrix(etat, produit)
                else -> OngletFournisseur(etat)
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { onNavigate("${Routes.STOCK_PRODUCT_FORM}?productId=${produit.id}") },
                modifier = Modifier.fillMaxWidth().height(46.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
            ) {
                Icon(Icons.Outlined.Edit, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(7.dp))
                Text(stringResource(R.string.st_modifier), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun OngletGeneral(
    etat: StockDetailState,
    produit: com.missa.b360.core.data.entity.ProductEntity,
    categorieNom: String?,
) {
    CarteStock {
        Text(stringResource(R.string.st_infos_generales), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MissaInk)
        Spacer(Modifier.height(6.dp))
        LigneInfo(stringResource(R.string.st_marque), produit.marque)
        LigneInfo(stringResource(R.string.st_code_barres), produit.barcode)
        LigneInfo(stringResource(R.string.st_unite), produit.unite)
        LigneInfo(stringResource(R.string.st_emplacement), produit.emplacement)
        LigneInfo(stringResource(R.string.st_categorie), categorieNom)
    }
}

@Composable
private fun OngletStock(
    etat: StockDetailState,
    produit: com.missa.b360.core.data.entity.ProductEntity,
) {
    val total = etat.stocks.sumOf { it.quantite }.coerceAtLeast(0.0)
    CarteStock {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TuileStock(stringResource(R.string.st_disponible), fmtQuantite(total), MissaInk, Modifier.weight(1f))
            TuileStock(stringResource(R.string.st_minimum), fmtQuantite(produit.stockMin), ProfileOrange, Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TuileStock(stringResource(R.string.st_maximum), produit.stockMax?.let { fmtQuantite(it) } ?: "—", MissaInk, Modifier.weight(1f))
            TuileStock(stringResource(R.string.st_securite), fmtQuantite(produit.stockSecurite), Green60, Modifier.weight(1f))
        }
        if (etat.stocks.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            etat.stocks.forEach { ligne ->
                LigneInfo(
                    libelle = etat.sites.firstOrNull { it.id == ligne.siteId }?.nom ?: stringResource(R.string.st_site),
                    valeur = fmtQuantite(ligne.quantite),
                )
            }
        }
    }
}

@Composable
private fun TuileStock(libelle: String, valeur: String, teinte: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(10.dp), color = com.missa.b360.ui.theme.MissaCanvas) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(libelle, fontSize = 10.sp, color = MissaMuted)
            Spacer(Modifier.height(2.dp))
            Text(valeur, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = teinte)
        }
    }
}

@Composable
private fun OngletPrix(
    etat: StockDetailState,
    produit: com.missa.b360.core.data.entity.ProductEntity,
) {
    val achat = produit.prixAchat
    val vente = produit.prixVente
    val marge = if (achat != null && vente != null && achat > 0) (vente - achat) / achat * 100 else null
    CarteStock {
        Text(stringResource(R.string.st_prix), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MissaInk)
        Spacer(Modifier.height(6.dp))
        LigneInfo(stringResource(R.string.st_prix_achat), achat?.let { fmtValeur(it, etat.devise) })
        LigneInfo(stringResource(R.string.st_prix_vente), vente?.let { fmtValeur(it, etat.devise) })
        LigneInfo(stringResource(R.string.st_marge), marge?.let { "%.1f %%".format(it) })
        LigneInfo(stringResource(R.string.st_remise_max), "%.0f %%".format(produit.remiseMaxPct))
    }
}

@Composable
private fun OngletFournisseur(etat: StockDetailState) {
    val f = etat.fournisseur
    CarteStock {
        LigneInfo(stringResource(R.string.st_fournisseur), f?.nom)
        LigneInfo(stringResource(R.string.st_reference), etat.product?.refFournisseur)
    }
}
