package com.missa.b360.ui.tresorerie

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.missa.b360.R
import com.missa.b360.core.data.entity.CategorieTresorerie
import com.missa.b360.core.data.entity.CompteTresorerieEntity
import com.missa.b360.core.data.entity.SensMouvement
import com.missa.b360.core.data.entity.TypeCompteTresorerie
import com.missa.b360.core.domain.model.SoldeCompte
import com.missa.b360.core.domain.model.TresorerieRules
import com.missa.b360.core.util.MoneyUtils
import com.missa.b360.ui.components.MissaOption
import com.missa.b360.ui.components.MissaSelecteurBleu
import com.missa.b360.ui.theme.BrandBlue
import com.missa.b360.ui.theme.MissaMuted

/** Création d'un compte de trésorerie. */
@Composable
internal fun TreCompteDialogue(
    enCours: Boolean,
    onFermer: () -> Unit,
    onValider: (String, TypeCompteTresorerie, String, String, String) -> Unit,
) {
    var nom by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(TypeCompteTresorerie.CAISSE) }
    var etablissement by remember { mutableStateOf("") }
    var numero by remember { mutableStateOf("") }
    var soldeInitial by remember { mutableStateOf("") }
    val nomValide = TresorerieRules.libelleValide(nom)

    AlertDialog(
        onDismissRequest = onFermer,
        title = { Text(stringResource(R.string.tre_nouveau_compte), fontSize = 16.sp) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                TreChamps(
                    valeur = nom,
                    onValeur = { nom = it },
                    labelRes = R.string.tre_champ_nom,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    TypeCompteTresorerie.entries.forEach { candidat ->
                        FilterChip(
                            selected = type == candidat,
                            onClick = { type = candidat },
                            label = {
                                Text(
                                    stringResource(TresorerieRules.libelleType(candidat)),
                                    fontSize = 11.5.sp,
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BrandBlue.copy(alpha = 0.15f),
                            ),
                        )
                    }
                }
                // Établissement et numéro n'ont de sens que hors espèces.
                if (type != TypeCompteTresorerie.CAISSE) {
                    TreChamps(
                        valeur = etablissement,
                        onValeur = { etablissement = it },
                        labelRes = R.string.tre_champ_etablissement,
                    )
                    TreChamps(
                        valeur = numero,
                        onValeur = { numero = it },
                        labelRes = R.string.tre_champ_numero,
                    )
                }
                TreChamps(
                    valeur = soldeInitial,
                    onValeur = { soldeInitial = it },
                    labelRes = R.string.tre_champ_solde_initial,
                    numerique = true,
                )
                Text(
                    text = stringResource(R.string.tre_solde_initial_aide),
                    fontSize = 10.5.sp,
                    color = MissaMuted,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onValider(nom, type, etablissement, numero, soldeInitial) },
                enabled = nomValide && !enCours,
            ) { Text(stringResource(R.string.ops_save)) }
        },
        dismissButton = {
            TextButton(onClick = onFermer) { Text(stringResource(R.string.ops_cancel)) }
        },
    )
}

/** Encaissement ou décaissement sur un compte. */
@Composable
internal fun TreMouvementDialogue(
    comptes: List<CompteTresorerieEntity>,
    sensInitial: SensMouvement,
    enCours: Boolean,
    onFermer: () -> Unit,
    onValider: (
        Long,
        SensMouvement,
        String,
        String,
        CategorieTresorerie,
        String,
        String,
        String,
    ) -> Unit,
) {
    var sens by remember { mutableStateOf(sensInitial) }
    var compteId by remember { mutableStateOf(comptes.firstOrNull()?.id ?: 0L) }
    var montant by remember { mutableStateOf("") }
    var libelle by remember { mutableStateOf("") }
    var tiers by remember { mutableStateOf("") }
    var reference by remember { mutableStateOf("") }
    var categorie by remember { mutableStateOf(TresorerieRules.categoriesPour(sensInitial).first()) }

    val categories = TresorerieRules.categoriesPour(sens)
    // Changer de sens change la liste des postes : on retombe sur le premier
    // valide plutôt que de conserver une catégorie devenue incohérente. L'effet
    // évite d'écrire dans un état pendant la composition.
    LaunchedEffect(sens) {
        if (categorie !in TresorerieRules.categoriesPour(sens)) {
            categorie = TresorerieRules.categoriesPour(sens).first()
        }
    }

    val saisieValide = TresorerieRules.montantSaisi(montant) != null &&
        TresorerieRules.libelleValide(libelle) &&
        compteId != 0L

    AlertDialog(
        onDismissRequest = onFermer,
        title = {
            Text(
                stringResource(
                    if (sens == SensMouvement.IN) {
                        R.string.tre_encaissement
                    } else {
                        R.string.tre_decaissement
                    },
                ),
                fontSize = 16.sp,
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    SensMouvement.entries.forEach { candidat ->
                        FilterChip(
                            selected = sens == candidat,
                            onClick = { sens = candidat },
                            label = {
                                Text(
                                    stringResource(
                                        if (candidat == SensMouvement.IN) {
                                            R.string.tre_sens_entree
                                        } else {
                                            R.string.tre_sens_sortie
                                        },
                                    ),
                                    fontSize = 11.5.sp,
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BrandBlue.copy(alpha = 0.15f),
                            ),
                        )
                    }
                }
                MissaSelecteurBleu(
                    label = stringResource(R.string.tre_champ_compte),
                    options = comptes.map {
                        MissaOption(
                            cle = it.id.toString(),
                            titre = it.nom,
                            sousTitre = it.etablissement,
                            badge = stringResource(
                                TresorerieRules.libelleType(TresorerieRules.typeCompte(it.type)),
                            ),
                        )
                    },
                    selectionCle = compteId.takeIf { it != 0L }?.toString(),
                    onSelection = { compteId = it.toLongOrNull() ?: 0L },
                )
                TreChamps(
                    valeur = montant,
                    onValeur = { montant = it },
                    labelRes = R.string.tre_champ_montant,
                    numerique = true,
                )
                TreChamps(
                    valeur = libelle,
                    onValeur = { libelle = it },
                    labelRes = R.string.tre_champ_libelle,
                )
                MissaSelecteurBleu(
                    label = stringResource(R.string.tre_champ_categorie),
                    options = categories.map {
                        MissaOption(
                            cle = it.name,
                            titre = stringResource(TresorerieRules.libelleCategorie(it)),
                        )
                    },
                    selectionCle = categorie.name,
                    onSelection = { cle -> categorie = TresorerieRules.categorie(cle) },
                )
                TreChamps(
                    valeur = tiers,
                    onValeur = { tiers = it },
                    labelRes = R.string.tre_champ_tiers,
                )
                TreChamps(
                    valeur = reference,
                    onValeur = { reference = it },
                    labelRes = R.string.tre_champ_reference,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onValider(compteId, sens, montant, libelle, categorie, tiers, "", reference)
                },
                enabled = saisieValide && !enCours,
            ) { Text(stringResource(R.string.ops_save)) }
        },
        dismissButton = {
            TextButton(onClick = onFermer) { Text(stringResource(R.string.ops_cancel)) }
        },
    )
}

