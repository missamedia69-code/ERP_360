package com.missa.b360.ui.onboarding

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.missa.b360.R
import com.missa.b360.core.domain.model.ProfilActivite
import com.missa.b360.core.util.ContactCommercial
import com.missa.b360.core.util.DateUtils
import com.missa.b360.core.util.Iso4217
import com.missa.b360.ui.icons.Iv
import com.missa.b360.ui.theme.BrandBlue
import com.missa.b360.ui.theme.Green60
import com.missa.b360.ui.theme.Green90
import com.missa.b360.ui.theme.MissaBorder
import com.missa.b360.ui.theme.MissaCanvas
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted
import com.missa.b360.ui.theme.MissaSoftBlue
import com.missa.b360.ui.theme.MissaSurface
import com.missa.b360.ui.theme.Red20
import com.missa.b360.ui.theme.Red80

/**
 * Écran 7 — Tout est prêt (clôture RA-11 ; l'application démarrera sur le
 * verrou PIN).
 *
 * La page de confirmation rassemble le récapitulatif, l'état de l'essai ou
 * de la licence, puis les canaux de commande du code d'activation. Les données
 * et actions métier restent celles du ViewModel : seule la présentation est
 * réorganisée selon la charte Missa (canevas pâle, cartes blanches, cobalt et
 * vert de confirmation).
 */
@Composable
internal fun OnbTermineStep(viewModel: OnboardingViewModel) {
    LaunchedEffect(Unit) { viewModel.chargerEtatLicence() }

    val locale = LocalConfiguration.current.locales.takeIf { !it.isEmpty }?.get(0)
        ?: java.util.Locale.getDefault()
    val paysListe = remember(locale) { Iso4217.paysDisponibles(locale) }
    val typeTaxe = remember(paysListe, viewModel.codePays) {
        paysListe.firstOrNull { it.code == viewModel.codePays }?.typeTaxe
    }
    val profilLabel = viewModel.profil?.let { stringResource(it.labelRes) } ?: "—"
    val personnel = viewModel.profil == ProfilActivite.PERSONNEL
    val tailleLabel = viewModel.palier?.let { stringResource(it.labelRes) } ?: "—"
    val deviseLabel = if (viewModel.devise.isBlank()) {
        "—"
    } else {
        "${viewModel.devise} · ${Iso4217.nomDevise(viewModel.devise, locale)}"
    }
    val identifiants = listOf(viewModel.numeroFiscal, viewModel.registreCommerce)
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .joinToString(" · ")
    val proprietaire = listOf(
        viewModel.votreNom.trim().ifBlank { viewModel.nomEntreprise.trim() },
        viewModel.emailSecours.trim(),
    ).filter { it.isNotEmpty() }.joinToString(" · ")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MissaCanvas)
            .statusBarsPadding(),
    ) {
        OnbTermineHero()

        // Le contenu défile indépendamment : le bouton d'accès reste toujours
        // visible, y compris quand le récapitulatif contient plusieurs lignes.
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            viewModel.erreurRes?.let { erreur ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Red80)
                        .border(1.dp, Red20.copy(alpha = 0.16f), RoundedCornerShape(12.dp))
                        .semantics(mergeDescendants = true) {
                            liveRegion = LiveRegionMode.Polite
                        }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(Iv.Warning),
                        contentDescription = null,
                        tint = Red20,
                        modifier = Modifier.size(17.dp),
                    )
                    Spacer(Modifier.width(9.dp))
                    Text(
                        text = stringResource(erreur),
                        color = Red20,
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            OnbRecapCarte(
                personnel = personnel,
                entreprise = viewModel.nomEntreprise,
                pays = viewModel.pays,
                devise = deviseLabel,
                fiscalite = libelleTaxePays(typeTaxe, viewModel.tauxTaxe),
                identifiants = identifiants,
                profil = profilLabel,
                taille = tailleLabel,
                modules = viewModel.modulesActifs.size.toString(),
                proprietaire = proprietaire,
            )
            OnbEssaiCarte(viewModel)
            OnbActivationCarte(viewModel)
            Spacer(Modifier.height(8.dp))
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MissaSurface,
            shadowElevation = 5.dp,
        ) {
            Column {
                HorizontalDivider(color = MissaBorder.copy(alpha = 0.6f))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    OnbBoutonAcceder(
                        actif = !viewModel.enregistrementEnCours,
                        onClick = viewModel::suivant,
                    )
                }
            }
        }
    }
}

