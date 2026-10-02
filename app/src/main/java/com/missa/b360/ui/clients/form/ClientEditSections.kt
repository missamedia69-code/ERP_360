package com.missa.b360.ui.clients.form

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
import com.missa.b360.ui.clients.labelRes
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
    ChoixClient(
        libelle = R.string.clients_type,
        valeurAffichee = stringResource((ClientType.entries.firstOrNull { it.name == d.type } ?: ClientType.PARTICULIER).labelRes()),
        options = ClientType.entries.map { it.name to stringResource(it.labelRes()) },
        onChoix = { v -> modifier { copy(type = v) } },
    )
    ClientPhoneField(
        codePays = d.codePays,
        telephoneLocal = d.telephoneLocal,
        onCodePays = { v -> modifier { copy(codePays = v) } },
        onTelephone = { v -> modifier { copy(telephoneLocal = v.filter { it.isDigit() || it == ' ' || it == '-' || it == '(' || it == ')' }) } },
        enErreur = DraftField.TELEPHONE in err,
    )
    ChampClient(d.email, { v -> modifier { copy(email = v.take(254)) } }, R.string.clients_email, DraftField.EMAIL in err, MissaClavier.EMAIL)
    ChampClient(d.adresse, { v -> modifier { copy(adresse = v.take(250)) } }, R.string.clients_adresse, DraftField.ADRESSE in err, lignes = 2)
}

@Composable
internal fun SectionFiscalite(etat: ClientEditUiState, modifier: (ClientDraft.() -> ClientDraft) -> Unit) {
    val d = etat.draft
    val err = etat.erreurs
    ChampClient(d.typeIdentifiantFiscal, { v -> modifier { copy(typeIdentifiantFiscal = v.take(40)) } }, R.string.cli_fiscal_type, DraftField.FISCAL_TEXTE in err)
    ChampClient(d.nif, { v -> modifier { copy(nif = v.take(80)) } }, R.string.cli_cond_nif, DraftField.FISCAL_TEXTE in err)
    ChampClient(d.numeroTva, { v -> modifier { copy(numeroTva = v.take(80)) } }, R.string.cli_fiscal_numero_tva)
    MissaInterrupteur(actif = d.assujettiTva, onChange = { v -> modifier { copy(assujettiTva = v) } }, libelle = stringResource(R.string.cli_fiscal_assujetti))
    MissaInterrupteur(actif = d.exonereTva, onChange = { v -> modifier { copy(exonereTva = v) } }, libelle = stringResource(R.string.cli_fiscal_exonere))
    if (d.exonereTva) {
        ChampClient(d.motifExoneration, { v -> modifier { copy(motifExoneration = v.take(240)) } }, R.string.cli_fiscal_motif, DraftField.MOTIF_EXONERATION in err)
    }
    ChampClient(d.tauxTva, { v -> modifier { copy(tauxTva = v) } }, R.string.cli_fiscal_taux, DraftField.TAUX_TVA in err, MissaClavier.DECIMAL)
}

@Composable
internal fun SectionConditions(etat: ClientEditUiState, modifier: (ClientDraft.() -> ClientDraft) -> Unit) {
    val d = etat.draft
    val err = etat.erreurs
    ChampClient(d.delaiPaiement, { v -> modifier { copy(delaiPaiement = v.take(3)) } }, R.string.cli_cond_delai, DraftField.DELAI in err, MissaClavier.ENTIER)
    ChampClient(d.limiteCredit, { v -> modifier { copy(limiteCredit = v) } }, R.string.clients_limite_credit, DraftField.LIMITE in err, MissaClavier.DECIMAL)
    ChampClient(d.remiseDefaut, { v -> modifier { copy(remiseDefaut = v) } }, R.string.cli_cond_remise, DraftField.REMISE in err, MissaClavier.DECIMAL)
    ChampClient(d.remiseMax, { v -> modifier { copy(remiseMax = v) } }, R.string.cli_cond_remise_max, DraftField.REMISE_MAX in err, MissaClavier.DECIMAL)
    ChoixClient(
        libelle = R.string.clients_categories,
        valeurAffichee = etat.categories.firstOrNull { it.id == d.categorieId }?.nom ?: stringResource(R.string.clients_aucune_categorie),
        options = listOf(AUCUN to stringResource(R.string.clients_aucune_categorie)) + etat.categories.map { it.id.toString() to it.nom },
        onChoix = { v -> modifier { copy(categorieId = v.toLongOrNull()) } },
    )
    ChoixClient(
        libelle = R.string.clients_badges,
        valeurAffichee = etat.badges.firstOrNull { it.id == d.badgeId }?.nom ?: stringResource(R.string.cli_aucun_badge),
        options = listOf(AUCUN to stringResource(R.string.cli_aucun_badge)) + etat.badges.map { it.id.toString() to it.nom },
        onChoix = { v -> modifier { copy(badgeId = v.toLongOrNull()) } },
    )
    ChampClient(d.commercial, { v -> modifier { copy(commercial = v.take(120)) } }, R.string.cli_cond_commercial)
    ChampClient(d.conditionsPaiement, { v -> modifier { copy(conditionsPaiement = v.take(240)) } }, R.string.cli_cond_conditions, DraftField.CONDITIONS_TEXTE in err)
    ChampClient(d.grilleTarifaire, { v -> modifier { copy(grilleTarifaire = v.take(80)) } }, R.string.cli_cond_grille)
    ChampClient(d.segment, { v -> modifier { copy(segment = v.take(80)) } }, R.string.cli_cond_segment)
    ChampClient(d.canalVente, { v -> modifier { copy(canalVente = v.take(80)) } }, R.string.cli_cond_canal)
    ChampClient(d.territoire, { v -> modifier { copy(territoire = v.take(120)) } }, R.string.cli_cond_territoire)
    ChampClient(d.compteComptable, { v -> modifier { copy(compteComptable = v.take(40)) } }, R.string.cli_cond_compte)
}

