package com.missa.b360.ui.clients.components

import android.content.Context
import android.content.Intent
import android.net.Uri

/** Actions à un geste : chaque fonction ignore silencieusement l'absence d'application cible. */
private fun Context.lancer(intent: Intent) {
    runCatching { startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

internal fun chiffresTelephone(telephone: String): String = telephone.filter { it.isDigit() }

internal fun Context.appeler(telephone: String) {
    if (telephone.isBlank()) return
    lancer(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(telephone.trim()))))
}

internal fun Context.ouvrirWhatsApp(telephone: String, message: String? = null) {
    val numero = chiffresTelephone(telephone)
    if (numero.isEmpty()) return
    val texte = message?.takeIf { it.isNotBlank() }?.let { "?text=" + Uri.encode(it) }.orEmpty()
    lancer(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$numero$texte")))
}

internal fun Context.envoyerSms(telephone: String, message: String? = null) {
    if (telephone.isBlank()) return
    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + Uri.encode(telephone.trim())))
    message?.takeIf { it.isNotBlank() }?.let { intent.putExtra("sms_body", it) }
    lancer(intent)
}
