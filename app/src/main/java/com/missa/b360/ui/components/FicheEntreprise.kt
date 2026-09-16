package com.missa.b360.ui.components

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Store
import androidx.core.content.FileProvider
import com.missa.b360.core.util.FicheEntreprisePdf
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.missa.b360.R
import com.missa.b360.core.data.entity.EnterpriseEntity
import com.missa.b360.core.data.entity.SiteEntity
import com.missa.b360.core.domain.usecase.GetEnterpriseUseCase
import com.missa.b360.core.domain.usecase.SiteUseCases
import com.missa.b360.core.domain.usecase.UserAdminUseCases
import com.missa.b360.ui.theme.BrandBlue
import com.missa.b360.ui.theme.Green90
import androidx.compose.ui.graphics.Brush
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
 * Carte d'identité détaillée de l'entreprise, présentée comme un petit
 * document : sections titrées (trait bleu) puis chaque élément en paragraphe
 * à puce bleue « Libellé : valeur ». Toutes les informations enregistrées
 * (identité, coordonnées, légal, activité) + celles du Propriétaire.
 * Copie champ par champ, partage de la fiche complète. Lecture seule.
 */
data class FicheEntrepriseState(
    val entreprise: EnterpriseEntity? = null,
    val proprietaireNom: String? = null,
    val proprietaireEmail: String? = null,
    val proprietaireRole: String? = null,
    val sites: List<SiteEntity> = emptyList(),
)

