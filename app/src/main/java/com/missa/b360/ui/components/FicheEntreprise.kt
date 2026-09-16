package com.missa.b360.ui.components

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Store
import androidx.compose.material.icons.outlined.Work
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
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
import com.missa.b360.ui.theme.MissaCanvas
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted
import com.missa.b360.ui.theme.MissaSoftBlue
import com.missa.b360.ui.theme.TendrePositive
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Carte d'identité détaillée de l'entreprise : toutes les informations
 * enregistrées (identité, coordonnées, légal, activité) plus celles du
 * Propriétaire, structurées en sections comme les popups explicatives des
 * packs de l'onboarding (puce d'icône bleu pâle + titre + panneau).
 * Copie champ par champ et partage de la fiche complète. Lecture seule.
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

private data class LigneFicheData(val libelleRes: Int, val valeur: String?)

private data class SectionFicheData(
    val titreRes: Int,
    val icone: ImageVector,
    val lignes: List<LigneFicheData>,
)

/** Code langue enregistré (fr/en/es/ar/zh) → libellé localisé affiché. */
private fun langueLibelleRes(code: String?): Int? = when (code?.lowercase()) {
    "fr" -> R.string.langue_fr
    "en" -> R.string.langue_en
    "es" -> R.string.langue_es
    "ar" -> R.string.langue_ar
    "zh" -> R.string.langue_zh
    else -> null
}

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
    val nonRenseigne = stringResource(R.string.fiche_non_renseigne)

    val copier: (String) -> Unit = { texte ->
        clipboard.setText(AnnotatedString(texte))
        Toast.makeText(context, context.getString(R.string.fiche_copie), Toast.LENGTH_SHORT).show()
    }

    val langueAffichee = entreprise?.langue?.let { code ->
        langueLibelleRes(code)?.let { context.getString(it) } ?: code
    }

    val sections = listOf(
        SectionFicheData(
            R.string.fiche_section_identite,
            Icons.Outlined.Business,
            listOf(
                LigneFicheData(R.string.obn_secteur, entreprise?.secteur),
                LigneFicheData(R.string.fiche_langue, langueAffichee),
                LigneFicheData(R.string.obn_recap_devise, entreprise?.devise),
                LigneFicheData(R.string.obn_recap_pays, entreprise?.pays),
            ),
        ),
        SectionFicheData(
            R.string.fiche_section_coordonnees,
            Icons.Outlined.Place,
            listOf(
                LigneFicheData(R.string.obn_adresse, entreprise?.adresse),
                LigneFicheData(R.string.obn_telephone, entreprise?.telephone),
                LigneFicheData(R.string.ob_email, entreprise?.email),
            ),
        ),
        SectionFicheData(
            R.string.fiche_section_legal,
            Icons.Outlined.Gavel,
            listOf(
                LigneFicheData(R.string.fisc_id_fiscal, entreprise?.numeroFiscal),
                LigneFicheData(R.string.fisc_id_registre, entreprise?.registreCommerce),
            ),
        ),
        SectionFicheData(
            R.string.fiche_section_activite,
            Icons.Outlined.Work,
            listOf(
                LigneFicheData(R.string.obn_recap_profil, entreprise?.profilActivite),
                LigneFicheData(R.string.obn_recap_taille, entreprise?.palierTaille),
            ),
        ),
        SectionFicheData(
            R.string.obn_recap_proprietaire,
            Icons.Outlined.Person,
            listOf(
                LigneFicheData(R.string.clients_nom, etat.proprietaireNom),
                LigneFicheData(R.string.ob_email, etat.proprietaireEmail),
            ),
        ),
    )

    val partager: () -> Unit = {
        val res: (Int) -> String = { context.getString(it) }
        val texte = buildString {
            appendLine(nom)
            appendLine()
            sections.forEach { section ->
                appendLine(res(section.titreRes))
                section.lignes.forEach { ligne ->
                    val valeur = ligne.valeur?.takeIf { v -> v.isNotBlank() }
                    appendLine("  ${res(ligne.libelleRes)} : ${valeur ?: res(R.string.fiche_non_renseigne)}")
                }
                appendLine()
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
                    .heightIn(max = 600.dp),
            ) {
                // En-tête : logo de l'utilisateur (jamais celui de MISSA), nom, fermeture.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 8.dp, top = 14.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CompanyLogo(
                        logoUri = entreprise?.logoUri,
                        contentDescription = null,
                        fallbackIcon = Icons.Outlined.Store,
                        modifier = Modifier.size(64.dp),
                        size = 64.dp,
                        shape = RoundedCornerShape(18.dp),
                        fallbackTint = TendrePositive,
                        fallbackBackground = Green90,
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.fiche_entreprise_titre),
                            color = MissaMuted,
                            fontSize = 11.sp,
                        )
                        Text(
                            text = nom,
                            color = MissaInk,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 2,
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
                // Corps défilable : sections façon popups de packs.
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                ) {
                    sections.forEach { section ->
                        SectionFiche(
                            section = section,
                            nonRenseigne = nonRenseigne,
                            onCopier = copier,
                        )
                    }
                }
                // Pied : modifier (destination existante) / partager la fiche complète.
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

/**
 * Section de la carte : puce d'icône bleu pâle + titre gras (comme les popups
 * de packs), puis panneau gris clair listant les champs. Un champ vide est
 * affiché « Non renseigné » ; la copie n'est proposée que si la valeur existe.
 */
@Composable
private fun SectionFiche(
    section: SectionFicheData,
    nonRenseigne: String,
    onCopier: (String) -> Unit,
) {
    Row(
        modifier = Modifier.padding(top = 4.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(32.dp),
            shape = RoundedCornerShape(9.dp),
            color = MissaSoftBlue,
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize(),
            ) {
                Icon(
                    imageVector = section.icone,
                    contentDescription = null,
                    tint = BrandBlue,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(section.titreRes),
            color = MissaInk,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
        )
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MissaCanvas,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
        ) {
            section.lignes.forEach { ligne ->
                LigneFiche(
                    libelle = stringResource(ligne.libelleRes),
                    valeur = ligne.valeur,
                    nonRenseigne = nonRenseigne,
                    onCopier = onCopier,
                )
            }
        }
    }
    Spacer(Modifier.height(12.dp))
}

/** Ligne libellé gris + valeur encre (ou « Non renseigné »), bouton copie si valeur. */
@Composable
private fun LigneFiche(
    libelle: String,
    valeur: String?,
    nonRenseigne: String,
    onCopier: (String) -> Unit,
) {
    val vide = valeur.isNullOrBlank()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = libelle, color = MissaMuted, fontSize = 11.sp)
            Text(
                text = if (vide) nonRenseigne else valeur,
                color = if (vide) MissaMuted.copy(alpha = 0.7f) else MissaInk,
                fontSize = 14.sp,
                fontWeight = if (vide) FontWeight.Normal else FontWeight.Medium,
            )
        }
        if (!vide) {
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
}
