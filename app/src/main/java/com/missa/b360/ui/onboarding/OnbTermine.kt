package com.missa.b360.ui.onboarding

import android.widget.Toast
import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.missa.b360.R
import com.missa.b360.core.domain.model.ProfilActivite
import com.missa.b360.core.util.ContactCommercial
import com.missa.b360.core.util.DateUtils
import com.missa.b360.core.util.Iso4217
import com.missa.b360.ui.icons.Iv
import com.missa.b360.ui.theme.BrandBlue
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaLime
import com.missa.b360.ui.theme.MissaMuted
import com.missa.b360.ui.theme.Red40
import com.missa.b360.ui.theme.Red80
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Écran 7 — Tout est prêt (clôture RA-11 ; l'application démarrera sur le
 * verrou PIN).
 *
 * Trois cartes et un bouton (maquette) : le **récapitulatif** de tout ce qui
 * vient d'être configuré (lignes avec icônes et badge « Configuration
 * réussie »), l'**annonce de l'essai de 7 jours** — qui a réellement démarré
 * en base à la création de l'entreprise (RA-04), d'où l'échéance lue et non
 * recalculée — et le moyen de **commander son code d'activation** via
 * WhatsApp, Telegram ou e-mail. Un code déjà activé fait disparaître
 * l'argumentaire : on ne vend pas ce qui est acheté.
 */
@Composable
internal fun OnbTermineStep(viewModel: OnboardingViewModel) {
    LaunchedEffect(Unit) { viewModel.chargerEtatLicence() }

    val locale = LocalConfiguration.current.locales.takeIf { !it.isEmpty }?.get(0) ?: java.util.Locale.getDefault()
    val paysListe = remember(locale) { Iso4217.paysDisponibles(locale) }
    val typeTaxe = remember(paysListe, viewModel.codePays) {
        paysListe.firstOrNull { it.code == viewModel.codePays }?.typeTaxe
    }
    val profilLabel = viewModel.profil?.let { stringResource(it.labelRes) } ?: "—"
    val personnel = viewModel.profil == ProfilActivite.PERSONNEL
    val tailleLabel = viewModel.palier?.let { stringResource(it.labelRes) } ?: "—"
    // Le nom de la devise vient du référentiel ISO du système : toutes les
    // devises sont couvertes, pas seulement le catalogue court.
    val deviseLabel = if (viewModel.devise.isBlank()) {
        "—"
    } else {
        "${viewModel.devise} · ${Iso4217.nomDevise(viewModel.devise, locale)}"
    }
    val identifiants = listOf(viewModel.numeroFiscal, viewModel.registreCommerce)
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .joinToString(" · ")
    val proprietaire = listOf(viewModel.votreNom.trim().ifBlank { viewModel.nomEntreprise.trim() }, viewModel.emailSecours.trim())
        .filter { it.isNotEmpty() }
        .joinToString(" · ")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFD))
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {
        // En-tête : confettis, coche, titre bicolore et sous-titre.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(212.dp),
            contentAlignment = Alignment.Center,
        ) {
            OnbConfettiRays()
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .shadow(10.dp, CircleShape)
                        .clip(CircleShape)
                        .background(Color(0xFF22C55E)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(Iv.Check),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(30.dp),
                    )
                }
                Spacer(Modifier.height(13.dp))
                OnbTermineTitre()
                Spacer(Modifier.height(5.dp))
                Text(
                    text = stringResource(R.string.obn_terminer_sous),
                    fontSize = 12.sp,
                    color = MissaMuted,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
            }
        }

        // La finalisation (terminer) peut échouer : on le signale comme les
        // autres étapes, l'écran reste sur « Tout est prêt » tant que ce n'est
        // pas consommé.
        val erreurActuelle = viewModel.erreurRes
        if (erreurActuelle != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Red80)
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(Iv.Warning),
                    contentDescription = null,
                    tint = Red40,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(erreurActuelle),
                    color = Red40,
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

        OnbBoutonAcceder(
            actif = !viewModel.enregistrementEnCours,
            onClick = viewModel::suivant,
        )
        Spacer(Modifier.height(6.dp))
    }
}

