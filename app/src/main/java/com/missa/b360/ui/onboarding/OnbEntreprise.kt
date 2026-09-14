package com.missa.b360.ui.onboarding

import android.content.Intent
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.Percent
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.missa.b360.R
import com.missa.b360.core.domain.model.CleIdentifiant
import com.missa.b360.core.domain.model.PackPays
import com.missa.b360.core.domain.model.ReferentielFiscal
import com.missa.b360.core.domain.model.ReferentielPackPays
import com.missa.b360.core.domain.model.TypeImpotRevenu
import com.missa.b360.core.domain.model.TypeTaxe
import com.missa.b360.core.domain.model.ZoneFiscale
import com.missa.b360.core.util.Iso4217
import com.missa.b360.core.util.MoneyUtils
import com.missa.b360.ui.components.CompanyLogo
import com.missa.b360.ui.components.MissaOption
import com.missa.b360.ui.components.MissaSectionPliable
import com.missa.b360.ui.components.MissaSelecteurLigne
import com.missa.b360.ui.theme.BrandBlue
import com.missa.b360.ui.theme.MissaBorder
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted
import com.missa.b360.ui.theme.MissaSoftBlue
import com.missa.b360.ui.theme.MissaSurface
import com.missa.b360.ui.theme.Red40

private val IMAGE_MIME_TYPES = arrayOf("image/png", "image/jpeg", "image/webp")

private const val LOGO_MAX_BYTES = 2L * 1024 * 1024

/**
 * Écran 4 — Informations sur votre entreprise.
 *
 * L'écran est découpé en cinq sections repliables de même facture
 * ([MissaSectionPliable]) : identité, localisation et fiscalité, coordonnées,
 * identifiants légaux, logo. Replié, chaque bloc résume en une ligne ce qu'il
 * contient ; les deux premiers — les seuls indispensables — sont ouverts
 * d'emblée. L'ensemble tient ainsi dans un écran et demi au lieu de trois.
 *
 * Les champs utilisent le libellé flottant de Material 3 plutôt qu'un titre
 * posé au-dessus : même information, une trentaine de points gagnés par champ.
 */
