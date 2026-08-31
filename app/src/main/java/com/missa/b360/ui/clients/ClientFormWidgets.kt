package com.missa.b360.ui.clients

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.annotation.StringRes
import com.missa.b360.R
import com.missa.b360.core.data.entity.BadgeLoyaltyEntity
import com.missa.b360.core.data.entity.CategoryClientEntity
import com.missa.b360.core.data.entity.ClientType

@StringRes
fun ClientType.labelRes(): Int = when (this) {
    ClientType.PARTICULIER -> R.string.clients_type_particulier
    ClientType.ENTREPRISE -> R.string.clients_type_entreprise
    ClientType.ADMINISTRATION -> R.string.clients_type_administration
    ClientType.ONG -> R.string.clients_type_ong
    ClientType.REVENDEUR -> R.string.clients_type_revendeur
    ClientType.PROSPECT -> R.string.clients_type_prospect
}

/** Menu déroulant catégorie client (RC). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChampsCategorie(
    catChoisie: String,
    catOuvert: Boolean,
    onExpanded: (Boolean) -> Unit,
    categories: List<CategoryClientEntity>,
    onSelect: (Long?) -> Unit,
    onClear: () -> Unit,
) {
    ExposedDropdownMenuBox(
        expanded = catOuvert,
        onExpandedChange = onExpanded,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    ) {
        OutlinedTextField(
            value = catChoisie,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.clients_categorie_optionnelle)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catOuvert) },
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = catOuvert, onDismissRequest = { onExpanded(false) }) {
            DropdownMenuItem(
                text = { Text("—") },
                onClick = onClear,
            )
            categories.forEach { cat ->
                DropdownMenuItem(
                    text = { Text(cat.nom) },
                    onClick = { onSelect(cat.id) },
                )
            }
        }
    }
}

/** Menu déroulant badge de fidélité (RC-16). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChampsBadge(
    badgeChoisi: String,
    badgeOuvert: Boolean,
    onExpanded: (Boolean) -> Unit,
    badges: List<BadgeLoyaltyEntity>,
    onSelect: (Long?) -> Unit,
    onClear: () -> Unit,
) {
    ExposedDropdownMenuBox(
        expanded = badgeOuvert,
        onExpandedChange = onExpanded,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    ) {
        OutlinedTextField(
            value = badgeChoisi,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.clients_badge_optionnel)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = badgeOuvert) },
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = badgeOuvert, onDismissRequest = { onExpanded(false) }) {
            DropdownMenuItem(
                text = { Text("—") },
                onClick = onClear,
            )
            badges.forEach { badge ->
                DropdownMenuItem(
                    text = { Text("${badge.nom} (-${badge.remisePct}%)") },
                    onClick = { onSelect(badge.id) },
                )
            }
        }
    }
}