/** Titre « Tout est prêt ! » : la dernière partie est surlignée en vert. */
@Composable
private fun OnbTermineTitre() {
    val base = stringResource(R.string.obn_terminer_titre)
    val surligne = stringResource(R.string.obn_terminer_pret)
    val titre = remember(base, surligne) {
        buildAnnotatedString {
            append(base)
            withStyle(SpanStyle(color = Color(0xFF16A34A))) {
                append(" $surligne")
            }
        }
    }
    Text(
        text = titre,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        color = MissaInk,
    )
}

/**
 * Récapitulatif complet, en trois blocs : l'entreprise et son cadre fiscal,
 * l'activité et ses modules, puis l'accès. Chaque ligne porte son icône ; les
 * lignes vides sont omises — afficher « — » à répétition donnerait
 * l'impression d'une configuration ratée.
 */
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, Color(0xFFE4EBF6), RoundedCornerShape(18.dp))
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(Color(0xFFE3F0FE)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(Iv.Description),
                    contentDescription = null,
                    tint = BrandBlue,
                    modifier = Modifier.size(19.dp),
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
                    .clip(RoundedCornerShape(50.dp))
                    .background(Color(0xFFE2F5E8))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(Iv.Check),
                    contentDescription = null,
                    tint = Color(0xFF16A34A),
                    modifier = Modifier.size(12.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.obn_recap_badge),
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF16A34A),
                )
            }
        }
        Spacer(Modifier.height(12.dp))

        OnbRecapLigne(R.string.obn_recap_entreprise, entreprise, Iv.Business)
        OnbRecapLigne(R.string.obn_recap_pays, pays, Iv.Person)
        OnbRecapLigne(R.string.obn_recap_devise, devise, Iv.Savings)
        // Fiscalité, taille et modules : sans objet pour le pack Personnel.
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
                .background(Color(0xFFEDF4FE))
                .padding(12.dp),
        ) {
            OnbRecapLigne(R.string.obn_recap_proprietaire, proprietaire, Iv.Security)
        }
    }
}

@Composable
private fun OnbRecapSeparateur() {
    Spacer(Modifier.height(8.dp))
    HorizontalDivider(color = Color(0xFFE4EBF6))
    Spacer(Modifier.height(8.dp))
}

