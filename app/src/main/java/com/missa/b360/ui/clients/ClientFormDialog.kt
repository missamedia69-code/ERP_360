package com.missa.b360.ui.clients

import com.missa.b360.ui.icons.Iv
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.Color
import com.missa.b360.R
import com.missa.b360.ui.navigation.AppModule
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted
import com.missa.b360.core.data.entity.CategoryClientEntity
import com.missa.b360.core.data.entity.ClientEntity
import com.missa.b360.core.data.entity.ClientStatus
import com.missa.b360.core.data.entity.ClientType
import com.missa.b360.core.data.entity.SiteEntity
import com.missa.b360.core.domain.usecase.ClientValidation
import com.missa.b360.core.util.Iso4217
import com.missa.b360.ui.components.*

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
    val locale = LocalConfiguration.current.locales.takeIf { !it.isEmpty }?.get(0) ?: java.util.Locale.getDefault()
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

    val formulaireValide = nomValide && telephoneValide && emailValide

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false,
        ),
    ) {
        MissaFormulaireTheme(AppModule.CLIENTS.couleur) {
            Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
                Column(Modifier.fillMaxSize()) {
                    MissaTopAppBar(
                        title = stringResource(if (client == null) R.string.clients_nouveau else R.string.clients_modifier),
                        onBack = onDismiss,
                        couleurFond = AppModule.CLIENTS.couleurPale,
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.clients_informations_principales_aide),
                            fontSize = 12.sp,
                            color = MissaMuted,
                        )
                        MissaFormSection(titre = stringResource(R.string.form_section_identite), numero = 1) {
                            MissaChampListe(
                                libelle = stringResource(R.string.clients_type),
                                options = ClientType.entries.map { it to stringResource(it.labelRes()) },
                                selection = type,
                                onSelection = { type = it },
                                icone = if (type == ClientType.PARTICULIER || type == ClientType.PROSPECT) Iv.Person else Iv.Business,
                            )
                            MissaChampTexte(
                                nom, { nom = it }, stringResource(R.string.clients_nom),
                                icone = Iv.PersonOutline, requis = true, longueurMax = ClientValidation.LONGUEUR_NOM_MAX,
                                placeholder = stringResource(R.string.clients_nom_exemple),
                                erreur = if (nom.isNotEmpty() && !nomValide) stringResource(R.string.clients_nom_invalide) else null,
                            )
                        }
                        MissaFormSection(titre = stringResource(R.string.form_section_contact), numero = 2) {
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
                            MissaChampTexte(
                                email, { email = it }, stringResource(R.string.clients_email),
                                icone = Iv.MailOutline, clavier = MissaClavier.EMAIL, longueurMax = ClientValidation.LONGUEUR_EMAIL_MAX,
                                placeholder = stringResource(R.string.clients_email_exemple),
                                erreur = if (email.isNotEmpty() && !emailValide) stringResource(R.string.clients_email_invalide) else null,
                            )
                        }
                        MissaFormSection(titre = stringResource(R.string.form_section_commercial), numero = 3) {
                            MissaRangee {
                                MissaChampListe<Long?>(
                                    libelle = stringResource(R.string.clients_categorie_optionnelle),
                                    options = listOf<Pair<Long?, String>>(null to stringResource(R.string.clients_aucune_categorie)) +
                                        categories.map { it.id to it.nom },
                                    selection = catId,
                                    onSelection = { catId = it },
                                    modifier = Modifier.weight(1f),
                                    icone = Iv.Category,
                                )
                                MissaChampListe<Long?>(
                                    libelle = stringResource(R.string.clients_site),
                                    options = listOf<Pair<Long?, String>>(null to stringResource(R.string.clients_aucun_site)) +
                                        sites.map { it.id to it.nom },
                                    selection = siteId,
                                    onSelection = { siteId = it },
                                    modifier = Modifier.weight(1f),
                                    icone = Iv.Store,
                                )
                            }
                            ClientReadOnlyFields(
                                statut = stringResource(statutRes),
                                devise = deviseEntreprise?.trim().takeUnless { it.isNullOrEmpty() } ?: "—",
                            )
                        }
                    }
                    MissaFormPied(
                        texte = stringResource(R.string.clients_enregistrer),
                        onValider = ::sauvegarder,
                        actif = formulaireValide,
                        modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
                    )
                }
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
            label = { Text(stringResource(R.string.clients_indicatif_pays), fontSize = 12.sp) },
            placeholder = { Text(stringResource(R.string.clients_rechercher_indicatif)) },
            leadingIcon = { Icon(painterResource(Iv.Public), null, tint = MissaInk, modifier = Modifier.size(20.dp)) },
            colors = missaChampCouleurs(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            shape = RoundedCornerShape(14.dp),
            tonalElevation = 6.dp,
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
        label = { Text(stringResource(R.string.clients_telephone) + " *", fontSize = 12.sp) },
        placeholder = { Text(stringResource(R.string.clients_telephone_exemple)) },
        leadingIcon = { Icon(painterResource(Iv.Call), null, tint = MissaInk, modifier = Modifier.size(20.dp)) },
        colors = missaChampCouleurs(),
        shape = RoundedCornerShape(12.dp),
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
