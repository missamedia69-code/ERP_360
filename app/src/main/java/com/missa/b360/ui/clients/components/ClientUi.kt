package com.missa.b360.ui.clients.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.missa.b360.ui.components.BoutonMissa as Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.missa.b360.R
import com.missa.b360.core.data.entity.ClientStatus
import com.missa.b360.core.domain.model.RiskLevel
import com.missa.b360.ui.components.MissaTopAppBar
import com.missa.b360.ui.icons.Iv
import com.missa.b360.ui.navigation.AppModule
import com.missa.b360.ui.theme.Green60
import com.missa.b360.ui.theme.MissaBorder
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Currency
import java.util.Locale

/** Couleurs de risque : la couleur porte le sens, l'icône et le libellé le répètent (jamais seuls). */
internal object RisqueCouleurs {
    val Normal = Green60
    val Attention = Color(0xFFD97706)
    val Eleve = Color(0xFFDC2626)
    val Bloque = Color(0xFF7F1D1D)
}

internal fun RiskLevel.couleur(): Color = when (this) {
    RiskLevel.NORMAL -> RisqueCouleurs.Normal
    RiskLevel.ATTENTION -> RisqueCouleurs.Attention
    RiskLevel.ELEVE -> RisqueCouleurs.Eleve
    RiskLevel.BLOQUE -> RisqueCouleurs.Bloque
}

internal fun RiskLevel.icone(): Int = when (this) {
    RiskLevel.NORMAL -> Iv.CheckCircle
    RiskLevel.ATTENTION -> Iv.Warning
    RiskLevel.ELEVE -> Iv.TrendingUp
    RiskLevel.BLOQUE -> Iv.Lock
}

internal fun RiskLevel.libelle(): Int = when (this) {
    RiskLevel.NORMAL -> R.string.cli_risque_normal
    RiskLevel.ATTENTION -> R.string.cli_risque_attention
    RiskLevel.ELEVE -> R.string.cli_risque_eleve
    RiskLevel.BLOQUE -> R.string.cli_risque_bloque
}

/** Montant dans la devise de l'entreprise ; sans devise connue, le nombre seul. */
internal fun clientMoney(montant: Double, devise: String): String {
    val valeur = if (montant.isFinite()) montant else 0.0
    val decimales = runCatching { Currency.getInstance(devise).defaultFractionDigits }.getOrDefault(2).coerceIn(0, 2)
    val motif = if (decimales == 0) "#,##0" else "#,##0.${"0".repeat(decimales)}"
    val nombre = DecimalFormat(motif, DecimalFormatSymbols(Locale.getDefault())).format(valeur)
    return if (devise.isBlank()) nombre else "$nombre $devise"
}

internal fun String.initiales(): String = trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    .take(2).joinToString("") { it.first().uppercase() }.ifBlank { "?" }