/** En-tête de réussite, dégradé de surface et coche dans un carré arrondi. */
@Composable
private fun OnbTermineHero() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(MissaSoftBlue, MissaCanvas),
                ),
            )
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        // Halo discret : il reprend le bleu de marque sans remettre les
        // confettis multicolores au premier plan.
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 26.dp, y = (-34).dp)
                .size(120.dp)
                .clip(CircleShape)
                .background(BrandBlue.copy(alpha = 0.045f)),
        )
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .shadow(5.dp, RoundedCornerShape(19.dp), spotColor = Green60.copy(alpha = 0.2f))
                    .clip(RoundedCornerShape(19.dp))
                    .background(Green90)
                    .border(1.dp, Green60.copy(alpha = 0.12f), RoundedCornerShape(19.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(Iv.Check),
                    contentDescription = null,
                    tint = Green60,
                    modifier = Modifier.size(28.dp),
                )
            }
            Spacer(Modifier.height(10.dp))
            OnbTermineTitre()
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.obn_terminer_sous),
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = MissaMuted,
                textAlign = TextAlign.Center,
                maxLines = 2,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
}

/** Titre « Tout est prêt ! » : l'accent vert confirme la réussite. */
@Composable
private fun OnbTermineTitre() {
    val base = stringResource(R.string.obn_terminer_titre)
    val surligne = stringResource(R.string.obn_terminer_pret)
    val titre = remember(base, surligne) {
        buildAnnotatedString {
            append(base)
            withStyle(SpanStyle(color = Green60)) {
                append(" $surligne")
            }
        }
    }
    Text(
        text = titre,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        color = MissaInk,
        textAlign = TextAlign.Center,
    )
}

/** Récapitulatif en trois groupes : entreprise, activité et accès. */
@Composable
private fun OnbRecapCarte(
    personnel: Boolean,
    entreprise: String,
    pays: String,
    devise: String,
    fiscalite: String,
    identifiants: String,
    profil: String,
    taille: String,
    modules: String,
    proprietaire: String,
) {
    val forme = RoundedCornerShape(18.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(forme)
            .background(MissaSurface)
            .border(1.dp, MissaBorder.copy(alpha = 0.65f), forme)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(MissaSoftBlue),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(Iv.Description),
                    contentDescription = null,
                    tint = BrandBlue,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.obn_recap),
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = MissaInk,
                maxLines = 2,
                modifier = Modifier.weight(1f),
            )
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Green90)
                    .padding(horizontal = 9.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(Iv.Check),
                    contentDescription = null,
                    tint = Green60,
                    modifier = Modifier.size(12.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.obn_recap_badge),
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Green60,
                )
            }
        }
        Spacer(Modifier.height(10.dp))

        OnbRecapLigne(R.string.obn_recap_entreprise, entreprise, Iv.Business)
        OnbRecapLigne(R.string.obn_recap_pays, pays, Iv.Person)
        OnbRecapLigne(R.string.obn_recap_devise, devise, Iv.Savings)
        if (!personnel) {
            OnbRecapLigne(R.string.obn_recap_fiscalite, fiscalite, Iv.Percent)
        }
        OnbRecapLigne(R.string.obn_recap_identifiants, identifiants, Iv.Badge)

        OnbRecapSeparateur()
        OnbRecapLigne(R.string.obn_recap_profil, profil, Iv.Workspaces)
        if (!personnel) {
            OnbRecapLigne(R.string.obn_recap_taille, taille, Iv.ExpandMore)
            OnbRecapLigne(R.string.obn_recap_modules, modules, Iv.Category)
        }

        OnbRecapSeparateur()
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MissaSoftBlue)
                .padding(10.dp),
        ) {
            OnbRecapLigne(R.string.obn_recap_proprietaire, proprietaire, Iv.Security)
        }
    }
}

