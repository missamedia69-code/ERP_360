package com.missa.b360.ui.fournisseurs.dossier

import com.missa.b360.ui.theme.OnbConfigCard
import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import com.missa.b360.ui.components.BoutonMissa as Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import com.missa.b360.ui.components.MissaMenuDeroulant
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import com.missa.b360.ui.components.BoutonContourMissa as OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.missa.b360.R
import com.missa.b360.core.data.entity.FournisseurCompteBancaireEntity
import com.missa.b360.core.data.entity.FournisseurDocType
import com.missa.b360.core.data.entity.FournisseurDocumentEntity
import com.missa.b360.core.data.entity.FournisseurEntity
import com.missa.b360.core.data.entity.FournisseurEvenementEntity
import com.missa.b360.core.data.entity.FournisseurEvenementType
import com.missa.b360.core.data.entity.FournisseurItemEntity
import com.missa.b360.core.data.entity.FournisseurStatus
import com.missa.b360.core.data.entity.TypeFournisseur
import com.missa.b360.core.data.entity.VerificationStatut
import com.missa.b360.core.domain.model.FournisseurRules
import com.missa.b360.core.util.PieceJointeAchat
import com.missa.b360.core.util.filterMoneyInput
import com.missa.b360.ui.components.MissaEmptyState
import com.missa.b360.ui.components.MissaTopAppBar
import com.missa.b360.ui.icons.Iv
import com.missa.b360.ui.navigation.AppModule
import com.missa.b360.ui.stock.fmtQuantite
import com.missa.b360.ui.stock.fmtValeur
import com.missa.b360.ui.theme.MissaBorder
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.missa.b360.ui.components.*

import com.missa.b360.ui.fournisseurs.*
import com.missa.b360.ui.fournisseurs.components.*
import com.missa.b360.ui.fournisseurs.form.*

@Composable
internal fun BoutonAction(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(containerColor = CouleurFournisseurs, contentColor = Color.White),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 7.dp, vertical = 6.dp),
    ) {
        Text(label, fontSize = 12.sp, color = Color.White)
    }
}

@Composable
internal fun LigneInfo(label: String, valeur: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, fontSize = 11.sp, color = MissaMuted, modifier = Modifier.weight(1f))
        Text(valeur, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MissaInk)
    }
}

@Composable
internal fun SectionRepliable(
    titre: String,
    icone: Int,
    action: (@Composable () -> Unit)? = null,
    contenu: @Composable () -> Unit,
) {
    var ouvert by remember { mutableStateOf(false) }
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = OnbConfigCard,
    ) {
        Column(Modifier.fillMaxWidth().padding(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clickable { ouvert = !ouvert },
            ) {
                Icon(painterResource(icone), null, tint = MissaInk, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(titre, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MissaInk, modifier = Modifier.weight(1f))
                action?.invoke()
                Icon(
                    painterResource(if (ouvert) Iv.ExpandLess else Iv.ExpandMore),
                    null,
                    tint = MissaInk,
                    modifier = Modifier.size(18.dp),
                )
            }
            if (ouvert) {
                Spacer(Modifier.height(6.dp))
                contenu()
            }
        }
    }
}

internal fun masquerReferenceConfidentielle(valeur: String?): String? = valeur?.let { reference ->
    val compacte = reference.filterNot { it.isWhitespace() }
    when {
        compacte.isEmpty() -> null
        compacte.length <= 4 -> "••••"
        else -> "•••• ${compacte.takeLast(4)}"
    }
}

@Composable
internal fun LigneCompte(
    compte: FournisseurCompteBancaireEntity,
    onVerifier: () -> Unit,
    onRejeter: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(Iv.Bank), null, tint = MissaInk, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    buildString {
                        append(compte.titulaire)
                        if (compte.principal) append(" · ").append("★")
                    },
                    fontSize = 12.sp,
                    color = MissaInk,
                )
                Text(
                    listOfNotNull(
                        compte.banque ?: compte.operateurMobile,
                        masquerReferenceConfidentielle(compte.numeroCompte ?: compte.iban ?: compte.numeroMobile),
                    ).joinToString(" · "),
                    fontSize = 10.sp,
                    color = MissaMuted,
                )
            }
            BadgeVerification(compte.verification)
        }
        if (compte.verification == VerificationStatut.A_VERIFIER) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                TextButton(onClick = onVerifier) {
                    Text(stringResource(R.string.four_verifier), fontSize = 11.sp, color = Color(0xFF15803D))
                }
                TextButton(onClick = onRejeter) {
                    Text(stringResource(R.string.four_rejeter), fontSize = 11.sp, color = Color(0xFFB91C1C))
                }
            }
        }
    }
}

@Composable
internal fun BadgeVerification(statut: VerificationStatut) {
    val couleur = when (statut) {
        VerificationStatut.A_VERIFIER -> Color(0xFFD97706)
        VerificationStatut.VERIFIE -> Color(0xFF15803D)
        VerificationStatut.REJETE -> Color(0xFFB91C1C)
    }
    val texte = when (statut) {
        VerificationStatut.A_VERIFIER -> stringResource(R.string.four_a_verifier)
        VerificationStatut.VERIFIE -> stringResource(R.string.four_verifie)
        VerificationStatut.REJETE -> stringResource(R.string.four_rejete)
    }
    Box(Modifier.background(couleur.copy(alpha = 0.14f), RoundedCornerShape(8.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
        Text(texte, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = couleur)
    }
}

@Composable
internal fun LigneDocument(
    document: FournisseurDocumentEntity,
    onSupprimer: () -> Unit,
) {
    val now = remember { System.currentTimeMillis() }
    val alerte = FournisseurRules.alerteDocument(document.dateExpiration, now)
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        var vignette by remember(document.cheminFichier) { mutableStateOf<Bitmap?>(null) }
        LaunchedEffect(document.cheminFichier) {
            vignette = document.cheminFichier?.let { PieceJointeAchat.charger(it) }
        }
        if (vignette != null) {
            androidx.compose.foundation.Image(
                bitmap = vignette!!.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.size(27.dp).background(MissaBorder, RoundedCornerShape(8.dp)),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            )
        } else {
            Icon(
                painterResource(if (document.cheminFichier?.let(PieceJointeAchat::estPdf) == true) Iv.PictureAsPdf else Iv.Description),
                null,
                tint = MissaInk,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(6.dp))
        Column(Modifier.weight(1f)) {
            Text(
                buildString {
                    append(libelleDocument(document.typeDocument))
                    document.reference?.let { append(" · ").append(it) }
                },
                fontSize = 12.sp,
                color = MissaInk,
            )
            Text(
                buildString {
                    append(stringResource(R.string.four_expire_le))
                    append(" ")
                    append(fmtDate(document.dateExpiration))
                },
                fontSize = 10.sp,
                color = when (alerte) {
                    FournisseurRules.AlerteDocument.EXPIRE, FournisseurRules.AlerteDocument.FORTE -> Color(0xFFB91C1C)
                    FournisseurRules.AlerteDocument.ALERTE -> Color(0xFFB45309)
                    else -> MissaMuted
                },
            )
        }
        IconButton(onClick = onSupprimer, modifier = Modifier.size(27.dp)) {
            Icon(
                painterResource(Iv.DeleteOutline),
                null,
                tint = Color(0xFFB91C1C),
                modifier = Modifier.size(16.dp),
            )
        }
    }
}
