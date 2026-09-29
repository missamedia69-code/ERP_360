package com.missa.b360.ui.comptabilite

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.missa.b360.R
import com.missa.b360.core.util.Iso4217
import com.missa.b360.core.domain.model.EcritureComptable
import com.missa.b360.core.domain.model.RubriqueComptable
import com.missa.b360.core.data.entity.AccountingAccountEntity
import com.missa.b360.core.data.entity.AccountingVoucherEntity
import com.missa.b360.core.data.entity.AccountingVoucherStatus
import com.missa.b360.ui.components.MissaEmptyState
import com.missa.b360.ui.components.MissaTopAppBar
import com.missa.b360.ui.icons.Iv
import com.missa.b360.ui.navigation.AppModule
import com.missa.b360.ui.stock.fmtValeur
import com.missa.b360.ui.theme.MissaBorder
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.missa.b360.ui.components.*

/** Palette ardoise du module Comptabilité — source unique : [AppModule.COMPTABILITE]. */
private val GrisCompta: Color get() = AppModule.COMPTABILITE.couleur

@Composable
fun ComptabiliteScreen(
    onBack: () -> Unit,
    onNaviguer: (String) -> Unit = {},
    vm: ComptabiliteViewModel = hiltViewModel(),
) {
    val synthese by vm.synthese.collectAsStateWithLifecycle()
    val devise by vm.devise.collectAsStateWithLifecycle()
    val periode by vm.periode.collectAsStateWithLifecycle()
    val registre by vm.registre.collectAsStateWithLifecycle()
    val feedback by vm.feedback.collectAsStateWithLifecycle()
    var showCreateVoucher by remember { mutableStateOf(false) }
    var showProfileConfiguration by remember { mutableStateOf(false) }

    val res = synthese.resultat
    val resultatNet = res.resultat
    val estBenefice = resultatNet >= 0

    Column(Modifier.fillMaxSize()) {
        MissaTopAppBar(
            title = stringResource(R.string.module_comptabilite),
            onBack = onBack,
            couleurFond = AppModule.COMPTABILITE.couleurPale,
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // --- Filtre Période ---
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = periode == ComptabiliteViewModel.Periode.MOIS,
                        onClick = { vm.choisirPeriode(ComptabiliteViewModel.Periode.MOIS) },
                        label = { Text(stringResource(R.string.cpt_periode_mois_courant), fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GrisCompta.copy(alpha = 0.2f),
                        ),
                    )
                    FilterChip(
                        selected = periode == ComptabiliteViewModel.Periode.MOIS_PRECEDENT,
                        onClick = { vm.choisirPeriode(ComptabiliteViewModel.Periode.MOIS_PRECEDENT) },
                        label = { Text(stringResource(R.string.cpt_periode_mois_precedent), fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GrisCompta.copy(alpha = 0.2f),
                        ),
                    )
                    FilterChip(
                        selected = periode == ComptabiliteViewModel.Periode.ANNEE,
                        onClick = { vm.choisirPeriode(ComptabiliteViewModel.Periode.ANNEE) },
                        label = { Text(stringResource(R.string.cpt_periode_annee), fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GrisCompta.copy(alpha = 0.2f),
                        ),
                    )
                }
            }

            item {
                Surface(shape = RoundedCornerShape(12.dp), color = GrisCompta.copy(alpha = 0.08f)) {
                    Text(
                        stringResource(R.string.cpt_avertissement_estimations),
                        modifier = Modifier.padding(12.dp), fontSize = 11.sp, color = MissaMuted,
                    )
                }
            }

            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.cpt_registre_titre), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MissaInk)
                        Text(stringResource(R.string.cpt_registre_sous_titre), fontSize = 11.sp, color = MissaMuted)
                    }
                    if (registre.accounts.isNotEmpty()) {
                        Button(onClick = { showCreateVoucher = true }) { Text(stringResource(R.string.cpt_nouvelle_od)) }
                    }
                }
            }

            if (feedback != null) {
                item {
                    Surface(shape = RoundedCornerShape(10.dp), color = GrisCompta.copy(alpha = 0.08f)) {
                        Row(Modifier.fillMaxWidth().padding(start = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(feedback.orEmpty(), modifier = Modifier.weight(1f).padding(vertical = 9.dp), fontSize = 11.sp, color = MissaInk)
                            TextButton(onClick = vm::effacerFeedback) { Text("OK") }
                        }
                    }
                }
            }

            if (registre.accounts.isEmpty()) {
                item {
                    Surface(shape = RoundedCornerShape(14.dp), color = Color.White, border = BorderStroke(1.dp, MissaBorder)) {
                        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(stringResource(R.string.cpt_aucun_plan), fontWeight = FontWeight.SemiBold, color = MissaInk)
                            Text(stringResource(R.string.cpt_aucun_plan_desc), fontSize = 11.sp, color = MissaMuted)
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Button(onClick = { showProfileConfiguration = true }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.cpt_configurer_profil)) }
                                Button(onClick = { vm.initialiserPlan() }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.cpt_initialiser_socle)) }
                            }
                        }
                    }
                }
            } else {
                if (registre.vouchers.isEmpty()) {
                    item { Text(stringResource(R.string.cpt_aucune_piece), fontSize = 12.sp, color = MissaMuted) }
                } else {
                    items(registre.vouchers, key = { "ledger-${it.id}" }) { voucher ->
                        CartePieceComptable(voucher, devise, onPost = { vm.comptabiliser(voucher.id) })
                    }
                }
            }

            // --- Carte Compte de Résultat Simplifié ---
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = GrisCompta.copy(alpha = 0.16f),
                ) {
                    Column(Modifier.fillMaxWidth().padding(14.dp)) {
                        Text(
                            stringResource(R.string.cpt_resultat_net),
                            fontSize = 11.sp,
                            color = MissaMuted,
                        )
                        Spacer(Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                fmtValeur(resultatNet, devise),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (estBenefice) Color(0xFF15803D) else Color(0xFFB91C1C),
                            )
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (estBenefice) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                            ) {
                                Text(
                                    "${String.format(Locale.getDefault(), "%.1f", res.marge)} %",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (estBenefice) Color(0xFF15803D) else Color(0xFFB91C1C),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(stringResource(R.string.cpt_total_produits), fontSize = 10.sp, color = MissaMuted)
                                Text(
                                    fmtValeur(res.totalProduits, devise),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF15803D),
                                )
                            }
                            Column {
                                Text(stringResource(R.string.cpt_total_charges), fontSize = 10.sp, color = MissaMuted)
                                Text(
                                    fmtValeur(res.totalCharges, devise),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFB91C1C),
                                )
                            }
                        }
                    }
                }
            }

            // --- Position Fiscale TVA ---
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, MissaBorder),
                ) {
                    Column(Modifier.fillMaxWidth().padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                stringResource(R.string.cpt_position_tva),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MissaInk,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                stringResource(R.string.cpt_taux_x, synthese.tauxTva.toString()),
                                fontSize = 11.sp,
                                color = MissaMuted,
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(stringResource(R.string.cpt_tva_collectee), fontSize = 10.sp, color = MissaMuted)
                                Text(fmtValeur(synthese.tvaCollectee, devise), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MissaInk)
                            }
                            Column {
                                Text(stringResource(R.string.cpt_tva_deductible), fontSize = 10.sp, color = MissaMuted)
                                Text(fmtValeur(synthese.tvaDeductible, devise), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MissaInk)
                            }
                            Column {
                                Text(
                                    stringResource(if (synthese.tvaAPayer >= 0) R.string.cpt_tva_due else R.string.cpt_tva_credit),
                                    fontSize = 10.sp,
                                    color = MissaMuted,
                                )
                                Text(
                                    fmtValeur(synthese.tvaAPayer, devise),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (synthese.tvaAPayer >= 0) Color(0xFFB91C1C) else Color(0xFF15803D),
                                )
                            }
                        }
                    }
                }
            }

            // --- Journal Général des Écritures ---
            item {
                Text(
                    stringResource(R.string.cpt_journal_general),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MissaInk,
                )
            }

            if (synthese.journal.isEmpty()) {
                item {
                    MissaEmptyState(
                        icon = Iv.Calculator,
                        title = stringResource(R.string.cpt_aucun_journal),
                        description = stringResource(R.string.cpt_aucun_journal_desc),
                        modifier = Modifier.padding(16.dp),
                    )
                }
            } else {
                items(synthese.journal, key = { "${it.reference}-${it.date}" }) { ecriture ->
                    CarteEcriture(ecriture = ecriture, devise = devise)
                }
            }
        }
    }

    if (showCreateVoucher && registre.accounts.isNotEmpty()) {
        NouvelleEcritureDialog(
            accounts = registre.accounts,
            devise = devise,
            onDismiss = { showCreateVoucher = false },
            onCreate = { description, debit, credit, amount ->
                vm.creerEcriture(description, debit, credit, amount, devise)
                showCreateVoucher = false
            },
        )
    }
    if (showProfileConfiguration && registre.accounts.isEmpty()) {
        AccountingProfileDialog(
            countryInitial = registre.settings?.countryCode ?: "CM",
            regimeInitial = registre.settings?.taxRegime.orEmpty(),
            exerciceInitial = registre.settings?.exerciceStartMonth ?: 1,
            onDismiss = { showProfileConfiguration = false },
            onSave = { country, regime, month ->
                vm.configurerProfil(country, regime, month)
                showProfileConfiguration = false
            },
        )
    }
}

