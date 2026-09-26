package com.missa.b360.ui.stock

import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import com.missa.b360.ui.components.MissaMenuDeroulant
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.missa.b360.R
import com.missa.b360.core.data.entity.ProductType
import com.missa.b360.ui.theme.BrandBlue
import com.missa.b360.ui.theme.MissaBorder
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted
import com.missa.b360.ui.theme.MissaSurface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Groupement manuel des milliers (« 18 450 000 ») sans dépendre de la locale. */
fun groupe(entier: Long): String {
    val negatif = entier < 0
    val chiffres = kotlin.math.abs(entier).toString()
    val sb = StringBuilder()
    chiffres.forEachIndexed { i, c ->
        val restants = chiffres.length - i
        if (i > 0 && restants % 3 == 0) sb.append(' ')
        sb.append(c)
    }
    return if (negatif) "-$sb" else sb.toString()
}

fun fmtValeur(v: Double, devise: String): String = "${groupe(kotlin.math.round(v).toLong())} $devise"

fun fmtQuantite(d: Double): String =
    if (d == kotlin.math.floor(d) && !d.isInfinite()) groupe(d.toLong()) else "%.1f".format(d)

/** Libellé pluriel d'un type d'article — les « catégories de stock » de la maquette. */
fun ProductType.libelleCatRes(): Int = when (this) {
    ProductType.ACHATE_REVENDU -> R.string.st_cat_marchandises
    ProductType.MATIERE_PREMIERE -> R.string.st_cat_matieres
    ProductType.CONNOMMABLE -> R.string.st_cat_consommables
    ProductType.PIECE_MAINTENANCE -> R.string.st_cat_pieces
    ProductType.EQUIPEMENT -> R.string.st_cat_equipements
    ProductType.MATERIEL -> R.string.st_cat_materiels
    ProductType.FABRIQUE -> R.string.st_cat_fabriques
    ProductType.COMPOSE -> R.string.st_cat_composes
    ProductType.AUTRE_BIEN -> R.string.st_cat_autres
    ProductType.PRESTATION -> R.string.st_cat_prestations
    ProductType.SEMI_FINI -> R.string.st_cat_semi_finis
    ProductType.EMBALLAGE -> R.string.st_cat_emballages
    ProductType.DECHET_VALORISABLE -> R.string.st_cat_dechets_val
    ProductType.DECHET_NON_VALORISABLE -> R.string.st_cat_dechets_nval
    ProductType.KIT -> R.string.st_cat_kits
    ProductType.CONSIGNATION -> R.string.st_cat_consignations
}

/** Libellé singulier d'un type d'article (grille « Type d'article »). */
fun ProductType.libelleTypeRes(): Int = when (this) {
    ProductType.ACHATE_REVENDU -> R.string.st_type_marchandise
    ProductType.MATIERE_PREMIERE -> R.string.st_type_matiere
    ProductType.CONNOMMABLE -> R.string.st_type_consommable
    ProductType.PIECE_MAINTENANCE -> R.string.st_type_piece
    ProductType.EQUIPEMENT -> R.string.st_type_equipement
    ProductType.MATERIEL -> R.string.st_type_materiel
    ProductType.FABRIQUE -> R.string.st_type_fabrique
    ProductType.COMPOSE -> R.string.st_type_compose
    ProductType.AUTRE_BIEN -> R.string.st_type_autre
    ProductType.PRESTATION -> R.string.st_type_prestation
    ProductType.SEMI_FINI -> R.string.st_type_semi_fini
    ProductType.EMBALLAGE -> R.string.st_type_emballage
    ProductType.DECHET_VALORISABLE -> R.string.st_type_dechet_val
    ProductType.DECHET_NON_VALORISABLE -> R.string.st_type_dechet_nval
    ProductType.KIT -> R.string.st_type_kit
    ProductType.CONSIGNATION -> R.string.st_type_consignation
}

