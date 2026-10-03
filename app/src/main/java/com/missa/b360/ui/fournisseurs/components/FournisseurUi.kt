package com.missa.b360.ui.fournisseurs.components

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.missa.b360.R
import com.missa.b360.core.domain.model.FournisseurLigne
import com.missa.b360.core.domain.model.SupplierReadinessLevel
import com.missa.b360.core.domain.model.SupplierReadinessReason
import com.missa.b360.ui.icons.Iv
import com.missa.b360.ui.stock.fmtValeur
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted
import com.missa.b360.ui.theme.OnbConfigCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Couleurs d'état du module Fournisseurs : réservées à l'aptitude et au retard, jamais décoratives. */
internal object FournisseurCouleurs {
    val Nuit = Color(0xFF101C43)
    val Trait = Color(0xFFCBD5E8)
    val Pret = Color(0xFF15803D)
    val Attention = Color(0xFFD97706)
    val Bloque = Color(0xFFB91C1C)
}

internal fun couleurAptitude(niveau: SupplierReadinessLevel): Color = when (niveau) {
    SupplierReadinessLevel.PRET -> FournisseurCouleurs.Pret
    SupplierReadinessLevel.A_REGULARISER -> FournisseurCouleurs.Attention
    SupplierReadinessLevel.BLOQUE -> FournisseurCouleurs.Bloque
}

@StringRes
internal fun SupplierReadinessLevel.libelleRes(): Int = when (this) {
    SupplierReadinessLevel.PRET -> R.string.four_apt_pret
    SupplierReadinessLevel.A_REGULARISER -> R.string.four_apt_a_regulariser
    SupplierReadinessLevel.BLOQUE -> R.string.four_apt_bloque
}

@StringRes
internal fun SupplierReadinessReason.libelleRes(): Int = when (this) {
    SupplierReadinessReason.STATUT_BLOQUE -> R.string.four_motif_statut_bloque
    SupplierReadinessReason.STATUT_ARCHIVE -> R.string.four_motif_statut_archive
    SupplierReadinessReason.STATUT_NON_ACTIF -> R.string.four_motif_statut_non_actif
    SupplierReadinessReason.PAIEMENT_BLOQUE -> R.string.four_motif_paiement_bloque
    SupplierReadinessReason.DOSSIER_INCOMPLET -> R.string.four_motif_dossier_incomplet
    SupplierReadinessReason.DOCUMENT_EXPIRE -> R.string.four_motif_document_expire
    SupplierReadinessReason.DOCUMENT_REJETE -> R.string.four_motif_document_rejete
    SupplierReadinessReason.COMPTE_NON_VERIFIE -> R.string.four_motif_compte_non_verifie
    SupplierReadinessReason.COMPTE_RECENT_NON_VERIFIE -> R.string.four_motif_compte_recent
}

internal fun formatDate(millis: Long): String = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(millis))

/** Pastille d'aptitude : Prêt / À régulariser / Bloqué. */
@Composable
internal fun BadgeAptitude(niveau: SupplierReadinessLevel) {
    val couleur = couleurAptitude(niveau)
    Box(Modifier.background(couleur.copy(alpha = 0.14f), RoundedCornerShape(8.dp)).padding(horizontal = 7.dp, vertical = 3.dp)) {
        Text(stringResource(niveau.libelleRes()), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = couleur)
    }
}

/** Puce de sélection : navy plein quand elle est sélectionnée, blanche à bord fin sinon. */
@Composable
internal fun PuceFournisseur(libelle: String, selectionnee: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = if (selectionnee) FournisseurCouleurs.Nuit else Color.White,
        border = if (selectionnee) null else BorderStroke(1.dp, FournisseurCouleurs.Trait),
        modifier = Modifier.heightIn(min = 34.dp),
    ) {
        Box(Modifier.padding(horizontal = 12.dp, vertical = 7.dp), contentAlignment = Alignment.Center) {
            Text(
                libelle,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (selectionnee) Color.White else MissaInk,
                maxLines = 1,
            )
        }
    }
}

/** Ligne de fournisseur : nom, aptitude, fiabilité, dette et retard — montants lus dans les soldes. */
@Composable
internal fun LigneFournisseur(ligne: FournisseurLigne, devise: String, onClick: () -> Unit) {
    val f = ligne.fournisseur
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = OnbConfigCard,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(34.dp).background(Color.White, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(Iv.Handshake), null, tint = MissaInk, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Text(f.nom, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = MissaInk, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val fiabilite = ligne.note?.let { stringResource(R.string.four_liste_fiabilite, it) }
                    ?: stringResource(R.string.four_liste_fiabilite_nd)
                Text("${f.code} · ${f.pays} · $fiabilite", fontSize = 11.sp, color = MissaMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (ligne.dette > 0.0) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            stringResource(R.string.four_liste_du, fmtValeur(ligne.dette, devise)),
                            fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = MissaInk,
                        )
                        if (ligne.enRetard > 0.0) {
                            Text(
                                stringResource(R.string.four_liste_retard, fmtValeur(ligne.enRetard, devise)),
                                fontSize = 11.5.sp, color = FournisseurCouleurs.Bloque,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.width(6.dp))
            BadgeAptitude(ligne.aptitude.niveau)
        }
    }
}

/** Bouton d'action du module : navy plein, ou contour navy pour les actions secondaires. */
@Composable
internal fun BoutonFournisseur(
    texte: String,
    modifier: Modifier = Modifier,
    plein: Boolean = true,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (plein) FournisseurCouleurs.Nuit else Color.White,
        border = if (plein) null else BorderStroke(1.dp, FournisseurCouleurs.Trait),
        modifier = modifier.heightIn(min = 44.dp),
    ) {
        Box(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
            Text(
                texte,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (plein) Color.White else MissaInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Carte grise d'un bloc d'information : titre, contenu. */
@Composable
internal fun CarteFournisseur(titre: String, modifier: Modifier = Modifier, contenu: @Composable () -> Unit) {
    Surface(shape = RoundedCornerShape(12.dp), color = OnbConfigCard, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(titre, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MissaInk)
            contenu()
        }
    }
}

/** Ligne « libellé … valeur » d'une carte. */
@Composable
internal fun LigneValeur(libelle: String, valeur: String, couleur: Color = MissaInk) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(libelle, fontSize = 11.5.sp, color = MissaMuted, modifier = Modifier.weight(1f))
        Text(valeur, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = couleur)
    }
}

/** Message d'état d'un écran (code stable du ViewModel) : vert pour un succès, rouge pour une erreur. */
@Composable
internal fun MessageEtat(code: String?, modifier: Modifier = Modifier) {
    if (code == null) return
    Text(
        libelleMessageFournisseur(code),
        modifier = modifier,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = if (code.startsWith("err_")) FournisseurCouleurs.Bloque else FournisseurCouleurs.Pret,
    )
}
