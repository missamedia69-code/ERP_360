package com.missa.b360.ui.fournisseurs.comparateur

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.missa.b360.R
import com.missa.b360.core.domain.model.SourcingRanking
import com.missa.b360.ui.components.MissaChampTexte
import com.missa.b360.ui.components.MissaClavier
import com.missa.b360.ui.components.MissaEmptyState
import com.missa.b360.ui.components.MissaTopAppBar
import com.missa.b360.ui.fournisseurs.components.BadgeAptitude
import com.missa.b360.ui.fournisseurs.components.BoutonFournisseur
import com.missa.b360.ui.fournisseurs.components.FournisseurCouleurs
import com.missa.b360.ui.icons.Iv
import com.missa.b360.ui.stock.fmtValeur
import com.missa.b360.ui.theme.MissaInk
import com.missa.b360.ui.theme.MissaMuted
import com.missa.b360.ui.theme.OnbConfigCard

/** Route `fournisseurs_comparateur?produit=` : classement des fournisseurs liés à un article. */
@Composable
fun FournisseurComparateurScreen(
    onBack: () -> Unit,
    onOuvrirFournisseur: (Long) -> Unit,
    viewModel: FournisseurComparateurViewModel = hiltViewModel(),
) {
    val etat by viewModel.etat.collectAsStateWithLifecycle()
    val article = etat.articleChoisi
    Column(Modifier.fillMaxSize().background(Color.White)) {
        MissaTopAppBar(
            title = stringResource(R.string.four_comparateur_titre),
            onBack = if (article != null) ({ viewModel.choisir(null) }) else onBack,
            titreCentre = false,
        )
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 10.dp, end = 10.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (article == null) {
                item {
                    MissaChampTexte(
                        etat.recherche,
                        { viewModel.rechercher(it) },
                        stringResource(R.string.four_comparateur_recherche),
                        icone = Iv.Search,
                    )
                }
                if (!etat.chargement && etat.articles.isEmpty()) {
                    item {
                        MissaEmptyState(
                            icon = Iv.Inventory2,
                            title = stringResource(R.string.four_comparateur_aucun_article),
                            description = stringResource(R.string.four_comparateur_aucun_article_desc),
                        )
                    }
                }
                items(etat.articles, key = { it.id }) { a ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = OnbConfigCard,
                        modifier = Modifier.fillMaxWidth().clickable { viewModel.choisir(a.id) },
                    ) {
                        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(a.nom, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MissaInk, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(a.code, fontSize = 11.sp, color = MissaMuted, maxLines = 1)
                            }
                            Text(stringResource(R.string.four_comparateur_nb_fournisseurs, a.fournisseurs), fontSize = 11.sp, color = MissaMuted)
                        }
                    }
                }
            } else {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(article.nom, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MissaInk)
                        MissaChampTexte(
                            etat.quantite,
                            { viewModel.quantite(it) },
                            stringResource(R.string.four_comparateur_quantite),
                            clavier = MissaClavier.DECIMAL,
                        )
                        BoutonFournisseur(stringResource(R.string.four_comparateur_changer), Modifier.fillMaxWidth(), plein = false) {
                            viewModel.choisir(null)
                        }
                    }
                }
                items(etat.classement) { rang ->
                    CarteClassement(rang, etat.devise) { onOuvrirFournisseur(rang.candidat.fournisseurId) }
                }
            }
        }
    }
}

@Composable
private fun CarteClassement(rang: SourcingRanking, devise: String, onClick: () -> Unit) {
    val c = rang.candidat
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = OnbConfigCard,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                rang.rang?.let {
                    Text(stringResource(R.string.four_comparateur_rang, it), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = FournisseurCouleurs.Nuit)
                }
                Text(
                    c.nom,
                    fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MissaInk,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                )
                BadgeAptitude(c.aptitude)
            }
            Text(
                c.prix?.let { fmtValeur(it, devise) } ?: stringResource(R.string.four_comparateur_prix_inconnu),
                fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MissaInk,
            )
            val delai = if (c.delaiJours > 0) {
                stringResource(R.string.four_comparateur_delai_jours, c.delaiJours)
            } else {
                stringResource(R.string.four_comparateur_delai_inconnu)
            }
            val fiabilite = c.fiabilite?.let { stringResource(R.string.four_liste_fiabilite, it) }
                ?: stringResource(R.string.four_liste_fiabilite_nd)
            Text("$delai · $fiabilite", fontSize = 11.sp, color = MissaMuted)
            if (c.quantiteMin > 0.0) {
                Text(stringResource(R.string.four_comparateur_qte_min, fmtQuantiteMin(c.quantiteMin)), fontSize = 11.sp, color = MissaMuted)
            }
            if (rang.raisons.isNotEmpty()) {
                Text(
                    rang.raisons.map { stringResource(it.libelleRes()) }.joinToString(" · "),
                    fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = FournisseurCouleurs.Pret,
                )
            }
            rang.exclusion?.let {
                Text(stringResource(it.libelleRes()), fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = FournisseurCouleurs.Bloque)
            }
        }
    }
}

private fun fmtQuantiteMin(valeur: Double): String =
    if (valeur == Math.rint(valeur)) valeur.toLong().toString() else valeur.toString()
