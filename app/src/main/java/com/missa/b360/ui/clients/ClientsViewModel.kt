package com.missa.b360.ui.clients

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.missa.b360.core.data.entity.BadgeLoyaltyEntity
import com.missa.b360.core.data.entity.CategoryClientEntity
import com.missa.b360.core.data.entity.ClientEntity
import com.missa.b360.core.data.entity.ClientType
import com.missa.b360.core.domain.usecase.BadgeLoyaltyUseCases
import com.missa.b360.core.domain.usecase.CategorieClientUseCases
import com.missa.b360.core.domain.usecase.CreateClientUseCase
import com.missa.b360.core.domain.usecase.DesactiverClientUseCase
import com.missa.b360.core.domain.usecase.ObserveClientsUseCase
import com.missa.b360.core.domain.usecase.UpdateClientUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** ViewModel Clients (9.2) : liste + formulaire (RC-01 doublons). */
@HiltViewModel
class ClientsViewModel @Inject constructor(
    private val observeClients: ObserveClientsUseCase,
    private val createClient: CreateClientUseCase,
    private val updateClient: UpdateClientUseCase,
    private val desactiverClient: DesactiverClientUseCase,
    private val categories: CategorieClientUseCases,
    private val badges: BadgeLoyaltyUseCases,
) : ViewModel() {

    val clients: Flow<List<ClientEntity>> = observeClients()
    val categoriesFlow: Flow<List<CategoryClientEntity>> = categories.observer()
    val badgesFlow: Flow<List<BadgeLoyaltyEntity>> = badges.observer()

    data class Resultat(val code: String? = null, val erreur: String? = null)

    private val _resultat = MutableStateFlow<Resultat?>(null)
    val resultat: StateFlow<Resultat?> = _resultat

    fun creer(
        nom: String,
        telephone: String,
        type: ClientType,
        doublonConfirme: Boolean,
    ) {
        viewModelScope.launch {
            when (val r = createClient(nom, telephone, type, doublonConfirme = doublonConfirme)) {
                is CreateClientUseCase.Result.Succes ->
                    _resultat.value = Resultat(code = r.code)
                is CreateClientUseCase.Result.DoublonPotentiel ->
                    _resultat.value = Resultat(erreur = "doublon")
                is CreateClientUseCase.Result.LicenceExpiree ->
                    _resultat.value = Resultat(erreur = "licence")
                is CreateClientUseCase.Result.TelephoneObligatoire ->
                    _resultat.value = Resultat(erreur = "telephone")
            }
        }
    }

    fun modifier(
        id: Long,
        nom: String,
        telephone: String,
        type: ClientType,
        email: String?,
        adresse: String?,
        categorieId: Long?,
        remiseDefautPct: Double,
        limiteCredit: Double?,
        badgeId: Long?,
        notes: String?,
    ) {
        viewModelScope.launch {
            val ok = updateClient(
                id = id, nom = nom, telephone = telephone, type = type,
                email = email, adresse = adresse, categorieId = categorieId,
                remiseDefautPct = remiseDefautPct, limiteCredit = limiteCredit,
                badgeId = badgeId, notes = notes,
            )
            _resultat.value = if (ok) Resultat(code = "edit") else Resultat(erreur = "err")
        }
    }

    fun desactiver(id: Long) {
        viewModelScope.launch { desactiverClient(id) }
    }

    fun creerCategorie(nom: String) {
        viewModelScope.launch { categories.creer(nom) }
    }

    fun supprimerCategorie(id: Long) {
        viewModelScope.launch { categories.supprimer(id) }
    }

    fun creerBadge(nom: String, remisePct: Double) {
        viewModelScope.launch { badges.creer(nom, remisePct) }
    }
}