@Composable
private fun OnbRecapSeparateur() {
    Spacer(Modifier.height(7.dp))
    HorizontalDivider(color = MissaBorder.copy(alpha = 0.65f))
    Spacer(Modifier.height(7.dp))
}

/** Une ligne du récapitulatif : libellé discret, valeur en premier plan. */
@Composable
private fun OnbRecapLigne(labelRes: Int, valeur: String, icone: Int) {
    if (valeur.isBlank()) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MissaSoftBlue),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(icone),
                contentDescription = null,
                tint = BrandBlue,
                modifier = Modifier.size(13.dp),
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(labelRes),
            fontSize = 11.5.sp,
            color = MissaMuted,
            modifier = Modifier.width(84.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = valeur,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = MissaInk,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

/** Essai ou licence active : échéance réelle relue depuis la base. */
@Composable
private fun OnbEssaiCarte(viewModel: OnboardingViewModel) {
    val echeance = viewModel.essaiExpireLe
    val packLabel = viewModel.profil?.let { stringResource(it.labelRes) }
    val actif = viewModel.licenceDejaActive
    val forme = RoundedCornerShape(18.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(forme)
            .background(MissaSurface)
            .border(1.dp, MissaBorder.copy(alpha = 0.65f), forme)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(if (actif) Green90 else MissaSoftBlue),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(if (actif) Iv.Check else Iv.Calendar),
                        contentDescription = null,
                        tint = if (actif) Green60 else BrandBlue,
                        modifier = Modifier.size(19.dp),
                    )
                }
                Spacer(Modifier.width(9.dp))
                Text(
                    text = stringResource(
                        if (actif) R.string.adm_licence_statut_actif else R.string.obn_essai_titre,
                    ),
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MissaInk,
                )
            }
            if (echeance != null) {
                Spacer(Modifier.height(5.dp))
                Text(
                    text = stringResource(
                        if (actif) R.string.obn_licence_jusqu_au else R.string.obn_essai_jusqu_au,
                        DateUtils.formatDate(echeance),
                    ),
                    fontSize = 11.5.sp,
                    color = MissaMuted,
                    lineHeight = 15.sp,
                )
            }
            Spacer(Modifier.height(3.dp))
            Text(
                text = if (actif) {
                    stringResource(R.string.ob_licence_avantage_sans_carte)
                } else {
                    stringResource(R.string.obn_essai_pack_dispo, packLabel ?: "")
                },
                fontSize = 10.5.sp,
                color = MissaMuted,
                lineHeight = 14.sp,
                maxLines = 2,
            )
        }
        Spacer(Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .size(if (actif) 62.dp else 72.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (actif) Green90 else MissaSoftBlue)
                .border(
                    1.dp,
                    (if (actif) Green60 else BrandBlue).copy(alpha = 0.12f),
                    RoundedCornerShape(16.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (actif) {
                Icon(
                    painter = painterResource(Iv.Check),
                    contentDescription = null,
                    tint = Green60,
                    modifier = Modifier.size(25.dp),
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "7",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandBlue,
                    )
                    Text(
                        text = stringResource(R.string.obn_term_jours),
                        fontSize = 9.sp,
                        letterSpacing = 1.2.sp,
                        color = MissaMuted,
                    )
                }
            }
        }
    }
}

