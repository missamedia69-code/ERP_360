package com.missa.b360.ui.fournisseurs.form

import com.missa.b360.ui.theme.OnbConfigCard
import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import com.missa.b360.ui.components.BoutonMissa as Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import com.missa.b360.ui.components.MissaMenuDeroulant
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import com.missa.b360.ui.components.BoutonContourMissa as OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.missa.b360.R
import com.missa.b360.core.data.entity.FournisseurCompteBancaireEntity
import com.missa.b360.core.data.entity.FournisseurDocType
import com.missa.b360.core.data.entity.FournisseurDocumentEntity
import com.missa.b360.core.data.entity.FournisseurEntity
import com.missa.b360.core.data.entity.FournisseurEvenementEntity
import com.missa.b360.core.data.entity.FournisseurEvenementType
import com.missa.b360.core.data.entity.FournisseurItemEntity
import com.missa.b360.core.data.entity.FournisseurStatus
import com.missa.b360.core.data.entity.TypeFournisseur
import com.missa.b360.core.data.entity.VerificationStatut
import com.missa.b360.core.domain.model.FournisseurRules
import com.missa.b360.core.util.PieceJointeAchat
import com.missa.b360.core.util.filterMoneyInput
import com.missa.b360.ui.components.MissaEmptyState
import com.missa.b360.ui.components.MissaTopAppBar
import com.missa.b360.ui.icons.Iv
import com.missa.b360.ui.navigation.AppModule
import com.missa.b360.ui.stock.fmtQuantite
import com.missa.b360.ui.stock.fmtValeur
import com.missa.b360.ui.theme.MissaBorder
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.missa.b360.ui.components.*

import com.missa.b360.ui.fournisseurs.*
import com.missa.b360.ui.fournisseurs.components.*
import com.missa.b360.ui.fournisseurs.dossier.*

internal fun androidx.compose.foundation.lazy.LazyListScope.itemsEtapeIdentite(
    vm: FournisseursViewModel,
    form: FournisseurFormState,
) {
    item {
        var typeOuvert by remember { mutableStateOf(false) }
        MissaChampListe(
            libelle = stringResource(R.string.four_type_fournisseur),
            options = TypeFournisseur.entries.map { option -> option to libelleType(option) },
            selection = form.type,
            onSelection = { option -> vm.updateForm { it.copy(type = option) } },
            icone = Iv.Category,
        )
    }
    item {
        MissaRangee {
            MissaChampTexte(form.nom, { valeur -> vm.updateForm { it.copy(nom = valeur) } }, stringResource(R.string.four_raison_sociale), icone = Iv.Business, requis = true, modifier = Modifier.weight(1f))
            MissaChampTexte(form.nomCommercial, { valeur -> vm.updateForm { it.copy(nomCommercial = valeur) } }, stringResource(R.string.four_nom_commercial), icone = Iv.Business, modifier = Modifier.weight(1f))
        }
    }
    item {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SelecteurSimple(
                icone = Iv.Public,
                label = stringResource(R.string.four_pays) + " *",
                valeur = form.pays,
                options = PAYS,
                modifier = Modifier.weight(1f),
            ) { choix -> vm.updateForm { it.copy(pays = choix) } }
            SelecteurSimple(
                icone = Iv.Payments,
                label = stringResource(R.string.four_devise),
                valeur = form.devise,
                options = DEVISES,
                modifier = Modifier.weight(1f),
            ) { choix -> vm.updateForm { it.copy(devise = choix) } }
        }
    }
    item {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            MissaChampTexte(form.telephone, { valeur -> vm.updateForm { it.copy(telephone = valeur) } }, stringResource(R.string.four_telephone), modifier = Modifier.weight(1f), icone = Iv.Call, clavier = MissaClavier.TELEPHONE, requis = true)
            MissaChampTexte(form.email, { valeur -> vm.updateForm { it.copy(email = valeur) } }, stringResource(R.string.four_email), modifier = Modifier.weight(1f), icone = Iv.MailOutline, clavier = MissaClavier.EMAIL)
        }
    }
    item {
        MissaRangee {
            MissaChampTexte(form.adresse, { valeur -> vm.updateForm { it.copy(adresse = valeur) } }, stringResource(R.string.four_adresse), icone = Iv.Place, modifier = Modifier.weight(1f))
            MissaChampTexte(form.siteWeb, { valeur -> vm.updateForm { it.copy(siteWeb = valeur) } }, stringResource(R.string.four_site_web), icone = Iv.Public, modifier = Modifier.weight(1f))
        }
    }
    item {
        MissaChampTexte(form.description, { valeur -> vm.updateForm { it.copy(description = valeur) } }, stringResource(R.string.four_description), icone = Iv.Description)
    }
}

