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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** ViewModel Clients (9.2) : liste + formulaire et confirmation explicite RC-01. */
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

    private data class DemandeCreation(
        val nom: String,
        val telephone: String,
        val type: ClientType,
        val email: String?,
        val adresse: String?,
        val categorieId: Long?,
        val remiseDefautPct: Double,
        val limiteCredit: Double?,
        val badgeId: Long?,
        val notes: String?,
    )

    private val _resultat = MutableStateFlow<Resultat?>(null)
    val resultat: StateFlow<Resultat?> = _resultat

    private val _erreurCategorie = MutableStateFlow<String?>(null)
    val erreurCategorie: StateFlow<String?> = _erreurCategorie

    private var demandeDoublon: DemandeCreation? = null
    private var creationEnCours = false

    fun creer(
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
        lancerCreation(
            DemandeCreation(
                nom = nom,
                telephone = telephone,
                type = type,
                email = email,
                adresse = adresse,
                categorieId = categorieId,
                remiseDefautPct = remiseDefautPct,
                limiteCredit = limiteCredit,
                badgeId = badgeId,
                notes = notes,
            ),
            doublonConfirme = false,
        )
    }

    /** RC-01 : cette méthode n'est appelée qu'après l'accord explicite dans le dialogue. */
    fun confirmerDoublon() {
        val demande = demandeDoublon ?: return
        demandeDoublon = null
        _resultat.value = null
        lancerCreation(demande, doublonConfirme = true)
    }

    fun annulerDoublon() {
        demandeDoublon = null
        _resultat.value = null
    }

    fun acquitterResultat() {
        _resultat.value = null
    }

    private fun lancerCreation(demande: DemandeCreation, doublonConfirme: Boolean) {
        if (creationEnCours) return
        creationEnCours = true
        viewModelScope.launch {
            try {
                when (
                    val resultat = createClient(
                        nom = demande.nom,
                        telephone = demande.telephone,
                        type = demande.type,
                        email = demande.email,
                        adresse = demande.adresse,
                        categorieId = demande.categorieId,
                        remiseDefautPct = demande.remiseDefautPct,
                        limiteCredit = demande.limiteCredit,
                        badgeId = demande.badgeId,
                        notes = demande.notes,
                        doublonConfirme = doublonConfirme,
                    )
                ) {
                    is CreateClientUseCase.Result.Succes -> {
                        demandeDoublon = null
                        _resultat.value = Resultat(code = resultat.code)
                    }
                    is CreateClientUseCase.Result.DoublonPotentiel -> {
                        demandeDoublon = demande
                        _resultat.value = Resultat(erreur = "doublon")
                    }
                    is CreateClientUseCase.Result.LicenceExpiree ->
                        _resultat.value = Resultat(erreur = "licence")
                    is CreateClientUseCase.Result.NomObligatoire ->
                        _resultat.value = Resultat(erreur = "nom")
                    is CreateClientUseCase.Result.NomInvalide ->
                        _resultat.value = Resultat(erreur = "nom_invalide")
                    is CreateClientUseCase.Result.TelephoneObligatoire ->
                        _resultat.value = Resultat(erreur = "telephone")
                    is CreateClientUseCase.Result.TelephoneInvalide ->
                        _resultat.value = Resultat(erreur = "telephone_invalide")
                    is CreateClientUseCase.Result.EmailInvalide ->
                        _resultat.value = Resultat(erreur = "email_invalide")
                    is CreateClientUseCase.Result.DonneesInvalides ->
                        _resultat.value = Resultat(erreur = "donnees")
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                _resultat.value = Resultat(erreur = "err")
            } finally {
                creationEnCours = false
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
            val ok = try {
                updateClient(
                    id = id,
                    nom = nom,
                    telephone = telephone,
                    type = type,
                    email = email,
                    adresse = adresse,
                    categorieId = categorieId,
                    remiseDefautPct = remiseDefautPct,
                    limiteCredit = limiteCredit,
                    badgeId = badgeId,
                    notes = notes,
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                false
            }
            _resultat.value = if (ok) Resultat(code = "edit") else Resultat(erreur = "err")
        }
    }

    fun desactiver(id: Long) {
        viewModelScope.launch {
            val ok = try {
                desactiverClient(id)
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                false
            }
            if (!ok) _resultat.value = Resultat(erreur = "err")
        }
    }

    fun creerCategorie(nom: String) {
        _erreurCategorie.value = null
        viewModelScope.launch {
            _erreurCategorie.value = try {
                if (categories.creer(nom) == null) "licence" else null
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                "err"
            }
        }
    }

    fun supprimerCategorie(id: Long) {
        _erreurCategorie.value = null
        viewModelScope.launch {
            _erreurCategorie.value = try {
                when (categories.supprimer(id)) {
                    CategorieClientUseCases.SuppressionResult.Supprimee -> null
                    CategorieClientUseCases.SuppressionResult.CategorieUtilisee -> "utilisee"
                    CategorieClientUseCases.SuppressionResult.LectureSeule -> "licence"
                    CategorieClientUseCases.SuppressionResult.Introuvable -> "err"
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                "err"
            }
        }
    }

    fun creerBadge(nom: String, remisePct: Double) {
        viewModelScope.launch {
            val cree = try {
                badges.creer(nom, remisePct)
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                null
            }
            if (cree == null) _resultat.value = Resultat(erreur = "err")
        }
    }
}
