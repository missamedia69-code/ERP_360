package com.missa.b360.ui.clients.components

import androidx.annotation.StringRes
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import com.missa.b360.R

/** Messages brefs (confirmation ou refus) affichés en bandeau ; l'état n'en garde qu'un à la fois. */
enum class ClientNotice(@StringRes val message: Int) {
    STATUT_CHANGE(R.string.cli_notice_statut_change),
    COORDONNEES_MANQUANTES(R.string.cli_notice_coordonnees_manquantes),
    FISCAL_MANQUANT(R.string.cli_notice_fiscal_manquant),
    TRANSITION_INTERDITE(R.string.cli_notice_transition_interdite),
    LICENCE_EXPIREE(R.string.cli_notice_licence),
    PERMISSION_REFUSEE(R.string.cli_notice_permission),
    ERREUR(R.string.cli_notice_erreur),
    RELANCE_ENREGISTREE(R.string.cli_notice_relance_enregistree),
    PROMESSE_ENREGISTREE(R.string.cli_notice_promesse_enregistree),
    PROMESSE_INVALIDE(R.string.cli_notice_promesse_invalide),
    NOTE_ENREGISTREE(R.string.cli_notice_note_enregistree),
    ENCAISSEMENT_ENREGISTRE(R.string.cli_notice_encaissement),
    ENCAISSEMENT_INVALIDE(R.string.cli_notice_encaissement_invalide),
    PDF_ERREUR(R.string.cli_notice_pdf_erreur),
}

/** Affiche le message dans le bandeau puis le marque comme lu. */
@Composable
internal fun ClientNoticeEffect(notice: ClientNotice?, hote: SnackbarHostState, onLu: () -> Unit) {
    val texte = notice?.let { stringResource(it.message) }
    LaunchedEffect(notice) {
        if (texte != null) {
            hote.showSnackbar(texte)
            onLu()
        }
    }
}
