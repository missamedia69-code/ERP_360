package com.missa.b360.ui.onboarding

import com.missa.b360.ui.icons.Iv
import android.content.Intent
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.missa.b360.R
import com.missa.b360.core.domain.model.CleIdentifiant
import com.missa.b360.core.domain.model.ProfilActivite
import com.missa.b360.core.domain.model.ReferentielFiscal
import com.missa.b360.core.domain.model.ReferentielPackPays
import com.missa.b360.core.domain.model.RegleIdentifiant
import com.missa.b360.core.domain.model.TypeImpotRevenu
import com.missa.b360.core.domain.model.TypeTaxe
import com.missa.b360.core.util.Iso4217
import com.missa.b360.core.util.MoneyUtils
import com.missa.b360.ui.components.CompanyLogo
import com.missa.b360.ui.components.MissaOption
import com.missa.b360.ui.components.MissaSelecteurLigne
import com.missa.b360.ui.theme.BrandBlue
import com.missa.b360.ui.theme.MissaBorder
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted
import com.missa.b360.ui.theme.MissaSurface
import com.missa.b360.ui.theme.OnbActionGreen
import com.missa.b360.ui.theme.OnbConfigCard
import com.missa.b360.ui.theme.Red40

private val IMAGE_MIME_TYPES = arrayOf("image/png", "image/jpeg", "image/webp")

private const val LOGO_MAX_BYTES = 2L * 1024 * 1024

/**
 * Remplissage des zones de saisie : toujours blanc, sur la carte bleue comme
 * ailleurs — le fond transparent par défaut de Material 3 laissait apparaître
 * le bleu de la carte et la zone à remplir ne se distinguait pas.
 */
@Composable
private fun OnbCouleursChampBlanc(): TextFieldColors = TextFieldDefaults.colors(
    unfocusedContainerColor = Color.White,
    focusedContainerColor = Color.White,
    disabledContainerColor = Color.White,
    errorContainerColor = Color.White,
)

/**
 * Écran 4 — Informations sur votre entreprise (version compacte).
 *
 * Plus de sections repliables : trois cartes toujours visibles — identité,
 * localisation, coordonnées — la carte coordonnées portant aussi la zone logo
 * comme dernière rangée de sa matrice. Les cadres de saisie et de sélection
 * sont remplis de blanc pour se détacher de la carte bleue.
 *
 * Dans la localisation, le choix de la devise est posé juste à côté du
 * sélecteur de pays (matrice deux colonnes) ; le bas de la carte est réservé
 * au pack fiscal (synthèse en une ligne, détail derrière un lien discret).
 * Dans les coordonnées, l'email de récupération du compte Propriétaire occupe
 * la place de la seconde ligne d'adresse. La zone logo reprend l'allure de la
 * restauration de sauvegarde (cadre blanc, bouton vert). Les cadres sont
 * serrés (padding 12 × 10, espacements 6–8 dp) et les sélecteurs sont
 * resserrés (padding vertical 10 dp) pour épouser la hauteur des champs.
 *
 * Les champs utilisent le libellé flottant de Material 3 plutôt qu'un titre
 * posé au-dessus : même information, une trentaine de points gagnés par champ.
 */
