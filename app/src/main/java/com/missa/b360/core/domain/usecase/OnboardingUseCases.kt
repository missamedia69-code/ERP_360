package com.missa.b360.core.domain.usecase

import androidx.room.withTransaction
import com.missa.b360.core.data.dao.EnterpriseDao
import com.missa.b360.core.data.dao.PaymentMethodDao
import com.missa.b360.core.data.dao.RoleDao
import com.missa.b360.core.data.dao.SiteDao
import com.missa.b360.core.data.dao.TaxDao
import com.missa.b360.core.data.datastore.SettingsStore
import com.missa.b360.core.data.db.AppDatabase
import com.missa.b360.core.data.entity.EnterpriseEntity
import com.missa.b360.core.data.entity.PaymentMethodEntity
import com.missa.b360.core.data.entity.RoleEntity
import com.missa.b360.core.data.entity.RolePermissionEntity
import com.missa.b360.core.data.entity.SiteEntity
import com.missa.b360.core.data.entity.TaxEntity
import com.missa.b360.core.licensing.LicenceManager
import com.missa.b360.core.journal.JournalManager
import com.missa.b360.ui.navigation.AppModule
import javax.inject.Inject

/**
 * **RA-19 / D4 / D5** — Configuration de l'entreprise au 1er lancement :
 * entreprise + site principal, taxes (suggérées par pays), modes de paiement,
 * 3 rôles système (D2), démarrage de l'essai licence (RA-04) — puis **pose des
 * 4 verrous d'amont** (devise, taxes, numérotation, paiements) : plus jamais
 * modifiables ensuite. Tout en une transaction Room.
 */
class SetupEnterpriseUseCase @Inject constructor(
    private val database: AppDatabase,
    private val enterpriseDao: EnterpriseDao,
    private val siteDao: SiteDao,
    private val taxDao: TaxDao,
    private val paymentMethodDao: PaymentMethodDao,
    private val roleDao: RoleDao,
    private val settingsStore: SettingsStore,
    private val licenceManager: LicenceManager,
    private val journalManager: JournalManager,
) {
    data class Params(
        val nomEntreprise: String,
        val devise: String,
        val pays: String?,
        val tauxTaxe: Double,
        val nomSitePrincipal: String,
    )

    suspend operator fun invoke(params: Params): Boolean {
        // Reprise d'un onboarding interrompu : l'entreprise, le site et les réglages
        // initiaux ont été insérés dans la même transaction. On peut donc continuer
        // vers le PIN au lieu de bloquer l'utilisateur sur le formulaire.
        if (enterpriseDao.get() != null) return true

        database.withTransaction {
            enterpriseDao.upsert(
                EnterpriseEntity(
                    nom = params.nomEntreprise.trim(),
                    devise = params.devise,
                    langue = settingsStore.get(SettingsStore.Keys.LANGUE) ?: "fr",
                    pays = params.pays,
                ),
            )
            siteDao.insert(
                SiteEntity(nom = params.nomSitePrincipal.trim(), type = "principal", principal = true),
            )
            taxDao.insertAll(
                listOf(TaxEntity(nom = "TVA", taux = params.tauxTaxe, parDefaut = true)),
            )
            // D13 — Mobile Money = moyen générique unique, parmi les modes figés.
            paymentMethodDao.insertAll(
                listOf(
                    PaymentMethodEntity(nom = "Espèces"),
                    PaymentMethodEntity(nom = "Mobile Money"),
                    PaymentMethodEntity(nom = "Virement bancaire"),
                    PaymentMethodEntity(nom = "Chèque"),
                    PaymentMethodEntity(nom = "Carte bancaire"),
                ),
            )
            seedSystemRoles()
        }

        // RA-19 — pose des 4 verrous d'amont (devise/taxes/numérotation/paiements).
        settingsStore.set(SettingsStore.Keys.DEVISE, params.devise)
        settingsStore.lock(SettingsStore.Keys.VERROU_DEVISE)
        settingsStore.lock(SettingsStore.Keys.VERROU_TAXES)
        settingsStore.lock(SettingsStore.Keys.VERROU_NUMEROTATION)
        settingsStore.lock(SettingsStore.Keys.VERROU_PAIEMENTS)

        // RA-04 — démarrage de l'essai 7 jours.
        licenceManager.ensureTrialStarted()

        journalManager.log("ADMIN", "ONBOARDING_ENTREPRISE", "Entreprise ${params.nomEntreprise} configurée")
        return true
    }
    private suspend fun seedSystemRoles() {
        val actions = listOf("VIEW", "CREATE", "EDIT", "DELETE", "VALIDATE")
        val modules = AppModule.entries.map { it.name } + "ADMIN"

        val proprietaire = roleDao.insert(RoleEntity(nom = "Propriétaire", type = "SYSTEM"))
        val gerant = roleDao.insert(RoleEntity(nom = "Gérant", type = "SYSTEM"))
        val consultation = roleDao.insert(RoleEntity(nom = "Consultation seule", type = "SYSTEM"))

        val permissions = buildList {
            modules.forEach { module ->
                // Propriétaire : tous les droits (le UseCase le bypass aussi).
                actions.forEach { action ->
                    add(RolePermissionEntity(proprietaire, module, action, granted = true))
                    add(RolePermissionEntity(gerant, module, action, granted = true))
                }
                // Consultation seule : uniquement « Voir ».
                add(RolePermissionEntity(consultation, module, "VIEW", granted = true))
            }
        }
        roleDao.insertPermissions(permissions)
    }
}

