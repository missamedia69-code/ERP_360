package com.missa.b360.ui.clients

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.missa.b360.R
import com.missa.b360.core.data.entity.CategoryClientEntity
import com.missa.b360.core.data.entity.ClientEntity
import com.missa.b360.core.data.entity.ClientStatus
import com.missa.b360.core.data.entity.ClientType
import com.missa.b360.core.data.entity.SiteEntity
import com.missa.b360.core.domain.usecase.ClientValidation
import com.missa.b360.core.util.Iso4217

/**
 * Formulaire client plein écran.
 *
 * Ce premier lot reprend l'en-tête et les informations principales de la maquette.
 * Les valeurs déjà prises en charge dans les futures sections (adresse, remise, limite,
 * badge et notes) sont conservées lors d'une édition afin de ne perdre aucune donnée.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientFormDialog(
    client: ClientEntity?,
    categories: List<CategoryClientEntity>,
    sites: List<SiteEntity>,
    deviseEntreprise: String?,
    codePaysParDefaut: String?,
    onDismiss: () -> Unit,
    onConfirm: (
        nom: String,
        tel: String,
        type: ClientType,
        email: String?,
        adresse: String?,
        catId: Long?,
        siteId: Long?,
        remise: Double,
        limite: Double?,
        badgeId: Long?,
        notes: String?,
    ) -> Unit,
) {
    val locale = LocalConfiguration.current.locales[0]
    val paysAvecIndicatif = remember(locale) { Iso4217.paysAvecIndicatif(locale) }
    val codePaysInitial = client?.telephone?.let(Iso4217::codePaysDepuisTelephone)
        ?: codePaysParDefaut
    var codePays by remember(client?.id) { mutableStateOf(codePaysInitial) }
    val indicatif = Iso4217.indicatifTelephone(codePays)
    var tel by remember(client?.id) {
        mutableStateOf(ClientValidation.telephoneSansIndicatif(client?.telephone.orEmpty(), indicatif))
    }
    LaunchedEffect(codePaysParDefaut) {
        if (codePays == null && codePaysParDefaut != null) codePays = codePaysParDefaut
    }

    var nom by remember(client?.id) { mutableStateOf(client?.nom.orEmpty()) }
    var type by remember(client?.id) { mutableStateOf(client?.type ?: ClientType.PARTICULIER) }
    var email by remember(client?.id) { mutableStateOf(client?.email.orEmpty()) }
    var catId by remember(client?.id) { mutableStateOf(client?.categorieId) }
    var siteId by remember(client?.id) { mutableStateOf(client?.siteId) }
    LaunchedEffect(client?.id, sites) {
        if (client == null && siteId == null) {
            siteId = sites.firstOrNull { it.principal }?.id
        }
    }

    // Champs conservés : ils seront replacés dans leurs onglets dédiés après validation du style.
    val adresse = client?.adresse.orEmpty()
    val remiseValeur = client?.remiseDefautPct ?: 0.0
    val limiteValeur = client?.limiteCredit
    val badgeId = client?.badgeId
    val notes = client?.notes.orEmpty()

    val telephoneComplet = ClientValidation.telephoneAvecIndicatif(tel, indicatif)
    val nomValide = ClientValidation.nomEstValide(nom)
    val telephoneValide = indicatif != null && ClientValidation.telephoneEstValide(telephoneComplet)
    val emailValide = ClientValidation.emailEstValide(email)
    val statutRes = if (client?.active == false || client?.statut == ClientStatus.DESACTIVE) {
        R.string.clients_inactif
    } else {
        R.string.clients_actif
    }

    fun sauvegarder() {
        onConfirm(
            nom,
            telephoneComplet,
            type,
            email.ifBlank { null },
            adresse.ifBlank { null },
            catId,
            siteId,
            remiseValeur,
            limiteValeur,
            badgeId,
            notes.ifBlank { null },
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false,
        ),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = stringResource(
                                    if (client == null) R.string.clients_nouveau else R.string.clients_modifier,
                                ),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.SemiBold,
                                ),
                                maxLines = 1,
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                    contentDescription = stringResource(R.string.ob_retour),
                                )
                            }
                        },
                        actions = {
                            TextButton(
                                onClick = ::sauvegarder,
                                enabled = nomValide && telephoneValide && emailValide,
                            ) {
                                Text(
                                    stringResource(R.string.clients_enregistrer),
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background,
                        ),
                    )
                },
                containerColor = MaterialTheme.colorScheme.background,
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.clients_informations_principales),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            )
                            Text(
                                text = stringResource(R.string.clients_informations_principales_aide),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            ClientTypeField(
                                type = type,
                                onTypeSelected = { type = it },
                            )
                            OutlinedTextField(
                                value = nom,
                                onValueChange = { nom = it.take(ClientValidation.LONGUEUR_NOM_MAX) },
                                label = { Text(stringResource(R.string.clients_nom)) },
                                placeholder = { Text(stringResource(R.string.clients_nom_exemple)) },
                                isError = nom.isNotEmpty() && !nomValide,
                                supportingText = {
                                    if (nom.isNotEmpty() && !nomValide) {
                                        Text(stringResource(R.string.clients_nom_invalide))
                                    }
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                modifier = Modifier.fillMaxWidth(),
                            )
                            TelephoneFields(
                                paysAvecIndicatif = paysAvecIndicatif,
                                codePays = codePays,
                                onCodePaysChange = { codePays = it },
                                tel = tel,
                                onTelChange = {
                                    tel = ClientValidation.filtrerTelephoneLocalPourSaisie(it).take(25)
                                },
                                telephoneValide = telephoneValide,
                                indicatifPresent = indicatif != null,
                            )
                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it.take(ClientValidation.LONGUEUR_EMAIL_MAX) },
                                label = { Text(stringResource(R.string.clients_email)) },
                                placeholder = { Text(stringResource(R.string.clients_email_exemple)) },
                                isError = email.isNotEmpty() && !emailValide,
                                supportingText = {
                                    if (email.isNotEmpty() && !emailValide) {
                                        Text(stringResource(R.string.clients_email_invalide))
                                    }
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Email,
                                    imeAction = ImeAction.Next,
                                ),
                                modifier = Modifier.fillMaxWidth(),
                            )
                            ClientSelectionFields(
                                categories = categories,
                                catId = catId,
                                onCategoryChange = { catId = it },
                                sites = sites,
                                siteId = siteId,
                                onSiteChange = { siteId = it },
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            ClientReadOnlyFields(
                                statut = stringResource(statutRes),
                                devise = deviseEntreprise?.trim().takeUnless { it.isNullOrEmpty() } ?: "—",
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClientTypeField(
    type: ClientType,
    onTypeSelected: (ClientType) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = stringResource(type.labelRes()),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.clients_type)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            ClientType.entries.forEach { item ->
                DropdownMenuItem(
                    text = { Text(stringResource(item.labelRes())) },
                    onClick = {
                        onTypeSelected(item)
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TelephoneFields(
    paysAvecIndicatif: List<Iso4217.PaysAvecIndicatif>,
    codePays: String?,
    onCodePaysChange: (String) -> Unit,
    tel: String,
    onTelChange: (String) -> Unit,
    telephoneValide: Boolean,
    indicatifPresent: Boolean,
) {
    var indicatifOuvert by remember { mutableStateOf(false) }
    var rechercheIndicatif by remember { mutableStateOf("") }
    val paysSelectionne = paysAvecIndicatif.firstOrNull { it.code == codePays }
    val paysFiltres = remember(paysAvecIndicatif, rechercheIndicatif) {
        val requete = rechercheIndicatif.trim()
        if (requete.isEmpty()) {
            paysAvecIndicatif
        } else {
            paysAvecIndicatif.filter { pays ->
                pays.nom.contains(requete, ignoreCase = true) ||
                    pays.code.contains(requete, ignoreCase = true) ||
                    pays.indicatif.contains(requete)
            }
        }
    }

    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val horizontal = maxWidth >= 520.dp
        if (horizontal) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                CountryCodeField(
                    value = if (indicatifOuvert) rechercheIndicatif else paysSelectionne?.indicatif.orEmpty(),
                    expanded = indicatifOuvert,
                    onExpandedChange = {
                        indicatifOuvert = it
                        if (it) rechercheIndicatif = ""
                    },
                    onQueryChange = {
                        rechercheIndicatif = it
                        indicatifOuvert = true
                    },
                    paysFiltres = paysFiltres,
                    onSelect = {
                        onCodePaysChange(it.code)
                        rechercheIndicatif = ""
                        indicatifOuvert = false
                    },
                    modifier = Modifier.weight(0.75f),
                )
                TelephoneField(
                    tel = tel,
                    onTelChange = onTelChange,
                    telephoneValide = telephoneValide,
                    indicatifPresent = indicatifPresent,
                    modifier = Modifier.weight(1.25f),
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CountryCodeField(
                    value = if (indicatifOuvert) {
                        rechercheIndicatif
                    } else {
                        paysSelectionne?.let { "${it.indicatif} · ${it.code}" }.orEmpty()
                    },
                    expanded = indicatifOuvert,
                    onExpandedChange = {
                        indicatifOuvert = it
                        if (it) rechercheIndicatif = ""
                    },
                    onQueryChange = {
                        rechercheIndicatif = it
                        indicatifOuvert = true
                    },
                    paysFiltres = paysFiltres,
                    onSelect = {
                        onCodePaysChange(it.code)
                        rechercheIndicatif = ""
                        indicatifOuvert = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                TelephoneField(
                    tel = tel,
                    onTelChange = onTelChange,
                    telephoneValide = telephoneValide,
                    indicatifPresent = indicatifPresent,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CountryCodeField(
    value: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onQueryChange: (String) -> Unit,
    paysFiltres: List<Iso4217.PaysAvecIndicatif>,
    onSelect: (Iso4217.PaysAvecIndicatif) -> Unit,
    modifier: Modifier,
) {
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onQueryChange,
            label = { Text(stringResource(R.string.clients_indicatif_pays)) },
            placeholder = { Text(stringResource(R.string.clients_rechercher_indicatif)) },
            singleLine = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
        ) {
            if (paysFiltres.isEmpty()) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.clients_aucun_indicatif)) },
                    onClick = {},
                    enabled = false,
                )
            } else {
                paysFiltres.forEach { pays ->
                    DropdownMenuItem(
                        text = { Text("${pays.nom} (${pays.code}) · ${pays.indicatif}") },
                        onClick = { onSelect(pays) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TelephoneField(
    tel: String,
    onTelChange: (String) -> Unit,
    telephoneValide: Boolean,
    indicatifPresent: Boolean,
    modifier: Modifier,
) {
    OutlinedTextField(
        value = tel,
        onValueChange = onTelChange,
        label = { Text(stringResource(R.string.clients_telephone)) },
        placeholder = { Text(stringResource(R.string.clients_telephone_exemple)) },
        isError = !indicatifPresent || tel.isNotEmpty() && !telephoneValide,
        supportingText = {
            when {
                !indicatifPresent -> Text(stringResource(R.string.clients_indicatif_obligatoire))
                tel.isNotEmpty() && !telephoneValide -> Text(stringResource(R.string.clients_telephone_invalide))
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Phone,
            imeAction = ImeAction.Next,
        ),
        modifier = modifier,
    )
}

@Composable
private fun ClientSelectionFields(
    categories: List<CategoryClientEntity>,
    catId: Long?,
    onCategoryChange: (Long?) -> Unit,
    sites: List<SiteEntity>,
    siteId: Long?,
    onSiteChange: (Long?) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val horizontal = maxWidth >= 520.dp
        if (horizontal) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                CategoryField(
                    categories = categories,
                    selectedId = catId,
                    onSelected = onCategoryChange,
                    modifier = Modifier.weight(1f),
                )
                SiteField(
                    sites = sites,
                    selectedId = siteId,
                    onSelected = onSiteChange,
                    modifier = Modifier.weight(1f),
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CategoryField(
                    categories = categories,
                    selectedId = catId,
                    onSelected = onCategoryChange,
                    modifier = Modifier.fillMaxWidth(),
                )
                SiteField(
                    sites = sites,
                    selectedId = siteId,
                    onSelected = onSiteChange,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryField(
    categories: List<CategoryClientEntity>,
    selectedId: Long?,
    onSelected: (Long?) -> Unit,
    modifier: Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = categories.firstOrNull { it.id == selectedId }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = selected?.nom.orEmpty(),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.clients_categorie_optionnelle)) },
            placeholder = { Text(stringResource(R.string.clients_aucune_categorie)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.clients_aucune_categorie)) },
                onClick = {
                    onSelected(null)
                    expanded = false
                },
            )
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.nom) },
                    onClick = {
                        onSelected(category.id)
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SiteField(
    sites: List<SiteEntity>,
    selectedId: Long?,
    onSelected: (Long?) -> Unit,
    modifier: Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = sites.firstOrNull { it.id == selectedId }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = selected?.nom.orEmpty(),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.clients_site)) },
            placeholder = { Text(stringResource(R.string.clients_aucun_site)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.clients_aucun_site)) },
                onClick = {
                    onSelected(null)
                    expanded = false
                },
            )
            sites.forEach { site ->
                DropdownMenuItem(
                    text = { Text(site.nom) },
                    onClick = {
                        onSelected(site.id)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun ClientReadOnlyFields(statut: String, devise: String) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val horizontal = maxWidth >= 520.dp
        if (horizontal) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                ReadOnlyClientValue(
                    label = stringResource(R.string.clients_statut),
                    value = statut,
                    modifier = Modifier.weight(1f),
                )
                ReadOnlyClientValue(
                    label = stringResource(R.string.clients_devise),
                    value = devise,
                    modifier = Modifier.weight(1f),
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ReadOnlyClientValue(
                    label = stringResource(R.string.clients_statut),
                    value = statut,
                    modifier = Modifier.fillMaxWidth(),
                )
                ReadOnlyClientValue(
                    label = stringResource(R.string.clients_devise),
                    value = devise,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun ReadOnlyClientValue(label: String, value: String, modifier: Modifier) {
    Column(
        modifier = modifier
            .heightIn(min = 58.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
    }
}
