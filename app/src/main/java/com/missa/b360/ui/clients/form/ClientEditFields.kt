package com.missa.b360.ui.clients.form

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.missa.b360.R
import com.missa.b360.ui.components.MissaChampTexte
import com.missa.b360.ui.components.MissaClavier
import com.missa.b360.ui.components.MissaOption
import com.missa.b360.ui.components.MissaSelecteurLigne
import com.missa.b360.ui.icons.Iv
import com.missa.b360.ui.theme.MissaBorder
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted

/** Champ texte avec message d'erreur générique quand le champ est invalide. */
@Composable
internal fun ChampClient(
    valeur: String,
    onValeur: (String) -> Unit,
    libelle: Int,
    enErreur: Boolean = false,
    clavier: MissaClavier = MissaClavier.TEXTE,
    lignes: Int = 1,
    requis: Boolean = false,
    modifier: Modifier = Modifier.fillMaxWidth(),
) {
    MissaChampTexte(
        valeur = valeur,
        onValeur = onValeur,
        libelle = stringResource(libelle),
        modifier = modifier,
        clavier = clavier,
        lignes = lignes,
        requis = requis,
        erreur = if (enErreur) stringResource(R.string.form_valeur_invalide) else null,
    )
}

/** Choix dans une courte liste (type, catégorie, badge, mode) : le sélecteur du kit, comme « Pays » à la configuration. */
@Composable
internal fun ChoixClient(
    libelle: Int,
    valeurAffichee: String,
    options: List<Pair<String, String>>,
    onChoix: (String) -> Unit,
) {
    MissaSelecteurLigne(
        label = stringResource(libelle),
        options = options.map { (cle, texte) -> MissaOption(cle = cle, titre = texte) },
        selectionCle = options.firstOrNull { it.second == valeurAffichee }?.first,
        onSelection = onChoix,
        placeholder = valeurAffichee,
        couleurCarte = Color.White,
        paddingVertical = 10.dp,
    )
}
