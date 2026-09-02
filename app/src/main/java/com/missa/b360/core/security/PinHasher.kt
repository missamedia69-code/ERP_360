package com.missa.b360.core.security

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Hashage du PIN (RA-01) : **jamais en clair, jamais loggé**.
 * PBKDF2WithHmacSHA256, sel aléatoire 128 bits, 120 000 itérations.
 * Format stocké : "base64(sel):base64(hash)".
 * (java.util.Base64 — compatible minSdk 26 et tests unitaires JVM.)
 */
object PinHasher {

    private const val ITERATIONS = 120_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_BYTES = 16

    fun newSalt(): String {
        val salt = ByteArray(SALT_BYTES)
        SecureRandom().nextBytes(salt)
        return Base64.getEncoder().encodeToString(salt)
    }

    fun hash(pin: String, salt: String): String {
        val spec = PBEKeySpec(
            pin.toCharArray(),
            Base64.getDecoder().decode(salt),
            ITERATIONS,
            KEY_LENGTH_BITS,
        )
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hash = factory.generateSecret(spec).encoded
        spec.clearPassword()
        return Base64.getEncoder().encodeToString(hash)
    }

    /** Hash complet à stocker (sel + hash), pour un PIN donné. */
    fun encode(pin: String): String {
        val salt = newSalt()
        return "$salt:${hash(pin, salt)}"
    }

    /** Vérifie un PIN candidat contre un encodage "salt:hash". Temps constant. */
    fun verify(pin: String, encoded: String): Boolean {
        val parts = encoded.split(':')
        if (parts.size != 2) return false
        val candidate = hash(pin, parts[0])
        return constantTimeEquals(candidate, parts[1])
    }

    /** PIN : 4 à 6 chiffres (D1). */
    fun isValidFormat(pin: String): Boolean = pin.length in 4..6 && pin.all { it.isDigit() }

    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var result = 0
        for (i in a.indices) result = result or (a[i].code xor b[i].code)
        return result == 0
    }
}