/**
 * Une ligne du récapitulatif : icône teintée, libellé, valeur. Rien ne
 * s'affiche si la valeur est vide.
 */
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
                .background(Color(0xFFEAF1FE)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(icone),
                contentDescription = null,
                tint = BrandBlue,
                modifier = Modifier.size(13.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
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

/**
 * Annonce de l'essai : carte verte (maquette) avec calendrier « 7 JOURS ».
 * L'échéance réelle, relue en base : l'essai a pu démarrer la veille. Un code
 * déjà activé retient la mention « licence active » à la place.
 */
@Composable
private fun OnbEssaiCarte(viewModel: OnboardingViewModel) {
    val echeance = viewModel.essaiExpireLe
    val packLabel = viewModel.profil?.let { stringResource(it.labelRes) }
    val actif = viewModel.licenceDejaActive
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFFECF7EF))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .shadow(4.dp, RoundedCornerShape(11.dp))
                        .clip(RoundedCornerShape(11.dp))
                        .background(Color.White),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(Iv.Calendar),
                        contentDescription = null,
                        tint = BrandBlue,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.width(10.dp))
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
                    color = Color(0xFF44546A),
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
                color = Color(0xFF7A8AA0),
                lineHeight = 14.sp,
                maxLines = 2,
            )
        }
        Spacer(Modifier.width(10.dp))
        // Calendrier « 7 JOURS » coché, à la droite de la carte.
        Box(
            modifier = Modifier.size(width = 102.dp, height = 82.dp),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(width = 92.dp, height = 70.dp)
                    .shadow(4.dp, RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .border(1.dp, Color(0xFFDCE8F5), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "7",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = MissaInk,
                    )
                    Text(
                        text = stringResource(R.string.obn_term_jours),
                        fontSize = 9.sp,
                        letterSpacing = 1.2.sp,
                        color = Color(0xFF7A8AA0),
                    )
                }
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(30.dp)
                    .shadow(4.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Color(0xFF22C55E)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(Iv.Check),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

/**
 * Commande du code d'activation : trois canaux (WhatsApp, Telegram, e-mail)
 * en pleine largeur, chacun avec sa couleur de marque et sa chevronne. Un code
 * déjà activé ou des coordonnées commerciales manquantes remplacent les
 * boutons par une mention.
 */
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, Color(0xFFE4EBF6), RoundedCornerShape(18.dp))
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(Iv.Share),
                contentDescription = null,
                tint = Color(0xFF5B6B84),
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.obn_code_titre),
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = MissaInk,
            )
        }
        Spacer(Modifier.height(12.dp))

        when {
            viewModel.licenceDejaActive -> Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE2F5E8)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(Iv.Check),
                        contentDescription = null,
                        tint = Color(0xFF16A34A),
                        modifier = Modifier.size(14.dp),
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.ob_licence_avantage_sans_carte),
                    fontSize = 11.5.sp,
                    color = Color(0xFF44546A),
                )
            }
            coordonneesManquantes -> Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(Iv.Warning),
                    contentDescription = null,
                    tint = Red40,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.obn_code_a_configurer),
                    fontSize = 11.sp,
                    color = Red40,
                )
            }
            else -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (whatsappOk) {
                    OnbActivationLigne(
                        icone = Iv.Chat,
                        couleur = Color(0xFF16A34A),
                        texteRes = R.string.obn_code_whatsapp,
                    ) {
                        val ouvert = ContactCommercial.ouvrirWhatsApp(contexte, numero, message)
                        if (!ouvert) Toast.makeText(contexte, echec, Toast.LENGTH_LONG).show()
                    }
                }
                if (telegramOk) {
                    OnbActivationLigne(
                        icone = Iv.Send,
                        couleur = Color(0xFF229ED9),
                        texteRes = R.string.obn_code_telegram,
                    ) {
                        val ouvert = ContactCommercial.ouvrirTelegram(contexte, telegram, message)
                        if (!ouvert) Toast.makeText(contexte, echec, Toast.LENGTH_LONG).show()
                    }
                }
                if (emailOk) {
                    OnbActivationLigne(
                        icone = Iv.MailOutline,
                        couleur = Color(0xFF7C3AED),
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

/** Un canal d'activation : icône de marque, libellé, chevronne (maquette). */
@Composable
private fun OnbActivationLigne(
    icone: Int,
    couleur: Color,
    texteRes: Int,
    onClick: () -> Unit,
) {
    val libelle = stringResource(texteRes)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
            .clickable(onClickLabel = libelle, role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(icone),
            contentDescription = null,
            tint = couleur,
            modifier = Modifier.size(18.dp),
        )
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
            tint = Color(0xFF94A3B8),
            modifier = Modifier.size(15.dp),
        )
    }
}

/** Gros bouton bleu « Accéder à l'application » (maquette). */
@Composable
private fun OnbBoutonAcceder(actif: Boolean, onClick: () -> Unit) {
    val libelle = stringResource(R.string.obn_acceder)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
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

/**
 * Confettis décoratifs de l'en-tête : traits courts rayonnant autour de la
 * coche, positions et couleurs fixes (aucun aléa entre recompositions).
 */
@Composable
private fun OnbConfettiRays() {
    val couleurs = listOf(
        Color(0xFF22C55E),
        BrandBlue,
        MissaLime,
        Color(0xFF9DB6D9),
    )
    Canvas(modifier = Modifier.fillMaxSize()) {
        val centre = Offset(size.width / 2f, size.height / 2f)
        for (index in 0 until 14) {
            val angle = index * (2f * PI / 14f) + 0.22f
            val rayonInterieur = (58 + (index % 3) * 4).toFloat() * density
            val longueur = (7 + (index % 4) * 3).toFloat() * density
            val cos = cos(angle).toFloat()
            val sin = sin(angle).toFloat()
            drawLine(
                color = couleurs[index % couleurs.size].copy(alpha = 0.75f),
                start = centre + Offset(cos * rayonInterieur, sin * rayonInterieur),
                end = centre + Offset(
                    cos * (rayonInterieur + longueur),
                    sin * (rayonInterieur + longueur),
                ),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
    }
}
