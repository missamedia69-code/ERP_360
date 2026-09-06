package com.missa.b360.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.missa.b360.R
import com.missa.b360.ui.theme.BrandBlue
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted
import com.missa.b360.ui.theme.MissaSoftBlue
import com.missa.b360.ui.theme.MissaSurface
import java.text.Normalizer

/**
 * Une entrée proposée par le sélecteur standard.
 *
 * @param cle valeur conservée par l'appelant (code langue, identifiant de fuseau…)
 * @param titre libellé principal (déjà traduit)
 * @param sousTitre ligne d'explication : villes repères, exemple de rendu…
 * @param badge pastille de gauche : « UTC+01:00 », « 30 j », « FR »…
 * @param badgeSecondaire seconde ligne de la pastille : heure locale, taux…
 */
data class MissaOption(
    val cle: String,
    val titre: String,
    val sousTitre: String? = null,
    val badge: String? = null,
    val badgeSecondaire: String? = null,
)

/**
 * Sélecteur standard de l'application (style retenu pour le fuseau horaire) :
 * une ligne « libellé + valeur courante + chevron » qui ouvre une boîte détaillée
 * — recherche insensible aux accents, liste défilante positionnée sur la valeur
 * active, pastille de gauche, sous-titre explicatif et coche de sélection.
 *
 * À utiliser partout où l'utilisateur choisit une valeur dans un catalogue,
 * en remplacement des menus déroulants compacts.
 */
@Composable
fun MissaSelecteurLigne(
    label: String,
    options: List<MissaOption>,
    selectionCle: String?,
    onSelection: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    placeholder: String = "",
    detail: String? = null,
    titreDialogue: String = label,
    indiceRecherche: String? = null,
    avecRecherche: Boolean = options.size >= 8,
) {
    var ouvert by remember { mutableStateOf(false) }
    val choisie = options.firstOrNull { it.cle == selectionCle }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (enabled) Modifier.clickable { ouvert = true } else Modifier)
            .padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = MissaInk,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = choisie?.let { option ->
                    option.badge?.let { "$it · ${option.titre}" } ?: option.titre
                } ?: placeholder,
                fontSize = 13.5.sp,
                color = MissaMuted,
            )
            if (detail != null) {
                Spacer(Modifier.height(1.dp))
                Text(text = detail, fontSize = 11.5.sp, color = BrandBlue)
            }
        }
        Icon(
            imageVector = Icons.Outlined.ArrowDropDown,
            contentDescription = null,
            tint = MissaMuted,
            modifier = Modifier.size(18.dp),
        )
    }
    if (ouvert) {
        MissaSelecteurDialogue(
            titre = titreDialogue,
            options = options,
            selectionCle = selectionCle,
            indiceRecherche = indiceRecherche,
            avecRecherche = avecRecherche,
            onSelection = {
                onSelection(it)
                ouvert = false
            },
            onFermer = { ouvert = false },
        )
    }
}

/**
 * Même sélecteur, présenté comme un champ plein bleu de marque (icône, libellé
 * clair, valeur en blanc) — mise en avant d'un choix important dans un écran.
 */
@Composable
fun MissaSelecteurBleu(
    label: String,
    options: List<MissaOption>,
    selectionCle: String?,
    onSelection: (String) -> Unit,
    modifier: Modifier = Modifier,
    icone: ImageVector? = null,
    enabled: Boolean = true,
    placeholder: String = "",
    titreDialogue: String = label,
    indiceRecherche: String? = null,
    avecRecherche: Boolean = options.size >= 8,
) {
    var ouvert by remember { mutableStateOf(false) }
    val choisie = options.firstOrNull { it.cle == selectionCle }
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = BrandBlue,
        modifier = modifier
            .fillMaxWidth()
            .then(if (enabled) Modifier.clickable { ouvert = true } else Modifier),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 15.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icone != null) {
                Icon(
                    imageVector = icone,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    fontSize = 11.5.sp,
                    color = Color.White.copy(alpha = 0.75f),
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = choisie?.titre ?: placeholder,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
            }
            Icon(
                imageVector = Icons.Outlined.ArrowDropDown,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp),
            )
        }
    }
    if (ouvert) {
        MissaSelecteurDialogue(
            titre = titreDialogue,
            options = options,
            selectionCle = selectionCle,
            indiceRecherche = indiceRecherche,
            avecRecherche = avecRecherche,
            onSelection = {
                onSelection(it)
                ouvert = false
            },
            onFermer = { ouvert = false },
        )
    }
}

