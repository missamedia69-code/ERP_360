package com.missa.b360.ui.clients.form

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.missa.b360.core.data.dao.ClientDao
import com.missa.b360.core.data.entity.BadgeLoyaltyEntity
import com.missa.b360.core.data.entity.CategoryClientEntity
import com.missa.b360.core.domain.usecase.BadgeLoyaltyUseCases
import com.missa.b360.core.domain.usecase.CategorieClientUseCases
import com.missa.b360.core.domain.usecase.ClientDefaultsUseCase
import com.missa.b360.core.domain.usecase.ClientProfileUseCase
import com.missa.b360.core.domain.usecase.UpdateClientUseCase
import com.missa.b360.ui.clients.ClientRoutes
import com.missa.b360.ui.clients.components.ClientNotice
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class ClientEditUiState(
    val chargement: Boolean = true,
    val erreur: Boolean = false,
    val introuvable: Boolean = false,
    val draft: ClientDraft = ClientDraft(),
    val erreurs: Set<DraftField> = emptySet(),
    val ouvertes: Set<ClientSection> = setOf(ClientSection.IDENTITE),
    val categories: List<CategoryClientEntity> = emptyList(),
    val badges: List<BadgeLoyaltyEntity> = emptyList(),
    val enCours: Boolean = false,
    val notice: ClientNotice? = null,
    val sauve: Boolean = false,
)

/**
 * Édition d'une fiche en sections pliables. Le brouillon vit dans le `SavedStateHandle` (JSON) :
 * une rotation ou l'arrêt du processus ne fait perdre aucune saisie.
 */
@HiltViewModel
class ClientEditViewModel @Inject constructor(
    private val savedState: SavedStateHandle,
    private val clientDao: ClientDao,
    private val profil: ClientProfileUseCase,
    private val defaults: ClientDefaultsUseCase,
    private val categories: CategorieClientUseCases,
    private val badges: BadgeLoyaltyUseCases,
    private val updateClient: UpdateClientUseCase,
) : ViewModel() {

    val clientId: Long = savedState.get<Long>(ClientRoutes.ARG_ID) ?: -1L

    private val _etat = MutableStateFlow(ClientEditUiState(ouvertes = sectionsSauvees()))
    val etat: StateFlow<ClientEditUiState> = _etat.asStateFlow()

    init {
        charger()
        viewModelScope.launch {
            try {
                _etat.update { it.copy(categories = categories.observer().first(), badges = badges.observer().first()) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                Unit
            }
        }
    }

    fun charger() {
        _etat.update { it.copy(chargement = true, erreur = false) }
        viewModelScope.launch {
            try {
                val client = clientDao.getById(clientId)
                if (client == null) {
                    _etat.update { it.copy(chargement = false, introuvable = true) }
                    return@launch
                }
                val sauve = ClientDraftMapper.depuisJson(savedState.get<String>(CLE_BROUILLON))
                val brouillon = sauve ?: ClientDraftMapper.depuis(
                    client = client,
                    contacts = profil.observeContacts(clientId).first(),
                    adresses = profil.observeAddresses(clientId).first(),
                    codePaysParDefaut = defaults().codePays,
                )
                _etat.update { it.copy(chargement = false, draft = brouillon) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _etat.update { it.copy(chargement = false, erreur = true) }
            }
        }
    }

    fun modifier(transformation: ClientDraft.() -> ClientDraft) {
        _etat.update { courant ->
            val nouveau = courant.draft.transformation()
            savedState[CLE_BROUILLON] = ClientDraftMapper.versJson(nouveau)
            courant.copy(draft = nouveau, erreurs = emptySet())
        }
    }

    fun basculerSection(section: ClientSection) {
        _etat.update { courant ->
            val ouvertes = if (section in courant.ouvertes) courant.ouvertes - section else courant.ouvertes + section
            savedState[CLE_SECTIONS] = ArrayList(ouvertes.map { it.name })
            courant.copy(ouvertes = ouvertes)
        }
    }

    fun noticeLue() = _etat.update { it.copy(notice = null) }

    fun enregistrer() {
        val courant = _etat.value
        if (courant.enCours || courant.chargement) return
        val controle = ClientDraftMapper.construire(courant.draft, clientId)
        val demande = controle.demande
        if (demande == null) {
            // Ouvre toutes les sections qui contiennent une erreur, pour que rien ne reste caché.
            val aOuvrir = controle.erreurs.map { it.section }.toSet()
            _etat.update { it.copy(erreurs = controle.erreurs, ouvertes = it.ouvertes + aOuvrir) }
            return
        }
        _etat.update { it.copy(enCours = true) }
        viewModelScope.launch {
            try {
                val ok = updateClient(
                    id = clientId,
                    nom = demande.nom,
                    telephone = demande.telephone,
                    type = demande.type,
                    email = demande.email,
                    adresse = demande.adresse,
                    categorieId = demande.categorieId,
                    siteId = demande.siteId,
                    remiseDefautPct = demande.remiseDefautPct,
                    limiteCredit = demande.limiteCredit,
                    badgeId = demande.badgeId,
                    notes = demande.notes,
                    profile = demande.profile,
                )
                if (ok) savedState.remove<String>(CLE_BROUILLON)
                _etat.update { it.copy(enCours = false, sauve = ok, notice = if (ok) null else ClientNotice.ERREUR) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _etat.update { it.copy(enCours = false, notice = ClientNotice.ERREUR) }
            }
        }
    }

    /** Abandon explicite : le brouillon sauvegardé est supprimé. */
    fun abandonner() {
        savedState.remove<String>(CLE_BROUILLON)
    }

    private fun sectionsSauvees(): Set<ClientSection> {
        val noms = savedState.get<ArrayList<String>>(CLE_SECTIONS) ?: return setOf(ClientSection.IDENTITE)
        return noms.mapNotNull { nom -> ClientSection.entries.firstOrNull { it.name == nom } }.toSet()
    }

    private companion object {
        const val CLE_BROUILLON = "brouillon"
        const val CLE_SECTIONS = "sections"
    }
}
