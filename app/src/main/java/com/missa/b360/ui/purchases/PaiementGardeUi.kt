package com.missa.b360.ui.purchases

import androidx.annotation.StringRes
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.missa.b360.R
import com.missa.b360.core.domain.model.PaymentGuardReason
import com.missa.b360.ui.theme.MissaInk

/** Texte de la garde de paiement fournisseur : jamais de numéro de compte, seulement le motif. */
@StringRes
fun PaymentGuardReason.messageRes(): Int = when (this) {
    PaymentGuardReason.STATUT_INTERDIT, PaymentGuardReason.PAIEMENT_BLOQUE -> R.string.ach_erreur_paiement_bloque
    PaymentGuardReason.MONTANT_INVALIDE -> R.string.ach_erreur_montant
    PaymentGuardReason.AU_DESSUS_PLAFOND -> R.string.ach_garde_plafond
    PaymentGuardReason.COMPTE_REQUIS -> R.string.ach_garde_compte_requis
    PaymentGuardReason.COMPTE_NON_VERIFIE -> R.string.ach_garde_compte_non_verifie
    PaymentGuardReason.COMPTE_REJETE -> R.string.ach_garde_compte_rejete
    PaymentGuardReason.COMPTE_RECENT_NON_VERIFIE -> R.string.ach_garde_compte_recent
}

/** Demande une confirmation explicite avant un paiement signalé par la garde. */
@Composable
fun DialogueConfirmationPaiement(
    motif: PaymentGuardReason,
    onConfirmer: () -> Unit,
    onAnnuler: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onAnnuler,
        title = { Text(stringResource(R.string.ach_garde_confirmer_titre), color = MissaInk) },
        text = { Text(stringResource(motif.messageRes()), color = MissaInk) },
        confirmButton = {
            TextButton(onClick = onConfirmer) {
                Text(stringResource(R.string.ach_garde_confirmer_action), color = MissaInk)
            }
        },
        dismissButton = {
            TextButton(onClick = onAnnuler) { Text(stringResource(R.string.st_annuler), color = MissaInk) }
        },
    )
}
