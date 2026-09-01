package com.missa.b360.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.missa.b360.R
import com.missa.b360.core.data.entity.RoleEntity
import com.missa.b360.core.data.entity.UserEntity
import com.missa.b360.ui.components.MissaPanel

/** Utilisateurs & rôles (D1/D2, RA-14) : création d'utilisateurs, liste, rôles. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUtilisateursScreen(
    onBack: () -> Unit,
    viewModel: UtilisateursViewModel = hiltViewModel(),
) {
    val utilisateurs by viewModel.utilisateurs.collectAsState(initial = emptyList())
    val roles by viewModel.roles.collectAsState(initial = emptyList())
    val resultat by viewModel.resultat.collectAsState()

    var nom by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var roleId by remember { mutableStateOf<Long?>(null) }

    AdminScaffold(titreRes = R.string.admin_utilisateurs, onBack = onBack) {
        // --- Ajout d'un utilisateur ---
        MissaPanel(modifier = Modifier.fillMaxWidth()) {
            Text(
                stringResource(R.string.adm_users_add),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        OutlinedTextField(
            value = nom,
            onValueChange = { nom = it },
            label = { Text(stringResource(R.string.adm_users_nom)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text(stringResource(R.string.adm_users_email)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        var roleOuvert by remember { mutableStateOf(false) }
        val roleChoisi = roles.firstOrNull { it.id == roleId }
        ExposedDropdownMenuBox(
            expanded = roleOuvert,
            onExpandedChange = { roleOuvert = it },
            modifier = Modifier.fillMaxWidth(),
        ) {
            OutlinedTextField(
                value = roleChoisi?.nom ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.adm_users_role)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roleOuvert) },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
            )
            ExposedDropdownMenu(expanded = roleOuvert, onDismissRequest = { roleOuvert = false }) {
                roles.forEach { role ->
                    DropdownMenuItem(
                        text = { Text(role.nom) },
                        onClick = {
                            roleId = role.id
                            roleOuvert = false
                        },
                    )
                }
            }
        }

        Button(
            onClick = {
                roleId?.let { viewModel.creerUtilisateur(nom, email, it) }
                nom = ""
                email = ""
            },
            enabled = nom.isNotBlank() && email.isNotBlank() && roleId != null,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.adm_users_creer))
        }
            resultat?.let {
                Text(
                    if (it.ok) stringResource(R.string.adm_users_resultat_ok) else it.message.orEmpty(),
                    color = if (it.ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }

        // --- Liste des utilisateurs ---
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.admin_utilisateurs),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        utilisateurs.forEach { user ->
            UserRow(user, roles) { viewModel.desactiver(user.id) }
        }
    }
}

@Composable
private fun UserRow(
    user: UserEntity,
    roles: List<RoleEntity>,
    onDesactiver: () -> Unit,
) {
    MissaPanel(modifier = Modifier.fillMaxWidth()) {
        Text(user.nom, fontWeight = FontWeight.Bold)
        Text(
            user.emailSecours,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            val role = roles.firstOrNull { it.id == user.roleId }
            Text("${stringResource(R.string.adm_users_role)}: ${role?.nom ?: "—"}", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.weight(1f))
            if (user.actif) {
                OutlinedButton(onClick = onDesactiver) {
                    Text(stringResource(R.string.adm_users_desactiver))
                }
            } else {
                Text(
                    stringResource(R.string.adm_users_inactif),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
