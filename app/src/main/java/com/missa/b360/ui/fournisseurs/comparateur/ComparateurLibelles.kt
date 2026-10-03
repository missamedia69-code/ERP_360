package com.missa.b360.ui.fournisseurs.comparateur

import androidx.annotation.StringRes
import com.missa.b360.R
import com.missa.b360.core.domain.model.SourcingExclusion
import com.missa.b360.core.domain.model.SourcingReason

@StringRes
internal fun SourcingReason.libelleRes(): Int = when (this) {
    SourcingReason.MOINS_CHER -> R.string.four_raison_moins_cher
    SourcingReason.PLUS_RAPIDE -> R.string.four_raison_plus_rapide
    SourcingReason.PLUS_FIABLE -> R.string.four_raison_plus_fiable
    SourcingReason.PREFERE -> R.string.four_raison_prefere
}

@StringRes
internal fun SourcingExclusion.libelleRes(): Int = when (this) {
    SourcingExclusion.NON_COMMANDABLE -> R.string.four_exclu_non_commandable
    SourcingExclusion.BLOQUE -> R.string.four_exclu_bloque
    SourcingExclusion.QTE_MIN_NON_ATTEINTE -> R.string.four_exclu_qte_min
    SourcingExclusion.SANS_PRIX -> R.string.four_exclu_sans_prix
}
