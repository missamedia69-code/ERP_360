package com.missa.b360.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.missa.b360.core.data.dao.SiteDao
import com.missa.b360.core.data.entity.SiteEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminSitesViewModel @Inject constructor(
    private val siteDao: SiteDao,
) : ViewModel() {

    private val _filtreType = MutableStateFlow<String?>(null)
    val filtreType: StateFlow<String?> = _filtreType

    val sites: StateFlow<List<SiteEntity>> = combine(
        siteDao.observeAll(),
        _filtreType,
    ) { tous, f ->
        if (f == null) tous else tous.filter { it.type.equals(f, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val totalSites: StateFlow<List<SiteEntity>> = siteDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun filtrer(type: String?) {
        _filtreType.value = if (_filtreType.value == type) null else type
    }

    fun creer(nom: String, adresse: String?, type: String, principal: Boolean = false) {
        if (nom.isBlank()) return
        viewModelScope.launch {
            siteDao.insert(
                SiteEntity(
                    nom = nom.trim(),
                    adresse = adresse?.trim()?.ifBlank { null },
                    type = type.trim(),
                    principal = principal,
                ),
            )
        }
    }

    fun supprimer(siteId: Long) {
        viewModelScope.launch {
            siteDao.delete(siteId)
        }
    }
}
