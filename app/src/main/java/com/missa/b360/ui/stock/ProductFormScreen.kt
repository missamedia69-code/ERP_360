package com.missa.b360.ui.stock

import com.missa.b360.ui.navigation.AppModule

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.missa.b360.ui.theme.Red40
import com.missa.b360.R
import com.missa.b360.core.data.entity.ProductType
import com.missa.b360.core.domain.usecase.ProductInput
import com.missa.b360.ui.components.MissaTopAppBar
import com.missa.b360.ui.theme.Blue90
import com.missa.b360.ui.theme.BrandBlue
import com.missa.b360.ui.theme.MissaBorder
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted
import com.missa.b360.ui.theme.MissaSurface

/** Maquette 11 — nouvel article : grille de types puis infos générales, prix & seuils. */
@Composable
fun ProductFormScreen(onBack: () -> Unit, productId: Long? = null) {
    val vm: ProductFormViewModel = hiltViewModel()
    val categories by vm.categories.collectAsStateWithLifecycle()
    val sites by vm.sites.collectAsStateWithLifecycle()
    val saveResult by vm.saveResult.collectAsStateWithLifecycle()
    val contexte = LocalContext.current
    val texteChampsRequis = stringResource(R.string.st_champs_requis)
    val texteImageEchec = stringResource(R.string.st_image_echec)
    var imageUri by remember { mutableStateOf<String?>(null) }
    var imageTemp by remember { mutableStateOf<String?>(null) }
    var supprimerImage by remember { mutableStateOf(false) }
    var aUneImage by remember { mutableStateOf(false) }
    var apercu by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let {
            imageUri = it.toString()
            supprimerImage = false
        }
    }

    var type by remember { mutableStateOf(ProductType.ACHATE_REVENDU) }
    var nom by remember { mutableStateOf("") }
    var reference by remember { mutableStateOf("") }
    var categorieId by remember { mutableStateOf<Long?>(null) }
    var marque by remember { mutableStateOf("") }
    var unite by remember { mutableStateOf("") }
    var prixAchat by remember { mutableStateOf("") }
    var prixVente by remember { mutableStateOf("") }
    var remiseMax by remember { mutableStateOf("") }
    var stockMin by remember { mutableStateOf("") }
    var stockMax by remember { mutableStateOf("") }
    var stockSecurite by remember { mutableStateOf("") }
    var stockInitial by remember { mutableStateOf("") }
    var siteId by remember { mutableStateOf<Long?>(null) }
    var modele by remember { mutableStateOf("") }
    var numeroSerie by remember { mutableStateOf("") }
    var dateAcquisition by remember { mutableStateOf("") }
    var responsable by remember { mutableStateOf("") }
    var garantieDebut by remember { mutableStateOf("") }
    var garantieFin by remember { mutableStateOf("") }
    var etape by remember { mutableStateOf(0) }
    var preRempli by remember { mutableStateOf(false) }

    LaunchedEffect(productId) {
        productId?.let { vm.load(it) }
    }
    val edit by vm.product.collectAsStateWithLifecycle()
    LaunchedEffect(imageUri, edit?.photoPath, supprimerImage) {
        if (imageUri != null) {
            val temp = com.missa.b360.core.util.ImageProduit.enregistrerTemp(
                contexte,
                android.net.Uri.parse(imageUri),
            )
            imageTemp = temp
            if (temp == null) {
                val cause = com.missa.b360.core.util.ImageProduit.derniereErreur
                Toast.makeText(
                    contexte,
                    if (cause == null) texteImageEchec else "$texteImageEchec ($cause)",
                    Toast.LENGTH_LONG,
                ).show()
            } else {
                apercu = android.graphics.BitmapFactory.decodeFile(temp)
            }
        } else {
            imageTemp = null
            val enEdition = edit
            apercu = if (enEdition?.photoPath != null && !supprimerImage) {
                com.missa.b360.core.util.ImageProduit.charger(contexte, enEdition.id)
            } else {
                null
            }
        }
    }
    LaunchedEffect(edit) {
        val p = edit ?: return@LaunchedEffect
        if (preRempli) return@LaunchedEffect
        preRempli = true
        type = p.type
        nom = p.nom
        reference = p.reference.orEmpty()
        categorieId = p.categorieId
        marque = p.marque.orEmpty()
        unite = p.unite.orEmpty()
        prixAchat = p.prixAchat?.toString().orEmpty()
        prixVente = p.prixVente?.toString().orEmpty()
        remiseMax = if (p.remiseMaxPct > 0) p.remiseMaxPct.toString() else ""
        stockMin = if (p.stockMin > 0) p.stockMin.toString() else ""
        stockMax = p.stockMax?.toString().orEmpty()
        stockSecurite = if (p.stockSecurite > 0) p.stockSecurite.toString() else ""
        siteId = p.siteId
        aUneImage = p.photoPath != null
    }

    saveResult?.let { r ->
        when (r) {
            is ProductFormViewModel.SaveResult.Saved -> {
                Toast.makeText(contexte, stringResource(R.string.st_article_enregistre), Toast.LENGTH_SHORT).show()
                vm.clearSaveResult()
                onBack()
            }
            else -> {
                Toast.makeText(contexte, texteChampsRequis, Toast.LENGTH_SHORT).show()
                vm.clearSaveResult()
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        MissaTopAppBar(
            title = stringResource(if (productId == null) R.string.st_nouvel_article else R.string.st_modifier),
            onBack = onBack,
            couleurFond = AppModule.STOCK.couleurPale,
        )
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(8.dp))
            if (etape == 0) {
                Text(stringResource(R.string.st_type_article), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MissaInk)
                Spacer(Modifier.height(8.dp))
                GrilleTypes(type) { type = it }
                Spacer(Modifier.height(14.dp))
                Text(stringResource(R.string.st_image_article), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MissaInk)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Blue90),
                        contentAlignment = Alignment.Center,
                    ) {
                        val image = apercu
                        if (image != null && !supprimerImage) {
                            Image(
                                bitmap = image.asImageBitmap(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            Icon(painterResource(type.icone()), null, tint = MissaInk, modifier = Modifier.size(28.dp))
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Button(
                            onClick = {
                                pickImage.launch(
                                    androidx.activity.result.PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly,
                                    ),
                                )
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        ) {
                            Text(
                                text = stringResource(
                                    if (apercu != null || aUneImage) R.string.st_modifier_image else R.string.st_ajouter_image,
                                ),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        if ((apercu != null || aUneImage) && !supprimerImage) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = stringResource(R.string.st_supprimer_image),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Red40,
                                modifier = Modifier.clickable {
                                    supprimerImage = true
                                    imageUri = null
                                    apercu = null
                                },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text(stringResource(R.string.st_infos_generales), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MissaInk)
                Spacer(Modifier.height(8.dp))
                Champ("${stringResource(R.string.st_nom_article)} *", nom) { nom = it }
                Spacer(Modifier.height(10.dp))
                Champ(stringResource(R.string.st_reference), reference) { reference = it }
                Spacer(Modifier.height(10.dp))
                DropdownChamp(
                    libelle = stringResource(R.string.st_categorie),
                    options = categories.map { it.id to it.nom },
                    selection = categorieId,
                    onSelection = { categorieId = it },
                    placeholder = stringResource(R.string.st_categorie),
                )
                Spacer(Modifier.height(10.dp))
                Champ(stringResource(R.string.st_marque), marque) { marque = it }
                Spacer(Modifier.height(10.dp))
                Champ("${stringResource(R.string.st_unite)} *", unite) { unite = it }
                if (TYPES_EQUIPEMENTS.contains(type)) {
                    Spacer(Modifier.height(10.dp))
                    Champ(stringResource(R.string.st_modele), modele) { modele = it }
                    Spacer(Modifier.height(10.dp))
                    Champ(stringResource(R.string.st_num_serie), numeroSerie) { numeroSerie = it }
                    Spacer(Modifier.height(10.dp))
                    Champ("${stringResource(R.string.st_date_acquisition)} (jj/mm/aaaa)", dateAcquisition) { dateAcquisition = it }
                    Spacer(Modifier.height(10.dp))
                    Champ(stringResource(R.string.st_responsable), responsable) { responsable = it }
                    Spacer(Modifier.height(10.dp))
                    Champ("${stringResource(R.string.st_garantie)} — ${stringResource(R.string.st_debut)} (jj/mm/aaaa)", garantieDebut) { garantieDebut = it }
                    Spacer(Modifier.height(10.dp))
                    Champ("${stringResource(R.string.st_garantie)} — ${stringResource(R.string.st_fin)} (jj/mm/aaaa)", garantieFin) { garantieFin = it }
                }
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        if (nom.isBlank()) {
                            Toast.makeText(contexte, texteChampsRequis, Toast.LENGTH_SHORT).show()
                        } else {
                            etape = 1
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                ) {
                    Text("${stringResource(R.string.st_suivant)} →", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Text(stringResource(R.string.st_prix_seuils), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MissaInk)
                Spacer(Modifier.height(8.dp))
                Champ(stringResource(R.string.st_prix_achat), prixAchat) { prixAchat = it }
                Spacer(Modifier.height(10.dp))
                Champ(stringResource(R.string.st_prix_vente), prixVente) { prixVente = it }
                Spacer(Modifier.height(10.dp))
                Champ("${stringResource(R.string.st_remise_max)} (%)", remiseMax) { remiseMax = it }
                Spacer(Modifier.height(10.dp))
                Champ(stringResource(R.string.st_minimum), stockMin) { stockMin = it }
                Spacer(Modifier.height(10.dp))
                Champ(stringResource(R.string.st_maximum), stockMax) { stockMax = it }
                Spacer(Modifier.height(10.dp))
                Champ(stringResource(R.string.st_securite), stockSecurite) { stockSecurite = it }
                Spacer(Modifier.height(10.dp))
                if (productId == null) {
                    Champ(stringResource(R.string.st_stock_initial), stockInitial) { stockInitial = it }
                    Spacer(Modifier.height(10.dp))
                    DropdownChamp(
                        libelle = stringResource(R.string.st_site),
                        options = sites.map { it.id to it.nom },
                        selection = siteId,
                        onSelection = { siteId = it },
                    )
                    Spacer(Modifier.height(16.dp))
                }
                Button(
                    onClick = {
                        val fmt = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                        val parse = { t: String -> runCatching { fmt.parse(t.trim())?.time }.getOrNull() }
                        val equipement = if (TYPES_EQUIPEMENTS.contains(type)) {
                            com.missa.b360.core.data.entity.ProductEquipementEntity(
                                produitId = 0L,
                                modele = modele.takeIf { it.isNotBlank() },
                                numeroSerie = numeroSerie.takeIf { it.isNotBlank() },
                                dateAcquisition = parse(dateAcquisition),
                                responsable = responsable.takeIf { it.isNotBlank() },
                                garantieDebut = parse(garantieDebut),
                                garantieFin = parse(garantieFin),
                            )
                        } else {
                            null
                        }
                        vm.save(
                            ProductInput(
                                nom = nom.trim(),
                                type = type,
                                reference = reference.takeIf { it.isNotBlank() },
                                categorieId = categorieId,
                                marque = marque.takeIf { it.isNotBlank() },
                                unite = unite.takeIf { it.isNotBlank() },
                                prixAchat = prixAchat.toDoubleOrNull(),
                                prixVente = prixVente.toDoubleOrNull(),
                                remiseMaxPct = remiseMax.toDoubleOrNull() ?: 0.0,
                                stockMin = stockMin.toDoubleOrNull() ?: 0.0,
                                stockMax = stockMax.toDoubleOrNull(),
                                stockSecurite = stockSecurite.toDoubleOrNull() ?: 0.0,
                                siteId = siteId,
                            ),
                            initialStock = stockInitial.toDoubleOrNull(),
                            equipement = equipement,
                            imageTemp = imageTemp,
                            supprimerImage = supprimerImage,
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                ) {
                    Text(stringResource(R.string.st_enregistrer), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun Champ(libelle: String, valeur: String, onValeur: (String) -> Unit) {
    OutlinedTextField(
        value = valeur,
        onValueChange = onValeur,
        label = { Text(libelle, fontSize = 11.sp, color = MissaMuted) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
    )
}

@Composable
private fun GrilleTypes(selection: ProductType, onSelection: (ProductType) -> Unit) {
    val lignes = TYPES_NOUVEL_ARTICLE.chunked(3)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        lignes.forEach { ligne ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ligne.forEach { t ->
                    TuileType(t, t == selection, Modifier.weight(1f)) { onSelection(t) }
                }
                repeat(3 - ligne.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun TuileType(type: ProductType, actif: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.then(Modifier.padding(0.dp)),
        shape = RoundedCornerShape(12.dp),
        color = if (actif) Blue90 else MissaSurface,
        border = BorderStroke(1.5.dp, if (actif) AppModule.STOCK.couleur else MissaBorder.copy(alpha = 0.6f)),
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                painterResource(type.icone()),
                null,
                tint = if (actif) MissaInk else MissaMuted,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(type.libelleTypeRes()),
                fontSize = 9.5.sp,
                fontWeight = if (actif) FontWeight.Bold else FontWeight.Medium,
                color = if (actif) MissaInk else MissaMuted,
                textAlign = TextAlign.Center,
            )
        }
    }
}
