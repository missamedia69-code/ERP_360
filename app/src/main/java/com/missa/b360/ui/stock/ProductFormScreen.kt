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
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
fun ProductFormScreen(
    onBack: () -> Unit,
    productId: Long? = null,
    initialCategorieId: Long? = null,
    initialType: ProductType? = null,
) {
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

    var type by remember { mutableStateOf(initialType ?: ProductType.ACHATE_REVENDU) }
    var nom by remember { mutableStateOf("") }
    var reference by remember { mutableStateOf("") }
    var barcode by remember { mutableStateOf("") }
    var categorieId by remember { mutableStateOf(initialCategorieId) }
    var dialogueNouvelleCategorie by remember { mutableStateOf(false) }
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
    // Drapeaux surchargeant les règles du groupe (spec §14).
    var vendable by remember { mutableStateOf(initialType?.let { com.missa.b360.core.domain.model.ProduitRules.estVendable(it) } ?: true) }
    var achetable by remember { mutableStateOf(initialType?.let { com.missa.b360.core.domain.model.ProduitRules.estAchetable(it) } ?: true) }
    var stockable by remember { mutableStateOf(initialType?.let { com.missa.b360.core.domain.model.ProduitRules.estStockable(it) } ?: true) }
    val choisirType = { t: ProductType ->
        type = t
        vendable = com.missa.b360.core.domain.model.ProduitRules.estVendable(t)
        achetable = com.missa.b360.core.domain.model.ProduitRules.estAchetable(t)
        stockable = com.missa.b360.core.domain.model.ProduitRules.estStockable(t)
    }
    // Extensions par famille (pré-remplies en édition).
    var dType by remember { mutableStateOf("") }
    var dCode by remember { mutableStateOf("") }
    var dDangereux by remember { mutableStateOf(false) }
    var dOrigine by remember { mutableStateOf("") }
    var dZone by remember { mutableStateOf("") }
    var dMode by remember { mutableStateOf("") }
    var dPrestataire by remember { mutableStateOf("") }
    var dCout by remember { mutableStateOf("") }
    var dFiliere by remember { mutableStateOf("") }
    var eType by remember { mutableStateOf("") }
    var eMatiere by remember { mutableStateOf("") }
    var eDims by remember { mutableStateOf("") }
    var ePoids by remember { mutableStateOf("") }
    var eCap by remember { mutableStateOf("") }
    var eReutil by remember { mutableStateOf(false) }
    var eConsigne by remember { mutableStateOf(false) }
    var eMax by remember { mutableStateOf("") }
    var cProprio by remember { mutableStateOf("") }
    var cRef by remember { mutableStateOf("") }
    var cDebut by remember { mutableStateOf("") }
    var cFin by remember { mutableStateOf("") }
    var cConditions by remember { mutableStateOf("") }
    var kMethode by remember { mutableStateOf("VIRTUEL") }
    var kComposants by remember { mutableStateOf(listOf<com.missa.b360.core.data.entity.KitComposantEntity>()) }
    var kSelection by remember { mutableStateOf<Long?>(null) }
    var kQuantite by remember { mutableStateOf("1") }
    val produitsVm by vm.produits.collectAsStateWithLifecycle()
    val dechetEdit by vm.dechet.collectAsStateWithLifecycle()
    val emballageEdit by vm.emballage.collectAsStateWithLifecycle()
    val consignationEdit by vm.consignation.collectAsStateWithLifecycle()
    val kitEdit by vm.kit.collectAsStateWithLifecycle()
    val composantsEdit by vm.composants.collectAsStateWithLifecycle()
    var preRempliExt by remember { mutableStateOf(false) }

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
        barcode = p.barcode.orEmpty()
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
        vendable = p.vendable
        achetable = p.achetable
        stockable = p.stockable
    }
    LaunchedEffect(dechetEdit, emballageEdit, consignationEdit, kitEdit, composantsEdit) {
        if (preRempliExt) return@LaunchedEffect
        preRempliExt = true
        dechetEdit?.let { d ->
            dType = d.typeDechet.orEmpty(); dCode = d.codeReglementaire.orEmpty()
            dDangereux = d.dangereux; dOrigine = d.origine.orEmpty(); dZone = d.zoneStockage.orEmpty()
            dMode = d.modeElimination.orEmpty(); dPrestataire = d.prestataire.orEmpty()
            dCout = d.coutElimination?.toString().orEmpty(); dFiliere = d.filiereRecyclage.orEmpty()
        }
        emballageEdit?.let { e ->
            eType = e.typeEmballage.orEmpty(); eMatiere = e.matiere.orEmpty(); eDims = e.dimensions.orEmpty()
            ePoids = e.poidsKg?.toString().orEmpty(); eCap = e.capacite?.toString().orEmpty()
            eReutil = e.reutilisable; eConsigne = e.consigne; eMax = e.reutilisationsMax?.toString().orEmpty()
        }
        consignationEdit?.let { c ->
            cProprio = c.proprietaire.orEmpty(); cRef = c.referenceContrat.orEmpty()
            val fmt = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
            cDebut = c.dateDebut?.let { fmt.format(java.util.Date(it)) }.orEmpty()
            cFin = c.dateFin?.let { fmt.format(java.util.Date(it)) }.orEmpty()
            cConditions = c.conditionsRetour.orEmpty()
        }
        kitEdit?.let { k ->
            kMethode = k.methode
            kComposants = composantsEdit
        }
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
                when {
                    // Édition : le type est l'identité de l'article (sections, extension
                    // équipement, listes) — il ne se change pas.
                    productId != null && edit == null -> Unit // chargement en cours
                    productId != null || initialType != null ->
                        BadgeVerrouille(type.icone(), stringResource(type.libelleTypeRes()))
                    else -> GrilleTypes(type) { choisirType(it) }
                }
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
                Champ(stringResource(R.string.st_code_barres), barcode) { barcode = it }
                Spacer(Modifier.height(10.dp))
                when {
                    // Création déjà dans une catégorie utilisateur : pas de sélecteur.
                    productId == null && initialCategorieId != null ->
                        BadgeVerrouille(
                            StockIv.Category,
                            categories.firstOrNull { it.id == initialCategorieId }?.nom
                                ?: stringResource(R.string.st_categorie),
                        )
                    else -> DropdownChamp(
                        libelle = stringResource(R.string.st_categorie),
                        options = categories.map { it.id to it.nom },
                        selection = categorieId,
                        onSelection = { categorieId = it },
                        placeholder = stringResource(R.string.st_categorie),
                        onNouveau = { dialogueNouvelleCategorie = true },
                        nouveauLibelle = stringResource(R.string.st_nouvelle_categorie_rapide),
                    )
                }
                if (productId == null && initialCategorieId == null && categories.isEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MissaSurface,
                        border = BorderStroke(1.dp, MissaBorder),
                        modifier = Modifier.fillMaxWidth().clickable { dialogueNouvelleCategorie = true },
                    ) {
                        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(painterResource(Iv.Add), null, tint = MissaInk, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                stringResource(R.string.st_creer_categorie_invite),
                                fontSize = 11.sp,
                                color = MissaInk,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text(stringResource(R.string.st_regles), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MissaInk)
                Spacer(Modifier.height(4.dp))
                Interrupteur(stringResource(R.string.st_vendable), vendable) { vendable = it }
                Interrupteur(stringResource(R.string.st_achetable), achetable) { achetable = it }
                Interrupteur(stringResource(R.string.st_stockable), stockable) { stockable = it }
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
                    ChampDate(stringResource(R.string.st_date_acquisition), dateAcquisition) { dateAcquisition = it }
                    Spacer(Modifier.height(10.dp))
                    Champ(stringResource(R.string.st_responsable), responsable) { responsable = it }
                    Spacer(Modifier.height(10.dp))
                    ChampDate("${stringResource(R.string.st_garantie)} — ${stringResource(R.string.st_debut)}", garantieDebut) { garantieDebut = it }
                    Spacer(Modifier.height(10.dp))
                    ChampDate("${stringResource(R.string.st_garantie)} — ${stringResource(R.string.st_fin)}", garantieFin) { garantieFin = it }
                }
                if (type == ProductType.DECHET_VALORISABLE || type == ProductType.DECHET_NON_VALORISABLE) {
                    Spacer(Modifier.height(14.dp))
                    Text(stringResource(R.string.st_donnees_dechet), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MissaInk)
                    Spacer(Modifier.height(8.dp))
                    Champ(stringResource(R.string.st_type_dechet), dType) { dType = it }
                    Spacer(Modifier.height(10.dp))
                    Champ(stringResource(R.string.st_code_dechet), dCode) { dCode = it }
                    Interrupteur(stringResource(R.string.st_dangereux), dDangereux) { dDangereux = it }
                    Champ(stringResource(R.string.st_origine_dechet), dOrigine) { dOrigine = it }
                    Spacer(Modifier.height(10.dp))
                    Champ(stringResource(R.string.st_zone_stockage), dZone) { dZone = it }
                    Spacer(Modifier.height(10.dp))
                    if (type == ProductType.DECHET_NON_VALORISABLE) {
                        Champ(stringResource(R.string.st_mode_elimination), dMode) { dMode = it }
                        Spacer(Modifier.height(10.dp))
                        Champ(stringResource(R.string.st_cout_elimination), dCout) { dCout = it }
                    } else {
                        Champ(stringResource(R.string.st_filiere_recyclage), dFiliere) { dFiliere = it }
                    }
                    Spacer(Modifier.height(10.dp))
                    Champ(stringResource(R.string.st_prestataire), dPrestataire) { dPrestataire = it }
                }
                if (type == ProductType.EMBALLAGE) {
                    Spacer(Modifier.height(14.dp))
                    Text(stringResource(R.string.st_donnees_emballage), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MissaInk)
                    Spacer(Modifier.height(8.dp))
                    Champ(stringResource(R.string.st_type_emballage), eType) { eType = it }
                    Spacer(Modifier.height(10.dp))
                    Champ(stringResource(R.string.st_matiere), eMatiere) { eMatiere = it }
                    Spacer(Modifier.height(10.dp))
                    Champ(stringResource(R.string.st_dimensions), eDims) { eDims = it }
                    Spacer(Modifier.height(10.dp))
                    Champ(stringResource(R.string.st_poids_kg), ePoids) { ePoids = it }
                    Spacer(Modifier.height(10.dp))
                    Champ(stringResource(R.string.st_capacite), eCap) { eCap = it }
                    Interrupteur(stringResource(R.string.st_reutilisable), eReutil) { eReutil = it }
                    Interrupteur(stringResource(R.string.st_consigne), eConsigne) { eConsigne = it }
                    if (eReutil) {
                        Champ(stringResource(R.string.st_reutilisations_max), eMax) { eMax = it }
                        Spacer(Modifier.height(10.dp))
                    }
                }
                if (type == ProductType.CONSIGNATION) {
                    Spacer(Modifier.height(14.dp))
                    Text(stringResource(R.string.st_donnees_consignation), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MissaInk)
                    Spacer(Modifier.height(8.dp))
                    Champ(stringResource(R.string.st_proprietaire), cProprio) { cProprio = it }
                    Spacer(Modifier.height(10.dp))
                    Champ(stringResource(R.string.st_ref_contrat), cRef) { cRef = it }
                    Spacer(Modifier.height(10.dp))
                    ChampDate(stringResource(R.string.st_debut), cDebut) { cDebut = it }
                    Spacer(Modifier.height(10.dp))
                    ChampDate(stringResource(R.string.st_fin), cFin) { cFin = it }
                    Spacer(Modifier.height(10.dp))
                    Champ(stringResource(R.string.st_conditions_retour), cConditions) { cConditions = it }
                }
                if (type == ProductType.KIT) {
                    Spacer(Modifier.height(14.dp))
                    Text(stringResource(R.string.st_donnees_kit), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MissaInk)
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.st_methode_stock), fontSize = 11.sp, color = MissaMuted)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StockChip(stringResource(R.string.st_kit_virtuel), actif = kMethode == "VIRTUEL") { kMethode = "VIRTUEL" }
                        StockChip(stringResource(R.string.st_kit_assemble), actif = kMethode == "ASSEMBLE") { kMethode = "ASSEMBLE" }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(stringResource(R.string.st_composants), fontSize = 11.sp, color = MissaMuted)
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DropdownChamp(
                            libelle = stringResource(R.string.st_composants),
                            options = produitsVm.filter { it.id != (productId ?: 0L) }.map { it.id to it.nom },
                            selection = kSelection,
                            onSelection = { kSelection = it },
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = kQuantite,
                            onValueChange = { kQuantite = it },
                            modifier = Modifier.width(70.dp),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                        )
                        Button(
                            onClick = {
                                val sel = kSelection ?: return@Button
                                val q = kQuantite.toDoubleOrNull() ?: return@Button
                                if (q <= 0) return@Button
                                kComposants = kComposants.filterNot { it.composantId == sel } +
                                    com.missa.b360.core.data.entity.KitComposantEntity(kitId = 0L, composantId = sel, quantite = q)
                                kSelection = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                        ) { Text("+", fontSize = 14.sp, fontWeight = FontWeight.Bold) }
                    }
                    kComposants.forEach { c ->
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = produitsVm.firstOrNull { it.id == c.composantId }?.nom ?: "#${c.composantId}",
                                fontSize = 11.5.sp,
                                color = MissaInk,
                                modifier = Modifier.weight(1f),
                            )
                            Text(fmtQuantite(c.quantite), fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = MissaInk)
                            IconButton(onClick = { kComposants = kComposants.filterNot { it.composantId == c.composantId } }, modifier = Modifier.size(36.dp)) {
                                Icon(painterResource(StockIv.Trash), null, tint = MissaInk, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        if (nom.isBlank() || unite.isBlank()) {
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
                if (achetable) {
                    Champ(stringResource(R.string.st_prix_achat), prixAchat) { prixAchat = it }
                    Spacer(Modifier.height(10.dp))
                }
                if (vendable) {
                    Champ(stringResource(R.string.st_prix_vente), prixVente) { prixVente = it }
                    Spacer(Modifier.height(10.dp))
                    Champ("${stringResource(R.string.st_remise_max)} (%)", remiseMax) { remiseMax = it }
                    Spacer(Modifier.height(10.dp))
                }
                if (stockable) {
                    Champ(stringResource(R.string.st_minimum), stockMin) { stockMin = it }
                    Spacer(Modifier.height(10.dp))
                    Champ(stringResource(R.string.st_maximum), stockMax) { stockMax = it }
                    Spacer(Modifier.height(10.dp))
                    Champ(stringResource(R.string.st_securite), stockSecurite) { stockSecurite = it }
                    Spacer(Modifier.height(10.dp))
                }
                if (productId == null && stockable) {
                    Champ(stringResource(R.string.st_stock_initial), stockInitial) { stockInitial = it }
                    Spacer(Modifier.height(10.dp))
                    DropdownChamp(
                        libelle = stringResource(R.string.st_site_depot),
                        options = sites.map { it.id to it.nom },
                        selection = siteId,
                        onSelection = { siteId = it },
                    )
                    Spacer(Modifier.height(16.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { etape = 0 },
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MissaSurface, contentColor = MissaInk),
                    ) {
                        Text("← ${stringResource(R.string.st_retour)}", fontSize = 13.sp, fontWeight = FontWeight.Bold)
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
                                barcode = barcode.takeIf { it.isNotBlank() },
                                categorieId = categorieId,
                                vendable = vendable,
                                achetable = achetable,
                                stockable = stockable,
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
                            dechet = if (type == ProductType.DECHET_VALORISABLE || type == ProductType.DECHET_NON_VALORISABLE) {
                                com.missa.b360.core.data.entity.ProductDechetEntity(
                                    produitId = 0L,
                                    typeDechet = dType.takeIf { it.isNotBlank() },
                                    codeReglementaire = dCode.takeIf { it.isNotBlank() },
                                    dangereux = dDangereux,
                                    valorisable = type == ProductType.DECHET_VALORISABLE,
                                    origine = dOrigine.takeIf { it.isNotBlank() },
                                    modeElimination = dMode.takeIf { it.isNotBlank() },
                                    prestataire = dPrestataire.takeIf { it.isNotBlank() },
                                    coutElimination = dCout.toDoubleOrNull(),
                                    filiereRecyclage = dFiliere.takeIf { it.isNotBlank() },
                                    zoneStockage = dZone.takeIf { it.isNotBlank() },
                                )
                            } else {
                                null
                            },
                            emballage = if (type == ProductType.EMBALLAGE) {
                                com.missa.b360.core.data.entity.ProductEmballageEntity(
                                    produitId = 0L,
                                    typeEmballage = eType.takeIf { it.isNotBlank() },
                                    matiere = eMatiere.takeIf { it.isNotBlank() },
                                    dimensions = eDims.takeIf { it.isNotBlank() },
                                    poidsKg = ePoids.toDoubleOrNull(),
                                    capacite = eCap.toDoubleOrNull(),
                                    reutilisable = eReutil,
                                    consigne = eConsigne,
                                    reutilisationsMax = eMax.toIntOrNull(),
                                )
                            } else {
                                null
                            },
                            consignation = if (type == ProductType.CONSIGNATION) {
                                val fmtC = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                                val parseC = { t: String -> runCatching { fmtC.parse(t.trim())?.time }.getOrNull() }
                                com.missa.b360.core.data.entity.ProductConsignationEntity(
                                    produitId = 0L,
                                    proprietaire = cProprio.takeIf { it.isNotBlank() },
                                    referenceContrat = cRef.takeIf { it.isNotBlank() },
                                    dateDebut = parseC(cDebut),
                                    dateFin = parseC(cFin),
                                    conditionsRetour = cConditions.takeIf { it.isNotBlank() },
                                )
                            } else {
                                null
                            },
                            kit = if (type == ProductType.KIT) {
                                com.missa.b360.core.data.entity.ProductKitEntity(produitId = 0L, methode = kMethode)
                            } else {
                                null
                            },
                            composants = if (type == ProductType.KIT) kComposants else emptyList(),
                        )
                    },
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(14.dp),
                    enabled = nom.isNotBlank() && unite.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                ) {
                        Text(stringResource(R.string.st_enregistrer), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }

    if (dialogueNouvelleCategorie) {
        DialogueCreationCategorieRapide(
            onDismiss = { dialogueNouvelleCategorie = false },
            onValider = { nomCat ->
                vm.addCategoryAsync(nomCat) { newId ->
                    if (newId != null) {
                        categorieId = newId
                    }
                }
                dialogueNouvelleCategorie = false
            },
        )
    }
}

/** Boîte de dialogue de création rapide d'une catégorie d'article. */
@Composable
private fun DialogueCreationCategorieRapide(
    onDismiss: () -> Unit,
    onValider: (String) -> Unit,
) {
    var nom by remember { mutableStateOf("") }
    val valide = nom.trim().isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(R.string.st_nouvelle_categorie_rapide),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MissaInk,
            )
        },
        text = {
            OutlinedTextField(
                value = nom,
                onValueChange = { nom = it.take(80) },
                label = { Text(stringResource(R.string.st_nom_categorie_requis), fontSize = 11.sp) },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            Button(
                onClick = { onValider(nom.trim()) },
                enabled = valide,
                colors = ButtonDefaults.buttonColors(containerColor = BrandBlue, contentColor = Color.White),
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(stringResource(R.string.ops_save), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.ops_cancel), color = MissaInk)
            }
        },
    )
}

