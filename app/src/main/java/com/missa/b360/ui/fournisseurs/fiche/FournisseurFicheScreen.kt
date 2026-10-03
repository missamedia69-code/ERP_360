package com.missa.b360.ui.fournisseurs.fiche

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.missa.b360.ui.components.MissaEmptyState
import com.missa.b360.ui.components.MissaTopAppBar
import com.missa.b360.ui.fournisseurs.components.BadgeAptitude
import com.missa.b360.ui.fournisseurs.components.BoutonFournisseur
import com.missa.b360.ui.fournisseurs.components.CarteFournisseur
import com.missa.b360.ui.fournisseurs.components.libelleStatut
import com.missa.b360.ui.fournisseurs.components.libelleRes
import com.missa.b360.ui.icons.Iv
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted

/** Route `fournisseur_fiche/{id}` : fiche 360, de la sécurité (peut-on commander / payer ?) à la fiabilité. */
@Composable
fun FournisseurFicheScreen(
    onBack: () -> Unit,
    onModifier: (Long) -> Unit,
    onCompte: (Long) -> Unit,
    onConformite: (Long) -> Unit,
    onEcheancier: () -> Unit,
    onDossier: (Long) -> Unit,
    viewModel: FournisseurFicheViewModel = hiltViewModel(),
) {
    val etat by viewModel.etat.collectAsStateWithLifecycle()
    val id = viewModel.fournisseurId
    val ligne = etat.ligne
    Column(Modifier.fillMaxSize().background(Color.White)) {
        MissaTopAppBar(
            title = ligne?.fournisseur?.nom ?: stringResource(R.string.module_fournisseurs),
            onBack = onBack,
            titreCentre = false,
        )
        if (ligne == null) {
            if (etat.introuvable) {
                MissaEmptyState(
                    icon = Iv.Handshake,
                    title = stringResource(R.string.four_introuvable),
                    modifier = Modifier.padding(12.dp),
                )
            }
            return@Column
        }
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 10.dp, end = 10.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { CarteAptitude(ligne) }
            item { CarteDette(ligne, etat.devise, etat.factures.size, onCompte = { onCompte(id) }) }
            item { CarteFiabilite(ligne.score) }
            item { CarteContact(etat.contact) }
            item { CarteComptePaiement(etat.compte) }
            item { CarteConformite(etat.conformite, onConformite = { onConformite(id) }) }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        BoutonFournisseur(stringResource(R.string.four_modifier), Modifier.weight(1f)) { onModifier(id) }
                        BoutonFournisseur(stringResource(R.string.four_fiche_echeancier), Modifier.weight(1f), plein = false, onClick = onEcheancier)
                    }
                    BoutonFournisseur(stringResource(R.string.four_fiche_dossier), Modifier.fillMaxWidth(), plein = false) { onDossier(id) }
                }
            }
        }
    }
}

@Composable
private fun CarteAptitude(ligne: com.missa.b360.core.domain.model.FournisseurLigne) {
    val f = ligne.fournisseur
    CarteFournisseur(titre = "${f.code} · ${f.pays}") {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            BadgeAptitude(ligne.aptitude.niveau)
            Text(libelleStatut(f.statut), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MissaMuted)
        }
        ligne.aptitude.motifs.forEach { motif ->
            Text(stringResource(motif.libelleRes()), fontSize = 11.5.sp, color = MissaInk)
        }
        f.motifBlocage?.takeIf { it.isNotBlank() }?.let { motif ->
            Text(stringResource(R.string.four_motif_blocage_affiche, motif), fontSize = 11.sp, color = MissaMuted)
        }
    }
}
