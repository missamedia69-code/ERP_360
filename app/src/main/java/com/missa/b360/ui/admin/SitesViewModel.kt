package com.missa.b360.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.missa.b360.core.data.entity.SiteEntity
import com.missa.b360.core.domain.usecase.SiteUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Multi-site (RA-21). */
@HiltViewModel
class SitesViewModel @Inject constructor(
    private val useCases: SiteUseCases,
) : ViewModel() {

    val sites: Flow<List<SiteEntity>> = useCases.observerSites()
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    fun ajouter(nom: String, type: String, adresse: String?) {
        viewModelScope.launch {
            useCases.ajouterSite(nom, type, adresse)
            _message.value = "ok"
        }
    }

    fun supprimer(site: SiteEntity) {
        viewModelScope.launch {
            val ok = useCases.supprimerSite(site)
            _message.value = if (ok) "supprime" else "principal"
        }
    }
}