@Composable
internal fun OnbEntrepriseStep(viewModel: OnboardingViewModel) {
    var siteModifieManuellement by remember { mutableStateOf(viewModel.nomSitePrincipal.isNotBlank()) }
    var detailFiscalOuvert by rememberSaveable { mutableStateOf(false) }
    val locale = LocalConfiguration.current.locales.takeIf { !it.isEmpty }?.get(0) ?: java.util.Locale.getDefault()
    val paysListe = remember(locale) { Iso4217.paysDisponibles(locale) }
    // Devise officielle de chaque pays : calculée une fois, réutilisée par la liste
    // des pays et par le pack appliqué à la sélection.
    val devisesParPays = remember(locale) {
        paysListe.associate { pays -> pays.code to Iso4217.deviseDuPays(pays.code) }
    }
    val optionsPays = paysListe.map { pays ->
        val taxe = libelleTaxePays(pays.typeTaxe, pays.tauxTaxeSuggere)
        MissaOption(
            cle = pays.code,
            titre = pays.nom,
            // La monnaie ouvre la ligne, la taxe suit : « XAF · 19,25 % · TVA ».
            sousTitre = devisesParPays[pays.code]?.let { devise -> "$devise · $taxe" } ?: taxe,
            badge = pays.code,
            // L'indicatif complète la pastille : il pilote le champ téléphone
            // et sert aussi de repère pour reconnaître le bon territoire.
            badgeSecondaire = Iso4217.indicatifTelephone(pays.code),
        )
    }
    val deviseSuggeree = devisesParPays[viewModel.codePays]
    // Catalogue ISO complet, la devise du pays choisi remontée en tête de liste.
    val devisesListe = remember(locale, deviseSuggeree) {
        Iso4217.devisesDisponibles(locale).sortedByDescending { it.code == deviseSuggeree }
    }
    val optionsDevise = devisesListe
        .map { devise ->
            MissaOption(
                cle = devise.code,
                titre = devise.nom,
                sousTitre = if (devise.code == deviseSuggeree && viewModel.pays.isNotBlank()) {
                    stringResource(R.string.fisc_devise_suggeree, viewModel.pays)
                } else {
                    null
                },
                badge = devise.code,
            )
        }
    val typeTaxePays = paysListe.firstOrNull { it.code == viewModel.codePays }?.typeTaxe
    val tauxTaxeInvalide = !viewModel.tauxTaxeEstValide()
    val emailValide = viewModel.emailEntrepriseEstValide()
    // Email de récupération : il crée le compte Propriétaire (RA-03 / D1),
    // saisi ici avec les coordonnées — sans lui valide, on n'avance pas.
    val emailSecoursValide = viewModel.emailEstValide()
    val emailSecoursInvalide = viewModel.emailSecours.isNotBlank() && !emailSecoursValide
    val zoneFiscale = ReferentielFiscal.zone(viewModel.codePays)
    val libelleZone = stringResource(zoneFiscale.libelleRes)
    val zoneComplet = zoneFiscale.referentielComptable?.let { "$libelleZone · $it" } ?: libelleZone
    val indicatif = Iso4217.indicatifTelephone(viewModel.codePays)
    val reglesIdentifiants = ReferentielFiscal.regles(
        codePays = viewModel.codePays,
        libelleFiscalGenerique = stringResource(R.string.fisc_id_fiscal),
        libelleRegistreGenerique = stringResource(R.string.fisc_id_registre),
    )
    val aCompleter = stringResource(R.string.obn_section_a_completer)
    val facultatif = stringResource(R.string.obn_logo_optionnel)
    // Pack Personnel : l'écran devient « votre espace » — pas d'identité
    // sociale, pas d'identifiants fiscaux, pas de taux ; le nom, le pays,
    // la devise et les coordonnées restent.
    val personnel = viewModel.profil == ProfilActivite.PERSONNEL
    // Le taux saisi prime sur le taux catalogue : c'est lui qui sera enregistré.
    val valeurTaxe = when {
        typeTaxePays == null -> stringResource(R.string.fisc_taux_a_renseigner)
        typeTaxePays == TypeTaxe.AUCUNE -> stringResource(typeTaxePays.libelleRes)
        viewModel.tauxTaxeTexte.isBlank() -> stringResource(typeTaxePays.libelleRes)
        else -> "${viewModel.tauxTaxeTexte} % · " + stringResource(typeTaxePays.libelleRes)
    }

    OnbScaffold(
        titreRes = if (personnel) R.string.obn_entreprise_titre_personnel else R.string.obn_entreprise_titre,
        sousTitreRes = if (personnel) R.string.obn_entreprise_sous_personnel else R.string.obn_entreprise_sous,
        viewModel = viewModel,
        boutonPleineLargeur = true,
        boutonActive = !viewModel.enregistrementEnCours && emailValide && emailSecoursValide,
        onRetour = viewModel::precedent,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // --- 1. Identité (ou « Vous » pour le pack Personnel) ---
            OnbCompactCarte(
                titreRes = if (personnel) {
                    R.string.obn_section_personnelle
                } else {
                    R.string.obn_section_identite
                },
                icone = if (personnel) Iv.Person else Iv.Business,
                etiquette = aCompleter.takeIf {
                    if (personnel) viewModel.votreNom.isBlank() else viewModel.nomEntreprise.isBlank()
                },
            ) {
                if (personnel) {
                    OnbChampTexte(
                        valeur = viewModel.votreNom,
                        onValeur = { viewModel.votreNom = it },
                        label = stringResource(R.string.ob_votre_nom),
                        icone = Iv.Person,
                        active = !viewModel.enregistrementEnCours,
                    )
                } else {
                    OnbChampTexte(
                        valeur = viewModel.nomEntreprise,
                        onValeur = { nom ->
                            val ancienNom = viewModel.nomEntreprise
                            viewModel.nomEntreprise = nom
                            if (!siteModifieManuellement || viewModel.nomSitePrincipal == ancienNom) {
                                viewModel.nomSitePrincipal = nom
                            }
                        },
                        label = stringResource(R.string.ob_nom_entreprise),
                        icone = Iv.Business,
                        placeholder = stringResource(R.string.obn_nom_ex),
                        active = !viewModel.enregistrementEnCours,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        OnbChampTexte(
                            valeur = viewModel.secteur,
                            onValeur = { viewModel.secteur = it },
                            label = stringResource(R.string.obn_secteur),
                            icone = Iv.Category,
                            placeholder = stringResource(R.string.obn_secteur_ex),
                            modifier = Modifier.weight(1f),
                            active = !viewModel.enregistrementEnCours,
                        )
                        OnbChampTexte(
                            valeur = viewModel.nomSitePrincipal,
                            onValeur = {
                                siteModifieManuellement = true
                                viewModel.nomSitePrincipal = it
                            },
                            label = stringResource(R.string.ob_site_principal),
                            icone = Iv.Storefront,
                            modifier = Modifier.weight(1f),
                            active = !viewModel.enregistrementEnCours,
                        )
                    }
                }
            }

            // --- 2. Localisation : le pays pilote tout le pack fiscal ---
            OnbCompactCarte(
                titreRes = R.string.obn_entreprise_localisation,
                icone = Iv.Public,
                etiquette = aCompleter.takeIf { viewModel.pays.isBlank() },
            ) {
                // Matrice pays / devise sur une seule rangée : le choix de la
                // devise remonte ici, au niveau du pays qui la pilote.
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    MissaSelecteurLigne(
                        label = stringResource(R.string.ob_pays),
                        options = optionsPays,
                        selectionCle = viewModel.codePays,
                        onSelection = { code ->
                            paysListe.firstOrNull { it.code == code }?.let { pays ->
                                viewModel.choisirPays(pays.nom, pays.code, pays.tauxTaxeSuggere)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !viewModel.enregistrementEnCours,
                        placeholder = viewModel.pays.ifBlank {
                            stringResource(R.string.ob_selectionne)
                        },
                        paddingVertical = 10.dp,
                        couleurCarte = MissaSurface,
                        bordureCarte = BorderStroke(1.dp, MissaBorder),
                        rayonCarte = RoundedCornerShape(12.dp),
                    )
                    MissaSelecteurLigne(
                        label = stringResource(R.string.obn_devise_principale),
                        options = optionsDevise,
                        selectionCle = viewModel.devise,
                        onSelection = { code -> viewModel.devise = code },
                        modifier = Modifier.weight(1f),
                        enabled = !viewModel.enregistrementEnCours,
                        paddingVertical = 10.dp,
                        couleurCarte = MissaSurface,
                        bordureCarte = BorderStroke(1.dp, MissaBorder),
                        rayonCarte = RoundedCornerShape(12.dp),
                    )
                }
                // Taux de taxe : sans objet pour le pack Personnel — il est
                // porté d'office par le pack pays.
                if (!personnel) {
                    OnbChampTexte(
                        valeur = viewModel.tauxTaxeTexte,
                        onValeur = viewModel::modifierTauxTaxe,
                        label = stringResource(R.string.ob_taux_taxe),
                        icone = Iv.Percent,
                        clavier = KeyboardType.Decimal,
                        erreur = tauxTaxeInvalide,
                        aide = stringResource(R.string.ob_erreur_taux_taxe)
                            .takeIf { tauxTaxeInvalide },
                        active = !viewModel.enregistrementEnCours,
                    )
                }
                // Le bas de la carte est réservé au pack fiscal du pays :
                // une ligne de synthèse, le détail derrière un lien discret.
                if (viewModel.pays.isBlank()) {
                    Text(
                        text = stringResource(R.string.fisc_pack_aucun_pays),
                        fontSize = 10.5.sp,
                        color = MissaMuted,
                    )
                } else if (!personnel) {
                    val resumePack = listOf(
                        valeurTaxe,
                        zoneComplet,
                        reglesIdentifiants.joinToString(" · ") { it.libelle },
                        indicatif.orEmpty(),
                    ).filter { it.isNotBlank() }
                    if (resumePack.isNotEmpty()) {
                        Text(
                            text = resumePack.joinToString("  ·  "),
                            fontSize = 10.5.sp,
                            color = MissaMuted,
                            maxLines = 2,
                        )
                    }
                    ReferentielPackPays.pack(viewModel.codePays)?.let { detail ->
                        OnbPackLien(
                            texteRes = R.string.fisc_pack_detail,
                            ouvert = detailFiscalOuvert,
                            onClic = { detailFiscalOuvert = !detailFiscalOuvert },
                        )
                        AnimatedVisibility(visible = detailFiscalOuvert) {
                            Column {
                                OnbPackRangee(
                                    gauche = detail.tauxReduits.takeIf { it.isNotEmpty() }?.let {
                                        R.string.fisc_pack_taux_reduits to
                                            it.joinToString(" · ", transform = Iso4217::formatPourcentage)
                                    },
                                    droite = R.string.fisc_pack_seuil to (
                                        detail.seuilAssujettissement
                                            ?.let { MoneyUtils.format(it.toDouble(), viewModel.devise) }
                                            ?: stringResource(R.string.fisc_pack_seuil_aucun)
                                        ),
                                )
                                val tauxIs = Iso4217.formatPourcentage(detail.impotSocietes)
                                OnbPackRangee(
                                    gauche = R.string.fisc_pack_impot_societes to (
                                        detail.impotSocietesMinimum?.let { minimum ->
                                            stringResource(
                                                R.string.fisc_pack_is_minimum,
                                                tauxIs,
                                                Iso4217.formatPourcentage(minimum),
                                            )
                                        } ?: tauxIs
                                        ),
                                    droite = R.string.fisc_pack_impot_revenu to
                                        if (detail.impotRevenu == TypeImpotRevenu.AUCUN) {
                                            stringResource(TypeImpotRevenu.AUCUN.libelleRes)
                                        } else {
                                            stringResource(
                                                R.string.fisc_pack_ir_valeur,
                                                stringResource(detail.impotRevenu.libelleRes),
                                                Iso4217.formatPourcentage(detail.impotRevenuMax),
                                            )
                                        },
                                )
                                val eFacture = detail.eFacturation
                                OnbPackRangee(
                                    gauche = R.string.fisc_pack_efacture to if (eFacture == null) {
                                        stringResource(R.string.fisc_pack_efacture_aucune)
                                    } else {
                                        val dispositif = eFacture.format
                                            ?.let { "${eFacture.systeme} ($it)" }
                                            ?: eFacture.systeme
                                        stringResource(
                                            if (eFacture.obligatoire) {
                                                R.string.fisc_pack_efacture_obligatoire
                                            } else {
                                                R.string.fisc_pack_efacture_facultative
                                            },
                                            dispositif,
                                        )
                                    },
                                    droite = indicatif?.let { R.string.fisc_pack_indicatif to it },
                                )
                            }
                        }
                    }
                }
            }

            // --- 3. Coordonnées : reprises sur les documents commerciaux ---
            // (et identifiants légaux, quand le pays en attend)
            OnbCompactCarte(
                titreRes = R.string.obn_section_coordonnees,
                icone = Iv.Call,
                etiquette = if (emailValide && emailSecoursValide) facultatif else aCompleter,
                etiquetteEnErreur = !emailValide || emailSecoursInvalide,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OnbChampTexte(
                        valeur = viewModel.telephone,
                        onValeur = { viewModel.telephone = it },
                        label = stringResource(R.string.obn_telephone),
                        icone = Iv.Call,
                        placeholder = indicatif,
                        clavier = KeyboardType.Phone,
                        modifier = Modifier.weight(1f),
                        active = !viewModel.enregistrementEnCours,
                    )
                    OnbChampTexte(
                        valeur = viewModel.email,
                        onValeur = { viewModel.email = it },
                        label = stringResource(R.string.obn_email),
                        icone = Iv.MailOutline,
                        placeholder = stringResource(R.string.obn_email_ex),
                        clavier = KeyboardType.Email,
                        erreur = !emailValide,
                        aide = stringResource(R.string.obn_erreur_email_entreprise)
                            .takeIf { !emailValide },
                        modifier = Modifier.weight(1f),
                        active = !viewModel.enregistrementEnCours,
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OnbChampTexte(
                        valeur = viewModel.adresse,
                        onValeur = { viewModel.adresse = it },
                        label = stringResource(R.string.obn_adresse),
                        icone = Iv.Place,
                        placeholder = stringResource(R.string.obn_adresse_ex),
                        modifier = Modifier.weight(1f),
                        active = !viewModel.enregistrementEnCours,
                    )
                    // Email de récupération (Propriétaire) : il s'est remplacé
                    // dans l'espace libéré par l'adresse réduite à une ligne.
                    OnbChampTexte(
                        valeur = viewModel.emailSecours,
                        onValeur = { viewModel.emailSecours = it },
                        label = stringResource(R.string.obn_email_recuperation),
                        icone = Iv.Email,
                        clavier = KeyboardType.Email,
                        erreur = emailSecoursInvalide,
                        aide = stringResource(R.string.ob_email_invalide)
                            .takeIf { emailSecoursInvalide },
                        modifier = Modifier.weight(1f),
                        active = !viewModel.enregistrementEnCours,
                    )
                }
                // Identifiants légaux : sans objet pour le pack Personnel
                // (personne, pas d'entité).
                if (!personnel && reglesIdentifiants.isNotEmpty()) {
                    if (reglesIdentifiants.size == 1) {
                        val regle = reglesIdentifiants[0]
                        OnbChampIdentifiant(
                            regle = regle,
                            valeur = when (regle.cle) {
                                CleIdentifiant.FISCAL -> viewModel.numeroFiscal
                                CleIdentifiant.REGISTRE -> viewModel.registreCommerce
                            },
                            onValeur = { saisie ->
                                when (regle.cle) {
                                    CleIdentifiant.FISCAL -> viewModel.numeroFiscal = saisie
                                    CleIdentifiant.REGISTRE -> viewModel.registreCommerce = saisie
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            active = !viewModel.enregistrementEnCours,
                        )
                    } else {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            reglesIdentifiants.forEach { regle ->
                                OnbChampIdentifiant(
                                    regle = regle,
                                    valeur = when (regle.cle) {
                                        CleIdentifiant.FISCAL -> viewModel.numeroFiscal
                                        CleIdentifiant.REGISTRE -> viewModel.registreCommerce
                                    },
                                    onValeur = { saisie ->
                                        when (regle.cle) {
                                            CleIdentifiant.FISCAL -> viewModel.numeroFiscal = saisie
                                            CleIdentifiant.REGISTRE -> viewModel.registreCommerce = saisie
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    active = !viewModel.enregistrementEnCours,
                                )
                            }
                        }
                    }
                }
                // La zone logo complète la matrice de la carte : toujours
                // visible, directement accessible, sans section à dérouler.
                OnbLogoDirect(
                    logoUri = viewModel.logoUri,
                    enabled = !viewModel.enregistrementEnCours,
                    onLogoSelected = viewModel::definirLogoUri,
                    onLogoCleared = { viewModel.definirLogoUri(null) },
                )
            }
        }
    }
}

/**
 * Carte compacte de l'écran : icône, titre court et contenu TOUJOURS visible
 * (plus de sections repliables). Le badge signale une case à compléter.
 */
@Composable
private fun OnbCompactCarte(
    titreRes: Int,
    icone: Int,
    etiquette: String?,
    etiquetteEnErreur: Boolean = false,
    contenu: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        color = OnbConfigCard,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(icone),
                    contentDescription = null,
                    tint = BrandBlue,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(titreRes),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MissaInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (etiquette != null) {
                    Surface(
                        color = if (etiquetteEnErreur) {
                            Red40.copy(alpha = 0.08f)
                        } else {
                            BrandBlue.copy(alpha = 0.08f)
                        },
                        shape = RoundedCornerShape(5.dp),
                    ) {
                        Text(
                            text = etiquette,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (etiquetteEnErreur) Red40 else BrandBlue,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.5.dp),
                        )
                    }
                }
            }
            contenu()
        }
    }
}