@Composable
internal fun SectionContacts(etat: ClientEditUiState, modifier: (ClientDraft.() -> ClientDraft) -> Unit) {
    val d = etat.draft
    val err = etat.erreurs
    d.contacts.forEachIndexed { index, c ->
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ChampClient(
                    c.nom, { v -> modifier { copy(contacts = contacts.replace(index) { it.copy(nom = v) }) } },
                    R.string.clients_nom, DraftField.CONTACTS in err && c.nom.trim().length < 2,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { modifier { copy(contacts = contacts.filterIndexed { i, _ -> i != index }) } }, modifier = Modifier.size(48.dp)) {
                    Icon(painterResource(Iv.Close), stringResource(R.string.cli_retirer), tint = com.missa.b360.ui.theme.MissaMuted)
                }
            }
            ChampClient(c.fonction, { v -> modifier { copy(contacts = contacts.replace(index) { it.copy(fonction = v) }) } }, R.string.cli_contact_fonction)
            ChampClient(c.telephone, { v -> modifier { copy(contacts = contacts.replace(index) { it.copy(telephone = v) }) } }, R.string.clients_telephone, DraftField.CONTACTS in err, MissaClavier.TELEPHONE)
            ChampClient(c.email, { v -> modifier { copy(contacts = contacts.replace(index) { it.copy(email = v) }) } }, R.string.clients_email, DraftField.CONTACTS in err, MissaClavier.EMAIL)
            MissaCaseACocher(
                coche = c.principal,
                onChange = { v -> modifier { copy(contacts = contacts.mapIndexed { i, x -> x.copy(principal = v && i == index) }) } },
                libelle = stringResource(R.string.cli_contact_principal),
            )
        }
    }
    OutlinedButton(
        onClick = { modifier { copy(contacts = contacts + ContactDraft(principal = contacts.isEmpty())) } },
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
    ) { Text(stringResource(R.string.cli_ajouter_contact)) }

    d.adresses.forEachIndexed { index, a ->
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ChampClient(
                    a.libelle, { v -> modifier { copy(adresses = adresses.replace(index) { it.copy(libelle = v.take(60)) }) } },
                    R.string.cli_adresse_libelle, modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { modifier { copy(adresses = adresses.filterIndexed { i, _ -> i != index }) } }, modifier = Modifier.size(48.dp)) {
                    Icon(painterResource(Iv.Close), stringResource(R.string.cli_retirer), tint = com.missa.b360.ui.theme.MissaMuted)
                }
            }
            ChampClient(a.adresse, { v -> modifier { copy(adresses = adresses.replace(index) { it.copy(adresse = v.take(250)) }) } }, R.string.clients_adresse, DraftField.ADRESSES in err && a.adresse.trim().length < 2, lignes = 2)
            ChampClient(a.ville, { v -> modifier { copy(adresses = adresses.replace(index) { it.copy(ville = v.take(100)) }) } }, R.string.cli_adresse_ville)
            MissaCaseACocher(
                coche = a.principale,
                onChange = { v -> modifier { copy(adresses = adresses.mapIndexed { i, x -> x.copy(principale = v && i == index) }) } },
                libelle = stringResource(R.string.cli_adresse_principale),
            )
        }
    }
    OutlinedButton(
        onClick = { modifier { copy(adresses = adresses + AddressDraft(principale = adresses.isEmpty())) } },
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
    ) { Text(stringResource(R.string.cli_ajouter_adresse)) }
}

@Composable
internal fun SectionNotes(etat: ClientEditUiState, modifier: (ClientDraft.() -> ClientDraft) -> Unit) {
    ChampClient(etat.draft.notes, { v -> modifier { copy(notes = v.take(1000)) } }, R.string.clients_notes, DraftField.NOTES in etat.erreurs, lignes = 5)
}

private fun <T> List<T>.replace(index: Int, transformation: (T) -> T): List<T> =
    mapIndexed { i, x -> if (i == index) transformation(x) else x }