@Composable
internal fun OnbEntrepriseStep(viewModel: OnboardingViewModel) {
    var siteModifieManuellement by remember { mutableStateOf(viewModel.nomSitePrincipal.isNotBlank()) }
    val locale = LocalConfiguration.current.locales[0]
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
    val zoneFiscale = ReferentielFiscal.zone(viewModel.codePays)
    val indicatif = Iso4217.indicatifTelephone(viewModel.codePays)
    val reglesIdentifiants = ReferentielFiscal.regles(
        codePays = viewModel.codePays,
        libelleFiscalGenerique = stringResource(R.string.fisc_id_fiscal),
        libelleRegistreGenerique = stringResource(R.string.fisc_id_registre),
    )
    val aCompleter = stringResource(R.string.obn_section_a_completer)
    val facultatif = stringResource(R.string.obn_logo_optionnel)

    OnbScaffold(
        titreRes = R.string.obn_entreprise_titre,
        sousTitreRes = R.string.obn_entreprise_sous,
        viewModel = viewModel,
        boutonPleineLargeur = true,
        boutonActive = !viewModel.enregistrementEnCours && emailValide,
        onRetour = viewModel::precedent,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // --- 1. Identité : ce qui nomme l'entreprise et son site ---
            MissaSectionPliable(
                titre = stringResource(R.string.obn_section_identite),
                icone = Icons.Outlined.Business,
                resume = resume(
                    viewModel.nomEntreprise,
                    viewModel.secteur,
                    viewModel.nomSitePrincipal.takeIf { it != viewModel.nomEntreprise },
                ),
                etiquette = aCompleter.takeIf { viewModel.nomEntreprise.isBlank() },
                ouvertParDefaut = true,
            ) {
                OnbChampsEmpiles {
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
                        icone = Icons.Outlined.Business,
                        placeholder = stringResource(R.string.obn_nom_ex),
                        active = !viewModel.enregistrementEnCours,
                    )
                    OnbChampTexte(
                        valeur = viewModel.secteur,
                        onValeur = { viewModel.secteur = it },
                        label = stringResource(R.string.obn_secteur),
                        icone = Icons.Outlined.Category,
                        placeholder = stringResource(R.string.obn_secteur_ex),
                        active = !viewModel.enregistrementEnCours,
                    )
                    OnbChampTexte(
                        valeur = viewModel.nomSitePrincipal,
                        onValeur = {
                            siteModifieManuellement = true
                            viewModel.nomSitePrincipal = it
                        },
                        label = stringResource(R.string.ob_site_principal),
                        icone = Icons.Outlined.Storefront,
                        active = !viewModel.enregistrementEnCours,
                    )
                }
            }

            // --- 2. Localisation : le pays pilote tout le pack fiscal ---
            MissaSectionPliable(
                titre = stringResource(R.string.obn_entreprise_localisation),
                icone = Icons.Outlined.Public,
                resume = if (viewModel.pays.isBlank()) {
                    stringResource(R.string.ob_selectionne)
                } else {
                    resume(
                        viewModel.pays,
                        viewModel.devise,
                        libelleTaxePays(typeTaxePays, viewModel.tauxTaxe),
                    )
                },
                etiquette = aCompleter.takeIf { viewModel.pays.isBlank() },
                ouvertParDefaut = true,
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
                    enabled = !viewModel.enregistrementEnCours,
                    placeholder = viewModel.pays.ifBlank {
                        stringResource(R.string.ob_selectionne)
                    },
                )
                Spacer(Modifier.height(4.dp))
                // Le pack : valeurs remplies d'office par le pays. Devise, taux et
                // pays libre n'ont pas de champ permanent ailleurs — ils ne
                // redeviennent modifiables que par « modifier manuellement ».
                OnbPackPays(
                    pays = viewModel.pays,
                    devise = viewModel.devise,
                    nomDevise = remember(viewModel.devise, locale) {
                        Iso4217.nomDevise(viewModel.devise, locale)
                    },
                    typeTaxe = typeTaxePays,
                    tauxTaxe = viewModel.tauxTaxeTexte,
                    pack = ReferentielPackPays.pack(viewModel.codePays),
                    indicatif = indicatif,
                    zone = zoneFiscale,
                    identifiants = reglesIdentifiants.map { it.libelle },
                ) {
                    MissaSelecteurLigne(
                        label = stringResource(R.string.obn_devise_principale),
                        options = optionsDevise,
                        selectionCle = viewModel.devise,
                        onSelection = { code -> viewModel.devise = code },
                        enabled = !viewModel.enregistrementEnCours,
                    )
                    HorizontalDivider(color = MissaBorder)
                    Spacer(Modifier.height(12.dp))
                    OnbChampsEmpiles {
                        OnbChampTexte(
                            valeur = viewModel.tauxTaxeTexte,
                            onValeur = viewModel::modifierTauxTaxe,
                            label = stringResource(R.string.ob_taux_taxe),
                            icone = Icons.Outlined.Percent,
                            clavier = KeyboardType.Decimal,
                            erreur = tauxTaxeInvalide,
                            aide = stringResource(R.string.ob_erreur_taux_taxe)
                                .takeIf { tauxTaxeInvalide },
                            active = !viewModel.enregistrementEnCours,
                        )
                        OnbChampTexte(
                            valeur = viewModel.pays,
                            onValeur = viewModel::modifierPaysManuel,
                            label = stringResource(R.string.ob_pays_personnalise),
                            icone = Icons.Outlined.Public,
                            aide = stringResource(R.string.ob_pays_saisie_manuelle_note),
                            active = !viewModel.enregistrementEnCours,
                        )
                    }
                }
            }

            // --- 3. Coordonnées : reprises sur les documents commerciaux ---
            MissaSectionPliable(
                titre = stringResource(R.string.obn_section_coordonnees),
                icone = Icons.Outlined.Call,
                resume = resume(viewModel.telephone, viewModel.email, viewModel.adresse)
                    ?: stringResource(R.string.obn_entreprise_contact_sous),
                etiquette = if (emailValide) facultatif else aCompleter,
                etiquetteEnErreur = !emailValide,
                // Le bouton « Suivant » est bloqué par un e-mail mal formé :
                // la section s'ouvre d'elle-même pour que la cause soit visible.
                ouvrirDOffice = !emailValide,
            ) {
                OnbChampsEmpiles {
                    OnbChampTexte(
                        valeur = viewModel.telephone,
                        onValeur = { viewModel.telephone = it },
                        label = stringResource(R.string.obn_telephone),
                        icone = Icons.Outlined.Call,
                        placeholder = indicatif,
                        clavier = KeyboardType.Phone,
                        active = !viewModel.enregistrementEnCours,
                    )
                    OnbChampTexte(
                        valeur = viewModel.email,
                        onValeur = { viewModel.email = it },
                        label = stringResource(R.string.obn_email),
                        icone = Icons.Outlined.MailOutline,
                        placeholder = stringResource(R.string.obn_email_ex),
                        clavier = KeyboardType.Email,
                        erreur = !emailValide,
                        aide = stringResource(R.string.obn_erreur_email_entreprise)
                            .takeIf { !emailValide },
                        active = !viewModel.enregistrementEnCours,
                    )
                    OnbChampTexte(
                        valeur = viewModel.adresse,
                        onValeur = { viewModel.adresse = it },
                        label = stringResource(R.string.obn_adresse),
                        icone = Icons.Outlined.Place,
                        placeholder = stringResource(R.string.obn_adresse_ex),
                        lignesMin = 2,
                        active = !viewModel.enregistrementEnCours,
                    )
                }
            }

            // --- 4. Identifiants légaux : ceux qu'attend le pays choisi ---
            if (reglesIdentifiants.isNotEmpty()) {
                val saisis = reglesIdentifiants.mapNotNull { regle ->
                    when (regle.cle) {
                        CleIdentifiant.FISCAL -> viewModel.numeroFiscal
                        CleIdentifiant.REGISTRE -> viewModel.registreCommerce
                    }.takeIf { it.isNotBlank() }
                }
                val formatsIncorrects = reglesIdentifiants.any { regle ->
                    !regle.estValide(
                        when (regle.cle) {
                            CleIdentifiant.FISCAL -> viewModel.numeroFiscal
                            CleIdentifiant.REGISTRE -> viewModel.registreCommerce
                        },
                    )
                }
                MissaSectionPliable(
                    titre = stringResource(R.string.fisc_pack_identifiants),
                    icone = Icons.Outlined.Badge,
                    resume = saisis.takeIf { it.isNotEmpty() }?.joinToString(" · ")
                        ?: reglesIdentifiants.joinToString(" · ") { it.libelle },
                    etiquette = if (formatsIncorrects) {
                        stringResource(R.string.obn_section_format)
                    } else {
                        facultatif
                    },
                    etiquetteEnErreur = formatsIncorrects,
                ) {
                    OnbChampsEmpiles {
                        reglesIdentifiants.forEach { regle ->
                            val valeur = when (regle.cle) {
                                CleIdentifiant.FISCAL -> viewModel.numeroFiscal
                                CleIdentifiant.REGISTRE -> viewModel.registreCommerce
                            }
                            val formatIncorrect = !regle.estValide(valeur)
                            OnbChampTexte(
                                valeur = valeur,
                                onValeur = { saisie ->
                                    when (regle.cle) {
                                        CleIdentifiant.FISCAL -> viewModel.numeroFiscal = saisie
                                        CleIdentifiant.REGISTRE ->
                                            viewModel.registreCommerce = saisie
                                    }
                                },
                                label = regle.libelle,
                                icone = when (regle.cle) {
                                    CleIdentifiant.FISCAL -> Icons.Outlined.Badge
                                    CleIdentifiant.REGISTRE -> Icons.Outlined.Gavel
                                },
                                placeholder = regle.exemple.takeIf { it.isNotEmpty() },
                                erreur = formatIncorrect,
                                aide = if (formatIncorrect && regle.exemple.isNotEmpty()) {
                                    stringResource(R.string.fisc_format_attendu, regle.exemple)
                                } else {
                                    null
                                },
                                aideNeutre = true,
                                active = !viewModel.enregistrementEnCours,
                            )
                        }
                    }
                }
            }

            // --- 5. Logo : une ligne, pas une zone de dépôt de 150 points ---
            OnbLogoSection(
                logoUri = viewModel.logoUri,
                enabled = !viewModel.enregistrementEnCours,
                etiquette = facultatif,
                onLogoSelected = viewModel::definirLogoUri,
                onLogoCleared = { viewModel.definirLogoUri(null) },
            )
        }
    }
}

