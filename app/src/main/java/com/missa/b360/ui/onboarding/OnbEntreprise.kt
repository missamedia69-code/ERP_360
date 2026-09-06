package com.missa.b360.ui.onboarding

import android.content.Intent
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.Percent
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.missa.b360.R
import com.missa.b360.core.domain.model.CleIdentifiant
import com.missa.b360.core.domain.model.ReferentielFiscal
import com.missa.b360.core.domain.model.ZoneFiscale
import com.missa.b360.core.util.Iso4217
import com.missa.b360.ui.components.CompanyLogo
import com.missa.b360.ui.components.MissaOption
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
 * Écran 4 — Informations sur votre entreprise : identité, localisation, devise,
 * coordonnées et identifiants légaux (NIU / RCCM), logo, site principal et taux
 * de taxe.
 *
 * Pays et devise passent par le sélecteur standard [MissaSelecteurLigne]
 * (recherche insensible aux accents, catalogue détaillé) ; site principal et
 * taux de taxe sont visibles directement, sans « Plus de détails » à déplier.
 */
@Composable
internal fun OnbEntrepriseStep(viewModel: OnboardingViewModel) {
    var siteModifieManuellement by remember { mutableStateOf(viewModel.nomSitePrincipal.isNotBlank()) }
    var saisiePaysManuelle by rememberSaveable { mutableStateOf(false) }
    val locale = LocalConfiguration.current.locales[0]
    val paysListe = remember(locale) { Iso4217.paysDisponibles(locale) }
    // Devise officielle de chaque pays : calculée une fois, réutilisée par la liste
    // des pays (pastille de droite) et par le pack appliqué à la sélection.
    val devisesParPays = remember(locale) {
        paysListe.associate { pays -> pays.code to Iso4217.deviseDuPays(pays.code) }
    }
    val optionsPays = paysListe.map { pays ->
        MissaOption(
            cle = pays.code,
            titre = pays.nom,
            // Le taux n'est affiché qu'une fois : le libellé le porte déjà.
            sousTitre = pays.libelleTaxe,
            badge = pays.code,
            badgeSecondaire = devisesParPays[pays.code],
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
    val tauxTaxeInvalide = !viewModel.tauxTaxeEstValide()
    val emailValide = viewModel.emailEntrepriseEstValide()
    val zoneFiscale = ReferentielFiscal.zone(viewModel.codePays)
    val reglesIdentifiants = ReferentielFiscal.regles(
        codePays = viewModel.codePays,
        libelleFiscalGenerique = stringResource(R.string.fisc_id_fiscal),
        libelleRegistreGenerique = stringResource(R.string.fisc_id_registre),
    )

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
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // --- Identité de l'entreprise ---
            OnbEntrepriseCarte {
                OnbChamp(
                    icone = Icons.Outlined.Business,
                    labelRes = R.string.ob_nom_entreprise,
                ) {
                    OutlinedTextField(
                        value = viewModel.nomEntreprise,
                        onValueChange = { nom ->
                            val ancienNom = viewModel.nomEntreprise
                            viewModel.nomEntreprise = nom
                            if (!siteModifieManuellement || viewModel.nomSitePrincipal == ancienNom) {
                                viewModel.nomSitePrincipal = nom
                            }
                        },
                        placeholder = { Text(stringResource(R.string.obn_nom_ex)) },
                        singleLine = true,
                        enabled = !viewModel.enregistrementEnCours,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                OnbChamp(
                    icone = Icons.Outlined.Category,
                    labelRes = R.string.obn_secteur,
                ) {
                    OutlinedTextField(
                        value = viewModel.secteur,
                        onValueChange = { viewModel.secteur = it },
                        placeholder = { Text(stringResource(R.string.obn_secteur_ex)) },
                        singleLine = true,
                        enabled = !viewModel.enregistrementEnCours,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            // --- Localisation et devise (sélecteur standard) ---
            OnbEntrepriseSection(titreRes = R.string.obn_entreprise_localisation)
            Card(
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MissaBorder),
                colors = CardDefaults.cardColors(containerColor = MissaSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    MissaSelecteurLigne(
                        label = stringResource(R.string.ob_pays),
                        options = optionsPays,
                        selectionCle = viewModel.codePays,
                        onSelection = { code ->
                            paysListe.firstOrNull { it.code == code }?.let { pays ->
                                saisiePaysManuelle = false
                                viewModel.choisirPays(pays.nom, pays.code, pays.tauxTaxeSuggere)
                            }
                        },
                        enabled = !viewModel.enregistrementEnCours,
                        placeholder = viewModel.pays.ifBlank {
                            stringResource(R.string.ob_selectionne)
                        },
                    )
                    HorizontalDivider(color = MissaBorder)
                    MissaSelecteurLigne(
                        label = stringResource(R.string.obn_devise_principale),
                        options = optionsDevise,
                        selectionCle = viewModel.devise,
                        onSelection = { code -> viewModel.devise = code },
                        enabled = !viewModel.enregistrementEnCours,
                    )
                    TextButton(
                        onClick = { saisiePaysManuelle = !saisiePaysManuelle },
                        modifier = Modifier.align(Alignment.Start),
                    ) {
                        Text(stringResource(R.string.ob_pays_saisie_manuelle), fontSize = 12.5.sp)
                    }
                    if (saisiePaysManuelle) {
                        Text(
                            text = stringResource(R.string.ob_pays_saisie_manuelle_note),
                            fontSize = 11.5.sp,
                            color = MissaMuted,
                        )
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(
                            value = viewModel.pays,
                            onValueChange = viewModel::modifierPaysManuel,
                            label = { Text(stringResource(R.string.ob_pays_personnalise)) },
                            singleLine = true,
                            enabled = !viewModel.enregistrementEnCours,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }

            // --- Pack appliqué par le pays ---
            OnbPackPays(
                pays = viewModel.pays,
                devise = viewModel.devise,
                nomDevise = remember(viewModel.devise, locale) {
                    Iso4217.nomDevise(viewModel.devise, locale)
                },
                libelleTaxe = paysListe.firstOrNull { it.code == viewModel.codePays }?.libelleTaxe,
                indicatif = Iso4217.indicatifTelephone(viewModel.codePays),
                zone = zoneFiscale,
                identifiants = reglesIdentifiants.map { it.libelle },
            )

            // --- Coordonnées et identifiants légaux ---
            OnbEntrepriseSection(
                titreRes = R.string.obn_entreprise_contact,
                sousTitreRes = R.string.obn_entreprise_contact_sous,
            )
            OnbEntrepriseCarte {
                OnbChamp(icone = Icons.Outlined.Call, labelRes = R.string.obn_telephone) {
                    OutlinedTextField(
                        value = viewModel.telephone,
                        onValueChange = { viewModel.telephone = it },
                        placeholder = { Text(stringResource(R.string.obn_telephone_ex)) },
                        singleLine = true,
                        enabled = !viewModel.enregistrementEnCours,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                OnbChamp(icone = Icons.Outlined.MailOutline, labelRes = R.string.obn_email) {
                    OutlinedTextField(
                        value = viewModel.email,
                        onValueChange = { viewModel.email = it },
                        placeholder = { Text(stringResource(R.string.obn_email_ex)) },
                        singleLine = true,
                        isError = !emailValide,
                        enabled = !viewModel.enregistrementEnCours,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        supportingText = if (emailValide) {
                            null
                        } else {
                            {
                                Text(
                                    text = stringResource(R.string.obn_erreur_email_entreprise),
                                    color = Red40,
                                )
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                OnbChamp(icone = Icons.Outlined.Place, labelRes = R.string.obn_adresse) {
                    OutlinedTextField(
                        value = viewModel.adresse,
                        onValueChange = { viewModel.adresse = it },
                        placeholder = { Text(stringResource(R.string.obn_adresse_ex)) },
                        singleLine = false,
                        minLines = 2,
                        enabled = !viewModel.enregistrementEnCours,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                reglesIdentifiants.forEach { regle ->
                    val valeur = when (regle.cle) {
                        CleIdentifiant.FISCAL -> viewModel.numeroFiscal
                        CleIdentifiant.REGISTRE -> viewModel.registreCommerce
                    }
                    val formatIncorrect = !regle.estValide(valeur)
                    OnbChamp(
                        icone = when (regle.cle) {
                            CleIdentifiant.FISCAL -> Icons.Outlined.Badge
                            CleIdentifiant.REGISTRE -> Icons.Outlined.Gavel
                        },
                        label = regle.libelle,
                    ) {
                        OutlinedTextField(
                            value = valeur,
                            onValueChange = { saisie ->
                                when (regle.cle) {
                                    CleIdentifiant.FISCAL -> viewModel.numeroFiscal = saisie
                                    CleIdentifiant.REGISTRE -> viewModel.registreCommerce = saisie
                                }
                            },
                            placeholder = {
                                if (regle.exemple.isNotEmpty()) Text(regle.exemple)
                            },
                            singleLine = true,
                            isError = formatIncorrect,
                            enabled = !viewModel.enregistrementEnCours,
                            supportingText = if (formatIncorrect && regle.exemple.isNotEmpty()) {
                                {
                                    Text(
                                        text = stringResource(
                                            R.string.fisc_format_attendu,
                                            regle.exemple,
                                        ),
                                        color = MissaMuted,
                                    )
                                }
                            } else {
                                null
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            // --- Logo de l'entreprise ---
            OnbLogoCard(
                logoUri = viewModel.logoUri,
                enabled = !viewModel.enregistrementEnCours,
                onLogoSelected = viewModel::definirLogoUri,
                onLogoCleared = { viewModel.definirLogoUri(null) },
            )

            // --- Site principal et taxe : visibles d'emblée ---
            OnbEntrepriseSection(titreRes = R.string.obn_entreprise_exploitation)
            OnbEntrepriseCarte {
                OnbChamp(
                    icone = Icons.Outlined.Storefront,
                    labelRes = R.string.ob_site_principal,
                ) {
                    OutlinedTextField(
                        value = viewModel.nomSitePrincipal,
                        onValueChange = {
                            siteModifieManuellement = true
                            viewModel.nomSitePrincipal = it
                        },
                        singleLine = true,
                        enabled = !viewModel.enregistrementEnCours,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                OnbChamp(
                    icone = Icons.Outlined.Percent,
                    labelRes = R.string.ob_taux_taxe,
                ) {
                    OutlinedTextField(
                        value = viewModel.tauxTaxeTexte,
                        onValueChange = viewModel::modifierTauxTaxe,
                        singleLine = true,
                        isError = tauxTaxeInvalide,
                        enabled = !viewModel.enregistrementEnCours,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        supportingText = {
                            if (tauxTaxeInvalide) {
                                Text(stringResource(R.string.ob_erreur_taux_taxe), color = Red40)
                            } else if (viewModel.pays.isNotBlank()) {
                                Text(stringResource(R.string.obn_taux_suggere, viewModel.pays))
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

/**
 * Récapitulatif de ce que le choix du pays a rempli : devise officielle, taxe,
 * indicatif téléphonique, zone fiscale et identifiants légaux attendus.
 * Purement informatif — chaque valeur reste modifiable dans les champs concernés.
 */
@Composable
private fun OnbPackPays(
    pays: String,
    devise: String,
    nomDevise: String,
    libelleTaxe: String?,
    indicatif: String?,
    zone: ZoneFiscale,
    identifiants: List<String>,
) {
    val libelleZone = stringResource(zone.libelleRes)
    Surface(
        color = MissaSoftBlue,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, BrandBlue),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Public,
                    contentDescription = null,
                    tint = BrandBlue,
                    modifier = Modifier.size(17.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.fisc_pack_titre),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MissaInk,
                )
            }
            Spacer(Modifier.height(3.dp))
            Text(
                text = if (pays.isBlank()) {
                    stringResource(R.string.fisc_pack_aucun_pays)
                } else {
                    stringResource(R.string.fisc_pack_note, pays)
                },
                fontSize = 11.sp,
                color = MissaMuted,
            )
            if (pays.isNotBlank()) {
                Spacer(Modifier.height(9.dp))
                OnbPackLigne(
                    labelRes = R.string.obn_devise_principale,
                    valeur = "$devise · $nomDevise",
                )
                libelleTaxe?.let { taxe ->
                    OnbPackLigne(labelRes = R.string.ob_taux_taxe, valeur = taxe)
                }
                indicatif?.let { code ->
                    OnbPackLigne(labelRes = R.string.fisc_pack_indicatif, valeur = code)
                }
                OnbPackLigne(
                    labelRes = R.string.fisc_zone_label,
                    valeur = zone.referentielComptable
                        ?.let { "$libelleZone · $it" }
                        ?: libelleZone,
                )
                if (identifiants.isNotEmpty()) {
                    OnbPackLigne(
                        labelRes = R.string.fisc_pack_identifiants,
                        valeur = identifiants.joinToString(" · "),
                    )
                }
            }
        }
    }
}

/** Ligne « libellé — valeur » du pack pays. */
@Composable
private fun OnbPackLigne(labelRes: Int, valeur: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(top = 3.dp), verticalAlignment = Alignment.Top) {
        Text(
            text = stringResource(labelRes),
            fontSize = 11.5.sp,
            color = MissaMuted,
            modifier = Modifier.width(112.dp),
        )
        Text(
            text = valeur,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = MissaInk,
            modifier = Modifier.weight(1f),
        )
    }
}

/** Intertitre de section de l'écran entreprise, avec explication facultative. */
@Composable
private fun OnbEntrepriseSection(titreRes: Int, sousTitreRes: Int? = null) {
    Column {
        Text(
            text = stringResource(titreRes),
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            color = MissaInk,
        )
        if (sousTitreRes != null) {
            Text(
                text = stringResource(sousTitreRes),
                fontSize = 11.5.sp,
                color = MissaMuted,
            )
        }
    }
}

/** Carte blanche standard regroupant des champs de l'écran entreprise. */
@Composable
private fun OnbEntrepriseCarte(contenu: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MissaBorder),
        colors = CardDefaults.cardColors(containerColor = MissaSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            contenu()
        }
    }
}

/**
 * Bloc champ de la maquette : icône + libellé au-dessus du champ, dans la même carte.
 */
@Composable
private fun OnbChamp(
    icone: ImageVector,
    labelRes: Int,
    content: @Composable () -> Unit,
) {
    OnbChamp(icone = icone, label = stringResource(labelRes), content = content)
}

/** Variante à libellé dynamique (identifiants légaux dépendant du pays). */
@Composable
private fun OnbChamp(
    icone: ImageVector,
    label: String,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = icone,
            contentDescription = null,
            tint = BrandBlue,
            modifier = Modifier
                .padding(top = 15.dp)
                .size(18.dp),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp),
        ) {
            Text(
                text = label,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = MissaInk,
            )
            Spacer(Modifier.height(5.dp))
            content()
        }
    }
}

/**
 * Zone de logo de la maquette : cadre pointillé, nuage de dépôt, bouton
 * « Parcourir », formats acceptés et limite 2MB vérifiée avant acceptation.
 */
@Composable
private fun OnbLogoCard(
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

    Card(
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MissaBorder),
        colors = CardDefaults.cardColors(containerColor = MissaSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.obn_logo_titre),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MissaInk,
            )
            Text(
                text = stringResource(R.string.obn_logo_optionnel),
                fontSize = 12.sp,
                color = MissaMuted,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.obn_logo_description),
                fontSize = 12.5.sp,
                color = MissaMuted,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))

            if (logoUri == null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .dashedBorder(1.2.dp, MissaBorder, 14.dp),
                    color = BrandBlue.copy(alpha = 0.025f),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Backup,
                            contentDescription = null,
                            tint = BrandBlue,
                            modifier = Modifier.size(38.dp),
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.obn_logo_deposer),
                            fontSize = 13.sp,
                            color = MissaInk,
                        )
                        Text(
                            text = stringResource(R.string.obn_logo_ou),
                            fontSize = 12.sp,
                            color = MissaMuted,
                        )
                        Spacer(Modifier.height(6.dp))
                        OutlinedButton(
                            onClick = { launcher.launch(IMAGE_MIME_TYPES) },
                            enabled = enabled,
                            shape = RoundedCornerShape(9.dp),
                        ) {
                            Text(stringResource(R.string.obn_logo_parcourir), fontSize = 13.sp)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.obn_logo_formats),
                    fontSize = 11.sp,
                    color = MissaMuted,
                    textAlign = TextAlign.Center,
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CompanyLogo(
                        logoUri = logoUri,
                        contentDescription = stringResource(R.string.ob_logo_apercu),
                        fallbackIcon = Icons.Outlined.Backup,
                        modifier = Modifier.size(84.dp),
                        size = 72.dp,
                        shape = RoundedCornerShape(14.dp),
                        fallbackTint = BrandBlue,
                        fallbackBackground = BrandBlue.copy(alpha = 0.07f),
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        OutlinedButton(
                            onClick = { launcher.launch(IMAGE_MIME_TYPES) },
                            enabled = enabled,
                            shape = RoundedCornerShape(9.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(stringResource(R.string.ob_logo_modifier), fontSize = 13.sp)
                        }
                        TextButton(onClick = onLogoCleared, enabled = enabled) {
                            Text(stringResource(R.string.ob_logo_supprimer), fontSize = 13.sp, color = Red40)
                        }
                    }
                }
            }
            if (logoTropGrand) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.obn_logo_trop_grand),
                    fontSize = 12.sp,
                    color = Red40,
                )
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