@HiltViewModel
class FicheEntrepriseViewModel @Inject constructor(
    getEnterprise: GetEnterpriseUseCase,
    users: UserAdminUseCases,
    sites: SiteUseCases,
) : ViewModel() {

    /** Entreprise + sites + utilisateur portant le rôle SYSTEM « Propriétaire » (à défaut, le premier). */
    val etat: StateFlow<FicheEntrepriseState> = combine(
        getEnterprise.observer(),
        users.observerUtilisateurs(),
        users.observerRoles(),
        sites.observerSites(),
    ) { entreprise, utilisateurs, roles, sitesEnregistres ->
        val roleIdProprietaire = roles.firstOrNull { it.nom.equals("Propriétaire", ignoreCase = true) }?.id
        val proprietaire = utilisateurs.firstOrNull { it.roleId != null && it.roleId == roleIdProprietaire }
            ?: utilisateurs.firstOrNull()
        val proprietaireRole = proprietaire?.roleId?.let { rid -> roles.firstOrNull { it.id == rid }?.nom }
        FicheEntrepriseState(
            entreprise = entreprise,
            proprietaireNom = proprietaire?.nom,
            proprietaireEmail = proprietaire?.emailSecours,
            proprietaireRole = proprietaireRole,
            sites = sitesEnregistres,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        FicheEntrepriseState(),
    )
}

/** Libellé de ligne : ressource traduite ou texte direct (ex. nom d'un site). */
private data class LigneFicheData(
    val libelleRes: Int? = null,
    val valeur: String?,
    val libelleTexte: String? = null,
)

private data class SectionFicheData(
    val titreRes: Int,
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
) {
    val entreprise = etat.entreprise
    val nom = entreprise?.nom.orEmpty().ifBlank { stringResource(R.string.home_company_placeholder) }
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val nonRenseigne = stringResource(R.string.fiche_non_renseigne)
    val dateTexte = "${context.getString(R.string.fiche_edite_le)} " +
        android.text.format.DateFormat.getDateFormat(context).format(java.util.Date())

    val langueAffichee = entreprise?.langue?.let { code ->
        langueLibelleRes(code)?.let { context.getString(it) } ?: code
    }

    val sections = listOf(
        SectionFicheData(
            R.string.fiche_section_identite,
            listOf(
                LigneFicheData(R.string.obn_secteur, entreprise?.secteur),
                LigneFicheData(R.string.fiche_langue, langueAffichee),
                LigneFicheData(R.string.obn_recap_devise, entreprise?.devise),
                LigneFicheData(R.string.obn_recap_pays, entreprise?.pays),
            ),
        ),
        SectionFicheData(
            R.string.fiche_section_coordonnees,
            listOf(
                LigneFicheData(R.string.obn_adresse, entreprise?.adresse),
                LigneFicheData(R.string.obn_telephone, entreprise?.telephone),
                LigneFicheData(R.string.ob_email, entreprise?.email),
            ),
        ),
        SectionFicheData(
            R.string.fiche_section_legal,
            listOf(
                LigneFicheData(R.string.fisc_id_fiscal, entreprise?.numeroFiscal),
                LigneFicheData(R.string.fisc_id_registre, entreprise?.registreCommerce),
            ),
        ),
        SectionFicheData(
            R.string.fiche_section_activite,
            listOf(
                LigneFicheData(R.string.obn_recap_profil, entreprise?.profilActivite),
                LigneFicheData(R.string.obn_recap_taille, entreprise?.palierTaille),
            ),
        ),
        SectionFicheData(
            R.string.obn_recap_proprietaire,
            listOf(
                LigneFicheData(R.string.clients_nom, etat.proprietaireNom),
                LigneFicheData(R.string.clients_flow_role, etat.proprietaireRole),
                LigneFicheData(R.string.ob_email, etat.proprietaireEmail),
            ),
        ),
    ) + if (etat.sites.isEmpty()) {
        emptyList()
    } else {
        // Section Sites : nom du site en libellé, type · adresse · principal en valeur.
        listOf(
            SectionFicheData(
                R.string.fiche_section_sites,
                etat.sites.map { site ->
                    LigneFicheData(
                        libelleTexte = site.nom,
                        valeur = listOfNotNull(
                            site.type.takeIf { t -> t.isNotBlank() },
                            site.adresse?.takeIf { a -> a.isNotBlank() },
                            if (site.principal) context.getString(R.string.clients_flow_primary) else null,
                        ).joinToString(" · ").takeIf { v -> v.isNotBlank() },
                    )
                },
            ),
        )
    }

    val res: (Int) -> String = { context.getString(it) }
    val libelle: (LigneFicheData) -> String = { ligne ->
        ligne.libelleRes?.let { context.getString(it) } ?: ligne.libelleTexte.orEmpty()
    }
    val texteFiche = buildString {
        appendLine(nom)
        appendLine()
        sections.forEach { section ->
            appendLine(res(section.titreRes))
            section.lignes.forEach { ligne ->
                val valeur = ligne.valeur?.takeIf { v -> v.isNotBlank() }
                appendLine("  • ${libelle(ligne)} : ${valeur ?: res(R.string.fiche_non_renseigne)}")
            }
            appendLine()
        }
    }.trimEnd()

    val partager: () -> Unit = {
        val intent = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_SUBJECT, nom)
            .putExtra(Intent.EXTRA_TEXT, texteFiche)
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.fiche_partager)))
    }

    // Copie la fiche complète d'un seul coup dans le presse-papiers.
    val copierTout: () -> Unit = {
        clipboard.setText(AnnotatedString(texteFiche))
        Toast.makeText(context, context.getString(R.string.fiche_copie), Toast.LENGTH_SHORT).show()
    }

    // Partage PDF : même contenu que le document affiché, généré hors-ligne
    // puis exposé via FileProvider (application/pdf).
    val partagerPdf: () -> Unit = {
        val sectionsPdf = sections.map { section ->
            FicheEntreprisePdf.Section(
                titre = res(section.titreRes),
                lignes = section.lignes.map { ligne ->
                    FicheEntreprisePdf.Ligne(
                        libelle = libelle(ligne),
                        valeur = ligne.valeur?.takeIf { v -> v.isNotBlank() } ?: nonRenseigne,
                    )
                },
            )
        }
        val nbInfos = sectionsPdf.sumOf { section ->
            section.lignes.count { ligne -> ligne.valeur != nonRenseigne }
        }
        val fichier = FicheEntreprisePdf.generer(
            context = context,
            nomEntreprise = nom,
            titreDoc = res(R.string.fiche_entreprise_titre),
            dateTexte = dateTexte,
            sections = sectionsPdf,
            // Aperçu d'impression avec le logo choisi par l'utilisateur.
            logo = entreprise?.logoUri?.let { chargerLogoBitmap(context, it) },
            piedNote = String.format(res(R.string.fiche_nb_infos), nbInfos),
        )
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", fichier)
        val intent = Intent(Intent.ACTION_SEND)
            .setType("application/pdf")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(Intent.createChooser(intent, res(R.string.fiche_partager_pdf)))
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
                        .padding(start = 14.dp, end = 6.dp, top = 10.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CompanyLogo(
                        logoUri = entreprise?.logoUri,
                        contentDescription = null,
                        fallbackIcon = Icons.Outlined.Store,
                        modifier = Modifier.size(48.dp),
                        size = 48.dp,
                        shape = RoundedCornerShape(14.dp),
                        fallbackTint = TendrePositive,
                        fallbackBackground = Green90,
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.fiche_entreprise_titre),
                            color = MissaMuted,
                            fontSize = 10.sp,
                        )
                        Text(
                            text = nom,
                            color = MissaInk,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 2,
                        )
                        Text(
                            text = dateTexte,
                            color = MissaMuted,
                            fontSize = 10.sp,
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(40.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = null,
                            tint = MissaMuted,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
                // Corps défilable : petit document à puces bleues.
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
                            libelle = libelle,
                        )
                    }
                }
                // Pied : copier tout · partager · partager en PDF.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    OutlinedButton(
                        onClick = copierTout,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = stringResource(R.string.fiche_copier),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = partager,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = stringResource(R.string.fiche_partager),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = partagerPdf,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PictureAsPdf,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = stringResource(R.string.fiche_partager_pdf),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Section du document : titre souligné d'un trait bleu, puis chaque élément en
 * paragraphe introduit par une puce bleue : « Libellé : valeur ». Un champ vide
 * affiche « Non renseigné » ; la copie n'est proposée que si la valeur existe.
 */