/** Résumé d'en-tête : les valeurs renseignées, séparées par des points médians. */
private fun resume(vararg valeurs: String?): String? =
    valeurs.filter { !it.isNullOrBlank() }.joinToString(" · ").ifBlank { null }

/** Espacement commun à toutes les piles de champs de l'écran. */
@Composable
private fun OnbChampsEmpiles(contenu: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(9.dp),
        content = contenu,
    )
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
    icone: ImageVector,
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
                imageVector = icone,
                contentDescription = null,
                tint = if (erreur) Red40 else BrandBlue,
                modifier = Modifier.size(18.dp),
            )
        },
        placeholder = placeholder?.let { { Text(it, fontSize = 13.sp, color = MissaMuted) } },
        singleLine = lignesMin == 1,
        minLines = lignesMin,
        isError = erreur,
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

/**
 * Le « pack pays » : tout ce que le choix du pays a rempli d'office.
 *
 * Les quatre valeurs qui gouvernent la facturation — devise, taxe, zone
 * comptable, identifiants attendus — sont présentées en grille de deux
 * colonnes, immédiatement lisibles. Le reste (taux réduits, seuil, impôts,
 * facturation électronique) est de la documentation : il attend derrière
 * « Détail fiscal ». Un second lien, « modifier manuellement », déplie
 * [personnalisation].
 */
