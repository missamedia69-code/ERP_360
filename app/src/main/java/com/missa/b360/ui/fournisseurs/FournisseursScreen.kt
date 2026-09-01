package com.missa.b360.ui.fournisseurs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.missa.b360.R
import com.missa.b360.core.data.entity.FournisseurEntity
import com.missa.b360.ui.components.MissaEmptyState
import com.missa.b360.ui.components.MissaLayout
import com.missa.b360.ui.components.MissaPanel
import com.missa.b360.ui.components.MissaSectionTitle
import com.missa.b360.ui.components.MissaTopAppBar
import com.missa.b360.ui.theme.MissaCanvas
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted
import com.missa.b360.ui.theme.Red40

/** Fournisseurs : liste compacte, recherche immédiate et actions cohérentes avec les autres modules. */
@Composable
fun FournisseursScreen(
    onBack: () -> Unit,
    /** Vrai lorsqu'une action rapide de l'accueil demande directement la création. */
    openCreate: Boolean = false,
    viewModel: FournisseursViewModel = hiltViewModel(),
) {
    val fournisseurs by viewModel.fournisseurs.collectAsState(initial = emptyList())
    var recherche by rememberSaveable { mutableStateOf("") }
    var formVisible by remember { mutableStateOf(false) }
    var fournisseurEdite by remember { mutableStateOf<FournisseurEntity?>(null) }

    LaunchedEffect(openCreate) {
        if (openCreate) {
            fournisseurEdite = null
            formVisible = true
        }
    }

    val filtres = fournisseurs.filter {
        recherche.isBlank() ||
            it.nom.contains(recherche, ignoreCase = true) ||
            it.code.contains(recherche, ignoreCase = true) ||
            it.telephone.contains(recherche)
    }

    Scaffold(
        containerColor = MissaCanvas,
        topBar = { MissaTopAppBar(title = androidx.compose.ui.res.stringResource(R.string.module_fournisseurs), onBack = onBack) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(
                horizontal = MissaLayout.screenHorizontal,
                vertical = MissaLayout.screenVertical,
            ),
            verticalArrangement = Arrangement.spacedBy(MissaLayout.itemGap),
        ) {
            item {
                Button(
                    onClick = {
                        fournisseurEdite = null
                        formVisible = true
                    },
                    modifier = Modifier.fillMaxWidth().height(MissaLayout.actionHeight),
                    colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.primary),
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(androidx.compose.ui.res.stringResource(R.string.fournisseurs_ajouter), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
            item {
                OutlinedTextField(
                    value = recherche,
                    onValueChange = { recherche = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(androidx.compose.ui.res.stringResource(R.string.fournisseurs_recherche)) },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    singleLine = true,
                )
            }
            item {
                MissaSectionTitle(
                    title = androidx.compose.ui.res.stringResource(R.string.module_fournisseurs),
                    subtitle = androidx.compose.ui.res.stringResource(R.string.fournisseurs_total, filtres.size),
                )
            }
            if (filtres.isEmpty()) {
                item {
                    MissaEmptyState(
                        icon = Icons.Outlined.Business,
                        title = androidx.compose.ui.res.stringResource(R.string.fournisseurs_vide),
                        action = {
                            TextButton(onClick = {
                                fournisseurEdite = null
                                formVisible = true
                            }) {
                                Text(androidx.compose.ui.res.stringResource(R.string.fournisseurs_ajouter))
                            }
                        },
                    )
                }
            } else {
                items(filtres, key = { it.id }) { fournisseur ->
                    FournisseurRow(
                        fournisseur = fournisseur,
                        onOpen = {
                            fournisseurEdite = fournisseur
                            formVisible = true
                        },
                        onDeactivate = { viewModel.desactiver(fournisseur.id) },
                    )
                }
            }
        }
    }

    if (formVisible) {
        val fournisseur = fournisseurEdite
        FournisseurFormDialog(
            fournisseur = fournisseur,
            onDismiss = { formVisible = false },
            onConfirm = { nom, tel ->
                if (fournisseur == null) {
                    viewModel.creer(nom, tel, doublonConfirme = true)
                } else {
                    viewModel.modifier(fournisseur.id, nom, tel)
                }
                formVisible = false
            },
        )
    }
}

@Composable
private fun FournisseurRow(
    fournisseur: FournisseurEntity,
    onOpen: () -> Unit,
    onDeactivate: () -> Unit,
) {
    MissaPanel(modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.size(38.dp),
                shape = CircleShape,
                color = androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Business,
                    contentDescription = null,
                    tint = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(9.dp),
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(fournisseur.nom, color = MissaInk, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${fournisseur.code} · ${fournisseur.telephone}", color = MissaMuted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            TextButton(onClick = onDeactivate, contentPadding = PaddingValues(0.dp)) {
                Text(androidx.compose.ui.res.stringResource(R.string.fournisseurs_desactiver), color = Red40, fontSize = 10.sp)
            }
        }
    }
}
