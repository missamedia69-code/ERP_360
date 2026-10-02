package com.missa.b360.ui.clients.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import com.missa.b360.ui.components.BoutonContourMissa as OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.missa.b360.ui.theme.MissaMuted
import com.missa.b360.ui.theme.MissaBorder
import androidx.compose.material3.Surface
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.missa.b360.R
import com.missa.b360.core.util.Iso4217
import com.missa.b360.ui.components.MissaChampTexte
import com.missa.b360.ui.components.MissaClavier
import com.missa.b360.ui.icons.Iv
import com.missa.b360.ui.theme.MissaInk
import java.util.Locale

/** Indicatif pays (feuille de recherche) + numéro local ; le `+` est géré par le sélecteur. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ClientPhoneField(
    codePays: String?,
    telephoneLocal: String,
    onCodePays: (String) -> Unit,
    onTelephone: (String) -> Unit,
    enErreur: Boolean,
    modifier: Modifier = Modifier,
) {
    val locale = LocalConfiguration.current.locales.takeIf { !it.isEmpty }?.get(0) ?: Locale.getDefault()
    val pays = remember(locale) { Iso4217.paysAvecIndicatif(locale) }
    val choisi = pays.firstOrNull { it.code == codePays }
    var selecteurOuvert by rememberSaveable { mutableStateOf(false) }
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        // Même cadre que les champs du kit : blanc, contour fin, coins de 10 dp.
        Surface(
            onClick = { selecteurOuvert = true },
            shape = RoundedCornerShape(10.dp),
            color = Color.White,
            border = BorderStroke(1.dp, MissaBorder),
            modifier = Modifier.width(112.dp).height(56.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(choisi?.indicatif ?: "…", fontSize = 15.sp, color = MissaInk)
                Icon(painterResource(Iv.ArrowDropDown), null, tint = MissaMuted, modifier = Modifier.size(18.dp))
            }
        }
        MissaChampTexte(
            valeur = telephoneLocal,
            onValeur = onTelephone,
            libelle = stringResource(R.string.clients_telephone),
            modifier = Modifier.weight(1f),
            icone = Iv.Call,
            clavier = MissaClavier.TELEPHONE,
            erreur = if (enErreur || (codePays == null && telephoneLocal.isNotBlank())) stringResource(R.string.form_valeur_invalide) else null,
        )
    }
    if (selecteurOuvert) {
        var requete by rememberSaveable { mutableStateOf("") }
        val visibles = pays.filter {
            requete.isBlank() || it.nom.contains(requete, true) || it.indicatif.contains(requete) || it.code.contains(requete, true)
        }
        ModalBottomSheet(onDismissRequest = { selecteurOuvert = false }, containerColor = Color.White) {
            Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                Text(stringResource(R.string.clients_indicatif_pays), color = MissaInk, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                MissaChampTexte(requete, { requete = it }, stringResource(R.string.clients_rechercher_indicatif), icone = Iv.Search)
                LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                    items(visibles, key = { it.code }) { p ->
                        Row(
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                .clickable { onCodePays(p.code); selecteurOuvert = false }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(p.indicatif, color = MissaInk, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.width(64.dp))
                            Text("${p.nom} (${p.code})", color = MissaInk, fontSize = 15.sp, modifier = Modifier.weight(1f))
                            if (p.code == codePays) Icon(painterResource(Iv.Check), null, tint = MissaInk)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}
