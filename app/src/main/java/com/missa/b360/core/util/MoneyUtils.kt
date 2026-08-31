package com.missa.b360.core.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * MoneyUtils — formatage des montants dans la devise de l'entreprise (RA-07).
 * Les montants ne sont **jamais convertis** (RA-07) : Double + devise ISO 4217.
 */
object MoneyUtils {

    /** Formate un montant : séparateurs de milliers, 2 décimales, symbole devise. */
    fun format(montant: Double, devise: String): String {
        val symbols = DecimalFormatSymbols(Locale.getDefault())
        val df = DecimalFormat("#,##0.00", symbols)
        return "${df.format(montant)} $devise"
    }

    /** Formate sans symbole (saisis, exports CSV/Excel). */
    fun formatBrut(montant: Double): String {
        val df = DecimalFormat("0.00", DecimalFormatSymbols(Locale.ROOT))
        return df.format(montant)
    }
}

/** Devises ISO 4217 courantes (réglage 9.1, verrou au premier usage — D4, défaut USD). */
object Iso4217 {
    data class Devise(val code: String, val nom: String)

    val DEFAUT = "USD"

    val COMMUNES = listOf(
        Devise("USD", "Dollar américain"),
        Devise("EUR", "Euro"),
        Devise("XAF", "Franc CFA (BEAC)"),
        Devise("XOF", "Franc CFA (BCEAO)"),
        Devise("GBP", "Livre sterling"),
        Devise("CHF", "Franc suisse"),
        Devise("CAD", "Dollar canadien"),
        Devise("CNY", "Yuan (RMB)"),
        Devise("MAD", "Dirham marocain"),
        Devise("NGN", "Naira"),
        Devise("BRL", "Réal brésilien"),
        Devise("INR", "Roupie indienne"),
    )

    /** Taux de taxes suggérés par pays (D5) — configuration 9.1. */
    val TAXES_SUGGEREES = mapOf(
        "Cameroun" to 19.25,
        "Côte d'Ivoire" to 18.0,
        "Sénégal" to 18.0,
        "France" to 20.0,
        "Espagne" to 21.0,
        "Maroc" to 20.0,
    )
}

/** Utilitaires de dates (horodatages en epoch ms). */
object DateUtils {
    private val FORMAT_DATE = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val FORMAT_DATE_HEURE = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    fun formatDate(timestamp: Long): String = FORMAT_DATE.format(Date(timestamp))

    fun formatDateHeure(timestamp: Long): String = FORMAT_DATE_HEURE.format(Date(timestamp))
}
