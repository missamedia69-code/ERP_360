package com.missa.b360.ui.fournisseurs.comparateur

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.missa.b360.core.data.dao.FournisseurItemDao
import com.missa.b360.core.data.dao.ProductDao
import com.missa.b360.core.domain.model.FournisseurComparateurRules
import com.missa.b360.core.domain.model.SourcingRanking
import com.missa.b360.core.domain.model.SourcingRules
import com.missa.b360.core.domain.usecase.GetEnterpriseUseCase
import com.missa.b360.core.domain.usecase.ObserveFournisseurPortefeuilleUseCase
import com.missa.b360.core.util.Iso4217
import com.missa.b360.ui.fournisseurs.FournisseurRoutes
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class ArticleComparable(val id: Long, val code: String, val nom: String, val fournisseurs: Int)

data class FournisseurComparateurUiState(
    val chargement: Boolean = true,
    /** Articles liés à au moins un fournisseur, filtrés par la recherche. */
    val articles: List<ArticleComparable> = emptyList(),
    val articleChoisi: ArticleComparable? = null,
    val recherche: String = "",
    val quantite: String = "1",
    val classement: List<SourcingRanking> = emptyList(),
    val devise: String = Iso4217.DEVISE_REPLI,
)

private data class Saisie(val produitId: Long? = null, val recherche: String = "", val quantite: String = "1")

/** Comparateur : quel fournisseur choisir pour un article (prix, délai, fiabilité mesurée). */
@HiltViewModel
class FournisseurComparateurViewModel @Inject constructor(
    savedState: SavedStateHandle,
    observePortefeuille: ObserveFournisseurPortefeuilleUseCase,
    productDao: ProductDao,
    itemDao: FournisseurItemDao,
    getEnterprise: GetEnterpriseUseCase,
) : ViewModel() {

    private val saisie = MutableStateFlow(
        Saisie(produitId = savedState.get<Long>(FournisseurRoutes.ARG_PRODUIT)?.takeIf { it > 0L }),
    )
    private val devise = getEnterprise.observer().map { it?.devise ?: Iso4217.DEVISE_REPLI }

    val etat: StateFlow<FournisseurComparateurUiState> = combine(
        observePortefeuille(),
        productDao.observeAll(),
        itemDao.observeActifs(),
        saisie,
        devise,
    ) { portefeuille, produits, liaisons, s, d ->
        val nombre = FournisseurComparateurRules.fournisseursParArticle(liaisons)
        val articles = produits.mapNotNull { p ->
            nombre[p.id]?.let { ArticleComparable(p.id, p.code, p.nom, it) }
        }
        val choisi = articles.firstOrNull { it.id == s.produitId }
        val classement = if (choisi == null) {
            emptyList()
        } else {
            val candidats = FournisseurComparateurRules.candidats(
                portefeuille.lignes,
                liaisons.filter { it.productId == choisi.id },
                portefeuille.now,
            )
            val quantite = s.quantite.replace(',', '.').toDoubleOrNull()?.takeIf { it > 0.0 } ?: 1.0
            SourcingRules.classer(candidats, quantite, portefeuille.now)
        }
        val filtre = s.recherche.trim()
        FournisseurComparateurUiState(
            chargement = false,
            articles = articles.filter {
                filtre.isEmpty() || it.nom.contains(filtre, ignoreCase = true) || it.code.contains(filtre, ignoreCase = true)
            },
            articleChoisi = choisi,
            recherche = s.recherche,
            quantite = s.quantite,
            classement = classement,
            devise = d,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FournisseurComparateurUiState())

    fun choisir(produitId: Long?) = saisie.update { it.copy(produitId = produitId) }

    fun rechercher(texte: String) = saisie.update { it.copy(recherche = texte) }

    fun quantite(texte: String) = saisie.update { it.copy(quantite = texte) }
}
