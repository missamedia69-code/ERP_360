package com.missa.b360.ui.clients

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.missa.b360.R
import com.missa.b360.core.data.entity.BadgeLoyaltyEntity
import com.missa.b360.core.data.entity.CategoryClientEntity
import com.missa.b360.core.data.entity.ClientEntity
import com.missa.b360.core.data.entity.ClientType
import com.missa.b360.core.domain.usecase.ClientValidation
import com.missa.b360.core.util.Iso4217

/**
 * Formulaire client (9.2) — création/édition.
 * Doublons RC-01 confirmés par l'UI (la confirmation est gérée au niveau de l'écran liste).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientFormDialog(
    client: ClientEntity?,
    categories: List<CategoryClientEntity>,
    badges: List<BadgeLoyaltyEntity>,
    codePaysParDefaut: String?,
    onDismiss: () -> Unit,
    onConfirm: (
        nom: String,
        tel: String,
        type: ClientType,
        email: String?,
        adresse: String?,
        catId: Long?,
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
    var indicatifOuvert by remember { mutableStateOf(false) }
    var rechercheIndicatif by remember { mutableStateOf("") }
    val paysIndicatifFiltres = remember(paysAvecIndicatif, rechercheIndicatif) {
        val requete = rechercheIndicatif.trim()
        if (requete.isEmpty()) paysAvecIndicatif else paysAvecIndicatif.filter { pays ->
            pays.nom.contains(requete, ignoreCase = true) ||
                pays.code.contains(requete, ignoreCase = true) ||
                pays.indicatif.contains(requete)
        }
    }
    val paysSelectionne = paysAvecIndicatif.firstOrNull { it.code == codePays }

    var nom by remember { mutableStateOf(client?.nom ?: "") }
    var type by remember { mutableStateOf(client?.type ?: ClientType.PARTICULIER) }
    var email by remember { mutableStateOf(client?.email ?: "") }
    var adresse by remember { mutableStateOf(client?.adresse ?: "") }
    var catId by remember { mutableStateOf(client?.categorieId) }
    var remise by remember { mutableStateOf(client?.remiseDefautPct?.toString() ?: "0") }
    var limite by remember { mutableStateOf(client?.limiteCredit?.toString() ?: "") }
    var badgeId by remember { mutableStateOf(client?.badgeId) }
    var notes by remember { mutableStateOf(client?.notes ?: "") }

    val telephoneComplet = ClientValidation.telephoneAvecIndicatif(tel, indicatif)
    val nomValide = ClientValidation.nomEstValide(nom)
    val telephoneValide = indicatif != null && ClientValidation.telephoneEstValide(telephoneComplet)
    val emailValide = ClientValidation.emailEstValide(email)
    val adresseValide = ClientValidation.adresseEstValide(adresse)
    val notesValides = ClientValidation.notesSontValides(notes)
    val remiseValeur = remise.replace(',', '.').toDoubleOrNull()
    val limiteValeur = limite.replace(',', '.').toDoubleOrNull()
    val remiseValide = remiseValeur != null && remiseValeur in 0.0..100.0
    val limiteValide = limite.isBlank() || limiteValeur != null && limiteValeur >= 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (client == null) R.string.clients_ajouter else R.string.clients_modifier,
                ),
            )
        },
        text = {
            Column(
                Modifier
                    .verticalScroll(rememberScrollState()),
            ) {
                OutlinedTextField(
                    value = nom,
                    onValueChange = { nom = it.take(ClientValidation.LONGUEUR_NOM_MAX) },
                    label = { Text(stringResource(R.string.clients_nom)) },
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
                ExposedDropdownMenuBox(
                    expanded = indicatifOuvert,
                    onExpandedChange = { ouvert ->
                        indicatifOuvert = ouvert
                        if (ouvert) rechercheIndicatif = ""
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                ) {
                    OutlinedTextField(
                        value = if (indicatifOuvert) {
                            rechercheIndicatif
                        } else {
                            paysSelectionne?.let { "${it.indicatif} — ${it.nom} (${it.code})" }.orEmpty()
                        },
                        onValueChange = {
                            rechercheIndicatif = it
                            indicatifOuvert = true
                        },
                        label = { Text(stringResource(R.string.clients_indicatif_pays)) },
                        placeholder = { Text(stringResource(R.string.clients_rechercher_indicatif)) },
                        singleLine = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = indicatifOuvert)
                        },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                    )
                    ExposedDropdownMenu(
                        expanded = indicatifOuvert,
                        onDismissRequest = {
                            indicatifOuvert = false
                            rechercheIndicatif = ""
                        },
                    ) {
                        if (paysIndicatifFiltres.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.clients_aucun_indicatif)) },
                                onClick = {},
                                enabled = false,
                            )
                        } else {
                            paysIndicatifFiltres.forEach { pays ->
                                DropdownMenuItem(
                                    text = { Text("${pays.nom} (${pays.code}) — ${pays.indicatif}") },
                                    onClick = {
                                        codePays = pays.code
                                        rechercheIndicatif = ""
                                        indicatifOuvert = false
                                    },
                                )
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = tel,
                    onValueChange = {
                        tel = ClientValidation.filtrerTelephoneLocalPourSaisie(it).take(25)
                    },
                    label = { Text(stringResource(R.string.clients_telephone)) },
                    isError = indicatif == null || tel.isNotEmpty() && !telephoneValide,
                    supportingText = {
                        when {
                            indicatif == null -> Text(stringResource(R.string.clients_indicatif_obligatoire))
                            tel.isNotEmpty() && !telephoneValide -> {
                                Text(stringResource(R.string.clients_telephone_invalide))
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Next,
                    ),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it.take(ClientValidation.LONGUEUR_EMAIL_MAX) },
                    label = { Text(stringResource(R.string.clients_email)) },
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
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                OutlinedTextField(
                    value = adresse,
                    onValueChange = { adresse = it.take(ClientValidation.LONGUEUR_ADRESSE_MAX) },
                    label = { Text(stringResource(R.string.clients_adresse)) },
                    isError = !adresseValide,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                Text(
                    stringResource(R.string.clients_type),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 8.dp),
                )
                ClientType.entries.chunked(3).forEach { rangees ->
                    Row(Modifier.fillMaxWidth()) {
                        rangees.forEach { t ->
                            Row(Modifier.weight(1f)) {
                                RadioButton(selected = type == t, onClick = { type = t })
                                Text(stringResource(t.labelRes()), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it.take(ClientValidation.LONGUEUR_NOTES_MAX) },
                    label = { Text(stringResource(R.string.clients_notes)) },
                    isError = !notesValides,
                    minLines = 2,
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
// Champs optionnels
                var catOuvert by remember { mutableStateOf(false) }
                val catChoisie = categories.firstOrNull { it.id == catId }
                ChampsCategorie(
                    catChoisie = catChoisie?.nom ?: "",
                    catOuvert = catOuvert,
                    onExpanded = { catOuvert = it },
                    categories = categories,
                    onSelect = {
                        catId = it
                        catOuvert = false
                    },
                ) {
                    catId = null
                    catOuvert = false
                }

                OutlinedTextField(
                    value = remise,
                    onValueChange = { remise = it.filter { c -> (c.isDigit() || c == '.' || c == ',') }.take(8) },
                    label = { Text(stringResource(R.string.clients_remise)) },
                    isError = !remiseValide,
                    supportingText = {
                        if (!remiseValide) Text(stringResource(R.string.clients_remise_invalide))
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next,
                    ),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                OutlinedTextField(
                    value = limite,
                    onValueChange = { limite = it.filter { c -> (c.isDigit() || c == '.' || c == ',') }.take(15) },
                    label = { Text(stringResource(R.string.clients_limite_credit)) },
                    isError = !limiteValide,
                    supportingText = {
                        if (!limiteValide) Text(stringResource(R.string.clients_limite_credit_invalide))
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done,
                    ),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )

                var badgeOuvert by remember { mutableStateOf(false) }
                val badgeChoisi = badges.firstOrNull { it.id == badgeId }
                ChampsBadge(
                    badgeChoisi = badgeChoisi?.let { "${it.nom} (-${it.remisePct}%)" } ?: "",
                    badgeOuvert = badgeOuvert,
                    onExpanded = { badgeOuvert = it },
                    badges = badges,
                    onSelect = {
                        badgeId = it
                        badgeOuvert = false
                    },
                ) {
                    badgeId = null
                    badgeOuvert = false
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        nom, telephoneComplet, type,
                        email.ifBlank { null },
                        adresse.ifBlank { null },
                        catId,
                        remiseValeur ?: 0.0,
                        limiteValeur,
                        badgeId,
                        notes.ifBlank { null },
                    )
                },
                enabled = nomValide && telephoneValide && emailValide && adresseValide &&
                    notesValides && remiseValide && limiteValide,
            ) {
                Text(
                    stringResource(
                        if (client == null) R.string.clients_ajouter else R.string.adm_sauvegarder,
                    ),
                )
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(stringResource(R.string.ob_retour))
            }
        },
    )
}