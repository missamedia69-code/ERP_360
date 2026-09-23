package com.missa.b360.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.missa.b360.core.data.entity.ProductType
import com.missa.b360.ui.components.MissaTopAppBar
import com.missa.b360.ui.icons.Iv
import com.missa.b360.ui.theme.MissaBorder
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted

private val GrisReferentiel: Color = Color(0xFF4B5563)

private data class TuileReferentielSpec(
    val icone: Int,
    val titre: String,
    val sousTitre: String,
    val estActif: Boolean,
    val onClick: () -> Unit,
)

private data class ItemReferentiel(
    val code: String,
    val libelle: String,
    val detail: String,
)

@Composable
fun ReferentielsScreen(
    onBack: () -> Unit,
) {
    var categorieChoisie by remember { mutableStateOf(0) } // 0: Familles, 1: Unités, 2: Devises, 3: Taxes

    val famillesTypes = remember {
        ProductType.entries.map {
            ItemReferentiel(it.name, it.name.replace('_', ' ').lowercase().replaceFirstChar { c -> c.uppercase() }, "Type standard OHADA")
        }
    }

    val unites = remember {
        listOf(
            ItemReferentiel("U", "Unité (pièce)", "Comptage unitaire"),
            ItemReferentiel("KG", "Kilogramme", "Masse / Poids"),
            ItemReferentiel("L", "Litre", "Volume liquide"),
            ItemReferentiel("M", "Mètre", "Longueur"),
            ItemReferentiel("M2", "Mètre carré", "Surface"),
            ItemReferentiel("CRT", "Carton", "Conditionnement groupé"),
            ItemReferentiel("PAL", "Palette", "Logistique lourde"),
            ItemReferentiel("H", "Heure", "Prestation horaire"),
            ItemReferentiel("J", "Jour", "Forfait journalier"),
        )
    }

    val devises = remember {
        listOf(
            ItemReferentiel("XAF", "Franc CFA (BEAC)", "Afrique Centrale - CEMAC"),
            ItemReferentiel("XOF", "Franc CFA (BCEAO)", "Afrique de l'Ouest - UEMOA"),
            ItemReferentiel("EUR", "Euro (€)", "Zone Euro"),
            ItemReferentiel("USD", "Dollar US ($)", "États-Unis"),
            ItemReferentiel("GNF", "Franc Guinéen", "Guinée"),
            ItemReferentiel("CDF", "Franc Congolais", "RDC"),
        )
    }

    val taxes = remember {
        listOf(
            ItemReferentiel("TVA 19.25%", "Taux Standard", "Cameroun / Zone CEMAC"),
            ItemReferentiel("TVA 18%", "Taux Standard UEMOA", "Côte d'Ivoire, Sénégal"),
            ItemReferentiel("TVA 0%", "Exonéré / Export", "Régime d'exportation"),
            ItemReferentiel("AIR 2.2%", "Acompte IS", "Retenue à la source"),
        )
    }

    val itemsAffiches = when (categorieChoisie) {
        0 -> famillesTypes
        1 -> unites
        2 -> devises
        else -> taxes
    }

    Column(Modifier.fillMaxSize()) {
        MissaTopAppBar(
            title = stringResource(R.string.refer_title),
            onBack = onBack,
            couleurFond = GrisReferentiel.copy(alpha = 0.15f),
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // --- Hero Synthèse Référentiels ---
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = GrisReferentiel.copy(alpha = 0.12f),
                ) {
                    Column(Modifier.fillMaxWidth().padding(14.dp)) {
                        Text(
                            stringResource(R.string.ref_titre_synthese),
                            fontSize = 11.sp,
                            color = MissaMuted,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            stringResource(R.string.ref_norme_ohada),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MissaInk,
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                stringResource(R.string.ref_familles_count, famillesTypes.size),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = GrisReferentiel,
                            )
                            Text(
                                stringResource(R.string.ref_unites_count, unites.size),
                                fontSize = 11.sp,
                                color = MissaMuted,
                            )
                        }
                    }
                }
            }

            // --- Structure Matricielle 4 Tuiles ---
            item {
                Text(
                    stringResource(R.string.ref_matrice_titre),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MissaInk,
                )
            }

            item {
                val tuiles = listOf(
                    TuileReferentielSpec(
                        icone = Iv.Folder,
                        titre = stringResource(R.string.ref_tuile_familles),
                        sousTitre = famillesTypes.size.toString(),
                        estActif = categorieChoisie == 0,
                        onClick = { categorieChoisie = 0 },
                    ),
                    TuileReferentielSpec(
                        icone = Iv.Straighten,
                        titre = stringResource(R.string.ref_tuile_unites),
                        sousTitre = unites.size.toString(),
                        estActif = categorieChoisie == 1,
                        onClick = { categorieChoisie = 1 },
                    ),
                    TuileReferentielSpec(
                        icone = Iv.Bank,
                        titre = stringResource(R.string.ref_tuile_devises),
                        sousTitre = devises.size.toString(),
                        estActif = categorieChoisie == 2,
                        onClick = { categorieChoisie = 2 },
                    ),
                    TuileReferentielSpec(
                        icone = Iv.Percent,
                        titre = stringResource(R.string.ref_tuile_taxes),
                        sousTitre = taxes.size.toString(),
                        estActif = categorieChoisie == 3,
                        onClick = { categorieChoisie = 3 },
                    ),
                )

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    tuiles.forEach { tuile ->
                        TuileReferentiel(
                            icone = tuile.icone,
                            titre = tuile.titre,
                            sousTitre = tuile.sousTitre,
                            estActif = tuile.estActif,
                            modifier = Modifier.weight(1f),
                            onClick = tuile.onClick,
                        )
                    }
                }
            }

            // --- Registre des Éléments du Référentiel ---
            item {
                Text(
                    stringResource(R.string.ref_titre_registre),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MissaInk,
                )
            }

            items(itemsAffiches, key = { "${it.code}_${it.libelle}" }) { itemRef ->
                CarteReferentiel(itemRef)
            }
        }
    }
}

@Composable
private fun TuileReferentiel(
    icone: Int,
    titre: String,
    sousTitre: String,
    estActif: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (estActif) GrisReferentiel.copy(alpha = 0.15f) else Color.White,
        border = BorderStroke(1.dp, if (estActif) GrisReferentiel else MissaBorder),
        modifier = modifier
            .height(82.dp)
            .clickable(onClick = onClick),
    ) {
        Column(
            Modifier.padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(painterResource(icone), null, tint = MissaInk, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(4.dp))
            Text(titre, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MissaInk, maxLines = 1)
            Text(sousTitre, fontSize = 9.sp, color = MissaMuted, maxLines = 1)
        }
    }
}

@Composable
private fun CarteReferentiel(
    item: ItemReferentiel,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, MissaBorder),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(item.libelle, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MissaInk)
                Spacer(Modifier.height(2.dp))
                Text(item.detail, fontSize = 11.5.sp, color = MissaMuted)
            }
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = GrisReferentiel.copy(alpha = 0.12f),
            ) {
                Text(
                    item.code,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = GrisReferentiel,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
    }
}