@Composable
private fun SectionFiche(
    section: SectionFicheData,
    nonRenseigne: String,
    libelle: (LigneFicheData) -> String,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 5.dp)) {
        Text(
            text = stringResource(section.titreRes),
            color = BrandBlue,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(2.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(listOf(BrandBlue.copy(alpha = 0.55f), BrandBlue.copy(alpha = 0.08f))),
                    RoundedCornerShape(0.5.dp),
                ),
        )
        Spacer(Modifier.height(2.dp))
        section.lignes.forEach { ligne ->
            LigneFiche(
                libelle = libelle(ligne),
                valeur = ligne.valeur,
                nonRenseigne = nonRenseigne,
            )
        }
        Spacer(Modifier.height(6.dp))
    }
}

/** Paragraphe à puce bleue : « Libellé : valeur ». */
@Composable
private fun LigneFiche(
    libelle: String,
    valeur: String?,
    nonRenseigne: String,
) {
    val vide = valeur.isNullOrBlank()
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .size(5.dp)
                .background(BrandBlue, CircleShape),
        )
        Spacer(Modifier.width(7.dp))
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = MissaInk)) {
                    append(libelle)
                    append(" : ")
                }
                if (vide) {
                    withStyle(SpanStyle(color = MissaMuted.copy(alpha = 0.7f))) { append(nonRenseigne) }
                } else {
                    withStyle(SpanStyle(color = MissaInk)) { append(valeur) }
                }
            },
            fontSize = 12.sp,
            lineHeight = 14.sp,
            modifier = Modifier.weight(1f),
        )
    }
}
