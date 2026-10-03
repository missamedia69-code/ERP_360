package com.missa.b360.ui.fournisseurs.documents

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.missa.b360.R
import com.missa.b360.core.domain.model.FournisseurConformiteRules
import com.missa.b360.core.domain.model.FournisseurRules
import com.missa.b360.ui.components.MissaEmptyState
import com.missa.b360.ui.components.MissaTopAppBar
import com.missa.b360.ui.fournisseurs.components.FournisseurCouleurs
import com.missa.b360.ui.fournisseurs.components.formatDate
import com.missa.b360.ui.fournisseurs.components.libelleDocument
import com.missa.b360.ui.fournisseurs.dossier.BadgeVerification
import com.missa.b360.ui.icons.Iv
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted
import com.missa.b360.ui.theme.OnbConfigCard

/** Route `fournisseurs_documents` : documents expirés ou à renouveler sous 90 jours. */
@Composable
fun FournisseurDocumentsScreen(
    onBack: () -> Unit,
    onOuvrirConformite: (Long) -> Unit,
    viewModel: FournisseurDocumentsViewModel = hiltViewModel(),
) {
    val etat by viewModel.etat.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().background(Color.White)) {
        MissaTopAppBar(title = stringResource(R.string.four_documents_titre), onBack = onBack, titreCentre = false)
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 10.dp, end = 10.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (!etat.chargement && etat.documents.isEmpty()) {
                item {
                    MissaEmptyState(
                        icon = Iv.CheckCircle,
                        title = stringResource(R.string.four_action_rien),
                        description = stringResource(R.string.four_documents_vide),
                    )
                }
            }
            items(etat.documents, key = { it.document.id }) { ligne ->
                val document = ligne.document
                val alerte = FournisseurConformiteRules.alerte(document, etat.now)
                val couleur = when (alerte) {
                    FournisseurRules.AlerteDocument.EXPIRE, FournisseurRules.AlerteDocument.FORTE -> FournisseurCouleurs.Bloque
                    FournisseurRules.AlerteDocument.ALERTE -> FournisseurCouleurs.Attention
                    else -> MissaMuted
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = OnbConfigCard,
                    modifier = Modifier.fillMaxWidth().clickable { onOuvrirConformite(document.fournisseurId) },
                ) {
                    Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(ligne.fournisseurNom, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MissaInk, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(libelleDocument(document.typeDocument), fontSize = 11.sp, color = MissaMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            document.dateExpiration?.let {
                                Text(stringResource(R.string.four_expire_le) + " " + formatDate(it), fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = couleur)
                            }
                        }
                        BadgeVerification(document.verification)
                    }
                }
            }
        }
    }
}
