package com.missa.b360.ui.clients.form

import com.missa.b360.ui.components.LocalCouleurSelection
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import com.missa.b360.ui.components.BoutonContourMissa as OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.missa.b360.R
import com.missa.b360.core.data.entity.ClientType
import com.missa.b360.ui.clients.components.ClientPhoneField
import com.missa.b360.ui.clients.components.ClientBoutonAjout
import androidx.compose.ui.unit.sp
import com.missa.b360.ui.clients.labelRes
import com.missa.b360.ui.components.MissaRangee
import com.missa.b360.ui.components.MissaFormulaireTheme
import com.missa.b360.ui.clients.components.ClientCouleurs
import com.missa.b360.ui.components.MissaCaseACocher
import com.missa.b360.ui.components.MissaClavier
import com.missa.b360.ui.components.MissaInterrupteur
import com.missa.b360.ui.icons.Iv

private const val AUCUN = "-"

@Composable
internal fun SectionIdentite(etat: ClientEditUiState, modifier: (ClientDraft.() -> ClientDraft) -> Unit) {
    val d = etat.draft
    val err = etat.erreurs
    ChampClient(d.nom, { v -> modifier { copy(nom = v.take(120)) } }, R.string.clients_nom, DraftField.NOM in err, requis = true)
    MissaRangee {
        ChoixClient(
            libelle = R.string.clients_type,
            valeurAffichee = stringResource((ClientType.entries.firstOrNull { it.name == d.type } ?: ClientType.PARTICULIER).labelRes()),
            options = ClientType.entries.map { it.name to stringResource(it.labelRes()) },
            onChoix = { v -> modifier { copy(type = v) } },
            modifier = Modifier.weight(1f),
        )
        ChampClient(d.email, { v -> modifier { copy(email = v.take(254)) } }, R.string.clients_email, DraftField.EMAIL in err, MissaClavier.EMAIL, modifier = Modifier.weight(1f))
    }
    ClientPhoneField(
        codePays = d.codePays,
        telephoneLocal = d.telephoneLocal,
        onCodePays = { v -> modifier { copy(codePays = v) } },
        onTelephone = { v -> modifier { copy(telephoneLocal = v.filter { it.isDigit() || it == ' ' || it == '-' || it == '(' || it == ')' }) } },
        enErreur = DraftField.TELEPHONE in err,
    )
    ChampClient(d.adresse, { v -> modifier { copy(adresse = v.take(250)) } }, R.string.clients_adresse, DraftField.ADRESSE in err)
}

@Composable
internal fun SectionFiscalite(etat: ClientEditUiState, modifier: (ClientDraft.() -> ClientDraft) -> Unit) {
    val d = etat.draft
    val err = etat.erreurs
    MissaRangee {
        ChampClient(d.typeIdentifiantFiscal, { v -> modifier { copy(typeIdentifiantFiscal = v.take(40)) } }, R.string.cli_fiscal_type, DraftField.FISCAL_TEXTE in err, modifier = Modifier.weight(1f))
        ChampClient(d.nif, { v -> modifier { copy(nif = v.take(80)) } }, R.string.cli_cond_nif, DraftField.FISCAL_TEXTE in err, modifier = Modifier.weight(1f))
    }
    MissaRangee {
        ChampClient(d.numeroTva, { v -> modifier { copy(numeroTva = v.take(80)) } }, R.string.cli_fiscal_numero_tva, modifier = Modifier.weight(1f))
        ChampClient(d.tauxTva, { v -> modifier { copy(tauxTva = v) } }, R.string.cli_fiscal_taux, DraftField.TAUX_TVA in err, MissaClavier.DECIMAL, modifier = Modifier.weight(1f))
    }
    MissaFormulaireTheme(ClientCouleurs.Violet) {
    MissaRangee {
        MissaInterrupteur(actif = d.assujettiTva, onChange = { v -> modifier { copy(assujettiTva = v) } }, libelle = stringResource(R.string.cli_fiscal_assujetti), modifier = Modifier.weight(1f))
        MissaInterrupteur(actif = d.exonereTva, onChange = { v -> modifier { copy(exonereTva = v) } }, libelle = stringResource(R.string.cli_fiscal_exonere), modifier = Modifier.weight(1f))
    }
    }
    if (d.exonereTva) {
        ChampClient(d.motifExoneration, { v -> modifier { copy(motifExoneration = v.take(240)) } }, R.string.cli_fiscal_motif, DraftField.MOTIF_EXONERATION in err)
    }
}

