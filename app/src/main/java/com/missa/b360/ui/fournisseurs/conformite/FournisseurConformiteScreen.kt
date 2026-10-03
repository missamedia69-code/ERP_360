package com.missa.b360.ui.fournisseurs.conformite

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.missa.b360.R
import com.missa.b360.core.data.entity.FournisseurDocumentEntity
import com.missa.b360.core.data.entity.VerificationStatut
import com.missa.b360.core.domain.model.FournisseurConformiteRules
import com.missa.b360.core.domain.model.FournisseurRules
import com.missa.b360.ui.components.MissaEmptyState
import com.missa.b360.ui.components.MissaTopAppBar
import com.missa.b360.ui.fournisseurs.components.BadgeAptitude
import com.missa.b360.ui.fournisseurs.dossier.BadgeVerification
import com.missa.b360.ui.fournisseurs.components.BoutonFournisseur
import com.missa.b360.ui.fournisseurs.components.CarteFournisseur
import com.missa.b360.ui.fournisseurs.components.FournisseurCouleurs
import com.missa.b360.ui.fournisseurs.components.MessageEtat
import com.missa.b360.ui.fournisseurs.components.formatDate
import com.missa.b360.ui.fournisseurs.components.libelleDocument
import com.missa.b360.ui.fournisseurs.components.libelleRes
import com.missa.b360.ui.fournisseurs.dossier.DialogueDocument
import com.missa.b360.ui.icons.Iv
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted

/** Route `fournisseur_conformite/{id}` : documents, échéances et motifs qui empêchent de commander ou payer. */
@Composable
fun FournisseurConformiteScreen(
    onBack: () -> Unit,
    viewModel: FournisseurConformiteViewModel = hiltViewModel(),
) {
    val etat by viewModel.etat.collectAsStateWithLifecycle()
    var ajoutOuvert by remember { mutableStateOf(false) }
    LaunchedEffect(etat.message) {
        if (etat.message != null) {
            kotlinx.coroutines.delay(3_000)
            viewModel.effacerMessage()
        }
    }
    Column(Modifier.fillMaxSize().background(Color.White)) {
        MissaTopAppBar(
            title = etat.ligne?.fournisseur?.nom ?: stringResource(R.string.four_fiche_conformite),
            onBack = onBack,
            titreCentre = false,
        )
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 10.dp, end = 10.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { MessageEtat(etat.message) }
            etat.ligne?.let { ligne ->
                item {
                    CarteFournisseur(stringResource(R.string.four_conformite_aptitude)) {
                        BadgeAptitude(ligne.aptitude.niveau)
                        ligne.aptitude.motifs.forEach {
                            Text(stringResource(it.libelleRes()), fontSize = 11.5.sp, color = MissaInk)
                        }
                    }
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.four_documents) + " (${etat.resume.total})",
                        fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MissaInk,
                        modifier = Modifier.weight(1f),
                    )
                    BoutonFournisseur(stringResource(R.string.four_ajouter), plein = false) { ajoutOuvert = true }
                }
            }
            if (etat.documents.isEmpty() && !etat.chargement) {
                item { MissaEmptyState(icon = Iv.Description, title = stringResource(R.string.four_aucun_document)) }
            }
            items(etat.documents, key = { it.id }) { document ->
                CarteDocument(
                    document = document,
                    now = etat.now,
                    onVerifier = { viewModel.verifier(document.id, true) },
                    onRejeter = { viewModel.verifier(document.id, false) },
                    onRetirer = { viewModel.retirer(document.id) },
                )
            }
        }
    }
    if (ajoutOuvert) {
        DialogueDocument(
            onConfirmer = { type, reference, chemin, emission, expiration ->
                viewModel.ajouter(type, reference, chemin, emission, expiration)
                ajoutOuvert = false
            },
            onAnnuler = { ajoutOuvert = false },
        )
    }
}

@Composable
private fun CarteDocument(
    document: FournisseurDocumentEntity,
    now: Long,
    onVerifier: () -> Unit,
    onRejeter: () -> Unit,
    onRetirer: () -> Unit,
) {
    val alerte = FournisseurConformiteRules.alerte(document, now)
    val couleur = when (alerte) {
        FournisseurRules.AlerteDocument.EXPIRE, FournisseurRules.AlerteDocument.FORTE -> FournisseurCouleurs.Bloque
        FournisseurRules.AlerteDocument.ALERTE -> FournisseurCouleurs.Attention
        else -> MissaMuted
    }
    CarteFournisseur(libelleDocument(document.typeDocument) + (document.reference?.let { " · $it" } ?: "")) {
        document.dateExpiration?.let {
            Text(stringResource(R.string.four_expire_le) + " " + formatDate(it), fontSize = 11.5.sp, color = couleur)
        }
        BadgeVerification(document.verification)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            if (document.verification != VerificationStatut.VERIFIE) {
                BoutonFournisseur(stringResource(R.string.four_verifier), Modifier.weight(1f), onClick = onVerifier)
            }
            if (document.verification != VerificationStatut.REJETE) {
                BoutonFournisseur(stringResource(R.string.four_rejeter), Modifier.weight(1f), plein = false, onClick = onRejeter)
            }
            BoutonFournisseur(stringResource(R.string.four_retirer), Modifier.weight(1f), plein = false, onClick = onRetirer)
        }
    }
}
