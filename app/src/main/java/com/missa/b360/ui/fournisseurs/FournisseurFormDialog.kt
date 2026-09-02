package com.missa.b360.ui.fournisseurs

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.missa.b360.R
import com.missa.b360.core.data.entity.FournisseurEntity

/** Formulaire fournisseur (9.3) — création/édition. Doublons RF-01 confirmés par l'UI. */
@Composable
fun FournisseurFormDialog(
    fournisseur: FournisseurEntity?,
    onDismiss: () -> Unit,
    onConfirm: (nom: String, tel: String) -> Unit,
) {
    var nom by remember { mutableStateOf(fournisseur?.nom ?: "") }
    var tel by remember { mutableStateOf(fournisseur?.telephone ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (fournisseur == null) R.string.fournisseurs_ajouter else R.string.fournisseurs_modifier,
                ),
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = nom,
                    onValueChange = { nom = it },
                    label = { Text(stringResource(R.string.fournisseurs_nom)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = tel,
                    onValueChange = { tel = it },
                    label = { Text(stringResource(R.string.fournisseurs_telephone)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(nom, tel) },
                enabled = nom.isNotBlank() && tel.isNotBlank(),
            ) {
                Text(
                    stringResource(
                        if (fournisseur == null) R.string.fournisseurs_ajouter else R.string.adm_sauvegarder,
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