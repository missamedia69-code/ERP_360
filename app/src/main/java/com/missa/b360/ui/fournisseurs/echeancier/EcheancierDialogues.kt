package com.missa.b360.ui.fournisseurs.echeancier

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.missa.b360.R
import com.missa.b360.ui.components.MissaChampDate
import com.missa.b360.ui.components.MissaChampTexte
import com.missa.b360.ui.components.MissaClavier
import com.missa.b360.ui.components.MissaFormDialogue
import com.missa.b360.ui.fournisseurs.components.FournisseurCouleurs
import com.missa.b360.ui.icons.Iv

/** Planifier un paiement : montant (au plus le reste à planifier) et date prévue. */
@Composable
internal fun DialoguePlanification(
    reste: Double,
    onConfirmer: (montant: Double, date: Long) -> Unit,
    onAnnuler: () -> Unit,
) {
    var montant by remember { mutableStateOf(Math.round(reste * 100.0).div(100.0).toString()) }
    var date by remember { mutableStateOf<Long?>(null) }
    val valeur = montant.replace(',', '.').toDoubleOrNull()
    MissaFormDialogue(
        titre = stringResource(R.string.four_echeancier_planifier),
        icone = Iv.Calendar,
        couleur = FournisseurCouleurs.Nuit,
        onFermer = onAnnuler,
        libelleValider = stringResource(R.string.four_echeancier_planifier),
        validerActif = valeur != null && valeur > 0.0 && date != null,
        onValider = { if (valeur != null && date != null) onConfirmer(valeur, date!!) },
    ) {
        MissaChampTexte(montant, { montant = it }, stringResource(R.string.four_echeancier_montant), clavier = MissaClavier.DECIMAL)
        MissaChampDate(date, { date = it }, stringResource(R.string.four_echeancier_date), requis = true)
    }
}

/** Reporter un paiement planifié : nouvelle date ; l'ancienne reste dans l'historique. */
@Composable
internal fun DialogueReport(
    dateInitiale: Long,
    onConfirmer: (date: Long) -> Unit,
    onAnnuler: () -> Unit,
) {
    var date by remember { mutableStateOf<Long?>(dateInitiale) }
    MissaFormDialogue(
        titre = stringResource(R.string.four_echeancier_reporter),
        icone = Iv.Calendar,
        couleur = FournisseurCouleurs.Nuit,
        onFermer = onAnnuler,
        libelleValider = stringResource(R.string.four_echeancier_reporter),
        validerActif = date != null && date != dateInitiale,
        onValider = { date?.let(onConfirmer) },
    ) {
        MissaChampDate(date, { date = it }, stringResource(R.string.four_echeancier_date), requis = true)
    }
}