@Composable
internal fun RiskBadge(risque: RiskLevel, modifier: Modifier = Modifier) {
    val couleur = risque.couleur()
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = Color.White,
        border = BorderStroke(1.dp, couleur.copy(alpha = 0.45f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(painterResource(risque.icone()), contentDescription = null, tint = couleur, modifier = Modifier.size(14.dp))
            Text(stringResource(risque.libelle()), color = MissaInk, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

internal fun ClientStatus.libelle(): Int = when (this) {
    ClientStatus.BROUILLON -> R.string.clients_status_draft
    ClientStatus.A_COMPLETER -> R.string.clients_status_complete
    ClientStatus.ACTIF -> R.string.clients_actif
    ClientStatus.SOUS_SURVEILLANCE -> R.string.clients_status_watch
    ClientStatus.BLOQUE_CREDIT -> R.string.clients_status_credit_block
    ClientStatus.BLOQUE_ADMINISTRATIF -> R.string.clients_status_admin_block
    ClientStatus.INACTIF, ClientStatus.DESACTIVE -> R.string.clients_inactif
    ClientStatus.ARCHIVE -> R.string.clients_status_archived
}

@Composable
internal fun ClientStatusChip(statut: ClientStatus, modifier: Modifier = Modifier) {
    val libelle = statut.libelle()
    val couleur = when (statut) {
        ClientStatus.ACTIF -> RisqueCouleurs.Normal
        ClientStatus.SOUS_SURVEILLANCE -> RisqueCouleurs.Attention
        ClientStatus.BLOQUE_CREDIT -> RisqueCouleurs.Eleve
        ClientStatus.BLOQUE_ADMINISTRATIF -> RisqueCouleurs.Bloque
        ClientStatus.BROUILLON, ClientStatus.A_COMPLETER -> MissaInk
        ClientStatus.INACTIF, ClientStatus.DESACTIVE, ClientStatus.ARCHIVE -> MissaMuted
    }
    val neutre = statut == ClientStatus.BROUILLON || statut == ClientStatus.A_COMPLETER ||
        statut == ClientStatus.INACTIF || statut == ClientStatus.DESACTIVE || statut == ClientStatus.ARCHIVE
    val fondChip = if (neutre) ClientCouleurs.Pastille else couleur.copy(alpha = 0.12f)
    val bloque = statut == ClientStatus.BLOQUE_CREDIT || statut == ClientStatus.BLOQUE_ADMINISTRATIF
    Surface(modifier = modifier, shape = RoundedCornerShape(8.dp), color = fondChip) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (bloque) Icon(painterResource(Iv.Lock), null, tint = couleur, modifier = Modifier.size(14.dp))
            Text(stringResource(libelle), color = couleur, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** Jauge d'utilisation de la limite de crédit ; sans limite, aucune barre n'est dessinée. */
@Composable
internal fun CreditGauge(utilisationPct: Double?, risque: RiskLevel, modifier: Modifier = Modifier) {
    val ratio = utilisationPct?.takeIf { !it.isNaN() }?.let { (it / 100.0).coerceIn(0.0, 1.0) }
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.clients_flow_credit_usage),
                color = MissaMuted, fontSize = 13.sp, modifier = Modifier.weight(1f),
            )
            Text(
                text = when {
                    utilisationPct == null -> stringResource(R.string.clients_flow_unlimited)
                    utilisationPct.isInfinite() -> stringResource(R.string.cli_limite_depassee)
                    else -> stringResource(R.string.cli_pourcentage, utilisationPct.toInt())
                },
                color = if (risque == RiskLevel.NORMAL) MissaInk else risque.couleur(), fontSize = 15.sp, fontWeight = FontWeight.Bold,
            )
        }
        if (ratio != null) {
            Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(MissaBorder)) {
                Box(Modifier.fillMaxWidth(ratio.toFloat().coerceAtLeast(0.02f)).height(10.dp).background(risque.couleur()))
            }
        }
    }
}

@Composable
internal fun ClientAvatar(nom: String, modifier: Modifier = Modifier, taille: Dp = 44.dp) {
    Surface(modifier = modifier.size(taille), shape = CircleShape, color = ClientCouleurs.Nuit) {
        Box(contentAlignment = Alignment.Center) {
            Text(nom.initiales(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = (taille.value * 0.36f).sp)
        }
    }
}

@Composable
internal fun ClientTopBar(
    titre: String,
    onBack: () -> Unit,
    titreCentre: Boolean = true,
    actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {},
) {
    MissaTopAppBar(title = titre, onBack = onBack, couleurFond = AppModule.CLIENTS.couleurPale, actions = actions, titreCentre = titreCentre)
}

@Composable
internal fun EtatChargement(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

@Composable
internal fun EtatErreur(onReessayer: () -> Unit, modifier: Modifier = Modifier, message: String = stringResource(R.string.cli_erreur_chargement)) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(painterResource(Iv.Warning), null, tint = RisqueCouleurs.Eleve, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(12.dp))
        Text(message, color = MissaInk, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onReessayer, modifier = Modifier.height(48.dp)) { Text(stringResource(R.string.cli_reessayer)) }
    }
}

@Composable
internal fun LigneInfo(libelle: String, valeur: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
        Text(libelle, color = MissaMuted, fontSize = 14.sp, modifier = Modifier.weight(0.45f))
        Spacer(Modifier.width(8.dp))
        Text(valeur, color = MissaInk, fontSize = 14.sp, textAlign = TextAlign.End, modifier = Modifier.weight(0.55f))
    }
}

internal fun clientDate(millis: Long): String =
    java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM).format(java.util.Date(millis))