/**
 * Champ de saisie standard de l'écran : libellé flottant Material 3 et icône de
 * marque à l'intérieur du contour. Le texte d'aide n'occupe de la place que
 * lorsqu'il a quelque chose à dire.
 *
 * @param aideNeutre affiche l'aide en gris même en erreur (format attendu)
 */
@Composable
private fun OnbChampTexte(
    valeur: String,
    onValeur: (String) -> Unit,
    label: String,
    icone: Int,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    clavier: KeyboardType = KeyboardType.Text,
    erreur: Boolean = false,
    aide: String? = null,
    aideNeutre: Boolean = false,
    lignesMin: Int = 1,
    active: Boolean = true,
) {
    OutlinedTextField(
        value = valeur,
        onValueChange = onValeur,
        label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingIcon = {
            Icon(
                painter = painterResource(icone),
                contentDescription = null,
                tint = if (erreur) Red40 else BrandBlue,
                modifier = Modifier.size(18.dp),
            )
        },
        placeholder = placeholder?.let { { Text(it, fontSize = 13.sp, color = MissaMuted) } },
        singleLine = lignesMin == 1,
        minLines = lignesMin,
        isError = erreur,
        colors = OnbCouleursChampBlanc(),
        enabled = active,
        keyboardOptions = KeyboardOptions(keyboardType = clavier),
        supportingText = aide?.let {
            {
                Text(
                    text = it,
                    fontSize = 11.sp,
                    color = if (erreur && !aideNeutre) Red40 else MissaMuted,
                )
            }
        },
        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier.fillMaxWidth(),
    )
}

