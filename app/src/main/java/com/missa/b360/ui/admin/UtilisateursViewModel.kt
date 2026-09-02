package com.missa.b360.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.missa.b360.core.data.entity.RoleEntity
import com.missa.b360.core.data.entity.UserEntity
import com.missa.b360.core.domain.usecase.UserAdminUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Utilisateurs & rôles (D1/D2, RA-14). */
@HiltViewModel
class UtilisateursViewModel @Inject constructor(
    private val useCases: UserAdminUseCases,
) : ViewModel() {

    val utilisateurs: Flow<List<UserEntity>> = useCases.observerUtilisateurs()
    val roles: Flow<List<RoleEntity>> = useCases.observerRoles()

    data class Resultat(val ok: Boolean, val message: String? = null)
    private val _resultat = MutableStateFlow<Resultat?>(null)
    val resultat: StateFlow<Resultat?> = _resultat

    fun creerUtilisateur(nom: String, email: String, roleId: Long) {
        viewModelScope.launch {
            val id = useCases.creerUtilisateur(nom, email, roleId)
            _resultat.value = Resultat(id > 0, if (id > 0) null else "email invalide ou déjà utilisée")
        }
    }

    fun changerRole(userId: Long, roleId: Long) {
        viewModelScope.launch { useCases.changerRole(userId, roleId) }
    }

    fun desactiver(userId: Long) {
        viewModelScope.launch { useCases.desactiverUtilisateur(userId) }
    }
}