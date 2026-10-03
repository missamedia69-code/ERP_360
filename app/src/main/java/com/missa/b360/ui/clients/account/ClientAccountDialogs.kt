package com.missa.b360.ui.clients.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.semantics.Role
import com.missa.b360.ui.clients.components.ClientPuce
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.missa.b360.R
import com.missa.b360.core.domain.usecase.ClientPaymentModes
import com.missa.b360.ui.clients.components.clientMoney
import com.missa.b360.ui.clients.form.ChoixClient
import com.missa.b360.ui.components.MissaChampTexte
import com.missa.b360.ui.components.MissaClavier

private fun String.libelleMode(): Int = when (this) {
    ClientPaymentModes.ESPECES -> R.string.cli_mode_especes
    ClientPaymentModes.MOBILE_MONEY -> R.string.cli_mode_mobile_money
    ClientPaymentModes.VIREMENT -> R.string.cli_mode_virement
    ClientPaymentModes.CHEQUE -> R.string.cli_mode_cheque
    else -> R.string.cli_mode_autre
}

/** Encaissement : montant (prérempli avec le reste dû), mode de règlement et note facultative. */
@Composable
internal fun EncaissementDialog(
    montantInitial: Double,
    devise: String,
    cibleFacture: Boolean,
    onValider: (montant: String, mode: String, note: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var montant by rememberSaveable { mutableStateOf(texteMontant(montantInitial)) }
    var mode by rememberSaveable { mutableStateOf(ClientPaymentModes.ESPECES) }
    var note by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (cibleFacture) R.string.cli_encaisser_facture else R.string.cli_encaisser)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.cli_reste_du, clientMoney(montantInitial, devise)))
                MissaChampTexte(
                    valeur = montant, onValeur = { montant = it }, libelle = stringResource(R.string.cli_montant),
                    clavier = MissaClavier.DECIMAL, requis = true,
                )
                ChoixClient(
                    libelle = R.string.cli_mode_paiement,
                    valeurAffichee = stringResource(mode.libelleMode()),
                    options = ClientPaymentModes.TOUS.map { it to stringResource(it.libelleMode()) },
                    onChoix = { mode = it },
                )
                MissaChampTexte(valeur = note, onValeur = { note = it.take(240) }, libelle = stringResource(R.string.cli_note_facultative))
            }
        },
        confirmButton = {
            TextButton(onClick = { onValider(montant, mode, note) }, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.cli_enregistrer))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.cli_annuler)) } },
    )
}

private fun texteMontant(valeur: Double): String =
    if (valeur > 0.0 && valeur.isFinite()) java.math.BigDecimal.valueOf(valeur).stripTrailingZeros().toPlainString() else ""

private val DELAIS_JOURS = listOf(0, 3, 7, 15, 30)

/** Promesse de paiement : montant promis et délai choisi parmi quelques échéances usuelles. */
@Composable
internal fun PromesseDialog(
    encours: Double,
    devise: String,
    onValider: (montant: String, jours: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var montant by rememberSaveable { mutableStateOf(texteMontant(encours)) }
    var jours by rememberSaveable { mutableStateOf(7) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.cli_type_promesse)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.cli_reste_du, clientMoney(encours, devise)))
                MissaChampTexte(
                    valeur = montant, onValeur = { montant = it }, libelle = stringResource(R.string.cli_montant_promis),
                    clavier = MissaClavier.DECIMAL, requis = true,
                )
                Text(stringResource(R.string.cli_promesse_delai), modifier = Modifier.padding(top = 2.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(DELAIS_JOURS.size) { index ->
                        val delai = DELAIS_JOURS[index]
                        ClientPuce(
                            libelle = if (delai == 0) stringResource(R.string.cli_aujourdhui) else stringResource(R.string.cli_dans_jours, delai),
                            actif = jours == delai,
                            onClick = { jours = delai },
                            role = Role.RadioButton,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onValider(montant, jours) }, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.cli_enregistrer))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.cli_annuler)) } },
    )
}
