package com.missa.b360.core.domain.usecase

import com.missa.b360.core.data.datastore.SettingsStore
import com.missa.b360.core.security.PinHasher
import com.missa.b360.core.security.PinManager
import javax.inject.Inject

/**
 * **RA-01 / RA-02 / RA-03** — Validation du PIN à chaque ouverture.
 * 4–6 chiffres, hashé, 5 échecs → blocage croissant, email de secours obligatoire.
 */
class ValidatePinUseCase @Inject constructor(
    private val pinManager: PinManager,
) {
    sealed class Outcome {
        data object AccesAccorde : Outcome()
        data object PinNonConfigure : Outcome()
        data class Refuse(val essaisRestants: Int) : Outcome()
        data class Bloque(val jusquA: Long) : Outcome()
    }

    suspend operator fun invoke(pin: String): Outcome = when (val r = pinManager.verify(pin)) {
        is PinManager.VerifyResult.Ok -> Outcome.AccesAccorde
        is PinManager.VerifyResult.Unset -> Outcome.PinNonConfigure
        is PinManager.VerifyResult.Wrong -> Outcome.Refuse(r.essaisRestants)
        is PinManager.VerifyResult.Locked -> Outcome.Bloque(r.jusquA)
    }

    /** Configure le PIN initial (onboarding, RA-01 : 4–6 chiffres). */
    suspend fun definirPin(pin: String): Boolean {
        if (!PinHasher.isValidFormat(pin)) return false
        return pinManager.setupPin(pin)
    }

    suspend fun isConfigure(): Boolean = pinManager.isConfigured()
}

/**
 * **RA-19 / RA-07..10** — Garde-fou des verrous d'amont : refus d'écriture si verrouillé.
 * Utilisé par tous les écrans de réglage (devise, taxes, numérotation, paiements).
 */
class CheckUpstreamLockUseCase @Inject constructor(
    private val settingsStore: SettingsStore,
) {
    /** @return true si l'écriture est autorisée (clé non verrouillée). */
    suspend fun ecritureAutorisee(cle: String): Boolean = !settingsStore.isLocked(cle)

    /** Verrouille une clé au premier usage (RA-19). */
    suspend fun verrouiller(cle: String) = settingsStore.lock(cle)
}
