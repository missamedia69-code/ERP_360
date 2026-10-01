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
import com.missa.b360.ui.icons.Iv
import com.missa.b360.ui.theme.MissaBorder
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted

/** Section pliable : l'en-tête est une cible de 48 dp, une pastille rouge signale une erreur cachée. */
@Composable
internal fun SectionPliable(
    titre: String,
    ouverte: Boolean,
    enErreur: Boolean,
    onBascule: () -> Unit,
    contenu: @Composable () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, if (enErreur) Color(0xFFDC2626) else MissaBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(onClick = onBascule),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(titre, color = MissaInk, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                if (enErreur) Icon(painterResource(Iv.Warning), null, tint = Color(0xFFDC2626), modifier = Modifier.size(20.dp))
                Icon(
                    painterResource(if (ouverte) Iv.ExpandLess else Iv.ExpandMore),
                    contentDescription = null, tint = MissaMuted, modifier = Modifier.size(24.dp),
                )
            }
            if (ouverte) {
                Column(Modifier.padding(bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { contenu() }
            }
        }
    }
}

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

/** Choix dans une courte liste (type, catégorie, badge) via un menu déroulant. */
@Composable
internal fun ChoixClient(
    libelle: Int,
    valeurAffichee: String,
    options: List<Pair<String, String>>,
    onChoix: (String) -> Unit,
) {
    var ouvert by rememberSaveable { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(stringResource(libelle), color = MissaMuted, fontSize = 13.sp)
        Box {
            OutlinedButton(
                onClick = { ouvert = true },
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            ) {
                Text(valeurAffichee, color = MissaInk, modifier = Modifier.weight(1f))
                Icon(painterResource(Iv.ArrowDropDown), null, tint = MissaMuted)
            }
            DropdownMenu(expanded = ouvert, onDismissRequest = { ouvert = false }) {
                options.forEach { (cle, texte) ->
                    DropdownMenuItem(
                        text = { Text(texte) },
                        onClick = {
                            ouvert = false
                            onChoix(cle)
                        },
                        modifier = Modifier.heightIn(min = 48.dp),
                    )
                }
            }
        }
    }
}
