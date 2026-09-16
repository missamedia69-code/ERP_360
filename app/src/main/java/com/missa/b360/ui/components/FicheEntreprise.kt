package com.missa.b360.ui.components

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Store
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.missa.b360.R
import com.missa.b360.core.data.entity.EnterpriseEntity
import com.missa.b360.core.domain.usecase.GetEnterpriseUseCase
import com.missa.b360.core.domain.usecase.UserAdminUseCases
import com.missa.b360.ui.theme.BrandBlue
import com.missa.b360.ui.theme.Green90
import com.missa.b360.ui.theme.MissaBorder
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted
import com.missa.b360.ui.theme.TendrePositive
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Fiche détaillée de l'entreprise : toutes les informations enregistrées à
 * l'onboarding (identité, contacts, identifiants légaux) plus celles du
 * Propriétaire, avec copie champ par champ et partage de la fiche complète.
 * Lecture seule : aucune donnée n'est inventée ni modifiée ici.
 */
data class FicheEntrepriseState(
    val entreprise: EnterpriseEntity? = null,
    val proprietaireNom: String? = null,
    val proprietaireEmail: String? = null,
)

@HiltViewModel
class FicheEntrepriseViewModel @Inject constructor(
    getEnterprise: GetEnterpriseUseCase,
    users: UserAdminUseCases,
) : ViewModel() {

    /** Entreprise + utilisateur portant le rôle SYSTEM « Propriétaire » (à défaut, le premier). */
    val etat: StateFlow<FicheEntrepriseState> = combine(
        getEnterprise.observer(),
        users.observerUtilisateurs(),
        users.observerRoles(),
    ) { entreprise, utilisateurs, roles ->
        val roleIdProprietaire = roles.firstOrNull { it.nom.equals("Propriétaire", ignoreCase = true) }?.id
        val proprietaire = utilisateurs.firstOrNull { it.roleId != null && it.roleId == roleIdProprietaire }
            ?: utilisateurs.firstOrNull()
        FicheEntrepriseState(
            entreprise = entreprise,
            proprietaireNom = proprietaire?.nom,
            proprietaireEmail = proprietaire?.emailSecours,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        FicheEntrepriseState(),
    )
}

/** Lignes (libellé, valeur) non vides de la fiche, pour l'affichage et le partage. */
private fun lignesEntreprise(e: EnterpriseEntity, res: (Int) -> String): List<Pair<String, String>> =
    listOfNotNull(
        e.secteur?.takeIf { it.isNotBlank() }?.let { res(R.string.obn_secteur) to it },
        e.adresse?.takeIf { it.isNotBlank() }?.let { res(R.string.obn_adresse) to it },
        e.telephone?.takeIf { it.isNotBlank() }?.let { res(R.string.obn_telephone) to it },
        e.email?.takeIf { it.isNotBlank() }?.let { res(R.string.ob_email) to it },
        e.pays?.takeIf { it.isNotBlank() }?.let { res(R.string.obn_recap_pays) to it },
        res(R.string.obn_recap_devise) to e.devise,
        e.numeroFiscal?.takeIf { it.isNotBlank() }?.let { res(R.string.fisc_id_fiscal) to it },
        e.registreCommerce?.takeIf { it.isNotBlank() }?.let { res(R.string.fisc_id_registre) to it },
        e.profilActivite?.takeIf { it.isNotBlank() }?.let { res(R.string.obn_recap_profil) to it },
        e.palierTaille?.takeIf { it.isNotBlank() }?.let { res(R.string.obn_recap_taille) to it },
    )

@Composable
fun FicheEntrepriseDialog(
    etat: FicheEntrepriseState,
    onDismiss: () -> Unit,
    onModifier: () -> Unit,
) {
    val entreprise = etat.entreprise
    val nom = entreprise?.nom.orEmpty().ifBlank { stringResource(R.string.home_company_placeholder) }
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    val copier: (String) -> Unit = { texte ->
        clipboard.setText(AnnotatedString(texte))
        Toast.makeText(context, context.getString(R.string.fiche_copie), Toast.LENGTH_SHORT).show()
    }

    val res: (Int) -> String = { context.getString(it) }
    val lignes = entreprise?.let { lignesEntreprise(it, res) }.orEmpty()
    val lignesProprietaire = listOfNotNull(
        etat.proprietaireNom?.takeIf { it.isNotBlank() }?.let { res(R.string.obn_recap_proprietaire) to it },
        etat.proprietaireEmail?.takeIf { it.isNotBlank() }?.let { res(R.string.ob_email) to it },
    )

    val partager: () -> Unit = {
        val texte = buildString {
            appendLine(nom)
            entreprise?.secteur?.takeIf { it.isNotBlank() }?.let { appendLine(it) }
            appendLine()
            lignes.forEach { (libelle, valeur) -> appendLine("$libelle : $valeur") }
            if (lignesProprietaire.isNotEmpty()) {
                appendLine()
                lignesProprietaire.forEach { (libelle, valeur) -> appendLine("$libelle : $valeur") }
            }
        }.trimEnd()
        val intent = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_SUBJECT, nom)
            .putExtra(Intent.EXTRA_TEXT, texte)
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.fiche_partager)))
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            shadowElevation = 8.dp,
            tonalElevation = 0.dp,
            border = BorderStroke(1.dp, MissaBorder.copy(alpha = 0.35f)),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 560.dp),
            ) {
                // En-tête : logo, nom, secteur, fermeture.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (entreprise?.logoUri != null) {
                        CompanyLogo(
                            logoUri = entreprise.logoUri,
                            contentDescription = null,
                            fallbackIcon = Icons.Outlined.Store,
                            modifier = Modifier.size(48.dp),
                            size = 48.dp,
                            shape = RoundedCornerShape(14.dp),
                            fallbackTint = TendrePositive,
                            fallbackBackground = Green90,
                        )
                    } else {
                        Image(
                            painter = painterResource(R.drawable.logo_missa),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp)),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = nom,
                            color = MissaInk,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 2,
                        )
                        Text(
                            text = stringResource(R.string.fiche_entreprise_titre),
                            color = MissaMuted,
                            fontSize = 12.sp,
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = null,
                            tint = MissaMuted,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MissaBorder.copy(alpha = 0.5f)),
                )
                // Corps défilable : champs entreprise puis propriétaire.
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                        .padding(top = 8.dp),
                ) {
                    lignes.forEach { (libelle, valeur) ->
                        LigneFiche(libelle = libelle, valeur = valeur, onCopier = copier)
                    }
                    if (lignesProprietaire.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.obn_recap_proprietaire),
                            color = BrandBlue,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                        )
                        lignesProprietaire.forEach { (libelle, valeur) ->
                            LigneFiche(libelle = libelle, valeur = valeur, onCopier = copier)
                        }
                    }
                }
                // Pied : partager la fiche complète / modifier (destination existante).
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    OutlinedButton(
                        onClick = onModifier,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(text = stringResource(R.string.clients_flow_edit), fontSize = 13.sp)
                    }
                    Spacer(Modifier.width(10.dp))
                    Button(
                        onClick = partager,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(text = stringResource(R.string.fiche_partager), fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

/** Ligne libellé gris + valeur encre, avec bouton de copie du champ. */
@Composable
private fun LigneFiche(
    libelle: String,
    valeur: String,
    onCopier: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = libelle, color = MissaMuted, fontSize = 11.sp)
            Text(text = valeur, color = MissaInk, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
        IconButton(
            onClick = { onCopier(valeur) },
            modifier = Modifier.size(40.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.ContentCopy,
                contentDescription = stringResource(R.string.fiche_copier),
                tint = MissaMuted,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
