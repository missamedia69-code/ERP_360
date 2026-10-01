package com.missa.b360.ui.clients.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.missa.b360.R
import com.missa.b360.core.data.entity.ClientBalanceEntity
import com.missa.b360.core.data.entity.ClientEntity
import com.missa.b360.core.domain.model.CreditInput
import com.missa.b360.core.domain.model.CreditPolicy
import com.missa.b360.core.domain.model.CreditReason
import com.missa.b360.core.domain.model.RiskLevel
import com.missa.b360.core.domain.model.SaleVerdict
import com.missa.b360.ui.icons.Iv
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted

internal fun CreditReason.message(): Int = when (this) {
    CreditReason.STATUT_BLOQUE_CREDIT -> R.string.cli_raison_bloque_credit
    CreditReason.STATUT_BLOQUE_ADMINISTRATIF -> R.string.cli_raison_bloque_admin
    CreditReason.STATUT_INACTIF -> R.string.cli_raison_inactif
    CreditReason.FICHE_INCOMPLETE -> R.string.cli_raison_fiche_incomplete
    CreditReason.SOUS_SURVEILLANCE -> R.string.cli_raison_surveillance
    CreditReason.LIMITE_ATTEINTE -> R.string.cli_raison_limite_atteinte
    CreditReason.LIMITE_PROCHE -> R.string.cli_raison_limite_proche
    CreditReason.LIMITE_DEPASSEE_PAR_VENTE -> R.string.cli_raison_limite_depassee
    CreditReason.RETARD_CRITIQUE -> R.string.cli_raison_retard_critique
    CreditReason.RETARD_MODERE -> R.string.cli_raison_retard_modere
    CreditReason.MONTANT_INVALIDE -> R.string.cli_raison_montant_invalide
}

/**
 * Risque du client et verdict « puis-je vendre à crédit maintenant ? » pour le panier courant.
 * La couleur porte le risque, toujours doublée d'une icône et d'un texte.
 */
@Composable
internal fun ClientCreditBanner(
    client: ClientEntity,
    balance: ClientBalanceEntity?,
    montantVente: Double,
    montantRegle: Double,
    devise: String,
    modifier: Modifier = Modifier,
) {
    val entree = CreditInput(
        statut = client.statut,
        limiteCredit = client.limiteCredit,
        encours = balance?.encours ?: 0.0,
        enRetard = balance?.enRetard ?: 0.0,
        joursRetardMax = balance?.joursRetardMax ?: 0,
    )
    val evaluation = remember(entree) { CreditPolicy.evaluate(entree) }
    val verdict = remember(entree, montantVente, montantRegle) { CreditPolicy.canSell(entree, montantVente, montantRegle) }
    val raison = when (verdict) {
        is SaleVerdict.Block -> verdict.raison
        is SaleVerdict.Warn -> verdict.raison
        SaleVerdict.Allow -> null
    }
    val risque = if (verdict is SaleVerdict.Block) RiskLevel.BLOQUE else evaluation.risque
    val couleur = risque.couleur()
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, couleur),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(painterResource(risque.icone()), contentDescription = null, tint = couleur, modifier = Modifier.size(24.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(risque.libelle()), color = couleur, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                if (raison != null) Text(stringResource(raison.message()), color = MissaInk, fontSize = 14.sp)
                val limite = client.limiteCredit
                Text(
                    stringResource(R.string.cli_banniere_encours, clientMoney(entree.encours, devise)) +
                        if (limite != null) " · " + stringResource(R.string.cli_banniere_limite, clientMoney(limite, devise)) else "",
                    color = MissaMuted, fontSize = 13.sp,
                )
            }
        }
    }
}