/** Identifiant légal (NIF, RC, …) : l'erreur de format s'affiche sur place. */
@Composable
private fun OnbChampIdentifiant(
    regle: RegleIdentifiant,
    valeur: String,
    onValeur: (String) -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = true,
) {
    val formatIncorrect = !regle.estValide(valeur)
    OnbChampTexte(
        valeur = valeur,
        onValeur = onValeur,
        label = regle.libelle,
        icone = when (regle.cle) {
            CleIdentifiant.FISCAL -> Iv.Badge
            CleIdentifiant.REGISTRE -> Iv.Gavel
        },
        placeholder = regle.exemple.takeIf { it.isNotEmpty() },
        erreur = formatIncorrect,
        aide = if (formatIncorrect && regle.exemple.isNotEmpty()) {
            stringResource(R.string.fisc_format_attendu, regle.exemple)
        } else {
            null
        },
        aideNeutre = true,
        modifier = modifier,
        active = active,
    )
}

/**
 * Lien compact (chevron + libellé) : remplace les anciens sous-menus — un
 * clic ouvre le bloc directement en place.
 */
@Composable
private fun OnbPackLien(texteRes: Int, ouvert: Boolean, onClic: () -> Unit) {
    TextButton(
        onClick = onClic,
        contentPadding = PaddingValues(
            horizontal = 6.dp,
            vertical = 2.dp,
        ),
    ) {
        Icon(
            painter = painterResource(if (ouvert) Iv.ExpandLess else Iv.ExpandMore),
            contentDescription = null,
            modifier = Modifier.size(15.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(text = stringResource(texteRes), fontSize = 11.5.sp)
    }
}

/**
 * Rangée de deux cellules du détail fiscal. La cellule de droite peut
 * manquer : la gauche garde alors sa demi-largeur, pour que les colonnes
 * restent alignées d'une rangée à l'autre.
 */
@Composable
private fun OnbPackRangee(
    gauche: Pair<Int, String>?,
    droite: Pair<Int, String>? = null,
) {
    if (gauche == null && droite == null) return
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        OnbPackCellule(gauche, Modifier.weight(1f))
        OnbPackCellule(droite, Modifier.weight(1f))
    }
}

/** Cellule « libellé au-dessus, valeur en dessous » du détail fiscal. */
@Composable
private fun OnbPackCellule(contenu: Pair<Int, String>?, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        if (contenu != null) {
            Text(
                text = stringResource(contenu.first),
                fontSize = 10.sp,
                color = MissaMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = contenu.second,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = MissaInk,
                lineHeight = 14.sp,
            )
        }
    }
}