/**
 * **D1 / RA-03** — Création du premier utilisateur (Propriétaire) avec email de
 * secours obligatoire. Le hash du PIN (déjà configuré à l'étape précédente)
 * est copié dans la fiche utilisateur — jamais le PIN en clair.
 */
class CreateOwnerUserUseCase @Inject constructor(
    private val userDao: com.missa.b360.core.data.dao.UserDao,
    private val roleDao: RoleDao,
    private val settingsStore: SettingsStore,
    private val journalManager: JournalManager,
) {
    sealed class Result {
        data class Succes(val userId: Long) : Result()
        data object EmailInvalide : Result()
        data object EmailDejaUtilise : Result()
    }

    suspend operator fun invoke(nom: String, email: String): Result {
        val emailNormalise = email.trim().lowercase()
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(emailNormalise).matches()) {
            return Result.EmailInvalide
        }
        if (userDao.findByEmail(emailNormalise) != null) return Result.EmailDejaUtilise

        val proprietaire = roleDao.getByNom("Propriétaire")
        val userId = userDao.insert(
            com.missa.b360.core.data.entity.UserEntity(
                nom = nom.trim().ifEmpty { "Propriétaire" },
                emailSecours = emailNormalise,
                pinHash = settingsStore.get(SettingsStore.Keys.PIN_HASH),
                roleId = proprietaire?.id,
                createdAt = System.currentTimeMillis(),
            ),
        )
        settingsStore.set(SettingsStore.Keys.CURRENT_USER_ID, userId.toString())
        journalManager.log("ADMIN", "ONBOARDING_UTILISATEUR", "Propriétaire créé (email de secours enregistré)")
        return Result.Succes(userId)
    }
}

/**
 * **RA-11** — Clôture de l'onboarding : le drapeau `onboarding_termine` est posé
 * (l'app démarrera désormais sur le verrou PIN) et l'action est tracée au journal.
 */
class CompleteOnboardingUseCase @Inject constructor(
    private val enterpriseDao: EnterpriseDao,
    private val settingsStore: SettingsStore,
    private val journalManager: JournalManager,
) {
    suspend operator fun invoke(): Boolean {
        val entreprise = enterpriseDao.get() ?: return false
        if (settingsStore.get(SettingsStore.Keys.ONBOARDING_TERMINE) == "true") return true
        settingsStore.set(SettingsStore.Keys.ONBOARDING_TERMINE, "true")
        settingsStore.set(SettingsStore.Keys.PROFIL_ACTIVITE, settingsStore.get(SettingsStore.Keys.PROFIL_ACTIVITE) ?: "B")
        enterpriseDao.upsert(entreprise.copy(onboardingTermine = true))
        journalManager.log("ADMIN", "ONBOARDING_TERMINE", "Configuration initiale terminée")
        return true
    }
}
