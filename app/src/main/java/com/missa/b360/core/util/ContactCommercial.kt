package com.missa.b360.core.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import java.net.URLEncoder

/**
 * Coordonnées commerciales de l'éditeur.
 *
 * Elles servent à l'utilisateur qui veut acheter son code d'activation
 * (1 code = 1 appareil = 1 an) et se règlent en un seul endroit :
 * `res/values/contact.xml`. Les valeurs de démonstration — domaine
 * `example.com` réservé par l'IANA, numéro `+237 6 00 00 00 00` attribué à
 * personne — sont détectées par [estTemoin] afin qu'aucun bouton d'achat ne
 * soit proposé tant que le canal ne joint réellement quelqu'un.
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

    /**
     * Ouvre la conversation Telegram correspondant au numéro.
     *
     * `tg://resolve?phone=` est le schéma que gère l'application ; il n'existe
     * pas d'équivalent web fiable à partir d'un simple numéro (les liens
     * `t.me/+…` désignent des invitations de groupe, pas des abonnés). Sans
     * Telegram installé, l'appel échoue proprement et l'écran le signale.
     */
    fun ouvrirTelegram(context: Context, numero: String, message: String): Boolean {
        val chiffres = numero.filter(Char::isDigit)
        if (chiffres.isEmpty()) return false
        val url = "tg://resolve?phone=$chiffres&text=" + URLEncoder.encode(message, "UTF-8")
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
