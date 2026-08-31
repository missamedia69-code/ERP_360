package com.missa.b360.ui.clients

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.missa.b360.R
import com.missa.b360.core.data.entity.BadgeLoyaltyEntity
import com.missa.b360.core.data.entity.CategoryClientEntity

/** Dialog de gestion des catégories de clients (création + suppression verrouillée). */
@Composable
fun CategoriesDialog(
    categories: List<CategoryClientEntity>,
    message: String?,
    onCreer: (String) -> Unit,
    onSupprimer: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var nom by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.clients_categories)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = nom,
                    onValueChange = { nom = it },
                    label = { Text(stringResource(R.string.clients_nom_categorie)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = {
                        if (nom.isNotBlank()) {
                            onCreer(nom)
                            nom = ""
                        }
                    },
                    enabled = nom.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                ) {
                    Text(stringResource(R.string.clients_ajouter_categorie))
                }
                message?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                Spacer(Modifier.height(8.dp))
                if (categories.isEmpty()) {
                    Text(
                        stringResource(R.string.clients_categories_vide),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
                categories.forEach { cat ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    ) {
                        Text(
                            cat.nom,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { onSupprimer(cat.id) }) {
                            Text(stringResource(R.string.clients_supprimer_categorie))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.ob_terminer))
            }
        },
    )
}

/** Dialog de gestion des badges de fidélité (RC-16). */
@Composable
fun BadgesDialog(
    badges: List<BadgeLoyaltyEntity>,
    onCreer: (String, Double) -> Unit,
    onDismiss: () -> Unit,
) {
    var nom by remember { mutableStateOf("") }
    var remise by remember { mutableStateOf("5") }
    val remiseValeur = remise.replace(',', '.').toDoubleOrNull()
    val remiseValide = remiseValeur != null && remiseValeur in 0.0..100.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.clients_badges)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = nom,
                    onValueChange = { nom = it },
                    label = { Text(stringResource(R.string.clients_nom_badge)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = remise,
                    onValueChange = { remise = it.filter { c -> c.isDigit() || c == '.' || c == ',' } },
                    label = { Text(stringResource(R.string.clients_remise_badge)) },
                    isError = !remiseValide,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                Button(
                    onClick = {
                        if (nom.isNotBlank() && remiseValide) {
                            onCreer(nom, remiseValeur ?: 0.0)
                            nom = ""
                            remise = "5"
                        }
                    },
                    enabled = nom.isNotBlank() && remiseValide,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                ) {
                    Text(stringResource(R.string.clients_ajouter_badge))
                }
                Spacer(Modifier.height(8.dp))
                if (badges.isEmpty()) {
                    Text(
                        stringResource(R.string.clients_badges_vide),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
                badges.forEach { badge ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    ) {
                        Text(
                            "${badge.nom} (-${badge.remisePct}%)",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            stringResource(if (badge.actif) R.string.clients_actif else R.string.clients_inactif),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.ob_terminer))
            }
        },
    )
}
