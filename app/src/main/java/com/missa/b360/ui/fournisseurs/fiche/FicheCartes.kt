package com.missa.b360.ui.fournisseurs.fiche

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.missa.b360.R
import com.missa.b360.core.data.entity.FournisseurCompteBancaireEntity
import com.missa.b360.core.data.entity.FournisseurContactEntity
import com.missa.b360.core.data.entity.FournisseurScoreEntity
import com.missa.b360.core.domain.model.ConformiteResume
import com.missa.b360.core.domain.model.FournisseurLigne
import com.missa.b360.core.domain.model.SupplierAccountRules
import com.missa.b360.ui.clients.components.appeler
import com.missa.b360.ui.clients.components.ouvrirWhatsApp
import com.missa.b360.ui.fournisseurs.dossier.BadgeVerification
import com.missa.b360.ui.fournisseurs.components.BoutonFournisseur
import com.missa.b360.ui.fournisseurs.components.CarteFournisseur
import com.missa.b360.ui.fournisseurs.components.FournisseurCouleurs
import com.missa.b360.ui.fournisseurs.components.LigneValeur
import com.missa.b360.ui.fournisseurs.components.formatDate
import com.missa.b360.ui.stock.fmtValeur
import com.missa.b360.ui.theme.MissaMuted

@Composable
internal fun CarteDette(ligne: FournisseurLigne, devise: String, nbFactures: Int, onCompte: () -> Unit) {
    val balance = ligne.balance
    CarteFournisseur(titre = stringResource(R.string.four_a_payer)) {
        LigneValeur(stringResource(R.string.four_kpi_dette), fmtValeur(ligne.dette, devise))
        LigneValeur(
            stringResource(R.string.four_kpi_retard),
            fmtValeur(ligne.enRetard, devise),
            if (ligne.enRetard > 0.0) FournisseurCouleurs.Bloque else com.missa.b360.ui.theme.MissaInk,
        )
        LigneValeur(
            stringResource(R.string.four_fiche_prochaine),
            balance?.prochaineEcheanceAt?.let { formatDate(it) } ?: "—",
        )
        LigneValeur(stringResource(R.string.four_fiche_achats_12m), fmtValeur(balance?.achats12Mois ?: 0.0, devise))
        Text(stringResource(R.string.four_fiche_factures_ouvertes, nbFactures), fontSize = 11.sp, color = MissaMuted)
        BoutonFournisseur(stringResource(R.string.four_fiche_voir_compte), Modifier.fillMaxWidth(), plein = false, onClick = onCompte)
    }
}

@Composable
internal fun CarteFiabilite(score: FournisseurScoreEntity?) {
    CarteFournisseur(titre = stringResource(R.string.four_tri_score)) {
        val note = score?.score
        if (score == null || note == null) {
            Text(stringResource(R.string.four_fiche_fiabilite_nd_desc), fontSize = 11.5.sp, color = MissaMuted)
        } else {
            LigneValeur(stringResource(R.string.four_fiche_score), stringResource(R.string.four_fiche_score_sur, note))
            LigneValeur(stringResource(R.string.four_fiche_ponctualite), pourcent(score.ponctualite))
            LigneValeur(stringResource(R.string.four_fiche_qualite_livraison), pourcent(score.conformite))
            LigneValeur(stringResource(R.string.four_fiche_prix), pourcent(score.prix))
        }
        score?.let {
            Text(stringResource(R.string.four_fiche_mesures, it.nbCommandesMesurees), fontSize = 11.sp, color = MissaMuted)
        }
    }
}

private fun pourcent(valeur: Double?): String = valeur?.let { "${Math.round(it)} %" } ?: "—"

@Composable
internal fun CarteContact(contact: FournisseurContactEntity?) {
    val contexte = LocalContext.current
    CarteFournisseur(titre = stringResource(R.string.four_principal)) {
        if (contact == null) {
            Text(stringResource(R.string.four_aucun_contact), fontSize = 11.5.sp, color = MissaMuted)
            return@CarteFournisseur
        }
        LigneValeur(
            listOfNotNull(contact.nom, contact.prenom).joinToString(" "),
            contact.fonction.orEmpty(),
        )
        val telephone = contact.telephone?.takeIf { it.isNotBlank() }
        val whatsapp = (contact.whatsapp?.takeIf { it.isNotBlank() }) ?: telephone
        telephone?.let { Text(it, fontSize = 11.5.sp, color = MissaMuted) }
        if (telephone != null || whatsapp != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                telephone?.let {
                    BoutonFournisseur(stringResource(R.string.four_fiche_appeler), Modifier.weight(1f), plein = false) { contexte.appeler(it) }
                }
                whatsapp?.let {
                    BoutonFournisseur(stringResource(R.string.four_fiche_whatsapp), Modifier.weight(1f), plein = false) { contexte.ouvrirWhatsApp(it) }
                }
            }
        }
    }
}

@Composable
internal fun CarteComptePaiement(compte: FournisseurCompteBancaireEntity?) {
    CarteFournisseur(titre = stringResource(R.string.four_compte_principal)) {
        if (compte == null) {
            Text(stringResource(R.string.four_aucun_compte), fontSize = 11.5.sp, color = MissaMuted)
            return@CarteFournisseur
        }
        // Jamais de numéro complet : seuls les 4 derniers caractères sont affichés.
        val reference = SupplierAccountRules.masquer(compte.iban ?: compte.numeroCompte ?: compte.numeroMobile)
        LigneValeur(compte.titulaire, reference)
        Row(verticalAlignment = Alignment.CenterVertically) {
            BadgeVerification(compte.verification)
        }
    }
}

@Composable
internal fun CarteConformite(resume: ConformiteResume, onConformite: () -> Unit) {
    CarteFournisseur(titre = stringResource(R.string.four_fiche_conformite)) {
        Text(stringResource(R.string.four_fiche_docs, resume.total), fontSize = 11.5.sp, color = MissaMuted)
        if (resume.conforme && resume.aExpirer == 0) {
            Text(stringResource(R.string.four_fiche_conforme), fontSize = 11.5.sp, color = FournisseurCouleurs.Pret)
        }
        if (resume.expires > 0) {
            Text(stringResource(R.string.four_fiche_docs_expires, resume.expires), fontSize = 11.5.sp, color = FournisseurCouleurs.Bloque)
        }
        if (resume.aExpirer > 0) {
            Text(stringResource(R.string.four_fiche_docs_bientot, resume.aExpirer), fontSize = 11.5.sp, color = FournisseurCouleurs.Attention)
        }
        if (resume.rejetes > 0) {
            Text(stringResource(R.string.four_fiche_docs_rejetes, resume.rejetes), fontSize = 11.5.sp, color = FournisseurCouleurs.Bloque)
        }
        BoutonFournisseur(stringResource(R.string.four_fiche_voir_conformite), Modifier.fillMaxWidth(), plein = false, onClick = onConformite)
    }
}
