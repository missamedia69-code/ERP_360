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
import androidx.compose.foundation.shape.RoundedCornerShape
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
    ClientType.GROSSISTE -> R.string.clients_type_grossiste
    ClientType.DISTRIBUTEUR -> R.string.clients_type_distributeur
    ClientType.CLIENT_EXPORT -> R.string.clients_type_export
    ClientType.CLIENT_PROJET -> R.string.clients_type_projet
    ClientType.PROSPECT -> R.string.clients_type_prospect
}