@Composable
private fun OnbPackPays(
    pays: String,
    devise: String,
    nomDevise: String,
    typeTaxe: TypeTaxe?,
    tauxTaxe: String,
    pack: PackPays?,
    indicatif: String?,
    zone: ZoneFiscale,
    identifiants: List<String>,
    personnalisation: @Composable ColumnScope.() -> Unit,
) {
    var detailOuvert by rememberSaveable { mutableStateOf(false) }
    var personnaliser by rememberSaveable { mutableStateOf(false) }
    val libelleZone = stringResource(zone.libelleRes)
    // Le taux saisi prime sur le taux catalogue : c'est lui qui sera enregistré.
    val valeurTaxe = when {
        typeTaxe == null -> stringResource(R.string.fisc_taux_a_renseigner)
        typeTaxe == TypeTaxe.AUCUNE -> stringResource(typeTaxe.libelleRes)
        tauxTaxe.isBlank() -> stringResource(typeTaxe.libelleRes)
        else -> "$tauxTaxe % · " + stringResource(typeTaxe.libelleRes)
    }
    Surface(
        color = MissaSoftBlue,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, BrandBlue),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Public,
                    contentDescription = null,
                    tint = BrandBlue,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    text = stringResource(R.string.fisc_pack_titre),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MissaInk,
                    modifier = Modifier.weight(1f),
                )
                if (pays.isNotBlank()) {
                    Surface(color = MissaSurface, shape = RoundedCornerShape(6.dp)) {
                        Text(
                            text = pays,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = BrandBlue,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                        )
                    }
                }
            }
            if (pays.isBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text = stringResource(R.string.fisc_pack_aucun_pays),
                    fontSize = 11.sp,
                    color = MissaMuted,
                )
            } else {
                Spacer(Modifier.height(8.dp))
                // Grille 2 × 2 : l'essentiel du pack en quatre cellules.
                OnbPackRangee(
                    gauche = R.string.obn_devise_principale to "$devise · $nomDevise",
                    droite = R.string.fisc_pack_taxe to valeurTaxe,
                )
                OnbPackRangee(
                    gauche = R.string.fisc_zone_label to (
                        zone.referentielComptable?.let { "$libelleZone · $it" } ?: libelleZone
                        ),
                    droite = identifiants.takeIf { it.isNotEmpty() }?.let {
                        R.string.fisc_pack_identifiants to it.joinToString(" · ")
                    },
                )
                AnimatedVisibility(visible = detailOuvert) {
                    Column {
                        pack?.let { detail ->
                            OnbPackRangee(
                                gauche = detail.tauxReduits.takeIf { it.isNotEmpty() }?.let {
                                    R.string.fisc_pack_taux_reduits to
                                        it.joinToString(" · ", transform = Iso4217::formatPourcentage)
                                },
                                droite = R.string.fisc_pack_seuil to (
                                    detail.seuilAssujettissement
                                        ?.let { MoneyUtils.format(it.toDouble(), devise) }
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (pays.isNotBlank() && pack != null) {
                    OnbPackLien(
                        texteRes = R.string.fisc_pack_detail,
                        ouvert = detailOuvert,
                        onClic = { detailOuvert = !detailOuvert },
                    )
                    Spacer(Modifier.width(4.dp))
                }
                OnbPackLien(
                    texteRes = R.string.fisc_pack_personnaliser,
                    ouvert = personnaliser,
                    onClic = { personnaliser = !personnaliser },
                )
            }
            AnimatedVisibility(visible = personnaliser) {
                Surface(
                    color = MissaSurface,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MissaBorder),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                        Text(
                            text = stringResource(R.string.fisc_pack_personnaliser_note),
                            fontSize = 11.sp,
                            color = MissaMuted,
                        )
                        personnalisation()
                    }
                }
            }
        }
    }
}