fun ProductType.icone(): Int = when (this) {
    ProductType.ACHATE_REVENDU -> StockIv.ShoppingCart
    ProductType.MATIERE_PREMIERE -> StockIv.Eco
    ProductType.CONNOMMABLE -> StockIv.Inventory2
    ProductType.PIECE_MAINTENANCE -> StockIv.Build
    ProductType.EQUIPEMENT -> StockIv.Settings
    ProductType.MATERIEL -> StockIv.Monitor
    ProductType.FABRIQUE -> StockIv.Factory
    ProductType.COMPOSE -> StockIv.BuildCircle
    ProductType.AUTRE_BIEN -> StockIv.Category
    ProductType.PRESTATION -> StockIv.Handshake
    ProductType.SEMI_FINI -> StockIv.Hammer
    ProductType.EMBALLAGE -> StockIv.Inventory2
    ProductType.DECHET_VALORISABLE -> StockIv.Trash
    ProductType.DECHET_NON_VALORISABLE -> StockIv.Warning
    ProductType.KIT -> StockIv.Kanban
    ProductType.CONSIGNATION -> StockIv.Warehouse
}

/** Types proposés dans le formulaire « Nouvel article » (maquette 11). */
val TYPES_NOUVEL_ARTICLE = listOf(
    ProductType.ACHATE_REVENDU,
    ProductType.MATIERE_PREMIERE,
    ProductType.FABRIQUE,
    ProductType.SEMI_FINI,
    ProductType.COMPOSE,
    ProductType.KIT,
    ProductType.CONNOMMABLE,
    ProductType.EMBALLAGE,
    ProductType.PIECE_MAINTENANCE,
    ProductType.EQUIPEMENT,
    ProductType.MATERIEL,
    ProductType.PRESTATION,
    ProductType.DECHET_VALORISABLE,
    ProductType.DECHET_NON_VALORISABLE,
    ProductType.CONSIGNATION,
)

/** Types « immobilisations » listés dans Équipements (maquettes 5-6). */
val TYPES_EQUIPEMENTS = listOf(
    ProductType.EQUIPEMENT,
    ProductType.MATERIEL,
    ProductType.PIECE_MAINTENANCE,
)

/** Carte blanche standard du module Stock. */
@Composable
fun CarteStock(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(16.dp),
        color = MissaSurface,
        border = BorderStroke(1.dp, MissaBorder.copy(alpha = 0.6f)),
    ) {
        Column(modifier = Modifier.padding(14.dp), content = content)
    }
}

/** Petit titre de section avec action optionnelle (« Voir tout »). */
@Composable
fun StockSectionTitle(titre: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = titre,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MissaInk,
            modifier = Modifier.weight(1f),
        )
        if (action != null && onAction != null) {
            Text(
                text = action,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = BrandBlue,
                modifier = Modifier.clickable(onClick = onAction).padding(4.dp),
            )
        }
    }
}

/** Tuile statistique 2×2 de l'accueil (valeur + libellé + icône teintée). */
@Composable
fun StatTile(
    icone: Int,
    teinte: Color,
    fond: Color,
    valeur: String,
    libelle: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Surface(
        modifier = modifier.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(14.dp),
        color = MissaSurface,
        border = BorderStroke(1.dp, MissaBorder.copy(alpha = 0.6f)),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(modifier = Modifier.size(34.dp), shape = RoundedCornerShape(10.dp), color = fond) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(painterResource(icone), contentDescription = null, tint = teinte, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(text = valeur, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = MissaInk)
                Text(text = libelle, fontSize = 10.5.sp, color = MissaMuted)
            }
        }
    }
}

/** Chip de filtre (Tous / catégories / statuts). */
@Composable
fun StockChip(texte: String, actif: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = if (actif) BrandBlue else MissaSurface,
        border = if (actif) null else BorderStroke(1.dp, MissaBorder.copy(alpha = 0.6f)),
    ) {
        Text(
            text = texte,
            fontSize = 11.sp,
            fontWeight = if (actif) FontWeight.Bold else FontWeight.Medium,
            color = if (actif) Color.White else MissaMuted,
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp),
        )
    }
}

/** Champ de recherche standard du module. */
@Composable
fun StockSearchField(valeur: String, onValeur: (String) -> Unit, placeholderRes: Int) {
    OutlinedTextField(
        value = valeur,
        onValueChange = onValeur,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(stringResource(placeholderRes), fontSize = 12.sp, color = MissaMuted) },
        leadingIcon = { Icon(painterResource(StockIv.Search), null, tint = MissaMuted, modifier = Modifier.size(18.dp)) },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
    )
}