internal fun androidx.compose.foundation.lazy.LazyListScope.itemsEtapeContacts(
    vm: FournisseursViewModel,
    form: FournisseurFormState,
) {
    item {
        var nom by remember { mutableStateOf("") }
        var prenom by remember { mutableStateOf("") }
        var fonction by remember { mutableStateOf("") }
        var telephone by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        var principal by remember { mutableStateOf(true) }
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = OnbConfigCard,
        ) {
            Column(Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (form.contacts.isEmpty()) {
                    Text(stringResource(R.string.four_aucun_contact), fontSize = 11.sp, color = MissaMuted)
                }
                form.contacts.forEachIndexed { index, contact ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                contact.nom + if (contact.principal) " ★" else "",
                                fontSize = 12.sp,
                                color = MissaInk,
                            )
                            Text(
                                listOfNotNull(
                                    contact.fonction.ifBlank { null },
                                    contact.telephone.ifBlank { null },
                                    contact.email.ifBlank { null },
                                ).joinToString(" · "),
                                fontSize = 10.sp,
                                color = MissaMuted,
                            )
                        }
                        IconButton(
                            onClick = { vm.removeContactSaisi(index) },
                            modifier = Modifier.size(27.dp),
                        ) {
                            Icon(
                                painterResource(Iv.DeleteOutline),
                                null,
                                tint = Color(0xFFB91C1C),
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
                HorizontalDivider(color = MissaBorder)
                Text(stringResource(R.string.four_ajouter_contact), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MissaInk)
                MissaRangee {
                    MissaChampTexte(nom, { nom = it }, stringResource(R.string.four_nom), icone = Iv.Person, modifier = Modifier.weight(1f))
                    MissaChampTexte(prenom, { prenom = it }, stringResource(R.string.four_prenom), icone = Iv.Person, modifier = Modifier.weight(1f))
                }
                MissaRangee {
                    MissaChampTexte(fonction, { fonction = it }, stringResource(R.string.four_fonction), icone = Iv.Badge, modifier = Modifier.weight(1f))
                    MissaChampTexte(telephone, { telephone = it }, stringResource(R.string.four_telephone), icone = Iv.Call, clavier = MissaClavier.TELEPHONE, modifier = Modifier.weight(1f))
                }
                MissaChampTexte(email, { email = it }, stringResource(R.string.four_email), icone = Iv.MailOutline, clavier = MissaClavier.EMAIL)
                MissaCaseACocher(principal, { principal = it }, stringResource(R.string.four_principal))
                Button(
                    onClick = {
                        vm.addContactSaisi(
                            ContactSaisi(
                                nom = nom,
                                prenom = prenom,
                                fonction = fonction,
                                telephone = telephone,
                                email = email,
                                principal = principal,
                            ),
                        )
                        nom = ""
                        prenom = ""
                        fonction = ""
                        telephone = ""
                        email = ""
                    },
                    enabled = nom.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = CouleurFournisseurs, contentColor = Color.White),
                ) {
                    Icon(painterResource(Iv.Add), null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.four_ajouter), fontSize = 12.sp, color = Color.White)
                }
            }
        }
    }
}

internal fun androidx.compose.foundation.lazy.LazyListScope.itemsEtapeFiscalite(
    vm: FournisseursViewModel,
    form: FournisseurFormState,
) {
    val requis = FournisseurRules.identifiantFiscalRequis(form.pays)
    item {
        if (form.type == TypeFournisseur.PARTICULIER) {
            Text(
                stringResource(R.string.four_fiscal_non_requis),
                fontSize = 11.sp,
                color = MissaMuted,
            )
        } else if (requis != null) {
            MissaChampTexte(form.identifiantFiscal, { valeur -> vm.updateForm { it.copy(identifiantFiscal = valeur) } }, requis, icone = Iv.Badge, requis = true)
        } else {
            MissaChampTexte(form.identifiantFiscal, { valeur -> vm.updateForm { it.copy(identifiantFiscal = valeur) } }, stringResource(R.string.four_identifiant), icone = Iv.Badge)
        }
    }
    item {
        MissaChampTexte(form.rccm, { valeur -> vm.updateForm { it.copy(rccm = valeur) } }, stringResource(R.string.four_rccm), icone = Iv.Badge)
    }
    item {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = OnbConfigCard,
        ) {
            Column(Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                MissaInterrupteur(
                    actif = form.assujettiTva,
                    onChange = { valeur -> vm.updateForm { it.copy(assujettiTva = valeur) } },
                    libelle = stringResource(R.string.four_assujetti_tva),
                )
                if (form.assujettiTva) {
                    MissaChampTexte(form.numTva, { valeur -> vm.updateForm { it.copy(numTva = valeur) } }, stringResource(R.string.four_num_tva), icone = Iv.Badge)
                }
                MissaInterrupteur(
                    actif = form.exonere,
                    onChange = { valeur -> vm.updateForm { it.copy(exonere = valeur) } },
                    libelle = stringResource(R.string.four_exonere),
                )
                if (!form.exonere) {
                    MissaChampTexte(form.tauxRetenue, { valeur -> vm.updateForm { it.copy(tauxRetenue = valeur.filterMoneyInput()) } }, stringResource(R.string.four_taux_retenue), icone = Iv.Percent, clavier = MissaClavier.DECIMAL)
                }
            }
        }
    }
}
