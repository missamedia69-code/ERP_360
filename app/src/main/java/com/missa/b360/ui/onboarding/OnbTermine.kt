package com.missa.b360.ui.onboarding

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.missa.b360.R
import com.missa.b360.core.util.ContactCommercial
import com.missa.b360.core.util.DateUtils
import com.missa.b360.core.util.Iso4217
import com.missa.b360.ui.theme.BrandBlue
import com.missa.b360.ui.theme.Green60
import com.missa.b360.ui.theme.MissaBorder
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaLime
import com.missa.b360.ui.theme.MissaMuted
import com.missa.b360.ui.theme.MissaSoftBlue
import com.missa.b360.ui.theme.MissaSurface
import com.missa.b360.ui.theme.OnboardingHeroGreen
import com.missa.b360.ui.theme.ProfileGreen
import com.missa.b360.ui.theme.ProfileOrange
import com.missa.b360.ui.theme.ProfilePurple
import com.missa.b360.ui.theme.Red40

/**
 * Écran 7 — Tout est prêt (clôture RA-11 ; l'application démarrera sur le
 * verrou PIN).
 *
 * Trois blocs et un seul de chaque : le **récapitulatif** de tout ce qui vient
 * d'être configuré, l'**annonce de l'essai de 7 jours** — qui a réellement
 * démarré en base à la création de l'entreprise (RA-04), d'où l'échéance lue
 * et non recalculée — et le moyen de **commander son code d'activation**.
 * Le contenu défile ; le bouton d'accès reste posé en bas.
 */
@Composable
internal fun OnbTermineStep(viewModel: OnboardingViewModel) {
    LaunchedEffect(Unit) { viewModel.chargerEtatLicence() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OnboardingHeroGreen)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        ConfettiDots()
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(18.dp))
                Surface(
                    color = Green60,
                    shape = CircleShape,
                    modifier = Modifier.size(72.dp),
                    shadowElevation = 10.dp,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(36.dp),
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.obn_terminer_titre),
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold,
                    color = MissaInk,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.obn_terminer_sous),
                    fontSize = 12.5.sp,
                    color = MissaMuted,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))

                OnbRecapCarte(viewModel)
                Spacer(Modifier.height(12.dp))
                OnbLicenceCarte(viewModel)
                Spacer(Modifier.height(14.dp))
            }

            Button(
                onClick = viewModel::suivant,
                enabled = !viewModel.enregistrementEnCours,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
            ) {
                Text(
                    text = stringResource(R.string.obn_acceder),
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.5.sp,
                )
                Spacer(Modifier.size(9.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.height(14.dp))
        }
    }
}

/**
 * Récapitulatif complet, en trois blocs : l'entreprise et son cadre fiscal,
 * l'activité et ses modules, puis l'accès. Les lignes vides sont omises —
 * afficher « — » à répétition donnerait l'impression d'une configuration ratée.
 */
@Composable
private fun OnbRecapCarte(viewModel: OnboardingViewModel) {
    val locale = LocalConfiguration.current.locales[0]
    val paysListe = remember(locale) { Iso4217.paysDisponibles(locale) }
    val typeTaxe = remember(paysListe, viewModel.codePays) {
        paysListe.firstOrNull { it.code == viewModel.codePays }?.typeTaxe
    }
    val profilLabel = viewModel.profil?.let { stringResource(it.labelRes) } ?: "—"
    val tailleLabel = viewModel.palier?.let { stringResource(it.labelRes) } ?: "—"
    // Le nom vient du référentiel ISO du système : toutes les devises sont
    // couvertes, pas seulement le catalogue court.
    val deviseLabel = if (viewModel.devise.isBlank()) {
        "—"
    } else {
        "${viewModel.devise} · ${Iso4217.nomDevise(viewModel.devise, locale)}"
    }
    val identifiants = listOf(viewModel.numeroFiscal, viewModel.registreCommerce)
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .joinToString(" · ")
    val proprietaire = listOf(viewModel.votreNom.trim(), viewModel.emailSecours.trim())
        .filter { it.isNotEmpty() }
        .joinToString(" · ")

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MissaSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 15.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.obn_recap),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MissaInk,
            )
            OnbRecapLigne(R.string.obn_recap_entreprise, viewModel.nomEntreprise)
            OnbRecapLigne(R.string.obn_recap_pays, viewModel.pays)
            OnbRecapLigne(R.string.obn_recap_devise, deviseLabel)
            OnbRecapLigne(R.string.obn_recap_fiscalite, libelleTaxePays(typeTaxe, viewModel.tauxTaxe))
            OnbRecapLigne(R.string.obn_recap_identifiants, identifiants)

            HorizontalDivider(color = MissaBorder, thickness = 0.8.dp)
            OnbRecapLigne(R.string.obn_recap_profil, profilLabel)
            OnbRecapLigne(R.string.obn_recap_taille, tailleLabel)
            OnbRecapLigne(
                R.string.obn_recap_modules,
                viewModel.modulesActifs.size.toString(),
            )

            HorizontalDivider(color = MissaBorder, thickness = 0.8.dp)
            OnbRecapLigne(R.string.obn_recap_proprietaire, proprietaire)
        }
    }
}

/**
 * Annonce de l'essai et commande du code. Un code déjà activé fait disparaître
 * l'argumentaire : on ne vend pas ce qui est acheté.
 */