/** Ligne « Libellé ......... valeur » des cartes détail. */
@Composable
fun LigneInfo(libelle: String, valeur: String?) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = libelle, fontSize = 11.5.sp, color = MissaMuted, modifier = Modifier.weight(1f))
        Text(
            text = valeur?.takeIf { it.isNotBlank() } ?: stringResource(R.string.fiche_non_renseigne),
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (valeur.isNullOrBlank()) MissaMuted else MissaInk,
        )
    }
}

/** Décode une photo locale de produit (chemin de fichier) en taille réduite. */
@Composable
fun rememberPhotoProduit(photoPath: String?): ImageBitmap? {
    var bitmap by remember(photoPath) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(photoPath) {
        bitmap = photoPath?.let { chemin ->
            withContext(Dispatchers.IO) {
                runCatching {
                    val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
                    BitmapFactory.decodeFile(chemin, opts)?.asImageBitmap()
                }.getOrNull()
            }
        }
    }
    return bitmap
}

/** Barre d'onglets horizontale légère du module. */
@Composable
fun StockOnglets(onglets: List<String>, selection: Int, onSelection: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        onglets.forEachIndexed { index, titre ->
            val actif = index == selection
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = { onSelection(index) }),
                shape = RoundedCornerShape(10.dp),
                color = if (actif) BrandBlue else Color.Transparent,
                border = if (actif) null else BorderStroke(1.dp, MissaBorder.copy(alpha = 0.6f)),
            ) {
                Text(
                    text = titre,
                    fontSize = 11.sp,
                    fontWeight = if (actif) FontWeight.Bold else FontWeight.Medium,
                    color = if (actif) Color.White else MissaMuted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** Champ avec menu déroulant (article, site, catégorie…). */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

/** Sélecteur simple (champ + menu) pour les formulaires stock. */
@Composable
fun DropdownChamp(
    label: String,
    valeur: String,
    options: List<String>,
    onOption: (String) -> Unit,
    modifier: Modifier = Modifier,
    onNouveau: (() -> Unit)? = null,
    nouveauLibelle: String? = null,
) {
    // Un sélecteur sans options et sans action d'ajout ne s'affiche pas.
    if (options.isEmpty() && onNouveau == null) return
    var ouvert by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedTextField(
            value = valeur,
            onValueChange = {},
            readOnly = true,
            label = { Text(label, fontSize = 11.sp, color = MissaMuted) },
            placeholder = if (options.isEmpty() && onNouveau != null) {
                { Text("+ ${nouveauLibelle ?: label}", fontSize = 12.sp, color = MissaMuted) }
            } else null,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            trailingIcon = {
                Icon(painterResource(StockIv.ExpandMore), null, tint = MissaMuted, modifier = Modifier.size(20.dp))
            },
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable {
                    if (options.isEmpty() && onNouveau != null) {
                        onNouveau()
                    } else {
                        ouvert = true
                    }
                },
        )
        MissaMenuDeroulant(expanded = ouvert, onDismissRequest = { ouvert = false }) {
            if (onNouveau != null) {
                DropdownMenuItem(
                    text = {
                        Text(
                            "+ ${nouveauLibelle ?: label}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MissaInk,
                        )
                    },
                    onClick = {
                        ouvert = false
                        onNouveau()
                    },
                )
            }
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, fontSize = 12.sp, color = MissaInk) },
                    onClick = {
                        ouvert = false
                        onOption(option)
                    },
                )
            }
        }
    }
}

/** Variante avec identifiants : options = id → libellé affiché. */
@Composable
fun DropdownChamp(
    libelle: String,
    options: List<Pair<Long, String>>,
    selection: Long?,
    onSelection: (Long?) -> Unit = {},
    placeholder: String = libelle,
    modifier: Modifier = Modifier,
    onNouveau: (() -> Unit)? = null,
    nouveauLibelle: String? = null,
) {
    DropdownChamp(
        label = libelle,
        valeur = options.firstOrNull { it.first == selection }?.second ?: "",
        options = options.map { it.second },
        onOption = { texte -> onSelection(options.firstOrNull { it.second == texte }?.first) },
        modifier = modifier,
        onNouveau = onNouveau,
        nouveauLibelle = nouveauLibelle,
    )
}