/** Boîte détaillée partagée : recherche, liste défilante, coche de sélection. */
@Composable
fun MissaSelecteurDialogue(
    titre: String,
    options: List<MissaOption>,
    selectionCle: String?,
    onSelection: (String) -> Unit,
    onFermer: () -> Unit,
    indiceRecherche: String? = null,
    avecRecherche: Boolean = options.size >= 8,
) {
    var requete by remember { mutableStateOf("") }
    val filtre = normaliserRecherche(requete)
    val visibles = if (!avecRecherche || filtre.isBlank()) {
        options
    } else {
        options.filter { option ->
            normaliserRecherche(
                "${option.badge.orEmpty()} ${option.titre} ${option.sousTitre.orEmpty()}",
            ).contains(filtre)
        }
    }
    val etatListe = rememberLazyListState()
    val indexCourant = visibles.indexOfFirst { it.cle == selectionCle }
    LaunchedEffect(Unit) {
        if (indexCourant > 1) etatListe.scrollToItem(indexCourant - 1)
    }
    AlertDialog(
        onDismissRequest = onFermer,
        confirmButton = {
            TextButton(onClick = onFermer) { Text(stringResource(R.string.ob_fermer)) }
        },
        title = {
            Text(
                text = titre,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MissaInk,
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (avecRecherche) {
                    OutlinedTextField(
                        value = requete,
                        onValueChange = { requete = it },
                        singleLine = true,
                        label = {
                            Text(
                                text = indiceRecherche ?: stringResource(R.string.selecteur_recherche),
                                fontSize = 12.sp,
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                }
                if (visibles.isEmpty()) {
                    Text(
                        text = stringResource(R.string.selecteur_aucun),
                        fontSize = 12.5.sp,
                        color = MissaMuted,
                        modifier = Modifier.padding(vertical = 12.dp),
                    )
                } else {
                    LazyColumn(
                        state = etatListe,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 380.dp),
                    ) {
                        items(visibles, key = { it.cle }) { option ->
                            MissaOptionLigne(
                                option = option,
                                actif = option.cle == selectionCle,
                                onClick = { onSelection(option.cle) },
                            )
                        }
                    }
                }
            }
        },
    )
}

/** Une entrée de la boîte : pastille, titre (+ sous-titre) et coche si active. */
@Composable
private fun MissaOptionLigne(
    option: MissaOption,
    actif: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (actif) MissaSoftBlue else MissaSurface)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (option.badge != null) {
            Column(modifier = Modifier.widthIn(min = 74.dp)) {
                Text(
                    text = option.badge,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (actif) BrandBlue else MissaInk,
                )
                if (option.badgeSecondaire != null) {
                    Text(
                        text = option.badgeSecondaire,
                        fontSize = 11.sp,
                        color = MissaMuted,
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = option.titre,
                fontSize = 12.5.sp,
                fontWeight = if (actif) FontWeight.SemiBold else FontWeight.Normal,
                color = MissaInk,
            )
            if (option.sousTitre != null) {
                Text(
                    text = option.sousTitre,
                    fontSize = 11.sp,
                    color = MissaMuted,
                )
            }
        }
        if (actif) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                tint = BrandBlue,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/** Recherche insensible à la casse et aux accents (Douala = douala = doualá). */
fun normaliserRecherche(texte: String): String =
    Normalizer.normalize(texte.lowercase(), Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .trim()