@Composable
private fun OnbLicenceCarte(viewModel: OnboardingViewModel) {
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

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MissaSoftBlue),
        border = BorderStroke(1.dp, BrandBlue.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 15.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (viewModel.licenceDejaActive) {
                        Icons.Outlined.WorkspacePremium
                    } else {
                        Icons.Outlined.Schedule
                    },
                    contentDescription = null,
                    tint = if (viewModel.licenceDejaActive) ProfileGreen else BrandBlue,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    text = stringResource(
                        if (viewModel.licenceDejaActive) {
                            R.string.adm_licence_statut_actif
                        } else {
                            R.string.obn_essai_titre
                        },
                    ),
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MissaInk,
                )
            }

            // Échéance réelle, relue en base : l'essai a pu démarrer la veille.
            val echeance = viewModel.essaiExpireLe
            Text(
                text = if (echeance != null) {
                    stringResource(
                        if (viewModel.licenceDejaActive) {
                            R.string.obn_licence_jusqu_au
                        } else {
                            R.string.obn_essai_jusqu_au
                        },
                        DateUtils.formatDate(echeance),
                    )
                } else {
                    stringResource(R.string.ob_licence_avantage_fonctions)
                },
                fontSize = 12.sp,
                color = MissaInk,
            )
            Text(
                text = stringResource(R.string.ob_licence_avantage_sans_carte),
                fontSize = 11.5.sp,
                color = MissaMuted,
            )

            if (!viewModel.licenceDejaActive) {
                Text(
                    text = stringResource(R.string.obn_essai_apres),
                    fontSize = 11.5.sp,
                    color = MissaMuted,
                )
                Spacer(Modifier.height(1.dp))
                Text(
                    text = stringResource(R.string.obn_code_titre),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MissaInk,
                )
                Text(
                    text = stringResource(R.string.ob_licence_note),
                    fontSize = 11.sp,
                    color = MissaMuted,
                )
                if (coordonneesManquantes) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Warning,
                            contentDescription = null,
                            tint = Red40,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.size(6.dp))
                        Text(
                            text = stringResource(R.string.obn_code_a_configurer),
                            fontSize = 11.sp,
                            color = Red40,
                        )
                    }
                } else {
                    // Les deux messageries partagent une ligne, l'e-mail prend la
                    // suivante : trois libellés côte à côte ne tiendraient pas sur
                    // un écran étroit sans être tronqués.
                    if (whatsappOk || telegramOk) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(9.dp),
                            modifier = Modifier.fillMaxWidth().padding(top = 3.dp),
                        ) {
                            if (whatsappOk) {
                                OnbContactBouton(
                                    texteRes = R.string.obn_code_whatsapp,
                                    icone = Icons.Outlined.Chat,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    val ouvert =
                                        ContactCommercial.ouvrirWhatsApp(contexte, numero, message)
                                    if (!ouvert) {
                                        Toast.makeText(contexte, echec, Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                            if (telegramOk) {
                                OnbContactBouton(
                                    texteRes = R.string.obn_code_telegram,
                                    icone = Icons.AutoMirrored.Outlined.Send,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    val ouvert = ContactCommercial.ouvrirTelegram(
                                        contexte,
                                        telegram,
                                        message,
                                    )
                                    if (!ouvert) {
                                        Toast.makeText(contexte, echec, Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        }
                    }
                    if (emailOk) {
                        OnbContactBouton(
                            texteRes = R.string.obn_code_email,
                            icone = Icons.Outlined.MailOutline,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            val ouvert =
                                ContactCommercial.ouvrirEmail(contexte, adresse, objet, message)
                            if (!ouvert) {
                                Toast.makeText(contexte, echec, Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OnbContactBouton(
    texteRes: Int,
    icone: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(42.dp),
        shape = RoundedCornerShape(11.dp),
        border = BorderStroke(1.dp, BrandBlue),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
    ) {
        Icon(
            imageVector = icone,
            contentDescription = null,
            tint = BrandBlue,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.size(6.dp))
        Text(
            text = stringResource(texteRes),
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = BrandBlue,
        )
    }
}

/** Une ligne du récapitulatif ; rien ne s'affiche si la valeur est vide. */
@Composable
private fun OnbRecapLigne(labelRes: Int, valeur: String) {
    if (valeur.isBlank()) return
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(labelRes),
            fontSize = 12.sp,
            color = MissaMuted,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = valeur,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = MissaInk,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1.3f),
        )
    }
}

/**
 * Confettis décoratifs du fond vert : positions fixes (aucun aléa entre
 * recompositions) dans les couleurs de la marque.
 */
private data class ConfettiPoint(val x: Float, val y: Float, val couleur: Color, val rayon: Float)

@Composable
private fun ConfettiDots() {
    val points = listOf(
        ConfettiPoint(0.08f, 0.10f, MissaLime, 5f),
        ConfettiPoint(0.92f, 0.14f, BrandBlue, 4f),
        ConfettiPoint(0.85f, 0.05f, ProfileOrange, 3.5f),
        ConfettiPoint(0.05f, 0.30f, ProfilePurple, 3f),
        ConfettiPoint(0.95f, 0.38f, ProfileGreen, 4f),
        ConfettiPoint(0.07f, 0.62f, BrandBlue, 3f),
        ConfettiPoint(0.93f, 0.70f, MissaLime, 5f),
        ConfettiPoint(0.10f, 0.88f, ProfileOrange, 3.5f),
        ConfettiPoint(0.90f, 0.92f, ProfilePurple, 3f),
    )
    Canvas(modifier = Modifier.fillMaxSize()) {
        points.forEach { point ->
            drawCircle(
                color = point.couleur.copy(alpha = 0.5f),
                radius = point.rayon * density,
                center = androidx.compose.ui.geometry.Offset(point.x * size.width, point.y * size.height),
            )
        }
    }
}
