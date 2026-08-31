package com.missa.b360.ui.clients

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.missa.b360.R
import com.missa.b360.core.data.entity.BadgeLoyaltyEntity
import com.missa.b360.core.data.entity.CategoryClientEntity
import com.missa.b360.core.data.entity.ClientEntity
import com.missa.b360.core.data.entity.ClientType

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
    var nom by remember { mutableStateOf(client?.nom ?: "") }
    var tel by remember { mutableStateOf(client?.telephone ?: "") }
    var type by remember { mutableStateOf(client?.type ?: ClientType.PARTICULIER) }
    var email by remember { mutableStateOf(client?.email ?: "") }
    var adresse by remember { mutableStateOf(client?.adresse ?: "") }
    var catId by remember { mutableStateOf(client?.categorieId) }
    var remise by remember { mutableStateOf(client?.remiseDefautPct?.toString() ?: "0") }
    var limite by remember { mutableStateOf(client?.limiteCredit?.toString() ?: "") }
    var badgeId by remember { mutableStateOf(client?.badgeId) }
    var notes by remember { mutableStateOf(client?.notes ?: "") }

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
                    onValueChange = { nom = it },
                    label = { Text(stringResource(R.string.clients_nom)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = tel,
                    onValueChange = { tel = it },
                    label = { Text(stringResource(R.string.clients_telephone)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(stringResource(R.string.clients_email)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                OutlinedTextField(
                    value = adresse,
                    onValueChange = { adresse = it },
                    label = { Text(stringResource(R.string.clients_adresse)) },
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
                                Text(t.name, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.clients_notes)) },
                    singleLine = true,
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
                    onValueChange = { remise = it.filter { c -> (c.isDigit() || c == '.') } },
                    label = { Text(stringResource(R.string.clients_remise)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                OutlinedTextField(
                    value = limite,
                    onValueChange = { limite = it.filter { c -> (c.isDigit() || c == '.') } },
                    label = { Text(stringResource(R.string.clients_limite_credit)) },
                    singleLine = true,
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
                        nom, tel, type,
                        email.ifBlank { null },
                        adresse.ifBlank { null },
                        catId,
                        remise.toDoubleOrNull() ?: 0.0,
                        limite.toDoubleOrNull(),
                        badgeId,
                        notes.ifBlank { null },
                    )
                },
                enabled = nom.isNotBlank() && tel.isNotBlank(),
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