@Composable
internal fun SectionConditions(etat: ClientEditUiState, modifier: (ClientDraft.() -> ClientDraft) -> Unit) {
    val d = etat.draft
    val err = etat.erreurs
    MissaRangee {
        ChampClient(d.delaiPaiement, { v -> modifier { copy(delaiPaiement = v.take(3)) } }, R.string.cli_cond_delai, DraftField.DELAI in err, MissaClavier.ENTIER, modifier = Modifier.weight(1f))
        ChampClient(d.limiteCredit, { v -> modifier { copy(limiteCredit = v) } }, R.string.clients_limite_credit, DraftField.LIMITE in err, MissaClavier.DECIMAL, modifier = Modifier.weight(1f))
    }
    MissaRangee {
        ChampClient(d.remiseDefaut, { v -> modifier { copy(remiseDefaut = v) } }, R.string.cli_cond_remise, DraftField.REMISE in err, MissaClavier.DECIMAL, modifier = Modifier.weight(1f))
        ChampClient(d.remiseMax, { v -> modifier { copy(remiseMax = v) } }, R.string.cli_cond_remise_max, DraftField.REMISE_MAX in err, MissaClavier.DECIMAL, modifier = Modifier.weight(1f))
    }
    MissaRangee {
        ChoixClient(
            libelle = R.string.clients_categories,
            valeurAffichee = etat.categories.firstOrNull { it.id == d.categorieId }?.nom ?: stringResource(R.string.clients_aucune_categorie),
            options = listOf(AUCUN to stringResource(R.string.clients_aucune_categorie)) + etat.categories.map { it.id.toString() to it.nom },
            onChoix = { v -> modifier { copy(categorieId = v.toLongOrNull()) } },
            modifier = Modifier.weight(1f),
        )
        ChoixClient(
            libelle = R.string.clients_badges,
            valeurAffichee = etat.badges.firstOrNull { it.id == d.badgeId }?.nom ?: stringResource(R.string.cli_aucun_badge),
            options = listOf(AUCUN to stringResource(R.string.cli_aucun_badge)) + etat.badges.map { it.id.toString() to it.nom },
            onChoix = { v -> modifier { copy(badgeId = v.toLongOrNull()) } },
            modifier = Modifier.weight(1f),
        )
    }
    MissaRangee {
        ChampClient(d.commercial, { v -> modifier { copy(commercial = v.take(120)) } }, R.string.cli_cond_commercial, modifier = Modifier.weight(1f))
        ChampClient(d.grilleTarifaire, { v -> modifier { copy(grilleTarifaire = v.take(80)) } }, R.string.cli_cond_grille, modifier = Modifier.weight(1f))
    }
    MissaRangee {
        ChampClient(d.segment, { v -> modifier { copy(segment = v.take(80)) } }, R.string.cli_cond_segment, modifier = Modifier.weight(1f))
        ChampClient(d.canalVente, { v -> modifier { copy(canalVente = v.take(80)) } }, R.string.cli_cond_canal, modifier = Modifier.weight(1f))
    }
    MissaRangee {
        ChampClient(d.territoire, { v -> modifier { copy(territoire = v.take(120)) } }, R.string.cli_cond_territoire, modifier = Modifier.weight(1f))
        ChampClient(d.compteComptable, { v -> modifier { copy(compteComptable = v.take(40)) } }, R.string.cli_cond_compte, modifier = Modifier.weight(1f))
    }
    ChampClient(d.conditionsPaiement, { v -> modifier { copy(conditionsPaiement = v.take(240)) } }, R.string.cli_cond_conditions, DraftField.CONDITIONS_TEXTE in err)
}

