package com.missa.b360.core.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: androidx.datastore.core.DataStore<Preferences> by preferencesDataStore(
    name = "missa_b360_settings",
)

/**
 * SettingsStore — réglages clé/valeur + **verrous d'amont** (RA-07..10 / RA-19).
 *
 * Les 4 verrous (devise, taxes, numérotation, paiements) sont `locked` dès le premier
 * usage : toute tentative d'écriture sur une clé verrouillée est **refusée**.
 * Utilisé aussi pour l'état du verrou PIN (échecs, blocage) — jamais le PIN lui-même.
 */
@Singleton
class SettingsStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    // --- Clés de réglages ---
    object Keys {
        const val LANGUE = "langue"
        const val DEVISE = "devise"
        const val PAYS = "pays"
        const val PROFIL_ACTIVITE = "profil_activite"
        const val PALIER_TAILLE = "palier_taille"
        const val ONBOARDING_TERMINE = "onboarding_termine"
        const val VIDEO_SPLASH_ACTIVE = "video_splash_active"
        const val VERROU_DEVISE = "verrou_devise"
        const val VERROU_TAXES = "verrou_taxes"
        const val VERROU_NUMEROTATION = "verrou_numerotation"
        const val VERROU_PAIEMENTS = "verrou_paiements"
        const val REFERENTIEL_COMPTABLE = "referentiel_comptable"
        const val REFERENTIEL_PAIE = "referentiel_paie"
        const val FREQUENCE_SAUVGARDE = "frequence_sauvegarde"
        const val PIN_FAIL_COUNT = "pin_fail_count"
        const val PIN_LOCK_UNTIL = "pin_lock_until"
        const val PIN_HASH = "pin_hash"
        const val CURRENT_USER_ID = "current_user_id"
    }

    fun lockedKeys() = setOf(
        Keys.VERROU_DEVISE,
        Keys.VERROU_TAXES,
        Keys.VERROU_NUMEROTATION,
        Keys.VERROU_PAIEMENTS,
    )

    private val lockedValues = setOf(
        Keys.DEVISE,
        Keys.REFERENTIEL_COMPTABLE,
        Keys.REFERENTIEL_PAIE,
    )

    fun observe(key: String): Flow<String?> =
        context.dataStore.data.map { prefs -> prefs[stringPreferencesKey(key)] }

    suspend fun get(key: String): String? =
        context.dataStore.data.first()[stringPreferencesKey(key)]

    suspend fun getLong(key: String): Long? {
        val value = get(key) ?: return null
        return value.toLongOrNull()
    }

    suspend fun set(key: String, value: String) = write(key, value)

    suspend fun setLong(key: String, value: Long) = write(key, value.toString())

    /**
     * RA-19 — écriture refusée si la clé relève d'un verrou d'amont activé.
     * @return true si l'écriture a été effectuée, false si elle a été refusée.
     */
    suspend fun setIfNotLocked(key: String, value: String): Boolean {
        val verrouActif = when (key) {
            Keys.DEVISE -> get(Keys.VERROU_DEVISE) == "true"
            else -> false
        }
        if (verrouActif) return false
        write(key, value)
        return true
    }

    private suspend fun write(key: String, value: String) {
        context.dataStore.edit { prefs ->
            prefs[stringPreferencesKey(key)] = value
        }
    }

    /** Active un verrou d'amont (au premier usage — RA-19). */
    suspend fun lock(key: String) {
        context.dataStore.edit { prefs ->
            prefs[stringPreferencesKey(key)] = "true"
        }
    }

    suspend fun isLocked(key: String): Boolean = get(key) == "true"

    /** Compteur d'échecs PIN (RA-02) — jamais le PIN lui-même. */
    suspend fun pinFailCount(): Int = getLong(Keys.PIN_FAIL_COUNT)?.toInt() ?: 0

    suspend fun setPinFailCount(count: Int) {
        if (count <= 0) {
            context.dataStore.edit { prefs -> prefs.remove(longKey(Keys.PIN_FAIL_COUNT)) }
        } else {
            setLong(Keys.PIN_FAIL_COUNT, count.toLong())
        }
    }

    suspend fun pinLockUntil(): Long = getLong(Keys.PIN_LOCK_UNTIL) ?: 0L

    suspend fun setPinLockUntil(timestamp: Long) {
        if (timestamp <= 0L) {
            context.dataStore.edit { prefs -> prefs.remove(longKey(Keys.PIN_LOCK_UNTIL)) }
        } else {
            setLong(Keys.PIN_LOCK_UNTIL, timestamp)
        }
    }

    private fun longKey(key: String): Preferences.Key<Long> = longPreferencesKey(key)
}
