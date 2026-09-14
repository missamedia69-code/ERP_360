package com.missa.b360.ui.admin

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.missa.b360.R
import com.missa.b360.core.backup.ResultatRestauration
import com.missa.b360.core.data.entity.BackupEntity
import com.missa.b360.core.domain.usecase.BackupUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Sauvegarde (RA-13) : sauvegarde locale + historique + restauration d'un fichier. */
@HiltViewModel
class SauvegardeViewModel @Inject constructor(
    private val useCases: BackupUseCases,
) : ViewModel() {

    data class UiState(
        val enCours: Boolean = false,
        val message: String? = null,
        val restaurationEnCours: Boolean = false,
        /** Clé de chaîne : issue de la dernière restauration. */
        val restaurationMessageRes: Int? = null,
        /** Base remplacée : l'écran doit redémarrer l'application. */
        val restaurationReussie: Boolean = false,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state
    val historique: Flow<List<BackupEntity>> = useCases.historique()

    fun sauvegarder() {
        _state.value = UiState(enCours = true)
        viewModelScope.launch {
            val fichier = useCases.sauvegarder()
            _state.value = UiState(enCours = false, message = if (fichier != null) "ok" else "err")
        }
    }

    /**
     * Restaure une sauvegarde `.db` : les données actuelles sont remplacées après
     * copie de sécurité, puis l'application doit redémarrer.
     */
    fun restaurer(source: Uri) {
        if (_state.value.restaurationEnCours) return
        _state.value = _state.value.copy(
            restaurationEnCours = true,
            restaurationMessageRes = null,
        )
        viewModelScope.launch {
            val resultat = runCatching { useCases.restaurer(source) }
                .getOrElse { ResultatRestauration.Echec(ResultatRestauration.Motif.ECHEC_COPIE) }
            _state.value = when (resultat) {
                is ResultatRestauration.Succes -> _state.value.copy(
                    restaurationEnCours = false,
                    restaurationMessageRes = R.string.obn_restaurer_ok,
                    restaurationReussie = true,
                )
                is ResultatRestauration.Echec -> _state.value.copy(
                    restaurationEnCours = false,
                    restaurationMessageRes = messageErreur(resultat.motif),
                )
            }
        }
    }

    private fun messageErreur(motif: ResultatRestauration.Motif): Int = when (motif) {
        ResultatRestauration.Motif.FICHIER_ILLISIBLE -> R.string.obn_restaurer_err_fichier
        ResultatRestauration.Motif.FORMAT_INVALIDE -> R.string.obn_restaurer_err_format
        ResultatRestauration.Motif.VERSION_TROP_RECENTE -> R.string.obn_restaurer_err_version
        ResultatRestauration.Motif.ECHEC_COPIE -> R.string.obn_restaurer_err_copie
    }
}
