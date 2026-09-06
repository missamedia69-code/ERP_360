package com.missa.b360.core.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import java.net.URLEncoder

/**
 * Coordonnées commerciales de l'éditeur — **unique point à personnaliser**.
 *
 * Elles servent à l'utilisateur qui veut acheter son code d'activation
 * (1 code = 1 appareil = 1 an). Les valeurs livrées dans
 * `res/values/contact.xml` sont des témoins volontairement injoignables :
 * `example.com` est un domaine réservé par l'IANA et `+237 6 00 00 00 00`
 * n'est attribué à aucun abonné. Remplacez-les avant toute diffusion.
 */
object ContactCommercial {

    /** Vrai tant que la valeur configurée est encore le témoin de livraison. */
    fun estTemoin(valeur: String): Boolean {
        val nettoye = valeur.trim()
        return nettoye.isEmpty() ||
            nettoye.contains("example.com", ignoreCase = true) ||
            nettoye.filter(Char::isDigit).endsWith("600000000")
    }

    /**
     * Ouvre une conversation WhatsApp avec le message pré-rempli.
     *
     * L'URL `wa.me` est volontairement préférée au schéma `whatsapp://` : sans
     * l'application installée, elle bascule sur le navigateur au lieu d'échouer.
     */
    fun ouvrirWhatsApp(context: Context, numero: String, message: String): Boolean {
        val chiffres = numero.filter(Char::isDigit)
        if (chiffres.isEmpty()) return false
        val url = "https://wa.me/$chiffres?text=" + URLEncoder.encode(message, "UTF-8")
        return lancer(context, Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    /** Ouvre l'application e-mail avec objet et corps pré-remplis. */
    fun ouvrirEmail(context: Context, adresse: String, objet: String, message: String): Boolean {
        if (adresse.isBlank()) return false
        val uri = Uri.parse(
            "mailto:" + Uri.encode(adresse.trim()) +
                "?subject=" + Uri.encode(objet) +
                "&body=" + Uri.encode(message),
        )
        return lancer(context, Intent(Intent.ACTION_SENDTO, uri))
    }

    private fun lancer(context: Context, intent: Intent): Boolean = try {
        // L'onboarding peut s'afficher hors pile d'activités : le drapeau évite
        // un plantage quand le contexte n'est pas celui d'une activité.
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}
