package com.missa.b360.ui.fournisseurs.compte

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.missa.b360.R
import com.missa.b360.core.data.entity.FournisseurCompteBancaireEntity
import com.missa.b360.core.data.entity.VerificationStatut
import com.missa.b360.core.domain.model.AgedBalanceRules
import com.missa.b360.core.domain.model.FournisseurFactureOuverte
import com.missa.b360.core.domain.model.SupplierAccountRules
import com.missa.b360.ui.components.MissaEmptyState
import com.missa.b360.ui.components.MissaTopAppBar
import com.missa.b360.ui.fournisseurs.dossier.BadgeVerification
import com.missa.b360.ui.fournisseurs.components.BoutonFournisseur
import com.missa.b360.ui.fournisseurs.components.CarteFournisseur
import com.missa.b360.ui.fournisseurs.components.FournisseurCouleurs
import com.missa.b360.ui.fournisseurs.components.LigneValeur
import com.missa.b360.ui.fournisseurs.components.MessageEtat
import com.missa.b360.ui.fournisseurs.components.formatDate
import com.missa.b360.ui.fournisseurs.dossier.DialogueCompte
import com.missa.b360.ui.icons.Iv
import com.missa.b360.ui.stock.fmtValeur
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted

/** Route `fournisseur_compte/{id}` : solde dû, factures ouvertes et comptes de paiement (numéros masqués). */
@Composable
fun FournisseurCompteScreen(
    onBack: () -> Unit,
    onPayer: () -> Unit,
    viewModel: FournisseurCompteViewModel = hiltViewModel(),
) {
    val etat by viewModel.etat.collectAsStateWithLifecycle()
    var ajoutOuvert by remember { mutableStateOf(false) }
    LaunchedEffect(etat.message) {
        if (etat.message != null) {
            kotlinx.coroutines.delay(3_000)
            viewModel.effacerMessage()
        }
    }
    val now = remember { System.currentTimeMillis() }
    Column(Modifier.fillMaxSize().background(Color.White)) {
        MissaTopAppBar(
            title = etat.ligne?.fournisseur?.nom ?: stringResource(R.string.four_compte_titre),
            onBack = onBack,
            titreCentre = false,
        )
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 10.dp, end = 10.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { MessageEtat(etat.message) }
            item {
                CarteFournisseur(stringResource(R.string.four_compte_titre)) {
                    val ligne = etat.ligne
                    LigneValeur(stringResource(R.string.four_kpi_dette), fmtValeur(ligne?.dette ?: 0.0, etat.devise))
                    LigneValeur(
                        stringResource(R.string.four_kpi_retard),
                        fmtValeur(ligne?.enRetard ?: 0.0, etat.devise),
                        if ((ligne?.enRetard ?: 0.0) > 0.0) FournisseurCouleurs.Bloque else MissaInk,
                    )
                    BoutonFournisseur(stringResource(R.string.four_compte_payer), Modifier.fillMaxWidth(), onClick = onPayer)
                }
            }
            item { Titre(stringResource(R.string.four_compte_factures, etat.factures.size)) }
            if (etat.factures.isEmpty()) {
                item { Text(stringResource(R.string.four_compte_aucune_facture), fontSize = 12.sp, color = MissaMuted) }
            }
            items(etat.factures, key = { it.recordId }) { LigneFactureCompte(it, etat.devise, now) }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Titre(stringResource(R.string.four_paiement_section)) }
                    BoutonFournisseur(stringResource(R.string.four_ajouter), plein = false) { ajoutOuvert = true }
                }
            }
            if (etat.comptes.isEmpty()) {
                item { Text(stringResource(R.string.four_aucun_compte), fontSize = 12.sp, color = MissaMuted) }
            }
            items(etat.comptes, key = { it.id }) { compte ->
                CarteCompte(
                    compte = compte,
                    onVerifier = { viewModel.verifier(compte.id, true) },
                    onRejeter = { viewModel.verifier(compte.id, false) },
                )
            }
            item { Text(stringResource(R.string.four_verification_interne_seulement), fontSize = 10.sp, color = MissaMuted) }
        }
    }
    if (ajoutOuvert) {
        DialogueCompte(
            onConfirmer = { titulaire, banque, numero, iban, bic, operateur, numeroMobile, principal ->
                viewModel.ajouter(titulaire, banque, numero, iban, bic, operateur, numeroMobile, principal)
                ajoutOuvert = false
            },
            onAnnuler = { ajoutOuvert = false },
        )
    }
}

@Composable
private fun Titre(texte: String) {
    Text(texte, fontSize = 13.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = MissaInk)
}

@Composable
private fun LigneFactureCompte(f: FournisseurFactureOuverte, devise: String, now: Long) {
    val retard = AgedBalanceRules.joursDeRetard(f.dueAt, now)
    CarteFournisseur(f.reference) {
        LigneValeur(stringResource(R.string.four_action_echeance, formatDate(f.dueAt)), fmtValeur(f.outstanding, devise))
        if (retard > 0) {
            Text(stringResource(R.string.four_action_retard_jours, retard), fontSize = 11.sp, color = FournisseurCouleurs.Bloque)
        }
    }
}

@Composable
private fun CarteCompte(compte: FournisseurCompteBancaireEntity, onVerifier: () -> Unit, onRejeter: () -> Unit) {
    // Jamais de numéro complet à l'écran : les 4 derniers caractères seulement.
    val reference = SupplierAccountRules.masquer(compte.iban ?: compte.numeroCompte ?: compte.numeroMobile)
    val support = compte.banque ?: compte.operateurMobile
    CarteFournisseur(compte.titulaire) {
        LigneValeur(support.orEmpty(), reference)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            BadgeVerification(compte.verification)
            if (compte.principal) Text(stringResource(R.string.four_compte_principal_court), fontSize = 11.sp, color = MissaMuted)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (compte.verification != VerificationStatut.VERIFIE) {
                BoutonFournisseur(stringResource(R.string.four_verifier), Modifier.weight(1f), onClick = onVerifier)
            }
            if (compte.verification != VerificationStatut.REJETE) {
                BoutonFournisseur(stringResource(R.string.four_rejeter), Modifier.weight(1f), plein = false, onClick = onRejeter)
            }
        }
    }
}
