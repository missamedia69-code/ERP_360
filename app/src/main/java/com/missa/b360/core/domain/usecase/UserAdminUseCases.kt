package com.missa.b360.core.domain.usecase

import com.missa.b360.core.data.dao.RoleDao
import com.missa.b360.core.data.dao.UserDao
import com.missa.b360.core.data.entity.RoleEntity
import com.missa.b360.core.data.entity.RolePermissionEntity
import com.missa.b360.core.data.entity.UserEntity
import com.missa.b360.core.journal.JournalManager
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** D1 / D2 / RA-14 — Gestion des utilisateurs et rôles. */
class UserAdminUseCases @Inject constructor(
    private val userDao: UserDao,
    private val roleDao: RoleDao,
    private val journalManager: JournalManager,
) {
    fun observerUtilisateurs(): Flow<List<UserEntity>> = userDao.observeAll()
    fun observerRoles(): Flow<List<RoleEntity>> = roleDao.observeAll()
    suspend fun getRole(id: Long): RoleEntity? = roleDao.getById(id)

    suspend fun creerUtilisateur(nom: String, email: String, roleId: Long): Long {
        val id = userDao.insert(
            UserEntity(
                nom = nom.trim(),
                emailSecours = email.trim().lowercase(),
                roleId = roleId,
                createdAt = System.currentTimeMillis(),
            ),
        )
        journalManager.log("ADMIN", "UTILISATEUR_CREE", "Utilisateur $nom créé")
        return id
    }

    suspend fun changerRole(userId: Long, nouveauRoleId: Long) {
        val user = userDao.getById(userId) ?: return
        userDao.update(user.copy(roleId = nouveauRoleId))
        journalManager.log("ADMIN", "ROLE_MODIFIE", "Rôle changé pour ${user.nom}")
    }

    suspend fun desactiverUtilisateur(userId: Long) {
        val user = userDao.getById(userId) ?: return
        userDao.update(user.copy(actif = false))
        journalManager.log("ADMIN", "UTILISATEUR_DESACTIVE", "Utilisateur ${user.nom} désactivé")
    }

    suspend fun creerRolePersonnalise(nom: String, modele: String?): Long {
        val id = roleDao.insert(RoleEntity(nom = nom.trim(), type = "CUSTOM", modele = modele))
        journalManager.log("ADMIN", "ROLE_CREE", "Rôle personnalisé $nom créé")
        return id
    }

    suspend fun definirPermission(roleId: Long, module: String, action: String, granted: Boolean) {
        roleDao.insertPermission(RolePermissionEntity(roleId, module, action, granted))
        journalManager.log("ADMIN", "PERMISSION_MODIFIEE", "Permission $action sur $module = $granted")
    }
}