package com.missa.b360.core.util

fun String.toMoneyOrNull(): Double? = trim()
    .takeIf { it.isNotEmpty() }
    ?.replace(',', '.')
    ?.toDoubleOrNull()
    ?.takeIf { it.isFinite() }

fun String.filterMoneyInput(): String = filter { it.isDigit() || it == ',' || it == '.' }

fun Double.toInputAmount(): String =
    if (this % 1.0 == 0.0) toInt().toString() else toString()
