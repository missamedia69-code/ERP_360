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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.missa.b360.ui.clients.components.BoutonClientPlein as Button
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
    val couleur = if (risque == RiskLevel.NORMAL) ClientCouleurs.Succes else risque.couleur()
    ClientPastille(
        texte = stringResource(risque.libelle()),
        fond = couleur.copy(alpha = 0.12f),
        couleurTexte = couleur,
        modifier = modifier,
        icone = risque.icone(),
    )
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
    val libelle = stringResource(statut.libelle())
    when (statut) {
        ClientStatus.ACTIF -> ClientPastille(
            libelle, ClientCouleurs.SuccesPale, ClientCouleurs.Succes, modifier,
            icone = Iv.CheckCircle, bord = Color(0xFFCDE9DA),
        )
        ClientStatus.SOUS_SURVEILLANCE -> ClientPastille(libelle, ClientCouleurs.AlertePale, ClientCouleurs.Alerte, modifier, icone = Iv.Warning)
        ClientStatus.BLOQUE_CREDIT -> ClientPastille(libelle, RisqueCouleurs.Eleve.copy(alpha = 0.12f), RisqueCouleurs.Eleve, modifier, icone = Iv.Lock)
        ClientStatus.BLOQUE_ADMINISTRATIF -> ClientPastille(libelle, RisqueCouleurs.Bloque.copy(alpha = 0.12f), RisqueCouleurs.Bloque, modifier, icone = Iv.Lock)
        ClientStatus.BROUILLON, ClientStatus.A_COMPLETER ->
            ClientPastille(libelle, ClientCouleurs.Neutre, ClientCouleurs.NeutreTexte, modifier)
        ClientStatus.INACTIF, ClientStatus.DESACTIVE, ClientStatus.ARCHIVE ->
            ClientPastille(libelle, ClientCouleurs.Neutre, MissaMuted, modifier)
    }
}

/** Jauge d'utilisation de la limite de crédit ; sans limite, aucune barre n'est dessinée. */
@Composable
internal fun CreditGauge(utilisationPct: Double?, risque: RiskLevel, modifier: Modifier = Modifier, surFonce: Boolean = false) {
    val ratio = utilisationPct?.takeIf { !it.isNaN() }?.let { (it / 100.0).coerceIn(0.0, 1.0) }
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.clients_flow_credit_usage),
                color = if (surFonce) Color(0xFFC3CBE0) else MissaMuted, fontSize = 12.sp, modifier = Modifier.weight(1f),
            )
            Text(
                text = when {
                    utilisationPct == null -> stringResource(R.string.clients_flow_unlimited)
                    utilisationPct.isInfinite() -> stringResource(R.string.cli_limite_depassee)
                    else -> stringResource(R.string.cli_pourcentage, utilisationPct.toInt())
                },
                color = if (risque == RiskLevel.NORMAL) (if (surFonce) Color.White else MissaInk) else risque.couleur(), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold,
            )
        }
        if (ratio != null) {
            Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(if (surFonce) Color.White.copy(alpha = 0.18f) else MissaBorder)) {
                Box(Modifier.fillMaxWidth(ratio.toFloat().coerceAtLeast(0.02f)).height(10.dp).background(risque.couleur()))
            }
        }
    }
}

@Composable
internal fun ClientAvatar(nom: String, modifier: Modifier = Modifier, taille: Dp = 44.dp) {
    Box(
        modifier
            .size(taille)
            .clip(RoundedCornerShape(taille * 0.34f))
            .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(ClientCouleurs.Violet, ClientCouleurs.VioletProfond))),
        contentAlignment = Alignment.Center,
    ) {
        Text(nom.initiales(), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = (taille.value * 0.36f).sp)
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
    val trait = ClientCouleurs.Trait
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 40.dp)
            .drawBehind { drawLine(trait, androidx.compose.ui.geometry.Offset(0f, size.height), androidx.compose.ui.geometry.Offset(size.width, size.height), 1.dp.toPx()) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(libelle, color = MissaMuted, fontSize = 13.sp, modifier = Modifier.weight(0.45f))
        Spacer(Modifier.width(8.dp))
        Text(valeur, color = MissaInk, fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(0.55f))
    }
}

internal fun clientDate(millis: Long): String =
    java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM).format(java.util.Date(millis))
