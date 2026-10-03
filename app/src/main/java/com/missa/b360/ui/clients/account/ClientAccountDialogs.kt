package com.missa.b360.ui.clients.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.missa.b360.ui.clients.components.BoutonClientPlein as Button
import com.missa.b360.ui.components.BoutonContourMissa as OutlinedButton
import com.missa.b360.ui.clients.components.BoutonClient
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.size
import androidx.compose.ui.res.painterResource
import com.missa.b360.ui.icons.Iv
import com.missa.b360.ui.clients.components.ClientCouleurs
import com.missa.b360.ui.clients.components.ClientEncadre
import com.missa.b360.ui.clients.components.ClientFeuille
import com.missa.b360.ui.components.MissaChampDate
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted
import androidx.compose.ui.semantics.Role
import androidx.compose.material3.Text
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

/** Encaissement : montant (prérempli avec le reste dû), mode de règlement et note facultative, en feuille basse. */
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
    ClientFeuille(
        titre = stringResource(if (cibleFacture) R.string.cli_encaisser_facture else R.string.cli_encaisser),
        onFermer = onDismiss,
        actions = {
            BoutonClient(onClick = onDismiss, modifier = Modifier.weight(0.75f), plein = false) { Text(stringResource(R.string.cli_annuler), color = MissaInk) }
            Button(onClick = { onValider(montant, mode, note) }, modifier = Modifier.weight(1.25f).heightIn(min = 48.dp)) {
                Icon(painterResource(Iv.Check), contentDescription = null, modifier = Modifier.size(15.dp))
                Text(stringResource(R.string.cli_enregistrer))
            }
        },
    ) {
        EncadreResteDu(montantInitial, devise)
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
}

@Composable
private fun EncadreResteDu(montant: Double, devise: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF6F3FC))
            .padding(horizontal = 11.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(R.string.cli_reste_du_libelle), color = MissaMuted, fontSize = 10.sp, modifier = Modifier.weight(1f))
        Text(clientMoney(montant, devise), color = MissaInk, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
    }
}

private fun texteMontant(valeur: Double): String =
    if (valeur > 0.0 && valeur.isFinite()) java.math.BigDecimal.valueOf(valeur).stripTrailingZeros().toPlainString() else ""

/** Échéances proposées en grille 2 × 2 ; la dernière ouvre un sélecteur de date. */
private const val ECHEANCE_DATE = -1
private val ECHEANCES = listOf(0, 3, 7, ECHEANCE_DATE)

private fun joursJusquau(dateMillis: Long): Int {
    val jour = java.time.Instant.ofEpochMilli(dateMillis).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
    return java.time.temporal.ChronoUnit.DAYS.between(java.time.LocalDate.now(), jour).toInt().coerceAtLeast(0)
}

/** Promesse de paiement en feuille basse : reste dû, montant promis et échéance (aujourd'hui, 3 j, 7 j ou date choisie). */
@Composable
internal fun PromesseDialog(
    encours: Double,
    devise: String,
    onValider: (montant: String, jours: Int) -> Unit,
    onDismiss: () -> Unit,
    sousTitre: String? = null,
) {
    var montant by rememberSaveable { mutableStateOf(texteMontant(encours)) }
    var choix by rememberSaveable { mutableStateOf(7) }
    var date by rememberSaveable { mutableStateOf<Long?>(null) }
    val dateRequise = choix == ECHEANCE_DATE && date == null
    ClientFeuille(
        titre = stringResource(R.string.cli_type_promesse),
        sousTitre = sousTitre,
        onFermer = onDismiss,
        actions = {
            BoutonClient(onClick = onDismiss, modifier = Modifier.weight(0.75f), plein = false) { Text(stringResource(R.string.cli_annuler), color = MissaInk) }
            Button(
                onClick = { onValider(montant, if (choix == ECHEANCE_DATE) date?.let { joursJusquau(it) } ?: 0 else choix) },
                enabled = !dateRequise,
                modifier = Modifier.weight(1.25f).heightIn(min = 48.dp),
            ) {
                Icon(painterResource(Iv.Check), contentDescription = null, modifier = Modifier.size(15.dp))
                Text(stringResource(R.string.cli_enregistrer))
            }
        },
    ) {
        EncadreResteDu(encours, devise)
        MissaChampTexte(
            valeur = montant, onValeur = { montant = it }, libelle = stringResource(R.string.cli_montant_promis),
            clavier = MissaClavier.DECIMAL, requis = true,
        )
        Text(stringResource(R.string.cli_promesse_delai), color = Color(0xFF626D83), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
        ECHEANCES.chunked(2).forEach { ligne ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ligne.forEach { echeance ->
                    val libelle = when (echeance) {
                        0 -> stringResource(R.string.cli_aujourdhui)
                        ECHEANCE_DATE -> stringResource(R.string.cli_choisir_date)
                        else -> stringResource(R.string.cli_dans_jours_long, echeance)
                    }
                    CaseEcheance(libelle, actif = choix == echeance, onClick = { choix = echeance }, modifier = Modifier.weight(1f))
                }
            }
        }
        if (choix == ECHEANCE_DATE) {
            MissaChampDate(
                valeur = date, onValeur = { date = it }, libelle = stringResource(R.string.cli_choisir_date),
                requis = true,
            )
        }
    }
}

@Composable
private fun CaseEcheance(libelle: String, actif: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val forme = RoundedCornerShape(11.dp)
    Box(
        modifier
            .heightIn(min = 48.dp)
            .clip(forme)
            .background(if (actif) ClientCouleurs.VioletPale else Color.White)
            .border(BorderStroke(if (actif) 1.5.dp else 1.dp, if (actif) ClientCouleurs.Violet else ClientCouleurs.Trait), forme)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            libelle, color = if (actif) ClientCouleurs.VioletProfond else Color(0xFF5D6880), fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, maxLines = 2,
        )
    }
}