@Composable
internal fun SectionContacts(etat: ClientEditUiState, modifier: (ClientDraft.() -> ClientDraft) -> Unit) {
    val d = etat.draft
    val err = etat.erreurs
    Text(stringResource(R.string.cli_form_note_contacts), color = com.missa.b360.ui.theme.MissaMuted, fontSize = 10.sp, lineHeight = 15.sp)
    d.contacts.forEachIndexed { index, c ->
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ChampClient(
                    c.nom, { v -> modifier { copy(contacts = contacts.replace(index) { it.copy(nom = v) }) } },
                    R.string.clients_nom, DraftField.CONTACTS in err && c.nom.trim().length < 2,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { modifier { copy(contacts = contacts.filterIndexed { i, _ -> i != index }) } }, modifier = Modifier.size(48.dp)) {
                    Icon(painterResource(Iv.Close), stringResource(R.string.cli_retirer), tint = LocalCouleurSelection.current ?: com.missa.b360.ui.theme.MissaMuted)
                }
            }
            MissaRangee {
                ChampClient(c.fonction, { v -> modifier { copy(contacts = contacts.replace(index) { it.copy(fonction = v) }) } }, R.string.cli_contact_fonction, modifier = Modifier.weight(1f))
                ChampClient(c.telephone, { v -> modifier { copy(contacts = contacts.replace(index) { it.copy(telephone = v) }) } }, R.string.clients_telephone, DraftField.CONTACTS in err, MissaClavier.TELEPHONE, modifier = Modifier.weight(1f))
            }
            ChampClient(c.email, { v -> modifier { copy(contacts = contacts.replace(index) { it.copy(email = v) }) } }, R.string.clients_email, DraftField.CONTACTS in err, MissaClavier.EMAIL)
            MissaFormulaireTheme(ClientCouleurs.Violet) {
            MissaCaseACocher(
                coche = c.principal,
                onChange = { v -> modifier { copy(contacts = contacts.mapIndexed { i, x -> x.copy(principal = v && i == index) }) } },
                libelle = stringResource(R.string.cli_contact_principal),
            )
            }
        }
    }
    ClientBoutonAjout(
        libelle = stringResource(R.string.cli_ajouter_contact),
        onClick = { modifier { copy(contacts = contacts + ContactDraft(principal = contacts.isEmpty())) } },
    )

    d.adresses.forEachIndexed { index, a ->
        Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ChampClient(
                    a.libelle, { v -> modifier { copy(adresses = adresses.replace(index) { it.copy(libelle = v.take(60)) }) } },
                    R.string.cli_adresse_libelle, modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { modifier { copy(adresses = adresses.filterIndexed { i, _ -> i != index }) } }, modifier = Modifier.size(48.dp)) {
                    Icon(painterResource(Iv.Close), stringResource(R.string.cli_retirer), tint = LocalCouleurSelection.current ?: com.missa.b360.ui.theme.MissaMuted)
                }
            }
            MissaRangee {
                ChampClient(a.adresse, { v -> modifier { copy(adresses = adresses.replace(index) { it.copy(adresse = v.take(250)) }) } }, R.string.clients_adresse, DraftField.ADRESSES in err && a.adresse.trim().length < 2, modifier = Modifier.weight(1f))
                ChampClient(a.ville, { v -> modifier { copy(adresses = adresses.replace(index) { it.copy(ville = v.take(100)) }) } }, R.string.cli_adresse_ville, modifier = Modifier.weight(1f))
            }
            MissaFormulaireTheme(ClientCouleurs.Violet) {
            MissaCaseACocher(
                coche = a.principale,
                onChange = { v -> modifier { copy(adresses = adresses.mapIndexed { i, x -> x.copy(principale = v && i == index) }) } },
                libelle = stringResource(R.string.cli_adresse_principale),
            )
            }
        }
    }
    ClientBoutonAjout(
        libelle = stringResource(R.string.cli_ajouter_adresse),
        onClick = { modifier { copy(adresses = adresses + AddressDraft(principale = adresses.isEmpty())) } },
    )
}

@Composable
internal fun SectionNotes(etat: ClientEditUiState, modifier: (ClientDraft.() -> ClientDraft) -> Unit) {
    Text(stringResource(R.string.cli_form_note_notes), color = com.missa.b360.ui.theme.MissaMuted, fontSize = 10.sp, lineHeight = 15.sp)
    ChampClient(etat.draft.notes, { v -> modifier { copy(notes = v.take(1000)) } }, R.string.clients_notes, DraftField.NOTES in etat.erreurs, lignes = 4)
}

private fun <T> List<T>.replace(index: Int, transformation: (T) -> T): List<T> =
    mapIndexed { i, x -> if (i == index) transformation(x) else x }