@Composable
private fun AccountingProfileDialog(
    countryInitial: String,
    regimeInitial: String,
    exerciceInitial: Int,
    onDismiss: () -> Unit,
    onSave: (String, String, Int) -> Unit,
) {
    var country by remember(countryInitial) { mutableStateOf(countryInitial) }
    var regime by remember(regimeInitial) { mutableStateOf(regimeInitial) }
    var month by remember(exerciceInitial) { mutableStateOf(exerciceInitial.coerceIn(1, 12)) }
    val locale = Locale.getDefault()
    val pays = remember(locale) { Iso4217.paysDisponibles(locale) }
    // Noms de mois fournis par le système, donc déjà traduits dans la langue active.
    val mois = remember(locale) {
        java.text.DateFormatSymbols.getInstance(locale).months.take(12)
            .mapIndexed { i, nom -> (i + 1) to nom.replaceFirstChar { it.titlecase(locale) } }
    }

    MissaFormDialogue(
        titre = stringResource(R.string.cpt_profil_titre),
        sousTitre = stringResource(R.string.cpt_profil_referentiel),
        icone = Iv.Calculator,
        couleur = AppModule.COMPTABILITE.couleur,
        onFermer = onDismiss,
        libelleValider = stringResource(R.string.ops_save),
        validerActif = country.length == 2,
        onValider = { onSave(country, regime.trim(), month) },
    ) {
        MissaFormSection(titre = stringResource(R.string.form_section_fiscalite), numero = 1) {
            MissaRangee {
                MissaChampListe(
                    libelle = stringResource(R.string.cpt_profil_pays),
                    options = pays.map { it.code to it.nom },
                    selection = country,
                    onSelection = { country = it },
                    icone = Iv.Public,
                    requis = true,
                    modifier = Modifier.weight(1f),
                )
                MissaChampTexte(regime, { regime = it }, stringResource(R.string.cpt_profil_regime), icone = Iv.Gavel, longueurMax = 80, modifier = Modifier.weight(1f))
            }
        }
        MissaFormSection(titre = stringResource(R.string.form_section_periode), numero = 2) {
            MissaChampListe(
                libelle = stringResource(R.string.cpt_profil_mois),
                options = mois,
                selection = month,
                onSelection = { month = it },
                icone = Iv.Calendar,
                requis = true,
                aide = stringResource(R.string.cpt_profil_avertissement),
            )
        }
    }
}

