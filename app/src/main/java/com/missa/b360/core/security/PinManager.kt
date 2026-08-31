package com.missa.b360.core.security

import com.missa.b360.core.data.datastore.SettingsStore
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PinManager (RA-01, RA-02, RA-03).
 *
 * - PIN 4–6 chiffres, hashé (PBKDF2), jamais en clair, jamais loggé.
 * - 5 échecs → blocage croissant : 30 s, 1 min, 5 min, 15 min, 30 min…
 * - Le hash est conservé dans SettingsStore (jamais de log du PIN).
 */
@Singleton
class PinManager @Inject constructor(
    private val settingsStore: SettingsStore,
) {
    sealed class VerifyResult {
        data object Ok : VerifyResult()
        data object Unset : VerifyResult()
        /** Échec : [essaisRestants] avant blocage. */
        data class Wrong(val essaisRestants: Int) : VerifyResult()
        /** Compte bloqué jusqu'à [jusquA] (epoch ms). */
        data class Locked(val jusquA: Long) : VerifyResult()
    }

    // RA-02 : blocage croissant après échecs répétés.
    private val blocagesMs = listOf(
        30_000L, // 30 s
        60_000L, // 1 min
        300_000L, // 5 min
        900_000L, // 15 min
        1_800_000L, // 30 min
        3_600_000L, // 1 h (au-delà de 5 échecs)
    )

    val maxEssais = 5

    suspend fun isConfigured(): Boolean =
        !settingsStore.get(SettingsStore.Keys.PIN_HASH).isNullOrBlank()

    /** Configure le PIN initial (onboarding). Échoue si format invalide (RA-01 : 4–6 chiffres). */
    suspend fun setupPin(pin: String): Boolean {
        if (!PinHasher.isValidFormat(pin)) return false
        settingsStore.set(SettingsStore.Keys.PIN_HASH, PinHasher.encode(pin))
        settingsStore.setPinFailCount(0)
        settingsStore.setPinLockUntil(0)
        return true
    }

    /** Vérifie le PIN et gère le compteur d'échecs / le blocage. */
    suspend fun verify(pin: String, now: Long = System.currentTimeMillis()): VerifyResult {
        val encoded = settingsStore.get(SettingsStore.Keys.PIN_HASH)
            ?: return VerifyResult.Unset

        val lockUntil = settingsStore.pinLockUntil()
        if (now < lockUntil) return VerifyResult.Locked(lockUntil)

        return if (PinHasher.verify(pin, encoded)) {
            settingsStore.setPinFailCount(0)
            settingsStore.setPinLockUntil(0)
            VerifyResult.Ok
        } else {
            val failCount = settingsStore.pinFailCount() + 1
            settingsStore.setPinFailCount(failCount)
            if (failCount >= maxEssais) {
                val duree = blocagesMs[(failCount - maxEssais).coerceAtMost(blocagesMs.size - 1)]
                val until = now + duree
                settingsStore.setPinLockUntil(until)
                VerifyResult.Locked(until)
            } else {
                VerifyResult.Wrong(maxEssais - failCount)
            }
        }
    }

    /** Change le PIN (après vérification de l'ancien ou via email de secours — RA-03). */
    suspend fun changePin(nouveauPin: String): Boolean = setupPin(nouveauPin)
}
