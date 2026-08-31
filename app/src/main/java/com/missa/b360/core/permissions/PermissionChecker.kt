package com.missa.b360.core.permissions

import com.missa.b360.core.data.dao.RoleDao
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PermissionChecker (RA-14, RA-24, RA-25 — D2).
 * Actions : Voir / Créer / Modifier / Supprimer / Valider × module.
 */
@Singleton
class PermissionChecker @Inject constructor(
    private val roleDao: RoleDao,
) {
    enum class Action(val code: String) {
        VIEW("VIEW"),
        CREATE("CREATE"),
        EDIT("EDIT"),
        DELETE("DELETE"),
        VALIDATE("VALIDATE"),
    }

    /** @return true si le rôle accorde [action] sur [module]. Le Propriétaire a tous les droits. */
    suspend fun hasPermission(roleId: Long?, module: String, action: Action): Boolean {
        if (roleId == null) return false
        val role = roleDao.getById(roleId) ?: return false
        if (role.nom.equals("Propriétaire", ignoreCase = true)) return true
        return roleDao.permissionsFor(roleId).any {
            it.module == module && it.action == action.code && it.granted
        }
    }
}
