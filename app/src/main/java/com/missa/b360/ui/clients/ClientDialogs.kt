package com.missa.b360.ui.clients

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.missa.b360.R
import com.missa.b360.ui.navigation.AppModule
import com.missa.b360.ui.theme.MissaBorder
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted
import com.missa.b360.core.data.entity.BadgeLoyaltyEntity
import com.missa.b360.core.data.entity.CategoryClientEntity
import com.missa.b360.core.domain.usecase.ClientValidation
import com.missa.b360.ui.components.*
import com.missa.b360.ui.icons.Iv

/** Dialog de gestion des catégories de clients (création + suppression verrouillée). */
@Composable
fun CategoriesDialog(
    categories: List<CategoryClientEntity>,
    message: String?,
    onCreer: (String) -> Unit,
    onSupprimer: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var nom by remember { mutableStateOf("") }
    val nomValide = ClientValidation.nomEstValide(nom)

    MissaFormDialogue(
        titre = stringResource(R.string.clients_categories),
        icone = Iv.Category,
        couleur = AppModule.CLIENTS.couleur,
        onFermer = onDismiss,
        libelleValider = stringResource(R.string.clients_ajouter_categorie),
        libelleAnnuler = stringResource(R.string.ob_terminer),
        validerActif = nomValide,
        erreur = message,
        onValider = {
            onCreer(nom)
            nom = ""
        },
    ) {
        MissaFormSection(titre = stringResource(R.string.clients_ajouter_categorie), numero = 1) {
            MissaChampTexte(
                nom, { nom = it }, stringResource(R.string.clients_nom_categorie),
                icone = Iv.Category, requis = true, longueurMax = ClientValidation.LONGUEUR_NOM_MAX,
                erreur = if (nom.isNotEmpty() && !nomValide) stringResource(R.string.clients_nom_invalide) else null,
            )
        }
        MissaFormSection(titre = stringResource(R.string.clients_categories), numero = 2) {
            if (categories.isEmpty()) {
                Text(stringResource(R.string.clients_categories_vide), fontSize = 12.sp, color = MissaMuted)
            }
            categories.forEach { cat ->
                LigneListeFormulaire(cat.nom) {
                    TextButton(onClick = { onSupprimer(cat.id) }) {
                        Text(stringResource(R.string.clients_supprimer_categorie), color = MissaInk, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

/** Ligne d'une liste existante sous un formulaire d'ajout (catégories, badges…). */
@Composable
private fun LigneListeFormulaire(texte: String, fin: @Composable () -> Unit) {
    Surface(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MissaBorder),
        color = androidx.compose.ui.graphics.Color.White,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(start = 14.dp, end = 4.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(texte, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MissaInk, modifier = Modifier.weight(1f).padding(vertical = 10.dp))
            fin()
        }
    }
}

/** Dialog de gestion des badges de fidélité (RC-16). */
@Composable
fun BadgesDialog(
    badges: List<BadgeLoyaltyEntity>,
    onCreer: (String, Double) -> Unit,
    onDismiss: () -> Unit,
) {
    var nom by remember { mutableStateOf("") }
    var remise by remember { mutableStateOf("5") }
    val nomValide = ClientValidation.nomEstValide(nom)
    val remiseValeur = remise.toDoubleOrNull()
    val remiseValide = remiseValeur != null && remiseValeur in 0.0..100.0

    MissaFormDialogue(
        titre = stringResource(R.string.clients_badges),
        icone = Iv.Star,
        couleur = AppModule.CLIENTS.couleur,
        onFermer = onDismiss,
        libelleValider = stringResource(R.string.clients_ajouter_badge),
        libelleAnnuler = stringResource(R.string.ob_terminer),
        validerActif = nomValide && remiseValide,
        onValider = {
            if (remiseValeur != null) onCreer(nom, remiseValeur)
            nom = ""
            remise = "5"
        },
    ) {
        MissaFormSection(titre = stringResource(R.string.clients_ajouter_badge), numero = 1) {
            MissaRangee {
                MissaChampTexte(
                    nom, { nom = it }, stringResource(R.string.clients_nom_badge),
                    modifier = Modifier.weight(1.6f), icone = Iv.Star, requis = true, longueurMax = ClientValidation.LONGUEUR_NOM_MAX,
                    erreur = if (nom.isNotEmpty() && !nomValide) stringResource(R.string.clients_nom_invalide) else null,
                )
                MissaChampTexte(
                    remise, { remise = it }, stringResource(R.string.clients_remise_badge),
                    modifier = Modifier.weight(1f), icone = Iv.Percent, clavier = MissaClavier.DECIMAL, requis = true, longueurMax = 8,
                    erreur = if (!remiseValide) stringResource(R.string.clients_remise_invalide) else null,
                )
            }
        }
        MissaFormSection(titre = stringResource(R.string.clients_badges), numero = 2) {
            if (badges.isEmpty()) {
                Text(stringResource(R.string.clients_badges_vide), fontSize = 12.sp, color = MissaMuted)
            }
            badges.forEach { badge ->
                LigneListeFormulaire("${badge.nom} (-${badge.remisePct}%)") {
                    Text(
                        stringResource(if (badge.actif) R.string.clients_actif else R.string.clients_inactif),
                        fontSize = 11.sp, color = MissaMuted, modifier = Modifier.padding(end = 10.dp),
                    )
                }
            }
        }
    }
}