/** Virement interne entre deux comptes. */
@Composable
internal fun TreVirementDialogue(
    comptes: List<CompteTresorerieEntity>,
    devise: String,
    soldes: List<SoldeCompte>,
    enCours: Boolean,
    onFermer: () -> Unit,
    onValider: (Long, Long, String, String) -> Unit,
) {
    var sourceId by remember { mutableStateOf(comptes.firstOrNull()?.id ?: 0L) }
    var destinationId by remember { mutableStateOf(comptes.getOrNull(1)?.id ?: 0L) }
    var montant by remember { mutableStateOf("") }
    val libelleParDefaut = stringResource(R.string.tre_virement_libelle)
    var libelle by remember { mutableStateOf(libelleParDefaut) }

    val valide = sourceId != 0L && destinationId != 0L && sourceId != destinationId &&
        TresorerieRules.montantSaisi(montant) != null &&
        TresorerieRules.libelleValide(libelle)

    fun options(exclu: Long) = comptes.filter { it.id != exclu }.map { compte ->
        MissaOption(
            cle = compte.id.toString(),
            titre = compte.nom,
            badge = soldes.firstOrNull { it.compte.id == compte.id }
                ?.let { MoneyUtils.format(it.solde, devise) },
        )
    }

    AlertDialog(
        onDismissRequest = onFermer,
        title = { Text(stringResource(R.string.tre_virement), fontSize = 16.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                MissaSelecteurBleu(
                    label = stringResource(R.string.tre_champ_source),
                    options = options(destinationId),
                    selectionCle = sourceId.takeIf { it != 0L }?.toString(),
                    onSelection = { sourceId = it.toLongOrNull() ?: 0L },
                )
                MissaSelecteurBleu(
                    label = stringResource(R.string.tre_champ_destination),
                    options = options(sourceId),
                    selectionCle = destinationId.takeIf { it != 0L }?.toString(),
                    onSelection = { destinationId = it.toLongOrNull() ?: 0L },
                )
                TreChamps(
                    valeur = montant,
                    onValeur = { montant = it },
                    labelRes = R.string.tre_champ_montant,
                    numerique = true,
                )
                TreChamps(
                    valeur = libelle,
                    onValeur = { libelle = it },
                    labelRes = R.string.tre_champ_libelle,
                )
                Text(
                    text = stringResource(R.string.tre_virement_aide),
                    fontSize = 10.5.sp,
                    color = MissaMuted,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onValider(sourceId, destinationId, montant, libelle) },
                enabled = valide && !enCours,
            ) { Text(stringResource(R.string.ops_save)) }
        },
        dismissButton = {
            TextButton(onClick = onFermer) { Text(stringResource(R.string.ops_cancel)) }
        },
    )
}

/** Champ de saisie compact commun aux trois dialogues. */
@Composable
private fun TreChamps(
    valeur: String,
    onValeur: (String) -> Unit,
    labelRes: Int,
    numerique: Boolean = false,
) {
    OutlinedTextField(
        value = valeur,
        onValueChange = { saisie ->
            // Sur un champ monétaire, seuls chiffres et séparateur décimal passent :
            // filtrer à la frappe évite un message d'erreur après coup.
            onValeur(
                if (numerique) saisie.filter { it.isDigit() || it == ',' || it == '.' } else saisie,
            )
        },
        label = { Text(stringResource(labelRes), fontSize = 12.sp) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (numerique) KeyboardType.Decimal else KeyboardType.Text,
        ),
        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth(),
    )
}