/**
 * Libellé unique de la taxe d'un pays, pour la liste comme pour le pack :
 * « 19,25 % · TVA », « Aucune taxe à la consommation » quand le pays n'en lève
 * pas, « Taux à renseigner » quand le référentiel ignore le territoire — jamais
 * un « 0 % » qui laisserait croire à une exonération.
 */
@Composable
internal fun libelleTaxePays(typeTaxe: TypeTaxe?, taux: Double): String = when {
    typeTaxe == null -> stringResource(R.string.fisc_taux_a_renseigner)
    typeTaxe == TypeTaxe.AUCUNE || taux <= 0.0 -> stringResource(typeTaxe.libelleRes)
    else -> Iso4217.formatPourcentage(taux) + " · " + stringResource(typeTaxe.libelleRes)
}

/**
 * Logo, directement accessible sur une seule ligne : vignette, formats,
 * bouton « Ajouter » — plus de section à dérouler avant de voir la zone.
 * Rangée de la carte Coordonnées, présentée comme la zone de chargement des
 * sauvegardes de la configuration initiale : cadre blanc à bordure fine et
 * bouton d'action vert plein.
 */
@Composable
private fun OnbLogoDirect(
    logoUri: String?,
    enabled: Boolean,
    onLogoSelected: (String) -> Unit,
    onLogoCleared: () -> Unit,
) {
    val context = LocalContext.current
    var logoTropGrand by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let {
            // OpenDocument fournit un droit persistant. Un fournisseur non compatible ne
            // bloque pas le choix : le droit temporaire permet tout de même l'aperçu en cours.
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            val taille = runCatching {
                context.contentResolver.query(it, null, null, null, null)?.use { cursor ->
                    val idx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (idx >= 0 && cursor.moveToFirst()) cursor.getLong(idx) else -1L
                } ?: -1L
            }.getOrDefault(-1L)
            if (taille in 1..LOGO_MAX_BYTES) {
                logoTropGrand = false
                onLogoSelected(it.toString())
            } else {
                logoTropGrand = true
            }
        }
    }

    Surface(
        color = MissaSurface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MissaBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (logoUri == null) {
                Surface(
                    modifier = Modifier
                        .size(40.dp)
                        .dashedBorder(1.2.dp, MissaBorder, 10.dp),
                    color = MissaSurface,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(Iv.Backup),
                            contentDescription = null,
                            tint = BrandBlue,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            } else {
                CompanyLogo(
                    logoUri = logoUri,
                    contentDescription = stringResource(R.string.ob_logo_apercu),
                    fallbackIcon = Iv.Backup,
                    modifier = Modifier.size(40.dp),
                    size = 40.dp,
                    shape = RoundedCornerShape(10.dp),
                    fallbackTint = BrandBlue,
                    fallbackBackground = BrandBlue.copy(alpha = 0.07f),
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.obn_logo_titre),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MissaInk,
                )
                Text(
                    text = stringResource(R.string.obn_logo_formats),
                    fontSize = 10.sp,
                    color = MissaMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (logoTropGrand) {
                    Text(
                        text = stringResource(R.string.obn_logo_trop_grand),
                        fontSize = 10.sp,
                        color = Red40,
                    )
                }
            }
            Spacer(Modifier.width(6.dp))
            // Bouton vert plein, comme la zone de restauration des sauvegardes.
            Button(
                onClick = { launcher.launch(IMAGE_MIME_TYPES) },
                enabled = enabled,
                shape = RoundedCornerShape(9.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = OnbActionGreen,
                    disabledContainerColor = OnbActionGreen.copy(alpha = 0.4f),
                ),
                contentPadding = PaddingValues(
                    horizontal = 12.dp,
                    vertical = 7.dp,
                ),
            ) {
                Text(
                    text = stringResource(
                        if (logoUri == null) {
                            R.string.obn_logo_parcourir
                        } else {
                            R.string.ob_logo_modifier
                        },
                    ),
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                )
            }
            if (logoUri != null) {
                IconButton(onClick = onLogoCleared, enabled = enabled) {
                    Icon(
                        painter = painterResource(Iv.DeleteOutline),
                        contentDescription = stringResource(R.string.ob_logo_supprimer),
                        tint = Red40,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

/** Bordure pointillée du cadre de dépôt de logo (maquette). */
private fun Modifier.dashedBorder(width: androidx.compose.ui.unit.Dp, color: androidx.compose.ui.graphics.Color, radius: androidx.compose.ui.unit.Dp): Modifier =
    this.drawBehind {
        val r = radius.toPx()
        val path = Path().apply {
            addRoundRect(
                androidx.compose.ui.geometry.RoundRect(
                    0f,
                    0f,
                    size.width,
                    size.height,
                    androidx.compose.ui.geometry.CornerRadius(r),
                ),
            )
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = width.toPx(),
                pathEffect = PathEffect.dashPathEffect(
                    floatArrayOf(7f * density, 6f * density),
                    0f,
                ),
            ),
        )
    }