/** Lien du pack : chevron + libellé, compact, deux par rangée. */
@Composable
private fun OnbPackLien(texteRes: Int, ouvert: Boolean, onClic: () -> Unit) {
    TextButton(
        onClick = onClic,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = 6.dp,
            vertical = 2.dp,
        ),
    ) {
        Icon(
            imageVector = if (ouvert) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
            contentDescription = null,
            modifier = Modifier.size(15.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(text = stringResource(texteRes), fontSize = 11.5.sp)
    }
}

/**
 * Rangée de deux cellules du pack. La cellule de droite peut manquer : la
 * gauche garde alors sa demi-largeur, pour que les colonnes restent alignées
 * d'une rangée à l'autre.
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

/** Cellule « libellé au-dessus, valeur en dessous » du pack pays. */
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
 * Section logo : vignette, formats acceptés et actions sur une seule ligne.
 *
 * Le cadre de dépôt en pointillés de la maquette web n'a pas de sens sur
 * téléphone — on n'y fait pas glisser de fichier — mais il en garde la trace
 * visuelle quand aucun logo n'est encore choisi.
 */
@Composable
private fun OnbLogoSection(
    logoUri: String?,
    enabled: Boolean,
    etiquette: String,
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

    MissaSectionPliable(
        titre = stringResource(R.string.obn_logo_titre),
        icone = Icons.Outlined.Image,
        resume = if (logoUri == null) {
            stringResource(R.string.obn_logo_formats)
        } else {
            stringResource(R.string.obn_logo_present)
        },
        etiquette = etiquette,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (logoUri == null) {
                Surface(
                    modifier = Modifier.size(56.dp).dashedBorder(1.2.dp, MissaBorder, 12.dp),
                    color = BrandBlue.copy(alpha = 0.025f),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Backup,
                        contentDescription = null,
                        tint = BrandBlue,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            } else {
                CompanyLogo(
                    logoUri = logoUri,
                    contentDescription = stringResource(R.string.ob_logo_apercu),
                    fallbackIcon = Icons.Outlined.Backup,
                    modifier = Modifier.size(56.dp),
                    size = 56.dp,
                    shape = RoundedCornerShape(12.dp),
                    fallbackTint = BrandBlue,
                    fallbackBackground = BrandBlue.copy(alpha = 0.07f),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.obn_logo_description),
                    fontSize = 11.5.sp,
                    color = MissaMuted,
                )
                if (logoTropGrand) {
                    Text(
                        text = stringResource(R.string.obn_logo_trop_grand),
                        fontSize = 11.sp,
                        color = Red40,
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(
                onClick = { launcher.launch(IMAGE_MIME_TYPES) },
                enabled = enabled,
                shape = RoundedCornerShape(9.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 12.dp,
                    vertical = 6.dp,
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
                    fontSize = 12.5.sp,
                )
            }
            if (logoUri != null) {
                IconButton(onClick = onLogoCleared, enabled = enabled) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = stringResource(R.string.ob_logo_supprimer),
                        tint = Red40,
                        modifier = Modifier.size(19.dp),
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