/** Interrupteur libellé + switch (règles vendable/achetable/stockable). */
@Composable
private fun Interrupteur(libelle: String, actif: Boolean, onActif: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(libelle, fontSize = 12.sp, color = MissaInk, modifier = Modifier.weight(1f))
        Switch(checked = actif, onCheckedChange = onActif)
    }
}

/** Champ de date en lecture seule ouvrant un petit calendrier (DatePicker). */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun ChampDate(libelle: String, valeur: String, onValeur: (String) -> Unit) {
    var ouvert by remember { mutableStateOf(false) }
    val fmt = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = valeur,
            onValueChange = {},
            readOnly = true,
            label = { Text(libelle, fontSize = 11.sp, color = MissaMuted) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            trailingIcon = {
                Icon(painterResource(StockIv.Calendar), null, tint = MissaInk, modifier = Modifier.size(18.dp))
            },
        )
        Box(modifier = Modifier.matchParentSize().clickable { ouvert = true })
    }
    if (ouvert) {
        // +12 h : midi UTC = même jour civil dans tous les fuseaux (aller/retour).
        val initial = runCatching { fmt.parse(valeur.trim())?.time }.getOrNull()
        val etatDate = rememberDatePickerState(initialSelectedDateMillis = initial?.plus(43_200_000L))
        DatePickerDialog(
            onDismissRequest = { ouvert = false },
            confirmButton = {
                TextButton(onClick = {
                    etatDate.selectedDateMillis?.let { millis ->
                        onValeur(fmt.format(java.util.Date(millis + 43_200_000L)))
                    }
                    ouvert = false
                }) { Text(stringResource(R.string.st_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { ouvert = false }) { Text(stringResource(R.string.st_annuler)) }
            },
        ) {
            DatePicker(state = etatDate)
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

/** Choix verrouillé par le contexte (type ou catégorie imposé) : badge lecture seule. */
@Composable
private fun BadgeVerrouille(icone: Int, texte: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MissaSurface,
        border = BorderStroke(1.dp, MissaBorder),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(icone), null, tint = MissaInk, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(texte, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MissaInk)
        }
    }
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