/** Canaux de commande du code d'activation, masqués si la licence est active. */
@Composable
private fun OnbActivationCarte(viewModel: OnboardingViewModel) {
    val contexte = LocalContext.current
    val numero = stringResource(R.string.contact_commercial_whatsapp)
    val telegram = stringResource(R.string.contact_commercial_telegram)
    val adresse = stringResource(R.string.contact_commercial_email)
    val whatsappOk = !ContactCommercial.estTemoin(numero)
    val telegramOk = !ContactCommercial.estTemoin(telegram)
    val emailOk = !ContactCommercial.estTemoin(adresse)
    val coordonneesManquantes = !whatsappOk && !telegramOk && !emailOk
    val objet = stringResource(R.string.obn_code_objet)
    val nomPourMessage = viewModel.nomEntreprise.trim()
        .ifEmpty { stringResource(R.string.obn_recap_entreprise) }
    val message = stringResource(R.string.obn_code_message, nomPourMessage)
    val echec = stringResource(R.string.obn_code_indispo)
    val forme = RoundedCornerShape(18.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(forme)
            .background(MissaSurface)
            .border(1.dp, MissaBorder.copy(alpha = 0.65f), forme)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(MissaSoftBlue),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(Iv.Share),
                    contentDescription = null,
                    tint = BrandBlue,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.width(9.dp))
            Text(
                text = stringResource(R.string.obn_code_titre),
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = MissaInk,
            )
        }
        Spacer(Modifier.height(10.dp))

        when {
            viewModel.licenceDejaActive -> Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Green90)
                    .padding(horizontal = 11.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(Iv.Check),
                    contentDescription = null,
                    tint = Green60,
                    modifier = Modifier.size(17.dp),
                )
                Spacer(Modifier.width(9.dp))
                Text(
                    text = stringResource(R.string.ob_licence_avantage_sans_carte),
                    fontSize = 11.5.sp,
                    color = MissaInk,
                )
            }
            coordonneesManquantes -> Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Red80)
                    .padding(horizontal = 11.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(Iv.Warning),
                    contentDescription = null,
                    tint = Red20,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.obn_code_a_configurer),
                    fontSize = 11.sp,
                    color = Red20,
                )
            }
            else -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (whatsappOk) {
                    OnbActivationLigne(
                        icone = Iv.Chat,
                        couleur = Green60,
                        texteRes = R.string.obn_code_whatsapp,
                    ) {
                        val ouvert = ContactCommercial.ouvrirWhatsApp(contexte, numero, message)
                        if (!ouvert) Toast.makeText(contexte, echec, Toast.LENGTH_LONG).show()
                    }
                }
                if (telegramOk) {
                    OnbActivationLigne(
                        icone = Iv.Send,
                        couleur = BrandBlue,
                        texteRes = R.string.obn_code_telegram,
                    ) {
                        val ouvert = ContactCommercial.ouvrirTelegram(contexte, telegram, message)
                        if (!ouvert) Toast.makeText(contexte, echec, Toast.LENGTH_LONG).show()
                    }
                }
                if (emailOk) {
                    OnbActivationLigne(
                        icone = Iv.MailOutline,
                        couleur = BrandBlue,
                        texteRes = R.string.obn_code_email,
                    ) {
                        val ouvert = ContactCommercial.ouvrirEmail(contexte, adresse, objet, message)
                        if (!ouvert) Toast.makeText(contexte, echec, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}

/** Ligne d'activation : icône en tonalité, libellé et chevron. */
@Composable
private fun OnbActivationLigne(
    icone: Int,
    couleur: Color,
    texteRes: Int,
    onClick: () -> Unit,
) {
    val libelle = stringResource(texteRes)
    val forme = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(forme)
            .background(MissaSurface)
            .border(1.dp, MissaBorder.copy(alpha = 0.75f), forme)
            .clickable(onClickLabel = libelle, role = Role.Button, onClick = onClick)
            .padding(horizontal = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(couleur.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(icone),
                contentDescription = null,
                tint = couleur,
                modifier = Modifier.size(17.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = libelle,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = MissaInk,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
        Icon(
            painter = painterResource(Iv.ChevronRight),
            contentDescription = null,
            tint = MissaMuted,
            modifier = Modifier.size(16.dp),
        )
    }
}

/** Bouton d'accès principal, toujours présent en bas de l'écran. */
@Composable
private fun OnbBoutonAcceder(actif: Boolean, onClick: () -> Unit) {
    val libelle = stringResource(R.string.obn_acceder)
    val forme = RoundedCornerShape(15.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .shadow(
                elevation = if (actif) 4.dp else 0.dp,
                shape = forme,
                spotColor = BrandBlue.copy(alpha = 0.28f),
            )
            .clip(forme)
            .background(if (actif) BrandBlue else BrandBlue.copy(alpha = 0.4f))
            .clickable(
                enabled = actif,
                onClickLabel = libelle,
                role = Role.Button,
                onClick = onClick,
            ),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(Iv.Home),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = libelle,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.width(8.dp))
        Icon(
            painter = painterResource(Iv.ArrowForward),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(18.dp),
        )
    }
}