@Composable
private fun NouvelleEcritureDialog(
    accounts: List<AccountingAccountEntity>,
    devise: String,
    onDismiss: () -> Unit,
    onCreate: (String, Long, Long, Double) -> Unit,
) {
    var description by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var debitId by remember(accounts) { mutableStateOf(accounts.firstOrNull { it.postable && it.active }?.id ?: 0L) }
    var creditId by remember(accounts) { mutableStateOf(accounts.firstOrNull { it.postable && it.active && it.id != debitId }?.id ?: 0L) }
    val comptes = accounts.filter { it.active && it.postable }.map { it.id to "${it.code} — ${it.name}" }
    val montant = amountText.toDoubleOrNull() ?: 0.0
    val comptesDistincts = debitId > 0 && creditId > 0 && debitId != creditId

    MissaFormDialogue(
        titre = stringResource(R.string.cpt_od_titre),
        sousTitre = stringResource(R.string.cpt_od_brouillon_note),
        icone = Iv.Description,
        couleur = AppModule.COMPTABILITE.couleur,
        onFermer = onDismiss,
        libelleValider = stringResource(R.string.cpt_od_creer),
        validerActif = description.isNotBlank() && montant > 0.0 && comptesDistincts,
        onValider = { onCreate(description.trim(), debitId, creditId, montant) },
    ) {
        MissaFormSection(titre = stringResource(R.string.form_section_details), numero = 1) {
            MissaRangee {
                MissaChampTexte(description, { description = it }, stringResource(R.string.cpt_od_libelle), icone = Iv.Description, requis = true, modifier = Modifier.weight(1f))
                MissaChampTexte(amountText, { amountText = it }, stringResource(R.string.cpt_od_montant), icone = Iv.Payments, clavier = MissaClavier.DECIMAL, requis = true, suffixe = devise, modifier = Modifier.weight(1f))
            }
        }
        MissaFormSection(titre = stringResource(R.string.form_section_comptes), numero = 2) {
            MissaRangee {
                MissaChampListe(
                    libelle = stringResource(R.string.cpt_od_debit),
                    options = comptes,
                    selection = debitId.takeIf { it > 0 },
                    onSelection = { debitId = it },
                    icone = Iv.TrendingUp,
                    requis = true,
                    placeholder = stringResource(R.string.cpt_od_choisir_compte),
                    modifier = Modifier.weight(1f),
                )
                MissaChampListe(
                    libelle = stringResource(R.string.cpt_od_credit),
                    options = comptes,
                    selection = creditId.takeIf { it > 0 },
                    onSelection = { creditId = it },
                    icone = Iv.TrendingDown,
                    requis = true,
                    placeholder = stringResource(R.string.cpt_od_choisir_compte),
                    erreur = if (debitId > 0 && debitId == creditId) stringResource(R.string.cpt_od_erreur_meme_compte) else null,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun CartePieceComptable(voucher: AccountingVoucherEntity, devise: String, onPost: () -> Unit) {
    val dateLabel = remember(voucher.accountingDate) { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(voucher.accountingDate)) }
    val posted = voucher.status == AccountingVoucherStatus.POSTED.name || voucher.status == AccountingVoucherStatus.REVERSED.name
    Surface(shape = RoundedCornerShape(12.dp), color = Color.White, border = BorderStroke(1.dp, MissaBorder)) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(voucher.reference, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MissaInk)
                Text(stringResource(if (posted) R.string.cpt_statut_comptabilisee else R.string.cpt_statut_brouillon), fontSize = 10.sp, color = if (posted) Color(0xFF15803D) else Color(0xFFB45309))
            }
            Text(voucher.description, fontSize = 12.sp, color = MissaInk)
            Text(stringResource(R.string.cpt_piece_ligne, dateLabel, voucher.sourceModule ?: "—", fmtValeur(voucher.totalDebit, devise), fmtValeur(voucher.totalCredit, devise)), fontSize = 10.sp, color = MissaMuted)
            if (voucher.status == AccountingVoucherStatus.DRAFT.name || voucher.status == AccountingVoucherStatus.TO_VALIDATE.name) {
                Button(onClick = onPost) { Text(stringResource(R.string.cpt_valider_comptabiliser)) }
            }
        }
    }
}

@Composable
private fun CarteEcriture(ecriture: EcritureComptable, devise: String) {
    val dateStr = remember(ecriture.date) {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(ecriture.date))
    }
    val estProduit = ecriture.rubrique in listOf(RubriqueComptable.VENTES, RubriqueComptable.AUTRES_PRODUITS)

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, MissaBorder),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(ecriture.reference ?: "—", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MissaInk)
                    Spacer(Modifier.width(6.dp))
                    Surface(shape = RoundedCornerShape(6.dp), color = GrisCompta.copy(alpha = 0.15f)) {
                        Text(
                            stringResource(ecriture.rubrique.libelleRes),
                            fontSize = 10.sp,
                            color = GrisCompta,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(ecriture.libelle, fontSize = 12.sp, color = MissaInk)
                Text(dateStr, fontSize = 10.sp, color = MissaMuted)
            }
            Text(
                (if (estProduit) "+ " else "- ") + fmtValeur(ecriture.montant, devise),
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (estProduit) Color(0xFF15803D) else Color(0xFFB91C1C),
            )
        }
    }
}
